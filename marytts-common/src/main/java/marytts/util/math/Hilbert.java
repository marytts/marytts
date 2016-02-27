/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.util.math;

/**
 * Computes the N-point Discrete Hilbert Transform of real valued vector x: The algorithm consists of the following stages: - X(w)
 * = FFT(x) is computed - H(w), DFT of a Hilbert transform filter h[n], is created: H[0]=H[N/2]=1 H[w]=2 for w=1,2,...,N/2-1
 * H[w]=0 for w=N/2+1,...,N-1 - x[n] and h[n] are convolved (i.e. X(w) and H(w) multiplied) - y[n], the Discrete Hilbert Transform
 * of x[n] is computed by y[n]=IFFT(X(w)H(w)) for n=0,...,N-1
 * 
 * @author Oytun T&uuml;rk
 */
public class Hilbert {
	public static ComplexArray transform(double[] x) {
		return transform(x, x.length);
	}

	public static ComplexArray transform(double[] x, int N) {
		ComplexArray X = FFTMixedRadix.fftReal(x, N);
		double[] H = new double[N];

		int NOver2 = (int) Math.floor(N / 2 + 0.5);
		int w;

		H[0] = 1.0;
		H[NOver2] = 1.0;

		for (w = 1; w <= NOver2 - 1; w++)
			H[w] = 2.0;

		for (w = NOver2 + 1; w <= N - 1; w++)
			H[w] = 0.0;

		for (w = 0; w < N; w++) {
			X.real[w] *= H[w];
			X.imag[w] *= H[w];
		}

		return FFTMixedRadix.ifft(X);
	}

}
