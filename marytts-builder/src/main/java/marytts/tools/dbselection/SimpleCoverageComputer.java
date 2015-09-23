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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Locale;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureRegistry;
import marytts.util.MaryUtils;

/**
 * This class takes a text file containing one sentence per line, and computes the phone, diphone and prosody coverage of the
 * corpus.
 * 
 * @author marc
 * 
 */
public class SimpleCoverageComputer {

	/**
	 * @param args
	 *            args
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {

		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));
		PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8"));
		Locale locale = MaryUtils.string2locale(args[2]);

		String featureNames = "phone next_phone selection_prosody";
		ArrayList<String> lines = new ArrayList<String>();
		String line;
		while ((line = in.readLine()) != null) {
			if (line.trim().isEmpty())
				continue;
			lines.add(line);
		}
		System.out.println("Computing coverage features for " + lines.size() + " sentences from " + args[0] + "...");
		byte[][] coverageFeatures = new byte[lines.size()][];
		for (int i = 0, max = lines.size(); i < max; i++) {
			coverageFeatures[i] = CoverageUtils.sentenceToFeatures(lines.get(i), locale, featureNames, false);
			if (i % 10 == 0) {
				System.out.print("\r" + i + "/" + max);
			}
		}
		System.out.println();
		System.out.println("Computing coverage...");
		CoverageFeatureProvider cfProvider = new InMemoryCFProvider(coverageFeatures, null);
		FeatureDefinition featDef = FeatureRegistry.getTargetFeatureComputer(locale, featureNames).getFeatureDefinition();
		CoverageDefinition coverageDefinition = new CoverageDefinition(featDef, cfProvider, null);
		coverageDefinition.initialiseCoverage();
		coverageDefinition.printTextCorpusStatistics(out);
		out.close();
		System.out.println("done -- see " + args[1] + " for results.");

	}
}
