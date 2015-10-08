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
package marytts.signalproc.analysis.distance;

/**
 * A general purpose class for computing various distance measures Examples include Euclidean, Mahalanobis, distance to GMMs etc.
 * 
 * @author Oytun T&uuml;rk
 **/

public class DistanceComputer {
	public static final int ABSOLUTE_VALUE_DISTANCE = 1;
	public static final int EUCLIDEAN_DISTANCE = 2;
	public static final int NORMALIZED_EUCLIDEAN_DISTANCE = 3;
	public static final int MAHALANOBIS_DISTANCE = 4;

	// Computes absolute value distance
	// dist = sum(|x(i)-y(i)|)
	public static double getAbsoluteValueDistance(double[] x, double[] y) {
		assert x.length == y.length;

		double dist = 0.0;
		for (int i = 0; i < x.length; i++)
			dist += Math.abs(x[i] - y[i]);

		return dist;
	}

	// Computes the Euclidean distance metric
	// dist = sqrt( sum( (x[i]-y[i])^2 ) )
	public static double getEuclideanDistance(double[] x, double[] y) {
		assert x.length == y.length;

		double dist = 0.0;
		for (int i = 0; i < x.length; i++)
			dist += (x[i] - y[i]) * (x[i] - y[i]);

		dist = Math.sqrt(dist);

		return dist;
	}

	// Computes the normalized Euclidean distance metric
	// dist = sqrt( sum( (x[i]-y[i])^2 / var[i] ) )
	public static double getNormalizedEuclideanDistance(double[] x, double[] y, double[] variances) {
		assert x.length == y.length;
		assert x.length == variances.length;

		double dist = 0.0;
		for (int i = 0; i < x.length; i++)
			dist += (x[i] - y[i]) * (x[i] - y[i]) / variances[i];

		dist = Math.sqrt(dist);

		return dist;
	}

	/*
	 * Inverse LSF harmonic distance which is used to find closest LSF matches for a given source LSF vector in the source
	 * codebook:
	 * 
	 * R. Laroia, N. Phamdo, and N. Farvardin, “Robust and efficient quantization of speech lsp parameters using structured vector
	 * quantizers,” in Proc. of the IEEE ICASSP 1991, pp. 641–644.
	 * 
	 * Note that the function requires the lsfWeights array to be created outside of this function for efficiency purposes as this
	 * function is called many times during transformation. The length of the array should be equal to the length of the lsf
	 * vectors
	 */
	public static double getLsfInverseHarmonicDistance(double[] lsfs1, double[] lsfs2, double freqRange) {
		assert lsfs1.length == lsfs2.length;

		double[] lsfWeights = getLsfWeights(lsfs1, freqRange);

		double dist = 0.0;

		for (int i = 0; i < lsfs1.length; i++)
			dist += lsfWeights[i] * Math.abs(lsfs1[i] - lsfs2[i]);

		return dist;
	}

	// A symmetric version of the inverse harmonic based lsf distance
	// The weights are averaged in a weighted manner using alpha prior to distance computation
	// alpha should be in the range [0.0,1.0].
	// If it is 0.0 lsfWeights1 get all the weighting, so the function is identical to getLsfInverseHarmonicDistance.
	// lsfWeights should be provided outside of the function and their length should match the
	public static double getLsfInverseHarmonicDistanceSymmetric(double[] lsfs1, double[] lsfs2, double alpha, double freqRange) {
		assert lsfs1.length == lsfs2.length;

		double[] lsfWeights1 = getLsfWeights(lsfs1, freqRange);
		double[] lsfWeights2 = getLsfWeights(lsfs2, freqRange);

		double dist = 0.0;
		double oneMinusAlpha = 1.0 - alpha;
		double absVal;
		for (int i = 0; i < lsfs1.length; i++) {
			absVal = Math.abs(lsfs1[i] - lsfs2[i]);
			dist += oneMinusAlpha * lsfWeights1[i] * absVal + alpha * lsfWeights2[i] * absVal;
		}

		return dist;
	}

	// Fills in the lsfWeights array with weights estimated for lsfs
	// Note that the function requires the lsfWeights array to be created outside of this function
	// for efficiency purposes as this function is called many times during transformation
	public static double[] getLsfWeights(double[] lsfs, double freqRange) {
		int i;
		double[] lsfWeights = new double[lsfs.length];
		lsfWeights[0] = 1.0 / Math.abs(lsfs[1] - lsfs[0]);
		for (i = 1; i < lsfWeights.length - 1; i++)
			lsfWeights[i] = 1.0 / Math.min(Math.abs(lsfs[i] - lsfs[i - 1]), Math.abs(lsfs[i + 1] - lsfs[i]));

		lsfWeights[lsfWeights.length - 1] = 1.0 / Math.abs(lsfs[lsfWeights.length - 1] - lsfs[lsfWeights.length - 2]);

		/*
		 * //Method 1 int ind = MathUtils.getMaxIndex(lsfWeights); double centerFreq; if (ind==0) centerFreq =
		 * 0.5*(lsfs[ind+1]+lsfs[ind])-0.5*freqRange; else { if (ind<lsfs.length-1) { if (Math.abs(lsfs[ind]-lsfs[ind-1]) <
		 * Math.abs(lsfs[ind+1]-lsfs[ind])) centerFreq = 0.5*(lsfs[ind]+lsfs[ind-1])-0.5*freqRange; else centerFreq =
		 * 0.5*(lsfs[ind+1]+lsfs[ind])-0.5*freqRange; } else centerFreq = 0.5*(lsfs[ind]+lsfs[ind-1])-0.5*freqRange; } //
		 */

		// Method 2
		double tempSum = 0.0;
		double centerFreq = 0.0;
		for (i = 0; i < lsfs.length; i++) {
			centerFreq += lsfs[i] * lsfWeights[i];
			tempSum += lsfWeights[i];
		}
		centerFreq /= tempSum;
		//

		double lowerFreq = Math.max(0.0, centerFreq - 0.5 * freqRange);
		double upperFreq = lowerFreq + freqRange;

		for (i = 0; i < lsfs.length; i++) {
			if (lsfs[i] < lowerFreq || lsfs[i] > upperFreq)
				lsfWeights[i] = 0.0;
		}

		return lsfWeights;
	}

	// Note that this requires an inverse covariance matrix
	// If you have a diagonal matrix only, then the Mahalanobis distance reduces to
	// Normalized Eucledian distance
	// dist = sqrt( sum( (x-meanX)' * (covarianceMatrix^-1) * (x-meanX) ) )
	public static double getMahalanobisDistance(double[] x, double[] mean, double[][] inverseCovarianceMatrix) {
		assert x.length == mean.length;
		assert x.length == inverseCovarianceMatrix.length;

		int i, j;
		double dist = 0.0;
		double tmpDist;

		for (i = 0; i < x.length; i++) {
			tmpDist = 0.0;
			for (j = 0; j < x.length; j++)
				tmpDist += (x[j] - mean[j]) * inverseCovarianceMatrix[j][i];

			dist += tmpDist * (x[i] - mean[i]);
		}

		dist = Math.sqrt(dist);

		return dist;
	}
}
