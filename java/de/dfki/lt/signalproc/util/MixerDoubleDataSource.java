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
import java.util.LinkedList;
import java.util.List;

/**
 * Create one DoubleDataSource from a parallel stream of DoubleDataSources.
 * The length of the resulting stream is the length of the longest stream. 
 * @author Marc Schr&ouml;der
 */
public class MixerDoubleDataSource extends BaseDoubleDataSource {
    protected DoubleDataSource[] sources;
    protected boolean hasMoreData = true;
    
    /**
     * 
     * @param inputSource
     */
    public MixerDoubleDataSource(DoubleDataSource[] inputSources) {
        super();
        sources = new DoubleDataSource[inputSources.length];
        // The length of the mixed data is the length of the longest input
        dataLength = 0;
        for (int i=0; i<inputSources.length; i++) {
            if (dataLength != DoubleDataSource.NOT_SPECIFIED) {
                long dl = inputSources[i].getDataLength();
                if (dl == DoubleDataSource.NOT_SPECIFIED) dataLength = DoubleDataSource.NOT_SPECIFIED;
                else if (dl > dataLength) dataLength = dl;
            }
            if (inputSources[i] instanceof BlockwiseDoubleDataSource) {
                sources[i] = new BufferedDoubleDataSource(inputSources[i]);
            } else {
                sources[i] = inputSources[i];
            }
        }
    }

    /**
     * 
     * @param inputSources a list of DoubleDataSource objects.
     */
    public MixerDoubleDataSource(List inputSources) {
        this((DoubleDataSource[]) inputSources.toArray(new DoubleDataSource[0]));
    }

    public boolean hasMoreData()
    {
        if (!hasMoreData) return false;
        // else, we may have more data. Check:
        boolean foundOneWithData = false;
        for (int i=0; i<sources.length; i++)
            if (sources[i].hasMoreData()) {
                foundOneWithData = true;
                break;
            }
        if (foundOneWithData) {
            return true;
        } else {
            hasMoreData = false;
            return false;
        }
    }

    /**
     * The number of doubles that can currently be read from this 
     * double data source without blocking. This number can change over time.
     * @return the number of doubles that can currently be read without blocking 
     */
    public int available()
    {
        // Return the value for the longest source
        int available = 0;
        for (int i=0; i<sources.length; i++) {
            int a = sources[i].available();
            if (a>available) available = a;
        }
        return available;
        
    }
    
    public int getData(double[] target, int targetPos, int length)
    {
        if (target.length - targetPos < length) {
            throw new IllegalArgumentException("Target array cannot hold enough data ("+(target.length-targetPos) + " left, but " + length + " requested)");
        }
        double[] buf = new double[length];
        int maxCopied = 0;
        
        for (int i=0; i<sources.length; i++) {
            int copied = sources[i].getData(buf);
            if (copied > maxCopied){
                // only initialise to 0 those sections that are actually written to:
                Arrays.fill(target, targetPos+maxCopied, targetPos+copied, 0.);
                maxCopied = copied;
            }
            for (int j=0; j<copied; j++) {
                target[targetPos+j] += buf[j];
            }
        }
        return maxCopied;
    }

}
