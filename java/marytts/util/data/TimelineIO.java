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
package marytts.util.data;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;


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
    public synchronized String getProcHeaderContents() {
        return( procHdr.getString() );
    }
    
    /**
     * Get the current byte position in the file
     */
    public synchronized long getBytePointer() throws IOException {
        return( raf.getFilePointer() );
    }
    
    /**
     * Get the current time position in the file
     */
    public synchronized long getTimePointer() {
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
    public synchronized long getNumDatagrams() {
        return( numDatagrams );
    }
    
    /**
     * Returns the position of the datagram zone
     */
    public synchronized long getDatagramsBytePos() {
        return( datagramsBytePos );
    }
    
    /**
     * Returns the timeline's sample rate.
     */
    public synchronized int getSampleRate() {
        return( sampleRate );
    }
    

    public synchronized Index getIndex()
    {
        return idx;
    }
    
    /**
     * Prints the index to System.out. (For testing purposes.))
     *
     */
    public synchronized void printIdx() {
        idx.print();
    }
    
    /*******************/
    /* MISC. METHODS   */
    /*******************/
    
    /**
     * Scales a discrete time to the timeline's sample rate.
     * 
     * @param reqSampleRate the externally given sample rate.
     * @param targetTimeInSamples a discrete time, with respect to the externally given sample rate.
     * 
     * @return a discrete time, in samples with respect to the timeline's sample rate.
     */
    protected long scaleTime( int reqSampleRate, long targetTimeInSamples ) {
        if ( reqSampleRate == sampleRate ) return( targetTimeInSamples );
        /* else */ return( (long)Math.round( (double)(reqSampleRate) * (double)(targetTimeInSamples) / (double)(sampleRate) ) );
    }
    
    /**
     * Unscales a discrete time from the timeline's sample rate.
      * 
     * @param reqSampleRate the externally given sample rate.
     * @param timelineTimeInSamples a discrete time, with respect to the timeline sample rate.
     * 
     * @return a discrete time, in samples with respect to the externally given sample rate.
    */
    protected long unScaleTime( int reqSampleRate, long timelineTimeInSamples ) {
        if ( reqSampleRate == sampleRate ) return( timelineTimeInSamples );
        /* else */ return( (long)Math.round( (double)(sampleRate) * (double)(timelineTimeInSamples) / (double)(reqSampleRate) ) );
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
    protected long loadHeaderAndIndex( String fileName ) throws IOException {
        
        /* Load the Mary header */
        maryHdr = new MaryHeader( raf );
        if ( !maryHdr.isMaryHeader() ) {
            throw new IOException( "File [" + fileName + "] is not a valid Mary format file." );
        }
        if ( maryHdr.getType() != MaryHeader.TIMELINE ) {
            throw new RuntimeException( "File [" + fileName + "] is not a valid timeline file." );
        }

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
    public class ProcHeader 
    {
        
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
        public ProcHeader( String procStr ) 
        {
            procHeader = procStr;
        }
        
        /****************/
        /* ACCESSORS    */
        /****************/

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
        public long loadProcHeader( RandomAccessFile rafIn ) throws IOException {
            long before = rafIn.getFilePointer();
            procHeader = rafIn.readUTF();
            long after = rafIn.getFilePointer();
            return after-before;
        }
        
        /**
         *  Method which writes the header to a RandomAccessFile.
         *  
         *  @return the number of written bytes.
         *  */
        public long dump( RandomAccessFile rafIn ) throws IOException {
            long before = rafIn.getFilePointer();
            rafIn.writeUTF( procHeader );
            long after = rafIn.getFilePointer();
            return after-before;
        }
    }


    /**
     * Simple helper class to read the index part of a timeline file.
     * The index points to datagrams at or before a certain point in time.
     * 
     * Note: If no datagram starts at the exact index time, it makes sense
     * to point to the previous datagram rather than the following one.
     * 
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
     * an index which points to a datagram falling BEFORE or ON the index location:
     * 
     * time axis --------------------------------->
     *             INDEX <-- REQUEST
     *               |
     *  DATAGRAM <---
     * 
     * Thus, forward hopping is always possible and the requested time can always be reached.
     * 
     * @author sacha
     */
    public  class Index 
    {
        private int idxInterval = 0;  // The fixed time interval (in samples) separating two index fields.
        
        /**
         * For index field i, bytePtrs[i] is the position in bytes, from the beginning of the file,
         * of the datagram coming on or just before that index field.
         */
        private long[] bytePtrs;
        
        /**
         * For index field i, timePtrs[i] is the time position in samples
         * of the datagram coming on or just before that index field.
         */
        private long[] timePtrs;
                
        
        /****************/
        /* CONSTRUCTORS */
        /****************/
        
        /**
         * File constructor: load the index from a data input stream or random access file.
         * 
         * */
        public Index( DataInput raf ) throws IOException {
            load( raf );
        }
        
        /**
         * Constructor which builds a new index with a specific index interval
         * and a given sample rate
         * 
         * @param idxInterval the index interval, samples
         * @param indexFields the actual index data
         */
        public Index(int idxInterval, Vector<IdxField> indexFields) {
            this.idxInterval = idxInterval;
            bytePtrs = new long[indexFields.size()];
            timePtrs = new long[indexFields.size()];
            for (int i=0; i<bytePtrs.length; i++) {
                IdxField f = indexFields.get(i);
                bytePtrs[i] = f.bytePtr;
                timePtrs[i] = f.timePtr;
            }
       }
        
        
        /*****************/
        /* I/O METHODS   */
        /*****************/
        
        /**
         * Method which loads an index from a data input (random access file or data input stream).
         * */
        public void load( DataInput rafIn ) throws IOException {
            int numIdx = rafIn.readInt();
            idxInterval = rafIn.readInt();
            
            bytePtrs = new long[numIdx];
            timePtrs = new long[numIdx];
            int numBytesToRead = 16 * numIdx + 16; // 2*8 bytes for each index field + 16 for prevBytePos and prevTimePos

            byte[] data = new byte[numBytesToRead];
            rafIn.readFully(data);
            DataInput bufIn = new DataInputStream(new ByteArrayInputStream(data));
            
            for( int i = 0; i < numIdx; i++ ) {
                bytePtrs[i] = bufIn.readLong();
                timePtrs[i] = bufIn.readLong();
            }
            /* Obsolete: Read the "last datagram" memory */
            /*prevBytePos =*/ bufIn.readLong();
            /*prevTimePos =*/ bufIn.readLong();
        }
        
        /**
         * Method which writes an index to a RandomAccessFile
         * */
        public long dump( RandomAccessFile rafIn ) throws IOException {
            long nBytes = 0;
            int numIdx = getNumIdx();
            rafIn.writeInt( numIdx );      nBytes += 4;
            rafIn.writeInt( idxInterval ); nBytes += 4;
            for( int i = 0; i < numIdx; i++ ) {
                rafIn.writeLong(bytePtrs[i]); nBytes += 8;
                rafIn.writeLong(timePtrs[i]); nBytes += 8;
            }
            // Obsolete, keep only for file format compatibility:
            // Register the "last datagram" memory as an additional field
            //rafIn.writeLong(prevBytePos);
            //rafIn.writeLong(prevTimePos);
            rafIn.writeLong(0l);
            rafIn.writeLong(0l);
            nBytes += 16l;
            
            return nBytes;
        }
        
        /**
         * Method which writes an index to stdout
         * */
        public void print() {
            System.out.println( "<INDEX>" );
            int numIdx = getNumIdx();
            System.out.println( "interval = " + idxInterval );
            System.out.println( "numIdx = " + numIdx );
            for( int i = 0; i < numIdx; i++ ) {
                System.out.println( "( " + bytePtrs[i] + " , " + timePtrs[i] + " )" );
            }
            /* Obsolete: Register the "last datagram" memory as an additional field */
            //System.out.println( "Last datagram: "
            //        + "( " + prevBytePos + " , " + prevTimePos + " )" );
            System.out.println( "</INDEX>" );
        }
        
        /*****************/
        /* ACCESSORS     */
        /*****************/
        /**
         * The number of index entries.
         */
        public int getNumIdx() {
            return bytePtrs.length;
        }
        
        /**
         * The interval, in samples, between two index entries.
         * @return
         */
        public int getIdxInterval() {
            return idxInterval;
        }
        
        public IdxField getIdxField( int i ) {
            if ( i < 0 ) {
                throw new IndexOutOfBoundsException( "Negative index." );
            }
            if (i >= bytePtrs.length) {
                throw new IndexOutOfBoundsException("Requested index no. "+i+", but highest is "+bytePtrs.length);
            }
            return new IdxField(bytePtrs[i], timePtrs[i]);
        }
        
        
        
        /*****************/
        /* OTHER METHODS */
        /*****************/
        
         
        /**
         * Returns the index field that comes immediately before or straight on the requested time.
         * 
         * @param timePosition
         * @return
         */
        public IdxField getIdxFieldBefore( long timePosition ) {
            int index = (int)( timePosition / idxInterval ); /* <= This is an integer division between two longs,
                                                            *    implying a flooring operation on the decimal result. */
            // System.out.println( "TIMEPOS=" + timePosition + " IDXINT=" + idxInterval + " IDX=" + idx );
            // System.out.flush();
            if ( index < 0 ) {
                throw new RuntimeException( "Negative index field: [" + index
                        + "] encountered when getting index before time=[" + timePosition
                        + "] (idxInterval=[" + idxInterval + "])." );
            }
            if ( index >= bytePtrs.length ) {
                index = bytePtrs.length - 1; // <= Protection against ArrayIndexOutOfBounds exception due to "time out of bounds"
            }
            return new IdxField(bytePtrs[index], timePtrs[index]);
        }
    }

    
    /**
     * Simple helper class to read the index fields in a timeline.
     * 
     * @author sacha
     *
     */
    public static class IdxField
    {
        // TODO: rethink if these should be public fields or if we should add accessors.
        public long bytePtr = 0;
        public long timePtr = 0;
        public IdxField() {
            bytePtr = 0;
            timePtr = 0;
        }
        public IdxField( long setBytePtr, long setTimePtr ) {
            bytePtr = setBytePtr;
            timePtr = setTimePtr;
        }
    }


}




