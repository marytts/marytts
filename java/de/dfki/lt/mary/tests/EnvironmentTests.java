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
package de.dfki.lt.mary.tests;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.tritonus.share.sampled.Encodings;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;

import de.dfki.lt.mary.MaryXML;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class EnvironmentTests extends TestCase {

    public void testJavaVersion() {
        String version = System.getProperty("java.version");
        assertTrue(version.startsWith("1.4") || version.startsWith("1.5"));
    }

    public void testXMLParserSupportsNamespaces() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        Document document =
            docBuilder.parse(
                this.getClass().getResourceAsStream("test1.namespaces"));
        NodeList nl =
            document.getElementsByTagNameNS(
                "http://www.w3.org/2001/10/synthesis",
                "*");
        assertNotNull(nl.item(0));
        assertTrue(nl.item(0).getNodeName().equals("ssml:emphasis"));
    }

    public void testDocumentTraversalAvailable() {
        Document doc = MaryXML.newDocument();
        assertTrue(doc instanceof DocumentTraversal);
    }

    public void testMP3Available() throws Exception {
        AudioFormat mp3af = new AudioFormat(
                Encodings.getEncoding("MPEG1L3"),
                AudioSystem.NOT_SPECIFIED,
                AudioSystem.NOT_SPECIFIED,
                1,
                AudioSystem.NOT_SPECIFIED,
                AudioSystem.NOT_SPECIFIED,
                false);
        AudioInputStream waveStream = AudioSystem.getAudioInputStream(this.getClass().getResourceAsStream("test.wav"));
        // Now attempt conversion:
        assertTrue(AudioSystem.isConversionSupported(mp3af, waveStream.getFormat()));
        AudioInputStream mp3Stream = AudioSystem.getAudioInputStream(mp3af, waveStream);
    }
}
