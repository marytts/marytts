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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.SortedMap;
import java.util.TreeMap;



public class HMMVoiceConfigure extends VoiceImportComponent{
    
    private DatabaseLayout db;
    private String name = "HMMVoiceConfigure";
    
    /** Tree files and TreeSet object */
    public final String CONFIGUREFILE = name+".configureFile";
    public final String SPEAKER       = name+".speaker";
    public final String DATASET       = name+".dataSet";
    public final String LOWERF0       = name+".lowerF0";
    public final String UPPERF0       = name+".upperF0";
    public final String NUMTESTFILES  = name+".numTestFiles";
    
    public final String VER           = name+".version";
    public final String QNUM          = name+".qestionsNum";
    public final String FRAMELEN      = name+".frameLen";
    public final String FRAMESHIFT    = name+".frameShift";
    public final String WINDOWTYPE    = name+".windowType";
    public final String NORMALIZE     = name+".normalize";
    public final String FFTLEN        = name+".fftLen";
    public final String FREQWARP      = name+".freqWarp";
    public final String GAMMA         = name+".gamma";
    public final String MGCLSP        = name+".mgcLSP";
    public final String MGCORDER      = name+".mgcOrder";
    public final String STRORDER      = name+".strOrder";
    public final String LNGAIN        = name+".lnGain";
    public final String PSTFILTER     = name+".pstFilter";
    public final String IMPLEN        = name+".impulseLen";
    public final String SAMPFREQ      = name+".sampfreq";
    public final String NMGCWIN       = name+".numMgcWin";
    public final String NSTRWIN       = name+".numStrWin";
    public final String NLF0WIN       = name+".numLf0Win";
    public final String NSTATE        = name+".numState";
    public final String NITER         = name+".numIterations";
    public final String WFLOOR        = name+".weightFloor";
     
    
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
           
           props.put(CONFIGUREFILE, rootdir+"hts/configure");           
           props.put(SPEAKER,       "slt");
           props.put(DATASET,       "cmu_us_arctic");
           props.put(LOWERF0,       "40");
           props.put(UPPERF0,       "350");
           props.put(NUMTESTFILES,  "10");
           
