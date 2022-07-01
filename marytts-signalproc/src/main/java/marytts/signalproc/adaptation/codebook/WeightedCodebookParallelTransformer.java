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

import org.apache.commons.io.FilenameUtils;

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
			firstPassOutputWavFile = FilenameUtils.getFullPath(outputItem.audioFile)
					+ StringUtils.getFileName(outputItem.audioFile) + "_vt.wav";
			smoothedVocalTractFile = FilenameUtils.getFullPath(outputItem.audioFile)
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
}
