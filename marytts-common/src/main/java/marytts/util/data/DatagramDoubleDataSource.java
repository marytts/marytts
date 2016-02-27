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
package marytts.util.data;

import java.util.LinkedList;

public class DatagramDoubleDataSource extends BufferedDoubleDataSource {
	protected LinkedList<Datagram> datagrams;

	/**
	 * Construct an double data source from the given array of datagrams.
	 * 
	 * @param datagrams
	 *            datagrams
	 */
	public DatagramDoubleDataSource(Datagram[] datagrams) {
		super((DoubleDataSource) null);
		this.datagrams = new LinkedList<Datagram>();
		dataLength = 0;
		for (int i = 0; i < datagrams.length; i++) {
			dataLength += datagrams[i].getDuration();
			this.datagrams.add(datagrams[i]);
		}
	}

	/**
	 * Construct an double data source from the given array of datagrams.
	 * 
	 * @param datagrams
	 *            datagrams
	 */
	public DatagramDoubleDataSource(LinkedList<Datagram> datagrams) {
		super((DoubleDataSource) null);
		this.datagrams = datagrams;
		dataLength = 0;
		for (Datagram d : datagrams) {
			dataLength += d.getDuration();
		}
	}

	/**
	 * Whether or not any more data can be read from this data source.
	 * 
	 * @return true if another call to getData() will return data, false otherwise.
	 */
	public boolean hasMoreData() {
		if (currentlyInBuffer() > 0 || !datagrams.isEmpty())
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
		int available = currentlyInBuffer();
		for (Datagram d : datagrams) {
			available += d.getDuration();
		}
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

		while (readSum < minLength && !datagrams.isEmpty()) {
			Datagram next = datagrams.removeFirst();
			int length = (int) next.getDuration();
			if (buf.length < writePos + length) {
				increaseBufferSize(writePos + length);
			}
			int read = readDatagram(next, buf, writePos);
			writePos += read;
			readSum += read;
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
