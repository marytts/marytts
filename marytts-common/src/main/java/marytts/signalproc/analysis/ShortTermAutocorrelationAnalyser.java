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
 *         Implements a frame based autocorrelation analyser
 * 
 */
public class ShortTermAutocorrelationAnalyser extends FrameBasedAnalyser {
	protected double[] correlationInput;

	/**
	 * @param signal
	 *            signal
	 * @param window
	 *            window
	 * @param frameShift
	 *            frameShift
	 * @param samplingRate
	 *            samplingRate
	 */
	public ShortTermAutocorrelationAnalyser(DoubleDataSource signal, Window window, int frameShift, int samplingRate) {
		super(signal, window, frameShift, samplingRate);
		this.correlationInput = new double[2 * window.getLength()];
	}

	/**
	 * Apply this FrameBasedAnalyser to the given data.
	 * 
	 * @param frame
	 *            the data to analyse, which must be of the length prescribed by this FrameBasedAnalyser, i.e. by
	 *            {@link #getFrameLengthSamples()}.
	 * @return a double array of the same length as frame
	 * @throws IllegalArgumentException
	 *             if frame does not have the prescribed length
	 */
	public Object analyse(double[] frame) {
		if (frame.length != getFrameLengthSamples())
			throw new IllegalArgumentException("Expected frame of length " + getFrameLengthSamples() + ", got " + frame.length);
		System.arraycopy(frame, 0, correlationInput, 0, frame.length);
		Arrays.fill(correlationInput, frame.length, correlationInput.length, 0);
		return FFT.autoCorrelate(correlationInput);
	}

}
