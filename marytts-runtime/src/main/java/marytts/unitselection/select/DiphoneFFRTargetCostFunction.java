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

import java.io.IOException;
import java.io.InputStream;

import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureVector;
import marytts.unitselection.data.DiphoneUnit;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.HalfPhoneFeatureFileReader;
import marytts.unitselection.data.Unit;

public class DiphoneFFRTargetCostFunction implements TargetCostFunction {
	protected FFRTargetCostFunction tcfForHalfphones;

	public DiphoneFFRTargetCostFunction() {
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
	 */
	@Override
	public void load(String featureFileName, InputStream weightsStream, FeatureProcessorManager featProc) throws IOException,
			MaryConfigurationException {
		FeatureFileReader ffr = FeatureFileReader.getFeatureFileReader(featureFileName);
		load(ffr, weightsStream, featProc);
	}

	@Override
	public void load(FeatureFileReader ffr, InputStream weightsStream, FeatureProcessorManager featProc) throws IOException {
		if (ffr instanceof HalfPhoneFeatureFileReader) {
			tcfForHalfphones = new HalfPhoneFFRTargetCostFunction();
		} else {
			tcfForHalfphones = new FFRTargetCostFunction();
		}
		tcfForHalfphones.load(ffr, weightsStream, featProc);
	}

	/**
	 * Provide access to the Feature Definition used.
	 * 
	 * @return the feature definition object.
	 */
	public FeatureDefinition getFeatureDefinition() {
		return tcfForHalfphones.getFeatureDefinition();
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
		return tcfForHalfphones.getFeature(unit, featureName);
	}

	public FeatureVector getFeatureVector(Unit unit) {
		return tcfForHalfphones.featureVectors[unit.index];
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
		if (target instanceof HalfPhoneTarget)
			return tcfForHalfphones.cost(target, unit);
		if (!(target instanceof DiphoneTarget))
			throw new IllegalArgumentException("This target cost function can only be called for diphone and half-phone targets!");
		if (!(unit instanceof DiphoneUnit))
			throw new IllegalArgumentException("Diphone targets need diphone units!");
		DiphoneTarget dt = (DiphoneTarget) target;
		DiphoneUnit du = (DiphoneUnit) unit;
		return tcfForHalfphones.cost(dt.left, du.left) + tcfForHalfphones.cost(dt.right, du.right);
	}

	/**
	 * Compute the features for a given target, and store them in the target.
	 * 
	 * @param target
	 *            the target for which to compute the features
	 * @see Target#getFeatureVector()
	 */
	public void computeTargetFeatures(Target target) {
		if (!(target instanceof DiphoneTarget)) {
			tcfForHalfphones.computeTargetFeatures(target);
		} else {
			DiphoneTarget dt = (DiphoneTarget) target;
			tcfForHalfphones.computeTargetFeatures(dt.left);
			tcfForHalfphones.computeTargetFeatures(dt.right);

		}
	}

	public FeatureVector[] getFeatureVectors() {
		if (tcfForHalfphones != null) {
			return tcfForHalfphones.getFeatureVectors();
		}
		return null;
	}

}
