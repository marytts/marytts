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

import marytts.signalproc.adaptation.BaselineAdaptationItem;
import marytts.signalproc.adaptation.BaselineAdaptationSet;
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.BaselinePostprocessor;
import marytts.signalproc.adaptation.BaselinePreprocessor;
import marytts.signalproc.adaptation.BaselineTransformerParams;
import marytts.signalproc.adaptation.FdpsolaAdapter;
import marytts.signalproc.adaptation.TargetLsfCopyMapper;
import marytts.signalproc.adaptation.prosody.PitchMapping;
import marytts.signalproc.adaptation.prosody.PitchMappingFile;
import marytts.signalproc.adaptation.prosody.PitchStatistics;
import marytts.signalproc.adaptation.prosody.PitchTransformationData;
import marytts.signalproc.adaptation.prosody.ProsodyTransformerParams;
import marytts.signalproc.adaptation.smoothing.SmoothingDefinitions;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;

/**
 * 
 * This class implements transformation for weighted codebook mapping based voice conversion using parallel training data (i.e.
 * source and target data in pairs of audio recordings which have identical content)
 * 
 * Reference for weighted codebook mapping: Arslan, L. M., 1999, “Speaker Transformation Algorithm using Segmental Codebooks”,
 * Speech Communication, 28, pp. 211-226.
 * 
 * Reference for weighted frame mapping: T&uuml;rk, O., 2007 “Cross-Lingual Voice Conversion”, PhD Thesis, Bogazici University.
 * 
 * @author Oytun T&uuml;rk
 */
public class WeightedCodebookParallelTransformer extends WeightedCodebookTransformer {
	public WeightedCodebookTransformerParams pa;
	public WeightedCodebookMapper mapper;

	private WeightedCodebookFile codebookFile;
	public WeightedCodebook codebook;

	private PitchMappingFile pitchMappingFile;
	public PitchMapping pitchMapping;

	public WeightedCodebookParallelTransformer(BaselinePreprocessor pp, BaselineFeatureExtractor fe, BaselinePostprocessor po,
			WeightedCodebookTransformerParams pa) {
		super(pp, fe, po, pa);

		codebook = null;
		mapper = null;
	}

	public boolean checkParams() throws IOException {
		super.checkParams();

		params.inputFolder = StringUtils.checkLastSlash(params.inputFolder);
		params.outputBaseFolder = StringUtils.checkLastSlash(params.outputBaseFolder);
		codebookFile = null;

		// Read codebook header only
		if (!FileUtils.exists(params.codebookFile)) {
			System.out.println("Error: Codebook file " + params.codebookFile + " not found!");
			return false;
		} else // Read lsfParams from the codebook header
		{
			codebookFile = new WeightedCodebookFile(params.codebookFile, WeightedCodebookFile.OPEN_FOR_READ);
			codebook = new WeightedCodebook();

			codebook.header = codebookFile.readCodebookHeader();
			params.lsfParams = new LsfFileHeader(codebook.header.lsfParams);
			params.mapperParams.lpOrder = params.lsfParams.dimension;
		}
		//

		// Read pitch mapping file header
		if (!FileUtils.exists(params.pitchMappingFile)) {
			System.out.println("Error: Pitch mapping file " + params.pitchMappingFile + " not found!");
			return false;
		} else // Read pitch mapping info from the pitch mapping file header
		{
			pitchMappingFile = new PitchMappingFile(params.pitchMappingFile, PitchMappingFile.OPEN_FOR_READ);
			pitchMapping = new PitchMapping();

			pitchMapping.header = pitchMappingFile.readPitchMappingHeader();
		}
		//

		if (!FileUtils.exists(params.inputFolder) || !FileUtils.isDirectory(params.inputFolder)) {
			System.out.println("Error: Input folder " + params.inputFolder + " not found!");
			return false;
		}

		if (!FileUtils.isDirectory(params.outputBaseFolder)) {
			System.out.println("Creating output base folder " + params.outputBaseFolder + "...");
			FileUtils.createDirectory(params.outputBaseFolder);
		}

		if (params.outputFolderInfoString != "") {
			params.outputFolder = params.outputBaseFolder + params.outputFolderInfoString + "_prosody"
					+ String.valueOf(params.prosodyParams.pitchStatisticsType) + "x"
					+ String.valueOf(params.prosodyParams.pitchTransformationMethod) + "x"
					+ String.valueOf(params.prosodyParams.durationTransformationMethod);
		} else {
			params.outputFolder = params.outputBaseFolder + "_prosody" + String.valueOf(params.prosodyParams.pitchStatisticsType)
					+ "x" + String.valueOf(params.prosodyParams.pitchTransformationMethod) + "x"
					+ String.valueOf(params.prosodyParams.durationTransformationMethod);
		}

		if (!FileUtils.isDirectory(params.outputFolder)) {
			System.out.println("Creating output folder " + params.outputFolder + "...");
			FileUtils.createDirectory(params.outputFolder);
		}

		if (!params.isSeparateProsody)
			params.isSaveVocalTractOnlyVersion = false;

		return true;
	}

