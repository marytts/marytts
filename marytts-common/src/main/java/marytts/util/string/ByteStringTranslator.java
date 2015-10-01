/**
 * Copyright 2000-2009 DFKI GmbH.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A helper class converting between a given set of bytes and strings.
 * 
 * @author schroed
 *
 */
public class ByteStringTranslator {
	/**
	 * The maximum number of values that this translator can handle. This is the highest possible number that we can still
	 * represent as an unsigned byte.
	 */
	public static final int MAXNUM = 255;

	private ArrayList<String> list;
	private Map<String, Byte> map;

	/**
	 * Initialize empty byte-string two-way translator.
	 *
	 */
	public ByteStringTranslator() {
		list = new ArrayList<String>();
		map = new HashMap<String, Byte>();
	}

	/**
	 * Prepare a ByteStringTranslator to hold the given amount of data. After calling this the {@link #getNumberOfValues()} will
	 * still return 0, since there is no actual data yet.
	 * 
	 * @param initialRange
	 *            the number of values to expect
	 * @throws IllegalArgumentException
	 *             if initialRange is larger than the maximum number of values, {@link #MAXNUM}.
	 */
	public ByteStringTranslator(int initialRange) {
		int range = initialRange & 0xFF;
		list = new ArrayList<String>(range);
		map = new HashMap<String, Byte>();
	}

	/**
	 * Initialize a byte-string two-way translator, setting byte values according to the position of strings in the array.
	 * 
	 * @param strings
	 *            a list of up to {@link #MAXNUM} strings to be represented by unique byte values.
	 * @throws IllegalArgumentException
	 *             if list of strings is longer than the maximum number of values, {@link #MAXNUM}.
	 */
	public ByteStringTranslator(String[] strings) {
		if (strings.length > MAXNUM) {
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < strings.length; i++) {
				buf.append("\"" + strings[i] + "\" ");
			}
			throw new IllegalArgumentException("Too many strings for a byte-string translator: \n" + buf.toString() + "("
					+ strings.length + " strings)");
		}
		list = new ArrayList<String>(Arrays.asList(strings));
		map = new HashMap<String, Byte>();
		for (int i = 0; i < strings.length; i++) {
			map.put(strings[i], (byte) i);
		}

	}

	/**
	 * Associate the given (unsigned) byte with the given String. Values greater than 127 can be used simply by casting an int to
	 * a byte: <code>set((byte)129, "mystring")</code>
	 * 
	 * @param b
	 *            b
	 * @param s
	 *            s
	 */
	public void set(byte b, String s) {
		int index = b & 0xFF; // make sure we treat the byte as an unsigned byte for position
		list.add(index, s);
		map.put(s, b);
	}

	/**
	 * Verify if the given string can be translated into a byte by this translator.
	 * 
	 * @param s
	 *            s
	 * @return map.containsKey(s)
	 */
	public boolean contains(String s) {
		return map.containsKey(s);
	}

	/**
	 * Check if the given (unsigned) byte value is contained in the list. This supports values between 0 and {@link #MAXNUM}, cast
	 * to byte: <code>contains((byte)129)</code> will indicate if there is a String for the 129'th byte value.
	 * 
	 * @param b
	 *            b
	 * @return false if index &lt; 0 or index &ge; size of list, true otherwise
	 */
	public boolean contains(byte b) {
		int index = b & 0xFF;
		if (index < 0 || index >= list.size())
			return false;
		return true;
	}

	/**
	 * Get the (unsigned) byte value associated to the given string.
	 * 
	 * @param s
	 *            s
	 * @return the (unsigned) byte value associated to the given string. To cast this into an integer, use
	 *         <code>value &amp; 0xFF</code>.
	 * @throws IllegalArgumentException
	 *             if the string is unknown to the translator.
	 */
	public byte get(String s) {
		Byte b = map.get(s);
		if (b == null)
			throw new IllegalArgumentException("No byte value known for string [" + s + "]");
		return b.byteValue();
	}

	/**
	 * Look up the (unsigned) byte in this translator. This supports values between 0 and {@link #MAXNUM}, cast to byte:
	 * <code>get((byte)129)</code> will get you the 129'th item in the string list.
	 * 
	 * @param b
	 *            b
	 * @return list.get(index)
	 */
	public String get(byte b) {
		int index = b & 0xFF;
		if (index < 0 || index >= list.size())
			throw new IndexOutOfBoundsException("Byte value out of range: " + index);
		return list.get(index);
	}

	public String[] getStringValues() {
		return list.toArray(new String[0]);
	}

	/**
	 * Give the number of different values in this translator.
	 * 
	 * @return size of list
	 */
	public int getNumberOfValues() {
		return list.size();
	}

}
