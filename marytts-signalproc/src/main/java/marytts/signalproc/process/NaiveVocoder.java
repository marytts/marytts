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

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.display.FunctionGraph;
import marytts.signalproc.display.SignalGraph;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.MathUtils;

/**
 * A naive overlap-add time stretching algorithm without any phase correction; used only for demonstrating the artefacts that
 * arise from not correcting phase.
 * 
 * @author Marc Schr&ouml;der
 * 
 */
public class NaiveVocoder extends FrameOverlapAddSource {
	public static final int DEFAULT_FRAMELENGTH = 2048;
	protected double rateChangeFactor;

	/**
	 * @param inputSource
	 *            input source
	 * @param samplingRate
	 *            sampling rate
	 * @param rateChangeFactor
	 *            the factor by which to speed up or slow down the source. Values greater than one will speed up, values smaller
	 *            than one will slow down the original.
	 */
	public NaiveVocoder(DoubleDataSource inputSource, int samplingRate, double rateChangeFactor) {
		this.rateChangeFactor = rateChangeFactor;
		initialise(inputSource, Window.HANNING, true, DEFAULT_FRAMELENGTH, samplingRate, null);
	}

	protected int getInputFrameshift(int outputFrameshift) {
		int inputFrameshift = (int) (outputFrameshift * rateChangeFactor);
		double actualFactor = (double) inputFrameshift / outputFrameshift;
		if (rateChangeFactor != actualFactor) {
			System.err.println("With output frameshift " + outputFrameshift + ", need to adjust rate change factor to "
					+ actualFactor);
			rateChangeFactor = actualFactor;
		}
		return inputFrameshift;
	}

	/**
	 * Based on the given rate change factor, compute the exact length change factor for a given signal length, based on the
	 * current frame length and input/output frame shifts.
	 * 
	 * From the illustrations in @see{FrameOverlapAddSource}, it can be seen that for a given frame length f and frame shift s,
	 * the length of a signal can be described as <code>l(n) = f + n*s - delta</code>.
	 * 
	 * f is fixed; s is si for input frameshift, so for output frameshift. For a given input length, one can compute n and rest
	 * and thus compute the output length.
	 * 
	 * @param inputLengthInSamples
	 *            inputLengthInSamples
	 * @return the output length
	 */
	public int computeOutputLength(int inputLengthInSamples) {
		int f = frameProvider.getFrameLengthSamples();
		int so = blockSize; // output frameshift
		int si = frameProvider.getFrameShiftSamples(); // input frameshift
		assert si == getInputFrameshift(so);
		int n = (int) Math.ceil(((double) inputLengthInSamples - f) / si);
		int delta = f + n * si - inputLengthInSamples;
		// System.err.println("li="+inputLengthInSamples+", f="+f+", si="+si+", n="+n+", delta="+delta+", => f+n*si-delta="+(f+n*si-delta));
		assert delta < si;
		int lo = f + n * so - delta;
		// System.err.println("so="+so+", => lo="+lo);
		return lo;
	}

	public static void main(String[] args) throws Exception {
		for (int i = 1; i < args.length; i++) {
			AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
			int samplingRate = (int) inputAudio.getFormat().getSampleRate();
			double[] signal = new AudioDoubleDataSource(inputAudio).getAllData();
			FunctionGraph signalGraph = new SignalGraph(signal, samplingRate);
			signalGraph.showInJFrame("signal", true, true);
			// SignalSpectrum signalSpectrum = new SignalSpectrum(signal, samplingRate);
			// signalSpectrum.showInJFrame("signal", true, true);
			double rateFactor = Double.parseDouble(args[0]);
			NaiveVocoder pv = new NaiveVocoder(new BufferedDoubleDataSource(signal), samplingRate, rateFactor);
			double[] result = pv.getAllData();
			FunctionGraph resultGraph = new SignalGraph(result, samplingRate);
			resultGraph.showInJFrame("result", true, true);
			// Spectrogram resultSpectrogram = new Spectrogram(result, samplingRate);
			// resultSpectrogram.showInJFrame("result", true, true);
			// SignalSpectrum resultSpectrum = new SignalSpectrum(result, samplingRate);
			// resultSpectrum.showInJFrame("result", true, true);
			System.err.println("Signal has length " + signal.length + ", result " + result.length);
			if (signal.length == result.length) {
				double err = MathUtils.sumSquaredError(signal, result);
				System.err.println("Sum squared error: " + err);
				// double[] difference = MathUtils.substract(signal, result);
				// FunctionGraph diffGraph = new SignalGraph(difference, samplingRate);
				// diffGraph.showInJFrame("difference", true, true);
			}
			System.err.println("Expected result length: " + pv.computeOutputLength(signal.length) + ", found: " + result.length);
			double meanSignalEnergy = MathUtils.mean(MathUtils.multiply(signal, signal));
			double meanResultEnergy = MathUtils.mean(MathUtils.multiply(result, result));
			System.err.println("Mean result energy: " + (meanResultEnergy / meanSignalEnergy * 100) + "% of mean signal energy");

			// DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(pv),
			// inputAudio.getFormat());
			// String outFileName = args[i].substring(0, args[i].length()-4) + "_copy.wav";
			// AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		}

	}

}
