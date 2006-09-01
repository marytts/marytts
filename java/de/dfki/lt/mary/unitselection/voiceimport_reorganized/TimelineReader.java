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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.util.Vector;

/**
 * The TimelineReader class provides an interface to read regularly or variably spaced
 * datagrams from a Timeline data file in Mary format.
 * 
 * @author sacha
 *
 */
public class TimelineReader extends TimelineIO {
    
    /****************/
    /* CONSTRUCTORS */
    /****************/
    
    /**
     * Constructor from an externally open random access file
     * 
     * @param raf The random access file to read the timeline from.
     */
    public TimelineReader( RandomAccessFile raf ) throws IOException {
        loadHeaderAndIndex();
    }
    
    /**
     * Constructor from a file name
     * 
     * @param fileName The file to read the timeline from
     */
    public TimelineReader( String fileName ) {
        try {
            /* Open the file */
            raf = new RandomAccessFile( fileName, "r" );
            /* Load the header and indexes */
            loadHeaderAndIndex();
        }
        catch ( FileNotFoundException e ) {
            throw new Error("Timeline file [" + fileName + "] was not found." );
        }
        catch ( SecurityException e ) {
            throw new Error("You do not have read access to the file [" + fileName + "]." );
        }
        catch ( IOException e ) {
            throw new Error("IO Exception caught when closing file [" + fileName + "]: " + e.getMessage() );
        }
    }
    
    /*****************/
    /* I/O METHODS   */
    /*****************/
    
    /*******************/
    /* MISC. METHODS   */
    /*******************/
    private long scaleTime( int reqSampleRate, long targetTimeInSamples ) {
        return( (long)Math.round( (double)(sampleRate) / (double)(reqSampleRate) * (double)(targetTimeInSamples) ) );
    }
    
    /******************/
    /* DATA ACCESSORS */
    /******************/
    
    /**
     * Skip the upcoming datagram.
     * 
     * @return the number of skipped bytes; internally updates the time pointer
     * 
     * @throws IOException
     */
    public long skipNextDatagram() throws IOException {
        
        if ( timeSpacingMode == VARIABLE ) {
            datagramSize = raf.readInt();
            datagramDuration = raf.readLong();
        }
        /* Else if timeSpacingMode == REGULAR, datagram size and duration are preset in the header. */
        timePtr += datagramDuration;
        raf.skipBytes( datagramSize );
        
        /* Return the number of skipped bytes; in the VARIABLE case, this includes skipping the size&duration fields. */
        return( (long)( datagramSize + ( timeSpacingMode == VARIABLE ? 12l : 0l ) ) );
    }
    
    /**
     * Read and return the upcoming datagram
     * 
     * @return the current datagram as a byte array; internally updates the time pointer
     * 
     * @throws IOException
     */
    public byte[] getNextDatagram() throws IOException {
        if ( timeSpacingMode == VARIABLE ) {
            datagramSize = raf.readInt();
            datagramDuration = raf.readLong();
        }
        /* Else if timeSpacingMode == REGULAR, datagram size and duration are preset in the header. */
        byte[] buff = new byte[datagramSize];
        timePtr += datagramDuration;
        raf.readFully( buff );
        return( buff );
    }
    
