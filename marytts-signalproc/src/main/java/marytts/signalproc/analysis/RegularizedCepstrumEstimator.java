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
package marytts.signalproc.analysis;

import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

//Note that RegularizedPreWarpedCepstrumEstimator or RegularizedPostWarpedCepstrumEstimator works much better
public class RegularizedCepstrumEstimator {
	public static final double DEFAULT_LAMBDA = 5e-4; // Regularization parameter

	// Warping for better estimation can be performed in two ways:
	// either pre-warping of linear frequency values in Bark-scale
	// or post-warping by converting cepstral coefficients computed from linear frequency values to mel scale
	public static final int REGULARIZED_CEPSTRUM_WITH_PRE_BARK_WARPING = 1;
	public static final int REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING = 2;

	//

	// lambda: regularization term (typically on the order of 0.0001
	// Note that cepstrum is always computed using log amps, therefore the fitted spectrum computed from these cepstrum coeffs
	// will be in log amp domain
	protected static float[] freqsLinearAmps2cepstrum(double[] linearAmps, double[] freqsInHz, int samplingRateInHz,
			int cepsOrder, boolean isPreWarping, double[] weights, double lambda) {
		assert linearAmps.length == freqsInHz.length;

		double[] a = MathUtils.amp2db(linearAmps);
		int L = linearAmps.length;
		int p = cepsOrder;
		double[][] M = new double[L][p + 1];
		int i, j;
		double f;
		double denum = 1.0;

		if (isPreWarping)
			denum = (2.0 * SignalProcUtils.freq2barkNew(0.5 * samplingRateInHz));

		for (i = 0; i < L; i++) {
			M[i][0] = 1.0;
			if (isPreWarping) {
				f = SignalProcUtils.freq2barkNew(freqsInHz[i]) / denum;
				for (j = 1; j < p + 1; j++)
					M[i][j] = 2.0 * Math.cos(MathUtils.TWOPI * f * j);
			} else {
				f = SignalProcUtils.hz2radian(freqsInHz[i], samplingRateInHz);
				for (j = 1; j < p + 1; j++)
					M[i][j] = 2.0 * Math.cos(f * j);
			}
		}

		double[] diagR = new double[p + 1];
		double tmp = 8.0 * (Math.PI) * (Math.PI);
		for (i = 0; i < p + 1; i++)
			diagR[i] = tmp * i * i;
		double[][] R = MathUtils.toDiagonalMatrix(diagR);

		double[] cepsDouble = null;
		if (weights != null) {
			double[][] W = MathUtils.toDiagonalMatrix(weights);
			double[][] MTrans = MathUtils.transpoze(M);
			double[][] MTransW = MathUtils.matrixProduct(MTrans, W);
			double[][] MTransWM = MathUtils.matrixProduct(MTransW, M);
			double[][] lambdaR = MathUtils.multiply(lambda, R);
			double[] MTransWa = MathUtils.matrixProduct(MTransW, a);
			double[][] inverted = MathUtils.inverse(MathUtils.add(MTransWM, lambdaR));
			cepsDouble = MathUtils.matrixProduct(inverted, MTransWa);
		} else // No weights given
		{
			double[][] MTrans = MathUtils.transpoze(M);
			double[][] MTransM = MathUtils.matrixProduct(MTrans, M);
			double[][] lambdaR = MathUtils.multiply(lambda, R);
			double[] MTransa = MathUtils.matrixProduct(MTrans, a);
			double[][] inverted = MathUtils.inverse(MathUtils.add(MTransM, lambdaR));
			cepsDouble = MathUtils.matrixProduct(inverted, MTransa);
		}

		float[] ceps = ArrayUtils.copyDouble2Float(cepsDouble);

		return ceps;
	}

