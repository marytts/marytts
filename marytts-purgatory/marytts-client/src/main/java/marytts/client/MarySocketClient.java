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
package marytts.client;

// General Java Classes
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat.Encoding;

import marytts.util.http.Address;
import marytts.util.string.StringUtils;

/**
 * A socket client implementing the MARY protocol. It can be used as a command line client or from within java code.
 * 
 * @author Marc Schr&ouml;der
 * @see MaryGUIClient A GUI interface to this client
 * @see marytts.server.MaryServer Description of the MARY protocol
 */

public class MarySocketClient extends MaryClient {

	/**
	 * The simplest way to create a mary client. It will connect to the MARY server running at DFKI. Only use this for testing
	 * purposes!
	 * 
	 * @throws IOException
	 *             if communication with the server fails
	 */
	public MarySocketClient() throws IOException {
		super();
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
	public MarySocketClient(Address serverAddress) throws IOException {
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
	public MarySocketClient(Address serverAddress, boolean profile, boolean quiet) throws IOException {
		super(serverAddress, profile, quiet);
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
		// Socket Client
		final Socket maryInfoSocket;
		try {
			maryInfoSocket = new Socket(data.hostAddress.getHost(), data.hostAddress.getPort());
		} catch (SocketException se) {
			throw new RuntimeException("Cannot connect to " + data.hostAddress.getFullAddress(), se);
		}
		final PrintWriter toServerInfo = new PrintWriter(new OutputStreamWriter(maryInfoSocket.getOutputStream(), "UTF-8"), true);
		final BufferedReader fromServerInfo = new BufferedReader(new InputStreamReader(maryInfoSocket.getInputStream(), "UTF-8"));

		// Formulate Request to Server:
		// System.err.println("Writing request to server.");
		toServerInfo.print("MARY IN=" + inputType + " OUT=" + outputType + " LOCALE=" + locale);
		if (audioType != null) {
			if (streamingAudio && data.serverCanStream) {
				toServerInfo.print(" AUDIO=STREAMING_" + audioType);
			} else {
				toServerInfo.print(" AUDIO=" + audioType);
			}
		}
		if (defaultVoiceName != null && !defaultVoiceName.equals("")) {
			toServerInfo.print(" VOICE=" + defaultVoiceName);
		}

		if (defaultStyle != null && !defaultStyle.equals("")) {
			toServerInfo.print(" STYLE=" + defaultStyle);
		}

		if (defaultEffects != null && !defaultEffects.equals("")) {
			toServerInfo.print(" EFFECTS=" + defaultEffects);
		}
		toServerInfo.println();

		// Receive a request ID:
		// System.err.println("Reading reply from server.");
		String helper = fromServerInfo.readLine();
		// System.err.println("Read from Server: " + helper);
		int id = -1;
		try {
			id = Integer.parseInt(helper);
		} catch (NumberFormatException e) {
			// Whatever we read from the server, it was not a number
			StringBuilder message = new StringBuilder("Server replied:\n");
			message.append(helper);
			message.append("\n");
			while ((helper = fromServerInfo.readLine()) != null) {
				message.append(helper);
				message.append("\n");
			}
			throw new IOException(message.toString());
		}

		// System.err.println("Read id " + id + " from server.");
		final Socket maryDataSocket = new Socket(data.hostAddress.getHost(), data.hostAddress.getPort());
		// System.err.println("Created second socket.");
		final PrintWriter toServerData = new PrintWriter(new OutputStreamWriter(maryDataSocket.getOutputStream(), "UTF-8"), true);
		toServerData.println(id);
		// System.err.println("Writing to server:");
		// System.err.print(input);
		toServerData.println(input.trim());
		maryDataSocket.shutdownOutput();

		// Check for warnings from server:
		final WarningReader warningReader = new WarningReader(fromServerInfo);
		warningReader.start();

		// Read from Server and copy into OutputStream output:
		// (as we only do low-level copying of bytes here,
		// we do not need to distinguish between text and audio)
		final InputStream fromServerStream = maryDataSocket.getInputStream();

		// If timeout is > 0, create a timer. It will close the input stream,
		// thus causing an IOException in the reading code.
		final Timer timer;
		if (timeout <= 0) {
			timer = null;
		} else {
			timer = new Timer();
			TimerTask timerTask = new TimerTask() {
				public void run() {
					System.err.println("Timer closes socket");
					try {
						maryDataSocket.close();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
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
						if (timer != null) {
							timer.cancel();
						}
						if (listener != null)
							listener.playerFinished();

						toServerInfo.close();
						fromServerInfo.close();
						maryInfoSocket.close();
						toServerData.close();
						maryDataSocket.close();
					} catch (Exception e) {
						e.printStackTrace();
					}

					try {
						warningReader.join();
					} catch (InterruptedException ie) {
					}
					if (warningReader.getWarnings().length() > 0) { // there are warnings
						String warnings = warningReader.getWarnings();
						System.err.println(warnings);
						if (listener != null)
							listener.playerException(new IOException(warnings));
					}

					if (doProfile) {
						long endTime = System.currentTimeMillis();
						long processingTime = endTime - startTime;
						System.err.println("Processed request in " + processingTime + " ms.");
					}
				}
			};
			if (streamingAudio) {
				t.start();
			} else {
				t.run(); // execute code in the current thread
			}
		} else { // output is an OutputStream
			OutputStream os = (OutputStream) output;
			InputStream bis = new BufferedInputStream(fromServerStream);
			byte[] bbuf = new byte[1024];
			int nr;
			while ((nr = bis.read(bbuf, 0, bbuf.length)) != -1) {
				// System.err.println("Read " + nr + " bytes from server.");
				os.write(bbuf, 0, nr);
			}
			os.flush();

			if (timeout > 0) {
				timer.cancel();
			}

			toServerInfo.close();
			fromServerInfo.close();
			maryInfoSocket.close();
			toServerData.close();
			maryDataSocket.close();

			try {
				warningReader.join();
			} catch (InterruptedException ie) {
			}
			if (warningReader.getWarnings().length() > 0) // there are warnings
				throw new IOException(warningReader.getWarnings());

			if (doProfile) {
				long endTime = System.currentTimeMillis();
				long processingTime = endTime - startTime;
				System.err.println("Processed request in " + processingTime + " ms.");
			}
		}
	}

	/**
	 * From an open server connection, read one chunk of info data. Writes the infoCommand to the server, then reads from the
	 * server until an empty line or eof is read.
	 * 
	 * @param infoCommand
	 *            the one-line request to send to the server
	 * @return a string representing the server response, lines being separated by a '\n' character.
	 * @throws IOException
	 *             if communication with the server fails
	 */
	private String getServerInfo(String infoCommand) throws IOException {
		Socket marySocket = new Socket(data.hostAddress.getHost(), data.hostAddress.getPort());
		PrintWriter toServerInfo = new PrintWriter(new OutputStreamWriter(marySocket.getOutputStream(), "UTF-8"), true);
		BufferedReader fromServerInfo = new BufferedReader(new InputStreamReader(marySocket.getInputStream(), "UTF-8"));

		toServerInfo.println(infoCommand);
		StringBuilder result = new StringBuilder();
		String line = null;
		// Read until either end of file or an empty line
		while ((line = fromServerInfo.readLine()) != null && !line.equals("")) {
			result.append(line);
			result.append("\n");
		}
		marySocket.close();
		return result.toString();
	}

	/**
	 * Get the version info from the server. This is optional information which is not required for the normal operation of the
	 * client, but may help to avoid incompatibilities.
	 * 
	 * @throws IOException
	 *             if communication with the server fails
	 * @throws UnknownHostException
	 *             if the host could not be found
	 */
	protected void fillServerVersion() throws IOException, UnknownHostException {
		// Expect 3 lines of the kind
		// Mary TTS server
		// Specification version 1.9.1
		// Implementation version 20030207
		String info = getServerInfo("MARY VERSION");
		if (info.length() == 0)
			throw new IOException("Could not get version info from Mary server");
		info = info.replace('\n', ' ');
		data.toServerVersionInfo(info);
	}

	@Override
	protected void fillDataTypes() throws UnknownHostException, IOException {
		// Expect a variable number of lines of the kind
		// RAWMARYXML INPUT OUTPUT
		// TEXT_DE LOCALE=de INPUT
		// AUDIO OUTPUT
		String info = getServerInfo("MARY LIST DATATYPES");
		if (info.length() == 0)
			throw new IOException("Could not get list of data types from Mary server");
		data.toDataTypes(info);
	}

	@Override
	protected void fillVoices() throws IOException, UnknownHostException {
		// Expect a variable number of lines of the kind
		// de7 de female
		// us2 en male
		// dfki-stadium-emo de male limited
		String info = getServerInfo("MARY LIST VOICES");
		if (info.length() == 0)
			throw new IOException("Could not get voice list from Mary server");
		data.toVoices(info);
	}

	@Override
	protected void fillLocales() throws IOException, UnknownHostException {
		String info = getServerInfo("MARY LIST LOCALES");
		if (info.length() == 0)
			throw new IOException("Could not get locales list from Mary server");
		data.toLocales(info);
	}

	@Override
	protected void fillVoiceExampleTexts(String voicename) throws IOException {
		String info = getServerInfo("MARY VOICE EXAMPLETEXT " + voicename);
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
		String info = getServerInfo("MARY EXAMPLETEXT " + dataType + " " + locale);

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
		return getServerInfo("MARY VOICE GETDEFAULTAUDIOEFFECTS");
	}

	@Override
	public String requestDefaultEffectParameters(String effectName) throws IOException, UnknownHostException {
		String info = getServerInfo("MARY VOICE GETAUDIOEFFECTDEFAULTPARAM " + effectName);
		return info.replaceAll("\n", System.getProperty("line.separator"));
	}

	@Override
	public String requestFullEffect(String effectName, String currentEffectParams) throws IOException, UnknownHostException {
		String info = getServerInfo("MARY VOICE GETFULLAUDIOEFFECT " + effectName + " " + currentEffectParams);
		return info.replaceAll("\n", System.getProperty("line.separator"));
	}

	@Override
	protected void fillEffectHelpText(String effectName) throws IOException {
		String info = getServerInfo("MARY VOICE GETAUDIOEFFECTHELPTEXT " + effectName);
		data.audioEffectHelpTextsMap.put(effectName, info.replaceAll("\n", System.getProperty("line.separator")));
	}

	@Override
	public boolean isHMMEffect(String effectName) throws IOException, UnknownHostException {
		String info = getServerInfo("MARY VOICE ISHMMAUDIOEFFECT " + effectName);

		if (info.length() == 0)
			return false;

		boolean bRet = false;
		info = info.toLowerCase();
		if (info.indexOf("yes") > -1)
			bRet = true;

		return bRet;
	}

	public String getFeatures(String locale) throws IOException {
		throw new RuntimeException("not implemented");
	}

	public String getFeaturesForVoice(String voice) throws IOException {
		throw new RuntimeException("not implemented");
	}

	@Override
	protected void fillAudioFileFormatAndOutTypes() throws IOException {
		String audioFormatInfo = getServerInfo("MARY LIST AUDIOFILEFORMATTYPES");
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

}
