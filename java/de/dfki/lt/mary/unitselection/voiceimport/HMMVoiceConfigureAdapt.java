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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;



public class HMMVoiceConfigureAdapt extends VoiceImportComponent{
    
    private DatabaseLayout db;
    private String name = "HMMVoiceConfigureAdapt";
    
    /** Tree files and TreeSet object */
    public final String CONFIGUREFILE = name+".configureFile";
    public final String HTSPATH       = name+".htsPath";
    public final String HTSENGINEPATH = name+".htsEnginePath";
    public final String SPTKPATH      = name+".sptkPath";
    public final String TCLPATH       = name+".tclPath";
    public final String SOXPATH       = name+".soxPath";
    
    public final String FEATURELIST   = name+".featureList";
    public final String VOICELANG     = name+".voiceLang";
    public final String SPEAKER       = name+".speaker"; 
    public final String DATASET       = name+".dataSet";
    public final String TRAINSPKR     = name+".trainSpkr";
    public final String ADAPTSPKR     = name+".adaptSpkr";
    public final String F0_RANGES     = name+".f0Ranges";
    public final String SPKRMASK      = name+".spkrMask";
    public final String ADAPTHEAD     = name+".adaptHead"; 
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
    
    public final String NMGCTRANSBLK     = name+".numMgcTransBlocks";
    public final String NSTRTRANSBLK     = name+".numStrTransBlocks";
    public final String NLF0TRANSBLK     = name+".numLf0TransBlocks";
    public final String MGCBANDWIDTH     = name+".mgcBandWidth";
    public final String STRBANDWIDTH     = name+".strBandWidth";
    public final String LF0BANDWIDTH     = name+".lf0BandWidth";
    public final String TREEKIND         = name+".treeKind";
    public final String TRANSKIND        = name+".transKind";
    public final String MGCOCCTHRESH     = name+".mgcOccThreshold";
    public final String STROCCTHRESH     = name+".strOccThreshold";
    public final String LF0OCCTHRESH     = name+".lf0OccThreshold";
    public final String SATMGCOCCTHRESH  = name+".satMgcOccThreshold";
    public final String SATSTROCCTHRESH  = name+".satStrOccThreshold";
    public final String SATLF0OCCTHRESH  = name+".satLf0OccThreshold";
    public final String PGTYPE           = name+".parameterGenerationType";

     
    
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
           props.put(HTSPATH,       "/project/mary/marcela/sw/HTS_2.0.1/htk/bin");
           props.put(HTSENGINEPATH, "/project/mary/marcela/sw/HTS_2.0.1/hts_engine_API-0.95/src/bin");
           props.put(SPTKPATH,      "/project/mary/marcela/sw/SPTK-3.1/bin");
           props.put(TCLPATH,       "/opt/ActiveTcl-8.4/bin");
           props.put(SOXPATH,       "/usr/bin");
           props.put(FEATURELIST,   rootdir+"data/feature_list_en.pl");
           props.put(VOICELANG,     "en");
           props.put(SPEAKER,       "slt");
           props.put(DATASET,       "cmu_us_arctic");
                   
           props.put(TRAINSPKR,     "'awb bdl clb jmk rms'");
           props.put(ADAPTSPKR,     "slt");
           props.put(F0_RANGES,     "'awb 40 280  bdl 40 280  clb 80 350  jmk 40 280  rms 40 280  slt 80 350'");
           props.put(SPKRMASK,      "*/cmu_us_arctic_%%%_*");
           props.put(ADAPTHEAD,     "b05");
           props.put(NUMTESTFILES,  "5");
           
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
                     
