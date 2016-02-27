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
import marytts.signalproc.adaptation.VocalTractTransformationData;
import marytts.util.string.StringUtils;

/**
 * 
 * Wrapper class for weighted codebooks for voice conversion
 * 
 * @author Oytun T&uuml;rk
 */
public class WeightedCodebook extends VocalTractTransformationData {
	// These are for feature requests from the codebook
	public static final int SOURCE = 1;
	public static final int TARGET = 2;
	public static final int SOURCE_TARGET = 3;
	public static final int TARGET_SOURCE = 4;
	//

	public WeightedCodebookEntry[] entries;
	public WeightedCodebookFileHeader header;

	public WeightedCodebook() {
		this(0, 0);
	}

	public WeightedCodebook(int totalLsfEntriesIn, int totalF0StatisticsIn) {
		if (header == null)
			header = new WeightedCodebookFileHeader(totalLsfEntriesIn);

		allocate();
	}

	public void allocate() {
		allocate(header.totalEntries);
	}

	public void allocate(int totalEntriesIn) {
		if (totalEntriesIn > 0) {
			entries = new WeightedCodebookEntry[totalEntriesIn];
			header.totalEntries = totalEntriesIn;
		} else {
			entries = null;
			header.totalEntries = 0;
		}
	}

	public double[][] getFeatures(int speakerType, int desiredFeatures) {
		double[][] features = null;

		if (entries != null) {
			features = new double[header.totalEntries][];
			int dimension = 0;
			boolean isLsfDesired = false;
			boolean isF0Desired = false;
			boolean isEnergyDesired = false;
			boolean isDurationDesired = false;
			boolean isMfccDesired = false;

			if (StringUtils.isDesired(BaselineFeatureExtractor.LSF_FEATURES, desiredFeatures)) {
				dimension += header.lsfParams.dimension;
				isLsfDesired = true;
			}
			if (StringUtils.isDesired(BaselineFeatureExtractor.F0_FEATURES, desiredFeatures)) {
				dimension += 1;
				isF0Desired = true;
			}
			if (StringUtils.isDesired(BaselineFeatureExtractor.ENERGY_FEATURES, desiredFeatures)) {
				dimension += 1;
				isEnergyDesired = true;
			}
			if (StringUtils.isDesired(BaselineFeatureExtractor.DURATION_FEATURES, desiredFeatures)) {
				dimension += 1;
				isDurationDesired = true;
			}
			if (StringUtils.isDesired(BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES, desiredFeatures)) {
				dimension += header.mfccParams.dimension;
				isMfccDesired = true;
			}

			int currentPos;
			for (int i = 0; i < header.totalEntries; i++) {
				features[i] = new double[dimension];
				currentPos = 0;

				// Source
				if (speakerType == SOURCE || speakerType == SOURCE_TARGET) {
					if (isLsfDesired && entries[i].sourceItem.lsfs != null) {
						System.arraycopy(entries[i].sourceItem.lsfs, 0, features[i], currentPos, header.lsfParams.dimension);
						currentPos += header.lsfParams.dimension;
					}
					if (isF0Desired) {
						features[i][currentPos] = entries[i].sourceItem.f0;
						currentPos += 1;
					}
					if (isEnergyDesired) {
						features[i][currentPos] = entries[i].sourceItem.energy;
						currentPos += 1;
					}
					if (isDurationDesired) {
						features[i][currentPos] = entries[i].sourceItem.duration;
						currentPos += 1;
					}
					if (isMfccDesired) {
						System.arraycopy(entries[i].sourceItem.mfccs, 0, features[i], currentPos, header.mfccParams.dimension);
						currentPos += header.mfccParams.dimension;
					}
				}

				// Target
				if (speakerType == TARGET || speakerType == TARGET_SOURCE) {
					if (isLsfDesired) {
						System.arraycopy(entries[i].targetItem.lsfs, 0, features[i], currentPos, header.lsfParams.dimension);
						currentPos += header.lsfParams.dimension;
					}
					if (isF0Desired) {
						features[i][currentPos] = entries[i].targetItem.f0;
						currentPos += 1;
					}
					if (isEnergyDesired) {
						features[i][currentPos] = entries[i].targetItem.energy;
						currentPos += 1;
					}
					if (isDurationDesired) {
						features[i][currentPos] = entries[i].targetItem.duration;
						currentPos += 1;
					}
					if (isMfccDesired) {
						System.arraycopy(entries[i].targetItem.mfccs, 0, features[i], currentPos, header.mfccParams.dimension);
						currentPos += header.mfccParams.dimension;
					}
				}

				// Repeat Source here (i.e. target is requested first)
				if (speakerType == TARGET_SOURCE) {
					if (isLsfDesired) {
						System.arraycopy(entries[i].sourceItem.lsfs, 0, features[i], currentPos, header.lsfParams.dimension);
						currentPos += header.lsfParams.dimension;
					}
					if (isF0Desired) {
						features[i][currentPos] = entries[i].sourceItem.f0;
						currentPos += 1;
					}
					if (isEnergyDesired) {
						features[i][currentPos] = entries[i].sourceItem.energy;
						currentPos += 1;
					}
					if (isDurationDesired) {
						features[i][currentPos] = entries[i].sourceItem.duration;
						currentPos += 1;
					}
					if (isMfccDesired) {
						System.arraycopy(entries[i].sourceItem.mfccs, 0, features[i], currentPos, header.mfccParams.dimension);
						currentPos += header.mfccParams.dimension;
					}
				}
			}
		}

		return features;
	}

}
