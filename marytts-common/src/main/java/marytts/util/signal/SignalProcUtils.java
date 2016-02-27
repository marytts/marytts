/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.util.signal;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.LpcAnalyser;
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.filter.BandPassFilter;
import marytts.signalproc.filter.FIRFilter;
import marytts.signalproc.filter.HighPassFilter;
import marytts.signalproc.filter.LowPassFilter;
import marytts.signalproc.filter.RecursiveFilter;
import marytts.signalproc.window.BartlettWindow;
import marytts.signalproc.window.BlackmanWindow;
import marytts.signalproc.window.FlattopWindow;
import marytts.signalproc.window.GaussWindow;
import marytts.signalproc.window.HammingWindow;
import marytts.signalproc.window.HanningWindow;
import marytts.signalproc.window.RectWindow;
import marytts.signalproc.window.Window;
import marytts.util.data.AlignLabelsUtils;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.MaryAudioUtils;
import marytts.util.display.DisplayUtils;
import marytts.util.math.ArrayUtils;
import marytts.util.math.ComplexArray;
import marytts.util.math.FFT;
import marytts.util.math.FFTMixedRadix;
import marytts.util.math.MathUtils;
import marytts.util.string.StringUtils;

public class SignalProcUtils {

	public static int getLPOrder(int fs) {
		int P = (int) (fs / 1000.0f + 2);

		if (P % 2 == 1)
			P += 1;

		return P;
	}

	public static int getDFTSize(int fs) {
		int dftSize;

		if (fs <= 8000)
			dftSize = 128;
		else if (fs <= 16000)
			dftSize = 256;
		else if (fs <= 22050)
			dftSize = 512;
		else if (fs <= 32000)
			dftSize = 1024;
		else if (fs <= 44100)
			dftSize = 2048;
		else
			dftSize = 4096;

		return dftSize;
	}

	// Returns an odd filter order depending on the sampling rate for FIR filter design
	public static int getFIRFilterOrder(int fs) {
		int oddFilterOrder = getDFTSize(fs) - 1;
		if (oddFilterOrder % 2 == 0)
			oddFilterOrder++;

		return oddFilterOrder;
	}

	public static int getLifterOrder(int fs) {
		int lifterOrder = 2 * (int) (fs / 1000.0f + 2);

		if (lifterOrder % 2 == 1)
			lifterOrder += 1;

		return lifterOrder;
	}

	public static int halfSpectrumSize(int fftSize) {
		return (int) (Math.floor(fftSize / 2.0 + 1.5));
	}

	public static int fullSpectrumSize(int maxFreq) {
		return 2 * (maxFreq - 1);
	}

	public static double getEnergydB(double x) {
		double[] y = new double[1];
		y[0] = x;

		return getEnergydB(y);
	}

	public static double getEnergydB(double[] x) {
		return getEnergydB(x, x.length);
	}

	public static double getEnergydB(double[] x, int len) {
		return getEnergydB(x, len, 0);
	}

	public static double getEnergydB(double[] x, int len, int start) {
		return 10 * Math.log10(getEnergy(x, len, start));
	}

	public static double getEnergy(double[] x, int len, int start) {
		if (start < 0)
			start = 0;
		if (start > x.length - 1)
			start = x.length - 1;
		if (start + len > x.length)
			len = x.length - start - 1;

		double en = 0.0;

		for (int i = start; i < start + len; i++)
			en += x[i] * x[i];

		en = Math.sqrt(en);
		en = Math.max(en, 1e-100); // Put a minimum floor to avoid -Ininity in log based computations

		return en;
	}

	public static double getEnergy(double[] x, int len) {
		return getEnergy(x, len, 0);
	}

	public static double getEnergy(double[] x) {
		return getEnergy(x, x.length, 0);
	}

	public static double getAverageSampleEnergy(double[] x, int len, int start) {
		if (start < 0)
			start = 0;
		if (start > x.length - 1)
			start = x.length - 1;
		if (start + len > x.length)
			len = x.length - start - 1;

		double avgSampEn = 0.0;

		for (int i = start; i < start + len; i++)
			avgSampEn += x[i] * x[i];

		avgSampEn = Math.sqrt(avgSampEn);
		avgSampEn /= len;

		return avgSampEn;
	}

	public static double getAverageSampleEnergy(double[] x, int len) {
		return getAverageSampleEnergy(x, len, 0);
	}

	public static double getAverageSampleEnergy(double[] x) {
		return getAverageSampleEnergy(x, x.length, 0);
	}

	public static double[] normalizeAverageSampleEnergy(double[] x, double newAverageSampleEnergy) {
		double gain = newAverageSampleEnergy / (1e-20 + getAverageSampleEnergy(x));

		return MathUtils.multiply(x, gain);
	}

	public static double[] getEnergyContourRms(double[] x, double windowSizeInSeconds, double skipSizeInSeconds, int samplingRate) {
		int ws = (int) Math.floor(windowSizeInSeconds * samplingRate + 0.5);
		int ss = (int) Math.floor(skipSizeInSeconds * samplingRate + 0.5);
		int numfrm = (int) Math.floor((x.length - (double) ws) / ss + 0.5);

		double[] energies = null;

		if (numfrm > 0) {
			energies = new double[numfrm];
			double[] frm = new double[ws];
			int i, j;
			for (i = 0; i < numfrm; i++) {
				Arrays.fill(frm, 0.0);
				System.arraycopy(x, i * ss, frm, 0, Math.min(ws, x.length - i * ss));
				energies[i] = 0.0;
				for (j = 0; j < ws; j++)
					energies[i] += frm[j] * frm[j];
				energies[i] /= ws;
				energies[i] = Math.sqrt(energies[i]);
				energies[i] = MathUtils.amp2db(energies[i] + 1e-20);
			}
		}

		return energies;
	}

	public static float[] getAverageSampleEnergyContour(double[] x, double windowSizeInSeconds, double skipSizeInSeconds,
			int samplingRate) {
		int ws = (int) Math.floor(windowSizeInSeconds * samplingRate + 0.5);
		int ss = (int) Math.floor(skipSizeInSeconds * samplingRate + 0.5);
		int numfrm = (int) Math.floor((x.length - (double) ws) / ss + 0.5);

		float[] averageSampleEnergies = null;

		if (numfrm > 0) {
			averageSampleEnergies = new float[numfrm];
			double[] frm = new double[ws];
			int i, j;
			for (i = 0; i < numfrm; i++) {
				Arrays.fill(frm, 0.0);
				System.arraycopy(x, i * ss, frm, 0, Math.min(ws, x.length - i * ss));

				averageSampleEnergies[i] = (float) SignalProcUtils.getAverageSampleEnergy(frm);
			}
		}

		return averageSampleEnergies;
	}

	// Returns the average sample energy contour around times using analysis windows of site windowDurationInSeconds
	public static float[] getAverageSampleEnergyContour(double[] x, float[] times, int samplingRateInHz,
			float windowDurationInSeconds) {
		float[] averageSampleEnergies = null;

		if (x != null && times != null) {
			int startInd, endInd;
			int len;
			double[] frm;
			averageSampleEnergies = new float[times.length];
			for (int i = 0; i < times.length; i++) {
				averageSampleEnergies[i] = 0.0f;
				if (times[i] > -1.0f) {
					startInd = SignalProcUtils.time2sample(Math.max(0.0f, times[i] - 0.5f * windowDurationInSeconds),
							samplingRateInHz);
					endInd = SignalProcUtils.time2sample(times[i] + 0.5 * windowDurationInSeconds, samplingRateInHz);
					if (endInd > x.length - 1)
						endInd = x.length - 1;

					len = endInd - startInd + 1;
					if (len > 0) {
						frm = new double[len];
						System.arraycopy(x, startInd, frm, 0, len);

						averageSampleEnergies[i] = (float) SignalProcUtils.getAverageSampleEnergy(frm);
					}
				}
			}
		}

		return averageSampleEnergies;
	}

	public static double[] normalizeAverageSampleEnergyContour(double[] x, float[] times, float[] currentContour,
			float[] targetContour, int samplingRateInHz, float windowDurationInSeconds) {
		float[] averageSampleEnergies = null;
		double[] y = null;

		if (x != null && times != null) {
			y = ArrayUtils.copy(x);
			int n;
			float t;
			int ind;
			float gain;
			for (n = 0; n < y.length; n++) {
				t = SignalProcUtils.sample2time(n, samplingRateInHz);
				ind = MathUtils.findClosest(times, t);
				gain = 1.0f;
				if (currentContour[ind] > 0.0f && times[ind] > -1.0f) {
					if (t < times[ind] && ind > 0 && currentContour[ind - 1] > 0.0f && times[ind - 1] > -1.0f)
						gain = MathUtils.linearMap(t, times[ind - 1], times[ind], targetContour[ind - 1]
								/ currentContour[ind - 1], targetContour[ind] / currentContour[ind]);
					else if (t > times[ind] && ind < times.length - 1 && currentContour[ind + 1] > 0.0f && times[ind + 1] > -1.0f)
						gain = MathUtils.linearMap(t, times[ind], times[ind + 1], targetContour[ind] / currentContour[ind],
								targetContour[ind + 1] / currentContour[ind + 1]);
					else
						gain = targetContour[ind] / currentContour[ind];
				}

				y[n] *= gain;
			}
		}

		return y;
	}

	// Returns the reversed version of the input array
	public static double[] reverse(double[] x) {
		double[] y = new double[x.length];
		for (int i = 0; i < x.length; i++)
			y[i] = x[x.length - i - 1];

		return y;
	}

	// Returns voiced/unvoiced information for each pitch frame
	public static boolean[] getVuvs(double[] f0s) {
		if (f0s != null) {
			boolean[] vuvs = new boolean[f0s.length];
			for (int i = 0; i < vuvs.length; i++) {
				if (f0s[i] < 10.0)
					vuvs[i] = false;
				else
					vuvs[i] = true;
			}

			return vuvs;
		} else
			return null;
	}

	/*
	 * Extracts pitch marks from a given pitch contour. // It is not very optimized, only inserts a pitch mark and waits for
	 * sufficient samples before inserting a new one // in order not to insert more than one pitch mark within one pitch period //
	 * // f0s: pitch contour vector // fs: sampling rate in Hz // len: total samples in original speech signal // ws: window size
	 * used for pitch extraction in seconds // ss: skip size used for pitch extraction in seconds // // offset: applies a fixed
	 * offset to the first pitch mark so that all pitch marks are shifted by same amount // // To do: // Perform marking on the
	 * residual by trying to locate glottal closure instants (might be implemented as a separate function indeed) // This may
	 * improve PSOLA performance also
	 */
	public static PitchMarks pitchContour2pitchMarks(double[] f0s, int fs, int len, double ws, double ss,
			boolean bPaddZerosForFinalPitchMark, int offset) {
		// Interpolate unvoiced segments
		double[] interpf0s = interpolate_pitch_uv(f0s);
		int numfrm = f0s.length;
		int maxTotalPitchMarks = len;
		int[] tmpPitchMarks = MathUtils.zerosInt(maxTotalPitchMarks);
		float[] tmpF0s = new float[maxTotalPitchMarks];

		int count = 0;
		int prevInd = 1;
		int ind;
		double T0;

		assert offset >= 0;

		for (int i = 1; i <= len; i++) {
			ind = (int) (Math.floor(((i - 1.0) / fs - 0.5 * ws) / ss + 0.5) + 1);
			if (ind < 1)
				ind = 1;

			if (ind > numfrm)
				ind = numfrm;

			if (interpf0s[ind - 1] > 10.0)
				T0 = (fs / interpf0s[ind - 1]);
			else
				T0 = (fs / 100.0f);

			if (i == 1 || i - T0 >= prevInd) // Insert new pitch mark
			{
				count++;

				tmpPitchMarks[count - 1] = i - 1 + offset;
				prevInd = i;

				if (i > 1)
					tmpF0s[count - 2] = (float) f0s[ind - 1];
			}
		}

		PitchMarks pm = null;
		if (count > 1) {
			// Check if last pitch mark corresponds to the end of signal, otherwise put an additional pitch mark to match the last
			// period and note to padd sufficient zeros
			if (bPaddZerosForFinalPitchMark && tmpPitchMarks[count - 1] != len - 1) {
				pm = new PitchMarks(count + 1, tmpPitchMarks, tmpF0s, 0);
				pm.pitchMarks[pm.pitchMarks.length - 1] = pm.pitchMarks[pm.pitchMarks.length - 2]
						+ (pm.pitchMarks[pm.pitchMarks.length - 2] - pm.pitchMarks[pm.pitchMarks.length - 3]);
				pm.totalZerosToPadd = pm.pitchMarks[pm.pitchMarks.length - 1] - (len - 1);
			} else
				pm = new PitchMarks(count, tmpPitchMarks, tmpF0s, 0);
		}

		return pm;
	}

	// Convert pitch marks to pitch contour values in Hz using a fixed analysis rate
	// Note that this function might result in inaccurate f0 values if the pitch marks are created from an f0 contour
	// The inaccuracy is due to conversion from float/double f0 values to integer pitch marks
	public static double[] pitchMarks2PitchContour(int[] pitchMarks, float ws, float ss, int samplingRate) {
		double[] f0s = null;
		float[] times = samples2times(pitchMarks, samplingRate);
		int numfrm = (int) Math.floor((times[times.length - 1] - 0.5 * ws) / ss + 0.5);

		if (numfrm > 0) {
			f0s = new double[numfrm];

			int currentInd;
			float currentTime;
			float T0;
			for (int i = 0; i < numfrm; i++) {
				currentTime = i * ss + 0.5f * ws;
				currentInd = MathUtils.findClosest(times, currentTime);

				if (currentInd > 0)
					f0s[i] = 1.0 / (times[currentInd] - times[currentInd - 1]);
				else
					f0s[i] = 1.0 / times[currentInd];
			}
		}

		return f0s;
	}

