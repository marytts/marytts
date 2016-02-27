/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.server.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.datatypes.MaryDataType;
import marytts.modules.synthesis.Voice;
import marytts.server.Request;
import marytts.server.RequestHandler.StreamingOutputPiper;
import marytts.server.RequestHandler.StreamingOutputWriter;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.data.audio.MaryAudioUtils;
import marytts.util.http.Address;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

/**
 * Provides functionality to process synthesis http requests
 * 
 * @author Oytun T&uuml;rk
 *
 */
public class SynthesisRequestHandler extends BaseHttpRequestHandler {
	private static int id = 0;

	private static synchronized int getId() {
		return id++;
	}

	private StreamingOutputWriter outputToStream;
	private StreamingOutputPiper streamToPipe;
	private PipedOutputStream pipedOutput;
	private PipedInputStream pipedInput;

	public SynthesisRequestHandler() {
		super();

		outputToStream = null;
		streamToPipe = null;
		pipedOutput = null;
		pipedInput = null;
	}

	@Override
	protected void handleClientRequest(String absPath, Map<String, String> queryItems, HttpResponse response,
			Address serverAddressAtClient) throws IOException {
		/*
		 * response.setStatusCode(HttpStatus.SC_OK); TestProducingNHttpEntity entity = new TestProducingNHttpEntity();
		 * entity.setContentType("audio/x-mp3"); response.setEntity(entity); if (true) return;
		 */
		logger.debug("New synthesis request: " + absPath);
		if (queryItems != null) {
			for (String key : queryItems.keySet()) {
				logger.debug("    " + key + "=" + queryItems.get(key));
			}
		}
		process(serverAddressAtClient, queryItems, response);

	}

