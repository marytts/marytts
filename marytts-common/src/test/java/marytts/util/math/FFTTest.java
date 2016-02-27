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
package marytts.util.math;

import marytts.signalproc.display.FunctionGraph;
import marytts.signalproc.filter.FIRFilter;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class FFTTest {
	protected int LEN = 1024;
	protected int ONE = LEN / 10;
	protected double[] x1;
	protected double[] x2;
	protected double[] y;

	protected FunctionGraph showGraph(double[] array, String title) {
		FunctionGraph graph = new FunctionGraph(400, 200, 0, 1. / ONE, array);
		graph.showInJFrame(title, 500, 300, true, false);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		return graph;
	}

	@Before
	public void setUp() {
		x1 = new double[ONE];
		for (int i = 0; i < ONE; i++)
			x1[i] = 2 * (1 - (double) i / ONE);
		x2 = new double[LEN];
		for (int i = 0; i < LEN; i++)
			x2[i] = (double) i / LEN;
		y = FFT.convolveWithZeroPadding(x1, x2, 1. / ONE);

	}

	@Test
	public void testTransform() {
		double[] signal = getSampleSignal(1024);
		double[] real = new double[signal.length];
		System.arraycopy(signal, 0, real, 0, signal.length);
		double[] imag = new double[signal.length];
		FFT.transform(real, imag, false);
		FFT.transform(real, imag, true);
		double err = MathUtils.sumSquaredError(signal, real);
		Assert.assertTrue("Error: " + err, err < 1.E-16);
	}

	@Test
	public void testConvolution() {
		Assert.assertTrue(y.length == x1.length + x2.length);
	}

	@Test
	public void testFIRConvolution() {
		double[] signal = x2;
		double[] ir = x1;
		double[] resultingSignal = new double[signal.length];
		double[] reference = new double[signal.length];
		int initialTrim = ir.length / 2;
		int finalTrim = ir.length - initialTrim;
		// reference is convolution trimmed by the impulse response length:
		System.arraycopy(y, initialTrim, reference, 0, reference.length);
		assert reference.length == y.length - initialTrim - finalTrim;
		int shift = LEN / 2 - ir.length;
		FIRFilter filter = new FIRFilter(ir, shift);
		DoubleDataSource filtered = filter.apply(new BufferedDoubleDataSource(signal));
		int pos = 0;
		int iteration = 1;
		while (filtered.hasMoreData()) {
			int read = filtered.getData(resultingSignal, pos, shift);
			pos += read;
			// showGraph(resultingSignal, "resultingSignal after iteration " + (iteration++));
		}
		for (int i = 0; i < resultingSignal.length; i++) {
			resultingSignal[i] *= 1. / ONE;
		}
		/*
		 * showGraph(resultingSignal, "resultingSignal"); showGraph(y, "y"); showGraph(ir, "impulse response"); showGraph(signal,
		 * "signal");
		 * 
		 * try {Thread.sleep(100000);}catch(Exception e) {}
		 */

		double err = MathUtils.sumSquaredError(reference, resultingSignal);
		Assert.assertTrue("Error: " + err, err < 1.E-20);
	}

	public static double[] getSampleSignal(int length) {
		double[] signal = new double[length];
		for (int i = 0; i < length; i++) {
			signal[i] = Math.round(10000 * Math.sin(2 * Math.PI * i / length)) / 32768.0;
		}
		return signal;
	}

	public static double[] getSampleSignal(int lengthInSamples, int samplingFrequency, int signalFrequency) {
		double[] signal = new double[lengthInSamples];
		for (int i = 0; i < lengthInSamples; i++) {
			double factor = MathUtils.TWOPI * signalFrequency / samplingFrequency;
			signal[i] = Math.round(10000 * Math.sin(factor * i)) / 32768.0;
		}
		return signal;
	}

}
