/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.signalproc.adaptation;

/**
 * Baseline class for voice conversion training
 * 
 * @author Oytun T&uuml;rk
 */
public class BaselineTrainer {

	public BaselinePreprocessor preprocessor;
	public BaselineFeatureExtractor featureExtractor;

	public BaselineTrainer(BaselinePreprocessor pp, BaselineFeatureExtractor fe) {
		preprocessor = new BaselinePreprocessor(pp);
		featureExtractor = new BaselineFeatureExtractor(fe);
	}

	// This baseline version does nothing. Please implement functionality in derived classes.
	public boolean checkParams() {
		return true;
	}

	// This baseline version just returns identical target indices for each source entry
	// Note that the returned map contains smallest number of items in source and target training sets
	public int[] getIndexedMapping(BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet) {
		int[] map = null;
		int numItems = Math.min(sourceTrainingSet.items.length, targetTrainingSet.items.length);
		if (numItems > 0) {
			map = new int[numItems];
			int i;

			for (i = 0; i < numItems; i++)
				map[i] = i;
		}

		return map;
	}
}
