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

import marytts.signalproc.adaptation.BaselineTrainerParams;
import marytts.signalproc.adaptation.outlier.GaussianOutlierEliminatorParams;
import marytts.signalproc.adaptation.outlier.KMeansMappingEliminatorParams;

/**
 * Parameters of weighted codebook training
 * 
 * @author Oytun T&uuml;rk
 */
public class WeightedCodebookTrainerParams extends BaselineTrainerParams {
	public static final int MAXIMUM_CONTEXT = 10;

	public WeightedCodebookFileHeader codebookHeader; // Header of codebook file

	public String trainingBaseFolder; // Training base directory
	public String sourceTrainingFolder; // Source training folder
	public String targetTrainingFolder; // Target training folder
	public String codebookFile; // Source and target codebook file
	public String temporaryCodebookFile; // Temporary codebook file

	public String pitchMappingFile; // Source and target pitch mapping file

	// Some filename extension for custom training file types
	public String indexMapFileExtension; // Index map file extensions

	public boolean isForcedAnalysis; // Set this to true if you want all acoustic features to be extracted even if their files
										// exist

	public GaussianOutlierEliminatorParams gaussianEliminatorParams; // Parameters of Gaussian based outlier eliminator
	public KMeansMappingEliminatorParams kmeansEliminatorParams; // Parameters of K-Means clustering based outlier eliminator

	public String[] labelsToExcludeFromTraining; // These labels are excluded from training

	public WeightedCodebookTrainerParams() {
		codebookHeader = new WeightedCodebookFileHeader();

		trainingBaseFolder = ""; // Training base directory
		sourceTrainingFolder = ""; // Source training folder
		targetTrainingFolder = ""; // Target training folder
		codebookFile = ""; // Source and target codebook file
		temporaryCodebookFile = ""; // Temporary codebook file

		pitchMappingFile = "";

		// Some filename extension for custom training file types
		indexMapFileExtension = ".imf";

		isForcedAnalysis = false;

		gaussianEliminatorParams = new GaussianOutlierEliminatorParams();
		kmeansEliminatorParams = new KMeansMappingEliminatorParams();

		labelsToExcludeFromTraining = null;
	}

	public WeightedCodebookTrainerParams(WeightedCodebookTrainerParams existing) {
		codebookHeader = new WeightedCodebookFileHeader(existing.codebookHeader);

		trainingBaseFolder = existing.trainingBaseFolder;
		sourceTrainingFolder = existing.sourceTrainingFolder;
		targetTrainingFolder = existing.targetTrainingFolder;
		codebookFile = existing.codebookFile;
		temporaryCodebookFile = existing.temporaryCodebookFile;

		pitchMappingFile = existing.pitchMappingFile;

		indexMapFileExtension = existing.indexMapFileExtension;

		isForcedAnalysis = existing.isForcedAnalysis;

		gaussianEliminatorParams = new GaussianOutlierEliminatorParams(existing.gaussianEliminatorParams);
		kmeansEliminatorParams = new KMeansMappingEliminatorParams(existing.kmeansEliminatorParams);

		labelsToExcludeFromTraining = existing.labelsToExcludeFromTraining;
	}
}
