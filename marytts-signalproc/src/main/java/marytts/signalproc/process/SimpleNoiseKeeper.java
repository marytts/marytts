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
package marytts.signalproc.process;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;

/**
 * 
 * @author Marc Schr&ouml;der
 * 
 *         A simple implementation of a separator of periodic and aperiodic components, based on an attenuation function for
 *         signals below a given threshold. This class provides the aperiodic (noise) components, as the exact complement of @see
 *         SimpleNoiseRemover.
 * 
 */
public class SimpleNoiseKeeper extends PolarFrequencyProcessor {
	protected double threshold;

	public SimpleNoiseKeeper(int fftSize, double threshold) {
		super(fftSize);
		this.threshold = threshold;
	}

	/**
	 * Perform the attenuation of low-intensity frequency components.
	 * 
	 * @param r
	 *            amplitude of FFT
	 * @param phi
	 *            phase of FFT
	 */
	protected void processPolar(double[] r, double[] phi) {
		int halfWinLength = r.length / 2;
		for (int i = 0; i < r.length; i++) {
			double rNorm = r[i] / halfWinLength;
			double factor = rNorm / (rNorm + threshold);
			// System.out.println("Threshold "+threshold+", rNorm "+rNorm+", factor "+factor);
			r[i] *= 1 - factor;
		}
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
			int samplingRate = (int) inputAudio.getFormat().getSampleRate();
			AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
			int frameLength = Integer.getInteger("signalproc.simplenoisekeeper.framelength", 1024).intValue();
			double threshold = Double.parseDouble(System.getProperty("signalproc.simplenoisekeeper.threshold", "50.0"));
			FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANNING, true, frameLength, samplingRate,
					new SimpleNoiseKeeper(frameLength, threshold));
			DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
			String outFileName = args[i].substring(0, args[i].length() - 4) + "_noiseonly.wav";
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		}

	}

}