	public static double[] fixedRateF0Values(PitchMarks pm, double wsFixedInSeconds, double ssFixedInSeconds, int numfrm,
			int samplingRate) {
		double[] f0s = new double[numfrm];

		int i, ind, sample;
		boolean isVoiced;
		for (i = 0; i < numfrm; i++) {
			sample = SignalProcUtils.time2sample((float) (i * ssFixedInSeconds + 0.5 * wsFixedInSeconds), samplingRate);
			ind = MathUtils.findClosest(pm.pitchMarks, sample);

			f0s[i] = 0.0;
			if (ind < 0) {
				if (sample > pm.pitchMarks[pm.pitchMarks.length - 1])
					ind = pm.pitchMarks.length - 1;
				else
					ind = 1;
			}

			isVoiced = pm.f0s[ind - 1] > 10.0 ? true : false;
			if (isVoiced) {
				if (ind > 0)
					f0s[i] = ((double) samplingRate) / (pm.pitchMarks[ind] - pm.pitchMarks[ind - 1]);
				else
					f0s[i] = ((double) samplingRate) / (pm.pitchMarks[ind + 1] - pm.pitchMarks[ind]);
			}
		}

		return f0s;
	}

	public static double[] interpolate_pitch_uv(double[] f0s) {
		return interpolate_pitch_uv(f0s, 10.0);
	}

	// Interpolates unvoiced parts of the f0 contour
	// using the neighbouring voiced parts
	// Linear interpolation is used
	public static double[] interpolate_pitch_uv(double[] f0s, double minVoicedVal) {
		int[] ind_v = MathUtils.find(f0s, MathUtils.GREATER_THAN, minVoicedVal);
		double[] new_f0s = null;

		if (ind_v == null) {
			new_f0s = new double[f0s.length];
			System.arraycopy(f0s, 0, new_f0s, 0, f0s.length);
		} else {
			double[] tmp_f0s = new double[f0s.length];
			System.arraycopy(f0s, 0, tmp_f0s, 0, f0s.length);

			int[] ind_v2 = null;
			if (ind_v[0] != 0) {
				tmp_f0s[0] = MathUtils.mean(f0s, ind_v);
				ind_v2 = new int[ind_v.length + 1];
				ind_v2[0] = 0;
				System.arraycopy(ind_v, 0, ind_v2, 1, ind_v.length);
			} else {
				ind_v2 = new int[ind_v.length];
				System.arraycopy(ind_v, 0, ind_v2, 0, ind_v.length);
			}

			int[] ind_v3 = null;
			if (ind_v2[ind_v2.length - 1] != tmp_f0s.length - 1) {
				tmp_f0s[tmp_f0s.length - 1] = tmp_f0s[ind_v2[ind_v2.length - 1]];
				ind_v3 = new int[ind_v2.length + 1];
				System.arraycopy(ind_v2, 0, ind_v3, 0, ind_v2.length);
				ind_v3[ind_v2.length] = f0s.length - 1;
			} else {
				ind_v3 = new int[ind_v2.length];
				System.arraycopy(ind_v2, 0, ind_v3, 0, ind_v2.length);
			}

			int i;
			double[] y = new double[ind_v3.length];
			for (i = 0; i < ind_v3.length; i++)
				y[i] = tmp_f0s[ind_v3[i]];

			int[] xi = new int[f0s.length];
			for (i = 0; i < f0s.length; i++)
				xi[i] = i;

			new_f0s = MathUtils.interpolate_linear(ind_v3, y, xi);
		}

		return new_f0s;
	}

	// A least squares line is fit to the given contour
	// and the parameters of the line are returned, i.e. line[0]=intercept and line[1]=slope
	public static double[] getContourLSFit(double[] contour, boolean isPitchUVInterpolation) {
		double[] line = null;

		if (contour != null) {
			double[] newContour = new double[contour.length];
			System.arraycopy(contour, 0, newContour, 0, contour.length);

			if (isPitchUVInterpolation)
				newContour = SignalProcUtils.interpolate_pitch_uv(newContour);

			double[] indices = new double[contour.length];
			for (int i = 0; i < contour.length; i++)
				indices[i] = i;

			line = fitLeastSquaresLine(indices, newContour);
		}

		return line;
	}

	public static double[] fitLeastSquaresLine(double[] x, double[] y) {
		assert x != null;
		assert y != null;
		assert x.length == y.length;

		double[] params = new double[2];

		double sx = 0.0;
		double sy = 0.0;
		double sxx = 0.0;
		double sxy = 0.0;
		double delta;

		int numPoints = x.length;

		for (int i = 0; i < numPoints; i++) {
			sx += x[i];
			sy += y[i];
			sxx += x[i] * x[i];
			sxy += x[i] * y[i];
		}

		delta = numPoints * sxx - sx * sx;

		// Intercept
		params[0] = (sxx * sy - sx * sxy) / delta;
		// Slope
		params[1] = (numPoints * sxy - sx * sy) / delta;

		return params;
	}

	public static boolean getVoicing(double[] windowedSpeechFrame, int samplingRateInHz) {
		return getVoicing(windowedSpeechFrame, samplingRateInHz, 0.35f);
	}

	public static boolean getVoicing(double[] windowedSpeechFrame, int samplingRateInHz, double voicingThreshold) {
		double Pvoiced = getVoicingProbability(windowedSpeechFrame, samplingRateInHz);

		return Pvoiced >= voicingThreshold;
	}

	public static double getVoicingProbability(double[] windowedSpeechFrame, int samplingRateInHz) {
		int maxT0 = (int) ((double) samplingRateInHz / 40.0);
		int minT0 = (int) ((double) samplingRateInHz / 400.0);
		if (maxT0 > windowedSpeechFrame.length - 1)
			maxT0 = windowedSpeechFrame.length - 1;
		if (minT0 > maxT0)
			minT0 = maxT0;

		double[] R = SignalProcUtils.autocorr(windowedSpeechFrame, maxT0);

		double maxR = R[minT0];

		for (int i = minT0 + 1; i <= maxT0; i++) {
			if (R[i] > maxR)
				maxR = R[i];
		}

		double Pvoiced = maxR / R[0];

		return Pvoiced;
	}

	public static double[] autocorr(double[] x, int LPOrder) {
		int N = x.length;
		double[] R = new double[LPOrder + 1];

		int n, m;

		for (m = 0; m <= LPOrder; m++) {
			R[m] = 0.0;

			for (n = 0; n <= x.length - m - 1; n++)
				R[m] += x[n] * x[n + m];
		}

		return R;
	}

	// Apply a 1st order highpass preemphasis filter to speech frame frm
	public static double[] applyPreemphasis(double[] frm, double preCoef) {
		double[] frmOut = new double[frm.length];
		System.arraycopy(frm, 0, frmOut, 0, frm.length);

		if (preCoef > 0.0) {
			double[] coeffs = new double[2];
			coeffs[0] = 1.0;
			coeffs[1] = -preCoef;

			RecursiveFilter r = new RecursiveFilter(coeffs);

			r.apply(frmOut);
		}

		return frmOut;
	}

	// Remove preemphasis from preemphasized frame frm (i.e. 1st order lowspass filtering)
	public static double[] removePreemphasis(double[] frm, double preCoef) {
		double[] frmOut = null;

		if (frm != null && frm.length > 0) {
			frmOut = new double[frm.length];
			System.arraycopy(frm, 0, frmOut, 0, frm.length);

			if (preCoef > 0.0) {
				double[] coeffs = new double[2];
				coeffs[0] = 1.0;
				coeffs[1] = preCoef;

				RecursiveFilter r = new RecursiveFilter(coeffs);

				r.apply(frmOut);
			}
		}

		return frmOut;
	}

	public static double[] freq2bark(double[] freqsInHz) {
		double[] barks = null;

		if (freqsInHz != null) {
			barks = new double[freqsInHz.length];
			for (int i = 0; i < barks.length; i++)
				barks[i] = freq2bark(freqsInHz[i]);
		}

		return barks;
	}

	public static double freq2bark(double freqInHz) {
		return 13.0 * Math.atan(0.00076 * freqInHz) + 3.5 * Math.atan((freqInHz * freqInHz / (7500 * 7500)));
	}

	/**
	 * Since there is no asinh in Math, here it is used its definition: asinh(x) = ln( x + sqrt(x^2+1) ) This function is used in
	 * fft2barkmx()
	 * 
	 * @param freqInHz
	 *            frequency In Hz
	 * @return 6 times log of f + square root of f times t + 1
	 */
	public static double hz2bark(double freqInHz) {
		// if should be: return 6 * asinh(f/600);
		double f = freqInHz / 600;
		return 6 * Math.log(f + Math.sqrt((f * f) + 1));
	}

	public static double freq2barkNew(double freqInHz) {
		if (freqInHz >= 605.0)
			return 13.0 * Math.atan(0.00076 * freqInHz); // 5.60265754
		else
			return 8.7 + 14.2 * Math.log10(1e-50 + freqInHz / 1000.0); // 5.60092632
	}

	public static double barkNew2freq(double barkNew) {
		if (barkNew >= 5.6017) // Roughly average of the above two values for 605.0 Hz in freq2barkNew
			return (Math.tan(barkNew / 13.0)) / 0.00076;
		else
			return 1000.0 * Math.pow(10.0, (barkNew - 8.7) / 14.2);
	}

	public static double[] bark2freq(double[] barks, int samplingRateInHz) {
		double[] freqsInHz = new double[barks.length];
		for (int i = 0; i < barks.length; i++)
			freqsInHz[i] = bark2freq(barks[i], samplingRateInHz);

		return freqsInHz;
	}

	public static double bark2freq(double bark, int samplingRateInHz) {
		double midFreqInHz = 0.25 * samplingRateInHz;
		double stepInHz = 0.5 * 0.25 * samplingRateInHz;
		double midFreqInBark = SignalProcUtils.freq2bark(midFreqInHz);
		while (Math.abs(midFreqInBark - bark) > 1e-10) {
			if (midFreqInBark < bark)
				midFreqInHz += stepInHz;
			else
				midFreqInHz -= stepInHz;

			stepInHz *= 0.5;
			midFreqInBark = SignalProcUtils.freq2bark(midFreqInHz);
		}

		return midFreqInHz;
	}

	public static double barkNew2radian(double bark, int samplingRateInHz) {
		return SignalProcUtils.hz2radian(barkNew2freq(bark), samplingRateInHz);
	}

	/***
	 * Java ported version of: wts = fft2barkmx(nfft, sr, nfilts, width) Generate a matrix of weights to combine FFT bins into
	 * Bark bins. nfft defines the source FFT size at sampling rate sr. Optional nfilts specifies the number of output bands
	 * required (else one per bark), and width is the constant width of each band in Bark (default 1). While wts has nfft columns,
	 * the second half are all zero. Hence, Bark spectrum is fft2barkmx(nfft,sr)*abs(fft(xincols,nfft)); 2004-09-05
	 * dpwe@ee.columbia.edu based on rastamat/audspec.m
	 * 
	 * @param nfft
	 *            FFT size
	 * @param sr
	 *            sampling rate
	 * @param nfilts
	 *            number of output bark bands
	 * @param width
	 *            width of each band in Bark (default 1)
	 * @param minfreq
	 *            min frequency
	 * @param maxfreq
	 *            max frequency
	 * @return wts
	 */
	public static double[][] fft2barkmx(int nfft, int sr, int nfilts, int width, double minfreq, double maxfreq) {

		int i, j, k;
		double min_bark = hz2bark(minfreq);
		double nyqbark = hz2bark(maxfreq) - min_bark;

		double wts[][] = new double[nfilts][nfft];
		for (i = 0; i < nfilts; i++)
			wts[i] = MathUtils.zeros(nfft);

		// bark per filter
		double step_barks = nyqbark / (nfilts - 1);

		// Frequency of each FFT bin in Bark
		int halfNfft = (nfft / 2);
		double binbarks[] = new double[(halfNfft + 1)];
		for (i = 0; i < (halfNfft + 1); i++) {
			binbarks[i] = hz2bark(i * sr / nfft);
		}

		double f_bark_mid, aux;
		double lof[] = new double[(halfNfft + 1)];
		double hif[] = new double[(halfNfft + 1)];
		for (i = 1; i <= nfilts; i++) {
			f_bark_mid = min_bark + (i - 1) * step_barks;
			// Linear slopes in log-space (i.e. dB) intersect to trapezoidal window
			for (j = 0; j < (halfNfft + 1); j++) {
				lof[j] = (binbarks[j] - f_bark_mid) / width - 0.5;
				hif[j] = (binbarks[j] - f_bark_mid) / width + 0.5;
			}
			for (k = 0; k < (halfNfft + 1); k++) {
				aux = Math.min(0, Math.min(hif[k], (-2.5 * lof[k])));
				wts[i - 1][k] = Math.pow(10, aux);
			}
		}

		return wts;
	}

