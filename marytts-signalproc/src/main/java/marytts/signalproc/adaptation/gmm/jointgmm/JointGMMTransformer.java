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

import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.io.FilenameUtils;

import marytts.machinelearning.ContextualGMMParams;
import marytts.signalproc.adaptation.BaselineAdaptationItem;
import marytts.signalproc.adaptation.BaselineAdaptationSet;
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.BaselinePostprocessor;
import marytts.signalproc.adaptation.BaselinePreprocessor;
import marytts.signalproc.adaptation.BaselineTransformer;
import marytts.signalproc.adaptation.BaselineTransformerParams;
import marytts.signalproc.adaptation.FdpsolaAdapter;
import marytts.signalproc.adaptation.MfccAdapter;
import marytts.signalproc.adaptation.TargetLsfCopyMapper;
import marytts.signalproc.adaptation.prosody.PitchMapping;
import marytts.signalproc.adaptation.prosody.PitchMappingFile;
import marytts.signalproc.adaptation.prosody.PitchStatistics;
import marytts.signalproc.adaptation.prosody.PitchTransformationData;
import marytts.signalproc.adaptation.prosody.ProsodyTransformerParams;
import marytts.signalproc.adaptation.smoothing.SmoothingDefinitions;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.signalproc.analysis.MfccFileHeader;
import marytts.util.io.BasenameList;
import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;

/**
 *
 * Voice conversion transformation using Joint-GMM approach.
 *
 * Reference: A. Kain and M. Macon, “Spectral voice conversion for text-to-speech synthesis,” in Proc. of the IEEE ICASSP 1998,
 * vol. 1, pp. 285-288.
 *
 * @author Oytun T&uuml;rk
 */
public class JointGMMTransformer extends BaselineTransformer {

	public JointGMMTransformerParams params;

	public JointGMMMapper mapper;
	public JointGMMSet jointGmmSet;

	private PitchMappingFile pitchMappingFile;
	public PitchMapping pitchMapping;

	public JointGMMTransformer(BaselinePreprocessor pp, BaselineFeatureExtractor fe, BaselinePostprocessor po,
			JointGMMTransformerParams pa) {
		super(pp, fe, po, pa);
		params = new JointGMMTransformerParams(pa);

		jointGmmSet = null;
		mapper = null;
	}

	public boolean checkParams() throws IOException {
		params.inputFolder = StringUtils.checkLastSlash(params.inputFolder);
		params.outputBaseFolder = StringUtils.checkLastSlash(params.outputBaseFolder);

		// Read joint GMM file
		JointGMM nonNullGmm = null;
		if (!FileUtils.exists(params.jointGmmFile)) {
			System.out.println("Error: Codebook file " + params.jointGmmFile + " not found!");
			return false;
		} else // Read full GMM from the joint GMM file
		{
			jointGmmSet = new JointGMMSet(params.jointGmmFile);

			assert jointGmmSet.gmms != null;

			for (int i = 0; i < jointGmmSet.gmms.length; i++) {
				if (jointGmmSet.gmms[i] != null) {
					nonNullGmm = new JointGMM(jointGmmSet.gmms[i]);
					break;
				}
			}

			if (nonNullGmm != null) {
				if (nonNullGmm.featureType == BaselineFeatureExtractor.LSF_FEATURES)
					params.lsfParams = new LsfFileHeader((LsfFileHeader) nonNullGmm.featureParams);
				else if (nonNullGmm.featureType == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES)
					params.mfccParams = new MfccFileHeader((MfccFileHeader) nonNullGmm.featureParams);
			}
		}

		if (nonNullGmm == null) {
			System.out.println("Error! All GMMs are null in " + params.jointGmmFile);
			return false;
		}
		//

		// Read pitch mapping file
		if (!FileUtils.exists(params.pitchMappingFile)) {
			System.out.println("Error: Pitch mapping file " + params.pitchMappingFile + " not found!");
			return false;
		} else // Read lsfParams from the codebook header
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
			params.outputFolder = params.outputBaseFolder + params.outputFolderInfoString + "_mixes"
					+ String.valueOf(nonNullGmm.source.totalComponents) + "_prosody"
					+ String.valueOf(params.prosodyParams.pitchStatisticsType) + "x"
					+ String.valueOf(params.prosodyParams.pitchTransformationMethod) + "x"
					+ String.valueOf(params.prosodyParams.durationTransformationMethod);
		} else {
			params.outputFolder = params.outputBaseFolder + "_mixes" + String.valueOf(nonNullGmm.source.totalComponents)
					+ "_prosody" + String.valueOf(params.prosodyParams.pitchStatisticsType) + "x"
					+ String.valueOf(params.prosodyParams.pitchTransformationMethod) + "x"
					+ String.valueOf(params.prosodyParams.durationTransformationMethod);
		}

		if (!FileUtils.isDirectory(params.outputFolder)) {
			System.out.println("Creating output folder " + params.outputFolder + "...");
			FileUtils.createDirectory(params.outputFolder);
		}

