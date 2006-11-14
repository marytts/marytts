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

package de.dfki.lt.signalproc.window;

import de.dfki.lt.signalproc.display.FunctionGraph;
import de.dfki.lt.signalproc.display.LogSpectrum;
import de.dfki.lt.signalproc.util.MathUtils;


/**
 * @author Marc Schr&ouml;der
 */
public class GaussWindow extends Window
{
    public static final double DEFAULT_SIGMA = 100;
    protected double sigma;
    protected double sigmasquare;

    /**
     * Crate a Gauss window with the given length and a default sigma.
     * @param length the length of the window, in samples (must be an odd number)
     */
    public GaussWindow(int length)
    {
        this(length, DEFAULT_SIGMA, 1.);
    }

    /**
     * Crate a Gauss window with the given length and a default sigma, and apply
     * a prescaling factor to each sample in the window.
     * @param length the length of the window, in samples (must be an odd number)
     */
    public GaussWindow(int length, double prescalingFactor)
    {
        this(length, DEFAULT_SIGMA, prescalingFactor);
    }

    /**
     * Create a Gauss window with the given length and sigma.
     * @param length the length of the window, in samples (should be an odd number)
     * @param sigma the sigma coefficient in the Gauss curve. A good starting point is 100.
     */
    public GaussWindow(int length, double sigma, double prescalingFactor)
    {
        window = new double[length];
        this.sigma = sigma;
        this.sigmasquare = sigma*sigma;
        this.prescalingFactor = prescalingFactor;
        initialise();
    }
    
    protected void initialise()
    {
        int mid = window.length/2+1;
        for (int i=0; i<window.length; i++) {
            int dist = i-mid;
            window[i] = Math.exp(-0.5*dist*dist/sigmasquare);
        }
    }
    
    public String toString()
    {
        return "Gauss window";
    }
    
    public static void main(String[] args)
    {
        int samplingRate = Integer.getInteger("samplingrate", 1).intValue();
        int windowLengthMs = Integer.getInteger("windowlength.ms", 0).intValue();
        int windowLength = Integer.getInteger("windowlength.samples", 512).intValue();
        // If both are given, use window length in milliseconds:
        if(windowLengthMs != 0) windowLength = windowLengthMs * samplingRate / 1000;
        int fftSize = Math.max(4096, MathUtils.closestPowerOfTwoAbove(windowLength));
        Window w = new GaussWindow(windowLength);
        FunctionGraph timeGraph = new FunctionGraph(0, 1./samplingRate, w.window);
        timeGraph.showInJFrame(w.toString() + " in time domain", true, false);
        double[] fftSignal = new double[fftSize];
        // fftSignal should integrate to one, so normalise amplitudes:
        double sum = MathUtils.sum(w.window);
        for (int i=0; i<w.window.length; i++) {
            fftSignal[i] = w.window[i] / sum;
        }
        LogSpectrum freqGraph = new LogSpectrum(fftSignal, samplingRate);
        freqGraph.showInJFrame(w.toString() + " log frequency response", true, false);
    }

}
