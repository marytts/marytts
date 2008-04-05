/**   
*           The HMM-Based Speech Synthesis System (HTS)             
*                       HTS Working Group                           
*                                                                   
*                  Department of Computer Science                   
*                  Nagoya Institute of Technology                   
*                               and                                 
*   Interdisciplinary Graduate School of Science and Engineering    
*                  Tokyo Institute of Technology                    
*                                                                   
*                Portions Copyright (c) 2001-2006                       
*                       All Rights Reserved.
*                         
*              Portions Copyright 2000-2007 DFKI GmbH.
*                      All Rights Reserved.                  
*                                                                   
*  Permission is hereby granted, free of charge, to use and         
*  distribute this software and its documentation without           
*  restriction, including without limitation the rights to use,     
*  copy, modify, merge, publish, distribute, sublicense, and/or     
*  sell copies of this work, and to permit persons to whom this     
*  work is furnished to do so, subject to the following conditions: 
*                                                                   
*    1. The source code must retain the above copyright notice,     
*       this list of conditions and the following disclaimer.       
*                                                                   
*    2. Any modifications to the source code must be clearly        
*       marked as such.                                             
*                                                                   
*    3. Redistributions in binary form must reproduce the above     
*       copyright notice, this list of conditions and the           
*       following disclaimer in the documentation and/or other      
*       materials provided with the distribution.  Otherwise, one   
*       must contact the HTS working group.                         
*                                                                   
*  NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF TECHNOLOGY,   
*  HTS WORKING GROUP, AND THE CONTRIBUTORS TO THIS WORK DISCLAIM    
*  ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL       
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT   
*  SHALL NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF         
*  TECHNOLOGY, HTS WORKING GROUP, NOR THE CONTRIBUTORS BE LIABLE    
*  FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY        
*  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,  
*  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTUOUS   
*  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR          
*  PERFORMANCE OF THIS SOFTWARE.                                    
*                                                                   
*/


package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;



public class HMMVoiceConfigure extends VoiceImportComponent{
    
    private DatabaseLayout db;
    private String name = "HMMVoiceConfigure";
    
    /** Tree files and TreeSet object */
    public final String CONFIGUREFILE = name+".configureFile";
    public final String HTSPATH = name+".htsPath";
    public final String SPTKPATH = name+".sptkPath";
    public final String TCLPATH = name+".tclPath";
    public final String SOXPATH = name+".soxPath";
    public final String FEATURELIST = name+".featureList";
    public final String VOICELANG = name+".voiceLang";
    public final String SPEAKER = name+".speaker";
    public final String DATASET = name+".dataSet";
    public final String LOWERF0 = name+".lowerF0";
    public final String UPPERF0 = name+".upperF0";
    public final String NUMTESTFILES = name+".numTestFiles";
    
    public String getName(){
        return name;
    }
    
    /**
     * Get the map of properties2values
     * containing the default values
     * @return map of props2values
     */
    public SortedMap<String,String> getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap<String,String>();
           String rootdir = db.getProp(db.ROOTDIR);
           
