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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.machinelearning.SoP;
import marytts.unitselection.select.Target;
import marytts.util.MaryUtils;

/**
 * Model for predicting duration and F0 from SoP models
 * 
 * @author marcela
 *
 */
public class SoPModel extends Model {

	/**
	 * The SopModels map contains one SoP model for F0 and three SoP models duration: vowel, consonant and pause.
	 */
	private Map<String, SoP> sopModels;

	/**
	 * Model constructor
	 * 
	 * @param featureManager
	 *            the feature processor manager used to compute the symbolic features used for prediction
	 * @param voiceName
	 *            voiceName
	 * @param dataStream
	 *            data file containing the sop data
	 * @param targetAttributeName
	 *            attribute in MARYXML to predict
	 * @param targetAttributeFormat
	 *            print style
	 * @param featureName
	 *            not used in SoP model
	 * @param predictFrom
	 *            not used in SoP model
	 * @param applyTo
	 *            not used in SoP model
	 * 
	 * @throws MaryConfigurationException
	 *             if there are missing files.
	 */
	public SoPModel(FeatureProcessorManager featureManager, String voiceName, InputStream dataStream, String targetAttributeName,
			String targetAttributeFormat, String featureName, String predictFrom, String applyTo)
			throws MaryConfigurationException {
		super(featureManager, voiceName, dataStream, targetAttributeName, targetAttributeFormat, featureName, predictFrom,
				applyTo);
		load();
	}

	/**
	 * Load SoP data.
	 * 
	 * @throws IOException
	 *             if data can not be read.
	 */
	@Override
	protected void loadData() throws IOException {
		sopModels = new HashMap<String, SoP>();
		String nextLine, nextType;
		String strContext = "";
		Scanner s = null;
		try {
			s = new Scanner(new BufferedReader(new InputStreamReader(dataStream, "UTF-8")));

			// The first part contains the feature definition
			while (s.hasNext()) {
				nextLine = s.nextLine();
				if (nextLine.trim().equals(""))
					break;
				else
					strContext += nextLine + "\n";
			}
			// the featureDefinition is the same for vowel, consonant and Pause
			FeatureDefinition sopFeatureDefinition = new FeatureDefinition(new BufferedReader(new StringReader(strContext)),
					false);
			predictionFeatureNames = sopFeatureDefinition.getFeatureNames();

			while (s.hasNext()) {
				nextType = s.nextLine();
				nextLine = s.nextLine();

				if (nextType.startsWith("f0")) {
					sopModels.put("f0", new SoP(nextLine, sopFeatureDefinition));
				} else {
					sopModels.put(nextType, new SoP(nextLine, sopFeatureDefinition));
				}
			}
			s.close();

		} catch (Exception e) {
			throw new IOException("Error reading SoP data", e);
		}
	}

	/**
	 * Apply the SoP to a Target to get its predicted value
	 * 
	 * @param target
	 *            target from where to predict
	 * @return result predicted value
	 */
	@Override
	protected float evaluate(Target target) {
		float result = 0;

		if (targetAttributeName.contentEquals("f0")) {
			result = (float) sopModels.get("f0").interpret(target);
		} else {
			if (target.getAllophone().isVowel())
				result = (float) sopModels.get("vowel").interpret(target);
			else if (target.getAllophone().isConsonant())
				result = (float) sopModels.get("consonant").interpret(target);
			else if (target.getAllophone().isPause())
				result = (float) sopModels.get("pause").interpret(target);
			else {
				// ignore but complain
				MaryUtils.getLogger("SoPModel").warn("Warning: No SoP model for target " + target.toString());
			}
		}

		return result;
	}

}
