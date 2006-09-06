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
import java.util.Vector;

import de.dfki.lt.mary.unitselection.voiceimport_reorganized.MaryHeader;

/**
 * Base class for the TimelineReader and TimelineWriter classes.
 * 
 * 
 * @author sacha
 *
 */
public class TimelineIO
{
    /****************/
    /* DATA FIELDS  */
    /****************/
    
    public final static int DEFAULT_INCREMENT = 128; // Default capacityIncrement for vectors
    
    /* Protected fields */
    protected RandomAccessFile raf = null; // The file to read from
    protected MaryHeader maryHdr = null;   // The standard Mary header
    protected ProcHeader procHdr = null;   // The processing info header
    
    protected Index idx = null; // A global time index for the variable-sized datagrams

    /* Some specific header fields: */
    protected int sampleRate = 0;
    protected long numDatagrams = 0;
    
    protected long datagramsBytePos = 0;
    protected long timeIdxBytePos = 0;
    
    /* Pointers to navigate the file: */
    protected long timePtr = 0; // A time pointer to keep track of the time position in the file
    // Note: a file pointer, keeping track of the byte position in the file, is implicitely
    //  maintained by the browsed RandomAccessFile.
    
    
    /*****************/
    /* ACCESSORS     */
    /*****************/
    
    /**
     * Return the content of the processing header as a String
     */
    public String getProcHeaderContents() {
        return( procHdr.getString() );
    }
    
    /**
     * Get the current byte position in the file
     */
    public long getBytePointer() throws IOException {
        return( raf.getFilePointer() );
    }
    
    /**
     * Get the current time position in the file
     */
    public long getTimePointer() {
        return( timePtr );
    }
    
    /**
     * Set the current byte position in the file
     */
    protected void setBytePointer( long bytePos ) throws IOException {
        raf.seek( bytePos );
    }
    
    /**
     * Set the current time position in the file
     */
    protected void setTimePointer( long timePosition ) {
        timePtr = timePosition;
    }
    
    /**
     * Returns the current number of datagrams in the timeline.
     * 
     * @return the number of datagrams, as a long. Warning: you may have to cast this
     * value into an int if you want to use it to create an array.
     */
    public long getNumDatagrams() {
        return( numDatagrams );
    }
    
    /**
     * Returns the position of the datagram zone
     */
    public long getDatagramsBytePos() {
        return( datagramsBytePos );
    }
    
    /* A secret function for testign purposes... */
    public void printIdx() {
        idx.print();
    }
    
    /*****************/
    /* I/O METHODS   */
    /*****************/
    
    /**
     * Load the headers and the info, the position the file pointer to the beginning of the datagram zone
     * and the time pointer to 0.
     * 
     * @throws IOException
     */
    protected long loadHeaderAndIndex() throws IOException {
        
        /* Load the Mary header */
        maryHdr = new MaryHeader( raf );
        
        /* Load the processing info header */
        procHdr = new ProcHeader( raf );
        
        /* Load the timeline dimensions */
        sampleRate = raf.readInt();
        numDatagrams = raf.readLong();
        
        /* Load the positions of the various subsequent components */
        datagramsBytePos = raf.readLong();
        timeIdxBytePos = raf.readLong();
        
        /* Go fetch the time index at the end of the file, and come back to the datagram zone */
        raf.seek( timeIdxBytePos );
        idx = new Index( raf );
        raf.seek( datagramsBytePos );
        
        /* Make sure the time pointer is zero */
        setTimePointer( 0l );
        
        return( raf.getFilePointer() );
    }

}


/*****************************************/
/* HELPER CLASSES                        */
/*****************************************/

/**
 * 
 * Simple helper class to load the processing header.
 * 
 * @author sacha
 *
 */
class ProcHeader {
    
    private String procHeader = null;
    
    /****************/
    /* CONSTRUCTORS */
    /****************/
    
    /**
     *  Constructor which loads the procHeader from a RandomAccessFile
     *  */
    public ProcHeader( RandomAccessFile raf )  throws IOException {
        loadProcHeader( raf );
    }
    
    /**
     *  Constructor which makes the procHeader from a String
     *  */
    public ProcHeader( String procStr )  throws IOException {
        procHeader = procStr;
    }
    
    /****************/
    /* ACCESSORS    */
    /****************/
    public long getByteSize() {
        return( (long)(procHeader.length() * 2) + 2 ); // This size accounts for the string length
    }
    public int getCharSize() {
        return( procHeader.length() );
    }
    public String getString() {
        return( procHeader );
    }
    
    /*****************/
    /* I/O METHODS   */
    /*****************/
    
    /**
     *  Method which loads the header from a RandomAccessFile.
     *  */
    public long loadProcHeader( RandomAccessFile raf ) throws IOException {
        procHeader = raf.readUTF();
        return( getByteSize() );
    }
    
