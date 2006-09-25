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
package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Loads a unit file in memory and provides accessors to the start times and durations.
 * 
 * @author sacha
 *
 */
public class UnitFileReader {

    private MaryHeader hdr = null;
    private int numberOfUnits = 0;
    private int sampleRate = 0;
    private long[] startTime = null;
    private int[] duration = null;
    
    /****************/
    /* CONSTRUCTORS */
    /****************/
    
    public UnitFileReader( String fileName ) {
        
        /* Open the file */
        DataInputStream dis = null;
        try {
            dis = new DataInputStream( new BufferedInputStream( new FileInputStream( fileName ) ) );
        }
        catch ( FileNotFoundException e ) {
            throw new RuntimeException( "File [" + fileName + "] was not found." );
        }
        try {
            /* Load the Mary header */
            hdr = new MaryHeader( dis );
            if ( !hdr.isMaryHeader() ) {
                throw new RuntimeException( "File [" + fileName + "] is not a valid Mary format file." );
            }
            if ( hdr.getType() != MaryHeader.UNITS ) {
                throw new RuntimeException( "File [" + fileName + "] is not a valid Mary Units file." );
            }
            /* Read the number of units */
            numberOfUnits = dis.readInt();
            if ( numberOfUnits < 0 ) {
                throw new RuntimeException( "File [" + fileName + "] has a negative number of units. Aborting." );
            }
            /* Read the sample rate */
            sampleRate = dis.readInt();
            if ( sampleRate < 0 ) {
                throw new RuntimeException( "File [" + fileName + "] has a negative number sample rate. Aborting." );
            }
            /* Read the start times and durations */
            startTime = new long[numberOfUnits];
            duration = new int[numberOfUnits];
            for ( int i = 0; i < numberOfUnits; i++ ) {
                startTime[i] = dis.readLong();
                duration[i] = dis.readInt();
            }
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Reading the Mary header from file [" + fileName + "] failed.", e );
        }
        
    }
    
    /*****************/
    /* OTHER METHODS */
    /*****************/
    
    /**
     * Get the number of units in the file.
     * @return The number of units.
     */
    public int getNumberOfUnits() {
        return( numberOfUnits );
    }
    
    /**
     * Get the sample rate of the file.
     * @return The sample rate, in Hz.
     */
    public int getSampleRate() {
        return( sampleRate );
    }
    
    /**
     * Return the duration of unit number i.
     * 
     * @param i The index of the considered unit.
     * @return The start time of the considered unit, in samples with respect
     * to the file's sample rate.
      */
    public long getStartTime( int i ) {
        return( startTime[i] );
    }
    
    /**
     * Return the duration of unit number i.
     * 
     * @param i The index of the considered unit.
     * @return The duration of the considered unit, in samples with respect
     * to the file's sample rate.
     */
    public int getDuration( int i ) {
        return( duration[i] );
    }
    
    /**
     * Determine whether the unit number i is an "edge" unit, i.e.
     * a unit marking the start or the end of an utterance.
     * 
     * @param i The index of the considered unit.
     * @return true if the unit is an edge unit in the unit file, false otherwise
     */
    public boolean isEdgeUnit(int i) {
        if (duration[i] == 0) return true;
        else return false;
    }
    
}
