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

import marytts.signalproc.process.FrameProvider;
import marytts.signalproc.process.InlineDataProcessor;
import marytts.util.data.BlockwiseDoubleDataSource;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.SequenceDoubleDataSource;
import marytts.util.math.FFT;
import marytts.util.math.MathUtils;

/**
 * @author Marc Schr&ouml;der A filter corresponding to a finite-impulse-response LTI system. The filtering of the input signal
 *         corresponds to a convolution with the impulse response of the system.
 */
public class FIRFilter implements InlineDataProcessor {
	protected double[] transformedIR;
	protected int impulseResponseLength;
	protected int sliceLength;
	protected double[] denumeratorCoefficients; // Digital filter coefficients for time domain digital filtering

	/**
	 * Create a new, uninitialised FIR filter. Subclasses need to call
	 * 
	 * check {@link #initialise(double[] impulseResponse, int sliceLen)} .
	 */
	protected FIRFilter() {

	}

	/**
	 * Create a new Finite Impulse Response filter.
	 * 
	 * @param impulseResponse
	 *            the impulse response signal
	 */
	public FIRFilter(double[] impulseResponse) {
		int sliceLen = MathUtils.closestPowerOfTwoAbove(2 * impulseResponse.length) - impulseResponse.length;
		initialise(impulseResponse, sliceLen);
	}

	public FIRFilter(double[] impulseResponse, int len) {
		initialise(impulseResponse, len);
	}

	/**
	 * Initialise the Finite Impulse Response filter.
	 * 
	 * @param impulseResponse
	 *            the impulse response signal
	 * @param sliceLen
	 *            the length of the slices in which to process the input data. IMPORTANT: impulseResponse.length+sliceLength must
	 *            be a power of two (256, 512, etc.)
	 * @throws IllegalArgumentException
	 *             if the slice length is shorter than the impulse response length, or it the sum of both lengths is not a power
	 *             of two.
	 */
	protected void initialise(double[] impulseResponse, int sliceLen) {
		denumeratorCoefficients = new double[impulseResponse.length];
		System.arraycopy(impulseResponse, 0, denumeratorCoefficients, 0, impulseResponse.length);

		if (!MathUtils.isPowerOfTwo(impulseResponse.length + sliceLen))
			throw new IllegalArgumentException("Impulse response length plus slice length must be a power of two");
		this.impulseResponseLength = impulseResponse.length;
		this.sliceLength = sliceLen;
		transformedIR = new double[sliceLen + impulseResponse.length];

		System.arraycopy(impulseResponse, 0, transformedIR, 0, impulseResponse.length);
		FFT.realTransform(transformedIR, false);
		// This means, we are not actually saving the impulseResponse, but only
		// its complex FFT transform.
	}

	/**
	 * Apply this filter to the given input signal. The input signal is filtered piece by piece, as it is read from the data
	 * source returned by this method. This is the recommended way to filter longer signals.
	 * 
	 * @param signal
	 *            the signal to which this filter should be applied
	 * @return a DoubleDataSource from which the data can be read
	 */
	public DoubleDataSource apply(DoubleDataSource signal) {
		return new FIROutput(signal);
	}

	/**
	 * Apply this filter to the given input signal. This method filters the entire signal, and returns the entire filtered signal.
	 * For long signals, it is better to use apply(DoubleDataSource).
	 * 
	 * @param signal
	 *            the signal to which this filter should be applied
	 * @return the filtered signal.
	 */
	public double[] apply(double[] signal) {
		return new FIROutput(new BufferedDoubleDataSource(signal)).getAllData();
	}

	public class FIROutput extends BlockwiseDoubleDataSource {
		protected FrameProvider frameProvider;
		protected int nTailCutoff;

		public FIROutput(DoubleDataSource inputSource) {
			super(null, sliceLength);
			int samplingRate = 1; // unknown
			int frameLength = sliceLength + impulseResponseLength;
			assert MathUtils.isPowerOfTwo(frameLength);
			// Need to start with zero padding of length impulseResponseLength:
			DoubleDataSource padding = new BufferedDoubleDataSource(new double[impulseResponseLength]);
			DoubleDataSource paddedSource = new SequenceDoubleDataSource(new DoubleDataSource[] { padding, inputSource });
			this.frameProvider = new FrameProvider(paddedSource, null, frameLength, sliceLength, 1, false);
			// discard the initial padding of impulseResponseLength/2:
			int nHeadCutoff = impulseResponseLength / 2;
			nTailCutoff = impulseResponseLength - nHeadCutoff;
			getData(nHeadCutoff); // and discard
		}

		@Override
		public boolean hasMoreData() {
			return currentlyInBuffer() > 0 || frameProvider.hasMoreData();
		}

		/**
		 * This implementation of getData() will cut off a tail corresponding to half of the FIR filter.
		 */
		@Override
		public int getData(double[] target, int targetPos, int length) {
			// if (target.length < targetPos+length)
			// throw new IllegalArgumentException("Not enough space left in target array");
			int toRead = length + nTailCutoff;
			if (currentlyInBuffer() < toRead) { // first need to try and read some more data
				readIntoBuffer(toRead - currentlyInBuffer());
			}
			int toDeliver = length;
			if (currentlyInBuffer() < toRead) {
				toDeliver = currentlyInBuffer() - nTailCutoff;
				writePos -= nTailCutoff;
			}
			System.arraycopy(buf, readPos, target, targetPos, toDeliver);
			readPos += toDeliver;
			assert readPos <= writePos;
			return toDeliver;
		}

		/**
		 * Try to get a block of getBlockSize() doubles from this DoubleDataSource, and copy them into target, starting from
		 * targetPos.
		 * 
		 * @param target
		 *            the double array to write into
		 * @param targetPos
		 *            position in target where to start writing
		 * @return the amount of data actually delivered. If 0 is returned, all further calls will also return 0 and not copy
		 *         anything.
		 */
		@Override
		protected int readBlock(double[] target, int targetPos) {
			double[] frame = frameProvider.getNextFrame();
			if (frame == null) { // already finished processing
				return 0;
			}
			assert blockSize <= frameProvider.getFrameLengthSamples();
			assert blockSize == frameProvider.getFrameShiftSamples();
			// Now do the convolution:
			double[] convResult = FFT.convolve_FD(frame, transformedIR);
			int toCopy = blockSize;
			if (frameProvider.validSamplesInFrame() < blockSize)
				toCopy = frameProvider.validSamplesInFrame();
			// System.err.println("Copying " + toCopy);
			// Overlap-save approach:
			// always ignore the first "impulseResponseLength" samples in convResult,
			// because they are contaminated due to circular convolution:
			System.arraycopy(convResult, impulseResponseLength, target, targetPos, toCopy);
			return toCopy;
		}
	}

	public void applyInline(double[] data, int off, int len) {
		double[] dataOut = apply(data);

		System.arraycopy(dataOut, 0, data, 0, len);
	}

	public int getImpulseResponseLength() {
		return impulseResponseLength;
	}

	public double[] getDenumeratorCoefficients() {
		return denumeratorCoefficients;
	}
}
