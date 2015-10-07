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

import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.adaptation.codebook.WeightedCodebookTrainerParams;
import marytts.signalproc.adaptation.codebook.WeightedCodebookTransformerParams;
import marytts.signalproc.adaptation.gmm.jointgmm.JointGMMTransformerParams;
import marytts.signalproc.analysis.EnergyContourRms;
import marytts.signalproc.analysis.EnergyFileHeader;
import marytts.signalproc.analysis.F0TrackerAutocorrelationHeuristic;
import marytts.signalproc.analysis.LsfAnalyser;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.signalproc.analysis.MfccFileHeader;
import marytts.signalproc.analysis.PitchFileHeader;
import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;

/**
 * 
 * Baseline class for acoustic feature analysis for voice conversion
 * 
 * @author Oytun T&uuml;rk
 */
public class BaselineFeatureExtractor {
	// Add more as necessary & make sure you can discriminate each using AND(&) operator
	// from a single integer that represents desired analyses (See the function run())
	public static final int NOT_DEFINED = Integer.parseInt("00000000", 2);
	public static final int LSF_FEATURES = Integer.parseInt("00000001", 2);
	public static final int F0_FEATURES = Integer.parseInt("00000010", 2);
	public static final int ENERGY_FEATURES = Integer.parseInt("00000100", 2);
	public static final int DURATION_FEATURES = Integer.parseInt("00001000", 2);
	public static final int MFCC_FEATURES_FROM_FILES = Integer.parseInt("00010000", 2);

	public BaselineFeatureExtractor() {
		this(null);
	}

	public BaselineFeatureExtractor(BaselineFeatureExtractor existing) {
		if (existing != null) {
			// Copy class members if you add any
		} else {
			// Set default class member values
		}

	}

	public void run(BaselineAdaptationSet fileSet, BaselineParams params, int desiredFeatures) throws IOException,
			UnsupportedAudioFileException {
		LsfFileHeader lsfParams = null;
		if (params instanceof WeightedCodebookTrainerParams)
			lsfParams = new LsfFileHeader(((WeightedCodebookTrainerParams) params).codebookHeader.lsfParams);
		else if (params instanceof WeightedCodebookTransformerParams)
			lsfParams = new LsfFileHeader(((WeightedCodebookTransformerParams) params).lsfParams);
		else if (params instanceof JointGMMTransformerParams)
			lsfParams = new LsfFileHeader(((JointGMMTransformerParams) params).lsfParams);

		PitchFileHeader ptcParams = null;
		if (params instanceof WeightedCodebookTrainerParams)
			ptcParams = new PitchFileHeader(((WeightedCodebookTrainerParams) params).codebookHeader.ptcParams);
		else if (params instanceof WeightedCodebookTransformerParams)
			ptcParams = new PitchFileHeader(((WeightedCodebookTransformerParams) params).ptcParams);
		else if (params instanceof JointGMMTransformerParams)
			ptcParams = new PitchFileHeader(((JointGMMTransformerParams) params).ptcParams);

		EnergyFileHeader energyParams = null;
		if (params instanceof WeightedCodebookTrainerParams)
			energyParams = new EnergyFileHeader(((WeightedCodebookTrainerParams) params).codebookHeader.energyParams);
		else if (params instanceof WeightedCodebookTransformerParams)
			energyParams = new EnergyFileHeader(((WeightedCodebookTransformerParams) params).energyParams);
		else if (params instanceof JointGMMTransformerParams)
			energyParams = new EnergyFileHeader(((JointGMMTransformerParams) params).energyParams);

		MfccFileHeader mfccParams = null;
		if (params instanceof WeightedCodebookTrainerParams)
			mfccParams = new MfccFileHeader(((WeightedCodebookTrainerParams) params).codebookHeader.mfccParams);
		else if (params instanceof WeightedCodebookTransformerParams)
			mfccParams = new MfccFileHeader(((WeightedCodebookTransformerParams) params).mfccParams);
		else if (params instanceof JointGMMTransformerParams)
			mfccParams = new MfccFileHeader(((JointGMMTransformerParams) params).mfccParams);

		boolean isForcedAnalysis = false;
		if (params instanceof WeightedCodebookTrainerParams)
			isForcedAnalysis = ((WeightedCodebookTrainerParams) params).isForcedAnalysis;
		else if (params instanceof WeightedCodebookTransformerParams)
			isForcedAnalysis = ((WeightedCodebookTransformerParams) params).isForcedAnalysis;
		else if (params instanceof JointGMMTransformerParams)
			isForcedAnalysis = ((JointGMMTransformerParams) params).isForcedAnalysis;

		// ADD more analyses as necessary
		if (StringUtils.isDesired(LSF_FEATURES, desiredFeatures))
			lsfAnalysis(fileSet, lsfParams, isForcedAnalysis);

		if (StringUtils.isDesired(F0_FEATURES, desiredFeatures))
			f0Analysis(fileSet, ptcParams, isForcedAnalysis);

		if (StringUtils.isDesired(ENERGY_FEATURES, desiredFeatures))
			energyAnalysis(fileSet, energyParams, isForcedAnalysis);

		if (StringUtils.isDesired(MFCC_FEATURES_FROM_FILES, desiredFeatures))
			checkMfccFiles(fileSet, mfccParams, isForcedAnalysis);
		//
	}

