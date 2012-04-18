/**
 * Copyright 2011 DFKI GmbH.
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
package marytts;

import java.util.Locale;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.w3c.dom.Document;

import marytts.config.MaryConfig;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.modules.synthesis.Voice;
import marytts.server.Request;
import marytts.util.MaryRuntimeUtils;

/**
 * This class and its subclasses are intended to grow into a simple-to-use, unified interface for both the local MARY server and a MARY client.
 * @author marc
 *
 */
public class LocalMaryInterface {
	
	private MaryDataType inputType;
	private MaryDataType outputType;
	private Locale locale;
	private Voice voice;
	private AudioFileFormat audioFileFormat;
	private String effects;
	private String style;
	private String outputTypeParams;
	private boolean isStreaming;

	
	public LocalMaryInterface() throws MaryConfigurationException {
		try {
			MaryRuntimeUtils.ensureMaryStarted();
		} catch (Exception e) {
			throw new MaryConfigurationException("Cannot start MARY server", e);
		}
		
		init();
	}
	
	protected void init() {
		setReasonableDefaults();
	}
	
	protected void setReasonableDefaults() {
		inputType = MaryDataType.TEXT;
		outputType = MaryDataType.AUDIO;
		locale = Locale.US;
        voice = Voice.getDefaultVoice(locale);
        setAudioFileFormatForVoice();
        effects = null;
        style = null;
        outputTypeParams = null;
        isStreaming = false;

	}

	private void setAudioFileFormatForVoice() {
    	if (voice != null) {
    		AudioFormat af = voice.dbAudioFormat();
    		audioFileFormat = new AudioFileFormat(AudioFileFormat.Type.WAVE, af, AudioSystem.NOT_SPECIFIED);
    	} else {
    		audioFileFormat = null;
    	}
	}
	
	/**
	 * Set the input type for processing to the new input type.
	 * @param newInputType a string representation of a MaryDataType.
	 * @throws SynthesisException if newInputType is not a valid and known input data type.
	 */
	public void setInputType(String newInputType) throws SynthesisException {
		inputType = MaryDataType.get(newInputType);
		if (inputType == null) {
			throw new SynthesisException("No such type: "+newInputType);
		} else if (!inputType.isInputType()) {
			throw new SynthesisException("Not an input type: "+newInputType);
		}
	}
	
	/**
	 * Get the current input type, either the default ("TEXT") or the value most recently set through {@link #setInputType(String)}.
	 * @return the currently set input type.
	 */
	public String getInputType() {
		return inputType.name();
	}

	/**
	 * Set the output type for processing to the new output type.
	 * @param newOutputType a string representation of a MaryDataType.
	 * @throws SynthesisException if newOutputType is not a valid and known output data type.
	 */
	public void setOutputType(String newOutputType) throws SynthesisException {
		outputType = MaryDataType.get(newOutputType);
		if (outputType == null) {
			throw new SynthesisException("No such type: "+newOutputType);
		} else if (!outputType.isOutputType()) {
			throw new SynthesisException("Not an output type: "+newOutputType);
		}
	}
	
	/**
	 * Get the current output type, either the default ("AUDIO") or the value most recently set through {@link #setInputType(String)}.
	 * @return the currently set input type.
	 */
	public String getOutputType() {
		return outputType.name();
	}

	/**
	 * Set the locale for processing. Set the voice to the default voice for this locale.
	 * @param newLocale a supported locale.
	 * @throws SynthesisException if newLocale is not one of the supported locales.
	 */
	public void setLocale(Locale newLocale) throws SynthesisException {
		if (MaryConfig.getLanguageConfig(newLocale) == null) {
			throw new SynthesisException("Unsupported locale: "+newLocale);
		}
		locale = newLocale;
		voice = Voice.getDefaultVoice(locale);
		setAudioFileFormatForVoice();
	}
	
	/**
	 * Get the current locale used for processing. Either the default (US English) or the value most recently set through {@link #setLocale(Locale)} or indirectly through {@link #setVoice(String)}.
	 * @return the locale
	 */
	public Locale getLocale() {
		return locale;
	}
	
	/**
	 * Set the voice to be used for processing. If the current locale differs from the voice's locale, the locale is updated accordingly.
	 * @param voiceName the name of a valid voice. 
	 * @throws SynthesisException
	 */
	public void setVoice(String voiceName) throws SynthesisException {
		voice = Voice.getVoice(voiceName);
		if (voice == null) {
			throw new SynthesisException("No such voice: "+voiceName);
		}
		locale = voice.getLocale();
	}
	
	/**
	 * The name of the current voice, if any.
	 * @return the voice name, or null if no voice is currently set.
	 */
	public String getVoice() {
		if (voice == null) {
			return null;
		}
		return voice.getName();
	}
	