    /**
     * Return an array of datagrams
     * 
     * @param numDatagrams the number of datagrams to read
     * 
     * @return an array of byte arrays; internally updates the time pointer
     * 
     * @throws IOException
     */
    public byte[][] getNextDatagrams( int numDatagrams ) throws IOException {
        byte[][] buff = new byte[numDatagrams][];
        for ( int i = 0; i < numDatagrams; i++ ) {
            buff[i] = getNextDatagram();
        }
        return( buff );
    }
    
    
    /**
     * Hop the datagrams until the time closest to the desired time
     * (time is in samples; the sample rate is assumed to be that of the timeline).
     * 
     * @param targetTimeInSamples the time location to reach
     * 
     * @return true if the requested time comes before the current time location in the timeline
     *         (this is an error case, because one can only hop forward), false otherwise
     * 
     * @throws IOException
     */
    private boolean hopToClosestDatagram( long targetTimeInSamples ) throws IOException {
        /* If the requested time is before the current time location, we cannot hop backwards to reach it;
         * send an error case. */
        if ( targetTimeInSamples < getTimePointer() ) {
            return( true );
        }
        
        /* If the current time position is the requested time [implicitely, if (targetTimeInSample == getTimePointer())],
         * do nothing, you are already at the right position. */
        
        /* Else hop: */
        if ( targetTimeInSamples > getTimePointer() ) {
            long byteRewind = 0;
            long timeBefore = getTimePointer();
            /* - first, browse the file and detect the crossing of the requested time: */
            while ( getTimePointer() < targetTimeInSamples ) { // Stop on or after the requested time
                timeBefore = getTimePointer();
                byteRewind = skipNextDatagram();
            }
            /* - then, refine the decision by choosing the closest datagram: */
            if ( (targetTimeInSamples - timeBefore) < (getTimePointer() - targetTimeInSamples) ) {
                /* If the time difference with the datagram before is smaller than the time difference
                 * with the datagram after, then step one datagram back. */
                setBytePointer( getBytePointer() - byteRewind );
                setTimePointer( timeBefore );
            }
        }
        /* Return "false", which means "no error" */
        return( false );
    }
    
    /**
     * Hop the datagrams until the one which begins or contains the desired time
     * (time is in samples; the sample rate is assumed to be that of the timeline).
     * 
     * @param targetTimeInSamples the time location to reach
     * 
     * @return true if the requested time comes before the current time location in the timeline
     *         (this is an error case, because one can only hop forward), false otherwise
     * 
     * @throws IOException
     */
    private boolean hopToDatagram( long targetTimeInSamples ) throws IOException {
        /* If the requested time is before the current time location, we cannot hop backwards to reach it;
         * send an error case. */
        if ( targetTimeInSamples < getTimePointer() ) {
            return( true );
        }
        
        /* If the current time position is the requested time [implicitely, if (targetTimeInSample == getTimePointer())],
         * do nothing, you are already at the right position */
        
        /* Else hop: */
        if ( targetTimeInSamples > getTimePointer() ) {
            long byteRewind = 0;
            long timeBefore = 0;
            /* Hop until the datagram which comes just after the requested time */
            while ( getTimePointer() <= targetTimeInSamples ) { // Stop after the requested time, we will step back
                // to the correct time in case of equality
                timeBefore = getTimePointer();
                byteRewind = skipNextDatagram();
            }
            /* Do one step back so that the pointed datagram contains the requested time */
            setBytePointer( getBytePointer() - byteRewind );
            setTimePointer( timeBefore );
        }
        /* Return "false", which means "no error" */
        return( false );
    }
    
    /**
     * Hop the datagrams until the desired time. Appllies to VARIABLE timelines only.
     * 
     * @param reqSampleRate the sample rate for the time specification
     * @param targetTimeInSamples the desired target time, in samples relative to the above sample rate
     * 
     * @throws IOException
     */
    private boolean hopToTime( int reqSampleRate, long targetTimeInSamples ) throws IOException {
        /* Resample the requested time location, in case the sample times are different between
         * the request and the timeline */
        long resampledTime = scaleTime(reqSampleRate,targetTimeInSamples);
        /* Then call the regular hopToTime() function */
        return( hopToDatagram( resampledTime ) );
    }
    
