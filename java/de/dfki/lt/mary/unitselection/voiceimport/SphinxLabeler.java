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

import java.io.*;
import java.util.*;
import java.text.DecimalFormat;

import de.dfki.lt.mary.unitselection.voiceimport.SphinxTrainer.StreamGobbler;


/**
 * Preparate the directory of the voice for sphinx labelling
 * @author Anna Hunecke
 */
public class SphinxLabeler extends VoiceImportComponent {
    
    private DatabaseLayout db;
    
    public final String SPHINX2DIR = "SphinxLabeler.sphinx2Dir";
    public final String STDIR = "SphinxLabeler.stDir";
    
     public final String getName(){
        return "SphinxLabeler";
    }
    
   public SortedMap getDefaultProps(DatabaseLayout db){
       this.db = db;
       if (props == null){
           props = new TreeMap();
           String sphinx2dir = System.getProperty("SPHINX2DIR");
           if ( sphinx2dir == null ) {
               sphinx2dir = "/project/mary/anna/sphinx/sphinx2/";
           }
           props.put(SPHINX2DIR,sphinx2dir);
           props.put(STDIR,db.getProp(db.ROOTDIR)
           				+"st");
       }
       return props;
   }
    
   protected void setupHelp(){
        props2Help = new TreeMap();
        props2Help.put(SPHINX2DIR,"directory containing the local installation of Sphinx2");
        props2Help.put(STDIR,"directory containing all files used for training and labeling");
   }

    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
        
        System.out.println("Labelling the voice data");
        
        //get voicename and root dir name
        //get the root dir and the voicename
        String rootDirName = db.getProp(db.ROOTDIR);
        String voicename = db.getProp(db.VOICENAME);
        
        
        /* Sphinx2 variables */
        System.out.println("Calling Sphinx2 ...");
        String stdir = getProp(STDIR);
        //model directory
        String hmm = stdir+"/model_parameters/"+voicename+".s2models/";
        
        // the 'task' 
        String task= stdir+"/wav";
        
        //dictionary and silence-symbol files
        String dictfile = stdir+"/etc/"+voicename+".dic";
        String ndictfile = stdir+"/etc/"+voicename+".sil";

        //list of filenames
        String ctlfile = stdir+"/etc/"+voicename+".fileids";

        //the transcription file
        String tactlfn = stdir+"/etc/"+voicename+".align";
        
        //make lab-directory if it does not exist
        File stLabDir = new File(stdir+"/lab");
        if (!stLabDir.exists()){
            stLabDir.mkdir();
        }
        
        /* Run Sphinx2 */
   
