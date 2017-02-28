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
public class LocalMaryInterface implements MaryInterface
{
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

	/**
	 * Synthesis will fail if {@link MaryDataType#AUDIO AUDIO} is requested but no voice is available for the requested Locale.
	 * Moreover, the {@linkplain #audioFileFormat} will be null because {@linkplain #setAudioFileFormatForVoice()} silently
	 * ignores if {@linkplain #voice} is null.
	 *
	 * @throws IllegalArgumentException
	 *             which should actually be a {@link MaryConfigurationException}.
	 */
	private void verifyVoiceIsAvailableForLocale() {
        if (getAvailableVoices(locale).isEmpty()) {
            throw new IllegalArgumentException("No voice is available for Locale: " + locale);
        }
	}

	private MaryData process(String configuration, String input_data)
        throws SynthesisException
    {
		Request r = new Request(configuration, input_data);
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
}
