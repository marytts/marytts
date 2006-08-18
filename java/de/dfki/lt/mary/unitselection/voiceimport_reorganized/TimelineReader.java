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

import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.FileNotFoundException;

import de.dfki.lt.mary.unitselection.voiceimport_reorganized.MaryHeader;

public class TimelineReader
{
    /* Timeline modes */
    public final static byte REGULAR  = 0;
    public final static byte VARIABLE = 1;
    
    /* Default interval between index values
     * (in SECONDS, it will be converted to samples later on) */
    public final static float DEFAULTIDXINTERVAL = 30.0f; // seconds
    
    /* Private fields */
    private RandomAccessFile raf = null; // The file to read from
    private MaryHeader maryHdr = null;   // The standard Mary header
    private ProcHeader procHdr = null;
    
    private int sampleRate = 0;
    private long numDatagrams = 0;
    private byte timeSpacingMode = -1;
    
    private int datagramSize = 0;
    private long duration = 0;
    private Index idx = null;
    
    private long byteNow = 0;
    
    /****************/
    /* CONSTRUCTORS */
    /****************/
    
    /**
     * Constructor from a random access file
     * 
     * @param raf The random access file to read the timeline from.
     */
    public TimelineReader( RandomAccessFile raf ) throws IOException {
        loadHeaderAndIndex( raf );
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
            /* Parse the header and load the data */
            loadHeaderAndIndex( raf );
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
    /* OTHER METHODS */
    /*****************/
    
    /**
     * Load the headers and the info down to the datagrams.
     * 
     * @param raf The random access file to read from
     * 
     * @throws IOException
     */
    private long loadHeaderAndIndex( RandomAccessFile raf ) throws IOException {
        maryHdr = new MaryHeader( raf ); byteNow += maryHdr.byteSize;
        procHdr = new ProcHeader( raf ); byteNow += procHdr.byteSize;
        
        sampleRate = raf.readInt();       byteNow += 4;
        numDatagrams = raf.readLong();    byteNow += 8;
        timeSpacingMode = raf.readByte(); byteNow += 1;
        
        if ( timeSpacingMode == REGULAR ) {
            datagramSize = raf.readInt(); byteNow += 4;
            duration = raf.readLong();    byteNow += 8;
        }
        else if ( timeSpacingMode == VARIABLE ) {
            idx = new Index( raf ); byteNow += idx.byteSize;
        }
        return( byteNow );
    }
    
    /***/
    
}


/**
 * 
 * Simple helper class to load the processing header.
 * 
 * @author sacha
 *
 */
class ProcHeader {
    short byteSize = 0;
    byte[] procHeader = null;
    
    public ProcHeader( RandomAccessFile raf )  throws IOException {
        read( raf );
    }
    
    public long read( RandomAccessFile raf ) throws IOException {
        byteSize = raf.readShort();
        procHeader = new byte[byteSize];
        int justRead = raf.read( procHeader );
        if ( justRead != byteSize ) { throw new IOException( "Could not read a whole procHeader!" ); }
        return( byteSize + 2 );
    }
}


/**
 * Simple helper class to read the index part of a timeline file
 * 
 * @author sacha
 *
 */
class Index {
    int numIdx = 0;
    int idxInterval = 0;
    long byteSize = 0;
    IdxField[] field = null;
    
    public Index( RandomAccessFile raf ) throws IOException {
        byteSize = this.read( raf );
    }
    
    public long read( RandomAccessFile raf ) throws IOException {
        long nBytes = 0;
        numIdx = raf.readInt(); nBytes += 8;
        idxInterval = raf.readInt(); nBytes += 8;
        field = new IdxField[numIdx];
        for( int i = 0; i < numIdx; i++ ) {
            nBytes += field[i].read( raf );
        }
        return( nBytes );
    }
}


/**
 * Simple helper class to read the index fields in a timeline.
 * 
 * @author sacha
 *
 */
class IdxField {
    long begin = 0;
    long offset = 0;
    public long read( RandomAccessFile raf ) throws IOException {
        begin = raf.readLong();
        offset = raf.readLong();
        return( 16l ); // 8+8 bytes have been read.
    }
    public long write( RandomAccessFile raf ) throws IOException {
        raf.writeLong( begin );
        raf.writeLong( offset );
        return( 16l ); // 8+8 bytes written.
    }
}
