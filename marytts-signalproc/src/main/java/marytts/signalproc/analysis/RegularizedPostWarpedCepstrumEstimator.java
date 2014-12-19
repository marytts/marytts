/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package marytts.signalproc.analysis;

import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * Implements a cepstral envelope estimation algorithm for fitting an envelope to discrete frequency amplitudes.
 * 
 * Reference: W. D'haes, X. Rodet: Discrete Cepstrum Coefficients as Perceptual Features, International Computer Music Conference
 * (ICMC), Singapore, 2003.
 * 
 * @author oytun.turk
 * 
 */
public class RegularizedPostWarpedCepstrumEstimator extends RegularizedCepstrumEstimator {
	public static int getAutoCepsOrderPre(int numSpectralLines) {
		if (numSpectralLines <= 0)
			return 40;
		else
			return numSpectralLines * 2;
	}

	public static float[] freqsLinearAmps2cepstrum(double[] linearAmps, double[] freqsInHz, int samplingRateInHz,
			int cepsOrderPre, int cepsOrder) {
		return freqsLinearAmps2cepstrum(linearAmps, freqsInHz, samplingRateInHz, cepsOrderPre, cepsOrder, null);
	}

	public static float[] freqsLinearAmps2cepstrum(double[] linearAmps, double[] freqsInHz, int samplingRateInHz,
			int cepsOrderPre, int cepsOrder, double[] weights) {
		return freqsLinearAmps2cepstrum(linearAmps, freqsInHz, samplingRateInHz, cepsOrderPre, cepsOrder, weights, DEFAULT_LAMBDA);
	}

	// Post warping as described in W. D'Haes and X. Rodet, "Discrete Cepstrum Coefficients as Perceptual Features"
	public static float[] freqsLinearAmps2cepstrum(double[] linearAmps, double[] freqsInHz, int samplingRateInHz,
			int cepsOrderPre, int cepsOrder, double[] weights, double lambda) {
		if (cepsOrderPre < 1)
			cepsOrderPre = getAutoCepsOrderPre(linearAmps.length);

		float[] ceps = freqsLinearAmps2cepstrum(linearAmps, freqsInHz, samplingRateInHz, cepsOrderPre, false, weights, lambda);

		// Post warping as described in W. D'Haes and X. Rodet, "Discrete Cepstrum Coefficients as Perceptual Features"
		double[][] A = new double[cepsOrder][cepsOrderPre];

		int k, l, n;
		double w;
		int N = cepsOrderPre;
		for (k = 0; k < cepsOrder; k++) {
			for (l = 0; l < cepsOrderPre; l++) {
				A[k][l] = 0.0;
				for (n = 0; n < N; n++) {
					w = SignalProcUtils.mel2radian((Math.PI * n) / N, samplingRateInHz);
					A[k][l] += Math.cos(l * w) * Math.cos((Math.PI * n * k) / N);
				}

				if (l == 0)
					A[k][l] *= 1.0 / N;
				else
					A[k][l] *= 2.0 / N;
			}
		}

		double[] cepsDouble = MathUtils.matrixProduct(A, ceps);
		ceps = ArrayUtils.copyDouble2Float(cepsDouble);

		return ceps;
	}

	public static float[] freqsLinearAmps2cepstrum(double[] linearAmps, double[] freqsInHz, int samplingRateInHz, int cepsOrder,
			double[] weights, double lambda) {
		return freqsLinearAmps2cepstrum(linearAmps, freqsInHz, samplingRateInHz, cepsOrder, false, weights, lambda);
	}

	public static double[] spectralEnvelopeDB(double[] linearAmps, double[] freqsInHz, int samplingRateInHz, int cepsOrder,
			int fftSize) {
		float[] ceps = freqsLinearAmps2cepstrum(linearAmps, freqsInHz, samplingRateInHz, cepsOrder);

		return cepstrum2logAmpHalfSpectrum(ceps, fftSize, samplingRateInHz);
	}

