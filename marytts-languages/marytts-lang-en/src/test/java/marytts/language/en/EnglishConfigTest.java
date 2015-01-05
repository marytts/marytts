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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import marytts.config.LanguageConfig;
import marytts.config.MaryConfig;
import marytts.exceptions.MaryConfigurationException;

import org.junit.Test;

/**
 * @author marc
 *
 */
public class EnglishConfigTest {

	@Test
	public void isNotMainConfig() throws MaryConfigurationException {
		MaryConfig m = new EnglishConfig();
		assertFalse(m.isMainConfig());
	}

	@Test
	public void haveLanguageConfig() {
		assertTrue(MaryConfig.countLanguageConfigs() > 0);
	}

	@Test
	public void haveLanguageConfig2() {
		Iterable<LanguageConfig> lcs = MaryConfig.getLanguageConfigs();
		assertNotNull(lcs);
		assertTrue(lcs.iterator().hasNext());
	}

	@Test
	public void canGet() {
		MaryConfig m = MaryConfig.getLanguageConfig(Locale.US);
		assertNotNull(m);
		assertTrue(((LanguageConfig) m).getLocales().contains(Locale.US));
	}

	@Test
	public void hasEnglishLocale() throws MaryConfigurationException {
		LanguageConfig e = new EnglishConfig();
		assertTrue(e.getLocales().contains(Locale.US));
	}

	@Test
	public void hasAllophoneSet() throws MaryConfigurationException {
		LanguageConfig e = new EnglishConfig();
		assertNotNull(e.getAllophoneSet(Locale.US));
		assertNotNull(e.getAllophoneSet(Locale.UK));
	}

}
