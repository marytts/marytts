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
package marytts.signalproc.filter;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.FFT;
import marytts.util.math.MathUtils;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class BandRejectFilter extends FIRFilter {
	public static double DEFAULT_TRANSITIONBANDWIDTH = 0.01;
	public double lowerNormalisedCutoffFrequency;
	public double upperNormalisedCutoffFrequency;

	/**
	 * Create a new band reject filter with the given normalized cutoff frequencies and a default transition band width.
	 *
	 * @param lowerNormalisedCutoffFrequencyIn
	 *            the cutoff frequency corresponding to the lower end of the band, expressed as a fraction of the sampling rate.
	 *            It must be in the range ]0, 0.5[. For example, with a sampling rate of 16000 Hz and a desired cutoff frequency
	 *            of 4000 Hz, the lowerNormalisedCutoffFrequency would have to be 0.25.
	 * @param upperNormalisedCutoffFrequencyIn
	 *            the cutoff frequency corresponding to the upper end of the band, expressed as a fraction of the sampling rate.
	 *            It must be in the range ]0, 0.5[. For example, with a sampling rate of 16000 Hz and a desired cutoff frequency
	 *            of 6000 Hz, the upperNormalisedCutoffFrequency would have to be 0.375.
	 */
	public BandRejectFilter(double lowerNormalisedCutoffFrequencyIn, double upperNormalisedCutoffFrequencyIn) {
		this(lowerNormalisedCutoffFrequencyIn, upperNormalisedCutoffFrequencyIn, DEFAULT_TRANSITIONBANDWIDTH);
	}

	/**
	 * Create a new band reject filter with the given normalized cutoff frequencies and a default transition band width.
	 *
	 * @param lowerNormalisedCutoffFrequencyIn
	 *            the cutoff frequency corresponding to the lower end of the band, expressed as a fraction of the sampling rate.
	 *            It must be in the range ]0, 0.5[. For example, with a sampling rate of 16000 Hz and a desired cutoff frequency
	 *            of 4000 Hz, the lowerNormalisedCutoffFrequency would have to be 0.25.
	 * @param upperNormalisedCutoffFrequencyIn
	 *            the cutoff frequency corresponding to the upper end of the band, expressed as a fraction of the sampling rate.
	 *            It must be in the range ]0, 0.5[. For example, with a sampling rate of 16000 Hz and a desired cutoff frequency
	 *            of 6000 Hz, the upperNormalisedCutoffFrequency would have to be 0.375.
	 * @param normalisedTransitionBandwidth
	 *            indicates the desired quality of the filter. The smaller the bandwidth, the more abrupt the cutoff at the cutoff
	 *            frequency, but also the larger the filter kernel (impulse response) and computationally costly the filter. Usual
	 *            range of this parameter is [0.002, 0.2].
	 */
	public BandRejectFilter(double lowerNormalisedCutoffFrequencyIn, double upperNormalisedCutoffFrequencyIn,
			double normalisedTransitionBandwidth) {
		this(lowerNormalisedCutoffFrequencyIn, upperNormalisedCutoffFrequencyIn,
				bandwidth2kernelLength(normalisedTransitionBandwidth));
	}

	/**
	 * Create a new band reject filter with the given normalised cutoff frequencies and a default transition band width.
	 *
	 * @param lowerNormalisedCutoffFrequencyIn
	 *            the cutoff frequency corresponding to the lower end of the band, expressed as a fraction of the sampling rate.
	 *            It must be in the range ]0, 0.5[. For example, with a sampling rate of 16000 Hz and a desired cutoff frequency
	 *            of 4000 Hz, the lowerNormalisedCutoffFrequency would have to be 0.25.
	 * @param upperNormalisedCutoffFrequencyIn
	 *            the cutoff frequency corresponding to the upper end of the band, expressed as a fraction of the sampling rate.
	 *            It must be in the range ]0, 0.5[. For example, with a sampling rate of 16000 Hz and a desired cutoff frequency
	 *            of 6000 Hz, the upperNormalisedCutoffFrequency would have to be 0.375.
	 * @param kernelLength
	 *            length of the filter kernel (the impulse response). The kernel length must be an odd number. The longer the
	 *            kernel, the sharper the cutoff, i.e. the narrower the transition band. Typical lengths are in the range of
	 *            10-1000.
	 * @throws IllegalArgumentException
	 *             if the kernel length is not a positive, odd number, or if normalisedCutoffFrequency is not in the range between
	 *             0 and 0.5.
	 */
	public BandRejectFilter(double lowerNormalisedCutoffFrequencyIn, double upperNormalisedCutoffFrequencyIn, int kernelLength) {
		super();
		if (kernelLength <= 0 || kernelLength % 2 == 0) {
			throw new IllegalArgumentException("Kernel length must be an odd positive number, got " + kernelLength);
		}

		lowerNormalisedCutoffFrequency = lowerNormalisedCutoffFrequencyIn;
		upperNormalisedCutoffFrequency = upperNormalisedCutoffFrequencyIn;
		if (lowerNormalisedCutoffFrequency <= 0 || lowerNormalisedCutoffFrequency >= 0.5 || upperNormalisedCutoffFrequency <= 0
				|| upperNormalisedCutoffFrequency >= 0.5) {
			throw new IllegalArgumentException("Normalised cutoff frequencies must be between 0 and 0.5, got "
					+ lowerNormalisedCutoffFrequency + " and " + upperNormalisedCutoffFrequency);
		}
		double[] kernel = getKernel(lowerNormalisedCutoffFrequency, upperNormalisedCutoffFrequency, kernelLength);
		// determine the length of the slices by which the signal will be consumed:
		// this is the distance to the second next power of two, so that the slice
		// will be at least as long as the kernel.
		sliceLength = MathUtils.closestPowerOfTwoAbove(2 * kernelLength) - kernelLength;
		initialise(kernel, sliceLength);
	}

	/**
	 * For a given sampling rate, return the width of the transition band for this filter, in Hertz.
	 *
	 * @param samplingRate
	 *            the sampling rate, in Hertz.
	 * @return samplingRate
	 */
	public double getTransitionBandWidth(int samplingRate) {
		return samplingRate * kernelLength2bandwidth(impulseResponseLength);
	}

	/**
	 * Compute the band-reject filter kernel, as the sum of a low-pass filter kernel and a high-pass filter kernel.
	 *
	 * @param lowerNormalisedCutoffFrequencyIn
	 *            lowerNormalisedCutoffFrequencyIn
	 * @param upperNormalisedCutoffFrequencyIn
	 *            upperNormalisedCutoffFrequencyIn
	 * @param kernelLength
	 *            kernelLength
	 * @return kernel
	 */
	protected static double[] getKernel(double lowerNormalisedCutoffFrequencyIn, double upperNormalisedCutoffFrequencyIn,
			int kernelLength) {
		double[] lowPassKernel = LowPassFilter.getKernel(lowerNormalisedCutoffFrequencyIn, kernelLength);
		double[] highPassKernel = HighPassFilter.getKernel(upperNormalisedCutoffFrequencyIn, kernelLength);
		double[] kernel = new double[kernelLength];
		for (int i = 0; i < kernelLength; i++) {
			kernel[i] = lowPassKernel[i] + highPassKernel[i];
		}
		return kernel;
	}

	/**
	 * Convert from normalisedTransitionBandwidth to filter kernel length, using the approximate formula l = 4/bw.
	 *
	 * @param normalisedTransitionBandwidth
	 *            normalisedTransitionBandwidth
	 * @return the corresponding filter kernel length (guaranteed to be an odd number).
	 */
	protected static int bandwidth2kernelLength(double normalisedTransitionBandwidth) {
		int l = (int) (4 / normalisedTransitionBandwidth);
		// kernel length must be odd:
		if (l % 2 == 0)
			l++;
		return l;
	}

	/**
	 * Convert from filter kernel length to normalisedTransitionBandwidth, using the approximate formula l = 4/bw.
	 *
	 * @param kernelLength
	 *            kernelLength
	 * @return the corresponding normalized transition bandwidth.
	 */
	protected static double kernelLength2bandwidth(int kernelLength) {
		return (double) 4 / kernelLength;
	}

	public String toString() {
		return "Band reject filter";
	}
}
