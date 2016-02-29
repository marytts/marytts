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
package marytts.signalproc.analysis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import marytts.util.io.MaryRandomAccessFile;
import marytts.util.io.StreamUtils;

/**
 * A wrapper class for frame based mel frequency cepstral coefficient vectors.
 * 
 * @author Oytun T&uuml;rk
 */
public class Mfccs {
	public double[][] mfccs;
	public MfccFileHeader params;

	public Mfccs() {
		this("");
	}

	public Mfccs(String mfccFile) {
		readMfccFile(mfccFile);
	}

	public Mfccs(int numfrmIn, int dimensionIn) {
		params = new MfccFileHeader();
		allocate(numfrmIn, dimensionIn);
	}

	public void readMfccFile(String mfccFile) {
		mfccs = null;
		params = new MfccFileHeader();

		if (mfccFile != "") {
			DataInputStream stream = null;
			try {
				stream = params.readHeader(mfccFile, true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (stream != null) {
				try {
					mfccs = readMfccs(stream, params);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void writeMfccFile(String mfccFile) {
		if (mfccFile != "") {
			DataOutputStream stream = null;
			try {
				stream = params.writeHeader(mfccFile, true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (stream != null) {
				try {
					writeMfccs(stream, mfccs);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void allocate() {
		allocate(params.numfrm, params.dimension);
	}

	public void allocate(int numEntries, int dimension) {
		mfccs = null;
		params.numfrm = 0;
		params.dimension = 0;

		if (numEntries > 0) {
			mfccs = new double[numEntries][];
			params.numfrm = numEntries;

			if (dimension > 0) {
				params.dimension = dimension;

				for (int i = 0; i < numEntries; i++)
					mfccs[i] = new double[dimension];
			}
		}
	}

	public static void writeMfccFile(double[][] mfccs, String mfccFileOut, MfccFileHeader params) throws IOException {
		params.numfrm = mfccs.length;
		DataOutputStream stream = params.writeHeader(mfccFileOut, true);
		writeMfccs(stream, mfccs);
	}

	public static void writeMfccsFloat(MaryRandomAccessFile stream, double[][] mfccs) throws IOException {
		if (stream != null && mfccs != null && mfccs.length > 0) {
			int i, j;
			for (i = 0; i < mfccs.length; i++) {
				for (j = 0; j < mfccs[i].length; j++)
					stream.writeFloat((float) mfccs[i][j]);
			}

			stream.close();
		}
	}

	public static void writeMfccs(DataOutputStream stream, double[][] mfccs) throws IOException {
		if (stream != null && mfccs != null && mfccs.length > 0) {
			for (int i = 0; i < mfccs.length; i++) {
				StreamUtils.writeDoubleArray(stream, mfccs[i]);
			}

			stream.close();
		}
	}

	public static void writeRawMfccFile(double[][] mfccs, String mfccFileOut) throws IOException {
		MaryRandomAccessFile stream = new MaryRandomAccessFile(mfccFileOut, "rw");

		if (stream != null) {
			writeMfccsFloat(stream, mfccs);
			stream.close();
		}
	}

	public static double[][] readMfccsFromFile(String mfccFile) throws IOException {
		MfccFileHeader params = new MfccFileHeader();
		DataInputStream stream = params.readHeader(mfccFile, true);
		return readMfccs(stream, params);
	}

	public static double[][] readMfccs(DataInputStream stream, MfccFileHeader params) throws IOException {
		double[][] mfccs = null;

		if (stream != null && params.numfrm > 0 && params.dimension > 0) {
			mfccs = new double[params.numfrm][];

			for (int i = 0; i < mfccs.length; i++) {
				mfccs[i] = StreamUtils.readDoubleArray(stream, params.dimension);
			}
			stream.close();
		}

		return mfccs;
	}

	public static double[][] readMfccsFromFloat(MaryRandomAccessFile stream, MfccFileHeader params) throws IOException {
		double[][] mfccs = null;

		if (stream != null && params.numfrm > 0 && params.dimension > 0) {
			mfccs = new double[params.numfrm][params.dimension];

			int i, j;
			for (i = 0; i < mfccs.length; i++) {
				for (j = 0; j < mfccs[i].length; j++)
					mfccs[i][j] = (double) (stream.readFloat());
			}

			stream.close();
		}

		return mfccs;
	}

	public static void readMfccsFromFloat(MaryRandomAccessFile stream, MfccFileHeader params, double[][] outputMfccs)
			throws IOException {
		if (stream != null && params.numfrm > 0 && params.dimension > 0) {
			int i, j;
			for (i = 0; i < params.numfrm; i++) {
				for (j = 0; j < params.dimension; j++)
					outputMfccs[i][j] = (double) (stream.readFloat());
			}

			stream.close();
		}
	}

	public static void main(String[] args) throws Exception {
		Mfccs l1 = new Mfccs();
		l1.params.dimension = 5;
		l1.params.numfrm = 1;
		l1.allocate();
		l1.mfccs[0][0] = 1.5;
		l1.mfccs[0][1] = 2.5;
		l1.mfccs[0][2] = 3.5;
		l1.mfccs[0][3] = 4.5;
		l1.mfccs[0][4] = 5.5;

		String mfccFile = "d:/1.lsf";
		l1.writeMfccFile(mfccFile);
		Lsfs l2 = new Lsfs(mfccFile);

		System.out.println("Test of class Lsfs completed...");
	}
}
