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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.TreeWalker;

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
		Voice voice = d.getDefaultVoice();
		String features = d.getOutputParams();
		TargetFeatureComputer featureComputer;
		if (voice != null) {
			featureComputer = FeatureRegistry.getTargetFeatureComputer(voice, features);
		} else {
			Locale locale = d.getLocale();
			assert locale != null;
			featureComputer = FeatureRegistry.getTargetFeatureComputer(locale, features);
		}
		assert featureComputer != null : "Cannot get a feature computer!";

        FeatureComputer the_feature_computer = FeatureComputer.the_feature_computer;
		Document doc = d.getDocument();

        XMLSerializer xml_ser = new XMLSerializer();
        Utterance utt = xml_ser.unpackDocument(d.getDocument());

		// Second, construct targets
		MaryData result = new MaryData(outputType(), d.getLocale());
		result.setPlainText(listTargetFeatures(the_feature_computer, utt));
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
	public String listTargetFeatures(FeatureComputer the_feature_computer,
                                     Utterance utt) throws Exception
    {

        List<FeatureMap> target_features = new ArrayList<FeatureMap>();

        Sequence<Item> items = (Sequence<Item>) utt.getSequence(SupportedSequenceType.PHONE);
        Set<String> keys = null;
        String tmp_out = "# ";
        for (Item it: items) {
            FeatureMap map = the_feature_computer.process(utt, it);

            if (keys == null) {
                keys = map.keySet();
                for (String k: keys)
                    tmp_out += k + "\t";
                tmp_out += "\n";

            }
            for (String k: keys) {
                String val = map.get(k).getStringValue();
                if (val == "")
                    val = "0"; // FIXME: hardcoded
                tmp_out += val + "\t";
            }
            tmp_out += "\n";
        }

        return tmp_out;
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
