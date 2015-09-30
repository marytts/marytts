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

import marytts.signalproc.window.Window;
import marytts.util.data.DoubleDataSource;
import marytts.util.math.FFT;
import marytts.util.math.MathUtils;

/**
 * 
 * @author Marc Schr&ouml;der
 *
 *         Implements a frame based spectrum analyser
 * 
 */
public class ShortTermSpectrumAnalyser extends FrameBasedAnalyser<double[]> {
	protected double[] real;

	/**
	 * Initialise a FrameBasedAnalyser.
	 * 
	 * @param signal
	 *            the signal source to read from
	 * @param fftSize
	 *            the size of the FFT to use
	 * @param window
	 *            the window function to apply to each frame
	 * @param frameShift
	 *            the number of samples by which to shift the window from one frame analysis to the next; if this is smaller than
	 *            window.getLength(), frames will overlap.
	 * @param samplingRate
	 *            the number of samples in one second.
	 * @throws IllegalArgumentException
	 *             if the window is longer than fftSize, or fftSize is not a power of two.
	 */
	public ShortTermSpectrumAnalyser(DoubleDataSource signal, int fftSize, Window window, int frameShift, int samplingRate) {
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
	 * 
	 * @param frame
	 *            the data to analyse, which must be of the length prescribed by this FrameBasedAnalyser, i.e. by works like
	 *            {@link #getFrameLengthSamples()} .
	 * @return a double array of half the frame length
	 * @throws IllegalArgumentException
	 *             if frame does not have the prescribed length
	 */
	public double[] analyse(double[] frame) {
		if (frame.length != getFrameLengthSamples())
			throw new IllegalArgumentException("Expected frame of length " + getFrameLengthSamples() + ", got " + frame.length);
		System.arraycopy(frame, 0, real, 0, frame.length);
		if (real.length > frame.length)
			Arrays.fill(real, frame.length, real.length, 0);
		FFT.realTransform(real, false);
		return FFT.computePowerSpectrum_FD(real);
	}

	/**
	 * The distance of two adjacent points on the frequency axis, in Hertz.
	 * 
	 * @return samplingRate / real.length
	 */
	public double getFrequencyResolution() {
		return (double) samplingRate / real.length;
	}

	public int getFFTWindowLength() {
		return real.length;
	}

}
