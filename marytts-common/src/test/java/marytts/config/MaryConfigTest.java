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
package marytts.config;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import marytts.exceptions.MaryConfigurationException;

import org.junit.Test;

/**
 * @author marc
 *
 */
public class MaryConfigTest {

	@Test
	public void canCountConfigs() {
		// exercise
		int num = MaryConfig.countConfigs();
		// verify
		assertTrue(num >= 0);
	}

	@Test
	public void haveMainConfig() {
		MaryConfig m = MaryConfig.getMainConfig();
		assertNotNull(m);
	}

	/**
	 * @author Tristan
	 * @author ingmar
	 * 
	 *         Test to check properties file for trailing whitespace [SOLVED] -> created PropertiesTrimTrailingWhitespace class to
	 *         trim trailing whitespace
	 * @throws MaryConfigurationException
	 * @throws IOException
	 * 
	 */
	@Test
	public void ensureTrailingWhitespaceIsTrimmed() throws MaryConfigurationException, IOException {
		String testResourceName = "test.config";
		InputStream actualInput = this.getClass().getResourceAsStream(testResourceName);
		MaryConfig testConfig = new TestConfig(actualInput);
		Properties testProps = new Properties();
		InputStream expectedInput = this.getClass().getResourceAsStream(testResourceName);
		testProps.load(expectedInput);
		for (String key : testProps.stringPropertyNames()) {
			String expected = testProps.getProperty(key).trim();
			String actual = testConfig.getProperties().getProperty(key);
			assertEquals(expected, actual);
		}
	}

	@Test(expected = MaryConfigurationException.class)
	public void requireLocale1() throws MaryConfigurationException {
		new LanguageConfig(new ByteArrayInputStream(new byte[0]));
	}

	@Test(expected = MaryConfigurationException.class)
	public void requireLocale2() throws MaryConfigurationException, IOException {
		Properties p = new Properties();
		p.setProperty("a", "b");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		p.store(baos, "");
		// exercise:
		new LanguageConfig(new ByteArrayInputStream(baos.toByteArray()));
	}

	@Test
	public void testGetList() throws MaryConfigurationException, IOException {
		Properties p = new Properties();
		p.setProperty("locale", "en");
		p.setProperty("testlist", "a b c");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		p.store(baos, "");
		LanguageConfig lc = new LanguageConfig(new ByteArrayInputStream(baos.toByteArray()));
		// exercise:
		List<String> theList = lc.getList("testlist");
		// verify
		assertNotNull(theList);
		assertEquals(3, theList.size());
		Iterator<String> it = theList.iterator();
		assertEquals("a", it.next());
		assertEquals("b", it.next());
		assertEquals("c", it.next());
	}

	class TestConfig extends MaryConfig {

		protected TestConfig(InputStream propertyStream) throws MaryConfigurationException {
			super(propertyStream);
		}

	}
}