	/**
	 * Set the audio effects. For advanced use only.
	 * @param audioEffects
	 */
	public void setAudioEffects(String audioEffects) {
		effects = audioEffects;
	}
	
	/**
	 * Get the currently set audio effects. For advanced use only.
	 * @return
	 */
	public String getAudioEffects() {
		return effects;
	}
	
	/**
	 * Set the speaking style. For advanced use only.
	 * @param params
	 */
	public void setStyle(String newStyle) {
		style = newStyle;
	}
	
	/**
	 * Get the currently speaking style. For advanced use only.
	 * @return
	 */
	public String getStyle() {
		return style;
	}
	
	/**
	 * Set the output type parameters. For advanced use only.
	 * @param params
	 */
	public void setOutputTypeParams(String params) {
		this.outputTypeParams = params;
	}
	
	/**
	 * Get the currently set output type parameters. For advanced use only.
	 * @return
	 */
	public String getOutputTypeParams() {
		return outputTypeParams;
	}
	
	/**
	 * Set whether to stream audio. For advanced use only.
	 * @param isStreaming
	 */
	public void setStreamingAudio(boolean newIsStreaming) {
		isStreaming = newIsStreaming;
	}
	
	/**
	 * Whether to stream audio. For advanced use only.
	 * @return
	 */
	public boolean isStreamingAudio() {
		return isStreaming;
	}

	public String generateText(String text) throws SynthesisException {
		verifyInputTypeIsText();
		verifyOutputTypeIsText();
		MaryData in = getMaryDataFromText(text);
		MaryData out = process(in);
		return out.getPlainText();
	}

	public String generateText(Document doc) throws SynthesisException {
		verifyInputTypeIsXML();
		verifyOutputTypeIsText();
		MaryData in = getMaryDataFromXML(doc);
		MaryData out = process(in);
		return out.getPlainText();
	}

	
	public Document generateXML(String text) throws SynthesisException {
		verifyInputTypeIsText();
		verifyOutputTypeIsXML();
		MaryData in = getMaryDataFromText(text);
		MaryData out = process(in);
		return out.getDocument();
	}



	public Document generateXML(Document doc) throws SynthesisException {
		verifyInputTypeIsXML();
		verifyOutputTypeIsXML();
		MaryData in = getMaryDataFromXML(doc);
		MaryData out = process(in);
		return out.getDocument();
	}

	public AudioInputStream generateAudio(String text) throws SynthesisException {
		verifyInputTypeIsText();
		verifyOutputTypeIsAudio();
		MaryData in = getMaryDataFromText(text);
		MaryData out = process(in);
		return out.getAudio();
	}

	public AudioInputStream generateAudio(Document doc) throws SynthesisException {
		verifyInputTypeIsXML();
		verifyOutputTypeIsAudio();
		MaryData in = getMaryDataFromXML(doc);
		MaryData out = process(in);
		return out.getAudio();
	}

	private void verifyOutputTypeIsXML() {
		if (!outputType.isXMLType()) {
			throw new IllegalArgumentException("Cannot provide XML output for non-XML-based output type "+outputType);
		}
	}

	private void verifyInputTypeIsXML() {
		if (!inputType.isXMLType()) {
			throw new IllegalArgumentException("Cannot privide XML input for non-XML-based input type "+inputType);
		}
	}
	
	private void verifyInputTypeIsText() {
		if (inputType.isXMLType()) {
			throw new IllegalArgumentException("Cannot provide plain-text input for XML-based input type "+inputType);
		}
	}

	private void verifyOutputTypeIsAudio() {
		if (!outputType.equals(MaryDataType.AUDIO)) {
			throw new IllegalArgumentException("Cannot provide audio output for non-audio output type "+outputType);
		}
	}

	private void verifyOutputTypeIsText() {
		if (outputType.isXMLType() || !outputType.isTextType()) {
			throw new IllegalArgumentException("Cannot provide text output for non-text output type "+outputType);
		}
	}

	private MaryData getMaryDataFromText(String text) throws SynthesisException {
		MaryData in = new MaryData(inputType, locale);
		try {
			in.setData(text);
		} catch (Exception ioe) {
			throw new SynthesisException(ioe);
		}
		return in;
	}
	
	private MaryData getMaryDataFromXML(Document doc) throws SynthesisException {
		MaryData in = new MaryData(inputType, locale);
		try {
			in.setDocument(doc);
		} catch (Exception ioe) {
			throw new SynthesisException(ioe);
		}
		return in;
	}

	private MaryData process(MaryData in) throws SynthesisException {		
		Request r = new Request(inputType, outputType, locale, voice, effects, style, 1, audioFileFormat, isStreaming, outputTypeParams);
		r.setInputData(in);
		try {
			r.process();
		} catch (Exception e) {
			throw new SynthesisException("cannot process", e);
		}
		return r.getOutputData();
	}
}
