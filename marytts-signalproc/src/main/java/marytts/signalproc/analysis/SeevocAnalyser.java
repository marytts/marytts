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
package marytts.signalproc.analysis;

import javax.swing.JFrame;

import marytts.signalproc.display.FunctionGraph;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * Spectral Enveope Estimation Vocoder (SEEVOC) - a simple implementation
 * 
 * Reference: Paul, D., 1981, "The Spectral Envelope Estimation Vocoder", IEEE Trans. Acoust. Speech Signal Proc., ASSP-29, pp.
 * 786-794.
 * 
 * @author Oytun T&uuml;rk
 */
public class SeevocAnalyser {
	public static SpectrumWithPeakIndices calcSpecEnvelopeDB(double[] absMagSpecIndB, int samplingRate) {
		return calcSpecEnvelopeDB(absMagSpecIndB, samplingRate, 100.0);
	}

	// The returned spectral envelope is in linear scale
	public static SpectrumWithPeakIndices calcSpecEnvelopeLinear(double[] absMagSpecIndB, int samplingRate, double f0) {
		SpectrumWithPeakIndices s = calcSpecEnvelopeDB(absMagSpecIndB, samplingRate, f0);
		s.spec = MathUtils.db2amp(s.spec);

		return s;
	}

	// The returned spectral envelope is also in dB
	public static SpectrumWithPeakIndices calcSpecEnvelopeDB(double[] absMagSpecIndB, int samplingRate, double f0) {
		SpectrumWithPeakIndices s = new SpectrumWithPeakIndices();
		int i, j;
		if (f0 < 10.0)
			f0 = 100.0f;

		int maxPeaks = (int) Math.floor(0.5 * samplingRate / f0 + 0.5) + 10;

		double[] peakVals = new double[maxPeaks];
		int[] peakInds = new int[maxPeaks];
		double[] peakFreqs = new double[maxPeaks];

		int maxFreqInd = absMagSpecIndB.length - 1;
		double[] freqs = new double[maxFreqInd + 1];

		for (i = 0; i <= maxFreqInd; i++)
			freqs[i] = SignalProcUtils.index2freq(i, samplingRate, maxFreqInd);

		int numPeaks = 0;
		double currentFreq = 0.0;
		double currentMax;
		int currentInd;
		int startInd, endInd;
		boolean bEndNotCovered = true;

		while (true) {
			if (currentFreq + 1.5 * f0 > 0.5 * samplingRate)
				break;

			startInd = SignalProcUtils.freq2index(currentFreq + 0.5 * f0, samplingRate, maxFreqInd);
			endInd = SignalProcUtils.freq2index(currentFreq + 1.5 * f0, samplingRate, maxFreqInd);

			startInd = Math.max(0, startInd);
			endInd = Math.min(endInd, maxFreqInd);
			startInd = Math.min(startInd, endInd);

			if (endInd == maxFreqInd)
				bEndNotCovered = false;

			currentInd = MathUtils.getAbsMaxInd(absMagSpecIndB, startInd, endInd);

			if (currentInd == -1) {
				peakVals[numPeaks + 1] = Math.max(absMagSpecIndB[startInd], absMagSpecIndB[endInd]);
				currentInd = (int) Math.floor(0.5 * (startInd + endInd) + 0.5);
			} else
				peakVals[numPeaks + 1] = absMagSpecIndB[currentInd];

			currentFreq = SignalProcUtils.index2freq(currentInd, samplingRate, maxFreqInd);

			peakInds[numPeaks + 1] = currentInd;
			peakFreqs[numPeaks + 1] = currentFreq;

			numPeaks++;

			// Search for the for the first interval
			if (numPeaks == 1) {
				startInd = 0;
				endInd = SignalProcUtils.freq2index(currentFreq - 0.5 * f0, samplingRate, maxFreqInd);

				startInd = Math.max(0, startInd);
				endInd = Math.min(endInd, maxFreqInd);
				startInd = Math.min(startInd, endInd);

				currentInd = MathUtils.getAbsMaxInd(absMagSpecIndB, startInd, endInd);

				if (currentInd == -1) {
					peakVals[0] = Math.max(absMagSpecIndB[startInd], absMagSpecIndB[endInd]);
					currentInd = (int) Math.floor(0.5 * (startInd + endInd) + 0.5);
				} else
					peakVals[0] = absMagSpecIndB[currentInd];

				peakInds[0] = currentInd;
				peakFreqs[0] = SignalProcUtils.index2freq(currentInd, samplingRate, maxFreqInd);
			}
			//

			if (numPeaks > maxPeaks - 3)
				break;
		}

		// Search for the last interval
		if (bEndNotCovered && numPeaks < maxPeaks) {
			startInd = SignalProcUtils.freq2index(currentFreq + 0.5 * f0, samplingRate, maxFreqInd);
			endInd = maxFreqInd;

			startInd = Math.max(0, startInd);
			startInd = Math.min(startInd, endInd);

			currentInd = MathUtils.getAbsMaxInd(absMagSpecIndB, startInd, endInd);

			if (currentInd == -1) {
				peakVals[numPeaks + 1] = Math.max(absMagSpecIndB[startInd], absMagSpecIndB[endInd]);
				currentInd = (int) Math.floor(0.5 * (startInd + endInd) + 0.5);
			} else
				peakVals[numPeaks + 1] = absMagSpecIndB[currentInd];

			peakInds[numPeaks + 1] = currentInd;
			peakFreqs[numPeaks + 1] = SignalProcUtils.index2freq(currentInd, samplingRate, maxFreqInd);

			numPeaks++;
		}
		//

		s.spec = new double[maxFreqInd + 1];

		for (j = 0; j < peakInds[0]; j++)
			s.spec[j] = absMagSpecIndB[0] + (absMagSpecIndB[peakInds[0]] - absMagSpecIndB[0]) / peakFreqs[0] * freqs[j];

		for (i = 0; i < numPeaks - 1; i++) {
			for (j = peakInds[i]; j < peakInds[i + 1]; j++)
				s.spec[j] = absMagSpecIndB[peakInds[i]] + (absMagSpecIndB[peakInds[i + 1]] - absMagSpecIndB[peakInds[i]])
						/ (peakFreqs[i + 1] - peakFreqs[i]) * (freqs[j] - peakFreqs[i]);
		}

		for (j = peakInds[numPeaks - 1]; j <= maxFreqInd; j++)
			s.spec[j] = absMagSpecIndB[peakInds[numPeaks - 1]]
					+ (absMagSpecIndB[maxFreqInd] - absMagSpecIndB[peakInds[numPeaks - 1]])
					/ (0.5 * samplingRate - peakFreqs[numPeaks - 1]) * (freqs[j] - peakFreqs[numPeaks - 1]);

		// s.spec = SignalProcUtils.medianFilter(H, 5, H[0], H[H.length-1]);
		// s.spec = SignalProcUtils.meanFilter(H, 19, H[0], H[H.length-1]);

		// Plot the DFT and the estimated spectral envelope to check visually
		// double [] excIndB = new double[s.spec.length];
		// for (i=0; i<s.spec.length; i++)
		// excIndB[i] = absMagSpecIndB[i]-H[i];

		// JFrame frame1 = showGraph(absMagSpecIndB, "DFT spectrum");
		// JFrame frame2 = showGraph(s.spec, "SEEVOC");
		// try { Thread.sleep(3000); } catch (InterruptedException e) {}
		// frame1.dispose();
		// frame2.dispose();

		s.indices = new int[numPeaks];
		System.arraycopy(peakInds, 0, s.indices, 0, numPeaks);

		return s;
	}

	protected static JFrame showGraph(double[] array, String title) {
		FunctionGraph graph = new FunctionGraph(400, 200, 0, 1, array);
		JFrame frame = graph.showInJFrame(title, 500, 300, true, false);

		return frame;
	}
}
