/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.unitselection.select;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.unitselection.data.Unit;
import marytts.unitselection.weightingfunctions.WeightFunc;
import marytts.unitselection.weightingfunctions.WeightFunctionManager;
import marytts.vocalizations.VocalizationFeatureFileReader;

/**
 * FFRTargetCostFunction for vocalization selection
 * 
 * @author sathish pammi
 * 
 */
public class VocalizationFFRTargetCostFunction extends FFRTargetCostFunction {

	private int MEANING_RATING_RANGE = 5; // the range of meaning rating scale

	public VocalizationFFRTargetCostFunction(VocalizationFeatureFileReader ffr) {
		this(ffr, ffr.getFeatureDefinition());
	}

	public VocalizationFFRTargetCostFunction(VocalizationFeatureFileReader ffr, FeatureDefinition fDef) {
		load(ffr, fDef);
	}

	/**
	 * load feature file reader and feature definition for a cost function
	 * 
	 * @param ffr
	 *            feature file reader
	 * @param fDef
	 *            feature definition
	 */
	private void load(VocalizationFeatureFileReader ffr, FeatureDefinition fDef) {
		this.featureVectors = ffr.featureVectorMapping(fDef);
		this.featureDefinition = fDef;

		weightFunction = new WeightFunc[featureDefinition.getNumberOfContinuousFeatures()];
		WeightFunctionManager wfm = new WeightFunctionManager();
		int nDiscreteFeatures = featureDefinition.getNumberOfByteFeatures() + featureDefinition.getNumberOfShortFeatures();
		for (int i = 0; i < weightFunction.length; i++) {
			String weightFunctionName = featureDefinition.getWeightFunctionName(nDiscreteFeatures + i);
			if ("".equals(weightFunctionName))
				weightFunction[i] = wfm.getWeightFunction("linear");
			else
				weightFunction[i] = wfm.getWeightFunction(weightFunctionName);
		}

		rememberWhichWeightsAreNonZero();
	}

	/**
	 * Compute the goodness-of-fit of a given unit for a given target
	 * 
	 * @param target
	 *            target unit
	 * @param unit
	 *            candidate unit
	 * @param weights
	 *            FeatureDefinition
	 * @param weightFunctions
	 *            array of WeightFunctions
	 * @return a non-negative number; smaller values mean better fit, i.e. smaller cost.
	 * @throws IllegalArgumentException
	 *             if featureName not available in featureDefinition
	 */
	protected double cost(Target target, Unit unit, FeatureDefinition weights, WeightFunc[] weightFunctions) {
		nCostComputations++; // for debug
		FeatureVector targetFeatures = target.getFeatureVector();
		assert targetFeatures != null : "Target " + target + " does not have pre-computed feature vector";
		FeatureVector unitFeatures = featureVectors[unit.index];
		int nBytes = targetFeatures.byteValuedDiscreteFeatures.length;
		int nShorts = targetFeatures.shortValuedDiscreteFeatures.length;
		int nFloats = targetFeatures.continuousFeatures.length;
		assert nBytes == unitFeatures.byteValuedDiscreteFeatures.length;
		assert nShorts == unitFeatures.shortValuedDiscreteFeatures.length;
		assert nFloats == unitFeatures.continuousFeatures.length;

		float[] weightVector = weights.getFeatureWeights();
		// Now the actual computation
		double cost = 0;
		// byte-valued features:
		if (nBytes > 0) {
			for (int i = 0; i < nBytes; i++) {
				if (weightsNonZero[i]) {
					float weight = weightVector[i];
					if (featureDefinition.hasSimilarityMatrix(i)) {
						byte targetFeatValueIndex = targetFeatures.byteValuedDiscreteFeatures[i];
						byte unitFeatValueIndex = unitFeatures.byteValuedDiscreteFeatures[i];
						float similarity = featureDefinition.getSimilarity(i, unitFeatValueIndex, targetFeatValueIndex);
						cost += similarity * weight;
						if (debugShowCostGraph)
							cumulWeightedCosts[i] += similarity * weight;
					} else if (targetFeatures.byteValuedDiscreteFeatures[i] != unitFeatures.byteValuedDiscreteFeatures[i]) {
						cost += weight;
						if (debugShowCostGraph)
							cumulWeightedCosts[i] += weight;
					}
				}
			}
		}
		// short-valued features:
		if (nShorts > 0) {
			for (int i = nBytes, n = nBytes + nShorts; i < n; i++) {
				if (weightsNonZero[i]) {
					float weight = weightVector[i];
					// if (targetFeatures.getShortFeature(i) != unitFeatures.getShortFeature(i)) {
					if (targetFeatures.shortValuedDiscreteFeatures[i - nBytes] != unitFeatures.shortValuedDiscreteFeatures[i
							- nBytes]) {
						cost += weight;
						if (debugShowCostGraph)
							cumulWeightedCosts[i] += weight;
					}
				}
			}
		}
		// continuous features:
		if (nFloats > 0) {
			int nDiscrete = nBytes + nShorts;
			for (int i = nDiscrete, n = nDiscrete + nFloats; i < n; i++) {
				if (weightsNonZero[i]) {
					float weight = weightVector[i];
					// float a = targetFeatures.getContinuousFeature(i);
					float a = targetFeatures.continuousFeatures[i - nDiscrete];
					// float b = unitFeatures.getContinuousFeature(i);
					float b = unitFeatures.continuousFeatures[i - nDiscrete];
					// if (!Float.isNaN(a) && !Float.isNaN(b)) {
					// Implementation of isNaN() is: (v != v).
					if (!(a != a)) {

						double myCost;
						if (!(b != b)) {
							myCost = weightFunctions[i - nDiscrete].cost(a, b);
						} else {
							myCost = this.MEANING_RATING_RANGE;
						}

						cost += weight * myCost;
						if (debugShowCostGraph) {
							cumulWeightedCosts[i] += weight * myCost;
						}
					} // and if it is NaN, simply compute no cost
				}
			}
		}
		return cost;
	}

