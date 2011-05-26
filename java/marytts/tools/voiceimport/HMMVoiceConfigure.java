/* ----------------------------------------------------------------- */
/*           The HMM-Based Speech Synthesis Engine "hts_engine API"  */
/*           developed by HTS Working Group                          */
/*           http://hts-engine.sourceforge.net/                      */
/* ----------------------------------------------------------------- */
/*                                                                   */
/*  Copyright (c) 2001-2010  Nagoya Institute of Technology          */
/*                           Department of Computer Science          */
/*                                                                   */
/*                2001-2008  Tokyo Institute of Technology           */
/*                           Interdisciplinary Graduate School of    */
/*                           Science and Engineering                 */
/*                                                                   */
/* All rights reserved.                                              */
/*                                                                   */
/* Redistribution and use in source and binary forms, with or        */
/* without modification, are permitted provided that the following   */
/* conditions are met:                                               */
/*                                                                   */
/* - Redistributions of source code must retain the above copyright  */
/*   notice, this list of conditions and the following disclaimer.   */
/* - Redistributions in binary form must reproduce the above         */
/*   copyright notice, this list of conditions and the following     */
/*   disclaimer in the documentation and/or other materials provided */
/*   with the distribution.                                          */
/* - Neither the name of the HTS working group nor the names of its  */
/*   contributors may be used to endorse or promote products derived */
/*   from this software without specific prior written permission.   */
/*                                                                   */
/* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND            */
/* CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,       */
/* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF          */
/* MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE          */
/* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS */
/* BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,          */
/* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED   */
/* TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,     */
/* DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON */
/* ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,   */
/* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY    */
/* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE           */
/* POSSIBILITY OF SUCH DAMAGE.                                       */
/* ----------------------------------------------------------------- */
/**
 * Copyright 2011 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
    public final String ADAPTSCRIPTS  = name+".adaptScripts";
    
    /** settings for speaker independent training */    
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
    public final String MGCORDER      = name+".mgcOrder";
    public final String STRORDER      = name+".strOrder";
    
    public final String MGCBANDWIDTH  = name+".mgcBandWidth";
    public final String STRBANDWIDTH  = name+".strBandWidth";
    public final String LF0BANDWIDTH  = name+".lf0BandWidth";
    
    public final String LNGAIN        = name+".lnGain";
    public final String PSTFILTER     = name+".pstFilter";
    public final String IMPLEN        = name+".impulseLen";
    public final String SAMPFREQ      = name+".sampfreq";
    public final String NSTATE        = name+".numState";
    public final String NITER         = name+".numIterations";
    
    /** settings for HTS ADAPT training scripts */
    public final String ADAPTTRAINSPKR = name+".adaptTrainSpkr";
    public final String ADAPTSPKR      = name+".adaptSpkr";
    public final String ADAPTF0_RANGES = name+".adaptF0Ranges";
    public final String ADAPTSPKRMASK  = name+".adaptSpkrMask";
    public final String ADAPTHEAD      = name+".adaptHead"; 
    
    public final String ADAPTTREEKIND  = name+".adaptTreeKind";
    public final String ADAPTTRANSKIND = name+".adaptTransKind";

     
    
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
           props.put(LOWERF0,       "110");
           props.put(UPPERF0,       "280");
           props.put(NUMTESTFILES,  "10");
           
           props.put(VER,         "1");
           props.put(QNUM,        "001");
           props.put(SAMPFREQ,    "48000");
           props.put(FRAMELEN,    "1200");
           props.put(FRAMESHIFT,  "240");
           props.put(WINDOWTYPE,  "1");
           props.put(NORMALIZE,   "1");
           props.put(FFTLEN,      "2048");
           props.put(FREQWARP,    "0.55");
           props.put(GAMMA,       "0");           
           props.put(MGCORDER,    "34");
           props.put(STRORDER,    "5");
           
           props.put(MGCBANDWIDTH,    "35");
           props.put(STRBANDWIDTH,    "5");
           props.put(LF0BANDWIDTH,    "1");
          
           props.put(LNGAIN,      "1");
           props.put(PSTFILTER,   "1.4");
           props.put(IMPLEN,      "4096");
           props.put(NSTATE,      "5");
           props.put(NITER,       "5"); 
             
           /** settings for HTS ADAPT training scripts */
           props.put(ADAPTTRAINSPKR,     "'bdl clb jmk rms'");
           props.put(ADAPTSPKR,     "slt");           
           //props.put(F0_RANGES,     "'awb 40 280  bdl 40 280  clb 80 350  jmk 40 280  rms 40 280  slt 80 350'");
           props.put(ADAPTF0_RANGES,     "'bdl 40 210 clb 130 260 jmk 50 180 rms 40 200 slt 110 280'");
           props.put(ADAPTSPKRMASK,      "*/cmu_us_arctic_%%%_*");
           props.put(ADAPTHEAD,     "b05");
           props.put(NUMTESTFILES,  "5");
               
           props.put(ADAPTTREEKIND,        "dec");
           props.put(ADAPTTRANSKIND,       "feat");  
           
           props.put(ADAPTSCRIPTS, "false");
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
        props2Help.put(SAMPFREQ,      "Sampling frequency in Hz (default=48000)");
        props2Help.put(FRAMELEN,      "Frame length in point (16Khz: 400, 48Khz:1200 default=1200)");
        props2Help.put(FRAMESHIFT,    "Frame shift in point (16Khz: 80, 48Khz: 240, default=240)");
        props2Help.put(WINDOWTYPE,  "Window type -> 0: Blackman 1: Hamming 2: Hanning (default=1)");
        props2Help.put(NORMALIZE,   "Normalization -> 0: none 1: by power 2: by magnitude (default=1)");
        props2Help.put(FFTLEN,      "FFT length in point (16KHz: 512, 48KHz: 2048,  default=2048)");
        props2Help.put(FREQWARP,      "Frequency warping factor +" +
                "8000  FREQWARP=0.31 " +
                "10000 FREQWARP=0.35 " +
                "12000 FREQWARP=0.37 " +
                "16000 FREQWARP=0.42 " +
                "22050 FREQWARP=0.45 " +
                "32000 FREQWARP=0.45 " +
                "44100 FREQWARP=0.53 " +
                "48000 FREQWARP=0.55  default=0.55)");
        props2Help.put(GAMMA,       "Pole/Zero weight factor (0: mel-cepstral analysis 1: LPC analysis 2,3,...,N: mel-generalized cepstral (MGC) analysis) (default=0)");
        props2Help.put(MGCORDER,    "Order of MGC analysis (default=24 for cepstral form, default=12 for LSP form)");
        props2Help.put(STRORDER,    "Order of strengths analysis (default=5 for 5 filter bands)");
        
        props2Help.put(MGCBANDWIDTH,    "band width for MGC transforms (default=24 for cepstral form, derault=1 for LSP form)");
        props2Help.put(STRBANDWIDTH,    "band width for STR transforms (default=5)");
        props2Help.put(LF0BANDWIDTH,    "band width for log F0 transforms (default=1)");
        
        props2Help.put(LNGAIN,      "Use logarithmic gain instead of linear gain (default=0)");
        props2Help.put(PSTFILTER,   "Postfiltering factor (default=1.4)");
        props2Help.put(IMPLEN,      "Length of impulse response (default=4096)");
        props2Help.put(NSTATE,      "number of HMM states (default=5)");
        props2Help.put(NITER,       "number of iterations of embedded training (default=5)");        
        
        // for scripts adapt
        props2Help.put(ADAPTTRAINSPKR,"only ADAPTSCRIPTS: speakers for training (default='awb bdl clb jmk rms')");
        props2Help.put(ADAPTSPKR,     "only ADAPTSCRIPTS: speakers for adaptation (default=slt)");
        props2Help.put(ADAPTSPKRMASK, "only ADAPTSCRIPTS: speaker name pattern (mask for file names, -h option in HERest) (default=*/cmu_us_arctic_%%%_*)");
        props2Help.put(ADAPTHEAD,     "only ADAPTSCRIPTS: file name header for adaptation data (default=b05)");      
        props2Help.put(ADAPTF0_RANGES,"only ADAPTSCRIPTS: F0 search ranges (spkr1 lower1 upper1  spkr2 lower2 upper2...). " +
                                      "only ADAPTSCRIPTS: Order of speakers in F0_RANGES should be equal to that in ALLSPKR=$(TRAINSPKR) $(ADAPTSPKR)" +
                                      "(default='bdl 40 210 clb 130 260 jmk 50 180 rms 40 200 slt 110 280')");        
        props2Help.put(ADAPTTREEKIND, "only ADAPTSCRIPTS: regression class tree kind (dec: decision tree, reg: regression tree, default=dec)");
        props2Help.put(ADAPTTRANSKIND,"only ADAPTSCRIPTS: adaptation transform kind (mean: MLLRMEAN, cov: MLLRCOV, feat: CMLLR, default=feat)");
          
        props2Help.put(ADAPTSCRIPTS, "ADAPTSCRIPTS=false: speaker dependent scripts, ADAPTSCRIPTS=true: speaker adaptation/adaptive scripts.  ");     
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
       File dirFea  = new File(filedir + "phonefeatures");
       File dirLab  = new File(filedir + "phonelab");

       if(getProp(ADAPTSCRIPTS).contentEquals("false")) { 
         // Check if wav directory exist and have files
         if( !dirWav.exists() || dirWav.list().length == 0 || !dirRaw.exists() || dirRaw.list().length == 0 ){ 
           System.out.println("Problem with wav and hts/data/raw directories: wav files and raw files do not exist" +
                " in current directory: " + filedir);
           speech_transcriptions = false;
         }        
         // check if hts/data/raw directory exist and have files
         if( !dirWav.exists() || dirWav.list().length == 0 || !dirRaw.exists() || dirRaw.list().length == 0 ){
           System.out.println("Problem with wav and hts/data/raw directories: wav files and raw files do not exist" +
                " in current directory: " + filedir);
           speech_transcriptions = false;
         }        
         // Check if text directory exist and have files
         if( ( !dirText.exists() || dirText.list().length == 0 ) && ( !dirUtt.exists() || dirUtt.list().length == 0 ) ){
           System.out.println("Problem with transcription directories text or hts/data/utts (Festival format): utts files and text files do not exist" +
                " in current directory: " + filedir);
           System.out.println(" the transcriptions in the directory text will be used to generate the phonelab directory, if there are no hts/data/utts files" +
                   "(in Festival format), please provide the transcriptions of the files you are going to use for trainning.");
           speech_transcriptions = false;
         } 
         // Check if phonefeatures and phonelab directory exist and have files
         if( ( !dirFea.exists() || dirFea.list().length == 0 ) && ( !dirLab.exists() || dirLab.list().length == 0 ) ){
           System.out.println("Problems with directories phonefeatures or phonelab, they do not exist or they are empty.");
           speech_transcriptions = false;
         }
       } else {  // configuration for ADAPTSCRIPT
           // Get the speakers directories
           File dirSpeakersFea = new File(filedir + "/phonefeatures");
           File dirSpeakersLab = new File(filedir + "/phonelab");
           
           String[] speakers;
           if(dirSpeakersFea.exists() && dirSpeakersFea.list().length > 0 && dirSpeakersLab.exists() && dirSpeakersLab.list().length > 0 ){ 
               speakers = dirSpeakersFea.list();
               for(int i=0; i<speakers.length; i++){
                 File dirSpeakerFea = new File(filedir + "/phonefeatures/" + speakers[i]);
                 File dirSpeakerLab = new File(filedir + "/phonelab/" + speakers[i]);
                 if(dirSpeakerFea.exists() && dirSpeakerFea.list().length > 0 && dirSpeakerLab.exists() && dirSpeakerLab.list().length > 0 ){
                   speech_transcriptions = true;     
                 } else {
                   System.out.println("Error: directories " + filedir + "/phonefeatures/" + speakers[i] + 
                           " and/or " + filedir + "/phonelab/" + speakers[i] + " do not contain files." );
                   speech_transcriptions = false;
                   break;
                 }
               }
           } else {            
               System.out.println("Error: directories " + filedir + "phonefeatures and/or " + filedir + "phonelab do not contain files." );
               speech_transcriptions = false;
           }               
       }
       
       if(speech_transcriptions){
           
       if(getProp(ADAPTSCRIPTS).contentEquals("false")) {                    
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
         " MGCORDER=" + getProp(MGCORDER) +
         " STRORDER=" + getProp(STRORDER) +
         " LNGAIN=" + getProp(LNGAIN) +
         " PSTFILTER=" + getProp(PSTFILTER) +
         " IMPLEN=" + getProp(IMPLEN) +
         " SAMPFREQ=" + getProp(SAMPFREQ) +
         " NSTATE=" + getProp(NSTATE) +
         " NITER=" + getProp(NITER);
         
       } else {            
             /* if previous files and directories exist then run configure */
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
             " TRAINSPKR=" + getProp(ADAPTTRAINSPKR) + 
             " ADAPTSPKR=" + getProp(ADAPTSPKR) + 
             " F0_RANGES=" + getProp(ADAPTF0_RANGES) + 
             " SPKRMASK=" + getProp(ADAPTSPKRMASK) + 
             " ADAPTHEAD=" + getProp(ADAPTHEAD) +          
             " VER=" + getProp(VER) +
             " QNUM=" + getProp(QNUM) +
             " FRAMELEN=" + getProp(FRAMELEN) +
             " FRAMESHIFT=" + getProp(FRAMESHIFT) +
             " WINDOWTYPE=" + getProp(WINDOWTYPE) +
             " NORMALIZE=" + getProp(NORMALIZE) +
             " FFTLEN=" + getProp(FFTLEN) +
             " FREQWARP=" + getProp(FREQWARP) +
             " GAMMA=" + getProp(GAMMA) +
             " MGCORDER=" + getProp(MGCORDER) +
             " STRORDER=" + getProp(STRORDER) +
             " LNGAIN=" + getProp(LNGAIN) +
             " PSTFILTER=" + getProp(PSTFILTER) +
             " IMPLEN=" + getProp(IMPLEN) +
             " SAMPFREQ=" + getProp(SAMPFREQ) +
             " NSTATE=" + getProp(NSTATE) +
             " NITER=" + getProp(NITER) +
             " MGCBANDWIDTH=" + getProp(MGCBANDWIDTH) +
             " STRBANDWIDTH=" + getProp(STRBANDWIDTH) +
             " LF0BANDWIDTH=" + getProp(LF0BANDWIDTH) +
             " TREEKIND=" + getProp(ADAPTTREEKIND) +
             " TRANSKIND=" + getProp(ADAPTTRANSKIND);
         }
              
         General.launchBatchProc(cmdLine, "Configure", filedir);
             
       } else {
         System.out.println("Problems with directories phonefeatures or phonelab, they do not exist or they are empty.");
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
