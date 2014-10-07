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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	 * Test to check properties file for trailing whitespace [SOLVED]
	 * -> created PropertiesTrimTrailingWhitespace class to trim trailing whitespace 
	 * @throws MaryConfigurationException 
	 * 
	 */
	@Test
	public void checkTrailingWhitespace() throws InstantiationException, IllegalAccessException, MaryConfigurationException {
		//should fail when Properties class is used in MaryConfig
		Pattern p = Pattern.compile("\\s+$");
		Matcher mat;
		InputStream is = this.getClass().getResourceAsStream("/marytts/tests/junit4/testConfig.config");
		Properties props = new Properties();
		MaryConfig m = new testMainConfig(is);
		
		props = m.getProperties();
		for(String key : props.stringPropertyNames()){
			
			if(props.getProperty(key).isEmpty()){
				//System.out.printf("'%s' --> '%s'%n", key, props.getProperty(key));
				continue;
			}
			if(props.getProperty(key).trim().isEmpty()){
				//System.out.println(key);
				//assertFalse(props.getProperty(key).trim().equals(""));
				fail(key + " has no value...check for trailing whitespace in config file");
			}
			mat = p.matcher(props.getProperty(key));
			if(mat.find()){
				fail(key + "'s value has trailing whitespace, check config file");
			}
			//System.out.printf("'%s' --> '%s'%n", key, props.getProperty(key));	
		}	
	}
	
	@Test
	public void testConfigExists() {
	   assertNotNull("Test file missing", this.getClass().getResource("/marytts/tests/junit4/testConfig.config"));
	   }
	
	/**
	 * @author Tristan
	 * Nested class to allow for instance of MaryConfig
	 * 
	 */
	public static class testMainConfig extends MaryConfig{

		protected testMainConfig(InputStream propertyStream)
				throws MaryConfigurationException {
			super(propertyStream);
		
		}
	}
	
	@Test(expected=MaryConfigurationException.class)
	public void requireLocale1() throws MaryConfigurationException {
		new LanguageConfig(new ByteArrayInputStream(new byte[0]));
	}
	
	@Test(expected=MaryConfigurationException.class)
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
}
