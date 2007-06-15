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


import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;

import de.dfki.lt.mary.unitselection.voiceimport.TimelineIO;


/**
 * The TimelineReader class provides an interface to read regularly or variably spaced
 * datagrams from a Timeline data file in Mary format.
 * 
 * @author sacha
 *
 */
public class TimelineReader extends TimelineIO 
{
    
    /****************/
    /* CONSTRUCTORS */
    /****************/
    
    /**
     * Empty constructor; need to call load() separately.
     * @see #load(String)
     */
    public TimelineReader()
    {
    }
    
    /**
     * Constructor from a file name.
     * 
     * @param fileName The file to read the timeline from
     * @throws IOException if a problem occurs during reading
     */
    public TimelineReader( String fileName ) throws IOException
    {
        load(fileName);
    }
    
    /**
     * Load a timeline from a file.
     * 
     * @param fileName The file to read the timeline from
     * @throws IOException if a problem occurs during reading
     */
    public void load(String fileName) throws IOException
    {
        /* Open the file */
        raf = new RandomAccessFile( fileName, "r" );
        /* Load the header and indexes */
        loadHeaderAndIndex( fileName );
    }
    
    /******************/
    /* DATA ACCESSORS */
    /******************/
    
    /**
     * Skip the upcoming datagram.
     * 
     * @return true if the end of the datagram zone was encountered; false otherwise.
     * 
     * @throws IOException
     */
    protected boolean skipNextDatagram() throws IOException {
        
        long datagramDuration = 0;
        int datagramSize = 0;
        
        /* If the end of the datagram zone is reached, refuse to skip */
        if ( getBytePointer() == timeIdxBytePos ) {
            throw new IndexOutOfBoundsException( "Byte pointer out of bounds: you are trying to skip further" +
                    " than the end of the datagram zone." );
        }
        /* else: */
        try {
            datagramDuration = raf.readLong();
            datagramSize = raf.readInt();
        }
        /* Detect a possible EOF encounter */
        catch ( EOFException e ) {
            throw new IOException( "While skipping a datagram, EOF was met before the time index position: "
                    + "you may be dealing with a corrupted timeline file." );
        }
        
        /* Skip the data field. */
        int nBytes = raf.skipBytes( datagramSize );
        
        /* If EOF has been encountered before the expected datagram size: */
        if ( nBytes < datagramSize ) {
            throw new IOException( "Failed to skip an expected datagram: "
                    + "you may be dealing with a corrupted timeline file." );
        }
        
        /* If the read was successful, update the time pointer */
        timePtr += datagramDuration;
        
        /* Return the number of skipped bytes; in the VARIABLE case, this includes skipping the size&duration fields. */
        return( false );
    }
    