    /**
     * Go to the datagram which contains the requested time location,
     * across the whole timeline (as opposed to within a local time in a file)
     * 
     * @param reqSampleRate the sample rate for the requested time
     * @param targetTimeInSamples the requested time location, in samples relative to reqSampleRate
     * 
     * @throws IOException
     */
    public void gotoDatagram( int reqSampleRate, long targetTimeInSamples ) throws IOException {
        
        /* Resample the requested time location, in case the sample times are different between
         * the request and the timeline */
        long scaledTargetTime = scaleTime(reqSampleRate,targetTimeInSamples);
        
        /* REGULAR mode: just jump to the requested datagram */
        if ( timeSpacingMode == REGULAR ) {
            /* Resample the requested time location, in case the sample times are different between
             * the request and the timeline */
            setTimePointer( scaledTargetTime );
            /* Get the corresponding datagram index */
            long datagramIdx = (long)Math.round( (double)(getTimePointer()) / (double)(datagramDuration) );
            /* Seek the relevant datagram through simple arithmetics */
            setBytePointer( datagramsBytePos + datagramIdx*datagramSize  );
        }
        
        /* VARIABLE mode: use the index and the hop to the requested datagram */
        else if ( timeSpacingMode == VARIABLE ) {
            /* Seek for the time index which comes just before the requested time */
            IdxField idxFieldBefore = idx.getIdxFieldBefore( scaledTargetTime );
            /* Then jump to the indexed datagram */
            setTimePointer( idxFieldBefore.timePtr );
            setBytePointer( idxFieldBefore.bytePtr );
            /* Then hop until the closest datagram: */
            if ( hopToDatagram( scaledTargetTime ) ) {
                throw new RuntimeException( "Trying to hop to a time location before the current time position."
                        + " Can't hop backwards. (This should never happen!)" );
            }
        }
        
        /* ELSE: check for a bad time spacing mode */
        else {
            throw new RuntimeException( "Unknown time spacing mode!" );
        }
    }
    
    /**
     * Get the datagrams spanning a particular time range from a particular time location
     * 
     * @param reqSampleRate the sample rate for the requested times
     * @param targetTimeInSamples the requested position, in samples
     * @param timeSpanInSamples the requested time span, in samples
     * 
     * @return the datagrams, as an array of byte arrays
     */
    public byte[][] getDatagrams( int reqSampleRate, long targetTimeInSamples, long timeSpanInSamples ) throws IOException {
        /* We are going to store the datagrams first in a vector, because we don't know how many datagrams we will
         * end up with, and vectors are easier to grow in size than arrays. */
        Vector v = new Vector( 32, 32 );
        /* Let's go to the requested time... */
        gotoDatagram( reqSampleRate, targetTimeInSamples );
        /* ... and read datagrams across the requested timeSpan: */
        long endTimeInSamples = targetTimeInSamples + timeSpanInSamples;
        endTimeInSamples = scaleTime(reqSampleRate,endTimeInSamples);
        while( getTimePointer() <= endTimeInSamples ) {
            v.add( getNextDatagram() );
        }
        
        /* Cast the vector into an array of datagrams (an array of byte arrays),
         * and return it */
        return( (byte[][])( v.toArray() ) );
    }
    
    /**
     * TODO
     * @param reqSampleRate
     * @param localTime
     * @param basename
     * @return
     * @throws IOException
     */
//    public long gotoLocalTime( int reqSampleRate, long localTime, String basename ) throws IOException {
//        return( raf.getFilePointer() );
//    }
    
    /**
     * TODO
     * @param baseName
     * @param reqSampleRate
     * @param localTime
     * @param timeSpan
     * @return
     */
//    public byte[] getDatagramFromFile( String baseName, int reqSampleRate, long localTime, long timeSpan ) {
//        byte[] buff = null;
//        return( buff );
//    }
    
    /**
     * TODO
     * @param retAbsTime
     * @param retAbsDatagramIdx
     * @param retBasename
     * @param retLocalTimeInBaseName
     * @param atSampleRate
     */
//    public void whereAmI( long retAbsTime, long retAbsDatagramIdx,
//            String retBasename, long retLocalTimeInBaseName, int atSampleRate ) {
//        
//    }
    
}
