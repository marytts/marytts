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
package marytts.signalproc.adaptation.codebook;

/**
 * Wrapper for parameters of weighted codebook mapping procedure
 * 
 * @author Oytun T&uuml;rk
 */
public class WeightedCodebookMapperParams {
	public int numBestMatches; // Number of best matches in codebook

	public int distanceMeasure; // Distance measure for comparing source training and transformation features
	public static int LSF_INVERSE_HARMONIC_DISTANCE = 1;
	public static int LSF_INVERSE_HARMONIC_DISTANCE_SYMMETRIC = 2;
	public static int LSF_EUCLIDEAN_DISTANCE = 3;
	public static int LSF_MAHALANOBIS_DISTANCE = 4;
	public static int LSF_ABSOLUTE_VALUE_DISTANCE = 5;
	public static int DEFAULT_DISTANCE_MEASURE = LSF_INVERSE_HARMONIC_DISTANCE;

	public double alphaForSymmetric; // Weighting factor for using weights of two lsf vectors in distance computation relatively.
										// The range is [0.0,1.0]
	public static double MIN_ALPHA_FOR_SYMMETRIC = 0.0;
	public static double MAX_ALPHA_FOR_SYMMETRIC = 1.0f;

	public int weightingMethod; // Method for weighting best codebook matches
	public static int EXPONENTIAL_HALF_WINDOW = 1;
	public static int TRIANGLE_HALF_WINDOW = 2;
	public static int DEFAULT_WEIGHTING_METHOD = EXPONENTIAL_HALF_WINDOW;

	public double weightingSteepness; // Steepness of weighting function in range [MIN_STEEPNESS, MAX_STEEPNESS]
	public static double MIN_STEEPNESS = 0.0; // All weights are equal
	public static double MAX_STEEPNESS = 10.0; // Best codebook weight is very large as compared to remaining
	public static double DEFAULT_WEIGHTING_STEEPNESS = 1.0;

	// //Mean and variance of a specific distance measure can be optionally kept in the follwoing
	// two parameters for z-normalization
	public double distanceMean;
	public double distanceVariance;
	public static final double DEFAULT_DISTANCE_MEAN = 0.0;
	public static final double DEFAULT_DISTANCE_VARIANCE = 1.0;

	public double freqRange; // Frequency range to be considered around center freq when matching LSFs (note that center freq is
								// estimated automatically as the middle of most closest LSFs)
	public static final double DEFAULT_FREQ_RANGE_FOR_LSF_MATCH = 5000.0;

	public int lpOrder; // Linear prediction oreder

	public WeightedCodebookMapperParams() {
		distanceMeasure = DEFAULT_DISTANCE_MEASURE;
		weightingMethod = DEFAULT_WEIGHTING_METHOD;
		weightingSteepness = DEFAULT_WEIGHTING_STEEPNESS;
		distanceMean = DEFAULT_DISTANCE_MEAN;
		distanceVariance = DEFAULT_DISTANCE_VARIANCE;
		freqRange = DEFAULT_FREQ_RANGE_FOR_LSF_MATCH;
	}

	public WeightedCodebookMapperParams(WeightedCodebookMapperParams w) {
		numBestMatches = w.numBestMatches;
		distanceMeasure = w.distanceMeasure;
		alphaForSymmetric = w.alphaForSymmetric;
		weightingMethod = w.weightingMethod;
		weightingSteepness = w.weightingSteepness;
		distanceMean = w.distanceMean;
		distanceVariance = w.distanceVariance;
		lpOrder = w.lpOrder;
		freqRange = w.freqRange;
	}

}