    /**
     * Read and return the upcoming datagram.
     * 
     * @return the current datagram, or null if EOF was encountered; internally updates the time pointer.
     * 
     * @throws IOException
     */
    protected Datagram getNextDatagram() throws IOException {
        
        Datagram d = null;
        
        long position = getBytePointer();
        /* If the end of the datagram zone is reached, refuse to read */
        if ( getBytePointer() == timeIdxBytePos ) {
            //throw new IndexOutOfBoundsException( "Time out of bounds: you are trying to read a datagram at" +
            //        " a time which is bigger than the total timeline duration." );
            return null;
        }
        /* Else, pop the datagram out of the file */
        try {
            d = new Datagram( raf );
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
    
    
    /**
     * Set the file pointer to the beginning of the datagram zone, and the time pointer to 0.
     *
     */
    protected void rewind() throws IOException {
        setBytePointer( datagramsBytePos );
        setTimePointer( 0 );
    }
    
    
    /**
     * Return an array of datagrams.
     * 
     * @param nDatagrams the number of datagrams to read.
     * 
     * @return an array of datagrams; internally updates the time pointer.
     * 
     * @throws IOException
     */
    protected Datagram[] getNextDatagrams( int nDatagrams ) throws IOException {
        Datagram[] buff = new Datagram[nDatagrams];
        for ( int i = 0; i < nDatagrams; i++ ) {
            buff[i] = getNextDatagram();
        }
        return( buff );
    }
    
    
    /**
     * Hop the datagrams until the one which begins or contains the desired time
     * (time is in samples; the sample rate is assumed to be that of the timeline).
     * 
     * @param targetTimeInSamples the time location to reach.
     * 
     * @return true if the requested time comes before the current time location in the timeline
     *         (this is an error case, because one can only hop forward), false otherwise.
     * 
     * @throws IOException
     */
    private boolean hopToTime( long targetTimeInSamples ) throws IOException {
        /* If the requested time is before the current time location, we cannot hop backwards to reach it;
         * send an error case. */
        if ( getTimePointer() > targetTimeInSamples ) {
            return( true );
        }
        
        /* If the current time position is the requested time
         * [implicitely, if (getTimePointer() == targetTimeInSample)],
         * do nothing, you are already at the right position */
        
        /* Else hop: */
        if ( getTimePointer() < targetTimeInSamples ) {
            long byteBefore = 0;
            long timeBefore = 0;
            /* Hop until the datagram which comes just after the requested time */
            while ( getTimePointer() <= targetTimeInSamples ) { // Stop after the requested time, we will step back
                // to the correct time in case of equality
                timeBefore = getTimePointer();
                byteBefore = getBytePointer();
                skipNextDatagram();
                // System.out.println( "Et hop: ( " + byteBefore + " , " + timeBefore + " ) TO ( " + getBytePointer() + " , " + getTimePointer() + " )" );
            }
            /* Do one step back so that the pointed datagram contains the requested time */
            setBytePointer( byteBefore );
            setTimePointer( timeBefore );
            // System.out.println( "Hopping finish: ( " + getBytePointer() + " , " + getTimePointer() + " )" );
        }
        /* Return "false", which means "no error" */
        return( false );
    }
    
    
    /**
     * Hop the datagrams until the desired time.
     * 
     * @param targetTimeInSamples the desired target time, in samples relative to the above sample rate.
     * @param reqSampleRate the sample rate for the time specification.
     * 
     * @throws IOException
     */
    private boolean hopToTime( long targetTimeInSamples, int reqSampleRate ) throws IOException {
        /* Resample the requested time location, and call the regular hopToTime() function */
        return( hopToTime( scaleTime(reqSampleRate,targetTimeInSamples) ) );
    }
    
    
    /**
     * Hop the datagrams until the time closest to the desired time
     * (time is in samples; the sample rate is assumed to be that of the timeline).
     * THIS ROUTINE IS UNTESTED and is mostly unused, kept there in case we change our
     * datagram accessing policy.
     * 
     * @param targetTimeInSamples the time location to reach.
     * 
     * @return true if the requested time comes before the current time location in the timeline
     *         (this is an error case, because one can only hop forward), false otherwise.
     * 
     * @throws IOException
     */
    private boolean hopToClosest( long targetTimeInSamples ) throws IOException {
        /* If the requested time is before the current time location, we cannot hop backwards to reach it;
         * send an error case. */
        if ( targetTimeInSamples < getTimePointer() ) {
            return( true );
        }
        
        /* If the current time position is the requested time [implicitely, if (targetTimeInSample == getTimePointer())],
         * do nothing, you are already at the right position. */
        
        /* Else hop: */
        if ( targetTimeInSamples > getTimePointer() ) {
            long byteBefore = 0;
            long timeBefore = getTimePointer();
            /* - first, browse the file and detect the crossing of the requested time: */
            while ( getTimePointer() < targetTimeInSamples ) { // Stop on or after the requested time
                timeBefore = getTimePointer();
                byteBefore = getBytePointer();
                skipNextDatagram();
            }
            /* - then, refine the decision by choosing the closest datagram: */
            if ( (targetTimeInSamples - timeBefore) < (getTimePointer() - targetTimeInSamples) ) {
                /* If the time difference with the datagram before is smaller than the time difference
                 * with the datagram after, then step one datagram back. */
                setBytePointer( getBytePointer() - byteBefore );
                setTimePointer( timeBefore );
            }
        }
        /* Return "false", which means "no error" */
        return( false );
    }
    
    
    /**
     * Go to the datagram which contains the requested time location
     * (time is in samples; the sample rate is assumed to be that of the timeline).
     * 
     * @param targetTimeInSamples the requested time location, in samples relative to the timeline's sample rate.
     * 
     * @throws IOException
     */
    private void gotoTime( long targetTimeInSamples ) throws IOException {
        /* If we are already there, do nothing: */
        if ( getTimePointer() == targetTimeInSamples ) return;
        /* Else: */
        /* Seek for the time index which comes just before the requested time */
        IdxField idxFieldBefore = idx.getIdxFieldBefore( targetTimeInSamples );
        // System.out.println( "IDXFIELDBEF = ( " + idxFieldBefore.bytePtr + " , " + idxFieldBefore.timePtr + " )" );
        /* Then jump to the indexed datagram */
        setTimePointer( idxFieldBefore.timePtr );
        setBytePointer( idxFieldBefore.bytePtr );
        /* Then hop until the closest datagram: */
        if ( hopToTime( targetTimeInSamples ) ) {
            throw new RuntimeException( "Trying to hop to a time location [" + targetTimeInSamples
                    + "] located before the current time position [" + getTimePointer() + "]."
                    + " Can't hop backwards. (This should never happen!)" );
        }
        // System.out.println( "Position after hopping: ( " + getBytePointer() + " , " + getTimePointer() + " )" );
        
    }
    
    /**
     * Go to the datagram which contains the requested time location.
     * 
     * @param targetTimeInSamples the requested time location, in samples relative to reqSampleRate.
     * @param reqSampleRate the sample rate for the requested time.
     * 
     * @throws IOException
     */
    protected void gotoTime( long targetTimeInSamples, int reqSampleRate ) throws IOException {
        /* Resample the requested time location, in case the sample times are different between
         * the request and the timeline */
        long scaledTargetTime = scaleTime(reqSampleRate,targetTimeInSamples);
        /* Then go to the requested location */
        gotoTime( scaledTargetTime );
    }
    
    /**
     * Get the datagrams spanning a particular time range from a particular time location.
     * 
     * @param targetTimeInSamples the requested position, in samples.
     * @param timeSpanInSamples the requested time span, in samples.
     * @param reqSampleRate the sample rate for the requested times.
     * 
     * @return an array of datagrams
     */
    public synchronized Datagram[] getDatagrams( long targetTimeInSamples, long timeSpanInSamples, int reqSampleRate ) throws IOException {
        return( getDatagrams( targetTimeInSamples, timeSpanInSamples, reqSampleRate, null ) );
    }

    /**
     * Get the datagrams spanning a particular time range from a particular time location,
     * given in the timeline's sampling rate.
     * 
     * @param targetTimeInSamples the requested position, in samples given
     * the timeline's sample rate.
     * @param timeSpanInSamples the requested time span, in samples given
     * the timeline's sample rate.
     * 
     * @return an array of datagrams
     * @see TimelineIO#getSampleRate()
     */
    public synchronized Datagram[] getDatagrams( long targetTimeInSamples, long timeSpanInSamples) throws IOException {
        return( getDatagrams( targetTimeInSamples, timeSpanInSamples, sampleRate, null ) );
    }

    /**
     * Get the datagrams spanning a particular time range from a particular time location,
     * and return the time offset between the time request and the actual location of the first
     * returned datagram.
     * 
     * @param targetTimeInSamples the requested position, in samples.
     * @param timeSpanInSamples the requested time span, in samples.
     * @param reqSampleRate the sample rate for the requested and returned times.
     * @param returnOffset the time difference, in samples, between the time request
     * and the actual beginning of the first datagram.
     * 
     * @return an array of datagrams
     */
    public synchronized Datagram[] getDatagrams( long targetTimeInSamples, long timeSpanInSamples, int reqSampleRate, long[] returnOffset ) throws IOException {
        /* Check the input arguments */
        if ( targetTimeInSamples < 0 ) {
            throw new IllegalArgumentException( "Can't get a datagram from a negative time position (given time position was [" + targetTimeInSamples + "])." );
        }
        if ( timeSpanInSamples < 0 ) {
            /* If the timeSapn is negative, return one datagram nonetheless,
             * the one beginning at targetTimeInSamples */
            timeSpanInSamples = 1;
        }
        /* Resample the requested time location, in case the sample times are different between
         * the request and the timeline */
        long scaledTargetTime = scaleTime( reqSampleRate, targetTimeInSamples );
        long endTime = scaleTime( reqSampleRate, (targetTimeInSamples+timeSpanInSamples) );
//        System.out.println( "Asked: (" + targetTimeInSamples + "," + (targetTimeInSamples+timeSpanInSamples) + ")@" + reqSampleRate
//                + " == (" + scaledTargetTime + "," + endTime + ")@" + sampleRate );
        /* We are going to store the datagrams first in a vector, because we don't know how many datagrams we will
         * end up with, and vectors are easier to grow in size than arrays. */
        Vector v = new Vector( 32, 32 );
        /* Let's go to the requested time... */
        gotoTime( scaledTargetTime );
        if ( returnOffset != null ) returnOffset[0] = unScaleTime( reqSampleRate, (scaledTargetTime - getTimePointer()) );
        /* ... and read datagrams across the requested timeSpan: */
        boolean readMore = true;
        while(readMore && getTimePointer() < endTime) {
            Datagram dat = getNextDatagram();
            if (dat == null) readMore = false;
            else {
                if ( reqSampleRate != sampleRate ) dat.setDuration(unScaleTime( reqSampleRate, dat.getDuration() )); // => Don't forget to stay time-consistent!
                v.add( dat );
            }
        }
        
        /* Cast the vector into an array of datagrams (an array of byte arrays),
         * and return it */
        return( (Datagram[])( v.toArray( new Datagram[0] ) ) );
    }
    
    /**
     * Get the datagrams spanning a particular unit.
     * 
     * @param unit The requested speech unit, containing its own position and duration.
     * @param reqSampleRate the sample rate for the requested times, as specified in the "unit space".
     * 
     * @return an array of datagrams
     */
    public synchronized Datagram[] getDatagrams( Unit unit, int reqSampleRate ) throws IOException {
        return( getDatagrams( unit, reqSampleRate, null ) );
    }

    /**
     * Get the datagrams spanning a particular unit, and return the time offset between the unit request
     * and the actual location of the first returned datagram.
     * 
     * @param unit The requested speech unit, containing its own position and duration.
     * @param reqSampleRate the sample rate for the requested times, as specified in the "unit space".
     * @param returnOffset the time difference, in samples, between the requested unit
     * and the actual beginning of the first returned datagram.
     * 
     * @return an array of datagrams
     */
    public synchronized Datagram[] getDatagrams( Unit unit, int reqSampleRate, long[] returnOffset ) throws IOException {
        return( getDatagrams( unit.getStart(), (long)(unit.getDuration()), reqSampleRate, returnOffset ) );
    }

    /**
     * Get a given number of datagrams from a particular time location.
     * 
     * @param targetTimeInSamples the requested position, in samples.
     * @param number the requested number of datagrams.
     * @param reqSampleRate the sample rate for the requested times.
     * 
     * @return an array of datagrams
     */
    public synchronized Datagram[] getDatagrams( long targetTimeInSamples, int number, int reqSampleRate ) throws IOException {
        /* Resample the requested time location, in case the sample times are different between
         * the request and the timeline */
        long scaledTargetTime = scaleTime( reqSampleRate, targetTimeInSamples );
        /* Let's go to the requested time... */
        gotoTime( scaledTargetTime );
        /* ... and return the requested number of datagrams. */
        Datagram[] buff = getNextDatagrams(number);
        if ( reqSampleRate != sampleRate ) {
            for ( int i = 0; i < buff.length; i++ ) {
                buff[i].setDuration( unScaleTime( reqSampleRate, buff[i].getDuration() )); // => Don't forget to stay time-consistent!
            }
        }
        return( buff );
    }
    
    /**
     * Get a single datagram from a particular time location.
     * 
     * @param targetTimeInSamples the requested position, in samples.
     * @param reqSampleRate the sample rate for the requested times.
     * 
     * @return a datagram.
     */
    public synchronized Datagram getDatagram( long targetTimeInSamples, int reqSampleRate ) throws IOException {
        /* Resample the requested time location, in case the sample times are different between
         * the request and the timeline */
        long scaledTargetTime = scaleTime( reqSampleRate, targetTimeInSamples );
        /* Let's go to the requested time... */
        gotoTime( scaledTargetTime );
        /* ... and return a single datagram. */
        Datagram dat = getNextDatagram();
        if (dat == null) 
            return null;
        if ( reqSampleRate != sampleRate ) dat.setDuration(unScaleTime( reqSampleRate, dat.getDuration() )); // => Don't forget to stay time-consistent!
        return( dat );
    }
    
    /**
     * Get a single datagram from a particular time location,
     * given in the timeline's sampling rate.
     * 
     * @param targetTimeInSamples the requested position, in samples.
     * 
     * @return a datagram.
     */
    public synchronized Datagram getDatagram( long targetTimeInSamples) throws IOException {
        /* Resample the requested time location, in case the sample times are different between
         * the request and the timeline */
        return getDatagram(targetTimeInSamples, sampleRate);
    }

    /**
     * Get a given number of datagrams from a particular time location,
     * and return the time offset between the time request and the actual location of the first
     * returned datagram.
     * 
     * @param targetTimeInSamples the requested position, in samples.
     * @param number the requested number of datagrams.
     * @param reqSampleRate the sample rate for the requested times.
     * @param returnOffset the time difference, in samples, between the time request
     * and the actual beginning of the first datagram.
     * 
     * @return an array of datagrams
     */
    public synchronized Datagram[] getDatagrams( long targetTimeInSamples, int number, int reqSampleRate, long[] returnOffset ) throws IOException {
        /* Resample the requested time location, in case the sample times are different between
         * the request and the timeline */
        long scaledTargetTime = scaleTime( reqSampleRate, targetTimeInSamples );
        /* Let's go to the requested time... */
        gotoTime( scaledTargetTime );
        if ( returnOffset != null ) returnOffset[0] = unScaleTime( reqSampleRate, (scaledTargetTime - getTimePointer()) );
        /* ... and return the requested number of datagrams. */
        Datagram[] buff = getNextDatagrams(number);
        if ( reqSampleRate != sampleRate ) {
            for ( int i = 0; i < buff.length; i++ ) {
                buff[i].setDuration(unScaleTime( reqSampleRate, buff[i].getDuration() )); // => Don't forget to stay time-consistent!
            }
        }
        return( buff );
    }
    
    /**
     * Returns the timeline's total time duration (i.e., the position
     * of the last datagram ever observed by the index).
     */
    public synchronized long getTotalTime() throws IOException {
        long bptr = getBytePointer();
        setBytePointer( idx.getPrevBytePos() );
        long ret = idx.getPrevTimePos();
        Datagram next = getNextDatagram();
        if (next != null) ret += next.duration;
        setBytePointer( bptr );
        return( ret );
    }
}