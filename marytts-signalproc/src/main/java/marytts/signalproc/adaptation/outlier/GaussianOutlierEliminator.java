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
package marytts.signalproc.adaptation.outlier;

import java.io.IOException;
import java.util.Arrays;

import marytts.signalproc.adaptation.OutlierStatus;
import marytts.signalproc.adaptation.codebook.WeightedCodebook;
import marytts.signalproc.adaptation.codebook.WeightedCodebookFile;
import marytts.signalproc.adaptation.codebook.WeightedCodebookFileHeader;
import marytts.signalproc.adaptation.codebook.WeightedCodebookMapperParams;
import marytts.signalproc.analysis.distance.DistanceComputer;
import marytts.util.math.MathUtils;

/**
 * 
 * Single Gaussian based outlier elimination. Looks at the difference distributions of aligned source-target LSF, f0, energy, and
 * duration features. Eliminates outliers that fall outside user specified ranges. The ranges can be specified using
 * GaussianOutlierEliminatorParams in the form of total standard deviations for each feature. It is also possible to eliminate too
 * similar feature pairs to enforce a certain amount of dissimilarity in the training data.
 * 
 * Reference: T&uuml;rk, O., and Arslan, L. M., 2006, "Robust Processing Techniques for Voice Conversion", Computer Speech and
 * Language 20 (2006), pp. 441-467.
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class GaussianOutlierEliminator {
	public void eliminate(GaussianOutlierEliminatorParams params, String codebookFileIn, String codebookFileOut) {
		WeightedCodebookFile fileIn = new WeightedCodebookFile(codebookFileIn, WeightedCodebookFile.OPEN_FOR_READ);

		WeightedCodebook codebookIn = null;

		try {
			codebookIn = fileIn.readCodebookFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (codebookIn != null) {
			int[] acceptanceStatus = new int[codebookIn.header.totalEntries];

			double[] lsfDistances = null;
			if (params.isCheckLsfOutliers)
				lsfDistances = new double[codebookIn.header.totalEntries];

			double[] f0Distances = null;
			int[] voicedInds = null;
			if (params.isCheckF0Outliers) {
				f0Distances = new double[codebookIn.header.totalEntries];
				voicedInds = new int[codebookIn.header.totalEntries];
				Arrays.fill(voicedInds, -1);
			}

			double[] durationDistances = null;
			if (params.isCheckDurationOutliers)
				durationDistances = new double[codebookIn.header.totalEntries];

			double[] energyDistances = null;
			if (params.isCheckEnergyOutliers)
				energyDistances = new double[codebookIn.header.totalEntries];

			Arrays.fill(acceptanceStatus, OutlierStatus.NON_OUTLIER);

			double lsfDistanceMean = 0.0;
			double lsfDistanceStdDev = 0.0;
			double f0DistanceMean = 0.0;
			double f0DistanceStdDev = 0.0;
			int totalVoiced = 0;
			double durationDistanceMean = 0.0;
			double durationDistanceStdDev = 0.0;
			double energyDistanceMean = 0.0;
			double energyDistanceStdDev = 0.0;

			int i;

			// Estimate mean of distances between source and target entries
			for (i = 0; i < codebookIn.header.totalEntries; i++) {
				if (params.isCheckLsfOutliers)
					lsfDistances[i] = DistanceComputer.getLsfInverseHarmonicDistance(codebookIn.entries[i].sourceItem.lsfs,
							codebookIn.entries[i].targetItem.lsfs, WeightedCodebookMapperParams.DEFAULT_FREQ_RANGE_FOR_LSF_MATCH);

				if (params.isCheckF0Outliers && codebookIn.entries[i].sourceItem.f0 > 10.0
						&& codebookIn.entries[i].targetItem.f0 > 10.0) {
					f0Distances[totalVoiced] = codebookIn.entries[i].sourceItem.f0 - codebookIn.entries[i].targetItem.f0;
					voicedInds[totalVoiced] = i;
					totalVoiced++;
				}

				if (params.isCheckDurationOutliers)
					durationDistances[i] = codebookIn.entries[i].sourceItem.duration - codebookIn.entries[i].targetItem.duration;

				if (params.isCheckEnergyOutliers)
					energyDistances[i] = codebookIn.entries[i].sourceItem.energy - codebookIn.entries[i].targetItem.energy;
			}

			if (params.isCheckLsfOutliers)
				lsfDistanceMean = MathUtils.mean(lsfDistances);

			if (params.isCheckF0Outliers)
				f0DistanceMean = MathUtils.mean(f0Distances, 0, totalVoiced - 1);

			if (params.isCheckDurationOutliers)
				durationDistanceMean = MathUtils.mean(durationDistances);

			if (params.isCheckEnergyOutliers)
				energyDistanceMean = MathUtils.mean(energyDistances);
			//

			// Estimate standard deviation of distances between source and target
			lsfDistanceStdDev = 0.5 * Double.MAX_VALUE;
			durationDistanceStdDev = 0.5 * Double.MAX_VALUE;
			energyDistanceStdDev = 0.5 * Double.MAX_VALUE;
			f0DistanceStdDev = 0.5 * Double.MAX_VALUE;

			if (codebookIn.header.totalEntries > 1) {
				if (params.isCheckLsfOutliers)
					lsfDistanceStdDev = MathUtils.standardDeviation(lsfDistances, lsfDistanceMean);

				if (params.isCheckDurationOutliers)
					durationDistanceStdDev = MathUtils.standardDeviation(durationDistances, durationDistanceMean);

				if (params.isCheckEnergyOutliers)
					energyDistanceStdDev = MathUtils.standardDeviation(energyDistances, energyDistanceMean);
			}

			if (params.isCheckF0Outliers && totalVoiced > 1)
				f0DistanceStdDev = MathUtils.standardDeviation(f0Distances, f0DistanceMean, 0, totalVoiced - 1);
			//

			int totalLsfOutliers = 0;
			int totalDurationOutliers = 0;
			int totalF0Outliers = 0;
			int totalEnergyOutliers = 0;
			for (i = 0; i < codebookIn.header.totalEntries; i++) {
				if (params.isCheckLsfOutliers) {
					if (lsfDistances[i] > lsfDistanceMean + params.totalStandardDeviations.lsf * lsfDistanceStdDev
							|| (params.isEliminateTooSimilarLsf && lsfDistances[i] < lsfDistanceMean
									- params.totalStandardDeviations.lsf * lsfDistanceStdDev)) {
						acceptanceStatus[i] += OutlierStatus.LSF_OUTLIER;
						totalLsfOutliers++;
					}
				}

				if (params.isCheckDurationOutliers
						&& durationDistances[i] > durationDistanceMean + params.totalStandardDeviations.duration
								* durationDistanceStdDev) {
					acceptanceStatus[i] += OutlierStatus.DURATION_OUTLIER;
					totalDurationOutliers++;
				}

				if (params.isCheckEnergyOutliers
						&& energyDistances[i] > energyDistanceMean + params.totalStandardDeviations.energy * energyDistanceStdDev) {
					acceptanceStatus[i] += OutlierStatus.ENERGY_OUTLIER;
					totalEnergyOutliers++;
				}

			}

			if (params.isCheckF0Outliers) {
				for (i = 0; i < totalVoiced; i++) {
					if (f0Distances[i] > f0DistanceMean + params.totalStandardDeviations.f0 * f0DistanceStdDev) {
						acceptanceStatus[voicedInds[i]] += OutlierStatus.F0_OUTLIER;
						totalF0Outliers++;
					}
				}
			}

			int newTotalEntries = 0;
			for (i = 0; i < codebookIn.header.totalEntries; i++) {
				if (acceptanceStatus[i] == OutlierStatus.NON_OUTLIER)
					newTotalEntries++;
			}

			// Write the output codebook
			WeightedCodebookFile codebookOut = new WeightedCodebookFile(codebookFileOut, WeightedCodebookFile.OPEN_FOR_WRITE);

			WeightedCodebookFileHeader headerOut = new WeightedCodebookFileHeader(codebookIn.header);
			headerOut.resetTotalEntries();
			codebookOut.writeCodebookHeader(headerOut);

			for (i = 0; i < codebookIn.header.totalEntries; i++) {
				if (acceptanceStatus[i] == OutlierStatus.NON_OUTLIER)
					codebookOut.writeEntry(codebookIn.entries[i]);
			}

			codebookOut.close();
			//

			System.out.println("Outliers detected = " + String.valueOf(codebookIn.header.totalEntries - newTotalEntries) + " of "
					+ String.valueOf(codebookIn.header.totalEntries));
			System.out.println("Total lsf outliers = " + String.valueOf(totalLsfOutliers));
			System.out.println("Total f0 outliers = " + String.valueOf(totalF0Outliers));
			System.out.println("Total duration outliers = " + String.valueOf(totalDurationOutliers));
			System.out.println("Total energy outliers = " + String.valueOf(totalEnergyOutliers));

		}
	}
}
