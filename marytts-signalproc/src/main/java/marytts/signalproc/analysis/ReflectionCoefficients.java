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

import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;

/**
 * 
 * @author sacha
 * 
 *         Implements the conversion between the Linear Prediction Coefficients (LPC) and the corresponding Reflection
 *         Coefficients.
 * 
 *         24.03.2009 - Added lattice synthesis filter function (oytun.turk)
 * 
 */
public class ReflectionCoefficients {

	/**
	 * Converts from LPC coefficients to reflection coefficients.
	 * 
	 * @param oneMinusA
	 *            A(z) = a0 - sum { ai * z^-i } . a0 = 1.
	 * 
	 * @return The reflection coefficents. If the LPC order is N, there are N reflection coefficients and (N+1) LPC coefficients
	 *         (if you count a0==1).
	 * 
	 */
	public static double[] lpc2lprefc(double[] oneMinusA) {
		/*
		 * int lpcOrder = oneMinusA.length - 1; double[] k = new double[lpcOrder]; double a1, a2, ki; double[] a = (double[])
		 * oneMinusA.clone();
		 * 
		 * // For each prediction coeff for ( int i = lpcOrder; i > 0; i-- ) { // Current reflection coeff equals last prediction
		 * coeff ki = a[i]; k[i-1] = ki; // Then update the remaining predictor coeffs // Recurse between 2nd and before-last LPC
		 * coeffs, going from border to // middle position of the equation array. for( int j = 1; j <= (i>>1); j++ ) { // Note: in
		 * the above, (i>>1) is to be understood as the integer division by 2 a1 = a[j]; a2 = a[i-j]; a[j] = ( a1 + ki * a2 ) / (1
		 * - ki*ki); a[i-j] = ( a2 + ki * a1 ) / (1 - ki*ki); } }
		 * 
		 * return( k );
		 */

		int p = oneMinusA.length - 1;
		double[][] A = new double[p][];
		A[p - 1] = new double[p];
		int i, j;
		for (i = 0; i < p; i++)
			A[p - 1][i] = oneMinusA[i + 1];

		double[] k = new double[p];

		for (i = p; i >= 1; i--) {
			k[i - 1] = A[i - 1][i - 1];

			if (i >= 2) {
				A[i - 2] = new double[i];
				for (j = 1; j < i; j++)
					A[i - 2][j - 1] = (A[i - 1][j - 1] + A[i - 1][i - 1] * A[i - 1][i - j - 1]) / (1.0 - k[i - 1] * k[i - 1]);
			}
		}

		double[] oneMinusAHat = lprefc2lpc(k);

		return k;
	}

	/**
	 * Converts from the reflection coefficients to the corresponding LPC coefficients.
	 * 
	 * @param k
	 *            the vector of p reflection coefficients [k_0 ... k_p-1].
	 * 
	 * @return The LPC coefficents [1 -a_1 -a_2 ... -a_p].
	 * 
	 */
	public static double[] lprefc2lpc(double[] k) {
		/*
		 * int lpcOrder = k.length; double[] a = new double[lpcOrder+1]; double a1,a2;
		 * 
		 * // Set first LPC coefficient to 1, and never touch it again. a[0] = 1.0;
		 * 
		 * // Then recurse for each reflection coefficient for( int i = 1; i <= lpcOrder; i++ ) { // WARNING: k[0] is the first
		 * reflection coefficient, leading from // a_j^(0) to a_j^(1). There is one more a_j than the // number of k_i. There are
		 * (lpcOrder) k_i and (lpcOrder+1) a_j.
		 * 
		 * // Recurse between 2nd and before-last LPC coeffs, going from border to // middle position of the equation array. for(
		 * int j = 1; j <= (i>>1); j++ ) { // Note: in the above, (i>>1) is to be understood // as the integer division by 2. a1 =
		 * a[j]; a2 = a[i-j]; a[j] = a1 - k[i-1] * a2; a[i-j] = a2 - k[i-1] * a1; } // The last a_j equals the current reflection
		 * coeff. a[i] = k[i-1]; }
		 * 
		 * return( a );
		 */

		int p = k.length;
		double[][] A = new double[p][];

		int i, j;
		for (i = 1; i <= p; i++) {
			A[i - 1] = new double[i];
			A[i - 1][i - 1] = k[i - 1];

			for (j = 1; j < i; j++)
				A[i - 1][j - 1] = A[i - 2][j - 1] - k[i - 1] * A[i - 2][i - j - 1];
		}

		double[] oneMinusA = new double[p + 1];
		oneMinusA[0] = 1.0;
		System.arraycopy(A[p - 1], 0, oneMinusA, 1, p);

		return oneMinusA;
	}

	// Synthesize output using reflection coefficients k, input x, and zero initial conditions
	// with lattice filtering
	public static double[] latticeSynthesisFilter(double[] k, double[] x) {
		int Nx = x.length - 1;
		int M = k.length;
		double[][] fg = new double[x.length][Nx + 1];
		System.arraycopy(x, 0, fg[0], 0, x.length);
		fg[1][0] = 0.0;
		System.arraycopy(x, 0, fg[1], 1, Nx);

		double[][] KMatrix = new double[2][2];
		KMatrix[0][0] = 1.0;
		KMatrix[1][1] = 1.0;
		double[] fg2 = new double[x.length];
		for (int m = 1; m <= M; m++) {
			KMatrix[0][1] = k[m - 1];
			KMatrix[1][0] = k[m - 1];
			fg = MathUtils.matrixProduct(KMatrix, fg);
			fg2[0] = 0.0;
			System.arraycopy(fg[1], 0, fg2, 1, Nx);
			fg[1] = ArrayUtils.copy(fg2);
		}

		/*
		 * MaryUtils.plot(x); MaryUtils.plot(fg[0]);
		 * 
		 * SignalProcUtils.displayDFTSpectrumInDB(x, 1024, Window.HAMMING); SignalProcUtils.displayDFTSpectrumInDB(fg[0], 1024,
		 * Window.HAMMING);
		 */

		return fg[0];

		/*
		 * int P = k.length; int N = x.length; double[][] e = new double[P+1][N]; double[][] b = new double[P+1][N]; double[] y =
		 * new double[N];
		 * 
		 * int n, i; System.arraycopy(x, 0, e[0], 0, N); for (i=1; i<P; i++) Arrays.fill(e[i], 0.0);
		 * 
		 * for (i=0; i<P; i++) Arrays.fill(b[i], 0.0);
		 * 
		 * Arrays.fill(y, 0.0);
		 * 
		 * for (n=0; n<N; n++) { for (i=1; i<=P; i++) { e[i][n] = e[i-1][n] + k[P-i]*b[i][n]; if (n>0) b[i-1][n] =
		 * -1.0*k[P-i]*e[i][n]+b[i][n-1]; else b[i-1][n] = -1.0*k[P-i]*e[i][n]; }
		 * 
		 * y[n] = e[P][n];
		 * 
		 * if (n>0) b[P][n] = y[n-1]; else b[P][n] = 0.0; }
		 */

		/*
		 * MaryUtils.plot(x); MaryUtils.plot(y);
		 * 
		 * SignalProcUtils.displayDFTSpectrumInDB(x, 1024, Window.HAMMING); SignalProcUtils.displayDFTSpectrumInDB(y, 1024,
		 * Window.HAMMING);
		 */

		// return y;
	}
}