           props.put(CONFIGUREFILE, rootdir+"configure");
           props.put(HTSPATH, "/project/mary/marcela/sw/HTS_2.0.1/htk/bin");
           props.put(SPTKPATH, "/project/mary/marcela/sw/SPTK-3.1/bin");
           props.put(TCLPATH, "/opt/ActiveTcl-8.4/bin");
           props.put(SOXPATH, "/usr/bin");
           props.put(FEATURELIST, rootdir+"data/feature_list_en.pl");
           props.put(VOICELANG, "en");
           props.put(SPEAKER, "slt");
           props.put(DATASET, "cmu_us_arctic");
           props.put(LOWERF0, "80");
           props.put(UPPERF0, "350");
           props.put(NUMTESTFILES, "10");

       }
       return props;
       }
    
    protected void setupHelp(){
        props2Help = new TreeMap<String,String>();
        
        props2Help.put(CONFIGUREFILE, "");
        props2Help.put(HTSPATH, "");
        props2Help.put(SPTKPATH, "");
        props2Help.put(TCLPATH, "");
        props2Help.put(SOXPATH, "");
        props2Help.put(FEATURELIST, "Mary context features file (default='data/feature_list.pl')");
        props2Help.put(VOICELANG,   "voice language (default='en')");
        props2Help.put(SPEAKER,     "speaker name (default=slt)");
        props2Help.put(DATASET,     "dataset (default=cmu_us_arctic)");
        props2Help.put(LOWERF0,     "Lower limit for F0 extraction in Hz (default=80)");
        props2Help.put(UPPERF0,     "Upper limit for F0 extraction in Hz (default=350)");
        props2Help.put(NUMTESTFILES, "Number of test files used for testing, these are copied from phonefeatures set.");
        
    }

    
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
        
        System.out.println("\nChecking directories and files for running HTS training scripts...");
        
        String filedir = db.getProp(db.ROOTDIR);
        String cmdLine;
        boolean speech_transcriptions = true;
 
       
       File dirWav  = new File("wav");
       File dirText = new File("text");
       File dirRaw  = new File("data/raw");
       File dirUtt  = new File("data/utts");
       
       /* Check if wav directory exist and have files */

       if( !dirWav.exists() || dirWav.list().length == 0 || !dirRaw.exists() || dirRaw.list().length == 0 ){ 
         System.out.println("Problem with wav and data/raw directories: wav files and raw files do not exist.");
         speech_transcriptions = false;
       }  
       
       /* check if data/raw directory exist and have files */
       if( !dirWav.exists() || dirWav.list().length == 0 || !dirRaw.exists() || dirRaw.list().length == 0 ){
          System.out.println("Problem with wav and data/raw directories: wav files and raw files do not exist.");
          speech_transcriptions = false;
       } 
       
       /* Check if text directory exist and have files */
       if( !dirText.exists() || dirText.list().length == 0  || !dirUtt.exists() || dirUtt.list().length == 0  ){
         System.out.println("Problem with transcription directories text or data/utts (Festival format): utts files and text files do not exist.");
         System.out.println(" the transcriptions in the directory text will be used to generate the phonelab directory, if there are no data/utts files" +
                   "(in Festival format), please provide the transcriptions of the files you are going to use for trainning.");
         speech_transcriptions = false;
       } 
       
       
       if(speech_transcriptions){
           
       File dirFea = new File("phonefeatures");
       File dirLab = new File("phonelab");
       if(dirFea.exists() && dirFea.list().length > 0 && dirLab.exists() && dirLab.list().length > 0 ){ 
        System.out.println("\nphonefeatures directory exists and contains files.");  
           
        System.out.println("\nphonelab directory exists and contains files.");
        /* Create a phonefeatures/gen directory and copy there some examples of .pfeats
           files for testing the synthesis procedure once the models have been trained.*/
       
       File dirGen = new File("phonefeatures/gen");
       if(!dirGen.exists()){
         System.out.println("\nCreating a phonefeatures/gen directory, copying some .pfeats examples for testing");  
         dirGen.mkdir();
       }
       if(dirGen.list().length == 0){
         int numFiles = Integer.parseInt(getProp(NUMTESTFILES));
         int n=0;
        
         String[] feaFiles = dirFea.list();
         if (feaFiles.length > 0 ) {
           for (int i=0; (i<numFiles); i++) {
             cmdLine = "cp phonefeatures/" + feaFiles[i] + " phonefeatures/gen/";  
             launchProc(cmdLine, "file copy", filedir);
           }
         }
       } else
         System.out.println("\nDirectory phonefeatures/gen already exist and has some .pfeats examples for testing");   
        
       /* Create symbolic links for the phonefeatures and phonelab */
       File link = new File("data/phonefeatures");
       if (!link.exists()){
         System.out.println("\nCreating symbolic link for phonefeatures in data/: ");
         cmdLine = "ln -s " + filedir + "phonefeatures " + filedir + "data/phonefeatures";
         launchProc(cmdLine, "creating symbolic links", filedir);
       } else
         System.out.println("\nSymbolic link data/phonefeatures already exist."); 
       link = new File("data/phonelab");
       if (!link.exists()){
         System.out.println("\nCreating symbolic link for phonelab in data/: ");
         cmdLine = "ln -s " + filedir + "phonelab " + filedir + "data/phonelab";        
         launchProc(cmdLine, "creating symbolic links", filedir);
       } else
          System.out.println("\nSymbolic link data/phonelab already exist.\n");
       
       
       /* if previous files and directories exist then run configure */
       System.out.println("Running make configure: ");
       cmdLine = getProp(CONFIGUREFILE) +
       " --with-tcl-search-path=" + getProp(TCLPATH) +
       " --with-sptk-search-path=" + getProp(SPTKPATH) +
       " --with-hts-search-path=" + getProp(HTSPATH) +
       " --with-sox-search-path=" + getProp(SOXPATH) +
       " SPEAKER=" + getProp(SPEAKER) +
       " DATASET=" + getProp(DATASET) +
       " VOICELANG=" + getProp(VOICELANG) +
       " FEATURELIST=" + getProp(FEATURELIST); 
       launchProc(cmdLine, "Configure", filedir);
       
        
       } else
         System.out.println("Problems with directories phonefeatures or phonelab, they do not exist or they are empty.");  
       
       } else /* if speech and transcriptions exist */
         System.out.println("Problems with directories wav, text or data/raw, they do not exist or they are empty.");
       
       return true;
       
    }
    
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress(){
        return -1;
    }
    

   
    /**
     * A general process launcher for the various tasks
     * (copied from ESTCaller.java)
     * @param cmdLine the command line to be launched.
     * @param task a task tag for error messages, such as "Pitchmarks" or "LPC".
     * @param the basename of the file currently processed, for error messages.
     */
    private void launchProc( String cmdLine, String task, String baseName ) {
        
        Process proc = null;
        BufferedReader procStdout = null;
        String line = null;
        System.out.println("Running: "+ cmdLine);
        // String[] cmd = null; // Java 5.0 compliant code
        
        try {
            /* Java 5.0 compliant code below. */
            /* Hook the command line to the process builder: */
            /* cmd = cmdLine.split( " " );
            pb.command( cmd ); /*
            /* Launch the process: */
            /*proc = pb.start(); */
            
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
                throw new RuntimeException( task + " computation failed on file [" + baseName + "]!\n"
                        + "Command line was: [" + cmdLine + "]." );
            }
        }
        catch ( IOException e ) {
            throw new RuntimeException( task + " computation provoked an IOException on file [" + baseName + "].", e );
        }
        catch ( InterruptedException e ) {
            throw new RuntimeException( task + " computation interrupted on file [" + baseName + "].", e );
        }
        
    }    


    
}