	public static float[] freqsLinearAmps2cepstrum(double[] linearAmps, double[] freqsInHz, int samplingRateInHz, int cepsOrder) {
		return freqsLinearAmps2cepstrum(linearAmps, freqsInHz, samplingRateInHz, cepsOrder, false, null, DEFAULT_LAMBDA);
	}

	public static double[] cepstrum2logAmpHalfSpectrum(float[] ceps, int fftSize, int samplingRateInHz) {
		return cepstrum2logAmpHalfSpectrum(ceps, fftSize, samplingRateInHz, false);
	}

	public static double[] cepstrum2linearSpectrumValues(float[] ceps, int maxFreqIndex, int samplingRateInHz) {
		double[] freqsInHz = new double[maxFreqIndex];
		for (int i = 0; i <= maxFreqIndex; i++)
			freqsInHz[i] = SignalProcUtils.index2freq(i, samplingRateInHz, maxFreqIndex);

		return cepstrum2linearSpectrumValues(ceps, freqsInHz, samplingRateInHz);
	}

	public static double[] cepstrum2linearSpectrumValues(float[] ceps, double[] freqsInHz, int samplingRateInHz) {
		return MathUtils.db2amp(cepstrum2dbSpectrumValues(ceps, freqsInHz, samplingRateInHz));
	}

	public static double[] cepstrum2dbSpectrumValues(float[] ceps, int maxFreqIndex, int samplingRateInHz) {
		double[] freqsInHz = new double[maxFreqIndex + 1];
		for (int i = 0; i <= maxFreqIndex; i++)
			freqsInHz[i] = SignalProcUtils.index2freq(i, samplingRateInHz, maxFreqIndex);

		return cepstrum2dbSpectrumValues(ceps, freqsInHz, samplingRateInHz);
	}

	public static double[] cepstrum2dbSpectrumValues(float[] ceps, double[] freqsInHz, int samplingRateInHz) {
		double[] vals = new double[freqsInHz.length];
		for (int i = 0; i < freqsInHz.length; i++)
			vals[i] = cepstrum2dbSpectrumValue(ceps, freqsInHz[i], samplingRateInHz);

		return vals;
	}

	public static double[][] precomputeM(double[] freqsInHz, int samplingRateInHz, int cepsOrder) {
		return precomputeM(freqsInHz, samplingRateInHz, cepsOrder, false);
	}

	public static double cepstrum2dbSpectrumValue(float[] ceps, double freqInHz, int samplingRateInHz) {
		int p = ceps.length - 1;

		double w = SignalProcUtils.hz2mel(freqInHz, samplingRateInHz);

		double val = ceps[0];
		for (int i = 1; i <= p; i++)
			val += 2.0 * ceps[i] * Math.cos(w * i);

		return val;
	}

	public static double[] spectralEnvelopeLinear(double[] linearAmps, double[] freqsInHz, int samplingRateInHz,
			int cepsOrderPre, int cepsOrder) {
		return spectralEnvelopeLinear(linearAmps, freqsInHz, samplingRateInHz, cepsOrderPre, cepsOrder,
				SignalProcUtils.getDFTSize(samplingRateInHz));
	}

	public static double[] spectralEnvelopeLinear(double[] linearAmps, double[] freqsInHz, int samplingRateInHz,
			int cepsOrderPre, int cepsOrder, int fftSize) {
		return MathUtils.db2amp(spectralEnvelopeDB(linearAmps, freqsInHz, samplingRateInHz, cepsOrderPre, cepsOrder, fftSize));
	}

	public static double[] spectralEnvelopeDB(double[] linearAmps, double[] freqsInHz, int samplingRateInHz, int cepsOrderPre,
			int cepsOrder, int fftSize) {
		float[] ceps = freqsLinearAmps2cepstrum(linearAmps, freqsInHz, samplingRateInHz, cepsOrderPre, cepsOrder);

		return cepstrum2logAmpHalfSpectrum(ceps, fftSize, samplingRateInHz);
	}

	public static double cepstrum2linearSpectrumValue(float[] ceps, double freqInHz, int samplingRateInHz) {
		return MathUtils.db2amp(cepstrum2dbSpectrumValue(ceps, freqInHz, samplingRateInHz));
	}
}
