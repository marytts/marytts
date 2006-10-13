/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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

import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Datagram  {

    /****************/
    /* DATA FIELDS  */
    /****************/
    
    /**
     * The datagram duration, in samples.
     */
    protected long duration = 0l; // The (time) duration of the datagram, in samples
    /**
     * The datagram's contents, as a generic byte array.
     */
    protected byte[] data = null;
    
    /****************/
    /* CONSTRUCTORS */
    /****************/
    /**
     * Constructor for subclasses which want to represent data in a different format.
     */
    public Datagram(long duration)
    {
        if ( duration < 0 ) {
            throw new IllegalArgumentException( "Can't create a datagram with the negative duration [" + duration + "]." );
        }
        this.duration = duration;
    }
    /**
     * Constructor from external data.
     * 
     * @param setDuration the datagram duration, in samples.
     * @param setBuff the address of a byte buffer to use as the datagram's data field.
     * WARNING: the contents of the given byte array is NOT deep-copied.
     */
    public Datagram( long setDuration, byte[] setData ) {
        if ( setDuration < 0 ) {
            throw new IllegalArgumentException( "Can't create a datagram with the negative duration [" + setDuration + "]." );
        }
        duration = setDuration;
        data = setData;
    }
    
    /**
     * Constructor which pops a datagram from a random access file.
     * 
     * @param raf the random access file to pop the datagram from.
     * 
     * @throws IOException
     * @throws EOFException
     */
    public Datagram( RandomAccessFile raf ) throws IOException, EOFException {
        duration = raf.readLong();
        if ( duration < 0 ) {
            throw new IOException( "Can't create a datagram with a negative duration [" + duration + "]." );
        }
        int len = raf.readInt();
        if ( len < 0 ) {
            throw new IOException( "Can't create a datagram with a negative data size [" + len + "]." );
        }
        data = new byte[len];
        raf.readFully( data );
    }
    
    
    /****************/
    /* SETTERS      */
    /****************/
    
    /**
     * Set the new duration.
     */
    public void setDuration(long duration)
    {
        this.duration = duration;
    }
    
    
    /****************/
    /* I/O METHODS  */
    /****************/
    
    /**
     * Write this datagram to a random access file or data output stream.
     */
    public void write( DataOutput raf ) throws IOException {
        raf.writeLong( duration );
        raf.writeInt( data.length );
        raf.write( data );
    }
    
    /****************/
    /* ACCESSORS    */
    /****************/
    
    /**
     * Get the datagram duration, in samples. Note: the sample rate has to be provided  externally.
     */
    public long getDuration() {
        return( duration );
    }
    
    /**
     * Get the length, in bytes, of the datagram's data field.
     */
    public int getLength() {
        return( data.length );
    }
    
    /**
     * Get the address of the datagram's data field. Warning: the returned byte array is NOT newly allocated,
     * and keeps on pointing at the datagram's data field.
     */
    public byte[] getData() {
        return( data );
    }
    
    /****************/
    /* MISC METHODS */
    /****************/
    
    /**
     * Tests if this datagram is equal to another datagram.
     */
    public boolean equals( Datagram other ) {
        if ( this.duration != other.duration ) return( false );
        if ( this.data.length != other.data.length ) return( false );
        for ( int i = 0; i < this.data.length; i++ ) {
            if ( this.data[i] != other.data[i] ) return( false );
        }
        return( true );
    }
  
}