           props.put(VER,         "1");
           props.put(QNUM,        "001");
           props.put(FRAMELEN,    "400");
           props.put(FRAMESHIFT,  "80");
           props.put(WINDOWTYPE,  "1");
           props.put(NORMALIZE,   "1");
           props.put(FFTLEN,      "512");
           props.put(FREQWARP,    "0.42");
           props.put(GAMMA,       "0");
           props.put(MGCLSP,      "0");
           props.put(MGCORDER,    "24");
           props.put(STRORDER,    "5");
           props.put(LNGAIN,      "0");
           props.put(PSTFILTER,   "1.4");
           props.put(IMPLEN,      "4096");
           props.put(SAMPFREQ,    "16000");
           props.put(NMGCWIN,     "3");
           props.put(NSTRWIN,     "3");
           props.put(NLF0WIN,     "3");
           props.put(NSTATE,      "5");
           props.put(NITER,       "5");
           props.put(WFLOOR,      "3");

       }
       return props;
       }
    
    protected void setupHelp(){
        props2Help = new TreeMap<String,String>();
        
        props2Help.put(CONFIGUREFILE, "Path and name of configure file.");
        props2Help.put(SPEAKER,       "speaker name (default=slt)");
        props2Help.put(DATASET,       "dataset (default=cmu_us_arctic)");
        props2Help.put(LOWERF0,       "Lower limit for F0 extraction in Hz (default slt=80 female=80, male=40)");
        props2Help.put(UPPERF0,       "Upper limit for F0 extraction in Hz (default slt=350 female=350, male=280)");
        props2Help.put(NUMTESTFILES,  "Number of test files used for testing, these are copied from phonefeatures set.");
            
        props2Help.put(VER,         "version number of this setting (default=1)");
        props2Help.put(QNUM,        "question set number (default='001')");
        props2Help.put(FRAMELEN,    "Frame length in point (default=400)");
        props2Help.put(FRAMESHIFT,  "Frame shift in point (default=80)");
        props2Help.put(WINDOWTYPE,  "Window type -> 0: Blackman 1: Hamming 2: Hanning (default=1)");
        props2Help.put(NORMALIZE,   "Normalization -> 0: none 1: by power 2: by magnitude (default=1)");
        props2Help.put(FFTLEN,      "FFT length in point (default=512)");
        props2Help.put(FREQWARP,    "Frequency warping factor (default=0.42)");
        props2Help.put(GAMMA,       "Pole/Zero weight factor (0: mel-cepstral analysis 1: LPC analysis 2,3,...,N: mel-generalized cepstral (MGC) analysis) (default=0)");
        props2Help.put(MGCLSP,      "Use MGC-LSPs instead of MGC coefficients (default=0)");
        props2Help.put(MGCORDER,    "Order of MGC analysis (default=24 for cepstral form, default=12 for LSP form)");
        props2Help.put(STRORDER,    "Order of strengths analysis (default=5 for 5 filter bands)");
        props2Help.put(LNGAIN,      "Use logarithmic gain instead of linear gain (default=0)");
        props2Help.put(PSTFILTER,   "Postfiltering factor (default=1.4)");
        props2Help.put(IMPLEN,      "Length of impulse response (default=4096)");
        props2Help.put(SAMPFREQ,    "Sampling frequency in Hz (default=16000)");
        props2Help.put(NMGCWIN,     "number of delta windows for MGC coefficients (default=3)");
        props2Help.put(NSTRWIN,     "number of delta windows for STR coefficients (default=3)");
        props2Help.put(NLF0WIN,     "number of delta windows for log F0 values (default=3)");
        props2Help.put(NSTATE,      "number of HMM states (default=5)");
        props2Help.put(NITER,       "number of iterations of embedded training (default=5)");
        props2Help.put(WFLOOR,      "mixture weight flooring scale (default=3)");
             
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
 
       
       File dirWav  = new File(filedir + "wav");
       File dirText = new File(filedir + "text");
       File dirRaw  = new File(filedir + "hts/data/raw");
       File dirUtt  = new File(filedir + "hts/data/utts");
       
       /* Check if wav directory exist and have files */

       if( !dirWav.exists() || dirWav.list().length == 0 || !dirRaw.exists() || dirRaw.list().length == 0 ){ 
         System.out.println("Problem with wav and hts/data/raw directories: wav files and raw files do not exist" +
                " in current directory: " + filedir);
         speech_transcriptions = false;
       }  
       
       /* check if hts/data/raw directory exist and have files */
       if( !dirWav.exists() || dirWav.list().length == 0 || !dirRaw.exists() || dirRaw.list().length == 0 ){
          System.out.println("Problem with wav and hts/data/raw directories: wav files and raw files do not exist" +
                " in current directory: " + filedir);
          speech_transcriptions = false;
       } 
       
       /* Check if text directory exist and have files */
       if( ( !dirText.exists() || dirText.list().length == 0 ) && ( !dirUtt.exists() || dirUtt.list().length == 0 ) ){
         System.out.println("Problem with transcription directories text or hts/data/utts (Festival format): utts files and text files do not exist" +
                " in current directory: " + filedir);
         System.out.println(" the transcriptions in the directory text will be used to generate the phonelab directory, if there are no hts/data/utts files" +
                   "(in Festival format), please provide the transcriptions of the files you are going to use for trainning.");
         speech_transcriptions = false;
       } 
       
       
       if(speech_transcriptions){
           
       File dirFea = new File(filedir + "phonefeatures");
       File dirLab = new File(filedir + "phonelab");
       if(dirFea.exists() && dirFea.list().length > 0 && dirLab.exists() && dirLab.list().length > 0 ){ 
        System.out.println("\nphonefeatures directory exists and contains files.");  
           
        System.out.println("\nphonelab directory exists and contains files.");
        /* Create a phonefeatures/gen directory and copy there some examples of .pfeats
           files for testing the synthesis procedure once the models have been trained.*/
       
       
       /* if previous files and directories exist then run configure */
       /* first it should go to the hts directory and there run ./configure*/
       System.out.println("Running make configure: ");
       cmdLine = "chmod +x " + getProp(CONFIGUREFILE);
       General.launchProc(cmdLine, "configure", filedir);
       
       cmdLine = "cd " + filedir + "hts\n" + 
       getProp(CONFIGUREFILE) +
       " --with-tcl-search-path="        + db.getExternal(db.TCLPATH) +
       " --with-sptk-search-path="       + db.getExternal(db.SPTKPATH) +
       " --with-hts-search-path="        + db.getExternal(db.HTSPATH) +
       " --with-hts-engine-search-path=" + db.getExternal(db.HTSENGINEPATH) +
       " --with-sox-search-path="        + db.getExternal(db.SOXPATH) +
       " SPEAKER=" + getProp(SPEAKER) +
       " DATASET=" + getProp(DATASET) +
       " LOWERF0=" + getProp(LOWERF0) +
       " UPPERF0=" + getProp(UPPERF0) +
       " VER=" + getProp(VER) +
       " QNUM=" + getProp(QNUM) +
       " FRAMELEN=" + getProp(FRAMELEN) +
       " FRAMESHIFT=" + getProp(FRAMESHIFT) +
       " WINDOWTYPE=" + getProp(WINDOWTYPE) +
       " NORMALIZE=" + getProp(NORMALIZE) +
       " FFTLEN=" + getProp(FFTLEN) +
       " FREQWARP=" + getProp(FREQWARP) +
       " GAMMA=" + getProp(GAMMA) +
       " MGCLSP=" + getProp(MGCLSP) +
       " MGCORDER=" + getProp(MGCORDER) +
       " STRORDER=" + getProp(STRORDER) +
       " LNGAIN=" + getProp(LNGAIN) +
       " PSTFILTER=" + getProp(PSTFILTER) +
       " IMPLEN=" + getProp(IMPLEN) +
       " SAMPFREQ=" + getProp(SAMPFREQ) +
       " NMGCWIN=" + getProp(NMGCWIN) +
       " NSTRWIN=" + getProp(NSTRWIN) +
       " NLF0WIN=" + getProp(NLF0WIN) +
       " NSTATE=" + getProp(NSTATE) +
       " NITER=" + getProp(NITER) +
       " WFLOOR=" + getProp(WFLOOR);
       
       
       General.launchBatchProc(cmdLine, "Configure", filedir);
       
        
       } else {
         System.out.println("Problems with directories phonefeatures or phonelab, they do not exist or they are empty.");
         return false;
       }
       
       } else { /* if speech and transcriptions exist */
         System.out.println("Problems with directories wav, text or hts/data/raw, they do not exist or they are empty.");
         return false;
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
    
}