           props.put(NMGCTRANSBLK,    "3");
           props.put(NSTRTRANSBLK,    "3");
           props.put(NLF0TRANSBLK,    "1");
           props.put(MGCBANDWIDTH,    "24");
           props.put(STRBANDWIDTH,    "5");
           props.put(LF0BANDWIDTH,    "1");
           props.put(TREEKIND,        "dec");
           props.put(TRANSKIND,       "feat");
           props.put(MGCOCCTHRESH,    "1000.0");
           props.put(STROCCTHRESH,    "1000.0");
           props.put(LF0OCCTHRESH,    "200.0");
           props.put(SATMGCOCCTHRESH, "10000.0");
           props.put(SATSTROCCTHRESH, "10000.0");
           props.put(SATLF0OCCTHRESH, "2000.0");
           props.put(PGTYPE ,         "0");

            
           

       }
       return props;
       }
    
    protected void setupHelp(){
        props2Help = new TreeMap<String,String>();
        
        props2Help.put(CONFIGUREFILE, "Path and name of configure file.");
        props2Help.put(HTSPATH,       "Path to HTS_2.0.1 - HTK bin directory.");
        props2Help.put(HTSENGINEPATH, "Path to HTS_2.0.1 Engine path  - in Mary it is used for testing during training.");
        props2Help.put(SPTKPATH,      "Path to SPTK-3.1 bin directory.");
        props2Help.put(TCLPATH,       "Path to Tcl bin, it should support snack.");
        props2Help.put(SOXPATH,       "Path to sox bin.");
        props2Help.put(FEATURELIST,   "Mary context features file (default English=data/feature_list_en.pl, for German=data/feature_list_de.pl), this file can be modified according to the number of context features used.");
        props2Help.put(VOICELANG,     "voice language (default='en')");
        props2Help.put(SPEAKER,       "speaker name (default=slt)");
        props2Help.put(DATASET,       "dataset (default=cmu_us_arctic)");
        
        props2Help.put(TRAINSPKR,     "speakers for training (default='awb bdl clb jmk rms')");
        props2Help.put(ADAPTSPKR,     "speakers for adaptation (default=slt)");
        props2Help.put(SPKRMASK,      "speaker name pattern (mask for file names, -h option in HERest) (default=*/cmu_us_arctic_%%%_*)");
        props2Help.put(ADAPTHEAD,     "file name header for adaptation data (default=b05)");
      
        props2Help.put(F0_RANGES,     "F0 search ranges (spkr1 lower1 upper1  spkr2 lower2 upper2...). " +
                                      "Order of speakers in F0_RANGES should be equal to that in ALLSPKR=$(TRAINSPKR) $(ADAPTSPKR)" +
                                      "(default='awb 40 280  bdl 40 280  clb 80 350  jmk 40 280  rms 40 280  slt 80 350')");
        props2Help.put(NUMTESTFILES,  "Number of test files used for testing, these are copied from each phonefeatures set.");
             
        props2Help.put(VER,           "version number of this setting (default=1)");
        props2Help.put(QNUM,          "question set number (default='001')");
        props2Help.put(FRAMELEN,      "Frame length in point (default=400)");
        props2Help.put(FRAMESHIFT,    "Frame shift in point (default=80)");
        props2Help.put(WINDOWTYPE,    "Window type -> 0: Blackman 1: Hamming 2: Hanning (default=1)");
        props2Help.put(NORMALIZE,     "Normalization -> 0: none 1: by power 2: by magnitude (default=1)");
        props2Help.put(FFTLEN,        "FFT length in point (default=512)");
        props2Help.put(FREQWARP,      "Frequency warping factor (default=0.42)");
        props2Help.put(GAMMA,         "Pole/Zero weight factor (0: mel-cepstral analysis 1: LPC analysis 2,3,...,N: mel-generalized cepstral (MGC) analysis) (default=0)");
        props2Help.put(MGCLSP,        "Use MGC-LSPs instead of MGC coefficients (default=0)");
        props2Help.put(MGCORDER,      "Order of MGC analysis (default=24 for cepstral form, default=12 for LSP form)");
        props2Help.put(STRORDER,      "Order of strengths analysis (default=5 for 5 filter bands)");
        props2Help.put(LNGAIN,        "Use logarithmic gain instead of linear gain (default=0)");
        props2Help.put(PSTFILTER,     "Postfiltering factor (default=1.4)");
        props2Help.put(IMPLEN,        "Length of impulse response (default=4096)");
        props2Help.put(SAMPFREQ,      "Sampling frequency in Hz (default=16000)");
        props2Help.put(NMGCWIN,       "number of delta windows for MGC coefficients (default=3)");
        props2Help.put(NSTRWIN,       "number of delta windows for STR coefficients (default=3)");
        props2Help.put(NLF0WIN,       "number of delta windows for log F0 values (default=3)");
        props2Help.put(NSTATE,        "number of HMM states (default=5)");
        props2Help.put(NITER,         "number of iterations of embedded training (default=5)");
        props2Help.put(WFLOOR,        "mixture weight flooring scale (default=3)");
        
        props2Help.put(WFLOOR,          "mixture weight flooring scale (default=3)");
        props2Help.put(NMGCTRANSBLK,    "number of blocks for MGC transforms (default=3)");
        props2Help.put(NSTRTRANSBLK,    "number of blocks for STR transforms (default=3)");
        props2Help.put(NLF0TRANSBLK,    "number of blocks for log F0 transforms (default=1)");
        props2Help.put(MGCBANDWIDTH,    "band width for MGC transforms (default=24 for cepstral form, derault=1 for LSP form)");
        props2Help.put(STRBANDWIDTH,    "band width for STR transforms (default=5)");
        props2Help.put(LF0BANDWIDTH,    "band width for log F0 transforms (default=1)");
        props2Help.put(TREEKIND,        "regression class tree kind (dec: decision tree, reg: regression tree, default=dec)");
        props2Help.put(TRANSKIND,       "adaptation transform kind (mean: MLLRMEAN, cov: MLLRCOV, feat: CMLLR, default=feat)");
        props2Help.put(MGCOCCTHRESH,    "occupancy threshold to adapt MGC stream (default=1000.0)");
        props2Help.put(STROCCTHRESH,    "occupancy threshold to adapt STR stream (default=1000.0)");
        props2Help.put(LF0OCCTHRESH,    "occupancy threshold to adapt log F0 streams (default=200.0)");
        props2Help.put(SATMGCOCCTHRESH, "occupancy threshold for adaptive training of MGC stream (default=10000.0)");
        props2Help.put(SATSTROCCTHRESH, "occupancy threshold for adaptive training of STR stream (default=10000.0)");
        props2Help.put(SATLF0OCCTHRESH, "occupancy threshold for adaptive training of log F0 streams (default=2000.0)");
        props2Help.put(PGTYPE,          "type of speech parameter generation algorithm (0: Cholesky, 1: MixHidden, 2: StateHidden, default=0)");

             
    }

    
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
        
        System.out.println("\nChecking directories and files for running HTS ADAPT training scripts...");
        
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
       if( ( !dirText.exists() || dirText.list().length == 0 ) && ( !dirUtt.exists() || dirUtt.list().length == 0 ) ){
         System.out.println("Problem with transcription directories text or data/utts (Festival format): utts files and text files do not exist.");
         System.out.println(" the transcriptions in the directory text will be used to generate the phonelab directory, if there are no data/utts files" +
                   "(in Festival format), please provide the transcriptions of the files you are going to use for trainning.");
         speech_transcriptions = false;
       } 
       
       
       if(speech_transcriptions){
           
       File dirFea = new File("phonefeatures");
       File dirLab = new File("phonelab");
       /* Check if phonefeatures directory exist and have files */
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
        
         String[] spkrDir = dirFea.list();
         for (int i=0; i<spkrDir.length; i++) {
           //System.out.println("ADAPTSPKR=" + getProp(ADAPTSPKR) + "  spkrDir["+i+"] = " + spkrDir[i]);    
           if(!spkrDir[i].contentEquals("gen") && getProp(ADAPTSPKR).contains(spkrDir[i])){  
             File dirAdapt = new File("phonefeatures/" + spkrDir[i]);                       
             String[] feaFiles = dirAdapt.list();  
             for (int j=0; j<numFiles; j ++){
               cmdLine = "cp phonefeatures/" + spkrDir[i] + "/" + feaFiles[j] + " phonefeatures/gen/";
               launchProc(cmdLine, "file copy", filedir);
             }
           }
         }
       } else
         System.out.println("\nDirectory phonefeatures/gen already exist and has some files.");   
        
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
       " --with-hts-engine-search-path=" + getProp(HTSENGINEPATH) +
       " --with-sox-search-path=" + getProp(SOXPATH) +
       " VOICELANG=" + getProp(VOICELANG) +
       " FEATURELIST=" + getProp(FEATURELIST) +
       " SPEAKER=" + getProp(SPEAKER) +
       " DATASET=" + getProp(DATASET) +
       " TRAINSPKR=" + getProp(TRAINSPKR) + 
       " ADAPTSPKR=" + getProp(ADAPTSPKR) + 
       " F0_RANGES=" + getProp(F0_RANGES) + 
       " SPKRMASK=" + getProp(SPKRMASK) + 
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
       " WFLOOR=" + getProp(WFLOOR) +                
       " NMGCTRANSBLK=" + getProp(NMGCTRANSBLK) +
       " NSTRTRANSBLK=" + getProp(NSTRTRANSBLK) +
       " NLF0TRANSBLK=" + getProp(NLF0TRANSBLK) +
       " MGCBANDWIDTH=" + getProp(MGCBANDWIDTH) +
       " STRBANDWIDTH=" + getProp(STRBANDWIDTH) +
       " LF0BANDWIDTH=" + getProp(LF0BANDWIDTH) +
       " TREEKIND=" + getProp(TREEKIND) +
       " TRANSKIND=" + getProp(TRANSKIND) +
       " MGCOCCTHRESH=" + getProp(MGCOCCTHRESH) +
       " STROCCTHRESH=" + getProp(STROCCTHRESH) +
       " LF0OCCTHRESH=" + getProp(LF0OCCTHRESH) +
       " SATMGCOCCTHRESH=" + getProp(SATMGCOCCTHRESH) +
       " SATSTROCCTHRESH=" + getProp(SATSTROCCTHRESH) +
       " SATLF0OCCTHRESH=" + getProp(SATLF0OCCTHRESH) +
       " PGTYPE=" + getProp(PGTYPE);
     
       launchBatchProc(cmdLine, "Configure", filedir);
        
       } else
         System.out.println("Problems with directories phonefeatures or phonelab, they do not exist or they are empty.");  
       
       } else /* if speech and transcriptions exist */
         System.out.println("Problems with directories wav, text or data/raw, they do not exist or they are empty.");
       
       /* delete the temporary file*/
       File tmpBatch = new File(filedir+"tmp-configure.bat");
       tmpBatch.delete();
       
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
     * A general process launcher for the various tasks but using an intermediate batch file
     * (copied from ESTCaller.java)
     * @param cmdLine the command line to be launched.
     * @param task a task tag for error messages, such as "Pitchmarks" or "LPC".
     * @param the basename of the file currently processed, for error messages.
     */
    private void launchBatchProc( String cmdLine, String task, String baseName ) {
        
        Process proc = null;
        Process proctmp = null;
        BufferedReader procStdout = null;
        String line = null;
        String filedir = db.getProp(db.ROOTDIR);
        String tmpFile = filedir+"tmp-configure.bat";
        System.out.println("Running: "+ cmdLine);
        // String[] cmd = null; // Java 5.0 compliant code
        
        try {
            FileWriter tmp = new FileWriter(tmpFile);
            tmp.write(cmdLine);
            tmp.close();
            
            /* make it executable... */
            proctmp = Runtime.getRuntime().exec( "chmod +x "+tmpFile );
            proctmp.waitFor();
            /* Java 5.0 compliant code below. */
            /* Hook the command line to the process builder: */
            /* cmd = cmdLine.split( " " );
            pb.command( cmd ); /*
            /* Launch the process: */
            /*proc = pb.start(); */
            
            /* Java 1.0 equivalent: */
            proc = Runtime.getRuntime().exec( tmpFile );
            
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
