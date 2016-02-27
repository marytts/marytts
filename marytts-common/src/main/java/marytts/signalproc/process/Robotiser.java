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
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.signal.SignalProcUtils;

/**
 * @author Marc Schr&ouml;der
 * 
 *         Create a robot-like impression on the output, by setting all phases to zero in each frame. This effectively creates a
 *         pitch equalling the output frame shift.
 * 
 */
public class Robotiser extends FrameOverlapAddSource {
	/**
	 * @param inputSource
	 *            inputSource
	 * @param samplingRate
	 *            samplingRate
	 * @param amount
	 *            the factor by which to speed up or slow down the source. Values greater than one will speed up, values smaller
	 *            than one will slow down the original.
	 */
	public Robotiser(DoubleDataSource inputSource, int samplingRate, float amount) {
		// int frameLength = Integer.getInteger("signalproc.robotiser.framelength", 256).intValue();
		int frameLength = SignalProcUtils.getDFTSize(samplingRate);
		initialise(inputSource, Window.HANNING, true, frameLength, samplingRate, new PhaseRemover(frameLength, amount));
	}

	public Robotiser(DoubleDataSource inputSource, int samplingRate) {
		this(inputSource, samplingRate, 1.0f);
	}

	public static class PhaseRemover extends PolarFrequencyProcessor {
		public PhaseRemover(int fftSize, double amount) {
			super(fftSize, amount);
		}

		public PhaseRemover(int fftSize) {
			this(fftSize, 1.0);
		}

		/**
		 * Perform the random manipulation.
		 * 
		 * @param r
		 *            r
		 * @param phi
		 *            phi
		 */
		protected void processPolar(double[] r, double[] phi) {
			for (int i = 0; i < phi.length; i++) {
				phi[i] = 0;
			}
		}

	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
			int samplingRate = (int) inputAudio.getFormat().getSampleRate();
			AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
			Robotiser pv = new Robotiser(signal, samplingRate);
			DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(pv), inputAudio.getFormat());
			String outFileName = args[i].substring(0, args[i].length() - 4) + "_robotised.wav";
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		}
	}
}
