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
public class MaryInterface {
	public static MaryInterface getLocalMaryInterface() throws MaryConfigurationException {
		return new MaryInterface();
	}
	
	
	private MaryDataType inputType;
	private MaryDataType outputType;
	private Locale locale;
	private AudioFileFormat audioFileFormat;
	private String outputTypeParams;

	
	private MaryInterface() throws MaryConfigurationException {
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
        Voice voice = Voice.getDefaultVoice(locale);
        if (voice != null) {
        	AudioFormat af = voice.dbAudioFormat();
            audioFileFormat = new AudioFileFormat(AudioFileFormat.Type.WAVE, af, AudioSystem.NOT_SPECIFIED);
        }

	}
	
	
	public void setInputType(MaryDataType newInputType) {
		this.inputType = newInputType;
	}
	
	public MaryDataType getInputType() {
		return inputType;
	}

	public void setOutputType(MaryDataType newOutputType) {
		this.outputType = newOutputType;
	}
	
	public MaryDataType getOutputType() {
		return outputType;
	}
	
	public void setLocale(Locale newLocale) {
		this.locale = newLocale;
	}
	
	public Locale getLocale() {
		return locale;
	}
	
	public void setAudioFileFormat(AudioFileFormat newAudioFileFormat) {
		this.audioFileFormat = newAudioFileFormat;
	}
	
	public AudioFileFormat getAudioFileFormat() {
		return audioFileFormat;
	}
	
	public void setOutputTypeParams(String params) {
		this.outputTypeParams = params;
	}
	
	public String getOutputTypeParams() {
		return outputTypeParams;
	}
	
	public Document generateXML(String text) throws SynthesisException {
		if (inputType.isXMLType()) {
			throw new IllegalArgumentException("Cannot provide plain-text input for XML-based input type "+inputType);
		}
		if (!outputType.isXMLType()) {
			throw new IllegalArgumentException("Cannot provide XML output for non-XML-based output type "+outputType);
		}
		MaryData in = new MaryData(inputType, locale);
		try {
			in.setData(text);
		} catch (Exception ioe) {
			throw new SynthesisException(ioe);
		}
		MaryData out = _process(in);
		return out.getDocument();
	}

	public AudioInputStream generateAudio(String text) throws SynthesisException {
		if (inputType.isXMLType()) {
			throw new IllegalArgumentException("Cannot provide plain-text input for XML-based input type "+inputType);
		}
		if (!outputType.equals(MaryDataType.AUDIO)) {
			throw new IllegalArgumentException("Cannot provide audio output for non-audio output type "+outputType);
		}
		MaryData in = new MaryData(inputType, locale);
		try {
			in.setData(text);
		} catch (Exception ioe) {
			throw new SynthesisException(ioe);
		}
		MaryData out = _process(in);
		return out.getAudio();
	}

	public String generateText(String text) throws SynthesisException {
		if (inputType.isXMLType()) {
			throw new IllegalArgumentException("Cannot provide plain-text input for XML-based input type "+inputType);
		}
		if (outputType.isXMLType() || !outputType.isTextType()) {
			throw new IllegalArgumentException("Cannot provide text output for non-text output type "+outputType);
		}
		MaryData in = new MaryData(inputType, locale);
		try {
			in.setData(text);
		} catch (Exception ioe) {
			throw new SynthesisException(ioe);
		}
		MaryData out = _process(in);
		return out.getPlainText();
	}

	
	
	protected MaryData _process(MaryData in) throws SynthesisException {		
		Request r = new Request(inputType, outputType, locale, null, null, null, 1, audioFileFormat, false, outputTypeParams);
		r.setInputData(in);
		try {
			r.process();
		} catch (Exception e) {
			throw new SynthesisException("cannot process", e);
		}
		return r.getOutputData();
	}
}
