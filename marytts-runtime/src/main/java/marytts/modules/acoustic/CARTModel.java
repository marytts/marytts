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

package marytts.modules.acoustic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import marytts.cart.DirectedGraph;
import marytts.cart.io.DirectedGraphReader;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureProcessorManager;
import marytts.unitselection.select.Target;

/**
 * Model for applying a CART to a list of Targets
 * 
 * @author steiner
 * 
 */
public class CARTModel extends Model {
	private DirectedGraph cart;

	public CARTModel(FeatureProcessorManager featureManager, String voiceName, InputStream dataStream,
			String targetAttributeName, String targetAttributeFormat, String featureName, String predictFrom, String applyTo)
			throws MaryConfigurationException {
		super(featureManager, voiceName, dataStream, targetAttributeName, targetAttributeFormat, featureName, predictFrom,
				applyTo);
		load();
	}

	/**
	 * Load CART from file for this Model
	 */
	@Override
	protected void loadData() throws IOException, MaryConfigurationException {
		cart = new DirectedGraphReader().load(dataStream);
		try {
			predictionFeatureNames = cart.getFeatureDefinition().getFeatureNames();
		} catch (NullPointerException e) {
			throw new IOException("Could not get FeatureDefinition from CART", e);
		}
		if (predictionFeatureNames.length() == 0) { // isEmpty
			throw new IOException("Could not get prediction feature names");
		}
	}

	/**
	 * Apply the CART to a Target to get its predicted value
	 */
	@Override
	protected float evaluate(Target target) throws Exception {
		assert target != null;

		float[] result = null;
		try {
			result = (float[]) cart.interpret(target);
		} catch (IllegalArgumentException e) {
			throw new Exception("Could not interpret target '" + target + "'", e);
		}

		float value = 0;
		try {
			value = result[1]; // assuming result is [stdev, val]
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new Exception("Could not handle predicted value: '" + value + "'", e);
		}
		return value;
	}
}
