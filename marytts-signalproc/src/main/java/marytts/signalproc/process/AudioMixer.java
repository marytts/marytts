/**
 * Copyright 2000-2009 DFKI GmbH.
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

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.signal.SignalProcUtils;

//Mixes the given audio source with a pre-recorded audio file
//by handling continuity and energy levels in real-time
public class AudioMixer implements InlineDataProcessor {
	private int silenceStart; // Silence before starting the mix in samples
	private int silenceInBetween; // Total silent samples between two consecutive mixes
	private int samplingRate;
	AudioDoubleDataSource mixSignalSource;
	double[] mixSignal; //
	int mixStart; // Index to copy & paste rom mixSignal
	double mixAmount;
	double oneMinusMixAmount;
	int bufferSize;
	int quarterBufferSize;
	double dataEn;
	double mixEn;
	double scale;
	double avgEn;
	double[] frm;
	boolean bFixed; // Plays the sound in fixed amount in the background
	double dataEnLongTerm; // long term average sample energy of the input data
	int enLongTermSize;
	double[] enLongTermBuff;
	int enLongTermInd;
	boolean bFirstEnBuff;

	// stSil: silence before starting the mix in seconds
	// stBwn: silence between consecutive mixes in seconds
	// fs: sampling rate in Hz
	public AudioMixer(InputStream inputStream, double stSil, double stBwn, int fs, int buffSize, double amount, boolean isFixed) {
		AudioInputStream inputAudio = null;
		try {
			inputAudio = AudioSystem.getAudioInputStream(inputStream);
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (inputAudio != null) {
			int mixFs = (int) inputAudio.getFormat().getSampleRate();
			mixSignalSource = new AudioDoubleDataSource(inputAudio);
			this.samplingRate = fs;

			if (mixFs != fs) {
				System.out.println("Error! Sampling rates do not match, will not do any modificaiton on input");
				mixSignalSource = null;
			} else {
				int i;
				silenceStart = (int) Math.floor(stSil * samplingRate + 0.5);
				silenceInBetween = (int) Math.floor(stBwn * samplingRate + 0.5);
				silenceStart = Math.max(0, silenceStart);
				silenceInBetween = Math.max(0, silenceInBetween);

				mixSignal = new double[(int) mixSignalSource.getDataLength() + silenceInBetween];
				mixSignalSource.getData(mixSignal);
				avgEn = SignalProcUtils.getAverageSampleEnergy(mixSignal, (int) mixSignalSource.getDataLength());
				for (i = (int) mixSignalSource.getDataLength(); i < (int) mixSignalSource.getDataLength() + silenceInBetween; i++)
					mixSignal[i] = 0.0;

				mixStart = 0;
				mixAmount = amount;
				mixAmount = Math.max(mixAmount, 0.0);
				mixAmount = Math.min(mixAmount, 1.0);
				oneMinusMixAmount = 1.0 - mixAmount;
				bufferSize = buffSize;
				quarterBufferSize = (int) Math.floor(bufferSize * 0.25 + 0.5);
				frm = new double[bufferSize];
				bFixed = isFixed;
				scale = 1.0;
				enLongTermSize = 40;
				enLongTermBuff = new double[enLongTermSize];
				for (i = 0; i < enLongTermSize; i++)
					enLongTermBuff[i] = 0.0;

				bFirstEnBuff = true;
				enLongTermInd = -1;
			}
		} else {
			mixSignalSource = null;
		}
	}

	public void applyInline(double[] data, int pos, int len) {
		if (data.length == bufferSize) {
			int i;
			for (i = 0; i < bufferSize; i++) {
				frm[i] = mixSignal[(mixStart + i) % mixSignal.length];
			}

			if (!bFixed) {
				dataEn = Math.sqrt(SignalProcUtils.getAverageSampleEnergy(data));
				mixEn = Math.sqrt(SignalProcUtils.getAverageSampleEnergy(frm));
				scale = dataEn / mixEn;
			} else {
				dataEn = Math.sqrt(SignalProcUtils.getAverageSampleEnergy(data));
				enLongTermInd++;

				if (bFirstEnBuff) {
					if (enLongTermInd < enLongTermSize)
						enLongTermBuff[enLongTermInd] = dataEn;

					if (enLongTermInd == enLongTermSize - 1) {
						bFirstEnBuff = false;
						enLongTermInd = -1;
					}
				}

				dataEnLongTerm = 0.0;
				for (i = 0; i < enLongTermSize; i++)
					dataEnLongTerm += enLongTermBuff[i];

				dataEnLongTerm /= enLongTermSize;
				scale = 0.25 * dataEnLongTerm / avgEn;
			}

			for (i = 0; i < bufferSize; i++) {
				data[i] = oneMinusMixAmount * data[i] + mixAmount * scale * frm[i];
			}

			mixStart += quarterBufferSize;
		}
	}
}
