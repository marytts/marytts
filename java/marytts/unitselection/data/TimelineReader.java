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
package marytts.unitselection.data;


import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UTFDataFormatException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Vector;

import marytts.util.MaryUtils;
import marytts.util.Pair;
import marytts.util.data.MaryHeader;
import marytts.util.io.StreamUtils;



/**
 * The TimelineReader class provides an interface to read regularly or variably spaced
 * datagrams from a Timeline data file in Mary format.
 * 
 * @author sacha, marc
 *
 */
public class TimelineReader 
{
    protected MaryHeader maryHdr = null;   // The standard Mary header
    protected ProcHeader procHdr = null;   // The processing info header
    
    protected Index idx = null; // A global time index for the variable-sized datagrams

    /* Some specific header fields: */
    protected int sampleRate = 0;
    protected long numDatagrams = 0;
    
    protected int datagramsBytePos = 0;
    protected int timeIdxBytePos = 0;
    
    // exactly one of the two following variables will be non-null after load():
    private MappedByteBuffer mappedBB = null;
    private FileChannel fileChannel = null;
    
    
    
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
    public void load(String fileName) throws IOException {
        RandomAccessFile file = new RandomAccessFile( fileName, "r" );
        FileChannel fc = file.getChannel();
        // Expect header to be no bigger than 64k bytes
        ByteBuffer headerBB = ByteBuffer.allocate(0x10000);
        fc.read(headerBB);
        headerBB.limit(headerBB.position());
        headerBB.position(0);
        
        try {
            /* Load the Mary header */
            maryHdr = new MaryHeader(headerBB);
            if ( !maryHdr.isMaryHeader() ) {
                throw new IOException( "File is not a valid Mary format file." );
            }
            if ( maryHdr.getType() != MaryHeader.TIMELINE ) {
                throw new RuntimeException( "File is not a valid timeline file." );
            }

            /* Load the processing info header */
            procHdr = new ProcHeader(headerBB);
            
            /* Load the timeline dimensions */
            sampleRate = headerBB.getInt();
            numDatagrams = headerBB.getLong();
            
            /* Load the positions of the various subsequent components */
            datagramsBytePos = (int) headerBB.getLong();
            timeIdxBytePos = (int) headerBB.getLong();
        } catch (IOException ioe) {
            IOException myioe = new IOException("Problem loading headers of file "+fileName);
            myioe.initCause(ioe);
            throw myioe;
        }
        
        /* Go fetch the time index at the end of the file */
        fc.position(timeIdxBytePos);
        ByteBuffer indexBB = ByteBuffer.allocate((int)(fc.size() - timeIdxBytePos));
        fc.read(indexBB);
        indexBB.limit(indexBB.position());
        indexBB.position(0);
        idx = new Index(indexBB);

        
        // Try if we can use a mapped byte buffer:
        try {
            mappedBB = fc.map(FileChannel.MapMode.READ_ONLY, datagramsBytePos, timeIdxBytePos-datagramsBytePos);
            file.close(); // if map() succeeded, we don't need the file anymore.
        } catch (OutOfMemoryError ome) {
            MaryUtils.getLogger("Timeline").warn("Cannot use memory mapping for timeline file '"+fileName+"' -- falling back to piecewise reading");
        }
        if (mappedBB == null) {
            fileChannel = fc;
            assert fileChannel != null;
            // and leave file open
        }
    }

    /**
     * Return the content of the processing header as a String
     */
    public String getProcHeaderContents() {
        return( procHdr.getString() );
    }
    
    /**
     * Returns the number of datagrams in the timeline.
     * 
     * @return the number of datagrams, as a long.
     */
    public long getNumDatagrams() {
        return numDatagrams;
    }
    
    /**
     * Returns the position of the datagram zone in the original file.
     */
    protected long getDatagramsBytePos() {
        return datagramsBytePos;
    }
    
    /**
     * Returns the timeline's sample rate.
     */
    public int getSampleRate() {
        return sampleRate;
    }
    
    public Index getIndex() {
        return idx;
    }
    
    
    /******************/
    /* DATA ACCESSORS */
    /******************/
    
