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
package marytts.vocalizations;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import marytts.exceptions.MaryConfigurationException;
import marytts.unitselection.data.Unit;
import marytts.util.data.MaryHeader;

/**
 * Loads a unit file in memory and provides accessors to the start times and durations.
 * 
 * @author sathish pammi
 * 
 */
public class VocalizationUnitFileReader {

	private MaryHeader hdr = null;
	private int numberOfUnits = 0;
	private int sampleRate = 0;
	private VocalizationUnit[] backchannelUnits;

	/****************/
	/* CONSTRUCTORS */
	/****************/

	/**
	 * Empty constructor; need to call load() separately.
	 * 
	 * @see #load(String)
	 */
	public VocalizationUnitFileReader() {
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
	public VocalizationUnitFileReader(String fileName) throws IOException, MaryConfigurationException {
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
			if (hdr.getType() != MaryHeader.LISTENERUNITS) {
				throw new RuntimeException("File [" + fileName + "] is not a valid Mary Units file.");
			}
			/* Read the number of units */
			numberOfUnits = dis.readInt();
			// System.out.println("No. of units : "+ numberOfUnits);
			if (numberOfUnits < 0) {
				throw new RuntimeException("File [" + fileName + "] has a negative number of units. Aborting.");
			}
			/* Read the sample rate */
			sampleRate = dis.readInt();
			// System.out.println("Samplerate : "+ sampleRate);
			if (sampleRate < 0) {
				throw new RuntimeException("File [" + fileName + "] has a negative number sample rate. Aborting.");
			}

			backchannelUnits = new VocalizationUnit[numberOfUnits];

			/* Read the start times and durations */
			for (int i = 0; i < numberOfUnits; i++) {
				int noOfUnits = dis.readInt();
				// System.out.println("No. of Local Units : "+ noOfUnits);
				Unit[] units = new Unit[noOfUnits];
				String[] unitNames = new String[noOfUnits];
				for (int j = 0; j < noOfUnits; j++) {
					long startTime = dis.readLong();
					int duration = dis.readInt();
					// System.out.println("Local Unit Data : "+ startTime+" "+ duration+" "+ j);
					units[j] = new Unit(startTime, duration, j);
					int charArraySize = dis.readInt();
					char[] phoneChar = new char[charArraySize];
					for (int k = 0; k < charArraySize; k++) {
						phoneChar[k] = dis.readChar();
					}
					unitNames[j] = new String(phoneChar);
				}
				long startBCTime = units[0].startTime;
				int bcDuration = (((int) units[noOfUnits - 1].startTime + units[noOfUnits - 1].duration) - (int) units[0].startTime);
				backchannelUnits[i] = new VocalizationUnit(startBCTime, bcDuration, i);
				backchannelUnits[i].setUnits(units);
				backchannelUnits[i].setUnitNames(unitNames);
				// System.out.println("BC UNIT START:"+backchannelUnits[i].getStart());
				// System.out.println("BC UNIT Duration:"+backchannelUnits[i].getDuration());
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
	 * Get the sample rate of the file.
	 * 
	 * @return The sample rate, in Hz.
	 */
	public int getSampleRate() {
		return (sampleRate);
	}

	/**
	 * Return the unit number i.
	 * 
	 * @param i
	 *            The index of the considered unit.
	 * @return The considered unit.
	 */
	public VocalizationUnit getUnit(int i) {
		return backchannelUnits[i];
	}

	/**
	 * Return an array of units from their indexes.
	 * 
	 * @param i
	 *            The indexes of the considered units.
	 * @return The array of considered units.
	 */
	public VocalizationUnit[] getUnit(int[] i) {
		VocalizationUnit[] ret = new VocalizationUnit[i.length];
		for (int k = 0; k < i.length; k++) {
			ret[k] = getUnit(i[k]);
		}
		return (ret);
	}

	/**
	 * Return the unit following the given unit in the original database.
	 * 
	 * @param u
	 *            a unit
	 * @return the next unit in the database, or null if there is no such unit.
	 */
	public VocalizationUnit getNextUnit(VocalizationUnit u) {
		if (u == null || u.index >= backchannelUnits.length - 1 || u.index < 0)
			return null;
		return backchannelUnits[u.index + 1];
	}

	/**
	 * Return the unit preceding the given unit in the original database.
	 * 
	 * @param u
	 *            a unit
	 * @return the previous unit in the database, or null if there is no such unit.
	 */
	public VocalizationUnit getPreviousUnit(VocalizationUnit u) {
		if (u == null || u.index >= backchannelUnits.length || u.index <= 0)
			return null;
		return backchannelUnits[u.index - 1];
	}

	/**
	 * Determine whether the unit number i is an "edge" unit, i.e. a unit marking the start or the end of an utterance.
	 * 
	 * @param i
	 *            The index of the considered unit.
	 * @return true if the unit is an edge unit in the unit file, false otherwise
	 */
	public boolean isEdgeUnit(int i) {
		return backchannelUnits[i].isEdgeUnit();
	}

	public static void main(String[] args) throws Exception {
		String fileName = "/home/sathish/Work/dfki399/backchannel/mary_files/BCCphoneUnits.mry";
		VocalizationUnitFileReader bcUfr = new VocalizationUnitFileReader();
		bcUfr.load(fileName);
	}
}
