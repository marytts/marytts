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

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author Oytun T&uuml;rk
 * 
 */
public class VocalTractScalingProcessor extends VocalTractModifier {
	private double[] vscales;
	private double[] PxOut;

	/**
	 * @param p
	 *            p
	 * @param fs
	 *            fs
	 * @param fftSize
	 *            fftSize
	 * @param vscalesIn
	 *            vscalesIn
	 */
	public VocalTractScalingProcessor(int p, int fs, int fftSize, double[] vscalesIn) {
		super(p, fs, fftSize);

		PxOut = new double[this.maxFreq];

		if (vscalesIn.length > 0) {
			vscales = MathUtils.modifySize(vscalesIn, this.maxFreq); // Modify length to match current length of spectrum

			for (int i = 0; i < this.maxFreq; i++) {
				if (vscales[i] < 0.05)
					vscales[i] = 0.05; // Put a floor to avoid divide by zero
			}
		} else
			vscales = null;
	}

	protected void processSpectrum(double[] Px) {
		if (vscales != null) {
			/*
			 * //Scale the vocal tract int i; int wInd; for (i=1; i<=maxFreq; i++) { wInd =
			 * (int)(Math.floor(((double)i)/vscales[i-1]+0.5)); //Find new index if (wInd<1) wInd=1; if (wInd>maxFreq)
			 * wInd=maxFreq;
			 * 
			 * PxOut[i-1] = Px[wInd-1]; } //
			 * 
			 * //Copy the modified vocal tract spectrum to input System.arraycopy(PxOut, 0, Px, 0, maxFreq); //
			 */

			int newLen = (int) Math.floor(Px.length * vscales[0] + 0.5);

			double[] Px2 = MathUtils.interpolate(Px, newLen);

			int i;

			if (newLen > maxFreq) {
				for (i = 0; i < maxFreq; i++)
					Px[i] = Px2[i];
			} else {
				for (i = 0; i < newLen; i++)
					Px[i] = Px2[i];
				for (i = newLen; i < maxFreq; i++)
					Px[i] = 0.0;
			}
		}
	}

	public static void main(String[] args) throws Exception {
		double[] vscales = { 1.0 };

		for (int i = 0; i < args.length; i++) {
			AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
			int samplingRate = (int) inputAudio.getFormat().getSampleRate();
			int p = SignalProcUtils.getLPOrder(samplingRate);
			int fftSize = Math.max(SignalProcUtils.getDFTSize(samplingRate), 1024);
			AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
			FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANNING, true, fftSize, samplingRate,
					new VocalTractScalingProcessor(p, samplingRate, fftSize, vscales));
			DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
			String outFileName = args[i].substring(0, args[i].length() - 4) + "_vocalTractScaled.wav";
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		}

	}

}
