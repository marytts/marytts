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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.SynthesisException;
import marytts.modules.synthesis.Voice;
import marytts.modules.synthesis.WaveformSynthesizer;
import marytts.server.MaryProperties;
import marytts.signalproc.effects.EffectsApplier;
import marytts.util.data.audio.AppendableSequenceAudioInputStream;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

/**
 * The synthesis module.
 *
 * @author Marc Schr&ouml;der
 */

public class Synthesis extends InternalModule {
	private List<WaveformSynthesizer> waveformSynthesizers;
	private EffectsApplier effects;

	public Synthesis() {
		super("Synthesis", MaryDataType.ACOUSTPARAMS, MaryDataType.AUDIO, null);
	}

	public void startup() throws Exception {
		startupSynthesizers();
		super.startup();
	}

	private void startupSynthesizers() throws ClassNotFoundException, InstantiationException, Exception {
		waveformSynthesizers = new ArrayList<WaveformSynthesizer>();
		for (String synthClassName : MaryProperties.synthesizerClasses()) {
			WaveformSynthesizer ws = (WaveformSynthesizer) Class.forName(synthClassName).newInstance();
			ws.startup();
			waveformSynthesizers.add(ws);
		}
	}

	/**
	 * Perform a power-on self test by processing some example input data.
	 * 
	 * @throws Error
	 *             if the module does not work properly.
	 */
	public synchronized void powerOnSelfTest() throws Error {
		for (Iterator<WaveformSynthesizer> it = waveformSynthesizers.iterator(); it.hasNext();) {
			WaveformSynthesizer ws = it.next();
			ws.powerOnSelfTest();
		}
	}

	public MaryData process(MaryData d) throws Exception {
		// We produce audio data, so we expect some helpers in our input:
		assert d.getAudioFileFormat() != null : "Audio file format is not set!";
		Document doc = d.getDocument();
		// As the input may contain multipe voice sections,
		// the challenge in this method is to join the audio data
		// resulting from individual synthesis calls with the respective
		// voice into one audio stream of the specified type.
		// Overall strategy:
		// * In input, identify the sections to be spoken by different voices
		// * For each of these sections,
		// - synthesise the section in the voice's native audio format
		// - convert to the common audio format if necessary / possible
		// * Join the audio input streams by appending each part to the output MaryData audio.
		// * Return a MaryData structure containing a single audio input stream
		// from which the audio data in the desired format can be read.

		AudioFormat targetFormat = d.getAudioFileFormat().getFormat();
		Voice defaultVoice = d.getDefaultVoice();
		String defaultStyle = d.getDefaultStyle();
		String defaultEffects = d.getDefaultEffects();
		Locale locale = d.getLocale();
		String outputParams = d.getOutputParams();

		if (defaultVoice == null) {
			defaultVoice = Voice.getDefaultVoice(locale);
			if (defaultVoice == null) {
				throw new SynthesisException("No voice available for locale '" + locale + "'");
			}
			logger.info("No default voice associated with data. Assuming global default " + defaultVoice.getName());
		}

		MaryData result = new MaryData(outputType(), d.getLocale());
		// Also remember XML document in "AUDIO" output data, to keep track of phone durations:
		result.setDocument(doc);
		result.setAudioFileFormat(d.getAudioFileFormat());
		if (d.getAudio() != null) {
			// This (empty) AppendableSequenceAudioInputStream object allows a
			// thread reading the audio data on the other "end" to get to our data as we are producing it.
			assert d.getAudio() instanceof AppendableSequenceAudioInputStream;
			result.setAudio(d.getAudio());
		}

		NodeIterator it = ((DocumentTraversal) doc).createNodeIterator(doc, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(
				new String[] { MaryXML.TOKEN, MaryXML.BOUNDARY, MaryXML.NONVERBAL }), false);
		List<Element> elements = new ArrayList<Element>();
		Element element = null;
		Voice currentVoice = defaultVoice;
		String currentStyle = defaultStyle;
		String currentEffect = defaultEffects;
		Element currentVoiceElement = null;
		Element currentSentence = null;
		while ((element = (Element) it.nextNode()) != null) {
			Element v = (Element) MaryDomUtils.getAncestor(element, MaryXML.VOICE);
			Element s = (Element) MaryDomUtils.getAncestor(element, MaryXML.SENTENCE);

			// Check for non-verbal elements
			if (element.getNodeName().equals(MaryXML.NONVERBAL)) {
				if (v != null) {
					Voice newvoice = Voice.getVoice(v);
					if (newvoice != null && newvoice.hasVocalizationSupport()) {
						AudioInputStream ais = newvoice.getVocalizationSynthesizer().synthesize(newvoice, d.getAudioFileFormat(),
								element);
						result.appendAudio(ais);
					}
				}
				continue;
			}

			// Chunk at boundaries between voice sections
			if (v == null) {
				if (currentVoiceElement != null) {
					// We have just left a voice section
					if (!elements.isEmpty()) {
						AudioInputStream ais = synthesizeOneSection(elements, currentVoice, currentStyle, currentEffect,
								targetFormat, outputParams);
						if (ais != null) {
							result.appendAudio(ais);
						}
						elements.clear();
					}
					currentVoice = defaultVoice;
					currentStyle = defaultStyle;
					currentEffect = defaultEffects;
					currentVoiceElement = null;
				}
			} else if (v != currentVoiceElement
					|| (v.getAttribute("style") != null && v.getAttribute("style") != "" && !v.getAttribute("style").equals(
							currentStyle))
					|| (v.getAttribute("effect") != null && v.getAttribute("effect") != "" && !v.getAttribute("effect").equals(
							currentEffect))) {
				// We have just entered a new voice section
				if (!elements.isEmpty()) {
					AudioInputStream ais = synthesizeOneSection(elements, currentVoice, currentStyle, currentEffect,
							targetFormat, outputParams);
					if (ais != null) {
						result.appendAudio(ais);
					}
					elements.clear();
				}

				// Override with new voice, style, and/or effect
				Voice newVoice = Voice.getVoice(v);
				if (newVoice != null) {
					currentVoice = newVoice;
				}

				if (v.getAttribute("style") != null && v.getAttribute("style") != "")
					currentStyle = v.getAttribute("style");

				if (v.getAttribute("effect") != null && v.getAttribute("effect") != "")
					currentEffect = v.getAttribute("effect");

				currentVoiceElement = v;
			}
			// Chunk at sentence boundaries
			if (s != currentSentence) {
				if (!elements.isEmpty()) {
					AudioInputStream ais = synthesizeOneSection(elements, currentVoice, currentStyle, currentEffect,
							targetFormat, outputParams);
					if (ais != null) {
						result.appendAudio(ais);
					}
					elements.clear();
				}
				currentSentence = s;
			}
			elements.add(element);
		}

		if (!elements.isEmpty()) {
			AudioInputStream ais = synthesizeOneSection(elements, currentVoice, currentStyle, currentEffect, targetFormat,
					outputParams);

			if (ais != null) {
				result.appendAudio(ais);
			}
		}

		return result;
	}

