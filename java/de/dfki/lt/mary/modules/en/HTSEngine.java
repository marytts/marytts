
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

package de.dfki.lt.mary.modules.en;

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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

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
    /** Data in the configuration file, .pdf file names and other parameters */
   // private HMMData hts_data = new HMMData();

    /** Contains the .pdf's (means and variances) for dur, lf0, mcp, str and mag
     * these are all the HMMs trained for a particular voice */   
   // private ModelSet ms = new ModelSet();
    
     
    /** Contains the tree-xxx.inf, xxx: dur, lf0, mcp, str and mag 
     * these are all the trees trained for a particular voice. */
   // private TreeSet ts = new TreeSet(hts_data.HTS_NUMMTYPE);
    
    
    public HTSEngine()
    {
        super("HTSEngine",
              MaryDataType.get("HTSCONTEXT_EN"),
              MaryDataType.get("AUDIO")
              );
    }

    public synchronized void powerOnSelfTest() throws Error
    {
        // TODO: add meaningful power-on self test
        logger.info("\n TODO: TO-BE DONE HTSEngine powerOnSelfTest()\n");
        
    }
    
    
    /**
     * when calling this function HMMengine must be initialised already.
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
   
        Voice v = d.getDefaultVoice();
        assert v instanceof HMMVoice;
        HMMVoice hmmv = (HMMVoice)v;
        
        
        String context = d.getPlainText();
        
        /* Process label file of Mary context features and creates UttModel um */
        ProcessUtt(context, um, hmmv.getHMMData());

        /* Process UttModel */
        /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */     
        pdf2par.HTS_MaximumLikelihoodParameterGeneration(um, hmmv.getHMMData());
    
        /* Process generated parameters */
        /* Synthesize speech waveform, generate speech out of sequence of parameters */
        ais = par2speech.HTS_MLSA_Vocoder(pdf2par, hmmv.getHMMData());
       
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
    public AudioInputStream processStr(String context)
    throws Exception
    {
        UttModel um = new UttModel();
        ParameterGeneration pdf2par = new ParameterGeneration();
        Vocoder par2speech = new Vocoder();
        AudioInputStream ais;
        
        /** Data in the configuration file, .pdf file names and other parameters 
         * After InitHMMData it contains TreeSet ts and ModelSet ms 
         * ModelSet: Contains the .pdf's (means and variances) for dur, lf0, mcp, str and mag
         *           these are all the HMMs trained for a particular voice 
         * TreeSet: Contains the tree-xxx.inf, xxx: dur, lf0, mcp, str and mag 
         *          these are all the trees trained for a particular voice. */
         HMMData hts_data = new HMMData();
         hts_data.InitHMMData("/project/mary/marcela/HTS-mix/hts_engine.config");
         
        //ModelSet ms = new ModelSet();
        //TreeSet ts = new TreeSet(hts_data.HTS_NUMMTYPE);
        //InitHMMengine(hts_data, ts, ms);
        
        System.out.println("CONTEXT:" + context);
        
        /* Process label file of Mary context features and creates UttModel um */
        ProcessUtt(context, um, hts_data);

        /* Process UttModel */
        /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */     
        pdf2par.HTS_MaximumLikelihoodParameterGeneration(um, hts_data);
    
        /* Process generated parameters */
        /* Synthesize speech waveform, generate speech out of sequence of parameters */
        ais = par2speech.HTS_MLSA_Vocoder(pdf2par, hts_data);
        
       return ais;
        
    }
  
    
    /** Reads the Label file, the file which contains the Mary context features,
     *  creates an scanner object and calls _ProcessUtt
     * @param LabFile
     */
    public void ProcessUttFromFile(String LabFile, UttModel um, HMMData hts_data){ 
        Scanner s = null;
        try {    
            /* parse text in label file */
            s = new Scanner(new BufferedReader(new FileReader(LabFile)));
            _ProcessUtt(s,um,hts_data,hts_data.getTreeSet(),hts_data.getModelSet());
              
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
    public void ProcessUtt(String LabText, UttModel um, HMMData hts_data) {
        Scanner s = null;
        try {
          s = new Scanner(LabText);
         _ProcessUtt(s, um, hts_data, hts_data.getTreeSet(),hts_data.getModelSet());
        } finally {
            if (s != null)
              s.close();
        }   
    }
    

    
    /** Parse Mary context features, triphones. 
     * For each triphone model in the file, it creates a Model object in a linked list of 
     * Model objects -> UttModel um 
     * It also estimates state duration from state duration model (Gaussian).
     * For each model in the vector, the mean and variance of the DUR, LF0, MCP, STR and MAG 
     * are searched in the ModelSet and copied in each triphone model.   */
    private void _ProcessUtt(Scanner s, UttModel um, HMMData hts_data, TreeSet ts, ModelSet ms){     
        int i, mstate,frame;
        Model m;                   /* current model, corresponds to a line in label file */
        String next_line;
        double diffdur_old = 0.0;
        double diffdur_new = 0.0;
        Tree aux_tree;

        /* parse text */
        i=0;
        while (s.hasNext()) {
            next_line = s.next();
            //System.out.println("STR: " + next_line);
            um.addUttModel(new Model(ms));            

            m = um.getUttModel(i);
            m.setName(next_line);               

            /* Estimate state duration from state duration model (Gaussian) 
             * 1. find the index idx of the durpdf corresponding (or that best match in the tree) 
             *    to the triphone+context features in next_line. 
             * NOTE 1: the indexes in the tree.inf file start in 1 ex. dur_s2_1, but here are stored 
             * in durpdf[i][j] array which starts in i=0, so when finding this dur pdf, the idx should 
             * be idx-1 !!!
             * 2. Calculate duration using the pdf idx found in the tree, function: FindDurPDF */
            aux_tree = ts.getTreeHead(hts_data.DUR);
            m.set_durpdf( ts.SearchTree(next_line, aux_tree.get_root(), false));

            //System.out.println("dur->pdf=" + m.get_durpdf());

            if (hts_data.LENGTH() == 0.0 ) {
                diffdur_new = ms.FindDurPDF(m, hts_data.RHO(), diffdur_old); 
                diffdur_old = diffdur_new;
                um.set_totalframe(um.get_totalframe() + m.get_totaldur());
                //System.out.println("total_frame=" + um.get_totalframe() + "  total_dur=" + m.get_totaldur());
            } /* else : when total length of generated speech is specified */
            /* Not implemented yet...*/

            /* Find pdf for LF0 */               
            for(aux_tree=ts.getTreeHead(hts_data.LF0), mstate=0; aux_tree != ts.getTreeTail(hts_data.LF0); aux_tree=aux_tree.get_next(), mstate++ ) {           
                m.set_lf0pdf(mstate, ts.SearchTree(next_line,aux_tree.get_root(),false));
                //System.out.println("lf0pdf[" + mstate + "]=" + m.get_lf0pdf(mstate));
                ms.FindLF0PDF(mstate, m, hts_data.UV());
            }

            /* Find pdf for MCP */
            for(aux_tree=ts.getTreeHead(hts_data.MCP), mstate=0; aux_tree != ts.getTreeTail(hts_data.MCP); aux_tree=aux_tree.get_next(), mstate++ ) {           
                m.set_mceppdf(mstate, ts.SearchTree(next_line,aux_tree.get_root(),false));
                //System.out.println("mceppdf[" + mstate + "]=" + m.get_mceppdf(mstate));
                ms.FindMcpPDF(mstate, m);
            }              

            /* Find pdf for strengths */
            for(aux_tree=ts.getTreeHead(hts_data.STR), mstate=0; aux_tree != ts.getTreeTail(hts_data.STR); aux_tree=aux_tree.get_next(), mstate++ ) {           
                m.set_strpdf(mstate, ts.SearchTree(next_line,aux_tree.get_root(),false));
                //System.out.println("strpdf[" + mstate + "]=" + m.get_strpdf(mstate));
                ms.FindStrPDF(mstate, m);                    
            }

            /* Find pdf for Fourier magnitudes */
            for(aux_tree=ts.getTreeHead(hts_data.MAG), mstate=0; aux_tree != ts.getTreeTail(hts_data.MAG); aux_tree=aux_tree.get_next(), mstate++ ) {           
                m.set_magpdf(mstate, ts.SearchTree(next_line,aux_tree.get_root(),false));
                //System.out.println("magpdf[" + mstate + "]=" + m.get_magpdf(mstate));
                ms.FindMagPDF(mstate, m);
            }

            //System.out.println();
            /* increment number of models in utterance model */
            um.set_nModel(um.get_nModel()+1);
            /* update number of states */
            um.set_nState(um.get_nState() + ms.get_nstate());
            i++;
        }

        for(i=0; i<um.getNumUttModel(); i++){
            m = um.getUttModel(i);                  
            for(mstate=0; mstate<ms.get_nstate(); mstate++)
                for(frame=0; frame<m.get_dur(mstate); frame++) 
                    if(m.get_voiced(mstate))
                        um.set_lf0frame(um.get_lf0frame() +1);
            //System.out.println("Vector m[" + i + "]=" + m.getName()); 
        }

        System.out.println("\nNumber of models in sentence nModel=" + um.get_nModel() + "  Total number of states nState=" + um.get_nState());
        System.out.println("Number of voiced frames=" + um.get_lf0frame());    

        
    } /* method _ProcessUtt */

   
    
    
    /**
     * Process UttModel: um
     * Generate parameters out of PDFs:   pdf2par and
     * Generate speech out of parameters: par2speech
     */
    public AudioInputStream pdf2speech(UttModel um, HMMData hts_data) {
      AudioInputStream ais;
      ParameterGeneration pdf2par = new ParameterGeneration();
      Vocoder par2speech = new Vocoder();
        
      /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */     
      pdf2par.HTS_MaximumLikelihoodParameterGeneration(um, hts_data);
        

      /* Synthesize speech waveform, generate speech out of sequence of parameters */
      ais = par2speech.HTS_MLSA_Vocoder(pdf2par, hts_data);
        
      return ais;
    }

 
    
    
    
    /** 
     * Stand alone testing using an HTSCONTEXT_EN file as input.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException, InterruptedException{

      HTSEngine hmm_tts = new HTSEngine();
      
      /** Data in the configuration file, .pdf file names and other parameters 
      * After InitHMMData it contains TreeSet ts and ModelSet ms 
      * ModelSet: Contains the .pdf's (means and variances) for dur, lf0, mcp, str and mag
      *           these are all the HMMs trained for a particular voice 
      * TreeSet: Contains the tree-xxx.inf, xxx: dur, lf0, mcp, str and mag 
      *          these are all the trees trained for a particular voice. */
      HMMData hts_data = new HMMData();
      hts_data.InitHMMData("/project/mary/marcela/HTS-mix/hts_engine.config");
 
      
      /** The utterance model, um, is a Vector (or linked list) of Model objects. 
       * It will contain the list of models for current label file. */
      UttModel um = new UttModel();
      ParameterGeneration pdf2par = new ParameterGeneration();
      Vocoder par2speech = new Vocoder();
      AudioInputStream ais;
      
      /** Example of HTSCONTEXT_EN context features file */
      String Flab = hts_data.LabFile();


      /* Process label file of Mary context features and creates UttModel um, a linked             
       * list of alt the models in the utterance. For each model, it searches in each tree, dur,   
       * cmp, etc, the pdf index that corresponds to a triphone context feature and with           
       * that index retrieves from the ModelSet the mean and variance for each state of the HMM.   */
      hmm_tts.ProcessUttFromFile(Flab, um, hts_data);


      /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */     
      pdf2par.HTS_MaximumLikelihoodParameterGeneration(um, hts_data);


      /* Synthesize speech waveform, generate speech out of sequence of parameters */
      ais = par2speech.HTS_MLSA_Vocoder(pdf2par, hts_data);

      System.out.println("Calling audioplayer:");
      AudioPlayer player = new AudioPlayer(ais, null);
      player.start();  // this call the run method..
      player.join();
      System.out.println("audioplayer finished...");
      System.exit(0);

    }  /* main method */
    
    


}  /* class HTSEngine*/

