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

import marytts.data.SupportedSequenceType;
import marytts.data.Relation;
import marytts.data.utils.IntegerPair;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.modeling.features.FeatureRegistry;
import marytts.modeling.features.FeatureVector;
import marytts.modeling.features.FeatureDefinition;
import marytts.modeling.features.TargetFeatureComputer;
import marytts.modules.synthesis.Voice;
import marytts.util.dom.MaryDomUtils;

import marytts.modules.InternalModule;
import marytts.features.FeatureComputer;
import marytts.features.FeatureMap;

import marytts.io.XMLSerializer;
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.item.Item;


/**
 * Read a simple phone string and generate default acoustic parameters.
 *
 * @author Marc Schr&ouml;der
 */

public class TargetFeatureLister extends InternalModule {

	public TargetFeatureLister(MaryDataType outputType) throws Exception{
		super("TargetFeatureLister", MaryDataType.ALLOPHONES, outputType, null);
        FeatureComputer.initDefault();
	}

	public TargetFeatureLister() throws Exception {
		this(MaryDataType.TARGETFEATURES);
	}

	public MaryData process(MaryData d) throws Exception {
		Utterance utt = d.getData();

        FeatureComputer the_feature_computer = FeatureComputer.the_feature_computer;

        listTargetFeatures(the_feature_computer, utt);
		// Second, construct targets
		MaryData result = new MaryData(outputType(), d.getLocale(), utt);
		return result;
	}

	/**
	 * For the given elements and using the given feature computer, create a string representation of the target features.
	 *
	 * @param featureComputer
	 *            featureComputer
	 * @param segmentsAndBoundaries
	 *            segmentsAndBoundaries
	 * @return a multi-line string.
	 */
	public void listTargetFeatures(FeatureComputer the_feature_computer,
                                     Utterance utt) throws Exception
    {

        Sequence<FeatureMap> target_features = new Sequence<FeatureMap>();
        Sequence<Item> items = (Sequence<Item>) utt.getSequence(SupportedSequenceType.PHONE);
        Set<String> keys = null;
        int i=0;
        List<IntegerPair> list_pairs = new ArrayList<IntegerPair>();
        for (Item it: items)
        {
            FeatureMap map = the_feature_computer.process(utt, it);
            target_features.add(map);
            i++;
            list_pairs.add(new IntegerPair(i, i));
        }

        Relation rel_phone_features = new Relation(items, target_features, list_pairs);

        utt.addSequence(SupportedSequenceType.FEATURES, target_features);
        utt.setRelation(SupportedSequenceType.PHONE, SupportedSequenceType.FEATURES, rel_phone_features);
	}

	/**
	 * Return directly the targets, and set in each target its feature vector
	 *
	 * @param featureComputer
	 *            featureComputer
	 * @param segmentsAndBoundaries
	 *            segmentsAndBoundaries
	 * @return targets
	 */
	public List<FeatureMap> getListTargetFeatures(FeatureComputer the_feature_computer, Utterance utt, ArrayList<Item> items)
        throws Exception
    {
        List<FeatureMap> target_features = new ArrayList<FeatureMap>();

        for (Item it: items)
            target_features.add(the_feature_computer.process(utt, it));

		return target_features;
	}
}
