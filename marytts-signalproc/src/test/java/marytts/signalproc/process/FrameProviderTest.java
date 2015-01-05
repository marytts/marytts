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
public class FrameProviderTest {
	@Test
	public void testIdentity1() {
		// Signal length not a multiple of the frame length/shift:
		double[] signal = FFTTest.getSampleSignal(10000);
		int samplingRate = 8000;
		// Set stopWhenTouchingEnd to false: always read only the first frameShift samples in each frame:
		FrameProvider fp = new FrameProvider(new BufferedDoubleDataSource(signal), null, 2048, 512, samplingRate, false);
		double[] result = new double[signal.length];
		int resultPos = 0;
		while (fp.hasMoreData()) {
			double[] frame = fp.getNextFrame();
			int toCopy = fp.validSamplesInFrame() >= fp.getFrameShiftSamples() ? fp.getFrameShiftSamples() : fp
					.validSamplesInFrame();
			System.arraycopy(frame, 0, result, resultPos, toCopy);
			resultPos += toCopy;
		}
		assertTrue("Got back " + resultPos + ", expected " + signal.length, resultPos == signal.length);
		double err = MathUtils.sumSquaredError(signal, result);
		assertTrue("Error: " + err, err < 1.E-20);
	}

	@Test
	public void testIdentity2() {
		// Signal length not a multiple of the frame length/shift:
		double[] signal = FFTTest.getSampleSignal(10000);
		int samplingRate = 8000;
		// Set stopWhenTouchingEnd to true: read only the first frameShift samples in each frame,
		// except in the last frame, which is read in full:
		FrameProvider fp = new FrameProvider(new BufferedDoubleDataSource(signal), null, 2048, 512, samplingRate, true);
		double[] result = new double[signal.length];
		int resultPos = 0;
		while (fp.hasMoreData()) {
			double[] frame = fp.getNextFrame();
			int toCopy = fp.validSamplesInFrame() == fp.getFrameLengthSamples() ? fp.getFrameShiftSamples() : fp
					.validSamplesInFrame();
			System.arraycopy(frame, 0, result, resultPos, toCopy);
			resultPos += toCopy;
		}
		assertTrue("Got back " + resultPos + ", expected " + signal.length, resultPos == signal.length);
		double err = MathUtils.sumSquaredError(signal, result);
		assertTrue("Error: " + err, err < 1.E-20);
	}

	@Test
	public void testIdentity3() {
		// Signal length a multiple of the frame length/shift:
		double[] signal = FFTTest.getSampleSignal(10240);
		int samplingRate = 8000;
		// Set stopWhenTouchingEnd to false: always read only the first frameShift samples in each frame:
		FrameProvider fp = new FrameProvider(new BufferedDoubleDataSource(signal), null, 2048, 512, samplingRate, false);
		double[] result = new double[signal.length];
		int resultPos = 0;
		while (fp.hasMoreData()) {
			double[] frame = fp.getNextFrame();
			int toCopy = fp.validSamplesInFrame() >= fp.getFrameShiftSamples() ? fp.getFrameShiftSamples() : fp
					.validSamplesInFrame();
			System.arraycopy(frame, 0, result, resultPos, toCopy);
			resultPos += toCopy;
		}
		assertTrue("Got back " + resultPos + ", expected " + signal.length, resultPos == signal.length);
		double err = MathUtils.sumSquaredError(signal, result);
		assertTrue("Error: " + err, err < 1.E-20);
	}

	@Test
	public void testIdentity4() {
		// Signal length a multiple of the frame length/shift:
		double[] signal = FFTTest.getSampleSignal(10240);
		int samplingRate = 8000;
		// Set stopWhenTouchingEnd to true: read only the first frameShift samples in each frame,
		// except in the last frame, which is read in full:
		FrameProvider fp = new FrameProvider(new BufferedDoubleDataSource(signal), null, 2048, 512, samplingRate, true);
		double[] result = new double[signal.length];
		int resultPos = 0;
		while (fp.hasMoreData()) {
			double[] frame = fp.getNextFrame();
			int toCopy = fp.hasMoreData() ? fp.getFrameShiftSamples() : fp.validSamplesInFrame();
			System.arraycopy(frame, 0, result, resultPos, toCopy);
			resultPos += toCopy;
		}
		assertTrue("Got back " + resultPos + ", expected " + signal.length, resultPos == signal.length);
		double err = MathUtils.sumSquaredError(signal, result);
		assertTrue("Error: " + err, err < 1.E-20);
	}

}
