/**
 * Copyright 2000-2008 DFKI GmbH.
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
package marytts.language.de.infostruct;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Vector;

import java.util.logging.Level;

import marytts.util.MaryUtils;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;


/************************************************
 * Execute a query in the mySql germanet database.
 * To run it /opt/jdbc/src/ or similar schould be 
 * in your classpath.  
 * 
 * How to run:   /opt/jdk-1.4.1/bin/java -cp ./:/opt/jdbc/lib/mysql-connector-java-3.0.9-stable-bin.jar de.dfki.lt.mary.util.GerNetQuery [Word] [pos] [type]
 * Run example:  /opt/jdk-1.4.1/bin/java -cp ./:/opt/jdbc/lib/mysql-connector-java-3.0.9-stable-bin.jar de.dfki.lt.mary.util.GerNetQuery Welt n hype
 * @author Marilisa Amoia & Massimo Romanelli
 ***********************************************/
public class GerNetQuery {
    private Connection con;
    private Statement stmt;
    private static Logger logger = MaryUtils.getLogger("GerNetQuery");

    public GerNetQuery(String database, String user, String password) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException {
        connect(database, user, password);
    } //constructor

    //Connecting
    private void connect(String database, String user, String password)
        throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException {
        //Load Driver:
        Class.forName("org.gjt.mm.mysql.Driver").newInstance();
        //Connection to Database
        con =
            DriverManager.getConnection(
                "jdbc:mysql://" + database + "?user=" + user + "&password=" + password);
        stmt = con.createStatement();
    } //connect

    private void closeResultSet(ResultSet rs){
        if(rs != null)
            try {
            rs.close();
        } catch (SQLException e) {
            logger.warn("Cannot access Germanet:", e);
        }
    }

    public String getSynString(String lex, String pos) {
        StringBuilder result = new StringBuilder();
        ResultSet rs = null;
        try {
            long startTime = System.currentTimeMillis();
            rs =
                stmt.executeQuery(
                    "SELECT DISTINCT(sw2.Word) FROM SynsetWord sw1, SynsetWord sw2 WHERE sw1.Word='"
                        + lex
                        + "' AND sw1.Pos='"
                        + pos
                        + "' AND sw1.Offset=sw2.Offset and sw1.Pos=sw2.Pos and sw2.Word<>sw1.Word ");
            long endTime = System.currentTimeMillis();
            logger.debug("Germanet Query took " + (endTime - startTime) + " ms.");
            while (rs.next()) {
                result.append("#").append(rs.getString("Word"));
            } //while
        } catch (SQLException e) {
            logger.warn("Cannot access Germanet:", e);        
        } finally{
            closeResultSet(rs);
        }
        return result.toString();

    } //getSynString



    public Vector getSynVector(String lex, String pos) {
        Vector result = new Vector();
        ResultSet rs = null;
        try {
            long startTime = System.currentTimeMillis();
            rs =
                stmt.executeQuery(
                    "SELECT DISTINCT(sw2.Word) FROM SynsetWord sw1, SynsetWord sw2 WHERE sw1.Word='"
                        + lex
                        + "' AND sw1.Pos='"
                        + pos
                        + "' AND sw1.Offset=sw2.Offset and sw1.Pos=sw2.Pos and sw2.Word<>sw1.Word ");
            long endTime = System.currentTimeMillis();
            logger.debug("Germanet Query took " + (endTime - startTime) + " ms.");
            while (rs.next()) {
                result.add(rs.getString("Word"));
            } //while
        } catch (SQLException e) {
            logger.warn("Cannot access Germanet:", e);        
        } finally{
            closeResultSet(rs);
        }
        return result;
    } //getSynVector

    public String getHyperString(String lex, String pos) {
        StringBuilder result = new StringBuilder();
        ResultSet rs = null;
        try {
            long startTime = System.currentTimeMillis();
            rs =
                stmt.executeQuery(
                    "SELECT DISTINCT(sw2.Word) FROM SynsetPtr ptr1, SynsetWord sw1, SynsetWord sw2 WHERE ptr1.Ptr='@' AND ptr1.SourcePos=sw1.Pos AND ptr1.SourceOffset=sw1.Offset AND sw2.Offset=ptr1.TargetOffSet AND sw1.Word='"
                        + lex
                        + "' AND sw1.Pos='"
                        + pos
                        + "' AND sw1.Pos=sw2.Pos");
            long endTime = System.currentTimeMillis();
            logger.debug("Germanet Query took " + (endTime - startTime) + " ms.");
            while (rs.next()) {
                result.append("#").append(rs.getString("Word"));
            } //while
        } catch (SQLException e) {
            logger.warn("Cannot access Germanet:", e);        
        } finally{
            closeResultSet(rs);
        }
        return result.toString();

    } //getHyperString

    public Vector getHyperVector(String lex, String pos) {
        Vector result = new Vector();
        ResultSet rs = null;
        try {
            long startTime = System.currentTimeMillis();
            rs =
                stmt.executeQuery(
                    "SELECT DISTINCT(sw2.Word) FROM SynsetPtr ptr1, SynsetWord sw1, SynsetWord sw2 WHERE ptr1.Ptr='@' AND ptr1.SourcePos=sw1.Pos AND ptr1.SourceOffset=sw1.Offset AND sw2.Offset=ptr1.TargetOffSet AND sw1.Word='"
                        + lex
                        + "' AND sw1.Pos='"
                        + pos
                        + "' AND sw1.Pos=sw2.Pos");
            long endTime = System.currentTimeMillis();
            logger.debug("Germanet Query took " + (endTime - startTime) + " ms.");
            while (rs.next()) {
                result.add(rs.getString("Word"));
            } //while
        } catch (SQLException e) {
            logger.warn("Cannot access Germanet:", e);        
        } finally{
            closeResultSet(rs);
        }
        return result;
    } //getHyperVector

