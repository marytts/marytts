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
package marytts.util.math;

import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.util.data.audio.MaryAudioUtils;

/**
 * @author Marc Schr&ouml;der
 * 
 */
public class FFT {
	static double[] cosDelta;
	static double[] sinDelta;

	static {
		int N = 32;
		cosDelta = new double[N];
		sinDelta = new double[N];
		for (int i = 1; i < N; i++) {
			double delta = -MathUtils.TWOPI / (1 << i);
			cosDelta[i] = Math.cos(delta);
			sinDelta[i] = Math.sin(delta);
		}
	}

	/**
	 * Convenience method for computing the log (dB) power spectrum of a real signal. The signal can be of any length; internally,
	 * zeroes will be added if signal length is not a power of two.
	 * 
	 * @param signal
	 *            the real signal for which to compute the power spectrum.
	 * @return the power spectrum, as an array of length N/2 (where N is the power of two greater than or equal to signal.length):
	 *         the log of the squared absolute values of the lower half of the complex fourier transform array.
	 */
	public static double[] computeLogPowerSpectrum(final double[] signal) {
		double[] spectrum = computePowerSpectrum(signal);
		for (int i = 0; i < spectrum.length; i++) {
			spectrum[i] = MathUtils.db(spectrum[i]);
		}
		return spectrum;
	}

	/**
	 * From the result of the FFT, compute the log (dB) power for each positive frequency.
	 * 
	 * @param fft
	 *            the array of real and imag parts of the complex number array, fft[0] = real[0], fft[1] = real[N/2], fft[2*i] =
	 *            real[i], fft[2*i+1] = imag[i] for 1&le;i&lt;N/2
	 * @return an array of length real.length/2 containing numbers representing the log of the square of the absolute value of
	 *         each complex number, p[i] = real[i]*real[i] + imag[i]*imag[i]
	 */
	public static double[] computeLogPowerSpectrum_FD(final double[] fft) {
		double[] spectrum = computePowerSpectrum_FD(fft);
		for (int i = 0; i < spectrum.length; i++) {
			spectrum[i] = MathUtils.db(spectrum[i]);
		}
		return spectrum;
	}

	/**
	 * Convenience method for computing the absolute power spectrum of a real signal. The signal can be of any length; internally,
	 * zeroes will be added if signal length is not a power of two.
	 * 
	 * @param signal
	 *            the real signal for which to compute the power spectrum.
	 * @return the power spectrum, as an array of length N/2 (where N is the power of two greater than or equal to signal.length):
	 *         the squared absolute values of the lower half of the complex fourier transform array.
	 */
	public static double[] computePowerSpectrum(final double[] signal) {
		if (signal == null)
			throw new NullPointerException("Received null argument");
		int N = signal.length;
		if (!MathUtils.isPowerOfTwo(N)) {
			N = MathUtils.closestPowerOfTwoAbove(N);
		}
		double[] real = new double[N];
		System.arraycopy(signal, 0, real, 0, signal.length);
		realTransform(real, false);
		return computePowerSpectrum_FD(real);
	}

	/**
	 * From the result of the FFT (in the frequency domain), compute the power for each positive frequency.
	 * 
	 * @param fft
	 *            the array of real and imag parts of the complex number array, fft[0] = real[0], fft[1] = real[N/2], fft[2*i] =
	 *            real[i], fft[2*i+1] = imag[i] for 1&le;i&lt;N/2
	 * @return an array of length real.length/2 containing numbers representing the square of the absolute value of each complex
	 *         number, p[i] = real[i]*real[i] + imag[i]*imag[i]
	 */
	public static double[] computePowerSpectrum_FD(final double[] fft) {
		if (fft == null)
			throw new NullPointerException("Received null argument");
		int halfN = fft.length / 2;
		double[] freqs = new double[halfN];
		freqs[0] = fft[0] * fft[0]; // and ignore fft[1], which is actually real[halfN].
		for (int i = 2; i < fft.length; i += 2) {
			freqs[i / 2] = fft[i] * fft[i] + fft[i + 1] * fft[i + 1];
		}
		return freqs;
	}

	/**
	 * Convenience method for computing the log amplitude spectrum of a real signal. The signal can be of any length; internally,
	 * zeroes will be added if signal length is not a power of two.
	 * 
	 * @param signal
	 *            the real signal for which to compute the power spectrum.
	 * @return the log amplitude spectrum, as an array of length N/2 (where N is the power of two greater than or equal to
	 *         signal.length): the log of the absolute values of the lower half of the complex fourier transform array.
	 */
	public static double[] computeLogAmplitudeSpectrum(final double[] signal) {
		double[] spectrum = computeAmplitudeSpectrum(signal);
		for (int i = 0; i < spectrum.length; i++) {
			spectrum[i] = Math.log(spectrum[i]);
		}
		return spectrum;
	}

