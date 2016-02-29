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

import marytts.signalproc.analysis.FeatureFileHeader;
import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.signalproc.analysis.MfccFileHeader;
import marytts.util.data.AlignLabelsUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;

/**
 * Generic utilities for voice conversion
 * 
 * @author Oytun T&uuml;rk
 */
public class AdaptationUtils {

	public static int ALL_AVAILABLE_TRAINING_FRAMES = -1;

	// An optimal alignment is found by dynamic programming if the labels are not identical
	public static IndexMap mapFramesFeatures(String sourceLabelFile, String targetLabelFile, String sourceFeatureFile,
			String targetFeatureFile, int vocalTractFeature, String[] labelsToExcludeFromTraining) throws IOException {
		IndexMap im = null;

		// Read label files
		Labels sourceLabels = new Labels(sourceLabelFile);
		Labels targetLabels = new Labels(targetLabelFile);
		//

		// Read feature file headers
		FeatureFileHeader hdr1 = null;
		FeatureFileHeader hdr2 = null;

		if (vocalTractFeature == BaselineFeatureExtractor.LSF_FEATURES) {
			hdr1 = new LsfFileHeader(sourceFeatureFile);
			hdr2 = new LsfFileHeader(targetFeatureFile);
		} else if (vocalTractFeature == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES) {
			hdr1 = new MfccFileHeader(sourceFeatureFile);
			hdr2 = new MfccFileHeader(targetFeatureFile);
		}
		//

		if (hdr1 != null && hdr2 != null && sourceLabels.items != null && targetLabels.items != null) {
			// Find the optimum alignment between the source and the target labels since the phone sequences may not be identical
			// due to silence periods etc.
			int[][] labelMap = AlignLabelsUtils.alignLabels(sourceLabels.items, targetLabels.items);
			//

			if (labelMap != null) {
				int j, srcLabInd, tgtLabInd, tgtFrmInd;
				double time1, time2;
				double srcStartTime, srcEndTime, tgtStartTime, tgtEndTime;

				srcLabInd = 0;

				// Find the corresponding target frame index for each source frame index
				int count = 0;
				im = new IndexMap(1);
				im.files[0] = new FileMap(hdr1.numfrm, 2);

				for (j = 0; j < hdr1.numfrm; j++) {
					time1 = SignalProcUtils.frameIndex2Time(j, hdr1.winsize, hdr1.skipsize);

					while (time1 > sourceLabels.items[srcLabInd].time) {
						srcLabInd++;
						if (srcLabInd > sourceLabels.items.length - 1) {
							srcLabInd = sourceLabels.items.length - 1;
							break;
						}
					}

					tgtLabInd = StringUtils.findInMap(labelMap, srcLabInd);

					if (tgtLabInd >= 0 && sourceLabels.items[srcLabInd].phn.compareTo(targetLabels.items[tgtLabInd].phn) == 0) {
						boolean isLabelDesired = true;
						if (labelsToExcludeFromTraining != null)
							isLabelDesired = !StringUtils.isOneOf(sourceLabels.items[srcLabInd].phn, labelsToExcludeFromTraining);

						if (isLabelDesired) {
							if (srcLabInd > 0)
								srcStartTime = sourceLabels.items[srcLabInd - 1].time;
							else
								srcStartTime = 0.0;

							if (tgtLabInd > 0)
								tgtStartTime = targetLabels.items[tgtLabInd - 1].time;
							else
								tgtStartTime = 0.0;

							srcEndTime = sourceLabels.items[srcLabInd].time;
							tgtEndTime = targetLabels.items[tgtLabInd].time;

							time2 = MathUtils.linearMap(time1, srcStartTime, srcEndTime, tgtStartTime, tgtEndTime);

							tgtFrmInd = SignalProcUtils.time2frameIndex(time2, hdr2.winsize, hdr2.skipsize);
							tgtFrmInd = Math.max(0, tgtFrmInd);
							tgtFrmInd = Math.min(tgtFrmInd, hdr2.numfrm - 1);

							im.files[0].indicesMap[count][0] = j;
							im.files[0].indicesMap[count][1] = tgtFrmInd;
							count++;

							if (count > hdr1.numfrm - 1)
								break;
						}
					}
				}
			}
		}

		return im;
	}

