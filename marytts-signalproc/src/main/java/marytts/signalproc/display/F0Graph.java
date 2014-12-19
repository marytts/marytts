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
package marytts.signalproc.display;

import java.awt.Color;
import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.analysis.F0TrackerAutocorrelationHeuristic;
import marytts.signalproc.analysis.PitchFileHeader;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;

/**
 * @author Marc Schr&ouml;der
 * 
 */
public class F0Graph extends FunctionGraph {
	public F0Graph(AudioInputStream ais) {
		this(ais, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public F0Graph(AudioInputStream ais, int width, int height) {
		super();
		if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
			ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
		}
		if (ais.getFormat().getChannels() > 1) {
			throw new IllegalArgumentException("Can only deal with mono audio signals");
		}
		AudioDoubleDataSource signal = new AudioDoubleDataSource(ais);
		initialise(signal, signal.getSamplingRate(), width, height);
	}

	public F0Graph(double[] signal, int samplingRate) {
		this(signal, samplingRate, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public F0Graph(double[] signal, int samplingRate, int width, int height) {
		initialise(new BufferedDoubleDataSource(signal), samplingRate, width, height);
	}

	protected void initialise(DoubleDataSource signal, int samplingRate, int width, int height) {
		/*
		 * F0Tracker f0Tracker = new F0TrackerAutocorrelationDP(); F0Tracker.F0Contour f0Contour = f0Tracker.analyse(signal,
		 * samplingRate); double frameShiftTime = f0Contour.getFrameShiftTime(); double[] f0Array = f0Contour.getContour(); double
		 * xOffset = 0;
		 */
		PitchFileHeader params = new PitchFileHeader();
		params.fs = samplingRate;
		F0TrackerAutocorrelationHeuristic tracker = new F0TrackerAutocorrelationHeuristic(params);
		tracker.pitchAnalyze(signal);
		double frameShiftTime = tracker.getSkipSizeInSeconds();
		double[] f0Array = tracker.getF0Contour();
		double xOffset = tracker.getWindowSizeInSeconds() / 2;
		super.initialise(width, height, xOffset, frameShiftTime, f0Array);
		setPrimaryDataSeriesStyle(Color.RED, DRAW_DOTS, DOT_FULLDIAMOND);
		dotSize = 8;
	}

	public static void main(String[] args) throws Exception {
		AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[0]));
		F0Graph f0Graph = new F0Graph(ais);
		f0Graph.showInJFrame(args[0], true, true);
	}
}
