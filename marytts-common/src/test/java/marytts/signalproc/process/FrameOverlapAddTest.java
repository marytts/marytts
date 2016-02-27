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
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.math.FFTTest;
import marytts.util.math.MathUtils;

import org.junit.Test;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class FrameOverlapAddTest {
	@Test
	public void testIdentity() {
		double[] signal = FFTTest.getSampleSignal(16000);
		int samplingRate = 8000;
		FrameOverlapAddSource ola = new FrameOverlapAddSource(new BufferedDoubleDataSource(signal), 2048, samplingRate, null);
		double[] result = ola.getAllData();
		double err = MathUtils.sumSquaredError(signal, result);
		assertTrue("Error: " + err, err < 1.E-19);
	}

	public void testStretch1() {
		double[] signal = FFTTest.getSampleSignal(2048 + 128);
		int samplingRate = 8000;
		double rateFactor = 0.5;
		NaiveVocoder nv = new NaiveVocoder(new BufferedDoubleDataSource(signal), samplingRate, rateFactor);
		double[] result = nv.getAllData();
		int expectedLength = nv.computeOutputLength(signal.length);
		assertTrue("Expected result length: " + expectedLength + ", found: " + result.length, result.length == expectedLength);
	}

	public void testStretch2() {
		double[] signal = FFTTest.getSampleSignal(16000);
		int samplingRate = 8000;
		double rateFactor = 0.5;
		NaiveVocoder nv = new NaiveVocoder(new BufferedDoubleDataSource(signal), samplingRate, rateFactor);
		double[] result = nv.getAllData();
		double meanSignalEnergy = MathUtils.mean(MathUtils.multiply(signal, signal));
		double meanResultEnergy = MathUtils.mean(MathUtils.multiply(result, result));
		double percentDifference = Math.abs(meanSignalEnergy - meanResultEnergy) / meanSignalEnergy * 100;
		assertTrue("Stretching changed signal energy by  " + percentDifference + "%", percentDifference < 6);
	}

}
