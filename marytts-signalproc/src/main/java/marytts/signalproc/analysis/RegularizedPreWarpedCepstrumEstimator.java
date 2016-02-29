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

import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * 
 * Implements the regularized cepstral envelope estimation in
 * 
 * Cappe, O., Laroche, J., and Moulines E., 1995, "Regularized estimation of cepstrum envelope from discrete frequency points", in
 * IEEE ASSP Workshop on app. of sig. proc. to audio and acoust.
 * 
 * This approach is used in Harmonic plus Noise (HNM) model for pitch modification for the purpose of estimating amplitudes of
 * harmonics at (new) pitch-modified locations. (See, i.e. (Stylianou, et. al.,1995) or Stylianou´s PhD thesis for details)
 * 
 * Stylianou, Y, Laroche, J., and Moulines E., 1995, "High quality speech modification based on a Harmonic + Noise model", in
 * Proc. of the Europseech 1995.
 * 
 * Various other techniques are used by other researchers to keep the overall spectral shape unchanged under pitch sclae
 * modifications. For example, Quatieri uses SEEVOC approach (linear interpolation) to find amplitude values at modified
 * frequencies. Failing to estimate the modified amplitudes successfully will result in changes in overal spectral envelope which
 * may affect voice quality, presence, or even the identity of phones after pitch scaling.
 * 
 * @author Oytun T&uuml;rk
 */
public class RegularizedPreWarpedCepstrumEstimator extends RegularizedCepstrumEstimator {
	public static float[] freqsLinearAmps2cepstrum(double[] linearAmps, double[] freqsInHz, int samplingRateInHz, int cepsOrder,
			double[] weights, double lambda) {
		return freqsLinearAmps2cepstrum(linearAmps, freqsInHz, samplingRateInHz, cepsOrder, true, weights, lambda);
	}

	public static float[] freqsLinearAmps2cepstrum(double[] linearAmps, double[] freqsInHz, int samplingRateInHz, int cepsOrder) {
		return freqsLinearAmps2cepstrum(linearAmps, freqsInHz, samplingRateInHz, cepsOrder, true, null, DEFAULT_LAMBDA);
	}

	public static double[] cepstrum2logAmpHalfSpectrum(float[] ceps, int fftSize, int samplingRateInHz) {
		return cepstrum2logAmpHalfSpectrum(ceps, fftSize, samplingRateInHz, true);
	}

	public static double cepstrum2linearSpectrumValue(float[] ceps, double freqInHz, int samplingRateInHz) {
		return MathUtils.db2amp(cepstrum2dbSpectrumValue(ceps, freqInHz, samplingRateInHz));
	}

	public static double cepstrum2dbSpectrumValue(float[] ceps, double freqInHz, int samplingRateInHz) {
		int p = ceps.length - 1;

		double denum = (2.0 * SignalProcUtils.freq2barkNew(0.5 * samplingRateInHz));
		double f = SignalProcUtils.freq2barkNew(freqInHz) / denum;

		double val = ceps[0];
		for (int i = 1; i <= p; i++)
			val += 2.0 * ceps[i] * Math.cos(MathUtils.TWOPI * f * i);

		return val;
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
		return precomputeM(freqsInHz, samplingRateInHz, cepsOrder, true);
	}

	public static double[] spectralEnvelopeLinear(double[] linearAmps, double[] freqsInHz, int samplingRateInHz, int cepsOrder) {
		return spectralEnvelopeLinear(linearAmps, freqsInHz, samplingRateInHz, cepsOrder,
				SignalProcUtils.getDFTSize(samplingRateInHz));
	}

	public static double[] spectralEnvelopeLinear(double[] linearAmps, double[] freqsInHz, int samplingRateInHz, int cepsOrder,
			int fftSize) {
		return MathUtils.db2amp(spectralEnvelopeDB(linearAmps, freqsInHz, samplingRateInHz, cepsOrder, fftSize));
	}

	public static double[] spectralEnvelopeDB(double[] linearAmps, double[] freqsInHz, int samplingRateInHz, int cepsOrder,
			int fftSize) {
		float[] ceps = freqsLinearAmps2cepstrum(linearAmps, freqsInHz, samplingRateInHz, cepsOrder);

		return cepstrum2logAmpHalfSpectrum(ceps, fftSize, samplingRateInHz);
	}
}
