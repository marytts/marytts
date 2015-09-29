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
package marytts.signalproc.adaptation.prosody;

import java.util.Arrays;

import marytts.util.math.MathUtils;

/**
 * @author Oytun T&uuml;rk
 * 
 */
public class PitchStatisticsMapping {
	public PitchStatistics sourceGlobalStatisticsHz;
	public PitchStatistics targetGlobalStatisticsHz;
	public PitchStatisticsCollection sourceLocalStatisticsHz;
	public PitchStatisticsCollection targetLocalStatisticsHz;
	public PitchStatistics sourceGlobalStatisticsLogHz;
	public PitchStatistics targetGlobalStatisticsLogHz;
	public PitchStatisticsCollection sourceLocalStatisticsLogHz;
	public PitchStatisticsCollection targetLocalStatisticsLogHz;
	public double[] sourceVariancesHz;
	public double[] targetVariancesHz;
	public double[] sourceVariancesLogHz;
	public double[] targetVariancesLogHz;

	public PitchStatisticsMapping(PitchStatisticsCollection allFromTraining) {
		PitchStatisticsCollection tmpCollection = null;
		// Hertz statistics
		sourceGlobalStatisticsHz = new PitchStatistics(allFromTraining.getGlobalStatisticsSourceHz());
		targetGlobalStatisticsHz = new PitchStatistics(allFromTraining.getGlobalStatisticsTargetHz());
		sourceLocalStatisticsHz = new PitchStatisticsCollection(allFromTraining.getLocalStatisticsSourceHz());
		targetLocalStatisticsHz = new PitchStatisticsCollection(allFromTraining.getLocalStatisticsTargetHz());
		sourceVariancesHz = setVariances(sourceLocalStatisticsHz);
		targetVariancesHz = setVariances(targetLocalStatisticsHz);

		// Log Hertz statistics
		sourceGlobalStatisticsLogHz = new PitchStatistics(allFromTraining.getGlobalStatisticsSourceLogHz());
		targetGlobalStatisticsLogHz = new PitchStatistics(allFromTraining.getGlobalStatisticsTargetLogHz());
		sourceLocalStatisticsLogHz = new PitchStatisticsCollection(allFromTraining.getLocalStatisticsSourceLogHz());
		targetLocalStatisticsLogHz = new PitchStatisticsCollection(allFromTraining.getLocalStatisticsTargetLogHz());
		sourceVariancesLogHz = setVariances(sourceLocalStatisticsLogHz);
		targetVariancesLogHz = setVariances(targetLocalStatisticsLogHz);
	}

	private double[] setVariances(PitchStatisticsCollection p) {
		double[] variances = new double[5];
		if (p.entries != null) {
			if (p.entries.length < 2)
				Arrays.fill(variances, 1.0);
			else {
				int i, j;
				double[][] vals = new double[variances.length][];
				for (i = 0; i < variances.length; i++)
					vals[i] = new double[p.entries.length];

				for (j = 0; j < p.entries.length; j++)
					vals[0][j] = p.entries[j].mean;

				for (j = 0; j < p.entries.length; j++)
					vals[1][j] = p.entries[j].standardDeviation;

				for (j = 0; j < p.entries.length; j++)
					vals[2][j] = p.entries[j].range;

				for (j = 0; j < p.entries.length; j++)
					vals[3][j] = p.entries[j].intercept;

				for (j = 0; j < p.entries.length; j++)
					vals[4][j] = p.entries[j].slope;

				variances = MathUtils.getVarianceRows(vals);
			}
		}

		return variances;
	}
}