        Runtime rtime = Runtime.getRuntime();        
        //get a shell
        Process process = rtime.exec("/bin/bash");
        //get an output stream to write to the shell
        PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(process.getOutputStream()));
        //go to voice directory
        pw.print("cd "+rootDirName+"\n");
        pw.flush();
        //call Sphinx2 and exit
        
        pw.print("( "+getProp(SPHINX2DIR)+"build/bin/sphinx2-batch -adcin TRUE -adcext wav "
                +"-ctlfn "+ctlfile+" -tactlfn "+tactlfn+" -ctloffset 0"
                +"-ctlcount 100000000 -datadir wav -agcmax FALSE "
                +"-langwt 6.5 -fwdflatlw 8.5 -rescorelw 9.5 -ugwt 0.5"
                +"-fillpen 1e-10 -silpen 1e-10 -inspen 0.65 -topn 5"
                +"-topsenfrm 3 -topsenthresh -70000 -beam 2e-90 "
                +"-npbeam 2e-90 -lpbeam 2e-90 -lponlybeam 0.0005 "
                +"-nwbeam 0.0005 -fwdflat FALSE -fwdflatbeam 1e-08 "
                +"-fwdflatnwbeam 0.0003 -bestpath TRUE -kbdumpdir "+task+" "
                +"-dictfn "+dictfile+" -fdictfn "+ndictfile+" "
                +"-phnfn "+hmm+"/phone -mapfn "+hmm+"/map -hmmdir "+hmm+" "
                +"-hmmdirlist "+hmm+" -8bsen TRUE -sendumpfn "+hmm+"/sendump"
                +" -cbdir "+hmm+" -phonelabdir st/lab"    
                +"; exit)\n");
        pw.flush();
        pw.close();
        
        //collect the output
        //collect error messages
        StreamGobbler errorGobbler = new 
            StreamGobbler(process.getErrorStream(), "err");            
        
        //collect output messages
        StreamGobbler outputGobbler = new 
            StreamGobbler(process.getInputStream(), "out");
            
        //start the stream readers
        errorGobbler.start();
        outputGobbler.start();
        
        //shut down
        process.waitFor();
        process.exitValue();    
        System.out.println("... done.");
       
        /* Write the labels into lab directory */
        System.out.println("Exporting Labels ...");
        
        //lab destination directory
        String labDestDir = db.getProp(db.LABDIR);
        String labExtension = db.getProp(db.LABEXT);
        //used to prune the times to 5 positions behind .	
        DecimalFormat df = new DecimalFormat( "0.00000" );
        String line;
        //go through original lab files
        File[] labFiles = stLabDir.listFiles();
        for (int i=0;i<labFiles.length;i++){
            File nextFile = labFiles[i];
            System.out.println(nextFile.getName());
            
            //open original lab file
            BufferedReader labIn = new BufferedReader(
                    new FileReader(nextFile));
            
            //open destination lab file
            PrintWriter labOut = new PrintWriter(
                    new FileWriter(new File(labDestDir
                            +nextFile.getName())));
            
            String pauseString = null;
           
            //go through original lab file 
            while ((line = labIn.readLine()) != null){
                if (line.startsWith("#")){
                   //copy the line to destination lab file
                    labOut.println(line);
                } else {
                    //tokenize the line
                    StringTokenizer tok = new StringTokenizer(line); 
                    
                    //first token is time
                    double time = Float.parseFloat(tok.nextToken());
                    //add 0.012
                    //TODO: find out why we are adding 0.012
                    time += 0.012;
                    //prune time to 5 positions behind the dot
                    String timeString = df.format(time);
                    
                    //next token is some number
                    String mysteriousNumber = tok.nextToken();
                   
                    //next token is the phone
                    String phone = tok.nextToken();
                    
                    if (phone.equals("SIL")){
                        //replace silence symbol
                        phone = "_";
                        //store the pause in pause string; to be written later
                        //(this has the effect that if two pauses follow 
                        //each other, only the last one is printed)
                        pauseString = timeString+" "+mysteriousNumber+" "+phone;
                   } else {
                       if (pauseString != null){
                           //there is still a pause to print
                           labOut.println(pauseString);
                           //remove the pause
                           pauseString = null;
                       }
                        //cut off the stuff behind the phone
                        phone = phone.substring(0,phone.indexOf("("));
                        //convert phone back to SAMPA
                        phone = convertPhone(phone);
                        labOut.println(timeString+" "+mysteriousNumber+" "+phone);
                    }
                    
                }
            }
        
            if (pauseString != null){
                //print last pause
                labOut.println(pauseString);
            }
            //close files     
            labIn.close();
            labOut.flush();
            labOut.close();
        }
        System.out.println("... done.");
        System.out.println("All done!");
        
        return true;
    }
    
    /**
     * Convert the given phone from Sphinx-readable format
     * back to SAMPA
     * 
     * @param phone the phone
     * @return the converted phone
     */
     private String convertPhone(String phone){
         boolean uppercase = false;
         char[] phoneChars = phone.toCharArray();
         StringBuffer convertedPhone = new StringBuffer();
         for (int i=0;i<phoneChars.length;i++){
             char phoneChar = phoneChars[i];
             if (Character.isLetter(phoneChar)){
                 if (uppercase){
                     //character originally was uppercase
                     //append the phone as it is
                     convertedPhone.append(phoneChar);
                     uppercase = false;
                 } else {
                     //character originally was lowercase
                     //convert back to lowercase
                     convertedPhone.append(Character.toLowerCase(phoneChar));
                 }
             } else {
                 if (phoneChar == '*'){
                     //next letter was uppercase, set uppercase to true
                     uppercase = true;
                 } else {
                     //just append other non-letter signs
                     convertedPhone.append(phoneChar);
                 }
             }
         }
         return convertedPhone.toString();
     }
     
   
    
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress()
    {
        return -1;
    }

}