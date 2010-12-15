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

package marytts.modules.synthesis;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.SynthesisException;
import marytts.features.FeatureRegistry;
import marytts.features.TargetFeatureComputer;
import marytts.htsengine.HMMVoice;
import marytts.modules.HTSEngine;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.modules.TargetFeatureLister;
import marytts.modules.synthesis.Voice.Gender;
import marytts.server.MaryProperties;
import marytts.unitselection.select.Target;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;


/**
 * HTS-HMM synthesiser.
 *
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marc Schr&ouml;der, Marcela Charfuelan 
 */
public class HMMSynthesizer implements WaveformSynthesizer {
    private TargetFeatureLister targetFeatureLister;
    private HTSEngine htsEngine;
    private Logger logger;
    //private TargetFeatureComputer comp;

    public HMMSynthesizer() {
    }

    public void startup() throws Exception {
        logger = MaryUtils.getLogger(this.toString());
        // Try to get instances of our tools from Mary; if we cannot get them,
        // instantiate new objects.

        try{
            targetFeatureLister = (TargetFeatureLister) ModuleRegistry.getModule(TargetFeatureLister.class);
        } catch (NullPointerException npe){
            targetFeatureLister = null;
        }
        if (targetFeatureLister == null) {
            logger.info("Starting my own TargetFeatureLister");
            targetFeatureLister = new TargetFeatureLister();
            targetFeatureLister.startup();
        } else if (targetFeatureLister.getState() == MaryModule.MODULE_OFFLINE) {
            targetFeatureLister.startup();
        }

        try{
            htsEngine = (HTSEngine) ModuleRegistry.getModule(HTSEngine.class);
        } catch (NullPointerException npe){
            htsEngine = null;
        }
        if (htsEngine == null) {
            logger.info("Starting my own HTSEngine");
            htsEngine = new HTSEngine();
            htsEngine.startup();
        } else if (htsEngine.getState() == MaryModule.MODULE_OFFLINE) {
            htsEngine.startup();
        }
        
        // Register HMM voices:
        String voiceNames = MaryProperties.getProperty("hmm.voices.list");
        if (voiceNames == null) {
            logger.debug("No HMM voices listed in config files.");
        } else {
            logger.debug("Register HMM voices:");
            for (StringTokenizer st = new StringTokenizer(voiceNames); st.hasMoreTokens(); ) {
                String voiceName = st.nextToken();
                logger.debug("Voice '" + voiceName + "'");
                Locale locale = MaryUtils.string2locale(MaryProperties.needProperty("voice."+voiceName+".locale"));
                int samplingRate = MaryProperties.getInteger("voice."+voiceName+".samplingRate", 16000);
                
                Gender gender = new Gender(MaryProperties.needProperty("voice."+voiceName+".gender"));
                AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                        samplingRate, // samples per second
                        16, // bits per sample
                        1, // mono
                        2, // nr. of bytes per frame
                        samplingRate, // nr. of frames per second
                        false);
                
                
               /** When creating a HMMVoice object it should create and initialise a 
                * TreeSet ts, a ModelSet ms and load the context feature list used in this voice. */
                
                HMMVoice v = new HMMVoice (new String[] { voiceName },
                    locale, format, this, gender,
                    MaryProperties.getProperty("voice."+voiceName+".alpha"),
                    MaryProperties.getProperty("voice."+voiceName+".gamma"),
                    MaryProperties.getProperty("voice."+voiceName+".logGain"),
                    MaryProperties.getProperty("voice."+voiceName+".beta"),
                    MaryProperties.getFilename("voice."+voiceName+".Ftd"),     /* Tree DUR */
                    MaryProperties.getFilename("voice."+voiceName+".Ftf"),     /* Tree LF0 */
                    MaryProperties.getFilename("voice."+voiceName+".Ftm"),     /* Tree MCP */
                    MaryProperties.getFilename("voice."+voiceName+".Fts"),     /* Tree STR */
                    MaryProperties.getFilename("voice."+voiceName+".Fta"),     /* Tree MAG */
                    MaryProperties.getFilename("voice."+voiceName+".Fmd"),     /* Model DUR */
                    MaryProperties.getFilename("voice."+voiceName+".Fmf"),     /* Model LF0 */
                    MaryProperties.getFilename("voice."+voiceName+".Fmm"),     /* Model MCP */
                    MaryProperties.getFilename("voice."+voiceName+".Fms"),     /* Model STR */
                    MaryProperties.getFilename("voice."+voiceName+".Fma"),     /* Model MAG */        
                    MaryProperties.getBoolean("voice."+voiceName+".useAcousticModels"), /* use AcousticModeller, so prosody modification is enabled */                    
                    MaryProperties.getBoolean("voice."+voiceName+".useMixExc"),     /* Use Mixed excitation */
                    MaryProperties.getBoolean("voice."+voiceName+".useGV"),     /* Use Global Variance in parameter generation */
                    MaryProperties.getInteger("voice."+voiceName+".maxMgcGvIter"),  /* Max number of iterations for MGC gv optimisation */
                    MaryProperties.getInteger("voice."+voiceName+".maxLf0GvIter"), /* Max number of iterations for LF0 gv optimisation */
                    MaryProperties.getFilename("voice."+voiceName+".Fgvf"),     /* GV Model LF0 */
                    MaryProperties.getFilename("voice."+voiceName+".Fgvm"),     /* GV Model MCP */
                    MaryProperties.getFilename("voice."+voiceName+".Fgvs"),     /* GV Model STR */
                    MaryProperties.getFilename("voice."+voiceName+".Fgva"),     /* GV Model MAG */
                    MaryProperties.getFilename("voice."+voiceName+".Fgmmgvf"),  /* GMM GV Model LF0 */
                    MaryProperties.getFilename("voice."+voiceName+".Fgmmgvm"),  /* GMM GV Model MCP */
                    MaryProperties.getFilename("voice."+voiceName+".FeaFile"),  /* targetfeatures file, for testing*/
                    MaryProperties.getFilename("voice."+voiceName+".trickyPhonesFile"),  /* tricky phones file, if any*/
                    MaryProperties.getFilename("voice."+voiceName+".Fif"),      /* Filter coefficients file for mixed excitation*/
                    MaryProperties.getInteger("voice."+voiceName+".in"));        /* Number of filters */                    
                Voice.registerVoice(v);
               
            }
        }
        logger.info("started.");
               
    }

    /**
      * Perform a power-on self test by processing some example input data.
      * @throws Error if the module does not work properly.
      */
     public synchronized void powerOnSelfTest() throws Error
     {

         logger.info("Starting power-on self test.");
         try {
             Collection<Voice> myVoices = Voice.getAvailableVoices(this);
             if (myVoices.size() == 0) {
                 return;
             }
             
             Voice v = (Voice) myVoices.iterator().next();
             MaryData in = new MaryData(MaryDataType.ACOUSTPARAMS, v.getLocale());
            
             String exampleText = MaryDataType.ACOUSTPARAMS.exampleText(v.getLocale());
             if (exampleText != null) {
                 in.readFrom(new StringReader(exampleText));
                 in.setDefaultVoice(v);
                 assert v instanceof HMMVoice : "Expected voice to be a HMMVoice, but it is a " + v.getClass().toString();
                 
                 //-- Here it is set the targetFeatureComputer for this voice                
                 String features = ((HMMVoice)v).getHMMData().getFeatureDefinition().getFeatureNames();
                 TargetFeatureComputer comp = FeatureRegistry.getTargetFeatureComputer(v, features);
                
                 in.setOutputParams(features);
                 Document doc = in.getDocument();
                 // First, get the list of segments and boundaries in the current document
                 TreeWalker tw = MaryDomUtils.createTreeWalker(doc, doc, MaryXML.PHONE, MaryXML.BOUNDARY);
                 List<Element> segmentsAndBoundaries = new ArrayList<Element>();
                 Element e;
                 while ((e = (Element) tw.nextNode()) != null) {
                     segmentsAndBoundaries.add(e);
                 }
                 
                 List<Target> targetFeaturesList = targetFeatureLister.getListTargetFeatures(comp, segmentsAndBoundaries);
                 
                 // The actual durations are already fixed in the htsEngine.process()
                 // here i pass segements and boundaries to update the realised acoustparams, dur and f0
                 MaryData audio = htsEngine.process(in, targetFeaturesList, segmentsAndBoundaries, null);     
                      
                 assert audio.getAudio() != null;           

             } else {
                 logger.debug("No example text -- no power-on self test!");
             }
         } catch (Throwable t) {
             throw new Error("Module " + toString() + ": Power-on self test failed.", t);
         }
         logger.info("Power-on self test complete.");
         

     }

    public String toString() {
        return "HMMSynthesizer";
    }

    public AudioInputStream synthesize(List<Element> tokensAndBoundaries, Voice voice)
        throws SynthesisException {
        
        if (!voice.synthesizer().equals(this)) {
            throw new IllegalArgumentException(
                "Voice " + voice.getName() + " is not an HMM voice.");
        }
        logger.info("Synthesizing one sentence.");

        // from tokens and boundaries, extract segments and boundaries:
        List<Element> segmentsAndBoundaries = new ArrayList<Element>();
        Document doc = null;
        for (Element tOrB : tokensAndBoundaries) {
            if (tOrB.getTagName().equals(MaryXML.BOUNDARY)) {
                segmentsAndBoundaries.add(tOrB);
            } else { // a token -- add all segments below it
                if (doc == null) {
                    doc = tOrB.getOwnerDocument();
                }
                NodeIterator ni = MaryDomUtils.createNodeIterator(doc, tOrB, MaryXML.PHONE);
                Element s;
                while ((s = (Element) ni.nextNode()) != null) {
                    segmentsAndBoundaries.add(s);
                }
            }
        }
        try {
            assert voice instanceof HMMVoice : "Expected voice to be a HMMVoice, but it is a " + voice.getClass().toString();
        
            //-- This can be done just once when powerOnSelfTest() of this voice
            //-- mmmmmm it did not work, it takes the comp from the default voice
            //-- CHECK: do we need to do this for every call???
            String features = ((HMMVoice)voice).getHMMData().getFeatureDefinition().getFeatureNames();
            TargetFeatureComputer comp = FeatureRegistry.getTargetFeatureComputer(voice, features);
              
            // it is not faster to pass directly a list of targets?
            //--String targetFeatureString = targetFeatureLister.listTargetFeatures(comp, segmentsAndBoundaries);
            
            MaryData d = new MaryData(targetFeatureLister.outputType(), voice.getLocale());
            //--d.setPlainText(targetFeatureString);
            d.setDefaultVoice(voice);
            
            List<Target> targetFeaturesList = targetFeatureLister.getListTargetFeatures(comp, segmentsAndBoundaries);
            
            // the actual durations are already fixed in the htsEngine.process()
            // here i pass segements and boundaries to update the realised acoustparams, dur and f0
            MaryData audio = htsEngine.process(d, targetFeaturesList, segmentsAndBoundaries, tokensAndBoundaries);     
                 
            return audio.getAudio();           
                     
        } catch (Exception e) {
            throw new SynthesisException("HMM Synthesiser could not synthesise: ", e);
        }
    }
    
 
    

}
