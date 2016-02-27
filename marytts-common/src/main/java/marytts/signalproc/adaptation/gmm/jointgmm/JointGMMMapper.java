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
package marytts.signalproc.adaptation.gmm.jointgmm;

import java.util.Arrays;

import marytts.signalproc.adaptation.gmm.GMMMapper;
import marytts.signalproc.adaptation.gmm.GMMMatch;
import marytts.util.math.MathUtils;

/**
 * 
 * Implements joint-GMM based speaker feature transformation from source acoustic space to target acoustic space
 * 
 * @author Oytun T&uuml;rk
 */
public class JointGMMMapper extends GMMMapper {

	public JointGMMMapper() {

	}

	// Weights should sum up to unity
	public GMMMatch transform(double[] inputLsfs, JointGMMSet jointGMMSet, double[] weights,
			boolean isVocalTractMatchUsingTargetModel) {
		JointGMMMatch jointGMMMatch = new JointGMMMatch(inputLsfs.length);
		JointGMMMatch tmpGMMMatch = new JointGMMMatch(inputLsfs.length);

		int i, n;
		Arrays.fill(jointGMMMatch.mappedSourceFeatures, 0.0);
		Arrays.fill(jointGMMMatch.outputFeatures, 0.0);

		for (i = 0; i < jointGMMSet.gmms.length; i++) {
			if (jointGMMSet.gmms[i] == null)
				weights[i] = 0.0;
		}

		weights = MathUtils.normalizeToSumUpTo(weights, 1.0);

		for (i = 0; i < jointGMMSet.gmms.length; i++) {
			if (jointGMMSet.gmms[i] != null && weights[i] > 0.0) {
				tmpGMMMatch = transform(inputLsfs, jointGMMSet.gmms[i], isVocalTractMatchUsingTargetModel);

				for (n = 0; n < inputLsfs.length; n++) {
					jointGMMMatch.mappedSourceFeatures[n] += weights[i] * tmpGMMMatch.mappedSourceFeatures[n];
					jointGMMMatch.outputFeatures[n] += weights[i] * tmpGMMMatch.outputFeatures[n];
				}
			}

		}

		return jointGMMMatch;
	}

	public JointGMMMatch transform(double[] inputLsfs, JointGMM jointGMM, boolean isVocalTractMatchUsingTargetModel) {
		JointGMMMatch jointGMMMatch = new JointGMMMatch(inputLsfs.length);

		int i, n;
		double[] h = new double[jointGMM.source.totalComponents];
		double totalP = 0.0;
		for (i = 0; i < jointGMM.source.totalComponents; i++) {
			h[i] = jointGMM.source.components[i].probability(inputLsfs);
			totalP += h[i];
		}

		for (i = 0; i < jointGMM.source.totalComponents; i++)
			h[i] = h[i] / totalP;

		if (jointGMM.covarianceTerms.isDiagonalCovariance) // Diagonal covariance, covariance terms are just vectors
		{
			Arrays.fill(jointGMMMatch.mappedSourceFeatures, 0.0);
			Arrays.fill(jointGMMMatch.outputFeatures, 0.0);

			for (n = 0; n < inputLsfs.length; n++) {
				for (i = 0; i < jointGMM.source.totalComponents; i++) {
					jointGMMMatch.mappedSourceFeatures[n] += h[i] * jointGMM.source.components[i].meanVector[n];
					jointGMMMatch.outputFeatures[n] += h[i]
							* (jointGMM.targetMeans.components[i].meanVector[n] + jointGMM.covarianceTerms.components[i].covMatrix[0][n]
									* (inputLsfs[n] - jointGMM.source.components[i].meanVector[n]));
				}
			}
		} else // Full covariance
		{
			Arrays.fill(jointGMMMatch.mappedSourceFeatures, 0.0);
			Arrays.fill(jointGMMMatch.outputFeatures, 0.0);

			double[] tmpMappedSourceLsfs = new double[inputLsfs.length];
			double[] tmpOutputLsfs = new double[inputLsfs.length];

			double[] inputMeanNormalized;
			double[] covarianceTransformed;
			double[] targetMeanAdded;

			for (i = 0; i < jointGMM.source.totalComponents; i++) {
				tmpMappedSourceLsfs = MathUtils.multiply(jointGMM.source.components[i].meanVector, h[i]);

				inputMeanNormalized = MathUtils.subtract(inputLsfs, jointGMM.source.components[i].meanVector);
				covarianceTransformed = MathUtils.matrixProduct(jointGMM.covarianceTerms.components[i].covMatrix,
						inputMeanNormalized);
				targetMeanAdded = MathUtils.add(jointGMM.targetMeans.components[i].meanVector, covarianceTransformed);
				tmpOutputLsfs = MathUtils.multiply(targetMeanAdded, h[i]);

				for (n = 0; n < inputLsfs.length; n++) {
					jointGMMMatch.mappedSourceFeatures[n] += tmpMappedSourceLsfs[n];
					jointGMMMatch.outputFeatures[n] += tmpOutputLsfs[n];
				}
			}
		}

		return jointGMMMatch;
	}
}
