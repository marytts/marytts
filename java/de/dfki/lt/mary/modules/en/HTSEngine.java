/**
 * Copyright 2000-2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
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
 * HMM synthesis.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marc Schr&ouml;der, Marcela Charfuelan 
 */
public class HTSEngine extends InternalModule
{
    /** Data in the configuration file, .pdf file names and other parameters */
    private static HMMData hts_data = new HMMData();

    /** Contains the .pdf's (means and variances) for dur, lf0, mcp, str and mag
     * these are the HMMs trained for a particular voice */   
    private static ModelSet ms = new ModelSet();
    
    /** The utterance model, um, is a Vector (or linked list) of Model objects. 
     * It will contain the list of models for current label file. 
     * Maybe um does not need to be static, because having loaded the modelSet ms 
     * it could be possible to process another label file without the need of loading 
     * again the model set ???. */
   // private static UttModel um = new UttModel();     
    
    
    /** Contains the tree-xxx.inf, xxx: dur, lf0, mcp, str and mag 
     * these are the trees trained for a particular voice. */
    private static TreeSet ts = new TreeSet(hts_data);
    
    
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
        UttModel um = new UttModel();
        ParameterGeneration pdf2par = new ParameterGeneration();
        Vocoder par2speech = new Vocoder();
        AudioInputStream ais;
        
        String context = d.getPlainText();
        
        System.out.println("CONTEXT:" + context);
        
        /* Process label file of Mary context features and creates UttModel um */
        ProcessUtt(context, um);

        /* Process UttModel */
        /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */     
        pdf2par.HTS_MaximumLikelihoodParameterGeneration(um, ms);
    
        /* Process generated parameters */
        /* Synthesize speech waveform, generate speech out of sequence of parameters */
        ais = par2speech.HTS_MLSA_Vocoder(pdf2par, ms);
      
        
 //???       
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
        //AudioInputStream dummyAudio = AudioSystem.getAudioInputStream(AllTests.class.getResourceAsStream("test.wav"));
        //AudioInputStream dummyAudio = new DDSAudioInputStream(new NoiseDoubleDataSource(32000, -6), d.getAudioFileFormat().getFormat());
        //output.appendAudio(dummyAudio);
//???      
//        System.out.println("HTSEngine Calling audioplayer:");
//        AudioPlayer player = new AudioPlayer(ais, null);
//        player.start();  // this call the run method..
//        System.out.println("HTSEngine audioplayer finished..."); 
        
