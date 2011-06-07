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
package marytts.tests.junit4;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Locale;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.modules.synthesis.Voice;
import marytts.server.Mary;
import marytts.server.Request;
import marytts.util.MaryUtils;
import marytts.util.dom.DomUtils;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class RequestIT {
    @BeforeClass
    public static void setUp() throws Exception {
        if (!MaryUtils.isLog4jConfigured()) {
            BasicConfigurator.configure();
            Logger.getRootLogger().setLevel(Level.DEBUG);
        }
        if (System.getProperty("mary.base") == null) {
            System.setProperty("mary.base", ".");
            Logger.getRootLogger().warn("System property 'mary.base' is not defined -- trying "+new File(".").getAbsolutePath()
                    +" -- if this fails, please start this using VM property \"-Dmary.base=/path/to/mary/runtime\"!");
        }

        if (Mary.currentState() == Mary.STATE_OFF)
            Mary.startup();
    }

    @Test
    public void testSetInputData() {
        Request r = new Request(MaryDataType.TEXT, MaryDataType.ACOUSTPARAMS, Locale.GERMAN, null, "", "", 1, null);
        MaryData md = new MaryData(MaryDataType.RAWMARYXML, null);
        try {
            r.setInputData(md);
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("should have thrown an IllegalArgumentException");
    }

    @Test
    public void testProcess1() throws Exception {
        Request r = new Request(MaryDataType.TEXT, MaryDataType.ACOUSTPARAMS, Locale.US, null, "", "", 1, null);
        try {
            r.process();
        } catch (NullPointerException e) {
            return;
        }
        fail("should have thrown a NullPointerException");
    }

    @Test
    public void testProcess2() throws Exception {
        Request r = new Request(MaryDataType.RAWMARYXML, MaryDataType.ACOUSTPARAMS, null, null, "", "", 1, null);
        MaryData md = new MaryData(MaryDataType.RAWMARYXML, null, true);
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

    @Test
    public void testProcess3() throws Exception {
        // ask for an impossible conversion:
        Request r = new Request(MaryDataType.INTONATION, MaryDataType.TOKENS, Locale.US, null, "", "", 1, null);
        MaryData md = new MaryData(MaryDataType.INTONATION, Locale.US);
        md.setData(MaryDataType.INTONATION.exampleText(Locale.US));
        r.setInputData(md);
        try {
            r.process();
        } catch (UnsupportedOperationException e) {
            return;
        }
        fail("should have thrown an UnsupportedOperationException");
    }

    @Test
    public void testProcess4() throws Exception {
        Voice voice = Voice.getDefaultVoice(Locale.ENGLISH);
        AudioFormat af = voice.dbAudioFormat();
        AudioFileFormat aff = new AudioFileFormat(AudioFileFormat.Type.WAVE,
            af, AudioSystem.NOT_SPECIFIED);
        Request r = new Request(MaryDataType.TEXT, MaryDataType.AUDIO, Locale.US, 
            voice, "", "", 1, aff);
        MaryData md = new MaryData(MaryDataType.TEXT, Locale.US);
        md.setPlainText("This is a test.");
        r.setInputData(md);
        r.process();
        assertNotNull(r.getOutputData());
    }

    @Test
    public void testProcess5() throws Exception {
        // Convert from RAWMARYXML to RAWMARYXML, and see if we lose nodes:
        InputStream maryxml = this.getClass().getResourceAsStream("test2.maryxml");
        assertTrue(maryxml != null);
        MaryDataType rawmaryxml = MaryDataType.RAWMARYXML;
        MaryData inputData = new MaryData(rawmaryxml, null);
        inputData.readFrom(maryxml, null);
        System.out.println("Input document namespace: "+inputData.getDocument().getNamespaceURI());
        Request r = new Request(rawmaryxml, rawmaryxml, null, null, "", "", 1, null);
        r.setInputData(inputData);
        r.process();
        MaryData processedOut = r.getOutputData();
        MaryData targetOut = new MaryData(rawmaryxml, null);
        targetOut.readFrom(this.getClass().getResourceAsStream("test2_result.maryxml"), null);
        try {
            DomUtils.compareNodes(targetOut.getDocument(), processedOut.getDocument(), true);
        } catch (Exception afe) {
            StringBuilder msg = new StringBuilder();
            msg.append("XML documents are not equal\n");
            msg.append("==========target:=============\n");
            Document target = (Document) targetOut.getDocument().cloneNode(true);
            DomUtils.trimAllTextNodes(target);
            msg.append(DomUtils.document2String(target)).append("\n\n");
            msg.append("==========processed:============\n");
            Document processed = (Document) processedOut.getDocument().cloneNode(true);
            DomUtils.trimAllTextNodes(processed);
            msg.append(DomUtils.document2String(processed)).append("\n");
            throw new Exception(msg.toString(), afe);
        }

    }
    
    public void testWriteOutputData1() throws Exception {
        Request r = new Request(MaryDataType.RAWMARYXML, MaryDataType.RAWMARYXML, null, null, "", "", 1, null);
        MaryData md = new MaryData(MaryDataType.RAWMARYXML, null);
        md.setData(MaryDataType.RAWMARYXML.exampleText(Locale.US));
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

