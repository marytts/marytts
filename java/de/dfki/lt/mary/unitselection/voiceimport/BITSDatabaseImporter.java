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
 *  \- ANNOT
 *      \- Session1
 *          \- Annotation files
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
            
            Map newBasenames = new HashMap();
 
            File annoDir = new File(rootDirName+"/ANNOT");
           if (!annoDir.exists()) throw new IOException("No such directory: "+ annoDir);
            //the subdirs of annoDir contain the sessions
            File[] sessions = annoDir.listFiles(); 
            for (int i=0;i<sessions.length;i++){
                File session = sessions[i];
                String pathname = session.getCanonicalPath();
                //take all files ending on _0.par
                String[] annoFiles = session.list();
                for (int j=0;j<annoFiles.length;j++){
                    String nextFile = annoFiles[j];
                    if (nextFile.endsWith("_0.par")){
                        //add to basenames
                        String nextBasename = nextFile.substring(0, nextFile.indexOf("."));
                        System.out.println(nextBasename);
                        //open the file
                        BufferedReader annoIn = 
                            new BufferedReader(
                                    new FileReader(
                                            new File(pathname+"/"+nextFile)));
                        
                        String line;
                        while((line = annoIn.readLine()) != null){
                            if (line.startsWith("ORT")){
                                //line looks like: ORT: 0   DATOOUHTADEU
                                StringTokenizer lineElements = new StringTokenizer(line.trim());
                                lineElements.nextToken();
                                lineElements.nextToken();
                                //next Element gives us the text
                                String text = lineElements.nextToken();
                                newBasenames.put(nextBasename,"( "+nextBasename+" \""+text+".\" )");
                            }
                        }
                        
                    }
                }
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
                        new FileWriter(
                                new File(rootDirName+"/etc/txt.done.data")));
           
           File wavDir = new File(dbLayout.wavDirName());
           String pathname = wavDir.getCanonicalPath();
           String[] wavFiles = wavDir.list();
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
                    System.out.println("Deleting wavfile "+nextFile);
                   File nextWav = new File(pathname+"/"+nextFile);
                   //comment this out if deletion fails:
                   //System.gc();
                   //Thread.sleep(100);
                   if(!nextWav.delete()){
                       System.out.println("Deletion failed!");
                   }
                   
                }
           }
          
            
           textOut.flush();
           textOut.close();
           
           //check if newBasenames is empty
           if (!newBasenames.isEmpty()){
               //we didnt find all wav files
               //delete all remaining basenames
               System.out.println("Did not find all wave files. Deleting basenames ...");
               newBasenameSet = newBasenames.keySet();
               for (Iterator it = newBasenameSet.iterator();it.hasNext();){
                   String nextBasename = (String) it.next();
                   System.out.println(nextBasename);
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


