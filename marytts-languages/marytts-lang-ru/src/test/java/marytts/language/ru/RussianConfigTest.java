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
package marytts.language.ru;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import marytts.config.LanguageConfig;
import marytts.config.MaryConfig;
import marytts.exceptions.MaryConfigurationException;

import org.junit.Test;

/**
 * @author marc
 *
 */
public class RussianConfigTest {
	private static final Locale RUSSIAN = new Locale("ru");

	@Test
	public void isNotMainConfig() throws MaryConfigurationException {
		MaryConfig m = new RussianConfig();
		assertFalse(m.isMainConfig());
	}

	@Test
	public void canGet() {
		MaryConfig m = MaryConfig.getLanguageConfig(RUSSIAN);
		assertNotNull(m);
		assertTrue(((LanguageConfig) m).getLocales().contains(RUSSIAN));
	}

	@Test
	public void hasRussianLocale() throws MaryConfigurationException {
		LanguageConfig e = new RussianConfig();
		assertTrue(e.getLocales().contains(RUSSIAN));
	}
}
