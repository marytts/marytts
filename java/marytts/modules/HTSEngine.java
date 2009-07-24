
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
import java.util.Vector;

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
import marytts.htsengine.HTSEngineTest.PhonemeDuration;
import marytts.modules.synthesis.Voice;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.util.data.audio.AppendableSequenceAudioInputStream;
import marytts.util.data.audio.AudioPlayer;

import org.apache.log4j.Logger;

import marytts.signalproc.analysis.*;


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
    private String realisedDurations;  // HMM realised duration to be save in a file
    private boolean phoneAlignmentForDurations;
    private boolean stateAlignmentForDurations=false;   
    private Vector<PhonemeDuration> alignDur=null;  // list of external duration per phone for alignment
     
    
    public String getRealisedDurations(){ return realisedDurations; }
    public boolean getPhonemeAlignmentForDurations(){ return phoneAlignmentForDurations; }
    public boolean getStateAlignmentForDurations(){ return stateAlignmentForDurations;}    
    public Vector<PhonemeDuration> getAlignDurations(){ return alignDur; }
  
    public void setRealisedDurations(String str){ realisedDurations=str; }
    public void setStateAlignmentForDurations(boolean bval){ stateAlignmentForDurations=bval; }
    public void setPhonemeAlignmentForDurations(boolean bval){ phoneAlignmentForDurations=bval; }    
    public void setAlignDurations(Vector<PhonemeDuration> val){ alignDur = val; }
     
    public HTSEngine()
    {
        super("HTSEngine",
              MaryDataType.TARGETFEATURES,
              MaryDataType.AUDIO,
              null);
        phoneAlignmentForDurations=false;
        stateAlignmentForDurations=false;
        alignDur = null;       
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
        int i, mstate,frame, k, nf;
        HTSModel m;                   /* current model, corresponds to a line in label file */
        String nextLine;
        double diffdurOld = 0.0;
        double diffdurNew = 0.0;
        double mean = 0.0;
        double var = 0.0;
        double f;
        int alignDurSize=0;
        float fperiodmillisec = ((float)htsData.getFperiod() / (float)htsData.getRate()) * 1000;
        float fperiodsec = ((float)htsData.getFperiod() / (float)htsData.getRate());
        Integer dur;
        boolean firstPh = true; 
        boolean lastPh = false;
        realisedDurations = "#\n";
        Float durSec;
        Integer numLab=0;
        FeatureVector fv;
        FeatureDefinition feaDef = htsData.getFeatureDefinition();
        
        
       /* Skip mary context features definition */
        while (s.hasNext()) {
          nextLine = s.nextLine(); 
          if (nextLine.trim().equals("")) break;
        }
        /* skip until byte values */
        int numLines=0;
        while (s.hasNext()) {
          nextLine = s.nextLine();          
          if (nextLine.trim().equals("")) break;
          numLines++;
        }
           
        
        if( phoneAlignmentForDurations || htsData.getUseUnitDurationContinuousFeature()) {
          if( alignDur != null ) { 
            alignDurSize = alignDur.size();
            logger.info("Using external prosody for duration: using phone alignment for duration from external file.");  
          } else {
            logger.info("Using external prosody for duration: using phone alignment for duration from ContinuousFeatureProcessors.");  
            //throw new Exception("No vector of durations for phone alignment.");
          }
        } else
            logger.info("Estimating state durations from (Gaussian) state duration model.");
        
        /* Parse byte values  */
        i=0;
        while (s.hasNext()) {
            nextLine = s.nextLine();
            //System.out.println("STR: " + nextLine);     
            
            fv = feaDef.toFeatureVector(0, nextLine);
            um.addUttModel(new HTSModel(cart.getNumStates()));            
            m = um.getUttModel(i);
            /* this function also sets the phone name, the phone between - and + */
            m.setName(fv.toString(), fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef));
            
            /*System.out.println("context: " + fv.getFeatureAsString(feaDef.getFeatureIndex("prev_prev_phone"), feaDef) + 
                                     " " + fv.getFeatureAsString(feaDef.getFeatureIndex("prev_phone"), feaDef) +
                                     " " + fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef) + 
                                     " " + fv.getFeatureAsString(feaDef.getFeatureIndex("next_phone"), feaDef) +
                                     " " + fv.getFeatureAsString(feaDef.getFeatureIndex("next_next_phone"), feaDef) +
                                     "  DUR= " + fv.getContinuousFeature(feaDef.getFeatureIndex("unit_duration")) +
                                     "  LF0= " + fv.getContinuousFeature(feaDef.getFeatureIndex("unit_logf0")) );
            */
            if (htsData.getUseUnitDurationContinuousFeature()) {
                m.setUnit_logF0(fv.getContinuousFeature(feaDef.getFeatureIndex("unit_logf0")));
                m.setUnit_logF0delta(fv.getContinuousFeature(feaDef.getFeatureIndex("unit_logf0delta")));
            }
            
            if(!(s.hasNext()) )
              lastPh = true;

            // Determine state-level duration                      
            if( phoneAlignmentForDurations || htsData.getUseUnitDurationContinuousFeature() ) {  // use phone alignment for duration 
              diffdurNew = cart.searchDurInCartTree(m, fv, htsData, firstPh, lastPh, diffdurOld);
              nf=0;
              // get the sum of state durations
              for(k=0; k<htsData.getCartTreeSet().getNumStates(); k++)
                nf += m.getDur(k);
                
              // get the external duration
              if( alignDur != null) { 
                // check if the external phone corresponds to the current  
                if( alignDur.get(i).getPhoneme().contentEquals(m.getPhoneName()) ){
                  if(i < alignDurSize )
                    f = alignDur.get(i).getDuration()/(fperiodsec*nf);
                  else
                    throw new Exception("The number of durations provided for phone alignment (" + alignDurSize +
                        ") is less than the number of feature vectors, so far (" + um.getNumUttModel() + ").");
                } else {
                  throw new Exception("External phone: " + alignDur.get(i).getPhoneme() +
                         " does not correspond to current feature vector phone: " + m.getPhoneName() );
                }
              } else {  // if no alignDur use ContinuousFeatureProcessors unit_duration float
                 f = fv.getContinuousFeature(feaDef.getFeatureIndex("unit_duration"))/(fperiodsec*nf);; 
              }
              
              m.setTotalDur(0);
              for(k=0; k<htsData.getCartTreeSet().getNumStates(); k++){
                nf = (int)(f*m.getDur(k)+0.5);
                if( nf <= 0 )
                  nf = 1;
                m.setDur(k, nf);
                m.setTotalDur(m.getTotalDur() + m.getDur(k)); 
                //System.out.println("   state: " + k + " durNew=" + m.getDur(k));       
              }  
              um.setTotalFrame(um.getTotalFrame() + m.getTotalDur());
              
            } else if(stateAlignmentForDurations) {  // use state alignment for duration
              // Not implemented yet  
                
            } else { // Estimate state duration from state duration model (Gaussian)                 
                diffdurNew = cart.searchDurInCartTree(m, fv, htsData, firstPh, lastPh, diffdurOld);  
                um.setTotalFrame(um.getTotalFrame() + m.getTotalDur());             
            }
            
            // Set realised durations 
            m.setTotalDurMillisec((int)(fperiodmillisec * m.getTotalDur()));               
            diffdurOld = diffdurNew;            
            durSec = um.getTotalFrame() * fperiodsec;
            //realisedDurations += durSec.toString() +  " 125 " + HTSContextTranslator.replaceBackTrickyPhones(m.getPhoneName()) + "\n";
            realisedDurations += durSec.toString() +  " " + numLab.toString() + " " + PhoneTranslator.replaceBackTrickyPhones(m.getPhoneName()) + "\n";
            numLab++;
            dur = m.getTotalDurMillisec();
            um.concatRealisedAcoustParams(m.getPhoneName() + " " + dur.toString() + "\n");
            //System.out.println("phone=" + m.getPhoneName() + " dur=" + m.getTotalDur() +" durTotal=" + um.getTotalFrame() );
            
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
        
        if(phoneAlignmentForDurations && alignDur != null)
          if( um.getNumUttModel() != alignDurSize )
              throw new Exception("The number of durations provided for phone alignment (" + alignDurSize +
                      ") is greater than the number of feature vectors (" + um.getNumUttModel() + ")."); 

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
    public static void main(String[] args) throws IOException, InterruptedException{
       
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
       * After initHMMData it containswhile(it.hasNext()){
            phon = it.next(); TreeSet ts and ModelSet ms 
       * ModelSet: Contains the .pdf's (means and variances) for dur, lf0, mcp, str and mag
       *           these are all the HMMs trained for a particular voice 
       * TreeSet: Contains the tree-xxx.inf, xxx: dur, lf0, mcp, str and mag 
       *          these are all the trees trained for a particular voice. */
      HMMData htsData = new HMMData();
            
      /* For initialise provide the name of the hmm voice and the name of its configuration file,*/
       
      String MaryBase    = "/project/mary/marcela/openmary/"; /* MARY_BASE directory.*/
      String voiceName   = "hsmm-slt";                        /* voice name */
      String voiceConfig = "english-hsmm-slt.config";         /* voice configuration file name. */        
      String durFile     = MaryBase + "tmp/tmp.lab";          /* to save realised durations in .lab format */
      String parFile     = MaryBase + "tmp/tmp";              /* to save generated parameters tmp.mfc and tmp.f0 in Mary format */
      String outWavFile  = MaryBase + "tmp/tmp.wav";          /* to save generated audio file */
      
      // The settings for using GV and MixExc can be changed in this way:      
      htsData.initHMMData(voiceName, MaryBase, voiceConfig);
      htsData.setUseGV(true);
      htsData.setUseMixExc(true);
      htsData.setUseFourierMag(true);  // if the voice was trained with Fourier magnitudes
      
       
      /** The utterance model, um, is a Vector (or linked list) of Model objects. 
       * It will contain the list of models for current label file. */
      HTSUttModel um = new HTSUttModel();
      HTSParameterGeneration pdf2par = new HTSParameterGeneration();        
      HTSVocoder par2speech = new HTSVocoder();
      AudioInputStream ais;
               
      /** Example of context features file */
      String feaFile = htsData.getFeaFile();
      
      try {
          /* Process Mary context features file and creates UttModel um, a linked             
           * list of all the models in the utterance. For each model, it searches in each tree, dur,   
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
     
          System.out.println("Saving to file: " + outWavFile);
          System.out.println("Realised durations saved to file: " + durFile);
          File fileOut = new File(outWavFile);
          
          if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE,ais)) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fileOut);
          }

          System.out.println("Calling audioplayer:");
          AudioPlayer player = new AudioPlayer(fileOut);
          player.start();  
          player.join();
          System.out.println("Audioplayer finished...");
   
     
      } catch (Exception e) {
          System.err.println("Exception: " + e.getMessage());
      }
    }  /* main method */
    
    
  
}  /* class HTSEngine*/

