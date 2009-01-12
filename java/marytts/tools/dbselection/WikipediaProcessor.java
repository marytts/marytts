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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;
import java.util.Vector;


/**
 * WikipediaProcessor
 * This program processes one by one the xml files split with wikipediaDumpSplitter.
 * Each xml file is converted to an sql source file with mwdumper-2008-04-13.jar (org.mediawiki.dumper.Dumper)
 * The tables names in the sql source are prefixed with the local (ex. en_US, de etc.) 
 * Each sql source is loaded in a mysql database, basically the tables local_text, local_page and local_revision are loaded.
 * Once the tables are loaded the WikipediMarkupCleaner is used to extract clean text and a wordList, as a 
 * result two tables will be created in the database: local_cleanText and local_wordList (the wordList is also 
 * saved in a file).
 * 
 * @author Marcela Charfuelan.
 */
public class WikipediaProcessor {
    
    // locale
    private String locale=null;
    // mySql database 
    private String mysqlHost=null;
    private String mysqlDB=null;
    private String mysqlUser=null;
    private String mysqlPasswd=null;
    // Wikipedia files:
    private String listFile=null;
    private String textFile=null;
    private String pageFile=null;
    private String revisionFile=null;
    private String wikiLog = null;
    private boolean debug = false;
    private String debugPageId = null;
    // Default settings for max page length and min and max text length
    private int minPageLength=10000;  // minimum size of a wikipedia page, to be used in the first filtering of pages
    private int minTextLength=1000;
    private int maxTextLength=15000;  // the average lenght in one big xml file is approx. 12000
    
    // Use this variable to save time not loading Wiki tables, if they already exist in the DB
    private boolean loadWikiTables = true;
   
    // Use this variable to do not create a new cleanText table, but adding to an already existing cleanText table.
    private boolean deleteCleanTextTable = false;
    
    public void setLocale(String str){ locale = str; }
    public void setMysqlHost(String str){ mysqlHost = str; }
    public void setMysqlDB(String str){ mysqlDB = str; }
    public void setMysqlUser(String str){ mysqlUser = str; }
    public void setMysqlPasswd(String str){ mysqlPasswd = str; }
   
    public void setListFile(String str){ listFile = str; }
    public void setTextFile(String str){ textFile = str; }
    public void setPageFile(String str){ pageFile = str; }
    public void setRevisionFile(String str){ revisionFile = str; }
    public void setWikiLog(String str){ wikiLog = str; }
    public void setTestId(String str){ debugPageId = str; }
    
    public void setMinPageLength(int val){ minPageLength = val; }
    public void setMinTextLength(int val){ minTextLength = val; }
    public void setMaxTextLength(int val){ maxTextLength = val; }
    
    public void setDebug(boolean bval){ debug = bval; }
    public void setLoadWikiTables(boolean bval){ loadWikiTables = bval; }
    public void setDeleteCleanTextTable(boolean bval){ deleteCleanTextTable = bval; }
       
    public String getLocale(){ return locale; }
    public String getMysqlHost(){ return mysqlHost; }
    public String getMysqlDB(){ return mysqlDB; }
    public String getMysqlUser(){ return mysqlUser; }
    public String getMysqlPasswd(){ return mysqlPasswd; }
    
    public String getListFile(){ return listFile; }
    public String getTextFile(){ return textFile; }
    public String getPageFile(){ return pageFile; }
    public String getRevisionFile(){ return revisionFile; }
    public String getWikiLog(){ return wikiLog; }
    public String getTestId(){ return debugPageId; }
    
    public int getMinPageLength(){ return minPageLength; }
    public int getMinTextLength(){ return minTextLength; }
    public int getMaxTextLength(){ return maxTextLength; }
    
    public boolean getDebug(){ return debug; }
    public boolean getLoadWikiTables(){ return loadWikiTables; }
    public boolean getDeleteCleanTextTable(){ return deleteCleanTextTable; }
    