	/**
	 * From the result of the FFT (in the frequency domain), compute the log amplitude for each positive frequency.
	 * 
	 * @param fft
	 *            the array of real and imag parts of the complex number array, fft[0] = real[0], fft[1] = real[N/2], fft[2*i] =
	 *            real[i], fft[2*i+1] = imag[i] for 1&le;i&lt;N/2
	 * @return an array of length real.length/2 containing numbers representing the log of the square of the absolute value of
	 *         each complex number, p[i] = real[i]*real[i] + imag[i]*imag[i]
	 */
	public static double[] computeLogAmplitudeSpectrum_FD(final double[] fft) {
		double[] spectrum = computeAmplitudeSpectrum_FD(fft);
		for (int i = 0; i < spectrum.length; i++) {
			spectrum[i] = Math.log(spectrum[i]);
		}
		return spectrum;
	}

	/**
	 * Convenience method for computing the absolute amplitude spectrum of a real signal. The signal can be of any length;
	 * internally, zeroes will be added if signal length is not a power of two.
	 * 
	 * @param signal
	 *            the real signal for which to compute the power spectrum.
	 * @return the power spectrum, as an array of length N/2 (where N is the power of two greater than or equal to signal.length):
	 *         the absolute values of the lower half of the complex fourier transform array.
	 */
	public static double[] computeAmplitudeSpectrum(final double[] signal) {
		if (signal == null)
			throw new NullPointerException("Received null argument");
		int N = signal.length;
		if (!MathUtils.isPowerOfTwo(N)) {
			N = MathUtils.closestPowerOfTwoAbove(N);
		}
		double[] real = new double[N];
		System.arraycopy(signal, 0, real, 0, signal.length);
		realTransform(real, false);
		return computeAmplitudeSpectrum_FD(real);
	}

	/**
	 * From the result of the FFT (in the frequency domain), compute the absolute value for each positive frequency, i.e. the norm
	 * of each complex number in the lower half of the array
	 * 
	 * @param fft
	 *            the array of real and imag parts of the complex number array, fft[0] = real[0], fft[1] = real[N/2], fft[2*i] =
	 *            real[i], fft[2*i+1] = imag[i] for 1&le;i&lt;N/2
	 * @return an array of length real.length/2 containing numbers representing the absolute value of each complex number, r[i] =
	 *         sqrt(real[i]*real[i] + imag[i]*imag[i])
	 */
	public static double[] computeAmplitudeSpectrum_FD(final double[] fft) {
		if (fft == null)
			throw new NullPointerException("Received null argument");
		int halfN = fft.length / 2;
		double[] freqs = new double[halfN];
		freqs[0] = fft[0]; // and ignore fft[1], which is actually real[halfN].
		for (int i = 2; i < fft.length; i += 2) {
			freqs[i / 2] = Math.sqrt(fft[i] * fft[i] + fft[i + 1] * fft[i + 1]);
		}
		return freqs;
	}

	/**
	 * From the result of the FFT (in the frequency domain), compute the phase spectrum for each positive frequency.
	 * 
	 * @param fft
	 *            the array of real and imag parts of the complex number array, fft[0] = real[0], fft[1] = real[N/2], fft[2*i] =
	 *            real[i], fft[2*i+1] = imag[i] for 1&le;i&lt;N/2
	 * @return an array of length real.length/2 containing numbers representing the phases of each complex number, phase[i] =
	 *         atan(imag[i], real[i])
	 */
	public static double[] computePhaseSpectrum_FD(final double[] fft) {
		if (fft == null)
			throw new NullPointerException("Received null argument");
		double[] phases = new double[fft.length / 2];
		phases[0] = Math.atan2(0, fft[0]); // and ignore fft[1], which is actually real[halfN].
		for (int i = 2; i < fft.length; i += 2) {
			phases[i / 2] = Math.atan2(fft[i + 1], fft[i]);
		}
		return phases;
	}

