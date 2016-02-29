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
package marytts.signalproc.sinusoidal.test;

import java.io.IOException;

import marytts.util.string.StringUtils;

/**
 * Generates test signals that consist of an artificially generated harmonic part and artificially generated noise part
 * 
 * @author oytun.turk
 * 
 */
public class HarmonicsNoiseTester extends SinusoidsNoiseTester {

	public HarmonicsNoiseTester(HarmonicsTester s, NoiseTester n) {
		super(s, n);
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws IOException {
		int i;
		HarmonicsTester s = null;
		NoiseTester n = null;
		HarmonicsNoiseTester h = null;

		// Harmonics part
		float f1 = 400.f;
		int numHarmonics = 8;
		float harmonicsStartTimeInSeconds = 0.0f;
		float harmonicsEndTimeInSeconds = 1.0f;
		s = new HarmonicsTester(f1, numHarmonics, harmonicsStartTimeInSeconds, harmonicsEndTimeInSeconds);
		//

		// Noise part
		int numNoises = 1;
		float[][] freqs = new float[numNoises][];
		float[] amps = new float[numNoises];
		float noiseStartTimeInSeconds = 0.0f;
		float noiseEndTimeInSeconds = 1.0f;
		for (i = 0; i < numNoises; i++)
			freqs[i] = new float[2];

		freqs[0][0] = 4000;
		freqs[0][1] = 6000;
		amps[0] = DEFAULT_AMP;

		n = new NoiseTester(freqs, amps, noiseStartTimeInSeconds, noiseEndTimeInSeconds);
		//

		String wavFile = args[0];
		String ptcFile;
		if (args.length > 1)
			ptcFile = args[1];
		else
			ptcFile = StringUtils.modifyExtension(wavFile, "ptc");

		String fileExt = StringUtils.getFileExtension(wavFile, true);
		String harmonicsWavFile = StringUtils.getFolderName(wavFile) + StringUtils.getFileName(wavFile, true) + "_harmonicsOrig"
				+ fileExt;
		String noiseWavFile = StringUtils.getFolderName(wavFile) + StringUtils.getFileName(wavFile, true) + "_noiseOrig"
				+ fileExt;
		s.write(harmonicsWavFile);
		n.write(noiseWavFile);

		h = new HarmonicsNoiseTester(s, n);

		if (args.length > 1)
			h.write(wavFile, ptcFile);
		else
			h.write(wavFile);
	}

}
