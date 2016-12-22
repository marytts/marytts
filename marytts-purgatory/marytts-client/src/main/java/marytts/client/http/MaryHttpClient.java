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
package marytts.client.http;

// General Java Classes
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.client.AudioFormatOutputStream;
import marytts.client.MaryClient;
import marytts.client.MaryGUIClient;
import marytts.util.http.Address;
import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;

/**
 * An HTTP client implementing the MARY protocol. It can be used as a command line client or from within java code.
 * 
 * @author Marc Schr&ouml;der, oytun.turk
 * @see MaryGUIClient A GUI interface to this client
 * @see marytts.server.MaryServer Description of the MARY protocol.
 */

public class MaryHttpClient extends MaryClient {

	/**
	 * The simplest way to create a mary client. It will connect to the MARY server running at DFKI. Only use this for testing
	 * purposes!
	 * 
	 * @throws IOException
	 *             if communication with the server fails
	 */
	public MaryHttpClient() throws IOException {
		super();
	}

	public MaryHttpClient(boolean quiet) throws IOException {
		super(quiet);
	}

	/**
	 * The typical way to create a mary client. It will connect to the MARY client running at the given host and port. This
	 * constructor reads two system properties:
	 * <ul>
	 * <li><code>mary.client.profile</code> (=true/false) - determines whether profiling (timing) information is calculated;</li>
	 * <li><code>mary.client.quiet</code> (=true/false) - tells the client not to print any of the normal information to stderr.</li>
	 * </ul>
	 * 
	 * @param serverAddress
	 *            the address of the server
	 * @throws IOException
	 *             if communication with the server fails
	 */
	public MaryHttpClient(Address serverAddress) throws IOException {
		super(serverAddress);
	}

	/**
	 * An alternative way to create a mary client, which works with applets. It will connect to the MARY client running at the
	 * given host and port. Note that in applets, the host must be the same as the one from which the applet was loaded;
	 * otherwise, a security exception is thrown.
	 * 
	 * @param serverAddress
	 *            the address of the server
	 * @param profile
	 *            determines whether profiling (timing) information is calculated
	 * @param quiet
	 *            tells the client not to print any of the normal information to stderr
	 * @throws IOException
	 *             if communication with the server fails
	 */
	public MaryHttpClient(Address serverAddress, boolean profile, boolean quiet) throws IOException {
		super(serverAddress, profile, quiet);
	}

	// /////////////////////////////////////////////////////////////////////
	// ////////////////////// Information requests /////////////////////////
	// /////////////////////////////////////////////////////////////////////

	@Override
	protected void fillAudioFileFormatAndOutTypes() throws IOException {
		String audioFormatInfo = serverInfoRequest("audioformats", null);
		data.audioOutTypes = new Vector<String>(Arrays.asList(StringUtils.toStringArray(audioFormatInfo)));
		data.audioFileFormatTypes = new Vector<String>();
		for (String af : data.audioOutTypes) {
			if (af.endsWith("_FILE")) {
				String typeName = af.substring(0, af.indexOf("_"));
				try {
					AudioFileFormat.Type type = MaryClient.getAudioFileFormatType(typeName);
					data.audioFileFormatTypes.add(typeName + " " + type.getExtension());
				} catch (Exception e) {
				}
			}
		}
	}

	@Override
	protected void fillServerVersion() throws IOException {
		data.toServerVersionInfo(serverInfoRequest("version", null));
	}

	@Override
	protected void fillDataTypes() throws IOException {
		data.toDataTypes(serverInfoRequest("datatypes", null));
	}

	protected void fillVoices() throws IOException {
		data.toVoices(serverInfoRequest("voices", null));
	}

	@Override
	protected void fillLocales() throws IOException {
		data.toLocales(serverInfoRequest("locales", null));
	}

	@Override
	protected void fillVoiceExampleTexts(String voicename) throws IOException {
		Map<String, String> queryItems = new HashMap<String, String>();
		queryItems.put("voice", voicename);
		String info = serverInfoRequest("exampletext", queryItems);

		if (info.length() == 0)
			throw new IOException("Could not get example text from Mary server");

		StringTokenizer st = new StringTokenizer(info, "\n");
		Vector<String> sentences = new Vector<String>();
		while (st.hasMoreTokens()) {
			sentences.add(st.nextToken());
		}
		data.voiceExampleTextsLimitedDomain.put(voicename, sentences);
	}

	@Override
	protected void fillServerExampleText(String dataType, String locale) throws IOException {
		Map<String, String> queryItems = new HashMap<String, String>();
		queryItems.put("datatype", dataType);
		queryItems.put("locale", locale);
		String info = serverInfoRequest("exampletext", queryItems);

		if (info.length() == 0)
			throw new IOException("Could not get example text from Mary server");

		data.serverExampleTexts.put(dataType + " " + locale, info.replaceAll("\n", System.getProperty("line.separator")));
	}