	/**
	 * Carry out the FFT or inverse FFT, and return the result in the same arrays given as parameters. In the case of the
	 * "forward" FFT, real is the signal to transform, and imag is an empty array. After the call, real will hold the real part of
	 * the complex frequency array, and imag will hold the imaginary part. They are ordered such that first come positive
	 * frequencies from 0 to fmax, then the negative frequencies from -fmax to 0 (which are the mirror image of the positive
	 * frequencies). In the case of the inverse FFT, real and imag are in input the real and imaginary part of the complex
	 * frequencies, and in output, real is the signal. The method already computes the division by array length required for the
	 * inverse transform.
	 * 
	 * @param real
	 *            in "forward" FFT: as input=the time-domain signal to transform, as output=the real part of the complex
	 *            frequencies; in inverse FFT: as input=the real part of the complex frequencies, as output= the time-domain
	 *            signal.
	 * @param imag
	 *            in "forward" FFT: as input=an empty array, as output=the imaginary part of the complex frequencies; in inverse
	 *            FFT: as input=the imaginary part of the complex frequencies, as output= not used.
	 * @param inverse
	 *            whether to calculate the FFT or the inverse FFT.
	 */
	public static void transform(double[] real, double[] imag, boolean inverse) {
		if (real == null || imag == null)
			throw new NullPointerException("Received null argument");
		if (real.length != imag.length)
			throw new IllegalArgumentException("Arrays must be equal length");
		int N = real.length;
		assert MathUtils.isPowerOfTwo(N);
		int halfN = N / 2;
		// Re-order arrays for FFT via bit-inversion
		int iReverse = 0;
		for (int i = 0; i < N; i++) {
			if (i > iReverse) {
				// System.err.println("Swapping " + Integer.toBinaryString(i) + " with " + Integer.toBinaryString(iReverse));
				double tmpReal = real[i];
				double tmpImag = imag[i];
				real[i] = real[iReverse];
				imag[i] = imag[iReverse];
				real[iReverse] = tmpReal;
				imag[iReverse] = tmpImag;
			}
			// Calculate iReverse for next round:
			int b = halfN;
			while (b >= 1 && iReverse >= b) {
				iReverse -= b;
				b >>= 1;
			}
			iReverse += b;
		}

		// Now real and imag are in the right order for the FFT.
		// FFT:
		// Look at blocks of increasing length blockLength;
		// in each block, pair the nth and the nPrime-th = (n+blockLength/2)th
		// element, and combine them using a factor w = exp(n*delta*I),
		// delta = (-) 2*PI/blockLength.
		for (int blockLength = 2, powerOfTwo = 1; blockLength <= N; blockLength <<= 1, powerOfTwo++) {
			double wStepReal = cosDelta[powerOfTwo];
			double wStepImag = sinDelta[powerOfTwo];
			if (inverse)
				wStepImag = -wStepImag;
			double wReal = 1;
			double wImag = 0;
			int halfBlockLength = blockLength / 2;
			for (int n = 0; n < halfBlockLength; n++) {
				// Do this for all blocks at once:
				for (int i = n; i < N; i += blockLength) {
					int j = i + halfBlockLength;
					// And now combine the ith and the jth element,
					// according to s(n)=s_even(n)+ w_n*s_odd(n)
					// where w_n = exp(-2*PI*I*n/blockLength)
					// s[i] = s[i] + w*s[j]
					// w_j = w_(i+halfBlockLength) = w_i*exp(-PI*I) = -w_i
					// => s[j] = s[i] - w*s[j]
					double tmpReal = wReal * real[j] - wImag * imag[j];
					double tmpImag = wReal * imag[j] + wImag * real[j];
					real[j] = real[i] - tmpReal;
					imag[j] = imag[i] - tmpImag;
					real[i] += tmpReal;
					imag[i] += tmpImag;
				}
				// Next w is computed by complex multiplication with wStep
				// exp(i*(phi+delta)) = exp(i*phi)*exp(i*delta)
				double oldWReal = wReal;
				wReal = oldWReal * wStepReal - wImag * wStepImag;
				wImag = oldWReal * wStepImag + wImag * wStepReal;
			}
		}
		// For the inverse transform, scale down the resulting
		// signal by a factor of 1/N:
		if (inverse) {
			for (int i = 0; i < N; i++) {
				real[i] /= N;
				imag[i] /= N;
			}
		}
	}

