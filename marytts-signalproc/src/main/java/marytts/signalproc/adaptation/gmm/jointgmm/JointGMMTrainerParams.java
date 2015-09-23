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
package marytts.signalproc.adaptation.gmm.jointgmm;

import marytts.machinelearning.ContextualGMMParams;
import marytts.machinelearning.GMMTrainerParams;
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.BaselineTrainerParams;

/**
 * 
 * Parameters for joint-GMM based voice conversion training
 * 
 * @author Oytun T&uuml;rk
 */
public class JointGMMTrainerParams extends BaselineTrainerParams {
	public boolean isContextualGMMs; // Train separate GMMs for each context class
	public int contextClassificationType; // Type of context classification to use. Only active when isContextualGMMs=true

	public GMMTrainerParams gmmEMTrainerParams; // Expectation-maximization training parameters
	public String jointGMMFile; // Binary file that keeps the GMMs

	public int vocalTractFeature; // Type of vocal tract feature that the GMMs will be trained for.

	public JointGMMTrainerParams() {
		isContextualGMMs = false;
		contextClassificationType = ContextualGMMParams.NO_PHONEME_CLASS;

		gmmEMTrainerParams = new GMMTrainerParams();
		jointGMMFile = "";

		vocalTractFeature = BaselineFeatureExtractor.LSF_FEATURES;
	}

	public JointGMMTrainerParams(JointGMMTrainerParams existing) {
		isContextualGMMs = existing.isContextualGMMs;
		contextClassificationType = existing.contextClassificationType;

		gmmEMTrainerParams = new GMMTrainerParams(existing.gmmEMTrainerParams);
		jointGMMFile = existing.jointGMMFile;

		vocalTractFeature = existing.vocalTractFeature;
	}
}
