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
package marytts.language.te;

import java.util.Locale;

import marytts.config.LanguageConfig;
import marytts.config.MaryConfigLoader;
import marytts.exceptions.MaryConfigurationException;

import org.testng.Assert;
import org.testng.annotations.*;

/**
 * @author marc
 *
 */
public class TeluguConfigTest {
    private static final Locale TELUGU = new Locale("te");

    @Test
    public void isNotMainConfig() throws MaryConfigurationException {
        MaryConfig m = new TeluguConfig();
        Assert.assertFalse(m.isMainConfig());
    }

    @Test
    public void canGet() {
        MaryConfig m = MaryConfig.getLanguageConfig(TELUGU);
        Assert.assertNotNull(m);
        Assert.assertTrue(((LanguageConfig) m).getLocales().contains(TELUGU));
    }

    @Test
    public void hasRussianLocale() throws MaryConfigurationException {
        LanguageConfig e = new TeluguConfig();
        Assert.assertTrue(e.getLocales().contains(TELUGU));
    }
}
