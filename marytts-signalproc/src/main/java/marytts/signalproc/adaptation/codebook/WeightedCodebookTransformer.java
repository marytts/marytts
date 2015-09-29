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

import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.adaptation.BaselineAdaptationSet;
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.BaselinePostprocessor;
import marytts.signalproc.adaptation.BaselinePreprocessor;
import marytts.signalproc.adaptation.BaselineTransformer;
import marytts.signalproc.adaptation.BaselineTransformerParams;
import marytts.util.io.BasenameList;
import marytts.util.string.StringUtils;

/**
 * 
 * Weighted codebook transformation class
 * 
 * @author Oytun T&uuml;rk
 */
public class WeightedCodebookTransformer extends BaselineTransformer {

	public WeightedCodebookTransformerParams params;

	public WeightedCodebookTransformer(BaselinePreprocessor pp, BaselineFeatureExtractor fe, BaselinePostprocessor po,
			WeightedCodebookTransformerParams pa) {
		super(pp, fe, po, (BaselineTransformerParams) pa);

		params = new WeightedCodebookTransformerParams(pa);
	}

	public boolean checkParams() throws IOException {
		return super.checkParams();
	}

	public void run() throws IOException, UnsupportedAudioFileException {
		if (checkParams()) {
			BaselineAdaptationSet inputSet = getInputSet(params.inputFolder);
			if (inputSet == null)
				System.out.println("No input files found in " + params.inputFolder);
			else {
				BaselineAdaptationSet outputSet = getOutputSet(inputSet, params.outputFolder);

				transform(inputSet, outputSet);
			}
		}
	}

	// Create list of input files
	public BaselineAdaptationSet getInputSet(String inputFolder) {
		BasenameList b = new BasenameList(inputFolder, BaselineAdaptationSet.WAV_EXTENSION_DEFAULT);

		BaselineAdaptationSet inputSet = new BaselineAdaptationSet(b.getListAsVector().size());

		for (int i = 0; i < inputSet.items.length; i++)
			inputSet.items[i].setFromWavFilename(inputFolder + b.getName(i) + BaselineAdaptationSet.WAV_EXTENSION_DEFAULT);

		return inputSet;
	}

	//

	// Create list of output files using input set
	public BaselineAdaptationSet getOutputSet(BaselineAdaptationSet inputSet, String outputFolder) {
		BaselineAdaptationSet outputSet = null;

		outputFolder = StringUtils.checkLastSlash(outputFolder);

		if (inputSet != null && inputSet.items != null) {
			outputSet = new BaselineAdaptationSet(inputSet.items.length);

			for (int i = 0; i < inputSet.items.length; i++)
				outputSet.items[i].audioFile = outputFolder + StringUtils.getFileName(inputSet.items[i].audioFile) + "_output"
						+ BaselineAdaptationSet.WAV_EXTENSION_DEFAULT;
		}

		return outputSet;
	}
	//
}
