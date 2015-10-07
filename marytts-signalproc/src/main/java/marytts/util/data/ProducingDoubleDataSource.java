/**
 * Copyright 2009 DFKI GmbH.
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

package marytts.util.data;

import java.util.concurrent.ArrayBlockingQueue;

import marytts.signalproc.process.InlineDataProcessor;

/**
 * @author marc
 *
 */
public abstract class ProducingDoubleDataSource extends BufferedDoubleDataSource implements Runnable {
	private static final Double END_OF_STREAM = Double.NEGATIVE_INFINITY;

	protected ArrayBlockingQueue<Double> queue = new ArrayBlockingQueue<Double>(1024);
	private Thread dataProducingThread = null;
	private boolean hasSentEndOfStream = false;
	private boolean hasReceivedEndOfStream = false;

	protected ProducingDoubleDataSource() {
		this(DoubleDataSource.NOT_SPECIFIED);
	}

	protected ProducingDoubleDataSource(long numDataThatWillBeProduced) {
		this(numDataThatWillBeProduced, null);
	}

	protected ProducingDoubleDataSource(InlineDataProcessor dataProcessor) {
		this(DoubleDataSource.NOT_SPECIFIED, dataProcessor);
	}

	protected ProducingDoubleDataSource(long numDataThatWillBeProduced, InlineDataProcessor dataProcessor) {
		super((DoubleDataSource) null, dataProcessor);
		this.dataLength = numDataThatWillBeProduced;
	}

	public void start() {
		dataProducingThread = new Thread(this);
		dataProducingThread.setDaemon(true);
		dataProducingThread.start();
	}

	/**
	 * Subclasses must implement this method such that it produces data and sends it through {@link #putOneDataPoint(double)}.
	 * When all data is sent, the subclass must call {@link #putEndOfStream()} exactly once.
	 */
	public abstract void run();

	/**
	 * The producing thread tries to put ont data item into the queue.
	 * 
	 * @param value
	 *            value
	 * @throws RuntimeException
	 *             runtime exception
	 */
	public void putOneDataPoint(double value) {
		try {
			queue.put(value);
		} catch (InterruptedException e) {
			throw new RuntimeException("Unexpected interruption", e);
		}
	}

	protected void putEndOfStream() {
		putOneDataPoint(END_OF_STREAM);
		hasSentEndOfStream = true;
	}

	@Override
	public boolean hasMoreData() {
		checkStarted();
		return !isAllProductionDataRead() || available() > 0;
	}

	@Override
	public int available() {
		checkStarted();
		return currentlyInBuffer() + currentlyInQueue();
	}

	private int currentlyInQueue() {
		if (isAllProductionDataRead()) {
			return 0;
		}
		int inQueue = queue.size();
		if (hasSentEndOfStream && !hasReceivedEndOfStream) {
			inQueue -= 1;
		}
		return inQueue;
	}

	@Override
	protected boolean readIntoBuffer(int minLength) {
		checkStarted();
		if (isAllProductionDataRead()) {
			return false;
		}
		if (bufferSpaceLeft() < minLength) {
			// current buffer cannot hold the data requested;
			// need to make it larger
			increaseBufferSize(minLength + currentlyInBuffer());
		} else if (buf.length - writePos < minLength) {
			compact(); // create a contiguous space for the new data
		}
		// Now we have a buffer that can hold at least minLength new data points
		int readSum = 0;
		while (readSum < minLength) {
			double data = getOneDataPoint();
			if (data == END_OF_STREAM) {
				hasReceivedEndOfStream = true;
				break;
			}
			buf[writePos] = data;
			writePos++;
			readSum++;
		}
		if (dataProcessor != null) {
			dataProcessor.applyInline(buf, writePos - readSum, readSum);
		}
		return readSum == minLength;
	}

	/**
	 * The reading thread tries to get one data item from the queue.
	 * 
	 * @return queue.take
	 */
	private double getOneDataPoint() {
		try {
			return queue.take();
		} catch (InterruptedException e) {
			throw new RuntimeException("Unexpected interruption", e);
		}
	}

	/**
	 * @throws IllegalStateException
	 */
	private void checkStarted() throws IllegalStateException {
		if (!isStarted()) {
			throw new IllegalStateException("Producer thread has not been started -- call start()");
		}
	}

	private boolean isStarted() {
		return dataProducingThread != null;
	}

	private boolean isAllProductionDataRead() {
		return hasReceivedEndOfStream;
	}
}