    /**
     *  Method which writes the header to a RandomAccessFile.
     *  
     *  @return the number of written bytes.
     *  */
    public long dump( RandomAccessFile raf ) throws IOException {
        raf.writeUTF( procHeader );
        return( procHeader.length() * 2 + 2 );
    }
}


/**
 * Simple helper class to read the index part of a timeline file
 * 
 * @author sacha
 *
 */
class Index {
    
    /* Default interval between index values
     * (in SECONDS, it will be converted to samples later on) */
    public final static double DEFAULTIDXINTERVAL_IN_SECONDS = 30.0d; // seconds
    
    private int idxInterval = 0;  // The fixed time interval (in samples) separating two index fields
    private Vector field = null;  // The actual index fields
    private static final int INCREMENT_SIZE = 512; // The field vector's capacityIncrement
                                                   // (see the Vector object in the Java reference).
    
    /* Memory of the location of the previous unit (see feed() below.) */
    private long prevBytePos = 0;
    private long prevTimePos = 0;
    
    /****************/
    /* CONSTRUCTORS */
    /****************/
    
    /**
     * File constructor: load the index from a random access file.
     * 
     * */
    public Index( RandomAccessFile raf ) throws IOException {
        load( raf );
    }
    
    /**
     * Constructor which builds a new index with the default index interval (30 seconds)
     * at a given sample rate
     * 
     * @param setSampleRate the requested sample rate
     * @param setInitialBytePosition the byte position of the first possible datagram
     * (i.e., the position of the datagram zone)
     */
    public Index( int setSampleRate, long setInitialBytePosition ) {
        if ( setSampleRate <= 0 ) {
            throw new RuntimeException( "The sample rate can't be negative or null when building a timeline index." );
        }
        idxInterval = (int)Math.round( DEFAULTIDXINTERVAL_IN_SECONDS * (double)(setSampleRate) );
        field = new Vector( 1, INCREMENT_SIZE );
        field.add( new IdxField(setInitialBytePosition,0) ); /* Initialize the first field */
        prevBytePos = setInitialBytePosition;
        prevTimePos = 0;
    }
    
    /**
     * Constructor which builds a new index with the default index interval (30 seconds)
     * at a given sample rate
     * 
     * @param setIdxIntervalInSeconds the requested index interval, in seconds
     * @param setSampleRate the requested sample rate
     * @param setInitialBytePosition the byte position of the first possible datagram
     * (i.e., the position of the datagram zone)
     */
    public Index( double setIdxIntervalInSeconds, int setSampleRate, long setInitialBytePosition ) {
        if ( setSampleRate <= 0 ) {
            throw new RuntimeException( "The index interval can't be negative or null when building a timeline index." );
        }
        if ( setIdxIntervalInSeconds <= 0.0 ) {
            throw new RuntimeException( "The index interval can't be negative or null when building a timeline index." );
        }
        idxInterval = (int)Math.round( setIdxIntervalInSeconds * (double)(setSampleRate) );
        field = new Vector( 1, INCREMENT_SIZE );
        field.add( new IdxField(setInitialBytePosition,0) ); /* Initialize the first field */
        prevBytePos = setInitialBytePosition;
        prevTimePos = 0;
   }
    
    
    /*****************/
    /* I/O METHODS   */
    /*****************/
    
    /**
     * Method which loads an index from a random access file.
     * */
    public void load( RandomAccessFile raf ) throws IOException {
        int numIdx = raf.readInt();
        idxInterval = raf.readInt();
        
        field = new Vector( numIdx, INCREMENT_SIZE );
        
        for( int i = 0; i < numIdx; i++ ) {
            field.add( new IdxField(raf) );
       }
        /* Read the "last datagram" memory */
        prevBytePos = raf.readLong();
        prevTimePos = raf.readLong();
    }
    
    /**
     * Method which writes an index to a RandomAccessFile
     * */
    public long dump( RandomAccessFile raf ) throws IOException {
        long nBytes = 0;
        int numIdx = getNumIdx();
        raf.writeInt( numIdx );      nBytes += 4;
        raf.writeInt( idxInterval ); nBytes += 4;
        IdxField buffer = null;
        for( int i = 0; i < numIdx; i++ ) {
            buffer = (IdxField)( field.elementAt(i) );
            nBytes += buffer.write( raf );
        }
        /* Register the "last datagram" memory as an additional field */
        raf.writeLong(prevBytePos);
        raf.writeLong(prevTimePos);
        nBytes += 16l;
        
        return( nBytes );
    }
    
