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


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.Properties;
import marytts.tools.dbselection.WikipediaMarkupCleaner;

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
  
  public void createDataBaseSelectionTable() {
      String dbselection = "CREATE TABLE dbselection ( id INT NOT NULL AUTO_INCREMENT, " +
                                                       "fromFile TEXT, " +
                                                       "sentence TEXT, " +
                                                       "features BLOB, " +
                                                       "reliable BOOLEAN, " +
                                                       "primary key(id));";
   
      boolean dbExist = false;
      boolean unExist = false;
      // if database does not exist create them      
      System.out.println("Checking if the database already exist.");
      try {
          rs = st.executeQuery("SHOW TABLES;");
      } catch (Exception e) {
          e.printStackTrace();
      }
      
      try { 
          if( rs.next() ) {
            String str = rs.getString(1);
            if( str.contentEquals("dbselection") ){
               System.out.println("TABLE = " + str + " already exist.");
               dbExist = true;
            }
          }
          if( !dbExist ) {
              boolean res = st.execute( dbselection );
              System.out.println("TABLE = dbselection succesfully created.");   
          }
      } catch (SQLException e) {
          e.printStackTrace();
      } 
  }
  
  /***
   * 
   * @param sourceFile is a text file.
   */
  public void createTextTable(String sourceFile) {
      // wiki must be already created
      // String creteWiki = "CREATE DATABASE wiki;";
      String createTextTable = "CREATE TABLE `text` (" +
              " old_id int UNSIGNED NOT NULL AUTO_INCREMENT," +
              " old_text mediumblob NOT NULL," +
              " old_flags tinyblob NOT NULL," +
              " PRIMARY KEY old_id (old_id)" +
              " ) MAX_ROWS=10000000 AVG_ROW_LENGTH=10240;";
      
      // If database does not exist create it, if it exists delete it and create an empty one.      
      System.out.println("Checking if the TABLE=text already exist.");
      try {
          rs = st.executeQuery("SHOW TABLES;");
      } catch (Exception e) {
          e.printStackTrace();
      }
      boolean res=false;
      try { 
         
          while( rs.next() ) {
            String str = rs.getString(1);
            if( str.contentEquals("text") ){
               
               res=true;          
            }
          } 
          if(res==true){
            System.out.println("TABLE = text already exist deleting.");  
            boolean res0 = st.execute( "DROP TABLE text;" );  
          }
          
          System.out.println("Creating table:" + createTextTable);
          boolean res1 = st.execute( createTextTable );
          
          System.out.println("Loading sql file: " + sourceFile);
          System.out.println("SOURCE " + sourceFile + ";" );
          //int res2 = st.executeUpdate("SOURCE " + sourceSqlFile + ";");  // This does not work, i do not know??
          int res2 = st.executeUpdate("LOAD DATA LOCAL INFILE '" + sourceFile + "' into table text;");
          System.out.println("TABLE = text succesfully created and loaded with: " + sourceFile);   
          
      } catch (SQLException e) {
          e.printStackTrace();
      } 
  }

  public String[] getPageIdsOfTextTable() {
      int num, i, j;
      String idSet[]=null;
      
      String str = queryTable("SELECT count(old_id) FROM text;");  // normally this should be 25000
      num = Integer.parseInt(str);
      idSet = new String[num];
      
      try {
          rs = st.executeQuery("SELECT old_id FROM text;"); 
      } catch (Exception e) {
          e.printStackTrace();
      }
      try { 
          i=0;
          while( rs.next() ) {
            idSet[i] = rs.getString(1);
            i++;
          }
      } catch (SQLException e) {
          e.printStackTrace();
      } 
      
      return idSet;
  }
  
  public String[] getPageIds() {
      int num, i, j;
      String idSet[]=null;
      
      String str = queryTable("SELECT count(page_id) FROM page;");  // normally this should be 25000
      num = Integer.parseInt(str);
      idSet = new String[num];
      
      try {
          rs = st.executeQuery("SELECT page_id FROM page;"); 
      } catch (Exception e) {
          e.printStackTrace();
      }
      try { 
          i=0;
          while( rs.next() ) {
            idSet[i] = rs.getString(1);
            i++;
          }
      } catch (SQLException e) {
          e.printStackTrace();
      } 
      
      return idSet;
  }
  
  
  
  public void setDBTable(String table){
    currentTable = table;
  }

  
  public void insertSentenceAndFeatures(String file, String sentence, byte features[]){
    System.out.println("inserting in dbselection: file=" + file + " Num features=" + features.length + " sentence=" + sentence);
    try { 
      // INSERT INTO dbselection VALUES (id, fromFile, sentence, features, realiable)
      PreparedStatement ps = cn.prepareStatement("INSERT INTO dbselection VALUES (null, ?, ?, ?, ?)");
    
      ps.setString(1, file);
      ps.setString(2, sentence);
      ps.setBytes(3, features);
      ps.setBoolean(4, true);
      ps.execute();
      
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
  
  public void insertUnreliableSentence(String file, String sentence){
      System.out.println("inserting in dbselection: file=" + file + "reliable=false" + " sentence=" + sentence);
      try { 
        // INSERT INTO dbselection VALUES (id, fromFile, sentence, features, realiable)
        PreparedStatement ps = cn.prepareStatement("INSERT INTO dbselection VALUES (null, ?, ?, ?, ?)");
      
        ps.setString(1, file);
        ps.setString(2, sentence);
        ps.setBytes(3, null);
        ps.setBoolean(4, false);
        ps.execute();
        
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  
  public void closeDBConnection(){
    try {
        cn.close();    
    } catch (SQLException e) {
        e.printStackTrace();
    }  
  }
  
  
  public int getNumberOfReliableSentences() {
      String dbQuery = "SELECT count(sentence) FROM dbselection where reliable=true;";
      String str = queryTable(dbQuery);
      return Integer.parseInt(str);
  }
  
  public int[] getIdListOfType(String type) {
      int num, i, j;
      int idSet[]=null;
      
      String str = queryTable("SELECT count(id) FROM dbselection where " + type + "=true;");
      num = Integer.parseInt(str);
      idSet = new int[num];
      
      try {
          rs = st.executeQuery("SELECT id FROM dbselection where " + type + "=true;"); 
      } catch (Exception e) {
          e.printStackTrace();
      }
      try { 
          i=0;
          while( rs.next() ) {
            idSet[i] = rs.getInt(1);
            i++;
          }
      } catch (SQLException e) {
          e.printStackTrace();
      } 
      
      return idSet;
  }
  
  public String getFileNameFromTable(int id, String table) {
      String dbQuery = "Select fromFile FROM " + table + " WHERE id=" + id;
      return queryTable(dbQuery);      
  }
  
  public String getSentenceFromTable(int id, String table) {
      String dbQuery = "Select sentence FROM " + table + " WHERE id=" + id;
      return queryTable(dbQuery);      
  }
  
  // Firts filtering:
  // get first the page_title and check if it is not Image: or  Wikipedia:Votes_for_deletion/
  // maybe we can check also the length
  public String getTextFromPage(String id) {
      String pageTitle, pageLen, dbQuery, textId, text=null;
      byte[] textBytes=null;
      int len;
      
      dbQuery = "Select page_title FROM page WHERE page_id=" + id;
      pageTitle = queryTable(dbQuery);
      
      dbQuery = "Select page_len FROM page WHERE page_id=" + id;
      pageLen = queryTable(dbQuery);
      len = Integer.parseInt(pageLen);
      
      if(len < 20000 || pageTitle.contains("Image:") 
                     || pageTitle.contains("Wikipedia:")
                     || pageTitle.contains("List_of_")){
        //System.out.println("PAGE NOT USED page title=" + pageTitle + " Len=" + len);       
        /*
        dbQuery = "select rev_text_id from revision where rev_page=" + id;
        textId = queryTable(dbQuery);
        dbQuery = "select old_text from text where old_id=" + textId;
        text = queryTable(dbQuery);
        System.out.println("TEXT: " + text);
        */
      }
      else {
        System.out.print("PAGE page_id=" + id + "  ");  
        System.out.println("PAGE USED page title=" + pageTitle + " Len=" + len);
        //text="";
        
        dbQuery = "select rev_text_id from revision where rev_page=" + id;
        textId = queryTable(dbQuery);
        dbQuery = "select old_text from text where old_id=" + textId;        
        textBytes = queryTableByte(dbQuery); 
        try {
          text = new String(textBytes, "UTF8");
          System.out.println("  TEXT: " + text);
        } catch (Exception e) {  // UnsupportedEncodedException
             e.printStackTrace();
        } 
        
      }
      return text;   
  }
  
  public String getFeaturesFromTable(int id, String table) {
      String dbQuery = "Select features FROM " + table + " WHERE id=" + id;
      return queryTable(dbQuery);
      
  }

  private String queryTable(String dbQuery)
  {
      String str = "";
      //String dbQuery = "Select * FROM " + currentTable;
      //System.out.println("querying: " + dbQuery);
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
      return str;
      
  }
  
  private byte[] queryTableByte(String dbQuery)
  {
      byte strBytes[]=null;
      //String dbQuery = "Select * FROM " + currentTable;
      //System.out.println("querying: " + dbQuery);
      try {
          rs = st.executeQuery( dbQuery );
      } catch (Exception e) {
          e.printStackTrace();
      } 

      try {   
          while( rs.next() ) {
              //String url = rs.getString(2);
              //str = rs.getString(field);
              //str = rs.getString(1);
              strBytes = rs.getBytes(1);
          }
      } catch (Exception e) {
          e.printStackTrace();
      } 
      return strBytes;
      
  }

  public byte[] getFeatures(int id)
  {
      byte[] fea = null;
      String dbQuery = "Select features FROM dbselection WHERE id=" + id;
      //System.out.println("querying: " + dbQuery);
      try {
          rs = st.executeQuery( dbQuery );
      } catch (Exception e) {
          e.printStackTrace();
      } 

      try {   
          while( rs.next() ) {
              fea = rs.getBytes(1);
          }
      } catch (SQLException e) {
          e.printStackTrace();
      } 
      return fea;
      
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
      
      //String sql = "/project/mary/marcela/anna_wikipedia/pages_xml_splits/text.sql";
      //wikiToDB.createTextTable(sql);
      
      String pageId[];
      pageId = wikiToDB.getPageIds();
     
      WikipediaMarkupCleaner wikiCleaner = new WikipediaMarkupCleaner(); 
      String text;
      int numPagesUsed=0;
      for(int i=0; i<pageId.length; i++){
       //System.out.print("PAGE page_id[" + i + "]=" + pageId[i] + "  ");  
        text = wikiToDB.getTextFromPage(pageId[i]);
             
        if(text!=null){
          text = wikiCleaner.removeMarKup(text);  
          System.out.println("\n\n***CLEANED PAGE page_id[" + i + "]=" + pageId[i] + " : " + text);  
          numPagesUsed++;
        }
     
        //System.out.println("PAGE page_id[" + i + "]=" + pageId[i] + " : " + text);
          
      }
      System.out.println("Number of PAGES USED=" + numPagesUsed);
      
      
      wikiToDB.createDataBaseSelectionTable();
      
      wikiToDB.getIdListOfType("reliable");
      
      int num = wikiToDB.getNumberOfReliableSentences();
      
      String feas = wikiToDB.getFeaturesFromTable(1, "dbselection");
      
      
      byte [] tmp = new byte[4];
      tmp[0] = 10;
      tmp[1] = 20;
      tmp[2] = 30;
      tmp[3] = 40;
      
      wikiToDB.insertSentenceAndFeatures("file1", "sentence1", tmp);
      byte res[];
      
      res = wikiToDB.getFeatures(1);
     /*
      for(int i= 1; i<5; i++) {
        wikiToDB.insertSentenceAndFeatures("file1", "this is a ( )"+ i + " test", "long feature string "+i);
      }
      */
      wikiToDB.closeDBConnection();

      //Get the URLs and send them to wget.
      wikiToDB.createDBConnection("localhost","wiki","marcela","wiki123");
      wikiToDB.setDBTable("dbselection");
      //String sentence = wikiToDB.queryTable(3, "sentence");
      String sentence = wikiToDB.getSentenceFromTable(3, "dbselection");
      System.out.println("sentence 3 = " + sentence );
      
      String fea = wikiToDB.getFeaturesFromTable(3, "dbselection");
      System.out.println("feature 3 = " + fea );
      
      wikiToDB.closeDBConnection();

  } // end of main()



}
