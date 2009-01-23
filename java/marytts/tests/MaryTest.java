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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import junit.framework.Assert;
import junit.framework.TestCase;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.modules.ModuleRegistry;
import marytts.modules.synthesis.Voice;
import marytts.server.Mary;
import marytts.server.MaryProperties;
import marytts.server.Request;

public class MaryTest extends TestCase {
    public void setUp() throws Exception {
        if (Mary.currentState() == Mary.STATE_OFF)
            Mary.startup();
    }

    public void testMaryRunning() {
        Assert.assertTrue(Mary.currentState() == Mary.STATE_RUNNING);
    }

    public void testDefaultVoicesAvailable() throws Exception {
        Assert.assertTrue(Voice.getDefaultVoice(Locale.ENGLISH) != null);
    }

    public void testModulesRequired1() {
        try {
            ModuleRegistry.modulesRequiredForProcessing(null, MaryDataType.AUDIO, Locale.ENGLISH);
        } catch (NullPointerException e) {
            return;
        }
        fail("should have thrown NullPointerException");
    }

    public void testModulesRequired2() {
        try {
            ModuleRegistry.modulesRequiredForProcessing(MaryDataType.TEXT, null, Locale.ENGLISH);
        } catch (NullPointerException e) {
            return;
        }
        fail("should have thrown NullPointerException");
    }

    public void testModulesRequired3() {
        try {
            ModuleRegistry.modulesRequiredForProcessing(MaryDataType.TEXT, MaryDataType.AUDIO, null);
        } catch (NullPointerException e) {
            return;
        }
        fail("should have thrown NullPointerException");
    }

    public void testTextToSpeechPossibleEnglish() {
        List modules =
            ModuleRegistry.modulesRequiredForProcessing(
                MaryDataType.TEXT,
                MaryDataType.AUDIO,
                Locale.US);
        Assert.assertTrue(modules != null && !modules.isEmpty());
    }

    public void testValidMaryXML1() throws Exception {
        convertToAndValidate("test1.maryxml", MaryDataType.get("RAWMARYXML"), MaryDataType.get("TOKENS_EN"), Locale.ENGLISH);
    }

    public void testValidMaryXML2() throws Exception {
        convertToAndValidate("test1.maryxml", MaryDataType.get("RAWMARYXML"), MaryDataType.get("INTONATION_EN"), Locale.US);
    }

    public void testValidMaryXML3() throws Exception {
        convertToAndValidate("test1.maryxml", MaryDataType.get("RAWMARYXML"), MaryDataType.get("ACOUSTPARAMS"), Locale.US);
    }

    public void testValidMaryXML4() throws Exception {
        convertToAndValidate("test1.ssml", MaryDataType.get("SSML"), MaryDataType.get("TOKENS_EN"), Locale.ENGLISH);
    }

    public void testValidMaryXML5() throws Exception {
        convertToAndValidate("test1.ssml", MaryDataType.get("SSML"), MaryDataType.get("INTONATION_EN"), Locale.US);
    }

    public void testValidMaryXML6() throws Exception {
        convertToAndValidate("test1.ssml", MaryDataType.get("SSML"), MaryDataType.get("ACOUSTPARAMS"), Locale.US);
    }

    protected void convertToAndValidate(String resourceName, MaryDataType inputType, MaryDataType targetType, Locale locale)
    throws Exception {
        Assert.assertTrue(MaryProperties.getBoolean("maryxml.validate.input"));
        InputStream maryxml = this.getClass().getResourceAsStream(resourceName);
        Assert.assertTrue(maryxml != null);
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

