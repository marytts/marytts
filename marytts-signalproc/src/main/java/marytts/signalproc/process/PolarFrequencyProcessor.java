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

import marytts.util.math.MathUtils;

/**
 * @author Marc Schr&ouml;der
 * 
 */
public class PolarFrequencyProcessor extends FrequencyDomainProcessor {

	/**
	 * @param fftSize
	 *            fftsize
	 * @param amount
	 *            amount
	 */
	public PolarFrequencyProcessor(int fftSize, double amount) {
		super(fftSize, amount);
		// super(fftSize);
	}

	public PolarFrequencyProcessor(int fftSize) {
		this(fftSize, 1.0);
	}

	/**
	 * Here the actual processing of the frequency-domain frame (in cartesian coordinates) happens. This implementation converts
	 * to polar coordinates calls processPolar(), and converts the result back to cartesian coordinates.
	 * 
	 * @param real
	 *            real
	 * @param imag
	 *            imag
	 */
	protected final void process(double[] real, double[] imag) {
		MathUtils.toPolarCoordinates(real, imag);
		// for readability:
		double[] r = real;
		double[] phi = imag;
		// Now do something meaningful with the fourier transform
		processPolar(r, phi);
		// Convert back:
		MathUtils.toCartesianCoordinates(real, imag);
	}

	/**
	 * Here the actual processing of the frequency-domain frame (in polar coordinates) happens. This base implementation does
	 * nothing.
	 * 
	 * @param r
	 *            r
	 * @param phi
	 *            phi
	 */
	protected void processPolar(double[] r, double[] phi) {
	}
}
