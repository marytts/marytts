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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.Defaults;
import marytts.signalproc.display.FunctionGraph;
import marytts.signalproc.display.SignalGraph;
import marytts.signalproc.filter.FIRFilter;
import marytts.signalproc.window.Window;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.ArrayUtils;
import marytts.util.math.ComplexArray;
import marytts.util.math.ComplexNumber;
import marytts.util.math.FFT;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * 
 * @author Marc Schr&ouml;der
 * 
 *         A class for linear prediction analysis
 * 
 */
public class LpcAnalyser extends FrameBasedAnalyser {
	public static int lpOrder = 0;
	public static float preemphasisCoefficient = 0.0f;

	public LpcAnalyser(DoubleDataSource signal, int framelength, int samplingRate) {
		this(signal, Window.get(Defaults.getWindowType(), framelength), framelength, samplingRate);
	}

	public LpcAnalyser(DoubleDataSource signal, int framelength, int frameShift, int samplingRate) {
		this(signal, Window.get(Defaults.getWindowType(), framelength), frameShift, samplingRate);
	}

	public LpcAnalyser(DoubleDataSource signal, int framelength, int frameShift, int samplingRate, int order, int windowType) {
		this(signal, Window.get(windowType, framelength), frameShift, samplingRate, order);
	}

	public LpcAnalyser(DoubleDataSource signal, int framelength, int frameShift, int samplingRate, int order, int windowType,
			float preCoef) {
		this(signal, Window.get(windowType, framelength), frameShift, samplingRate, order, preCoef);
	}

	public LpcAnalyser(DoubleDataSource signal, Window window, int frameShift, int samplingRate) {
		this(signal, window, frameShift, samplingRate, SignalProcUtils.getLPOrder(samplingRate));
	}

	public LpcAnalyser(DoubleDataSource signal, Window window, int frameShift, int samplingRate, int order) {
		this(signal, window, frameShift, samplingRate, order, 0.0f);
	}

	public LpcAnalyser(DoubleDataSource signal, Window window, int frameShift, int samplingRate, int order, float preCoef) {
		super(signal, window, frameShift, samplingRate);
		lpOrder = order;
		preemphasisCoefficient = preCoef;
	}

	/**
	 * Apply this FrameBasedAnalyser to the given data.
	 * 
	 * @param frame
	 *            the data to analyse, which must be of the length prescribed by this FrameBasedAnalyser, i.e. by
	 *            {@link #getFrameLengthSamples()}.
	 * @return an LPCoeffs object representing the lpc coefficients and gain factor of the frame.
	 * @throws IllegalArgumentException
	 *             if frame does not have the prescribed length
	 */
	public Object analyse(double[] frame) {
		if (frame.length != getFrameLengthSamples())
			throw new IllegalArgumentException("Expected frame of length " + getFrameLengthSamples() + ", got " + frame.length);

		return calcLPC(frame, lpOrder, preemphasisCoefficient);
	}

	/**
	 * Calculate LPC parameters for a given input signal.
	 * 
	 * @param x
	 *            input signal
	 * @param p
	 *            prediction order
	 * @return an LPCoeffs object encapsulating the LPC coefficients, a = [1, -a_1, -a_2, ... -a_p], and the gain factor
	 */
	public static LpCoeffs calcLPC(double[] x, int p) {
		return calcLPC(x, p, 0.0f);
	}

	public static LpCoeffs calcLPC(double[] x, int p, float preCoef) {
		if (p <= 0)
			p = Integer.getInteger("signalproc.lpcorder", 24).intValue();

		if (preCoef > 0.0)
			x = SignalProcUtils.applyPreemphasis(x, preCoef);

		int i;

		if (MathUtils.allZeros(x)) {
			for (i = 0; i < x.length; i++)
				x[i] += Math.random() * 1e-100;
		}

		double[] r;

		// Frequency domain autocorrelation computation
		double[] autocorr = FFT.autoCorrelateWithZeroPadding(x);
		if (2 * (p + 1) < autocorr.length) { // normal case: frame long enough
			r = ArrayUtils.subarray(autocorr, autocorr.length / 2, p + 1);
		} else { // absurdly short frame
			// still compute LPC coefficients, by zero-padding the r
			r = new double[p + 1];
			System.arraycopy(autocorr, autocorr.length / 2, r, 0, autocorr.length - autocorr.length / 2);
		}
		//

		double[] coeffs = MathUtils.levinson(r, p); // These are oneMinusA!

		// gain factor:
		double g = Math.sqrt(MathUtils.sum(MathUtils.multiply(coeffs, r)));

		return new LpCoeffs(coeffs, g);
	}

