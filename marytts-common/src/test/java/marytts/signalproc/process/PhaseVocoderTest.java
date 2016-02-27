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

import static org.junit.Assert.assertTrue;

import java.awt.Color;

import marytts.signalproc.display.FunctionGraph;
import marytts.signalproc.display.SignalGraph;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.math.FFTTest;
import marytts.util.math.MathUtils;

import org.junit.Test;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class PhaseVocoderTest {
	@Test
	public void testIdentity() {
		double[] signal = FFTTest.getSampleSignal(16000);
		int samplingRate = 8000;
		PhaseVocoder pv = new PhaseVocoder(new BufferedDoubleDataSource(signal), samplingRate, 1);
		double[] result = pv.getAllData();
		double err = MathUtils.sumSquaredError(signal, result);
		if (err > 1.E-20) {
			SignalGraph graph = new SignalGraph(signal, 16000);
			graph.addDataSeries(result, Color.RED, FunctionGraph.DRAW_LINE, -1);
			graph.showInJFrame("Test signal", true, true);
			try {
				Thread.sleep(100000);
			} catch (Exception e) {
			}
		}
		assertTrue("Error: " + err, err < 1.E-15);
	}

	@Test
	public void testStretch1() {
		double[] signal = FFTTest.getSampleSignal(16000);
		int samplingRate = 8000;
		double rateFactor = 0.5;
		PhaseVocoder pv = new PhaseVocoder(new BufferedDoubleDataSource(signal), samplingRate, rateFactor);
		double[] result = pv.getAllData();
		int expectedLength = pv.computeOutputLength(signal.length);
		assertTrue("Expected result length: " + expectedLength + ", found: " + result.length, result.length == expectedLength);
	}

	@Test
	public void testStretch2() {
		double[] signal = FFTTest.getSampleSignal(16000);
		int samplingRate = 8000;
		double rateFactor = 0.5;
		PhaseVocoder pv = new PhaseVocoder(new BufferedDoubleDataSource(signal), samplingRate, rateFactor);
		double[] result = pv.getAllData();
		double meanSignalEnergy = MathUtils.mean(MathUtils.multiply(signal, signal));
		double meanResultEnergy = MathUtils.mean(MathUtils.multiply(result, result));
		double percentDifference = Math.abs(meanSignalEnergy - meanResultEnergy) / meanSignalEnergy * 100;
		assertTrue("Stretching changed signal energy by  " + percentDifference + "%", percentDifference < 2);
	}

}