	/**
	 * Carry out the FFT or inverse FFT, and return the result in the same arrays given as parameters. This works exactly like
	 * #transform(real, imag, boolean), but data is represented differently: the even indices of the input array hold the real
	 * part, the odd indices the imag part of each complex number.
	 * 
	 * @param realAndImag
	 *            the array of complex numbers to transform
	 * @param inverse
	 *            whether to calculate the FFT or the inverse FFT.
	 */
	public static void transform(double[] realAndImag, boolean inverse) {
		if (realAndImag == null)
			throw new NullPointerException("Received null argument");
		int N = realAndImag.length >> 1;
		assert MathUtils.isPowerOfTwo(N);
		int halfN = N >> 1;
		// Re-order arrays for FFT via bit-inversion
		int iReverse = 0;
		for (int i = 0; i < N; i++) {
			if (i > iReverse) {
				// System.err.println("Swapping " + Integer.toBinaryString(i) + " with " + Integer.toBinaryString(iReverse));
				int twoi = i << 1;
				int twoi1 = twoi + 1;
				int twoirev = iReverse << 1;
				int twoirev1 = twoirev + 1;
				double tmpReal = realAndImag[twoi];
				double tmpImag = realAndImag[twoi1];
				realAndImag[twoi] = realAndImag[twoirev];
				realAndImag[twoi1] = realAndImag[twoirev1];
				realAndImag[twoirev] = tmpReal;
				realAndImag[twoirev1] = tmpImag;
			}
			// Calculate iReverse for next round:
			int b = halfN;
			while (b >= 1 && iReverse >= b) {
				iReverse -= b;
				b >>= 1;
			}
			iReverse += b;
		}

		// Now real and imag are in the right order for the FFT.
		// FFT:
		// Look at blocks of increasing length blockLength;
		// in each block, pair the nth and the nPrime-th = (n+blockLength/2)th
		// element, and combine them using a factor w = exp(n*delta*I),
		// delta = (-) 2*PI/blockLength.
		for (int blockLength = 2, powerOfTwo = 1; blockLength <= N; blockLength <<= 1, powerOfTwo++) {
			double wStepReal = cosDelta[powerOfTwo];
			double wStepImag = sinDelta[powerOfTwo];
			if (inverse)
				wStepImag = -wStepImag;
			double wReal = 1;
			double wImag = 0;
			int halfBlockLength = blockLength >> 1;
			for (int n = 0; n < halfBlockLength; n++) {
				// Do this for all blocks at once:
				for (int i = n; i < N; i += blockLength) {
					int j = i + halfBlockLength;
					// And now combine the ith and the jth element,
					// according to s(n)=s_even(n)+ w_n*s_odd(n)
					// where w_n = exp(-2*PI*I*n/blockLength)
					// s[i] = s[i] + w*s[j]
					// w_j = w_(i+halfBlockLength) = w_i*exp(-PI*I) = -w_i
					// => s[j] = s[i] - w*s[j]
					int twoi = i << 1;
					int twoi1 = twoi + 1;
					int twoj = j << 1;
					int twoj1 = twoj + 1;
					double tmpReal = wReal * realAndImag[twoj] - wImag * realAndImag[twoj1];
					double tmpImag = wReal * realAndImag[twoj1] + wImag * realAndImag[twoj];
					realAndImag[twoj] = realAndImag[twoi] - tmpReal;
					realAndImag[twoj1] = realAndImag[twoi1] - tmpImag;
					realAndImag[twoi] += tmpReal;
					realAndImag[twoi1] += tmpImag;
				}
				// Next w is computed by complex multiplication with wStep
				// exp(i*(phi+delta)) = exp(i*phi)*exp(i*delta)
				double oldWReal = wReal;
				wReal = oldWReal * wStepReal - wImag * wStepImag;
				wImag = oldWReal * wStepImag + wImag * wStepReal;
			}
		}
		// For the inverse transform, scale down the resulting
		// signal by a factor of 1/N:
		if (inverse) {
			for (int i = 0; i < realAndImag.length; i++) {
				realAndImag[i] /= N;
			}
		}
	}

