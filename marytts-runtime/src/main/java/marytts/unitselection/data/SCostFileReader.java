/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package marytts.unitselection.data;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import marytts.exceptions.MaryConfigurationException;
import marytts.util.data.MaryHeader;

/**
 * sCost file reader
 * 
 * @author sathish pammi
 * 
 */
public class SCostFileReader {

	private MaryHeader hdr = null;
	private int numberOfUnits = 0;
	private double[] sCost;

	/****************/
	/* CONSTRUCTORS */
	/****************/

	/**
	 * Empty constructor; need to call load() separately.
	 * 
	 * @see #load(String)
	 */
	public SCostFileReader() {
	}

	/**
	 * Create a unit file reader from the given unit file
	 * 
	 * @param fileName
	 *            the unit file to read
	 * @throws IOException
	 *             if a problem occurs while reading
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public SCostFileReader(String fileName) throws IOException, MaryConfigurationException {
		load(fileName);
	}

	/**
	 * Load the given unit file
	 * 
	 * @param fileName
	 *            the unit file to read
	 * @throws IOException
	 *             if a problem occurs while reading
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public void load(String fileName) throws IOException, MaryConfigurationException {
		/* Open the file */
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
		} catch (FileNotFoundException e) {
			throw new RuntimeException("File [" + fileName + "] was not found.");
		}
		try {
			/* Load the Mary header */
			hdr = new MaryHeader(dis);
			if (hdr.getType() != MaryHeader.SCOST) {
				throw new RuntimeException("File [" + fileName + "] is not a valid Mary Units file.");
			}
			/* Read the number of units */
			numberOfUnits = dis.readInt();
			if (numberOfUnits < 0) {
				throw new RuntimeException("File [" + fileName + "] has a negative number of units. Aborting.");
			}

			sCost = new double[numberOfUnits];
			/* Read the start times and durations */
			for (int i = 0; i < numberOfUnits; i++) {
				sCost[i] = dis.readFloat();
			}
		} catch (IOException e) {
			throw new RuntimeException("Reading the Mary header from file [" + fileName + "] failed.", e);
		}

	}

	/*****************/
	/* OTHER METHODS */
	/*****************/

	/**
	 * Get the number of units in the file.
	 * 
	 * @return The number of units.
	 */
	public int getNumberOfUnits() {
		return (numberOfUnits);
	}

	/**
	 * Get sCost for a unit index
	 * 
	 * @param index
	 *            index
	 * @return sCost
	 */
	public double getSCost(int index) {
		return (this.sCost[index]);
	}

}
