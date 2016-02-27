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

/**
 * @author Marc Schr&ouml;der
 *
 */
public class BlockwiseDoubleDataSource extends BufferedDoubleDataSource {
	protected int blockSize;

	/**
	 * @param inputSource
	 *            inputSource
	 * @param blockSize
	 *            block size
	 */
	public BlockwiseDoubleDataSource(DoubleDataSource inputSource, int blockSize) {
		super(inputSource);
		this.blockSize = blockSize;
	}

	/**
	 * Attempt to get more data from the input source. If less than this can be read, the possible amount will be read, but
	 * canReadMore() will return false afterwards.
	 * 
	 * @param minLength
	 *            the amount of data to get from the input source
	 * @return true if the requested amount could be read, false if none or less data could be read.
	 */
	@Override
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

		while (readSum < minLength && hasMoreData()) {
			prepareBlock();
			int blockSize = getBlockSize();
			if (buf.length < writePos + blockSize) {
				increaseBufferSize(writePos + blockSize);
			}
			int read = readBlock(buf, writePos);
			if (read == 0) {
				break; // cannot read any more blocks
			}
			writePos += read;
			readSum += read;
		}
		if (dataProcessor != null) {
			dataProcessor.applyInline(buf, writePos - readSum, readSum);
		}
		return readSum >= minLength;

	}

	/**
	 * Provide the size of the next block. This implementation returns the fixed blocksize given in the constructor. Subclasses
	 * may want to override this method.
	 *
	 * @return blocksize
	 */
	protected int getBlockSize() {
		return blockSize;
	}

	/**
	 * Prepare a block of data for output. This method is called before readBlock() is called. This implementation does nothing.
	 * Subclasses will want to override this method.
	 *
	 */
	protected void prepareBlock() {
	}

	/**
	 * Read a block of data. This method is called after prepareBlock() is called. This implementation simply reads getBlockSize()
	 * data from the inputSource given in the constructor. Subclasses will want to override this method.
	 * 
	 * @param target
	 *            target
	 * @param pos
	 *            pos
	 * @return number of values written into target from position pos
	 */
	protected int readBlock(double[] target, int pos) {
		return inputSource.getData(target, pos, getBlockSize());
	}

}
