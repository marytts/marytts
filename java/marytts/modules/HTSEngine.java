
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

package marytts.modules;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;


import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.htsengine.HMMData;
import marytts.htsengine.HMMVoice;
import marytts.htsengine.HTSModel;
import marytts.htsengine.HTSParameterGeneration;
import marytts.htsengine.CartTreeSet;
import marytts.htsengine.HTSUttModel;
import marytts.htsengine.HTSVocoder;
import marytts.htsengine.PhoneTranslator;
import marytts.modules.synthesis.Voice;
import marytts.signalproc.analysis.F0ReaderWriter;
import marytts.util.data.audio.AudioPlayer;

import org.apache.log4j.Logger;
import org.jsresources.AppendableSequenceAudioInputStream;


/**
 * HTSEngine: a compact HMM-based speech synthesis engine.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marc Schr&ouml;der, Marcela Charfuelan 
 */
public class HTSEngine extends InternalModule
{
    private Logger logger = Logger.getLogger("HTSEngine");
    private String realisedDurations;
    
    public HTSEngine()
    {
        super("HTSEngine",
              MaryDataType.TARGETFEATURES,
              MaryDataType.AUDIO,
              null);
    }

    /**
     * This module is actually tested as part of the HMMSynthesizer test,
     * for which reason this method does nothing.
     */
    public synchronized void powerOnSelfTest() throws Error
    {
    }
    
    
    /**
     * when calling this function HMMVoice must be initialised already.
     * that is TreeSet and ModelSet must be loaded already.
     * @param d
     * @return
     * @throws Exception
     */
    public MaryData process(MaryData d)
    throws Exception
    {
        /** The utterance model, um, is a Vector (or linked list) of Model objects. 
         * It will contain the list of models for current label file. */
        HTSUttModel um = new HTSUttModel();
        HTSParameterGeneration pdf2par = new HTSParameterGeneration();
        HTSVocoder par2speech = new HTSVocoder();
        AudioInputStream ais;
              
        Voice v = d.getDefaultVoice(); /* This is the way of getting a Voice through a MaryData type */
        assert v instanceof HMMVoice;
        HMMVoice hmmv = (HMMVoice)v;
              
        String context = d.getPlainText();
        //System.out.println("TARGETFEATURES:" + TARGETFEATURES);
              
        /* Process label file of Mary context features and creates UttModel um */
        processUtt(context, um, hmmv.getHMMData());

        /* Process UttModel */
        /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */  
        boolean debug = false;  /* so it does not save the generated parameters. */
        pdf2par.htsMaximumLikelihoodParameterGeneration(um, hmmv.getHMMData(),"", debug);
    
        
        /* set parameters for generation: f0Std, f0Mean and length, default values 1.0, 0.0 and 0.0 */
        /* These values are fixed in HMMVoice */
        
        /* Process generated parameters */
        /* Synthesize speech waveform, generate speech out of sequence of parameters */
        ais = par2speech.htsMLSAVocoder(pdf2par, hmmv.getHMMData());
       
        MaryData output = new MaryData(outputType(), d.getLocale());
        if (d.getAudioFileFormat() != null) {
            output.setAudioFileFormat(d.getAudioFileFormat());
            if (d.getAudio() != null) {
               // This (empty) AppendableSequenceAudioInputStream object allows a 
               // thread reading the audio data on the other "end" to get to our data as we are producing it.
                assert d.getAudio() instanceof AppendableSequenceAudioInputStream;
                output.setAudio(d.getAudio());
            }
        }     
       output.appendAudio(ais);
       
       /* include correct durations in MaryData output */
       output.setPlainText(um.getRealisedAcoustParams());
              
       return output;
        
    }
   