	/**
	 * Request the available audio effects for a voice from the server
	 * 
	 * @return A string of available audio effects and default parameters, i.e. "FIRFilter,Robot(amount=50)"
	 * @throws IOException
	 *             IOException
	 */
	@Override
	protected String getDefaultAudioEffects() throws IOException {
		return serverInfoRequest("audioeffects", null);
	}

	@Override
	public String requestDefaultEffectParameters(String effectName) throws IOException {
		Map<String, String> queryItems = new HashMap<String, String>();
		queryItems.put("effect", effectName);

		String info = serverInfoRequest("audioeffect-default-param", queryItems);

		if (info == null || info.length() == 0)
			return "";

		return info.replaceAll("\n", System.getProperty("line.separator"));
	}

	@Override
	public String requestFullEffect(String effectName, String currentEffectParameters) throws IOException {
		Map<String, String> queryItems = new HashMap<String, String>();
		queryItems.put("effect", effectName);
		queryItems.put("params", currentEffectParameters);

		String info = serverInfoRequest("audioeffect-full", queryItems);

		if (info.length() == 0)
			return "";

		return info.replaceAll("\n", System.getProperty("line.separator"));
	}

	@Override
	protected void fillEffectHelpText(String effectName) throws IOException {
		Map<String, String> queryItems = new HashMap<String, String>();
		queryItems.put("effect", effectName);

		String info = serverInfoRequest("audioeffect-help", queryItems);
		data.audioEffectHelpTextsMap.put(effectName, info.replaceAll("\n", System.getProperty("line.separator")));
	}

	@Override
	public boolean isHMMEffect(String effectName) throws IOException {
		Map<String, String> queryItems = new HashMap<String, String>();
		queryItems.put("effect", effectName);

		String info = serverInfoRequest("audioeffect-is-hmm-effect", queryItems);

		if (info.length() == 0)
			return false;

		boolean bRet = false;
		info = info.toLowerCase();
		if (info.indexOf("yes") > -1)
			bRet = true;

		return bRet;
	}

	public String getFeatures(String locale) throws IOException {
		return serverInfoRequest("features?locale=" + locale, null);
	}

	public String getDiscreteFeatures(String locale) throws IOException {
		return serverInfoRequest("features-discrete?locale=" + locale, null);
	}

	public String getFeaturesForVoice(String voice) throws IOException {
		return serverInfoRequest("features?voice=" + voice, null);
	}

	private String serverInfoRequest(String request, Map<String, String> queryItems) throws IOException {
		StringBuilder url = new StringBuilder();
		url.append(data.hostAddress.getHttpAddress()).append("/").append(request);
		if (queryItems != null) {
			url.append("?");
			boolean first = true;
			for (String key : queryItems.keySet()) {
				if (first)
					first = false;
				else
					url.append("&");
				url.append(key).append("=");
				url.append(URLEncoder.encode(queryItems.get(key), "UTF-8"));
			}
		}
		return serverInfoRequest(new URL(url.toString()));

	}

	private String serverInfoRequest(URL url) throws IOException {
		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		http.setRequestMethod("GET");
		http.connect();

		if (http.getResponseCode() != HttpURLConnection.HTTP_OK) {
			String errorData = "";
			try {
				errorData = FileUtils.getStreamAsString(http.getErrorStream(), "UTF-8");
			} catch (Exception e) {
			}
			throw new IOException(http.getResponseCode() + ":" + http.getResponseMessage() + "\n" + errorData);
		}
		return FileUtils.getStreamAsString(http.getInputStream(), "UTF-8");
		/*
		 * The following is example code if we were to use HttpClient: HttpClient httpclient = new DefaultHttpClient();
		 * 
		 * HttpGet httpget = new HttpGet("http://www.google.com/");
		 * 
		 * System.out.println("executing request " + httpget.getURI());
		 * 
		 * // Create a response handler ResponseHandler<String> responseHandler = new BasicResponseHandler(); String responseBody
		 * = httpclient.execute(httpget, responseHandler); System.out.println(responseBody);
		 */
	}

	/*
	 * private String getFromServer(String key) throws IOException { return getFromServer(key, null); }
	 * 
	 * //This handles each request one by one private String getFromServer(String key, String params) throws IOException { if
	 * (data.keyValuePairs==null) data.keyValuePairs = new HashMap<String, String>();
	 * 
	 * Map<String, String> singleKeyValuePair = new HashMap<String, String>();
	 * 
	 * if (params==null || params.length()<1) singleKeyValuePair.put(key, "?"); else singleKeyValuePair.put(key, "? " + params);
	 * 
	 * singleKeyValuePair = httpRequester.request(data.hostAddress, singleKeyValuePair);
	 * 
	 * data.keyValuePairs.put(key, singleKeyValuePair.get(key));
	 * 
	 * return data.keyValuePairs.get(key); }
	 */

