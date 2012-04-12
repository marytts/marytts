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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Locale;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.util.data.audio.AudioPlayer;
import marytts.client.MaryClient;
import marytts.client.http.Address;

/**
 * A demo class illustrating how to use the MaryClient class.
 * This will connect to a MARY server, version 4.x.
 * It requires maryclient.jar from MARY 4.0.
 * This works transparently with MARY servers in both http and socket server mode.
 * 
 * Compile this as follows:
 * <code>javac -cp maryclient.jar MaryClientUser.java</code>
 * 
 * And run as:
 * <code>java -cp .:maryclient.jar MaryClientUser</code>
 * 
 * @author marc
 *
 */

public class MaryClientUser {

    public static void main(String[] args)
    throws IOException, UnknownHostException, UnsupportedAudioFileException,
        InterruptedException
    {
        String serverHost = System.getProperty("server.host", "cling.dfki.uni-sb.de");
        int serverPort = Integer.getInteger("server.port", 59125).intValue();
        MaryClient mary = MaryClient.getMaryClient(new Address(serverHost, serverPort));
        String text = "Willkommen in der Welt der Sprachsynthese!";
        String locale = "de"; // or US English (en-US), Telugu (te), Turkish (tr), ...
        String inputType = "TEXT";
        String outputType = "AUDIO";
        String audioType = "WAVE";
        String defaultVoiceName = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mary.process(text, inputType, outputType, locale, audioType, defaultVoiceName, baos);
        // The byte array constitutes a full wave file, including the headers.
        // And now, play the audio data:
        AudioInputStream ais = AudioSystem.getAudioInputStream(
            new ByteArrayInputStream(baos.toByteArray()));
        LineListener lineListener = new LineListener() {
            public void update(LineEvent event) {
                if (event.getType() == LineEvent.Type.START) {
                    System.err.println("Audio started playing.");
                } else if (event.getType() == LineEvent.Type.STOP) {
                    System.err.println("Audio stopped playing.");
                } else if (event.getType() == LineEvent.Type.OPEN) {
                    System.err.println("Audio line opened.");
                } else if (event.getType() == LineEvent.Type.CLOSE) {
                    System.err.println("Audio line closed.");
                }
            }
        };

        AudioPlayer ap = new AudioPlayer(ais, lineListener);
        ap.start();
    }
}
