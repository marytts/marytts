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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Locale;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.dfki.lt.mary.Mary;
import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.Request;
import de.dfki.lt.mary.emospeak.EmoTransformer;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.util.dom.DomUtils;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class EmospeakTest extends TestCase {

    public void testTransform() throws Exception 
    {
        TransformerFactory tFactory = javax.xml.transform.TransformerFactory.newInstance();
        System.err.println("Using XSL processor " + tFactory.getClass().getName());
        javax.xml.transform.stream.StreamSource stylesheetStream =
            new javax.xml.transform.stream.StreamSource (
                EmoTransformer.class.getResourceAsStream("emotion-to-mary.xsl")
            );
        Templates stylesheet = tFactory.newTemplates( stylesheetStream );
        DocumentBuilderFactory dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
        Transformer transformer = stylesheet.newTransformer();

        Document emotion = docBuilder.parse(EmospeakTest.class.getResourceAsStream("emotion.xml"));
        javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource (emotion);
        java.io.StringWriter sw = new java.io.StringWriter();
        javax.xml.transform.stream.StreamResult streamResult = new javax.xml.transform.stream.StreamResult (sw);
        transformer.transform(domSource, streamResult);
        String maryxmlString = sw.toString();
        System.out.println("Converted to maryxml: "+maryxmlString);
    }
    

}
