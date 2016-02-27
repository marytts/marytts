/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.signalproc.adaptation.prosody;

import java.io.IOException;

import marytts.signalproc.adaptation.BaselineAdaptationSet;
import marytts.signalproc.adaptation.IndexMap;
import marytts.signalproc.adaptation.codebook.WeightedCodebookFeatureCollection;
import marytts.signalproc.adaptation.codebook.WeightedCodebookTrainerParams;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

public class PitchTrainer {
	private WeightedCodebookTrainerParams params;

	public PitchTrainer(WeightedCodebookTrainerParams pa) {
		params = new WeightedCodebookTrainerParams(pa);
	}

	// A number of f0 based analyses performed here for mapping source and target patterns:
	// Global parameters (in Hz):
	// Mean of all voiced f0s
	// Standard deviation of all voiced f0s
	// Global minimum of all voiced f0s
	// Global maximum of all voiced f0s
	// Average tilt of all f0 contours
	//
	// Contour parameters (in Hz):
	// Mean of voiced f0s
	// Standard deviation of voiced f0s
	// Minimum of f0s
	// Maximum of f0s
	// Average tilt of f0s
	//
	// and all above parameters computed for log(f0s)
	// (This may work better since pitch perception is logarithmic)
	public void learnMapping(PitchMappingFile pitchMappingFile, WeightedCodebookFeatureCollection fcol,
			BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int[] map) {
		PitchMappingFileHeader header = new PitchMappingFileHeader();
		pitchMappingFile.writePitchMappingHeader(header);

		getStatistics(pitchMappingFile, fcol, sourceTrainingSet, true, map, PitchStatistics.STATISTICS_IN_HERTZ); // Source,
																													// Hertz:
																													// Locals+Global
		getStatistics(pitchMappingFile, fcol, sourceTrainingSet, true, map, PitchStatistics.STATISTICS_IN_LOGHERTZ); // Source,
																														// logHertz:
																														// Locals+Global
		getStatistics(pitchMappingFile, fcol, targetTrainingSet, false, map, PitchStatistics.STATISTICS_IN_HERTZ); // Target,
																													// Hertz:
																													// Locals+Global
		getStatistics(pitchMappingFile, fcol, targetTrainingSet, false, map, PitchStatistics.STATISTICS_IN_LOGHERTZ); // Target,
																														// logHertz:
																														// Locals+Global
	}

	public void getStatistics(PitchMappingFile pitchMappingFile, WeightedCodebookFeatureCollection fcol,
			BaselineAdaptationSet trainingSet, boolean isSource, int[] map, int statisticsType) {
		PitchStatistics global = new PitchStatistics(statisticsType, isSource, true);
		PitchStatistics local = new PitchStatistics(statisticsType, isSource, false);

		PitchEntry pitchEntry = null;

		PitchReaderWriter f0s = null;

		double[] voiceds = null;

		double[] contourInt = null;
		double[] line = null;

		int globalCount = 0;
		double temp, tempSum;
		int tiltCount = 0;

		global.init();

		IndexMap imap = new IndexMap();
		int i;

		for (i = 0; i < fcol.indexMapFiles.length; i++) {
			System.out.println("Pitch mapping for pair " + String.valueOf(i + 1) + " of "
					+ String.valueOf(fcol.indexMapFiles.length) + ":");

			try {
				imap.readFromFile(fcol.indexMapFiles[i]); // imap keeps information about a single source-target pair only
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (imap.files != null && trainingSet.items.length > i) {
				local.init();

				if (isSource)
					f0s = new PitchReaderWriter(trainingSet.items[i].pitchFile);
				else
					f0s = new PitchReaderWriter(trainingSet.items[map[i]].pitchFile);

				voiceds = f0s.getVoiceds();

				local.range = SignalProcUtils.getF0Range(voiceds);

				if (statisticsType == PitchStatistics.STATISTICS_IN_LOGHERTZ) {
					f0s.contour = SignalProcUtils.getLogF0s(f0s.contour);
					if (voiceds != null)
						voiceds = SignalProcUtils.getLogF0s(voiceds);

					local.range = Math.log(local.range);
				}

				if (voiceds != null) {
					tempSum = MathUtils.sum(voiceds);
					global.mean += tempSum;
					globalCount += voiceds.length;
					local.mean = tempSum / voiceds.length;
					local.standardDeviation = MathUtils.standardDeviation(voiceds, local.mean);

					if (i == 0 || local.range > global.range)
						global.range = local.range;

					contourInt = SignalProcUtils.interpolate_pitch_uv(f0s.contour);
					line = SignalProcUtils.getContourLSFit(contourInt, false);
					local.intercept = line[0];
					local.slope = line[1];

					global.intercept += local.intercept;
					global.slope += local.slope;
					tiltCount++;
				}

				pitchMappingFile.writeF0StatisticsEntry(local);
			}
		}

		if (globalCount > 0)
			global.mean /= globalCount;
		else
			global.mean = 0.0;

		if (tiltCount > 0) {
			global.intercept /= tiltCount;
			global.slope /= tiltCount;
		}

		System.out.println("Computing global pitch standard deviations...");

		tempSum = 0.0;
		for (i = 0; i < fcol.indexMapFiles.length; i++) {
			try {
				imap.readFromFile(fcol.indexMapFiles[i]); // imap keeps information about a single source-target pair only
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (imap.files != null && trainingSet.items.length > i) {
				if (isSource)
					f0s = new PitchReaderWriter(trainingSet.items[i].pitchFile);
				else
					f0s = new PitchReaderWriter(trainingSet.items[map[i]].pitchFile);

				voiceds = f0s.getVoiceds();

				if (voiceds != null)
					tempSum += MathUtils.sumSquared(voiceds, -1.0 * global.mean);
			}
		}

		if (globalCount > 1)
			global.standardDeviation = Math.sqrt(tempSum / (globalCount - 1));
		else
			global.standardDeviation = 1.0;

		pitchMappingFile.writeF0StatisticsEntry(global);
	}
}
