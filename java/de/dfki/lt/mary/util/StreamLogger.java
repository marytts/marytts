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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;


/**
 * Read from a stream and log.
 * @author Marc Schr&ouml;der
 */

public class StreamLogger extends Thread
{
    private InputStream is;
    private Logger logger;
    private Pattern ignorePattern = null;

    /**
     * Read from an input stream, logging to category <code>logCategory</code>,
     * ignoring lines matching
     * the regular expression specified in <code>ignorePattern</code>.
     * If <code>logCategory</code> is <code>null</code>, "unnamed" will be used.
     * If <code>ignorePattern</code> is <code>null</code>, no filtering will be
     * performed.
     * The thread will silently die when it reaches end-of-file from the input
     * stream.
     */
    public StreamLogger(InputStream is, String logCategory, String ignorePattern)
    {
        this.is = is;
        if (logCategory == null)
            logger = Logger.getLogger("unnamed");
        else
            logger = Logger.getLogger(logCategory);
        if (ignorePattern != null) {
            try {
                this.ignorePattern = Pattern.compile(ignorePattern);
            } catch (PatternSyntaxException e) {
                logger.warn("Problem with regular expression pattern", e);
                this.ignorePattern = null;
            }
        }
    }

    public void run()
    {
        String line = null;
        try {
            BufferedReader b = new BufferedReader(new InputStreamReader(is));
            while ((line = b.readLine()) != null) {
                if (ignorePattern != null && ignorePattern.matcher(line).matches())
                    continue; // do not log
                logger.info(line);
            }
        } catch (IOException e) {
            logger.warn("Cannot read from stream", e);
        }
    }
}

