
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

package de.dfki.lt.mary.modules;

import de.dfki.lt.mary.htsengine.HMMData;
import de.dfki.lt.mary.htsengine.Model;
import de.dfki.lt.mary.htsengine.ModelSet;
import de.dfki.lt.mary.htsengine.ParameterGeneration;
import de.dfki.lt.mary.htsengine.Tree;
import de.dfki.lt.mary.htsengine.TreeSet;
import de.dfki.lt.mary.htsengine.UttModel;
import de.dfki.lt.mary.htsengine.Vocoder;
import de.dfki.lt.mary.htsengine.HMMVoice;

import de.dfki.lt.mary.modules.synthesis.Voice;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.log4j.Logger;
import org.jsresources.AppendableSequenceAudioInputStream;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.modules.InternalModule;
import de.dfki.lt.mary.tests.AllTests;
import de.dfki.lt.signalproc.util.AudioPlayer;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.NoiseDoubleDataSource;


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
    
    public HTSEngine()
    {
        super("HTSEngine",
              MaryDataType.get("HTSCONTEXT"),
              MaryDataType.get("AUDIO")
              );
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
        UttModel um = new UttModel();
        ParameterGeneration pdf2par = new ParameterGeneration();
        Vocoder par2speech = new Vocoder();
        AudioInputStream ais;
              
        Voice v = d.getDefaultVoice(); /* This is the way of getting a Voice through a MaryData type */
        assert v instanceof HMMVoice;
        HMMVoice hmmv = (HMMVoice)v;
        
        
        String context = d.getPlainText();
        
        /* Process label file of Mary context features and creates UttModel um */
        processUtt(context, um, hmmv.getHMMData());

        /* Process UttModel */
        /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */     
        pdf2par.htsMaximumLikelihoodParameterGeneration(um, hmmv.getHMMData());
    
        /* Process generated parameters */
        /* Synthesize speech waveform, generate speech out of sequence of parameters */
        ais = par2speech.htsMLSAVocoder(pdf2par, hmmv.getHMMData());
       
        MaryData output = new MaryData(outputType());
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
         
       return output;
        
    }
   
    /* For stand alone testing. */
    public AudioInputStream processStr(String context, HMMData htsData)
    throws Exception
    {
        UttModel um = new UttModel();
        ParameterGeneration pdf2par = new ParameterGeneration();
        Vocoder par2speech = new Vocoder();
        AudioInputStream ais;
        
        /* htsData contains:
         * data in the configuration file, .pdf file names and other parameters. 
         * After InitHMMData it contains TreeSet ts and ModelSet ms 
         * ModelSet: Contains the .pdf's (means and variances) for dur, lf0, mcp, str and mag
         *           these are all the HMMs trained for a particular voice 
         * TreeSet: Contains the tree-xxx.inf, xxx: dur, lf0, mcp, str and mag 
         *          these are all the trees trained for a particular voice. */
 
        
        logger.info("CONTEXT:" + context);
        
        /* Process label file of Mary context features and creates UttModel um */
        processUtt(context, um, htsData);

        /* Process UttModel */
        /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */     
        pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData);
    
        /* Process generated parameters */
        /* Synthesize speech waveform, generate speech out of sequence of parameters */
        ais = par2speech.htsMLSAVocoder(pdf2par, htsData);
        
       return ais;
        
    }
  
 
    
    /** Reads the Label file, the file which contains the Mary context features,
     *  creates an scanner object and calls _ProcessUtt
     * @param LabFile
     */
    public void processUttFromFile(String LabFile, UttModel um, HMMData htsData){ 
        Scanner s = null;
        try {    
            /* parse text in label file */
            s = new Scanner(new BufferedReader(new FileReader(LabFile)));
            _processUtt(s,um,htsData,htsData.getTreeSet(),htsData.getModelSet());
              
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
    public void processUtt(String LabText, UttModel um, HMMData htsData) {
        Scanner s = null;
        try {
          s = new Scanner(LabText);
         _processUtt(s, um, htsData, htsData.getTreeSet(),htsData.getModelSet());
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
    private void _processUtt(Scanner s, UttModel um, HMMData htsData, TreeSet ts, ModelSet ms){     
        int i, mstate,frame;
        Model m;                   /* current model, corresponds to a line in label file */
        String nextLine;
        double diffdurOld = 0.0;
        double diffdurNew = 0.0;
        Tree auxTree;

        /* parse text */
        i=0;
        while (s.hasNext()) {
            nextLine = s.next();
            //System.out.println("STR: " + nextLine);
            um.addUttModel(new Model(ms));            

            m = um.getUttModel(i);
            m.setName(nextLine);               

            /* Estimate state duration from state duration model (Gaussian) 
             * 1. find the index idx of the durpdf corresponding (or that best match in the tree) 
             *    to the triphone+context features in nextLine. 
             * NOTE 1: the indexes in the tree.inf file start in 1 ex. dur_s2_1, but here are stored 
             * in durpdf[i][j] array which starts in i=0, so when finding this dur pdf, the idx should 
             * be idx-1 !!!
             * 2. Calculate duration using the pdf idx found in the tree, function: FindDurPDF */
            auxTree = ts.getTreeHead(HMMData.DUR);
            m.setDurPdf( ts.searchTree(nextLine, auxTree.getRoot(), false));

            //System.out.println("dur->pdf=" + m.get_durpdf());

            if (htsData.getLength() == 0.0 ) {
                diffdurNew = ms.findDurPdf(m, htsData.getRho(), diffdurOld); 
                diffdurOld = diffdurNew;
                um.setTotalFrame(um.getTotalFrame() + m.getTotalDur());
                //System.out.println("total_frame=" + um.get_totalframe() + "  total_dur=" + m.get_totaldur());
            } /* else : when total length of generated speech is specified */
            /* Not implemented yet...*/

            /* Find pdf for LF0 */               
            for(auxTree=ts.getTreeHead(HMMData.LF0), mstate=0; auxTree != ts.getTreeTail(HMMData.LF0); auxTree=auxTree.getNext(), mstate++ ) {           
                m.setLf0Pdf(mstate, ts.searchTree(nextLine,auxTree.getRoot(),false));
                //System.out.println("lf0pdf[" + mstate + "]=" + m.get_lf0pdf(mstate));
                ms.findLf0Pdf(mstate, m, htsData.getUV());
            }

            /* Find pdf for MCP */
            for(auxTree=ts.getTreeHead(HMMData.MCP), mstate=0; auxTree != ts.getTreeTail(HMMData.MCP); auxTree=auxTree.getNext(), mstate++ ) {           
                m.setMcepPdf(mstate, ts.searchTree(nextLine,auxTree.getRoot(),false));
                //System.out.println("mceppdf[" + mstate + "]=" + m.get_mceppdf(mstate));
                ms.findMcpPdf(mstate, m);
            }              

            /* Find pdf for strengths */
            /* If there is no STRs then auxTree=null=ts.getTreeTail so it will not try to find pdf for strengths */
            for(auxTree=ts.getTreeHead(HMMData.STR), mstate=0; auxTree != ts.getTreeTail(HMMData.STR); auxTree=auxTree.getNext(), mstate++ ) {           
                m.setStrPdf(mstate, ts.searchTree(nextLine,auxTree.getRoot(),false));
                //System.out.println("strpdf[" + mstate + "]=" + m.get_strpdf(mstate));
                ms.findStrPdf(mstate, m);                    
            }

            /* Find pdf for Fourier magnitudes */
            /* If there is no MAGs then auxTree=null=ts.getTreeTail so it will not try to find pdf for Fourier magnitudes */
            for(auxTree=ts.getTreeHead(HMMData.MAG), mstate=0; auxTree != ts.getTreeTail(HMMData.MAG); auxTree=auxTree.getNext(), mstate++ ) {           
                m.setMagPdf(mstate, ts.searchTree(nextLine,auxTree.getRoot(),false));
                //System.out.println("magpdf[" + mstate + "]=" + m.get_magpdf(mstate));
                ms.findMagPdf(mstate, m);
            }

            //System.out.println();
            /* increment number of models in utterance model */
            um.setNumModel(um.getNumModel()+1);
            /* update number of states */
            um.setNumState(um.getNumState() + ms.getNumState());
            i++;
        }

        for(i=0; i<um.getNumUttModel(); i++){
            m = um.getUttModel(i);                  
            for(mstate=0; mstate<ms.getNumState(); mstate++)
                for(frame=0; frame<m.getDur(mstate); frame++) 
                    if(m.getVoiced(mstate))
                        um.setLf0Frame(um.getLf0Frame() +1);
            //System.out.println("Vector m[" + i + "]=" + m.getName()); 
        }

        logger.info("Number of models in sentence numModel=" + um.getNumModel() + "  Total number of states numState=" + um.getNumState());
        logger.info("Total number of frames=" + um.getTotalFrame() + "  Number of voiced frames=" + um.getLf0Frame());    

        
    } /* method _ProcessUtt */

    
    
    /** 
     * Stand alone testing using an HTSCONTEXT_EN file as input.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException, InterruptedException{

      HTSEngine hmm_tts = new HTSEngine();
      
      /* htsData contains:
       * Data in the configuration file, .pdf, tree-xxx.inf file names and other parameters. 
       * After initHMMData it contains TreeSet ts and ModelSet ms 
       * ModelSet: Contains the .pdf's (means and variances) for dur, lf0, mcp, str and mag
       *           these are all the HMMs trained for a particular voice 
       * TreeSet: Contains the tree-xxx.inf, xxx: dur, lf0, mcp, str and mag 
       *          these are all the trees trained for a particular voice. */
      HMMData htsData = new HMMData();
      //htsData.initHMMData("/project/mary/marcela/HTS-mix/hts_engine.config");
      //htsData.initHMMData("/project/mary/sacha/HTS_BITS_24features_stableonly/hts_engine.config");
      htsData.initHMMData("/project/mary/marcela/german-hmm-bits/hts/hts_engine.config");
      
      /** The utterance model, um, is a Vector (or linked list) of Model objects. 
       * It will contain the list of models for current label file. */
      UttModel um = new UttModel();
      ParameterGeneration pdf2par = new ParameterGeneration();
      Vocoder par2speech = new Vocoder();
      AudioInputStream ais;
      
      /** Example of HTSCONTEXT_EN context features file */
      String flab = htsData.getLabFile();

      try {
          /* Process label file of Mary context features and creates UttModel um, a linked             
           * list of alt the models in the utterance. For each model, it searches in each tree, dur,   
           * cmp, etc, the pdf index that corresponds to a triphone context feature and with           
           * that index retrieves from the ModelSet the mean and variance for each state of the HMM.   */
          hmm_tts.processUttFromFile(flab, um, htsData);


          /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */     
          pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData);


          /* Synthesize speech waveform, generate speech out of sequence of parameters */
          ais = par2speech.htsMLSAVocoder(pdf2par, htsData);

          System.out.println("Calling audioplayer:");
          AudioPlayer player = new AudioPlayer(ais, null);
          player.start();  // this call the run method..
          player.join();
          System.out.println("audioplayer finished...");
          System.exit(0);

      } catch (Exception e) {
          System.err.println("Exception: " + e.getMessage());
      }

    }  /* main method */
    
    


}  /* class HTSEngine*/

