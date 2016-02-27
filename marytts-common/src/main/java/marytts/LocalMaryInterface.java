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

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.w3c.dom.Document;

import marytts.config.LanguageConfig;
import marytts.config.MaryConfig;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.modules.synthesis.Voice;
import marytts.server.Request;
import marytts.util.MaryRuntimeUtils;

/**
 * This class and its subclasses are intended to grow into a simple-to-use, unified interface for both the local MARY server and a
 * MARY client.
 * 
 * @author marc
 *
 */
public class LocalMaryInterface implements MaryInterface {

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#setInputType(java.lang.String)
	 */
	@Override
	public void setInputType(String newInputType) throws IllegalArgumentException {
		inputType = MaryDataType.get(newInputType);
		if (inputType == null) {
			throw new IllegalArgumentException("No such type: " + newInputType);
		} else if (!inputType.isInputType()) {
			throw new IllegalArgumentException("Not an input type: " + newInputType);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#getInputType()
	 */
	@Override
	public String getInputType() {
		return inputType.name();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#setOutputType(java.lang.String)
	 */
	@Override
	public void setOutputType(String newOutputType) throws IllegalArgumentException {
		outputType = MaryDataType.get(newOutputType);
		if (outputType == null) {
			throw new IllegalArgumentException("No such type: " + newOutputType);
		} else if (!outputType.isOutputType()) {
			throw new IllegalArgumentException("Not an output type: " + newOutputType);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#getOutputType()
	 */
	@Override
	public String getOutputType() {
		return outputType.name();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#setLocale(java.util.Locale)
	 */
	@Override
	public void setLocale(Locale newLocale) throws IllegalArgumentException {
		if (MaryConfig.getLanguageConfig(newLocale) == null) {
			throw new IllegalArgumentException("Unsupported locale: " + newLocale);
		}
		locale = newLocale;
		voice = Voice.getDefaultVoice(locale);
		setAudioFileFormatForVoice();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#getLocale()
	 */
	@Override
	public Locale getLocale() {
		return locale;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#setVoice(java.lang.String)
	 */
	@Override
	public void setVoice(String voiceName) throws IllegalArgumentException {
		voice = Voice.getVoice(voiceName);
		if (voice == null) {
			throw new IllegalArgumentException("No such voice: " + voiceName);
		}
		locale = voice.getLocale();
		setAudioFileFormatForVoice();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#getVoice()
	 */
	@Override
	public String getVoice() {
		if (voice == null) {
			return null;
		}
		return voice.getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#setAudioEffects(java.lang.String)
	 */
	@Override
	public void setAudioEffects(String audioEffects) {
		effects = audioEffects;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#getAudioEffects()
	 */
	@Override
	public String getAudioEffects() {
		return effects;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#setStyle(java.lang.String)
	 */
	@Override
	public void setStyle(String newStyle) {
		style = newStyle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#getStyle()
	 */
	@Override
	public String getStyle() {
		return style;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#setOutputTypeParams(java.lang.String)
	 */
	@Override
	public void setOutputTypeParams(String params) {
		this.outputTypeParams = params;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#getOutputTypeParams()
	 */
	@Override
	public String getOutputTypeParams() {
		return outputTypeParams;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#setStreamingAudio(boolean)
	 */
	@Override
	public void setStreamingAudio(boolean newIsStreaming) {
		isStreaming = newIsStreaming;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#isStreamingAudio()
	 */
	@Override
	public boolean isStreamingAudio() {
		return isStreaming;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#generateText(java.lang.String)
	 */
	@Override
	public String generateText(String text) throws SynthesisException {
		verifyInputTypeIsText();
		verifyOutputTypeIsText();
		MaryData in = getMaryDataFromText(text);
		MaryData out = process(in);
		return out.getPlainText();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#generateText(org.w3c.dom.Document)
	 */
	@Override
	public String generateText(Document doc) throws SynthesisException {
		verifyInputTypeIsXML();
		verifyOutputTypeIsText();
		MaryData in = getMaryDataFromXML(doc);
		MaryData out = process(in);
		return out.getPlainText();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#generateXML(java.lang.String)
	 */
	@Override
	public Document generateXML(String text) throws SynthesisException {
		verifyInputTypeIsText();
		verifyOutputTypeIsXML();
		MaryData in = getMaryDataFromText(text);
		MaryData out = process(in);
		return out.getDocument();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#generateXML(org.w3c.dom.Document)
	 */
	@Override
	public Document generateXML(Document doc) throws SynthesisException {
		verifyInputTypeIsXML();
		verifyOutputTypeIsXML();
		MaryData in = getMaryDataFromXML(doc);
		MaryData out = process(in);
		return out.getDocument();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#generateAudio(java.lang.String)
	 */
	@Override
	public AudioInputStream generateAudio(String text) throws SynthesisException {
		verifyInputTypeIsText();
		verifyOutputTypeIsAudio();
		verifyVoiceIsAvailableForLocale();
		MaryData in = getMaryDataFromText(text);
		MaryData out = process(in);
		return out.getAudio();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.MaryInterface#generateAudio(org.w3c.dom.Document)
	 */
	@Override
	public AudioInputStream generateAudio(Document doc) throws SynthesisException {
		verifyInputTypeIsXML();
		verifyOutputTypeIsAudio();
		verifyVoiceIsAvailableForLocale();
		MaryData in = getMaryDataFromXML(doc);
		MaryData out = process(in);
		return out.getAudio();
	}

	private void verifyOutputTypeIsXML() {
		if (!outputType.isXMLType()) {
			throw new IllegalArgumentException("Cannot provide XML output for non-XML-based output type " + outputType);
		}
	}

	private void verifyInputTypeIsXML() {
		if (!inputType.isXMLType()) {
			throw new IllegalArgumentException("Cannot provide XML input for non-XML-based input type " + inputType);
		}
	}

	private void verifyInputTypeIsText() {
		if (inputType.isXMLType()) {
			throw new IllegalArgumentException("Cannot provide plain-text input for XML-based input type " + inputType);
		}
	}

	private void verifyOutputTypeIsAudio() {
		if (!outputType.equals(MaryDataType.AUDIO)) {
			throw new IllegalArgumentException("Cannot provide audio output for non-audio output type " + outputType);
		}
	}

	private void verifyOutputTypeIsText() {
		if (outputType.isXMLType() || !outputType.isTextType()) {
			throw new IllegalArgumentException("Cannot provide text output for non-text output type " + outputType);
		}
	}

	/**
	 * Synthesis will fail if {@link MaryDataType#AUDIO AUDIO} is requested but no voice is available for the requested Locale.
	 * Moreover, the {@linkplain #audioFileFormat} will be null because {@linkplain #setAudioFileFormatForVoice()} silently
	 * ignores if {@linkplain #voice} is null.
	 * 
	 * @throws IllegalArgumentException
	 *             which should actually be a {@link MaryConfigurationException}.
	 */
	private void verifyVoiceIsAvailableForLocale() {
		if (outputType.equals(MaryDataType.AUDIO)) {
			if (getAvailableVoices(locale).isEmpty()) {
				throw new IllegalArgumentException("No voice is available for Locale: " + locale);
			}
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
		Request r = new Request(inputType, outputType, locale, voice, effects, style, 1, audioFileFormat, isStreaming,
				outputTypeParams);
		r.setInputData(in);
		try {
			r.process();
		} catch (Exception e) {
			throw new SynthesisException("cannot process", e);
		}
		return r.getOutputData();
	}

	@Override
	public Set<String> getAvailableVoices() {
		Set<String> voices = new HashSet<String>();
		for (Voice v : Voice.getAvailableVoices()) {
			voices.add(v.getName());
		}
		return voices;
	}

	@Override
	public Set<String> getAvailableVoices(Locale aLocale) {
		Set<String> voices = new HashSet<String>();
		for (Voice v : Voice.getAvailableVoices(aLocale)) {
			voices.add(v.getName());
		}
		return voices;
	}

	@Override
	public Set<Locale> getAvailableLocales() {
		Set<Locale> locales = new HashSet<Locale>();
		for (LanguageConfig lc : MaryConfig.getLanguageConfigs()) {
			locales.addAll(lc.getLocales());
		}
		return locales;
	}

	@Override
	public Set<String> getAvailableInputTypes() {
		return new HashSet<String>(MaryDataType.getInputTypeStrings());
	}

	@Override
	public Set<String> getAvailableOutputTypes() {
		return new HashSet<String>(MaryDataType.getOutputTypeStrings());
	}

	@Override
	public boolean isTextType(String dataType) {
		MaryDataType d = MaryDataType.get(dataType);
		if (d != null) {
			return d.isTextType() && !d.isXMLType();
		}
		return false;
	}

	@Override
	public boolean isXMLType(String dataType) {
		MaryDataType d = MaryDataType.get(dataType);
		if (d != null) {
			return d.isXMLType();
		}
		return false;
	}

	@Override
	public boolean isAudioType(String dataType) {
		return "AUDIO".equals(dataType);
	}
}
