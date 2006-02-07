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

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.log4j.Logger;

public class LoggingReader extends FilterReader
{
    protected Logger logger;
    protected StringBuffer logText;

    public LoggingReader(Reader in, Logger logger)
    {
        super(in);
        this.logger = logger;
        logText = new StringBuffer();
    }

    public int read() throws IOException
    {
        int c = super.read();
        if (c == -1) {
            logRead();
        } else {
            logText.append((char)c);
        }
        return c;
    }

    public int read(char[] cbuf, int off, int len) throws IOException
    {
        int nr = super.read(cbuf, off, len);
        if (nr == -1) {
            logRead();
        } else {
            logText.append(new String(cbuf, off, nr));
        }
        return nr;
    }

    public void close() throws IOException
    {
        super.close();
        logRead();
    }

    public void logRead()
    {
        if (logText.length() > 0) {
            logger.info("Read:\n" + logText.toString());
            logText.setLength(0);
        }
    }
}
