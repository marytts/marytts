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
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.util.data.MaryHeader;

/**
 * Reads a single file which contains HNM analysis features of vocalizations
 * 
 * @author sathish pammi
 * 
 */
public class HNMFeatureFileReader {

	private MaryHeader hdr = null;
	private HntmSpeechSignal[] hnmSignals;
	private int numberOfUnits = 0;

	/**
	 * Create a feature file reader from the given HNM feature file
	 * 
	 * @param fileName
	 *            the unit file to read
	 * @throws IOException
	 *             if a problem occurs while reading
	 * @throws MaryConfigurationException
	 *             if runtime configuration fails
	 */
	public HNMFeatureFileReader(String fileName) throws IOException, MaryConfigurationException {
		load(fileName);
	}

	/**
	 * Load the given feature file
	 * 
	 * @param fileName
	 *            the feature file to read
	 * @throws IOException
	 *             if a problem occurs while reading
	 * @throws MaryConfigurationException
	 *             if runtime configuration fails
	 */
	private void load(String fileName) throws IOException, MaryConfigurationException {
		// Open the file
		DataInputStream dis = null;

		try {
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
		} catch (FileNotFoundException e) {
			throw new MaryConfigurationException("File [" + fileName + "] was not found.");
		}

		// Load the Mary header
		hdr = new MaryHeader(dis);
		if (hdr.getType() != MaryHeader.LISTENERFEATS) {
			throw new MaryConfigurationException("File [" + fileName + "] is not a valid Mary Units file.");
		}

		numberOfUnits = dis.readInt(); // Read the number of units
		if (numberOfUnits < 0) {
			throw new MaryConfigurationException("File [" + fileName + "] has a negative number of units. Aborting.");
		}

		hnmSignals = new HntmSpeechSignal[numberOfUnits];
		for (int i = 0; i < numberOfUnits; i++) {
			hnmSignals[i] = new HntmSpeechSignal(0, 0, 0);
			hnmSignals[i].read(dis, HntmAnalyzerParams.WAVEFORM);
		}

		System.out.println();
	}

	/**
	 * Get the number of units in the file.
	 * 
	 * @return The number of units.
	 */
	public int getNumberOfUnits() {
		return (numberOfUnits);
	}

	/**
	 * get HntmSpeechSignal for a unit index
	 * 
	 * @param unitnumber
	 *            unit index number
	 * @return HntmSpeechSignal hnm analysis feature
	 * @throws IllegalArgumentException
	 *             if given index number is not less than available units
	 */
	public HntmSpeechSignal getHntmSpeechSignal(int unitnumber) {
		if (unitnumber >= this.numberOfUnits) {
			throw new IllegalArgumentException("the given unit index number(" + unitnumber
					+ ") must be less than number of available units(" + this.numberOfUnits + ")");
		}
		return this.hnmSignals[unitnumber];
	}

	public static void main(String[] args) throws Exception {
		String fileName = "/home/sathish/Work/phd/voices/mlsa-poppy-listener/vocalizations/files/vocalization_hnm_analysis.mry";
		HNMFeatureFileReader bcUfr = new HNMFeatureFileReader(fileName);
		// bcUfr.load(fileName);
	}
}
