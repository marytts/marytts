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

/**
 * 
 * @author sacha
 * 
 *         Implements the conversion between the Linear Prediction Coefficients (LPC) and the corresponding Linear Prediction
 *         Cepstrum Coefficients.
 * 
 */
public class CepstrumLPCAnalyser {

	/**
	 * Converts from LPC coefficients to LPCC coefficients.
	 * 
	 * @param oneMinusA
	 *            A(z) = a0 - sum { ai * z^-i } . a0 = 1.
	 * @param gain
	 *            The LPC gain factor.
	 * @param cepstrumOrder
	 *            Cepstrum order (equal to the index of the last cepstrum coefficient).
	 * 
	 * @return The LPCC coefficents. c[0] is set to log(gain).
	 * 
	 */
	public static double[] lpc2lpcc(double[] oneMinusA, double gain, int cepstrumOrder) {

		// Check the cepstrum order
		if (cepstrumOrder <= 0) {
			throw new RuntimeException("The cepstrum order [" + cepstrumOrder + "] must be a positive integer.");
		}

		// Declare misc. useful variables
		int k, m;
		double acc;
		double[] c = new double[cepstrumOrder + 1];
		int lpcOrder = oneMinusA.length - 1;

		/* First cepstral coeff */
		c[0] = Math.log(gain);
		/* Other coeffs: */
		for (m = 1; m <= cepstrumOrder; m++) {
			/* Coeffs within the LPC order: */
			if (m <= lpcOrder) {
				acc = 0.0;
				for (k = 1; k < m; k++)
					acc += ((double) (m - k) * oneMinusA[k] * c[m - k]);
				c[m] = acc / (double) (m) + oneMinusA[m];
			}
			/* Coeffs above the LPC order: */
			else {
				acc = 0.0;
				for (k = 1; k <= lpcOrder; k++)
					acc += ((double) (m - k) * oneMinusA[k] * c[m - k]);
				c[m] = acc / (double) (m);
			}
		}

		return (c);
	}

	/**
	 * Converts from LPCC coefficients to LPC coefficients.
	 * 
	 * @param c
	 *            the vector of cepstral coefficients. Note: c[0] = log(gain).
	 * @param lpcOrder
	 *            The original LPC order (equal to the index of the last LPC coefficient).
	 * 
	 * @return The LPC coefficents [1 -a_1 -a_2 ... -a_p].
	 * 
	 *         The gain is not returned, but it can be recovered as exp(c[0]).
	 * 
	 */
	public static double[] lpcc2lpc(double[] c, int lpcOrder) {

		// Check the LPC order
		if (lpcOrder <= 0) {
			throw new RuntimeException("The LPC order [" + lpcOrder + "] must be a positive integer.");
		}

		// Declare misc. useful variables
		int k, m;
		double acc;
		double[] a = new double[lpcOrder + 1];
		int cepstrumOrder = c.length - 1;

		/* First lpc coeff */
		a[0] = 1.0;
		/* Other coeffs: */
		for (m = 1; m <= lpcOrder; m++) {
			/* Coeffs within the Cepstrum order: */
			if (m <= cepstrumOrder) {
				acc = 0.0;
				for (k = 1; k < m; k++)
					acc += ((double) (k) * a[m - k] * c[k]);
				a[m] = c[m] - acc / (double) (m);
			}
			/* Coeffs above the Cepstrum order: */
			else {
				acc = 0.0;
				for (k = 1; k <= cepstrumOrder; k++)
					acc += ((double) (k) * a[m - k] * c[k]);
				a[m] = -acc / (double) (m);
			}
		}

		return (a);
	}

}
