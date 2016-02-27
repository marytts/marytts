/**
 * Copyright 2009 DFKI GmbH.
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

package marytts.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.sql.SQLException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;

/**
 * @author marc
 * 
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MaryCacheTest {
	@ClassRule
	public static TemporaryFolder tmp = new TemporaryFolder();

	private static MaryCache c;
	private static File maryCacheFile;
	private static String inputtype = "TEXT";
	private static String outputtype = "RAWMARYXML";
	private static String locale = "de";
	private static String voice = "de1";
	private static String inputtext = "Welcome to the world of speech synthesis";
	private static String targetValue = "<rawmaryxml/>";
	private static byte[] targetAudio = new byte[12345];
	private static String inputtext2 = "Some other input text";
	private static String targetValue2 = "Two\nlines";

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUp() throws Exception {
		maryCacheFile = tmp.newFile();
		c = new MaryCache(maryCacheFile, true);
		c.insertText(inputtype, outputtype, locale, voice, inputtext, targetValue);
		c.insertAudio(inputtype, locale, voice, inputtext, targetAudio);
		c.insertText(inputtype, outputtype, locale, voice, inputtext2, targetValue2);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDown() throws Exception {
		c.shutdown();
	}

	@Test
	public void lookupText() throws Exception {
		String lookupValue = c.lookupText(inputtype, outputtype, locale, voice, inputtext);
		assertEquals(targetValue, lookupValue);
	}

	@Test
	public void lookupText2() throws Exception {
		String lookupValue = c.lookupText(inputtype, outputtype, locale, voice, inputtext2);
		assertEquals(targetValue2, lookupValue);
	}

	@Test
	public void lookupAudio() throws Exception {
		byte[] lookupAudio = c.lookupAudio(inputtype, locale, voice, inputtext);
		assertNotNull(lookupAudio);
		assertArrayEquals(targetAudio, lookupAudio);
	}

	@Test
	public void canInsertAgain() throws Exception {
		int numExceptions = 0;
		try {
			c.insertText(inputtype, outputtype, locale, voice, inputtext, targetValue);
		} catch (SQLException e) {
			numExceptions++;
		}
		try {
			c.insertAudio(inputtype, locale, voice, inputtext, targetAudio);
		} catch (SQLException e) {
			numExceptions++;
		}
		assertEquals(0, numExceptions);
	}

	@Test
	public void isPersistent() throws Exception {
		c.shutdown();
		c = new MaryCache(maryCacheFile, false);
		lookupText();
		lookupAudio();
	}

	@Test
	public void zzz_isClearable() throws Exception {
		c.shutdown();
		c = new MaryCache(maryCacheFile, true);
		String lookupValue = c.lookupText(inputtype, outputtype, locale, voice, inputtext);
		assertNull(lookupValue);
		byte[] lookupAudio = c.lookupAudio(inputtype, locale, voice, inputtext);
		assertNull(lookupAudio);
	}

}