       output.appendAudio(ais); 
        
        
       return output;
        
    }
   
    
  
    
    /** This is the file that contains the requested features for the full context
     * names and tree questions, for example feature_list_en_05.pl, 
     * the un-commented context features in this file are used during synthesis.*/
    public String getFeaListFile(){ return hts_data.FeaListFile(); }
 
    /** Example of HTSCONTEXT_EN context features file */
    public String getLabFile(){ return hts_data.LabFile(); }
    
    /** Reads the Label file, the file which contains the Mary context features,
     *  creates an scanner object and calls _ProcessUtt
     * @param LabFile
     */
    public void ProcessUttFromFile(String LabFile, UttModel um){ 
        Scanner s = null;
        try {    
            /* parse text in label file */
            s = new Scanner(new BufferedReader(new FileReader(LabFile)));
            _ProcessUtt(s,um);
              
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
    public void ProcessUtt(String LabText, UttModel um) {
        Scanner s = null;
        try {
          s = new Scanner(LabText);
         _ProcessUtt(s, um);
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
    private void _ProcessUtt(Scanner s, UttModel um){     
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
            System.out.println("STR: " + next_line);
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
            aux_tree = ts.getTreeHead(HMMData.DUR);
            m.set_durpdf( TreeSet.SearchTree(next_line, aux_tree.get_root(), false));

            System.out.println("dur->pdf=" + m.get_durpdf());

            if (HMMData.LENGTH() == 0.0 ) {
                diffdur_new = ms.FindDurPDF(m, HMMData.RHO(), diffdur_old); 
                diffdur_old = diffdur_new;
                um.set_totalframe(um.get_totalframe() + m.get_totaldur());
                System.out.println("total_frame=" + um.get_totalframe() + "  total_dur=" + m.get_totaldur());
            } /* else : when total length of generated speech is specified */
            /* Not implemented yet...*/

            /* Find pdf for LF0 */               
            for(aux_tree=ts.getTreeHead(HMMData.LF0), mstate=0; aux_tree != ts.getTreeTail(HMMData.LF0); aux_tree=aux_tree.get_next(), mstate++ ) {           
                m.set_lf0pdf(mstate, TreeSet.SearchTree(next_line,aux_tree.get_root(),false));
                //System.out.println("lf0pdf[" + mstate + "]=" + m.get_lf0pdf(mstate));
                ms.FindLF0PDF(mstate, m, HMMData.UV());
            }

            /* Find pdf for MCP */
            for(aux_tree=ts.getTreeHead(HMMData.MCP), mstate=0; aux_tree != ts.getTreeTail(HMMData.MCP); aux_tree=aux_tree.get_next(), mstate++ ) {           
                m.set_mceppdf(mstate, TreeSet.SearchTree(next_line,aux_tree.get_root(),false));
                //System.out.println("mceppdf[" + mstate + "]=" + m.get_mceppdf(mstate));
                ms.FindMcpPDF(mstate, m);
            }              

            /* Find pdf for strengths */
            for(aux_tree=ts.getTreeHead(HMMData.STR), mstate=0; aux_tree != ts.getTreeTail(HMMData.STR); aux_tree=aux_tree.get_next(), mstate++ ) {           
                m.set_strpdf(mstate, TreeSet.SearchTree(next_line,aux_tree.get_root(),false));
                //System.out.println("strpdf[" + mstate + "]=" + m.get_strpdf(mstate));
                ms.FindStrPDF(mstate, m);                    
            }

            /* Find pdf for Fourier magnitudes */
            for(aux_tree=ts.getTreeHead(HMMData.MAG), mstate=0; aux_tree != ts.getTreeTail(HMMData.MAG); aux_tree=aux_tree.get_next(), mstate++ ) {           
                m.set_magpdf(mstate, TreeSet.SearchTree(next_line,aux_tree.get_root(),false));
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
     * Load configuration file: hts_engine.config.
     * Load Trees and PDFs from HTS-HMMs.
     */
    public void InitHMMengine() {
        
        hts_data.InitHMMData("/project/mary/marcela/HTS-mix/hts_engine.config");
        
        System.out.println("InitHMMengine: pdfMcp file: " + HMMData.PdfDurFile());
        
        
        /* load Tree set for Duration, log f0, mel-cepstrum, stregths and Fourier magnitudes */
        TreeSet.LoadTreeSet(HMMData.TreeDurFile(), HMMData.DUR);        
        TreeSet.LoadTreeSet(HMMData.TreeLf0File(), HMMData.LF0);        
        TreeSet.LoadTreeSet(HMMData.TreeMcpFile(), HMMData.MCP);        
        TreeSet.LoadTreeSet(HMMData.TreeStrFile(), HMMData.STR);        
        TreeSet.LoadTreeSet(HMMData.TreeMagFile(), HMMData.MAG);
        
        /* Load PDFs, mean and variances from HMM models for duration, log f0, mel-cepstrum, stregths 
         * and Fourier magnitudes. All the means and variances are copied in the ModelSet object ms. */
        ModelSet.LoadModelSet(hts_data);
        
    }
    
    /**
     * Process UttModel: um
     * Generate parameters out of PDFs:   pdf2par and
     * Generate speech out of parameters: par2speech
     */
    public AudioInputStream pdf2speech(UttModel um) {
      AudioInputStream ais;
      ParameterGeneration pdf2par = new ParameterGeneration();
      Vocoder par2speech = new Vocoder();
        
      /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */     
      pdf2par.HTS_MaximumLikelihoodParameterGeneration(um, ms);
        

      /* Synthesize speech waveform, generate speech out of sequence of parameters */
      ais = par2speech.HTS_MLSA_Vocoder(pdf2par, ms);
        
      return ais;
    }

 
    
    
    
    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException, InterruptedException{

      HTSEngine hmm_tts = new HTSEngine();
      UttModel um = new UttModel();
      ParameterGeneration pdf2par = new ParameterGeneration();
      Vocoder par2speech = new Vocoder();
      AudioInputStream ais;
      
      //String  Flab = "//project/mary/marcela/MaryClientUserHMM/gen/test.lab";
      String Flab = hmm_tts.getLabFile();


      hmm_tts.InitHMMengine();

      /* Process label file of Mary context features and creates UttModel um, a linked             
       * list of alt the models in the utterance. For each model, it searches in each tree, dur,   
       * cmp, etc, the pdf index that corresponds to a triphone context feature and with           
       * that index retrieves from the ModelSet the mean and variance for each state of the HMM.   */
      hmm_tts.ProcessUttFromFile(Flab,um);


      /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */     
      pdf2par.HTS_MaximumLikelihoodParameterGeneration(um, ms);


      /* Synthesize speech waveform, generate speech out of sequence of parameters */
      ais = par2speech.HTS_MLSA_Vocoder(pdf2par, ms);

      System.out.println("Calling audioplayer:");
      AudioPlayer player = new AudioPlayer(ais, null);
      player.start();  // this call the run method..
      player.join();
      System.out.println("audioplayer finished...");
      System.exit(0);

    }  /* main method */
    
    


}  /* class HTSEngine*/

