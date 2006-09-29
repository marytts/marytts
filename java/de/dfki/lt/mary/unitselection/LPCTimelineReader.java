/**
 * Copyright 2006 DFKI GmbH.
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
package de.dfki.lt.mary.unitselection;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Properties;


public class LPCTimelineReader extends TimelineReader
{
    protected int lpcOrder;
    protected float lpcMin;
    protected float lpcRange;

    public LPCTimelineReader()
    {
    }

    public LPCTimelineReader(String fileName) throws IOException
    {
        super(fileName);
    }

    public void load(String fileName) throws IOException
    {
        super.load(fileName);
        // Now make sense of the processing header
        Properties props = new Properties();
        ByteArrayInputStream bais = new ByteArrayInputStream(procHdr.getString().getBytes("latin1"));
        props.load(bais);
        ensurePresent(props, "lpc.order");
        lpcOrder = Integer.parseInt(props.getProperty("lpc.order"));
        ensurePresent(props, "lpc.min");
        lpcMin = Float.parseFloat(props.getProperty("lpc.min")); 
        ensurePresent(props, "lpc.range");
        lpcRange = Float.parseFloat(props.getProperty("lpc.range")); 
    }
    
    
    private void ensurePresent(Properties props, String key) throws IOException
    {
        if (!props.containsKey(key))
            throw new IOException("Processing header does not contain required field '"+key+"'");

    }

    public int getLPCOrder() { return lpcOrder; }
    
    public float getLPCMin() { return lpcMin; }
    
    public float getLPCRange() { return lpcRange; }
    
    
    /**
     * Read and return the upcoming datagram.
     * 
     * @return the current datagram, or null if EOF was encountered; internally updates the time pointer.
     * 
     * @throws IOException
     */
    protected Datagram getNextDatagram() throws IOException {
        
        Datagram d = null;
        
        /* If the end of the datagram zone is reached, gracefully refuse to read */
        if ( getBytePointer() == timeIdxBytePos ) return( null );
        /* Else, pop the datagram out of the file */
        try {
            d = new LPCDatagram( raf, lpcOrder );
        }
        /* Detect a possible EOF encounter */
        catch ( EOFException e ) {
            throw new IOException( "While reading a datagram, EOF was met before the time index position: "
                    + "you may be dealing with a corrupted timeline file." );
        }
        
        /* If the read was successful, update the time pointer */
        timePtr += d.getDuration();
        
        return( d );
    }




}
