/**
 * Copyright 2010 DFKI GmbH.
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

import marytts.util.io.StreamUtils;
import marytts.util.math.MathUtils;

/**
 * A wrapper class for frame based voice quality parameters
 * 
 * @author Marcela Charfuelan
 */
public class VoiceQuality {

	public double[][] vq;
	public VoiceQualityFileHeader params;

	public VoiceQuality() {
		this("");
	}

	public VoiceQuality(String vqFile) {
		readVqFile(vqFile);
	}

	/**
	 * VoiceQuality object containing various vq mesures
	 * 
	 * @param numVqParams
	 *            number of vq parameters per frame
	 * @param Fs
	 *            sampling rate
	 * @param skipSize
	 *            skip size in seconds
	 * @param winSize
	 *            window size in seconds
	 */
	public VoiceQuality(int numVqParams, int Fs, float skipSize, float winSize) {
		params = new VoiceQualityFileHeader();
		params.dimension = numVqParams;
		params.samplingRate = 16000;
		params.skipsize = skipSize; // in seconds
		params.winsize = winSize; // in seconds

	}

	public void allocate(int numFramesVq, double[][] par) {
		params.numfrm = numFramesVq;
		vq = new double[params.dimension][numFramesVq];
		for (int i = 0; i < params.dimension; i++)
			for (int j = 0; j < numFramesVq; j++)
				vq[i][j] = par[i][j];

	}

	public double[] getOQG() {
		return vq[0];
	}

	public double[] getGOG() {
		return vq[1];
	}

	public double[] getSKG() {
		return vq[2];
	}

	public double[] getRCG() {
		return vq[3];
	}

	public double[] getIC() {
		return vq[4];
	}

	public void printPar() {
		System.out.println("Features Read:\nframe\tOQG\tGOG\tSKG\tRCG\tIC");
		for (int i = 0; i < params.numfrm; i++)
			System.out.format("%d\t%.3f %.3f %.3f %.3f %.3f \n", i + 1, vq[0][i], vq[1][i], vq[2][i], vq[3][i], vq[4][i]);
	}

	public void printMeanStd() {
		System.out.println("Mean +- Standard deviation:\n\tOQG\tGOG\tSKG\tRCG\tIC");
		System.out.format("mean: %.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", MathUtils.mean(vq[0], 0), MathUtils.mean(vq[1], 0),
				MathUtils.mean(vq[2], 0), MathUtils.mean(vq[3], 0), MathUtils.mean(vq[4], 0));
		System.out.format("std : %.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", MathUtils.standardDeviation(vq[0], 0),
				MathUtils.standardDeviation(vq[1], 0), MathUtils.standardDeviation(vq[2], 0),
				MathUtils.standardDeviation(vq[3], 0), MathUtils.standardDeviation(vq[4], 0));
	}

	public void applyZscoreNormalization() {
		for (int i = 0; i < vq.length; i++)
			vq[i] = MathUtils.normalizeZscore(vq[i]);
	}

	public void readVqFile(String vqFile) {
		params = new VoiceQualityFileHeader();

		if (vqFile != "") {
			DataInputStream stream = null;
			try {
				stream = params.readHeader(vqFile, true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (stream != null) {
				try {
					vq = readVqs(stream, params);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void writeVqFile(String vqFile) {
		if (vqFile != "") {
			DataOutputStream stream = null;
			try {
				stream = params.writeHeader(vqFile, true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (stream != null) {
				try {
					writeVqs(stream, vq);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public static void writeVqs(DataOutputStream stream, double[][] vqs) throws IOException {
		if (stream != null && vqs != null && vqs.length > 0) {
			for (int i = 0; i < vqs.length; i++) {
				StreamUtils.writeDoubleArray(stream, vqs[i]);
			}
			stream.close();
		}
	}

	public static double[][] readVqs(DataInputStream stream, VoiceQualityFileHeader params) throws IOException {
		double[][] vqs = null;

		if (stream != null && params.dimension > 0 && params.dimension > 0) {
			vqs = new double[params.dimension][];

			for (int i = 0; i < vqs.length; i++) {
				vqs[i] = StreamUtils.readDoubleArray(stream, params.numfrm);
			}
			stream.close();
		}

		return vqs;
	}

}