    /* For stand alone testing. */
    public AudioInputStream processStr(String context, HMMData htsData)
    throws Exception
    {
        HTSUttModel um = new HTSUttModel();
        HTSParameterGeneration pdf2par = new HTSParameterGeneration();
        HTSVocoder par2speech = new HTSVocoder();
        AudioInputStream ais;
        
        /* htsData contains:
         * data in the configuration file, .pdf file names and other parameters. 
         * After InitHMMData it contains TreeSet ts and ModelSet ms 
         * ModelSet: Contains the .pdf's (means and variances) for dur, lf0, mcp, str and mag
         *           these are all the HMMs trained for a particular voice 
         * TreeSet: Contains the tree-xxx.inf, xxx: dur, lf0, mcp, str and mag 
         *          these are all the trees trained for a particular voice. */
 
        
        logger.info("TARGETFEATURES:" + context);
        
        /* Process label file of Mary context features and creates UttModel um */
        processUtt(context, um, htsData);

        /* Process UttModel */
        /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */ 
        boolean debug = false;  /* so it does not save the generated parameters. */
        pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData, "", debug);
    
        /* Process generated parameters */
        /* Synthesize speech waveform, generate speech out of sequence of parameters */
        ais = par2speech.htsMLSAVocoder(pdf2par, htsData);
        