    /**
     * Skip the upcoming datagram at the current position of the byte buffer.
     * 
     * @return the duration of the datagram we skipped
     * @throws BufferUnderflowException if we cannot skip another datagram because we have reached the end of the byte buffer
     */
    protected long skipNextDatagram(ByteBuffer bb) throws BufferUnderflowException {
        long datagramDuration = bb.getLong();
        int datagramSize = bb.getInt();
        bb.position(bb.position() + datagramSize);
        return datagramDuration;
    }
    
    /**
     * Read and return the upcoming datagram from the given byte buffer.
     * Subclasses should override this method to create subclasses of Datagram.
     * 
     * @param bb the timeline byte buffer to read from
     * 
     * @return the current datagram, or null if EOF was encountered
     * 
     * @throws IOException
     */
    protected Datagram getNextDatagram(ByteBuffer bb) throws IOException {
        
        Datagram d = null;

        /* If the end of the datagram zone is reached, refuse to read */
        if (bb.position() == bb.limit() ) {
            //throw new IndexOutOfBoundsException( "Time out of bounds: you are trying to read a datagram at" +
            //        " a time which is bigger than the total timeline duration." );
            return null;
        }
        /* Else, pop the datagram out of the file */
        try {
            d = new Datagram(bb);
        }
        /* Detect a possible EOF encounter */
        catch ( Exception e ) {
            throw (IOException) new IOException( "While reading a datagram, EOF was met before the time index position: "
                    + "you may be dealing with a corrupted timeline file." ).initCause(e);
        }
   
        return d;
    }
    

    
    /**
     * Return an array of datagrams from the given byte buffer.
     * 
     * @param bb the timeline byte buffer to read from.
     * 
     * @param nDatagrams the number of datagrams to read.
     * 
     * @return an array of datagrams
     * 
     * @throws IOException
     */
    protected Datagram[] getNextDatagrams(ByteBuffer bb, int nDatagrams) throws IOException {
        Datagram[] buff = new Datagram[nDatagrams];
        for ( int i = 0; i < nDatagrams; i++ ) {
            buff[i] = getNextDatagram(bb);
        }
        return buff;
    }
    
    
    /**
     * Hop the datagrams in the given byte buffer until the one which begins at or contains the desired time
     * (time is in samples; the sample rate is assumed to be that of the timeline).
     * 
     * @param bb the timeline byte buffer to use
     * @param currentTimeInSamples the time position corresponding to the current position of the byte buffer
     * @param targetTimeInSamples the time location to reach.
     * 
     * @return the actual time at which we end up after hopping. This is less than or equal to targetTimeInSamples, never greater than it.
     * @throws IOException if there is a problem skipping the datagrams
     * @throws IllegalArgumentException if targetTimeInSamples is less than currentTimeInSamples
     */
    protected long hopToTime(ByteBuffer bb, long currentTimeInSamples, long targetTimeInSamples ) throws IOException, IllegalArgumentException {
        /* If the requested time is before the current time location, we cannot hop backwards to reach it;
         * send an error case. */
        if ( currentTimeInSamples > targetTimeInSamples ) {
            throw new IllegalArgumentException("Cannot hop back from time "+currentTimeInSamples+" to time "+targetTimeInSamples);
        }
        
        /* If the current time position is the requested time
         * do nothing, you are already at the right position */
        if (currentTimeInSamples == targetTimeInSamples) {
            return currentTimeInSamples;
        }
        /* Else hop: */
        int byteBefore = bb.position();
        long timeBefore = currentTimeInSamples;
        /* Hop until the datagram which comes just after the requested time */
        while ( currentTimeInSamples <= targetTimeInSamples ) { // Stop after the requested time, we will step back
            // to the correct time in case of equality
            timeBefore = currentTimeInSamples;
            byteBefore = bb.position();
            long skippedDuration = skipNextDatagram(bb);
            currentTimeInSamples += skippedDuration;
        }
        /* Do one step back so that the pointed datagram contains the requested time */
        bb.position(byteBefore);
        return timeBefore;
    }
    


