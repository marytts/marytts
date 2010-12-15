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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;


import static org.junit.Assert.*;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.modules.synthesis.Voice;
import marytts.server.Mary;
import marytts.server.MaryProperties;
import marytts.server.Request;


public class MaryTest {
    @BeforeClass
    public static void setUp() throws Exception {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.DEBUG);
        if (System.getProperty("mary.base") == null) {
            System.setProperty("mary.base", ".");
            Logger.getRootLogger().warn("System property 'mary.base' is not defined -- trying "+new File(".").getAbsolutePath()
                    +" -- if this fails, please start this using VM property \"-Dmary.base=/path/to/mary/runtime\"!");
        }

        if (Mary.currentState() == Mary.STATE_OFF)
            Mary.startup();
    }

    @Test
    public void testMaryRunning() {
        assertTrue(Mary.currentState() == Mary.STATE_RUNNING);
    }

    @Test
    public void testDefaultVoicesAvailable() throws Exception {
        assertTrue(Voice.getDefaultVoice(Locale.ENGLISH) != null);
    }
    
    

    @Test
    public void testModulesRequired1() {
        try {
            ModuleRegistry.modulesRequiredForProcessing(null, MaryDataType.AUDIO, Locale.ENGLISH);
        } catch (NullPointerException e) {
            return;
        }
        fail("should have thrown NullPointerException");
    }

    @Test
    public void testModulesRequired2() {
        try {
            ModuleRegistry.modulesRequiredForProcessing(MaryDataType.TEXT, null, Locale.ENGLISH);
        } catch (NullPointerException e) {
            return;
        }
        fail("should have thrown NullPointerException");
    }

    @Test
    public void testModulesRequired3() {
        List<MaryModule> mods = ModuleRegistry.modulesRequiredForProcessing(MaryDataType.TEXT, MaryDataType.AUDIO, null);
        assertNull(mods);
    }

    @Test
    public void testTextToSpeechPossibleEnglish() {
        List<MaryModule> modules =
            ModuleRegistry.modulesRequiredForProcessing(
                MaryDataType.TEXT,
                MaryDataType.AUDIO,
                Locale.US);
        assertTrue(modules != null && !modules.isEmpty());
    }

    @Test
    public void testValidMaryXML1() throws Exception {
        convertToAndValidate("test1.maryxml", MaryDataType.RAWMARYXML, MaryDataType.TOKENS, Locale.ENGLISH);
    }

    @Test
    public void testValidMaryXML2() throws Exception {
        convertToAndValidate("test1.maryxml", MaryDataType.RAWMARYXML, MaryDataType.INTONATION, Locale.US);
    }

    @Test
    public void testValidMaryXML3() throws Exception {
        convertToAndValidate("test1.maryxml", MaryDataType.RAWMARYXML, MaryDataType.ACOUSTPARAMS, Locale.US);
    }

    @Test
    public void testValidMaryXML4() throws Exception {
        convertToAndValidate("test1.ssml", MaryDataType.SSML, MaryDataType.TOKENS, Locale.ENGLISH);
    }

    @Test
    public void testValidMaryXML5() throws Exception {
        convertToAndValidate("test1.ssml", MaryDataType.SSML, MaryDataType.INTONATION, Locale.US);
    }

    @Test
    public void testValidMaryXML6() throws Exception {
        convertToAndValidate("test1.ssml", MaryDataType.SSML, MaryDataType.ACOUSTPARAMS, Locale.US);
    }
    
    

    @Test
    public void testDefaultUSVoiceAvailable() throws Exception {
        assertTrue(Voice.getDefaultVoice(Locale.US) != null);
    }


    @Test
    public void testTextToSpeechPossibleUS() {
        List<MaryModule> modules =
            ModuleRegistry.modulesRequiredForProcessing(
                MaryDataType.TEXT,
                MaryDataType.AUDIO,
                Locale.US);
        assertTrue(modules != null && !modules.isEmpty());
    }

    
    
    
    
    
    
    
    
    
    
    

    protected void convertToAndValidate(String resourceName, MaryDataType inputType, MaryDataType targetType, Locale locale)
    throws Exception {
        assertTrue(MaryProperties.getBoolean("maryxml.validate.input"));
        InputStream maryxml = this.getClass().getResourceAsStream(resourceName);
        assertTrue(maryxml != null);
        MaryData inputData = new MaryData(inputType, locale);
        inputData.readFrom(maryxml, null);
        Request r = new Request(inputType, targetType, locale, null, "", "", 1, null);
        r.setInputData(inputData);
        r.process();
        MaryData outputData = r.getOutputData();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        outputData.writeTo(baos);
        // And now a validating parse:
        MaryData testData = new MaryData(outputData.getType(), locale);        
        testData.readFrom(new ByteArrayInputStream(baos.toByteArray()), null);
    }

}

