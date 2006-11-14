/**
 * Copyright 2004-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package de.dfki.lt.signalproc.analysis;

import java.util.Arrays;

import de.dfki.lt.signalproc.FFT;
import de.dfki.lt.signalproc.window.BlackmanWindow;
import de.dfki.lt.signalproc.window.Window;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class F0TrackerAutocorrelation extends F0Tracker
{
    
    protected DoubleDataSource preprocess(DoubleDataSource signal)
    {
        double CENTERCLIPPING_THRESHOLD = 0.2;
        double[] signalData = signal.getAllData();
        // Remove DC component:
        double mean = MathUtils.mean(signalData);
        //System.err.println("Removing DC component: " + mean);
        for (int i=0; i<signalData.length; i++) {
            signalData[i] -= mean;
        }
        // Center clipping
        double maxAmplitude = MathUtils.absMax(signalData);
        double cutoff = maxAmplitude * CENTERCLIPPING_THRESHOLD;
        for (int i=0; i<signalData.length; i++) {
            if (Math.abs(signalData[i]) < cutoff) signalData[i] = 0; 
        }
        return new BufferedDoubleDataSource(signalData);
    }

    protected FrameBasedAnalyser getCandidateEstimator(DoubleDataSource preprocessedSignal, int samplingRate)
    {
        // Window length should be about 3 periods at minimum F0:
        int windowLength = 3*samplingRate/DEFAULT_MINF0;
        // make sure it is an odd number:
        if (windowLength%2 == 0) windowLength++;
        Window window = new BlackmanWindow(windowLength);
        //System.err.println("Window length: " + window.getLength());
        int windowShift = windowLength / 2;
        return new CandidateEstimator(preprocessedSignal, window, windowShift, samplingRate);
    }
    
    protected TransitionCost getTransitionCost()
    {
        return null;
    }
    
    public class CandidateEstimator extends F0Tracker.CandidateEstimator
    {
        public static final int NCANDIDATES = 15;
        protected int minF0;
        protected int maxF0;
        protected double[] correlationInput;

        /**
         * Track the F0 contour, using the Autocorrelation method.
         * @param signal the signal for which to track the F0 contour
         * @param window the Window to use for cutting out frames
         * @param frameShift the number of samples to shift between frames
         * @param samplingRate the sampling rate of the signal, in samples per second
         */
        public CandidateEstimator(DoubleDataSource signal, Window window,
                int frameShift, int samplingRate, int minF0, int maxF0) {
            this(signal, window, frameShift, samplingRate);
            this.minF0 = minF0;
            this.maxF0 = maxF0;
        }

        /**
         * Track the F0 contour, using the Autocorrelation method.
         * @param signal the signal for which to track the F0 contour
         * @param window the Window to use for cutting out frames
         * @param frameShift the number of samples to shift between frames
         * @param samplingRate the sampling rate of the signal, in samples per second
         */
        public CandidateEstimator(DoubleDataSource signal, Window window,
                int frameShift, int samplingRate) {
            super(signal, window, frameShift, samplingRate, NCANDIDATES);
            this.correlationInput = new double[MathUtils.closestPowerOfTwoAbove(2*window.getLength())];
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
            double deltaT = 1./samplingRate;
            while ((peak = MathUtils.findNextPeakLocation(acf, valley)) != acf.length - 1) {
                double f0 = 1/(peak*deltaT);
                // Ignore implausible f0 values:
                if (f0>=minF0 && f0<=maxF0) {
                    addCandidate(candidates, new F0Candidate(f0, acf[peak]));
                }
                valley = MathUtils.findNextValleyLocation(acf, peak);
            }
        }
    }
}
