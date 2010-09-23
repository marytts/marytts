/**
 * Copyright 2009 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package marytts.util;

import java.io.File;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import marytts.server.MaryProperties;

/**
 * @author marc
 *
 */
public class MaryCache
{
    private static MaryCache maryCache;
    
    /**
     * Try to get a MaryCache object. This will either return a previously
     * created MaryCache, or if none exists, it will try to create one.
     * @see #haveCache if you just want to check if a cache exists.
     * @return a MaryCache object, or null if none could be created.
     */
    public static MaryCache getCache()
    {
        if (maryCache == null) {
            try {
                File targetFile = new File(MaryProperties.getFilename("cache.file", "maryCache"));
                File directory = targetFile.getParentFile();
                if (!directory.isDirectory()) {
                    directory.mkdirs();
                }
                maryCache = new MaryCache(targetFile, MaryProperties.getBoolean("cache.clearOnStart", false));
            } catch (Exception e) {
                MaryUtils.getLogger(MaryCache.class).warn("Cannot set up cache", e);
            }
        }
        return maryCache;
    }
    
    /**
     * Indicate whether there is a MaryCache currently available.
     * @return true if there is a MaryCache, false otherwise.
     */
    public static boolean haveCache() {
        return maryCache != null;
    }
    
    private Connection connection;
    
    public MaryCache(File cacheFile, boolean clearCache) throws ClassNotFoundException, SQLException, MalformedURLException
    {
        // Load the HSQL Database Engine JDBC driver
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection("jdbc:hsqldb:"+cacheFile.toURL().toString(), "sa", "");
        boolean mustCreateTable = false;
        if (clearCache) {
            Statement st = connection.createStatement();
            st.executeUpdate("DROP TABLE MARYCACHE IF EXISTS");
            st.close();
            mustCreateTable = true;
        } else { // don't clear -- check if table exists
            DatabaseMetaData dbInfo = connection.getMetaData();
            ResultSet rs = dbInfo.getTables(null, null, "MARYCACHE", new String[]{"TABLE"});
            if (rs.next()) {
                // table exists
            } else {
                mustCreateTable = true;
            }
        }
        if (mustCreateTable) {
            String query = "CREATE CACHED TABLE MARYCACHE (id INTEGER IDENTITY, "
                +"inputtype VARCHAR(50), "
                +"outputtype VARCHAR(50), "
                +"locale VARCHAR, "
                +"voice VARCHAR, "
                +"outputparams VARCHAR, "
                +"style VARCHAR, "
                +"effects VARCHAR, "
                +"inputtext LONGVARCHAR, "
                +"outputtext LONGVARCHAR, "
                +"outputaudio LONGVARBINARY, "
                +"UNIQUE(inputtype, outputtype, locale, voice, outputparams, style, effects, inputtext)"
                +")";
            update(query);
        }
    }
    
    private synchronized void update(String query) throws SQLException
    {
        Statement st = connection.createStatement();
        int ok = st.executeUpdate(query);
        if (ok == -1) {
            throw new RuntimeException("DB problem with query: "+query);
        }
        st.close();
    }
    
    public void insertText(String inputtype, String outputtype, String locale, String voice, String inputtext, String outputtext)
    throws SQLException
    {
        insertText(inputtype, outputtype, locale, voice, null, null, null, inputtext, outputtext);
    }
    
    public synchronized void insertText(String inputtype, String outputtype, String locale, String voice, String outputparams, String style, String effects, String inputtext, String outputtext)
    throws SQLException
    {
        // Need to verify, here in the synchronized code, once again that really we don't have this entry already.
        // If we do, we ignore this call.
        if (lookupText(inputtype, outputtype, locale, voice, outputparams, style, effects, inputtext) != null) {
            return;
        }

        String query = "INSERT INTO MARYCACHE (inputtype, outputtype, locale, voice, outputparams, style, effects, inputtext, outputtext) VALUES ('"
            +inputtype+"','"+outputtype+"','"+locale+"','"+voice+"','"+outputparams+"','"+style+"','"+effects+"',?,?)";

        PreparedStatement st = connection.prepareStatement(query);
        // We set the input and output text separately because they could contain single quote characters
        st.setString(1, inputtext);
        st.setString(2, outputtext);
        st.executeUpdate();
        st.close();
    }
    
