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
import marytts.util.data.MaryHeader;

/**
 * Reads a single file which contains all MLSA features (logfo, mgc and strengths) of vocalizations
 * 
 * @author sathish pammi
 * 
 */
public class MLSAFeatureFileReader {

	private MaryHeader hdr = null;

	private int numberOfUnits;
	private int STRVECTORSIZE;
	private int MGCVECTORSIZE;
	private int LF0VECTORSIZE;

	private int[] numberOfFrames;
	private boolean[][] voiced;
	private double[][] logf0;
	private double[][][] strengths;
	private double[][][] mgc;

	/**
	 * Create a feature file reader from the given MLSA feature file
	 * 
	 * @param fileName
	 *            the unit file to read
	 * @throws IOException
	 *             if a problem occurs while reading
	 * @throws MaryConfigurationException
	 *             if runtime configuration fails
	 */
	public MLSAFeatureFileReader(String fileName) throws IOException, MaryConfigurationException {
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

		LF0VECTORSIZE = dis.readInt(); // Read LF0 vector size
		MGCVECTORSIZE = dis.readInt(); // Read MGC vector size
		STRVECTORSIZE = dis.readInt(); // Read STR vector size

		if (LF0VECTORSIZE != 1 || MGCVECTORSIZE <= 0 || STRVECTORSIZE <= 0) {
			throw new MaryConfigurationException("File [" + fileName
					+ "] has no proper feature vector size information... Aborting.");
		}

		logf0 = new double[numberOfUnits][];
		voiced = new boolean[numberOfUnits][];
		mgc = new double[numberOfUnits][][];
		strengths = new double[numberOfUnits][][];
		numberOfFrames = new int[numberOfUnits];

		for (int i = 0; i < numberOfUnits; i++) {

			numberOfFrames[i] = dis.readInt();

			// read LF0 data
			int checkLF0Size = dis.readInt();
			assert checkLF0Size == (numberOfFrames[i] * LF0VECTORSIZE) : fileName + " feature file do not has proper format";
			logf0[i] = new double[numberOfFrames[i]];
			voiced[i] = new boolean[numberOfFrames[i]];
			for (int j = 0; j < numberOfFrames[i]; j++) {
				logf0[i][j] = dis.readFloat();
				if (logf0[i][j] < 0) {
					voiced[i][j] = false;
				} else {
					voiced[i][j] = true;
				}
			}

			// read MGC data
			int checkMGCSize = dis.readInt();
			assert checkMGCSize == (numberOfFrames[i] * this.MGCVECTORSIZE) : fileName + " feature file do not has proper format";
			mgc[i] = new double[numberOfFrames[i]][MGCVECTORSIZE];
			for (int j = 0; j < numberOfFrames[i]; j++) {
				for (int k = 0; k < MGCVECTORSIZE; k++) {
					mgc[i][j][k] = dis.readFloat();
				}
			}

			// read STR data
			int checkSTRSize = dis.readInt();
			assert checkSTRSize == (numberOfFrames[i] * this.STRVECTORSIZE) : fileName + " feature file do not has proper format";
			strengths[i] = new double[numberOfFrames[i]][STRVECTORSIZE];
			for (int j = 0; j < numberOfFrames[i]; j++) {
				for (int k = 0; k < STRVECTORSIZE; k++) {
					strengths[i][j][k] = dis.readFloat();
				}
			}
		}
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
	 * get boolean array of voiced frame information: true, if voiced; false if unvoiced;
	 * 
	 * @param unitnumber
	 *            unit index number
	 * @return boolean[] boolean array of voiced frames
	 * @throws IllegalArgumentException
	 *             if given index number is not less than available units
	 */
	public boolean[] getVoicedFrames(int unitnumber) {
		if (unitnumber >= this.numberOfUnits) {
			throw new IllegalArgumentException("the given unit index number(" + unitnumber
					+ ") must be less than number of available units(" + this.numberOfUnits + ")");
		}
		return this.voiced[unitnumber];
	}

	/**
	 * get array of logf0 features
	 * 
	 * @param unitnumber
	 *            unit index number
	 * @return double[] array of logf0 values
	 * @throws IllegalArgumentException
	 *             if given index number is not less than available units
	 */
	public double[] getUnitLF0(int unitnumber) {
		if (unitnumber >= this.numberOfUnits) {
			throw new IllegalArgumentException("the given unit index number(" + unitnumber
					+ ") must be less than number of available units(" + this.numberOfUnits + ")");
		}
		return this.logf0[unitnumber];
	}

	/**
	 * get double array of MGC features
	 * 
	 * @param unitnumber
	 *            unit index number
	 * @return double[][] array of mgc vectors
	 * @throws IllegalArgumentException
	 *             if given index number is not less than available units
	 */
	public double[][] getUnitMGCs(int unitnumber) {
		if (unitnumber >= this.numberOfUnits) {
			throw new IllegalArgumentException("the given unit index number(" + unitnumber
					+ ") must be less than number of available units(" + this.numberOfUnits + ")");
		}
		return this.mgc[unitnumber];
	}

	/**
	 * get double array of strength features
	 * 
	 * @param unitnumber
	 *            unit index number
	 * @return double[][] array of strength vectors
	 * @throws IllegalArgumentException
	 *             if given index number is not less than available units
	 */
	public double[][] getUnitStrengths(int unitnumber) {
		if (unitnumber >= this.numberOfUnits) {
			throw new IllegalArgumentException("the given unit index number(" + unitnumber
					+ ") must be less than number of available units(" + this.numberOfUnits + ")");
		}
		return this.strengths[unitnumber];
	}

	/**
	 * get vector size of MGC features
	 * 
	 * @return int mgc vector size
	 */
	public int getMGCVectorSize() {
		return this.MGCVECTORSIZE;
	}

	/**
	 * get vector size of LF0 features
	 * 
	 * @return int lf0 vector size
	 */
	public int getLF0VectorSize() {
		return this.LF0VECTORSIZE;
	}

	/**
	 * get vector size of strength features
	 * 
	 * @return int strengths vector size
	 */
	public int getSTRVectorSize() {
		return this.STRVECTORSIZE;
	}

	public static void main(String[] args) throws Exception {
		String fileName = "/home/sathish/Work/phd/voices/mlsa-poppy-listener/vocalizations/files/vocalization_mlsa_features.mry";
		MLSAFeatureFileReader bcUfr = new MLSAFeatureFileReader(fileName);
		// bcUfr.load(fileName);
	}
}
