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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.SortedMap;
import java.util.TreeMap;

public class HMMVoiceDataPreparation extends VoiceImportComponent{
    
    private DatabaseLayout db;
    private String name = "HMMVoiceDataPreparation";
    
    /** Tree files and TreeSet object */
    public final String ADAPTSCRIPTS = name+".adaptScripts";
    public final String RAW2WAVCOMMAND = name+".raw2wavCommand";
    public final String WAV2RAWCOMMAND = name+".wav2rawCommand";
    public final String UTT2TRANSCOMMAND = name+".utt2transCommand";
    
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
   
           props.put(ADAPTSCRIPTS, "false");
           props.put(RAW2WAVCOMMAND, rootdir+"data/scripts/raw2wav.sh");
           props.put(WAV2RAWCOMMAND, rootdir+"data/scripts/wav2raw.sh");
           props.put(UTT2TRANSCOMMAND, rootdir+"data/scripts/utt2trans.sh");
       }
       return props;
       }
    
    protected void setupHelp(){
        props2Help = new TreeMap<String,String>();
        
        props2Help.put(ADAPTSCRIPTS, "ADAPTSCRIPTS=false: speaker dependent scripts, ADAPTSCRIPTS=true:  speaker adaptation/adaptive scripts.  ");
        props2Help.put(RAW2WAVCOMMAND, "raw2wav command");
        props2Help.put(WAV2RAWCOMMAND, "wav2raw command");
        props2Help.put(UTT2TRANSCOMMAND, "utt2trans command");
        
    }

    
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
        
        System.out.println("\nChecking directories and files for running HTS training scripts...");
        
        String filedir = db.getProp(db.ROOTDIR);
        String adaptScripts = getProp(ADAPTSCRIPTS);
        String cmdLine;
        System.out.println("adaptScripts = " + adaptScripts);
       
       File dirWav  = new File("wav");
       File dirText = new File("text");
       File dirRaw  = new File("data/raw");
       File dirUtt  = new File("data/utts");
       
       /* Check if the wav directory exist and have files */
       /* if wav/* does not exist but data/raw/* exist then can be converted and copied from raw */
       if(adaptScripts.contentEquals("false") ) {
       if( ( !dirWav.exists() || dirWav.list().length == 0 ) && (dirRaw.exists() && dirRaw.list().length > 0 ) ){
         if(!dirWav.exists())
           dirWav.mkdir();
         /* set the script as executable */
         cmdLine = "chmod +x " + getProp(RAW2WAVCOMMAND);
         launchProc(cmdLine, "raw2wav", filedir);
         cmdLine = getProp(RAW2WAVCOMMAND) + " data/raw wav" ;
         launchProc(cmdLine, "raw2wav", filedir);
       } else {
           if( !dirWav.exists() || dirWav.list().length == 0 || !dirRaw.exists() || dirRaw.list().length == 0 ){ 
            System.out.println("Problem with wav and data/raw directories: wav files and raw files do not exist.");
          } else
            System.out.println("\nwav directory exists and contains files.");    
       } 
       } else {  /* adaptScripts = true */
           if( ( !dirWav.exists() || dirWav.list().length == 0 ) && (dirRaw.exists() && dirRaw.list().length > 0 ) ){
             if(!dirWav.exists())
               dirWav.mkdir();
             /* set the script as executable */
             cmdLine = "chmod +x " + getProp(RAW2WAVCOMMAND);
             launchProc(cmdLine, "raw2wav", filedir);
             
             String[] dirRawList = dirRaw.list();
             for (int i=0; (i<dirRawList.length); i++) {
               System.out.println("RAW DIR: " + dirRawList[i] );
               File tmp = new File("wav/" + dirRawList[i]);
               tmp.mkdir();
               cmdLine = getProp(RAW2WAVCOMMAND) + " data/raw/" + dirRawList[i] + " wav/" + dirRawList[i];
               launchProc(cmdLine, "raw2wav", filedir);
             }             
           } else {
               if( !dirWav.exists() || dirWav.list().length == 0 || !dirRaw.exists() || dirRaw.list().length == 0 ){ 
                   System.out.println("Problem with wav and data/raw directories: wav directories and raw directories do not exist.");
                 } else
                   System.out.println("\nwav directory exists and contains directories.");    
              }     
       }
       
       /* check if data/raw directory exist and have files */
       /* if data/raw/* does not exist but wav/* exist then can be converted and copied from wav */
       if(adaptScripts.contentEquals("false") ) {
       if((!dirRaw.exists() || dirRaw.list().length == 0) && (dirWav.exists() && dirWav.list().length > 0 ) ){
         if(!dirRaw.exists())
           dirRaw.mkdir();
         /* set the script as executable */
         cmdLine = "chmod +x " + getProp(WAV2RAWCOMMAND);
         launchProc(cmdLine, "wav2raw", filedir);
         cmdLine = getProp(WAV2RAWCOMMAND) + " wav data/raw" ;
         launchProc(cmdLine, "wav2raw", filedir);
       } else {
           if( !dirWav.exists() || dirWav.list().length == 0 || !dirRaw.exists() || dirRaw.list().length == 0 ){
             System.out.println("Problem with wav and data/raw directories: wav files and raw files do not exist.");
           } else
               System.out.println("\ndata/raw directory exists and contains files.");
        }
       } else {  /* adaptScripts = true */
           if((!dirRaw.exists() || dirRaw.list().length == 0) && (dirWav.exists() && dirWav.list().length > 0 ) ){
             if(!dirRaw.exists())
               dirRaw.mkdir();
             /* set the script as executable */
             cmdLine = "chmod +x " + getProp(WAV2RAWCOMMAND);
             launchProc(cmdLine, "wav2raw", filedir);
                        
             String[] dirWavList = dirWav.list();
             for (int i=0; (i<dirWavList.length); i++) {
             System.out.println("WAV DIR: " + dirWavList[i] );
             File tmp = new File("data/raw/" + dirWavList[i]);
             tmp.mkdir();
             cmdLine = getProp(WAV2RAWCOMMAND) + " wav/" + dirWavList[i] + " data/raw/" + dirWavList[i];
             launchProc(cmdLine, "raw2wav", filedir);
           }
         } else {
             if( !dirWav.exists() || dirWav.list().length == 0 || !dirRaw.exists() || dirRaw.list().length == 0 ){
                 System.out.println("Problem with wav and data/raw directories: wav directories and raw directories do not exist.");
               } else
                   System.out.println("\ndata/raw directory exists and contains directories.");
            }
       }
       
       /* Check if text directory exist and have files */
       if(adaptScripts.contentEquals("false") ) {
       if((!dirText.exists() || dirText.list().length == 0) && (dirUtt.exists() && dirUtt.list().length > 0 ) ){
         if(!dirText.exists())
           dirText.mkdir();
         /* set the script as executable */
         cmdLine = "chmod +x " + getProp(UTT2TRANSCOMMAND);
         launchProc(cmdLine, "utt2trans", filedir);
         cmdLine = getProp(UTT2TRANSCOMMAND) + " data/utts text" ;
         launchProc(cmdLine, "utt2trans", filedir);      
       } else {
           if( (!dirText.exists() || dirText.list().length == 0) && ( !dirUtt.exists() || dirUtt.list().length == 0 ) ){
             System.out.println("Problem with transcription directories text or data/utts (Festival format): utts files and text files do not exist.");
             System.out.println(" the transcriptions in the directory text will be used to generate the phonelab directory, if there are no data/utts files" +
                    "(in Festival format), please provide the transcriptions of the files you are going to use for trainning.");
           } else
               System.out.println("\ntext directory exists and contains files.");
        }
       } else {  /* adaptScripts = true */
           if((!dirText.exists() || dirText.list().length == 0) && (dirUtt.exists() && dirUtt.list().length > 0 ) ){
               if(!dirText.exists())
                 dirText.mkdir();
               /* set the script as executable */
               cmdLine = "chmod +x " + getProp(UTT2TRANSCOMMAND);
               launchProc(cmdLine, "utt2trans", filedir);
               
               String[] dirUttList = dirUtt.list();
               for (int i=0; (i<dirUttList.length); i++) {
                 System.out.println("UTT DIR: " + dirUttList[i] );
                 File tmp = new File("text/" + dirUttList[i]);
                 tmp.mkdir();
                 cmdLine = getProp(UTT2TRANSCOMMAND) + " data/utts/" + dirUttList[i] + " text/" + dirUttList[i];
                 launchProc(cmdLine, "utt2trans", filedir);
               }
           } else {
               if( (!dirText.exists() || dirText.list().length == 0) && ( !dirUtt.exists() || dirUtt.list().length == 0 ) ){
                   System.out.println("Problem with transcription directories text or data/utts (Festival format): utts files and text files do not exist.");
                   System.out.println(" the transcriptions in the directory text will be used to generate the phonelab directory, if there are no data/utts files" +
                          "(in Festival format), please provide the transcriptions of the files you are going to use for trainning.");
                 } else
                     System.out.println("\ntext directory exists and contains directories.");
              }
       }
        
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