       return ais;
        
    }
  
 
    
    /** Reads the Label file, the file which contains the Mary context features,
     *  creates an scanner object and calls _ProcessUtt
     * @param LabFile
     */
    public void processUttFromFile(String LabFile, HTSUttModel um, HMMData htsData) throws Exception { 
        Scanner s = null;
        try {    
            /* parse text in label file */
            s = new Scanner(new BufferedReader(new FileReader(LabFile)));
            _processUtt(s,um,htsData,htsData.getCartTreeSet());
              
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e.getMessage());
            
        } finally {
            if (s != null)
                s.close();
        }           
    }
    
    /** Creates a scanner object with the Mary context features contained in Labtext
     * and calls _ProcessUtt
     * @param LabText
     */
    public void processUtt(String LabText, HTSUttModel um, HMMData htsData) throws Exception {
        Scanner s = null;
        try {
          s = new Scanner(LabText);
         _processUtt(s, um, htsData, htsData.getCartTreeSet());
        } finally {
            if (s != null)
              s.close();
        }   
    }
    

    
    /** Parse Mary context features. 
     * For each triphone model in the file, it creates a Model object in a linked list of 
     * Model objects -> UttModel um 
     * It also estimates state duration from state duration model (Gaussian).
     * For each model in the vector, the mean and variance of the DUR, LF0, MCP, STR and MAG 
     * are searched in the ModelSet and copied in each triphone model.   */
    private void _processUtt(Scanner s, HTSUttModel um, HMMData htsData, CartTreeSet cart)
      throws Exception {     
        int i, mstate,frame;
        HTSModel m;                   /* current model, corresponds to a line in label file */
        String nextLine;
        double diffdurOld = 0.0;
        double diffdurNew = 0.0;
        double mean = 0.0;
        double var = 0.0;
        float fperiodmillisec = ((float)htsData.getFperiod() / (float)htsData.getRate()) * 1000;
        float fperiodsec = ((float)htsData.getFperiod() / (float)htsData.getRate());
        Integer dur;
        boolean firstPh = true; 
        boolean lastPh = false;
        realisedDurations = "#\n";
        Float durSec;
        Integer numLab=0;
        FeatureVector fv;
        FeatureDefinition feaDef;
        feaDef = htsData.getFeatureDefinition();
        
       /* skip mary context features definition */
        while (s.hasNext()) {
          nextLine = s.nextLine(); 
          if (nextLine.trim().equals("")) break;
        }
        /* skip until byte values */
        while (s.hasNext()) {
            nextLine = s.nextLine(); 
          if (nextLine.trim().equals("")) break;
        }
           
        /* parse byte values  */
        i=0;
        while (s.hasNext()) {
            nextLine = s.nextLine();
            //System.out.println("STR: " + nextLine);
            
            fv = feaDef.toFeatureVector(0, nextLine);
            um.addUttModel(new HTSModel(cart.getNumStates()));            
            m = um.getUttModel(i);
            /* this function also sets the phoneme name, the phoneme between - and + */
            m.setName(fv.toString(), fv.getFeatureAsString(0, feaDef));  
            
            if(!(s.hasNext()) )
              lastPh = true;

            // Estimate state duration from state duration model (Gaussian)                       
            if (htsData.getLength() == 0.0 ) {
                diffdurNew = cart.searchDurInCartTree(m, fv, feaDef, firstPh, lastPh, 
                             htsData.getRho(), diffdurOld, htsData.getDurationScale());
                
                m.setTotalDurMillisec((int)(fperiodmillisec * m.getTotalDur()));               
                diffdurOld = diffdurNew;
                um.setTotalFrame(um.getTotalFrame() + m.getTotalDur());
                durSec = um.getTotalFrame() * fperiodsec;
                //realisedDurations += durSec.toString() +  " 125 " + HTSContextTranslator.replaceBackTrickyPhones(m.getPhoneName()) + "\n";
                realisedDurations += durSec.toString() +  " " + numLab.toString() + " " + PhoneTranslator.replaceBackTrickyPhones(m.getPhoneName()) + "\n";
                numLab++;
                dur = m.getTotalDurMillisec();
                um.concatRealisedAcoustParams(m.getPhoneName() + " " + dur.toString() + "\n");
                //System.out.println("phoneme=" + PhoneTranslator.replaceBackTrickyPhones(m.getPhoneName()) + " dur=" + m.getTotalDur() +" durTotal=" + um.getTotalFrame());
            } /* else : when total length of generated speech is specified (not implemented yet) */
            
            /* Find pdf for LF0, this function sets the pdf for each state. */ 
            cart.searchLf0InCartTree(m, fv, feaDef, htsData.getUV());
     
            /* Find pdf for MCP, this function sets the pdf for each state.  */
            cart.searchMcpInCartTree(m, fv, feaDef);

            /* Find pdf for strengths, this function sets the pdf for each state.  */
            if(htsData.getTreeStrFile() != null)
              cart.searchStrInCartTree(m, fv, feaDef);
            
            /* Find pdf for Fourier magnitudes, this function sets the pdf for each state.  */
            if(htsData.getTreeMagFile() != null)
              cart.searchMagInCartTree(m, fv, feaDef);
            
            /* increment number of models in utterance model */
            um.setNumModel(um.getNumModel()+1);
            /* update number of states */
            um.setNumState(um.getNumState() + cart.getNumStates());
            i++;
            
            if(firstPh)
              firstPh = false;
        }

        for(i=0; i<um.getNumUttModel(); i++){
            m = um.getUttModel(i);                  
            for(mstate=0; mstate<cart.getNumStates(); mstate++)
                for(frame=0; frame<m.getDur(mstate); frame++) 
                    if(m.getVoiced(mstate))
                        um.setLf0Frame(um.getLf0Frame() +1);
            //System.out.println("Vector m[" + i + "]=" + m.getName()); 
        }

        logger.info("Number of models in sentence numModel=" + um.getNumModel() + "  Total number of states numState=" + um.getNumState());
        logger.info("Total number of frames=" + um.getTotalFrame() + "  Number of voiced frames=" + um.getLf0Frame());    
        
    } /* method _ProcessUtt */

    /** 
     * Stand alone testing using a TARGETFEATURES file as input. 
     * @param args
     * @throws IOException
     */
    public static void mainSingleFile(String[] args) throws IOException, InterruptedException{
       
      int i, j;  
      /* configure log info */
      org.apache.log4j.BasicConfigurator.configure();

      /* To run the stand alone version of HTSEngine, it is necessary to pass a configuration
       * file. It can be one of the hmm configuration files in MARY_BASE/conf/*hmm*.config 
       * The input for creating a sound file is a TARGETFEATURES file in MARY format, there
       * is an example indicated in the configuration file as well.
       * For synthesising other text please generate first a TARGETFEATURES file with the MARY system
       * save it in a file and use it as feaFile. */
      HTSEngine hmm_tts = new HTSEngine();
      
      /* htsData contains:
       * Data in the configuration file, .pdf, tree-xxx.inf file names and other parameters. 
       * After initHMMData it contains TreeSet ts and ModelSet ms 
       * ModelSet: Contains the .pdf's (means and variances) for dur, lf0, mcp, str and mag
       *           these are all the HMMs trained for a particular voice 
       * TreeSet: Contains the tree-xxx.inf, xxx: dur, lf0, mcp, str and mag 
       *          these are all the trees trained for a particular voice. */
      HMMData htsData = new HMMData();
      
      /* For initialise provide the name of the hmm voice and the name of its configuration file,*/
       
      String MaryBase    = "/project/mary/marcela/openmary/"; /* ARY_BASE directory.*/
      String voiceName   = "hsmm-slt";                        /* voice name */
      String voiceConfig = "english-hsmm-slt.config";         /* voice configuration file name. */        
      String durFile     = MaryBase + "tmp/tmp.lab";          /* to save realised durations in .lab format */
      String parFile     = MaryBase + "tmp/tmp";              /* to save generated parameters tmp.mfc and tmp.f0 in Mary format */
      String outWavFile  = MaryBase + "tmp/tmp.wav";          /* to save generated audio file */
      
      htsData.initHMMData(voiceName, MaryBase, voiceConfig);
      
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
               
      /** Example of context features file */
      //String feaFile = htsData.getFeaFile();
      //String feaFile = "/project/mary/marcela/openmary/tmp/welcome.fea";
      //String feaFile = "/project/mary/marcela/openmary/lib/voices/hsmm-slt/cmu_us_arctic_slt_a0001.pfeats.new";
      String feaFile = "/project/mary/marcela/openmary/lib/voices/hsmm-slt/cmu_us_arctic_slt_a0001.pfeats";
      
      try {
          /* Process Mary context features file and creates UttModel um, a linked             
           * list of alt the models in the utterance. For each model, it searches in each tree, dur,   
           * cmp, etc, the pdf index that corresponds to a triphone context feature and with           
           * that index retrieves from the ModelSet the mean and variance for each state of the HMM.   */
          hmm_tts.processUttFromFile(feaFile, um, htsData);
        
          /* save realised durations in a lab file */             
          FileWriter outputStream = new FileWriter(durFile);
          outputStream.write(hmm_tts.realisedDurations);
          outputStream.close();

          /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */
          /* the generated parameters will be saved in tmp.mfc and tmp.f0, including Mary header. */
          boolean debug = true;  /* so it save the generated parameters in parFile */
          pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData, parFile, debug);
          
          /* Synthesize speech waveform, generate speech out of sequence of parameters */
          ais = par2speech.htsMLSAVocoder(pdf2par, htsData);
     
          System.out.println("saving to file: " + outWavFile);
          System.out.println("realised durations saved to file: " + durFile);
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
    

    
    /** 
     * Stand alone testing using a TARGETFEATURES list of files as input. 
     * @param args
     * @throws IOException
     */
    public static void mainList(String[] args) throws IOException, InterruptedException{
       
      int i, j;  
      /* configure log info */
      org.apache.log4j.BasicConfigurator.configure();

      /* To run the stand alone version of HTSEngine, it is necessary to pass a configuration
       * file. It can be one of the hmm configuration files in MARY_BASE/conf/*hmm*.config 
       * The input for creating a sound file is a label file in HTSCONTEXT format, there
       * is an example indicated in the configuration file as well, if one wants to 
       * change this example file, another HTSCONTEX file, for whatever text, can be generated 
       * and saved in a file with MARY system.
       * The output sound file is located in MARY_BASE/tmp/tmp.wav */
      HTSEngine hmm_tts = new HTSEngine();
      
      /* htsData contains:
       * Data in the configuration file, .pdf, tree-xxx.inf file names and other parameters. 
       * After initHMMData it contains TreeSet ts and ModelSet ms 
       * ModelSet: Contains the .pdf's (means and variances) for dur, lf0, mcp, str and mag
       *           these are all the HMMs trained for a particular voice 
       * TreeSet: Contains the tree-xxx.inf, xxx: dur, lf0, mcp, str and mag 
       *          these are all the trees trained for a particular voice. */
      HMMData htsData = new HMMData();
      
      /* For initialise provide the name of the hmm voice and the name of its configuration file,
       * also indicate the name of your MARY_BASE directory.*/
      String MaryBase = "/project/mary/marcela/openmary/";
      String voice = "";
      String outputDir, hmmSourceDir, origTargetDir, outWavFile, testFilesList;
    
      // Mel-Cepstrum voices
      //voice = "hmm-mag-slt";
      voice = "hsmm-slt";   //this is actually "hsmm-24-mel-cepstrum";            
      // LSP voices
      //voice = "hsmm-12-lsp";
      //voice = "hsmm-40-lsp";
      //voice = "hsmm-20-lsp";
      //voice = "hsmm-20-mel-lsp";
      //voice = "hsmm-20-mgc-lsp";
      
      htsData.initHMMData(voice, MaryBase, "english-" + voice + ".config");
      
      // for LSP voices do not use GV
      htsData.setUseGV(true);
      htsData.setUseMixExc(true);
      htsData.setUseFourierMag(false);
      
      String testFile, feaFile, parFile, durFile, mgcModifiedFile;
      Scanner testFiles;
      //String flab;
      String labDir = "/project/mary/marcela/hmmVoiceConversion/lab_htscontext_all/";
      String contextFeaDir = "/project/mary/marcela/HMM-voices/MARY-PATCH-2.1/HTS-demo_CMU-ARCTIC-SLT-mary-24mgc/phonefeatures/";
      //outputDir = "/project/mary/marcela/hmmVoiceConversion/lsp_13Dimensional/";
      outputDir = "/project/mary/marcela/quality-control-experiment/";
        
      /* list of files to train */      
      //testFilesList = "/project/mary/marcela/hmmVoiceConversion/phonefeatures-100-list.txt";
      //hmmSourceDir = "hmmSource/train_100/";
      //origTargetDir = "origTarget/train_100/";
      testFilesList = "/project/mary/marcela/quality-control-experiment/phonefeatures-list.txt";
      hmmSourceDir = "hmmSource/";
      origTargetDir = "origTarget/";
     

      /* generate files out of HMMs */
      try {
      testFiles = new Scanner(new BufferedReader(new FileReader(testFilesList))); 
      while (testFiles.hasNext()) {
        testFile = testFiles.nextLine();
 
        feaFile    = contextFeaDir + testFile + ".pfeats";
        parFile    = outputDir + hmmSourceDir + testFile;            /* generated parameters mfcc and f0, Mary format */
        durFile    = outputDir + hmmSourceDir + testFile + ".lab";   /* realised durations */
        outWavFile = outputDir + hmmSourceDir + testFile + ".wav";   /* generated wav file */
       
  
        /** The utterance model, um, is a Vector (or linked list) of Model objects. 
         * It will contain the list of models for current label file. */
        HTSUttModel um = new HTSUttModel();
        HTSParameterGeneration pdf2par = new HTSParameterGeneration();
        HTSVocoder par2speech = new HTSVocoder();
        AudioInputStream ais;
        
        /* Process label file of Mary context features and creates UttModel um.   */
        hmm_tts.processUttFromFile(feaFile, um, htsData);
          
        /* save realised durations in a lab file */
        FileWriter outputStream = new FileWriter(durFile);
        outputStream.write(hmm_tts.realisedDurations);
        outputStream.close();

        /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */ 
        boolean debug = true;  /* so it save the generated parameters in parFile */
        pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData, parFile, debug);
          
        /* Synthesize speech waveform, generate speech out of sequence of parameters */
        ais = par2speech.htsMLSAVocoder(pdf2par, htsData);
     
        System.out.println("saving to file: " + outWavFile);
        File fileOut = new File(outWavFile);
         
        if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE,ais)) {
          AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fileOut);
        }
