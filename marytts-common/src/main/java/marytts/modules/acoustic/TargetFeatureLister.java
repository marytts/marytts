/**
 * Copyright 2000-2006 DFKI GmbH.
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

import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import marytts.config.MaryConfiguration;

import marytts.data.SupportedSequenceType;
import marytts.data.Relation;
import marytts.data.utils.IntegerPair;
import marytts.data.Utterance;

import marytts.modules.MaryModule;
import marytts.features.FeatureComputer;
import marytts.features.FeatureMap;
import marytts.io.serializer.XMLSerializer;
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.item.Item;

import marytts.MaryException;
import marytts.exceptions.MaryConfigurationException;

import org.apache.logging.log4j.core.Appender;
/**
 * The module which compute the features
 *
 * @author Marc Schr&ouml;der
 */
public class TargetFeatureLister extends MaryModule {

    FeatureComputer feature_computer;
    /**
     * Default constructor
     *
     */
    public TargetFeatureLister() throws Exception {
        super();

        feature_computer = new FeatureComputer();
    }

    public void checkStartup() throws MaryConfigurationException {
    }

    /**
     *  Check if the input contains all the information needed to be
     *  processed by the module.
     *
     *  @param utt the input utterance
     *  @throws MaryException which indicates what is missing if something is missing
     */
    public void checkInput(Utterance utt) throws MaryException {
        if (!utt.hasSequence(SupportedSequenceType.PHONE)) {
            throw new MaryException("Phone sequence is missing", null);
        }
    }

    /**
     * The process method which take a Utterance object in parameter, compute the
     * features of the utterance referenced in the parameter and return a new
     * Utterance object which contains the reference to the updated utterance.
     *
     * @param d
     *            the input Utterance object
     * @return the Utterance object with the updated reference
     * @throws Exception
     *             [TODO]
     */
    public Utterance process(Utterance utt, MaryConfiguration configuration) throws Exception {
	utt.setFeatureNames(feature_computer.listFeatures());
        Sequence<FeatureMap> target_features = new Sequence<FeatureMap>();
        Sequence<Item> items = (Sequence<Item>) utt.getSequence(SupportedSequenceType.PHONE);
        Set<String> keys = null;
        int i = 0;
        List<IntegerPair> list_pairs = new ArrayList<IntegerPair>();
        for (Item it : items) {
            FeatureMap map = feature_computer.process(utt, it);
            target_features.add(map);
            list_pairs.add(new IntegerPair(i, i));
            i++;
        }

        Relation rel_phone_features = new Relation(items, target_features, list_pairs);

        utt.addSequence(SupportedSequenceType.FEATURES, target_features);
	utt.setRelation(SupportedSequenceType.PHONE, SupportedSequenceType.FEATURES, rel_phone_features);
	return utt;
    }

    /**
     * Return directly the targets, and set in each target its feature vector
     *
     * @param the_feature_computer
     *            the feature computer used
     * @param utt
     *            the utterance used to compute the features
     * @param items
     *            the items whose features are going to be computed
     * @return a list of map of features corresponding of the given items
     * @throws Exception
     *             [TODO]
     */
    public List<FeatureMap> getListTargetFeatures(FeatureComputer the_feature_computer, Utterance utt,
            ArrayList<Item> items) throws Exception {
        List<FeatureMap> target_features = new ArrayList<FeatureMap>();
        for (Item it : items) {
            target_features.add(feature_computer.process(utt, it));
        }

        return target_features;
    }


    public void setFeatureProcessors(ArrayList<String> features) throws MaryConfigurationException {
	for (String feature: features)
	    feature_computer.addFeatureProcessor(feature, feature);
    }

    public void setLevelProcessors(ArrayList<String> levels) throws MaryConfigurationException {
	for (String level: levels)
	    feature_computer.addLevelProcessor(level, level);

    }

    public void setContextProcessors(ArrayList<String> contexts) throws MaryConfigurationException {
	for (String context: contexts)
	    feature_computer.addContextProcessor(context, context);
    }
}
