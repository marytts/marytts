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

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.sun.speech.freetts.ProcessException;
import com.sun.speech.freetts.Utterance;
import com.sun.speech.freetts.UtteranceProcessor;
import com.sun.speech.freetts.VoiceDirectory;
import com.sun.speech.freetts.relp.LPCResult;

import de.dfki.lt.freetts.ConcatenativeVoice;
import de.dfki.lt.mary.Mary;
import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.NoSuchPropertyException;
import de.dfki.lt.mary.modules.MaryModule;
import de.dfki.lt.mary.modules.XML2UttAcoustParams;
//import de.dfki.lt.mary.unitselection.voices.ArcticVoiceBuilder;

/**
 * The Mbrola waveform synthesizer wrapper.
 */
public class FreeTTSSynthesizer implements WaveformSynthesizer {
    private XML2UttAcoustParams x2u;
    /**
     * A map with Voice objects as keys, and Lists of UtteranceProcessors as values.
     * Idea: For a given voice, find the list of utterance processors to apply. 
     */
    private Map processorsByVoice;
    private Logger logger;

    public FreeTTSSynthesizer() {
    }

    public void startup() throws Exception
    {
        logger = Logger.getLogger(this.toString());
        // Try to get instances of our tools from Mary; if we cannot get them,
        // instantiate new objects.
        try{ 
            x2u = (XML2UttAcoustParams) Mary.getModule(XML2UttAcoustParams.class);
        } catch(NullPointerException npe){
            x2u = null;
        }
        if (x2u == null) {
            logger.info("Starting my own XML2UttAcoustParams");
            x2u = new XML2UttAcoustParams();
            x2u.startup();
        } else if (x2u.getState() == MaryModule.MODULE_OFFLINE) {
            x2u.startup();
        }
        processorsByVoice = new HashMap();

        // Register FreeTTS voices:
        logger.debug("Register FreeTTS voices:");
        /**
        ArcticVoiceBuilder voiceBuilder = new ArcticVoiceBuilder();
        String voiceNames = MaryProperties.getProperty("freetts.voices.list",
                "");
        for (StringTokenizer st = new StringTokenizer(voiceNames); st
                .hasMoreTokens();) {
            String voiceName = st.nextToken();
            // take the time
            long time = System.currentTimeMillis();
            com.sun.speech.freetts.Voice freettsVoice = voiceBuilder
                    .buildVoice(voiceName);
            logger.debug("Voice '" + freettsVoice + "'");
            if (!freettsVoice.isLoaded()) {
                logger.debug("...allocating");
                freettsVoice.allocate();
            }
            int rate = MaryProperties.getInteger("voice."
                    + freettsVoice.getName() + ".rate");
            if (rate != -1)
                freettsVoice.setRate(rate);
            int pitch = MaryProperties.getInteger("voice."
                    + freettsVoice.getName() + ".pitch");
            if (pitch != -1)
                freettsVoice.setPitch(pitch);
            int range = MaryProperties.getInteger("voice."
                    + freettsVoice.getName() + ".range");
            if (range != -1)
                freettsVoice.setPitchRange(range);
            UtteranceProcessor unitSel = ((ConcatenativeVoice) freettsVoice)
                    .getUnitSelector();
            UtteranceProcessor pitchmarkGen = ((ConcatenativeVoice) freettsVoice)
                    .getPitchmarkGenerator();
            UtteranceProcessor unitConcat = ((ConcatenativeVoice) freettsVoice)
                    .getUnitConcatenator();
            Voice maryVoice = new Voice(freettsVoice, this);
            UtteranceProcessor[] processors = new UtteranceProcessor[] {
                    unitSel, pitchmarkGen, unitConcat };
            processorsByVoice.put(maryVoice, Arrays.asList(processors));
            Voice.registerVoice(maryVoice, freettsVoice);
            long newtime = System.currentTimeMillis() - time;
            logger.debug("Loading of voice "+voiceName+" took "+newtime+" milliseconds");
        }
        **/
        String voiceClassNames = MaryProperties.getProperty(
                "freetts.voice.classes.list", "");
        for (StringTokenizer st = new StringTokenizer(voiceClassNames); st.hasMoreTokens();) {
            String voiceClassName = st.nextToken();
            Object voiceDir = Class.forName(voiceClassName).newInstance();
            if (!(voiceDir instanceof VoiceDirectory)) {
                throw new NoSuchPropertyException(
                        "Invalid entry in MARY config file: `" + voiceDir
                                + "' is not an instance of VoiceDirectory.");
            }
            assert voiceDir instanceof VoiceDirectory;
            logger.debug("Voice directory '" + voiceClassName + "':");
            com.sun.speech.freetts.Voice[] va = ((VoiceDirectory) voiceDir)
                    .getVoices();
            for (int j = 0; j < va.length; j++) {
                com.sun.speech.freetts.Voice freettsVoice = va[j];
                logger.debug("Voice '" + freettsVoice + "'");
                if (MaryProperties.needAutoBoolean("freetts.lexicon.preload") && !freettsVoice.isLoaded()) {
                    logger.debug("...allocating");
                    freettsVoice.allocate();
                }
                int rate = MaryProperties.getInteger("voice."
                        + freettsVoice.getName() + ".rate");
                if (rate != -1)
                    freettsVoice.setRate(rate);
                int pitch = MaryProperties.getInteger("voice."
                        + freettsVoice.getName() + ".pitch");
                if (pitch != -1)
                    freettsVoice.setPitch(pitch);
                int range = MaryProperties.getInteger("voice."
                        + freettsVoice.getName() + ".range");
                if (range != -1)
                    freettsVoice.setPitchRange(range);
                UtteranceProcessor unitSel = ((ConcatenativeVoice) freettsVoice)
                        .getUnitSelector();
                UtteranceProcessor pitchmarkGen = ((ConcatenativeVoice) freettsVoice)
                        .getPitchmarkGenerator();
                UtteranceProcessor unitConcat = ((ConcatenativeVoice) freettsVoice)
                        .getUnitConcatenator();
                Voice maryVoice = new Voice(freettsVoice, this);
                UtteranceProcessor[] processors = new UtteranceProcessor[] {
                        unitSel, pitchmarkGen, unitConcat };
                processorsByVoice.put(maryVoice, Arrays.asList(processors));
                Voice.registerVoice(maryVoice, freettsVoice);
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
         try {
             MaryData in = new MaryData(x2u.inputType());
             Collection myVoices = Voice.getAvailableVoices(this);
             if (myVoices.size() == 0) {
                 return;
             }
             
             Voice v = (Voice) myVoices.iterator().next();
             com.sun.speech.freetts.Voice freettsVoice = FreeTTSVoices.getFreeTTSVoice(v);
             assert freettsVoice != null;
             if (!freettsVoice.getDomain().equals("general")) {
                 logger.info("Cannot perform power-on self test using limited-domain voice '" + v.getName() + "' - skipping.");
                 return;
             }
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
             verifyDebugLog(utt, x2u.name());
             process(utt, v);
             AudioInputStream ais = extractAudio(utt);
             assert ais != null;
         } catch (Throwable t) {
             throw new Error("Module " + toString() + ": Power-on self test failed.", t);
         }
         logger.info("Power-on self test complete.");
     }

    public String toString() {
        return "FreeTTSSynthesizer";
    }

    public AudioInputStream synthesize(List<Element> tokensAndBoundaries, Voice voice)
        throws SynthesisException {
        if (!voice.synthesizer().equals(this)) {
            throw new IllegalArgumentException(
                "Voice " + voice.getName() + " is not a FreeTTS voice.");
        }
        logger.info("Synthesizing one utterance.");
        Utterance utt = x2u.convert(tokensAndBoundaries, voice);
        verifyDebugLog(utt, x2u.name());
        process(utt, voice);
        AudioInputStream ais = extractAudio(utt);
        assert ais != null;
        return ais;
    }

    private void process(Utterance utt, Voice v) throws SynthesisException
    {
        List voiceProcessors = (List) processorsByVoice.get(v);
        if (voiceProcessors == null)
            throw new SynthesisException("Cannot find a processor list for voice " + v.toString());
        try {
            for (Iterator it = voiceProcessors.iterator(); it.hasNext(); ) {
                UtteranceProcessor up = (UtteranceProcessor) it.next();
                up.processUtterance(utt);
                verifyDebugLog(utt, up.toString());
            }
        } catch (ProcessException pe) {
            throw new SynthesisException("cannot synthesize", pe);
        }
    }

    private void verifyDebugLog(Utterance utt, String moduleName)
    {
        if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            utt.dump(pw, 2, moduleName, true); // padding, justRelations
            logger.debug("Constructed the following Utterance:");
            logger.debug(sw.toString());
        }

    }
    
    /**
     * For a given Utterance containing LPC-encoded audio,
     * extract the audio and return it as an AudioInputStream.
     * @param utterance
     * @return a non-null AudioInputStream
     */
    public AudioInputStream extractAudio(Utterance utterance)
    {
        if (utterance == null)
            throw new NullPointerException("Received null utterance");
        LPCResult lpcResult = (LPCResult) utterance.getObject("target_lpcres");
        if (lpcResult == null)
            throw new IllegalArgumentException("Utterance does not contain lpcresult");
        /*
        ByteArrayAudioPlayer baap = new ByteArrayAudioPlayer();
        lpcResult.playWave(baap);
        AudioInputStream ais = baap.getAudioInputStream();
        */
        byte[] audio = lpcResult.getWaveSamples();
        ByteArrayInputStream bais = new ByteArrayInputStream(audio);
        AudioFormat af = FreeTTSVoices.getMaryVoice(utterance.getVoice()).dbAudioFormat();
        int samplesize = af.getSampleSizeInBits();
        if (samplesize == AudioSystem.NOT_SPECIFIED)
            samplesize = 16; // usually 16 bit data
        long lengthInSamples = audio.length / (samplesize/8);
        AudioInputStream ais = new AudioInputStream(bais, af, lengthInSamples);

        return ais;
    }

    public static boolean isFreeTTSVoice(Voice voice)
    {
        if (voice == null) throw new NullPointerException("Received null argument");
        WaveformSynthesizer ws = voice.synthesizer();
        if (ws == null) throw new NullPointerException("Voice has no waveform synthesizer");
        return (ws instanceof FreeTTSSynthesizer);
    }

}
