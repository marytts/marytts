/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.modules;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.modules.synthesis.Voice;
import marytts.util.data.audio.AppendableSequenceAudioInputStream;

import org.xml.sax.SAXException;

/**
 * A base class for a synthesis caller. This can work as a normal MARY module, converting synthesis markup data into audio, or it
 * can be indirectly called from the corresponding Synthesizer.
 * 
 * @author Marc Schr&ouml;der
 */

public abstract class SynthesisCallerBase extends InternalModule {
	protected SynthesisCallerBase(String name, MaryDataType inputType, MaryDataType outputType) {
		super(name, inputType, outputType, null);
	}

	public synchronized void startup() throws Exception {
		super.startup();
	}

	/**
	 * Perform a power-on self test by processing some example input data. This implementation does nothing; instead, the module
	 * test is carried out via Synthesis in the WaveformSynthesizer associated with this Caller.
	 */
	public synchronized void powerOnSelfTest() {
	}

	/**
	 * From synthesis markup input <code>d</code>, create audio output of the type specified by a preceding call to
	 * <code>setAudioType()</code>. Returns a MaryData structure whose data is an input stream from which audio data of the
	 * specified type can be read.
	 * 
	 * @param d
	 *            d
	 * @throws TransformerConfigurationException
	 *             TransformerConfigurationException
	 * @throws TransformerException
	 *             TransformerException
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 * @throws IOException
	 *             IOException
	 * @throws ParserConfigurationException
	 *             ParserConfigurationException
	 * @throws SAXException
	 *             SAXException
	 * @throws Exception
	 *             Exception
	 */
	public MaryData process(MaryData d) throws TransformerConfigurationException, TransformerException, FileNotFoundException,
			IOException, ParserConfigurationException, SAXException, Exception {
		assert d.getAudioFileFormat() != null;
		assert getState() == MODULE_RUNNING;

		// As the input may contain multipe voice sections,
		// the challenge in this method is to join the audio data
		// resulting from individual synthesis calls with the respective
		// voice into one audio stream of the specified type.
		// Overall strategy:
		// * In input, identify the sections to be spoken by different voices
		// * For each of these sections,
		// - synthesise the section in the voice's native audio format
		// - convert to the common audio format if necessary / possible
		// * Join the audio input streams
		// * Return a MaryData structure containing a single audio input stream
		// from which the audio data in the desired format can be read.

		String input = (String) d.getData();
		Voice defaultVoice = d.getDefaultVoice();
		if (defaultVoice == null) {
			defaultVoice = Voice.getDefaultVoice(Locale.GERMAN);
			assert defaultVoice != null;
			logger.info("No default voice associated with data. Assuming global default " + defaultVoice.getName());
		}

		MaryData result = new MaryData(outputType(), d.getLocale());
		result.setAudioFileFormat(d.getAudioFileFormat());
		AudioFormat targetFormat = d.getAudioFileFormat().getFormat();
		if (d.getAudio() != null) {
			// This (empty) AppendableSequenceAudioInputStream object allows a
			// thread reading the audio data on the other "end" to get to our data as we are producing it.
			assert d.getAudio() instanceof AppendableSequenceAudioInputStream;
			result.setAudio(d.getAudio());
		}

		return result;
	}

	protected AudioInputStream convertIfNeededAndPossible(AudioInputStream input, AudioFormat format, String voiceName) {
		if (input.getFormat().equals(format)) {
			return input;
		}
		// Attempt conversion; if not supported, log a warning
		// and provide the non-converted stream.
		logger.info("Conversion required for voice " + voiceName);
		if (AudioSystem.isConversionSupported(format, input.getFormat())) {
			return AudioSystem.getAudioInputStream(format, input);
		}
		// conversion not supported
		logger.warn("Conversion to audio format " + format + " not supported. Providing voice default instead: "
				+ input.getFormat());
		return input;
	}

	/**
	 * Synthesise one chunk of synthesis markup with a given voice.
	 * 
	 * @param synthesisMarkup
	 *            the input data in the native format expected by the synthesis engine
	 * @param voice
	 *            the voice with which to synthesise the data
	 * @throws IOException
	 *             IOException
	 * @return an AudioInputStream in the native audio format of the voice
	 */
	public abstract AudioInputStream synthesiseOneSection(String synthesisMarkup, Voice voice) throws IOException;
}
