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
package marytts.signalproc.process;

import java.util.Arrays;

import marytts.signalproc.analysis.LpcAnalyser;
import marytts.signalproc.analysis.LpcAnalyser.LpCoeffs;
import marytts.util.math.ComplexArray;
import marytts.util.math.FFT;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author Oytun T&uuml;rk
 * 
 */
public class VocalTractModifier implements InlineDataProcessor {

	protected int p;
	protected int fs;
	protected int fftSize;
	protected int maxFreq;
	protected ComplexArray h;
	protected double[] vtSpectrum;
	private ComplexArray expTerm;
	private boolean bAnalysisOnly;
	public static int tmpCount = 0;

	/**
     * 
     */

	// For derived classes which will call initialise on their own
	public VocalTractModifier() {

	}

	public VocalTractModifier(int pIn, int fsIn, int fftSizeIn) {
		initialise(pIn, fsIn, fftSizeIn);
	}

	public void initialise(int pIn, int fsIn, int fftSizeIn) {
		initialise(pIn, fsIn, fftSizeIn, false);
	}

	// If bAnalysisOnly is true, it will not process the spectrum and after each call to applyInline, you will obtain
	// the real valued vocal tract spectrum in vtSpectrum and the ComplexArray valued excitation spectrum in real and imag
	public void initialise(int pIn, int fsIn, int fftSizeIn, boolean bAnalysisOnlyIn) {
		this.p = pIn;
		this.fs = fsIn;
		this.fftSize = fftSizeIn;
		fftSize = MathUtils.closestPowerOfTwoAbove(fftSize);
		h = new ComplexArray(fftSize);
		this.maxFreq = SignalProcUtils.halfSpectrumSize(fftSize);
		this.vtSpectrum = new double[maxFreq];
		this.expTerm = new ComplexArray(p * maxFreq);
		this.expTerm = LpcAnalyser.calcExpTerm(fftSize, p);
		this.bAnalysisOnly = bAnalysisOnlyIn;
	}

	public void applyInline(double[] data, int pos, int len) {
		int k;
		assert pos == 0;
		assert len == data.length;

		tmpCount++;

		if (len > fftSize)
			len = fftSize;

		if (len < fftSize) {
			double[] data2 = new double[fftSize];
			System.arraycopy(data, 0, data2, 0, data.length);
			Arrays.fill(data2, data.length, data2.length - 1, 0);
			data = new double[data2.length];
			System.arraycopy(data2, 0, data, 0, data2.length);
		}

		double origAvgEnergy = SignalProcUtils.getAverageSampleEnergy(data);

		// Compute LPC coefficients
		LpCoeffs coeffs = LpcAnalyser.calcLPC(data, p);
		double sqrtGain = coeffs.getGain();

		System.arraycopy(data, 0, h.real, 0, Math.min(len, h.real.length));

		if (h.real.length > len)
			Arrays.fill(h.real, h.real.length - len, h.real.length - 1, 0);

		Arrays.fill(h.imag, 0, h.imag.length - 1, 0);

		// Convert to polar coordinates in frequency domain
		// h = FFTMixedRadix.fftComplexArray(h);
		FFT.transform(h.real, h.imag, false);

		vtSpectrum = LpcAnalyser.calcSpecLinear(coeffs.getA(), p, fftSize, expTerm);

		for (k = 0; k < maxFreq; k++)
			vtSpectrum[k] *= sqrtGain;

		// Filter out vocal tract to obtain residual spectrum
		for (k = 0; k < maxFreq; k++) {
			h.real[k] /= vtSpectrum[k];
			h.imag[k] /= vtSpectrum[k];
		}

		if (!bAnalysisOnly) {
			// Process vocal tract spectrum
			processSpectrum(vtSpectrum);

			// Apply modified vocal tract filter on the residual spectrum
			for (k = 0; k < maxFreq; k++) {
				h.real[k] *= vtSpectrum[k];
				h.imag[k] *= vtSpectrum[k];
			}

			// Generate the complex conjugate part to make the output the DFT of a real-valued signal
			for (k = maxFreq; k < fftSize; k++) {
				h.real[k] = h.real[2 * maxFreq - k];
				h.imag[k] = -1 * h.imag[2 * maxFreq - k];
			}
			//

			// h = FFTMixedRadix.ifft(h);
			FFT.transform(h.real, h.imag, true);

			double newAvgEnergy = SignalProcUtils.getAverageSampleEnergy(h.real, len);
			double scale = origAvgEnergy / newAvgEnergy;

			for (k = 0; k < len; k++)
				h.real[k] *= scale;

			System.arraycopy(h.real, 0, data, 0, len);
		}
	}

	// Overload this function in the derived classes to modify the vocal tract spectrum Px in anyway you wish
	protected void processSpectrum(double[] Px) {
	}

}
