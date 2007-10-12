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

import de.dfki.lt.mary.Mary;
import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.MaryModule;
import de.dfki.lt.mary.modules.MaryXMLToMbrola;
import de.dfki.lt.mary.modules.MbrolaCaller;
import de.dfki.lt.mary.modules.synthesis.Voice.Gender;
import de.dfki.lt.mary.util.MaryUtils;

/**
 * The Mbrola waveform synthesizer wrapper.
 */
public class MbrolaSynthesizer implements WaveformSynthesizer {
    private MaryXMLToMbrola maryxmlToMbrola;
    private MbrolaCaller mbrolaCaller;
    private Logger logger;

    public MbrolaSynthesizer() {
    }

    public void startup() throws Exception {
        logger = Logger.getLogger(this.toString());
        // Try to get instances of our tools from Mary; if we cannot get them,
        // instantiate new objects.
        try{
        maryxmlToMbrola =
            (MaryXMLToMbrola) Mary.getModule(MaryXMLToMbrola.class);
        } catch (NullPointerException npe){
            maryxmlToMbrola = null;
        }
        if (maryxmlToMbrola == null) {
            logger.info("Starting my own MaryXMLToMbrola");
            maryxmlToMbrola = new MaryXMLToMbrola();
            maryxmlToMbrola.startup();
        } else if (maryxmlToMbrola.getState() == MaryModule.MODULE_OFFLINE) {
            maryxmlToMbrola.startup();
        }
        String mbrolaCallerProperty;
        if (System.getProperty("os.name").startsWith("Windows")) {
            mbrolaCallerProperty = "mbrolasynthesizer.mbrolacaller.class.win32"; 
        } else {
            mbrolaCallerProperty = "mbrolasynthesizer.mbrolacaller.class";
        }
		Class mbrolaClass = MaryProperties.needClass(mbrolaCallerProperty);
		Object obj;
		try {
		    obj = Mary.getModule(mbrolaClass);
		} catch (NullPointerException npe){
		    obj = null;
		}
        if (obj == null) {
            logger.info("Starting my own MbrolaCaller (" + mbrolaClass.getName() + ")");
            obj = mbrolaClass.newInstance();
        }
        if (!(obj instanceof MbrolaCaller)) {
            throw new ClassCastException("Class `" + mbrolaClass.getName() +
                "' is not an MbrolaCaller. Check property `" + mbrolaCallerProperty +
                "' in configuration files");
        }
        mbrolaCaller = (MbrolaCaller) obj;
        if (mbrolaCaller.getState() == MaryModule.MODULE_OFFLINE) {
            mbrolaCaller.startup();
        }

        // Register Mbrola voices:
        String basePath =
            System.getProperty("mary.base")
                + File.separator
                + "lib"
                + File.separator
                + "voices"
                + File.separator;

        logger.debug("Register MBROLA voices:");
        String voiceNames = MaryProperties.needProperty("mbrola.voices.list");
        for (StringTokenizer st = new StringTokenizer(voiceNames); st.hasMoreTokens(); ) {
            String voiceName = st.nextToken();
            String path = MaryProperties.getFilename("voice."+voiceName+".path", basePath + voiceName + File.separator + voiceName);
            if (new File(path).exists()) {
                logger.debug("Voice '" + voiceName + "'");
                Locale locale = MaryUtils.string2locale(MaryProperties.needProperty("voice."+voiceName+".locale"));
                int samplingRate = MaryProperties.getInteger("voice."+voiceName+".samplingrate", 16000);
                
                Gender gender = new Gender(MaryProperties.needProperty("voice."+voiceName+".gender"));
                int topStart = MaryProperties.needInteger("voice."+voiceName+".topline.start");
                int topEnd = MaryProperties.needInteger("voice."+voiceName+".topline.end");
                int baseStart = MaryProperties.needInteger("voice."+voiceName+".baseline.start");
                int baseEnd = MaryProperties.needInteger("voice."+voiceName+".baseline.end");
                String vqString = MaryProperties.getProperty("voice."+voiceName+".voicequalities", null);
                String[] voiceQualities = null;
                if (vqString != null) voiceQualities = vqString.split("\\s+");
                String missingDiphones = MaryProperties.getFilename("voice."+voiceName+".missingdiphones", null);
                Voice v = new MbrolaVoice (path,
                        new String[] { voiceName },
                        locale,
                        mbrolaAudioFormat(samplingRate),
                        this,
                        gender,
                        topStart, topEnd, baseStart, baseEnd,
                        voiceQualities,
                        missingDiphones);
                Voice.registerVoice(v);
            } else { // voice not present
                logger.debug("Voice `"+voiceName+"' is not present. Skipping.");
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
             MaryDataType inType = maryxmlToMbrola.inputType();
             Collection voices = Voice.getAvailableVoices(this);
             if (voices.isEmpty())
                 throw new Error("No MBROLA voices present");
             Voice v = (Voice) voices.iterator().next();
             assert v != null;
             MaryData in = new MaryData(inType);
             MaryDataType langSpecType = MaryDataType.getLanguageSpecificVersion(inType, v.getLocale());
             if (langSpecType == null)
                 throw new NullPointerException("Could not get language specific data type for type "+ inType.name() + ", language "+v.getLocale());
             in.readFrom(new StringReader(langSpecType.exampleText()));
             in.setDefaultVoice(v);
             MaryData mbrola = maryxmlToMbrola.process(in);
             mbrola.setAudioFileFormat(new AudioFileFormat(
                     AudioFileFormat.Type.WAVE, Voice.AF22050, AudioSystem.NOT_SPECIFIED)
                 );
             mbrola.setDefaultVoice(v);
             mbrolaCaller.process(mbrola);
         } catch (Throwable t) {
             throw new Error("Module " + toString() + ": Power-on self test failed.", t);
         }
         logger.info("Power-on self test complete.");
     }

    public String toString() {
        return "MbrolaSynthesizer";
    }

    public AudioInputStream synthesize(List<Element> tokensAndBoundaries, Voice voice)
        throws SynthesisException {
        if (!voice.synthesizer().equals(this)) {
            throw new IllegalArgumentException(
                "Voice " + voice.getName() + " is not an MBROLA voice.");
        }
        logger.info("Synthesizing one sentence.");
        // 1. Convert into MBROLA .pho format.
        List<Element> phonemesAndBoundaries = new ArrayList<Element>();
        for (Element element : tokensAndBoundaries) {
            if (element.getTagName().equals(MaryXML.TOKEN)) {
                NodeList nl = element.getElementsByTagName(MaryXML.PHONE);
                for (int i = 0; i < nl.getLength(); i++) {
                    phonemesAndBoundaries.add((Element)nl.item(i));
                }
            } else if (element.getTagName().equals(MaryXML.BOUNDARY)) {
                phonemesAndBoundaries.add(element);
            } else {
                throw new IllegalArgumentException(
                    "Expected only <t> and <boundary> elements, got <"
                        + element.getTagName()
                        + ">");
            }
        }
        String pho =
            maryxmlToMbrola.convertToMbrola(phonemesAndBoundaries, voice);

        if (Boolean.getBoolean("democenter.workaround")) {
            pho = pho + "_ 300\n";
        }

        // 2. Call MBROLA synthesizer.
        /*Vector audioInputStreams = new Vector();
        long totalFrames = 0;
        StringTokenizer st = new StringTokenizer(pho, "#");
        try {
            while (st.hasMoreTokens()) {
                AudioInputStream ais =
                    mbrolaCaller.synthesiseOneSection(st.nextToken(), voice);
                audioInputStreams.add(ais);
                totalFrames += ais.getFrameLength();
            }
        } catch (IOException ioe) {
            throw new SynthesisException("Cannot synthesise", ioe);
        }
        assert !audioInputStreams.isEmpty();
        return MaryAudioUtils.createSingleAudioInputStream(audioInputStreams);*/
        AudioInputStream ais;
        try {
            ais = mbrolaCaller.synthesiseOneSection(pho, voice);
        } catch (IOException ioe) {
            throw new SynthesisException("Cannot synthesise", ioe);
        }
        assert ais != null;
        return ais;
    }

    public static AudioFormat mbrolaAudioFormat(int samplingRate)
    {
        boolean bigEndian;
        if (System.getProperty("os.name").equals("Mac OS X")) {
            bigEndian = true; // big-endian
            // Special treatment for Mac OS X, because the MBROLA binary 
            // (which stems from PowerPC times) produces big-endian 
            // even on an i386 machine
        } else if (System.getProperty("os.arch").equals("x86") ||
                System.getProperty("os.arch").equals("i386")) {
            bigEndian = false;
        } else {
            // all others -- e.g., sparc
            bigEndian = true;
        }
        return  new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
         samplingRate, // samples per second
         16, // bits per sample
         1, // mono
         2, // nr. of bytes per frame
         samplingRate, // nr. of frames per second
         bigEndian);
    }
    
    public static boolean isMbrolaVoice(Voice voice)
    {
        if (voice == null) throw new NullPointerException("Received null argument");
        WaveformSynthesizer ws = voice.synthesizer();
        if (ws == null) throw new NullPointerException("Voice has no waveform synthesizer");
        return (ws instanceof MbrolaSynthesizer);
    }
}
