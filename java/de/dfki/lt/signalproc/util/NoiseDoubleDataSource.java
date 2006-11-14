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

import java.util.Arrays;

public class NoiseDoubleDataSource extends BaseDoubleDataSource {
    protected long n;
    protected double amplitude;
    
    /**
     * Construct an double data source from which a given amount of
     * white noise can be read.
     * @param n the number of samles samples to be read
     * @param dB the energy of the noise, in dB relative to max amplitude.
     * This must be non-positive, and will typically be between -100 and -3.
     */
    public NoiseDoubleDataSource(long n, double dB) {
        super();
        this.n = n;
        dataLength = n;
        if (dB > 0) {
            throw new IllegalArgumentException("Energy must be non-positive");
        }
        this.amplitude = DDSAudioInputStream.MAX_AMPLITUDE * Math.sqrt(MathUtils.db2linear(dB));
    }

    public boolean hasMoreData()
    {
        return n > 0;
    }
    
    /**
     * The number of doubles that can currently be read from this 
     * double data source without blocking. This number can change over time.
     * @return the number of doubles that can currently be read without blocking 
     */
    public int available()
    {
        return (int) n;
    }
    
    public int getData(double[] target, int targetPos, int length)
    {
        if (target.length - targetPos < length) {
            throw new IllegalArgumentException("Target array cannot hold enough data ("+(target.length-targetPos) + " left, but " + length + " requested)");
        }
        int toCopy = (int) Math.min(length, n);
        for (int i=targetPos; i<targetPos+toCopy; i++) {
            target[i] = 2*(0.5-Math.random())*amplitude;
        }
        n -= toCopy;
        return toCopy;
    }

}
