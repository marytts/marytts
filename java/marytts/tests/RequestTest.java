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
import java.io.InputStream;
import java.io.StringReader;
import java.util.Locale;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.modules.synthesis.Voice;
import marytts.server.Mary;
import marytts.server.Request;
import marytts.util.dom.DomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


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
        Request r = new Request(MaryDataType.get("TEXT_DE"), MaryDataType.get("ACOUSTPARAMS"), Locale.GERMAN, null, "", "", 1, null);
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"), null);
        try {
            r.setInputData(md);
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("should have thrown an IllegalArgumentException");
    }

    public void testProcess1() throws Exception {
        Request r = new Request(MaryDataType.get("TEXT_EN"), MaryDataType.get("ACOUSTPARAMS"), Locale.US, null, "", "", 1, null);
        try {
            r.process();
        } catch (NullPointerException e) {
            return;
        }
        fail("should have thrown a NullPointerException");
    }

    public void testProcess2() throws Exception {
        Request r = new Request(MaryDataType.get("RAWMARYXML"), MaryDataType.get("ACOUSTPARAMS"), null, null, "", "", 1, null);
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"), null, true);
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
        Request r = new Request(MaryDataType.get("INTONATION"), MaryDataType.get("TOKENS"), Locale.US, null, "", "", 1, null);
        MaryData md = new MaryData(MaryDataType.get("INTONATION"), Locale.US);
        md.readFrom(new StringReader(MaryDataType.get("INTONATION").exampleText(Locale.US)), null);
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
        Request r = new Request(MaryDataType.get("TEXT_EN"), MaryDataType.get("AUDIO"), Locale.US, 
            voice, "", "", 1, aff);
        MaryData md = new MaryData(MaryDataType.get("TEXT_EN"), Locale.US);
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
        MaryData inputData = new MaryData(rawmaryxml, null);
        inputData.readFrom(maryxml, null);
        Request r = new Request(rawmaryxml, rawmaryxml, null, null, "", "", 1, null);
        r.setInputData(inputData);
        r.process();
        MaryData processedOut = r.getOutputData();
        MaryData targetOut = new MaryData(rawmaryxml, null);
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
        Request r = new Request(MaryDataType.get("RAWMARYXML"), MaryDataType.get("RAWMARYXML"), null, null, "", "", 1, null);
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML"), null);
        md.readFrom(new StringReader(MaryDataType.get("RAWMARYXML").exampleText(Locale.US)), null);
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
        Request r = new Request(MaryDataType.get("TEXT"), MaryDataType.get("AUDIO"), Locale.US,
            voice, "", "", 1, aff);
        MaryData md = new MaryData(MaryDataType.get("TEXT"), Locale.US);
        md.setPlainText("This is a test.");
        r.setInputData(md);
        r.process();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        r.writeOutputData(baos);
        assertNotNull(baos.toByteArray());
    }

}