	public void transform(BaselineAdaptationSet inputSet, BaselineAdaptationSet outputSet) throws UnsupportedAudioFileException {
		System.out.println("Transformation started...");

		if (inputSet.items != null && outputSet.items != null) {
			int numItems = Math.min(inputSet.items.length, outputSet.items.length);

			if (numItems > 0) {
				preprocessor.run(inputSet);

				int desiredFeatures = BaselineFeatureExtractor.F0_FEATURES + BaselineFeatureExtractor.ENERGY_FEATURES;

				try {
					featureExtractor.run(inputSet, params, desiredFeatures);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			// Read the codebook
			codebookFile.readCodebookFileExcludingHeader(codebook);

			// Read the pitch mapping file
			pitchMappingFile.readPitchMappingFileExcludingHeader(pitchMapping);

			// Create a mapper object
			mapper = new WeightedCodebookMapper(params.mapperParams);

			// Do the transformations now
			for (int i = 0; i < numItems; i++) {
				try {
					transformOneItem(inputSet.items[i], outputSet.items[i], params, mapper, codebook, pitchMapping);
				} catch (UnsupportedAudioFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				System.out.println("Transformed file " + String.valueOf(i + 1) + " of " + String.valueOf(numItems));
			}
		}

		System.out.println("Transformation completed...");
	}

	// This function performs the actual voice conversion
	public static void transformOneItem(BaselineAdaptationItem inputItem, BaselineAdaptationItem outputItem,
			WeightedCodebookTransformerParams wctParams, WeightedCodebookMapper wcMapper, WeightedCodebook wCodebook,
			PitchTransformationData pMap) throws UnsupportedAudioFileException, IOException {
		TargetLsfCopyMapper tcMapper = new TargetLsfCopyMapper();

		if (wctParams.isFixedRateVocalTractConversion) {
			if (wctParams.prosodyParams.pitchTransformationMethod != ProsodyTransformerParams.NO_TRANSFORMATION
					|| wctParams.prosodyParams.durationTransformationMethod != ProsodyTransformerParams.NO_TRANSFORMATION) {
				wctParams.isSeparateProsody = true;
			}
		}

		// Desired values should be specified in the following four parameters
		double[] pscales = { 1.0 };
		double[] tscales = { 1.0 };
		double[] escales = { 1.0 };
		double[] vscales = { 1.0 };
		//

		// These are for fixed rate vocal tract transformation: Do not change these!!!
		double[] pscalesNone = { 1.0 };
		double[] tscalesNone = { 1.0 };
		double[] escalesNone = { 1.0 };
		double[] vscalesNone = { 1.0 };
		boolean noPscaleFromFestivalUttFile = false;
		boolean noTscaleFromFestivalUttFile = false;
		boolean noEscaleFromTargetWavFile = false;
		//

		FdpsolaAdapter adapter = null;
		WeightedCodebookTransformerParams currentWctParams = new WeightedCodebookTransformerParams(wctParams);

		String firstPassOutputWavFile = "";
		String smoothedVocalTractFile = "";

		if (currentWctParams.isTemporalSmoothing) // Need to do two pass for smoothing
			currentWctParams.isSeparateProsody = true;

		if (currentWctParams.isSeparateProsody) // First pass with no prosody modifications
		{
			firstPassOutputWavFile = StringUtils.getFolderName(outputItem.audioFile)
					+ StringUtils.getFileName(outputItem.audioFile) + "_vt.wav";
			smoothedVocalTractFile = StringUtils.getFolderName(outputItem.audioFile)
					+ StringUtils.getFileName(outputItem.audioFile) + "_vt.vtf";
			int tmpPitchTransformationMethod = currentWctParams.prosodyParams.pitchTransformationMethod;
			int tmpDurationTransformationMethod = currentWctParams.prosodyParams.durationTransformationMethod;
			currentWctParams.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.NO_TRANSFORMATION;
			currentWctParams.prosodyParams.durationTransformationMethod = ProsodyTransformerParams.NO_TRANSFORMATION;

			boolean tmpPitchFromTargetFile = currentWctParams.isPitchFromTargetFile;
			boolean tmpDurationFromTargetFile = currentWctParams.isDurationFromTargetFile;
			boolean tmpEnergyFromTargetFile = currentWctParams.isEnergyFromTargetFile;
			currentWctParams.isPitchFromTargetFile = noPscaleFromFestivalUttFile;
			currentWctParams.isDurationFromTargetFile = noTscaleFromFestivalUttFile;
			currentWctParams.isEnergyFromTargetFile = noEscaleFromTargetWavFile;

			if (currentWctParams.isTemporalSmoothing) // This estimates the vocal tract filter but performs no prosody and vocal
														// tract transformations
			{
				currentWctParams.smoothingState = SmoothingDefinitions.ESTIMATING_SMOOTHED_VOCAL_TRACT;
				currentWctParams.smoothedVocalTractFile = smoothedVocalTractFile; // It is an output at first pass

				adapter = new FdpsolaAdapter(inputItem, firstPassOutputWavFile, currentWctParams, pscalesNone, tscalesNone,
						escalesNone, vscalesNone);

				adapter.bSilent = !currentWctParams.isDisplayProcessingFrameCount;
				if (!currentWctParams.isLsfsFromTargetFile)
					adapter.fdpsolaOnline(wcMapper, wCodebook, pMap);
				else
					adapter.fdpsolaOnline(tcMapper, wCodebook, pMap);

				currentWctParams.smoothingState = SmoothingDefinitions.TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT;
				currentWctParams.smoothedVocalTractFile = smoothedVocalTractFile; // Now it is an input

				adapter = new FdpsolaAdapter(inputItem, firstPassOutputWavFile, currentWctParams, pscalesNone, tscalesNone,
						escalesNone, vscalesNone);
			} else {
				currentWctParams.smoothingMethod = SmoothingDefinitions.NO_SMOOTHING;
				currentWctParams.smoothingState = SmoothingDefinitions.NONE;
				currentWctParams.smoothedVocalTractFile = "";

				adapter = new FdpsolaAdapter(inputItem, firstPassOutputWavFile, currentWctParams, pscalesNone, tscalesNone,
						escalesNone, vscalesNone);
			}

			currentWctParams.isPitchFromTargetFile = tmpPitchFromTargetFile;
			currentWctParams.isDurationFromTargetFile = tmpDurationFromTargetFile;
			currentWctParams.isEnergyFromTargetFile = tmpEnergyFromTargetFile;

			// Then second step: prosody modification (with possible additional vocal tract scaling)
			if (adapter != null) {
				adapter.bSilent = !currentWctParams.isDisplayProcessingFrameCount;
				if (!currentWctParams.isLsfsFromTargetFile)
					adapter.fdpsolaOnline(wcMapper, wCodebook, pMap);
				else
					adapter.fdpsolaOnline(tcMapper, wCodebook, pMap);

				if (isScalingsRequired(pscales, tscales, escales, vscales)
						|| tmpPitchTransformationMethod != ProsodyTransformerParams.NO_TRANSFORMATION
						|| tmpDurationTransformationMethod != ProsodyTransformerParams.NO_TRANSFORMATION) {
					System.out.println("Performing prosody modifications...");

					currentWctParams.isVocalTractTransformation = false; // isVocalTractTransformation should be false
					currentWctParams.isFixedRateVocalTractConversion = false; // isFixedRateVocalTractConversion should be false
																				// to enable prosody modifications with FD-PSOLA
					currentWctParams.isResynthesizeVocalTractFromSourceModel = false; // isResynthesizeVocalTractFromSourceCodebook
																						// should be false
					currentWctParams.isVocalTractMatchUsingTargetModel = false; // isVocalTractMatchUsingTargetCodebook should be
																				// false
					currentWctParams.prosodyParams.pitchTransformationMethod = tmpPitchTransformationMethod;
					currentWctParams.prosodyParams.durationTransformationMethod = tmpDurationTransformationMethod;
					currentWctParams.smoothingMethod = SmoothingDefinitions.NO_SMOOTHING;
					currentWctParams.smoothingState = SmoothingDefinitions.NONE;
					currentWctParams.smoothedVocalTractFile = "";
					currentWctParams.isContextBasedPreselection = false;

					String tmpInputWavFile = inputItem.audioFile;
					inputItem.audioFile = firstPassOutputWavFile;

					adapter = new FdpsolaAdapter(inputItem, outputItem.audioFile, currentWctParams, pscales, tscales, escales,
							vscales);

					inputItem.audioFile = tmpInputWavFile;

					adapter.bSilent = true;
					adapter.fdpsolaOnline(null, wCodebook, pMap);
				} else
					// Copy output file
					FileUtils.copy(firstPassOutputWavFile, outputItem.audioFile);

				// Delete first pass output file
				if (!currentWctParams.isSaveVocalTractOnlyVersion)
					FileUtils.delete(firstPassOutputWavFile);

				System.out.println("Done...");
			}
		} else // Single-pass prosody+vocal tract transformation and modification
		{
			currentWctParams.smoothingMethod = SmoothingDefinitions.NO_SMOOTHING;
			currentWctParams.smoothingState = SmoothingDefinitions.NONE;
			currentWctParams.smoothedVocalTractFile = "";

			adapter = new FdpsolaAdapter(inputItem, outputItem.audioFile, currentWctParams, pscales, tscales, escales, vscales);

			adapter.bSilent = !wctParams.isDisplayProcessingFrameCount;

			if (!currentWctParams.isLsfsFromTargetFile)
				adapter.fdpsolaOnline(wcMapper, wCodebook, pMap);
			else
				adapter.fdpsolaOnline(tcMapper, wCodebook, pMap);
		}
	}

	public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
		// mainNeutralSad();
		mainQuickTest2();

	}

	public static void mainNeutralSad() throws IOException, UnsupportedAudioFileException {
		String emotion = "sad";
		String method = "F";
		String inputFolder = "D:/Oytun/DFKI/voices/Interspeech08/neutral/test_tts_" + emotion;
		String outputBaseFolder = "D:/Oytun/DFKI/voices/Interspeech08_out/neutral2" + emotion + "/neutral2" + emotion
				+ "Out_codebook" + method + "3";
		String baseFile = "D:/Oytun/DFKI/voices/Interspeech08_out/neutral2" + emotion + "/neutral" + method + "_X_" + emotion
				+ method + "_200";

		/*
		 * //for method: L boolean isSourceVocalTractSpectrumFromModel = true; int numBestMatches = 15; // Number of best matches
		 * in codebook boolean isTemporalSmoothing = false; int smoothingNumNeighbours = 1; boolean isContextBasedPreselection =
		 * false; int totalContextNeighbours = 2; boolean isPscaleFromFestivalUttFile = true; //false=>mean std dev tfm of pitch,
		 * true=>from target CART boolean isTscaleFromFestivalUttFile = true;
		 */

		// for method: F
		boolean isSourceVocalTractSpectrumFromModel = true;
		int numBestMatches = 15; // Number of best matches in codebook
		boolean isTemporalSmoothing = true;
		int smoothingNumNeighbours = 1;
		boolean isContextBasedPreselection = true;
		int totalContextNeighbours = 5;

		// Note that these two can be true or false together, not yet implemented separate processing
		boolean isPitchFromTargetFile = true; // false=>mean std dev tfm of pitch, true=>from target CART
		int pitchFromTargetMethod = ProsodyTransformerParams.FULL_CONTOUR;
		// int pitchFromTargetMethod = ProsodyTransformerParams.SENTENCE_MEAN;
		// int pitchFromTargetMethod = ProsodyTransformerParams.SENTENCE_MEAN_STDDEV;
		boolean isDurationFromTargetFile = true;
		int durationFromTargetMethod = ProsodyTransformerParams.PHONEME_DURATIONS;
		// int durationFromTargetMethod = ProsodyTransformerParams.TRIPHONE_DURATIONS;
		// int durationFromTargetMethod = ProsodyTransformerParams.SENTENCE_DURATION;
		boolean isEnergyFromTargetFile = false;
		boolean isLsfsFromTargetFile = false;
		int targetAlignmentFileType = BaselineTransformerParams.FESTIVAL_UTT;
		//

		String outputFolderInfoString = "isSrc" + String.valueOf(isSourceVocalTractSpectrumFromModel ? 1 : 0) + "_nBest"
				+ String.valueOf(numBestMatches) + "_smooth" + String.valueOf(isTemporalSmoothing ? 1 : 0) + "_"
				+ String.valueOf(smoothingNumNeighbours) + "_context" + String.valueOf(isContextBasedPreselection ? 1 : 0) + "_"
				+ String.valueOf(totalContextNeighbours) + "_psUtt" + String.valueOf(isPitchFromTargetFile ? 1 : 0) + "_tsUtt"
				+ String.valueOf(isDurationFromTargetFile ? 1 : 0);

		mainParametric(inputFolder, outputBaseFolder, baseFile, outputFolderInfoString, isSourceVocalTractSpectrumFromModel,
				numBestMatches, isTemporalSmoothing, smoothingNumNeighbours, isContextBasedPreselection, totalContextNeighbours,
				isPitchFromTargetFile, pitchFromTargetMethod, isDurationFromTargetFile, durationFromTargetMethod,
				isEnergyFromTargetFile, isLsfsFromTargetFile, targetAlignmentFileType);
	}

	/**
	 * This example uses the ouput of the example:
	 * marytts.signalproc.adaptation.codebook.WeightedCodebookParallelTrainer.mainQuickTest2() Input:
	 * /Neutral-Spike-Conversion/codebook/neutral2angry/neutralF_X_angryF_99.pmf
	 * /Neutral-Spike-Conversion/codebook/neutral/train_99/*.wav Ouput:
	 * /Neutral-Spike-Conversion/codebook/neutral2angry/neutral2angryOut_codebookF3/
	 * isSrc1_nBest15_smooth1_1_context1_5_psUtt1_tsUtt1_prosody1x0x0/*.wav
	 * 
	 * @throws IOException
	 *             IO exception
	 * @throws UnsupportedAudioFileException
	 *             unsupported audio file exception
	 */
	public static void mainQuickTest2() throws IOException, UnsupportedAudioFileException {
		String emotion = "angry";
		String method = "F";
		String inputFolder = "/project/mary/marcela/VoiceConversion/Neutral-Spike-Conversion/codebook/neutral/train_99";
		String outputBaseFolder = "/project/mary/marcela/VoiceConversion/Neutral-Spike-Conversion/codebook/neutral2" + emotion
				+ "/neutral2" + emotion + "Out_codebook" + method + "3";
		String baseFile = "/project/mary/marcela/VoiceConversion/Neutral-Spike-Conversion/codebook/neutral2" + emotion
				+ "/neutral" + method + "_X_" + emotion + method + "_99";

		/*
		 * //for method: L boolean isSourceVocalTractSpectrumFromModel = true; int numBestMatches = 15; // Number of best matches
		 * in codebook boolean isTemporalSmoothing = false; int smoothingNumNeighbours = 1; boolean isContextBasedPreselection =
		 * false; int totalContextNeighbours = 2; boolean isPscaleFromFestivalUttFile = true; //false=>mean std dev tfm of pitch,
		 * true=>from target CART boolean isTscaleFromFestivalUttFile = true;
		 */

		// for method: F
		boolean isSourceVocalTractSpectrumFromModel = true;
		int numBestMatches = 15; // Number of best matches in codebook
		boolean isTemporalSmoothing = true;
		int smoothingNumNeighbours = 1;
		boolean isContextBasedPreselection = true;
		int totalContextNeighbours = 5;

		// Note that these two can be true or false together, not yet implemented separate processing
		boolean isPitchFromTargetFile = true; // false=>mean std dev tfm of pitch, true=>from target CART
		int pitchFromTargetMethod = ProsodyTransformerParams.FULL_CONTOUR;
		// int pitchFromTargetMethod = ProsodyTransformerParams.SENTENCE_MEAN;
		// int pitchFromTargetMethod = ProsodyTransformerParams.SENTENCE_MEAN_STDDEV;
		boolean isDurationFromTargetFile = true;
		int durationFromTargetMethod = ProsodyTransformerParams.PHONEME_DURATIONS;
		// int durationFromTargetMethod = ProsodyTransformerParams.TRIPHONE_DURATIONS;
		// int durationFromTargetMethod = ProsodyTransformerParams.SENTENCE_DURATION;
		boolean isEnergyFromTargetFile = false;
		boolean isLsfsFromTargetFile = false;
		int targetAlignmentFileType = BaselineTransformerParams.FESTIVAL_UTT;
		//

		String outputFolderInfoString = "isSrc" + String.valueOf(isSourceVocalTractSpectrumFromModel ? 1 : 0) + "_nBest"
				+ String.valueOf(numBestMatches) + "_smooth" + String.valueOf(isTemporalSmoothing ? 1 : 0) + "_"
				+ String.valueOf(smoothingNumNeighbours) + "_context" + String.valueOf(isContextBasedPreselection ? 1 : 0) + "_"
				+ String.valueOf(totalContextNeighbours) + "_psUtt" + String.valueOf(isPitchFromTargetFile ? 1 : 0) + "_tsUtt"
				+ String.valueOf(isDurationFromTargetFile ? 1 : 0);

		mainParametric(inputFolder, outputBaseFolder, baseFile, outputFolderInfoString, isSourceVocalTractSpectrumFromModel,
				numBestMatches, isTemporalSmoothing, smoothingNumNeighbours, isContextBasedPreselection, totalContextNeighbours,
				isPitchFromTargetFile, pitchFromTargetMethod, isDurationFromTargetFile, durationFromTargetMethod,
				isEnergyFromTargetFile, isLsfsFromTargetFile, targetAlignmentFileType);
	}

	public static void mainParametric(String inputFolder, String outputBaseFolder, String baseFile,
			String outputFolderInfoString, boolean isSourceVocalTractSpectrumFromModel, int numBestMatches,
			boolean isTemporalSmoothing, int smoothingNumNeighbours, boolean isContextBasedPreselection,
			int totalContextNeighbours, boolean isPitchFromTargetFile, int pitchFromTargetMethod,
			boolean isDurationFromTargetFile, int durationFromTargetMethod, boolean isEnergyFromTargetFile,
			boolean isLsfsFromTargetFile, int targetAlignmentFileType) throws IOException, UnsupportedAudioFileException {
		BaselinePreprocessor pp = new BaselinePreprocessor();
		BaselineFeatureExtractor fe = new BaselineFeatureExtractor();
		BaselinePostprocessor po = new BaselinePostprocessor();
		WeightedCodebookTransformerParams pa = new WeightedCodebookTransformerParams();

		pa.isDisplayProcessingFrameCount = true;

		pa.inputFolder = inputFolder;
		pa.outputBaseFolder = outputBaseFolder;
		pa.codebookFile = baseFile + WeightedCodebookFile.DEFAULT_EXTENSION;
		pa.pitchMappingFile = baseFile + PitchMappingFile.DEFAULT_EXTENSION;

		pa.outputFolderInfoString = outputFolderInfoString;

		// Set codebook mapper parameters
		pa.mapperParams.numBestMatches = numBestMatches; // Number of best matches in codebook
		pa.mapperParams.weightingSteepness = 1.0; // Steepness of weighting function in range
													// [WeightedCodebookMapperParams.MIN_STEEPNESS,
													// WeightedCodebookMapperParams.MAX_STEEPNESS]
		pa.mapperParams.freqRange = 8000.0; // Frequency range to be considered around center freq when matching LSFs (note that
											// center freq is estimated automatically as the middle of most closest LSFs)

		// Distance measure for comparing source training and transformation features
		// pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_INVERSE_HARMONIC_DISTANCE;
		pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_INVERSE_HARMONIC_DISTANCE_SYMMETRIC;
		pa.mapperParams.alphaForSymmetric = 0.5; // Weighting factor for using weights of two lsf vectors in distance computation
													// relatively. The range is [0.0,1.0]
		// pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_EUCLIDEAN_DISTANCE;
		// pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_MAHALANOBIS_DISTANCE;
		// pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_ABSOLUTE_VALUE_DISTANCE;

		// Method for weighting best codebook matches
		pa.mapperParams.weightingMethod = WeightedCodebookMapperParams.EXPONENTIAL_HALF_WINDOW;
		// pa.mapperParams.weightingMethod = WeightedCodebookMapperParams.TRIANGLE_HALF_WINDOW;

		// //Mean and variance of a specific distance measure can be optionally kept in the following
		// two parameters for z-normalization
		pa.mapperParams.distanceMean = 0.0;
		pa.mapperParams.distanceVariance = 1.0;
		//

		pa.isForcedAnalysis = false;
		pa.isSourceVocalTractSpectrumFromModel = isSourceVocalTractSpectrumFromModel;
		pa.isVocalTractTransformation = true;
		pa.isResynthesizeVocalTractFromSourceModel = false;
		pa.isVocalTractMatchUsingTargetModel = false;

		pa.isSeparateProsody = true;
		pa.isSaveVocalTractOnlyVersion = true;
		pa.isFixedRateVocalTractConversion = true;

		pa.isContextBasedPreselection = isContextBasedPreselection;
		pa.totalContextNeighbours = totalContextNeighbours;

		// Prosody transformation
		pa.prosodyParams.pitchStatisticsType = PitchStatistics.STATISTICS_IN_HERTZ;
		// pa.prosodyParams.pitchStatisticsType = PitchStatistics.STATISTICS_IN_LOGHERTZ;

		pa.prosodyParams.durationTransformationMethod = ProsodyTransformerParams.NO_TRANSFORMATION;

		pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.NO_TRANSFORMATION;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_MEAN;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_STDDEV;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_RANGE;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_SLOPE;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_INTERCEPT;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_MEAN_STDDEV;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_MEAN_SLOPE;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_INTERCEPT_STDDEV;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_INTERCEPT_SLOPE;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_MEAN;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_STDDEV;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_RANGE;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_SLOPE;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_INTERCEPT;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_MEAN_STDDEV;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_MEAN_SLOPE;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_INTERCEPT_STDDEV;
		// pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_INTERCEPT_SLOPE;

		pa.prosodyParams.isUseInputMeanPitch = false;
		pa.prosodyParams.isUseInputStdDevPitch = false;
		pa.prosodyParams.isUseInputRangePitch = false;
		pa.prosodyParams.isUseInputInterceptPitch = false;
		pa.prosodyParams.isUseInputSlopePitch = false;
		//

		// Smoothing
		pa.isTemporalSmoothing = isTemporalSmoothing;
		pa.smoothingNumNeighbours = smoothingNumNeighbours;
		// pa.smoothingMethod = SmoothingDefinitions.OUTPUT_LSFCONTOUR_SMOOTHING;
		// pa.smoothingMethod = SmoothingDefinitions.OUTPUT_VOCALTRACTSPECTRUM_SMOOTHING;
		pa.smoothingMethod = SmoothingDefinitions.TRANSFORMATION_FILTER_SMOOTHING;
		//

		// TTS tests
		pa.isPitchFromTargetFile = isPitchFromTargetFile;
		pa.pitchFromTargetMethod = pitchFromTargetMethod;
		pa.isDurationFromTargetFile = isDurationFromTargetFile;
		pa.durationFromTargetMethod = durationFromTargetMethod;
		pa.isEnergyFromTargetFile = isEnergyFromTargetFile;
		pa.isLsfsFromTargetFile = isLsfsFromTargetFile;
		pa.targetAlignmentFileType = targetAlignmentFileType;
		//

		WeightedCodebookParallelTransformer t = new WeightedCodebookParallelTransformer(pp, fe, po, pa);
		t.run();
	}
}
