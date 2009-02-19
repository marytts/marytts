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
package marytts.htsengine;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.modules.HTSEngine;
import marytts.signalproc.analysis.Mfccs;
import marytts.util.data.audio.AudioPlayer;

/***
 * Several functions for running the htsEngine or other components stand alone
 * @author Marcela Charfuelan
 *
 */
public class HTSEngineTest {
    
    
    public class PhonemeDuration {
        private String phoneme;
        private float duration;
        
        public PhonemeDuration(String ph, float dur){
            phoneme = ph;
            duration = dur;
        }
        
        public void setPhoneme(String str){ phoneme=str; }
        public void setDuration(float fval){ duration=fval; }
        public String getPhoneme(){ return phoneme; }
        public float getDuration(){ return duration; }
        
      }

    
    /** 
     * Generation of speech using external specification of duration
     * NOTE: The use of external F0 contour is not finished yet, it needs to be aligned with the duration.
     *       It is not clear yet which F0 contour to use, CART generated?, MARY generated?.
     * Stand alone testing using a TARGETFEATURES file as input. 
     * @param args
     * @throws IOException
     */
    public void synthesisWithExternalProsodySpecification() throws Exception{
       
      int i, j;  
      // context features file
      String feaFile = "/project/mary/marcela/openmary/lib/voices/hsmm-slt-mary4.0/cmu_us_arctic_slt_a0001.pfeats";     
      // external duration extracted with the voice import tools - EHMM
      String labFile = "/project/mary/marcela/f0-hsmm-experiment/cmu_us_arctic_slt_a0001.lab";
      // external duration obtained with MARY, there is a problem with this because it does not have an initial sil
      //String labFile = "/project/mary/marcela/f0-hsmm-experiment/cmu_us_arctic_slt_a0001.realised_durations";
      // external F0 contour
      String f0File = "/project/mary/marcela/f0-hsmm-experiment/cmu_us_arctic_slt_a0001-littend.lf0";

      HTSEngine hmm_tts = new HTSEngine();
      HMMData htsData = new HMMData();
      
      /* For initialise provide the name of the hmm voice and the name of its configuration file,*/     
      String MaryBase    = "/project/mary/marcela/openmary/"; /* MARY_BASE directory.*/
      String voiceName   = "hsmm-slt-mary4.0";                        /* voice name */
      String voiceConfig = "english-hsmm-slt-mary4.0.config";         /* voice configuration file name. */        
      String outWavFile  = MaryBase + "tmp/tmp.wav";          /* to save generated audio file */
      
      htsData.initHMMData(voiceName, MaryBase, voiceConfig);
      
      // Load and set durations    
      hmm_tts.setPhonemeAlignmentForDurations(true);
      Vector<PhonemeDuration> durations = loadDurationsForAlignment(labFile);
      hmm_tts.setAlignDurations(durations);
      
      
      // The settings for using GV and MixExc can be change in this way:
      htsData.setUseGV(true);
      htsData.setUseMixExc(true);
      htsData.setUseFourierMag(false);  // if the voice was trained with Fourier magnitudes
        
      /** The utterance model, um, is a Vector (or linked list) of Model objects. 
       * It will contain the list of models for current label file. */
      HTSUttModel um = new HTSUttModel();
      HTSParameterGeneration pdf2par = new HTSParameterGeneration();        
      HTSVocoder par2speech = new HTSVocoder();
      AudioInputStream ais;
                  
      try {
          /* Process Mary context features file and creates UttModel um.   */
          hmm_tts.processUttFromFile(feaFile, um, htsData);

          /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */
          /* the generated parameters will be saved in tmp.mfc and tmp.f0, including Mary header. */
          boolean debug = false;  /* so it DOES NOT save the generated parameters in parFile */
          pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData, null, debug);
          
          // load F0
          // It does not work yet because the contour is NOT aligned with the duration
          //loadF0contour(f0File, pdf2par);
          
          /* Synthesize speech waveform, generate speech out of sequence of parameters */
          ais = par2speech.htsMLSAVocoder(pdf2par, htsData);
     
          System.out.println("saving to file: " + outWavFile);
          File fileOut = new File(outWavFile);
          
          if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE,ais)) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fileOut);
          }

          System.out.println("Calling audioplayer:");
          AudioPlayer player = new AudioPlayer(fileOut);
          player.start();  
          player.join();
          System.out.println("audioplayer finished...");
       
     
      } catch (Exception e) {
          System.err.println("Exception: " + e.getMessage());
      }
    }  /* main method */
    
    
    /***
     * Load durations for phoneme alignment when the durations have been generated by MARY.
     * @param fileName the format is the same as for REALISED_DURATIONS in MARY.
     * @return
     */
    public Vector<PhonemeDuration> loadDurationsForAlignment(String fileName){
        Vector<PhonemeDuration> alignDur = new Vector<PhonemeDuration>(); 
        Scanner s = null;
        String line;
        float previous=0;
        float current=0;
        try {
          s = new Scanner(new File(fileName));
          int i=0;
          while (s.hasNext()) {
           line = s.nextLine();
           if( !line.startsWith("#") && !line.startsWith("format") ){  
             String val[] = line.split(" ");
             current = Float.parseFloat(val[0]);
             PhonemeDuration var;
             if(previous==0)
               alignDur.add(new PhonemeDuration(val[2],current));
             else
               alignDur.add(new PhonemeDuration(val[2],(current-previous)));
           
             System.out.println("phoneme = " + alignDur.get(i).getPhoneme() + " dur(" + i +")=" + alignDur.get(i).getDuration());
             i++;
             previous = current;
           }       
          }
          System.out.println();
          s.close();      
        } catch (IOException e) {
            e.printStackTrace();   
        } 
        
        return alignDur;
      }    
    
    /***
     * Load F0, in HTS format, create a voiced array and set this values in pdf2par
     * (This contour is NOT aligned with the duration yet.)
     * @param lf0File
     * @param pdf2par
     * @throws Exception
     */
     public void loadF0contour(String lf0File, HTSParameterGeneration pdf2par) throws Exception{
         HTSPStream lf0Pst=null;
         boolean [] voiced = null;
         DataInputStream lf0Data;
         
         int lf0Vsize = 3;
         int totalFrame = 0;
         int lf0VoicedFrame = 0;
         float fval;
         lf0Data = new DataInputStream (new BufferedInputStream(new FileInputStream(lf0File)));   
         /* First i need to know the size of the vectors */
         try { 
           while (true) {
             fval = lf0Data.readFloat();
             totalFrame++;  
             if(fval>0)
              lf0VoicedFrame++;
           } 
         } catch (EOFException e) { }
         lf0Data.close();
        
         voiced = new boolean[totalFrame];
         lf0Pst = new HTSPStream(lf0Vsize, totalFrame, HMMData.LF0);
         
         /* load lf0 data */
         /* for lf0 i just need to load the voiced values */
         lf0VoicedFrame = 0;
         lf0Data = new DataInputStream (new BufferedInputStream(new FileInputStream(lf0File)));
         for(int i=0; i<totalFrame; i++){
           fval = lf0Data.readFloat();  
           //lf0Pst.setPar(i, 0, fval);
           if(fval < 0)
             voiced[i] = false;
           else{
             voiced[i] = true;
             lf0Pst.setPar(lf0VoicedFrame, 0, fval);
             lf0VoicedFrame++;
           }
         }
         lf0Data.close();
         
         // Set lf0 and voiced in pdf2par
         pdf2par.setlf0Pst(lf0Pst);
         pdf2par.setVoicedArray(voiced);
         
     }
    
    
    /** 
     * Stand alone testing using a TARGETFEATURES file as input. 
     * Generates duration: file.lab, duration state level: file.slab, 
     * f0: file.f0, mfcc: file.mfcc and sound file: file.wav out of HMM models
     * @param args file.pfeats and hmm voice
     * @throws IOException
     */
    public void generateParameters() throws IOException, InterruptedException{
       
      int i, j;  
      /* For initialise provide the name of the hmm voice and the name of its configuration file,
       * also indicate the name of your MARY_BASE directory.*/
      String MaryBase = "/project/mary/marcela/openmary/";
      String locale = "english";
      String voice = "hsmm-slt";
      String configFile = locale + "-" + voice + ".config";
      
      // directory where the context features of each file are
      String contextFeaDir = "/project/mary/marcela/quality-control-experiment/slt/phonefeatures/";
      // the output dir has to be created already
      String outputDir = "/project/mary/marcela/quality-control-experiment/slt/hmmGenerated/";
      // list of contex features files, the file names contain the basename without path and ext 
      String filesList = "/project/mary/marcela/quality-control-experiment/slt/phonefeatures-list.txt";
   
      // Create a htsengine object
      HTSEngine hmm_tts = new HTSEngine();
      
      // Create and set HMMData
      HMMData htsData = new HMMData();
      htsData.initHMMData(voice, MaryBase, configFile);
      float fperiodmillisec = ((float)htsData.getFperiod() / (float)htsData.getRate()) * 1000;
      float fperiodsec = ((float)htsData.getFperiod() / (float)htsData.getRate());
         
      // Settings for using GV, mixed excitation
      htsData.setUseGV(true);
      htsData.setUseMixExc(true);
   
      /* generate files out of HMMs */
      String file, feaFile, parFile, durStateFile, durFile, mgcModifiedFile, outWavFile;
      try {
      Scanner filesScanner = new Scanner(new BufferedReader(new FileReader(filesList))); 
      while (filesScanner.hasNext()) {
          
        file = filesScanner.nextLine();
 
        feaFile      = contextFeaDir + file + ".pfeats";
        parFile      = outputDir + file;            /* generated parameters mfcc and f0, Mary format */
        durFile      = outputDir + file + ".lab";   /* realised durations */
        durStateFile = outputDir + file + ".slab";  /* state level realised durations */
        outWavFile   = outputDir + file + ".wav";   /* generated wav file */
       
  
        /* The utterance model, um, is a Vector (or linked list) of Model objects. 
         * It will contain the list of models for the current label file. */
        HTSUttModel um = new HTSUttModel();
        HTSParameterGeneration pdf2par = new HTSParameterGeneration();
        HTSVocoder par2speech = new HTSVocoder();
        AudioInputStream ais;
        
        /* Process label file of Mary context features and creates UttModel um.   */
        hmm_tts.processUttFromFile(feaFile, um, htsData);
          
        /* save realised durations in a lab file */
        FileWriter outputStream;
        outputStream = new FileWriter(durFile);
        outputStream.write(hmm_tts.getRealisedDurations());
        outputStream.close();
        
        /* save realised durations at state label in a slab file */
        float totalDur=0;
        int numStates=htsData.getCartTreeSet().getNumStates();
        outputStream = new FileWriter(durStateFile);
        outputStream.write("#\n");
        for(i=0; i<um.getNumModel(); i++){
          for(j=0; j<numStates; j++){
              totalDur += (um.getUttModel(i).getDur(j)*fperiodsec);
              if(j < (numStates-1))
                outputStream.write(totalDur + " 0 " + um.getUttModel(i).getPhoneName() + "\n");
              else
                outputStream.write(totalDur + " 1 " + um.getUttModel(i).getPhoneName() + "\n");   
            }         
        }
        outputStream.close();
        
        /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */ 
        boolean debug = true;  /* with debug=true it saves the generated parameters  
                                  f0 and mfcc in parFile.f0 and parFile.mfcc in Mary format. */
        pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData, parFile, debug);
          
        /* Synthesize speech waveform, generate speech out of sequence of parameter */
        ais = par2speech.htsMLSAVocoder(pdf2par, htsData);
     
        System.out.println("saving to file: " + outWavFile);
        File fileOut = new File(outWavFile);
         
        if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE,ais)) {
          AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fileOut);
        }
