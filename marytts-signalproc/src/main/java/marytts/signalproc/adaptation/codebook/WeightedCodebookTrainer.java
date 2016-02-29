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

import marytts.signalproc.adaptation.AdaptationUtils;
import marytts.signalproc.adaptation.BaselineAdaptationSet;
import marytts.signalproc.adaptation.BaselineFeatureCollection;
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.BaselinePreprocessor;
import marytts.signalproc.adaptation.BaselineTrainer;
import marytts.signalproc.adaptation.IndexMap;
import marytts.signalproc.adaptation.prosody.PitchMappingFile;
import marytts.signalproc.adaptation.prosody.PitchTrainer;
import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;

/**
 * 
 * Baseline class for weighted codebook training
 * 
 * @author Oytun T&uuml;rk
 */
public class WeightedCodebookTrainer extends BaselineTrainer {

	public WeightedCodebookTrainerParams wcParams;
	public WeightedCodebookOutlierEliminator outlierEliminator;

	public WeightedCodebookTrainer(BaselinePreprocessor pp, BaselineFeatureExtractor fe, WeightedCodebookTrainerParams pa) {
		super(pp, fe);

		wcParams = new WeightedCodebookTrainerParams(pa);
		outlierEliminator = new WeightedCodebookOutlierEliminator();
	}

	// Call this function after initializing the trainer to perform training
	public void run() throws IOException, UnsupportedAudioFileException {
		if (checkParams()) {
			BaselineAdaptationSet sourceTrainingSet = new BaselineAdaptationSet(wcParams.sourceTrainingFolder);
			BaselineAdaptationSet targetTrainingSet = new BaselineAdaptationSet(wcParams.targetTrainingFolder);

			int[] map = getIndexedMapping(sourceTrainingSet, targetTrainingSet);

			train(sourceTrainingSet, targetTrainingSet, map);
		}
	}

	// Validate parameters
	public boolean checkParams() {
		boolean bContinue = true;

		wcParams.trainingBaseFolder = StringUtils.checkLastSlash(wcParams.trainingBaseFolder);
		wcParams.sourceTrainingFolder = StringUtils.checkLastSlash(wcParams.sourceTrainingFolder);
		wcParams.targetTrainingFolder = StringUtils.checkLastSlash(wcParams.targetTrainingFolder);

		FileUtils.createDirectory(wcParams.trainingBaseFolder);

		if (!FileUtils.exists(wcParams.trainingBaseFolder) || !FileUtils.isDirectory(wcParams.trainingBaseFolder)) {
			System.out.println("Error! Training base folder " + wcParams.trainingBaseFolder + " not found.");
			bContinue = false;
		}

		if (!FileUtils.exists(wcParams.sourceTrainingFolder) || !FileUtils.isDirectory(wcParams.sourceTrainingFolder)) {
			System.out.println("Error! Source training folder " + wcParams.sourceTrainingFolder + " not found.");
			bContinue = false;
		}

		if (!FileUtils.exists(wcParams.targetTrainingFolder) || !FileUtils.isDirectory(wcParams.targetTrainingFolder)) {
			System.out.println("Error! Target training folder " + wcParams.targetTrainingFolder + " not found.");
			bContinue = false;
		}

		wcParams.temporaryCodebookFile = wcParams.codebookFile + ".temp";

		return bContinue;
	}

	// General purpose training with indexed pairs
	// <map> is a vector of same length as sourceItems showing the index of the corresponding target item
	// for each source item. This allows to specify the target files in any order, i.e. file names are not required to be in
	// alphabetical order
	public void train(BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int[] map)
			throws IOException, UnsupportedAudioFileException {
		if (sourceTrainingSet.items != null && targetTrainingSet.items != null && map != null) {
			if (sourceTrainingSet.items.length != targetTrainingSet.items.length || sourceTrainingSet.items.length != map.length) {
				throw new RuntimeException("Lengths of source, target and map must be the same");
			}
			int numItems = sourceTrainingSet.items.length;

			if (numItems > 0) {
				preprocessor.run(sourceTrainingSet);
				preprocessor.run(targetTrainingSet);

				int desiredFeatures = wcParams.codebookHeader.vocalTractFeature + BaselineFeatureExtractor.F0_FEATURES
						+ BaselineFeatureExtractor.ENERGY_FEATURES;

				featureExtractor.run(sourceTrainingSet, wcParams, desiredFeatures);
				featureExtractor.run(targetTrainingSet, wcParams, desiredFeatures);
			}

			WeightedCodebookFeatureCollection fcol = collectFeatures(sourceTrainingSet, targetTrainingSet, map);

			learnMapping(fcol, sourceTrainingSet, targetTrainingSet, map);

			outlierEliminator.run(wcParams);

			deleteTemporaryFiles(fcol, sourceTrainingSet, targetTrainingSet);
		}
	}

