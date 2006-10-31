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
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

import de.dfki.lt.mary.unitselection.voiceimport.General;

public class MCepDatagram extends Datagram {
    
    protected float[] coeffs;
    
    /**
     * Construct a MCep datagram from a float vector.
     * 
     * @param duration the duration, in samples, of the data represented by this datagram 
     * @param coeffs the array of Mel-Cepstrum coefficients.
     */
    public MCepDatagram(long setDuration, float[] coeffs )
    {
        super(setDuration);
        this.coeffs = coeffs;
    }

    /**
     * Constructor which pops a datagram from a random access file.
     * 
     * @param raf the random access file to pop the datagram from.
     * 
     * @throws IOException
     * @throws EOFException
     */
    public MCepDatagram( RandomAccessFile raf, int order ) throws IOException, EOFException
    {
        super(raf.readLong()); // duration
        int len = raf.readInt();
        if ( len < 0 ) {
            throw new IOException( "Can't create a datagram with a negative data size [" + len + "]." );
        }
        if (len < 4*order) {
            throw new IOException("Mel-Cepstrum datagram too short (len="+len
                    +"): cannot be shorter than the space needed for Mel-Cepstrum coefficients (4*"+order+")");
        }
        // For speed concerns, read into a byte[] first:
        byte[] buf = new byte[len];
        raf.readFully(buf);
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));

        coeffs = new float[order];
        for (int i=0; i<order; i++) {
            coeffs[i] = dis.readFloat();
        }
    }

    /**
     * Get the length, in bytes, of the datagram's data field.
     */
    public int getLength()
    {
        return 4*coeffs.length;
    }
    
    /**
     * Get the order, i.e. the number of MEl-Cepstrum coefficients.
     * @return the order
     * @see #getCoeffs()
     */
    public int order()
    {
        return coeffs.length;
    }
    
    /**
     * Get the array of Mel-Cepstrum coefficients.
     */
    public float[] getCoeffs()
    {
        return coeffs;
    }
    
    /**
     * Get the array of Mel-Cepstrum coefficients.
     */
    public double[] getCoeffsAsDouble()
    {
        double[] ret = new double[coeffs.length];
        for ( int i = 0; i < coeffs.length; i++ ) {
            ret[i] = (double)(coeffs[i]);
        }
        return( ret );
    }
    
    /**
     * Get a particular Mel-Cepstrum coefficient.
     */
    public float getCoeff( int i )
    {
        return coeffs[i];
    }
    
    /**
     * Write this datagram to a random access file or data output stream.
     */
    public void write( DataOutput out ) throws IOException {
        out.writeLong( duration );
        out.writeInt( getLength() );
        for (int i=0; i<coeffs.length; i++) {
            out.writeFloat(coeffs[i]);
        }
    }

    /**
     * Tests if this datagram is equal to another datagram.
     */
    public boolean equals( Datagram other ) {
        if (! (other instanceof MCepDatagram)) return false;
        MCepDatagram otherMCep = (MCepDatagram) other;
        if ( this.duration != otherMCep.duration ) return false;
        if ( this.coeffs.length != otherMCep.coeffs.length ) return false;
        for ( int i = 0; i < this.coeffs.length; i++ ) {
            if ( this.coeffs[i] != otherMCep.coeffs[i] ) return false;
        }
        return true;
    }

    
}