	protected static double[][] precomputeM(double[] freqsInHz, int samplingRateInHz, int cepsOrder, boolean isPreWarping) {
		int L = freqsInHz.length;
		int p = cepsOrder;
		double[][] M = new double[L][p + 1];
		int i, j;
		double f;
		double denum = 1.0;

		if (isPreWarping)
			denum = (2.0 * SignalProcUtils.freq2barkNew(0.5 * samplingRateInHz));

		for (i = 0; i < L; i++) {
			M[i][0] = 1.0;
			if (isPreWarping) {
				f = SignalProcUtils.freq2barkNew(freqsInHz[i]) / denum;
				for (j = 1; j < p + 1; j++)
					M[i][j] = 2.0 * Math.cos(MathUtils.TWOPI * f * j);
			} else {
				f = SignalProcUtils.hz2radian(freqsInHz[i], samplingRateInHz);
				for (j = 1; j < p + 1; j++)
					M[i][j] = 2.0 * Math.cos(f * j);
			}
		}

		return M;
	}

	public static double[][] precomputeMTransW(double[][] M, double[] weights) {
		double[][] MTransW = null;
		if (weights != null) {
			double[][] W = MathUtils.toDiagonalMatrix(weights);
			double[][] MTrans = MathUtils.transpoze(M);
			MTransW = MathUtils.matrixProduct(MTrans, W);
		} else
			// No weights given
			MTransW = MathUtils.transpoze(M);

		return MTransW;
	}

	public static double[][] precomputeMTransWM(double[][] MTransW, double[][] M) {
		double[][] MTransWM = MathUtils.matrixProduct(MTransW, M);

		return MTransWM;
	}

	public static double[][] precomputeLambdaR(double lambda, int cepsOrder) {
		int p = cepsOrder;
		double[] diagR = new double[p + 1];
		double tmp = 8.0 * (Math.PI) * (Math.PI);
		for (int i = 0; i < p + 1; i++)
			diagR[i] = tmp * i * i;
		double[][] R = MathUtils.toDiagonalMatrix(diagR);

		double[][] lambdaR = MathUtils.multiply(lambda, R);

		return lambdaR;
	}

	public static double[][] precomputeInverted(double[][] MTransWM, double[][] lambdaR) {
		double[][] inverted = MathUtils.inverse(MathUtils.add(MTransWM, lambdaR));

		return inverted;
	}

	// Another version when frequencies are fixed and all precomputations were done by calling precomputeForCepstrum with these
	// fixed values
	// Note that cepstrum is always computed using log amps, therefore the fitted spectrum computed from these cepstrum coeffs
	// will be in log amp domain
	public static float[] freqsLinearAmps2cepstrum(double[] linearAmps, double[][] MTransW, double[][] inverted) {
		double[] logAmps = MathUtils.log10(linearAmps);
		double[] a = MathUtils.multiply(logAmps, 20.0);

		double[] cepsDouble = null;

		double[] MTransWa = MathUtils.matrixProduct(MTransW, a);
		cepsDouble = MathUtils.matrixProduct(inverted, MTransWa);

		float[] ceps = ArrayUtils.copyDouble2Float(cepsDouble);

		return ceps;
	}

	protected static double[] cepstrum2logAmpHalfSpectrum(float[] ceps, int fftSize, int samplingRateInHz, boolean isPreWarping) {
		int maxFreq = SignalProcUtils.halfSpectrumSize(fftSize);
		double[] halfAbsSpectrum = new double[maxFreq];
		int p = ceps.length - 1;
		int i, k;

		double f;
		double denum = samplingRateInHz;

		if (isPreWarping)
			denum = (2.0 * SignalProcUtils.freq2barkNew(0.5 * samplingRateInHz));

		for (k = 0; k < maxFreq; k++) {
			halfAbsSpectrum[k] = ceps[0];
			if (isPreWarping) {
				f = SignalProcUtils.freq2barkNew(((double) k / (maxFreq - 1.0)) * 0.5 * samplingRateInHz) / denum;
				for (i = 1; i <= p; i++)
					halfAbsSpectrum[k] += 2.0 * ceps[i] * Math.cos(MathUtils.TWOPI * f * i);
			} else {
				f = SignalProcUtils.index2freq(k, samplingRateInHz, maxFreq - 1);
				f = SignalProcUtils.hz2radian(f, samplingRateInHz);
				for (i = 1; i <= p; i++)
					halfAbsSpectrum[k] += 2.0 * ceps[i] * Math.cos(f * i);
			}
		}

		return halfAbsSpectrum;
	}
}
