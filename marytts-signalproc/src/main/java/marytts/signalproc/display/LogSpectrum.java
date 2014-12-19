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

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.util.math.MathUtils;

/**
 * 
 * @author Marc Schr&ouml;der
 * 
 *         Computes log of DFT spectrum
 * 
 */
public class LogSpectrum extends SignalSpectrum {
	public LogSpectrum(AudioInputStream ais) {
		this(ais, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public LogSpectrum(AudioInputStream ais, int width, int height) {
		super(ais, width, height);
	}

	public LogSpectrum(final double[] signal, int samplingRate) {
		this(signal, samplingRate, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public LogSpectrum(final double[] signal, int samplingRate, int width, int height) {
		super(signal, samplingRate, width, height);
	}

	/**
	 * Compute log spectrum.
	 * 
	 * @param freqs
	 *            the frequencies that come out of the FFT.
	 */
	protected void process(double[] freqs) {
		for (int i = 0; i < freqs.length; i++) {
			// convert frequency amplitudes into energy, then into db.
			freqs[i] = MathUtils.db(freqs[i] * freqs[i]);
		}
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[i]));
			LogSpectrum signalSpectrum = new LogSpectrum(ais);
			signalSpectrum.showInJFrame(args[i], true, false);
		}
	}

}
