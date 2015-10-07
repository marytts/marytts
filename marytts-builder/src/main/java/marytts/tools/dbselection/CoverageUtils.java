/**
 * Copyright 2011 DFKI GmbH.
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
package marytts.tools.dbselection;

import java.io.IOException;
import java.util.Locale;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.datatypes.MaryDataType;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.util.FeatureUtils;

/**
 * @author marc
 * 
 */
public class CoverageUtils {

	public static byte[] sentenceToFeatures(String sentence, Locale locale, String featureNames, boolean clientServer)
			throws MaryConfigurationException, SynthesisException, IOException {
		if (clientServer) {
			throw new UnsupportedOperationException("Not implemented");
		}
		MaryInterface mary = new LocalMaryInterface();
		mary.setLocale(locale);
		mary.setOutputType(MaryDataType.TARGETFEATURES.name());
		mary.setOutputTypeParams(featureNames);
		String targetFeatureData = mary.generateText(sentence);
		FeatureDefinition def = FeatureUtils.readFeatureDefinition(targetFeatureData);
		FeatureVector[] featureVectors = FeatureUtils.readFeatureVectors(targetFeatureData);
		byte[] data = toCoverageFeatures(featureNames, def, featureVectors);
		return data;
	}

	/**
	 * Convert the given feature vectors to the coverage features format, containing all byte features in a single byte array.
	 * 
	 * @param featureNames
	 *            featureNames
	 * @param def
	 *            def
	 * @param featureVectors
	 *            featureVectors
	 * @return data
	 */
	public static byte[] toCoverageFeatures(String featureNames, FeatureDefinition def, FeatureVector[] featureVectors) {
		String[] featureNameArray = featureNames.split(" ");
		int numFeatures = featureNameArray.length;
		byte[] data = new byte[featureVectors.length * featureNameArray.length];
		for (int f = 0; f < featureNameArray.length; f++) {
			int featureIndex = def.getFeatureIndex(featureNameArray[f]);
			for (int i = 0; i < featureVectors.length; i++) {
				int pos = i * numFeatures + f;
				data[pos] = featureVectors[i].getByteFeature(featureIndex);
			}
		}
		return data;
	}

}
