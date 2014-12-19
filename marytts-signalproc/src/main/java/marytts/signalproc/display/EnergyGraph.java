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

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.analysis.EnergyAnalyser;
import marytts.signalproc.analysis.FrameBasedAnalyser;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;

/**
 * @author Marc Schr&ouml;der
 * 
 */
public class EnergyGraph extends FunctionGraph {
	public EnergyGraph(AudioInputStream ais) {
		this(ais, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public EnergyGraph(AudioInputStream ais, int width, int height) {
		super();
		if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
			ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
		}
		if (ais.getFormat().getChannels() > 1) {
			throw new IllegalArgumentException("Can only deal with mono audio signals");
		}
		int samplingRate = (int) ais.getFormat().getSampleRate();
		DoubleDataSource signal = new AudioDoubleDataSource(ais);
		initialise(signal, samplingRate, width, height);
	}

	public EnergyGraph(double[] signal, int samplingRate) {
		this(signal, samplingRate, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public EnergyGraph(double[] signal, int samplingRate, int width, int height) {
		initialise(new BufferedDoubleDataSource(signal), samplingRate, width, height);
	}

	protected void initialise(DoubleDataSource signal, int samplingRate, int width, int height) {
		double frameDuration = 0.01; // seconds
		int frameLength = (int) (samplingRate * frameDuration);
		int frameShift = frameLength / 2;
		if (frameLength % 2 == 0)
			frameLength++; // make sure frame length is odd
		EnergyAnalyser energyAnalyser = new EnergyAnalyser(signal, frameLength, frameShift, samplingRate);
		FrameBasedAnalyser.FrameAnalysisResult[] results = energyAnalyser.analyseAllFrames();
		double[] energyData = new double[results.length];
		for (int i = 0; i < results.length; i++) {
			energyData[i] = ((Double) results[i].get()).doubleValue();
		}
		super.initialise(width, height, 0, (double) frameShift / samplingRate, energyData);
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[i]));
			EnergyGraph signalGraph = new EnergyGraph(ais);
			signalGraph.showInJFrame(args[i], true, false);
		}
	}
}