	// Convert frequency in Hz to frequency sample index
	// maxFreq corresponds to half sampling rate, i.e. sample no: fftSize/2+1 where freq sample indices are 0,1,...,maxFreq-1
	public static int[] freq2index(double[] freqsInHz, int samplingRateInHz, int maxFreq) {
		int[] inds = null;

		if (freqsInHz != null && freqsInHz.length > 0) {
			inds = new int[freqsInHz.length];

			for (int i = 0; i < inds.length; i++)
				inds[i] = freq2index(freqsInHz[i], samplingRateInHz, maxFreq);
		}

		return inds;
	}

	// maxFreqIndex: Actually we have indices from 0,...,maxFreqIndex-1
	public static int freq2index(double freqInHz, double samplingRateInHz, int maxFreqIndex) {
		int index = (int) Math.floor(freqInHz / (0.5 * samplingRateInHz) * (maxFreqIndex - 1) + 0.5);
		index = (int) Math.max(0, index);
		index = (int) Math.min(index, maxFreqIndex);

		return index;
	}

	// Convert frequency in Hz to frequency sample index
	// maxFreq corresponds to half sampling rate, i.e. sample no: fftSize/2+1 where freq sample indices are 0,1,...,maxFreq-1
	public static double[] freq2indexDouble(double[] freqsInHz, double samplingRateInHz, int maxFreq) {
		double[] inds = null;

		if (freqsInHz != null && freqsInHz.length > 0) {
			inds = new double[freqsInHz.length];

			for (int i = 0; i < inds.length; i++)
				inds[i] = freq2indexDouble(freqsInHz[i], samplingRateInHz, maxFreq);
		}

		return inds;
	}

	public static double freq2indexDouble(double freqInHz, double samplingRateInHz, int maxFreqIndex) {
		double index = freqInHz / (0.5 * samplingRateInHz) * (maxFreqIndex - 1);
		index = Math.max(0, index);
		index = Math.min(index, maxFreqIndex);

		return index;
	}

	// Convert a zero based spectrum index value to frequency in Hz
	// If fftSize is 512, zeroBasedMaxFreqIndex=256 is the last index
	public static double index2freq(int zeroBasedFreqIndex, int samplingRateInHz, int zeroBasedMaxFreqIndex) {
		return zeroBasedFreqIndex * (0.5 * samplingRateInHz) / zeroBasedMaxFreqIndex;
	}

	// Convert a zero based spectrum index value to frequency in Hz
	public static double indexDouble2freq(double zeroBasedFreqIndex, int samplingRateInHz, int zeroBasedMaxFreqIndex) {
		return zeroBasedFreqIndex * (0.5 * samplingRateInHz) / zeroBasedMaxFreqIndex;
	}

	// Convert sample index to time value in seconds
	public static float sample2time(int sample, int samplingRate) {
		return ((float) sample) / samplingRate;
	}

	public static float sampleFloat2time(float sample, int samplingRate) {
		return sample / samplingRate;
	}

	public static float sample2time(long sample, int samplingRate) {
		return (float) (((double) sample) / ((double) samplingRate));
	}

	public static float sample2time(float sample, int samplingRate) {
		return sample / samplingRate;
	}

	// Convert time value in seconds to sample index
	public static int time2sample(float time, int samplingRate) {
		return (int) Math.floor(time * samplingRate + 0.5f);
	}

	public static int[] time2sample(float[] times, int samplingRate) {
		int[] samples = null;
		if (times != null && times.length > 0) {
			samples = new int[times.length];
			for (int i = 0; i < times.length; i++)
				samples[i] = time2sample(times[i], samplingRate);
		}

		return samples;
	}

	public static int time2sample(double time, int samplingRate) {
		return (int) Math.floor(time * samplingRate + 0.5);
	}

	public static int[] time2sample(double[] times, int samplingRate) {
		int[] samples = null;
		if (times != null && times.length > 0) {
			samples = new int[times.length];
			for (int i = 0; i < times.length; i++)
				samples[i] = time2sample(times[i], samplingRate);
		}

		return samples;
	}

	public static double time2sampleDouble(double time, int samplingRate) {
		return time * samplingRate;
	}

	// Convert sample indices to time values in seconds
	public static float[] samples2times(int[] samples, int samplingRate) {
		float[] times = null;

		if (samples != null && samples.length > 0) {
			times = new float[samples.length];
			for (int i = 0; i < samples.length; i++)
				times[i] = ((float) samples[i]) / samplingRate;
		}

		return times;
	}

	// Convert time values in seconds to sample indices
	public static int[] times2samples(float[] times, int samplingRate) {
		int[] samples = null;

		if (times != null && times.length > 0) {
			samples = new int[times.length];
			for (int i = 0; i < samples.length; i++)
				samples[i] = (int) Math.floor(times[i] * samplingRate + 0.5f);
		}

		return samples;
	}

	// Find the scaled version of a given time value t by performing time varying scaling using
	// scales s at times given by alphas
	// t: instant of time which we want to convert to the new time scale
	// s: time scale factors at times given by alphas
	// alphas: time instants at which the time scale modification factor is the corresponding entry in s
	public static float timeScaledTime(float t, float[] scales, float[] times) {
		assert scales != null;
		if (times != null)
			assert scales.length == times.length;

		int N = scales.length;
		float tNew = t;
		int i;

		if (N > 0) {
			if (times == null || t <= times[0])
				tNew = t * scales[0];
			else {
				int ind = -1; // greatest time index that t is greater than
				for (i = 0; i < N; i++) {
					if (t > times[i])
						ind = i;
					else
						break;
				}

				if (ind == -1)
					tNew = scales[0] * t;
				else {
					tNew = scales[0] * times[0];
					for (i = 0; i < ind; i++)
						tNew += scales[i + 1] * (times[i + 1] - times[i]);

					tNew += scales[ind] * (t - times[ind]);
				}
			}
		}

		return tNew;
	}

	// Find the scaled version of a set of time values times by performing time varying scaling using
	// tScales at times given by tScalesTimes
	public static float[] timeScaledTimes(float[] times, float[] tScales, float[] tScalesTimes) {
		float[] newTimes = null;

		if (times != null && times.length > 0) {
			newTimes = new float[times.length];

			for (int i = 0; i < times.length; i++)
				newTimes[i] = timeScaledTime(times[i], tScales, tScalesTimes);
		}

		return newTimes;
	}

	// Time scale a pitch contour as specified by a time varying pattern tScales and tScalesTimes
	// f0s: f0 values
	// ws: Window size in seconds in f0 analysis
	// ss: Skip size in seconds in f0 analysis
	// tScales: time scale factors at times given by tScalesTimes
	// tScalesTimes: time instants at which the time scale modification factor is the corresponding entry in tScales
	public static double[] timeScalePitchContour(double[] f0s, float ws, float ss, float[] tScales, float[] tScalesTimes) {
		if (tScales == null || tScalesTimes == null)
			return f0s;

		assert tScales.length == tScalesTimes.length;

		int i, ind;

		// First compute the original time axis
		float[] times = new float[f0s.length];
		for (i = 0; i < f0s.length; i++)
			times[i] = i * ss + 0.5f * ws;

		float[] newTimes = timeScaledTimes(times, tScales, tScalesTimes);

		int numfrm = (int) Math.floor((newTimes[newTimes.length - 1] - 0.5 * ws) / ss + 0.5);

		double[] f0sNew = new double[numfrm];

		for (i = 0; i < numfrm; i++) {
			ind = MathUtils.findClosest(newTimes, i * ss + 0.5f * ws);
			f0sNew[i] = f0s[ind];
		}

		return f0sNew;
	}

	// Pitch scale a pitch contour as specified by a time varying pattern pScales and pScalesTimes
	// f0s: f0 values
	// ws: Window size in seconds in f0 analysis
	// ss: Skip size in seconds in f0 analysis
	// pScales: pitch scale factors at times given by pScalesTimes
	// pScalesTimes: time instants at which the pitch scale modification factor is the corresponding entry in pScales
	public static double[] pitchScalePitchContour(double[] f0s, float ws, float ss, float[] pScales, float[] pScalesTimes) {
		if (pScales == null || pScalesTimes == null)
			return f0s;

		assert pScales.length == pScalesTimes.length;

		int i, smallerCloseInd;
		float currentTime;
		float alpha;

		double[] f0sNew = new double[f0s.length];

		for (i = 0; i < f0s.length; i++) {
			currentTime = i * ss + 0.5f * ws;

			smallerCloseInd = MathUtils.findClosest(pScalesTimes, currentTime);

			if (pScalesTimes[smallerCloseInd] > currentTime)
				smallerCloseInd--;

			if (smallerCloseInd >= 0 && smallerCloseInd < pScales.length - 1) {
				alpha = (pScalesTimes[smallerCloseInd + 1] - currentTime)
						/ (pScalesTimes[smallerCloseInd + 1] - pScalesTimes[smallerCloseInd]);
				f0sNew[i] = (alpha * pScales[smallerCloseInd] + (1.0f - alpha) * pScales[smallerCloseInd + 1]) * f0s[i];
			} else {
				smallerCloseInd = Math.max(0, smallerCloseInd);
				smallerCloseInd = Math.min(smallerCloseInd, pScales.length - 1);
				f0sNew[i] = pScales[smallerCloseInd] * f0s[i];
			}
		}

		return f0sNew;
	}

	// Returns samples from a white noise process. The sample amplitudes are between [-0.5,0.5]
	public static double[] getNoise(double startFreqInHz, double endFreqInHz, double transitionBandwidthInHz,
			int samplingRateInHz, int len) {
		return getNoiseNormalizedFreqs(startFreqInHz / samplingRateInHz, endFreqInHz / samplingRateInHz, transitionBandwidthInHz
				/ samplingRateInHz, len);
	}

	public static double[] getNoiseNormalizedFreqs(double normalizedStartFreq, double normalizedEndFreq,
			double normalizedTransitionBandwidth, int len) {
		double[] noise = null;

		if (len > 0) {
			FIRFilter f = null;
			int origLen = len;
			noise = new double[origLen];

			if (normalizedStartFreq != 0.0 || normalizedEndFreq != 0.5) // else --> No filtering is required
			{
				if (normalizedStartFreq > 0.0 && normalizedEndFreq < 0.5) // Bandpass
					f = new BandPassFilter(normalizedStartFreq, normalizedEndFreq, normalizedTransitionBandwidth);
				else if (normalizedStartFreq > 0.0)
					f = new HighPassFilter(normalizedStartFreq, normalizedTransitionBandwidth);
				else if (normalizedEndFreq < 0.5f)
					f = new LowPassFilter(normalizedEndFreq, normalizedTransitionBandwidth);

				origLen = len;
				len += 3 * f.getImpulseResponseLength(); // This is for avoiding initial zeros in the beginning of the signal
			}

			if (f == null) {
				for (int i = 0; i < origLen; i++)
					noise[i] = Math.random() - 0.5;

				return noise;
			} else {
				double[] noise2 = new double[len];
				for (int i = 0; i < len; i++)
					noise2[i] = Math.random() - 0.5;

				noise2 = f.apply(noise2);

				System.arraycopy(noise2, f.getImpulseResponseLength(), noise, 0, origLen);
			}
		}

		return noise;
	}

	public static float radian2hz(float rad, int samplingRate) {
		return (float) ((rad / MathUtils.TWOPI) * samplingRate);
	}

	public static double radian2hz(double rad, int samplingRate) {
		return (rad / MathUtils.TWOPI) * samplingRate;
	}

	public static float hz2radian(float hz, int samplingRate) {
		return (float) (hz * MathUtils.TWOPI / samplingRate);
	}

	public static double hz2radian(double hz, int samplingRate) {
		return hz * MathUtils.TWOPI / samplingRate;
	}

	// Median filtering: All values in x are replaced by the median of the 3-closest context neighbours (i.e. the left value, the
	// value itself, and the right value)
	// The output y[k] is the median of x[k-1], x[k], and x[k+1]
	// All out-of-boundary values are assumed 0.0
	public static double[] medianFilter(double[] x) {
		return medianFilter(x, 3);
	}

	public static float[] medianFilter(float[] x) {
		return medianFilter(x, 3);
	}

	public static float[] medianFilter(float[] x, int N) {
		double[] x2 = new double[x.length];
		int i;
		for (i = 0; i < x.length; i++)
			x2[i] = x[i];

		x2 = medianFilter(x2, N);

		float[] y = new float[x.length];
		for (i = 0; i < x.length; i++)
			y[i] = (float) (x2[i]);

		return y;
	}

