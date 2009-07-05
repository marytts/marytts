/**
 * Copyright (C) 2002 DFKI GmbH. All rights reserved.
 */

package marytts.tests;

import java.util.List;
import java.util.Locale;

import junit.framework.Assert;
import marytts.datatypes.MaryDataType;
import marytts.modules.ModuleRegistry;
import marytts.modules.synthesis.Voice;
import marytts.server.Mary;
import marytts.tests.MaryTest;

public class GermanMaryTest extends MaryTest {
    public void setUp() throws Exception {
        if (Mary.currentState() == Mary.STATE_OFF)
            Mary.startup();
    }

    public void testDefaultGermanVoiceAvailable() throws Exception {
        Assert.assertTrue(Voice.getDefaultVoice(Locale.GERMAN) != null);
    }

    public void testModulesRequired1() {
        try {
            ModuleRegistry.modulesRequiredForProcessing(null, MaryDataType.AUDIO, Locale.GERMAN);
        } catch (NullPointerException e) {
            return;
        }
        fail("should have thrown NullPointerException");
    }

    public void testModulesRequired2() {
        try {
            ModuleRegistry.modulesRequiredForProcessing(MaryDataType.TEXT, null, Locale.GERMAN);
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

    public void testTextToSpeechPossibleGerman() {
        List modules =
            ModuleRegistry.modulesRequiredForProcessing(
                MaryDataType.TEXT,
                MaryDataType.AUDIO,
                Locale.GERMAN);
        Assert.assertTrue(modules != null && !modules.isEmpty());
    }



}
