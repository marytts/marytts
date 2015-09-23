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

import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.exceptions.MaryConfigurationException;
import marytts.machinelearning.ContextualGMMParams;
import marytts.machinelearning.GMM;
import marytts.machinelearning.GMMTrainer;
import marytts.machinelearning.GMMTrainerParams;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.BaselinePreprocessor;
import marytts.signalproc.adaptation.codebook.WeightedCodebook;
import marytts.signalproc.adaptation.codebook.WeightedCodebookFile;
import marytts.signalproc.adaptation.codebook.WeightedCodebookFileHeader;
import marytts.signalproc.adaptation.codebook.WeightedCodebookParallelTrainer;
import marytts.signalproc.adaptation.codebook.WeightedCodebookTrainerParams;
import marytts.signalproc.adaptation.outlier.KMeansMappingEliminatorParams;
import marytts.signalproc.adaptation.outlier.TotalStandardDeviations;
import marytts.signalproc.adaptation.prosody.PitchMappingFile;
import marytts.signalproc.analysis.distance.DistanceComputer;
import marytts.signalproc.window.Window;
import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;

/**
 * Joint-GMM voice conversion training using parallel source and target databases
 * 
 * Reference: A. Kain and M. Macon, “Spectral voice conversion for text-to-speech synthesis,” in Proc. of the IEEE ICASSP 1998,
 * vol. 1, pp. 285-288.
 * 
 * @author Oytun T&uuml;rk
 */
public class JointGMMParallelTrainer extends JointGMMTrainer {
	protected WeightedCodebookParallelTrainer wcpTrainer;
	protected JointGMMTrainerParams jgParams;

	public JointGMMParallelTrainer(BaselinePreprocessor pp, BaselineFeatureExtractor fe, WeightedCodebookTrainerParams pa,
			JointGMMTrainerParams gp, ContextualGMMParams cg) {
		super(pp, fe, pa, gp, cg);

		wcpTrainer = new WeightedCodebookParallelTrainer(pp, fe, pa);
		jgParams = new JointGMMTrainerParams(gp);
	}

	public void run() {
		train();
	}