	// Median filtering: All values in x are replaced by the median of the N closest context neighbours and the value itself
	// If N is odd, the output y[k] is the median of x[k-(N-1)/2],...,x[k+(N-1)/2]
	// If N is even, the output y[k] is the median of x[k-(N/2)+1],...,x[k+(N/2)-1], i.e. the average of the (N/2-1)th and (N/2)th
	// of the sorted values
	public static double[] medianFilter(double[] x, int N) {
		double[] y = new double[x.length];
		Vector<Double> v = new Vector<Double>();

		int k, j, midVal;

		if (N % 2 == 1) // Odd version
		{
			midVal = (N - 1) / 2;

			for (k = 0; k < x.length; k++) {
				/*
				 * for (j=0; j<midVal; j++) { if (k-j>=0) v.add(x[k-j]); else v.add(leftOutOfBound); }
				 * 
				 * for (j=midVal; j<N; j++) { if (k+j<x.length) v.add(x[k+j]); else v.add(rightOutOfBound); }
				 */
				// MS, 27.2.09: Ignore left/right out of bound; use window of size N, centered around current point
				// if possible but staying within the range of data.
				int iLeft = Math.max(0, k - midVal);
				int iRight = Math.min(x.length - 1, iLeft + N);
				for (j = iLeft; j <= iRight; j++) {
					v.add(x[j]);
				}
				Collections.sort(v);

				y[k] = ((Double) (v.get(midVal))).doubleValue();

				v.clear();
			}
		} else // Even version
		{
			midVal = N / 2 - 1;

			for (k = 0; k < x.length; k++) {
				/*
				 * for (j=0; j<=midVal; j++) { if (k-j>=0) v.add(x[k-j]); else v.add(leftOutOfBound); }
				 * 
				 * for (j=midVal+1; j<N; j++) { if (k+j<x.length) v.add(x[k+j]); else v.add(rightOutOfBound); }
				 */
				// MS, 27.2.09: Ignore left/right out of bound; use window of size N, centered around current point
				// if possible but staying within the range of data.
				int iLeft = Math.max(0, k - midVal);
				int iRight = Math.min(x.length - 1, k + midVal);
				for (j = iLeft; j <= iRight; j++) {
					v.add(x[j]);
				}

				Collections.sort(v);

				if (midVal + 1 < v.size())
					y[k] = 0.5 * (((Double) (v.get(midVal))).doubleValue() + ((Double) (v.get(midVal + 1))).doubleValue());
				else
					y[k] = ((Double) (v.get(midVal))).doubleValue();

				v.clear();
			}
		}

		return y;
	}

	public static double[] meanFilter(double[] x, int N) {
		return meanFilter(x, N, x[0], x[x.length - 1]);
	}

	public static float[] meanFilter(float[] x, int N) {
		return meanFilter(x, N, x[0], x[x.length - 1]);
	}

	public static float[] meanFilter(float[] x, int N, float leftOutOfBound, float rightOutOfBound) {
		double[] xd = ArrayUtils.copyFloat2Double(x);

		xd = meanFilter(xd, N, (double) leftOutOfBound, (double) rightOutOfBound);

		return ArrayUtils.copyDouble2Float(xd);
	}

	// Mean filtering: All values in x are replaced by the mean of the N closest context neighbours and the value itself
	// If N is odd, the output y[k] is the mean of x[k-(N-1)/2],...,x[k+(N-1)/2]
	// If N is even, the output y[k] is the mean of x[k-(N/2)+1],...,x[k+(N/2)-1]
	// The out-of-boundary values are assumed leftOutOfBound for k-i<0 and rightOutOfBound for k+i>x.length-1
	public static double[] meanFilter(double[] x, int N, double leftOutOfBound, double rightOutOfBound) {
		double[] y = new double[x.length];
		Vector<Double> v = new Vector<Double>();

		int k, j, midVal;

		if (N % 2 == 1) // Odd version
		{
			midVal = (N - 1) / 2;

			for (k = 0; k < x.length; k++) {
				for (j = 0; j < midVal; j++) {
					if (k - j >= 0)
						v.add(x[k - j]);
					else
						v.add(leftOutOfBound);
				}

				for (j = midVal; j < N; j++) {
					if (k + j < x.length)
						v.add(x[k + j]);
					else
						v.add(rightOutOfBound);
				}

				y[k] = mean(v);

				v.clear();
			}
		} else // Even version
		{
			midVal = N / 2 - 1;

			for (k = 0; k < x.length; k++) {
				for (j = 0; j <= midVal; j++) {
					if (k - j >= 0)
						v.add(x[k - j]);
					else
						v.add(leftOutOfBound);
				}

				for (j = midVal + 1; j < N; j++) {
					if (k + j < x.length)
						v.add(x[k + j]);
					else
						v.add(rightOutOfBound);
				}

				y[k] = mean(v);

				v.clear();
			}
		}

		return y;
	}

	public static double mean(Vector<Double> v) {
		double m = 0.0;

		for (int i = 0; i < v.size(); i++)
			m += ((Double) (v.get(i))).doubleValue();

		m /= v.size();

		return m;
	}

	public static float frameIndex2Time(int zeroBasedFrameIndex, float windowSizeInSeconds, float skipSizeInSeconds) {
		return Math.max(0.0f, 0.5f * windowSizeInSeconds + zeroBasedFrameIndex * skipSizeInSeconds);
	}

	public static double frameIndex2Time(int zeroBasedFrameIndex, double windowSizeInSeconds, double skipSizeInSeconds) {
		return Math.max(0.0, 0.5 * windowSizeInSeconds + zeroBasedFrameIndex * skipSizeInSeconds);
	}

	public static int time2frameIndex(float time, float windowSizeInSeconds, float skipSizeInSeconds) {
		return (int) Math.max(0, Math.floor((time - 0.5f * windowSizeInSeconds) / skipSizeInSeconds + 0.5));
	}

	public static int time2frameIndex(double time, double windowSizeInSeconds, double skipSizeInSeconds) {
		return (int) Math.max(0, Math.floor((time - 0.5 * windowSizeInSeconds) / skipSizeInSeconds + 0.5));
	}

	// Center-clipping using the amount <ratio>
	// Valid values of ratio are in the range [0.0,1.0]
	// greater values result in more clipping (i.e. with 1.0 you will get all zeros at the output)
	public static void centerClip(double[] x, double ratio) {
		if (ratio < 0.0)
			ratio = 0.0;
		if (ratio > 1.0)
			ratio = 1.0;

		double positiveMax = MathUtils.getMax(x);
		double negativeMax = MathUtils.getMin(x);
		double positiveTh = positiveMax * ratio;
		double negativeTh = negativeMax * ratio;

		for (int i = 0; i < x.length; i++) {
			if (x[i] > positiveTh)
				x[i] -= positiveTh;
			else if (x[i] < negativeTh)
				x[i] -= negativeTh;
			else
				x[i] = 0.0;
		}
	}

	public static double[] getVoiceds(double[] f0s) {
		double[] voiceds = null;

		if (f0s != null) {
			int totalVoiceds = 0;
			int i;

			for (i = 0; i < f0s.length; i++) {
				if (f0s[i] > 10.0)
					totalVoiceds++;
			}

			if (totalVoiceds > 0) {
				voiceds = new double[totalVoiceds];
				int count = 0;
				for (i = 0; i < f0s.length; i++) {
					if (f0s[i] > 10.0)
						voiceds[count++] = f0s[i];

					if (count >= totalVoiceds)
						break;
				}
			}
		}

		return voiceds;
	}

	// Convert an f0 contour into a log-f0 contour by handling unvoiced parts specially
	// The unvoiced values (i.e. f0 values less than or equal to 10 Hz are set to 0.0
	public static double[] getLogF0s(double[] f0s) {
		return MathUtils.log(f0s, 10.0, 0.0);
	}

	// Inverse of getLogF0s functions
	// i.e. log f0 values are converted to values in Hz with special handling of unvoiceds
	public static double[] getExpF0s(double[] logF0s) {
		double[] f0s = null;

		if (logF0s != null) {
			f0s = new double[logF0s.length];

			for (int i = 0; i < f0s.length; i++) {
				if (logF0s[i] > Math.log(10.0))
					f0s[i] = Math.exp(logF0s[i]);
				else
					f0s[i] = 0.0;
			}
		}

		return f0s;
	}

	public static double getF0Range(double[] f0s) {
		return getF0Range(f0s, 0.10, 0.10);
	}

	public static double getF0Range(double[] f0s, double percentileMin, double percentileMax) {
		double range = 0.0;

		double[] voiceds = SignalProcUtils.getVoiceds(f0s);

		if (voiceds != null) {
			if (percentileMin < 0.0)
				percentileMin = 0.0;
			if (percentileMin > 1.0)
				percentileMin = 1.0;
			if (percentileMax < 0.0)
				percentileMax = 0.0;
			if (percentileMax > 1.0)
				percentileMax = 1.0;

			MathUtils.quickSort(voiceds);
			int ind1 = (int) Math.floor(voiceds.length * percentileMin + 0.5);
			int ind2 = (int) Math.floor(voiceds.length * (1.0 - percentileMax) + 0.5);
			range = Math.max(0.0, voiceds[ind2] - voiceds[ind1]);
		}

		return range;
	}

	public static int frameIndex2LabelIndex(int zeroBasedFrameIndex, Labels labels, double windowSizeInSeconds,
			double skipSizeInSeconds) {
		double time = zeroBasedFrameIndex * skipSizeInSeconds + 0.5 * windowSizeInSeconds;

		return time2LabelIndex(time, labels);
	}

	public static int time2LabelIndex(double time, Labels labels) {
		int index = 0;

		for (int i = 0; i < labels.items.length; i++) {
			if (labels.items[i].time > time) {
				index = i;
				break;
			}
		}

		if (index < 0)
			index = 0;
		if (index > labels.items.length - 1)
			index = labels.items.length - 1;

		return index;

	}

	public static double getRmsDistance(double[] x, double[] y) {
		double rmsDist = 0.0;

		for (int i = 0; i < Math.min(x.length, y.length); i++)
			rmsDist += (x[i] - y[i]) * (x[i] - y[i]);

		rmsDist /= Math.min(x.length, y.length);
		rmsDist = Math.sqrt(rmsDist);

		return rmsDist;
	}

	public static int[] merge(int[] x1, int[] x2) {
		int[] y = null;
		int ylen = 0;
		if (x1 != null)
			ylen += x1.length;
		if (x2 != null)
			ylen += x2.length;
		y = new int[ylen];
		int pos = 0;
		if (x1 != null) {
			System.arraycopy(x1, 0, y, 0, x1.length);
			pos += x1.length;
		}

		if (x2 != null)
			System.arraycopy(x2, 0, y, pos, x2.length);

		return y;
	}

	public static double[] merge(double[] x1, double[] x2) {
		double[] y = null;
		int ylen = 0;
		if (x1 != null)
			ylen += x1.length;
		if (x2 != null)
			ylen += x2.length;
		y = new double[ylen];
		int pos = 0;
		if (x1 != null) {
			System.arraycopy(x1, 0, y, 0, x1.length);
			pos += x1.length;
		}

		if (x2 != null)
			System.arraycopy(x2, 0, y, pos, x2.length);

		return y;
	}

	// Decimate data by a factor D
	// For non-integer index values, linear interpolation is performed
	public static double[] decimate(double[] x, double D) {
		double[] y = null;

		if (x != null) {
			int ylen = (int) Math.floor(x.length / D + 0.5);
			y = new double[ylen];
			double dind = 0.5 * D;
			int total = 0;
			int ind1, ind2;
			while (total < ylen) {
				ind1 = (int) Math.floor(dind);
				ind2 = ind1 + 1;
				if (ind2 > x.length - 1)
					ind2 = x.length - 1;
				ind1 = ind2 - 1;

				y[total++] = (ind2 - dind) * x[ind1] + (dind - ind1) * x[ind2];

				dind += D;
			}
		}

		return y;
	}

	// Interpolate data by a factor D
	// For non-integer index values, linear interpolation is performed
	public static double[] interpolate(double[] x, double D) {
		double[] y = null;

		if (x != null) {
			int ylen = (int) Math.floor(x.length * D + 0.5);
			y = new double[ylen];
			int xind;
			double xindDouble;
			for (int i = 0; i < ylen; i++) {
				xindDouble = i / D;
				xind = (int) Math.floor(xindDouble);

				if (xind <= 0)
					y[i] = (xind - xindDouble) * (2 * x[0] - x[1]) + (1 - xind + xindDouble) * x[xind];
				else if (xind > x.length - 1)
					y[i] = (x[x.length - 1] - x[x.length - 2]) * (xindDouble - x.length + 1) + x[x.length - 1];
				else
					y[i] = (xind - xindDouble) * x[xind - 1] + (1 - xind + xindDouble) * x[xind];
			}
		}

		return y;
	}

	public static double energy(double[] x) {
		double e = 0.0;
		for (int i = 0; i < x.length; i++)
			e += x[i] * x[i];

		return e;
	}

	public static double[] filter(double[] b, double[] x) {
		double[] a = new double[1];
		a[0] = 1.0;

		return filter(b, a, x);
	}

	public static double[] filter(double[] b, double[] a, double[] x) {
		return filter(b, a, x, false);
	}

	public static double[] filter(double[] b, double[] x, boolean bNormalize) {
		double[] a = new double[1];
		a[0] = 1.0;

		return filter(b, a, x, bNormalize);
	}

	public static double[] filter(double[] b, double[] a, double[] x, boolean bNormalize) {
		double[] zi = new double[Math.max(a.length, b.length) - 1];
		Arrays.fill(zi, 0.0);

		return filter(b, a, x, bNormalize, zi);
	}

	public static double[] filter(double[] b, double[] x, boolean bNormalize, double[] zi) {
		double[] a = new double[1];
		a[0] = 1.0;

		return filter(b, a, x, bNormalize, zi);
	}