	public void process(Address serverAddressAtClient, Map<String, String> queryItems, HttpResponse response) {
		if (queryItems == null
				|| !(queryItems.containsKey("INPUT_TYPE") && queryItems.containsKey("OUTPUT_TYPE")
						&& queryItems.containsKey("LOCALE") && queryItems.containsKey("INPUT_TEXT"))) {
			MaryHttpServerUtils.errorMissingQueryParameter(response,
					"'INPUT_TEXT' and 'INPUT_TYPE' and 'OUTPUT_TYPE' and 'LOCALE'");
			return;
		}

		String inputText = queryItems.get("INPUT_TEXT");

		MaryDataType inputType = MaryDataType.get(queryItems.get("INPUT_TYPE"));
		if (inputType == null) {
			MaryHttpServerUtils.errorWrongQueryParameterValue(response, "INPUT_TYPE", queryItems.get("INPUT_TYPE"), null);
			return;
		}

		MaryDataType outputType = MaryDataType.get(queryItems.get("OUTPUT_TYPE"));
		if (outputType == null) {
			MaryHttpServerUtils.errorWrongQueryParameterValue(response, "OUTPUT_TYPE", queryItems.get("OUTPUT_TYPE"), null);
			return;
		}
		boolean isOutputText = true;
		boolean streamingAudio = false;
		AudioFileFormat.Type audioFileFormatType = null;
		if (outputType.name().contains("AUDIO")) {
			isOutputText = false;
			String audioTypeName = queryItems.get("AUDIO");
			if (audioTypeName == null) {
				MaryHttpServerUtils.errorMissingQueryParameter(response, "'AUDIO' when OUTPUT_TYPE=AUDIO");
				return;
			}
			if (audioTypeName.endsWith("_STREAM")) {
				streamingAudio = true;
			}
			int lastUnderscore = audioTypeName.lastIndexOf('_');
			if (lastUnderscore != -1) {
				audioTypeName = audioTypeName.substring(0, lastUnderscore);
			}
			try {
				audioFileFormatType = MaryAudioUtils.getAudioFileFormatType(audioTypeName);
			} catch (Exception ex) {
			}
			if (audioFileFormatType == null) {
				MaryHttpServerUtils.errorWrongQueryParameterValue(response, "AUDIO", queryItems.get("AUDIO"), null);
				return;
			} else if (audioFileFormatType.toString().equals("MP3") && !MaryRuntimeUtils.canCreateMP3()) {
				MaryHttpServerUtils.errorWrongQueryParameterValue(response, "AUDIO", queryItems.get("AUDIO"),
						"Conversion to MP3 not supported.");
				return;
			} else if (audioFileFormatType.toString().equals("Vorbis") && !MaryRuntimeUtils.canCreateOgg()) {
				MaryHttpServerUtils.errorWrongQueryParameterValue(response, "AUDIO", queryItems.get("AUDIO"),
						"Conversion to OGG Vorbis format not supported.");
				return;
			}
		}
		// optionally, there may be output type parameters
		// (e.g., the list of features to produce for the output type TARGETFEATURES)
		String outputTypeParams = queryItems.get("OUTPUT_TYPE_PARAMS");

		Locale locale = MaryUtils.string2locale(queryItems.get("LOCALE"));
		if (locale == null) {
			MaryHttpServerUtils.errorWrongQueryParameterValue(response, "LOCALE", queryItems.get("LOCALE"), null);
			return;
		}

		Voice voice = null;
		String voiceName = queryItems.get("VOICE");
		if (voiceName != null) {
			if (voiceName.equals("male") || voiceName.equals("female")) {
				voice = Voice.getVoice(locale, new Voice.Gender(voiceName));
			} else {
				voice = Voice.getVoice(voiceName);
			}
			if (voice == null) {
				// a voice name was given but there is no such voice
				MaryHttpServerUtils.errorWrongQueryParameterValue(response, "VOICE", queryItems.get("VOICE"), null);
				return;
			}
		}
		if (voice == null) { // no voice tag -- use locale default if it exists.
			voice = Voice.getDefaultVoice(locale);
			logger.debug("No voice requested -- using default " + voice);
		}

		String style = queryItems.get("STYLE");
		if (style == null)
			style = "";

		String effects = toRequestedAudioEffectsString(queryItems);
		if (effects.length() > 0)
			logger.debug("Audio effects requested: " + effects);
		else
			logger.debug("No audio effects requested");

		String logMsg = queryItems.get("LOG");
		if (logMsg != null) {
			logger.info("Connection info: " + logMsg);
		}

		// Now, the parse is complete.

		// Construct audio file format -- even when output is not AUDIO,
		// in case we need to pass via audio to get our output type.
		if (audioFileFormatType == null) {
			audioFileFormatType = AudioFileFormat.Type.AU;
		}
		AudioFormat audioFormat;
		if (audioFileFormatType.toString().equals("MP3")) {
			audioFormat = MaryRuntimeUtils.getMP3AudioFormat();
		} else if (audioFileFormatType.toString().equals("Vorbis")) {
			audioFormat = MaryRuntimeUtils.getOggAudioFormat();
		} else if (voice != null) {
			audioFormat = voice.dbAudioFormat();
		} else {
			audioFormat = Voice.AF16000;
		}
		AudioFileFormat audioFileFormat = new AudioFileFormat(audioFileFormatType, audioFormat, AudioSystem.NOT_SPECIFIED);

		final Request maryRequest = new Request(inputType, outputType, locale, voice, effects, style, getId(), audioFileFormat,
				streamingAudio, outputTypeParams);

		// Process the request and send back the data
		boolean ok = true;
		try {
			maryRequest.setInputData(inputText);
			logger.info("Read: " + inputText);
		} catch (Exception e) {
			String message = "Problem reading input";
			logger.warn(message, e);
			MaryHttpServerUtils.errorInternalServerError(response, message, e);
			ok = false;
		}
		if (ok) {
			if (streamingAudio) {
				// Start two separate threads:
				// 1. one thread to process the request;
				new Thread("RH " + maryRequest.getId()) {
					public void run() {
						Logger myLogger = MaryUtils.getLogger(this.getName());
						try {
							maryRequest.process();
							myLogger.info("Streaming request processed successfully.");
						} catch (Throwable t) {
							myLogger.error("Processing failed.", t);
						}
					}
				}.start();

				// 2. one thread to take the audio data as it becomes available
				// and write it into the ProducingNHttpEntity.
				// The second one does not depend on the first one practically,
				// because the AppendableSequenceAudioInputStream returned by
				// maryRequest.getAudio() was already created in the constructor of Request.
				AudioInputStream audio = maryRequest.getAudio();
				assert audio != null : "Streaming audio but no audio stream -- very strange indeed! :-(";
				AudioFileFormat.Type audioType = maryRequest.getAudioFileFormat().getType();
				AudioStreamNHttpEntity entity = new AudioStreamNHttpEntity(maryRequest);
				new Thread(entity, "HTTPWriter " + maryRequest.getId()).start();
				// entity knows its contentType, no need to set explicitly here.
				response.setEntity(entity);
				response.setStatusCode(HttpStatus.SC_OK);
				return;
			} else { // not streaming audio
				// Process input data to output data
				try {
					maryRequest.process(); // this may take some time
				} catch (Throwable e) {
					String message = "Processing failed.";
					logger.error(message, e);
					MaryHttpServerUtils.errorInternalServerError(response, message, e);
					ok = false;
				}
				if (ok) {
					// Write output data to client
					try {
						ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
						maryRequest.writeOutputData(outputStream);
						String contentType;
						if (maryRequest.getOutputType().isXMLType() || maryRequest.getOutputType().isTextType()) // text output
							contentType = "text/plain; charset=UTF-8";
						else
							// audio output
							contentType = MaryHttpServerUtils.getMimeType(maryRequest.getAudioFileFormat().getType());
						MaryHttpServerUtils.toHttpResponse(outputStream.toByteArray(), response, contentType);
					} catch (Exception e) {
						String message = "Cannot write output";
						logger.warn(message, e);
						MaryHttpServerUtils.errorInternalServerError(response, message, e);
						ok = false;
					}
				}
			}
		}

		if (ok)
			logger.info("Request handled successfully.");
		else
			logger.info("Request couldn't be handled successfully.");
		if (MaryRuntimeUtils.lowMemoryCondition()) {
			logger.info("Low memory condition detected (only " + MaryUtils.availableMemory()
					+ " bytes left). Triggering garbage collection.");
			Runtime.getRuntime().gc();
			logger.info("After garbage collection: " + MaryUtils.availableMemory() + " bytes available.");
		}
	}

	protected String toRequestedAudioEffectsString(Map<String, String> keyValuePairs) {
		StringBuilder effects = new StringBuilder();
		StringTokenizer tt;
		Set<String> keys = keyValuePairs.keySet();
		String currentKey;
		String currentEffectName, currentEffectParams;
		for (Iterator<String> it = keys.iterator(); it.hasNext();) {
			currentKey = it.next();
			if (currentKey.startsWith("effect_")) {
				if (currentKey.endsWith("_selected")) {
					if (keyValuePairs.get(currentKey).compareTo("on") == 0) {
						if (effects.length() > 0)
							effects.append("+");

						tt = new StringTokenizer(currentKey, "_");
						if (tt.hasMoreTokens())
							tt.nextToken(); // Skip "effects_"
						if (tt.hasMoreTokens()) // The next token is the effect name
						{
							currentEffectName = tt.nextToken();

							currentEffectParams = keyValuePairs.get("effect_" + currentEffectName + "_parameters");
							if (currentEffectParams != null && currentEffectParams.length() > 0)
								effects.append(currentEffectName).append("(").append(currentEffectParams).append(")");
							else
								effects.append(currentEffectName);
						}
					}
				}
			}
		}

		return effects.toString();
	}

}
