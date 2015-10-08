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
package marytts.signalproc.process;

import java.util.Arrays;

import marytts.util.math.FFT;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

public class FrequencyDomainProcessor implements InlineDataProcessor {
	private double[] real;
	private double[] imag;
	private double amount; // A double value between 0.5 and 1.0, if 1.0 full modification, if 0.5 half modification
	private double oneMinusAmount; // 1.0-amount

	/**
	 * Create a frequencydomainprocessor with the given FFT size.
	 * 
	 * @param fftSize
	 *            length of the array to be used for the FFT. Must be a power of two.
	 * @param amount
	 *            amount
	 * @throws IllegalArgumentException
	 *             if fftSize is not a power of two.
	 */
	public FrequencyDomainProcessor(int fftSize, double amount) {
		if (!MathUtils.isPowerOfTwo(fftSize)) {
			throw new IllegalArgumentException("FFT size must be a power of two");
		}
		this.real = new double[fftSize];
		this.imag = new double[fftSize];
		this.amount = amount;
		this.oneMinusAmount = 1.0 - this.amount;
	}

	public FrequencyDomainProcessor(int fftSize) {
		this(fftSize, 1.0);
	}

	public int getFFTSize() {
		return real.length;
	}

	/**
	 * Apply this frequency domain processor to the given data, and return the processing result in-place.
	 * 
	 * @param data
	 *            the (time-domain) data to process
	 * @param pos
	 *            the position in the data array where the data lies
	 * @param len
	 *            the length of the to-be-processed data
	 * @throws IllegalArgumentException
	 *             if len is greater than the fftSize of this frequency domain processor.
	 * 
	 */
	public void applyInline(double[] data, int pos, int len) {
		int i;
		double[] dataOut = new double[len];

		if (len > real.length) {
			throw new IllegalArgumentException("Length must not be larger than FFT size");
		}
		// For correct phase, center time origin in the middle of windowed frame:
		int middle = len / 2 + len % 2; // e.g., 3 if len==5
		System.arraycopy(data, 0, dataOut, 0, len);
		System.arraycopy(dataOut, pos + middle, real, 0, len - middle);
		System.arraycopy(dataOut, pos, real, real.length - middle, middle);

		if (real.length > len)
			Arrays.fill(real, len - middle, real.length - middle, 0);
		Arrays.fill(imag, 0, imag.length, 0.);
		// Convert to polar coordinates in frequency domain
		FFT.transform(real, imag, false);
		process(real, imag);
		FFT.transform(real, imag, true);

		System.arraycopy(real, 0, dataOut, pos + middle, len - middle);
		System.arraycopy(real, real.length - middle, dataOut, pos, middle);

		double origAvgEnergy = SignalProcUtils.getAverageSampleEnergy(data, len);

		for (i = 0; i < len; i++)
			data[i] = amount * dataOut[i] + oneMinusAmount * data[i];

		double newAvgEnergy = SignalProcUtils.getAverageSampleEnergy(data, len);
		double scale = origAvgEnergy / newAvgEnergy;

		for (i = 0; i < len; i++)
			data[i] *= 0.8 * scale;
	}

	/**
	 * Here the actual processing of the frequency-domain frame (in cartesian coordinates) happens. This base implementation does
	 * nothing.
	 * 
	 * @param real
	 *            real
	 * @param imag
	 *            imag
	 */
	protected void process(double[] real, double[] imag) {
	}
}