	// Time domain digital filtering
	// a[0]*y[n] = b[0]*x[n] + b[1]*x[n-1] + ... + b[nb]*x[n-nb]
	// - a[1]*y[n-1] - ... - a[na]*y[n-na]
	// b and a are filter coefficients (impulse response of the filter)
	// If bNormalize is true, all the coeffs are normalized with a[0].
	// The initial conditions should be specified in zi.
	// Setting zi to all zeroes causes no initial conditions to be used.
	// Length of zi should be max(a.length, b.length)-1.
	public static double[] filter(double[] b, double[] a, double[] x, boolean bNormalize, double[] zi) {
		int n;
		double x_terms;
		double y_terms;
		int ind;

		double[] y = new double[x.length];

		int nb = b.length - 1;
		int na = a.length - 1;

		if (bNormalize) {
			// Normalize with a[0] first
			if (a[0] != 1.0) {
				for (n = 0; n < b.length; n++)
					b[n] /= a[0];
				for (n = 0; n < a.length; n++)
					a[n] /= a[0];
			}
		}

		for (n = 0; n < x.length; n++) {
			x_terms = 0.0;
			for (ind = n; ind > n - nb - 1; ind--) {
				if (ind >= 0)
					x_terms += b[n - ind] * x[ind];
				else
					x_terms += b[n - ind] * zi[-ind - 1];
			}

			y_terms = 0.0;
			for (ind = n - 1; ind > n - na - 1; ind--) {
				if (ind >= 0)
					y_terms += -a[n - ind] * y[ind];
			}

			y[n] = x_terms + y_terms;
		}

		return y;
	}

	// Time domain digital filtering with phase distortion correction
	// using filter denumerator b. The numerator is 1.0, i.e. applies FIR filtering
	public static double[] filtfilt(double[] b, double[] x) {
		double[] a = new double[1];
		a[0] = 1.0;

		return filtfilt(b, a, x);
	}

	// Time domain digital filtering with phase distortion correction
	// using filter denumerator b and numerator a
	public static double[] filtfilt(double[] b, double[] a, double[] x) {
		int nfilt;
		int i, j;
		double[] tmpb = null;
		double[] tmpa = null;

		if (b.length > a.length) {
			nfilt = b.length;
			tmpb = new double[nfilt];
			tmpa = new double[nfilt];

			for (i = 0; i < b.length; i++)
				tmpb[i] = b[i];
			for (i = 0; i < a.length; i++)
				tmpa[i] = a[i];
			for (i = a.length; i < nfilt; i++)
				tmpa[i] = 0.0;
		} else {
			nfilt = a.length;
			tmpb = new double[nfilt];
			tmpa = new double[nfilt];

			for (i = 0; i < a.length; i++)
				tmpa[i] = a[i];
			for (i = 0; i < b.length; i++)
				tmpb[i] = b[i];
			for (i = b.length; i < nfilt; i++)
				tmpb[i] = 0.0;
		}

		int nfact = 3 * (nfilt - 1); // Length of edge transients
		int rwlen = 3 * nfilt - 5;

		int ylen = 2 * nfact + x.length;
		double[] y = new double[ylen];

		double[] yRet = null;
		if (!(x.length <= nfact)) // Input data too short!
		{
			// Solve system of linear equations for initial conditions
			// zi are the steady-state states of the filter b(z)/a(z) in the state-space
			// implementation of the 'filter' command.
			int[] rows = new int[rwlen];
			for (i = 0; i <= nfilt - 2; i++)
				rows[i] = i;
			for (i = nfilt - 1; i <= 2 * nfilt - 4; i++)
				rows[i] = i - nfilt + 2;
			for (i = 2 * nfilt - 3; i <= 3 * nfilt - 6; i++)
				rows[i] = i - 2 * nfilt + 3;

			int[] cols = new int[rwlen];
			for (i = 0; i <= nfilt - 2; i++)
				cols[i] = 0;
			for (i = nfilt - 1; i <= 2 * nfilt - 4; i++)
				cols[i] = i - nfilt + 2;
			for (i = 2 * nfilt - 3; i <= 3 * nfilt - 6; i++)
				cols[i] = i - 2 * nfilt + 4;

			double[] data = new double[rwlen];
			data[0] = 1.0 + tmpa[1];
			for (i = 1; i <= nfilt - 2; i++)
				data[i] = tmpa[i + 1];
			for (i = nfilt - 1; i <= 2 * nfilt - 4; i++)
				data[i] = 1.0;
			for (i = 2 * nfilt - 3; i <= 3 * nfilt - 6; i++)
				data[i] = -1.0;

			int N = nfilt - 1;
			double[][] sp = new double[N][N];

			for (i = 0; i < N; i++) {
				for (j = 0; j < N; j++)
					sp[i][j] = 0.0f;
			}

			for (i = 0; i < rwlen; i++)
				sp[rows[i]][cols[i]] = data[i];

			double[] denum = new double[N];
			for (i = 0; i < N; i++)
				denum[i] = 0.0;
			for (i = 2; i < nfilt + 1; i++)
				denum[i - 2] = b[i - 1] - tmpa[i - 1] * b[0];

			double[] zi = new double[N];
			for (i = 0; i < N; i++)
				zi[i] = 0.0;

			sp = MathUtils.inverse(sp);

			double tmp;
			for (i = 0; i < N; i++) {
				tmp = 0.0;

				for (j = 0; j < N; j++)
					tmp += sp[i][j] * denum[i];

				zi[i] = tmp;
			}

			// Extrapolate beginning and end of data sequence using a "reflection
			// method". Slopes of original and extrapolated sequences match at
			// the end points.
			// This reduces end effects.
			for (i = 0; i < nfact; i++)
				y[i] = 2 * x[0] - x[nfact - i];

			for (i = 0; i < x.length; i++)
				y[i + nfact] = x[i];

			for (i = 0; i < nfact; i++)
				y[nfact + x.length + i] = 2 * x[x.length - 1] - x[x.length - 2 - i];

			// Filter, reverse the data, filter again, and reverse the data again
			for (i = 0; i < N; i++)
				zi[i] = zi[i] * y[0];

			y = filter(tmpb, tmpa, y, false, zi);
			y = SignalProcUtils.reverse(y);

			y = filter(tmpb, tmpa, y, false, zi);
			y = SignalProcUtils.reverse(y);

			// remove extrapolated pieces of y to write the output to x
			yRet = new double[x.length];
			for (i = 0; i < x.length; i++)
				yRet[i] = y[i + nfact];
		} else {
			yRet = filter(b, a, x);
		}

		return yRet;
	}

	public static double[] filterfd(double[] filterFFTAbsMag, double[] x, double samplingRate) {
		return filterfd(filterFFTAbsMag, x, samplingRate, 0.020);
	}

	public static double[] filterfd(double[] filterFFTAbsMag, double[] x, double samplingRate, double winsize) {
		return filterfd(filterFFTAbsMag, x, samplingRate, winsize, 0.010);
	}

	public static double[] filterfd(double[] filterFFTAbsMag, double[] x, double samplingRate, double winsize, double skipsize) {
		double[] y = null;

		if (x != null && filterFFTAbsMag != null) {
			int ws = (int) Math.floor(winsize * samplingRate + 0.5);
			int ss = (int) Math.floor(skipsize * samplingRate + 0.5);

			int maxFreq = filterFFTAbsMag.length;
			int fftSize = 2 * (maxFreq - 1);
			if (ws > fftSize)
				ws = fftSize;

			int numfrm = (int) Math.floor((x.length - 0.5 * ws) / ss + 0.5) + 1;
			HammingWindow wgt = new HammingWindow(ws);
			wgt.normalize(1.0f);

			y = new double[x.length];
			Arrays.fill(y, 0.0);
			double[] w = new double[x.length];
			Arrays.fill(w, 0.0);
			int i, j;

			ComplexArray XFRM = new ComplexArray(fftSize);
			double[] yfrm = new double[ws];

			for (i = 1; i <= numfrm; i++) {
				Arrays.fill(XFRM.real, 0.0);
				Arrays.fill(XFRM.imag, 0.0);
				for (j = 1; j <= Math.min(ws, x.length - (i - 1) * ss); j++)
					XFRM.real[j - 1] = x[(i - 1) * ss + j - 1];

				wgt.applyInline(XFRM.real, 0, ws);

				FFT.transform(XFRM.real, XFRM.imag, false);

				for (j = 0; j < maxFreq; j++) {
					XFRM.real[j] *= filterFFTAbsMag[j];
					XFRM.imag[j] *= filterFFTAbsMag[j];
				}

				for (j = maxFreq + 1; j <= fftSize; j++) {
					XFRM.real[j - 1] = XFRM.real[2 * maxFreq - 1 - j];
					XFRM.imag[j - 1] = -XFRM.imag[2 * maxFreq - 1 - j];
				}

				FFT.transform(XFRM.real, XFRM.imag, true);
				System.arraycopy(XFRM.real, 0, yfrm, 0, ws);

				for (j = (i - 1) * ss + 1; j <= Math.min(x.length, (i - 1) * ss + ws); j++) {
					y[j - 1] += wgt.value(j - (i - 1) * ss - 1) * yfrm[j - (i - 1) * ss - 1];
					w[j - 1] += wgt.value(j - (i - 1) * ss - 1) * wgt.value(j - (i - 1) * ss - 1);
				}
			}

			for (i = 1; i <= x.length; i++) {
				if (w[i - 1] > 0.0)
					y[i - 1] /= w[i - 1];
			}
		}

		return y;
	}

	public static void addWhiteNoise(double[] x, double level) {
		for (int i = 0; i < x.length; i++)
			x[i] += level * Math.random();
	}

	public static double[] getWhiteNoise(int totalSamples, double maxAbsGain) {
		double[] n = null;

		if (totalSamples > 0) {
			n = new double[totalSamples];
			for (int i = 0; i < totalSamples; i++)
				n[i] = 2.0 * maxAbsGain * (Math.random() - 0.5);
		}

		return n;
	}

	public static double[] getWhiteNoiseOfVariance(int totalSamples, double variance) {
		double[] n = getWhiteNoise(totalSamples, 1.0);
		MathUtils.adjustVariance(n, variance);

		return n;
	}

	public static double[] getWhiteNoiseOfMeanVariance(int totalSamples, double mean, double variance) {
		double[] n = getWhiteNoise(totalSamples, 1.0);
		MathUtils.adjustMean(n, mean);
		MathUtils.adjustVariance(n, variance);

		return n;
	}

	// These functions implement cepstrum computations asdescribed in van Santen et. al.´s book - Chapter 5
	// (van Santen, et. al., Progress in Speech Synthesis)
	public static float[] specLinear2cepstrum(double[] specLinear, int cepsOrder) {
		int fftSizeFromLen = 2 * (specLinear.length - 1);
		int fftSize = 2;
		while (fftSize < fftSizeFromLen)
			fftSize *= 2;

		// double[] specLog = MathUtils.log10(specLinear);
		double[] specLog = MathUtils.log(specLinear);
		double[] real = new double[fftSize];
		double[] imag = new double[fftSize];
		Arrays.fill(real, 0.0);
		Arrays.fill(imag, 0.0);
		System.arraycopy(specLog, 0, real, 0, Math.min(specLog.length, fftSize));

		FFT.transform(real, imag, true);

		float[] nceps = new float[cepsOrder + 1];
		for (int i = 0; i < cepsOrder + 1; i++)
			nceps[i] = (float) real[i];

		return nceps;
	}

	public static double cepstrum2linearSpecAmp(float[] ceps, float freqInRadians) {
		double logM = ceps[0];
		for (int m = 1; m < ceps.length; m++)
			logM += 2 * ceps[m] * Math.cos(m * freqInRadians);

		// return Math.pow(10.0, logM);
		return Math.exp(logM);
	}

	public static double cepstrum2minimumPhase(float[] ceps, float freqInRadians) {
		double minPhase = 0.0;
		for (int m = 1; m < ceps.length; m++)
			minPhase -= 2 * ceps[m] * Math.sin(m * freqInRadians);

		return minPhase;
	}

	public static double getMaximumFreqOfVoicingInHz(double[] specAmpsLinear, int[] peakInds, int[][] freqBandInds,
			int samplingRate) {
		// freqBandInds is an Nx2 array, [0][0]: left index of first band, [0][1]: right index of first band, etc
		// use overlapping bands in frequency

		double maximumFreqOfVoicingInHz = 0.5 * samplingRate;
		int i;
		for (i = 0; i < freqBandInds.length; i++) {
			// TO DO:
			// -Find peakInds in this range
			// -Compute total energy of non-peaks in this range and divide by total energy in this range
			// -Take log of the value
			// -Compare with threshold and declare voicingfor this band
		}

		// 3-point Median filter voicing decisions
		// Find last "voiced"
		// Declare frequency of voicing as the end of the last voiced band

		return maximumFreqOfVoicingInHz;
	}

	// Converts half-left part of the spectrum into a full spectrum as follows:
	// fullSpectrum[0 ... maxFreq-1] = halfSpectrum[0 ... maxFreq-1]
	// fullSpectrum[maxFreq ... fftSize-1] = halfSpectrum[maxFreq-2 ... 1]
	// where maxFreq-1 = halfSpectrum.length
	// and fftSize = 2*(maxFreq-1);
	public static double[] spectralMirror(double[] halfSpectrum) {
		int maxFreq = halfSpectrum.length + 1;
		int fftSize = fullSpectrumSize(maxFreq);
		double[] fullSpectrum = new double[fftSize];

		int k;
		System.arraycopy(halfSpectrum, 0, fullSpectrum, 0, maxFreq - 1);
		for (k = maxFreq; k <= fftSize - 1; k++)
			fullSpectrum[k] = halfSpectrum[2 * maxFreq - k - 2];

		return fullSpectrum;
	}