	// /////////////////////////////////////////////////////////////////////
	// ////////////////////// Actual synthesis requests ////////////////////
	// /////////////////////////////////////////////////////////////////////

	private InputStream requestInputStream(String input, String inputType, String outputType, String locale, String audioType,
			String defaultVoiceName, String defaultStyle, Map<String, String> effects, // String defaultEffects,
			boolean streamingAudio, String outputTypeParams) throws IOException {

		StringBuilder params = new StringBuilder();
		params.append("INPUT_TEXT=").append(URLEncoder.encode(input, "UTF-8"));
		params.append("&INPUT_TYPE=").append(URLEncoder.encode(inputType, "UTF-8"));
		params.append("&OUTPUT_TYPE=").append(URLEncoder.encode(outputType, "UTF-8"));
		if (locale != null) {
			params.append("&LOCALE=").append(URLEncoder.encode(locale, "UTF-8"));
		}
		if (audioType != null) {
			params.append("&AUDIO=").append(
					URLEncoder.encode((streamingAudio && data.serverCanStream) ? audioType + "_STREAM" : audioType + "_FILE",
							"UTF-8"));
		}
		if (outputTypeParams != null) {
			params.append("&OUTPUT_TYPE_PARAMS=").append(URLEncoder.encode(outputTypeParams, "UTF-8"));
		}

		if (defaultVoiceName != null) {
			params.append("&VOICE=").append(URLEncoder.encode(defaultVoiceName, "UTF-8"));
		}

		if (defaultStyle != null) {
			params.append("&STYLE=").append(URLEncoder.encode(defaultStyle, "UTF-8"));
		}

		if (effects != null) {
			for (String key : effects.keySet()) {
				params.append("&").append(key).append("=").append(URLEncoder.encode(effects.get(key), "UTF-8"));
			}
		}

		// to make HTTP Post request with HttpURLConnection
		URL url = new URL(data.hostAddress.getHttpAddress() + "/process");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestMethod("POST");
		conn.setAllowUserInteraction(false); // no user interact [like pop up]
		conn.setDoOutput(true); // want to send
		conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
		OutputStream ost = conn.getOutputStream();
		PrintWriter pw = new PrintWriter(ost);
		pw.print(params.toString()); // here we "send" our body!
		pw.flush();
		pw.close();

		// and InputStream from here will be body
		try {
			return conn.getInputStream();
		} catch (IOException e) {
			String error;
			try {
				error = FileUtils.getStreamAsString(conn.getErrorStream(), "UTF-8");
			} catch (IOException errE) {
				// ok cannot get error message, just re-throw original e
				throw new IOException("No detailed error message available", e);
			}
			throw new IOException("Error message from server:\n" + error, e);
		}

	}

	private Map<String, String> effectsString2EffectsMap(String effectsString) {
		if (effectsString == null)
			return null;
		Map<String, String> effectsMap = new HashMap<String, String>();

		StringTokenizer st = new StringTokenizer(effectsString, ",");
		while (st.hasMoreTokens()) {
			String oneEffect = st.nextToken().trim();
			String name;
			String params;
			int iBracket = oneEffect.indexOf('(');
			if (iBracket > -1) {
				name = oneEffect.substring(0, iBracket);
				params = oneEffect.substring(iBracket + 1, oneEffect.indexOf(')', iBracket + 1));
			} else { // no parameters
				name = oneEffect;
				params = "";
			}
			effectsMap.put("effect_" + name + "_selected", "on");
			effectsMap.put("effect_" + name + "_parameters", params);
		}
		return effectsMap;
	}

