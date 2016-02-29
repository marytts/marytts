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
 * A helper class converting between a given set of shorts and strings.
 * 
 * @author schroed
 *
 */
public class ShortStringTranslator {
	ArrayList<String> list;
	Map<String, Short> map;

	/**
	 * Initialize empty short-string two-way translator.
	 *
	 */
	public ShortStringTranslator() {
		list = new ArrayList<String>();
		map = new HashMap<String, Short>();
	}

	public ShortStringTranslator(short initialRange) {
		list = new ArrayList<String>(initialRange);
		map = new HashMap<String, Short>();
	}

	/**
	 * Initialize a short-string two-way translator, setting short values according to the position of strings in the array.
	 * 
	 * @param strings
	 *            strings
	 */
	public ShortStringTranslator(String[] strings) {
		if (strings.length > Short.MAX_VALUE)
			throw new IllegalArgumentException("Too many strings for a short-string translator");
		list = new ArrayList<String>(Arrays.asList(strings));
		map = new HashMap<String, Short>();
		for (int i = 0; i < strings.length; i++) {
			map.put(strings[i], (short) i);
		}
	}

	public void set(short b, String s) {
		list.add(b, s);
		map.put(s, b);
	}

	public boolean contains(String s) {
		return map.containsKey(s);
	}

	public boolean contains(short b) {
		int index = (int) b;
		if (index < 0 || index >= list.size())
			return false;
		return true;
	}

	public short get(String s) {
		Short index = map.get(s);
		if (index == null)
			throw new IllegalArgumentException("No short value known for string [" + s + "]");
		return index.shortValue();
	}

	public String get(short b) {
		int index = (int) b;
		if (index < 0 || index >= list.size())
			throw new IndexOutOfBoundsException("Short value out of range: " + index);
		return list.get(index);
	}

	public String[] getStringValues() {
		return list.toArray(new String[0]);
	}

	public short getNumberOfValues() {
		return (short) list.size();
	}

}
