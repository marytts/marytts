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

import marytts.server.MaryProperties;

/**
 * @author marc
 * 
 */
public class MaryCache {
	private static MaryCache maryCache;

	/**
	 * Try to get the MaryCache object. This will either return the previously created MaryCache, or if none exists, it will try
	 * to create one.
	 * 
	 * @see #haveCache if you just want to check if the cache exists.
	 * 
	 *      To the extent possible this method gives the no-throw guarantee: if the cache cannot be created, null will be returned
	 *      and any exception will be logged.
	 * @return the MaryCache singleton object, or null if none could be created.
	 */
	public static MaryCache getCache() {
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
	 * 
	 * @return true if there is a MaryCache, false otherwise.
	 */
	public static boolean haveCache() {
		return maryCache != null;
	}

	// //////////////////////////// non-static code /////////////////////////////

	private Connection connection;

	/**
	 * Create a MaryCache with the given file prefix. This constructor is public only for tests; it should not normally be called.
	 * User code should call {@link #getCache()} instead. TODO: Find a more elegant way to create a custom MaryCache from test
	 * code.
	 * 
	 * @param cacheFile
	 *            the file name prefix with which to create the cache database.
	 * @param clearCache
	 *            if true, clear the cache; if false, keep it.
	 * @throws ClassNotFoundException
	 *             if the HSQL JDBC driver is not in the classpath.
	 * @throws SQLException
	 *             if the database connection cannot be set up
	 */
	public MaryCache(File cacheFile, boolean clearCache) throws ClassNotFoundException, SQLException {
		// Load the HSQL Database Engine JDBC driver
		Class.forName("org.hsqldb.jdbcDriver");
		connection = DriverManager.getConnection("jdbc:hsqldb:" + cacheFile.toURI().toString(), "sa", "");
		boolean mustCreateTable = false;
		if (clearCache) {
			Statement st = connection.createStatement();
			st.executeUpdate("DROP TABLE MARYCACHE IF EXISTS");
			st.close();
			mustCreateTable = true;
		} else { // don't clear -- check if table exists
			DatabaseMetaData dbInfo = connection.getMetaData();
			ResultSet rs = dbInfo.getTables(null, null, "MARYCACHE", new String[] { "TABLE" });
			if (rs.next()) {
				// table exists
			} else {
				mustCreateTable = true;
			}
		}
		if (mustCreateTable) {
			String query = "CREATE CACHED TABLE MARYCACHE (id INTEGER IDENTITY, " + "inputtype VARCHAR(50), "
					+ "outputtype VARCHAR(50), " + "locale VARCHAR(10), " + "voice VARCHAR(100), "
					+ "outputparams VARCHAR(1000), " + "style VARCHAR(50), " + "effects VARCHAR(1000), "
					+ "inputtext LONGVARCHAR, " + "outputtext LONGVARCHAR, " + "outputaudio LONGVARBINARY, "
					+ "UNIQUE(inputtype, outputtype, locale, voice, outputparams, style, effects, inputtext)" + ")";
			update(query);
		}
	}

	/**
	 * Carry out an UPDATE SQL command on the database.
	 * 
	 * @param query
	 *            the UPDATE SQL command to carry out
	 * @throws SQLException
	 *             if there is a problem executing the update.
	 */
	private synchronized void update(String query) throws SQLException {
		Statement st = connection.createStatement();
		int ok = st.executeUpdate(query);
		if (ok == -1) {
			throw new SQLException("DB problem with query: " + query);
		}
		st.close();
	}

	/**
	 * Insert a record of a MARY request producing data of type text into the cache. If a record with the same lookup keys (i.e.,
	 * all parameters everything except outputtext) exists already, this call does nothing.
	 * 
	 * @param inputtype
	 *            the request's input type. Must not be null.
	 * @param outputtype
	 *            the request's output type, which must be a text type. Must not be null.
	 * @param locale
	 *            the locale of the request. Must not be null.
	 * @param voice
	 *            the voice of the request. Can be null.
	 * @param inputtext
	 *            the request's input text. Must not be null.
	 * @param outputtext
	 *            the request's output text. Must not be null.
	 * @throws NullPointerException
	 *             if one of the fields is null which must be non-null.
	 * @throws SQLException
	 *             if the record could not be entered into the cache.
	 */
	public void insertText(String inputtype, String outputtype, String locale, String voice, String inputtext, String outputtext)
			throws SQLException {
		insertText(inputtype, outputtype, locale, voice, null, null, null, inputtext, outputtext);
	}

	/**
	 * Insert a record of a MARY request producing data of type text into the cache. If a record with the same lookup keys (i.e.,
	 * all parameters everything except outputtext) exists already, this call does nothing.
	 * 
	 * @param inputtype
	 *            the request's input type. Must not be null.
	 * @param outputtype
	 *            the request's output type, which must be a text type. Must not be null.
	 * @param locale
	 *            the locale of the request. Must not be null.
	 * @param voice
	 *            the voice of the request. Can be null.
	 * @param outputparams
	 *            optionally, any output parameters. Can be null.
	 * @param style
	 *            optionally, any style. Can be null.
	 * @param effects
	 *            optionally, any effects. Can be null.
	 * @param inputtext
	 *            the request's input text. Must not be null.
	 * @param outputtext
	 *            the request's output text. Must not be null.
	 * @throws NullPointerException
	 *             if one of the fields is null which must be non-null.
	 * @throws SQLException
	 *             if the record could not be entered into the cache.
	 */
	public synchronized void insertText(String inputtype, String outputtype, String locale, String voice, String outputparams,
			String style, String effects, String inputtext, String outputtext) throws SQLException {
		if (inputtype == null || outputtype == null || locale == null || voice == null || inputtext == null || outputtext == null) {
			throw new NullPointerException("Null argument");
		}
		// Need to verify, here in the synchronized code, once again that really we don't have this entry already.
		// If we do, we ignore this call.
		if (lookupText(inputtype, outputtype, locale, voice, outputparams, style, effects, inputtext) != null) {
			return;
		}

		String query = "INSERT INTO MARYCACHE (inputtype, outputtype, locale, voice, outputparams, style, effects, inputtext, outputtext) VALUES ('"
				+ inputtype
				+ "','"
				+ outputtype
				+ "','"
				+ locale
				+ "','"
				+ voice
				+ "','"
				+ outputparams
				+ "','"
				+ style
				+ "','"
				+ effects + "',?,?)";

		PreparedStatement st = connection.prepareStatement(query);
		// We set the input and output text separately because they could contain single quote characters
		st.setString(1, inputtext);
		st.setString(2, outputtext);
		st.executeUpdate();
		st.close();
	}

	/**
	 * Insert a record of a MARY request producing data of output type AUDIO into the cache. If a record with the same lookup keys
	 * (i.e., all parameters everything except audio) exists already, this call does nothing.
	 * 
	 * @param inputtype
	 *            the request's input type. Must not be null.
	 * @param locale
	 *            the locale of the request. Must not be null.
	 * @param voice
	 *            the voice of the request. Can be null.
	 * @param inputtext
	 *            the request's input text. Must not be null.
	 * @param audio
	 *            the request's output data. Must not be null.
	 * @throws NullPointerException
	 *             if one of the fields is null which must be non-null.
	 * @throws SQLException
	 *             if the record could not be entered into the cache.
	 */
	public void insertAudio(String inputtype, String locale, String voice, String inputtext, byte[] audio) throws SQLException {
		insertAudio(inputtype, locale, voice, null, null, null, inputtext, audio);
	}

	/**
	 * Insert a record of a MARY request producing data of output type AUDIO into the cache. If a record with the same lookup keys
	 * (i.e., all parameters everything except audio) exists already, this call does nothing.
	 * 
	 * @param inputtype
	 *            the request's input type. Must not be null.
	 * @param locale
	 *            the locale of the request. Must not be null.
	 * @param voice
	 *            the voice of the request. Can be null.
	 * @param outputparams
	 *            optionally, any output parameters. Can be null.
	 * @param style
	 *            optionally, any style. Can be null.
	 * @param effects
	 *            optionally, any effects. Can be null.
	 * @param inputtext
	 *            the request's input text. Must not be null.
	 * @param audio
	 *            the request's output data. Must not be null.
	 * @throws NullPointerException
	 *             if one of the fields is null which must be non-null.
	 * @throws SQLException
	 *             if the record could not be entered into the cache.
	 */
	public synchronized void insertAudio(String inputtype, String locale, String voice, String outputparams, String style,
			String effects, String inputtext, byte[] audio) throws SQLException {
		if (inputtype == null || locale == null || voice == null || inputtext == null) {
			throw new NullPointerException("Null argument");
		}
		// Need to verify, here in the synchronized code, once again that really we don't have this entry already.
		// If we do, we ignore this call.
		if (lookupAudio(inputtype, locale, voice, outputparams, style, effects, inputtext) != null) {
			return;
		}

		String query = "INSERT INTO MARYCACHE (inputtype, outputtype, locale, voice, outputparams, style, effects, inputtext, outputaudio) VALUES('"
				+ inputtype
				+ "','AUDIO','"
				+ locale
				+ "','"
				+ voice
				+ "','"
				+ outputparams
				+ "','"
				+ style
				+ "','"
				+ effects
				+ "',?,?)";

		PreparedStatement st = connection.prepareStatement(query);
		st.setString(1, inputtext);
		st.setBytes(2, audio);
		st.executeUpdate();
		st.close();
	}

	/**
	 * Carry out a lookup in the cache with the given parameters.
	 * 
	 * @param inputtype
	 *            the request's input type. Must not be null.
	 * @param outputtype
	 *            the request's output type, which must be a text type. Must not be null.
	 * @param locale
	 *            the locale of the request. Must not be null.
	 * @param voice
	 *            the voice of the request. Can be null.
	 * @param inputtext
	 *            the request's input text. Must not be null.
	 * @return the output text associated with the with the given record, or null if the cache does not contain a record with
	 *         these keys.
	 * @throws NullPointerException
	 *             if one of the fields is null which must be non-null.
	 * @throws SQLException
	 *             if there is a problem querying the cache.
	 */
	public String lookupText(String inputtype, String outputtype, String locale, String voice, String inputtext)
			throws SQLException {
		return lookupText(inputtype, outputtype, locale, voice, null, null, null, inputtext);
	}

	/**
	 * Carry out a lookup in the cache with the given parameters.
	 * 
	 * @param inputtype
	 *            the request's input type. Must not be null.
	 * @param outputtype
	 *            the request's output type, which must be a text type. Must not be null.
	 * @param locale
	 *            the locale of the request. Must not be null.
	 * @param voice
	 *            the voice of the request. Can be null.
	 * @param outputparams
	 *            optionally, any output parameters. Can be null.
	 * @param style
	 *            optionally, any style. Can be null.
	 * @param effects
	 *            optionally, any effects. Can be null.
	 * @param inputtext
	 *            the request's input text. Must not be null.
	 * @return the output text associated with the with the given record, or null if the cache does not contain a record with
	 *         these keys.
	 * @throws NullPointerException
	 *             if one of the fields is null which must be non-null.
	 * @throws SQLException
	 *             if there is a problem querying the cache.
	 */
	public synchronized String lookupText(String inputtype, String outputtype, String locale, String voice, String outputparams,
			String style, String effects, String inputtext) throws SQLException {
		if (inputtype == null || outputtype == null || locale == null || voice == null || inputtext == null) {
			throw new NullPointerException("Null argument");
		}
		String outputtext = null;
		String query = "SELECT outputtext FROM marycache WHERE inputtype = '" + inputtype + "' AND outputtype = '" + outputtype
				+ "' AND locale = '" + locale + "' AND voice = '" + voice + "' AND outputparams = '" + outputparams
				+ "' AND style = '" + style + "' AND effects = '" + effects + "' AND inputtext = ?";

		PreparedStatement st = connection.prepareStatement(query);
		st.setString(1, inputtext);
		ResultSet results = st.executeQuery();
		if (results.next()) { // we expect only a single result, if any, so no while loop
			outputtext = results.getString(1);
		}
		st.close();
		return outputtext;
	}

	/**
	 * Carry out a lookup in the cache with the given parameters, for a request with output type AUDIO.
	 * 
	 * @param inputtype
	 *            the request's input type. Must not be null.
	 * @param locale
	 *            the locale of the request. Must not be null.
	 * @param voice
	 *            the voice of the request. Can be null.
	 * @param inputtext
	 *            the request's input text. Must not be null.
	 * @return the output text associated with the with the given record, or null if the cache does not contain a record with
	 *         these keys.
	 * @throws NullPointerException
	 *             if one of the fields is null which must be non-null.
	 * @throws SQLException
	 *             if there is a problem querying the cache.
	 */
	public byte[] lookupAudio(String inputtype, String locale, String voice, String inputtext) throws SQLException {
		return lookupAudio(inputtype, locale, voice, null, null, null, inputtext);
	}

	/**
	 * Carry out a lookup in the cache with the given parameters, for a request with output type AUDIO.
	 * 
	 * @param inputtype
	 *            the request's input type. Must not be null.
	 * @param locale
	 *            the locale of the request. Must not be null.
	 * @param voice
	 *            the voice of the request. Can be null.
	 * @param outputparams
	 *            optionally, any output parameters. Can be null.
	 * @param style
	 *            optionally, any style. Can be null.
	 * @param effects
	 *            optionally, any effects. Can be null.
	 * @param inputtext
	 *            the request's input text. Must not be null.
	 * @return the output text associated with the with the given record, or null if the cache does not contain a record with
	 *         these keys.
	 * @throws NullPointerException
	 *             if one of the fields is null which must be non-null.
	 * @throws SQLException
	 *             if there is a problem querying the cache.
	 */
	public synchronized byte[] lookupAudio(String inputtype, String locale, String voice, String outputparams, String style,
			String effects, String inputtext) throws SQLException {
		if (inputtype == null || locale == null || voice == null || inputtext == null) {
			throw new NullPointerException("Null argument");
		}
		byte[] audio = null;
		String query = "Select outputaudio FROM marycache WHERE inputtype = '" + inputtype
				+ "' AND outputtype = 'AUDIO' AND locale = '" + locale + "' AND voice = '" + voice + "' AND outputparams = '"
				+ outputparams + "' AND style = '" + style + "' AND effects = '" + effects + "' AND inputtext = ?";
		PreparedStatement st = connection.prepareStatement(query);
		st.setString(1, inputtext);
		ResultSet results = st.executeQuery();
		if (results.next()) {
			audio = results.getBytes(1);
		}
		return audio;
	}

	/**
	 * Shut down the cache. After this has been called, any further calls to the object will throw exceptions.
	 * 
	 * @throws SQLException
	 *             if there is a problem executing the database SHUTDOWN command.
	 */
	public void shutdown() throws SQLException {

		Statement st = connection.createStatement();
		st.execute("SHUTDOWN");
		connection.close(); // if there are no other open connection
	}

	/**
	 * @param args
	 *            args
	 * @throws SQLException
	 *             SQLException
	 * @throws MalformedURLException
	 *             MalformedURLException
	 * @throws ClassNotFoundException
	 *             ClassNotFoundException
	 */
	public static void main(String[] args) throws SQLException, MalformedURLException, ClassNotFoundException {
		MaryCache c = new MaryCache(new File("/Users/marc/Desktop/testdb/testDB"), false);
		// c.insertText("TEXT", "RAWMARYXML", "de", "de1", "Welcome to the world of speech synthesis", "<rawmaryxml/>");

		String text = c.lookupText("TEXT", "RAWMARYXML", "de", "de1", "Welcome to the world of speech synthesis");
		System.out.println("looked up text: " + text);

		byte[] zeros = new byte[200000];
		// c.insertAudio("TEXT", "de", "de1", "some dummy text", zeros);
		byte[] newones = c.lookupAudio("TEXT", "de", "de1", "some dummy text");
		System.out.println("Retrieved binary data of length " + newones.length);

		c.shutdown();
	}

}
