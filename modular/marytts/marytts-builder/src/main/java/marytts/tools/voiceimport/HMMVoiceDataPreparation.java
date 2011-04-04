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
import marytts.util.io.General;



/**
 * This program was modified from previous version to:
 * 1. copy $MARY_BASE/lib/external/hts directory to the voice building directory
 * 2. check again that all the external necessary programs are installed.
 * 3. check as before that wav and text directories exist and make convertions:
 *    voiceDir/wav -> voiceDir/hts/data/raw 
 *    userProvidedDir/utts (festival format) -> voiceDir/text (one file per transcription)
 *    userProvidedDir/raw move to voiceDir/hts/data/raw
 *
 * @author marcela
 *
 */
public class HMMVoiceDataPreparation extends VoiceImportComponent{
    private DatabaseLayout db;
    private String name = "HMMVoiceDataPreparation";
    public final String USERRAWDIR = name + ".userRawDirectory";
    public final String USERUTTDIR = name + ".userUttDirectory";        
    private String marybase; 
    private String voiceDir;
    private String soxPath;
    private String sep;
    private String dataDir;
    private String scriptsDir;


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
           props.put(USERRAWDIR, "");
           props.put(USERUTTDIR, "");           
       }
       return props;
       }
    
    protected void setupHelp(){
        props2Help = new TreeMap<String,String>();                
        props2Help.put(USERRAWDIR, "raw files directory, user provided directory (default empty)");
        props2Help.put(USERUTTDIR, "utterance directory (transcriptions in festival format), user provided directory (default empty)");        
    }

       
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
       boolean raw = false;
       boolean text = false;
       marybase   = db.getProp(db.MARYBASE); 
       voiceDir   = db.getProp(db.ROOTDIR);
       soxPath    = db.getExternal(db.SOXPATH);
       sep        = System.getProperty("file.separator");
       dataDir    = voiceDir + "hts" + sep + "data" + sep;
       scriptsDir = dataDir + "scripts" + sep;
 
       // 1. copy from $MARY_TTS/lib/external/hts directory in the voice building directory
       String sourceFolder = marybase + sep + "lib" + sep + "external" + sep + "hts";
       String htsFolder = voiceDir + sep + "hts";
       FileUtils.copyFolderRecursive(sourceFolder, htsFolder, false);
       
       // 2. check again that all the external necessary programs are installed.
       System.out.println("\nHMMVoiceDataPreparation:\nChecking paths of external programs");
       if( !checkExternalPaths() )
           return false;
       
       // 3. check as before that wav, raw and text directories exist and are in the correct place     
       System.out.println("\nChecking directories and files for running HTS training scripts...");
                            
       // default locations of directories:
       String wavDirName  = voiceDir + "wav";           
       String textDirName = voiceDir + "text";
       String rawDirName  = dataDir + "raw";
       
       // 3.1 check raw and wav files:
       String userRawDirName = getProp(USERRAWDIR);  
       if( existWithFiles(rawDirName) ) {
         raw = true;
       } else {
         // check if the user has provided a raw directory  
         if( !userRawDirName.equals("") ) {
           File userRawDir  = new File(userRawDirName);
         
           // check if user provided raw dir contains files
           if( existWithFiles(userRawDirName) ) {
             // copy the user provided raw directory to hts/data/raw/
             System.out.println("Copying files from: " + userRawDirName + "  to: " + rawDirName);
             FileUtils.copyFolder(userRawDirName, rawDirName);
           
             // the raw files should be the same as in wav file, if wav file empty convert from raw --> wav
             if( !existWithFiles(wavDirName))
               convertRaw2Wav(rawDirName, wavDirName);    
            
             raw = true;
           } else
             System.out.println("User provided raw directory: " + userRawDirName + " does not exist or does not contain files\n");
         }
         // if we still do not have raw files...
         // then there must be a wav directory, check that it contains files, if so convert wav --> raw
         if(!raw) {
           System.out.println("Checking if " + wavDirName + " contains files");
           if( existWithFiles(wavDirName) ){
             convertWav2Raw(wavDirName, rawDirName);            
             raw = true;
           } else {
             System.out.println("There are no wav files in " + wavDirName);  
           }
         }
       } 
       
       // 3.2 check text files:
       if( existWithFiles(textDirName) ) {
         text = true;
       } else {   
         // check if the user has provided a utterance directory
         String userUttDirName = getProp(USERUTTDIR);
         if( !userUttDirName.equals("") ) {
           // check if user provided utt dir contains files
           if( existWithFiles(userUttDirName) ) {
             // convert utt --> text (transcriptions festival format --> MARY format)
             convertUtt2Text(userUttDirName, textDirName);
             text = true;
           } else
             System.out.println("User provided utterance directory: " + userUttDirName + " does not exist or does not contain files\n");
         } else                
           System.out.println("\nThere are no text files in " + textDirName);  
       }
       
       if( raw && text ){
         System.out.println("\nHMMVoiceDataPreparation finished:\n" +
                "HTS scripts copied in current voice building directory\n" +
                "wav/raw and text directories in place.");  
         return true;
       }
       else
         return false;
       
    }
    
    
    /**
     * Check the paths of all the necessary external programs
     * @return true if all the paths are defined
     */
    private boolean checkExternalPaths() throws Exception{
        
        boolean result = true;
        
        if ( db.getExternal(db.AWKPATH) == null ){
          System.out.println("  *Missing path for awk"); 
          result = false;
        }
        if (db.getExternal(db.PERLPATH) == null){
            System.out.println("  *Missing path for perl"); 
            result = false;
        }        
        if (db.getExternal(db.BCPATH) == null){
            System.out.println("  *Missing path for bc"); 
            result = false;
        }
        if (db.getExternal(db.TCLPATH) == null){
            System.out.println("  *Missing path for tclsh"); 
            result = false;
        }
        if (db.getExternal(db.SOXPATH) == null){
            System.out.println("  *Missing path for sox"); 
            result = false;
        }
        if(db.getExternal(db.HTSPATH) == null){
            System.out.println("  *Missing path for hts/htk"); 
            result = false;
        }
        if(db.getExternal(db.HTSENGINEPATH) == null){
            System.out.println("  *Missing path for hts_engine"); 
            result = false;
        }
        if(db.getExternal(db.SPTKPATH) == null){
            System.out.println("  *Missing path for sptk"); 
            result = false;
        }
        if(db.getExternal(db.EHMMPATH) == null){
            System.out.println("  *Missing path for ehmm"); 
            result = false;
        }
       
        if(!result)
          System.out.println("Please run MARYBASE/lib/external/check_install_external_programs.sh and check/install the missing programs");
        else
          System.out.println("Paths for all external programs are defined.");  
        
        return result;
    }
    
    /**
     * Checks if the directory exist and has files
     * @param dir
     * @return
     */
    private boolean existWithFiles(String dirName) {
      File dir = new File(dirName);
      if( dir.exists() && dir.list().length>0 )
        return true;
      else
        return false;
    }

    private void convertRaw2Wav(String rawDirName, String wavDirName) {
        String cmdLine;
        String raw2wavCmd   = scriptsDir + "raw2wav.sh";
        System.out.println("Converting raw files to wav from: " + rawDirName + "  to: " + wavDirName);
        File wavDir = new File(wavDirName);
        if(!wavDir.exists())
            wavDir.mkdir();
        cmdLine = "chmod +x " + raw2wavCmd;
        General.launchProc(cmdLine, "raw2wav", voiceDir);
        cmdLine = raw2wavCmd + " " + soxPath + " " + rawDirName + " " + wavDirName ;
        General.launchProc(cmdLine, "raw2wav", voiceDir);  
    }
    
    private void convertWav2Raw(String wavDirName, String rawDirName) {
        String cmdLine;
        String wav2rawCmd   = scriptsDir + "wav2raw.sh";
        System.out.println("Converting wav files to raw from: " + wavDirName + "  to: " + rawDirName);
        File rawDir  = new File(rawDirName);
        if(!rawDir.exists())
          rawDir.mkdir();  
        cmdLine = "chmod +x " + wav2rawCmd;
        General.launchProc(cmdLine, "wav2raw", voiceDir);
        cmdLine = wav2rawCmd + " " + soxPath + " " + wavDirName + " " + rawDirName ;
        General.launchProc(cmdLine, "wav2raw", voiceDir);  
    }
    
    private void convertUtt2Text(String userUttDirName, String textDirName) {
        String cmdLine;
        String utt2transCmd = scriptsDir + "utt2trans.sh";  // festival to mary format
        System.out.println("\nConverting transcription files (festival format) to text from: " + userUttDirName + "  to: " + textDirName);
        File textDir = new File(textDirName);
        if(!textDir.exists())
          textDir.mkdir();
        cmdLine = "chmod +x " + utt2transCmd;
        General.launchProc(cmdLine, "utt2trans", voiceDir);
        cmdLine = utt2transCmd + " " + userUttDirName + " " + textDirName;
        General.launchProc(cmdLine, "utt2trans", voiceDir);
 
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

