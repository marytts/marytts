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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureVector;
import marytts.features.TargetFeatureComputer;
import marytts.server.MaryProperties;
import marytts.signalproc.display.Histogram;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.Unit;
import marytts.unitselection.weightingfunctions.WeightFunc;
import marytts.unitselection.weightingfunctions.WeightFunctionManager;
import marytts.util.MaryUtils;

public class FFRTargetCostFunction implements TargetCostFunction {
	protected WeightFunc[] weightFunction;
	protected TargetFeatureComputer targetFeatureComputer;
	protected FeatureVector[] featureVectors;
	protected FeatureDefinition featureDefinition;
	protected boolean[] weightsNonZero;

	protected boolean debugShowCostGraph = false;
	protected double[] cumulWeightedCosts = null;
	protected int nCostComputations = 0;

	public FFRTargetCostFunction() {
	}

	/**
	 * Compute the goodness-of-fit of a given unit for a given target.
	 * 
	 * @param target
	 *            target
	 * @param unit
	 *            unit
	 * @return a non-negative number; smaller values mean better fit, i.e. smaller cost.
	 */
	public double cost(Target target, Unit unit) {
		return cost(target, unit, featureDefinition, weightFunction);
	}

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
					if (!(a != a) && !(b != b)) {
						double myCost = weightFunctions[i - nDiscrete].cost(a, b);
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
	 * @return a non-negative number; smaller values mean better fit, i.e. smaller cost.
	 * @throws IllegalArgumentException
	 *             if featureName not available in featureDefinition
	 */
	public double featureCost(Target target, Unit unit, String featureName) {
		return featureCost(target, unit, featureName, featureDefinition, weightFunction);
	}

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
				if (!(a != a) && !(b != b)) {
					double myCost = weightFunctions[featureIndex - nDiscrete].cost(a, b);
					cost = weight * myCost;
					if (debugShowCostGraph) {
						cumulWeightedCosts[featureIndex] += weight * myCost;
					}
				} // and if it is NaN, simply compute no cost
			}
		}
		return cost;
	}

	/**
	 * Initialise the data needed to do a target cost computation.
	 * 
	 * @param featureFileName
	 *            name of a file containing the unit features
	 * @param weightsStream
	 *            an optional weights file -- if non-null, contains feature weights that override the ones present in the feature
	 *            file.
	 * @param featProc
	 *            a feature processor manager which can provide feature processors to compute the features for a target at run
	 *            time
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	@Override
	public void load(String featureFileName, InputStream weightsStream, FeatureProcessorManager featProc) throws IOException,
			MaryConfigurationException {
		FeatureFileReader ffr = FeatureFileReader.getFeatureFileReader(featureFileName);
		load(ffr, weightsStream, featProc);
	}

	@Override
	public void load(FeatureFileReader ffr, InputStream weightsStream, FeatureProcessorManager featProc) throws IOException {
		this.featureDefinition = ffr.getFeatureDefinition();
		this.featureVectors = ffr.getFeatureVectors();
		if (weightsStream != null) {
			MaryUtils.getLogger("TargetCostFeatures").debug("Overwriting target cost weights from file");
			// overwrite weights from file

			FeatureDefinition newWeights = new FeatureDefinition(
					new BufferedReader(new InputStreamReader(weightsStream, "UTF-8")), true);
			if (!newWeights.featureEquals(featureDefinition)) {
				throw new IOException("Weights file: feature definition incompatible with feature file");
			}
			featureDefinition = newWeights;
		}
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
		// TODO: If the target feature computer had direct access to the feature definition, it could do some consistency checking
		this.targetFeatureComputer = new TargetFeatureComputer(featProc, featureDefinition.getFeatureNames());

		rememberWhichWeightsAreNonZero();

		if (MaryProperties.getBoolean("debug.show.cost.graph")) {
			debugShowCostGraph = true;
			cumulWeightedCosts = new double[featureDefinition.getNumberOfFeatures()];
			TargetCostReporter tcr2 = new TargetCostReporter(cumulWeightedCosts);
			tcr2.showInJFrame("Average weighted target costs", false, false);
			tcr2.start();
		}
	}

	protected void rememberWhichWeightsAreNonZero() {
		// remember which weights are non-zero
		weightsNonZero = new boolean[featureDefinition.getNumberOfFeatures()];
		for (int i = 0, n = featureDefinition.getNumberOfFeatures(); i < n; i++) {
			weightsNonZero[i] = (featureDefinition.getWeight(i) > 0);
		}
	}

	/**
	 * Compute the features for a given target, and store them in the target.
	 * 
	 * @param target
	 *            the target for which to compute the features
	 * @see Target#getFeatureVector()
	 */
	public void computeTargetFeatures(Target target) {
		FeatureVector fv = targetFeatureComputer.computeFeatureVector(target);
		target.setFeatureVector(fv);
	}

	/**
	 * Look up the features for a given unit.
	 * 
	 * @param unit
	 *            a unit in the database
	 * @return the FeatureVector for target cost computation associated to this unit
	 */
	public FeatureVector getFeatureVector(Unit unit) {
		return featureVectors[unit.index];
	}

	/**
	 * Get the string representation of the feature value associated with the given unit
	 * 
	 * @param unit
	 *            the unit whose feature value is requested
	 * @param featureName
	 *            name of the feature requested
	 * @return a string representation of the feature value
	 * @throws IllegalArgumentException
	 *             if featureName is not a known feature
	 */
	public String getFeature(Unit unit, String featureName) {
		int featureIndex = featureDefinition.getFeatureIndex(featureName);
		if (featureDefinition.isByteFeature(featureIndex)) {
			byte value = featureVectors[unit.index].getByteFeature(featureIndex);
			return featureDefinition.getFeatureValueAsString(featureIndex, value);
		} else if (featureDefinition.isShortFeature(featureIndex)) {
			short value = featureVectors[unit.index].getShortFeature(featureIndex);
			return featureDefinition.getFeatureValueAsString(featureIndex, value);
		} else { // continuous -- return float as string
			float value = featureVectors[unit.index].getContinuousFeature(featureIndex);
			return String.valueOf(value);
		}
	}

	public FeatureDefinition getFeatureDefinition() {
		return featureDefinition;
	}

	public class TargetCostReporter extends Histogram {
		private double[] data;
		private int lastN = 0;

		public TargetCostReporter(double[] data) {
			super(0, 1, data);
			this.data = data;
		}

		public void start() {
			new Thread() {
				public void run() {
					while (isVisible()) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException ie) {
						}
						updateGraph();
					}
				}
			}.start();
		}

		protected void updateGraph() {
			if (nCostComputations == lastN)
				return;
			lastN = nCostComputations;
			double[] newCosts = new double[data.length];
			for (int i = 0; i < newCosts.length; i++) {
				newCosts[i] = data[i] / nCostComputations;
			}
			updateData(0, 1, newCosts);
			repaint();
		}
	}

	public FeatureVector[] getFeatureVectors() {
		return featureVectors;
	}

}
