/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package marytts.tools.dbselection;


import java.io.*;
import java.sql.*;
import java.util.Properties;

/**
 * Various functions for handling connection, inserting and querying a mysql database.
 * 
 * @author Holmer Hemsen, Marcela Charfuelan.
 */
public class DBHandler {
    
  private Connection cn = null;
  private Statement  st = null;
  private ResultSet  rs = null;
  private String currentTable = null;
  
  /**
   * The constructor loads the database driver.
   *
   */
  public DBHandler() {
    initDB_Driver();
    System.out.println("Mysql driver loaded.");  
  } 
  
  /** Loading DB Driver */
  private void initDB_Driver()
  {  
    try {
        String driver = "com.mysql.jdbc.Driver";
        Class.forName( driver );
    }
    catch( Exception e ) {
        e.printStackTrace();
    }
  }

  
  /**
   * The <code>createDBConnection</code> method creates the database connection.
   *
   * @param host a <code>String</code> value. The database host e.g. 'localhost'.
   * @param db a <code>String</code> value. The database to connect to. 
   * @param user a <code>String</code> value. Database user that has excess to the database.
   * @param passwd a <code>String</code> value. The 'secret' password. 
   */
  public void createDBConnection(String host, String db, String user, String passwd){
    String url = "jdbc:mysql://" + host + "/" + db;
    try {
      Properties p = new Properties();
      p.put("user",user);
      p.put("password",passwd);
      p.put("database", db);
      cn = DriverManager.getConnection( url, p );
      
     
      st = cn.createStatement();
    
    } catch (Exception e) {
        e.printStackTrace();
    } 
  }

  public void setDBTable(String table){
    currentTable = table;
  }

  public void insertSentenceAndFeatures(int id, String file, String sentence, String features){
    
    String dbInsert = "INSERT INTO " + "dbselection" 
                    + " VALUES (\"" + id 
                    + "\", \"" + file
                    + "\", \"" + sentence 
                    + "\", \"" + features 
                    + "\")";
    insertIntoTable(dbInsert);
    
  }
  
  public void insertUnreliableSentence(int id, String file, String sentence){     
      sentence = mysqlEscapeCharacters(sentence);
      String dbInsert = "INSERT INTO " + "unreliable" 
                      + " VALUES (\"" + id
                      + "\", \"" + file 
                      + "\", \"" + sentence 
                      + "\")";
      insertIntoTable(dbInsert);
    }
  
  private void insertIntoTable(String dbInsert){
     System.out.println("inserting: " + dbInsert);
      try {
          int res = st.executeUpdate( dbInsert );
      } catch (Exception e) {
          e.printStackTrace();
      }
      /*
      try {
          st.close();   // do we need this all the time???
      } catch (SQLException e) {
          e.printStackTrace();
      }
      */ 
    }

  public void closeDBConnection(){
    try {
        cn.close();    
    } catch (SQLException e) {
        e.printStackTrace();
    }
     
  }
  
  public String getSentenceFromTable(int id)
  {
      String str = "";
      String dbQuery = "Select sentence FROM " + currentTable + " WHERE id=" + id;
      return queryTable(dbQuery);      
  }
  
  public String getFeaturesFromTable(int id)
  {
      String str = "";
      String dbQuery = "Select features FROM " + currentTable + " WHERE id=" + id;
      return queryTable(dbQuery);
      
  }

  private String queryTable(String dbQuery)
  {
      String str = "";
      //String dbQuery = "Select * FROM " + currentTable;
      System.out.println("querying: " + dbQuery);
      try {
          rs = st.executeQuery( dbQuery );
      } catch (Exception e) {
          e.printStackTrace();
      } 

      try {   
          while( rs.next() ) {
              //String url = rs.getString(2);
              //str = rs.getString(field);
              str = rs.getString(1);
          }
      } catch (SQLException e) {
          e.printStackTrace();
      } 

      //Close database statement  // do i need to do this all the time???
      /*
      try {
          st.close();
      } catch (SQLException e) {
          e.printStackTrace();
      }    
      */
      return str;
      
  }

  /** The following characteres should be escaped:
   * \0  An ASCII 0 (NUL) character.
   * \'  A single quote (“'”) character.
   * \"  A double quote (“"”) character.
   */
  public String mysqlEscapeCharacters(String str){
    
    str = str.replace("\0", "");
    str = str.replace("'", "\'");
    str = str.replace("\"", "\\\"");  
    
    return str;  
  }

  public static void main(String[] args) {
      
      //Put sentences and features in the database.
      DBHandler wikiToDB = new DBHandler();

      wikiToDB.createDBConnection("localhost","wiki","marcela","wiki123");
      wikiToDB.setDBTable("dbselection");
      for(int i= 1; i<5; i++) {
        wikiToDB.insertSentenceAndFeatures(i,"file1", "this is a ( )"+ i + " test", "long feature string "+i);
      }
      wikiToDB.closeDBConnection();

      //Get the URLs and send them to wget.
      wikiToDB.createDBConnection("localhost","wiki","marcela","wiki123");
      wikiToDB.setDBTable("dbselection");
      //String sentence = wikiToDB.queryTable(3, "sentence");
      String sentence = wikiToDB.getSentenceFromTable(3);
      System.out.println("sentence 3 = " + sentence );
      
      String fea = wikiToDB.getFeaturesFromTable(3);
      System.out.println("feature 3 = " + fea );
      
      wikiToDB.closeDBConnection();

  } // end of main()



}
