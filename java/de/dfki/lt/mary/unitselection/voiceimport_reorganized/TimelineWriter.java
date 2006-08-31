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
import java.io.File;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;

public class TimelineWriter extends TimelineIO {

    /****************/
    /* DATA FIELDS  */
    /****************/
    
    /****************/
    /* CONSTRUCTORS */
    /****************/
    
    /**
     * Constructor from a file name
     * 
     * @param fileName The file to read the timeline from
     * @param mode "c" for create/overwrite, "a" for append
     * @param procHdrString the string to use as a processing header
     */
    public TimelineWriter( String fileName , String mode, String procHdrString, int reqSampleRate, byte reqTimeSpacingMode ) {
        
        /* Check the arguments */
        if ( (!mode.equals("c")) && (!mode.equals("a")) ) {
            throw new RuntimeException( "[" + mode + "] is an unknown timeline creation mode." );
        }
        if ( reqSampleRate <= 0 ) {
            throw new RuntimeException( "The sample rate [" + reqSampleRate + "] can't be negative or null when creating a timeline." );
        }
        if ( (reqTimeSpacingMode != REGULAR) && (reqTimeSpacingMode != VARIABLE) ) {
            throw new RuntimeException( "The timeline's spacing mode must be one of VARIABLE or REGULAR." );
        }
        
        /* Open and create or read the headers */
        try {
            File fid = new File( fileName );
            /* Check if the file exists and should be deleted first */
            if ( (mode.equals("c")) && fid.exists() ) {
                fid.delete();
            }
            /* Open the file */
            raf = new RandomAccessFile( fid, "rw" );
            /* If the file is open for appending: */
            if ( mode.equals("a") ) {
                /* Load the existing header and indexes */
                loadHeaderAndIndex();
                /* Truncate the file after to the end of the datagram zone, just before the time index */
                raf.setLength( timeIdxBytePos );
                /* Position the file pointer to the end of the datagram zone */
                raf.seek( timeIdxBytePos );
                /* Reset the index positions, to indicate that the headers on disk have been truncated out */
                timeIdxBytePos = 0;
//                baseNameIdxBytePos = 0;
            }
            else {
                makeHeaderAndIndex( procHdrString, reqSampleRate, reqTimeSpacingMode );
            }
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
    
    /*******************/
    /* MISC. METHODS   */
    /*******************/
    
    /**
     * Make new headers for an empty file, and locate the file pointer at the beginning of the datagram zone.
     */
    private void makeHeaderAndIndex( String procHdrStr, int reqSampleRate, byte reqTimeSpacingMode ) throws IOException {
        
        /* Make a new Mary header and write it */
        maryHdr = new MaryHeader( MaryHeader.TIMELINE );
        maryHdr.write( raf );
        
        /* Make a new processing header and write it */
        procHdr = new ProcHeader( procHdrStr );
        procHdr.dump( raf );
        
        /* Make/write the data header */
        sampleRate = reqSampleRate;
        raf.writeInt( sampleRate );
        
        numDatagrams = 0;
        raf.writeLong( numDatagrams );
        
        timeSpacingMode = reqTimeSpacingMode;
        raf.writeByte( timeSpacingMode );
        
        /* Write the positions, with fake ones for the idx and basenames */
        //datagramsBytePos = getBytePointer() + 24; // +24: account for the 3 long fields
        datagramsBytePos = getBytePointer() + 16; // +16: account for the 2 long fields
        System.out.println( "DEBUG: datagrams byte position = " + datagramsBytePos );
        raf.writeLong( datagramsBytePos );
        timeIdxBytePos = 0;     raf.writeLong( 0l );
//        baseNameIdxBytePos = 0; raf.writeLong( 0l );
        
        /* Make a new time index */
        idx = new Index( sampleRate );
        
        /* Now we can output the datagrams. */
        
    }

    /**
     * Output the internally maintained indexes and close the file.
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        /* Correct the number of datagrams */
        setBytePointer( datagramsBytePos - 25l );
        raf.writeLong( numDatagrams );
        /* Go to the end of the file and output the time index */
        setBytePointer( raf.length() );
        timeIdxBytePos = getBytePointer();
        idx.dump( raf );
        /* Output the basename index */
//        baseNameIdxBytePos = getBytePointer();
//        baseName.dump( raf );
//        Long[] buff = (Long[])( baseNameBytePos.toArray() );
//        for ( int i = 0; i < buff.length; i++ ) {
//            raf.writeLong( buff[i].longValue() );
//        }
//        buff = (Long[])( baseNameTimePos.toArray() );
//        for ( int i = 0; i < buff.length; i++ ) {
//            raf.writeLong( buff[i].longValue() );
//        }
        /* Register the index positions */
        setBytePointer( datagramsBytePos - 16l );
        raf.writeLong( timeIdxBytePos );
//        raf.writeLong( baseNameIdxBytePos );
        /* Finally, close the random access file */
        raf.close();
    }
    
    /**
     * Set the datagram size and duration in memory and on disk.
     * Applies to the REGULAR case only.
     * 
     * @param setDatagramSize
     * @param setDatagramDuration
     * 
     * @throws IOException
     */
    public void setRegularParameters( int setDatagramSize, long setDatagramDuration ) throws IOException {
        if ( timeSpacingMode == VARIABLE ) {
            throw new RuntimeException( "Trying to set a fixed datagram size and duration on a VARIABLE timeline." );
        }
        /* Register the set values in memory */
        datagramSize = setDatagramSize;
        datagramDuration = setDatagramDuration;
        /* Go to the beginning of the datagrams zone */
        setBytePointer( datagramsBytePos );
        /* Register the values on disk */
        raf.writeInt(datagramSize);
        raf.writeLong(datagramDuration);
    }
    
    
    /**
     * Write one regularly spaced datagram to the timeline.
     * Warning: assumes that datagramDuration has been previously correctly set.
     * 
     * @param newDatagram a datagram (i.e., a byte array)
     * 
     * @throws IOException
     */
    public void feed( byte[] newDatagram ) throws IOException {
        if ( timeSpacingMode == VARIABLE ) {
            throw new RuntimeException( "Trying to feed a fixed-spacing datagram to a VARIABLE timeline." );
        }
        raf.write( newDatagram );   
        setTimePointer( getTimePointer() + datagramDuration );
        numDatagrams += 1;
    }
    
    /**
     * Write a series of regularly spaced datagrams to the timeline.
     * Warning: assumes that datagramDuration has been previously correctly set.
     * 
     * @param newDatagrams an array of datagrams (i.e., an array of byte arrays)
     * 
     * @throws IOException
     */
    public void feed( byte[][] newDatagrams ) throws IOException {
        if ( timeSpacingMode == VARIABLE ) {
            throw new RuntimeException( "Trying to feed a set of fixed-spacing datagrams to a VARIABLE timeline." );
        }
        for ( int i = 0; i < newDatagrams.length; i++ ) {
            feed( newDatagrams[i] );   
        }
    }
    
    /**
     * Write one variably spaced datagram to the timeline
     * 
     * @param newDatagram a datagrams (i.e., a byte array)
     * @param newDatagramDuration a long indicating the time span (in samples) of the datagram
     * 
     * @throws IOException
     */
    public void feed( byte[] newDatagram, long newDatagramDuration ) throws IOException {
        if ( timeSpacingMode == REGULAR ) {
            throw new RuntimeException( "Trying to feed a variable-spaced datagram to a REGULAR timeline." );
        }
        /* Filter the datagram through the index (to automatically add an index field if needed) */
        idx.feed( getBytePointer(), getTimePointer() );
        /* Then write it on disk */
        raf.writeInt( newDatagram.length );
        raf.writeLong( newDatagramDuration );
        raf.write( newDatagram );
        /* Then advance various pointers */
        setTimePointer( getTimePointer() + newDatagramDuration );
        numDatagrams += 1;
    }

    /**
     * Write a series of variably spaced datagrams to the timeline
     * 
     * @param newDatagrams an array of datagrams (i.e., an array of byte arrays)
     * @param newDatagramDurations an array of long indicating the time positions of the datagrams
     * 
     * @throws IOException
     */
    public void feed( byte[][] newDatagrams, long[] newDatagramDurations ) throws IOException {
        if ( timeSpacingMode == REGULAR ) {
            throw new RuntimeException( "Trying to feed a set of variable-spacing datagrams to a REGULAR timeline." );
        }
        for ( int i = 0; i < newDatagrams.length; i++ ) {
            feed( newDatagrams[i], newDatagramDurations[i] );
        }
    }

}
