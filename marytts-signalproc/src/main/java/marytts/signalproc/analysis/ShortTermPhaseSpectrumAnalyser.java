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

/**
 * 
 * @author Marc Schr&ouml;der
 * 
 *         Implements a frame based phase spectrum analyser
 * 
 */
public class ShortTermPhaseSpectrumAnalyser extends ShortTermSpectrumAnalyser {

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
	 *            {@link marytts.signalproc.window.Window#getLength() window.getLength()}, frames will overlap.
	 * @param samplingRate
	 *            the number of samples in one second.
	 * @throws IllegalArgumentException
	 *             if the window is longer than fftSize, or fftSize is not a power of two.
	 */
	public ShortTermPhaseSpectrumAnalyser(DoubleDataSource signal, int fftSize, Window window, int frameShift, int samplingRate) {
		super(signal, fftSize, window, frameShift, samplingRate);
	}

	/**
	 * Apply this FrameBasedAnalyser to the given data.
	 * 
	 * @param frame
	 *            the data to analyse, which must be of the length prescribed by this FrameBasedAnalyser, i.e. by
	 *            {@link #getFrameLengthSamples()}.
	 * @return a double array of half the frame length
	 * @throws IllegalArgumentException
	 *             if frame does not have the prescribed length
	 */
	public double[] analyse(double[] frame) {
		if (frame.length != getFrameLengthSamples())
			throw new IllegalArgumentException("Expected frame of length " + getFrameLengthSamples() + ", got " + frame.length);
		// For correct phase, center time origin in the middle of windowed frame:
		int len = frame.length;
		int middle = len / 2 + len % 2; // e.g., 3 if len==5
		System.arraycopy(frame, middle, real, 0, len - middle);
		System.arraycopy(frame, 0, real, real.length - middle, middle);
		if (real.length > frame.length)
			Arrays.fill(real, len - middle, real.length - middle, 0);
		FFT.realTransform(real, false);
		return FFT.computePhaseSpectrum_FD(real);
	}

}