	// Returns s1+s2 with length max(length(s1), length(s2))
	public static double[] addSignals(double[] s1, double[] s2) {
		int len = 0;
		if (s1 != null)
			len = s1.length;
		if (s2 != null && s2.length > len)
			len = s2.length;

		double[] y = null;
		if (len > 0)
			y = new double[len];

		if (s1 != null)
			System.arraycopy(s1, 0, y, 0, s1.length);

		if (s2 != null) {
			for (int i = 0; i < s2.length; i++)
				y[i] += s2[i];
		}

		return y;
	}

	// Returns gain1.s1+gain2.s2 with length max(length(s1), length(s2))
	public static double[] addSignals(double[] s1, double gain1, double[] s2, double gain2) {
		int len = 0;
		if (s1 != null)
			len = s1.length;
		if (s2 != null && s2.length > len)
			len = s2.length;

		double[] y = null;
		if (len > 0)
			y = new double[len];

		if (s1 != null)
			System.arraycopy(s1, 0, y, 0, s1.length);

		if (s2 != null) {
			for (int i = 0; i < s2.length; i++)
				y[i] = gain1 * y[i] + gain2 * s2[i];
		} else if (s1 != null) {
			for (int i = 0; i < s1.length; i++)
				y[i] = gain1 * y[i];
		}

		return y;
	}

	public static double[] subtractSignals(double[] s1, double[] s2) {
		return addSignals(s1, 1.0, s2, -1.0);
	}

	public static double[] arFilter(double[] x, double[] a, double lpGain) {
		return arFilter(x, a, lpGain, null);
	}

	public static double[] arFilter(double[] x, float[] a, double lpGain) {
		return arFilter(x, a, lpGain, null);
	}

	public static double[] arFilter(double[] x, float[] a, double lpGain, double[] yInitial) {
		double[] aDouble = ArrayUtils.copyFloat2Double(a);

		return arFilter(x, aDouble, lpGain, yInitial);
	}

	public static double[] arFilter(double[] x, double[] a, double lpGain, double[] yInitial) {
		double[] y = new double[x.length];
		int p = a.length;
		int n, k;
		for (n = 0; n < x.length; n++) {
			y[n] = lpGain * x[n];
			for (k = 1; k <= Math.min(p, n); k++)
				y[n] += a[k - 1] * y[n - k];

			if (yInitial != null) {
				for (k = n + 1; k <= p; k++)
					y[n] += a[k - 1] * yInitial[p - k + n];
			}
		}

		return y;
	}

	public static double[] arFilterFreqDomain(double[] windowedFrame, double[] a, double lpGain, double startFreqInHz,
			double endFreqInHz, int samplingRateInHz) {
		int k;
		int fftSize = 2;
		while (fftSize < windowedFrame.length)
			fftSize *= 2;
		int maxFreqInd = fftSize / 2;

		ComplexArray X = new ComplexArray(fftSize);
		System.arraycopy(windowedFrame, 0, X.real, 0, windowedFrame.length);

		if (MathUtils.isPowerOfTwo(fftSize))
			FFT.transform(X.real, X.imag, false);
		else
			X = FFTMixedRadix.fftComplex(X);

		double[] H = LpcAnalyser.calcSpecLinear(a, lpGain, fftSize);
		int startFreqInd = SignalProcUtils.freq2index(startFreqInHz, samplingRateInHz, maxFreqInd);
		int endFreqInd = SignalProcUtils.freq2index(endFreqInHz, samplingRateInHz, maxFreqInd);
		for (k = 0; k < startFreqInd; k++)
			H[k] = 0.0;
		for (k = endFreqInd + 1; k <= maxFreqInd; k++)
			H[k] = 0.0;

		ComplexArray Y = new ComplexArray(fftSize);

		for (k = 0; k <= maxFreqInd; k++) {
			Y.real[k] = X.real[k] * H[k];
			Y.imag[k] = X.imag[k] * H[k];
			;
		}

		for (k = maxFreqInd + 1; k < fftSize; k++) {
			Y.real[k] = Y.real[fftSize - k];
			Y.imag[k] = -1.0 * Y.imag[fftSize - k];
		}

		if (MathUtils.isPowerOfTwo(fftSize))
			FFT.transform(Y.real, Y.imag, true);
		else
			Y = FFTMixedRadix.ifft(Y);

		double[] y = new double[windowedFrame.length];
		for (k = 0; k < windowedFrame.length; k++)
			y[k] = Y.real[k];

		return y;
	}

	public static double[] fdFilter(double[] x, double[] filterFreqResponse) {
		int i;
		int maxFreqInd = filterFreqResponse.length - 1;
		int fftSize = 2 * maxFreqInd;

		ComplexArray frameDft = SignalProcUtils.getFrameDft(x, fftSize);

		for (i = 0; i <= maxFreqInd; i++) {
			frameDft.real[i] *= filterFreqResponse[i];
			frameDft.imag[i] *= filterFreqResponse[i];
		}

		for (i = maxFreqInd + 1; i < fftSize; i++) {
			frameDft.real[i] = frameDft.real[fftSize - i];
			frameDft.imag[i] = -1.0 * frameDft.imag[fftSize - i];
		}

		if (MathUtils.isPowerOfTwo(fftSize))
			FFT.transform(frameDft.real, frameDft.imag, true);
		else
			frameDft = FFTMixedRadix.ifft(frameDft);

		double[] y = new double[Math.min(x.length, frameDft.real.length)];
		for (i = 0; i < x.length; i++)
			y[i] = frameDft.real[i];

		return y;
	}

	public static double[] fdFilter(double[] x, float startFreqInHz, float endFreqInHz, int samplingRateInHz, int fftSize) {
		while (fftSize < x.length)
			fftSize *= 2;

		ComplexArray frameDft = SignalProcUtils.getFrameDft(x, fftSize);

		return fdFilter(frameDft, startFreqInHz, endFreqInHz, samplingRateInHz, x.length);
	}

	public static double[] fdFilter(ComplexArray frameDft, float startFreqInHz, float endFreqInHz, int samplingRateInHz,
			int origLen) {
		int fftSize = frameDft.real.length;
		int maxFreqInd = fftSize / 2;
		int startFreqInd = SignalProcUtils.freq2index(startFreqInHz, samplingRateInHz, maxFreqInd);
		int endFreqInd = SignalProcUtils.freq2index(endFreqInHz, samplingRateInHz, maxFreqInd);

		double[] y = null;

		float totalRmsEnergy = 0.0f;
		float passbandRmsEnergy = 0.0f;
		int totalPassbandSamples = 0;

		int i;

		for (i = 0; i <= startFreqInd; i++) {
			totalRmsEnergy += frameDft.real[i] * frameDft.real[i] + frameDft.imag[i] * frameDft.imag[i];
			frameDft.real[i] = 0.0;
			frameDft.imag[i] = 0.0;
		}

		for (i = startFreqInd + 1; i < endFreqInd; i++) {
			totalRmsEnergy += frameDft.real[i] * frameDft.real[i] + frameDft.imag[i] * frameDft.imag[i];
			passbandRmsEnergy += frameDft.real[i] * frameDft.real[i] + frameDft.imag[i] * frameDft.imag[i];
			totalPassbandSamples++;
		}

		for (i = endFreqInd; i <= maxFreqInd; i++) {
			totalRmsEnergy += frameDft.real[i] * frameDft.real[i] + frameDft.imag[i] * frameDft.imag[i];
			frameDft.real[i] = 0.0;
			frameDft.imag[i] = 0.0;
		}

		for (i = maxFreqInd + 1; i < fftSize; i++) {
			frameDft.real[i] = frameDft.real[fftSize - i];
			frameDft.imag[i] = -1.0 * frameDft.imag[fftSize - i];
		}

		if (MathUtils.isPowerOfTwo(fftSize))
			FFT.transform(frameDft.real, frameDft.imag, true);
		else
			frameDft = FFTMixedRadix.ifft(frameDft);

		y = new double[Math.min(origLen, frameDft.real.length)];
		for (i = 0; i < origLen; i++)
			y[i] = frameDft.real[i];

		return y;
	}

	public static void displayDFTSpectrumLinearNoWindowing(double[] frame) {
		int fftSize = 2;
		while (fftSize < frame.length)
			fftSize *= 2;

		displayDFTSpectrumLinearNoWindowing(frame, fftSize);
	}

	public static void displayDFTSpectrumLinearNoWindowing(double[] frame, int fftSize) {
		displayDFTSpectrumLinear(frame, fftSize, Window.RECT);
	}

	public static void displayDFTSpectrumLinear(double[] frame) {
		int fftSize = 2;
		while (fftSize < frame.length)
			fftSize *= 2;

		displayDFTSpectrumLinear(frame, fftSize);
	}

	public static void displayDFTSpectrumLinear(double[] frame, int fftSize) {
		displayDFTSpectrumLinear(frame, fftSize, Window.HAMMING);
	}

	public static void displayDFTSpectrumLinear(double[] frame, int fftSize, int windowType) {
		Window win = Window.get(windowType, frame.length);
		win.normalizeSquaredSum(1.0f);
		double[] frameW = win.apply(frame, 0);

		while (fftSize < frameW.length)
			fftSize *= 2;

		if (fftSize % 2 != 0)
			fftSize++;

		ComplexArray frameDft = new ComplexArray(fftSize);
		System.arraycopy(frameW, 0, frameDft.real, 0, frame.length);

		if (MathUtils.isPowerOfTwo(fftSize))
			FFT.transform(frameDft.real, frameDft.imag, false);
		else
			frameDft = FFTMixedRadix.fftComplex(frameDft);

		DisplayUtils.plot(MathUtils.magnitudeComplex(frameDft));
	}

	public static void displayDFTSpectrumInDBNoWindowing(double[] frame) {
		int fftSize = 2;
		while (fftSize < frame.length)
			fftSize *= 2;

		displayDFTSpectrumInDBNoWindowing(frame, fftSize);
	}

	public static void displayDFTSpectrumInDBNoWindowing(double[] frame, int fftSize) {
		displayDFTSpectrumInDB(frame, fftSize, Window.RECT);
	}

	public static void displayDFTSpectrumInDB(double[] frame) {
		int fftSize = 2;
		while (fftSize < frame.length)
			fftSize *= 2;

		displayDFTSpectrumInDB(frame, fftSize);
	}

	public static void displayDFTSpectrumInDB(double[] frame, int fftSize) {
		displayDFTSpectrumInDB(frame, fftSize, Window.HAMMING);
	}

	public static void displayDFTSpectrumInDB(double[] frame, int fftSize, int windowType) {
		Window win = Window.get(windowType, frame.length);
		if (windowType == Window.RECT)
			win.normalizePeakValue(1.0f);

		displayDFTSpectrumInDB(frame, fftSize, win.getCoeffs());
	}

	public static void displayDFTSpectrumInDB(double[] frame, int fftSize, double[] wgt) {
		ComplexArray frameDft = getFrameDft(frame, fftSize, wgt);

		int maxFreqInd = (int) Math.floor(0.5 * fftSize + 0.5);

		DisplayUtils.plot(MathUtils.amp2db(MathUtils.magnitudeComplex(frameDft)), 0, maxFreqInd);
	}

	public static double[] getFrameHalfMagnitudeSpectrum(double[] frame, int fftSize) {
		double[] fullSpec = getFrameMagnitudeSpectrum(frame, fftSize, Window.RECT);
		int maxFreq = SignalProcUtils.halfSpectrumSize(fftSize);
		double[] halfSpec = ArrayUtils.subarray(fullSpec, 0, maxFreq);

		return halfSpec;
	}

	public static double[] getFrameMagnitudeSpectrum(double[] frame, int fftSize) {
		return getFrameMagnitudeSpectrum(frame, fftSize, Window.RECT);
	}

	public static double[] getFrameHalfMagnitudeSpectrum(double[] frame, int fftSize, int windowType) {
		double[] fullSpec = getFrameMagnitudeSpectrum(frame, fftSize, windowType);
		int maxFreq = SignalProcUtils.halfSpectrumSize(fftSize);
		double[] halfSpec = ArrayUtils.subarray(fullSpec, 0, maxFreq);

		return halfSpec;
	}

	public static double[] getFrameMagnitudeSpectrum(double[] frame, int fftSize, int windowType) {
		Window win = Window.get(windowType, frame.length);
		if (windowType == Window.RECT)
			win.normalizePeakValue(1.0f);

		return getFrameMagnitudeSpectrum(frame, fftSize, win.getCoeffs());
	}

	public static double[] getFrameHalfMagnitudeSpectrum(double[] frame, int fftSize, double[] wgt) {
		double[] fullSpec = getFrameMagnitudeSpectrum(frame, fftSize, wgt);
		int maxFreq = SignalProcUtils.halfSpectrumSize(fftSize);
		double[] halfSpec = ArrayUtils.subarray(fullSpec, 0, maxFreq);

		return halfSpec;
	}

	public static double[] getFrameMagnitudeSpectrum(double[] frame, int fftSize, double[] wgt) {
		return MathUtils.magnitudeComplex(getFrameDft(frame, fftSize, wgt));
	}

	// No windowing, i.e. rectangular window
	public static ComplexArray getFrameDft(double[] frame, int fftSize) {
		return getFrameDft(frame, fftSize, Window.RECT);
	}

	public static ComplexArray getFrameDft(double[] frame, int fftSize, int windowType) {
		Window win = Window.get(windowType, frame.length);
		if (windowType == Window.RECT)
			win.normalizePeakValue(1.0f);

		double[] wgt = win.getCoeffs();

		return getFrameDft(frame, fftSize, wgt);
	}

