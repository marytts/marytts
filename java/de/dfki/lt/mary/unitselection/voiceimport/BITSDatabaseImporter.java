/**
 * Copyright 2006 DFKI GmbH.
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
package de.dfki.lt.mary.unitselection.voiceimport;

import de.dfki.lt.mary.MaryProperties;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Class to import a BITS-database
 * The assumed directory structure is the following:
 * dvd-lg*
 *  \- list of logatones
 *  \- wav
 *      \- all wav files from all sessions
 * 
 * @author Anna
 * @date 08.12.2006
 */
public class BITSDatabaseImporter implements VoiceImportComponent{

    
    private DatabaseLayout dbLayout;
    private BasenameList basenames;
    
    
    /**
     * Create new BITSDatabaseImporter
     * 
     * @param dbLayout the database layout
     * @param baseNames the list of file base names
     */
    public BITSDatabaseImporter(DatabaseLayout dbLayout,
            BasenameList basenames){
        this.dbLayout = dbLayout;
        this.basenames = basenames;
    }
    
    public boolean compute(){
        
        System.out.println("Converting BITS Text into Festival format ... ");
        try {
            /* Convert the annotation into etc/txt.done.data
             * and the label files */
            File rootDirFile = new File(dbLayout.rootDirName());
            String rootDirName = rootDirFile.getCanonicalPath();
            //make etc-directory if it does not exist
            File etcDir = new File(rootDirName+"/etc");
            if (!etcDir.exists()){
                etcDir.mkdir();
            }
           /* Find out the naming conventions of the wavFiles */ 
           File wavDir = new File(dbLayout.wavDirName());
           String pathname = wavDir.getCanonicalPath();
           String[] wavFiles = wavDir.list();
           String baseName = wavFiles[0];
           baseName = baseName.substring(0,baseName.indexOf("_")-4);
           System.out.println(baseName);
           
           /* Read in the list of logatones (=transcript)*/
            Map newBasenames = new HashMap();
 
            //logatone file
            File logatoneFile = new File(rootDirName+"/BITS-US.TBL");
            //open the file
            BufferedReader logaToneIn = 
                  new BufferedReader(
                       new InputStreamReader(
                            new FileInputStream(logatoneFile) ,"UTF-8"));
                        
            String line;
            while((line = logaToneIn.readLine()) != null){
                //line looks like 0001;d|a|s d|'U|N|k|@|l v|'a:6 Q|aI|n k|'U6|ts|@|s Q|U|n|t l|'aI|z|6 v|'e:6|d|@|n|d|@|s f|E6|S|v|'I|n|d|@|n;das Dunkel war ein kurzes und leiser werdendes Verschwinden .;das Dunkel war ein kurzes und leiser werdendes Verschwinden 
                StringTokenizer lineElements = new StringTokenizer(line.trim(),";");
                String nextExt = lineElements.nextToken();
                lineElements.nextToken();
                String nextBasename = baseName+nextExt+"_0";
                
                String text = lineElements.nextToken();
                //TODO: ugly; clean up
                if (!((text.endsWith("."))
                        || (text.endsWith("?")
                                || (text.endsWith("!")
                                        || (text.endsWith("!")
                                                || (text.endsWith(","))))))){
                    text = text+".";
                }
                System.out.println("Adding "+nextBasename+" : "+text);
                newBasenames.put(nextBasename," ( "+nextBasename
                                            +" \""+text+"\" )");
            }
            
            
           /* Correct the basenameList */
           basenames.clear();
           Set newBasenameSet = newBasenames.keySet();
           for (Iterator it = newBasenameSet.iterator();it.hasNext();){
               basenames.add((String)it.next());
           }
           
           
           /* Now delete the wav-files not in the basenames*/
           
            //open text output file
            PrintWriter textOut = 
                new PrintWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(
                                        new File( rootDirName+"/etc/txt.done.data")), "UTF-8"));                                
           
           
           List wavFilesToDelete = new ArrayList();
           for (int j=0;j<wavFiles.length;j++){
                String nextFile = wavFiles[j];
                String nextBasename = nextFile.substring(0,nextFile.indexOf("."));
                if (newBasenames.containsKey(nextBasename)){
                   //print basename and remove it from the list
                   textOut.println(newBasenames.get(nextBasename));
                   newBasenames.remove(nextBasename);
                } else {
                   //delete the wave file
                    System.out.println("Deleting wavfile "+nextBasename);
                   File nextWav = new File(pathname+"/"+nextFile);
                   //comment this out if deletion fails:
                   //System.gc();
                   //Thread.sleep(100);
                   //if(!nextWav.delete()){
                   //    System.out.println("Deletion failed!");
                   //}
                   
                }
           }
          
            
           textOut.flush();
           textOut.close();
           
           //check if newBasenames is empty
           if (!newBasenames.isEmpty()){
               //we didnt find all wav files
               //delete all remaining basenames
               //System.out.println("Did not find all wave files. Deleting basenames ...");
               newBasenameSet = newBasenames.keySet();
               for (Iterator it = newBasenameSet.iterator();it.hasNext();){
                   String nextBasename = (String) it.next();
                   //System.out.println(nextBasename);
                   basenames.remove(nextBasename);
               }
           }
            
           System.out.println("... done.");
            
            
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    
    
    public int getProgress(){
        return -1;
    }
    
}


