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
import java.util.Arrays;

import marytts.signalproc.adaptation.BaselineAdaptationSet;
import marytts.signalproc.adaptation.Context;
import marytts.signalproc.adaptation.IndexMap;
import marytts.signalproc.analysis.EnergyContourRms;
import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.MfccFileHeader;
import marytts.signalproc.analysis.Mfccs;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * 
 * Implements mapping functionality of MFCCs between source and target
 * 
 * @author Oytun T&uuml;rk
 */
public class WeightedCodebookMfccMapper extends WeightedCodebookFeatureMapper {
	private WeightedCodebookTrainerParams params;

	public WeightedCodebookMfccMapper(WeightedCodebookTrainerParams pa) {
		params = new WeightedCodebookTrainerParams(pa);
	}

	public void learnMappingFrames(WeightedCodebookFile codebookFile, WeightedCodebookFeatureCollection fcol,
			BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int[] map) throws IOException {
		assert params.codebookHeader.codebookType == WeightedCodebookFileHeader.FRAMES;

		IndexMap imap = new IndexMap();
		int i, j, index;

		WeightedCodebookEntry entry = null;

		boolean bHeaderWritten = false;

		// Take directly the corresponding source-target frame vocal tract feature vectors and write them as a new entry
		for (i = 0; i < fcol.indexMapFiles.length; i++) {
			System.out.println("MFCC mapping for pair " + String.valueOf(i + 1) + " of "
					+ String.valueOf(fcol.indexMapFiles.length) + ":");

			try {
				imap.readFromFile(fcol.indexMapFiles[i]); // imap keeps information about a single source-target pair only
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (imap.files != null && sourceTrainingSet.items.length > i && targetTrainingSet.items.length > i) {
				// Mfccs
				Mfccs srcFeatures = new Mfccs(sourceTrainingSet.items[i].mfccFile);
				Mfccs tgtFeatures = new Mfccs(targetTrainingSet.items[map[i]].mfccFile);
				//

				// Pitch: for outlier elimination not prosody modeling!
				PitchReaderWriter sourceF0s = new PitchReaderWriter(sourceTrainingSet.items[i].pitchFile);
				PitchReaderWriter targetF0s = new PitchReaderWriter(targetTrainingSet.items[map[i]].pitchFile);
				//

				// Duration
				Labels sourceLabels = new Labels(sourceTrainingSet.items[i].labelFile);
				Labels targetLabels = new Labels(targetTrainingSet.items[map[i]].labelFile);
				//

				// Energy
				EnergyContourRms sourceEnergies = EnergyContourRms.ReadEnergyFile(sourceTrainingSet.items[i].energyFile);
				EnergyContourRms targetEnergies = EnergyContourRms.ReadEnergyFile(targetTrainingSet.items[map[i]].energyFile);
				//

				if (!bHeaderWritten) {
					params.codebookHeader.mfccParams.dimension = ((MfccFileHeader) (srcFeatures.params)).dimension;
					params.codebookHeader.mfccParams.samplingRate = ((MfccFileHeader) (srcFeatures.params)).samplingRate;

					codebookFile.writeCodebookHeader(params.codebookHeader);
					bHeaderWritten = true;
				}

				if (srcFeatures.mfccs != null) {
					for (j = 0; j < imap.files[0].indicesMap.length; j++) // j is the index for labels
					{
						if (srcFeatures.mfccs.length > imap.files[0].indicesMap[j][0]
								&& tgtFeatures.mfccs.length > imap.files[0].indicesMap[j][1]) {
							// Write to codebook file
							entry = new WeightedCodebookEntry(0, ((MfccFileHeader) (srcFeatures.params)).dimension);
							entry.setMfccs(srcFeatures.mfccs[imap.files[0].indicesMap[j][0]],
									tgtFeatures.mfccs[imap.files[0].indicesMap[j][1]]);

							// Pitch
							index = MathUtils.linearMap(imap.files[0].indicesMap[j][0], 0, srcFeatures.mfccs.length - 1, 0,
									sourceF0s.contour.length - 1);
							entry.sourceItem.f0 = sourceF0s.contour[index];
							index = MathUtils.linearMap(imap.files[0].indicesMap[j][1], 0, tgtFeatures.mfccs.length - 1, 0,
									targetF0s.contour.length - 1);
							entry.targetItem.f0 = targetF0s.contour[index];
							//

							// Duration & Phone
							index = SignalProcUtils.frameIndex2LabelIndex(imap.files[0].indicesMap[j][0], sourceLabels,
									((MfccFileHeader) (srcFeatures.params)).winsize,
									((MfccFileHeader) (srcFeatures.params)).skipsize);
							if (index > 0)
								entry.sourceItem.duration = sourceLabels.items[index].time - sourceLabels.items[index - 1].time;
							else
								entry.sourceItem.duration = sourceLabels.items[index].time;
							entry.sourceItem.phn = sourceLabels.items[index].phn;
							entry.sourceItem.context = new Context(sourceLabels, index,
									WeightedCodebookTrainerParams.MAXIMUM_CONTEXT);

							index = SignalProcUtils.frameIndex2LabelIndex(imap.files[0].indicesMap[j][1], targetLabels,
									((MfccFileHeader) (tgtFeatures.params)).winsize,
									((MfccFileHeader) (tgtFeatures.params)).skipsize);
							if (index > 0)
								entry.targetItem.duration = targetLabels.items[index].time - targetLabels.items[index - 1].time;
							else
								entry.targetItem.duration = targetLabels.items[index].time;
							entry.targetItem.phn = targetLabels.items[index].phn;
							entry.targetItem.context = new Context(targetLabels, index,
									WeightedCodebookTrainerParams.MAXIMUM_CONTEXT);
							//

							// Energy
							index = MathUtils.linearMap(imap.files[0].indicesMap[j][0], 0, srcFeatures.mfccs.length - 1, 0,
									sourceEnergies.contour.length - 1);
							index = MathUtils.CheckLimits(index, 0, sourceEnergies.contour.length - 1);
							entry.sourceItem.energy = sourceEnergies.contour[index];
							index = MathUtils.linearMap(imap.files[0].indicesMap[j][1], 0, tgtFeatures.mfccs.length - 1, 0,
									targetEnergies.contour.length - 1);
							index = MathUtils.CheckLimits(index, 0, targetEnergies.contour.length - 1);
							entry.targetItem.energy = targetEnergies.contour[index];
							//

							if ((entry.sourceItem.f0 > 10.0 && entry.targetItem.f0 > 10.0)
									|| (entry.sourceItem.f0 <= 10.0 && entry.targetItem.f0 <= 10.0))
								codebookFile.writeEntry(entry);
							//
						}
					}

					System.out.println("Frame pairs processed in file " + String.valueOf(i + 1) + " of "
							+ String.valueOf(fcol.indexMapFiles.length));
				}
			}
		}
	}

	public void learnMappingFrameGroups(WeightedCodebookFile codebookFile, WeightedCodebookFeatureCollection fcol,
			BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int[] map) throws IOException {
		assert params.codebookHeader.codebookType == WeightedCodebookFileHeader.FRAME_GROUPS;

		IndexMap imap = new IndexMap();
		int i, j, k, n, totalFrames, index;
		double[] meanSourceEntries = null;
		double[] meanTargetEntries = null;

		double sourceAverageF0;
		double targetAverageF0;
		double sourceAverageDuration;
		double targetAverageDuration;
		double sourceAverageEnergy;
		double targetAverageEnergy;
		int sourceTotalVoiceds;
		int targetTotalVoiceds;
		int sourceTotal;
		int targetTotal;
		String sourcePhn = "";
		String targetPhn = "";
		Context sourceContext = null;
		Context targetContext = null;
		int middle;

		boolean bSourceOK = false;
		boolean bTargetOK = false;

		WeightedCodebookEntry entry = null;

		boolean bHeaderWritten = false;

		// Average neighbouring frame lsfs to obtain a smoother estimate of the source and target LSF vectors and write the
		// averaged versions as a new entry
		for (i = 0; i < fcol.indexMapFiles.length; i++) {
			System.out.println("LSF mapping for pair " + String.valueOf(i + 1) + " of "
					+ String.valueOf(fcol.indexMapFiles.length) + ":");

			try {
				imap.readFromFile(fcol.indexMapFiles[i]); // imap keeps information about a single source-target pair only
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (imap.files != null && sourceTrainingSet.items.length > i && targetTrainingSet.items.length > i) {
				// Mfccs
				Mfccs srcFeatures = new Mfccs(sourceTrainingSet.items[i].mfccFile);
				Mfccs tgtFeatures = new Mfccs(targetTrainingSet.items[map[i]].mfccFile);
				//

				// Pitch: for outlier elimination not prosody modeling!
				PitchReaderWriter sourceF0s = new PitchReaderWriter(sourceTrainingSet.items[i].pitchFile);
				PitchReaderWriter targetF0s = new PitchReaderWriter(targetTrainingSet.items[map[i]].pitchFile);
				//

				// Duration
				Labels sourceLabels = new Labels(sourceTrainingSet.items[i].labelFile);
				Labels targetLabels = new Labels(targetTrainingSet.items[map[i]].labelFile);
				//

				// Energy
				EnergyContourRms sourceEnergies = EnergyContourRms.ReadEnergyFile(sourceTrainingSet.items[i].energyFile);
				EnergyContourRms targetEnergies = EnergyContourRms.ReadEnergyFile(targetTrainingSet.items[map[i]].energyFile);
				//

				if (!bHeaderWritten) {
					params.codebookHeader.mfccParams.dimension = ((MfccFileHeader) (srcFeatures.params)).dimension;
					params.codebookHeader.mfccParams.samplingRate = ((MfccFileHeader) (srcFeatures.params)).samplingRate;

					codebookFile.writeCodebookHeader(params.codebookHeader);
					bHeaderWritten = true;
				}

				if (i == 0) {
					meanSourceEntries = new double[((MfccFileHeader) (srcFeatures.params)).dimension];
					meanTargetEntries = new double[((MfccFileHeader) (tgtFeatures.params)).dimension];
				} else {
					if (meanSourceEntries.length != ((MfccFileHeader) (srcFeatures.params)).dimension) {
						System.out.println("Error! LSF vector size mismatch in source lsf file "
								+ sourceTrainingSet.items[i].mfccFile);
						return;
					}

					if (meanTargetEntries.length != ((MfccFileHeader) (tgtFeatures.params)).dimension) {
						System.out.println("Error! LSF vector size mismatch in target lsf file "
								+ targetTrainingSet.items[map[i]].mfccFile);
						return;
					}
				}

				if (srcFeatures.mfccs != null && tgtFeatures.mfccs != null) {
					for (j = 0; j < imap.files[0].indicesMap.length; j++) // j is the index for labels
					{
						Arrays.fill(meanSourceEntries, 0.0);
						Arrays.fill(meanTargetEntries, 0.0);

						sourceAverageF0 = 0.0;
						targetAverageF0 = 0.0;
						sourceAverageDuration = 0.0;
						targetAverageDuration = 0.0;
						sourceAverageEnergy = 0.0;
						targetAverageEnergy = 0.0;
						sourceTotalVoiceds = 0;
						targetTotalVoiceds = 0;
						sourceTotal = 0;
						targetTotal = 0;

						totalFrames = 0;
						bSourceOK = false;
						middle = (int) Math.floor(0.5 * (imap.files[0].indicesMap[j][0] + imap.files[0].indicesMap[j][1]) + 0.5);
						for (k = imap.files[0].indicesMap[j][0]; k <= imap.files[0].indicesMap[j][1]; k++) {
							if (k >= 0 && k < srcFeatures.mfccs.length) {
								totalFrames++;
								bSourceOK = true;

								for (n = 0; n < ((MfccFileHeader) (srcFeatures.params)).dimension; n++)
									meanSourceEntries[n] += srcFeatures.mfccs[k][n];

								// Pitch
								index = MathUtils.linearMap(k, 0, srcFeatures.mfccs.length - 1, 0, sourceF0s.contour.length - 1);
								if (sourceF0s.contour[index] > 10.0) {
									sourceAverageF0 += sourceF0s.contour[index];
									sourceTotalVoiceds++;
								}
								//

								// Duration
								index = SignalProcUtils.frameIndex2LabelIndex(k, sourceLabels,
										((MfccFileHeader) (srcFeatures.params)).winsize,
										((MfccFileHeader) (srcFeatures.params)).skipsize);
								if (index > 0)
									sourceAverageDuration += sourceLabels.items[index].time - sourceLabels.items[index - 1].time;
								else
									sourceAverageDuration += sourceLabels.items[index].time;
								//

								// Phone: Middle frames phonetic identity
								if (k == middle) {
									sourcePhn = sourceLabels.items[index].phn;
									sourceContext = new Context(sourceLabels, index,
											WeightedCodebookTrainerParams.MAXIMUM_CONTEXT);
								}
								//

								// Energy
								index = MathUtils.linearMap(k, 0, srcFeatures.mfccs.length - 1, 0,
										sourceEnergies.contour.length - 1);
								index = MathUtils.CheckLimits(index, 0, sourceEnergies.contour.length - 1);
								sourceAverageEnergy += sourceEnergies.contour[index];
								//

								sourceTotal++;
							}
						}

						if (bSourceOK) {
							for (n = 0; n < ((MfccFileHeader) (srcFeatures.params)).dimension; n++)
								meanSourceEntries[n] /= totalFrames;

							totalFrames = 0;
							bTargetOK = false;
							middle = (int) Math
									.floor(0.5 * (imap.files[0].indicesMap[j][2] + imap.files[0].indicesMap[j][3]) + 0.5);
							for (k = imap.files[0].indicesMap[j][2]; k <= imap.files[0].indicesMap[j][3]; k++) {
								if (k >= 0 && k < tgtFeatures.mfccs.length) {
									totalFrames++;
									bTargetOK = true;

									for (n = 0; n < ((MfccFileHeader) (tgtFeatures.params)).dimension; n++)
										meanTargetEntries[n] += tgtFeatures.mfccs[k][n];

									// Pitch
									index = MathUtils.linearMap(k, 0, tgtFeatures.mfccs.length - 1, 0,
											targetF0s.contour.length - 1);
									if (targetF0s.contour[index] > 10.0) {
										targetAverageF0 += targetF0s.contour[index];
										targetTotalVoiceds++;
									}
									//

									// Duration
									index = SignalProcUtils.frameIndex2LabelIndex(k, targetLabels,
											((MfccFileHeader) (tgtFeatures.params)).winsize,
											((MfccFileHeader) (tgtFeatures.params)).skipsize);
									if (index > 0)
										targetAverageDuration += targetLabels.items[index].time
												- targetLabels.items[index - 1].time;
									else
										targetAverageDuration += targetLabels.items[index].time;
									//

									// Phone: Middle frames phonetic identity
									if (k == middle) {
										targetPhn = targetLabels.items[index].phn;
										targetContext = new Context(targetLabels, index,
												WeightedCodebookTrainerParams.MAXIMUM_CONTEXT);
									}
									//

									// Energy
									index = MathUtils.linearMap(k, 0, tgtFeatures.mfccs.length - 1, 0,
											targetEnergies.contour.length - 1);
									index = MathUtils.CheckLimits(index, 0, targetEnergies.contour.length - 1);
									targetAverageEnergy += targetEnergies.contour[index];
									//

									targetTotal++;
								}
							}

							if (bTargetOK) {
								for (n = 0; n < ((MfccFileHeader) (tgtFeatures.params)).dimension; n++)
									meanTargetEntries[n] /= totalFrames;

								// Write to codebook file
								entry = new WeightedCodebookEntry(0, meanSourceEntries.length);
								entry.setMfccs(meanSourceEntries, meanTargetEntries);

								// Pitch
								if (sourceTotalVoiceds > 0)
									sourceAverageF0 /= sourceTotalVoiceds;
								if (targetTotalVoiceds > 0)
									targetAverageF0 /= targetTotalVoiceds;
								entry.sourceItem.f0 = sourceAverageF0;
								entry.targetItem.f0 = targetAverageF0;
								//

								// Duration
								if (sourceTotal > 0)
									sourceAverageDuration /= sourceTotal;
								if (targetTotal > 0)
									sourceAverageDuration /= targetTotal;
								entry.sourceItem.duration = sourceAverageDuration;
								entry.targetItem.duration = targetAverageDuration;
								//

								// Phone
								entry.sourceItem.phn = sourcePhn;
								entry.targetItem.phn = targetPhn;
								entry.sourceItem.context = new Context(sourceContext);
								entry.targetItem.context = new Context(targetContext);
								//

								// Energy
								if (sourceTotal > 0)
									sourceAverageEnergy /= sourceTotal;
								if (targetTotal > 0)
									targetAverageEnergy /= targetTotal;
								entry.sourceItem.energy = sourceAverageEnergy;
								entry.targetItem.energy = targetAverageEnergy;
								//

								if ((entry.sourceItem.f0 > 10.0 && entry.targetItem.f0 > 10.0)
										|| (entry.sourceItem.f0 <= 10.0 && entry.targetItem.f0 <= 10.0))
									codebookFile.writeEntry(entry);
								//
							}
						}
					}

					System.out.println("Frame pairs processed in file " + String.valueOf(i + 1) + " of "
							+ String.valueOf(fcol.indexMapFiles.length));
				}
			}
		}
	}

	public void learnMappingLabels(WeightedCodebookFile codebookFile, WeightedCodebookFeatureCollection fcol,
			BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int[] map) throws IOException {
		assert params.codebookHeader.codebookType == WeightedCodebookFileHeader.LABELS;

		IndexMap imap = new IndexMap();
		int i, j, k, n, totalFrames, index;
		boolean bSourceOK = false;
		boolean bTargetOK = false;

		double[] meanSourceEntries = null;
		double[] meanTargetEntries = null;

		double sourceAverageF0;
		double targetAverageF0;
		double sourceAverageDuration;
		double targetAverageDuration;
		double sourceAverageEnergy;
		double targetAverageEnergy;
		int sourceTotalVoiceds;
		int targetTotalVoiceds;
		int sourceTotal;
		int targetTotal;
		String sourcePhn = "";
		String targetPhn = "";
		Context sourceContext = null;
		Context targetContext = null;
		int middle;

		WeightedCodebookEntry entry = null;

		boolean bHeaderWritten = false;

		// Take an average of LSF vectors within each label pair and write the resulting vector as the state
		// average for source and target
		// To do: Weighting of vectors within each label according to some criteria
		// on how typical they represent the current phone.
		// This can be implemented by looking at some distance measure (eucledian, mahalonoibis, LSF, etc)
		// to the cluster mean (i.e. mean of all LSF vectors for this phone), for example.
		for (i = 0; i < fcol.indexMapFiles.length; i++) {
			System.out.println("LSF mapping for pair " + String.valueOf(i + 1) + " of "
					+ String.valueOf(fcol.indexMapFiles.length) + ":");

			try {
				imap.readFromFile(fcol.indexMapFiles[i]); // imap keeps information about a single source-target pair only
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (imap.files != null && sourceTrainingSet.items.length > i && targetTrainingSet.items.length > i) {
				Mfccs srcFeatures = new Mfccs(sourceTrainingSet.items[i].mfccFile);
				Mfccs tgtFeatures = new Mfccs(targetTrainingSet.items[map[i]].mfccFile);

				// Pitch: for outlier elimination not prosody modeling!
				PitchReaderWriter sourceF0s = new PitchReaderWriter(sourceTrainingSet.items[i].pitchFile);
				PitchReaderWriter targetF0s = new PitchReaderWriter(targetTrainingSet.items[map[i]].pitchFile);
				//

				// Duration
				Labels sourceLabels = new Labels(sourceTrainingSet.items[i].labelFile);
				Labels targetLabels = new Labels(targetTrainingSet.items[map[i]].labelFile);
				//

				// Energy
				EnergyContourRms sourceEnergies = EnergyContourRms.ReadEnergyFile(sourceTrainingSet.items[i].energyFile);
				EnergyContourRms targetEnergies = EnergyContourRms.ReadEnergyFile(targetTrainingSet.items[map[i]].energyFile);
				//

				if (!bHeaderWritten) {
					params.codebookHeader.mfccParams.dimension = ((MfccFileHeader) (srcFeatures.params)).dimension;
					params.codebookHeader.mfccParams.samplingRate = ((MfccFileHeader) (srcFeatures.params)).samplingRate;

					codebookFile.writeCodebookHeader(params.codebookHeader);
					bHeaderWritten = true;
				}

				if (i == 0) {
					meanSourceEntries = new double[((MfccFileHeader) (srcFeatures.params)).dimension];
					meanTargetEntries = new double[((MfccFileHeader) (tgtFeatures.params)).dimension];
				} else {
					if (meanSourceEntries.length != ((MfccFileHeader) (srcFeatures.params)).dimension) {
						System.out.println("Error! LSF vector size mismatch in source lsf file "
								+ sourceTrainingSet.items[i].mfccFile);
						return;
					}

					if (meanTargetEntries.length != ((MfccFileHeader) (tgtFeatures.params)).dimension) {
						System.out.println("Error! LSF vector size mismatch in target lsf file "
								+ targetTrainingSet.items[map[i]].mfccFile);
						return;
					}
				}

				if (srcFeatures.mfccs != null && tgtFeatures.mfccs != null) {
					for (j = 0; j < imap.files[0].indicesMap.length; j++) // j is the index for labels
					{
						Arrays.fill(meanSourceEntries, 0.0);
						Arrays.fill(meanTargetEntries, 0.0);

						sourceAverageF0 = 0.0;
						targetAverageF0 = 0.0;
						sourceAverageDuration = 0.0;
						targetAverageDuration = 0.0;
						sourceAverageEnergy = 0.0;
						targetAverageEnergy = 0.0;
						sourceTotalVoiceds = 0;
						targetTotalVoiceds = 0;
						sourceTotal = 0;
						targetTotal = 0;

						totalFrames = 0;
						bSourceOK = false;
						middle = (int) Math.floor(0.5 * (imap.files[0].indicesMap[j][0] + imap.files[0].indicesMap[j][1]) + 0.5);
						for (k = imap.files[0].indicesMap[j][0]; k <= imap.files[0].indicesMap[j][1]; k++) {
							if (k >= 0 && k < srcFeatures.mfccs.length) {
								totalFrames++;
								bSourceOK = true;

								for (n = 0; n < ((MfccFileHeader) (srcFeatures.params)).dimension; n++)
									meanSourceEntries[n] += srcFeatures.mfccs[k][n];

								// Pitch
								index = MathUtils.linearMap(k, 0, srcFeatures.mfccs.length - 1, 0, sourceF0s.contour.length - 1);
								if (sourceF0s.contour[index] > 10.0) {
									sourceAverageF0 += sourceF0s.contour[index];
									sourceTotalVoiceds++;
								}
								//

								// Duration
								index = SignalProcUtils.frameIndex2LabelIndex(k, sourceLabels,
										((MfccFileHeader) (srcFeatures.params)).winsize,
										((MfccFileHeader) (srcFeatures.params)).skipsize);
								if (index > 0)
									sourceAverageDuration += sourceLabels.items[index].time - sourceLabels.items[index - 1].time;
								else
									sourceAverageDuration += sourceLabels.items[index].time;
								//

								// Phone: Middle frames phonetic identity
								if (k == middle) {
									sourcePhn = sourceLabels.items[index].phn;
									sourceContext = new Context(sourceLabels, index,
											WeightedCodebookTrainerParams.MAXIMUM_CONTEXT);
								}
								//

								// Energy
								index = MathUtils.linearMap(k, 0, srcFeatures.mfccs.length - 1, 0,
										sourceEnergies.contour.length - 1);
								index = MathUtils.CheckLimits(index, 0, sourceEnergies.contour.length - 1);
								sourceAverageEnergy += sourceEnergies.contour[index];
								//

								sourceTotal++;
							}
						}

						if (bSourceOK) {
							for (n = 0; n < ((MfccFileHeader) (srcFeatures.params)).dimension; n++)
								meanSourceEntries[n] /= totalFrames;

							totalFrames = 0;
							bTargetOK = false;
							middle = (int) Math
									.floor(0.5 * (imap.files[0].indicesMap[j][2] + imap.files[0].indicesMap[j][3]) + 0.5);
							for (k = imap.files[0].indicesMap[j][2]; k <= imap.files[0].indicesMap[j][3]; k++) {
								if (k >= 0 && k < tgtFeatures.mfccs.length) {
									totalFrames++;
									bTargetOK = true;

									for (n = 0; n < ((MfccFileHeader) (tgtFeatures.params)).dimension; n++)
										meanTargetEntries[n] += tgtFeatures.mfccs[k][n];

									// Pitch
									index = MathUtils.linearMap(k, 0, tgtFeatures.mfccs.length - 1, 0,
											targetF0s.contour.length - 1);
									if (targetF0s.contour[index] > 10.0) {
										targetAverageF0 += targetF0s.contour[index];
										targetTotalVoiceds++;
									}
									//

									// Duration
									index = SignalProcUtils.frameIndex2LabelIndex(k, targetLabels,
											((MfccFileHeader) (tgtFeatures.params)).winsize,
											((MfccFileHeader) (tgtFeatures.params)).skipsize);
									if (index > 0)
										targetAverageDuration += targetLabels.items[index].time
												- targetLabels.items[index - 1].time;
									else
										targetAverageDuration += targetLabels.items[index].time;
									//

									// Phone: Middle frames phonetic identity
									if (k == middle) {
										targetPhn = targetLabels.items[index].phn;
										targetContext = new Context(targetLabels, index,
												WeightedCodebookTrainerParams.MAXIMUM_CONTEXT);
									}
									//

									// Energy
									index = MathUtils.linearMap(k, 0, tgtFeatures.mfccs.length - 1, 0,
											targetEnergies.contour.length - 1);
									index = MathUtils.CheckLimits(index, 0, targetEnergies.contour.length - 1);
									targetAverageEnergy += targetEnergies.contour[index];
									//

									targetTotal++;
								}
							}

							if (bTargetOK) {
								for (n = 0; n < ((MfccFileHeader) (tgtFeatures.params)).dimension; n++)
									meanTargetEntries[n] /= totalFrames;

								// Write to codebook file
								entry = new WeightedCodebookEntry(0, meanSourceEntries.length);
								entry.setMfccs(meanSourceEntries, meanTargetEntries);

								// Pitch
								if (sourceTotalVoiceds > 0)
									sourceAverageF0 /= sourceTotalVoiceds;
								if (targetTotalVoiceds > 0)
									targetAverageF0 /= targetTotalVoiceds;
								entry.sourceItem.f0 = sourceAverageF0;
								entry.targetItem.f0 = targetAverageF0;
								//

								// Duration
								if (sourceTotal > 0)
									sourceAverageDuration /= sourceTotal;
								if (targetTotal > 0)
									sourceAverageDuration /= targetTotal;
								entry.sourceItem.duration = sourceAverageDuration;
								entry.targetItem.duration = targetAverageDuration;
								//

								// Phone
								entry.sourceItem.phn = sourcePhn;
								entry.targetItem.phn = targetPhn;
								entry.sourceItem.context = new Context(sourceContext);
								entry.targetItem.context = new Context(targetContext);
								//

								// Energy
								if (sourceTotal > 0)
									sourceAverageEnergy /= sourceTotal;
								if (targetTotal > 0)
									targetAverageEnergy /= targetTotal;
								entry.sourceItem.energy = sourceAverageEnergy;
								entry.targetItem.energy = targetAverageEnergy;
								//

								if ((entry.sourceItem.f0 > 10.0 && entry.targetItem.f0 > 10.0)
										|| (entry.sourceItem.f0 <= 10.0 && entry.targetItem.f0 <= 10.0))
									codebookFile.writeEntry(entry);
								//

								System.out.println("Label pair " + String.valueOf(j + 1) + " of "
										+ String.valueOf(imap.files[0].indicesMap.length));
							}
						}
					}
				}
			}
		}
	}

	// This function is identical to learnMappingLabels since the mapping is performed accordingly in previous steps
	public void learnMappingLabelGroups(WeightedCodebookFile codebookFile, WeightedCodebookFeatureCollection fcol,
			BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int[] map) throws IOException {
		learnMappingLabels(codebookFile, fcol, sourceTrainingSet, targetTrainingSet, map);
	}

	public void learnMappingSpeech(WeightedCodebookFile codebookFile, WeightedCodebookFeatureCollection fcol,
			BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int[] map) {
		assert params.codebookHeader.codebookType == WeightedCodebookFileHeader.SPEECH;

		int i, j, n;

		double[] meanSourceEntries = null;
		double[] meanTargetEntries = null;

		WeightedCodebookEntry entry = null;

		boolean bHeaderWritten = false;

		// Take an average of LSF vectors within each label pair and write the resulting vector as the state
		// average for source and target
		// To do: Weighting of vectors within each label according to some criteria
		// on how typical they represent the current phone.
		// This can be implemented by looking at some distance measure (eucledian, mahalonoibis, LSF, etc)
		// to the cluster mean (i.e. mean of all LSF vectors for this phone), for example.
		int totalFramesSrc = 0;
		boolean bSourceOK = false;
		int totalFramesTgt = 0;
		boolean bTargetOK = false;
		int lpOrderSrc = 0;
		int lpOrderTgt = 0;
		for (i = 0; i < fcol.indexMapFiles.length; i++) {
			System.out.println("LSF mapping for pair " + String.valueOf(i + 1) + " of "
					+ String.valueOf(fcol.indexMapFiles.length) + ":");

			if (sourceTrainingSet.items.length > i) {
				Mfccs srcFeatures = new Mfccs(sourceTrainingSet.items[i].mfccFile);
				Mfccs tgtFeatures = new Mfccs(targetTrainingSet.items[map[i]].mfccFile);

				if (!bHeaderWritten) {
					params.codebookHeader.mfccParams.dimension = ((MfccFileHeader) (srcFeatures.params)).dimension;
					params.codebookHeader.mfccParams.samplingRate = ((MfccFileHeader) (srcFeatures.params)).samplingRate;

					codebookFile.writeCodebookHeader(params.codebookHeader);
					bHeaderWritten = true;
				}

				if (i == 0) {
					meanSourceEntries = new double[((MfccFileHeader) (srcFeatures.params)).dimension];
					meanTargetEntries = new double[((MfccFileHeader) (tgtFeatures.params)).dimension];
					Arrays.fill(meanSourceEntries, 0.0);
					Arrays.fill(meanTargetEntries, 0.0);
					lpOrderSrc = ((MfccFileHeader) (srcFeatures.params)).dimension;
					lpOrderTgt = ((MfccFileHeader) (srcFeatures.params)).dimension;
				} else {
					if (meanSourceEntries.length != ((MfccFileHeader) (srcFeatures.params)).dimension) {
						System.out.println("Error! LSF vector size mismatch in source lsf file "
								+ sourceTrainingSet.items[i].mfccFile);
						return;
					}

					if (meanTargetEntries.length != ((MfccFileHeader) (tgtFeatures.params)).dimension) {
						System.out.println("Error! LSF vector size mismatch in target lsf file "
								+ targetTrainingSet.items[map[i]].mfccFile);
						return;
					}
				}

				if (srcFeatures.mfccs != null) {
					for (j = 0; j < ((MfccFileHeader) (srcFeatures.params)).numfrm; j++) {
						totalFramesSrc++;
						bSourceOK = true;
						for (n = 0; n < lpOrderSrc; n++)
							meanSourceEntries[n] += srcFeatures.mfccs[j][n];
					}
				}

				if (tgtFeatures.mfccs != null) {
					for (j = 0; j < ((MfccFileHeader) (tgtFeatures.params)).numfrm; j++) {
						totalFramesTgt++;
						bTargetOK = true;
						for (n = 0; n < lpOrderTgt; n++)
							meanTargetEntries[n] += tgtFeatures.mfccs[j][n];
					}
				}
			}
		}

		if (bSourceOK) {
			for (n = 0; n < lpOrderSrc; n++)
				meanSourceEntries[n] /= totalFramesSrc;
		}

		if (bTargetOK) {
			for (n = 0; n < lpOrderTgt; n++)
				meanTargetEntries[n] /= totalFramesTgt;
		}

		if (bSourceOK && bTargetOK) {
			// Write to codebook file
			entry = new WeightedCodebookEntry(0, meanSourceEntries.length);
			entry.setMfccs(meanSourceEntries, meanTargetEntries);
			codebookFile.writeEntry(entry);
			//
		}
	}
}
