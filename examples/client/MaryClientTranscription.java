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

/////
import marytts.util.dom.MaryDomUtils;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryXML;
import marytts.datatypes.MaryDataType;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.TreeWalker;

/**
 * A demo class illustrating how to use the MaryClient class. This will connect to a MARY server, version 4.x. It requires
 * maryclient.jar from MARY 4.0. This works transparently with MARY servers in both http and socket server mode.
 * 
 * Compile this as follows: <code>javac -cp maryclient.jar MaryClientTranscription.java</code>
 * 
 * And run as: <code>java -cp .:maryclient.jar MaryClientTranscription</code>
 * 
 * @author marc
 * 
 */

public class MaryClientTranscription {

    public static void main(String[] args) throws IOException, UnknownHostException, UnsupportedAudioFileException,
            InterruptedException, ParserConfigurationException, SAXException, TransformerConfigurationException,
            TransformerException {
        String serverHost = System.getProperty("server.host", "localhost");
        int serverPort = Integer.getInteger("server.port", 59125).intValue();
        MaryClient mary = MaryClient.getMaryClient(new Address(serverHost, serverPort));
        String text = "Questa è una frase di prova.";
        String locale = "it"; // or US English (en-US), Telugu (te), Turkish (tr), ...
        String inputType = "TEXT";
        String outputType = "ALLOPHONES";
        String audioType = null;
        String defaultVoiceName = null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mary.process(text, inputType, outputType, locale, audioType, defaultVoiceName, baos);

        // read into mary data object
        MaryData maryData = new MaryData(MaryDataType.ALLOPHONES, null);
        maryData.readFrom(new ByteArrayInputStream(baos.toByteArray()));
        Document doc = maryData.getDocument();
        assert doc != null: "null sentence";
        
        TreeWalker phWalker = MaryDomUtils.createTreeWalker(doc, doc, MaryXML.PHONE);
        Element ph;
        String lTranscription = "";
        while ((ph = (Element) phWalker.nextNode()) != null) {
            lTranscription = lTranscription + ph.getAttribute("p") + ' ';
        }
        lTranscription = lTranscription.substring(0, lTranscription.length() - 1);
        System.out.println('<' + lTranscription + '>');
    }

}
