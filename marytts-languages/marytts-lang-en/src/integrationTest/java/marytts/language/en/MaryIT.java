/**
 * Copyright 2011 DFKI GmbH.
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
package marytts.language.en;

// Module part
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.modules.nlp.JPhonemiser;

// Configuration part
import marytts.config.MaryConfigurationFactory;

// Locale
import java.util.Locale;

import org.testng.Assert;
import org.testng.annotations.*;

/**
 * Some more coverage tests with actual language modules
 *
 * @author marc
 *
 */
public class MaryIT extends marytts.MaryIT {



    /*****************************************************************************
     ** Preprocess
     *****************************************************************************/
    @Test
    public void testParensAndNumber() throws Exception {
        MaryModule module = ModuleRegistry.getDefaultModule(Preprocess.class.getName());
        assert processAndCompare("parens-and-number.tokenised", "parens-and-number.words", Locale.US.toString(), module);
    }

    @Test
    public void testBy() throws Exception {
        MaryModule module = ModuleRegistry.getDefaultModule(Preprocess.class.getName());
        assert processAndCompare("by.tokenised","by.words", Locale.US.toString(), module);
    }


    /*****************************************************************************
     ** USJPhonemiser test
     *****************************************************************************/
    @Test
    public void testIsPosPunctuationUS() throws Exception {
        JPhonemiser phonemiser = (JPhonemiser) ModuleRegistry.getDefaultModule(USJPhonemiser.class.getName());
	Assert.assertNotNull(phonemiser);
	MaryConfigurationFactory.getConfiguration("en_US").applyConfiguration(phonemiser);

        Assert.assertTrue(phonemiser.isPosPunctuation("."));
        Assert.assertTrue(phonemiser.isPosPunctuation(","));
        Assert.assertTrue(phonemiser.isPosPunctuation(":"));
        Assert.assertFalse(phonemiser.isPosPunctuation("NN"));
    }

    @Test
    public void testMaybePronounceableUS() throws Exception {
        JPhonemiser phonemiser = (JPhonemiser) ModuleRegistry.getDefaultModule(USJPhonemiser.class.getName());
	Assert.assertNotNull(phonemiser);
	MaryConfigurationFactory.getConfiguration("en_US").applyConfiguration(phonemiser);

        Assert.assertFalse(phonemiser.maybePronounceable(null, "NN"));
        Assert.assertFalse(phonemiser.maybePronounceable(null, "."));
        Assert.assertFalse(phonemiser.maybePronounceable("", "NN"));
        Assert.assertFalse(phonemiser.maybePronounceable("", "."));
        Assert.assertTrue(phonemiser.maybePronounceable("foo", "NN"));
        Assert.assertTrue(phonemiser.maybePronounceable("foo", "."));
        Assert.assertTrue(phonemiser.maybePronounceable("@", "NN"));
        Assert.assertFalse(phonemiser.maybePronounceable("@", "."));
    }



    /*****************************************************************************
     ** GBJPhonemiser test
     *****************************************************************************/
    @Test
    public void testIsPosPunctuationGB() throws Exception {
        JPhonemiser phonemiser = (JPhonemiser) ModuleRegistry.getDefaultModule(GBJPhonemiser.class.getName());
	Assert.assertNotNull(phonemiser);
	MaryConfigurationFactory.getConfiguration("en_GB").applyConfiguration(phonemiser);

        Assert.assertTrue(phonemiser.isPosPunctuation("."));
        Assert.assertTrue(phonemiser.isPosPunctuation(","));
        Assert.assertTrue(phonemiser.isPosPunctuation(":"));
        Assert.assertFalse(phonemiser.isPosPunctuation("NN"));
    }

    @Test
    public void testMaybePronounceableGB() throws Exception {
        JPhonemiser phonemiser = (JPhonemiser) ModuleRegistry.getDefaultModule(GBJPhonemiser.class.getName());
	Assert.assertNotNull(phonemiser);
	MaryConfigurationFactory.getConfiguration("en_GB").applyConfiguration(phonemiser);

        Assert.assertFalse(phonemiser.maybePronounceable(null, "NN"));
        Assert.assertFalse(phonemiser.maybePronounceable(null, "."));
        Assert.assertFalse(phonemiser.maybePronounceable("", "NN"));
        Assert.assertFalse(phonemiser.maybePronounceable("", "."));
        Assert.assertTrue(phonemiser.maybePronounceable("foo", "NN"));
        Assert.assertTrue(phonemiser.maybePronounceable("foo", "."));
        Assert.assertTrue(phonemiser.maybePronounceable("@", "NN"));
        Assert.assertFalse(phonemiser.maybePronounceable("@", "."));
    }

    @Test
    public void testLTS() throws Exception {
        JPhonemiser phonemiser = (JPhonemiser) ModuleRegistry.getDefaultModule(GBJPhonemiser.class.getName());
	Assert.assertNotNull(phonemiser);
	MaryConfigurationFactory.getConfiguration("en_GB").applyConfiguration(phonemiser);

	StringBuilder sb = new StringBuilder();
	Assert.assertNotNull(phonemiser.phonemise("webmail", "NN", sb));
    }
}
