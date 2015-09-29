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

/**
 * @author Oytun T&uuml;rk
 * 
 */
public class Chorus implements InlineDataProcessor {
	private double[] buffIn;
	private double[] buffOut;
	private boolean[] bFirstInBuff;
	private int[] delays;
	private double[] amps;

	private int buffInStart;
	private int numChannels;
	private double sumAmps;

	public Chorus(int samplingRate) {
		this(null, null, samplingRate);
	}

	public Chorus(int[] delaysInMiliseconds, double[] ampsIn, int samplingRate) {
		// If null parameters, use a default chorus
		boolean bDelete = false;
		if (delaysInMiliseconds == null) {
			bDelete = true;

			delaysInMiliseconds = new int[2];
			delaysInMiliseconds[0] = 466;
			delaysInMiliseconds[1] = 600;

			ampsIn = new double[2];
			ampsIn[0] = 0.54;
			ampsIn[1] = -0.10;
		}

		numChannels = Math.min(delaysInMiliseconds.length, ampsIn.length);

		if (numChannels > 0) {
			delays = new int[numChannels];
			amps = new double[numChannels];
			bFirstInBuff = new boolean[numChannels];

			int i;
			for (i = 0; i < numChannels; i++)
				delays[i] = (int) (Math.floor(delaysInMiliseconds[i] / 1000.0 * samplingRate + 0.5));

			int buffInLen = delays[0];
			for (i = 0; i < numChannels; i++) {
				if (buffInLen < delays[i])
					buffInLen = delays[i];
			}

			if (buffInLen < 1)
				buffInLen = 1;

			buffIn = new double[buffInLen];
			for (i = 0; i < buffInLen; i++)
				buffIn[i] = 0.0;

			buffOut = null;

			buffInStart = 1;
			for (i = 0; i < numChannels; i++)
				amps[i] = ampsIn[i];

			sumAmps = 1.0;
			for (i = 0; i < numChannels; i++)
				sumAmps += amps[i];

			for (i = 0; i < numChannels; i++)
				bFirstInBuff[i] = true;
		} else {
			buffIn = null;
			buffOut = null;
			bFirstInBuff = null;
			delays = null;
			amps = null;

			buffInStart = 1;
			numChannels = 0;
			sumAmps = 1.0;
		}

		if (bDelete) {
			delaysInMiliseconds = null;
			ampsIn = null;
		}
	}

	public void applyInline(double[] data, int pos, int buffOutLen) {
		if (buffOutLen != data.length)
			buffOutLen = data.length;

		if (buffOutLen > 0) {
			// Perform processing on each channel
			if (buffOut == null || buffOut.length != buffOutLen)
				buffOut = new double[buffOutLen];

			int i, j, ind;
			for (i = 0; i < buffOutLen; i++)
				buffOut[i] = 0.0;

			for (j = 1; j <= buffOutLen; j++) {
				buffIn[buffInStart - 1] = data[j - 1];

				for (i = 1; i <= numChannels; i++) {
					if (i == 1)
						buffOut[j - 1] = 1.0 / sumAmps * buffIn[buffInStart - 1]; // Delay-less channel

					ind = buffInStart - delays[i - 1];

					if (!(bFirstInBuff[i - 1]) && ind < 1)
						ind += buffIn.length;

					if (buffInStart + 1 > buffIn.length)
						bFirstInBuff[i - 1] = false;

					if (ind >= 1)
						buffOut[j - 1] += amps[i - 1] / sumAmps * buffIn[ind - 1];
				}

				buffInStart++;

				if (buffInStart > buffIn.length)
					buffInStart = 1;
			}

			for (i = 0; i < buffOutLen; i++)
				data[i] = buffOut[i];
		}
	}

	public static void main(String[] args) throws Exception {
		// Simple stadium effect
		int[] delaysInMiliseconds = { 366, 500 };
		double[] amps = { 0.54, -0.10 };
		//

		for (int i = 0; i < args.length; i++) {
			AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
			int samplingRate = (int) inputAudio.getFormat().getSampleRate();
			AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
			FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANNING, true, 1024, samplingRate, new Chorus(
					delaysInMiliseconds, amps, samplingRate));
			DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
			String outFileName = args[i].substring(0, args[i].length() - 4) + "_chorusAdded.wav";
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		}
	}

}