    private void printParameters(){
        System.out.println("WikipediaMarkupCleaner parameters:" +
        "\n  -mysqlHost " + getMysqlHost() +
        "\n  -mysqlUser " + getMysqlUser() +
        "\n  -mysqlPasswd " + getMysqlPasswd() +
        "\n  -mysqlDB " + getMysqlDB() +
        "\n  -listFile " + getListFile() +
        "\n  -minPage " + getMinPageLength() +
        "\n  -minText " + getMinTextLength() +
        "\n  -maxText " + getMaxTextLength() );
        
    }
    
    
    /**
     * Read and parse the command line args
     * 
     * @param args the args
     * @return true, if successful, false otherwise
     */
    private boolean readArgs(String[] args){
        
        String help = "\nUsage: java WikipediaProcessor -locale en_US -mysqlHost host -mysqlUser user -mysqlPasswd passwd \n" +
        "                                   -mysqlDB wikiDB -listFile wikiFileList.\n" +
        "                                   [-minPage 10000 -minText 1000 -maxText 15000] \n\n" +
        "      -listFile is a a text file that contains the xml wikipedia file names to be procesed. \n" +
        "      This program requires the jar file mwdumper-2008-04-13.jar (or latest). \n\n" +
        "      default/optional: [-minPage 10000 -minText 1000 -maxText 15000] \n" +
        "      -minPage is the minimum size of a wikipedia page that will be considered for cleaning.\n" +
        "      -minText is the minimum size of a text to be kept in the DB.\n" +
        "      -maxText is used to split big articles in small chunks, this is the maximum chunk size. \n";
              
        if (args.length >= 12){  // minimum 12 parameters
          for(int i=0; i<args.length; i++) { 
            if(args[i].contentEquals("-locale") && args.length >= (i+1) )
              setLocale(args[++i]);
              
            else if(args[i].contentEquals("-mysqlHost") && args.length >= (i+1) )
              setMysqlHost(args[++i]);
            
            else if(args[i].contentEquals("-mysqlUser") && args.length >= (i+1) )
               setMysqlUser(args[++i]);
              
            else if(args[i].contentEquals("-mysqlPasswd") && args.length >= (i+1) )
               setMysqlPasswd(args[++i]);
            
            else if(args[i].contentEquals("-mysqlDB") && args.length >= (i+1) )
              setMysqlDB(args[++i]);
                      
            else if(args[i].contentEquals("-listFile") && args.length >= (i+1) )
                setListFile(args[++i]);
            
            // From here the arguments are optional
            else if(args[i].contentEquals("-minPage") && args.length >= (i+1) )
              setMinPageLength(Integer.parseInt(args[++i]));
            
            else if(args[i].contentEquals("-minText") && args.length >= (i+1) )
              setMinTextLength(Integer.parseInt(args[++i]));
            
            else if(args[i].contentEquals("-maxText") && args.length >= (i+1) )
             setMaxTextLength(Integer.parseInt(args[++i]));
            
            else { //unknown argument
                System.out.println("\nOption not known: " + args[i]);
                System.out.println(help);
                return false;
              }
            
          }
       } else { // num arguments less than 12
          System.out.println(help);
          return false;
        }
        
        if(getMysqlHost()==null || getMysqlUser()==null || getMysqlPasswd()==null || getMysqlDB()==null){
            System.out.println("\nMissing required mysql parameters (one/several required variables are null).");
            printParameters();
            System.out.println(help);
            return false;
         } 
        
        if(getListFile()==null){
            System.out.println("\nMissing required parameter -listFile wikiFileList.\n");
            printParameters();
            System.out.println(help);
            return false; 
        }
        
        
      return true;  
    }
    
    private Vector<String> getWikipediaFiles(String fileName){
        
      BufferedReader in = null;
      String line;
      Vector<String> files = null;
      
      // check if the file exist
      File f  = new File(fileName);
      if(f.exists())
      {  
        files = new Vector<String>();    
        try {
            in = new BufferedReader(new FileReader(fileName)); 
            while ((line = in.readLine()) != null) {
              files.add(line);
            }
            in.close();
          
        } catch( Exception e ) {
          e.printStackTrace();
        }       
      }
      
      return files;
      
    }
    

