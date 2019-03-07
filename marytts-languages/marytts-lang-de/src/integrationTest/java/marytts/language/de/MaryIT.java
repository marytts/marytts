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
package marytts.language.de;

import java.util.Set;

import marytts.phonetic.AlphabetFactory;
import marytts.phonetic.converter.Alphabet;

import marytts.language.de.JPhonemiser;
import marytts.modules.ModuleRegistry;

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
     ** JPhonemiser test
     *****************************************************************************/
    @Test
    public void testIsPosPunctuation() throws Exception {
        JPhonemiser phonemiser = (JPhonemiser) ModuleRegistry.getDefaultModule(JPhonemiser.class.getName());
	Assert.assertNotNull(phonemiser);

        Assert.assertTrue(phonemiser.isPosPunctuation("$,"));
        Assert.assertTrue(phonemiser.isPosPunctuation("$."));
        Assert.assertTrue(phonemiser.isPosPunctuation("$("));
        Assert.assertFalse(phonemiser.isPosPunctuation("NN"));
    }


    @Test
    public void testArpabetConversion() throws Exception {

        Alphabet sampa2ipa = AlphabetFactory.getAlphabet("sampa");
        Alphabet ipa2arpabet = AlphabetFactory.getAlphabet("arpabet");

        JPhonemiser phonemiser = (JPhonemiser) ModuleRegistry.getDefaultModule(JPhonemiser.class.getName());

        Set<String> phonemes = phonemiser.getAllophoneSet().getAllophoneNames();
        for (String ph: phonemes) {
            // Ignor epause
            if (ph.equals("_")) {
                continue;
            }
            String ipa = sampa2ipa.getCorrespondingIPA(ph);
            String arpabet = ipa2arpabet.getLabelFromIPA(ipa);
            logger.debug(ipa);
        }

    }
}