		if (!params.isSeparateProsody)
			params.isSaveVocalTractOnlyVersion = false;

		if (params.isPitchFromTargetFile)
			params.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.CUSTOM_TRANSFORMATION;

		if (params.isDurationFromTargetFile)
			params.prosodyParams.durationTransformationMethod = ProsodyTransformerParams.CUSTOM_TRANSFORMATION;

		if (params.isEnergyFromTargetFile)
			params.prosodyParams.energyTransformationMethod = ProsodyTransformerParams.CUSTOM_TRANSFORMATION;

		if (!params.isVocalTractTransformation && !params.isLsfsFromTargetFile)
			params.isTemporalSmoothing = false;

		return true;
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

			for (int i = 0; i < inputSet.items.length; i++) {
				outputSet.items[i].audioFile = outputFolder + StringUtils.getFileName(inputSet.items[i].audioFile) + "_output"
						+ BaselineAdaptationSet.WAV_EXTENSION_DEFAULT;
				outputSet.items[i].rawMfccFile = StringUtils.modifyExtension(outputSet.items[i].audioFile,
						BaselineAdaptationSet.RAWMFCC_EXTENSION_DEFAULT);
			}
		}

		return outputSet;
	}

	//

	public void transform(BaselineAdaptationSet inputSet, BaselineAdaptationSet outputSet) throws UnsupportedAudioFileException {
		System.out.println("Transformation started...");

		if (inputSet.items != null && outputSet.items != null) {
			int numItems = Math.min(inputSet.items.length, outputSet.items.length);

			if (numItems > 0) {
				preprocessor.run(inputSet);

				int desiredFeatures = BaselineFeatureExtractor.F0_FEATURES;

				try {
					featureExtractor.run(inputSet, params, desiredFeatures);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			// Read the pitch mapping file
			pitchMappingFile.readPitchMappingFileExcludingHeader(pitchMapping);

			// Create a mapper object
			mapper = new JointGMMMapper();

			// Do the transformations now
			for (int i = 0; i < numItems; i++) {
				try {
					transformOneItem(inputSet.items[i], outputSet.items[i], params, mapper, jointGmmSet, pitchMapping);
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
			JointGMMTransformerParams wctParams, JointGMMMapper jgMapper, JointGMMSet jgSet, PitchTransformationData pMap)
			throws UnsupportedAudioFileException, IOException {
		TargetLsfCopyMapper tcMapper = new TargetLsfCopyMapper();

		// LSF transformation is done fully from audio to audio
		if (jgSet.gmms[0].featureType == BaselineFeatureExtractor.LSF_FEATURES) {
			if (wctParams.isTemporalSmoothing) // Need to do two pass for smoothing
			{
				wctParams.isSeparateProsody = true;
				wctParams.isFixedRateVocalTractConversion = true;
			}

			if (wctParams.isFixedRateVocalTractConversion) {
				if (wctParams.prosodyParams.pitchTransformationMethod != ProsodyTransformerParams.NO_TRANSFORMATION
						|| wctParams.prosodyParams.pitchTransformationMethod != ProsodyTransformerParams.CUSTOM_TRANSFORMATION
						|| wctParams.prosodyParams.durationTransformationMethod != ProsodyTransformerParams.CUSTOM_TRANSFORMATION
						|| wctParams.prosodyParams.energyTransformationMethod != ProsodyTransformerParams.CUSTOM_TRANSFORMATION) {
					wctParams.isSeparateProsody = true;
				}
			}

			// Desired values should be specified in the following four parameters
			double[] pscales = { 1.0 };
			double[] tscales = { 1.0 };
			double[] escales = { 1.0 };
			double[] vscales = { 1.0 };

			// These are for fixed rate vocal tract transformation: Do not change these!!!
			double[] pscalesNone = { 1.0 };
			double[] tscalesNone = { 1.0 };
			double[] escalesNone = { 1.0 };
			double[] vscalesNone = { 1.0 };
			//

			FdpsolaAdapter adapter = null;
			JointGMMTransformerParams currentWctParams = new JointGMMTransformerParams(wctParams);

			String firstPassOutputWavFile = "";
			String smoothedVocalTractFile = "";

			if (currentWctParams.isSeparateProsody) // First pass with no prosody modifications
			{
				firstPassOutputWavFile = FilenameUtils.getFullPath(outputItem.audioFile)
						+ StringUtils.getFileName(outputItem.audioFile) + "_vt.wav";
				smoothedVocalTractFile = FilenameUtils.getFullPath(outputItem.audioFile)
						+ StringUtils.getFileName(outputItem.audioFile) + "_vt.vtf";
				int tmpPitchTransformationMethod = currentWctParams.prosodyParams.pitchTransformationMethod;
				int tmpDurationTransformationMethod = currentWctParams.prosodyParams.durationTransformationMethod;
				int tmpEnergyTransformationMethod = currentWctParams.prosodyParams.energyTransformationMethod;
				currentWctParams.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.NO_TRANSFORMATION;
				currentWctParams.prosodyParams.durationTransformationMethod = ProsodyTransformerParams.NO_TRANSFORMATION;
				currentWctParams.prosodyParams.energyTransformationMethod = ProsodyTransformerParams.NO_TRANSFORMATION;

				boolean tmpPitchFromTargetFile = currentWctParams.isPitchFromTargetFile;
				boolean tmpDurationFromTargetFile = currentWctParams.isDurationFromTargetFile;
				boolean tmpEnergyFromTargetFile = currentWctParams.isEnergyFromTargetFile;
				currentWctParams.isPitchFromTargetFile = false;
				currentWctParams.isDurationFromTargetFile = false;
				currentWctParams.isEnergyFromTargetFile = false;

				if (currentWctParams.isTemporalSmoothing) // This estimates the vocal tract filter but performs no prosody and
															// vocal tract transformations
				{
					currentWctParams.smoothingState = SmoothingDefinitions.ESTIMATING_SMOOTHED_VOCAL_TRACT;
					currentWctParams.smoothedVocalTractFile = smoothedVocalTractFile; // It is an output at first pass

					adapter = new FdpsolaAdapter(inputItem, firstPassOutputWavFile, currentWctParams, pscalesNone, tscalesNone,
							escalesNone, vscalesNone);

					adapter.bSilent = !currentWctParams.isDisplayProcessingFrameCount;

					if (!currentWctParams.isLsfsFromTargetFile)
						adapter.fdpsolaOnline(jgMapper, jgSet, pMap);
					else
						adapter.fdpsolaOnline(tcMapper, jgSet, pMap);

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

				if (adapter != null) {
					adapter.bSilent = !currentWctParams.isDisplayProcessingFrameCount;

					if (!currentWctParams.isLsfsFromTargetFile)
						adapter.fdpsolaOnline(jgMapper, jgSet, pMap);
					else
						adapter.fdpsolaOnline(tcMapper, jgSet, pMap);

					// Then second step: prosody modification (with possible additional vocal tract scaling)
					if (isScalingsRequired(pscales, tscales, escales, vscales)
							|| tmpPitchTransformationMethod != ProsodyTransformerParams.NO_TRANSFORMATION) {
						System.out.println("Performing prosody modifications...");

						currentWctParams.isVocalTractTransformation = false; // isVocalTractTransformation should be false
						currentWctParams.isFixedRateVocalTractConversion = false; // isFixedRateVocalTractConversion should be
																					// false to enable prosody modifications with
																					// FD-PSOLA
						currentWctParams.isResynthesizeVocalTractFromSourceModel = false; // isResynthesizeVocalTractFromSourceCodebook
																							// should be false
						currentWctParams.isVocalTractMatchUsingTargetModel = false; // isVocalTractMatchUsingTargetCodebook should
																					// be false
						currentWctParams.prosodyParams.pitchTransformationMethod = tmpPitchTransformationMethod;
						currentWctParams.prosodyParams.durationTransformationMethod = tmpDurationTransformationMethod;
						currentWctParams.prosodyParams.energyTransformationMethod = tmpEnergyTransformationMethod;
						currentWctParams.smoothingMethod = SmoothingDefinitions.NO_SMOOTHING;
						currentWctParams.smoothingState = SmoothingDefinitions.NONE;
						currentWctParams.smoothedVocalTractFile = "";

						String tmpInputWavFile = inputItem.audioFile;
						inputItem.audioFile = firstPassOutputWavFile;

						adapter = new FdpsolaAdapter(inputItem, outputItem.audioFile, currentWctParams, pscales, tscales,
								escales, vscales);

						inputItem.audioFile = tmpInputWavFile;

						adapter.bSilent = true;
						adapter.fdpsolaOnline(null, jgSet, pMap);
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

				adapter = new FdpsolaAdapter(inputItem, outputItem.audioFile, currentWctParams, pscales, tscales, escales,
						vscales);

				adapter.bSilent = !wctParams.isDisplayProcessingFrameCount;

				if (!currentWctParams.isLsfsFromTargetFile)
					adapter.fdpsolaOnline(jgMapper, jgSet, pMap);
				else
					adapter.fdpsolaOnline(tcMapper, jgSet, pMap);
			}
		} else if (jgSet.gmms[0].featureType == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES) {
			// MFCC transformation is done from MFCC file to MFCC file
			MfccAdapter adapter = null;
			JointGMMTransformerParams currentWctParams = new JointGMMTransformerParams(wctParams);

			adapter = new MfccAdapter(inputItem, outputItem.rawMfccFile, currentWctParams);

			// Then second step: vocal tract transformation
			if (adapter != null) {
				adapter.bSilent = !currentWctParams.isDisplayProcessingFrameCount;
				adapter.transformOnline(jgMapper, jgSet); // Call voice conversion version

				System.out.println("Done...");
			}
		}
	}
}
