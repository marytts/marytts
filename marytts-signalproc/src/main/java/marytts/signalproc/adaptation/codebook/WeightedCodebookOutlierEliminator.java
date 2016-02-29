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

import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.outlier.GMMOutlierEliminator;
import marytts.signalproc.adaptation.outlier.GaussianOutlierEliminator;
import marytts.signalproc.adaptation.outlier.KMeansMappingEliminator;
import marytts.util.io.FileUtils;

/**
 * 
 * A collection of outlier eliminators for weighted codebook mapping
 * 
 * @author Oytun T&uuml;rk
 */
public class WeightedCodebookOutlierEliminator {
	private GaussianOutlierEliminator gaussian;
	private KMeansMappingEliminator kmeans;
	private GMMOutlierEliminator gmm;

	public void run(WeightedCodebookTrainerParams params) {
		String tempIn = params.temporaryCodebookFile;
		String tempOut = params.temporaryCodebookFile + "2";

		if (!params.gaussianEliminatorParams.isCheckDurationOutliers && !params.gaussianEliminatorParams.isCheckEnergyOutliers
				&& !params.gaussianEliminatorParams.isCheckF0Outliers && !params.gaussianEliminatorParams.isCheckLsfOutliers)
			params.gaussianEliminatorParams.isActive = false;

		if (!params.kmeansEliminatorParams.isCheckDurationOutliers && !params.kmeansEliminatorParams.isCheckEnergyOutliers
				&& !params.kmeansEliminatorParams.isCheckF0Outliers && !params.kmeansEliminatorParams.isCheckLsfOutliers)
			params.kmeansEliminatorParams.isActive = false;

		if (params.gaussianEliminatorParams.isActive) {
			if (!params.kmeansEliminatorParams.isActive)
				tempOut = params.codebookFile;

			gaussian = new GaussianOutlierEliminator();

			if (params.codebookHeader.vocalTractFeature != BaselineFeatureExtractor.LSF_FEATURES)
				params.gaussianEliminatorParams.isCheckLsfOutliers = false;

			gaussian.eliminate(params.gaussianEliminatorParams, tempIn, tempOut);
		}

		if (params.kmeansEliminatorParams.isActive) {
			if (params.gaussianEliminatorParams.isActive)
				tempIn = tempOut;

			tempOut = params.codebookFile; // This should be changed if you add more eliminators below

			kmeans = new KMeansMappingEliminator();

			if (params.codebookHeader.vocalTractFeature != BaselineFeatureExtractor.LSF_FEATURES)
				params.kmeansEliminatorParams.isCheckLsfOutliers = false;

			kmeans.eliminate(params.kmeansEliminatorParams, tempIn, tempOut);

			if (params.gaussianEliminatorParams.isActive)
				FileUtils.delete(tempIn);
		}

		/*
		 * if (params.gmmEliminatorParams.isActive) { gmm = new KMeansOutlierEliminator(params.totalStandardDeviations);
		 * gmm.eliminate(); }
		 */

		// If no outlier elimintor was run, just rename the temporary input file to final codebook file
		if (!params.gaussianEliminatorParams.isActive && !params.kmeansEliminatorParams.isActive)
			FileUtils.rename(tempIn, params.codebookFile);
	}

}
