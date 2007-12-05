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

package de.dfki.lt.mary.modules.synthesis;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import com.sun.speech.freetts.Utterance;

import de.dfki.lt.mary.Mary;
import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.MaryModule;
import de.dfki.lt.mary.modules.MaryXMLToMbrola;
import de.dfki.lt.mary.modules.MbrolaCaller;
import de.dfki.lt.mary.modules.XML2UttAcoustParams;
import de.dfki.lt.mary.modules.HTSContextTranslator;
import de.dfki.lt.mary.modules.HTSEngine;
import de.dfki.lt.mary.modules.TargetFeatureLister;


import de.dfki.lt.mary.htsengine.HMMData;
import de.dfki.lt.mary.htsengine.HTSModel;
import de.dfki.lt.mary.htsengine.HTSModelSet;
import de.dfki.lt.mary.htsengine.HTSParameterGeneration;
import de.dfki.lt.mary.htsengine.HTSTreeSet;
import de.dfki.lt.mary.htsengine.HTSUttModel;
import de.dfki.lt.mary.htsengine.HTSVocoder;
import de.dfki.lt.mary.htsengine.HMMVoice;

import de.dfki.lt.mary.modules.synthesis.Voice.Gender;
import de.dfki.lt.mary.unitselection.UnitSelectionVoice;
import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.mary.util.dom.NameNodeFilter;
import de.dfki.lt.signalproc.util.AudioPlayer;


/**
 * HTS-HMM synthesiser.
 *
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marc Schr&ouml;der, Marcela Charfuelan 
 */
public class HMMSynthesizer implements WaveformSynthesizer {
    private XML2UttAcoustParams x2u;
    private TargetFeatureLister targetFeatureLister;
    private HTSContextTranslator htsContextTranslator;
    private HTSEngine htsEngine;
    private Logger logger;

    public HMMSynthesizer() {
    }