    private void setWikipediaFileDone(String fileName, String fileDone){
        
        RandomAccessFile out = null;
        
        try {
            out = new RandomAccessFile(fileName, "rw");
            out.seek(out.length());
            out.writeBytes(fileDone+"\n");
            out.close();
            
        } catch( Exception e ) {
            e.printStackTrace();
        }       
        
      }
    
    
    private void addLocalePrefixToTables(String sqlFile, String outFile){
        String line, localLine;
        Scanner s = null;
        FileWriter outputStream = null;
        
        try {
          s = new Scanner(new BufferedReader(new FileReader(sqlFile)));
          
          
          System.out.println("Adding local prefix to sql tables.");
          outputStream = new FileWriter(outFile);
          
          
          while (s.hasNext()) {
            line = s.nextLine();
            
            if(line.contains("INSERT INTO ")){
               localLine = line.replaceAll("INSERT INTO ", "INSERT INTO " + locale + "_");
               outputStream.write(localLine+"\n");
            } else
                outputStream.write(line+"\n");  
            
          }
          outputStream.close();
          System.out.println("Added local=" + locale + " to tables in outFile:" + outFile);
          
        }catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
        } finally {
            if (s != null)
              s.close();
        }   
        
        
    }
    
    
    public static void main(String[] args) throws Exception{
        String wFile;                    // xml wiki file
        String sqlDump;                  // sql source file after converting xml --> sql
        String sqlLocaleDump;            // sql source file after adding locale prefix to tables names
        String doneFile = "./done.txt";  // file that contains the xml files already processed
        Vector<String> filesToProcess;
        Vector<String> filesDone;
        WikipediaProcessor wiki = new WikipediaProcessor(); 
        
        /* check the arguments */
        if (!wiki.readArgs(args))
            return;
        wiki.printParameters();
        
        DBHandler wikiToDB = new DBHandler(wiki.getLocale());
       
       
        filesToProcess = wiki.getWikipediaFiles(wiki.getListFile());
        filesDone = wiki.getWikipediaFiles(doneFile);
        if(filesDone==null)
           filesDone = new Vector<String>();
        
        if(filesToProcess != null){
          for(int i=0; i<filesToProcess.size(); i++){
            wFile = filesToProcess.elementAt(i);
            if(filesDone.indexOf(wFile) == -1){
               System.out.println("\n_______________________________________________________________________________"); 
               
               System.out.println("\nProcessing xml file:" + wFile);
               System.out.println("Converting xml file into sql source file and loading text, page and revision tables into the DB.");
               
               WikipediaMarkupCleaner wikiCleaner = new WikipediaMarkupCleaner();
               
               // Set parameters in the WikipediaMarkupCleaner
               wikiCleaner.setDebug(false);
               wikiCleaner.setDeleteCleanTextTable(false);
               wikiCleaner.setLoadWikiTables(true);
               wikiCleaner.setLocale(wiki.getLocale());
               
               wikiCleaner.setMaxTextLength(wiki.getMaxTextLength());
               wikiCleaner.setMinPageLength(wiki.getMinPageLength());
               wikiCleaner.setMinTextLength(wiki.getMinTextLength());
               wikiCleaner.setMysqlDB(wiki.getMysqlDB());
               wikiCleaner.setMysqlHost(wiki.getMysqlHost());
               wikiCleaner.setMysqlPasswd(wiki.getMysqlPasswd());
               wikiCleaner.setMysqlUser(wiki.getMysqlUser());
           /*    
              System.out.println("Creating connection to DB server...");
              wikiToDB.createDBConnection(wiki.getMysqlHost(),wiki.getMysqlDB(),wiki.getMysqlUser(),wiki.getMysqlPasswd());
               
               // Before runing the mwdumper the tables text, page and revision should be deleted and created empty.
               wikiToDB.createEmptyWikipediaTables();
               
               // Run the mwdumper jar file xml -> sql
               /*
               sqlDump = wFile + ".sql";
               String[] argsDump = new String[3];
               argsDump[0] = "--output=file:"+sqlDump;
               argsDump[1] = "--format=sql:1.5";
               argsDump[2] = wFile;
               */
               
            /*   
               sqlDump = wFile + ".sql";
               String[] argsDump = new String[3];
               argsDump[0] = "--output=mysql://" + wiki.getMysqlHost() + "/" + wiki.getMysqlDB() 
                             + "?user=" + wiki.getMysqlUser() + "&password=" + wiki.getMysqlPasswd() 
                             + "&useUnicode=true&characterEncoding=utf8";
               argsDump[1] = "--format=sql:1.5";
               argsDump[2] = wFile;
               
               //--- The following ClassLoader code from:
               //   http://java.sun.com/docs/books/tutorial/deployment/jar/examples/JarClassLoader.java
               // Class c = loadClass(name);                                    // this does not work (example from sun)
               // Class c = Class.forName("org.mediawiki.dumper.Dumper");                              // this works
               Class c = ClassLoader.getSystemClassLoader().loadClass("org.mediawiki.dumper.Dumper");  // this also works
               Method m = c.getMethod("main", new Class[] { argsDump.getClass() });
               m.setAccessible(true);
               int mods = m.getModifiers();
               if (m.getReturnType() != void.class || !Modifier.isStatic(mods) ||
                   !Modifier.isPublic(mods)) {
                   throw new NoSuchMethodException("main");
               }
               try {
                   m.invoke(null, new Object[] { argsDump });
             
               } catch (IllegalAccessException e) {
                   // This should not happen, as we have disabled access checks
               } 
               
               // System.out.println("Created sql source file:" + sqlDump);
               // Now I need to add/change the prefix locale to the table names
               wikiToDB.addLocalePrefixToWikipediaTables();  // this change the name of already created and loaded tables
               
               
               sqlLocaleDump = sqlDump + "." + wiki.getLocale() + ".sql";
               //++wiki.addLocalePrefixToTables(sqlDump, sqlLocaleDump);
               // delete generated files
            //   System.out.println("Deleting file:" + sqlDump);    
            //   File dump = new File(sqlDump);
            //   if(dump.exists())
            //     dump.delete();
              
              wikiToDB.closeDBConnection(); 
              */ 
               // now call the wikipediaMarkupCleaner
              //++ wikiCleaner.setSqlSourceFile(sqlLocaleDump);
               
               
               
               wikiCleaner.setXmlWikiFile(wFile);
               wikiCleaner.processWikipediaSQLSourceFile();              
               wikiCleaner = null;
               
               
               // when finished
               wiki.setWikipediaFileDone("./done.txt", wFile);
               
               // delete generated files
           //    System.out.println("Deleting file:" + sqlLocaleDump);              
           //    File localDump = new File(sqlLocaleDump);
           //    if(localDump.exists())
           //      localDump.delete();
              
            } else
              System.out.println("File already procesed: " + wFile);
              
          }
            
            
        } else
          System.out.println("No files to process..");
             
       
        
        
    }
    
    
    
}
