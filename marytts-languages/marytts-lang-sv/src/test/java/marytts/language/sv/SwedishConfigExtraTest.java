/**
 * Copyright 2011 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * <p>
 * This file is part of MARY TTS.
 * <p>
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package marytts.language.sv;

import java.util.Locale;

import marytts.config.LanguageConfig;
import marytts.config.MaryConfig;
import marytts.exceptions.MaryConfigurationException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author marc
 */
public class SwedishConfigExtraTest {

    private static final Locale SWEDISH = new Locale("sv");

    @Test
    public void canGet() {
        MaryConfig m = MaryConfig.getLanguageConfig(SWEDISH);
        Assert.assertNotNull(m);
        Assert.assertTrue(((LanguageConfig) m).getLocales().contains(SWEDISH));
    }

    @Test
    public void hasSwedishLocale() throws MaryConfigurationException {
        LanguageConfig e = new SwedishConfig();
        Assert.assertTrue(e.getLocales().contains(SWEDISH));
    }
}
