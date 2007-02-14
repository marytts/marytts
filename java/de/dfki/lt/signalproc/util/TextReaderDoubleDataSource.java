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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;


/**
 * @author Marc Schr&ouml;der
 * A Double Data Source reading doubles from a Reader, in their string representation.
 * The Reader is expected to contain the text representation of exactly one double per line.
 */
public class TextReaderDoubleDataSource extends BaseDoubleDataSource {
    protected BufferedReader reader;
    
    /**
     * Read Double data from a Text file. Read one double per line. 
     * @param reader
     */
    public TextReaderDoubleDataSource(File file) throws FileNotFoundException
    {
        this(new FileReader(file));
    }

    /**
     * Initialise this double data source with the reader from which a text
     * representation of doubles (one per line) can be read. 
     */
    public TextReaderDoubleDataSource(Reader reader) {
        this.reader = new BufferedReader(reader);
    }


    /**
     * Try to get length doubles from this DoubleDataSource, and copy them into target, starting from targetPos.
     * This is the core method getting the data. Subclasses may want to override this method.
     * If an exception occurs reading from the underlying reader, or converting data to double,
     * the method will print a stack trace to standard error, but otherwise will 
     * silently stop and behave as if all data was read.
     * @param target the double array to write into
     * @param targetPos position in target where to start writing
     * @param length the amount of data requested
     * @return the amount of data actually delivered. If the returned value is less than length,
     * only that many data items have been copied into target; further calls will return 0 and not copy anything.
     */
    public int getData(double[] target, int targetPos, int length)
    {
        for (int i=0; i<length; i++) {
            try {
                String line = reader.readLine().trim();
                double value = Double.parseDouble(line);
                target[targetPos+i] = value;                
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return i;
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
                return i;
            }
        }
        return length;
    }

    /**
     * Whether or not any more data can be read from this data source.
     * @return true if another call to getData() will return data, false otherwise.
     */
    public boolean hasMoreData()
    { 
        int c = -1;
        try {
            reader.mark(10);
            c = reader.read();
            reader.reset();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return c != -1;
    }

}
