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
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.BaselinePreprocessor;
import marytts.signalproc.adaptation.BaselineTrainer;
import marytts.signalproc.adaptation.codebook.WeightedCodebookTrainerParams;

/**
 * Joint-GMM voice conversion training Both parallel and non-parallel training should derive from this class
 * 
 * @author Oytun T&uuml;rk
 */
public class JointGMMTrainer extends BaselineTrainer {

	protected WeightedCodebookTrainerParams codebookTrainerParams;
	protected JointGMMTrainerParams gmmTrainerParams;
	protected ContextualGMMParams cgParams;

	public JointGMMTrainer(BaselinePreprocessor pp, BaselineFeatureExtractor fe, WeightedCodebookTrainerParams pa,
			JointGMMTrainerParams gp, ContextualGMMParams cg) {
		super(pp, fe);

		codebookTrainerParams = new WeightedCodebookTrainerParams(pa);
		gmmTrainerParams = new JointGMMTrainerParams(gp);
		cgParams = new ContextualGMMParams(cg);
	}

}