/*
        // uncomment to listen the files
          System.out.println("Calling audioplayer:");
          AudioPlayer player = new AudioPlayer(fileOut);
          player.start();  
          player.join();
          System.out.println("audioplayer finished...");
*/        
               
        }  // while files in testFiles   
        filesScanner.close();
         
      } catch (Exception e) {
          System.err.println("Exception: " + e.getMessage());
      }
      
    }  /* main method */
     
    
    /***
     * Calculate mfcc using SPTK, uses sox to convert wav-->raw
     * @throws IOException
     * @throws InterruptedException
     * @throws Exception
     */
    public void getSptkMfcc() throws IOException, InterruptedException, Exception{
       
       String inFile = "/project/mary/marcela/quality-control-experiment/slt/cmu_us_arctic_slt_a0001.wav";     
       String outFile = "/project/mary/marcela/quality-control-experiment/slt/cmu_us_arctic_slt_a0001.mfc";
       String tmpFile = "/project/mary/marcela/quality-control-experiment/slt/tmp.mfc";
       String tmpRawFile = "/project/mary/marcela/quality-control-experiment/slt/tmp.raw";
       String cmd;
       // SPTK parameters
       int fs = 16000;
       int frameLength = 400;
       int frameLengthOutput = 512;
       int framePeriod = 80;
       int mgcOrder = 24;
       int mgcDimension = 25;
       // Mary header parameters
       double ws = (frameLength/fs);   // window size in seconds 
       double ss = (framePeriod/fs);   // skip size in seconds
       
       // SOX and SPTK commands
       String sox = "/usr/bin/sox";
       String x2x = " /project/mary/marcela/sw/SPTK-3.1/bin/x2x";
       String frame = " /project/mary/marcela/sw/SPTK-3.1/bin/frame";
       String window = " /project/mary/marcela/sw/SPTK-3.1/bin/window";
       String mcep = " /project/mary/marcela/sw/SPTK-3.1/bin/mcep";
       String swab = "/project/mary/marcela/sw/SPTK-3.1/bin/swab";
       
      
       // convert the wav file to raw file with sox
       cmd = sox + " " + inFile + " " +  tmpRawFile;
       launchProc( cmd, "sox", inFile);
       
       System.out.println("Extracting MGC coefficients from " + inFile);
       
       cmd = x2x + " +sf " + tmpRawFile + " | " +
                    frame + " +f -l " + frameLength + " -p " + framePeriod + " | " +
                    window + " -l " + frameLength + " -L " + frameLengthOutput + " -w 1 -n 1 | " +
                    mcep + " -a 0.42 -m " + mgcOrder + "  -l " + frameLengthOutput + " | " +
                    swab + " +f > " + tmpFile;
       
       System.out.println("cmd=" + cmd);
       launchBatchProc( cmd, "getSptkMfcc", inFile );
       
       // Now get the data and add the Mary header
       int numFrames;
       DataInputStream mfcData=null;
       Vector <Float> mfc = new Vector<Float>();
      
       mfcData = new DataInputStream (new BufferedInputStream(new FileInputStream(tmpFile)));
       try {
         while(true){
           mfc.add(mfcData.readFloat());
         }         
       } catch (EOFException e) { }
       mfcData.close();

       numFrames = mfc.size();
       int numVectors = numFrames/mgcDimension;
       Mfccs mgc = new Mfccs(numVectors, mgcDimension);
       
       int k=0;
       for(int i=0; i<numVectors; i++){
         for(int j=0; j<mgcDimension; j++){
           mgc.mfccs[i][j] = mfc.get(k);
           k++;
         }
       }
       // Mary header parameters
       mgc.params.samplingRate = fs;         /* samplingRateInHz */
       mgc.params.skipsize     = (float)ss;  /* skipSizeInSeconds */
       mgc.params.winsize      = (float)ws;  /* windowSizeInSeconds */
       
       mgc.writeMfccFile(outFile);

        
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
        try {
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
        String tmpFile = "./tmp.bat";
       
        try {
            FileWriter tmp = new FileWriter(tmpFile);
            tmp.write(cmdLine);
            tmp.close();
            
            /* make it executable... */
            proctmp = Runtime.getRuntime().exec( "chmod +x "+tmpFile );
            proctmp.waitFor();
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
    
    
    public static void main(String[] args) throws Exception {
       /* configure log info */
       org.apache.log4j.BasicConfigurator.configure();
        
       HTSEngineTest test = new HTSEngineTest();
       
       // generate parameters out of a hsmm voice
       //test.generateParameters();
       
       // extract mfcc from a wav file using sptk
       //test.getSptkMfcc();
       
       // Synthesis with external duration and f0
       test.synthesisWithExternalProsodySpecification();
      
     }
     
    
}
