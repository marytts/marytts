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
package de.dfki.lt.mary.modules;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.modules.synthesis.FestivalUttSectioner;
import de.dfki.lt.mary.modules.synthesis.MbrolaVoiceSectioner;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.modules.synthesis.VoiceSection;
import de.dfki.lt.mary.modules.synthesis.VoiceSectioner;
import de.dfki.lt.mary.util.MaryAudioUtils;

/**
 * A base class for a synthesis caller. This can work as a normal MARY module,
 * converting synthesis markup data into audio, or it can be indirectly called
 * from the corresponding Synthesizer.
 *
 * @author Marc Schr&ouml;der
 */

public abstract class SynthesisCallerBase extends InternalModule
{
    protected SynthesisCallerBase(String name, MaryDataType inputType,
			       MaryDataType outputType)
    {
        super(name, inputType, outputType);
    }
        
    public synchronized void startup() throws Exception {
        super.startup();
    }

    /**
      * Perform a power-on self test by processing some example input data.
      * This implementation does nothing; instead, the module test is carried out
      * via Synthesis in the WaveformSynthesizer associated with this Caller.
      * @throws Exception if the module does not work properly.
      */
     public synchronized void powerOnSelfTest()
     {
     }

    /**
     * From synthesis markup input <code>d</code>, create audio output of the
     * type specified by a preceding call to <code>setAudioType()</code>.
     * Returns a MaryData structure whose data is an input stream from which
     * audio data of the specified type can be read.
     */
    public MaryData process(MaryData d)
        throws TransformerConfigurationException, TransformerException,
               FileNotFoundException, IOException,
               ParserConfigurationException, SAXException, Exception
    {
        assert d.getAudioFileFormat() != null;
        assert getState() == MODULE_RUNNING;

        // As the input may contain multipe voice sections,
        // the challenge in this method is to join the audio data
        // resulting from individual synthesis calls with the respective
        // voice into one audio stream of the specified type.
        // Overall strategy:
        // * In input, identify the sections to be spoken by different voices
        // * For each of these sections,
        //   - synthesise the section in the voice's native audio format
        //   - convert to the common audio format if necessary / possible
        // * Join the audio input streams
        // * Return a MaryData structure containing a single audio input stream
        //   from which the audio data in the desired format can be read.

        String input = (String) d.getData();
        Voice defaultVoice = d.getDefaultVoice();
        if (defaultVoice == null) {
            defaultVoice = Voice.getDefaultVoice(Locale.GERMAN);
            assert defaultVoice != null;
            logger.info("No default voice associated with data. Assuming global default " +
                         defaultVoice.getName());
        }
        VoiceSectioner sectioner = null;
        if (MaryDataType.exists("FESTIVAL_UTT") && inputType().equals(MaryDataType.get("FESTIVAL_UTT"))) {
            sectioner = new FestivalUttSectioner(input, defaultVoice);
        } else {
            sectioner = new MbrolaVoiceSectioner(input, defaultVoice);
        }
        VoiceSection section = null;
        // A first pass identifying the voice with the highest sampling rate:
        AudioFormat commonAudioFormat = null;
        while ((section = sectioner.nextSection()) != null) {
            if (commonAudioFormat == null)
                commonAudioFormat = section.voice().dbAudioFormat();
            else if (section.voice().dbAudioFormat().getSampleRate()
                     > commonAudioFormat.getSampleRate())
                commonAudioFormat = section.voice().dbAudioFormat();
        }
        // And second pass:
        Vector audioInputStreams = new Vector();
        if (MaryDataType.exists("FESTIVAL_UTT") && inputType().equals(MaryDataType.get("FESTIVAL_UTT"))) {
            sectioner = new FestivalUttSectioner(input, defaultVoice);
        } else {
            sectioner = new MbrolaVoiceSectioner(input, defaultVoice);
        }
        section = null;
        while ((section = sectioner.nextSection()) != null) {
            AudioInputStream ais = synthesiseOneSection
                (section.text(), section.voice());
            // Conversion required?
            if (!ais.getFormat().equals(commonAudioFormat)) {
                // Attempt conversion; if not supported, log a warning
                // and provide the non-converted stream.
                logger.info("Conversion required for voice " +
                             section.voice().getName());
                if (AudioSystem.isConversionSupported
                    (commonAudioFormat, ais.getFormat())) {
                    AudioInputStream intermedStream = AudioSystem.
                        getAudioInputStream(commonAudioFormat, ais);
                    ais = intermedStream;
                } else { // conversion not supported
                    logger.warn
                        ("Conversion to common audio format " +
                         commonAudioFormat +
                         " not supported. Providing voice default instead: " +
                         ais.getFormat());
                }
            }
			audioInputStreams.add(ais);
        }
        // Create a single audio input stream:
        AudioInputStream singleStream = MaryAudioUtils.createSingleAudioInputStream(audioInputStreams);

        // Conversion to target format needed?
        // Attempt conversion; if not supported, log a warning
        // and provide the non-converted stream.
        AudioInputStream convertedStream = null;
        AudioFormat targetFormat = d.getAudioFileFormat().getFormat();
        if (targetFormat.matches(commonAudioFormat)) {
            // requested audio format is the default format
            logger.info("Conversion not required.");
            convertedStream = singleStream;
        } else { // conversion required
            logger.info("Conversion required.");
            if (AudioSystem.isConversionSupported
                (targetFormat, commonAudioFormat)) {
                convertedStream = AudioSystem.getAudioInputStream
                    (targetFormat, singleStream);
            } else { // conversion not supported
                logger.warn("Requested audio format not supported: " +
                            targetFormat +
                            ". Providing default instead: " +
                            commonAudioFormat);
                convertedStream = singleStream;
            }
        }
        MaryData result = new MaryData(MaryDataType.get("AUDIO"));
        result.setAudioFileFormat(d.getAudioFileFormat());
        result.readFrom(convertedStream, null);
        return result;
    }
    


    /**
     * Synthesise one chunk of synthesis markup with a given voice.
     * @param synthesisMarkup the input data in the native format expected by
     * the synthesis engine
     * @param voice the voice with which to synthesise the data
     * @return an AudioInputStream in the native audio format of the voice
     */
    public abstract AudioInputStream synthesiseOneSection(String synthesisMarkup, Voice voice) throws IOException;
}
