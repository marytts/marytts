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

import java.io.FileNotFoundException;
import java.io.IOException;

import marytts.util.io.LEDataInputStream;
import marytts.util.io.LEDataOutputStream;

/**
 * A class for handling source-target frame indices mapping It can be used in various ways:
 * 
 * @author Oytun T&uuml;rk
 */
public class IndexMap {
	public FileMap[] files; // A frame map for individual file pairs

	public IndexMap() {
		files = null;
	}

	public IndexMap(int numItems) {
		allocate(numItems);
	}

	public IndexMap(IndexMap im) {
		copyFrom(im);
	}

	public void allocate(int numItems) {
		if (numItems > 0)
			files = new FileMap[numItems];
		else
			files = null;
	}

	public void copyFrom(IndexMap im) {
		if (im.files.length > 0) {
			files = new FileMap[im.files.length];
			for (int i = 0; i < im.files.length; i++)
				files[i] = new FileMap(im.files[i]);
		} else
			files = null;
	}

	// Write the object into a binary file
	public void writeToFile(String binaryFileName) throws IOException {
		LEDataOutputStream out = null;

		try {
			out = new LEDataOutputStream(binaryFileName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (out != null) {
			int i, j;

			out.writeInt(files.length);
			for (i = 0; i < files.length; i++) {
				out.writeInt(files[i].indicesMap.length);
				for (j = 0; j < files[i].indicesMap.length; j++) {
					out.writeInt(files[i].indicesMap[j].length);
					out.writeInt(files[i].indicesMap[j]);
				}
			}

			out.close();
		}
	}

	// Read the object from a binary file
	public void readFromFile(String binaryFileName) throws IOException {
		LEDataInputStream in = null;

		try {
			in = new LEDataInputStream(binaryFileName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (in != null) {
			int i, j;

			int numItems = in.readInt();
			allocate(numItems);
			int tmpNumGroups;
			int tmpNumItems;
			int[] tmpInts;

			for (i = 0; i < numItems; i++) {
				tmpNumGroups = in.readInt();

				files[i] = new FileMap(tmpNumGroups, 0);

				for (j = 0; j < tmpNumGroups; j++) {
					tmpNumItems = in.readInt();
					if (tmpNumItems > 0) {
						files[i].indicesMap[j] = new int[tmpNumItems];
						tmpInts = in.readInt(tmpNumItems);
						System.arraycopy(tmpInts, 0, files[i].indicesMap[j], 0, files[i].indicesMap[j].length);
					}
				}
			}

			in.close();
		}

	}
}
