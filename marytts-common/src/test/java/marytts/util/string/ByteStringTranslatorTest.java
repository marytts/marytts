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

package marytts.util.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author marc
 *
 */
public class ByteStringTranslatorTest {
	private static String[] list10;
	private static ByteStringTranslator t10;
	private static String[] listMax;
	private static ByteStringTranslator tMax;

	@BeforeClass
	public static void prepareData() {
		list10 = new String[10];
		for (int i = 0; i < 10; i++) {
			list10[i] = "a" + i;
		}
		t10 = new ByteStringTranslator(list10);

		listMax = new String[ByteStringTranslator.MAXNUM];
		for (int i = 0; i < ByteStringTranslator.MAXNUM; i++) {
			listMax[i] = "b" + i;
		}
		tMax = new ByteStringTranslator(listMax);

	}

	@Test
	public void smallListLength() {
		assertEquals(t10.getNumberOfValues(), list10.length);
	}

	@Test
	public void smallListContainsByte() {
		assertTrue(t10.contains((byte) 5));
	}

	@Test
	public void smallListDoesntContainByte() {
		assertFalse(t10.contains((byte) 20));
	}

	@Test
	public void smallListContainsString() {
		assertTrue(t10.contains("a5"));
	}

	@Test
	public void smallListDoesntContainString() {
		assertFalse(t10.contains("abc"));
	}

	@Test
	public void smallListCompareString() {
		assertEquals(t10.get((byte) 5), "a" + 5);
	}

	@Test
	public void maxListLength() {
		assertEquals(tMax.getNumberOfValues(), listMax.length);
	}

	@Test
	public void maxListContainsByte() {
		assertTrue(tMax.contains((byte) (ByteStringTranslator.MAXNUM - 1)));
	}

	@Test
	public void maxListContainsString() {
		assertTrue(tMax.contains("b" + (ByteStringTranslator.MAXNUM - 1)));
	}

	@Test
	public void maxListDoesntContainString() {
		assertFalse(tMax.contains("abc"));
	}

	@Test
	public void maxListCompareString() {
		assertEquals(tMax.get((byte) (ByteStringTranslator.MAXNUM - 1)), "b" + (ByteStringTranslator.MAXNUM - 1));
	}

	@Test
	public void maxListCompareString2() {
		assertEquals(tMax.get("b" + (ByteStringTranslator.MAXNUM - 1)), (byte) (ByteStringTranslator.MAXNUM - 1));
	}

	@Test
	public void canCreateMaxList() {
		ByteStringTranslator t = new ByteStringTranslator(ByteStringTranslator.MAXNUM);
		// (test is that this doesn't throw an exception)
	}

	@Test
	public void tooLargeList() {
		String[] list300 = new String[300];
		for (int i = 0; i < 300; i++) {
			list300[i] = "d" + i;
		}
		try {
			ByteStringTranslator t = new ByteStringTranslator(list300);
			fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}
	}
}
