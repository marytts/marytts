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

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioInputStream;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.sun.speech.freetts.Utterance;

import de.dfki.lt.mary.Mary;
import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.modules.FestivalCaller;
import de.dfki.lt.mary.modules.FreeTTS2FestivalUtt;
import de.dfki.lt.mary.modules.MaryModule;
import de.dfki.lt.mary.modules.XML2UttAcoustParams;
import de.dfki.lt.mary.util.MaryUtils;

/**
 * The Mbrola waveform synthesizer wrapper.
 */
public class FestivalSynthesizer implements WaveformSynthesizer {
    private XML2UttAcoustParams x2u;
    private FreeTTS2FestivalUtt f2f;
    private FestivalCaller caller;
    /**
     * A map with Voice objects as keys, and Lists of UtteranceProcessors as values.
     * Idea: For a given voice, find the list of utterance processors to apply. 
     */
    private Logger logger;

    public FestivalSynthesizer() {
    }

    public void startup() throws Exception {
        logger = Logger.getLogger(this.toString());
        // Try to get instances of our tools from Mary; if we cannot get them,
        // instantiate new objects.
        x2u = (XML2UttAcoustParams) Mary.getModule(XML2UttAcoustParams.class);
        if (x2u == null) {
            logger.info("Starting my own XML2UttAcoustParams");
            x2u = new XML2UttAcoustParams();
            x2u.startup();
        } else if (x2u.getState() == MaryModule.MODULE_OFFLINE) {
            x2u.startup();
        }
        f2f = (FreeTTS2FestivalUtt) Mary.getModule(FreeTTS2FestivalUtt.class);
        if (f2f == null) {
            logger.info("Starting my own FreeTTS2FestivalUtt");
            f2f = new FreeTTS2FestivalUtt();
            f2f.startup();
        } else if (f2f.getState() == MaryModule.MODULE_OFFLINE) {
            f2f.startup();
        }
        caller = (FestivalCaller) Mary.getModule(FestivalCaller.class);
        if (caller == null) {
            logger.info("Starting my own FestivalCaller");
            caller = new FestivalCaller();
            caller.startup();
        } else if (caller.getState() == MaryModule.MODULE_OFFLINE) {
            caller.startup();
        }

        // Register Festival voices:
        String voiceNames = MaryProperties.needProperty("festival.voices.list");
        for (StringTokenizer st = new StringTokenizer(voiceNames); st.hasMoreTokens(); ) {
            String voiceName = st.nextToken();
            int pitch = MaryProperties.needInteger("festival.voices." + voiceName + ".pitch");
            int range = MaryProperties.needInteger("festival.voices." + voiceName + ".range");
            int topStart = pitch + range;
            int topEnd = pitch;
            int baseStart = pitch;
            int baseEnd = pitch - range;
            Voice voice = new Voice(new String[] {voiceName},
                MaryUtils.string2locale(MaryProperties.needProperty("festival.voices." + voiceName + ".locale")),
                Voice.AF16000,
                this,
                new Voice.Gender(MaryProperties.needProperty("festival.voices." + voiceName + ".gender")),
                topStart, topEnd, baseStart, baseEnd);
            Voice.registerVoice(voice);
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
             in.readFrom(new StringReader(MaryDataType.get("ACOUSTPARAMS_EN").exampleText()));
             in.setDefaultVoice(v);
             MaryData d1 = x2u.process(in);
             Utterance utt = (Utterance) d1.getUtterances().get(0);
             AudioInputStream ais = process(utt, v);
             assert ais != null;
         } catch (Throwable t) {
             throw new Error("Module " + toString() + ": Power-on self test failed.", t);
         }
         logger.info("Power-on self test complete.");
     }

    public String toString() {
        return "FestivalSynthesizer";
    }

    public AudioInputStream synthesize(List<Element> tokensAndBoundaries, Voice voice)
        throws SynthesisException {
        if (!voice.synthesizer().equals(this)) {
            throw new IllegalArgumentException(
                "Voice " + voice.getName() + " is not a Festival voice.");
        }
        logger.info("Synthesizing one utterance.");
        Utterance utt = x2u.convert(tokensAndBoundaries, voice);
        AudioInputStream ais = process(utt, voice);
        assert ais != null;
        return ais;
    }

    private AudioInputStream process(Utterance utt, Voice v) throws SynthesisException
    {
        AudioInputStream ais = null;
        try {
            String festivalUtt = f2f.convertUtt(utt);
            ais = caller.synthesiseOneSection(festivalUtt, v);
        } catch (IOException pe) {
            throw new SynthesisException("cannot synthesize", pe);
        }
        return ais;
    }

    public static boolean isFestivalVoice(Voice voice)
    {
        if (voice == null) throw new NullPointerException("Received null argument");
        WaveformSynthesizer ws = voice.synthesizer();
        if (ws == null) throw new NullPointerException("Voice has no waveform synthesizer");
        return (ws instanceof FestivalSynthesizer);
    }

}