	public void train() {
		if (!FileUtils.exists(codebookTrainerParams.codebookFile)) {
			// Parallel codebook training
			try {
				wcpTrainer.run();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedAudioFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//
		}

		// Read parallel codebook
		WeightedCodebookFile codebookFile = new WeightedCodebookFile(wcpTrainer.wcParams.codebookFile,
				WeightedCodebookFile.OPEN_FOR_READ);
		WeightedCodebook codebook = null;

		try {
			codebook = codebookFile.readCodebookFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//

		// Get codebook entries in suitable format for GMM training and train joint GMMs
		if (cgParams == null || cgParams.phoneClasses == null) // No context
		{
			JointGMMSet gmmSet = null;
			GMM gmm = null;
			if (codebook != null) {
				double[][] xy = null;
				boolean bFeatureExisting = false;

				if (codebookTrainerParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.LSF_FEATURES) {
					xy = new double[codebook.entries.length][2 * codebook.header.lsfParams.dimension];
					for (int i = 0; i < codebook.entries.length; i++) {
						System.arraycopy(codebook.entries[i].sourceItem.lsfs, 0, xy[i], 0, codebook.header.lsfParams.dimension);
						System.arraycopy(codebook.entries[i].targetItem.lsfs, 0, xy[i], codebook.header.lsfParams.dimension,
								codebook.header.lsfParams.dimension);
					}

					bFeatureExisting = true;
				} else if (codebookTrainerParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES) {
					xy = new double[codebook.entries.length][2 * codebook.header.mfccParams.dimension];
					for (int i = 0; i < codebook.entries.length; i++) {
						System.arraycopy(codebook.entries[i].sourceItem.mfccs, 0, xy[i], 0, codebook.header.mfccParams.dimension);
						System.arraycopy(codebook.entries[i].targetItem.mfccs, 0, xy[i], codebook.header.mfccParams.dimension,
								codebook.header.mfccParams.dimension);
					}

					bFeatureExisting = true;
				}

				assert bFeatureExisting;

				GMMTrainer g = new GMMTrainer();
				gmmSet = new JointGMMSet(1, cgParams);
				gmm = g.train(xy, jgParams.gmmEMTrainerParams);

				if (codebookTrainerParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.LSF_FEATURES)
					gmmSet.gmms[0] = new JointGMM(gmm, codebook.header.lsfParams);
				else if (codebookTrainerParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES)
					gmmSet.gmms[0] = new JointGMM(gmm, codebook.header.mfccParams);
			}

			// Convert joint GMM into a suitable format for using in transformation and save to a binary output file
			if (gmmSet != null)
				gmmSet.write(jgParams.jointGMMFile);
		} else // Train contextual GMMs - a separate GMM will be trained for each phone class, all GMMs will be written to the
				// same GMM file
		{
			double[][] xy = null;
			int[] totals = new int[cgParams.phoneClasses.length + 1];
			int[] classIndices = new int[codebook.entries.length];
			Arrays.fill(totals, 0);
			int i, n;
			JointGMMSet gmmSet = new JointGMMSet(totals.length, cgParams);

			if (codebook != null) {
				for (i = 0; i < codebook.entries.length; i++) {
					classIndices[i] = cgParams.getClassIndex(codebook.entries[i].sourceItem.phn);
					if (classIndices[i] < 0)
						classIndices[i] = totals.length - 1;

					totals[classIndices[i]]++;
				}
			}

			for (n = 0; n < totals.length; n++) {
				GMM gmm = null;
				int count = 0;
				if (totals[n] > 0) {
					if (codebookTrainerParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.LSF_FEATURES) {
						xy = new double[totals[n]][2 * codebook.header.lsfParams.dimension];

						for (i = 0; i < classIndices.length; i++) {
							if (count >= totals[n])
								break;

							if (classIndices[i] == n) {
								System.arraycopy(codebook.entries[i].sourceItem.lsfs, 0, xy[count], 0,
										codebook.header.lsfParams.dimension);
								System.arraycopy(codebook.entries[i].targetItem.lsfs, 0, xy[count],
										codebook.header.lsfParams.dimension, codebook.header.lsfParams.dimension);
								count++;
							}
						}
					} else if (codebookTrainerParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES) {
						xy = new double[totals[n]][2 * codebook.header.mfccParams.dimension];

						for (i = 0; i < classIndices.length; i++) {
							if (count >= totals[n])
								break;

							if (classIndices[i] == n) {
								System.arraycopy(codebook.entries[i].sourceItem.mfccs, 0, xy[count], 0,
										codebook.header.mfccParams.dimension);
								System.arraycopy(codebook.entries[i].targetItem.mfccs, 0, xy[count],
										codebook.header.mfccParams.dimension, codebook.header.mfccParams.dimension);
								count++;
							}
						}
					}

					GMMTrainer g = new GMMTrainer();
					gmm = g.train(xy, cgParams.classTrainerParams[n]);
					if (n < totals.length - 1) {
						gmm.info = "";
						for (i = 0; i < cgParams.phoneClasses[n].length - 1; i++)
							gmm.info += cgParams.phoneClasses[n][i] + " ";

						gmm.info += cgParams.phoneClasses[n][cgParams.phoneClasses[n].length - 1];
					} else
						gmm.info = "other";

					if (codebookTrainerParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.LSF_FEATURES) {
						codebook.header.lsfParams.numfrm = totals[n];
						gmmSet.gmms[n] = new JointGMM(gmm, codebook.header.lsfParams);
					} else if (codebookTrainerParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES) {
						codebook.header.mfccParams.numfrm = totals[n];
						gmmSet.gmms[n] = new JointGMM(gmm, codebook.header.mfccParams);
					}
				}
			}

			// Convert joint GMM into a suitable format for using in transformation and save to a binary output file
			if (gmmSet != null)
				gmmSet.write(jgParams.jointGMMFile);
			//
		}
		//

		System.out.println("Joint source-target GMM training completed...");
	}

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException, MaryConfigurationException {
		// mainIEEE_TASLP_2009_rap(args);

		// mainInterspeech2008(args);

		// mainHmmVoiceConversion(args);

		// mainQuickTest(args);
		mainQuickTest2(args);
	}

	public static void mainIEEE_TASLP_2009_rap(String[] args) throws UnsupportedAudioFileException, IOException,
			MaryConfigurationException {
		String wavBaseFolder = "D:/Oytun/Papers/IEEE_Transaction_VT/musicVC/final_gmm/";

		String sourceTag = "uch";
		String targetTag = "target";
		String method;

		boolean isContextualGMMs = false;
		int contextClassificationType = ContextualGMMParams.NO_PHONEME_CLASS;
		int[] numComponents = { 32 };
		// int contextClassificationType = ContextualGMMParams.SILENCE_SPEECH; int[] numComponents = {16, 128};
		// int contextClassificationType = ContextualGMMParams.VOWEL_SILENCE_CONSONANT; int[] numComponents = {128, 16, 128};
		// int contextClassificationType = ContextualGMMParams.PHONOLOGY_CLASS; int[] numComponents = {numMixes};
		// int contextClassificationType = ContextualGMMParams.FRICATIVE_GLIDELIQUID_NASAL_PLOSIVE_VOWEL_OTHER; int[]
		// numComponents = {128, 128, 128, 128, 128, 16};
		// int contextClassificationType = ContextualGMMParams.PHONEME_IDENTITY; int[] numComponents = {128};

		BaselinePreprocessor pp = new BaselinePreprocessor();
		BaselineFeatureExtractor fe = new BaselineFeatureExtractor();
		WeightedCodebookTrainerParams pa = new WeightedCodebookTrainerParams();
		JointGMMTrainerParams gp = new JointGMMTrainerParams();
		ContextualGMMParams cg = null;
		int i;

		pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAMES;
		method = "F"; // Frame-by-frame mapping of features
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAME_GROUPS; method = "FG";
		// pa.codebookHeader.numNeighboursInFrameGroups = 3; //Mapping of frame average features (no label information but fixed
		// amount of neighbouring frames is used)
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABELS; method = "L"; //Mapping of label average features
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABEL_GROUPS; method = "LG";
		// pa.codebookHeader.numNeighboursInLabelGroups = 1; //Mapping of average features collected across label groups (i.e.
		// vowels, consonants, etc)
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.SPEECH; method = "S"; //Mapping of average features
		// collected across all speech parts (i.e. like spectral equalization)

		pa.codebookHeader.vocalTractFeature = BaselineFeatureExtractor.LSF_FEATURES; // Use Lsf features - full speech to speech
																						// transformation
		// pa.codebookHeader.vocalTractFeature = BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES; //Use MFCC features -
		// currently supports only feature to featur etransformation

		pa.codebookHeader.sourceTag = sourceTag + method; // Source name tag (i.e. style or speaker identity)
		pa.codebookHeader.targetTag = targetTag + method; // Target name tag (i.e. style or speaker identity)

		pa.trainingBaseFolder = wavBaseFolder; // Training base directory
		pa.sourceTrainingFolder = wavBaseFolder + sourceTag + "_train/"; // Source training folder
		pa.targetTrainingFolder = wavBaseFolder + targetTag + "_train/"; // Target training folder

		pa.indexMapFileExtension = ".imf"; // Index map file extensions

		pa.codebookHeader.lsfParams.dimension = 0; // Auto set
		pa.codebookHeader.lsfParams.preCoef = 0.97f;
		pa.codebookHeader.lsfParams.skipsize = 0.010f;
		pa.codebookHeader.lsfParams.winsize = 0.020f;
		pa.codebookHeader.lsfParams.windowType = Window.HAMMING;
		pa.codebookHeader.lsfParams.isBarkScaled = true;

		// Gaussian trainer params: commenting out results in using default value for each
		gp.vocalTractFeature = pa.codebookHeader.vocalTractFeature;
		gp.isContextualGMMs = isContextualGMMs;
		gp.gmmEMTrainerParams.totalComponents = numComponents[0];
		gp.gmmEMTrainerParams.isDiagonalCovariance = true;
		gp.gmmEMTrainerParams.kmeansMaxIterations = 200;
		gp.gmmEMTrainerParams.kmeansMinClusterChangePercent = 0.1;
		gp.gmmEMTrainerParams.kmeansMinSamplesInOneCluster = 50;
		gp.gmmEMTrainerParams.emMinIterations = 100;
		gp.gmmEMTrainerParams.emMaxIterations = 400;
		gp.gmmEMTrainerParams.isUpdateCovariances = true;
		gp.gmmEMTrainerParams.tinyLogLikelihoodChangePercent = 1e-5;
		gp.gmmEMTrainerParams.minCovarianceAllowed = 1e-4;
		gp.gmmEMTrainerParams.useNativeCLibTrainer = true;

		if (gp.isContextualGMMs) {
			GMMTrainerParams[] gmmParams = new GMMTrainerParams[numComponents.length];
			for (i = 0; i < numComponents.length; i++) {
				gmmParams[i] = new GMMTrainerParams(gp.gmmEMTrainerParams);
				gmmParams[i].totalComponents = numComponents[i];
			}
			String phonemeSetFile = "D:/Mary TTS New/lib/modules/de/cap/phoneme-list-de.xml";
			cg = getContextualGMMParams(phonemeSetFile, gmmParams, contextClassificationType);
		}

		String baseFile = StringUtils.checkLastSlash(pa.trainingBaseFolder) + pa.codebookHeader.sourceTag + "_X_"
				+ pa.codebookHeader.targetTag;
		// pa.codebookFile = baseFile + "_" + String.valueOf(gp.gmmEMTrainerParams.totalComponents) +
		// WeightedCodebookFile.DEFAULT_EXTENSION;
		pa.codebookFile = baseFile + WeightedCodebookFile.DEFAULT_EXTENSION;
		pa.pitchMappingFile = baseFile + PitchMappingFile.DEFAULT_EXTENSION;

		if (!isContextualGMMs)
			gp.jointGMMFile = baseFile + "_" + String.valueOf(gp.gmmEMTrainerParams.totalComponents)
					+ JointGMMSet.DEFAULT_EXTENSION;
		else {
			gp.jointGMMFile = baseFile + "_context" + String.valueOf(contextClassificationType);
			for (i = 0; i < numComponents.length; i++)
				gp.jointGMMFile += "_" + String.valueOf(numComponents[i]);

			gp.jointGMMFile += JointGMMSet.DEFAULT_EXTENSION;
		}

		pa.isForcedAnalysis = false;

		pa.codebookHeader.ptcParams.windowSizeInSeconds = 0.040;
		pa.codebookHeader.ptcParams.skipSizeInSeconds = 0.005;
		pa.codebookHeader.ptcParams.voicingThreshold = 0.30;
		pa.codebookHeader.ptcParams.isDoublingCheck = false;
		pa.codebookHeader.ptcParams.isHalvingCheck = false;
		pa.codebookHeader.ptcParams.minimumF0 = 40.0f;
		pa.codebookHeader.ptcParams.maximumF0 = 400.0f;
		pa.codebookHeader.ptcParams.centerClippingRatio = 0.3;
		pa.codebookHeader.ptcParams.cutOff1 = pa.codebookHeader.ptcParams.minimumF0 - 20.0;
		pa.codebookHeader.ptcParams.cutOff2 = pa.codebookHeader.ptcParams.maximumF0 + 200.0;

		pa.codebookHeader.energyParams.windowSizeInSeconds = 0.020;
		pa.codebookHeader.energyParams.skipSizeInSeconds = 0.010;

		TotalStandardDeviations tsd = new TotalStandardDeviations();
		tsd.lsf = 1.5;
		tsd.f0 = 1.0;
		tsd.duration = 1.0;
		tsd.energy = 2.0;

		// Gaussian outlier eliminator
		// Decreasing totalStandardDeviations will lead to more outlier eliminations, i.e. smaller codebooks
		pa.gaussianEliminatorParams.isActive = true; // Set to false if you do not want to use this eliminator at all
		pa.gaussianEliminatorParams.isCheckLsfOutliers = true;
		pa.gaussianEliminatorParams.isEliminateTooSimilarLsf = false;
		pa.gaussianEliminatorParams.isCheckF0Outliers = true;
		pa.gaussianEliminatorParams.isCheckDurationOutliers = true;
		pa.gaussianEliminatorParams.isCheckEnergyOutliers = true;
		pa.gaussianEliminatorParams.totalStandardDeviations = new TotalStandardDeviations(tsd);
		//

		// KMeans one-to-many and many-to-one mapping eliminator
		pa.kmeansEliminatorParams.isActive = false; // Set to false if you do not want to use this eliminator at all

		// pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_LEAST_LIKELY_MAPPINGS;
		// pa.kmeansEliminatorParams.eliminationLikelihood = 0.20;

		pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_MEAN_DISTANCE_MISMATCHES;
		pa.kmeansEliminatorParams.distanceType = DistanceComputer.NORMALIZED_EUCLIDEAN_DISTANCE;
		// pa.kmeansEliminatorParams.distanceType = DistanceComputer.EUCLIDEAN_DISTANCE;
		pa.kmeansEliminatorParams.isGlobalVariance = true;

		// pa.kmeansEliminatorParams.eliminationAlgorithm =
		// KMeansMappingEliminatorParams.ELIMINATE_USING_SUBCLUSTER_MEAN_DISTANCES;

		pa.kmeansEliminatorParams.isSeparateClustering = false; // Cluster features separately(true) or together(false)?

		// Effective only when isSeparateClustering clustering is false
		tsd.general = 0.1;
		pa.kmeansEliminatorParams.numClusters = 30;

		// Effective only when isSeparateClustering clustering is true
		tsd.lsf = 1.0;
		tsd.f0 = 1.0;
		tsd.duration = 1.0;
		tsd.energy = 1.0;
		pa.kmeansEliminatorParams.numClustersLsf = 30;
		pa.kmeansEliminatorParams.numClustersF0 = 50;
		pa.kmeansEliminatorParams.numClustersDuration = 5;
		pa.kmeansEliminatorParams.numClustersEnergy = 5;

		pa.kmeansEliminatorParams.isCheckLsfOutliers = false;
		pa.kmeansEliminatorParams.isCheckF0Outliers = false;
		pa.kmeansEliminatorParams.isCheckDurationOutliers = false;
		pa.kmeansEliminatorParams.isCheckEnergyOutliers = false;
		//

		pa.kmeansEliminatorParams.totalStandardDeviations = new TotalStandardDeviations(tsd);
		//

		JointGMMParallelTrainer t = new JointGMMParallelTrainer(pp, fe, pa, gp, cg);

		t.run();
	}

	// Testing voice conversion as a post-processor to enhance HMM based synthesis output
	// The idea is to train a voice conversion function between HMM outputs and natural recordings
	// Then, any HMM output is to be transformed with the voice conversion function to make it closer to original recordings
	public static void mainHmmVoiceConversion(String[] args) throws UnsupportedAudioFileException, IOException,
			MaryConfigurationException {
		// String wavBaseFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/hsmmMfcc_25Dimensional/";
		// String wavBaseFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/lsp_21Dimensional/";
		// String wavBaseFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/mellsp_21Dimensional/";
		// String baseFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/mfcc_25Dimensional/";
		// String wavBaseFolder = "/home/oytun/";

		String wavBaseFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest2/";

		String sourceTag = "hmmSource_gv";
		String targetTag = "origTarget";
		String method;

		int numTrainingFiles = 1092;
		boolean isContextualGMMs = false;
		int contextClassificationType = ContextualGMMParams.NO_PHONEME_CLASS;
		int[] numComponents = { 128 };
		// int contextClassificationType = ContextualGMMParams.SILENCE_SPEECH; int[] numComponents = {16, 128};
		// int contextClassificationType = ContextualGMMParams.VOWEL_SILENCE_CONSONANT; int[] numComponents = {128, 16, 128};
		// int contextClassificationType = ContextualGMMParams.PHONOLOGY_CLASS; int[] numComponents = {numMixes};
		// int contextClassificationType = ContextualGMMParams.FRICATIVE_GLIDELIQUID_NASAL_PLOSIVE_VOWEL_OTHER; int[]
		// numComponents = {128, 128, 128, 128, 128, 16};
		// int contextClassificationType = ContextualGMMParams.PHONEME_IDENTITY; int[] numComponents = {128};

		BaselinePreprocessor pp = new BaselinePreprocessor();
		BaselineFeatureExtractor fe = new BaselineFeatureExtractor();
		WeightedCodebookTrainerParams pa = new WeightedCodebookTrainerParams();
		JointGMMTrainerParams gp = new JointGMMTrainerParams();
		ContextualGMMParams cg = null;
		int i;

		pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAMES;
		method = "F"; // Frame-by-frame mapping of features
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAME_GROUPS; method = "FG";
		// pa.codebookHeader.numNeighboursInFrameGroups = 3; //Mapping of frame average features (no label information but fixed
		// amount of neighbouring frames is used)
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABELS; method = "L"; //Mapping of label average features
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABEL_GROUPS; method = "LG";
		// pa.codebookHeader.numNeighboursInLabelGroups = 1; //Mapping of average features collected across label groups (i.e.
		// vowels, consonants, etc)
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.SPEECH; method = "S"; //Mapping of average features
		// collected across all speech parts (i.e. like spectral equalization)

		pa.codebookHeader.vocalTractFeature = BaselineFeatureExtractor.LSF_FEATURES; // Use Lsf features - full speech to speech
																						// transformation
		// pa.codebookHeader.vocalTractFeature = BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES; //Use MFCC features -
		// currently supports only feature to featur etransformation

		pa.codebookHeader.sourceTag = sourceTag + method; // Source name tag (i.e. style or speaker identity)
		pa.codebookHeader.targetTag = targetTag + method; // Target name tag (i.e. style or speaker identity)

		pa.trainingBaseFolder = wavBaseFolder + "output/" + sourceTag + "2" + targetTag; // Training base directory
		pa.sourceTrainingFolder = wavBaseFolder + sourceTag + "/train_" + String.valueOf(numTrainingFiles) + "/"; // Source
																													// training
																													// folder
		pa.targetTrainingFolder = wavBaseFolder + targetTag + "/train_" + String.valueOf(numTrainingFiles) + "/"; // Target
																													// training
																													// folder

		pa.indexMapFileExtension = ".imf"; // Index map file extensions

		pa.codebookHeader.lsfParams.dimension = 0; // Auto set
		pa.codebookHeader.lsfParams.preCoef = 0.97f;
		pa.codebookHeader.lsfParams.skipsize = 0.010f;
		pa.codebookHeader.lsfParams.winsize = 0.020f;
		pa.codebookHeader.lsfParams.windowType = Window.HAMMING;

		// Gaussian trainer params: commenting out results in using default value for each
		gp.vocalTractFeature = pa.codebookHeader.vocalTractFeature;
		gp.isContextualGMMs = isContextualGMMs;
		gp.gmmEMTrainerParams.totalComponents = numComponents[0];
		gp.gmmEMTrainerParams.isDiagonalCovariance = true;
		gp.gmmEMTrainerParams.kmeansMaxIterations = 200;
		gp.gmmEMTrainerParams.kmeansMinClusterChangePercent = 0.1;
		gp.gmmEMTrainerParams.kmeansMinSamplesInOneCluster = 50;
		gp.gmmEMTrainerParams.emMinIterations = 100;
		gp.gmmEMTrainerParams.emMaxIterations = 400;
		gp.gmmEMTrainerParams.isUpdateCovariances = true;
		gp.gmmEMTrainerParams.tinyLogLikelihoodChangePercent = 1e-5;
		gp.gmmEMTrainerParams.minCovarianceAllowed = 1e-4;
		gp.gmmEMTrainerParams.useNativeCLibTrainer = true;

		if (gp.isContextualGMMs) {
			GMMTrainerParams[] gmmParams = new GMMTrainerParams[numComponents.length];
			for (i = 0; i < numComponents.length; i++) {
				gmmParams[i] = new GMMTrainerParams(gp.gmmEMTrainerParams);
				gmmParams[i].totalComponents = numComponents[i];
			}
			String phoneSetFile = "D:/Mary TTS New/lib/modules/de/cap/phone-list-de.xml";
			cg = getContextualGMMParams(phoneSetFile, gmmParams, contextClassificationType);
		}

		String baseFile = StringUtils.checkLastSlash(pa.trainingBaseFolder) + pa.codebookHeader.sourceTag + "_X_"
				+ pa.codebookHeader.targetTag;
		// pa.codebookFile = baseFile + "_" + String.valueOf(gp.gmmEMTrainerParams.totalComponents) +
		// WeightedCodebookFile.DEFAULT_EXTENSION;
		pa.codebookFile = baseFile + "_" + String.valueOf(numTrainingFiles) + WeightedCodebookFile.DEFAULT_EXTENSION;
		pa.pitchMappingFile = baseFile + "_" + String.valueOf(numTrainingFiles) + PitchMappingFile.DEFAULT_EXTENSION;

		if (!isContextualGMMs)
			gp.jointGMMFile = baseFile + "_" + String.valueOf(numTrainingFiles) + "_"
					+ String.valueOf(gp.gmmEMTrainerParams.totalComponents) + JointGMMSet.DEFAULT_EXTENSION;
		else {
			gp.jointGMMFile = baseFile + "_" + String.valueOf(numTrainingFiles) + "_context"
					+ String.valueOf(contextClassificationType);
			for (i = 0; i < numComponents.length; i++)
				gp.jointGMMFile += "_" + String.valueOf(numComponents[i]);

			gp.jointGMMFile += JointGMMSet.DEFAULT_EXTENSION;
		}

		pa.isForcedAnalysis = false;

		pa.codebookHeader.ptcParams.windowSizeInSeconds = 0.040;
		pa.codebookHeader.ptcParams.skipSizeInSeconds = 0.005;
		pa.codebookHeader.ptcParams.voicingThreshold = 0.30;
		pa.codebookHeader.ptcParams.isDoublingCheck = false;
		pa.codebookHeader.ptcParams.isHalvingCheck = false;
		pa.codebookHeader.ptcParams.minimumF0 = 40.0f;
		pa.codebookHeader.ptcParams.maximumF0 = 400.0f;
		pa.codebookHeader.ptcParams.centerClippingRatio = 0.3;
		pa.codebookHeader.ptcParams.cutOff1 = pa.codebookHeader.ptcParams.minimumF0 - 20.0;
		pa.codebookHeader.ptcParams.cutOff2 = pa.codebookHeader.ptcParams.maximumF0 + 200.0;

		pa.codebookHeader.energyParams.windowSizeInSeconds = 0.020;
		pa.codebookHeader.energyParams.skipSizeInSeconds = 0.010;

		TotalStandardDeviations tsd = new TotalStandardDeviations();
		tsd.lsf = 1.5;
		tsd.f0 = 1.0;
		tsd.duration = 1.0;
		tsd.energy = 2.0;

		// Gaussian outlier eliminator
		// Decreasing totalStandardDeviations will lead to more outlier eliminations, i.e. smaller codebooks
		pa.gaussianEliminatorParams.isActive = true; // Set to false if you do not want to use this eliminator at all
		pa.gaussianEliminatorParams.isCheckLsfOutliers = true;
		pa.gaussianEliminatorParams.isEliminateTooSimilarLsf = false;
		pa.gaussianEliminatorParams.isCheckF0Outliers = true;
		pa.gaussianEliminatorParams.isCheckDurationOutliers = true;
		pa.gaussianEliminatorParams.isCheckEnergyOutliers = true;
		pa.gaussianEliminatorParams.totalStandardDeviations = new TotalStandardDeviations(tsd);
		//

		// KMeans one-to-many and many-to-one mapping eliminator
		pa.kmeansEliminatorParams.isActive = false; // Set to false if you do not want to use this eliminator at all

		// pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_LEAST_LIKELY_MAPPINGS;
		// pa.kmeansEliminatorParams.eliminationLikelihood = 0.20;

		pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_MEAN_DISTANCE_MISMATCHES;
		pa.kmeansEliminatorParams.distanceType = DistanceComputer.NORMALIZED_EUCLIDEAN_DISTANCE;
		// pa.kmeansEliminatorParams.distanceType = DistanceComputer.EUCLIDEAN_DISTANCE;
		pa.kmeansEliminatorParams.isGlobalVariance = true;

		// pa.kmeansEliminatorParams.eliminationAlgorithm =
		// KMeansMappingEliminatorParams.ELIMINATE_USING_SUBCLUSTER_MEAN_DISTANCES;

		pa.kmeansEliminatorParams.isSeparateClustering = false; // Cluster features separately(true) or together(false)?

		// Effective only when isSeparateClustering clustering is false
		tsd.general = 0.1;
		pa.kmeansEliminatorParams.numClusters = 30;

		// Effective only when isSeparateClustering clustering is true
		tsd.lsf = 1.0;
		tsd.f0 = 1.0;
		tsd.duration = 1.0;
		tsd.energy = 1.0;
		pa.kmeansEliminatorParams.numClustersLsf = 30;
		pa.kmeansEliminatorParams.numClustersF0 = 50;
		pa.kmeansEliminatorParams.numClustersDuration = 5;
		pa.kmeansEliminatorParams.numClustersEnergy = 5;

		pa.kmeansEliminatorParams.isCheckLsfOutliers = false;
		pa.kmeansEliminatorParams.isCheckF0Outliers = false;
		pa.kmeansEliminatorParams.isCheckDurationOutliers = false;
		pa.kmeansEliminatorParams.isCheckEnergyOutliers = false;
		//

		pa.kmeansEliminatorParams.totalStandardDeviations = new TotalStandardDeviations(tsd);
		//

		JointGMMParallelTrainer t = new JointGMMParallelTrainer(pp, fe, pa, gp, cg);

		t.run();
	}

	public static void mainInterspeech2008(String[] args) throws UnsupportedAudioFileException, IOException,
			MaryConfigurationException {
		String emotion = "angry";
		String method = "F";
		int numTrainingFiles = 200; // 2, 20, 200, 350

		boolean isContextualGMMs = false;
		int contextClassificationType = ContextualGMMParams.NO_PHONEME_CLASS;
		int[] numComponents = { 40 };
		// int contextClassificationType = ContextualGMMParams.SILENCE_SPEECH; int[] numComponents = {16, 128};
		// int contextClassificationType = ContextualGMMParams.VOWEL_SILENCE_CONSONANT; int[] numComponents = {128, 16, 128};
		// int contextClassificationType = ContextualGMMParams.PHONOLOGY_CLASS; int[] numComponents = {numMixes};
		// int contextClassificationType = ContextualGMMParams.FRICATIVE_GLIDELIQUID_NASAL_PLOSIVE_VOWEL_OTHER; int[]
		// numComponents = {128, 128, 128, 128, 128, 16};
		// int contextClassificationType = ContextualGMMParams.PHONEME_IDENTITY; int[] numComponents = {128};

		mainParametric(numTrainingFiles, numComponents, isContextualGMMs, contextClassificationType, "neutral", emotion, method);

		/*
		 * mainParametric(numTrainingFiles, numMixes, isContextualGMMs, contextClassificationType, "neutral", "angry", method);
		 * mainParametric(numTrainingFiles, numMixes, isContextualGMMs, contextClassificationType, "neutral", "happy", method);
		 * mainParametric(numTrainingFiles, numMixes, isContextualGMMs, contextClassificationType, "neutral", "sad", method);
		 */
	}

	public static void mainParametric(int numTrainingFiles, int[] numComponents, boolean isContextualGMMs,
			int contextClassificationType, String sourceTag, String targetTag, String method)
			throws UnsupportedAudioFileException, IOException, MaryConfigurationException {
		BaselinePreprocessor pp = new BaselinePreprocessor();
		BaselineFeatureExtractor fe = new BaselineFeatureExtractor();
		WeightedCodebookTrainerParams pa = new WeightedCodebookTrainerParams();
		JointGMMTrainerParams gp = new JointGMMTrainerParams();
		ContextualGMMParams cg = null;
		int i;

		pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAMES; // Frame-by-frame mapping of features
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAME_GROUPS; pa.codebookHeader.numNeighboursInFrameGroups
		// = 3; //Mapping of frame average features (no label information but fixed amount of neighbouring frames is used)
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABELS; //Mapping of label average features
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABEL_GROUPS; pa.codebookHeader.numNeighboursInLabelGroups
		// = 1; //Mapping of average features collected across label groups (i.e. vowels, consonants, etc)
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.SPEECH; //Mapping of average features collected across all
		// speech parts (i.e. like spectral equalization)

		pa.codebookHeader.sourceTag = sourceTag + method; // Source name tag (i.e. style or speaker identity)
		pa.codebookHeader.targetTag = targetTag + method; // Target name tag (i.e. style or speaker identity)

		pa.trainingBaseFolder = "D:/Oytun/DFKI/voices/Interspeech08_out2/" + sourceTag + "2" + targetTag; // Training base
																											// directory
		pa.sourceTrainingFolder = "D:/Oytun/DFKI/voices/Interspeech08/" + sourceTag + "/train_"
				+ String.valueOf(numTrainingFiles); // Source training folder
		pa.targetTrainingFolder = "D:/Oytun/DFKI/voices/Interspeech08/" + targetTag + "/train_"
				+ String.valueOf(numTrainingFiles); // Target training folder

		pa.indexMapFileExtension = ".imf"; // Index map file extensions

		pa.codebookHeader.lsfParams.dimension = 0; // Auto set
		pa.codebookHeader.lsfParams.preCoef = 0.97f;
		pa.codebookHeader.lsfParams.skipsize = 0.010f;
		pa.codebookHeader.lsfParams.winsize = 0.020f;
		pa.codebookHeader.lsfParams.windowType = Window.HAMMING;

		// Gaussian trainer params: commenting out results in using default value for each
		gp.isContextualGMMs = isContextualGMMs;
		gp.gmmEMTrainerParams.totalComponents = numComponents[0];
		gp.gmmEMTrainerParams.isDiagonalCovariance = true;
		gp.gmmEMTrainerParams.kmeansMaxIterations = 200;
		gp.gmmEMTrainerParams.kmeansMinClusterChangePercent = 0.1;
		gp.gmmEMTrainerParams.kmeansMinSamplesInOneCluster = 50;
		gp.gmmEMTrainerParams.emMinIterations = 200;
		gp.gmmEMTrainerParams.emMaxIterations = 2000;
		gp.gmmEMTrainerParams.isUpdateCovariances = true;
		gp.gmmEMTrainerParams.tinyLogLikelihoodChangePercent = 1e-5;
		gp.gmmEMTrainerParams.minCovarianceAllowed = 1e-4;
		gp.gmmEMTrainerParams.useNativeCLibTrainer = true;

		if (gp.isContextualGMMs) {
			GMMTrainerParams[] gmmParams = new GMMTrainerParams[numComponents.length];
			for (i = 0; i < numComponents.length; i++) {
				gmmParams[i] = new GMMTrainerParams(gp.gmmEMTrainerParams);
				gmmParams[i].totalComponents = numComponents[i];
			}
			String phoneSetFile = "D:/Mary TTS New/lib/modules/de/cap/phone-list-de.xml";
			cg = getContextualGMMParams(phoneSetFile, gmmParams, contextClassificationType);
		}

		String baseFile = StringUtils.checkLastSlash(pa.trainingBaseFolder) + pa.codebookHeader.sourceTag + "_X_"
				+ pa.codebookHeader.targetTag;
		// pa.codebookFile = baseFile + "_" + String.valueOf(gp.gmmEMTrainerParams.totalComponents) +
		// WeightedCodebookFile.DEFAULT_EXTENSION;
		pa.codebookFile = baseFile + "_" + String.valueOf(numTrainingFiles) + WeightedCodebookFile.DEFAULT_EXTENSION;
		pa.pitchMappingFile = baseFile + "_" + String.valueOf(numTrainingFiles) + PitchMappingFile.DEFAULT_EXTENSION;

		if (!isContextualGMMs)
			gp.jointGMMFile = baseFile + "_" + String.valueOf(numTrainingFiles) + "_"
					+ String.valueOf(gp.gmmEMTrainerParams.totalComponents) + JointGMMSet.DEFAULT_EXTENSION;
		else {
			gp.jointGMMFile = baseFile + "_" + String.valueOf(numTrainingFiles) + "_context"
					+ String.valueOf(contextClassificationType);
			for (i = 0; i < numComponents.length; i++)
				gp.jointGMMFile += "_" + String.valueOf(numComponents[i]);

			gp.jointGMMFile += JointGMMSet.DEFAULT_EXTENSION;
		}

		pa.isForcedAnalysis = false;

		pa.codebookHeader.ptcParams.windowSizeInSeconds = 0.040;
		pa.codebookHeader.ptcParams.skipSizeInSeconds = 0.005;
		pa.codebookHeader.ptcParams.voicingThreshold = 0.30;
		pa.codebookHeader.ptcParams.isDoublingCheck = false;
		pa.codebookHeader.ptcParams.isHalvingCheck = false;
		pa.codebookHeader.ptcParams.minimumF0 = 40.0f;
		pa.codebookHeader.ptcParams.maximumF0 = 400.0f;
		pa.codebookHeader.ptcParams.centerClippingRatio = 0.3;
		pa.codebookHeader.ptcParams.cutOff1 = pa.codebookHeader.ptcParams.minimumF0 - 20.0;
		pa.codebookHeader.ptcParams.cutOff2 = pa.codebookHeader.ptcParams.maximumF0 + 200.0;

		pa.codebookHeader.energyParams.windowSizeInSeconds = 0.020;
		pa.codebookHeader.energyParams.skipSizeInSeconds = 0.010;

		TotalStandardDeviations tsd = new TotalStandardDeviations();
		tsd.lsf = 1.5;
		tsd.f0 = 1.0;
		tsd.duration = 1.0;
		tsd.energy = 2.0;

		// Gaussian outlier eliminator
		// Decreasing totalStandardDeviations will lead to more outlier eliminations, i.e. smaller codebooks
		pa.gaussianEliminatorParams.isActive = true; // Set to false if you do not want to use this eliminator at all
		pa.gaussianEliminatorParams.isCheckLsfOutliers = true;
		pa.gaussianEliminatorParams.isEliminateTooSimilarLsf = true;
		pa.gaussianEliminatorParams.isCheckF0Outliers = true;
		pa.gaussianEliminatorParams.isCheckDurationOutliers = true;
		pa.gaussianEliminatorParams.isCheckEnergyOutliers = true;
		pa.gaussianEliminatorParams.totalStandardDeviations = new TotalStandardDeviations(tsd);
		//

		// KMeans one-to-many and many-to-one mapping eliminator
		pa.kmeansEliminatorParams.isActive = true; // Set to false if you do not want to use this eliminator at all

		// pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_LEAST_LIKELY_MAPPINGS;
		// pa.kmeansEliminatorParams.eliminationLikelihood = 0.20;

		pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_MEAN_DISTANCE_MISMATCHES;
		pa.kmeansEliminatorParams.distanceType = DistanceComputer.NORMALIZED_EUCLIDEAN_DISTANCE;
		// pa.kmeansEliminatorParams.distanceType = DistanceComputer.EUCLIDEAN_DISTANCE;
		pa.kmeansEliminatorParams.isGlobalVariance = true;

		// pa.kmeansEliminatorParams.eliminationAlgorithm =
		// KMeansMappingEliminatorParams.ELIMINATE_USING_SUBCLUSTER_MEAN_DISTANCES;

		pa.kmeansEliminatorParams.isSeparateClustering = false; // Cluster features separately(true) or together(false)?

		// Effective only when isSeparateClustering clustering is false
		tsd.general = 0.1;
		pa.kmeansEliminatorParams.numClusters = 30;

		// Effective only when isSeparateClustering clustering is true
		tsd.lsf = 1.0;
		tsd.f0 = 1.0;
		tsd.duration = 1.0;
		tsd.energy = 1.0;
		pa.kmeansEliminatorParams.numClustersLsf = 30;
		pa.kmeansEliminatorParams.numClustersF0 = 50;
		pa.kmeansEliminatorParams.numClustersDuration = 5;
		pa.kmeansEliminatorParams.numClustersEnergy = 5;

		pa.kmeansEliminatorParams.isCheckLsfOutliers = true;
		pa.kmeansEliminatorParams.isCheckF0Outliers = false;
		pa.kmeansEliminatorParams.isCheckDurationOutliers = false;
		pa.kmeansEliminatorParams.isCheckEnergyOutliers = false;
		//

		pa.labelsToExcludeFromTraining = new String[1];
		pa.labelsToExcludeFromTraining[0] = "_";

		pa.kmeansEliminatorParams.totalStandardDeviations = new TotalStandardDeviations(tsd);
		//

		JointGMMParallelTrainer t = new JointGMMParallelTrainer(pp, fe, pa, gp, cg);

		t.run();
	}

	// Testing voice conversion as a post-processor to enhance HMM based synthesis output
	// The idea is to train a voice conversion function between HMM outputs and natural recordings
	// Then, any HMM output is to be transformed with the voice conversion function to make it closer to original recordings
	public static void mainQuickTest(String[] args) throws UnsupportedAudioFileException, IOException, MaryConfigurationException {
		String wavBaseFolder = "D:/quickTest/";

		String sourceTag = "source";
		String targetTag = "target";
		String method;

		int numTrainingFiles = 50;
		boolean isContextualGMMs = false;
		int contextClassificationType = ContextualGMMParams.NO_PHONEME_CLASS;
		int[] numComponents = { 10 };
		// int contextClassificationType = ContextualGMMParams.SILENCE_SPEECH; int[] numComponents = {16, 128};
		// int contextClassificationType = ContextualGMMParams.VOWEL_SILENCE_CONSONANT; int[] numComponents = {128, 16, 128};
		// int contextClassificationType = ContextualGMMParams.PHONOLOGY_CLASS; int[] numComponents = {numMixes};
		// int contextClassificationType = ContextualGMMParams.FRICATIVE_GLIDELIQUID_NASAL_PLOSIVE_VOWEL_OTHER; int[]
		// numComponents = {128, 128, 128, 128, 128, 16};
		// int contextClassificationType = ContextualGMMParams.PHONEME_IDENTITY; int[] numComponents = {128};

		BaselinePreprocessor pp = new BaselinePreprocessor();
		BaselineFeatureExtractor fe = new BaselineFeatureExtractor();
		WeightedCodebookTrainerParams pa = new WeightedCodebookTrainerParams();
		JointGMMTrainerParams gp = new JointGMMTrainerParams();
		ContextualGMMParams cg = null;
		int i;

		pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAMES;
		method = "F"; // Frame-by-frame mapping of features
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAME_GROUPS; method = "FG";
		// pa.codebookHeader.numNeighboursInFrameGroups = 3; //Mapping of frame average features (no label information but fixed
		// amount of neighbouring frames is used)
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABELS; method = "L"; //Mapping of label average features
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABEL_GROUPS; method = "LG";
		// pa.codebookHeader.numNeighboursInLabelGroups = 1; //Mapping of average features collected across label groups (i.e.
		// vowels, consonants, etc)
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.SPEECH; method = "S"; //Mapping of average features
		// collected across all speech parts (i.e. like spectral equalization)

		pa.codebookHeader.vocalTractFeature = BaselineFeatureExtractor.LSF_FEATURES; // Use Lsf features - full speech to speech
																						// transformation
		// pa.codebookHeader.vocalTractFeature = BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES; //Use MFCC features -
		// currently supports only feature to featur etransformation

		pa.codebookHeader.sourceTag = sourceTag + method; // Source name tag (i.e. style or speaker identity)
		pa.codebookHeader.targetTag = targetTag + method; // Target name tag (i.e. style or speaker identity)

		pa.trainingBaseFolder = wavBaseFolder + "output/" + sourceTag + "2" + targetTag; // Training base directory
		pa.sourceTrainingFolder = wavBaseFolder + sourceTag + "/train_" + String.valueOf(numTrainingFiles) + "/"; // Source
																													// training
																													// folder
		pa.targetTrainingFolder = wavBaseFolder + targetTag + "/train_" + String.valueOf(numTrainingFiles) + "/"; // Target
																													// training
																													// folder

		pa.indexMapFileExtension = ".imf"; // Index map file extensions

		pa.codebookHeader.lsfParams.dimension = 0; // Auto set
		pa.codebookHeader.lsfParams.preCoef = 0.97f;
		pa.codebookHeader.lsfParams.skipsize = 0.010f;
		pa.codebookHeader.lsfParams.winsize = 0.020f;
		pa.codebookHeader.lsfParams.windowType = Window.HAMMING;

		// Gaussian trainer params: commenting out results in using default value for each
		gp.vocalTractFeature = pa.codebookHeader.vocalTractFeature;
		gp.isContextualGMMs = isContextualGMMs;
		gp.gmmEMTrainerParams.totalComponents = numComponents[0];
		gp.gmmEMTrainerParams.isDiagonalCovariance = true;
		gp.gmmEMTrainerParams.kmeansMaxIterations = 200;
		gp.gmmEMTrainerParams.kmeansMinClusterChangePercent = 0.1;
		gp.gmmEMTrainerParams.kmeansMinSamplesInOneCluster = 50;
		gp.gmmEMTrainerParams.emMinIterations = 100;
		gp.gmmEMTrainerParams.emMaxIterations = 400;
		gp.gmmEMTrainerParams.isUpdateCovariances = true;
		gp.gmmEMTrainerParams.tinyLogLikelihoodChangePercent = 1e-5;
		gp.gmmEMTrainerParams.minCovarianceAllowed = 1e-4;
		gp.gmmEMTrainerParams.useNativeCLibTrainer = true;

		if (gp.isContextualGMMs) {
			GMMTrainerParams[] gmmParams = new GMMTrainerParams[numComponents.length];
			for (i = 0; i < numComponents.length; i++) {
				gmmParams[i] = new GMMTrainerParams(gp.gmmEMTrainerParams);
				gmmParams[i].totalComponents = numComponents[i];
			}
			String phoneSetFile = "D:/Mary TTS New/lib/modules/de/cap/phone-list-de.xml";
			cg = getContextualGMMParams(phoneSetFile, gmmParams, contextClassificationType);
		}

		String baseFile = StringUtils.checkLastSlash(pa.trainingBaseFolder) + pa.codebookHeader.sourceTag + "_X_"
				+ pa.codebookHeader.targetTag;
		// pa.codebookFile = baseFile + "_" + String.valueOf(gp.gmmEMTrainerParams.totalComponents) +
		// WeightedCodebookFile.DEFAULT_EXTENSION;
		pa.codebookFile = baseFile + "_" + String.valueOf(numTrainingFiles) + WeightedCodebookFile.DEFAULT_EXTENSION;
		pa.pitchMappingFile = baseFile + "_" + String.valueOf(numTrainingFiles) + PitchMappingFile.DEFAULT_EXTENSION;

		if (!isContextualGMMs)
			gp.jointGMMFile = baseFile + "_" + String.valueOf(numTrainingFiles) + "_"
					+ String.valueOf(gp.gmmEMTrainerParams.totalComponents) + JointGMMSet.DEFAULT_EXTENSION;
		else {
			gp.jointGMMFile = baseFile + "_" + String.valueOf(numTrainingFiles) + "_context"
					+ String.valueOf(contextClassificationType);
			for (i = 0; i < numComponents.length; i++)
				gp.jointGMMFile += "_" + String.valueOf(numComponents[i]);

			gp.jointGMMFile += JointGMMSet.DEFAULT_EXTENSION;
		}

		pa.isForcedAnalysis = false;

		pa.codebookHeader.ptcParams.windowSizeInSeconds = 0.040;
		pa.codebookHeader.ptcParams.skipSizeInSeconds = 0.005;
		pa.codebookHeader.ptcParams.voicingThreshold = 0.30;
		pa.codebookHeader.ptcParams.isDoublingCheck = false;
		pa.codebookHeader.ptcParams.isHalvingCheck = false;
		pa.codebookHeader.ptcParams.minimumF0 = 40.0f;
		pa.codebookHeader.ptcParams.maximumF0 = 400.0f;
		pa.codebookHeader.ptcParams.centerClippingRatio = 0.3;
		pa.codebookHeader.ptcParams.cutOff1 = pa.codebookHeader.ptcParams.minimumF0 - 20.0;
		pa.codebookHeader.ptcParams.cutOff2 = pa.codebookHeader.ptcParams.maximumF0 + 200.0;

		pa.codebookHeader.energyParams.windowSizeInSeconds = 0.020;
		pa.codebookHeader.energyParams.skipSizeInSeconds = 0.010;

		TotalStandardDeviations tsd = new TotalStandardDeviations();
		tsd.lsf = 1.5;
		tsd.f0 = 1.0;
		tsd.duration = 1.0;
		tsd.energy = 2.0;

		// Gaussian outlier eliminator
		// Decreasing totalStandardDeviations will lead to more outlier eliminations, i.e. smaller codebooks
		pa.gaussianEliminatorParams.isActive = true; // Set to false if you do not want to use this eliminator at all
		pa.gaussianEliminatorParams.isCheckLsfOutliers = true;
		pa.gaussianEliminatorParams.isEliminateTooSimilarLsf = false;
		pa.gaussianEliminatorParams.isCheckF0Outliers = true;
		pa.gaussianEliminatorParams.isCheckDurationOutliers = true;
		pa.gaussianEliminatorParams.isCheckEnergyOutliers = true;
		pa.gaussianEliminatorParams.totalStandardDeviations = new TotalStandardDeviations(tsd);
		//

		// KMeans one-to-many and many-to-one mapping eliminator
		pa.kmeansEliminatorParams.isActive = false; // Set to false if you do not want to use this eliminator at all

		// pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_LEAST_LIKELY_MAPPINGS;
		// pa.kmeansEliminatorParams.eliminationLikelihood = 0.20;

		pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_MEAN_DISTANCE_MISMATCHES;
		pa.kmeansEliminatorParams.distanceType = DistanceComputer.NORMALIZED_EUCLIDEAN_DISTANCE;
		// pa.kmeansEliminatorParams.distanceType = DistanceComputer.EUCLIDEAN_DISTANCE;
		pa.kmeansEliminatorParams.isGlobalVariance = true;

		// pa.kmeansEliminatorParams.eliminationAlgorithm =
		// KMeansMappingEliminatorParams.ELIMINATE_USING_SUBCLUSTER_MEAN_DISTANCES;

		pa.kmeansEliminatorParams.isSeparateClustering = false; // Cluster features separately(true) or together(false)?

		// Effective only when isSeparateClustering clustering is false
		tsd.general = 0.1;
		pa.kmeansEliminatorParams.numClusters = 30;

		// Effective only when isSeparateClustering clustering is true
		tsd.lsf = 1.0;
		tsd.f0 = 1.0;
		tsd.duration = 1.0;
		tsd.energy = 1.0;
		pa.kmeansEliminatorParams.numClustersLsf = 30;
		pa.kmeansEliminatorParams.numClustersF0 = 50;
		pa.kmeansEliminatorParams.numClustersDuration = 5;
		pa.kmeansEliminatorParams.numClustersEnergy = 5;

		pa.kmeansEliminatorParams.isCheckLsfOutliers = false;
		pa.kmeansEliminatorParams.isCheckF0Outliers = false;
		pa.kmeansEliminatorParams.isCheckDurationOutliers = false;
		pa.kmeansEliminatorParams.isCheckEnergyOutliers = false;
		//

		pa.kmeansEliminatorParams.totalStandardDeviations = new TotalStandardDeviations(tsd);
		//

		pa.labelsToExcludeFromTraining = new String[1];
		pa.labelsToExcludeFromTraining[0] = "_";

		JointGMMParallelTrainer t = new JointGMMParallelTrainer(pp, fe, pa, gp, cg);

		t.run();
	}

	/**
	 * Depending on the parameters it will train GMMs. For example the ouput in this example will be: sourceF_X_targetF_99_10.jgs
	 * &rarr; numTrainingFiles = 99, numComponents = 10 (10 mixes) Input: two directories source and target containing:
	 * /Neutral-Spike-Conversion/source/train_99/*.wav and *.lab /Neutral-Spike-Conversion/target/train_99/*.wav and *.lab In
	 * these directories it will calculate *.lsf, *.ptc, *.ene Output:
	 * /Neutral-Spike-Conversion/ouput/source2target/sourceF_X_targetF_99_10.jgs
	 * 
	 * @param args
	 *            args
	 * @throws UnsupportedAudioFileException
	 *             UnsupportedAudioFileException
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public static void mainQuickTest2(String[] args) throws UnsupportedAudioFileException, IOException,
			MaryConfigurationException {
		String wavBaseFolder = "/project/mary/marcela/VoiceConversion/Neutral-Spike-Conversion/";

		String sourceTag = "source";
		String targetTag = "target";
		String method;

		int numTrainingFiles = 99;
		boolean isContextualGMMs = false;
		int contextClassificationType = ContextualGMMParams.NO_PHONEME_CLASS;
		int[] numComponents = { 10 };
		// int contextClassificationType = ContextualGMMParams.SILENCE_SPEECH; int[] numComponents = {16, 128};
		// int contextClassificationType = ContextualGMMParams.VOWEL_SILENCE_CONSONANT; int[] numComponents = {128, 16, 128};
		// int contextClassificationType = ContextualGMMParams.PHONOLOGY_CLASS; int[] numComponents = {numMixes};
		// int contextClassificationType = ContextualGMMParams.FRICATIVE_GLIDELIQUID_NASAL_PLOSIVE_VOWEL_OTHER; int[]
		// numComponents = {128, 128, 128, 128, 128, 16};
		// int contextClassificationType = ContextualGMMParams.PHONEME_IDENTITY; int[] numComponents = {128};

		BaselinePreprocessor pp = new BaselinePreprocessor();
		BaselineFeatureExtractor fe = new BaselineFeatureExtractor();
		WeightedCodebookTrainerParams pa = new WeightedCodebookTrainerParams();
		JointGMMTrainerParams gp = new JointGMMTrainerParams();
		ContextualGMMParams cg = null;
		int i;

		pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAMES;
		method = "F"; // Frame-by-frame mapping of features
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAME_GROUPS; method = "FG";
		// pa.codebookHeader.numNeighboursInFrameGroups = 3; //Mapping of frame average features (no label information but fixed
		// amount of neighbouring frames is used)
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABELS; method = "L"; //Mapping of label average features
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABEL_GROUPS; method = "LG";
		// pa.codebookHeader.numNeighboursInLabelGroups = 1; //Mapping of average features collected across label groups (i.e.
		// vowels, consonants, etc)
		// pa.codebookHeader.codebookType = WeightedCodebookFileHeader.SPEECH; method = "S"; //Mapping of average features
		// collected across all speech parts (i.e. like spectral equalization)

		pa.codebookHeader.vocalTractFeature = BaselineFeatureExtractor.LSF_FEATURES; // Use Lsf features - full speech to speech
																						// transformation
		// pa.codebookHeader.vocalTractFeature = BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES; //Use MFCC features -
		// currently supports only feature to featur etransformation

		pa.codebookHeader.sourceTag = sourceTag + method; // Source name tag (i.e. style or speaker identity)
		pa.codebookHeader.targetTag = targetTag + method; // Target name tag (i.e. style or speaker identity)

		pa.trainingBaseFolder = wavBaseFolder + "output/" + sourceTag + "2" + targetTag; // Training base directory
		pa.sourceTrainingFolder = wavBaseFolder + sourceTag + "/train_" + String.valueOf(numTrainingFiles) + "/"; // Source
																													// training
																													// folder
		pa.targetTrainingFolder = wavBaseFolder + targetTag + "/train_" + String.valueOf(numTrainingFiles) + "/"; // Target
																													// training
																													// folder

		pa.indexMapFileExtension = ".imf"; // Index map file extensions

		pa.codebookHeader.lsfParams.dimension = 0; // Auto set
		pa.codebookHeader.lsfParams.preCoef = 0.97f;
		pa.codebookHeader.lsfParams.skipsize = 0.010f;
		pa.codebookHeader.lsfParams.winsize = 0.020f;
		pa.codebookHeader.lsfParams.windowType = Window.HAMMING;

		// Gaussian trainer params: commenting out results in using default value for each
		gp.vocalTractFeature = pa.codebookHeader.vocalTractFeature;
		gp.isContextualGMMs = isContextualGMMs;
		gp.gmmEMTrainerParams.totalComponents = numComponents[0];
		gp.gmmEMTrainerParams.isDiagonalCovariance = true;
		gp.gmmEMTrainerParams.kmeansMaxIterations = 200;
		gp.gmmEMTrainerParams.kmeansMinClusterChangePercent = 0.1;
		gp.gmmEMTrainerParams.kmeansMinSamplesInOneCluster = 50;
		gp.gmmEMTrainerParams.emMinIterations = 100;
		gp.gmmEMTrainerParams.emMaxIterations = 400;
		gp.gmmEMTrainerParams.isUpdateCovariances = true;
		gp.gmmEMTrainerParams.tinyLogLikelihoodChangePercent = 1e-5;
		gp.gmmEMTrainerParams.minCovarianceAllowed = 1e-4;
		gp.gmmEMTrainerParams.useNativeCLibTrainer = true;

		if (gp.isContextualGMMs) {
			GMMTrainerParams[] gmmParams = new GMMTrainerParams[numComponents.length];
			for (i = 0; i < numComponents.length; i++) {
				gmmParams[i] = new GMMTrainerParams(gp.gmmEMTrainerParams);
				gmmParams[i].totalComponents = numComponents[i];
			}
			// String phoneSetFile = "D:/Mary TTS New/lib/modules/de/cap/phone-list-de.xml";
			String phoneSetFile = "/project/mary/marcela/openmary/lib/modules/de/cap/phone-list-de.xml";
			cg = getContextualGMMParams(phoneSetFile, gmmParams, contextClassificationType);
		}

		String baseFile = StringUtils.checkLastSlash(pa.trainingBaseFolder) + pa.codebookHeader.sourceTag + "_X_"
				+ pa.codebookHeader.targetTag;
		// pa.codebookFile = baseFile + "_" + String.valueOf(gp.gmmEMTrainerParams.totalComponents) +
		// WeightedCodebookFile.DEFAULT_EXTENSION;
		pa.codebookFile = baseFile + "_" + String.valueOf(numTrainingFiles) + WeightedCodebookFile.DEFAULT_EXTENSION;
		pa.pitchMappingFile = baseFile + "_" + String.valueOf(numTrainingFiles) + PitchMappingFile.DEFAULT_EXTENSION;

		if (!isContextualGMMs)
			gp.jointGMMFile = baseFile + "_" + String.valueOf(numTrainingFiles) + "_"
					+ String.valueOf(gp.gmmEMTrainerParams.totalComponents) + JointGMMSet.DEFAULT_EXTENSION;
		else {
			gp.jointGMMFile = baseFile + "_" + String.valueOf(numTrainingFiles) + "_context"
					+ String.valueOf(contextClassificationType);
			for (i = 0; i < numComponents.length; i++)
				gp.jointGMMFile += "_" + String.valueOf(numComponents[i]);

			gp.jointGMMFile += JointGMMSet.DEFAULT_EXTENSION;
		}

		pa.isForcedAnalysis = false;

		pa.codebookHeader.ptcParams.windowSizeInSeconds = 0.040;
		pa.codebookHeader.ptcParams.skipSizeInSeconds = 0.005;
		pa.codebookHeader.ptcParams.voicingThreshold = 0.30;
		pa.codebookHeader.ptcParams.isDoublingCheck = false;
		pa.codebookHeader.ptcParams.isHalvingCheck = false;
		pa.codebookHeader.ptcParams.minimumF0 = 40.0f;
		pa.codebookHeader.ptcParams.maximumF0 = 400.0f;
		pa.codebookHeader.ptcParams.centerClippingRatio = 0.3;
		pa.codebookHeader.ptcParams.cutOff1 = pa.codebookHeader.ptcParams.minimumF0 - 20.0;
		pa.codebookHeader.ptcParams.cutOff2 = pa.codebookHeader.ptcParams.maximumF0 + 200.0;

		pa.codebookHeader.energyParams.windowSizeInSeconds = 0.020;
		pa.codebookHeader.energyParams.skipSizeInSeconds = 0.010;

		TotalStandardDeviations tsd = new TotalStandardDeviations();
		tsd.lsf = 1.5;
		tsd.f0 = 1.0;
		tsd.duration = 1.0;
		tsd.energy = 2.0;

		// Gaussian outlier eliminator
		// Decreasing totalStandardDeviations will lead to more outlier eliminations, i.e. smaller codebooks
		pa.gaussianEliminatorParams.isActive = true; // Set to false if you do not want to use this eliminator at all
		pa.gaussianEliminatorParams.isCheckLsfOutliers = true;
		pa.gaussianEliminatorParams.isEliminateTooSimilarLsf = false;
		pa.gaussianEliminatorParams.isCheckF0Outliers = true;
		pa.gaussianEliminatorParams.isCheckDurationOutliers = true;
		pa.gaussianEliminatorParams.isCheckEnergyOutliers = true;
		pa.gaussianEliminatorParams.totalStandardDeviations = new TotalStandardDeviations(tsd);
		//

		// KMeans one-to-many and many-to-one mapping eliminator
		pa.kmeansEliminatorParams.isActive = false; // Set to false if you do not want to use this eliminator at all

		// pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_LEAST_LIKELY_MAPPINGS;
		// pa.kmeansEliminatorParams.eliminationLikelihood = 0.20;

		pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_MEAN_DISTANCE_MISMATCHES;
		pa.kmeansEliminatorParams.distanceType = DistanceComputer.NORMALIZED_EUCLIDEAN_DISTANCE;
		// pa.kmeansEliminatorParams.distanceType = DistanceComputer.EUCLIDEAN_DISTANCE;
		pa.kmeansEliminatorParams.isGlobalVariance = true;

		// pa.kmeansEliminatorParams.eliminationAlgorithm =
		// KMeansMappingEliminatorParams.ELIMINATE_USING_SUBCLUSTER_MEAN_DISTANCES;

		pa.kmeansEliminatorParams.isSeparateClustering = false; // Cluster features separately(true) or together(false)?

		// Effective only when isSeparateClustering clustering is false
		tsd.general = 0.1;
		pa.kmeansEliminatorParams.numClusters = 30;

		// Effective only when isSeparateClustering clustering is true
		tsd.lsf = 1.0;
		tsd.f0 = 1.0;
		tsd.duration = 1.0;
		tsd.energy = 1.0;
		pa.kmeansEliminatorParams.numClustersLsf = 30;
		pa.kmeansEliminatorParams.numClustersF0 = 50;
		pa.kmeansEliminatorParams.numClustersDuration = 5;
		pa.kmeansEliminatorParams.numClustersEnergy = 5;

		pa.kmeansEliminatorParams.isCheckLsfOutliers = false;
		pa.kmeansEliminatorParams.isCheckF0Outliers = false;
		pa.kmeansEliminatorParams.isCheckDurationOutliers = false;
		pa.kmeansEliminatorParams.isCheckEnergyOutliers = false;
		//

		pa.kmeansEliminatorParams.totalStandardDeviations = new TotalStandardDeviations(tsd);
		//

		pa.labelsToExcludeFromTraining = new String[1];
		pa.labelsToExcludeFromTraining[0] = "_";

		JointGMMParallelTrainer t = new JointGMMParallelTrainer(pp, fe, pa, gp, cg);

		t.run();
	}

	public static ContextualGMMParams getContextualGMMParams(String phoneSetFile, GMMTrainerParams[] params,
			int contextClassificationType) throws MaryConfigurationException {
		AllophoneSet allophoneSet = AllophoneSet.getAllophoneSet(phoneSetFile);
		assert allophoneSet != null;
		ContextualGMMParams cg = new ContextualGMMParams(allophoneSet, params, contextClassificationType);

		return cg;
	}
}