    public String getHypoString(String lex, String pos) {
        StringBuilder result = new StringBuilder();
        ResultSet rs = null;
        try {
            long startTime = System.currentTimeMillis();
            rs =
                stmt.executeQuery(
                    "SELECT DISTINCT(sw2.Word) FROM SynsetPtr ptr1, SynsetWord sw1, SynsetWord sw2 WHERE ptr1.Ptr='~' AND ptr1.SourcePos=sw1.Pos AND ptr1.SourceOffset=sw1.Offset AND sw2.Offset=ptr1.TargetOffSet AND sw1.Word='"
                        + lex
                        + "' AND sw1.Pos='"
                        + pos
                        + "' AND sw1.Pos=sw2.Pos");
            long endTime = System.currentTimeMillis();
            logger.debug("Germanet Query took " + (endTime - startTime) + " ms.");
            while (rs.next()) {
                result.append("#").append(rs.getString("Word"));
            } //while
        } catch (SQLException e) {
            logger.warn("Cannot access Germanet:", e);        
        } finally{
            closeResultSet(rs);
        }
        return result.toString();
    } //getHypoString

    public Vector getHypoVector(String lex, String pos) {
        Vector result = new Vector();
        ResultSet rs = null;
        try {
            long startTime = System.currentTimeMillis();
            rs =
                stmt.executeQuery(
                    "SELECT DISTINCT(sw2.Word) FROM SynsetPtr ptr1, SynsetWord sw1, SynsetWord sw2 WHERE ptr1.Ptr='~' AND ptr1.SourcePos=sw1.Pos AND ptr1.SourceOffset=sw1.Offset AND sw2.Offset=ptr1.TargetOffSet AND sw1.Word='"
                        + lex
                        + "' AND sw1.Pos='"
                        + pos
                        + "' AND sw1.Pos=sw2.Pos");
            long endTime = System.currentTimeMillis();
            logger.debug("Germanet Query took " + (endTime - startTime) + " ms.");
            while (rs.next()) {
                result.add(rs.getString("Word"));
            } //while
        } catch (SQLException e) {
            logger.warn("Cannot access Germanet:", e);        
        } finally{
            closeResultSet(rs);
        }
        return result;
    } //getHypoVector

    public String getAntoString(String lex, String pos) {
        StringBuilder result = new StringBuilder();
        ResultSet rs = null;
        try {
            long startTime = System.currentTimeMillis();
            rs =
                stmt.executeQuery(
                    "SELECT DISTINCT(sw2.Word) FROM SynsetPtr ptr1, SynsetWord sw1, SynsetWord sw2 WHERE ptr1.Ptr='!' AND ptr1.SourcePos=sw1.Pos AND ptr1.SourceOffset=sw1.Offset AND sw2.Offset=ptr1.TargetOffSet AND sw1.Word='"
                        + lex
                        + "' AND sw1.Pos='"
                        + pos
                        + "' AND sw1.Pos=sw2.Pos");
            long endTime = System.currentTimeMillis();
            logger.debug("Germanet Query took " + (endTime - startTime) + " ms.");
            while (rs.next()) {
                result.append("#").append(rs.getString("Word"));
            } //while
        } catch (SQLException e) {
            logger.warn("Cannot access Germanet:", e);        
        } finally{
            closeResultSet(rs);
        }
        return result.toString();

    } //getAntoString

    public Vector getAntoVector(String lex, String pos) {
        Vector result = new Vector();
        ResultSet rs = null;
        try {
            long startTime = System.currentTimeMillis();
            rs =
                stmt.executeQuery(
                    "SELECT DISTINCT(sw2.Word) FROM SynsetPtr ptr1, SynsetWord sw1, SynsetWord sw2 WHERE ptr1.Ptr='!' AND ptr1.SourcePos=sw1.Pos AND ptr1.SourceOffset=sw1.Offset AND sw2.Offset=ptr1.TargetOffSet AND sw1.Word='"
                        + lex
                        + "' AND sw1.Pos='"
                        + pos
                        + "' AND sw1.Pos=sw2.Pos");
            long endTime = System.currentTimeMillis();
            logger.debug("Germanet Query took " + (endTime - startTime) + " ms.");
            while (rs.next()) {
                result.add(rs.getString("Word"));
            } //while
        } catch (SQLException e) {
            logger.warn("Cannot access Germanet:", e);        
        } finally{
            closeResultSet(rs);
        }
        return result;
    } //getAntoVector

    public static void main(java.lang.String argv[])
        throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException {
        GerNetQuery query = new GerNetQuery(System.getProperty("germanet.database"), 
            System.getProperty("germanet.user"), System.getProperty("germanet.password"));
        BasicConfigurator.configure();
        Vector result = new Vector();
        if (argv[2].equals("hypo"))
            result = query.getHypoVector(argv[0], argv[1]);
        else if (argv[2].equals("hype"))
            result = query.getHyperVector(argv[0], argv[1]);
        else if (argv[2].equals("anto"))
            result = query.getAntoVector(argv[0], argv[1]);
        else if (argv[2].equals("syn"))
            result = query.getSynVector(argv[0], argv[1]);
        else if (argv[0].equals("intDef"))
            result = query.getHypoVector("gepäck", "n");

        System.out.println("result is: " + (Collection) result);
    } //main

} //GerNetQuery
