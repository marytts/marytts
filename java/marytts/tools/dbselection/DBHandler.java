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


import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.Iterator;
import java.util.Properties;
import java.util.HashMap;
import java.util.TreeMap;

import marytts.tools.dbselection.WikipediaMarkupCleaner;

/**
 * Various functions for handling connection, inserting and querying a mysql database.
 * 
 * @author Marcela Charfuelan, Holmer Hemsen.
 */
public class DBHandler {
    
  private Connection cn = null;
  private Statement  st = null;
  private ResultSet  rs = null;
  private String currentTable = null;
  private PreparedStatement psSentence = null;
  private PreparedStatement psWord = null;
  private PreparedStatement psCleanText = null;
  
  /**
   * The constructor loads the database driver.
   *
   */
  public DBHandler() {
    initDB_Driver();
    System.out.println("\nMysql driver loaded.");  
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
      
      psCleanText = cn.prepareStatement("INSERT INTO cleanText VALUES (null, ?, ?, ?, ?)");
      psWord      = cn.prepareStatement("INSERT INTO wordList VALUES (null, ?, ?)");
      psSentence  = cn.prepareStatement("INSERT INTO dbselection VALUES (null, ?, ?, ?, ?, ?, ?)");
      
    } catch (Exception e) {
        e.printStackTrace();
    } 
  }
  
  public void createDataBaseSelectionTable() {
      String dbselection = "CREATE TABLE dbselection ( id INT NOT NULL AUTO_INCREMENT, " +                                                       
                                                       "sentence TEXT, " +
                                                       "features BLOB, " +
                                                       "reliable BOOLEAN, " +
                                                       "unknownWords BOOLEAN, " +
                                                       "strangeSymbols BOOLEAN, " +
                                                       "cleanText_id INT UNSIGNED NOT NULL, " +   // the cleanText id where this sentence comes from
                                                       "primary key(id));";
      String str;
      boolean dbExist = false;
      // if database does not exist create it    
      System.out.println("Checking if dbselection already exist.");
      try {
          rs = st.executeQuery("SHOW TABLES;");
      } catch (Exception e) {
          e.printStackTrace();
      }
      
      try { 
          while( rs.next() ) {
            str = rs.getString(1);
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
  public void createAndLoadWikipediaTables(String textFile, String pageFile, String revisionFile) {
      // wiki must be already created
      // String creteWiki = "CREATE DATABASE wiki;";
      String createTextTable = "CREATE TABLE `text` (" +
              " old_id int UNSIGNED NOT NULL AUTO_INCREMENT," +
              " old_text mediumblob NOT NULL," +
              " old_flags tinyblob NOT NULL," +
              " PRIMARY KEY old_id (old_id)" +
              " ) MAX_ROWS=250000 AVG_ROW_LENGTH=10240;";
      
      String createPageTable = "CREATE TABLE `page` (" +
            "page_id int UNSIGNED NOT NULL AUTO_INCREMENT," +
            "page_namespace int(11) NOT NULL," +
            "page_title varchar(255) NOT NULL," +
            "page_restrictions tinyblob NOT NULL," +
            "page_counter bigint(20) unsigned NOT NULL," +
            "page_is_redirect tinyint(3) unsigned NOT NULL," +
            "page_is_new tinyint(3) unsigned NOT NULL," +
            "page_random double unsigned NOT NULL," +
            "page_touched binary(14) NOT NULL," +
            "page_latest int(10) unsigned NOT NULL," +
            "page_len int(10) unsigned NOT NULL," +
            "PRIMARY KEY page_id (page_id)," +
            "KEY page_namespace (page_namespace)," +
            "KEY page_random (page_random)," +
            "KEY page_len (page_len) ) MAX_ROWS=250000 AVG_ROW_LENGTH=10240; ";
      
      String createRevisionTable = "CREATE TABLE `revision` (" +
            "rev_id int UNSIGNED NOT NULL AUTO_INCREMENT," +
            "rev_page int(10) unsigned NOT NULL," +
            "rev_text_id int(10) unsigned NOT NULL," +
            "rev_comment tinyblob NOT NULL," +
            "rev_user int(10) unsigned NOT NULL," +
            "rev_user_text varchar(255) NOT NULL, " +
            "rev_timestamp binary(14) NOT NULL, " +
            "rev_minor_edit tinyint(3) unsigned NOT NULL," +
            " rev_deleted tinyint(3) unsigned NOT NULL," +
            "rev_len int(10) unsigned NULL," +
            "rev_parent_id int(10) unsigned NULL," +
            "KEY rev_user (rev_user),KEY rev_user_text (rev_user_text)," +
            "KEY rev_timestamp (rev_timestamp)," +
            "PRIMARY KEY rev_id (rev_id)) MAX_ROWS=250000 AVG_ROW_LENGTH=10240;";
      
      // If database does not exist create it, if it exists delete it and create an empty one.      
      System.out.println("Checking if the TABLE=text already exist.");
      try {
          rs = st.executeQuery("SHOW TABLES;");
      } catch (Exception e) {
          e.printStackTrace();
      }
      boolean resText=false, resPage=false, resRevision=false;
      try { 
         
          while( rs.next() ) {
            String str = rs.getString(1);
            if( str.contentEquals("text") )
               resText=true;
            else if( str.contentEquals("page") )
               resPage=true;
            else if( str.contentEquals("revision") )
               resRevision=true;
            
          } 
          if(resText==true){
            System.out.println("TABLE = text already exist deleting.");  
            boolean res0 = st.execute( "DROP TABLE text;" );  
          }
          if(resPage==true){
              System.out.println("TABLE = page already exist deleting.");  
              boolean res0 = st.execute( "DROP TABLE page;" );  
          }
          if(resRevision==true){
              System.out.println("TABLE = revision already exist deleting.");  
              boolean res0 = st.execute( "DROP TABLE revision;" );  
          }
          
          boolean res1;
          int res2;
          // creating TABLE=text
          System.out.println("\nCreating table:" + createTextTable);
          res1 = st.execute( createTextTable );         
          System.out.println("Loading sql file: " + textFile);
          //int res2 = st.executeUpdate("SOURCE " + sourceSqlFile + ";");  // This does not work, i do not know??
          res2 = st.executeUpdate("LOAD DATA LOCAL INFILE '" + textFile + "' into table text;");
          System.out.println("TABLE = text succesfully created.");   
          
          // creating TABLE=page
          System.out.println("\nCreating table:" + createPageTable);
          res1 = st.execute( createPageTable );         
          System.out.println("Loading sql file: " + pageFile);
          res2 = st.executeUpdate("LOAD DATA LOCAL INFILE '" + pageFile + "' into table page;");
          System.out.println("TABLE = page succesfully created.");  
          
          // creating TABLE=revision
          System.out.println("\n\nCreating table:" + createRevisionTable);
          res1 = st.execute( createRevisionTable );         
          System.out.println("Loading sql file: " + revisionFile);
          System.out.println("SOURCE " + revisionFile + ";" );
          res2 = st.executeUpdate("LOAD DATA LOCAL INFILE '" + revisionFile + "' into table revision;");
          System.out.println("TABLE = revision succesfully created." );   
                 
          
      } catch (SQLException e) {
          e.printStackTrace();
      } 
  }

  public boolean checkWikipediaTables() {
      // wiki must be already created
      
      // If database does not exist create it, if it exists delete it and create an empty one.      
      System.out.println("Checking if the TABLE=text already exist.");
      try {
          rs = st.executeQuery("SHOW TABLES;");
      } catch (Exception e) {
          e.printStackTrace();
      }
      boolean resText=false, resPage=false, resRevision=false;
      try { 
         
          while( rs.next() ) {
            String str = rs.getString(1);
            if( str.contentEquals("text") )
               resText=true;
            else if( str.contentEquals("page") )
               resPage=true;
            else if( str.contentEquals("revision") )
               resRevision=true;
            
          } 
          if(resText)
            System.out.println("TABLE = text already exist.");          
          if(resPage)
            System.out.println("TABLE = page already exist.");  
          if(resRevision)
            System.out.println("TABLE = revision already exist.");  
                  
          
      } catch (SQLException e) {
          e.printStackTrace();
      } 
      
      if(resText && resPage && resRevision)
        return true;
      else
        return false;
      
  }

  
  public void createWikipediaCleanTextTable() {
      // wiki must be already created
      // String creteWiki = "CREATE DATABASE wiki;";
      String createCleanTextTable = "CREATE TABLE `cleanText` (" +
              " id int UNSIGNED NOT NULL AUTO_INCREMENT," +
              " cleanText mediumblob NOT NULL," +
              " processed BOOLEAN, " +
              " page_id int UNSIGNED NOT NULL, " +
              " text_id int UNSIGNED NOT NULL, " +
              " PRIMARY KEY id (id)" +
              " ) MAX_ROWS=250000 AVG_ROW_LENGTH=10240;";
           
      // If database does not exist create it, if it exists delete it and create an empty one.      
      System.out.println("Checking if the TABLE=cleanText already exist.");
      try {
          rs = st.executeQuery("SHOW TABLES;");
      } catch (Exception e) {
          e.printStackTrace();
      }
      boolean resText=false;
      try { 
         
          while( rs.next() ) {
            String str = rs.getString(1);
            if( str.contentEquals("cleanText") )
               resText=true;
          } 
          if(resText==true){
            System.out.println("TABLE = cleanText already exist deleting.");  
            boolean res0 = st.execute( "DROP TABLE cleanText;" );  
          }
          
          boolean res1;
          int res2;
          // creating TABLE=cleanText
          System.out.println("\nCreating table:" + createCleanTextTable);
          res1 = st.execute( createCleanTextTable );         
          System.out.println("TABLE = cleanText succesfully created.");
         
           
      } catch (SQLException e) {
          e.printStackTrace();
      } 
  }

  /***
   * 
   * @return true if TABLE=tableName exist.
   */
  public boolean tableExist(String tableName) {
      // wiki must be already created
           
      // If database does not exist create it, if it exists delete it and create an empty one.      
      System.out.println("Checking if the TABLE=" + tableName + " exist.");
      try {
          rs = st.executeQuery("SHOW TABLES;");
      } catch (Exception e) {
          e.printStackTrace();
      }
      boolean res=false;
      try {        
          while( rs.next() ) {
            String str = rs.getString(1);
            if( str.contentEquals(tableName) )
               res=true;
          } 
          
      } catch (SQLException e) {
          e.printStackTrace();
      } 
      
      return res;
      
  }

 
  /***
   * 
   * @param field
   * @param table
   * @return
   */
  public String[] getIds(String field, String table) {
      int num, i, j;
      String idSet[]=null;
      
      String str = queryTable("SELECT count("+ field + ") FROM " + table + ";");  
      num = Integer.parseInt(str);
      idSet = new String[num];
      
      try {
          rs = st.executeQuery("SELECT " + field + " FROM " + table + ";"); 
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
  
  /***
   * This function will select just the unprocessed cleanText records.
   * @param field
   * @param table
   * @return
   */
  public String[] getUnprocessedTextIds() {
      int num, i, j;
      String idSet[]=null;
      
      String str = queryTable("select count(id) from cleanText where processed=false;");  
      num = Integer.parseInt(str);
      idSet = new String[num];
      
      try {
          rs = st.executeQuery("select id from cleanText where processed=false;"); 
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

  
  public void insertCleanText(String text, String page_id, String text_id){
      //System.out.println("inserting in cleanText: ");
      byte cleanText[]=null;
      
      try {
        cleanText = text.getBytes("UTF8");
      } catch (Exception e) {  // UnsupportedEncodedException
        e.printStackTrace();
      } 
      
      try { 
        //ps = cn.prepareStatement("INSERT INTO cleanText VALUES (null, ?, ?, ?, ?)");
        if(cleanText != null){
            psCleanText.setBytes(1, cleanText);
            psCleanText.setBoolean(2, false);   // it will be true after processed by the FeatureMaker
            psCleanText.setInt(3, Integer.parseInt(page_id));
            psCleanText.setInt(4, Integer.parseInt(text_id));
            psCleanText.execute();
            psCleanText.clearParameters();
        } else
           System.out.println("WARNING: can not insert in cleanText: " + text); 
        
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  
  /****
   * Insert processed sentence in dbselection
   * @param sentence text of the sentence.
   * @param features features if sentences is reliable.
   * @param reliable true/false.
   * @param unknownWords true/false.
   * @param strangeSymbols true/false.
   * @param cleanText_id the id of the cleanText this sentence comes from.
   */
  public void insertSentence(String sentence, byte features[], boolean reliable, boolean unknownWords, boolean strangeSymbols, int cleanText_id){
   /* 
    if(unknownWords) 
      System.out.print("unknownWords");
    if(strangeSymbols)
      System.out.print(" strangeSymbols");  
    if(!reliable)  
      System.out.println(" : inserting unreliable sentence = " + sentence);  
    */
    
    try { 
      // INSERT INTO dbselection VALUES (id, sentence, features, realiable)
      //ps = cn.prepareStatement("INSERT INTO dbselection VALUES (null, ?, ?, ?, ?, ?, ?)");
    
        psSentence.setString(1, sentence);
        psSentence.setBytes(2, features);
        psSentence.setBoolean(3, reliable);
        psSentence.setBoolean(4, unknownWords);
        psSentence.setBoolean(5, strangeSymbols);
        psSentence.setInt(6, cleanText_id);
        psSentence.execute();
      
        psSentence.clearParameters();
      
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /****
   * Creates a wordList table, if already exists deletes it and creates a new to insert
   * current wordList.
   * 
   */
  public void insertWordList(HashMap<String, Integer> wordList){
    String key;
    Integer value; 
    boolean res;
    
    String wordListTable = "CREATE TABLE wordList ( id INT NOT NULL AUTO_INCREMENT, " +                                                       
                                              "word varchar(255) NOT NULL, " +
                                              "frequency INT UNSIGNED NOT NULL, " +
                                              "primary key(id));";
    
    try { 
        System.out.println("Inserting wordList in DB...");
        // if wordList table already exist it should be deleted before inserting this list
        if( tableExist("wordList") ){
          res = st.execute( "DROP TABLE wordList;" );
          res = st.execute( wordListTable );
        } else
          res = st.execute( wordListTable );   
        
        //psWord = cn.prepareStatement("INSERT INTO wordList VALUES (null, ?, ?)");           
        Iterator iteratorSorted = wordList.keySet().iterator();
        while (iteratorSorted.hasNext()) {
           key = iteratorSorted.next().toString();
           value = wordList.get(key);
           psWord.setString(1, key);
           psWord.setInt(2, value);
           psWord.execute();
           psWord.clearParameters();
        } 
        System.out.println("Inserted new wordList table."); 
        
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
  
  /***
   * Get number of words in the wordList table.
   * @return int number of words.
   */
  public int getNumberOfWords(){
      String dbQuery = "SELECT count(word) FROM wordList;";
      String str = queryTable(dbQuery);
      return Integer.parseInt(str);  
  }
  
  /****
   * 
   * @return the word list in a HashMap
   * @param numWords max number of words to retrieve, if numWords=0 then it will retrieve
   *                 all the words in the list in descending order of frequency.
   */
  public HashMap<String, Integer> getMostFrequentWords(int numWords) {

      HashMap<String, Integer> wordList;
      String query;
      int initialCapacity = 200000;  
      
      if(numWords>0){
        query = "SELECT word,frequency from wordList order by frequency desc limit " + numWords +";";
        wordList = new HashMap<String, Integer>(numWords);
      }
      else {  
        query = "SELECT word,frequency from wordList order by frequency desc";
        wordList = new HashMap<String, Integer>(initialCapacity);
      }
      
      try {
          rs = st.executeQuery(query); 
      } catch (Exception e) {
          e.printStackTrace();
      }
      try { 
          while( rs.next() ) {
            wordList.put(rs.getString(1), new Integer(rs.getInt(2)));
          }
      } catch (SQLException e) {
          e.printStackTrace();
      } 
      
      return wordList;
  }
  
  /****
   * 
   * @param fileName file to write the list
   * @param order word or frequency
   * @param numWords max number of words, if numWords=0 then it will retrieve all the
   *                 words in the list.
   */
  public void printWordList(String fileName, String order, int numWords) {
      PrintWriter pw;
      String query;
      String orderBy;
      
      if(order.contentEquals("word"))
        orderBy = "word asc";
      else
        orderBy = "frequency desc";  
      
      if(numWords>0)
        query = "SELECT word,frequency from wordList order by " + orderBy + " limit " + numWords +";"; 
      else   
        query = "SELECT word,frequency from wordList order by " + orderBy;
          
      
      try {
          rs = st.executeQuery(query); 
      } catch (Exception e) {
          e.printStackTrace();
      }
      try { 
          pw = new PrintWriter(new FileWriter(new File(fileName)));
          while( rs.next() ) {
            pw.println(rs.getString(1) + " " + rs.getInt(2));
          }
          pw.close();
          
      } catch (SQLException e) {
          e.printStackTrace();
      } catch (Exception e){
          e.printStackTrace();
      } 
      System.out.println("wordList printed in file: " + fileName + " ordered by " + order);
      
  }
  
  /****
   * 
   * @return the word list in an ordered TreeMap
   */
  public TreeMap<String, Integer> getWordListOrdered() {

      int num;
      TreeMap<String, Integer> wordList = new TreeMap<String, Integer>();
      
      try {
          rs = st.executeQuery("SELECT word,frequency from wordList;"); 
      } catch (Exception e) {
          e.printStackTrace();
      }
      try { 
          while( rs.next() ) {
            wordList.put(rs.getString(1), new Integer(rs.getInt(2)));
          }
      } catch (SQLException e) {
          e.printStackTrace();
      } 
      
      return wordList;
  }
  
  public String getSentenceFromTable(int id, String table) {
      String dbQuery = "Select sentence FROM " + table + " WHERE id=" + id;
      return queryTable(dbQuery);      
  }
  
  // Firts filtering:
  // get first the page_title and check if it is not Image: or  Wikipedia:Votes_for_deletion/
  // maybe we can check also the length
  public String getTextFromWikiPage(String id, int minPageLength, StringBuffer old_id, PrintWriter pw) {
      String pageTitle, pageLen, dbQuery, textId, text=null;
      byte[] textBytes=null;
      int len;
      
      dbQuery = "Select page_title FROM page WHERE page_id=" + id;
      pageTitle = queryTable(dbQuery);
      
      dbQuery = "Select page_len FROM page WHERE page_id=" + id;
      pageLen = queryTable(dbQuery);
      len = Integer.parseInt(pageLen);
      
      if(len < minPageLength || pageTitle.contains("Wikipedia:") 
                             || pageTitle.contains("Image:")
                             || pageTitle.contains("Template:")
                             || pageTitle.contains("Category:")
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
        //System.out.println("PAGE page_id=" + id + " PAGE SELECTED page title=" + pageTitle + " Len=" + len);
        if(pw!=null)
          pw.println("\nSELECTED PAGE TITLE=" + pageTitle + " Len=" + len);
        
        dbQuery = "select rev_text_id from revision where rev_page=" + id;
        textId = queryTable(dbQuery);
        old_id.delete(0, old_id.length());
        old_id.insert(0, textId);
        
        dbQuery = "select old_text from text where old_id=" + textId;        
        textBytes = queryTableByte(dbQuery); 
        try {
          text = new String(textBytes, "UTF8");
          //System.out.println("  TEXT: " + text);
        } catch (Exception e) {  // UnsupportedEncodedException
             e.printStackTrace();
        } 
        
      }
      return text;   
  }
  
  public String getCleanText(String id){
      String dbQuery, text=null;
      byte[] textBytes=null;
             
      dbQuery = " select cleanText from cleanText where id=" + id;
      textBytes = queryTableByte(dbQuery);
      
      try {
          text = new String(textBytes, "UTF8");
          //System.out.println("  TEXT: " + text);
      } catch (Exception e) {  // UnsupportedEncodedException
        e.printStackTrace();
      } 
      // once retrieved the text record mark it as processed
      updateTable("UPDATE cleanText SET processed=true WHERE id="+id);   
     
      return text;     
  }
  
  
  public String getFeaturesFromTable(int id, String table) {
      String dbQuery = "Select features FROM " + table + " WHERE id=" + id;
      return queryTable(dbQuery);
      
  }
  
  private boolean updateTable(String sql)
  {
      String str = "";
      boolean res=false;
      
      try {
          res = st.execute(sql);
      } catch (Exception e) {
          e.printStackTrace();
      } 
      
      return res;
      
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

  public static void main(String[] args) throws Exception{
      
      //Put sentences and features in the database.
      DBHandler wikiToDB = new DBHandler();

      wikiToDB.createDBConnection("localhost","wiki","marcela","wiki123");
      wikiToDB.setDBTable("dbselection");
      
 
      wikiToDB.createDataBaseSelectionTable();
      
      wikiToDB.getIdListOfType("reliable");
      
      int num = wikiToDB.getNumberOfReliableSentences();
      
      String feas = wikiToDB.getFeaturesFromTable(1, "dbselection");
      
      
      byte [] tmp = new byte[4];
      tmp[0] = 10;
      tmp[1] = 20;
      tmp[2] = 30;
      tmp[3] = 40;
      
      wikiToDB.insertSentence("sentence1", tmp, true, false, false, 1);
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
