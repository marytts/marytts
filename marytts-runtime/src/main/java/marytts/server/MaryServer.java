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
package marytts.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.Version;
import marytts.config.LanguageConfig;
import marytts.config.MaryConfig;
import marytts.datatypes.MaryDataType;
import marytts.htsengine.HMMVoice;
import marytts.modules.synthesis.Voice;
import marytts.signalproc.effects.AudioEffect;
import marytts.signalproc.effects.AudioEffects;
import marytts.signalproc.effects.BaseAudioEffect;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.interpolation.InterpolatingVoice;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.data.audio.MaryAudioUtils;

import org.apache.log4j.Logger;

/**
 * Listen for clients on socket port <code>MaryProperties.socketPort()</code>. For each new client, create a new RequestHandler
 * thread.
 * <p>
 * Clients are expected to follow the following <b>protocol</b>:
 * <p>
 * A client opens two socket connections to the server. The first, <code>infoSocket</code>, serves for passing meta-information,
 * such as the requested input and output types or warnings. The second, <code>dataSocket</code>, serves for passing the actual
 * input and output data. The server expects the communication as follows.
 * <ol>
 * <li>The client opens an <code>infoSocket</code>, optionally sends one line "MARY VERSION" to obtain three lines of version
 * information, and then sends one line "MARY IN=INPUTTYPE OUT=OUTPUTTYPE [AUDIO=AUDIOTYPE]", where INPUTTYPE and OUTPUTTYPE can
 * have a number of different values, depending on the configuration with which the server was started. For an English system,
 * these values include:
 * <ul>
 * <li>TEXT plain ASCII text, English (input only)</li>
 * <li>SABLE text annotated with SABLE markup (input only)</li>
 * <li>SSML text annotated with SSML markup (input only)</li>
 * <li>APML text annotated with APML markup (input only)</li>
 * <li>RAWMARYXML untokenised MaryXML</li>
 * <li>TOKENS tokenized text</li>
 * <li>WORDS numbers and abbreviations expanded</li>
 * <li>POS parts of speech tags added</li>
 * <li>PHONEMES phone symbols</li>
 * <li>INTONATION ToBI intonation symbols</li>
 * <li>ALLOPHONES post-lexical phonological rules</li>
 * <li>ACOUSTPARAMS acoustic parameters in MaryXML structure</li>
 * <li>AUDIO audio data (output only)</li>
 * </ul>
 * INPUTTYPE must be earlier in this list than OUTPUTTYPE. The list of input and output data types can be requested from the
 * server by sending it a line "MARY LIST DATATYPES". The server will reply with a list of lines where each line represents one
 * data type, e.g. "RAWMARYXML INPUT OUTPUT", "TEXT INPUT" or "AUDIO OUTPUT". See the code in MaryClient.fillDataTypes().
 * <p>
 * The optional AUDIO=AUDIOTYPE specifies the type of audio file to be sent for audio output. Possible values are:
 * <ul>
 * <li>WAVE</li>
 * <li>AU</li>
 * <li>SND</li>
 * <li>AIFF</li>
 * <li>AIFC</li>
 * <li>MP3</li>
 * <li>Vorbis</li>
 * <li>STREAMING_AU</li>
 * <li>STREAMING_MP3</li>
 * </ul>
 * <p>
 * The optional VOICE=VOICENAME specifies the default voice with which the text is to be spoken. As for the data types, possible
 * values depend on the configuration of the server. The list can be retrieved by sending the server a line "MARY LIST VOICES",
 * which will reply with lines such as "de7 de female", "kevin16 en male" or "us2 en male".
 * <p>
 * The optional EFFECTS=EFFECTSWITHPARAMETERS specifies the audio effects to be applied as a post-processing step along with their
 * parameters. EFFECTSWITHPARAMETERS is a String of the form
 * "Effect1Name(Effect1Parameter1=Effect1Value1; Effect1Parameter2=Effect1Value2), Effect2Name(Effect2Parameter1=Effect2Value1)"
 * For example, "Robot(amount=100),Whisper(amount=50)" will convert the output into a whispered robotic voice with the specified
 * amounts.
 * <p>
 * Example: The line
 * 
 * <pre>
 *   MARY IN=TEXT OUT=AUDIO AUDIO=WAVE VOICE=kevin16 EFFECTS
 * </pre>
 * 
 * will process normal ASCII text, and send back a WAV audio file synthesised with the voice "kevin16".</li>
 *
 * <li>The server reads and parses this input line. If its format is correct, a line containing a single integer is sent back to
 * the client on <code>infoSocket</code>. This integer is a unique identification number for this request.</li>
 *
 * <li>The client opens a second socket connection to the server, on the same port, the <code>dataSocket</code>. As a first line
 * on this <code>dataSocket</code>, it sends the single integer it had just received via the <code>infoSocket</code>.</li>
 *
 * <li>The server groups dataSocket and infoSocket together based on this identification number, and starts reading data of the
 * requested input type from <code>dataSocket</code>.</li>
 *
 * <li>If any errors or warning messages are issued during input parsing or consecutive processing, these are printed to
 * <code>infoSocket</code>.</li>
 *
 * <li>The processing result is output to <code>dataSocket</code>.</li>
 * </ol>
 *
 * @see RequestHandler
 * @author Marc Schr&ouml;der
 */
