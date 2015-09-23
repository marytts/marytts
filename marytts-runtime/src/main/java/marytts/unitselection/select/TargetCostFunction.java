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
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.Unit;

/**
 * A target cost function for evaluating the goodness-of-fit of a given unit for a given target.
 * 
 * @author Marc Schr&ouml;der
 * 
 */
public interface TargetCostFunction {
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
	 *             if a configuration problem is detected while loading the data
	 */
	public void load(String featureFileName, InputStream weightsStream, FeatureProcessorManager featProc) throws IOException,
			MaryConfigurationException;

	/**
	 * Initialise the data needed to do a target cost computation.
	 * 
	 * @param featureFileReader
	 *            a reader for the file containing the unit features
	 * @param weightsStream
	 *            an optional weights file -- if non-null, contains feature weights that override the ones present in the feature
	 *            file.
	 * @param featProc
	 *            a feature processor manager which can provide feature processors to compute the features for a target at run
	 *            time
	 * @throws IOException
	 *             IOException
	 */
	public void load(FeatureFileReader featureFileReader, InputStream weightsStream, FeatureProcessorManager featProc)
			throws IOException;

	/**
	 * Compute the goodness-of-fit of a given unit for a given target.
	 * 
	 * @param target
	 *            target
	 * @param unit
	 *            unit
	 * @return a non-negative number; smaller values mean better fit, i.e. smaller cost.
	 */
	public double cost(Target target, Unit unit);

	/**
	 * Compute the features for a given target, and store them in the target.
	 * 
	 * @param target
	 *            the target for which to compute the features
	 * @see Target#getFeatureVector()
	 */
	public void computeTargetFeatures(Target target);

	/**
	 * Provide access to the Feature Definition used.
	 * 
	 * @return the feature definition object.
	 */
	public FeatureDefinition getFeatureDefinition();

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
	public String getFeature(Unit unit, String featureName);

	/**
	 * Get the target cost feature vector for the given unit.
	 * 
	 * @param unit
	 *            unit
	 * @return the feature vector
	 */
	public FeatureVector getFeatureVector(Unit unit);

	/**
	 * Get all feature vectors. This is useful for more efficient access.
	 * 
	 * @return the full array of feature vectors, or null if this method is not supported.
	 */
	public FeatureVector[] getFeatureVectors();

}
