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

/***
 * Several functions for running the htsEngine or other components stand alone
 * @author Marcela Charfuelan
 *
 */
public class HTSEngineTest {

    /** 
     * Stand alone testing using a TARGETFEATURES list of files as input. 
     * @param args
     * @throws IOException
     */
    public void generateParameters(String[] args) throws IOException, InterruptedException{
       
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
        
       HTSEngineGUI test = new HTSEngineGUI();
       
       // generate parameters out of a hsmm voice
       test.generateParameters(args);
       
       // extract mfcc from a wav file using sptk
       test.getSptkMfcc();
      
     }
     
    
}
