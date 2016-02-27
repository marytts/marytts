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

import java.io.IOException;
import java.util.Arrays;

import marytts.signalproc.adaptation.gmm.GMMMatch;
import marytts.signalproc.adaptation.gmm.jointgmm.JointGMMMapper;
import marytts.signalproc.adaptation.gmm.jointgmm.JointGMMMatch;
import marytts.signalproc.adaptation.gmm.jointgmm.JointGMMSet;
import marytts.signalproc.adaptation.gmm.jointgmm.JointGMMTransformerParams;
import marytts.signalproc.analysis.Mfccs;
import marytts.util.io.FileUtils;

/**
 * This class transforms MFCCs to MFCCs
 * 
 * @author Oytun T&uuml;rk
 */
public class MfccAdapter {

	protected Mfccs inputMfccs;
	public boolean bSilent;
	private BaselineTransformerParams baseParams;
	protected String outputFile;
	protected int numfrm;

	public MfccAdapter(BaselineAdaptationItem inputItem, String strOutputFile, JointGMMTransformerParams jgmmParamsIn) {
		baseParams = new JointGMMTransformerParams(jgmmParamsIn);

		init(inputItem, strOutputFile);
	}

	public void init(BaselineAdaptationItem inputItem, String strOutputFile) {
		outputFile = null;

		boolean bContinue = true;

		if (!FileUtils.exists(inputItem.mfccFile)) {
			System.out.println("Error! MFCC file " + inputItem.mfccFile + " not found.");
			bContinue = false;
		}

		if (strOutputFile == null || strOutputFile == "") {
			System.out.println("Invalid output file...");
			bContinue = false;
		}

		numfrm = 0;
		if (bContinue) {
			inputMfccs = new Mfccs(inputItem.mfccFile);
			numfrm = inputMfccs.mfccs.length;
			outputFile = strOutputFile;
		}
	}

	public void transformOnline(VocalTractTransformationFunction vtMapper, VocalTractTransformationData vtData) {
		int i;

		for (i = 0; i < numfrm; i++)
			inputMfccs.mfccs[i] = processFrame(inputMfccs.mfccs[i], vtMapper, vtData);

		try {
			Mfccs.writeRawMfccFile(inputMfccs.mfccs, outputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// Voice conversion version
	public double[] processFrame(double[] frameMfccs, VocalTractTransformationFunction mapper, VocalTractTransformationData data) {
		GMMMatch gmmMatch = null;

		// Find target estimate from codebook
		if (baseParams.isVocalTractTransformation) // isTransformUnvoiced=false not supported for MFCCs currently! Voicing
													// information should be passed here to support this feature
		{
			if (mapper instanceof JointGMMMapper) {
				// Different weighting strategies can be tested here, i.e. doing a fuzzy phone classification
				double[] gmmWeights = new double[1];
				Arrays.fill(gmmWeights, 1.0);

				gmmMatch = ((JointGMMMapper) mapper).transform(frameMfccs, (JointGMMSet) data, gmmWeights,
						baseParams.isVocalTractMatchUsingTargetModel);
			}
		}

		if (!baseParams.isResynthesizeVocalTractFromSourceModel)
			return ((JointGMMMatch) gmmMatch).outputFeatures;
		else
			return ((JointGMMMatch) gmmMatch).mappedSourceFeatures;
	}
}
