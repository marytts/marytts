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
public interface DoubleDataSource {
    public int NOT_SPECIFIED = -1;
    /**
     * Request to get the specified amount of data in a new double array.
     * @param amount the number of doubles requested
     * @return a new double array; its length may be smaller than amount if not
     * enough data could be obtained. If no data could be read, null is returned. 
     */
    public double[] getData(int amount);

    /**
     * Try to get as many double data from this DoubleDataSource as target can hold.
     * @param target the double array in which to write the data
     * @return the number of data items written into target. If the returned value is less than target.length,
     * only that many data items have been copied into target; further calls will return 0 and not copy anything.
     */
    public int getData(double[] target);

    /**
     * Try to get length doubles from this DoubleDataSource, and copy them into target, starting from targetPos.
     * @param target the double array to write into
     * @param targetPos position in target where to start writing
     * @param length the amount of data requested
     * @return the amount of data actually delivered. If the returned value is less than length,
     * only that many data items have been copied into target; further calls will return 0 and not copy anything.
     */
    public int getData(double[] target, int targetPos, int length);

    /**
     * Whether or not any more data can be read from this data source.
     * @return true if another call to getData() will return data, false otherwise.
     */
    public boolean hasMoreData();
    
    /**
     * The number of doubles that can currently be read from this 
     * double data source without blocking. This number can change over time.
     * @return the number of doubles that can currently be read without blocking 
     */
    public int available();

    /**
     * Get all the data that can be read from this data source, in a single
     * double array.
     * @throws OutOfMemoryError if a sufficiently large double array cannot be created.
     * @return a double array of exactly the length required to contain all the data
     * that can be read from this source.  Returns an array of length 0 if no data can be
     * read from this source.
     */
    public double[] getAllData();
    
    /**
     * Get the total length of the data in this data source, if available.
     * @return the number of doubles that can be read from this data source, or
     * DoubleDataSource.NOT_SPECIFIED if unknown.
     */
    public long getDataLength();

}