	/**
	 * Calculates the Fourier transform of a set of n real-valued data points. Replaces this data (which is stored in array
	 * data[1..n]) by the positive frequency half of its complex Fourier transform. The real-valued first and last components of
	 * the complex transform are returned as elements data[1] and data[2], respectively. n must be a power of 2. This routine also
	 * calculates the inverse transform of a complex data array if it is the transform of real data. (Result in this case must be
	 * multiplied by 2/n.)
	 * 
	 * @param data
	 *            data
	 * @param inverse
	 *            inverse
	 */
	public static void realTransform(double data[], boolean inverse) {
		double c1 = 0.5;
		int n = data.length;
		double twoPi = -MathUtils.TWOPI;
		if (inverse)
			twoPi = MathUtils.TWOPI;
		double delta = twoPi / n;
		double wStepReal = Math.cos(delta);
		double wStepImag = Math.sin(delta);
		double wReal = wStepReal;
		double wImag = wStepImag;

		double c2;
		if (!inverse) {
			c2 = -0.5;
			transform(data, false); // The forward transform is here.
		} else {
			c2 = 0.5; // Otherwise set up for an inverse transform
		}
		int n4 = n >> 2;
		for (int i = 1; i < n4; i++) { // Case i=0 done separately below.
			int twoI = i << 1;
			int twoIPlus1 = twoI + 1;
			int nMinusTwoI = n - twoI;
			int nMinusTwoIPlus1 = nMinusTwoI + 1;
			double h1r = c1 * (data[twoI] + data[nMinusTwoI]); // The two separate transforms are separated out of data.
			double h1i = c1 * (data[twoIPlus1] - data[nMinusTwoIPlus1]);
			double h2r = -c2 * (data[twoIPlus1] + data[nMinusTwoIPlus1]);
			double h2i = c2 * (data[twoI] - data[nMinusTwoI]);
			// Here they are recombined to form the true transform of the original real data.
			data[twoI] = h1r + wReal * h2r - wImag * h2i;
			data[twoIPlus1] = h1i + wReal * h2i + wImag * h2r;
			data[nMinusTwoI] = h1r - wReal * h2r + wImag * h2i;
			data[nMinusTwoIPlus1] = -h1i + wReal * h2i + wImag * h2r;
			// Next w is computed by complex multiplication with wStep
			// exp(i*(phi+delta)) = exp(i*phi)*exp(i*delta)
			double oldWReal = wReal;
			wReal = oldWReal * wStepReal - wImag * wStepImag;
			wImag = oldWReal * wStepImag + wImag * wStepReal;

		}
		if (!inverse) {
			double tmp = data[0];
			// Squeeze the first and last data together to get them all within the original array.
			data[0] += data[1];
			data[1] = tmp - data[1];
			data[n / 2 + 1] = -data[n / 2 + 1];
		} else { // inverse
			double tmp = data[0];
			data[0] = 0.5 * (tmp + data[1]);
			data[1] = 0.5 * (tmp - data[1]);
			data[n / 2 + 1] = -data[n / 2 + 1];
			transform(data, true);
		}
	}

	/**
	 * Compute the convolution of two signals, by multipying them in the frequency domain. Normalise the result with respect to
	 * deltaT (the inverse of the sampling rate). This method applies zero padding where necessary to ensure that the result is
	 * not polluted because of assumed periodicity. The two signals need not be of equal length.
	 * 
	 * @param signal1
	 *            signal 1
	 * @param signal2
	 *            signal 2
	 * @param deltaT
	 *            , the time difference between two samples (= 1/samplingrate)
	 * @return the convolved signal, with length signal1.length+signal2.length
	 */
	public static double[] convolveWithZeroPadding(final double[] signal1, final double[] signal2, double deltaT) {
		double[] result = convolveWithZeroPadding(signal1, signal2);
		for (int i = 0; i < result.length; i++) {
			result[i] *= deltaT;
		}
		return result;
	}

	/**
	 * Compute the convolution of two signals, by multipying them in the frequency domain. This method applies zero padding where
	 * necessary to ensure that the result is not polluted because of assumed periodicity. The two signals need not be of equal
	 * length.
	 * 
	 * @param signal1
	 *            signal 1
	 * @param signal2
	 *            signal 2
	 * @return the convolved signal, with length signal1.length+signal2.length
	 */
	public static double[] convolveWithZeroPadding(final double[] signal1, final double[] signal2) {
		if (signal1 == null || signal2 == null)
			throw new NullPointerException("Received null argument");
		int N = signal1.length + signal2.length;
		if (!MathUtils.isPowerOfTwo(N)) {
			N = MathUtils.closestPowerOfTwoAbove(N);
		}
		double[] fft1 = new double[N];
		double[] fft2 = new double[N];
		System.arraycopy(signal1, 0, fft1, 0, signal1.length);
		System.arraycopy(signal2, 0, fft2, 0, signal2.length);
		double[] fftResult = convolve(fft1, fft2);
		double[] result = new double[signal1.length + signal2.length];
		System.arraycopy(fftResult, 0, result, 0, result.length);
		return result;
	}

	/**
	 * Compute the convolution of two signals, by multiplying them in the frequency domain. Normalise the result with respect to
	 * deltaT (the inverse of the sampling rate). This is the core method, requiring two signals of equal length, which must be a
	 * power of two, and not checking for pollution arising from the assumed periodicity of both signals.
	 * 
	 * @param signal1
	 *            signal 1
	 * @param signal2
	 *            signal 2
	 * @param deltaT
	 *            , the time difference between two samples (= 1/samplingrate)
	 * @return the convolved signal, of the same length as the two input signals
	 * @throws IllegalArgumentException
	 *             if the two input signals do not have the same length.
	 */
	public static double[] convolve(final double[] signal1, final double[] signal2, double deltaT) {
		double[] result = convolve(signal1, signal2);
		for (int i = 0; i < result.length; i++) {
			result[i] *= deltaT;
		}
		return result;
	}

