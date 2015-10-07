/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.util.io;

// Java classes
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * A class splitting a Reader into chunks. In a continuous input Reader, search for lines containing a specific "end-of-chunk"
 * marking (e.g., an XML root end tag), and return individual readers, each of which will provide one chunk (including the line
 * containing the end-of-chunk marking).
 * 
 * @author Marc Schr&ouml;der
 */

public class ReaderSplitter {
	private BufferedReader in;
	private StringBuffer buf;
	private String endMarker;

	public ReaderSplitter(Reader in, String endMarker) {
		this.in = new BufferedReader(in);
		this.endMarker = endMarker;
		buf = new StringBuffer(1000);
	}

	/**
	 * Return a reader from which one chunk can be read, followed by EOF. Chunks are delimited by start of file, lines containing
	 * the end marker string (line is last line in chunk), and end of file. Returns null if nothing more can be read.
	 * 
	 * @throws IOException
	 *             IOException
	 * @return stringReader(buf.toString())
	 */
	public Reader nextReader() throws IOException {
		String line = null;
		buf.setLength(0); // start with an empty buffer
		while ((line = in.readLine()) != null) {
			buf.append(line);
			buf.append(System.getProperty("line.separator"));
			if (line.indexOf(endMarker) != -1) { // found end marker in line
				break;
			}
		}
		if (buf.length() == 0)
			return null; // nothing more to read.
		return (Reader) new StringReader(buf.toString());
	}
}
