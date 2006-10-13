/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.mary.emospeak;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Vector;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.client.MaryClient;

/**
 * A MaryClient that runs in a thread of its own. Requests for synthesis
 * are scheduled through <code>scheduleRequest()</code>, which is not
 * synchronized. Only the last unprocessed request is remembered.
 *
 * @author  Marc Schr&ouml;der
 */
public class AsynchronousThreadedMaryClient extends Thread {
    private int r;
    private AudioFileReceiver emoSpeak;
    private de.dfki.lt.mary.client.MaryClient processor;
    private boolean inputAvailable = false;
    private String latestRequest = null;
    private MaryClient.Voice latestRequestVoice = null;
    private AudioInputStream latestAudio = null;
    private boolean exitRequested = false;
    
    /** Creates new AsynchronousThreadedMaryClient */
    public AsynchronousThreadedMaryClient(AudioFileReceiver emoSpeak) 
    throws IOException, UnknownHostException {
        this.emoSpeak = emoSpeak;
        processor = new MaryClient();
    }

    /** Constructor to be used by applets */
    public AsynchronousThreadedMaryClient(AudioFileReceiver emoSpeak,
       String serverHost, int serverPort, boolean printProfilingInfo, boolean beQuiet)
    throws IOException, UnknownHostException {
        this.emoSpeak = emoSpeak;
        processor = new MaryClient(serverHost, serverPort, printProfilingInfo, beQuiet);
    }

    /**
     * Schedule the latest request. Any previous, unprocessed requests 
     * are deleted.
     * @param prosodyxmlString the maryxml data to be synthesised.
     * @param voice the synthesis voice to use
     * @param requestNumber request number
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
    private void processInput()
    throws IOException, UnknownHostException, UnsupportedAudioFileException {
        java.io.ByteArrayOutputStream os = new ByteArrayOutputStream();
        assert latestRequestVoice != null;
        processor.process(latestRequest,
                          "RAWMARYXML",
                          "AUDIO",
                          "AU",
                          latestRequestVoice.name(),
                          os);
        byte[] bytes = os.toByteArray();
        latestAudio = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes));
    }
    
    public String getHost()
    {
        return processor.getHost();
    }
    
    public int getPort()
    {
        return processor.getPort();
    }

    public Vector getServerVoices() throws IOException
    {
        return processor.getGeneralDomainVoices();
    }
    
    public Vector getServerVoices(Locale locale) throws IOException
    {
        return processor.getGeneralDomainVoices(locale);
    }

    private synchronized void doWait() {
        try {
            wait();
        } catch (InterruptedException e) {}
    }
    
    public void run() {
        while (!exitRequested) {
            if (inputAvailable) {
                // heuristic sleep value, waiting for more reasonable new mouse position:
                try {
                    sleep(200);
                } catch (InterruptedException e) {}
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
