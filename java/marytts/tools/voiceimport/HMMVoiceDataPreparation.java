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
package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.SortedMap;
import java.util.TreeMap;
import marytts.util.io.FileUtils;



/**
 * This program will be modified from previous versions for:
 * 1. copy from $MARY_TTS/lib/external/HTS-demo_for_MARY-4.0.zip in the voice building directory
 * 2. unpack the zip file in the voice building directory
 * 3. check again that all the external necessary programs are installed.
 * 4. check as before that wav, raw and text directories exist and are in the correct place

 * @author marcela
 *
 */
public class HMMVoiceDataPreparation extends VoiceImportComponent{
    
    private DatabaseLayout db;
    private String name = "HMMVoiceDataPreparation";
    
    
    public final String ADAPTSCRIPTS = name + ".adaptScripts";
    public final String RAW2WAVCOMMAND = name + ".raw2wavCommand";
    public final String WAV2RAWCOMMAND = name + ".wav2rawCommand";
    public final String UTT2TRANSCOMMAND = name + ".utt2transCommand";
    public final String HTSDEMOFORMARY = name + ".HTSDemoForMARY";
    public final String EXTERNALPATHS = name + ".externalPathsFile";
    public final String UNZIPCOMMAND = name + ".unZipCommand";
    
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
           String marybase = db.getProp(db.MARYBASE); 
           props.put(ADAPTSCRIPTS, "false");
           props.put(RAW2WAVCOMMAND, rootdir   + "hts/data/scripts/raw2wav.sh");
           props.put(WAV2RAWCOMMAND, rootdir   + "hts/data/scripts/wav2raw.sh");
           props.put(UTT2TRANSCOMMAND, rootdir + "hts/data/scripts/utt2trans.sh");
           props.put(HTSDEMOFORMARY, marybase  + "lib/external/HTS-demo_for_MARY-4.0.zip");
           props.put(EXTERNALPATHS, rootdir  + "mary/externalPaths.txt");
           props.put(UNZIPCOMMAND, "/usr/bin/unzip");
       }
       return props;
       }
    
    protected void setupHelp(){
        props2Help = new TreeMap<String,String>();
        
        props2Help.put(ADAPTSCRIPTS, "ADAPTSCRIPTS=false: speaker dependent scripts, ADAPTSCRIPTS=true:  speaker adaptation/adaptive scripts.  ");
        props2Help.put(RAW2WAVCOMMAND, "raw2wav command");
        props2Help.put(WAV2RAWCOMMAND, "wav2raw command");
        props2Help.put(UTT2TRANSCOMMAND, "utt2trans command");
        props2Help.put(HTSDEMOFORMARY, "set of scripts for creating a HMM voice for MARY");
        props2Help.put(EXTERNALPATHS, "file that contains the paths of the external programs necessary to create a HMM voice, " +
                                      "it is generated by MARYBASE/lib/external/check_install_external_programs.sh " +
                                      "and located in VOICEDIR/mary/externalPaths.txt.");
        props2Help.put(UNZIPCOMMAND, "unzip command to extract files in HTS-demo_for_MARY-4.0.zip");
    }

    
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
       
       String marybase = db.getProp(db.MARYBASE); 
       String voicedir = db.getProp(db.ROOTDIR); 
        
       // 1. copy from $MARY_TTS/lib/external/HTS-demo_for_MARY-4.0.zip in the voice building directory       
       FileUtils.copy(getProp(HTSDEMOFORMARY), voicedir);
               
       // 2. unpack the zip file in the voice building directory
       String cmdLine = "cd " + voicedir + "\n" + getProp(UNZIPCOMMAND) + " -o " + FileUtils.getFileName(getProp(HTSDEMOFORMARY));
       General.launchBatchProc(cmdLine, "unzip", voicedir);
       cmdLine = "rm " + voicedir + FileUtils.getFileName(getProp(HTSDEMOFORMARY));
       General.launchProc(cmdLine, "rm", voicedir);
       
       // 3. check again that all the external necessary programs are installed.
       System.out.println("\nChecking paths of external programs");
       if( !checkExternalPaths(getProp(EXTERNALPATHS)) )
           return false;
       
       // 4. check as before that wav, raw and text directories exist and are in the correct place     
       System.out.println("\nChecking directories and files for running HTS training scripts...");
        
       
       String adaptScripts = getProp(ADAPTSCRIPTS);
       boolean rawCurrentDir = false;
       System.out.println("adaptScripts = " + adaptScripts);
              
       File dirWav  = new File(voicedir + "wav");
       File dirText = new File(voicedir + "text");
       
       //check first if dirRaw and dirUtt are in current directory or in data directory
       // it could be the case that the raw and utt are in the current directory
       String rawDirName = "hts/data/raw";
       String uttsDirName = "hts/data/utts";        
       File dirRaw  = new File(voicedir + rawDirName);
       File dirUtt  = new File(voicedir + uttsDirName);
       if( !dirRaw.exists() ) {
           // try then in current directory
           rawDirName = "raw";
           dirRaw  = new File(voicedir + rawDirName);
           rawCurrentDir = true;
       }
       if( !dirUtt.exists() ) {
           // try then in current directory
           uttsDirName = "utts";
           dirUtt  = new File(voicedir + uttsDirName);
       }
       
       
       /* Check if the wav directory exist and have files */
       /* if wav/* does not exist but hts/data/raw/* or raw/* exist then can be converted and copied from raw */
       if(adaptScripts.contentEquals("false") ) {
       if( ( !dirWav.exists() || dirWav.list().length == 0 ) && (dirRaw.exists() && dirRaw.list().length > 0 ) ){
         if(!dirWav.exists())
           dirWav.mkdir();
         /* set the script as executable */
         cmdLine = "chmod +x " + getProp(RAW2WAVCOMMAND);
         General.launchProc(cmdLine, "raw2wav", voicedir);
         cmdLine = getProp(RAW2WAVCOMMAND) + " " + voicedir + rawDirName + " " + voicedir + "wav" ;
         General.launchProc(cmdLine, "raw2wav", voicedir);        
       } else {
           if( (!dirWav.exists() || dirWav.list().length == 0) && (!dirRaw.exists() || dirRaw.list().length == 0 ) ){ 
            System.out.println("Problem with wav and " + rawDirName + " directories: wav files and raw files do not exist.");
            return false;
          } else
            System.out.println("\nwav directory exists and contains files.");    
       } 
       } else {  /* adaptScripts = true */
           if( ( !dirWav.exists() || dirWav.list().length == 0 ) && (dirRaw.exists() && dirRaw.list().length > 0 ) ){
             if(!dirWav.exists())
               dirWav.mkdir();
             /* set the script as executable */
             cmdLine = "chmod +x " + getProp(RAW2WAVCOMMAND);
             General.launchProc(cmdLine, "raw2wav", voicedir);
             
             String[] dirRawList = dirRaw.list();
             for (int i=0; (i<dirRawList.length); i++) {
               System.out.println("RAW DIR: " + dirRawList[i] );
               File tmp = new File("wav/" + dirRawList[i]);
               tmp.mkdir();
               cmdLine = getProp(RAW2WAVCOMMAND) + " " + voicedir + rawDirName + "/" + dirRawList[i] 
                                                 + " " + voicedir + "wav/" + dirRawList[i];
               General.launchProc(cmdLine, "raw2wav", voicedir);
               
             }             
           } else {
               if( (!dirWav.exists() || dirWav.list().length == 0 ) && (!dirRaw.exists() || dirRaw.list().length == 0 )){ 
                   System.out.println("Problem with wav and " + rawDirName + " directories: wav directories and raw directories do not exist.");
                   return false;
                 } else
                   System.out.println("\nwav directory exists and contains directories.");    
              }     
       }
       
       /* check if hts/data/raw or raw directory exist and have files */
       /* if hts/data/raw/* or raw/* does not exist but wav/* exist then can be converted and copied from wav */
       if(adaptScripts.contentEquals("false") ) {
       if((!dirRaw.exists() || dirRaw.list().length == 0) && (dirWav.exists() && dirWav.list().length > 0 ) ){
         // the raw dir should be in hts/data/raw
         File dirRawInData = new File(voicedir + "hts/data/raw");
         if(!dirRawInData.exists())
             dirRawInData.mkdir();
         /* set the script as executable */
         cmdLine = "chmod +x " + getProp(WAV2RAWCOMMAND);
         General.launchProc(cmdLine, "wav2raw", voicedir);
         cmdLine = getProp(WAV2RAWCOMMAND) + " " + voicedir + "wav " + voicedir + "hts/data/raw" ;
         General.launchProc(cmdLine, "wav2raw", voicedir);
       } else {
           if( (!dirWav.exists() || dirWav.list().length == 0 ) && (!dirRaw.exists() || dirRaw.list().length == 0 )){
             System.out.println("Problem with wav and " + rawDirName + " directories: wav files and raw files do not exist.");
             return false;
           } else
               System.out.println("\n" + rawDirName + " directory exists and contains files.");
        }
       } else {  /* adaptScripts = true */
           if((!dirRaw.exists() || dirRaw.list().length == 0) && (dirWav.exists() && dirWav.list().length > 0 ) ){
             // the raw dir should be in hts/data/raw
             File dirRawInData = new File(voicedir + "hts/data/raw");
             if(!dirRawInData.exists())
                 dirRawInData.mkdir();
             /* set the script as executable */
             cmdLine = "chmod +x " + getProp(WAV2RAWCOMMAND);
             General.launchProc(cmdLine, "wav2raw", voicedir);
                        
             String[] dirWavList = dirWav.list();
             for (int i=0; (i<dirWavList.length); i++) {
             System.out.println("WAV DIR: " + dirWavList[i] );
             File tmp = new File("hts/data/raw/" + dirWavList[i]);
             tmp.mkdir();
             cmdLine = getProp(WAV2RAWCOMMAND) + " " + voicedir + "wav/" + dirWavList[i] 
                                               + " " + voicedir + "hts/data/raw/" + dirWavList[i];
             General.launchProc(cmdLine, "raw2wav", voicedir);
           }
         } else {
             if( (!dirWav.exists() || dirWav.list().length == 0 ) && (!dirRaw.exists() || dirRaw.list().length == 0 )){
                 System.out.println("Problem with wav and hts/data/raw directories: wav directories and raw directories do not exist.");
                 return false;
               } else
                   System.out.println("\nhts/data/raw directory exists and contains directories.");
            }
       }
       
       /* Check if text directory exist and have files */
       if(adaptScripts.contentEquals("false") ) {
       if((!dirText.exists() || dirText.list().length == 0) && (dirUtt.exists() && dirUtt.list().length > 0 ) ){
         if(!dirText.exists())
           dirText.mkdir();
         /* set the script as executable */
         cmdLine = "chmod +x " + getProp(UTT2TRANSCOMMAND);
         General.launchProc(cmdLine, "utt2trans", voicedir);
         cmdLine = getProp(UTT2TRANSCOMMAND) + " " + voicedir + uttsDirName + " " + voicedir + "text" ;
         General.launchProc(cmdLine, "utt2trans", voicedir);      
       } else {
           if( (!dirText.exists() || dirText.list().length == 0) && ( !dirUtt.exists() || dirUtt.list().length == 0 ) ){
             System.out.println("Problem with transcription directories text or hts/data/utts (Festival format): utts files and text files do not exist.");
             System.out.println(" the transcriptions in the directory text will be used to generate the phonelab directory, if there are no hts/data/utts files" +
                    "(in Festival format), please provide the transcriptions of the files you are going to use for trainning.");
             return false;
           } else
               System.out.println("\ntext directory exists and contains files.");
        }
       } else {  /* adaptScripts = true */
           if((!dirText.exists() || dirText.list().length == 0) && (dirUtt.exists() && dirUtt.list().length > 0 ) ){
               if(!dirText.exists())
                 dirText.mkdir();
               /* set the script as executable */
               cmdLine = "chmod +x " + getProp(UTT2TRANSCOMMAND);
               General.launchProc(cmdLine, "utt2trans", voicedir);
               
               String[] dirUttList = dirUtt.list();
               for (int i=0; (i<dirUttList.length); i++) {
                 System.out.println("UTT DIR: " + dirUttList[i] );
                 File tmp = new File("text/" + dirUttList[i]);
                 tmp.mkdir();
                 cmdLine = getProp(UTT2TRANSCOMMAND) + " " + voicedir + uttsDirName + "/" + dirUttList[i] 
                                                     + " " + voicedir + "text/" + dirUttList[i];
                 General.launchProc(cmdLine, "utt2trans", voicedir);
               }
           } else {
               if( (!dirText.exists() || dirText.list().length == 0) && ( !dirUtt.exists() || dirUtt.list().length == 0 ) ){
                   System.out.println("Problem with transcription directories text or hts/data/utts (Festival format): utts files and text files do not exist.");
                   System.out.println(" the transcriptions in the directory text will be used to generate the phonelab directory, if there are no hts/data/utts files" +
                          "(in Festival format), please provide the transcriptions of the files you are going to use for trainning.");
                   return false;
                 } else
                     System.out.println("\ntext directory exists and contains directories.");
              }
       }
       
       /* we need the raw directory in hts/data/raw, so if if raw is in current directory move it to data */
       if(rawCurrentDir && dirRaw.exists() && dirRaw.list().length > 0  ){
           System.out.println("Moving raw directory to data directory");
           cmdLine = "mv " + voicedir + rawDirName + " " + voicedir + "hts/data/";
           General.launchProc(cmdLine, "move to data", voicedir);
       }
       
       return true;
       
    }
    
    /**
     * Check that the paths of all the necessary external programs are in the externalPaths file
     * @param externalPaths
     * @return true if all the paths are found
     */
    private boolean checkExternalPaths(String externalPaths) throws Exception{
        
        boolean result = true;
        boolean awk=false, perl=false, bc=false, tclsh=false, sox=false, htk=false, hts_engine=false, sptk=false, ehmm=false;
        BufferedReader inputStream = null;
        String line;
        String program[];
        try{
          inputStream = new BufferedReader(new FileReader(externalPaths));     
          while ((line = inputStream.readLine()) != null) {
             program = line.split(" ");              
             if(program[0].contentEquals("awk")){
               awk = true;  
               System.out.println("  " + program[0] + " : " + program[1]);
             } 
             else if (program[0].contentEquals("perl")){
               perl = true;
               System.out.println("  " + program[0] + " : " + program[1]);
             }
             else if (program[0].contentEquals("bc")){
               bc = true;  
               System.out.println("  " + program[0] + " : " + program[1]);
             }
             else if (program[0].contentEquals("tclsh")){
               tclsh = true;  
               System.out.println("  " + program[0] + " : " + program[1]);
             }
             else if (program[0].contentEquals("sox")){
               sox = true;  
               System.out.println("  " + program[0] + " : " + program[1]);
             }
             else if (program[0].contentEquals("htk")){
               htk = true;  
               System.out.println("  " + program[0] + " : " + program[1]);
             }
             else if (program[0].contentEquals("hts_engine")){
               hts_engine = true;
               System.out.println("  " + program[0] + " : " + program[1]);
             }
             else if (program[0].contentEquals("sptk")){
               sptk = true;   
               System.out.println("  " + program[0] + " : " + program[1]);
             }
             else if (program[0].contentEquals("ehmm")){
               ehmm = true;
               System.out.println("  " + program[0] + " : " + program[1]);
             }
          
          }
          inputStream.close();
        } catch (Exception e) {
            throw new FileNotFoundException("checkExternalPaths: " + e.getMessage());
        }  
        
        if (!awk){
          System.out.println("  Missing path for awk"); 
          result = false;
        }
        if (!perl){
            System.out.println("  Missing path for perl"); 
            result = false;
          }        
        if (!bc){
            System.out.println("  Missing path for bc"); 
            result = false;
          }
        
        if (!tclsh){
            System.out.println("  Missing path for tclsh"); 
            result = false;
          }
        if (!sox){
            System.out.println("  Missing path for sox"); 
            result = false;
          }
        if(!htk){
            System.out.println("  Missing path for htk"); 
            result = false;
          }
        if(!hts_engine){
            System.out.println("  Missing path for hts_engine"); 
            result = false;
          }
        if(!sptk){
            System.out.println("  Missing path for sptk"); 
            result = false;
          }
        if(!ehmm){
            System.out.println("  Missing path for ehmm"); 
            result = false;
          }
       
        if(!result)
          System.out.println("Please run again MARYBASE/lib/external/check_install_external_programs.sh and install the missing programs");  
        
        return result;
    }
    
    
    
    
    
    
    
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress(){
        return -1;
    }
    
    
    
    
    
}

