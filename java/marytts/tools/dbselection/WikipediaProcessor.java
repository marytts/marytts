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
import java.util.Vector;

/**
 * WikipediaProcesso
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
    private String xml2sqlCommand=null;
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
    public void setXml2SqlCommand(String str){ xml2sqlCommand = str; }
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
    public String getXml2SqlCommand(){return xml2sqlCommand; }
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
    
    
    //
    /**
     * Read and parse the command line args
     * 
     * @param args the args
     * @return true, if successful, false otherwise
     */
    private boolean readArgs(String[] args){
        
        String help = "\nUsage: java WikipediaMarkupCleaner -locale en_US -mysqlHost host -mysqlUser user -mysqlPasswd passwd \n" +
        "                                       -mysqlDB wikiDB -listFile wikiFileList -xml2sql ./bin/xml2sql\n" +
        "                                       [-minPage 10000 -minText 1000 -maxText 15000] \n\n" +
        "      -listFile is a a text file that contains the xml wikipedia file names to be procesed. \n" +
        "      -xml2sql is the command (and its path) used to convert the xml files into mysql tables.\n\n" +
        "      default/optional: [-minPage 10000 -minText 1000 -maxText 15000] \n" +
        "      -minPage is the minimum size of a wikipedia page that will be considered for cleaning.\n" +
        "      -minText is the minimum size of a text to be kept in the DB.\n" +
        "      -maxText is used to split big articles in small chunks, this is the maximum chunk size. \n";
              
        if (args.length >= 14){  // minimum 10 parameters
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
            
            else if(args[i].contentEquals("-xml2sql") && args.length >= (i+1) )
                setXml2SqlCommand(args[++i]);
            
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
       } else { // num arguments less than 14
          System.out.println(help);
          return false;
        }
        
        if(getLocale() == null || getXml2SqlCommand() == null ) {
            System.out.println("\nMissing required parameters, (one/several required variables are null).");
            printParameters();
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
    
    /**
     * A general process launcher for the various tasks
     * (copied from ESTCaller.java)
     * @param cmdLine the command line to be launched.
     * @param task a task tag for error messages, such as "Pitchmarks" or "LPC".
     * @param the basename of the file currently processed, for error messages.
     */
    private void launchProc( String cmdLine) {
        
        Process proc = null;
        BufferedReader procStdout = null;
        String line = null;
        System.out.println("Running: "+ cmdLine);
        
        try {
            /* Java 1.0 equivalent: */
            proc = Runtime.getRuntime().exec( cmdLine );
            
            /* Collect stdout and send it to System.out: */
            procStdout = new BufferedReader( new InputStreamReader( proc.getInputStream() ) );
            while( true ) {
                line = procStdout.readLine();
                if ( line == null ) break;
                System.out.println( line );
            }
            /* Wait and check the exit value */
            proc.waitFor();
            if ( proc.exitValue() != 0 ) {
                throw new RuntimeException("Computation failed, command line was: [" + cmdLine + "]." );
            }
        }
        catch ( IOException e ) {
            throw new RuntimeException("Computation provoked an IOException: ", e );
        }
        catch ( InterruptedException e ) {
            throw new RuntimeException("Computation interrupted on file : ", e );
        }
        
    }    
    
    
    public static void main(String[] args) throws Exception{
        
        String wFile, cmdLine;
        Vector<String> filesToProcess;
        Vector<String> filesDone;
        
        WikipediaProcessor wiki = new WikipediaProcessor(); 
       // WikipediaMarkupCleaner wikiCleaner = new WikipediaMarkupCleaner(); 
        
        /* check the arguments */
        if (!wiki.readArgs(args))
            return;
        
        wiki.printParameters();
        
       
        
        filesToProcess = wiki.getWikipediaFiles(wiki.getListFile());
        filesDone = wiki.getWikipediaFiles("./done.txt");
        if(filesDone==null)
           filesDone = new Vector<String>();
        
        if(filesToProcess != null){
          for(int i=0; i<filesToProcess.size(); i++){
            wFile = filesToProcess.elementAt(i);
            if(filesDone.indexOf(wFile) == -1){
               System.out.println("\n_______________________________________________________________________________"); 
               System.out.println("\nProcessing file:" + wFile);
               
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
               
               
               // create the wikipedia files text, page and revision.
               cmdLine = wiki.getXml2SqlCommand() + " " + wFile; 
               System.out.println("Using command xml2sql to create files: text.txt, page.txt and revision.txt");
               wiki.launchProc(cmdLine);
               
               // now call the wikipediaMarkupCleaner
               wikiCleaner.setTextFile("./text.txt");
               wikiCleaner.setPageFile("./page.txt");
               wikiCleaner.setRevisionFile("./revision.txt");
               wikiCleaner.processWikipediaSQLTables();
               
               wikiCleaner = null;
               
               // when finished
               wiki.setWikipediaFileDone("./done.txt", wFile);
               // delete files text, page and revision
               
            } else
              System.out.println("File already procesed: " + wFile);
              
          }
            
            
        } else
          System.out.println("No files to process..");
            
        //wikiCleaner.processWikipediaSQLTables();
        
        
        
        
        
    }
    
    
    
}