	/**
	 * Compute the goodness-of-fit between given unit and given target for a given feature
	 * 
	 * @param target
	 *            target unit
	 * @param unit
	 *            candidate unit
	 * @param featureName
	 *            feature name
	 * @param weights
	 *            FeatureDefinition
	 * @param weightFunctions
	 *            array of WeightFunctions
	 * @return a non-negative number; smaller values mean better fit, i.e. smaller cost.
	 * @throws IllegalArgumentException
	 *             if featureName not available in featureDefinition
	 */
	protected double featureCost(Target target, Unit unit, String featureName, FeatureDefinition weights,
			WeightFunc[] weightFunctions) {
		if (!this.featureDefinition.hasFeature(featureName)) {
			throw new IllegalArgumentException("this feature does not exists in feature definition");
		}

		FeatureVector targetFeatures = target.getFeatureVector();
		assert targetFeatures != null : "Target " + target + " does not have pre-computed feature vector";
		FeatureVector unitFeatures = featureVectors[unit.index];
		int nBytes = targetFeatures.byteValuedDiscreteFeatures.length;
		int nShorts = targetFeatures.shortValuedDiscreteFeatures.length;
		int nFloats = targetFeatures.continuousFeatures.length;
		assert nBytes == unitFeatures.byteValuedDiscreteFeatures.length;
		assert nShorts == unitFeatures.shortValuedDiscreteFeatures.length;
		assert nFloats == unitFeatures.continuousFeatures.length;

		int featureIndex = this.featureDefinition.getFeatureIndex(featureName);
		float[] weightVector = weights.getFeatureWeights();
		double cost = 0;

		if (featureIndex < nBytes) {
			if (weightsNonZero[featureIndex]) {
				float weight = weightVector[featureIndex];
				if (featureDefinition.hasSimilarityMatrix(featureIndex)) {
					byte targetFeatValueIndex = targetFeatures.byteValuedDiscreteFeatures[featureIndex];
					byte unitFeatValueIndex = unitFeatures.byteValuedDiscreteFeatures[featureIndex];
					float similarity = featureDefinition.getSimilarity(featureIndex, unitFeatValueIndex, targetFeatValueIndex);
					cost = similarity * weight;
					if (debugShowCostGraph)
						cumulWeightedCosts[featureIndex] += similarity * weight;
				} else if (targetFeatures.byteValuedDiscreteFeatures[featureIndex] != unitFeatures.byteValuedDiscreteFeatures[featureIndex]) {
					cost = weight;
					if (debugShowCostGraph)
						cumulWeightedCosts[featureIndex] += weight;
				}
			}
		} else if (featureIndex < nShorts + nBytes) {
			if (weightsNonZero[featureIndex]) {
				float weight = weightVector[featureIndex];
				// if (targetFeatures.getShortFeature(i) != unitFeatures.getShortFeature(i)) {
				if (targetFeatures.shortValuedDiscreteFeatures[featureIndex - nBytes] != unitFeatures.shortValuedDiscreteFeatures[featureIndex
						- nBytes]) {
					cost = weight;
					if (debugShowCostGraph)
						cumulWeightedCosts[featureIndex] += weight;
				}
			}
		} else {
			int nDiscrete = nBytes + nShorts;
			if (weightsNonZero[featureIndex]) {
				float weight = weightVector[featureIndex];
				// float a = targetFeatures.getContinuousFeature(i);
				float a = targetFeatures.continuousFeatures[featureIndex - nDiscrete];
				// float b = unitFeatures.getContinuousFeature(i);
				float b = unitFeatures.continuousFeatures[featureIndex - nDiscrete];
				// if (!Float.isNaN(a) && !Float.isNaN(b)) {
				// Implementation of isNaN() is: (v != v).
				if (!(a != a)) {

					double myCost;
					if (!(b != b)) {
						myCost = weightFunctions[featureIndex - nDiscrete].cost(a, b);
					} else {
						myCost = this.MEANING_RATING_RANGE;
					}

					cost = weight * myCost;
					if (debugShowCostGraph) {
						cumulWeightedCosts[featureIndex] += weight * myCost;
					}
				} // and if it is NaN, simply compute no cost
			}
		}
		return cost;
	}
}