	/**
	 * Compute the convolution of two signals, by multiplying them in the frequency domain. This is the core method, requiring two
	 * signals of equal length, which must be a power of two, and not checking for pollution arising from the assumed periodicity
	 * of both signals.
	 * 
	 * @param signal1
	 *            signal 1
	 * @param signal2
	 *            signal 2
	 * @return the convolved signal, of the same length as the two input signals
	 * @throws IllegalArgumentException
	 *             if the two input signals do not have the same length.
	 */
	public static double[] convolve(final double[] signal1, final double[] signal2) {
		if (signal1 == null || signal2 == null)
			throw new NullPointerException("Received null argument");
		if (signal1.length != signal2.length)
			throw new IllegalArgumentException("Arrays must be equal length");
		int N = signal1.length;
		assert MathUtils.isPowerOfTwo(N);
		double[] fft1 = new double[N];
		System.arraycopy(signal1, 0, fft1, 0, N);
		double[] fft2 = new double[N];
		System.arraycopy(signal2, 0, fft2, 0, N);
		realTransform(fft1, false);
		realTransform(fft2, false);
		// Now multiply in the frequency domain,
		// and save in fft1:
		fft1[0] = fft1[0] * fft2[0]; // because imag[0] is 0
		fft1[1] = fft1[1] * fft2[1]; // and fft1[1] is actually real[N/2]
		for (int i = 2; i < N; i += 2) {
			double tmp = fft1[i];
			fft1[i] = fft1[i] * fft2[i] - fft1[i + 1] * fft2[i + 1];
			fft1[i + 1] = tmp * fft2[i + 1] + fft1[i + 1] * fft2[i];
		}
		// And transform back:
		realTransform(fft1, true);
		return fft1;
	}

	/**
	 * Compute the convolution of two signals, by multiplying them in the frequency domain. Normalise the result with respect to
	 * deltaT (the inverse of the sampling rate). This is a specialised version of the core method, requiring two signals of equal
	 * length, which must be a power of two, and not checking for pollution arising from the assumed periodicity of both signals.
	 * In this version, the first signal is provided in the time domain, while the second is already transformed into the
	 * frequency domain.
	 * 
	 * @param signal1
	 *            the first input signal, in the time domain
	 * @param fft2
	 *            the complex transform of the second signal, in the frequency domain fft[0] = real[0], fft[1] = real[N/2],
	 *            fft[2*i] = real[i], fft[2*i+1] = imag[i] for 1&le;i&lt;N/2
	 * @param deltaT
	 *            , the time difference between two samples (= 1/samplingrate)
	 * @return the convolved signal, of the same length as the two input signals
	 * @throws IllegalArgumentException
	 *             if the two input signals do not have the same length.
	 */
	public static double[] convolve_FD(final double[] signal1, final double[] fft2, double deltaT) {
		double[] result = convolve_FD(signal1, fft2);
		for (int i = 0; i < result.length; i++) {
			result[i] *= deltaT;
		}
		return result;
	}

	/**
	 * Compute the convolution of two signals, by multiplying them in the frequency domain. This is a specialised version of the
	 * core method, requiring two signals of equal length, which must be a power of two, and not checking for pollution arising
	 * from the assumed periodicity of both signals. In this version, the first signal is provided in the time domain, while the
	 * second is already transformed into the frequency domain.
	 * 
	 * @param signal1
	 *            the first input signal, in the time domain
	 * @param fft2
	 *            the complex transform of the second signal, in the frequency domain fft[0] = real[0], fft[1] = real[N/2],
	 *            fft[2*i] = real[i], fft[2*i+1] = imag[i] for 1&le;i&lt;N/2
	 * @return the convolved signal, of the same length as the two input signals
	 * @throws IllegalArgumentException
	 *             if the two input signals do not have the same length.
	 */
	public static double[] convolve_FD(final double[] signal1, final double[] fft2) {
		if (signal1 == null || fft2 == null)
			throw new NullPointerException("Received null argument");
		if (signal1.length != fft2.length)
			throw new IllegalArgumentException("Arrays must be equal length");
		int N = signal1.length;
		assert MathUtils.isPowerOfTwo(N);
		double[] fft1 = new double[N];
		System.arraycopy(signal1, 0, fft1, 0, N);
		realTransform(fft1, false);
		// Now multiply in the frequency domain,
		// and save in fft1:
		fft1[0] = fft1[0] * fft2[0]; // because imag[0] is 0
		fft1[1] = fft1[1] * fft2[1]; // and fft1[1] is actually real[N/2]
		for (int i = 2; i < N; i += 2) {
			double tmp = fft1[i];
			fft1[i] = fft1[i] * fft2[i] - fft1[i + 1] * fft2[i + 1];
			fft1[i + 1] = tmp * fft2[i + 1] + fft1[i + 1] * fft2[i];
		}
		// And transform back:
		realTransform(fft1, true);
		return fft1;
	}