	// Each frame is mapped as a group of frames, i.e. with frames on the left and right context
	public static IndexMap mapFrameGroupsFeatures(String sourceLabelFile, String targetLabelFile, String sourceFeatureFile,
			String targetFeatureFile, int numNeighbours, int vocalTractFeature, String[] labelsToExcludeFromTraining)
			throws IOException {
		IndexMap im = null;

		// Read label files
		Labels sourceLabels = new Labels(sourceLabelFile);
		Labels targetLabels = new Labels(targetLabelFile);
		//

		// Read feature file headers
		FeatureFileHeader hdr1 = null;
		FeatureFileHeader hdr2 = null;

		if (vocalTractFeature == BaselineFeatureExtractor.LSF_FEATURES) {
			hdr1 = new LsfFileHeader(sourceFeatureFile);
			hdr2 = new LsfFileHeader(targetFeatureFile);
		} else if (vocalTractFeature == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES) {
			hdr1 = new MfccFileHeader(sourceFeatureFile);
			hdr2 = new MfccFileHeader(targetFeatureFile);
		}
		//

		if (hdr1 != null && hdr2 != null && sourceLabels.items != null && targetLabels.items != null) {
			// Find the optimum alignment between the source and the target labels since the phone sequences may not be identical
			// due to silence periods etc.
			int[][] labelMap = AlignLabelsUtils.alignLabels(sourceLabels.items, targetLabels.items);
			//

			if (labelMap != null) {
				int j, srcLabInd, tgtLabInd, tgtFrmInd;
				double time1, time2;
				double srcStartTime, srcEndTime, tgtStartTime, tgtEndTime;

				srcLabInd = 0;

				// Find the corresponding target frame index for each source frame index
				int count = 0;
				im = new IndexMap(1);
				im.files[0] = new FileMap(hdr1.numfrm, 4);

				for (j = 0; j < hdr1.numfrm; j++) {
					time1 = SignalProcUtils.frameIndex2Time(j, hdr1.winsize, hdr1.skipsize);

					while (time1 > sourceLabels.items[srcLabInd].time) {
						srcLabInd++;
						if (srcLabInd > sourceLabels.items.length - 1) {
							srcLabInd = sourceLabels.items.length - 1;
							break;
						}
					}

					tgtLabInd = StringUtils.findInMap(labelMap, srcLabInd);

					if (tgtLabInd >= 0 && sourceLabels.items[srcLabInd].phn.compareTo(targetLabels.items[tgtLabInd].phn) == 0) {
						boolean isLabelDesired = true;
						if (labelsToExcludeFromTraining != null)
							isLabelDesired = !StringUtils.isOneOf(sourceLabels.items[srcLabInd].phn, labelsToExcludeFromTraining);

						if (isLabelDesired) {
							if (srcLabInd > 0)
								srcStartTime = sourceLabels.items[srcLabInd - 1].time;
							else
								srcStartTime = 0.0;

							if (tgtLabInd > 0)
								tgtStartTime = targetLabels.items[tgtLabInd - 1].time;
							else
								tgtStartTime = 0.0;

							srcEndTime = sourceLabels.items[srcLabInd].time;
							tgtEndTime = targetLabels.items[tgtLabInd].time;

							time2 = MathUtils.linearMap(time1, srcStartTime, srcEndTime, tgtStartTime, tgtEndTime);

							tgtFrmInd = SignalProcUtils.time2frameIndex(time2, hdr2.winsize, hdr2.skipsize);

							im.files[0].indicesMap[count][0] = Math.max(0, j - numNeighbours);
							im.files[0].indicesMap[count][1] = Math.min(j + numNeighbours, hdr1.numfrm - 1);
							im.files[0].indicesMap[count][2] = Math.max(0, tgtFrmInd - numNeighbours);
							im.files[0].indicesMap[count][3] = Math.min(tgtFrmInd + numNeighbours, hdr2.numfrm - 1);
							count++;

							if (count > hdr1.numfrm - 1)
								break;
						}
					}
				}
			}
		}

		return im;
	}

