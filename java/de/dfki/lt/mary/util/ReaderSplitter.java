/**
 * Copyright 2000-2006 DFKI GmbH.
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
package de.dfki.lt.mary.util;

// Java classes
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * A class splitting a Reader into chunks.
 * In a continuous input Reader, search for lines containing
 *          a specific "end-of-chunk" marking (e.g., an XML root end tag),
 *          and return individual readers, each of which will provide
 *          one chunk (including the line containing the end-of-chunk marking).
 * @author Marc Schr&ouml;der
 */

public class ReaderSplitter
{
    private BufferedReader in;
    private StringBuffer buf;
    private String endMarker;

    public ReaderSplitter(Reader in, String endMarker)
    {
        this.in = new BufferedReader(in);
        this.endMarker = endMarker;
        buf = new StringBuffer(1000);
    }

    /**
     * Return a reader from which one chunk can be read, followed by EOF.
     * Chunks are delimited by start of file, lines containing the end marker
     * string (line is last line in chunk), and end of file.
     * Returns null if nothing more can be read.
     */
    public Reader nextReader()
        throws IOException
    {
        String line = null;
        buf.setLength(0); // start with an empty buffer
        while ((line = in.readLine()) != null) {
            buf.append(line);
            buf.append(System.getProperty("line.separator"));
            if (line.indexOf(endMarker) != -1) { // found end marker in line
                break;
            }
        }
        if (buf.length() == 0) return null; // nothing more to read.
        return (Reader) new StringReader(buf.toString());
    }
}
