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

package de.dfki.lt.mary.unitselection.concat;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import de.dfki.lt.mary.unitselection.Datagram;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DoubleDataSource;

public class DatagramDoubleDataSource extends BufferedDoubleDataSource
{
    protected LinkedList<Datagram> datagrams;
    
    /**
     * Construct an double data source from the given array of datagrams.
     * @param datagrams
     */
    public DatagramDoubleDataSource(Datagram[] datagrams)
    {
        super((DoubleDataSource)null);
        this.datagrams = new LinkedList<Datagram>();
        dataLength = 0;
        for (int i=0; i<datagrams.length; i++) {
            dataLength += datagrams[i].getDuration();
            this.datagrams.add(datagrams[i]);
        }
    }

    /**
     * Construct an double data source from the given array of datagrams.
     * @param datagrams
     */
    public DatagramDoubleDataSource(LinkedList<Datagram> datagrams)
    {
        super((DoubleDataSource)null);
        this.datagrams = datagrams;
        dataLength = 0;
        for (Datagram d : datagrams) {
            dataLength += d.getDuration();
        }
    }

    /**
     * Whether or not any more data can be read from this data source.
     * @return true if another call to getData() will return data, false otherwise.
     */
    public boolean hasMoreData()
    {
        if (currentlyInBuffer() > 0 || !datagrams.isEmpty())
            return true;
        return false;
    }
   
    /**
     * The number of doubles that can currently be read from this 
     * double data source without blocking. This number can change over time.
     * @return the number of doubles that can currently be read without blocking 
     */
    public int available()
    {
        int available = currentlyInBuffer();
        for (Datagram d : datagrams) {
            available += d.getDuration();
        }
        return available;
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
        
        while (readSum < minLength && !datagrams.isEmpty()) {
            Datagram next = datagrams.removeFirst();
            int length = (int) next.getDuration();
            if (buf.length < writePos + length) {
                increaseBufferSize(writePos+length);
            }
            int read = readDatagram(next, buf, writePos);
            writePos += read;
            readSum += read;
        }
        if (dataProcessor != null) {
            dataProcessor.applyInline(buf, writePos-readSum, readSum);
        }
        return readSum >= minLength;
    }

    protected int readDatagram(Datagram d, double[] target, int pos)
    {
        int dur = (int) d.getDuration();
        byte[] frameAudio = d.getData();
        assert frameAudio.length / 2 == dur : "expected datagram data length to be "+(dur*2)+", found "+frameAudio.length;
        for (int i=0; i<frameAudio.length; i+=2, pos++) {
            int sample;
            byte lobyte;
            byte hibyte;
            // big endian:
            lobyte = frameAudio[i+1];
            hibyte = frameAudio[i];
            sample = hibyte<<8 | lobyte&0xFF;
            target[pos] = sample / 32768.0;// normalise to range [-1, 1];
        }


        return dur;
    }

}
