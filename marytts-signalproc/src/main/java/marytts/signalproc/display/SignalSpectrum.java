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
package marytts.signalproc.display;

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.util.data.audio.MaryAudioUtils;
import marytts.util.math.FFT;
import marytts.util.math.MathUtils;

/**
 * 
 * @author Marc Schr&ouml;der
 * 
 *         Displays the DFT spectrum of input signal
 * 
 */
public class SignalSpectrum extends FunctionGraph {
	public SignalSpectrum(AudioInputStream ais) {
		this(ais, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public SignalSpectrum(AudioInputStream ais, int width, int height) {
		super();
		if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
			ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
		}
		if (ais.getFormat().getChannels() > 1) {
			throw new IllegalArgumentException("Can only deal with mono audio signals");
		}
		int samplingRate = (int) ais.getFormat().getSampleRate();
		double[] signal = MaryAudioUtils.getSamplesAsDoubleArray(ais);
		initialise(signal, samplingRate, width, height);
	}

	public SignalSpectrum(final double[] signal, int samplingRate) {
		this(signal, samplingRate, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public SignalSpectrum(final double[] signal, int samplingRate, int width, int height) {
		initialise(signal, samplingRate, width, height);
	}

	protected void initialise(final double[] signal, int samplingRate, int width, int height) {
		int N = signal.length;
		if (!MathUtils.isPowerOfTwo(N)) {
			N = MathUtils.closestPowerOfTwoAbove(N);
		}
		double[] ar = new double[N];
		System.arraycopy(signal, 0, ar, 0, signal.length);
		// Transform:
		FFT.realTransform(ar, false);
		double[] freqs = FFT.computeAmplitudeSpectrum_FD(ar);
		process(freqs);
		double deltaF = (double) samplingRate / N;
		super.initialise(width, height, 0, deltaF, freqs);
	}

	/**
	 * Subclass can use this to compute power or log spectrum
	 * 
	 * @param freqs
	 *            the frequencies that come out of the FFT.
	 */
	protected void process(double[] freqs) {
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[i]));
			SignalSpectrum signalSpectrum = new SignalSpectrum(ais);
			signalSpectrum.showInJFrame(args[i], true, false);
		}
	}

}