	/**
	 * Synthesize one section, consisting of tokens and boundaries, with a given voice, to the given target audio format.
	 */
	private AudioInputStream synthesizeOneSection(List<Element> tokensAndBoundaries, Voice voice, String currentStyle,
			String currentEffect, AudioFormat targetFormat, String outputParams) throws SynthesisException,
			UnsupportedAudioFileException {
		// sanity check: are there any tokens containing phone descendants?
		if (!containsPhoneDescendants(tokensAndBoundaries)) {
			logger.warn("No PHONE segments found in this section; will not attempt to synthesize it!");
			return null;
		}

		EffectsApplier ef = new EffectsApplier();

		// HMM-only effects need to get their parameters prior to synthesis
		ef.setHMMEffectParameters(voice, currentEffect);
		//

		AudioInputStream ais = null;
		ais = voice.synthesize(tokensAndBoundaries, outputParams);
		if (ais == null)
			return null;
		// Conversion to targetFormat required?
		if (!ais.getFormat().matches(targetFormat)) {
			// Attempt conversion; if not supported, log a warning
			// and provide the non-converted stream.
			logger.info("Audio format conversion required for voice " + voice.getName());
			try {
				AudioInputStream intermedStream = AudioSystem.getAudioInputStream(targetFormat, ais);
				ais = intermedStream;
			} catch (IllegalArgumentException iae) { // conversion not supported
				boolean solved = false;
				// try again with intermediate sample rate conversion
				if (!targetFormat.getEncoding().equals(ais.getFormat())
						&& targetFormat.getSampleRate() != ais.getFormat().getSampleRate()) {
					AudioFormat sampleRateConvFormat = new AudioFormat(ais.getFormat().getEncoding(),
							targetFormat.getSampleRate(), ais.getFormat().getSampleSizeInBits(), ais.getFormat().getChannels(),
							ais.getFormat().getFrameSize(), ais.getFormat().getFrameRate(), ais.getFormat().isBigEndian());
					try {
						AudioInputStream intermedStream = AudioSystem.getAudioInputStream(sampleRateConvFormat, ais);
						ais = AudioSystem.getAudioInputStream(targetFormat, intermedStream);
						// No exception thrown, i.e. success
						solved = true;
					} catch (IllegalArgumentException iae1) {
					}
				}
				if (!solved)
					throw new UnsupportedAudioFileException("Conversion from audio format " + ais.getFormat()
							+ " to requested audio format " + targetFormat + " not supported.\n" + iae.getMessage());
			}
		}
		// Apply effect if present
		if (currentEffect != null && !currentEffect.equals("")) {
			ais = ef.apply(ais, currentEffect);
		}
		return ais;
	}

	/**
	 * Check if the List of Elements contains any TOKENS that have PHONE descendants
	 * 
	 * @param tokensAndBoundaries
	 *            the List of Elements to check for PHONE elements
	 * @return true once a PHONE has been found within a TOKEN, false if this never happens
	 */
	private boolean containsPhoneDescendants(List<Element> tokensAndBoundaries) {
		for (Element element : tokensAndBoundaries) {
			if (element.getTagName().equals(MaryXML.TOKEN) && element.getElementsByTagName(MaryXML.PHONE).getLength() > 0) {
				return true;
			}
		}
		return false;
	}

}