    public void insertAudio(String inputtype, String locale, String voice, String inputtext, byte[] audio)
    throws SQLException
    {
        insertAudio(inputtype, locale, voice, null, null, null, inputtext, audio);
    }
    
    public synchronized void insertAudio(String inputtype, String locale, String voice, String outputparams, String style, String effects, String inputtext, byte[] audio)
    throws SQLException
    {
        // Need to verify, here in the synchronized code, once again that really we don't have this entry already.
        // If we do, we ignore this call.
        if (lookupAudio(inputtype, locale, voice, outputparams, style, effects, inputtext) != null) {
            return;
        }
        
        String query = "INSERT INTO MARYCACHE (inputtype, outputtype, locale, voice, outputparams, style, effects, inputtext, outputaudio) VALUES('"
            +inputtype+"','AUDIO','"+locale+"','"+voice+"','"+outputparams+"','"+style+"','"+effects+"',?,?)";

        PreparedStatement st = connection.prepareStatement(query);
        st.setString(1, inputtext);
        st.setBytes(2, audio);
        st.executeUpdate();
        st.close();
    }

    public String lookupText(String inputtype, String outputtype, String locale, String voice, String inputtext)
    throws SQLException
    {
        return lookupText(inputtype, outputtype, locale, voice, null, null, null, inputtext);
    }
    
    public synchronized String lookupText(String inputtype, String outputtype, String locale, String voice, String outputparams, String style, String effects, String inputtext)
    throws SQLException
    {
        String outputtext = null;
        String query = "SELECT outputtext FROM marycache WHERE inputtype = '"+inputtype
            + "' AND outputtype = '"+outputtype
            + "' AND locale = '"+locale
            + "' AND voice = '"+voice
            + "' AND outputparams = '"+outputparams
            + "' AND style = '"+style
            + "' AND effects = '"+effects
            + "' AND inputtext = ?";

        PreparedStatement st = connection.prepareStatement(query);
        st.setString(1, inputtext);
        ResultSet results = st.executeQuery();
        if (results.next()) { // we expect only a single result, if any, so no while loop
            outputtext = results.getString(1);
        }
        st.close();
        return outputtext;
    }
    
    public byte[] lookupAudio(String inputtype, String locale, String voice, String inputtext)
    throws SQLException
    {
        return lookupAudio(inputtype, locale, voice, null, null, null, inputtext);
    }
    
    public synchronized byte[] lookupAudio(String inputtype, String locale, String voice, String outputparams, String style, String effects, String inputtext)
    throws SQLException
    {
        byte[] audio = null;
        String query = "Select outputaudio FROM marycache WHERE inputtype = '"+inputtype
            + "' AND outputtype = 'AUDIO' AND locale = '"+locale
            + "' AND voice = '"+voice
            + "' AND outputparams = '"+outputparams
            + "' AND style = '"+style
            + "' AND effects = '"+effects
            + "' AND inputtext = ?";
        PreparedStatement st = connection.prepareStatement(query);
        st.setString(1, inputtext);
        ResultSet results = st.executeQuery();
        if (results.next()) {
            audio = results.getBytes(1);
        }
        return audio;
    }
    
    public void shutdown() throws SQLException
    {

        Statement st = connection.createStatement();
        st.execute("SHUTDOWN");
        connection.close();    // if there are no other open connection
    }


    /**
     * @param args
     */
    public static void main(String[] args) throws SQLException, MalformedURLException, ClassNotFoundException
    {
        MaryCache c = new MaryCache(new File("/Users/marc/Desktop/testdb/testDB"), false);
//        c.insertText("TEXT", "RAWMARYXML", "de", "de1", "Welcome to the world of speech synthesis", "<rawmaryxml/>");
        
        String text = c.lookupText("TEXT", "RAWMARYXML", "de", "de1", "Welcome to the world of speech synthesis");
        System.out.println("looked up text: "+text);
        
        byte[] zeros = new byte[200000];
        //c.insertAudio("TEXT", "de", "de1", "some dummy text", zeros);
        byte[] newones = c.lookupAudio("TEXT", "de", "de1", "some dummy text");
        System.out.println("Retrieved binary data of length "+newones.length);
        
        c.shutdown();
    }

}
