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
package marytts.tests;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;

import junit.framework.Assert;
import junit.framework.TestCase;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;

import org.w3c.dom.Document;


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
            MaryData md = new MaryData(null, Locale.GERMAN);
        } catch (NullPointerException e) {
            return;
        }
        fail();
    }

    public void testConstructor2() {
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"), null, false);
        Assert.assertTrue(md.getDocument() == null);
    }

    public void testConstructor3() {
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"), null, true);
        Assert.assertTrue(md.getDocument() != null);
    }

    public void testConstructor4() {
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"), null, true);
        Assert.assertTrue(md.getPlainText() == null);
    }

    public void testConstructor5() {
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"), null, true);
        Assert.assertTrue(md.getAudio() == null);
    }

    public void testConstructor6() {
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"), null, true);
        Assert.assertTrue(md.getData() instanceof Document);
    }

    public void testConstructor7() {
        MaryData md = new MaryData(MaryDataType.get("TEXT_DE"), Locale.GERMAN, true);
        Assert.assertTrue(md.getDocument() == null);
    }

    public void testConstructor8() {
        MaryData md = new MaryData(MaryDataType.get("AUDIO"), null, true);
        Assert.assertTrue(md.getDocument() == null);
    }

    public void testConstructor9() {
        MaryData md = new MaryData(MaryDataType.get("SSML"), null, true);
        Assert.assertTrue(md.getDocument() == null);
    }

    public void testTextRead1() throws Exception {
        MaryData md = new MaryData(MaryDataType.get("TEXT_DE"), Locale.GERMAN);
        md.readFrom(new StringReader(textString), null);
        Assert.assertTrue(md.getPlainText().trim().equals(textString.trim()));
    }

    public void testTextRead2() throws Exception {
        //MaryData md = new MaryData(MaryDataType.TEXT_DE);
        //md.readFrom(new StringInputStream(textString), null);
        //Assert.assertTrue(md.getPlainText().equals(textString));
    }

    public void testTextWrite() throws Exception {
        MaryData md = new MaryData(MaryDataType.get("TEXT_DE"), Locale.GERMAN);
        md.readFrom(new StringReader(textString), null);
        StringWriter sw = new StringWriter();
        md.writeTo(sw);
        Assert.assertTrue(sw.toString().trim().equals(textString.trim()));
    }

    public void testXMLRead1() throws Exception {
        MaryData md = new MaryData(MaryDataType.get("TOKENISED_DE"), Locale.GERMAN);
        md.readFrom(new StringReader(maryxmlString), null);
        Assert.assertTrue(md.getDocument() != null);
    }

    public void testXMLRead2() throws Exception {
        //MaryData md = new MaryData(MaryDataType.get("TOKENISED_DE"));
        //md.readFrom(new InputStream(maryxmlString), null);
        //Assert.assertTrue(md.getDocument() != null);
    }

    public void testXMLWrite() throws Exception {
        MaryData md = new MaryData(MaryDataType.get("TOKENISED_DE"), Locale.GERMAN);
        md.readFrom(new StringReader(maryxmlString), null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        md.writeTo(baos);
        Assert.assertTrue(!baos.toString().equals(""));
    }

}

