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

import java.io.IOException;

import marytts.signalproc.adaptation.BaselineAdaptationSet;

/**
 * 
 * Baseline class for mapping different acoustic features
 * 
 * @author Oytun T&uuml;rk
 */
public abstract class WeightedCodebookFeatureMapper {
	// Implement functionality in derived classes!
	public abstract void learnMappingFrames(WeightedCodebookFile codebookFile, WeightedCodebookFeatureCollection fcol,
			BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int[] map) throws IOException;

	public abstract void learnMappingFrameGroups(WeightedCodebookFile codebookFile, WeightedCodebookFeatureCollection fcol,
			BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int[] map) throws IOException;

	public abstract void learnMappingLabels(WeightedCodebookFile codebookFile, WeightedCodebookFeatureCollection fcol,
			BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int[] map) throws IOException;

	public abstract void learnMappingLabelGroups(WeightedCodebookFile codebookFile, WeightedCodebookFeatureCollection fcol,
			BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int[] map) throws IOException;

	public abstract void learnMappingSpeech(WeightedCodebookFile codebookFile, WeightedCodebookFeatureCollection fcol,
			BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int[] map) throws IOException;
	//
}
