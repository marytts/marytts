/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.util.data.text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.StringTokenizer;

/**
 * @author Marc Schr&ouml;der
 * 
 */
public class ESTTextfileDoubleDataSource extends TextReaderDoubleDataSource {

	/**
	 * Read Double data from a Text file e.g. in EST format. Skip the header, then read one double per line, which is the first
	 * token in that line.
	 * 
	 * @param file
	 *            file
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 */
	public ESTTextfileDoubleDataSource(File file) throws FileNotFoundException {
		this(new FileReader(file));
	}

	/**
	 * Read Double data from a Text file e.g. in EST format. Skip the header, then read one double per line, which is the first
	 * token in that line.
	 * 
	 * @param reader
	 *            reader
	 */
	public ESTTextfileDoubleDataSource(Reader reader) {
		super(reader);
		// Skip header:
		try {
			while (!this.reader.readLine().startsWith("EST_Header_End")) {
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Try to get length doubles from this DoubleDataSource, and copy them into target, starting from targetPos. This is the core
	 * method getting the data. Subclasses may want to override this method. If an exception occurs reading from the underlying
	 * reader, or converting data to double, the method will print a stack trace to standard error, but otherwise will silently
	 * stop and behave as if all data was read.
	 * 
	 * @param target
	 *            the double array to write into
	 * @param targetPos
	 *            position in target where to start writing
	 * @param length
	 *            the amount of data requested
	 * @return the amount of data actually delivered. If the returned value is less than length, only that many data items have
	 *         been copied into target; further calls will return 0 and not copy anything.
	 */
	public int getData(double[] target, int targetPos, int length) {
		for (int i = 0; i < length; i++) {
			try {
				String line = reader.readLine();
				if (line == null)
					return i;
				String token = new StringTokenizer(line.trim()).nextToken();
				double value = Double.parseDouble(token);
				target[targetPos + i] = value;
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return i;
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
				return i;
			}
		}
		return length;
	}

}
