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

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.dfki.lt.mary.Mary;
import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.Request;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.util.dom.DomUtils;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class RequestTest extends TestCase {
    public void setUp() throws Exception {
        if (Mary.currentState() == Mary.STATE_OFF)
            Mary.startup();
    }

    public void testSetInputData() {
        Request r = new Request(MaryDataType.get("TEXT_DE"), MaryDataType.get("ACOUSTPARAMS"), null, "", "", 1, null);
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"));
        try {
            r.setInputData(md);
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("should have thrown an IllegalArgumentException");
    }

    public void testProcess1() throws Exception {
        Request r = new Request(MaryDataType.get("TEXT_EN"), MaryDataType.get("ACOUSTPARAMS"), null, "", "", 1, null);
        try {
            r.process();
        } catch (NullPointerException e) {
            return;
        }
        fail("should have thrown a NullPointerException");
    }

    public void testProcess2() throws Exception {
        Request r = new Request(MaryDataType.get("RAWMARYXML"), MaryDataType.get("ACOUSTPARAMS"), null, "", "", 1, null);
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"), true);
        Document d = md.getDocument();
        Element elt = d.getDocumentElement();
        if (elt.hasAttribute("xml:lang"))
            elt.removeAttribute("xml:lang");
        r.setInputData(md);
        try {
            r.process();
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("should have thrown an IllegalArgumentException");
    }

    public void testProcess3() throws Exception {
        // ask for an impossible conversion:
        Request r = new Request(MaryDataType.get("INTONATION_EN"), MaryDataType.get("TOKENS_EN"), null, "", "", 1, null);
        MaryData md = new MaryData(MaryDataType.get("INTONATION_EN"));
        md.readFrom(new StringReader(MaryDataType.get("INTONATION_EN").exampleText()), null);
        r.setInputData(md);
        try {
            r.process();
        } catch (UnsupportedOperationException e) {
            return;
        }
        fail("should have thrown an UnsupportedOperationException");
    }
        
    public void testProcess4() throws Exception {
        Voice voice = Voice.getDefaultVoice(Locale.ENGLISH);
        AudioFormat af = voice.dbAudioFormat();
        AudioFileFormat aff = new AudioFileFormat(AudioFileFormat.Type.WAVE,
            af, AudioSystem.NOT_SPECIFIED);
        Request r = new Request(MaryDataType.get("TEXT_EN"), MaryDataType.get("AUDIO"),
            voice, "", "", 1, aff);
        MaryData md = new MaryData(MaryDataType.get("TEXT_EN"));
        md.setPlainText("This is a test.");
        r.setInputData(md);
        r.process();
        assertNotNull(r.getOutputData());
    }

    public void testProcess5() throws Exception {
        // Convert from RAWMARYXML to RAWMARYXML, and see if we lose nodes:
        InputStream maryxml = this.getClass().getResourceAsStream("test2.maryxml");
        Assert.assertTrue(maryxml != null);
        MaryDataType rawmaryxml = MaryDataType.get("RAWMARYXML");
        MaryData inputData = new MaryData(rawmaryxml);
        inputData.readFrom(maryxml, null);
        Request r = new Request(rawmaryxml, rawmaryxml, null, "", "", 1, null);
        r.setInputData(inputData);
        r.process();
        MaryData processedOut = r.getOutputData();
        MaryData targetOut = new MaryData(rawmaryxml);
        targetOut.readFrom(this.getClass().getResourceAsStream("test2_result.maryxml"), null);
        try {
            assertTrue(DomUtils.areEqual(targetOut.getDocument(), processedOut.getDocument()));
        } catch (AssertionFailedError afe) {
            System.err.println("==========target:=============");
            System.err.println(DomUtils.serializeToString(targetOut.getDocument()));
            System.err.println();
            System.err.println("==========processed:============");
            System.err.println(DomUtils.serializeToString(processedOut.getDocument()));
            throw afe;
        }

    }
    
    public void testWriteOutputData1() throws Exception {
        Request r = new Request(MaryDataType.get("RAWMARYXML"), MaryDataType.get("RAWMARYXML"), null, "", "", 1, null);
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"));
        md.readFrom(new StringReader(MaryDataType.get("RAWMARYXML_EN").exampleText()), null);
        r.setInputData(md);
        r.process();
        try {
            r.writeOutputData(null);
        } catch(NullPointerException e) {
            return;
        }
        fail("should have thrown NullPointerException");
    }
        
    public void testWriteOutputData2() throws Exception {
        Voice voice = Voice.getDefaultVoice(Locale.ENGLISH);
        AudioFormat af = voice.dbAudioFormat();
        AudioFileFormat aff = new AudioFileFormat(AudioFileFormat.Type.WAVE,
            af, AudioSystem.NOT_SPECIFIED);
        Request r = new Request(MaryDataType.get("TEXT_EN"), MaryDataType.get("AUDIO"),
            voice, "", "", 1, aff);
        MaryData md = new MaryData(MaryDataType.get("TEXT_EN"));
        md.setPlainText("This is a test.");
        r.setInputData(md);
        r.process();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        r.writeOutputData(baos);
        assertNotNull(baos.toByteArray());
    }

}