	// Computes LP smoothed spectrum of a windowed speech frame (linear)
	public static double[] calcSpecFrameLinear(double[] windowedFrame, int p) {
		return calcSpecFrameLinear(windowedFrame, p, windowedFrame.length);
	}

	// Computes LP smoothed spectrum of a windowed speech frame
	public static double[] calcSpecFrameLinear(double[] windowedFrame, int p, int fftSize) {
		return calcSpecFrameLinear(windowedFrame, p, fftSize, null);
	}

	// Computes LP smoothed spectrum of a windowed speech frame
	public static double[] calcSpecFrameLinear(double[] windowedFrame, int p, int fftSize, ComplexArray expTerm) {
		LpCoeffs c = calcLPC(windowedFrame, p);

		if (expTerm == null || expTerm.real == null)
			return calcSpecLinear(c.getA(), c.getGain(), fftSize, null);
		else
			return calcSpecLinear(c.getA(), c.getGain(), fftSize, expTerm);
	}

	public static double[] calcSpecLinearFromOneMinusA(double[] oneMinusA, float gain, int fftSize, ComplexArray expTerm) {
		double[] alpha = new double[oneMinusA.length - 1];
		for (int i = 1; i < oneMinusA.length; i++)
			alpha[i - 1] = -1 * oneMinusA[i];
		return calcSpecLinear(alpha, gain, fftSize, expTerm);
	}

	// Computes LP smoothed spectrum from LP coefficients
	public static double[] calcSpec(double[] alpha, int fftSize) {
		return calcSpecLinear(alpha, 1.0f, fftSize, null);
	}

	public static double[] calcSpec(double[] alpha, int fftSize, ComplexArray expTerm) {
		return calcSpecLinear(alpha, 1.0f, fftSize, expTerm);
	}

	// Computes LP smoothed spectrum from LP coefficients
	public static double[] calcSpecLinear(double[] alpha, double sqrtGain, int fftSize) {
		return calcSpecLinear(alpha, sqrtGain, fftSize, null);
	}

	public static double[] calcSpecLinearf(float[] alpha, double sqrtGain, int fftSize, ComplexArray expTerm) {
		double[] alphaDouble = ArrayUtils.copyFloat2Double(alpha);

		return calcSpecLinear(alphaDouble, sqrtGain, fftSize, expTerm);
	}

	// Computes LP smoothed spectrum from LP coefficients
	public static double[] calcSpecLinear(double[] alpha, double sqrtGain, int fftSize, ComplexArray expTerm) {
		int p = alpha.length;
		int maxFreq = SignalProcUtils.halfSpectrumSize(fftSize);
		double[] vtSpectrum = new double[maxFreq];

		if (expTerm == null || expTerm.real == null || expTerm.real.length != p * maxFreq)
			expTerm = calcExpTerm(fftSize, p);

		int w, i, fInd;
		ComplexArray tmp = new ComplexArray(1);

		for (w = 0; w <= maxFreq - 1; w++) {
			tmp.real[0] = 1.0;
			tmp.imag[0] = 0.0;
			for (i = 0; i <= p - 1; i++) {
				fInd = i * maxFreq + w;
				tmp.real[0] -= alpha[i] * expTerm.real[fInd];
				tmp.imag[0] -= alpha[i] * expTerm.imag[fInd];
			}

			vtSpectrum[w] = sqrtGain / Math.sqrt(tmp.real[0] * tmp.real[0] + tmp.imag[0] * tmp.imag[0]);
		}

		return vtSpectrum;
	}

	public static double calcSpecValLinear(double[] alpha, double sqrtGain, double freqInHz, int samplingRateInHz) {
		ComplexNumber denum = new ComplexNumber(1.0f, 0.0f);
		double w;

		for (int k = 1; k <= alpha.length; k++) {
			w = SignalProcUtils.hz2radian(freqInHz, samplingRateInHz);
			denum = MathUtils.subtractComplex(denum, alpha[k - 1] * Math.cos(w * k), -1.0 * alpha[k - 1] * Math.sin(w * k));
		}

		double specValLinear = sqrtGain / MathUtils.magnitudeComplex(denum);

		return specValLinear;
	}

