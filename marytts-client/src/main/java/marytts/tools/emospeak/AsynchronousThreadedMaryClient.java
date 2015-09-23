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
package marytts.tools.emospeak;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Vector;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.client.MaryClient;
import marytts.util.http.Address;

/**
 * A MaryClient that runs in a thread of its own. Requests for synthesis are scheduled through <code>scheduleRequest()</code>,
 * which is not synchronized. Only the last unprocessed request is remembered.
 * 
 * @author Marc Schr&ouml;der
 */
public class AsynchronousThreadedMaryClient extends Thread {
	private int r;
	private AudioFileReceiver emoSpeak;
	private marytts.client.MaryClient processor;
	private boolean inputAvailable = false;
	private String latestRequest = null;
	private MaryClient.Voice latestRequestVoice = null;
	private AudioInputStream latestAudio = null;
	private boolean exitRequested = false;

	/**
	 * Creates new AsynchronousThreadedMaryClient
	 * 
	 * @param emoSpeak
	 *            emoSpeak
	 * @throws IOException
	 *             IOException
	 * @throws UnknownHostException
	 *             UnknownHostException
	 */
	public AsynchronousThreadedMaryClient(AudioFileReceiver emoSpeak) throws IOException, UnknownHostException {
		this.emoSpeak = emoSpeak;
		processor = MaryClient.getMaryClient();
	}

	/**
	 * Constructor to be used by applets
	 * 
	 * @param emoSpeak
	 *            emoSpeak
	 * @param serverHost
	 *            serverHost
	 * @param serverPort
	 *            serverPort
	 * @param printProfilingInfo
	 *            printProfilingInfo
	 * @param beQuiet
	 *            beQuiet
	 * @throws IOException
	 *             IOException
	 * @throws UnknownHostException
	 *             UnknownHostException
	 */
	public AsynchronousThreadedMaryClient(AudioFileReceiver emoSpeak, String serverHost, int serverPort,
			boolean printProfilingInfo, boolean beQuiet) throws IOException, UnknownHostException {
		this.emoSpeak = emoSpeak;
		processor = MaryClient.getMaryClient(new Address(serverHost, serverPort), printProfilingInfo, beQuiet);
	}

	/**
	 * Schedule the latest request. Any previous, unprocessed requests are deleted.
	 * 
	 * @param prosodyxmlString
	 *            the maryxml data to be synthesised.
	 * @param voice
	 *            the synthesis voice to use
	 * @param requestNumber
	 *            request number
	 */
	public synchronized void scheduleRequest(String prosodyxmlString, MaryClient.Voice voice, int requestNumber) {
		latestRequest = prosodyxmlString;
		latestRequestVoice = voice;
		inputAvailable = true;
		this.r = requestNumber;
		notifyAll();
	}

	public synchronized void requestExit() {
		exitRequested = true;
		notifyAll();
	}

	// Call the mary client
	private void processInput() throws IOException, UnknownHostException, UnsupportedAudioFileException {
		java.io.ByteArrayOutputStream os = new ByteArrayOutputStream();
		assert latestRequestVoice != null;
		processor.process(latestRequest, "RAWMARYXML", "AUDIO", latestRequestVoice.getLocale().toString(), "AU",
				latestRequestVoice.name(), os);
		byte[] bytes = os.toByteArray();
		latestAudio = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes));
	}

	public String getHost() {
		return processor.getHost();
	}

	public int getPort() {
		return processor.getPort();
	}

	public Vector getServerVoices() throws IOException {
		return processor.getGeneralDomainVoices();
	}

	public Vector getServerVoices(Locale locale) throws IOException {
		return processor.getGeneralDomainVoices(locale);
	}

	private synchronized void doWait() {
		try {
			wait();
		} catch (InterruptedException e) {
		}
	}

	public void run() {
		while (!exitRequested) {
			if (inputAvailable) {
				// heuristic sleep value, waiting for more reasonable new mouse position:
				try {
					sleep(200);
				} catch (InterruptedException e) {
				}
				inputAvailable = false;
				int r1 = r;
				long t0 = System.currentTimeMillis();
				try {
					processInput();
					long t = System.currentTimeMillis() - t0;
					System.err.println("MaryClient has processed request no." + r1 + " in " + t + " ms.");
					emoSpeak.setNextAudio(latestAudio);
				} catch (Exception e) {
					System.err.println("Problem creating synthesis audio:");
					e.printStackTrace();
					emoSpeak.setNextAudio(null);
				}
			} else {
				doWait();
				System.err.println("MaryClient waking up from wait.");
			}
		}
		System.err.println("MaryClient exiting.");
	}
}