	// For parallel training, sourceItems and targetItems should have at least map.length elements (ensured if this function is
	// called through train)
	public WeightedCodebookFeatureCollection collectFeatures(BaselineAdaptationSet sourceTrainingSet,
			BaselineAdaptationSet targetTrainingSet, int[] map) throws IOException {
		WeightedCodebookFeatureCollection fcol = new WeightedCodebookFeatureCollection(wcParams, map.length);
		int i;
		IndexMap imap = null;

		if (wcParams.codebookHeader.codebookType == WeightedCodebookFileHeader.FRAMES) {
			for (i = 0; i < map.length; i++) {
				if (wcParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.LSF_FEATURES) {
					imap = AdaptationUtils.mapFramesFeatures(sourceTrainingSet.items[i].labelFile,
							targetTrainingSet.items[map[i]].labelFile, sourceTrainingSet.items[i].lsfFile,
							targetTrainingSet.items[map[i]].lsfFile, wcParams.codebookHeader.vocalTractFeature,
							wcParams.labelsToExcludeFromTraining);
				} else if (wcParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES) {
					imap = AdaptationUtils.mapFramesFeatures(sourceTrainingSet.items[i].labelFile,
							targetTrainingSet.items[map[i]].labelFile, sourceTrainingSet.items[i].mfccFile,
							targetTrainingSet.items[map[i]].mfccFile, wcParams.codebookHeader.vocalTractFeature,
							wcParams.labelsToExcludeFromTraining);
				}

				try {
					imap.writeToFile(fcol.indexMapFiles[i]);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else if (wcParams.codebookHeader.codebookType == WeightedCodebookFileHeader.FRAME_GROUPS) {
			for (i = 0; i < map.length; i++) {
				if (wcParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.LSF_FEATURES) {
					imap = AdaptationUtils.mapFrameGroupsFeatures(sourceTrainingSet.items[i].labelFile,
							targetTrainingSet.items[map[i]].labelFile, sourceTrainingSet.items[i].lsfFile,
							targetTrainingSet.items[map[i]].lsfFile, wcParams.codebookHeader.numNeighboursInFrameGroups,
							wcParams.codebookHeader.vocalTractFeature, wcParams.labelsToExcludeFromTraining);
				} else if (wcParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES) {
					imap = AdaptationUtils.mapFrameGroupsFeatures(sourceTrainingSet.items[i].labelFile,
							targetTrainingSet.items[map[i]].labelFile, sourceTrainingSet.items[i].mfccFile,
							targetTrainingSet.items[map[i]].mfccFile, wcParams.codebookHeader.numNeighboursInFrameGroups,
							wcParams.codebookHeader.vocalTractFeature, wcParams.labelsToExcludeFromTraining);
				}

				try {
					imap.writeToFile(fcol.indexMapFiles[i]);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else if (wcParams.codebookHeader.codebookType == WeightedCodebookFileHeader.LABELS) {
			for (i = 0; i < map.length; i++) {
				if (wcParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.LSF_FEATURES) {
					imap = AdaptationUtils.mapLabelsFeatures(sourceTrainingSet.items[i].labelFile,
							targetTrainingSet.items[map[i]].labelFile, sourceTrainingSet.items[i].lsfFile,
							targetTrainingSet.items[map[i]].lsfFile, wcParams.codebookHeader.vocalTractFeature,
							wcParams.labelsToExcludeFromTraining);
				} else if (wcParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES) {
					imap = AdaptationUtils.mapLabelsFeatures(sourceTrainingSet.items[i].labelFile,
							targetTrainingSet.items[map[i]].labelFile, sourceTrainingSet.items[i].mfccFile,
							targetTrainingSet.items[map[i]].mfccFile, wcParams.codebookHeader.vocalTractFeature,
							wcParams.labelsToExcludeFromTraining);
				}

				try {
					imap.writeToFile(fcol.indexMapFiles[i]);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else if (wcParams.codebookHeader.codebookType == WeightedCodebookFileHeader.LABEL_GROUPS) {
			for (i = 0; i < map.length; i++) {
				if (wcParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.LSF_FEATURES) {
					imap = AdaptationUtils.mapLabelGroupsFeatures(sourceTrainingSet.items[i].labelFile,
							targetTrainingSet.items[map[i]].labelFile, sourceTrainingSet.items[i].lsfFile,
							targetTrainingSet.items[map[i]].lsfFile, wcParams.codebookHeader.numNeighboursInLabelGroups,
							wcParams.codebookHeader.vocalTractFeature, wcParams.labelsToExcludeFromTraining);
				} else if (wcParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES) {
					imap = AdaptationUtils.mapLabelGroupsFeatures(sourceTrainingSet.items[i].labelFile,
							targetTrainingSet.items[map[i]].labelFile, sourceTrainingSet.items[i].mfccFile,
							targetTrainingSet.items[map[i]].mfccFile, wcParams.codebookHeader.numNeighboursInLabelGroups,
							wcParams.codebookHeader.vocalTractFeature, wcParams.labelsToExcludeFromTraining);
				}

				try {
					imap.writeToFile(fcol.indexMapFiles[i]);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else if (wcParams.codebookHeader.codebookType == WeightedCodebookFileHeader.SPEECH) {
			if (wcParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.LSF_FEATURES)
				imap = AdaptationUtils.mapSpeechFeatures();
			else if (wcParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES)
				imap = AdaptationUtils.mapSpeechFeatures();

			try {
				imap.writeToFile(fcol.indexMapFiles[0]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return fcol;
	}

	public void learnMapping(BaselineFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet,
			BaselineAdaptationSet targetTrainingSet, int[] map) throws IOException {
		assert fcol instanceof WeightedCodebookFeatureCollection;

		learnMapping((WeightedCodebookFeatureCollection) fcol, sourceTrainingSet, targetTrainingSet, map);
	}

	// This function generates the codebooks from training pairs
	public void learnMapping(WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet,
			BaselineAdaptationSet targetTrainingSet, int[] map) throws IOException {
		WeightedCodebookFeatureMapper featureMapper = null;

		if (wcParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.LSF_FEATURES)
			featureMapper = new WeightedCodebookLsfMapper(wcParams);
		else if (wcParams.codebookHeader.vocalTractFeature == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES)
			featureMapper = new WeightedCodebookMfccMapper(wcParams);

		if (featureMapper != null) {
			WeightedCodebookFile temporaryCodebookFile = new WeightedCodebookFile(wcParams.temporaryCodebookFile,
					WeightedCodebookFile.OPEN_FOR_WRITE);
			PitchMappingFile pitchMappingFile = new PitchMappingFile(wcParams.pitchMappingFile, PitchMappingFile.OPEN_FOR_WRITE);

			if (wcParams.codebookHeader.codebookType == WeightedCodebookFileHeader.FRAMES)
				featureMapper.learnMappingFrames(temporaryCodebookFile, (WeightedCodebookFeatureCollection) fcol,
						sourceTrainingSet, targetTrainingSet, map);
			else if (wcParams.codebookHeader.codebookType == WeightedCodebookFileHeader.FRAME_GROUPS)
				featureMapper.learnMappingFrameGroups(temporaryCodebookFile, (WeightedCodebookFeatureCollection) fcol,
						sourceTrainingSet, targetTrainingSet, map);
			else if (wcParams.codebookHeader.codebookType == WeightedCodebookFileHeader.LABELS)
				featureMapper.learnMappingLabels(temporaryCodebookFile, (WeightedCodebookFeatureCollection) fcol,
						sourceTrainingSet, targetTrainingSet, map);
			else if (wcParams.codebookHeader.codebookType == WeightedCodebookFileHeader.LABEL_GROUPS)
				featureMapper.learnMappingLabelGroups(temporaryCodebookFile, (WeightedCodebookFeatureCollection) fcol,
						sourceTrainingSet, targetTrainingSet, map);
			else if (wcParams.codebookHeader.codebookType == WeightedCodebookFileHeader.SPEECH)
				featureMapper.learnMappingSpeech(temporaryCodebookFile, (WeightedCodebookFeatureCollection) fcol,
						sourceTrainingSet, targetTrainingSet, map);

			temporaryCodebookFile.close();

			PitchTrainer ptcTrainer = new PitchTrainer(wcParams);
			ptcTrainer.learnMapping(pitchMappingFile, (WeightedCodebookFeatureCollection) fcol, sourceTrainingSet,
					targetTrainingSet, map);

			pitchMappingFile.close();
		} else
			System.out.println("Error! Specified feature mapper does not exist...");
	}

	public void deleteTemporaryFiles(WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet,
			BaselineAdaptationSet targetTrainingSet) {
		FileUtils.delete(fcol.indexMapFiles, true);
		// FileUtils.delete(sourceTrainingSet.getLsfFiles(), true);
		// FileUtils.delete(targetTrainingSet.getLsfFiles(), true);
		// FileUtils.delete(sourceTrainingSet.getF0Files(), true);
		// FileUtils.delete(targetTrainingSet.getF0Files(), true);
		// FileUtils.delete(sourceTrainingSet.getEnergyFiles(), true);
		// FileUtils.delete(targetTrainingSet.getEnergyFiles(), true);

		FileUtils.delete(wcParams.temporaryCodebookFile);
	}
}