	public static IndexMap mapLabelsFeatures(String sourceLabelFile, String targetLabelFile, String sourceFeatureFile,
			String targetFeatureFile, int vocalTractFeature, String[] labelsToExcludeFromTraining) throws IOException {
		IndexMap im = null;

		// Read label files
		Labels sourceLabels = new Labels(sourceLabelFile);
		Labels targetLabels = new Labels(targetLabelFile);
		//

		// Read feature file headers
		FeatureFileHeader hdr1 = null;
		FeatureFileHeader hdr2 = null;

		if (vocalTractFeature == BaselineFeatureExtractor.LSF_FEATURES) {
			hdr1 = new LsfFileHeader(sourceFeatureFile);
			hdr2 = new LsfFileHeader(targetFeatureFile);
		} else if (vocalTractFeature == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES) {
			hdr1 = new MfccFileHeader(sourceFeatureFile);
			hdr2 = new MfccFileHeader(targetFeatureFile);
		}
		//

		if (hdr1 != null && hdr2 != null && sourceLabels.items != null && targetLabels.items != null) {
			// Find the optimum alignment between the source and the target labels since the phone sequences may not be identical
			// due to silence periods etc.
			int[][] labelMap = AlignLabelsUtils.alignLabels(sourceLabels.items, targetLabels.items);
			//

			if (labelMap != null) {
				int j, tgtLabInd;
				double srcStartTime, srcEndTime, tgtStartTime, tgtEndTime;

				// Find the corresponding target frame index for each source frame index
				int count = 0;
				im = new IndexMap(1);
				im.files[0] = new FileMap(sourceLabels.items.length, 4);

				for (j = 0; j < sourceLabels.items.length; j++) {
					if (j > 0)
						srcStartTime = sourceLabels.items[j - 1].time;
					else
						srcStartTime = 0.0;

					tgtLabInd = StringUtils.findInMap(labelMap, j);

					if (tgtLabInd >= 0 && sourceLabels.items[j].phn.compareTo(targetLabels.items[tgtLabInd].phn) == 0) {
						boolean isLabelDesired = true;
						if (labelsToExcludeFromTraining != null)
							isLabelDesired = !StringUtils.isOneOf(sourceLabels.items[j].phn, labelsToExcludeFromTraining);

						if (isLabelDesired) {
							if (tgtLabInd > 0)
								tgtStartTime = targetLabels.items[tgtLabInd - 1].time;
							else
								tgtStartTime = 0.0;

							srcEndTime = sourceLabels.items[j].time;
							tgtEndTime = targetLabels.items[tgtLabInd].time;

							im.files[0].indicesMap[count][0] = SignalProcUtils.time2frameIndex(srcStartTime, hdr1.winsize,
									hdr1.skipsize);
							im.files[0].indicesMap[count][1] = SignalProcUtils.time2frameIndex(srcEndTime, hdr1.winsize,
									hdr1.skipsize);
							im.files[0].indicesMap[count][2] = SignalProcUtils.time2frameIndex(tgtStartTime, hdr2.winsize,
									hdr2.skipsize);
							im.files[0].indicesMap[count][3] = SignalProcUtils.time2frameIndex(tgtEndTime, hdr2.winsize,
									hdr2.skipsize);

							count++;

							if (count > sourceLabels.items.length - 1)
								break;
						}
					}
				}
			}
		}

		return im;
	}