	public static ComplexArray getFrameDft(double[] frame, int fftSize, double[] windowWgt) {
		double[] frameW = MathUtils.multiply(frame, windowWgt);

		while (fftSize < frameW.length)
			fftSize *= 2;

		if (fftSize % 2 != 0)
			fftSize++;

		ComplexArray frameDft = new ComplexArray(fftSize);
		System.arraycopy(frameW, 0, frameDft.real, 0, frame.length);

		if (MathUtils.isPowerOfTwo(fftSize))
			FFT.transform(frameDft.real, frameDft.imag, false);
		else
			frameDft = FFTMixedRadix.fftComplex(frameDft);

		return frameDft;
	}

	public static void displayLPSpectrumLinear(double[] alpha, double lpGain, int fftSize) {
		double[] lpSpec = LpcAnalyser.calcSpecLinear(alpha, lpGain, fftSize);
		DisplayUtils.plot(lpSpec);
	}

	public static void displayLPSpectrumInDB(double[] alpha, double lpGain, int fftSize) {
		double[] lpSpecInDB = MathUtils.amp2db(LpcAnalyser.calcSpecLinear(alpha, lpGain, fftSize));
		DisplayUtils.plot(lpSpecInDB);
	}

	// Shifts an array by N points (to right if N is positive, to left if negative)
	// The length of the returned array is the same as the original
	// Additonal end points are simply replicates of values at boundaries
	public static double[] shift(double[] x, int N) {
		if (N == 0)
			return x;

		double[] y = new double[x.length];
		int i;
		if (N > 0) {
			for (i = 0; i < N; i++)
				y[i] = x[0];
			for (i = N; i < x.length - N; i++)
				y[i] = x[i - N];
		} else {
			N = -1 * N;
			for (i = 0; i < x.length - N; i++)
				y[i] = x[i + N];
			for (i = x.length - N; i < x.length; i++)
				y[i] = x[x.length - 1];
		}

		return y;
	}

	// If N<0, shift the sequence left
	// If N>0, shift the sequence right
	public static float[] shift(float[] x, int N) {
		if (N == 0)
			return x;

		float[] y = new float[x.length];
		int i;

		if (N > 0) // Shift right
		{
			for (i = 0; i < N; i++)
				y[i] = x[0];
			for (i = N; i < x.length; i++)
				y[i] = x[i - N];
		} else // Shift left
		{
			N = -1 * N;
			for (i = 0; i < x.length - N; i++)
				y[i] = x[i + N];
			for (i = x.length - N; i < x.length; i++)
				y[i] = x[x.length - 1];
		}

		return y;
	}

	public static double[] getPeakAmplitudes(double[] sDft, double f0InHz, int numHarmonics, int fftSize,
			double samplingRateInHz, boolean bIncludeZerothHarmonic) {
		int startHarmonicIndex;
		if (bIncludeZerothHarmonic)
			startHarmonicIndex = 0;
		else
			startHarmonicIndex = 1;

		int endHarmonicIndex = numHarmonics;

		return getPeakAmplitudes(sDft, f0InHz, startHarmonicIndex, endHarmonicIndex, fftSize, samplingRateInHz, true);
	}

	public static double[] getPeakAmplitudeFrequencies(double[] sDft, double f0InHz, int numHarmonics, int fftSize,
			double samplingRateInHz, boolean bIncludeZerothHarmonic) {
		int startHarmonicIndex;
		if (bIncludeZerothHarmonic)
			startHarmonicIndex = 0;
		else
			startHarmonicIndex = 1;

		int endHarmonicIndex = numHarmonics;

		return getPeakAmplitudes(sDft, f0InHz, startHarmonicIndex, endHarmonicIndex, fftSize, samplingRateInHz, false);
	}

	/**
	 * 
	 * @param sDft
	 *            sDtf
	 * @param f0InHz
	 *            f0InHz
	 * @param startHarmonicIndex
	 *            startHarmonicIndex
	 * @param endHarmonicIndex
	 *            endHarmonicIndex
	 * @param fftSize
	 *            fftSize
	 * @param samplingRateInHz
	 *            samplingRateInHz
	 * @param amplitudes
	 *            : if amplitudes true it returns the amplitude values, original function if amplitudes false it returns the
	 *            amplitude frequencies where the peaks were located
	 * @return amps if amplitudes, ampsFreq otherwise
	 */
	public static double[] getPeakAmplitudes(double[] sDft, double f0InHz, int startHarmonicIndex, int endHarmonicIndex,
			int fftSize, double samplingRateInHz, boolean amplitudes) {
		int maxFreqIndex = (int) Math.floor(0.5 * fftSize + 0.5);

		int numHarmonics = endHarmonicIndex - startHarmonicIndex + 1;
		int numAmps = numHarmonics;

		double[] amps = new double[numAmps];
		double[] ampsFreq = new double[numAmps];

		int freqStartInd, freqEndInd;
		int i, k;
		int zeroBasedMaxFreqIndex = fftSize / 2;

		for (i = startHarmonicIndex; i <= endHarmonicIndex; i++) {
			freqStartInd = SignalProcUtils.freq2index(i * f0InHz - 0.3 * f0InHz, (int) samplingRateInHz, maxFreqIndex);
			freqEndInd = SignalProcUtils.freq2index(i * f0InHz + 0.3 * f0InHz, (int) samplingRateInHz, maxFreqIndex);
			k = MathUtils.getMaxIndex(sDft, freqStartInd, freqEndInd);
			amps[i - startHarmonicIndex] = sDft[k];
			if (!amplitudes)
				ampsFreq[i - startHarmonicIndex] = SignalProcUtils.index2freq(k, (int) samplingRateInHz, zeroBasedMaxFreqIndex);
		}

		if (amplitudes)
			return amps;
		else
			return ampsFreq;
	}

	public static float[] getAnalysisTimes(int numfrm, double windowSizeInSeconds, double frameShiftInSeconds) {
		float[] analysisTimesInSeconds = null;
		if (numfrm > 0) {
			analysisTimesInSeconds = new float[numfrm];
			for (int i = 0; i < numfrm; i++)
				analysisTimesInSeconds[i] = (float) (i * frameShiftInSeconds + 0.5 * windowSizeInSeconds);
		}

		return analysisTimesInSeconds;
	}

	public static double[][] getMapped(double[][] x, int[] mapInds) {
		double[][] y = null;

		if (mapInds != null && x != null) {
			y = new double[mapInds.length][];

			for (int i = 0; i < mapInds.length; i++)
				y[i] = ArrayUtils.copy(x[mapInds[i]]);
		}

		return y;
	}

	public static float[][] getMapped(float[][] x, int[] mapInds) {
		float[][] y = null;

		if (mapInds != null && x != null) {
			y = new float[mapInds.length][];

			for (int i = 0; i < mapInds.length; i++)
				y[i] = ArrayUtils.copy(x[mapInds[i]]);
		}

		return y;
	}

	public static Window getWindow(int windowType, int windowSizeInSamples) {
		if (windowType == Window.BARTLETT)
			return new BartlettWindow(windowSizeInSamples);
		else if (windowType == Window.BLACKMAN)
			return new BlackmanWindow(windowSizeInSamples);
		else if (windowType == Window.FLATTOP)
			return new FlattopWindow(windowSizeInSamples);
		else if (windowType == Window.GAUSS)
			return new GaussWindow(windowSizeInSamples);
		else if (windowType == Window.HAMMING)
			return new HammingWindow(windowSizeInSamples);
		else if (windowType == Window.HANNING)
			return new HanningWindow(windowSizeInSamples);
		else if (windowType == Window.RECT)
			return new RectWindow(windowSizeInSamples);
		else {
			System.out.println("Undefined window type!");
			return null;
		}
	}

	public static int getTotalFrames(int totalSamples, int windowLengthInSamples, int skipSizeInSamples) {
		int samplingRate = 16000; // Dummy sampling rate which will be cancelled out in calculations anyway
		return getTotalFrames(sample2time(totalSamples, samplingRate), sample2time(windowLengthInSamples, samplingRate),
				sample2time(skipSizeInSamples, samplingRate));
	}

	public static int getTotalFrames(int totalSamples, double windowSizeInSeconds, double skipSizeInSeconds) {
		int samplingRate = 16000; // Dummy sampling rate which will be cancelled out in calculations anyway
		return getTotalFrames(sample2time(totalSamples, samplingRate), windowSizeInSeconds, skipSizeInSeconds);
	}

	public static int getTotalFrames(double totalTimeInSeconds, double windowSizeInSeconds, double skipSizeInSeconds) {
		int numfrm = 0;
		if (skipSizeInSeconds > 0.0) {
			numfrm = (int) Math.floor((totalTimeInSeconds - windowSizeInSeconds) / skipSizeInSeconds + 0.5);
			if ((numfrm - 1) * skipSizeInSeconds + windowSizeInSeconds < totalTimeInSeconds)
				numfrm++;
		}

		return numfrm;
	}

	public static double melNonMultiplied(double freqInRadian, int samplingRateInHz) {
		return Math.log(1.0 + freqInRadian * samplingRateInHz / (MathUtils.TWOPI * 700));
	}

	public static double radian2mel(double freqInRadian, int samplingRateInHz) {
		return Math.PI * melNonMultiplied(freqInRadian, samplingRateInHz) / melNonMultiplied(Math.PI, samplingRateInHz);
	}

	public static double hz2mel(double freqInHz, int samplingRateInHz) {
		return radian2mel(SignalProcUtils.hz2radian(freqInHz, samplingRateInHz), samplingRateInHz);
	}

	public static double mel2radian(double mel, int samplingRateInHz) {
		return MathUtils.TWOPI * 700.0 / samplingRateInHz
				* (-1.0 + Math.exp(mel * melNonMultiplied(Math.PI, samplingRateInHz) / Math.PI));
	}

	public static double mel2hz(double mel, int samplingRateInHz) {
		return SignalProcUtils.radian2hz(mel2radian(mel, samplingRateInHz), samplingRateInHz);
	}

	public static double[] replaceNaNsWith(double[] x, double val) {
		double[] y = null;

		if (x != null && x.length > 0) {
			y = new double[x.length];
			for (int i = 0; i < x.length; i++) {
				if (Double.isNaN(x[i]))
					y[i] = 0.0;
				else
					y[i] = x[i];
			}
		}

		return y;
	}

	public static double sourceTime2targetTime(double sourceTime, Labels sourceLabels, Labels targetLabels) {
		int[][] map = AlignLabelsUtils.alignLabels(sourceLabels.items, targetLabels.items);

		return sourceTime2targetTime(sourceTime, sourceLabels, targetLabels, map);
	}

	public static double sourceTime2targetTime(double sourceTime, Labels sourceLabels, Labels targetLabels, int[][] map) {
		int sourceLabInd = SignalProcUtils.time2LabelIndex(sourceTime, sourceLabels);
		double sourceDuration, targetDuration;
		double locationInLabelPercent;

		if (sourceLabInd > 0) {
			sourceDuration = sourceLabels.items[sourceLabInd].time - sourceLabels.items[sourceLabInd - 1].time;
			locationInLabelPercent = (sourceTime - sourceLabels.items[sourceLabInd - 1].time) / sourceDuration;
		} else {
			sourceDuration = sourceLabels.items[sourceLabInd].time;
			locationInLabelPercent = sourceTime / sourceLabels.items[sourceLabInd].time;
		}

		int targetLabInd = StringUtils.findInMap(map, sourceLabInd);
		if (targetLabInd > 0)
			targetDuration = targetLabels.items[targetLabInd].time - targetLabels.items[targetLabInd - 1].time;
		else
			targetDuration = targetLabels.items[targetLabInd].time;

		double targetTime;
		if (targetLabInd > 0)
			targetTime = targetLabels.items[targetLabInd - 1].time + locationInLabelPercent * targetDuration;
		else
			targetTime = locationInLabelPercent * targetDuration;

		return targetTime;
	}

