/**
 * Copyright 2000-2006 DFKI GmbH.
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
package de.dfki.lt.mary.modules.synthesis;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
import de.dfki.lt.mary.modules.en.HTSContextTranslator;
import de.dfki.lt.mary.modules.en.HTSEngine;
import de.dfki.lt.mary.modules.en.TargetFeatureLister;
import de.dfki.lt.mary.modules.synthesis.Voice.Gender;
import de.dfki.lt.mary.util.MaryUtils;

/**
 * The Mbrola waveform synthesizer wrapper.
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
            
            Voice v = new Voice (null,
                    new String[] { voiceName },
                    locale,
                    format,
                    this,
                    gender,
                    -1, -1, -1, -1,
                    null,
                    null);
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
         // TODO: add meaningful power-on self test
     }

    public String toString() {
        return "HMMSynthesizer";
    }

    public AudioInputStream synthesize(List tokensAndBoundaries, Voice voice)
        throws SynthesisException {
        if (!voice.synthesizer().equals(this)) {
            throw new IllegalArgumentException(
                "Voice " + voice.getName() + " is not an HMM voice.");
        }
        logger.info("Synthesizing one sentence.");
        logger.info("Synthesizing one utterance.");
        Utterance utt = x2u.convert(tokensAndBoundaries, voice);
        MaryData freettsAcoustparams = new MaryData(x2u.outputType());
        List utts = new ArrayList();
        utts.add(utt);
        freettsAcoustparams.setUtterances(utts);
        try {
            MaryData targetFeatures = targetFeatureLister.process(freettsAcoustparams);
            MaryData htsContext = htsContextTranslator.process(targetFeatures);
            MaryData audio = htsEngine.process(htsContext);
            return audio.getAudio();
        } catch (Exception e) {
            throw new SynthesisException("HMM Synthesiser could not synthesise: ", e);
        }
    }

}
