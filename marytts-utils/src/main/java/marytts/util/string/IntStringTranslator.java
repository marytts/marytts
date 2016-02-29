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
 * A helper class converting between a given set of integers and strings.
 * 
 * @author schroed
 *
 */
public class IntStringTranslator {
	ArrayList<String> list;
	Map<String, Integer> map;

	/**
	 * Initialize empty int-string two-way translator.
	 *
	 */
	public IntStringTranslator() {
		list = new ArrayList<String>();
		map = new HashMap<String, Integer>();
	}

	public IntStringTranslator(int initialRange) {
		list = new ArrayList<String>(initialRange);
		map = new HashMap<String, Integer>();
	}

	/**
	 * Initialize a int-string two-way translator, setting int values according to the position of strings in the array.
	 * 
	 * @param strings
	 *            strings
	 */
	public IntStringTranslator(String[] strings) {
		list = new ArrayList<String>(Arrays.asList(strings));
		map = new HashMap<String, Integer>();
		for (int i = 0; i < strings.length; i++) {
			map.put(strings[i], i);
		}
	}

	public void set(int i, String s) {
		list.add(i, s);
		map.put(s, i);
	}

	public boolean contains(String s) {
		return map.containsKey(s);
	}

	public boolean contains(int b) {
		int index = b;
		if (index < 0 || index >= list.size())
			return false;
		return true;
	}

	public int get(String s) {
		Integer index = map.get(s);
		if (index == null)
			throw new IllegalArgumentException("No int value known for string [" + s + "]");
		return index.intValue();
	}

	public String get(int i) {
		if (i < 0 || i >= list.size())
			throw new IndexOutOfBoundsException("Int value out of range: " + i);
		return list.get(i);
	}

	public String[] getStringValues() {
		return list.toArray(new String[0]);
	}

	public int getHighestValue() {
		return list.size();
	}
}
