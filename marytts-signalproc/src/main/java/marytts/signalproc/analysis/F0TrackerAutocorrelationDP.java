/**
 * Copyright 2004-2006 DFKI GmbH.
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

import java.util.Arrays;

import marytts.signalproc.window.BlackmanWindow;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.math.FFT;
import marytts.util.math.MathUtils;

/**
 * 
 * @author Marc Schr&ouml;der
 * 
 *         Autocorrelation based F0 tracker with dynamic programming
 * 
 */
public class F0TrackerAutocorrelationDP extends F0Tracker {
	protected DoubleDataSource preprocess(DoubleDataSource signal) {
		double CENTERCLIPPING_THRESHOLD = 0.2;
		double[] signalData = signal.getAllData();
		// Remove DC component:
		double mean = MathUtils.mean(signalData);
		// System.err.println("Removing DC component: " + mean);
		for (int i = 0; i < signalData.length; i++) {
			signalData[i] -= mean;
		}
		// Center clipping
		double maxAmplitude = MathUtils.absMax(signalData);
		double cutoff = maxAmplitude * CENTERCLIPPING_THRESHOLD;
		for (int i = 0; i < signalData.length; i++) {
			if (Math.abs(signalData[i]) < cutoff)
				signalData[i] = 0;
		}
		return new BufferedDoubleDataSource(signalData);
	}

	protected FrameBasedAnalyser getCandidateEstimator(DoubleDataSource preprocessedSignal, int samplingRate) {
		// Window length should be about 3 periods at minimum F0:
		int windowLength = 3 * samplingRate / DEFAULT_MINF0;
		// make sure it is an odd number:
		if (windowLength % 2 == 0)
			windowLength++;
		Window window = new BlackmanWindow(windowLength);
		// System.err.println("Window length: " + window.getLength());
		int windowShift = windowLength / 2;
		return new CandidateEstimator(preprocessedSignal, window, windowShift, samplingRate);
	}

	protected TransitionCost getTransitionCost() {
		return null;
	}

	public class CandidateEstimator extends F0Tracker.CandidateEstimator {
		public static final int NCANDIDATES = 15;
		protected int minF0;
		protected int maxF0;
		protected double[] correlationInput;

		/**
		 * Track the F0 contour, using the Autocorrelation method.
		 * 
		 * @param signal
		 *            the signal for which to track the F0 contour
		 * @param window
		 *            the Window to use for cutting out frames
		 * @param frameShift
		 *            the number of samples to shift between frames
		 * @param samplingRate
		 *            the sampling rate of the signal, in samples per second
		 * @param minF0
		 *            minF0
		 * @param maxF0
		 *            maxF0
		 */
		public CandidateEstimator(DoubleDataSource signal, Window window, int frameShift, int samplingRate, int minF0, int maxF0) {
			this(signal, window, frameShift, samplingRate);
			this.minF0 = minF0;
			this.maxF0 = maxF0;
		}

		/**
		 * Track the F0 contour, using the Autocorrelation method.
		 * 
		 * @param signal
		 *            the signal for which to track the F0 contour
		 * @param window
		 *            the Window to use for cutting out frames
		 * @param frameShift
		 *            the number of samples to shift between frames
		 * @param samplingRate
		 *            the sampling rate of the signal, in samples per second
		 */
		public CandidateEstimator(DoubleDataSource signal, Window window, int frameShift, int samplingRate) {
			super(signal, window, frameShift, samplingRate, NCANDIDATES);
			this.correlationInput = new double[MathUtils.closestPowerOfTwoAbove(2 * window.getLength())];
			this.minF0 = DEFAULT_MINF0;
			this.maxF0 = DEFAULT_MAXF0;
		}

		protected void findCandidates(F0Candidate[] candidates, double[] frame) {
			System.arraycopy(frame, 0, correlationInput, 0, frame.length);
			Arrays.fill(correlationInput, frame.length, correlationInput.length, 0);
			double[] acf = FFT.autoCorrelate(correlationInput);
			// Simple model: take all the peaks, rate them by their height.
			int valley = MathUtils.findNextValleyLocation(acf, 0);
			int peak;
			double deltaT = 1. / samplingRate;
			while ((peak = MathUtils.findNextPeakLocation(acf, valley)) != acf.length - 1) {
				double f0 = 1 / (peak * deltaT);
				// Ignore implausible f0 values:
				if (f0 >= minF0 && f0 <= maxF0) {
					addCandidate(candidates, new F0Candidate(f0, acf[peak]));
				}
				valley = MathUtils.findNextValleyLocation(acf, peak);
			}
		}
	}
}
