/**
 * Copyright 2004-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package de.dfki.lt.signalproc.util;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class BlockwiseDoubleDataSource extends BufferedDoubleDataSource {
    protected int blockSize;
    
    /**
     * @param inputSource
     */
    public BlockwiseDoubleDataSource(DoubleDataSource inputSource, int blockSize) {
        super(inputSource);
        this.blockSize = blockSize;
    }

    /**
     * Attempt to get more data from the input source. If less than this can be read,
     * the possible amount will be read, but canReadMore() will return false afterwards.
     * @param minLength the amount of data to get from the input source
     * @return true if the requested amount could be read, false if none or less data could be read.
     */
    protected boolean readIntoBuffer(int minLength)
    {
        if (bufferSpaceLeft()<minLength) {
            // current buffer cannot hold the data requested;
            // need to make it larger
            increaseBufferSize(minLength+currentlyInBuffer());
        } else if (buf.length-writePos<minLength) {
            compact(); // create a contiguous space for the new data
        }
        // Now we have a buffer that can hold at least minLength new data points
        int readSum = 0;
        // read blocks:
        
        while (readSum < minLength && hasMoreData()) {
            prepareBlock();
            int blockSize = getBlockSize();
            if (buf.length < writePos + blockSize) {
                increaseBufferSize(writePos+blockSize);
            }
            int read = readBlock(buf, writePos);
            writePos += read;
            readSum += read;
        }
        if (dataProcessor != null) {
            dataProcessor.applyInline(buf, writePos-readSum, readSum);
        }
        return readSum >= minLength;
        
    }

    /**
     * Provide the size of the next block. This implementation returns the fixed
     * blocksize given in the constructor.
     * Subclasses may want to override this method.
     *
     */
    protected int getBlockSize() { return blockSize; }

    /**
     * Prepare a block of data for output. This method is called before readBlock() is called.
     * This implementation does nothing.
     * Subclasses will want to override this method.
     *
     */
    protected void prepareBlock() {}
    
    /**
     * Read a block of data. This method is called after prepareBlock() is called. 
     * This implementation simply reads getBlockSize()
     * data from the inputSource given in the constructor.
     * Subclasses will want to override this method.
     * @param target
     * @param pos
     * @return number of values written into target from position pos
     */
    protected int readBlock(double[] target, int pos)
    {
        return inputSource.getData(target, pos, getBlockSize());
    }
    

}
