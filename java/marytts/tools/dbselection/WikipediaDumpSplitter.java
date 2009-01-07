package marytts.tools.dbselection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;
import java.util.Vector;

public class WikipediaDumpSplitter {
    
    private int maxPages = 0;
    private String xmlWikipediaDumpFile = null;
    private String dirOuputFiles = null;
    
    public void setXmlWikipediaDumpFile(String str){ xmlWikipediaDumpFile = str; }
    public void setDirOuputFiles(String str){ dirOuputFiles = str; }
    public void setMaxPages(int val){ maxPages = val; }
    
    public String getXmlWikipediaDumpFile(){ return xmlWikipediaDumpFile; }
    public String getDirOuputFiles(){ return dirOuputFiles; }
    public int getMaxPages(){ return maxPages; }
    
    
    /***
     * This function splits a big XML wikipedia file (ex. 19GB for enwiki) into small XML chunks according to the 
     * specified maximum number of pages per chunk.
     * @param xmlFile name of the XML wikipedia file.
     * @param dirFiles directory where to save the small xml chunks.
     * @param maxPagesPerChunk maximum number of pages per chunk, it can be for example 250000 pages (~30MB).
     */
    private void splitWikipediaDump(String xmlFile, String dirFiles, int maxPagesPerChunk){
        
      int totalPageNumber = 0;
      int currentPageNumber = 0;   
      int numFiles = 0;
      String outFileName = "";
      String nextLine; 
      boolean checkSiteInfo = true;
      boolean siteInfo = false;
      StringBuffer strInfo = new StringBuffer("");
      FileWriter outputStream = null;
      int num = (int)Math.round(maxPagesPerChunk * 0.50);
      
      // we need to scan line by line a big (for ex. 19GB for enwiki) xml file
      Scanner s = null;
      try {
        s = new Scanner(new BufferedReader(new FileReader(xmlFile)));
        
        while (s.hasNext()) {
          nextLine = s.nextLine(); 
         
          // get first the siteinfo
         if(checkSiteInfo) {
              if (nextLine.startsWith("  <siteinfo") )
                siteInfo=true;
              else if(nextLine.startsWith("  </siteinfo") ){
                siteInfo=false;
                checkSiteInfo=false;
                strInfo.append(nextLine + "\n");
                System.out.println("Extracted <siteinfo> from header.");
                //System.out.println("siteInfo:" + strInfo);
              } else if( nextLine.startsWith("  </page") ){
                // if a page appears before the siteInfo maybe there is no siteinfo in the header 
                System.out.println("Error: problem with siteInfo in file " + xmlFile);
                return;
              }
              if(siteInfo)
                strInfo.append(nextLine + "\n");

        } else if( !nextLine.startsWith("<mediawiki") && !nextLine.startsWith("</mediawiki>") ){
 
        if( currentPageNumber == maxPagesPerChunk){
             outputStream.write("</mediawiki>\n");
             currentPageNumber = 0; 
             outputStream.close();
             outputStream = null; 
        } 
        if( outputStream == null ){
              numFiles++;
              outFileName = dirFiles + "page" + Integer.toString(numFiles) + ".xml";
              System.out.println("outFileName("+ maxPagesPerChunk + "):" + outFileName);
              outputStream = new FileWriter(outFileName);
              outputStream.write("<mediawiki>\n");
              // we need the siteinfo at the begining of each chunk
              outputStream.write(strInfo.toString());
              outputStream.write(nextLine + "\n");
        } else
            outputStream.write(nextLine + "\n");  
         
        if( nextLine.startsWith("  </page") ){
            currentPageNumber++;
            totalPageNumber++;
            if( (totalPageNumber % num) == 0 )
              System.out.println("number of wikipages = " + totalPageNumber);   
        }  
       } // if no mediawiki line
       } // while next line
        
       // final part if remaining pages
        if(currentPageNumber > 0){
            System.out.println("number of wikipages = " + totalPageNumber + " last chunk with " + currentPageNumber + " pages.");
            outputStream.write("</mediawiki>\n");        
            outputStream.close();
         }
      } catch (Exception e) {
          System.err.println("Exception: " + e.getMessage());
      } finally {
          if (s != null)
            s.close();
      }   
     }
    
    /**
     * Read and parse the command line args
     * 
     * @param args the args
     * @return true, if successful, false otherwise
     */
    private boolean readArgs(String[] args){
        
        String help = "\nUsage: java WikipediaDumpSplitter xmlDumpFile outputFilesDir maxNumberPages \n" +
        "      xmlDumpFile: xml wikipedia file. \n" +
        "      outputFilesDir: directory where the small xml chunks will be saved.\n" +
        "      maxNumberPages: maximum number of pages of each small xml chunk. \n\n";
              
        if (args.length == 3){  // minimum 3 parameters
          setXmlWikipediaDumpFile(args[0]);
          setDirOuputFiles(args[1]);
          setMaxPages(Integer.parseInt(args[2]));
        }      
        if(getXmlWikipediaDumpFile()==null || getDirOuputFiles()==null || getMaxPages()==0 ){
          System.out.println(help);
          return false; 
        } else
          return true;
    }
    
    
    public static void main(String[] args) throws Exception{
        String wFile, cmdLine;
        Vector<String> filesToProcess;
        Vector<String> filesDone;
        
        WikipediaDumpSplitter wiki = new WikipediaDumpSplitter(); 
      
        /* check the arguments */
        if (!wiki.readArgs(args))
            return;
      
        wiki.splitWikipediaDump(wiki.getXmlWikipediaDumpFile(), wiki.getDirOuputFiles(), wiki.getMaxPages());
        
    }  

}
