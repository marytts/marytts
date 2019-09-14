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

import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureMap;
import marytts.features.FeatureComputer;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.item.Item;

/**
 * Base class for acoustic modeling; specific Models should extend this and
 * override methods as needed.
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
     * The name of the predicted acoustic feature, if any.
     */
    protected String featureName;

    /**
     * The names of the features used for prediction.
     */
    protected String predictionFeatureNames;

    /**
     * The producer of feature vectors
     */
    protected FeatureComputer featureComputer;

    /**
     * Model constructor
     *
     * @param dataStream
     *            data file for this Model
     */
    protected Model(InputStream dataStream) {
        this.dataStream = dataStream;
    }

    /**
     * Try to load this model and set the target feature computer appropriately.
     * This must be called from the constructor of subclasses, so that the
     * subclass implementation of loadDataFile() is visible.
     *
     * @throws MaryConfigurationException
     *             if the model cannot be set up properly.
     */
    protected final void load()
    // throws MaryConfigurationException, ClassNotFoundException,
    // InstantiationException, IllegalAccessException
    throws Exception {
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

    // FIXME
    protected final void setupFeatureComputer() throws Exception
        // MaryConfigurationException, ClassNotFoundException,
        // InstantiationException, IllegalAccessException
    {
        try {
            // FeatureComputer.initDefault();
            // featureComputer = FeatureComputer.the_feature_computer;
        } catch (IllegalArgumentException iae) {
        }
    }

    /**
     * Apply this Model to a list of items
     *
     * @param utt
     *            the utterance
     * @param seq_type
     *            the type of inpacted sequence
     * @param item_indexes
     *            the indexes of the inpacted items
     * @throws MaryConfigurationException
     *             if attribute values cannot be predicted because of an invalid
     *             voice configuration
     */
    public abstract void applyTo(Utterance utt, String seq_type,
                                 List<Integer> item_indexes)
    throws Exception;

    /**
     * For a list of items, return a list of features, where each FeatureMap is
     * constructed from the corresponding items.
     *
     * @param utt
     *            the utterance
     * @param seq_type
     *            the type of inpacted sequence
     * @param item_indexes
     *            the indexes of the inpacted items
     * @return List of features
     * @throws Exception
     *             [TODO]
     */
    protected List<FeatureMap> getTargets(Utterance utt, String seq_type,
                                          List<Integer> item_indexes)
    throws Exception {
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