public class MaryServer implements Runnable {

	private ServerSocket server;
	private Logger logger;
	private int runningNumber = 1;
	private Map<Integer, Object[]> clientMap = Collections.synchronizedMap(new HashMap<Integer, Object[]>());
	private Executor clients = Executors.newCachedThreadPool();

	public MaryServer() {
		logger = MaryUtils.getLogger("server");
	}

	public void run() {
		logger.info("Starting server.");
		try {
			server = new ServerSocket(MaryProperties.needInteger("socket.port"));

			while (true) {
				logger.info("Waiting for client to connect on port " + server.getLocalPort());
				Socket client = server.accept();
				logger.info("Connection from " + client.getInetAddress().getHostName() + " ("
						+ client.getInetAddress().getHostAddress() + ").");
				clients.execute(new ClientHandler(client));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private synchronized int getID() {
		return runningNumber++;
	}

	public class ClientHandler implements Runnable {

		Socket client;
		PrintWriter clientOut;

		public ClientHandler(Socket client) throws IOException {
			this.client = client;
		}

		public void run() {
			logger = MaryUtils.getLogger("server");
			try {
				OutputStreamWriter clientUTFOutput = new OutputStreamWriter(client.getOutputStream(), "UTF-8");
				clientOut = new PrintWriter(clientUTFOutput, true);
				handle();
			} catch (UnsupportedEncodingException ex) {
				throw new AssertionError("UTF-8 is always a supported encoding.");
			} catch (Exception e) {
				logger.info("Error parsing request:", e);
				if (clientOut == null) {
					logger.info("Cannot write to client.");
				} else {
					clientOut.println("Error parsing request:");
					clientOut.println(e.getMessage());
				}
			} finally {
				// info Sockets must not be closed before the corresponding data socket is here and the request parsed:
				// FileUtils.close(client, clientOut);
			}
		}

		// Implement the protocol for communicating with a socket client.
		private void handle() throws Exception {
			// !!!! reject all clients that are not from authorized domains?

			// Read one line from client
			BufferedReader buffReader = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
			String line = buffReader.readLine();
			logger.debug("read request: `" + line + "'");

			if (line == null) {
				logger.info("Client seems to have disconnected - cannot read.");
				return;
			}

			// A: General information request, no synthesis.
			// This may consist of one or several lines of info requests and
			// may either stand alone or precede another request.
			while (handleInfoRequest(line)) {
				// In case this precedes another request, try to read another line:
				line = buffReader.readLine();
				if (line == null) {
					return;
				}
			}

			// VARIANT B1: Synthesis request.
			if (handleSynthesisRequest(line)) {
				return;
				// VARIANT B2: Second connection of synthesis request.
			} else if (handleNumberRequest(line, buffReader)) {
				return;
			} else {
				// complain
				String nl = System.getProperty("line.separator");
				throw new Exception("Expected either a line" + nl + "MARY IN=<INPUTTYPE> OUT=<OUTPUTTYPE> [AUDIO=<AUDIOTYPE>]"
						+ nl + "or a line containing only a number identifying a request.");
			}

		}

		private boolean handleInfoRequest(String inputLine) {
			if (inputLine.startsWith("MARY VERSION")) {
				logger.debug("InfoRequest " + inputLine);
				return handleVersion();
			} else if (inputLine.startsWith("MARY LIST DATATYPES")) {
				logger.debug("InfoRequest " + inputLine);
				return listDataTypes();
			} else if (inputLine.startsWith("MARY LIST LOCALES")) {
				logger.debug("InfoRequest " + inputLine);
				return listLocales();
			} else if (inputLine.startsWith("MARY LIST VOICES")) {
				logger.debug("InfoRequest " + inputLine);
				return listVoices();
			} else if (inputLine.startsWith("MARY LIST AUDIOFILEFORMATTYPES")) {
				logger.debug("InfoRequest " + inputLine);
				return listAudioFileFormatTypes();
			} else if (inputLine.startsWith("MARY EXAMPLETEXT")) {
				logger.debug("InfoRequest " + inputLine);
				return exampleText(inputLine);
			} else if (inputLine.startsWith("MARY VOICE EXAMPLETEXT")) {
				logger.debug("InfoRequest " + inputLine);
				return voiceExampleText(inputLine);
			} else if (inputLine.startsWith("MARY VOICE GETDEFAULTAUDIOEFFECTS")) {
				logger.debug("InfoRequest " + inputLine);
				// the request is about the available audio effects
				return voiceGetDefaultAudioEffects(inputLine);
			} else if (inputLine.startsWith("MARY VOICE GETAUDIOEFFECTHELPTEXTLINEBREAK")) {
				logger.debug("InfoRequest " + inputLine);
				return voiceGetAudioEffectHelpTextLineBreak();
			} else if (inputLine.startsWith("MARY VOICE GETAUDIOEFFECTDEFAULTPARAM ")) {
				return getAudioEffectDefaultParameters(inputLine);
			} else if (inputLine.startsWith("MARY VOICE GETFULLAUDIOEFFECT ")) {
				return voiceGetFullAudioEffect(inputLine);
			} else if (inputLine.startsWith("MARY VOICE GETAUDIOEFFECTHELPTEXT ")) {
				return getAudioEffectHelpText(inputLine);
			} else if (inputLine.startsWith("MARY VOICE ISHMMAUDIOEFFECT ")) {
				return isHMMAudioEffect(inputLine);
			} else {
				return false;
			}
		}

		private boolean handleSynthesisRequest(String inputLine) throws Exception {
			int id = 0;

			if (!inputLine.startsWith("MARY")) {
				return false;
			}

			StringTokenizer t = new StringTokenizer(inputLine);

			if (t.hasMoreTokens()) {
				t.nextToken(); // discard MARY head
			}

			MaryDataType inputType = parseSynthesisRequiredInputType(t);
			MaryDataType outputType = parseSynthesisRequiredOutputType(t);
			Locale locale = parseSynthesisRequiredLocale(t);

			// Optional from here on
			AudioFileFormat.Type audioFileFormatType = null;
			boolean streamingAudio = false;
			Voice voice = null;
			String style = null;
			String effects = null;

			while (t.hasMoreTokens()) {
				String token = t.nextToken();
				if (token.startsWith("AUDIO")) {
					// AUDIO (optional and ignored if output type != AUDIO)
					String audio = parseProtocolParameter(token, "AUDIO", "AUDIOTYPE");
					streamingAudio = audio.startsWith("STREAMING_");
					if (outputType == MaryDataType.get("AUDIO")) {
						if (streamingAudio) {
							audioFileFormatType = MaryAudioUtils.getAudioFileFormatType(audio.substring(10));
						} else {
							audioFileFormatType = MaryAudioUtils.getAudioFileFormatType(audio);
						}
					}
				} else if (token.startsWith("VOICE")) {
					// Optional VOICE field
					voice = parseSynthesisVoiceType(token, locale);
				} else if (token.startsWith("STYLE")) {
					// Optional STYLE field
					style = parseProtocolParameter(token, "STYLE", "STYLE_NAME");
				} else if (token.startsWith("EFFECTS")) {
					// Optional EFFECTS field
					effects = parseProtocolParameter(token, "EFFECTS", "EFFECTS_LIST");
				} else if (token.startsWith("LOG")) {
					// Optional LOG field
					// If present, the rest of the line counts as the value of LOG=
					parseSynthesisLog(token, t);
				}
			}

			// Construct audio file format -- even when output is not AUDIO,
			// in case we need to pass via audio to get our output type.
			if (audioFileFormatType == null) {
				audioFileFormatType = AudioFileFormat.Type.WAVE;
			}
			if (voice == null) {
				// no voice tag -- use locale default
				voice = Voice.getDefaultVoice(locale);
				logger.debug("No voice requested -- using default " + voice);
			}
			if (style == null) {
				logger.debug("No style requested");
			} else {
				logger.debug("Style requested: " + style);
			}
			if (effects == null) {
				logger.debug("No audio effects requested");
			} else {
				logger.debug("Audio effects requested: " + effects);
			}

			// Now, the parse is complete.
			// this request's id:
			id = getID();

			AudioFormat audioFormat = voice.dbAudioFormat();
			if (audioFileFormatType.toString().equals("MP3")) {
				if (!MaryRuntimeUtils.canCreateMP3()) {
					throw new UnsupportedAudioFileException("Conversion to MP3 not supported.");
				}
				audioFormat = MaryRuntimeUtils.getMP3AudioFormat();
			} else if (audioFileFormatType.toString().equals("Vorbis")) {
				if (!MaryRuntimeUtils.canCreateOgg()) {
					throw new UnsupportedAudioFileException("Conversion to OGG Vorbis format not supported.");
				}
				audioFormat = MaryRuntimeUtils.getOggAudioFormat();
			}

			AudioFileFormat audioFileFormat = new AudioFileFormat(audioFileFormatType, audioFormat, AudioSystem.NOT_SPECIFIED);
			Request request = new Request(inputType, outputType, locale, voice, effects, style, id, audioFileFormat,
					streamingAudio, null);
			clientOut.println(id);
			// -- create new clientMap entry
			Object[] value = new Object[2];
			value[0] = client;
			value[1] = request;
			clientMap.put(id, value);
			return true;
		}

		/**
		 * Verifies and parses the protocol parameter
		 * 
		 * @param token
		 *            the string to read the parameter from
		 * @param expectedParameterName
		 *            the expected parameter name
		 * @param parameterDescription
		 *            human readable description of the parameter
		 * @return The value for the given parameter.
		 * @throws Exception
		 *             if the parameter is not of the type expected or the protocol is malformed.
		 * @throws NullPointerException
		 *             - if token is null
		 */
		private String parseProtocolParameter(String token, String expectedParameterType, String parameterDescription)
				throws Exception {
			StringTokenizer tt = new StringTokenizer(token, "=");
			if (tt.countTokens() != 2 || !tt.nextToken().equals(expectedParameterType)) {
				throw new Exception("Expected " + expectedParameterType + "=<" + parameterDescription + ">");
			}
			return tt.nextToken();
		}

		private void parseSynthesisLog(String token, StringTokenizer t) throws Exception {
			String log = parseProtocolParameter(token, "LOG", "LOG_INPUT");
			// Rest of line:
			while (t.hasMoreTokens()) {
				log = log + " " + t.nextToken();
			}
			logger.info("Connection info: " + log);
		}

		private Voice parseSynthesisVoiceType(String t, Locale locale) throws Exception {
			String voiceName = parseProtocolParameter(t, "VOICE", "VOICE_NAME_OR_GENDER");
			if ((voiceName.equals("male") || voiceName.equals("female")) && locale != null) {
				// Locale-specific interpretation of gender
				return Voice.getVoice(locale, new Voice.Gender(voiceName));
			} else {
				// Plain old voice name
				return Voice.getVoice(voiceName);
			}
		}

		private MaryDataType parseSynthesisRequiredInputType(StringTokenizer t) throws Exception {
			if (!t.hasMoreTokens()) {
				throw new Exception("Expected IN=<INPUTTYPE>");
			}
			String input = parseProtocolParameter(t.nextToken(), "IN", "INPUTTYPE");
			MaryDataType inputType = MaryDataType.get(input);
			if (inputType == null) {
				throw new Exception("Invalid input type: " + input);
			}
			return inputType;
		}

		private MaryDataType parseSynthesisRequiredOutputType(StringTokenizer t) throws Exception {
			if (!t.hasMoreTokens()) {
				throw new Exception("Expected OUT=<OUTPUTTYPE>");
			}
			String output = parseProtocolParameter(t.nextToken(), "OUT", "OUTPUTTYPE");
			MaryDataType outputType = MaryDataType.get(output);
			if (outputType == null) {
				throw new Exception("Invalid output type: " + output);
			}
			return outputType;
		}

		private Locale parseSynthesisRequiredLocale(StringTokenizer t) throws Exception {
			if (!t.hasMoreTokens()) {
				throw new Exception("Expected LOCALE=<locale>");
			}
			String localeString = parseProtocolParameter(t.nextToken(), "LOCALE", "locale");
			return MaryUtils.string2locale(localeString);
		}

		private boolean handleNumberRequest(String inputLine, Reader reader) throws Exception {
			// * if number
			int id = 0;
			try {
				id = Integer.parseInt(inputLine);
			} catch (NumberFormatException e) {
				return false;
			}
			// -- find corresponding infoSocket and request in clientMap
			Socket infoSocket = null;
			Request request = null;
			// Wait up to TIMEOUT milliseconds for the first ClientHandler
			// to write its clientMap entry:
			long TIMEOUT = 1000;
			long startTime = System.currentTimeMillis();
			Object[] value = null;
			do {
				Thread.yield();
				value = (Object[]) clientMap.get(id);
			} while (value == null && System.currentTimeMillis() - startTime < TIMEOUT);
			if (value != null) {
				infoSocket = (Socket) value[0];
				request = (Request) value[1];
			}
			// Verify that the request is non-null and that the
			// corresponding socket comes from the same IP address:
			if (request == null || infoSocket == null || !infoSocket.getInetAddress().equals(client.getInetAddress())) {
				throw new Exception("Invalid identification number.");
				// Don't be more specific, because in general it is none of
				// their business whether in principle someone else has
				// this id.
			}

			// -- delete clientMap entry
			try {
				clientMap.remove(id);
			} catch (UnsupportedOperationException e) {
				logger.info("Cannot remove clientMap entry", e);
			}
			// -- send off to new request
			RequestHandler rh = new RequestHandler(request, infoSocket, client, reader);
			rh.start();
			return true;
		}

		private boolean handleVersion() {
			// Write version information to client.
			clientOut.println("Mary TTS server " + Version.specificationVersion() + " (impl. " + Version.implementationVersion()
					+ ")");
			// Empty line marks end of info:
			clientOut.println();
			return true;
		}

		private boolean isHMMAudioEffect(String inputLine) {
			String prefix = "MARY VOICE ISHMMAUDIOEFFECT ";
			assert inputLine.startsWith(prefix);
			String effectName = inputLine.substring(prefix.length());
			AudioEffect effect = AudioEffects.getEffect(effectName);
			if (effect == null) {
				return false;
			}
			logger.debug("InfoRequest " + inputLine);
			clientOut.println(effect.isHMMEffect() ? "yes" : "no");
			clientOut.println();
			return true;
		}

		private boolean listAudioFileFormatTypes() {
			String info = MaryRuntimeUtils.getAudioFileFormatTypes();
			clientOut.println(info);
			// Empty line marks end of info:
			clientOut.println();
			return true;
		}

		private boolean listDataTypes() {
			// List all known datatypes
			for (MaryDataType t : MaryDataType.getDataTypes()) {
				clientOut.print(t.name());
				if (t.isInputType()) {
					clientOut.print(" INPUT");
				}
				if (t.isOutputType()) {
					clientOut.print(" OUTPUT");
				}
				clientOut.println();
			}
			// Empty line marks end of info:
			clientOut.println();
			return true;
		}

		private boolean listLocales() {
			StringBuilder out = new StringBuilder();
			for (LanguageConfig conf : MaryConfig.getLanguageConfigs()) {
				for (Locale locale : conf.getLocales()) {
					out.append(locale).append('\n');
				}
			}
			clientOut.print(out.toString());
			// Empty line marks end of info:
			clientOut.println();
			return true;
		}

		private boolean listVoices() {
			// list all known voices
			for (Voice v : Voice.getAvailableVoices()) {
				if (v instanceof InterpolatingVoice) {
					// do not list interpolating voice
				} else if (v instanceof UnitSelectionVoice) {
					clientOut.println(v.getName() + " " + v.getLocale() + " " + v.gender().toString() + " " + "unitselection"
							+ " " + ((UnitSelectionVoice) v).getDomain());
				} else if (v instanceof HMMVoice) {
					clientOut.println(v.getName() + " " + v.getLocale() + " " + v.gender().toString() + " " + "hmm");
				} else {
					clientOut.println(v.getName() + " " + v.getLocale() + " " + v.gender().toString() + " " + "other");
				}
			}
			// Empty line marks end of info:
			clientOut.println();
			return true;
		}

		private boolean exampleText(String inputLine) {
			// send an example text for a given data type
			StringTokenizer st = new StringTokenizer(inputLine);
			st.nextToken();
			st.nextToken();
			try {
				String typeName = st.nextToken();
				// next should be locale:
				Locale locale = MaryUtils.string2locale(st.nextToken());
				MaryDataType type = MaryDataType.get(typeName);
				String exampleText = type.exampleText(locale);
				if (exampleText != null) {
					clientOut.println(exampleText.trim());
				}
			} catch (NullPointerException err) {/* type doesn't exist */

			} catch (NoSuchElementException nse) {/* type doesn't exist */

			}
			// upon failure, simply return nothing
			clientOut.println();
			return true;
		}

		private boolean voiceExampleText(String inputLine) {
			// the request is about the example text of
			// a limited domain unit selection voice
			// send an example text for a given data type
			StringTokenizer st = new StringTokenizer(inputLine);
			st.nextToken();
			st.nextToken();
			st.nextToken();
			try {
				String voiceName = st.nextToken();
				Voice v = Voice.getVoice(voiceName);
				String text = ((marytts.unitselection.UnitSelectionVoice) v).getExampleText();
				if (text != null) {
					clientOut.println(text);
				}
			} catch (NullPointerException err) {/* type doesn't exist */

			} catch (NoSuchElementException nse) {/* type doesn't exist */

			}
			// upon failure, simply return nothing
			clientOut.println();
			return true;
		}

		private boolean voiceGetAudioEffectHelpTextLineBreak() {
			clientOut.println(BaseAudioEffect.strLineBreak);
			// upon failure, simply return nothing
			clientOut.println();
			return true;
		}

		private boolean voiceGetDefaultAudioEffects(String inputLine) {
			// Marc, 8.1.09: Simplified format
			// name params
			StringBuilder sb = new StringBuilder();
			for (AudioEffect effect : AudioEffects.getEffects()) {
				sb.append(effect.getName()).append(" ").append(effect.getExampleParameters()).append("\n");
			}
			clientOut.println(sb.toString());
			clientOut.println();
			return true;
		}

		private boolean getAudioEffectDefaultParameters(String inputLine) {
			String prefix = "MARY VOICE GETAUDIOEFFECTDEFAULTPARAM ";
			assert inputLine.startsWith(prefix);
			String effectName = inputLine.substring(prefix.length()).trim();
			AudioEffect effect = AudioEffects.getEffect(effectName);
			if (effect == null) {
				return false;
			}
			clientOut.println(effect.getExampleParameters().trim());
			clientOut.println();
			return true;
		}

		private boolean voiceGetFullAudioEffect(String inputLine) {
			String prefix = "MARY VOICE GETFULLAUDIOEFFECT ";
			assert inputLine.startsWith(prefix);
			String effectPlusParams = inputLine.substring(prefix.length()).trim();
			String[] parts = effectPlusParams.split("\\s", 2);
			String effectName = parts[0];
			String params = "";
			if (parts.length > 1) {
				params = parts[1]; // request contains effect params
			}
			AudioEffect effect = AudioEffects.getEffect(effectName);
			if (effect == null) {
				logger.error("Effect name missing in request!");
				return false;
			}
			// the request is about the parameters of a specific audio effect
			logger.debug("InfoRequest " + inputLine);
			effect.setParams(params);
			clientOut.println(effect.getFullEffectAsString());
			clientOut.println();
			return true;
		}

		private boolean getAudioEffectHelpText(String inputLine) {
			String prefix = "MARY VOICE GETAUDIOEFFECTHELPTEXT ";
			assert inputLine.startsWith(prefix);
			String effectName = inputLine.substring(prefix.length());
			AudioEffect effect = AudioEffects.getEffect(effectName);
			if (effect == null) {
				return false;
			}
			clientOut.println(effect.getHelpText().trim());
			clientOut.println();
			return true;
		}
	}
}