	public static void lsfAnalysis(BaselineAdaptationItem item, LsfFileHeader lsfParams, boolean isForcedAnalysis)
			throws IOException {
		BaselineAdaptationSet fileSet = new BaselineAdaptationSet(1);
		fileSet.items[0] = new BaselineAdaptationItem(item);
		lsfAnalysis(fileSet, lsfParams, isForcedAnalysis);
	}

	public static void lsfAnalysis(BaselineAdaptationSet fileSet, LsfFileHeader lsfParams, boolean isForcedAnalysis)
			throws IOException {
		System.err.println("Starting LSF analysis...");

		boolean bAnalyze;
		for (int i = 0; i < fileSet.items.length; i++) {
			bAnalyze = true;
			if (!isForcedAnalysis && FileUtils.exists(fileSet.items[i].lsfFile)) {
				LsfFileHeader tmpParams = new LsfFileHeader(fileSet.items[i].lsfFile);
				if (tmpParams.isIdenticalAnalysisParams(lsfParams))
					bAnalyze = false;
			}

			if (bAnalyze) {
				LsfAnalyser.lsfAnalyzeWavFile(fileSet.items[i].audioFile, fileSet.items[i].lsfFile, lsfParams);
				System.err.println("Extracted LSFs: " + fileSet.items[i].lsfFile);
			} else
				System.err.println("LSF file found with identical analysis parameters: " + fileSet.items[i].lsfFile);
		}

		System.err.println("LSF analysis completed...");
	}

	public static void f0Analysis(BaselineAdaptationSet fileSet, PitchFileHeader ptcParams, boolean isForcedAnalysis)
			throws UnsupportedAudioFileException, IOException {
		System.err.println("Starting f0 analysis...");

		boolean bAnalyze;
		F0TrackerAutocorrelationHeuristic p = new F0TrackerAutocorrelationHeuristic(ptcParams);

		for (int i = 0; i < fileSet.items.length; i++) {
			bAnalyze = true;

			if (!isForcedAnalysis && FileUtils.exists(fileSet.items[i].pitchFile)) // No f0 detection if ptc file already exists
				bAnalyze = false;

			if (bAnalyze) {
				p.pitchAnalyzeWavFile(fileSet.items[i].audioFile, fileSet.items[i].pitchFile);

				System.err.println("Extracted f0 contour: " + fileSet.items[i].pitchFile);
			} else
				System.err.println("F0 file found with identical analysis parameters: " + fileSet.items[i].pitchFile);
		}

		System.err.println("f0 analysis completed...");
	}

	public static void energyAnalysis(BaselineAdaptationSet fileSet, EnergyFileHeader energyParams, boolean isForcedAnalysis)
			throws UnsupportedAudioFileException, IOException {
		System.err.println("Starting energy analysis...");

		boolean bAnalyze;
		EnergyContourRms e = null;

		for (int i = 0; i < fileSet.items.length; i++) {
			bAnalyze = true;

			if (!isForcedAnalysis && FileUtils.exists(fileSet.items[i].energyFile)) // No f0 detection if ptc file already exists
				bAnalyze = false;

			if (bAnalyze) {
				e = new EnergyContourRms(fileSet.items[i].audioFile, fileSet.items[i].energyFile,
						energyParams.windowSizeInSeconds, energyParams.skipSizeInSeconds);

				System.err.println("Extracted energy contour: " + fileSet.items[i].energyFile);
			} else
				System.err.println("Energy file found with identical analysis parameters: " + fileSet.items[i].energyFile);
		}

		System.err.println("Energy analysis completed...");
	}

	public static void checkMfccFiles(BaselineAdaptationSet fileSet, MfccFileHeader mfccParams, boolean isForcedAnalysis)
			throws IOException {
		System.err.println("Attempting to read MFCC parameters from files...");

		for (int i = 0; i < fileSet.items.length; i++) {
			if (!FileUtils.exists(fileSet.items[i].mfccFile))
				System.err.println("MFCC files not found!Please use SPTK generated raw MFCC file named as "
						+ fileSet.items[i].mfccFile);
		}

		System.err.println("MFCC files verified...");
	}
}
