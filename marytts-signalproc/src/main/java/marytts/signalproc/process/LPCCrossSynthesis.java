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
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.analysis.LpcAnalyser;
import marytts.signalproc.analysis.LpcAnalyser.LpCoeffs;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.SequenceDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;

/**
 * @author Marc Schr&ouml;der
 * 
 */
public class LPCCrossSynthesis extends LPCAnalysisResynthesis {
	protected FrameProvider newResidualAudioFrames;

	public LPCCrossSynthesis(FrameProvider newResidualAudioFrames, int p) {
		super(p);
		this.newResidualAudioFrames = newResidualAudioFrames;
	}

	/**
	 * Replace residual with new residual from audio signal, adapting the gain in order to maintain overall volume.
	 */
	protected void processLPC(LpCoeffs coeffs, double[] residual) {
		double gain = coeffs.getGain();
		double[] frame = newResidualAudioFrames.getNextFrame();
		assert frame.length == residual.length;
		int excP = 3;
		LpCoeffs newCoeffs = LpcAnalyser.calcLPC(frame, excP);
		double newResidualGain = newCoeffs.getGain();
		// double[] newResidual = ArrayUtils.subarray(new FIRFilter(oneMinusA).apply(frame),0,frame.length);
		// System.arraycopy(newResidual, 0, residual, 0, residual.length);
		double gainFactor = gain / newResidualGain;
		Arrays.fill(residual, 0);
		for (int n = 0; n < residual.length; n++) {
			for (int i = 0; i <= excP && i <= n; i++) {
				residual[n] += newCoeffs.getOneMinusA(i) * frame[n - i];
			}
			residual[n] *= gainFactor;
		}
		// System.out.println("Gain:" + coeffs.getGain() + ", otherGain:"+newCoeffs.getGain()+", factor="+gainFactor);

	}

	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		AudioInputStream newResidualAudio = AudioSystem.getAudioInputStream(new File(args[1]));
		DoubleDataSource newResidual = new AudioDoubleDataSource(newResidualAudio);
		int frameLength = Integer.getInteger("signalproc.lpcanalysisresynthesis.framelength", 512).intValue();
		int predictionOrder = Integer.getInteger("signalproc.lpcanalysisresynthesis.predictionorder", 20).intValue();
		DoubleDataSource padding1 = new BufferedDoubleDataSource(new double[3 * frameLength / 4]);
		DoubleDataSource paddedExcitation = new SequenceDoubleDataSource(new DoubleDataSource[] { padding1, newResidual });
		FrameProvider newResidualAudioFrames = new FrameProvider(paddedExcitation, Window.get(Window.HANNING, frameLength, 0.5),
				frameLength, frameLength / 4, samplingRate, true);
		FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANNING, false, frameLength, samplingRate,
				new LPCCrossSynthesis(newResidualAudioFrames, predictionOrder));
		DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
		String outFileName = args[0].substring(0, args[0].length() - 4) + "_"
				+ args[1].substring(args[1].lastIndexOf("\\") + 1, args[1].length() - 4) + "_lpcCrossSynth.wav";
		AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		long endTime = System.currentTimeMillis();
		System.out.println("LPC cross synthesis took " + (endTime - startTime) + " ms");

	}

}
