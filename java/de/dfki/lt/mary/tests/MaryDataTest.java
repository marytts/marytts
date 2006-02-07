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
import java.io.StringReader;
import java.io.StringWriter;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.w3c.dom.Document;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;

public class MaryDataTest extends TestCase {
    String textString = "Hallöchen Welt!";
    String maryxmlString =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<maryxml version=\"0.4\"\n"
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "xmlns=\"http://mary.dfki.de/2002/MaryXML\" xml:lang=\"de\">\n"
            + "<s>\n"
            + "<t>\n"
            + "Willkommen\n"
            + "</t>\n"
            + "<t>\n"
            + "!\n"
            + "</t>\n"
            + "</s>\n"
            + "</maryxml>";

    public void testConstructor1() {
        try {
            MaryData md = new MaryData(null);
        } catch (NullPointerException e) {
            return;
        }
        fail();
    }

    public void testConstructor2() {
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"), false);
        Assert.assertTrue(md.getDocument() == null);
    }

    public void testConstructor3() {
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"), true);
        Assert.assertTrue(md.getDocument() != null);
    }

    public void testConstructor4() {
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"), true);
        Assert.assertTrue(md.getPlainText() == null);
    }

    public void testConstructor5() {
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"), true);
        Assert.assertTrue(md.getAudio() == null);
    }

    public void testConstructor6() {
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"), true);
        Assert.assertTrue(md.getData() instanceof Document);
    }

    public void testConstructor7() {
        MaryData md = new MaryData(MaryDataType.get("TEXT_DE"), true);
        Assert.assertTrue(md.getDocument() == null);
    }

    public void testConstructor8() {
        MaryData md = new MaryData(MaryDataType.get("AUDIO"), true);
        Assert.assertTrue(md.getDocument() == null);
    }

    public void testConstructor9() {
        MaryData md = new MaryData(MaryDataType.get("SSML"), true);
        Assert.assertTrue(md.getDocument() == null);
    }

    public void testTextRead1() throws Exception {
        MaryData md = new MaryData(MaryDataType.get("TEXT_DE"));
        md.readFrom(new StringReader(textString), null);
        Assert.assertTrue(md.getPlainText().trim().equals(textString.trim()));
    }

    public void testTextRead2() throws Exception {
        //MaryData md = new MaryData(MaryDataType.TEXT_DE);
        //md.readFrom(new StringInputStream(textString), null);
        //Assert.assertTrue(md.getPlainText().equals(textString));
    }

    public void testTextWrite() throws Exception {
        MaryData md = new MaryData(MaryDataType.get("TEXT_DE"));
        md.readFrom(new StringReader(textString), null);
        StringWriter sw = new StringWriter();
        md.writeTo(sw);
        Assert.assertTrue(sw.toString().trim().equals(textString.trim()));
    }

    public void testXMLRead1() throws Exception {
        MaryData md = new MaryData(MaryDataType.get("TOKENISED_DE"));
        md.readFrom(new StringReader(maryxmlString), null);
        Assert.assertTrue(md.getDocument() != null);
    }

    public void testXMLRead2() throws Exception {
        //MaryData md = new MaryData(MaryDataType.get("TOKENISED_DE"));
        //md.readFrom(new InputStream(maryxmlString), null);
        //Assert.assertTrue(md.getDocument() != null);
    }

    public void testXMLWrite() throws Exception {
        MaryData md = new MaryData(MaryDataType.get("TOKENISED_DE"));
        md.readFrom(new StringReader(maryxmlString), null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        md.writeTo(baos);
        Assert.assertTrue(!baos.toString().equals(""));
    }

}