	public static IndexMap mapLabelGroupsFeatures(String sourceLabelFile, String targetLabelFile, String sourceFeatureFile,
			String targetFeatureFile, int numNeighbours, int vocalTractFeature, String[] labelsToExcludeFromTraining)
			throws IOException {
		IndexMap im = null;

		// Read label files
		Labels sourceLabels = new Labels(sourceLabelFile);
		Labels targetLabels = new Labels(targetLabelFile);
		//

		// Read feature file headers
		FeatureFileHeader hdr1 = null;
		FeatureFileHeader hdr2 = null;

		if (vocalTractFeature == BaselineFeatureExtractor.LSF_FEATURES) {
			hdr1 = new LsfFileHeader(sourceFeatureFile);
			hdr2 = new LsfFileHeader(targetFeatureFile);
		} else if (vocalTractFeature == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES) {
			hdr1 = new MfccFileHeader(sourceFeatureFile);
			hdr2 = new MfccFileHeader(targetFeatureFile);
		}
		//

		if (hdr1 != null && hdr2 != null && sourceLabels.items != null && targetLabels.items != null) {
			// Find the optimum alignment between the source and the target labels since the phone sequences may not be identical
			// due to silence periods etc.
			int[][] labelMap = AlignLabelsUtils.alignLabels(sourceLabels.items, targetLabels.items);
			//

			if (labelMap != null) {
				int j, tgtLabInd;
				double srcStartTime, srcEndTime, tgtStartTime, tgtEndTime;

				// Find the corresponding target frame index for each source frame index
				int count = 0;
				im = new IndexMap(1);
				im.files[0] = new FileMap(sourceLabels.items.length, 4);

				for (j = 0; j < sourceLabels.items.length; j++) {
					if (j - numNeighbours - 1 >= 0)
						srcStartTime = sourceLabels.items[j - numNeighbours - 1].time;
					else
						srcStartTime = 0.0;

					tgtLabInd = StringUtils.findInMap(labelMap, j);

					if (tgtLabInd >= 0 && sourceLabels.items[j].phn.compareTo(targetLabels.items[tgtLabInd].phn) == 0) {
						boolean isLabelDesired = true;
						if (labelsToExcludeFromTraining != null)
							isLabelDesired = !StringUtils.isOneOf(sourceLabels.items[j].phn, labelsToExcludeFromTraining);

						if (isLabelDesired) {
							if (tgtLabInd - numNeighbours - 1 >= 0)
								tgtStartTime = targetLabels.items[tgtLabInd - numNeighbours - 1].time;
							else
								tgtStartTime = 0.0;

							srcEndTime = sourceLabels.items[Math.min(j + numNeighbours, sourceLabels.items.length - 1)].time;
							tgtEndTime = targetLabels.items[Math.min(tgtLabInd + numNeighbours, targetLabels.items.length - 1)].time;

							im.files[0].indicesMap[count][0] = SignalProcUtils.time2frameIndex(srcStartTime, hdr1.winsize,
									hdr1.skipsize);
							im.files[0].indicesMap[count][1] = SignalProcUtils.time2frameIndex(srcEndTime, hdr1.winsize,
									hdr1.skipsize);
							im.files[0].indicesMap[count][2] = SignalProcUtils.time2frameIndex(tgtStartTime, hdr2.winsize,
									hdr2.skipsize);
							im.files[0].indicesMap[count][3] = SignalProcUtils.time2frameIndex(tgtEndTime, hdr2.winsize,
									hdr2.skipsize);

							count++;

							if (count > sourceLabels.items.length - 1)
								break;
						}
					}
				}
			}
		}

		return im;
	}

	public static IndexMap mapSpeechFeatures() {
		IndexMap im = new IndexMap(1);
		im.files[0] = new FileMap(1, 1);

		im.files[0].indicesMap[0][0] = ALL_AVAILABLE_TRAINING_FRAMES;

		return im;
	}
}
