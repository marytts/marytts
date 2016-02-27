/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.signalproc.adaptation;

/**
 * A class for handling source-target frame indices mapping for a single pair
 * 
 * @author Oytun T&uuml;rk
 */
public class FileMap {
	public int[][] indicesMap;

	public FileMap() {
		allocate(0, 0);
	}

	public FileMap(int numGroups) {
		allocate(numGroups, 0);
	}

	public FileMap(int numGroups, int numItems) {
		allocate(numGroups, numItems);
	}

	public FileMap(FileMap fm) {
		if (fm.indicesMap != null) {
			indicesMap = new int[fm.indicesMap.length][];
			for (int i = 0; i < fm.indicesMap.length; i++) {
				if (fm.indicesMap[i] != null) {
					indicesMap[i] = new int[fm.indicesMap[i].length];
					System.arraycopy(fm.indicesMap[i], 0, indicesMap[i], 0, fm.indicesMap[i].length);
				} else
					indicesMap[i] = null;
			}
		} else
			indicesMap = null;
	}

	public void allocate(int numGroups, int numItems) {
		if (numGroups > 0) {
			if (numItems > 0)
				indicesMap = new int[numGroups][numItems];
			else
				indicesMap = new int[numGroups][];
		} else
			indicesMap = null;
	}
}