	public static int[] mapFrameIndices(int numfrmSource, Labels srcLabs, double srcWindowSizeInSeconds,
			double srcSkipSizeInSeconds, int numFrmTarget, Labels tgtLabs, double tgtWindowSizeInSeconds,
			double tgtSkipSizeInSeconds) {
		int[] mappedInds = null;
		int[][] mappedLabelInds = AlignLabelsUtils.alignLabels(srcLabs.items, tgtLabs.items);

		if (numfrmSource > 0) {
			mappedInds = new int[numfrmSource];
			double tSource, tTarget, tFrameInd, sourceDuration, sourceLocationInLabelPercent;
			double sMapStart, tMapStart, sMapEnd, tMapEnd;
			int sourceLabInd, targetLabInd;
			int targetFrmInd;
			for (int i = 0; i < numfrmSource; i++) {
				tSource = i * srcSkipSizeInSeconds + 0.5 * srcWindowSizeInSeconds;
				sourceLabInd = SignalProcUtils.time2LabelIndex(tSource, srcLabs);
				targetLabInd = StringUtils.findInMap(mappedLabelInds, sourceLabInd);

				if (targetLabInd < 0) {
					sMapStart = 0.0;
					tMapStart = 0.0;
					sMapEnd = srcLabs.items[srcLabs.items.length - 1].time;
					tMapEnd = tgtLabs.items[tgtLabs.items.length - 1].time;

					for (int j = targetLabInd - 1; j >= 0; j--) {
						int prevSourceLabInd = StringUtils.findInMapReverse(mappedLabelInds, j);
						if (prevSourceLabInd > -1) {
							sMapStart = srcLabs.items[prevSourceLabInd].time;
							tMapStart = tgtLabs.items[j].time;
							break;
						}
					}

					for (int j = targetLabInd + 1; j < tgtLabs.items.length; j++) {
						int nextSourceLabInd = StringUtils.findInMapReverse(mappedLabelInds, j);
						if (nextSourceLabInd > -1) {
							sMapEnd = srcLabs.items[nextSourceLabInd].time;
							tMapEnd = tgtLabs.items[j].time;
							break;
						}
					}
				} else {
					sMapStart = 0.0;
					if (sourceLabInd > 0)
						sMapStart = srcLabs.items[sourceLabInd - 1].time;

					tMapStart = 0.0;
					if (targetLabInd > 0)
						tMapStart = tgtLabs.items[targetLabInd - 1].time;

					sMapEnd = srcLabs.items[sourceLabInd].time;
					tMapEnd = tgtLabs.items[targetLabInd].time;
				}

				tTarget = MathUtils.linearMap(tSource, sMapStart, sMapEnd, tMapStart, tMapEnd);
				targetFrmInd = SignalProcUtils.time2frameIndex(tTarget, tgtWindowSizeInSeconds, tgtSkipSizeInSeconds);
				targetFrmInd = MathUtils.CheckLimits(targetFrmInd, 0, numFrmTarget - 1);
				mappedInds[i] = targetFrmInd;
			}
		}

		return mappedInds;
	}

	// This version uses source and target labels to align speech frames
	public static double[] normalizeVocalTract(double[] srcSignal, double[] tgtSignal, Labels sourceLabels, Labels targetLabels,
			int windowType, double windowSizeInSeconds, double frameShiftInSeconds, int lpcOrder, int samplingRateInHz,
			float preCoef) {
		float[][] sourceLpcs = LpcAnalyser.signal2lpCoeffsf(srcSignal, windowType, windowSizeInSeconds, frameShiftInSeconds,
				samplingRateInHz, lpcOrder, preCoef);
		float[] sAnalysisInSeconds = SignalProcUtils
				.getAnalysisTimes(sourceLpcs.length, windowSizeInSeconds, frameShiftInSeconds);

		float[][] targetLpcs = LpcAnalyser.signal2lpCoeffsf(tgtSignal, windowType, windowSizeInSeconds, frameShiftInSeconds,
				samplingRateInHz, lpcOrder, preCoef);

		// Mapping
		int[] mappedInds = SignalProcUtils.mapFrameIndices(sourceLpcs.length, sourceLabels, windowSizeInSeconds,
				frameShiftInSeconds, targetLpcs.length, targetLabels, windowSizeInSeconds, frameShiftInSeconds);
		float[][] mappedTargetLpcs = SignalProcUtils.getMapped(targetLpcs, mappedInds);

		return normalizeVocalTract(srcSignal, sAnalysisInSeconds, mappedTargetLpcs, windowType, windowSizeInSeconds, lpcOrder,
				samplingRateInHz, preCoef);
	}

	public static double[] normalizeVocalTract(double[] s, float[] sAnalysisInSeconds, float[][] mappedTgtLpcs, int windowType,
			double windowSizeInSeconds, int lpcOrderSrc, int samplingRateInHz, float preCoef) {
		float[][] srcLpcs = LpcAnalyser.signal2lpCoeffsf(s, windowType, sAnalysisInSeconds, windowSizeInSeconds,
				samplingRateInHz, lpcOrderSrc, preCoef);

		return normalizeVocalTract(s, sAnalysisInSeconds, srcLpcs, mappedTgtLpcs, windowSizeInSeconds, samplingRateInHz, preCoef);
	}

	public static double[] normalizeVocalTract(double[] x, float[] tAnalysisInSeconds, float[][] srcLpcs,
			float[][] mappedTgtLpcs, double windowSizeInSeconds, int samplingRateInHz, float preCoef) {
		double[] y = null;

		assert tAnalysisInSeconds.length == srcLpcs.length;
		assert tAnalysisInSeconds.length == mappedTgtLpcs.length;

		int lpOrder = srcLpcs[0].length;
		int numfrm = tAnalysisInSeconds.length;
		int ws = SignalProcUtils.time2sample(windowSizeInSeconds, samplingRateInHz);
		int halfWs = (int) Math.floor(0.5 * ws + 0.5);
		Window wgt = new HammingWindow(ws);
		double[] winWgt = wgt.getCoeffs();
		double[] frm = new double[ws];
		int frmStartIndex;
		int frmEndIndex;

		int fftSize = SignalProcUtils.getDFTSize(samplingRateInHz);
		while (fftSize < ws)
			fftSize *= 2;

		ComplexArray expTerm = LpcAnalyser.calcExpTerm(fftSize, lpOrder);

		y = new double[x.length];
		double[] w = new double[x.length];
		Arrays.fill(y, 0.0);
		Arrays.fill(w, 0.0);

		double[] xPreemp = SignalProcUtils.applyPreemphasis(x, preCoef);

		int i, k;
		for (i = 0; i < numfrm; i++) {
			if (i == 0)
				frmStartIndex = 0;
			else
				frmStartIndex = Math.max(0,
						SignalProcUtils.time2sample(tAnalysisInSeconds[i] - 0.5 * windowSizeInSeconds, samplingRateInHz));

			frmEndIndex = Math.min(frmStartIndex + ws - 1, xPreemp.length - 1);
			Arrays.fill(frm, 0.0);
			System.arraycopy(xPreemp, frmStartIndex, frm, 0, frmEndIndex - frmStartIndex + 1);

			frm = wgt.apply(frm, 0);
			double origEn = SignalProcUtils.energy(frm);

			double[] inputVocalTractSpectrum = LpcAnalyser.calcSpecLinearf(srcLpcs[i], 1.0f, fftSize, expTerm);
			double[] outputVocalTractSpectrum = LpcAnalyser.calcSpecLinearf(mappedTgtLpcs[i], 1.0f, fftSize, expTerm);

			ComplexArray inputDft = new ComplexArray(fftSize);
			int maxFreq = fftSize / 2 + 1;

			Arrays.fill(inputDft.real, 0.0);
			Arrays.fill(inputDft.imag, 0.0);

			System.arraycopy(frm, 0, inputDft.real, 0, ws);

			inputDft = FFTMixedRadix.fftComplex(inputDft);

			// MaryUtils.plot(MathUtils.amp2db(MathUtils.abs(inputDft)));
			// MaryUtils.plot(MathUtils.amp2db(inputVocalTractSpectrum));
			// MaryUtils.plot(MathUtils.amp2db(outputVocalTractSpectrum));

			for (k = 1; k <= maxFreq; k++) {
				inputDft.real[k - 1] = inputDft.real[k - 1] * outputVocalTractSpectrum[k - 1] / inputVocalTractSpectrum[k - 1];
				inputDft.imag[k - 1] = inputDft.imag[k - 1] * outputVocalTractSpectrum[k - 1] / inputVocalTractSpectrum[k - 1];
			}

			for (k = maxFreq + 1; k <= fftSize; k++) {
				inputDft.real[k - 1] = inputDft.real[2 * maxFreq - 1 - k];
				inputDft.imag[k - 1] = -inputDft.imag[2 * maxFreq - 1 - k];
			}

			// MaryUtils.plot(MathUtils.amp2db(MathUtils.abs(inputDft)));

			inputDft = FFTMixedRadix.ifft(inputDft);

			// MaryUtils.plot(frm);

			System.arraycopy(inputDft.real, 0, frm, 0, ws);

			double newEn = SignalProcUtils.energy(frm);

			frm = MathUtils.multiply(frm, Math.sqrt(origEn) / Math.sqrt(newEn));

			// MaryUtils.plot(frm);

			for (k = 0; k < ws; k++) {
				if (frmStartIndex + k > y.length - 1)
					break;

				if (i == 0) {
					if (k < halfWs) {
						y[frmStartIndex + k] += frm[k] * winWgt[k];
						w[frmStartIndex + k] += 1.0;
					} else {
						y[frmStartIndex + k] += frm[k] * winWgt[k];
						w[frmStartIndex + k] += winWgt[k] * winWgt[k];
					}
				} else if (i == numfrm - 1) {
					if (k > halfWs) {
						y[frmStartIndex + k] += frm[k] * winWgt[k];
						w[frmStartIndex + k] = 1.0;
					} else {
						y[frmStartIndex + k] += frm[k] * winWgt[k];
						w[frmStartIndex + k] += winWgt[k] * winWgt[k];
					}
				} else {
					y[frmStartIndex + k] += frm[k] * winWgt[k];
					w[frmStartIndex + k] += winWgt[k] * winWgt[k];
				}
			}

			System.out.println(String.valueOf(frmStartIndex) + "-" + String.valueOf(frmEndIndex)
					+ " Normalized vocal tract spectrum for frame " + String.valueOf(i + 1) + " of " + String.valueOf(numfrm));
		}

		for (k = 0; k < y.length; k++) {
			if (w[k] > 0.0)
				y[k] /= w[k];
		}

		y = SignalProcUtils.removePreemphasis(y, preCoef);

		// MaryUtils.plot(x);
		// MaryUtils.plot(y);

		return y;
	}

	public static void test_normalizeVocalTract() throws UnsupportedAudioFileException, IOException {
		String sourceWavFile = "d:\\src.wav";
		String sourceLabFile = "d:\\src.lab";
		String targetWavFile = "d:\\tgt.wav";
		String targetLabFile = "d:\\tgt.lab";
		String outputWavFile = "d:\\srcResidual_tgtVocalTract.wav";

		int windowType = Window.HAMMING;
		double windowSizeInSeconds = 0.020;
		double frameShiftInSeconds = 0.010;
		float preCoef = 0.97f;

		// File input source and LPC analysis
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(sourceWavFile));
		AudioFormat format = inputAudio.getFormat();
		int fsSrc = (int) inputAudio.getFormat().getSampleRate();
		int lpcOrderSrc = SignalProcUtils.getLPOrder(fsSrc);
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] s = signal.getAllData();
		Labels sourceLabels = new Labels(sourceLabFile);
		//

		// File input target and LPC analysis
		inputAudio = AudioSystem.getAudioInputStream(new File(targetWavFile));
		int fsTgt = (int) inputAudio.getFormat().getSampleRate();
		int lpcOrderTgt = SignalProcUtils.getLPOrder(fsTgt);
		signal = new AudioDoubleDataSource(inputAudio);
		double[] t = signal.getAllData();
		Labels targetLabels = new Labels(targetLabFile);
		//

		double[] sNorm = normalizeVocalTract(s, t, sourceLabels, targetLabels, windowType, windowSizeInSeconds,
				frameShiftInSeconds, lpcOrderSrc, fsSrc, preCoef);

		MaryAudioUtils.writeWavFile(sNorm, outputWavFile, format);
	}

	// This version does linear mapping between the whole source and target signals
	public static double[] normalizeVocalTract(double[] srcSignal, double[] tgtSignal, int windowType,
			double windowSizeInSeconds, double frameShiftInSeconds, int lpcOrder, int samplingRateInHz, float preCoef) {
		float[][] sourceLpcs = LpcAnalyser.signal2lpCoeffsf(srcSignal, windowType, windowSizeInSeconds, frameShiftInSeconds,
				samplingRateInHz, lpcOrder, preCoef);
		float[] sAnalysisInSeconds = SignalProcUtils
				.getAnalysisTimes(sourceLpcs.length, windowSizeInSeconds, frameShiftInSeconds);

		float[][] targetLpcs = LpcAnalyser.signal2lpCoeffsf(tgtSignal, windowType, windowSizeInSeconds, frameShiftInSeconds,
				samplingRateInHz, lpcOrder, preCoef);

		// Mapping
		int[] mappedInds = new int[sourceLpcs.length];
		for (int i = 0; i < sourceLpcs.length; i++)
			mappedInds[i] = MathUtils.linearMap(i, 0, sourceLpcs.length - 1, 0, targetLpcs.length - 1);

		float[][] mappedTargetLpcs = SignalProcUtils.getMapped(targetLpcs, mappedInds);

		return normalizeVocalTract(srcSignal, sAnalysisInSeconds, mappedTargetLpcs, windowType, windowSizeInSeconds, lpcOrder,
				samplingRateInHz, preCoef);
	}

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
		/*
		 * LowPassFilter f = new LowPassFilter(0.25, 11);
		 * 
		 * double[] b = f.getDenumeratorCoefficients();
		 * 
		 * double[] a = new double[1]; a[0] = 1.0;
		 * 
		 * double[] x; double[] y;
		 * 
		 * int i; String str;
		 * 
		 * x = new double[100]; for (i=0; i<x.length; i++) x[i] = i;
		 * 
		 * str = ""; for (i=0; i<x.length; i++) str += String.valueOf(x[i]) + " "; System.out.println(str);
		 * 
		 * y = filter(b, a, x); str = "filtered="; for (i=0; i<y.length; i++) str += String.valueOf(y[i]) + " ";
		 * System.out.println(str);
		 * 
		 * y = filtfilt(b, a, x); str = "filtfilted="; for (i=0; i<y.length; i++) str += String.valueOf(y[i]) + " ";
		 * System.out.println(str);
		 */

		test_normalizeVocalTract();
	}

}