    public void startup() throws Exception {
        logger = Logger.getLogger(this.toString());
        // Try to get instances of our tools from Mary; if we cannot get them,
        // instantiate new objects.

        try{
            x2u = (XML2UttAcoustParams) Mary.getModule(XML2UttAcoustParams.class);
        } catch (NullPointerException npe){
            x2u = null;
        }
        if (x2u == null) {
            logger.info("Starting my own XML2UttAcoustParams");
            x2u = new XML2UttAcoustParams();
            x2u.startup();
        } else if (x2u.getState() == MaryModule.MODULE_OFFLINE) {
            x2u.startup();
        }

        try{
            targetFeatureLister = (TargetFeatureLister) Mary.getModule(TargetFeatureLister.class);
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
            htsContextTranslator = (HTSContextTranslator) Mary.getModule(HTSContextTranslator.class);
        } catch (NullPointerException npe){
            htsContextTranslator = null;
        }
        if (htsContextTranslator == null) {
            logger.info("Starting my own HTSContextTranslator");
            htsContextTranslator = new HTSContextTranslator();
            htsContextTranslator.startup();
        } else if (htsContextTranslator.getState() == MaryModule.MODULE_OFFLINE) {
            htsContextTranslator.startup();
        }

        try{
            htsEngine = (HTSEngine) Mary.getModule(HTSEngine.class);
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
        String basePath =
            System.getProperty("mary.base")
                + File.separator
                + "lib"
                + File.separator
                + "voices"
                + File.separator;

        logger.debug("Register HMM voices:");
        
        
        String voiceNames = MaryProperties.needProperty("hmm.voices.list");
        for (StringTokenizer st = new StringTokenizer(voiceNames); st.hasMoreTokens(); ) {
            String voiceName = st.nextToken();
            logger.debug("Voice '" + voiceName + "'");
            Locale locale = MaryUtils.string2locale(MaryProperties.needProperty("voice."+voiceName+".locale"));
            int samplingRate = MaryProperties.getInteger("voice."+voiceName+".samplingrate", 16000);
            
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
                locale, format, this, gender, -1, -1, -1, -1,
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
                MaryProperties.getBoolean("voice."+voiceName+".useGV"),    /* Use Global Variance in parameter generation */
                MaryProperties.getFilename("voice."+voiceName+".Fgvf"),    /* GV Model LF0 */
                MaryProperties.getFilename("voice."+voiceName+".Fgvm"),    /* GV Model MCP */
                MaryProperties.getFilename("voice."+voiceName+".Fgvs"),    /* GV Model STR */
                MaryProperties.getFilename("voice."+voiceName+".Fgva"),    /* GV Model MAG */
                MaryProperties.getFilename("voice."+voiceName+".FeaList"), /* Feature list file */
                MaryProperties.getFilename("voice."+voiceName+".Flab"),    /* label file, for testing*/
                MaryProperties.getFilename("voice."+voiceName+".Fif"),     /* Filter coefficients file for mixed excitation*/
                MaryProperties.getInteger("voice."+voiceName+".in"),       /* Number of filters */
                MaryProperties.getInteger("voice."+voiceName+".io"));      /* Number of taps per filter or filters order */
            Voice.registerVoice(v);
           
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
             MaryData in = new MaryData(x2u.inputType());
             Collection myVoices = Voice.getAvailableVoices(this);
             if (myVoices.size() == 0) {
                 return;
             }
             
             Voice v = (Voice) myVoices.iterator().next();
            
             String exampleText;
             if (v.getLocale().equals(Locale.GERMAN)) {
                 exampleText = MaryDataType.get("ACOUSTPARAMS_DE").exampleText();
             } else {
                 exampleText = MaryDataType.get("ACOUSTPARAMS_EN").exampleText();
             }
             in.readFrom(new StringReader(exampleText));
             in.setDefaultVoice(v);
             MaryData d1 = x2u.process(in);
             Utterance utt = (Utterance) d1.getUtterances().get(0);           
             MaryData freettsAcoustparams = new MaryData(x2u.outputType());
             List<Utterance> utts = new ArrayList<Utterance>();
             utts.add(utt);
             freettsAcoustparams.setUtterances(utts);
             freettsAcoustparams.setDefaultVoice(v);
             MaryData targetFeatures = targetFeatureLister.process(freettsAcoustparams);
             targetFeatures.setDefaultVoice(v);
             MaryData htsContext = htsContextTranslator.process(targetFeatures);
             htsContext.setDefaultVoice(v);
             
             /* add the ACOUSTPARAMS Document to the HTSCONTEXT Mary data Object */
             htsContext.setDocument(in.getDocument()); 
             MaryData audio = htsEngine.process(htsContext);
             
             assert audio.getAudio() != null;
             
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
        logger.info("Synthesizing one utterance.");
        Utterance utt = x2u.convert(tokensAndBoundaries, voice);
        MaryData freettsAcoustparams = new MaryData(x2u.outputType());
        List<Utterance> utts = new ArrayList<Utterance>();
        utts.add(utt);
        freettsAcoustparams.setUtterances(utts);
        freettsAcoustparams.setDefaultVoice(voice);
       
        try {
            MaryData targetFeatures = targetFeatureLister.process(freettsAcoustparams);
            targetFeatures.setDefaultVoice(voice);
            MaryData htsContext = htsContextTranslator.process(targetFeatures);
            
            htsContext.setDefaultVoice(voice);
            MaryData audio = htsEngine.process(htsContext);
            
            setActualDurations(tokensAndBoundaries, audio.getPlainText());
            
            return audio.getAudio();
            
                     
        } catch (Exception e) {
            throw new SynthesisException("HMM Synthesiser could not synthesise: ", e);
        }
    }
    
 
    public void setActualDurations(List<Element> tokensAndBoundaries, String durations) 
      throws SynthesisException {
      int i,j,index;
      NodeList no1, no2;
      NamedNodeMap att;
      Scanner s = null;
      Vector<String> ph = new Vector<String>();
      Vector<Integer> dur = new Vector<Integer>();
      String line, str[];
      Integer totalDur = 0;
      Integer auxDur = 0;
      
      
      s = new Scanner(durations).useDelimiter("\n");
      while(s.hasNext()) {
         line = s.next();
         str = line.split(" ");
         ph.add(htsContextTranslator.replaceBackTrickyPhones(str[0]));
         dur.add(Integer.valueOf(str[1]));
         
      }
      /* the duration of the first phoneme includes de duration of of the initial pause */
      if(ph.get(0).contentEquals("_")) {
         dur.set(1, (dur.get(1) + dur.get(0)) );
         ph.set(0, "");
         /* remove this element of the vector otherwise next time it will return the same */
         ph.set(0, "");
      }
      
      for (Element e : tokensAndBoundaries) {
        
         if( e.getTagName().contentEquals(MaryXML.TOKEN) ) {
          no1 = e.getChildNodes();
          for(i=0; i< no1.getLength(); i++) {
            if(no1.item(i).getNodeName().contentEquals(MaryXML.SYLLABLE)) {
           
              no2 = no1.item(i).getChildNodes();
              for(j=0; j<no2.getLength(); j++){
             
                if( no2.item(j).getNodeName().contentEquals(MaryXML.PHONE) ) {
                   att = no2.item(j).getAttributes(); 
                   
                   
                   if( ( index = ph.indexOf(att.getNamedItem("p").getNodeValue()) ) >= 0 ){ 
                     totalDur = totalDur + dur.elementAt(index);
                     att.getNamedItem("d").setNodeValue(dur.elementAt(index).toString());
                     att.getNamedItem("end").setNodeValue(totalDur.toString());                  
                     
                     /* remove this element of the vector otherwise next time it will return the same */
                     ph.set(index, "");
                   } else
                       throw new SynthesisException("problems phoneme " + att.getNamedItem("p") + " NOT found in HTSEngine phoneme set");
                   
                } else
                    throw new SynthesisException("setActualDurations: problem parsing PHONE in List<Element> tokensAndBoundaries.");               
              }              
            } 
          }
         } else if( e.getTagName().contentEquals(MaryXML.BOUNDARY) ) {
             if(e.hasAttribute("duration")) {
               index = ph.indexOf("_");  
             
               e.setAttribute("duration", dur.elementAt(index).toString());   
               /* remove this element of the vector otherwise next time it will return the same */
               ph.set(index, "");
             
             }
         } // else ignore whatever other label...
            
      }
       
        
        
    }
    

}
