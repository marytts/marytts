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
package marytts.signalproc.analysis;

import java.io.File;
import java.io.FileReader;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.Defaults;
import marytts.signalproc.analysis.FrameBasedAnalyser.FrameAnalysisResult;
import marytts.signalproc.analysis.LpcAnalyser.LpCoeffs;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.text.ESTTextfileDoubleDataSource;

/**
 * 
 * A Pitch-synchronous LPC analyser.
 * 
 * @author Marc Schr&ouml;der
 * 
 */
public class LpcPitchSynchronousAnalyser extends PitchFrameAnalyser {
	public static int lpOrder = 0;

	/**
	 * Initialise a PitchLPCAnalyser.
	 * 
	 * @param signal
	 *            the signal source to read from
	 * @param pitchmarks
	 *            the source of the pitchmarks, in seconds from the start of signal
	 * @param windowType
	 *            type of analysis window to use, @see{de.dfki.signalproc.window.Window#getAvailableTypes()}
	 * @param samplingRate
	 *            the number of samples in one second.
	 */
	public LpcPitchSynchronousAnalyser(DoubleDataSource signal, DoubleDataSource pitchmarks, int windowType, int samplingRate) {
		super(signal, pitchmarks, windowType, samplingRate);
	}

	/**
	 * Create a new PitchLPCAnalyser with a configurable number of pitch periods per frame and pitch periods to shift by.
	 * 
	 * @param signal
	 *            audio signal
	 * @param pitchmarks
	 *            an array of pitchmarks; each pitch mark is in seconds from signal start
	 * @param windowType
	 *            type of analysis window to use, @see{de.dfki.signalproc.window.Window#getAvailableTypes()}
	 * @param samplingRate
	 *            number of samples per second in signal
	 * @param framePeriods
	 *            number of periods that each frame should contain
	 * @param shiftPeriods
	 *            number of periods that frames should be shifted by
	 */
	public LpcPitchSynchronousAnalyser(DoubleDataSource signal, DoubleDataSource pitchmarks, int windowType, int samplingRate,
			int framePeriods, int shiftPeriods) {
		super(signal, pitchmarks, windowType, samplingRate, framePeriods, shiftPeriods);
	}

	/**
	 * Apply this PitchFrameAnalyser to the given data.
	 * 
	 * @param frame
	 *            the data to analyse, which must be of the length prescribed by this FrameBasedAnalyser, i.e. by
	 *            {@link #getFrameLengthSamples()}.
	 * @return an LPCoeffs object representing the lpc coefficients and gain factor of the frame.
	 * @throws IllegalArgumentException
	 *             if frame does not have the prescribed length
	 */
	public Object analyse(double[] frame) {
		// for assertion only:
		int expectedFrameLength = 0;
		for (int i = 0; i < periodLengths.length; i++) {
			expectedFrameLength += periodLengths[i];
		}
		if (frame.length != expectedFrameLength)
			System.err.println("Expected frame of length " + expectedFrameLength + "(" + periodLengths.length + " periods)"
					+ ", got " + frame.length);

		return LpcAnalyser.calcLPC(frame, lpOrder);
	}

	public static void main(String[] args) throws Exception {
		File audioFile = new File(args[0]);
		File pitchmarkFile = new File(args[1]);
		AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile);
		int samplingRate = (int) ais.getFormat().getSampleRate();
		DoubleDataSource signal = new AudioDoubleDataSource(ais);
		DoubleDataSource pitchmarks = new ESTTextfileDoubleDataSource(new FileReader(pitchmarkFile));

		int windowType = Defaults.getWindowType();
		int fftSize = Defaults.getFFTSize();
		int p = Integer.getInteger("signalproc.lpcorder", 24).intValue();

		LpcPitchSynchronousAnalyser pla = new LpcPitchSynchronousAnalyser(signal, pitchmarks, windowType, samplingRate, 2, 1);
		FrameAnalysisResult[] far = pla.analyseAllFrames();
		for (int i = 0; i < far.length; i++) {
			LpCoeffs coeffs = (LpCoeffs) far[i].get();
			System.out.print(far[i].getStartTime() + ": gain " + coeffs.getGain() + ", coffs: ");
			for (int j = 0; j < coeffs.getOrder(); j++) {
				System.out.print(coeffs.getA(j) + "  ");
			}
			System.out.println();

		}
	}

}
