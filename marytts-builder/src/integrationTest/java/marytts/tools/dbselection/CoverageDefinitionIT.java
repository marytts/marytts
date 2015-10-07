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

import java.util.Locale;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureRegistry;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author marc
 *
 */
public class CoverageDefinitionIT {

	private static String[] testSentences = new String[] { "Author of the danger trail, Philip Steels, etc.",
			"Not at this particular case, Tom, apologized Whittemore.",
			"For the twentieth time that evening the two men shook hands.", "Lord, but I'm glad to see you again, Phil.",
			"Will we ever forget it.", "God bless 'em, I hope I'll go on seeing them forever.",
			"And you always want to see it in the superlative degree.", "Gad, your letter came just in time.",
			"He turned sharply, and faced Gregson across the table.",
			"I'm playing a single hand in what looks like a losing game." };

	private static byte[][] coverageFeatures = new byte[testSentences.length][];
	private static Locale locale = Locale.US;
	private static String featureNames = "phone next_phone selection_prosody";
	private static FeatureDefinition featDef;

	@BeforeClass
	public static void setup() throws Exception {
		for (int i = 0; i < testSentences.length; i++) {
			coverageFeatures[i] = CoverageUtils.sentenceToFeatures(testSentences[i], locale, featureNames, false);
		}
		featDef = FeatureRegistry.getTargetFeatureComputer(locale, featureNames).getFeatureDefinition();
	}

	@Test
	public void testAll() throws Exception {
		CoverageFeatureProvider cfProvider = new InMemoryCFProvider(coverageFeatures, null);
		CoverageDefinition cd = new CoverageDefinition(featDef, cfProvider, null);
		cd.initialiseCoverage();
		System.out.println(cd.getCorpusStatistics());
	}
}
