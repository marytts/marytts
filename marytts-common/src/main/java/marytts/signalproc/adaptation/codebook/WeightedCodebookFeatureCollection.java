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
package marytts.signalproc.adaptation.codebook;

import marytts.signalproc.adaptation.BaselineFeatureCollection;
import marytts.util.string.StringUtils;

/**
 * 
 * A wrapper class for indexed binary files of acoustic feature sets
 * 
 * @author Oytun T&uuml;rk
 */
public class WeightedCodebookFeatureCollection extends BaselineFeatureCollection {
	public String[] indexMapFiles;

	public WeightedCodebookFeatureCollection(WeightedCodebookTrainerParams params, int numFiles) {
		if (numFiles > 0)
			indexMapFiles = StringUtils.indexedNameGenerator(params.trainingBaseFolder + params.codebookHeader.sourceTag + "_"
					+ params.codebookHeader.targetTag + "_", numFiles, 1, "", params.indexMapFileExtension);
		else
			indexMapFiles = null;
	}

}