/*
          System.out.println("Calling audioplayer:");
          AudioPlayer player = new AudioPlayer(fileOut);
          player.start();  
          player.join();
          System.out.println("audioplayer finished...");
*/        
               
        }  // while files in testFiles   
         testFiles.close();
         
      } catch (Exception e) {
          System.err.println("Exception: " + e.getMessage());
      }
      
    }  /* main method */
    
    
 
    /** 
     * Stand alone testing using an TARGETFEATURES file as input. 
     * @param args
     * @throws IOException
     */
    public static void mainTesting(String[] args) throws IOException, InterruptedException{
       
      int i, j;  
      /* configure log info */
      org.apache.log4j.BasicConfigurator.configure();

      /* To run the stand alone version of HTSEngine, it is necessary to pass a configuration
       * file. It can be one of the hmm configuration files in MARY_BASE/conf/*hmm*.config 
       * The input for creating a sound file is a label file in TARGETFEATURES format, there
       * is an example indicated in the configuration file as well, if one wants to 
       * change this example file, another TARGETFEATURES file, for whatever text, can be generated 
       * and saved in a file with MARY system.
       * The output sound file is located in MARY_BASE/tmp/tmp.wav */
      HTSEngine hmm_tts = new HTSEngine();
      
      /* htsData contains:
       * Data in the configuration file, .pdf, tree-xxx.inf file names and other parameters. 
       * After initHMMData it contains TreeSet ts and ModelSet ms 
       * ModelSet: Contains the .pdf's (means and variances) for dur, lf0, mcp, str and mag
       *           these are all the HMMs trained for a particular voice 
       * TreeSet: Contains the tree-xxx.inf, xxx: dur, lf0, mcp, str and mag 
       *          these are all the trees trained for a particular voice. */
      HMMData htsData = new HMMData();
      
      /* For initialise provide the name of the hmm voice and the name of its configuration file,
       * also indicate the name of your MARY_BASE directory.*/
      String MaryBase = "/project/mary/marcela/openmary/";
      String voice = "";
      String outputDir, outWavFile, testFilesList;
    
      // Mel-Cepstrum voices
      //voice = "hmm-mag-slt";
      //voice = "hmm-slt";
      voice = "hsmm-slt";  // is the same as: "hsmm-24-mel-cepstrum";            
      // LSP voices
      //voice = "hsmm-12-lsp";
      //voice = "hsmm-40-lsp";
      //voice = "hsmm-20-lsp";
      //voice = "hsmm-20-mel-lsp";
      //voice = "hsmm-20-mgc-lsp";
      
      htsData.initHMMData(voice, MaryBase, "english-" + voice + ".config");
      
      // for LSP voices do not use GV
      htsData.setUseGV(true);
      htsData.setUseMixExc(true);
      htsData.setUseFourierMag(false);
       
      String feaFile, parFile, durFile, mgcModifiedFile;
      Scanner testFiles;
      String flab;
      String labDir = "/project/mary/marcela/hmmVoiceConversion/lab_htscontext_all/";
      
        //feaFile         = "cmu_us_arctic_slt_b0503";
        feaFile         = "cmu_us_arctic_slt_a0001";
        //flab            = labDir + feaFile + ".lab";
        flab = htsData.getFeaFile();
        
        //outputDir       = "/project/mary/marcela/hmmVoiceConversion/lsp_13Dimensional/output/gmmF_500_128/isSrc0_smooth0_0_mixes128_prosody1x2/";
        //outputDir       = "/project/mary/marcela/hmmVoiceConversion/lsp_13Dimensional/output/gmmF_100_64/isSrc0_smooth0_0_mixes64_prosody1x2/";
        //outputDir       = "/project/mary/marcela/hmmVoiceConversion/hsmmMfcc_25Dimensional/gmmF_100_8/isSrc0_smooth0_0_mixes8_prosody1x2/";
        outputDir       = "/project/mary/marcela/openmary/tmp/";
        mgcModifiedFile = outputDir + feaFile + "_output.mgc";
        //outWavFile      = outputDir + feaFile + "_output.wav";        
   
        outWavFile      = outputDir + feaFile + "_output.wav";
        //outWavFile        = "/project/mary/marcela/hmmVoiceConversion/lsp_13Dimensional/hmmSource/train_100/cmu_us_arctic_slt_a0001-test.wav";
        //outWavFile      = "/project/mary/marcela/openmary/tmp/tmp.wav";
        //outWavFile      = "/project/mary/marcela/hmm-gen-experiment/MLSA-MGLSA/hsmm-20-lsp/slt-lpc-vocoder-gen-par-gen-exc.wav";
        
        parFile           = outputDir + feaFile + "_output";
        //parFile           = "/project/mary/marcela/hmmVoiceConversion/lsp_13Dimensional/hmmSource/train_100/cmu_us_arctic_slt_a0001-test";
        //parFile         = outputDir + "gen" + feaFile + ".mgc";
        
        //String f0File = "/project/mary/marcela/quality-control-experiment/hmmSource/cmu_us_arctic_slt_a0001.ptc";
        String f0File = "/project/mary/marcela/HMM-voices/MARY-PATCH-2.1/HTS-demo_CMU-ARCTIC-SLT-mary-24mgc/data/lf0/cmu_us_arctic_slt_a0001.ptc";
        F0ReaderWriter f0 = new F0ReaderWriter(f0File);

       // mgcModifiedFile = outputDir + "original_parameters/" + feaFile + ".mgc";
       // outWavFile = outputDir + "original_parameters/" + feaFile + ".wav";
       // mgcModifiedFile = outputDir + "hmm_gen_parameters/" + feaFile + ".mgc";
       // outWavFile = outputDir + "hmm_gen_parameters/" + feaFile + ".wav";
        
       // mgcModifiedFile = outputDir + feaFile + "_output.mgc";
       // outWavFile = outputDir + "hsmm-lsp20-gmmF_100_64/isSrc1_smooth0_0_mixes64_prosody1x2/" + feaFile + "_output.wav";
        
       // mgcModifiedFile = outputDir + "hsmmMfcc_25Dimensional/gmmF_100_8/isSrc1_smooth0_0_mixes8_prosody1x2/" + feaFile + "_output.mgc";
       // outWavFile = outputDir + "hsmmMfcc_25Dimensional/gmmF_100_8/isSrc1_smooth0_0_mixes8_prosody1x2/" + feaFile + "_output.wav";
        
        
        /** The utterance model, um, is a Vector (or linked list) of Model objects. 
         * It will contain the list of models for current label file. */
        HTSUttModel um = new HTSUttModel();
        HTSParameterGeneration pdf2par = new HTSParameterGeneration();        
        HTSVocoder par2speech = new HTSVocoder();
        AudioInputStream ais;
        
        
      /** Example of HTSCONTEXT_EN context features file */
     // flab = htsData.getLabFile();
        /* flab now is a mary contextfeatures file! but this file should be generated 
         * with next, next_next, prev and prev_prev phonemes features  ...*/
        //flab = "/project/mary/marcela/openmary/tmp/tmp.fea";
        flab = "/project/mary/marcela/openmary/lib/voices/hsmm-slt/cmu_us_arctic_slt_a0001.pfeats";
        
        /* convert MfccRaw2Mfcc */

      try {
          /* Process label file of Mary context features and creates UttModel um, a linked             
           * list of alt the models in the utterance. For each model, it searches in each tree, dur,   
           * cmp, etc, the pdf index that corresponds to a triphone context feature and with           
           * that index retrieves from the ModelSet the mean and variance for each state of the HMM.   */
          hmm_tts.processUttFromFile(flab, um, htsData);
          /* save realised durations in a lab file */
          durFile="/project/mary/marcela/openmary/tmp/tmp.lab";
          FileWriter outputStream = new FileWriter(durFile);
          outputStream.write(hmm_tts.realisedDurations);
          outputStream.close();

          /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */ 
          boolean debug = true;  /* so it save the generated parameters in parFile */
          pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData, parFile, debug);
          
//////////////// load modified parameters mgc data
 /*         
          //mgcModifiedFile = "/project/mary/marcela/hmmVoiceConversion/lspRaw_13Dimensional/test_files/original_parameters/cmu_us_arctic_slt_b0501.mfc";
          //mgcModifiedFile = "/project/mary/marcela/HMM-voices/MARY-PATCH-2.1/HTS-demo_CMU-ARCTIC-SLT-mary-12lsp/data/mgc/cmu_us_arctic_slt_a0001.mfc";
          //mgcModifiedFile = "/project/mary/marcela/HMM-voices/MARY-PATCH-2.1/HTS-demo_CMU-ARCTIC-SLT-mary-12lsp/data/tmp.mfc";
          //mgcModifiedFile = "/project/mary/marcela/hmmVoiceConversion/lsp_13Dimensional/hmmSource/train_100/cmu_us_arctic_slt_a0001-test.mfc";
          mgcModifiedFile = "/project/mary/marcela/HMM-voices/MARY-PATCH-2.1/HTS-demo_CMU-ARCTIC-SLT-mary-24mgc/data/mgc/cmu_us_arctic_slt_a0001.mfc";
          
          Mfccs mgc = new Mfccs(mgcModifiedFile);
                   
          System.out.println("loaded mgc from: " + mgcModifiedFile);
          DataInputStream mcepData = new DataInputStream (new BufferedInputStream(new FileInputStream(mgcModifiedFile)));
          int frm = mcepData.readInt();
          int dim = mcepData.readInt();
          float winsize = mcepData.readFloat();
          float skipsize = mcepData.readFloat();
          int sr = mcepData.readInt();
          //ler.writeInt(numfrm);
          //ler.writeInt(dimension);
          //ler.writeFloat(winsize);
          //ler.writeFloat(skipsize);
          //ler.writeInt(samplingRate);
          double val;
          for(i=0; i<frm; i++){ 
            for(j=0; j<dim; j++){
              val = mcepData.readDouble();
              System.out.println(val);
            }
          }
          mcepData.close();
   */     
/*         
          System.out.println("loaded mgc from: " + mgcModifiedFile);
          DataInputStream mcepData = new DataInputStream (new BufferedInputStream(new FileInputStream(mgcModifiedFile)));
          for(i=0; i<um.getTotalFrame(); i++){ 
            for(j=0; j<pdf2par.getMcepOrder(); j++)
              pdf2par.getMcepPst().setPar(i, j, mcepData.readFloat());
          }
          mcepData.close();
 */         
///////////////

          /* Synthesize speech waveform, generate speech out of sequence of parameters */
//          par2speech.setUseLpcVocoder(true);
          ais = par2speech.htsMLSAVocoder(pdf2par, htsData);
     
          //System.out.println("saving to file: " + MaryBase + "tmp/" + voice + ".wav");
          //File fileOut = new File(MaryBase + "tmp/" + voice + ".wav");
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
    

    
    
    public static void main(String[] args) throws IOException, InterruptedException{
        
       // mainTesting(args); 
       mainSingleFile(args);     
       //mainList(args);
    }

}  /* class HTSEngine*/

