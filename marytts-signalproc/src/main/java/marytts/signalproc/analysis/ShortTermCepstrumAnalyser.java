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
public class ShortTermCepstrumAnalyser extends FrameBasedAnalyser<double[]> {
	int fftSize;
	int invFftSize;
	double frequencyResolution;
	double quefrencyResolution;

	/**
	 * Initialise a FrameBasedAnalyser.
	 * 
	 * @param signal
	 *            the signal source to read from
	 * @param fftSize
	 *            the size of the FFT to use
	 * @param invFftSize
	 *            inverted FftSize
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
	public ShortTermCepstrumAnalyser(DoubleDataSource signal, int fftSize, int invFftSize, Window window, int frameShift,
			int samplingRate) {
		super(signal, window, frameShift, samplingRate);
		if (window.getLength() > fftSize)
			throw new IllegalArgumentException("Window must not be longer than fftSize");
		if (!MathUtils.isPowerOfTwo(fftSize))
			throw new IllegalArgumentException("fftSize must be a power of two!");
		if (!MathUtils.isPowerOfTwo(invFftSize))
			throw new IllegalArgumentException("invFftSize must be a power of two!");
		this.fftSize = fftSize;
		this.invFftSize = invFftSize;
		assert fftSize >= frame.length;

		this.frequencyResolution = (double) samplingRate / fftSize;
		this.quefrencyResolution = (double) fftSize / ((double) samplingRate * invFftSize);
	}

	/**
	 * Apply this FrameBasedAnalyser to the given data.
	 * 
	 * @param aFrame
	 *            the data to analyse, which must be of the length prescribed by this FrameBasedAnalyser, i.e. by similar to
	 *            {@link #getFrameLengthSamples()} .
	 * @return a double array of half the frame length
	 * @throws IllegalArgumentException
	 *             if frame does not have the prescribed length
	 */
	@Override
	public double[] analyse(double[] aFrame) {
		if (aFrame.length != frameLength)
			throw new IllegalArgumentException("Expected frame of length " + frameLength + ", got " + aFrame.length);
		double[] real = new double[fftSize];
		double[] imag = new double[fftSize];
		System.arraycopy(aFrame, 0, real, 0, aFrame.length);
		FFT.transform(real, imag, false);
		// Now real + j*imag is the complex spectrum
		MathUtils.toPolarCoordinates(real, imag);
		// now real = abs(X), imag = phi
		real = MathUtils.log(real);
		Arrays.fill(imag, 0.);

		// For computing the cepstrum, use only frequencies below b:
		double b = 5000; // Hz
		int bIndex = (int) (b / frequencyResolution);
		double[] invReal;
		double[] invImag;
		if (invFftSize == fftSize) {
			invReal = real;
			invImag = imag;
		} else {
			invReal = new double[invFftSize];
			System.arraycopy(real, 0, invReal, 0, bIndex + 1);
			invImag = new double[invFftSize];
		}
		for (int i = bIndex + 1; i < invFftSize / 2; i++) {
			invReal[i] = invReal[bIndex];
		}
		for (int i = 0; i < invFftSize / 2; i++) {
			invReal[invFftSize - i - 1] = invReal[i];
		}
		FFT.transform(invReal, invImag, true);
		return invReal;
	}

	/**
	 * The distance of two adjacent points on the quefrency axis, in ms
	 * 
	 * @return quefrencyResolution
	 */
	public double getQuefrencyResolution() {
		return quefrencyResolution;
	}

	public int getFFTWindowLength() {
		return fftSize;
	}

	public int getInverseFFTWindowLength() {
		return invFftSize;
	}

}