    /**
     * This method produces a new byte buffer whose current position
     * represents the requested positionInFile. It cannot be assumed that
     * a call to byteBuffer.position() produces any meaningful values. 
     * @param positionInFile the position in the file which should be accessed as a byte buffer
     * @return a pair representing the byte buffer from which to read, and the exact time corresponding to the
     * current position of the byte buffer. No assumptions should be made regarding that position.
     * @throws IOException
     */
    protected Pair<ByteBuffer, Long> getByteBufferAtTime(long targetTimeInSamples) throws IOException {
        /* Seek for the time index which comes just before the requested time */
        IdxField idxFieldBefore = idx.getIdxFieldBefore( targetTimeInSamples );
        long time = idxFieldBefore.timePtr;
        ByteBuffer bb;
        if (mappedBB != null) {
            int bytePos = (int) (idxFieldBefore.bytePtr - datagramsBytePos);
            bb = mappedBB.duplicate();
            bb.position(bytePos);
        } else { // we must load a chunk of data from the FileChannel
            long bytePos = idxFieldBefore.bytePtr;
            int bufSize = 0x10000; // 64k
            bb = ByteBuffer.allocate(bufSize);
            fileChannel.read(bb, bytePos); // this will block if another thread is currently reading from fileChannel
            bb.limit(bb.position());
            bb.position(0);
        }
        time = hopToTime(bb, time, targetTimeInSamples);
        return new Pair<ByteBuffer, Long>(bb, time);
    }

    
    
    
    /**
     * Get a single datagram from a particular time location,
     * given in the timeline's sampling rate.
     * 
     * @param targetTimeInSamples the requested position, in samples.
     * 
     * @return a datagram.
     */
    public Datagram getDatagram( long targetTimeInSamples) throws IOException {
        Pair<ByteBuffer, Long> p = getByteBufferAtTime(targetTimeInSamples);
        ByteBuffer bb = p.getFirst();
        return getNextDatagram(bb);

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
    protected Datagram[] getDatagrams(long targetTimeInSamples, int nDatagrams, long timeSpanInSamples, int reqSampleRate, long[] returnOffset) throws IOException {
        /* Check the input arguments */
        if ( targetTimeInSamples < 0 ) {
            throw new IllegalArgumentException( "Can't get a datagram from a negative time position (given time position was [" + targetTimeInSamples + "])." );
        }
        // Get the datagrams by number or by time span?
        boolean byNumber;
        if ( timeSpanInSamples > 0 ) {
            byNumber = false;
        } else {
            byNumber = true;
            if (nDatagrams <= 0) {
                nDatagrams = 1; // return at least one datagram
            }
        }
        
        /* Resample the requested time location, in case the sample times are different between
         * the request and the timeline */
        long scaledTargetTime = scaleTime( reqSampleRate, targetTimeInSamples );
        
        Pair<ByteBuffer, Long> p = getByteBufferAtTime(scaledTargetTime);
        ByteBuffer bb = p.getFirst();
        long time = p.getSecond();
        if ( returnOffset != null ) { // return offset between target and actual start time
            returnOffset[0] = unScaleTime( reqSampleRate, (scaledTargetTime - time) );
        }

        ArrayList<Datagram> datagrams = new ArrayList<Datagram>(byNumber ? nDatagrams : 10);
        // endTime is stop criterion if reading by time scale:
        long endTime = byNumber ? -1 : scaleTime( reqSampleRate, (targetTimeInSamples+timeSpanInSamples) );
        int nRead = 0;
        boolean haveReadAll = false;
        while (!haveReadAll) {
            Datagram dat = getNextDatagram(bb);
            if (dat == null) {
                // we may have reached the end of the current byte buffer... try reading another:
                p = getByteBufferAtTime(time);
                bb = p.getFirst();
                dat = getNextDatagram(bb);
                if (dat == null) { // no, indeed we cannot read any more
                    break; // abort, we could not read all
                }
            }
            assert dat != null;
            time += dat.getDuration(); // duration in timeline sample rate
            nRead++; // number of datagrams read
            if (reqSampleRate != sampleRate) {
                dat.setDuration(unScaleTime(reqSampleRate, dat.getDuration())); // convert duration into reqSampleRate
            }
            datagrams.add(dat);
            if (byNumber && nRead == nDatagrams || !byNumber && time >= endTime) {
                haveReadAll = true;
            }
        }
        return (Datagram[])datagrams.toArray(new Datagram[0]);
    }
    
    
    /////////////////////// Convenience methods: variants of getDatagrams() ///////////////////////


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
    public Datagram[] getDatagrams(long targetTimeInSamples, long timeSpanInSamples, int reqSampleRate, long[] returnOffset ) throws IOException {
        return getDatagrams(targetTimeInSamples, -1, timeSpanInSamples, reqSampleRate, returnOffset);
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
    public Datagram[] getDatagrams( long targetTimeInSamples, int number, int reqSampleRate, long[] returnOffset ) throws IOException {
        return getDatagrams(targetTimeInSamples, number, -1, reqSampleRate, returnOffset);
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
    public Datagram[] getDatagrams(long targetTimeInSamples, long timeSpanInSamples, int reqSampleRate ) throws IOException {
        return getDatagrams(targetTimeInSamples, timeSpanInSamples, reqSampleRate, null);
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
     */
    public Datagram[] getDatagrams(long targetTimeInSamples, long timeSpanInSamples) throws IOException {
        return getDatagrams(targetTimeInSamples, timeSpanInSamples, sampleRate, null);
    }
    
    
    /**
     * Get a single datagram from a particular time location.
     * 
     * @param targetTimeInSamples the requested position, in samples.
     * @param reqSampleRate the sample rate for the requested times.
     * 
     * @return a datagram.
     */
    public Datagram getDatagram( long targetTimeInSamples, int reqSampleRate ) throws IOException {
        /* Resample the requested time location, in case the sample times are different between
         * the request and the timeline */
        long scaledTargetTime = scaleTime( reqSampleRate, targetTimeInSamples );
        Datagram dat = getDatagram(scaledTargetTime);
        if (dat == null) 
            return null;
        if ( reqSampleRate != sampleRate ) dat.setDuration(unScaleTime( reqSampleRate, dat.getDuration() )); // => Don't forget to stay time-consistent!
        return( dat );
    }
    
    
    /**
     * Get the datagrams spanning a particular unit.
     * 
     * @param unit The requested speech unit, containing its own position and duration.
     * @param reqSampleRate the sample rate for the requested times, as specified in the "unit space".
     * 
     * @return an array of datagrams
     */
    public Datagram[] getDatagrams( Unit unit, int reqSampleRate ) throws IOException {
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
    public Datagram[] getDatagrams( Unit unit, int reqSampleRate, long[] returnOffset ) throws IOException {
        return( getDatagrams( unit.startTime, (long)(unit.duration), reqSampleRate, returnOffset ) );
    }
    
    
    
    
    
    
    
    
    
    
    
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
    
    
    /*****************************************/
    /* HELPER CLASSES                        */
    /*****************************************/


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
    public static class Index 
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
        
        public Index(ByteBuffer bb) throws BufferUnderflowException {
            load(bb);
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
         * Method which loads an index from a byte buffer.
         * */
        public void load(ByteBuffer bb) {
            int numIdx = bb.getInt();
            idxInterval = bb.getInt();
            
            bytePtrs = new long[numIdx];
            timePtrs = new long[numIdx];

            for( int i = 0; i < numIdx; i++ ) {
                bytePtrs[i] = bb.getLong();
                timePtrs[i] = bb.getLong();
            }
            /* Obsolete: Read the "last datagram" memory */
            /*prevBytePos =*/ bb.getLong();
            /*prevTimePos =*/ bb.getLong();
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

    
    /**
     * 
     * Simple helper class to load the processing header.
     * 
     * @author sacha
     *
     */
    public static class ProcHeader 
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
        
        public ProcHeader(ByteBuffer bb) throws BufferUnderflowException, UTFDataFormatException {
            loadProcHeader(bb);
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
        private void loadProcHeader( RandomAccessFile rafIn ) throws IOException {
            procHeader = rafIn.readUTF();
        }
        
        /**
         *  Method which loads the header from a byte buffer.
         *  */
        private void loadProcHeader(ByteBuffer bb) throws BufferUnderflowException, UTFDataFormatException {
            procHeader = StreamUtils.readUTF(bb);
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

    
    
}