	/**
	 * Compute the correlation of two signals, by multipying them in the frequency domain. This method applies zero padding where
	 * necessary to ensure that the result is not polluted because of assumed periodicity. The two signals need not be of equal
	 * length.
	 * 
	 * @param signal1
	 *            signal 1
	 * @param signal2
	 *            signal 2
	 * @return the correlation function, with length signal1.length+signal2.length
	 */
	public static double[] correlateWithZeroPadding(final double[] signal1, final double[] signal2) {
		if (signal1 == null || signal2 == null)
			throw new NullPointerException("Received null argument");
		int N = signal1.length + signal2.length;
		if (!MathUtils.isPowerOfTwo(N)) {
			N = MathUtils.closestPowerOfTwoAbove(N);
		}
		double[] fft1 = new double[N];
		double[] fft2 = new double[N];
		System.arraycopy(signal1, 0, fft1, 0, signal1.length);
		System.arraycopy(signal2, 0, fft2, 0, signal2.length);
		double[] fftResult = correlate(fft1, fft2);
		double[] result = new double[signal1.length + signal2.length];
		System.arraycopy(fftResult, 0, result, 0, result.length);
		return result;
	}

	/**
	 * Compute the correlation of two signals, by multiplying the transform of signal2 with the conjugate complex of the transform
	 * of signal1, in the frequency domain. Sign convention: If signal2 is shifted by n to the right of signal2, then the
	 * correlation function will have a peak at positive n. This is the core method, requiring two signals of equal length, which
	 * must be a power of two, and not checking for pollution arising from the assumed periodicity of both signals.
	 * 
	 * @param signal1
	 *            signal 1
	 * @param signal2
	 *            signal 2
	 * @return the correlated signal, of the same length as the two input signals
	 * @throws IllegalArgumentException
	 *             if the two input signals do not have the same length.
	 */
	public static double[] correlate(final double[] signal1, final double[] signal2) {
		if (signal1 == null || signal2 == null)
			throw new NullPointerException("Received null argument");
		if (signal1.length != signal2.length)
			throw new IllegalArgumentException("Arrays must be equal length");
		int N = signal1.length;
		assert MathUtils.isPowerOfTwo(N);
		double[] fft1 = new double[N];
		System.arraycopy(signal1, 0, fft1, 0, N);
		double[] fft2 = new double[N];
		System.arraycopy(signal2, 0, fft2, 0, N);
		realTransform(fft1, false);
		realTransform(fft2, false);
		// Now multiply with complex conjugate in the frequency domain,
		// and save in fft1:
		fft1[0] = fft1[0] * fft2[0]; // because imag[0] is 0
		fft1[1] = fft1[1] * fft2[1]; // and fft1[1] is actually real[N/2]
		for (int i = 2; i < N; i += 2) {
			double tmp = fft1[i];
			fft1[i] = fft1[i] * fft2[i] + fft1[i + 1] * fft2[i + 1];
			fft1[i + 1] = tmp * fft2[i + 1] - fft1[i + 1] * fft2[i];
		}
		// And transform back:
		realTransform(fft1, true);
		return fft1;
	}

	/**
	 * Compute the autocorrelation of a signal, by inverse transformation of its power spectrum. This is the core method,
	 * requiring a signal whose length must be a power of two, and not checking for pollution arising from the assumed periodicity
	 * of the signal.
	 * 
	 * @param signal
	 *            signal
	 * @return the correlated signal, of the same length as the input signal
	 */
	public static double[] autoCorrelate(final double[] signal) {
		if (signal == null)
			throw new NullPointerException("Received null argument");
		int N = signal.length;
		assert MathUtils.isPowerOfTwo(N);
		double[] fft = new double[N];
		System.arraycopy(signal, 0, fft, 0, N);
		realTransform(fft, false);

		// Now multiply with complex conjugate in the frequency domain,
		// and save in fft:
		fft[0] = fft[0] * fft[0]; // because imag[0] is 0
		fft[1] = fft[1] * fft[1]; // and fft[1] is actually real[N/2]
		for (int i = 2; i < N; i += 2) {
			fft[i] = fft[i] * fft[i] + fft[i + 1] * fft[i + 1];
			fft[i + 1] = 0;
		}
		// And transform back:
		realTransform(fft, true);
		return fft;
	}

