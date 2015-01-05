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
package marytts.signalproc.filter;

import static org.junit.Assert.assertTrue;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.FFTTest;
import marytts.util.math.MathUtils;

import org.junit.Test;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class FilterTest {
	@Test
	public void testSameLength() {
		int samplingFrequency = 16000;
		double lengthInSeconds = 0.01;
		int lengthInSamples = (int) (lengthInSeconds * samplingFrequency);
		int signalFrequency = 1000; // in Hz
		double[] signal = FFTTest.getSampleSignal(lengthInSamples, samplingFrequency, signalFrequency);
		int cutoffFrequencyInHz = 100;
		double normalisedCutoff = (double) cutoffFrequencyInHz / samplingFrequency;
		HighPassFilter filter = new HighPassFilter(normalisedCutoff);
		double[] result = filter.apply(signal);
		/*
		 * SignalGraph graph = new SignalGraph(signal, samplingFrequency); graph.showInJFrame("Test signal", true, true);
		 * SignalGraph graph2 = new SignalGraph(result, samplingFrequency); graph2.showInJFrame("Test result", true, true); try
		 * {Thread.sleep(100000);} catch(Exception e) {}
		 */
		assertTrue("Result has length " + result.length + " instead of expected " + signal.length
				+ " (impulse response length is " + filter.getImpulseResponseLength() + ")", result.length == signal.length);
	}

	@Test
	public void highPassFilter() {
		int samplingFrequency = 16000;
		double lengthInSeconds = 0.01;
		int lengthInSamples = (int) (lengthInSeconds * samplingFrequency);
		int signalFrequency = 1000; // in Hz
		double[] signal = FFTTest.getSampleSignal(lengthInSamples, samplingFrequency, signalFrequency);
		int cutoffFrequencyInHz = 50;
		double normalisedCutoff = (double) cutoffFrequencyInHz / samplingFrequency;
		FIRFilter filter = new HighPassFilter(normalisedCutoff, 0.002);

		double[] result = filter.apply(signal);
		double err = MathUtils.sumSquaredError(signal, result);
		double criterion = 0.01;
		/*
		 * if (err > criterion) { SignalGraph graph = new SignalGraph(signal, samplingFrequency); graph.addDataSeries(result,
		 * Color.RED, FunctionGraph.DRAW_LINE, -1); graph.showInJFrame("Test signal", true, true); try {Thread.sleep(100000);}
		 * catch(Exception e) {} }
		 */
		assertTrue("Error: " + err, err < criterion);
	}

	@Test
	public void lowPassFilter() throws Exception {
		AudioInputStream ais = AudioSystem.getAudioInputStream(FilterTest.class.getResourceAsStream("arctic_a0123.wav"));
		double[] signal = new AudioDoubleDataSource(ais).getAllData();
		double normalisedCutoff = 0.5 - 1.e-10; // i.e., low-pass filter at the Nyquist frequency -- expect identity
		double[] result = new LowPassFilter(normalisedCutoff, 0.002).apply(signal);
		double err = MathUtils.sumSquaredError(signal, result);
		double criterion = 1.e-20;
		assertTrue("Error: " + err, err < criterion);
	}

}
