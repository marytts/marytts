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
package marytts.unitselection.concat;

import marytts.signalproc.window.DynamicTwoHalvesWindow;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.Datagram;
import marytts.util.data.DoubleDataSource;

public class DatagramOverlapDoubleDataSource extends BufferedDoubleDataSource {
	protected Datagram[][] datagrams;
	protected Datagram[] rightContexts;
	protected int p; // point to current datagrams/rightContext
	protected int q; // point to current datagram within datagrams[p]
	protected int totalRead; // count samples read from datagrams

	/**
	 * Construct an double data source from the given array of datagram arrays and right contexts.
	 * 
	 * @param datagrams
	 *            datagrams
	 * @param rightContexts
	 *            rightContexts
	 */
	public DatagramOverlapDoubleDataSource(Datagram[][] datagrams, Datagram[] rightContexts) {
		super((DoubleDataSource) null);
		this.datagrams = datagrams;
		this.rightContexts = rightContexts;
		dataLength = 0;
		for (int i = 0; i < datagrams.length; i++) {
			for (int j = 0; j < datagrams[i].length; j++) {
				dataLength += datagrams[i][j].getDuration();
			}
		}
		p = 0;
		q = 0;
	}

	/**
	 * Whether or not any more data can be read from this data source.
	 * 
	 * @return true if another call to getData() will return data, false otherwise.
	 */
	public boolean hasMoreData() {
		if (currentlyInBuffer() > 0 || totalRead < dataLength)
			return true;
		return false;
	}

	/**
	 * The number of doubles that can currently be read from this double data source without blocking. This number can change over
	 * time.
	 * 
	 * @return the number of doubles that can currently be read without blocking
	 */
	public int available() {
		int available = (int) (currentlyInBuffer() + dataLength - totalRead);
		return available;
	}

	/**
	 * Attempt to get more data from the input source. If less than this can be read, the possible amount will be read, but
	 * canReadMore() will return false afterwards.
	 * 
	 * @param minLength
	 *            the amount of data to get from the input source
	 * @return true if the requested amount could be read, false if none or less data could be read.
	 */
	protected boolean readIntoBuffer(int minLength) {
		if (bufferSpaceLeft() < minLength) {
			// current buffer cannot hold the data requested;
			// need to make it larger
			increaseBufferSize(minLength + currentlyInBuffer());
		} else if (buf.length - writePos < minLength) {
			compact(); // create a contiguous space for the new data
		}
		// Now we have a buffer that can hold at least minLength new data points
		int readSum = 0;
		// read blocks:

		while (readSum < minLength && p < datagrams.length) {
			if (q >= datagrams[p].length) {
				p++;
				q = 0;
			} else {
				Datagram next = datagrams[p][q];
				int length = (int) next.getDuration();
				// System.out.println("Unit duration = " + String.valueOf(length));
				if (buf.length < writePos + length) {
					increaseBufferSize(writePos + length);
				}
				int read = readDatagram(next, buf, writePos);
				if (q == 0 && p > 0 && rightContexts[p - 1] != null) {
					// overlap-add situation
					// window the data that we have just read with the left half of a HANN window:
					new DynamicTwoHalvesWindow(Window.HANNING).applyInlineLeftHalf(buf, writePos, read);
					// and overlap-add the previous right context, windowed with the right half of a HANN window:
					double[] context = new double[(int) rightContexts[p - 1].getDuration()];
					readDatagram(rightContexts[p - 1], context, 0);
					new DynamicTwoHalvesWindow(Window.HANNING).applyInlineRightHalf(context, 0, context.length);
					for (int i = 0, iMax = Math.min(read, context.length); i < iMax; i++) {
						buf[writePos + i] += context[i];
					}
				}
				writePos += read;
				readSum += read;
				totalRead += read;
				q++;
			}
		}
		if (dataProcessor != null) {
			dataProcessor.applyInline(buf, writePos - readSum, readSum);
		}
		return readSum >= minLength;
	}

	protected int readDatagram(Datagram d, double[] target, int pos) {
		int dur = (int) d.getDuration();
		byte[] frameAudio = d.getData();
		assert frameAudio.length / 2 == dur : "expected datagram data length to be " + (dur * 2) + ", found " + frameAudio.length;
		for (int i = 0; i < frameAudio.length; i += 2, pos++) {
			int sample;
			byte lobyte;
			byte hibyte;
			// big endian:
			lobyte = frameAudio[i + 1];
			hibyte = frameAudio[i];
			sample = hibyte << 8 | lobyte & 0xFF;
			target[pos] = sample / 32768.0;// normalise to range [-1, 1];
		}
		return dur;
	}
}
