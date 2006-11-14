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
import de.dfki.lt.signalproc.window.Window;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class ShortTermSpectrumAnalyser extends FrameBasedAnalyser
{
    protected double[] real;

    /**
     * Initialise a FrameBasedAnalyser.
     * @param signal the signal source to read from
     * @param fftSize the size of the FFT to use
     * @param window the window function to apply to each frame
     * @param frameShift the number of samples by which to shift the window from
     * one frame analysis to the next; if this is smaller than window.getLength(),
     * frames will overlap.
     * @param samplingRate the number of samples in one second.
     * @throws IllegalArgumentException if the window is longer than fftSize, or
     * fftSize is not a power of two.
     */
    public ShortTermSpectrumAnalyser(DoubleDataSource signal, int fftSize, Window window,
            int frameShift, int samplingRate) {
        super(signal, window, frameShift, samplingRate);
        if (window.getLength() > fftSize)
            throw new IllegalArgumentException("Window must not be longer than fftSize");
        if (!MathUtils.isPowerOfTwo(fftSize))
            throw new IllegalArgumentException("fftSize must be a power of two!");
        real = new double[fftSize];
        assert real.length >= frame.length;
    }

    /**
     * Apply this FrameBasedAnalyser to the given data.
     * @param frame the data to analyse, which must be of the length prescribed by this
     * FrameBasedAnalyser, i.e. by @see{#getFrameLengthSamples()}.
     * @return a double array of half the frame length
     * @throws IllegalArgumentException if frame does not have the prescribed length 
     */
    public Object analyse(double[] frame)
    {
        if (frame.length != getFrameLengthSamples())
            throw new IllegalArgumentException("Expected frame of length " + getFrameLengthSamples()
                    + ", got " + frame.length);
        System.arraycopy(frame, 0, real, 0, frame.length);
        if (real.length > frame.length)
            Arrays.fill(real, frame.length, real.length, 0);
        FFT.realTransform(real, false);
        double[] analysisResult = FFT.computePowerSpectrum_FD(real);
        return analysisResult;
    }

    /**
     * The distance of two adjacent points on the frequency axis, in Hertz.
     */
    public double getFrequencyResolution()
    {
        return (double)samplingRate/real.length;
    }
    
    public int getFFTWindowLength()
    {
        return real.length;
    }
    
}
