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
import marytts.util.math.MathUtils;

/**
 * @author Marc Schr&ouml;der
 * 
 *         A simple whisperiser destroying the voicedness of the speech by setting all phases to random.
 * 
 */
public class Whisperiser extends PolarFrequencyProcessor {
	public Whisperiser(int fftSize, double amount) {
		super(fftSize, amount);
	}

	public Whisperiser(int fftSize) {
		super(fftSize, 1.0);
	}

	/**
	 * Perform the random phase manipulation.
	 * 
	 * @param r
	 *            r
	 * @param phi
	 *            phi
	 */
	protected void processPolar(double[] r, double[] phi) {
		for (int i = 0; i < r.length; i++) {
			phi[i] = MathUtils.TWOPI * Math.random();
		}
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
			int samplingRate = (int) inputAudio.getFormat().getSampleRate();
			AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
			int frameLength = Integer.getInteger("signalproc.whisperiser.framelength", 64).intValue();
			FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANNING, true, frameLength, samplingRate,
					new Whisperiser(frameLength));
			DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
			String outFileName = args[i].substring(0, args[i].length() - 4) + "_whisperised.wav";
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		}

	}

}
