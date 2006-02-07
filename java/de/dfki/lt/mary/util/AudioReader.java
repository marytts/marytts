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

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;


/**
 * An convenience class copying audio data from an input stream
 * (e.g., a MARY module) to an AudioDestination object. Used by
 * java-external synthesis methods (SynthesisCallerBase and subclasses). 
 * @author Marc Schröder.
 *
 */
public class AudioReader extends Thread
{
    private InputStream from;
    private AudioDestination audioDestination;
    private byte[] endMarker;
    private long latestSeenTime;
    private Logger logger;
    
    public AudioReader(InputStream from, AudioDestination audioDestination)
    {
        this(from, audioDestination, null);
    }
    public AudioReader(InputStream from, AudioDestination audioDestination, String endMarker)
    {
        super(Thread.currentThread().getName() + " reader");
        this.from = from;
        this.audioDestination = audioDestination;
        this.endMarker = endMarker != null? endMarker.getBytes(): null;
        latestSeenTime = System.currentTimeMillis();
        logger = Logger.getLogger("Audio reader");
    }

    public void run() {
        byte[] bytes = new byte[8192];
        int nrRead;
        boolean terminate = false;
        try {
            while (!terminate && (nrRead = from.read(bytes)) > -1) {
                latestSeenTime = System.currentTimeMillis();                        
                logger.debug("Read " + nrRead + " bytes from audio source.");
                if (endMarker != null) {
                    int start = MaryUtils.indexOf(bytes, endMarker);
                    if (start != -1) { // found the end marker!
                        nrRead = start; // truncate
                        terminate = true;
                        logger.debug("Found end marker at index position " + start);
                    }
                }
                audioDestination.write(bytes, 0, nrRead);
            }
            logger.info("Finished reading.");
            from.close();
        } catch (IOException e) {
            logger.warn("Problem reading from module:", e);
        }
    }
    
    public long latestSeenTime() { return latestSeenTime; }
    
}