	@Override
	protected void _process(String input, String inputType, String outputType, String locale, String audioType,
			String defaultVoiceName, String defaultStyle, String defaultEffects, Object output, long timeout,
			boolean streamingAudio, String outputTypeParams, AudioPlayerListener playerListener) throws IOException {
		boolean isMaryAudioPlayer = false;
		if (output instanceof marytts.util.data.audio.AudioPlayer) {
			isMaryAudioPlayer = true;
		} else if (output instanceof OutputStream) {
		} else {
			throw new IllegalArgumentException("Expected OutputStream or AudioPlayer, got " + output.getClass().getName());
		}
		final long startTime = System.currentTimeMillis();

		final InputStream fromServerStream = requestInputStream(input, inputType, outputType, locale, audioType,
				defaultVoiceName, defaultStyle, effectsString2EffectsMap(defaultEffects), streamingAudio, outputTypeParams);

		// If timeout is > 0, create a timer. It will close the input stream,
		// thus causing an IOException in the reading code.
		final Timer timer;
		if (timeout <= 0) {
			timer = null;
		} else {
			timer = new Timer();
			TimerTask timerTask = new TimerTask() {
				public void run() {
					System.err.println("Timer closes connection");
					/*
					 * try { maryDataSocket.close(); } catch (IOException ioe) { ioe.printStackTrace(); }
					 */
				}
			};
			timer.schedule(timerTask, timeout);
		}

		if (isMaryAudioPlayer) {
			final marytts.util.data.audio.AudioPlayer player = (marytts.util.data.audio.AudioPlayer) output;
			final AudioPlayerListener listener = playerListener;
			Thread t = new Thread() {
				public void run() {
					try {
						InputStream in = fromServerStream;
						if (doProfile)
							System.err.println("After " + (System.currentTimeMillis() - startTime)
									+ " ms: Trying to read data from server");
						in = new BufferedInputStream(in);
						in.mark(1000);
						AudioInputStream fromServerAudio = AudioSystem.getAudioInputStream(in);

						if (fromServerAudio.getFrameLength() == 0) { // weird bug under Java 1.4
							// in.reset();
							fromServerAudio = new AudioInputStream(in, fromServerAudio.getFormat(), AudioSystem.NOT_SPECIFIED);
						}
						// System.out.println("Audio framelength: "+fromServerAudio.getFrameLength());
						// System.out.println("Audio frame size: "+fromServerAudio.getFormat().getFrameSize());
						// System.out.println("Audio format: "+fromServerAudio.getFormat());
						if (doProfile)
							System.err.println("After " + (System.currentTimeMillis() - startTime) + " ms: Audio available: "
									+ in.available());
						AudioFormat audioFormat = fromServerAudio.getFormat();
						if (!audioFormat.getEncoding().equals(Encoding.PCM_SIGNED)) { // need conversion, e.g. for mp3
							audioFormat = new AudioFormat(fromServerAudio.getFormat().getSampleRate(), 16, 1, true, false);
							fromServerAudio = AudioSystem.getAudioInputStream(audioFormat, fromServerAudio);
						}
						player.setAudio(fromServerAudio);
						player.run(); // not start(), i.e. execute in this thread

						if (timer != null)
							timer.cancel();

						if (listener != null)
							listener.playerFinished();

						// toServerInfo.close();
						// fromServerInfo.close();
						// maryInfoSocket.close();
						// toServerData.close();
						// maryDataSocket.close();
					} catch (Exception e) {
						e.printStackTrace();
					}

					// try {
					// warningReader.join();
					// } catch (InterruptedException ie) {}
					// if (warningReader.getWarnings().length() > 0) { // there are warnings
					// String warnings = warningReader.getWarnings();
					// System.err.println(warnings);
					// if (listener != null) listener.playerException(new IOException(warnings));
					// }

					if (doProfile) {
						long endTime = System.currentTimeMillis();
						long processingTime = endTime - startTime;
						System.err.println("Processed request in " + processingTime + " ms.");
					}
				}
			};

			if (streamingAudio)
				t.start();
			else
				t.run(); // execute code in the current thread
		} else // output is an OutputStream
		{
			OutputStream os = (OutputStream) output;
			InputStream bis = new BufferedInputStream(fromServerStream);
			if (os instanceof AudioFormatOutputStream) {
				AudioFormatOutputStream afos = (AudioFormatOutputStream) os;
				try {
					AudioFileFormat format = AudioSystem.getAudioFileFormat(bis);
					afos.setFormat(format.getFormat());
				} catch (UnsupportedAudioFileException e) {
					throw new IOException(e.getMessage(), e);
				}
			}
			byte[] bbuf = new byte[1024];
			int nr;
			while ((nr = bis.read(bbuf, 0, bbuf.length)) != -1) {
				// System.err.println("Read " + nr + " bytes from server.");
				os.write(bbuf, 0, nr);
			}
			os.flush();

			if (timeout > 0)
				timer.cancel();

			// toServerInfo.close();
			// fromServerInfo.close();
			// maryInfoSocket.close();
			// toServerData.close();
			// maryDataSocket.close();

			// try {
			// warningReader.join();
			// } catch (InterruptedException ie) {}
			// if (warningReader.getWarnings().length() > 0) // there are warnings
			// throw new IOException(warningReader.getWarnings());

			if (doProfile) {
				long endTime = System.currentTimeMillis();
				long processingTime = endTime - startTime;
				System.err.println("Processed request in " + processingTime + " ms.");
			}
		}
	}

}
