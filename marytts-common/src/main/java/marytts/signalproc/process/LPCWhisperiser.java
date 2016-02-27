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

import marytts.signalproc.analysis.LpcAnalyser.LpCoeffs;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;

/**
 * @author Marc Schr&ouml;der
 * 
 */
public class LPCWhisperiser extends LPCAnalysisResynthesis {
	protected double whisperAmount; // Amount of whispered voice at the output between 0.5 (half whispered+half unmodified) and
									// 1.0 (full whispered)
	protected double oneMinusWhisperAmount; // 1.0-whisperAmount

	public LPCWhisperiser(int predictionOrder, double amount) {
		super(predictionOrder);
		this.whisperAmount = MathUtils.trimToRange(amount, 0., 1.);
		this.oneMinusWhisperAmount = 1.0 - this.whisperAmount;
	}

	public LPCWhisperiser(int predictionOrder) {
		super(predictionOrder);
		whisperAmount = 1.0;
	}

	/**
	 * Replace residual with white noise
	 */
	protected void processLPC(LpCoeffs coeffs, double[] residual) {
		// Determine average residual energy:
		double totalResidualEnergy = coeffs.getGain() * coeffs.getGain();
		double avgAbsAmplitude = Math.sqrt(totalResidualEnergy / residual.length);
		double maxAbsAmplitude = 2 * avgAbsAmplitude;
		double spread = 2 * maxAbsAmplitude;
		for (int i = 0; i < residual.length; i++)
			residual[i] = whisperAmount * spread * (Math.random() - 0.5) + oneMinusWhisperAmount * residual[i];
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
			int samplingRate = (int) inputAudio.getFormat().getSampleRate();
			AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
			int frameLength = Integer.getInteger("signalproc.lpcanalysissynthesis.framelength", 512).intValue();
			int predictionOrder = Integer.getInteger("signalproc.lpcwhisperiser.predictionorder", 20).intValue();
			FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANNING, true, frameLength, samplingRate,
					new LPCWhisperiser(predictionOrder));
			DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
			String outFileName = args[i].substring(0, args[i].length() - 4) + "_lpcwhisperised.wav";
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		}

	}

}
