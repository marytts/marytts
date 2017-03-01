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

package marytts.modules.acoustic.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureMap;
import marytts.features.FeatureComputer;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.item.Item;
import marytts.data.SupportedSequenceType;

/**
 * Base class for acoustic modeling; specific Models should extend this and override methods as needed.
 *
 * @author steiner
 *
 */
public abstract class Model {

	/**
	 * The stream from which we will read our acoustic model.
	 */
	protected InputStream dataStream;


	/**
	 * The name of the predicted acoustic feature, if any. The feature processor that will be created from this will read the
	 * value from {@link #targetAttributeName}.
	 */
	protected String featureName;

	/**
	 * The names of the features used for prediction.
	 */
	protected String predictionFeatureNames;

	/**
	 * The producer of feature vectors for the features in {@link #predictionFeatureNames} as computed by the feature processors
	 * in {@link #featureManager}.
	 */
	protected FeatureComputer featureComputer;


	/**
	 * Model constructor
	 *
	 * @param featureManager
	 *            the feature processor manager used to compute the symbolic features used for prediction
	 * @param voiceName
	 *            name of the voice
	 * @param dataStream
	 *            data file for this Model
	 * @param targetAttributeName
	 *            attribute in MaryXML to predict
	 * @param targetAttributeFormat
	 *            printf-style format String to specify the attribute value, i.e. "%.3f" to round to 3 decimal places; "%s" by
	 *            default
	 * @param featureName
	 *            name of the custom continuous feature predicted by this model, or null
	 * @param predictFrom
	 *            key of Element Lists from which to predict values; "segments" by default
	 * @param applyTo
	 *            key of Element Lists to which to apply values; "segments" by default
	 */
	protected Model(InputStream dataStream)
    {
		this.dataStream = dataStream;
	}

	/**
	 * Try to load this model and set the target feature computer appropriately. This must be called from the constructor of
	 * subclasses, so that the subclass implementation of loadDataFile() is visible.
	 *
	 * @throws MaryConfigurationException
	 *             if the model cannot be set up properly.
	 */
	protected final void load()
        throws MaryConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException
    {
		try {
			loadData();
		} catch (IOException ioe) {
			throw new MaryConfigurationException("Cannot load model data from stream", ioe);
		}
		setupFeatureComputer();
	}

	/**
	 * Load dataFile for this model; only extension classes know how to do this
	 *
	 * @throws IOException
	 *             if any files cannot be properly read
	 * @throws MaryConfigurationException
	 *             if files can be read but contain problematic content
	 */
	protected abstract void loadData() throws IOException, MaryConfigurationException;

	protected final void setupFeatureComputer()
        throws MaryConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException
    {
		try {
            FeatureComputer.initDefault();
			featureComputer = FeatureComputer.the_feature_computer;
		} catch (IllegalArgumentException iae) {
		}
	}

	/**
	 * Apply this Model to a List of Elements, predicting from those same Elements
	 *
	 * @param elements
	 *            Elements for which to predict the values
	 * @throws MaryConfigurationException
	 *             if attribute values cannot be predicted because of an invalid voice configuration
	 */
	public abstract void applyTo(Utterance utt, SupportedSequenceType seq_type, List<Integer> item_indexes)
        throws Exception;

	/**
	 * For a list of <code>PHONE</code> elements, return a list of Targets, where each Target is constructed from the
	 * corresponding Element.
	 *
	 * @param elements
	 *            List of Elements
	 * @return List of Targets
	 */
	protected List<FeatureMap> getTargets(Utterance utt, SupportedSequenceType seq_type, List<Integer> item_indexes)
        throws Exception
    {
        Sequence<Item> seq = (Sequence<Item>) utt.getSequence(seq_type);
		List<FeatureMap> targets = new ArrayList<FeatureMap>(item_indexes.size());
		for (Integer idx : item_indexes) {
            Item item = seq.get(idx);
            // compute FeatureMaps for Targets:
			FeatureMap targetFeatureMap = featureComputer.process(utt, item);
			targets.add(targetFeatureMap); // this is critical!
		}
		return targets;
	}

	/**
	 * Evaluate model on a Target to obtain the target value as a float.
	 *
	 * @param target
	 *            target
	 * @return target value
	 * @throws Exception
	 *             if the target value cannot be predicted
	 */
	protected abstract float evaluate(FeatureMap target) throws Exception;

}