	/**
	 * Compute the autocorrelation of a signal, by inverse transformation of its power spectrum. This method applies zero padding
	 * where necessary to ensure that the result is not polluted because of assumed periodicity.
	 * 
	 * @param signal
	 *            signal
	 * @return the correlated signal, of the same length as the input signal
	 */
	public static double[] autoCorrelateWithZeroPadding(final double[] signal) {
		int n = MathUtils.closestPowerOfTwoAbove(2 * signal.length);
		double[] fftSignal = new double[n];
		System.arraycopy(signal, 0, fftSignal, 0, signal.length);
		double[] fftAutocorr = autoCorrelate(fftSignal);
		double[] result = new double[signal.length];
		int halfLength = signal.length / 2;
		int odd = signal.length % 2;
		System.arraycopy(fftAutocorr, n - halfLength, result, 0, halfLength);
		System.arraycopy(fftAutocorr, 0, result, halfLength, halfLength + odd);
		return result;
	}

	public static void main(String[] args) throws Exception {
		/*
*/
		for (int i = 0; i < args.length; i++) {
			System.out.println("Measuring FFT accuracy for " + args[i]);
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[i]));
			double[] signal = MaryAudioUtils.getSamplesAsDoubleArray(ais);
			int N = signal.length;
			if (!MathUtils.isPowerOfTwo(N)) {
				N = MathUtils.closestPowerOfTwoAbove(N);
			}
			double[] ar = new double[N];
			double[] ai = new double[N];
			System.arraycopy(signal, 0, ar, 0, signal.length);

			/*
			 * double[] conv1 = autoCorrelateOrig(ar); double[] conv2 = autoCorrelate(ar);
			 * System.err.println("Difference between conv1 and 2: "+ MathUtils.sumSquaredError(conv1, conv2)); for (int j=0;
			 * j<conv1.length; j++) { double ddelta = Math.abs(conv1[j]-conv2[j]); //if (ddelta > 1.E-4)
			 * System.err.println("delta["+j+"]="+ddelta+" out of "+conv1[j]); }
			 */
			// Transform:
			FFT.transform(ar, ai, false);
			double[] result1 = new double[2 * N];
			for (int j = 0; j < N; j++) {
				result1[2 * j] = ar[j];
				result1[2 * j + 1] = ai[j];
			}
			// Transform 2:
			double[] result2 = new double[2 * N];
			for (int j = 0; j < signal.length; j++) {
				result2[2 * j] = signal[j];
			}
			FFT.transform(result2, false);
			System.err.println("Difference between result1 and 2: " + MathUtils.sumSquaredError(result1, result2));
			// Transform 3:
			double[] result3 = new double[N];
			System.arraycopy(signal, 0, result3, 0, signal.length);
			FFT.realTransform(result3, false);
			double[] result2a = new double[N];
			System.arraycopy(result2, 0, result2a, 0, N);
			System.err.println("F2(N/2)=" + result2[N] + " F3(N/2)=" + result3[1]);
			result3[1] = 0;
			System.err.println("Difference between result 2a and 3: " + MathUtils.sumSquaredError(result2a, result3));
			double[] delta = new double[N];
			for (int j = 0; j < N; j++) {
				delta[j] = Math.abs(result2a[j] - result3[j]);
				if (delta[j] > 1.E-4)
					System.err.println("delta[" + j + "]=" + delta[j]);
			}
			result3[1] = result2[N];

			// And backwards:
			FFT.transform(ar, ai, true);
			FFT.transform(result2, true);
			double[] inverse2 = new double[N];
			for (int j = 0; j < N; j++) {
				inverse2[j] = result2[2 * j];
			}
			FFT.realTransform(result3, true);
			System.err.println("Difference between inverse 1 and 2:" + MathUtils.sumSquaredError(ar, inverse2));
			System.err.println("Difference between inverse 1 and 3:" + MathUtils.sumSquaredError(ar, result3));
			for (int j = 0; j < N; j++) {
				delta[j] = Math.abs(ar[j] - result3[j]);
				if (delta[j] > 1.E-4)
					System.err.println("delta[" + j + "]=" + delta[j]);
			}

			// Compute speed
			System.out.println("Computing FFT speed for 1000 transforms at different n");
			for (int k = 5; k <= 11; k++) {
				int n = 1 << k;
				double[] re = new double[n];
				double[] im = new double[n];
				System.arraycopy(signal, 0, re, 0, Math.min(signal.length, n));
				long start = System.currentTimeMillis();
				for (int j = 0; j < 5000; j++) {
					FFT.transform(re, im, false);
					FFT.transform(re, im, true);
				}
				long mid = System.currentTimeMillis();
				for (int j = 0; j < 5000; j++) {
					FFT.realTransform(re, false);
					FFT.realTransform(re, true);
				}
				long end = System.currentTimeMillis();
				long t1 = (mid - start);
				long t2 = (end - mid);
				System.out.println("n=" + n + " fft=" + t1 + ", realFFT=" + t2);
			}

		}
	}

}
