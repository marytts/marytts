/**
 * Copyright 2009 DFKI GmbH.
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

package marytts.tools.voiceimport.traintrees;

import java.io.IOException;

import marytts.features.FeatureVector;
import marytts.unitselection.data.FeatureFileReader;
import marytts.util.math.Polynomial;

/**
 * @author marc
 * 
 */
public class F0ContourPolynomialDistanceMeasure implements DistanceMeasure {
	private float[][] contourCoeffs;

	public F0ContourPolynomialDistanceMeasure(FeatureFileReader contours) throws IOException {
		this.contourCoeffs = new float[contours.getNumberOfUnits()][];
		for (int i = 0; i < contourCoeffs.length; i++) {
			contourCoeffs[i] = contours.getFeatureVector(i).getContinuousFeatures();
		}
	}

	/**
	 * Compute the distance between the f0 contours corresponding to the given feature vectors. From the feature vectors, only
	 * their unit index number is used.
	 * 
	 * @param fv1
	 *            fv1
	 * @param fv2
	 *            fv2
	 * @return dist
	 * @see marytts.tools.voiceimport.traintrees.DistanceMeasure#distance(marytts.features.FeatureVector,
	 *      marytts.features.FeatureVector)
	 */
	public float distance(FeatureVector fv1, FeatureVector fv2) {
		float dist = (float) Polynomial.polynomialDistance(contourCoeffs[fv1.unitIndex], contourCoeffs[fv2.unitIndex]);
		return dist;
	}

	/**
	 * Compute the distance between the f0 contours corresponding to the given feature vectors. From the feature vectors, only
	 * their unit index number is used.
	 * 
	 * @param fv1
	 *            fv1
	 * @param fv2
	 *            fv2
	 * @return dist
	 * @see marytts.tools.voiceimport.traintrees.DistanceMeasure#distance(marytts.features.FeatureVector,
	 *      marytts.features.FeatureVector)
	 */
	public float squaredDistance(FeatureVector fv1, FeatureVector fv2) {
		float dist = (float) Polynomial.polynomialSquaredDistance(contourCoeffs[fv1.unitIndex], contourCoeffs[fv2.unitIndex]);
		return dist;
	}

	public float squaredDistance(FeatureVector fv, float[] polynomial) {
		float dist = (float) Polynomial.polynomialSquaredDistance(contourCoeffs[fv.unitIndex], polynomial);
		return dist;
	}

	/**
	 * Compute the mean polynomial from the given set of polynomials.
	 * 
	 * @param fvs
	 *            fvs
	 * @return mean
	 */
	public float[] computeMean(FeatureVector[] fvs) {
		float[][] contours = new float[fvs.length][];
		for (int i = 0; i < fvs.length; i++) {
			contours[i] = contourCoeffs[fvs[i].unitIndex];
		}
		float[] mean = Polynomial.mean(contours);
		return mean;
	}

	/**
	 * Compute the variance of the given set of feature vectors.
	 * 
	 * @param fvs
	 *            fvs
	 * @return variance
	 */
	public double computeVariance(FeatureVector[] fvs) {
		float[][] contours = new float[fvs.length][];
		for (int i = 0; i < fvs.length; i++) {
			contours[i] = contourCoeffs[fvs[i].unitIndex];
		}
		float[] mean = Polynomial.mean(contours);
		double variance = Polynomial.variance(contours, mean);
		return variance;
	}
}