	public static ComplexArray calcExpTerm(int fftSize, int p) {
		int maxFreq = SignalProcUtils.halfSpectrumSize(fftSize);
		ComplexArray expTerm = new ComplexArray(p * maxFreq);
		int i, w;
		double r;

		for (w = 0; w <= maxFreq - 1; w++) {
			r = (MathUtils.TWOPI / fftSize) * w;

			for (i = 0; i <= p - 1; i++) {
				expTerm.real[i * maxFreq + w] = Math.cos(r * (i + 1));
				expTerm.imag[i * maxFreq + w] = -1 * Math.sin(r * (i + 1));
			}
		}

		return expTerm;
	}

	public static double[][] wavFile2lpCoeffs(String wavFile, int windowType, double windowSizeInSeconds,
			double frameShiftInSeconds, int lpcOrder, float preCoef) throws UnsupportedAudioFileException, IOException {
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(wavFile));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] x = signal.getAllData();

		return signal2lpCoeffs(x, windowType, windowSizeInSeconds, frameShiftInSeconds, samplingRate, lpcOrder, preCoef);
	}

	public static LpCoeffs[] wavFile2lpCoeffsWithGain(String wavFile, int windowType, double windowSizeInSeconds,
			double frameShiftInSeconds, int lpcOrder, float preCoef) throws UnsupportedAudioFileException, IOException {
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(wavFile));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] x = signal.getAllData();

		return signal2lpCoeffsWithGain(x, windowType, windowSizeInSeconds, frameShiftInSeconds, samplingRate, lpcOrder, preCoef);
	}

	public static double[][] signal2lpCoeffs(double[] x, int windowType, double windowSizeInSeconds, double frameShiftInSeconds,
			int samplingRateInHz, int lpcOrder, float preCoef) {
		LpCoeffs[] lpAnaResults = signal2lpCoeffsWithGain(x, windowType, windowSizeInSeconds, frameShiftInSeconds,
				samplingRateInHz, lpcOrder, preCoef);

		double[][] lpCoeffs = null;
		if (lpAnaResults != null) {
			lpCoeffs = new double[lpAnaResults.length][];
			for (int i = 0; i < lpAnaResults.length; i++)
				lpCoeffs[i] = ArrayUtils.copy(((LpCoeffs) lpAnaResults[i]).getA());
		}

		return lpCoeffs;
	}

	public static float[][] signal2lpCoeffsf(double[] x, int windowType, double windowSizeInSeconds, double frameShiftInSeconds,
			int samplingRateInHz, int lpcOrder, float preCoef) {
		LpCoeffs[] lpAnaResults = signal2lpCoeffsWithGain(x, windowType, windowSizeInSeconds, frameShiftInSeconds,
				samplingRateInHz, lpcOrder, preCoef);

		float[][] lpCoeffs = null;
		if (lpAnaResults != null) {
			lpCoeffs = new float[lpAnaResults.length][];
			for (int i = 0; i < lpAnaResults.length; i++)
				lpCoeffs[i] = ArrayUtils.copyDouble2Float(((LpCoeffs) lpAnaResults[i]).getA());
		}

		return lpCoeffs;
	}

	public static double[][] signal2lpCoeffs(double[] x, int windowType, float[] tAnalysisInSeconds, double windowSizeInSeconds,
			int samplingRateInHz, int lpcOrder, float preCoef) {
		LpCoeffs[] lpAnaResults = signal2lpCoeffsWithGain(x, windowType, tAnalysisInSeconds, windowSizeInSeconds,
				samplingRateInHz, lpcOrder, preCoef);

		double[][] lpCoeffs = null;
		if (lpAnaResults != null) {
			lpCoeffs = new double[lpAnaResults.length][];
			for (int i = 0; i < lpAnaResults.length; i++)
				lpCoeffs[i] = ArrayUtils.copy(((LpCoeffs) lpAnaResults[i]).getA());
		}

		return lpCoeffs;
	}

	public static float[][] signal2lpCoeffsf(double[] x, int windowType, float[] tAnalysisInSeconds, double windowSizeInSeconds,
			int samplingRateInHz, int lpcOrder, float preCoef) {
		LpCoeffs[] lpAnaResults = signal2lpCoeffsWithGain(x, windowType, tAnalysisInSeconds, windowSizeInSeconds,
				samplingRateInHz, lpcOrder, preCoef);

		float[][] lpCoeffs = null;
		if (lpAnaResults != null) {
			lpCoeffs = new float[lpAnaResults.length][];
			for (int i = 0; i < lpAnaResults.length; i++)
				lpCoeffs[i] = ArrayUtils.copyDouble2Float(((LpCoeffs) lpAnaResults[i]).getA());
		}

		return lpCoeffs;
	}

	public static LpCoeffs[] signal2lpCoeffsWithGain(double[] x, int windowType, double windowSizeInSeconds,
			double frameShiftInSeconds, int samplingRateInHz, int lpcOrder, float preCoef) {
		int ws = SignalProcUtils.time2sample(windowSizeInSeconds, samplingRateInHz);
		int ss = SignalProcUtils.time2sample(frameShiftInSeconds, samplingRateInHz);

		int numfrm = SignalProcUtils.getTotalFrames(SignalProcUtils.sample2time(x.length, samplingRateInHz), windowSizeInSeconds,
				frameShiftInSeconds);
		float[] tAnalysisInSeconds = SignalProcUtils.getAnalysisTimes(numfrm, windowSizeInSeconds, frameShiftInSeconds);

		return signal2lpCoeffsWithGain(x, windowType, tAnalysisInSeconds, windowSizeInSeconds, samplingRateInHz, lpcOrder,
				preCoef);
	}

	public static LpCoeffs[] signal2lpCoeffsWithGain(double[] x, int windowType, float[] tAnalysisInSeconds,
			double windowSizeInSeconds, int samplingRateInHz, int lpcOrder, float preCoef) {
		int ws = SignalProcUtils.time2sample(windowSizeInSeconds, samplingRateInHz);
		int numfrm = tAnalysisInSeconds.length;
		LpCoeffs[] lpCoeffs = null;

		if (numfrm > 0) {
			Window w = SignalProcUtils.getWindow(windowType, ws);
			double[] frm = new double[ws];
			lpCoeffs = new LpCoeffs[numfrm];

			double[] xPreemp = SignalProcUtils.applyPreemphasis(x, preCoef);
			int frmStartInd;
			for (int i = 0; i < numfrm; i++) {
				Arrays.fill(frm, 0.0);

				frmStartInd = SignalProcUtils.time2sample(tAnalysisInSeconds[i] - 0.5 * windowSizeInSeconds, samplingRateInHz);
				frmStartInd = MathUtils.CheckLimits(frmStartInd, 0, xPreemp.length - 1);
				System.arraycopy(xPreemp, frmStartInd, frm, 0, Math.min(ws, xPreemp.length - frmStartInd));

				lpCoeffs[i] = LpcAnalyser.calcLPC(frm, lpcOrder);
			}
		}

		return lpCoeffs;
	}

	public static void main(String[] args) throws Exception {
		int windowSize = Defaults.getWindowSize();
		int windowType = Defaults.getWindowType();
		int fftSize = Defaults.getFFTSize();
		int frameShift = Defaults.getFrameShift();
		int p = Integer.getInteger("signalproc.lpcorder", 24).intValue();
		int pre = p;
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] signalData = signal.getAllData();
		int position = 6000;
		Window w = Window.get(windowType, windowSize);
		double[] sliceToAnalyse = w.apply(signalData, position);
		LpCoeffs lpc = calcLPC(sliceToAnalyse, p);
		double g_db = 2 * MathUtils.db(lpc.getGain()); // *2 because g is signal, not energy
		double[] signalPowerSpectrum = FFT.computeLogPowerSpectrum(sliceToAnalyse);
		double[] a = lpc.getOneMinusA();
		double[] fftA = new double[fftSize];
		System.arraycopy(a, 0, fftA, 0, a.length);
		double[] lpcPowerSpectrum = FFT.computeLogPowerSpectrum(fftA);

		double offset = 0; // 2*MathUtils.db(2./Nfft);
		for (int i = 0; i < lpcPowerSpectrum.length; i++) {
			lpcPowerSpectrum[i] = -lpcPowerSpectrum[i] + offset + g_db;
		}
		for (int i = 0; i < signalPowerSpectrum.length; i++) {
			signalPowerSpectrum[i] += offset;
		}
		double[] lsp = LsfAnalyser.lpc2lsf(a, 1);
		System.out.println("Line spectral frequencies:");
		for (int i = 0; i < lsp.length; i++) {
			System.out.println(i + ": " + lsp[i] + " = " + lsp[i] / (2 * Math.PI) * samplingRate);
		}

		double deltaF = (double) samplingRate / fftSize;
		FunctionGraph signalSpectrumGraph = new FunctionGraph(0, deltaF, signalPowerSpectrum);
		signalSpectrumGraph.showInJFrame("signal spectrum", true, true);
		FunctionGraph lpcSpectrumGraph = new FunctionGraph(0, deltaF, lpcPowerSpectrum);
		lpcSpectrumGraph.showInJFrame("lpc spectrum", true, true);

		FIRFilter whiteningFilter = new FIRFilter(a);
		double[] testSlice = new double[fftSize + p];
		System.arraycopy(signalData, position - p, testSlice, 0, testSlice.length);
		double[] residuum = whiteningFilter.apply(testSlice);
		double[] usableSignal = ArrayUtils.subarray(testSlice, p, fftSize);
		double[] usableResiduum = ArrayUtils.subarray(residuum, p, fftSize);
		FunctionGraph signalGraph = new SignalGraph(usableSignal, samplingRate);
		signalGraph.showInJFrame("signal", true, true);
		FunctionGraph residuumGraph = new SignalGraph(usableResiduum, samplingRate);
		residuumGraph.showInJFrame("residual", true, true);
		double predictionGain = MathUtils.db(MathUtils.sum(MathUtils.multiply(usableSignal, usableSignal))
				/ MathUtils.sum(MathUtils.multiply(usableResiduum, usableResiduum)));
		System.out.println("Prediction gain: " + predictionGain + " dB");
	}

	public static class LpCoeffs {
		protected double[] oneMinusA = null;
		protected double gain = 1.0;

		protected double[] lsf = null;
		protected double[] lpcc = null;
		protected double[] lprefc = null;

		/**
		 * Create a set of LPC coefficients
		 * 
		 * @param oneMinusA
		 *            the coefficients, a = [1, -a_1, -a_2, ... -a_p], where p = prediction order
		 * @param gain
		 *            the gain factor, i.e. the square root of the total energy or the prediction error.
		 */
		public LpCoeffs(double[] oneMinusA, double gain) {
			this.oneMinusA = oneMinusA;
			this.gain = gain;

			this.lsf = null;
			this.lpcc = null;
			this.lprefc = null;
		}

		public LpCoeffs(LpCoeffs existing) {
			oneMinusA = ArrayUtils.copy(existing.oneMinusA);
			gain = existing.gain;

			lsf = ArrayUtils.copy(existing.lsf);
			lpcc = ArrayUtils.copy(existing.lpcc);
			lprefc = ArrayUtils.copy(existing.lprefc);
		}

		/**
		 * Return a clone of the internal representation of the LPC coefficients.
		 * 
		 * @return clone of oneMinusA
		 */
		public double[] getOneMinusA() {
			return (double[]) oneMinusA.clone();
		}

		public void setOneMinusA(double[] oneMinusA) {
			this.oneMinusA = (double[]) oneMinusA.clone();
			// Reset the cache:
			this.lsf = null;
			this.lpcc = null;
			this.lprefc = null;
		}

		// The following are the preferred ways to read/write the individual lpc coefficients:
		public final double getOneMinusA(int i) {
			return oneMinusA[i];
		}

		public double getA(int i) {
			return -oneMinusA[i + 1];
		}

		public void setOneMinusA(int i, double value) {
			oneMinusA[i] = value;
			// Clean the cache:
			this.lsf = null;
			this.lpcc = null;
			this.lprefc = null;
		}

		public void setA(int i, double value) {
			oneMinusA[i + 1] = -value;
			// Clean the cache:
			this.lsf = null;
			this.lpcc = null;
			this.lprefc = null;
		}

		/**
		 * Get the gain, i.e. the square root of the total energy of the prediction error.
		 * 
		 * @return the gain
		 */
		public double getGain() {
			return gain;
		}

		public void setGain(double gain) {
			this.gain = gain;
			this.lpcc = null; /*
							 * Note: lpcc[0] is related to the gain value, whereas the LSFs and reflection coeffs are oblivious to
							 * the gain value.
							 */
		}

		public int getOrder() {
			return oneMinusA.length - 1;
		}

		public double[] getA() {
			double[] a = new double[getOrder()];
			for (int i = 0; i < a.length; i++)
				a[i] = -oneMinusA[i + 1];
			return a;
		}

		public void setA(double[] a) {
			this.oneMinusA = new double[a.length + 1];
			oneMinusA[0] = 1;
			for (int i = 0; i < a.length; i++)
				oneMinusA[i + 1] = -a[i];
			// Clean the cache:
			this.lsf = null;
			this.lpcc = null;
			this.lprefc = null;
		}

		/**
		 * Convert these LPC coefficients into Line spectral frequencies.
		 * 
		 * @return the LSFs.
		 */
		public double[] getLSF() {
			if (lsf == null)
				lsf = LsfAnalyser.lpc2lsf(oneMinusA, 1);
			return ((double[]) lsf.clone());
		}

		public void setLSF(double[] someLsf) {
			this.lsf = (double[]) someLsf.clone();
			this.oneMinusA = LsfAnalyser.lsf2lpc(lsf);
			// Clean the cache:
			this.lpcc = null;
			this.lprefc = null;
		}

		/**
		 * Convert these LPC coefficients into LPC-Cesptrum coefficients.
		 * 
		 * @param cepstrumOrder
		 *            The cepstrum order (i.e., the index of the last cepstrum coefficient).
		 * @return the LPCCs. c[0] is set to log(gain).
		 */
		public double[] getLPCC(int cepstrumOrder) {
			if (lpcc == null)
				lpcc = CepstrumLPCAnalyser.lpc2lpcc(oneMinusA, gain, cepstrumOrder);
			return ((double[]) lpcc.clone());
		}

		/**
		 * Convert some LPC-Cepstrum coefficients into these LPC coefficients.
		 * 
		 * @param someLpcc
		 *            some Lpcc
		 * @param LPCOrder
		 *            The LPC order (i.e., the index of the last LPC coefficient). The gain is set to exp(c[0]) and the LPCs are
		 *            represented in the oneMinusA format [1 -a_1 -a_2 ... -a_p].
		 */
		public void setLPCC(double[] someLpcc, int LPCOrder) {
			this.lpcc = (double[]) someLpcc.clone();
			oneMinusA = CepstrumLPCAnalyser.lpcc2lpc(lpcc, LPCOrder);
			gain = Math.exp(lpcc[0]);
			// Clean the cache:
			this.lsf = null;
			this.lprefc = null;
		}

		/**
		 * Convert these LPC coefficients into reflection coefficients.
		 * 
		 * @return the reflection coefficients.
		 */
		public double[] getLPRefc() {
			if (lprefc == null)
				lprefc = ReflectionCoefficients.lpc2lprefc(oneMinusA);
			return ((double[]) lprefc.clone());
		}

		/**
		 * Convert some reflection coefficients into these LPC coefficients.
		 * 
		 * @param someLprefc
		 *            some Lprefc
		 */
		public void setLPRefc(double[] someLprefc) {
			this.lprefc = (double[]) someLprefc.clone();
			oneMinusA = ReflectionCoefficients.lprefc2lpc(lprefc);
			// Clean the cache:
			this.lsf = null;
			this.lpcc = null;
		}

		/**
		 * Check for the stability of the LPC filter.
		 * 
		 * @return true if the filter is stable, false otherwise.
		 */
		public boolean isStable() {
			/* If the reflection coeffs have not been cached before, compute them: */
			if (this.lprefc == null)
				this.lprefc = ReflectionCoefficients.lpc2lprefc(oneMinusA);
			/*
			 * Check the stability condition: a LPC filter is stable if all its reflection coefficients have values in the
			 * interval [-1.0 1.0].
			 */
			for (int i = 0; i < this.lprefc.length; i++) {
				if ((this.lprefc[i] > 1.0) || (this.lprefc[i] < -1.0))
					return (false);
			}
			return (true);
		}
	}
}
