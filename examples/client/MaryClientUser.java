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

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.signalproc.util.AudioPlayer;
import de.dfki.lt.mary.client.MaryClient;


/**
/**
 * MARY client <b>protocol</b>:
 * <p>
 * A client opens two socket connections to the server. The first,
 * <code>infoSocket</code>, serves for passing meta-information,
 * such as the requested input and output types or warnings.
 * The second, <code>dataSocket</code>, serves for passing the actual
 * input and output data.
 * The server expects the communication as follows.
 * <ol>
 * <li> The client opens an <code>infoSocket</code>,
 * optionally sends one line "MARY VERSION" to obtain
 * three lines of version information, and then sends one line
 * "MARY IN=INPUTTYPE OUT=OUTPUTTYPE [AUDIO=AUDIOTYPE]",
 * where INPUTTYPE and OUTPUTTYPE can have the following values:
 * <ul>
 *   <li>  TEXT_DE          plain ASCII text, German (input only) </li>
 *   <li>  TEXT_EN          plain ASCII text, English (input only) </li>
 *   <li>  SABLE         text annotated with SABLE markup (input only) </li>
 *   <li>  SSML          text annotated with SSML markup (input only) </li>
 *   <li>  RAWMARYXML    untokenised MaryXML </li>
 *   <li>  TOKENISED_DE     tokenized text </li>
 *   <li>  PREPROCESSED_DE  numbers and abbreviations expanded </li>
 *   <li>  CHUNKED_DE       parts of speech and chunk tags added </li>
 *   <li>  PHONEMISED_DE    phoneme symbols </li>
 *   <li>  INTONISED_DE     GToBI intonation symbols </li>
 *   <li>  POSTPROCESSED_DE post-lexical phonological rules </li>
 *   <li>  ACOUSTPARAMS  acoustic parameters in MaryXML structure </li>
 *   <li>  MBROLA        phone symbols, duration and frequency values </li>
 *   <li>  AUDIO         audio data (output only) </li>
 * </ul>
 * INPUTTYPE must be earlier in this list than OUTPUTTYPE.
 * <p>
 * The optional AUDIO=AUDIOTYPE specifies the type of audio file
 * to be sent for audio output. Possible values are:
 * <ul>
 *   <li> WAVE </li>
 *   <li> AU </li>
 *   <li> SND </li>
 *   <li> AIFF </li>
 *   <li> AIFC </li>
 *   <li> MP3 </li>
 * </ul>
 * <p>
 * The optional VOICE=VOICENAME specifies the default voice with which
 * the text is to be spoken. Possible values are currently:
 * <ul>
 *   <li> female </li>
 *   <li> male </li>
 *   <li> de1 </li>
 *   <li> de2 </li>
 *   <li> de3 </li>
 *   <li> de4 </li>
 *   <li> de5 </li>
 *   <li> de6 </li>
 *   <li> de7 </li>
 *   <li> us1 </li>
 *   <li> us2 </li>
 *   <li> us3 </li>
 * </ul>
 * <p>
 * Example: The line
 * <pre>
 *   MARY IN=TEXT_DE OUT=AUDIO AUDIO=WAVE VOICE=female
 * </pre>
 * will process normal ASCII text, and send back a WAV audio file
 * synthesised with a female voice.
 * </li>
 *
 * <li> The server reads and parses this input line. If its format is correct,
 * a line containing a single integer is sent back to the client
 * on <code>infoSocket</code>. This
 * integer is a unique identification number for this request.
 * </li>
 *
 * <li> The client opens a second socket connection to the server, on the same
 * port, the <code>dataSocket</code>. As a first line on this
 * <code>dataSocket</code>,
 * it sends the single integer it had just received via the
 * <code>infoSocket</code>.
 * </li>
 *
 * <li> The server groups dataSocket and infoSocket together based on this
 * identification number, and starts reading data of the requested input
 * type from <code>dataSocket</code>.
 * </li>
 *
 * <li> If any errors or warning messages are issued during input parsing or
 * consecutive processing, these are printed to <code>infoSocket</code>.
 * </li>
 *
 * <li> The processing result is output to <code>dataSocket</code>.
 * </li>
 * </ol>
 * @author Marc Schr&ouml;der
 */
public class MaryClientUser {

    public static void main(String[] args)
    throws IOException, UnknownHostException, UnsupportedAudioFileException,
        InterruptedException
    {
        String serverHost = System.getProperty("server.host", "cling.dfki.uni-sb.de");
        int serverPort = Integer.getInteger("server.port", 59125).intValue();
        MaryClient mary = new MaryClient(serverHost, serverPort);
        String text = "Willkommen in der Welt der Sprachsynthese!";
        String inputType = "TEXT_DE";
        String outputType = "AUDIO";
        String audioType = "WAVE";
        String defaultVoiceName = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mary.process(text, inputType, outputType, audioType,
            defaultVoiceName, baos);
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