    /**
     * Method which writes an index to stdout
     * */
    public void print() {
        System.out.println( "<INDEX>" );
        int numIdx = getNumIdx();
        System.out.println( "interval = " + idxInterval );
        System.out.println( "numIdx = " + numIdx );
        IdxField buffer = null;
        for( int i = 0; i < numIdx; i++ ) {
            buffer = (IdxField)( field.elementAt(i) );
            System.out.println( "( " + buffer.bytePtr + " , " + buffer.timePtr + " )" );
        }
        /* Register the "last datagram" memory as an additional field */
        System.out.println( "Last datagram: "
                + "( " + prevBytePos + " , " + prevTimePos + " )" );
        System.out.println( "</INDEX>" );
    }
    
    /*****************/
    /* ACCESSORS     */
    /*****************/
    public int getNumIdx() {
        return( field.size() );
    }
    public int getIdxInterval() {
        return( idxInterval );
    }
    public IdxField getIdxField( int i ) {
        if ( i < 0 ) {
            throw new RuntimeException( "Negative index." );
        }
        return( (IdxField)(field.elementAt(i)) );
    }
    public long getPrevTimePos() {
        return( prevTimePos );
    }
    
    
    /*****************/
    /* OTHER METHODS */
    /*****************/
    
    /**
     * Feeds a file position (in bytes) and a time position (in samples) from a timeline,
     * and determines if a new index field is to be added.
     * 
     * @return the number of index fields after the feed.
     */
    public int feed( long bytePosition, long timePosition ) {
        /* Get the time associated with the yet to come index field */
        int currentNumIdx = field.size();
        long nextIdxTime = currentNumIdx * idxInterval;
        /* If the current time position passes the next possible index field,
         * register the PREVIOUS datagram position in the new index field */
        if ( nextIdxTime < timePosition ) {
//            System.out.println( "Hitting a new index at position\t[" + bytePosition + "," + timePosition + "]." );
//            System.out.println( "The crossed index is [" + nextIdxTime + "]." );
//            System.out.println( "The registered (previous) position is\t[" + prevBytePos + "," + prevTimePos + "]." );
//            IdxField testField = (IdxField)field.elementAt(currentNumIdx-1);
//            System.out.println( "The previously indexed position was\t[" + testField.bytePtr + "," + testField.timePtr + "]." );
            
            field.add( new IdxField(prevBytePos,prevTimePos) );
        }
        
        /* Note:
         * If one would store the location of the datagram which comes just after the index
         * position (the currently tested datagram), there would be a possibility that
         * a particular time request falls between the index and the datagram:
         * 
         * time axis --------------------------------->
         *             INDEX <-- REQUEST
         *               |
         *                ---------------> DATAGRAM
         *                
         * This would require a subsequent backwards time hopping, which is impossible
         * because the datagrams are a singly linked list.
         * 
         * By registering the location of the previous datagram, any time request will find
         * an index which points to a datagram falling BEFORE on ON the index location:
         * 
         * time axis --------------------------------->
         *             INDEX <-- REQUEST
         *               |
         *  DATAGRAM <---
         * 
         * Thus, forward hopping is always possible and the requested time can always be reached.
         * 
         * */
        
        /* Memorize the observed datagram position */
        prevBytePos = bytePosition;
        prevTimePos = timePosition;
        
        /* Return the (possibly new) index size */
        return( field.size() );
    }
    
    /**
     * Returns the index field that comes immediately before or straight on the requested time.
     * 
     * @param timePosition
     * @return
     */
    public IdxField getIdxFieldBefore( long timePosition ) {
        int idx = (int)Math.floor( (long)(timePosition) / (long)(idxInterval) ); /* The castings aim at preventing an automatic cast to double,
                                                                                  * I'm not sure that they are needed. */
        if ( idx < 0 ) {
            throw new RuntimeException( "Negative index field: [" + idx
                    + "] encountered when getting index before time=[" + timePosition
                    + "] (idxInterval=[" + idxInterval + "])." );
        }
        return( (IdxField)( field.elementAt( idx ) ) );
    }
    
}


/**
 * Simple helper class to read the index fields in a timeline.
 * 
 * @author sacha
 *
 */
class IdxField {
    long bytePtr = 0;
    long timePtr = 0;
    public IdxField() {
        bytePtr = 0;
        timePtr = 0;
    }
    public IdxField( long setBytePtr, long setTimePtr ) {
        bytePtr = setBytePtr;
        timePtr = setTimePtr;
    }
    public IdxField( RandomAccessFile raf ) throws IOException {
        bytePtr = raf.readLong();
        timePtr = raf.readLong();
     }
    public long write( RandomAccessFile raf ) throws IOException {
        raf.writeLong( bytePtr );
        raf.writeLong( timePtr );
        return( 16l ); // 8+8 bytes written.
    }
}
