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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UTFDataFormatException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Vector;

import marytts.exceptions.MaryConfigurationException;
import marytts.util.MaryUtils;
import marytts.util.Pair;
import marytts.util.data.Datagram;
import marytts.util.data.MaryHeader;
import marytts.util.io.StreamUtils;

/**
 * The TimelineReader class provides an interface to read regularly or variably spaced datagrams from a Timeline data file in Mary
 * format.
 * 
 * @author sacha, marc
 *
 */
public class TimelineReader {
	protected MaryHeader maryHdr = null; // The standard Mary header
	protected ProcHeader procHdr = null; // The processing info header

	protected Index idx = null; // A global time index for the variable-sized datagrams

	/* Some specific header fields: */
	protected int sampleRate = 0;
	protected long numDatagrams = 0;
	/**
	 * The total duration of the timeline data, in samples. This is only computed upon request.
	 */
	protected long totalDuration = -1;

	protected int datagramsBytePos = 0;
	protected int timeIdxBytePos = 0;

	// exactly one of the two following variables will be non-null after load():
	private MappedByteBuffer mappedBB = null;
	private FileChannel fileChannel = null;

	/****************/
	/* CONSTRUCTORS */
	/****************/

	/**
	 * Construct a timeline from the given file name.
	 * 
	 * Aiming for the fundamental guarantee: If an instance of this class is created, it is usable.
	 * 
	 * @param fileName
	 *            The file to read the timeline from. Must be non-null and point to a valid timeline file.
	 * @throws NullPointerException
	 *             if null argument is given
	 * @throws MaryConfigurationException
	 *             if no timeline reader can be instantiated from fileName
	 */
	public TimelineReader(String fileName) throws MaryConfigurationException {
		this(fileName, true);
	}

	/**
	 * Construct a timeline from the given file name.
	 * 
	 * Aiming for the fundamental guarantee: If an instance of this class is created, it is usable.
	 * 
	 * @param fileName
	 *            The file to read the timeline from. Must be non-null and point to a valid timeline file.
	 * @param tryMemoryMapping
	 *            if true, will attempt to read audio data via a memory map, and fall back to piecewise reading. If false, will
	 *            immediately go for piecewise reading using a RandomAccessFile.
	 * @throws NullPointerException
	 *             if null argument is given
	 * @throws MaryConfigurationException
	 *             if no timeline reader can be instantiated from fileName
	 */
	public TimelineReader(String fileName, boolean tryMemoryMapping) throws MaryConfigurationException {
		if (fileName == null) {
			throw new NullPointerException("Filename is null");
		}
		try {
			load(fileName, tryMemoryMapping);
		} catch (Exception e) {
			throw new MaryConfigurationException("Cannot load timeline file from " + fileName, e);
		}
	}

	/**
	 * Only subclasses can instantiate a TimelineReader object that doesn't call {@link #load(String)}. It is their responsibility
	 * then to ensure the fundamental guarantee.
	 */
	protected TimelineReader() {
	}

	/**
	 * Load a timeline from a file.
	 * 
	 * @param fileName
	 *            The file to read the timeline from. Must be non-null and point to a valid timeline file.
	 * 
	 * @throws IOException
	 *             if a problem occurs during reading
	 * @throws BufferUnderflowException
	 *             if a problem occurs during reading
	 * @throws MaryConfigurationException
	 *             if fileName does not point to a valid timeline file
	 */
	protected void load(String fileName) throws IOException, BufferUnderflowException, MaryConfigurationException,
			NullPointerException {
		load(fileName, true);
	}

	/**
	 * Load a timeline from a file.
	 * 
	 * @param fileName
	 *            The file to read the timeline from. Must be non-null and point to a valid timeline file.
	 * @param tryMemoryMapping
	 *            tryMemoryMapping
	 * @throws IOException
	 *             if a problem occurs during reading
	 * @throws BufferUnderflowException
	 *             if a problem occurs during reading
	 * @throws MaryConfigurationException
	 *             if fileName does not point to a valid timeline file
	 * @throws NullPointerException
	 *             NullPointerException
	 */
	protected void load(String fileName, boolean tryMemoryMapping) throws IOException, BufferUnderflowException,
			MaryConfigurationException, NullPointerException {
		assert fileName != null : "filename is null";

		RandomAccessFile file = new RandomAccessFile(fileName, "r");
		FileChannel fc = file.getChannel();
		// Expect header to be no bigger than 64k bytes
		ByteBuffer headerBB = ByteBuffer.allocate(0x10000);
		fc.read(headerBB);
		headerBB.limit(headerBB.position());
		headerBB.position(0);

		maryHdr = new MaryHeader(headerBB);
		if (maryHdr.getType() != MaryHeader.TIMELINE) {
			throw new MaryConfigurationException("File is not a valid timeline file.");
		}
		/* Load the processing info header */
		procHdr = new ProcHeader(headerBB);

		/* Load the timeline dimensions */
		sampleRate = headerBB.getInt();
		numDatagrams = headerBB.getLong();
		if (sampleRate <= 0 || numDatagrams < 0) {
			throw new MaryConfigurationException("Illegal values in timeline file.");
		}

		/* Load the positions of the various subsequent components */
		datagramsBytePos = (int) headerBB.getLong();
		timeIdxBytePos = (int) headerBB.getLong();
		if (timeIdxBytePos < datagramsBytePos) {
			throw new MaryConfigurationException("File seems corrupt: index is expected after data, not before");
		}

		/* Go fetch the time index at the end of the file */
		fc.position(timeIdxBytePos);
		ByteBuffer indexBB = ByteBuffer.allocate((int) (fc.size() - timeIdxBytePos));
		fc.read(indexBB);
		indexBB.limit(indexBB.position());
		indexBB.position(0);
		idx = new Index(indexBB);

		if (tryMemoryMapping) {
			// Try if we can use a mapped byte buffer:
			try {
				mappedBB = fc.map(FileChannel.MapMode.READ_ONLY, datagramsBytePos, timeIdxBytePos - datagramsBytePos);
				file.close(); // if map() succeeded, we don't need the file anymore.
			} catch (IOException ome) {
				MaryUtils.getLogger("Timeline").warn(
						"Cannot use memory mapping for timeline file '" + fileName + "' -- falling back to piecewise reading");
			}
		}
		if (!tryMemoryMapping || mappedBB == null) { // use piecewise reading
			fileChannel = fc;
			assert fileChannel != null;
			// and leave file open
		}

		// postconditions:
		assert idx != null;
		assert procHdr != null;
		assert fileChannel == null && mappedBB != null || fileChannel != null && mappedBB == null;
	}

	/**
	 * Return the content of the processing header as a String.
	 * 
	 * @return a non-null string representing the proc header.
	 */
	public String getProcHeaderContents() {
		return procHdr.getString();
	}

	/**
	 * Returns the number of datagrams in the timeline.
	 * 
	 * @return the (non-negative) number of datagrams, as a long.
	 */
	public long getNumDatagrams() {
		assert numDatagrams >= 0;
		return numDatagrams;
	}

	/**
	 * Returns the position of the datagram zone in the original file.
	 * 
	 * @return the byte position of the datagram zone.
	 */
	protected long getDatagramsBytePos() {
		return datagramsBytePos;
	}

	/**
	 * Returns the timeline's sample rate.
	 * 
	 * @return the sample rate as a positive integer.
	 */
	public int getSampleRate() {
		assert sampleRate > 0;
		return sampleRate;
	}

	/**
	 * Return the total duration of all data in this timeline. Implementation note: this is an expensive operation that should not
	 * be used in production.
	 * 
	 * @return a non-negative long representing the accumulated duration of all datagrams.
	 * @throws MaryConfigurationException
	 *             if the duration cannot be obtained.
	 */
	public long getTotalDuration() throws MaryConfigurationException {
		if (totalDuration == -1) {
			computeTotalDuration();
		}
		assert totalDuration >= 0;
		return totalDuration;
	}

	/**
	 * Compute the total duration of a timeline. This is an expensive method, since it goes through all datagrams to compute this
	 * duration. It should not normally be used in production.
	 * 
	 * @throws MaryConfigurationException
	 *             if the duration could not be computed.
	 */
	protected void computeTotalDuration() throws MaryConfigurationException {
		long time = 0;
		long nRead = 0;
		boolean haveReadAll = false;
		try {
			Pair<ByteBuffer, Long> p = getByteBufferAtTime(0);
			ByteBuffer bb = p.getFirst();
			assert p.getSecond() == 0;
			while (!haveReadAll) {
				Datagram dat = getNextDatagram(bb);
				if (dat == null) {
					// we may have reached the end of the current byte buffer... try reading another:
					p = getByteBufferAtTime(time);
					bb = p.getFirst();
					assert p.getSecond() == time;
					dat = getNextDatagram(bb);
					if (dat == null) { // no, indeed we cannot read any more
						break; // abort, we could not read all
					}
				}
				assert dat != null;
				time += dat.getDuration(); // duration in timeline sample rate
				nRead++; // number of datagrams read
				if (nRead == numDatagrams) {
					haveReadAll = true;
				}
			}
		} catch (Exception e) {
			throw new MaryConfigurationException("Could not compute total duration", e);
		}
		if (!haveReadAll) {
			throw new MaryConfigurationException("Could not read all datagrams to compute total duration");
		}
		totalDuration = time;
	}

	/**
	 * The index object.
	 * 
	 * @return the non-null index object.
	 */
	public Index getIndex() {
		assert idx != null;
		return idx;
	}

	// Helper methods

	/**
	 * Scales a discrete time to the timeline's sample rate.
	 * 
	 * @param reqSampleRate
	 *            the externally given sample rate.
	 * @param targetTimeInSamples
	 *            a discrete time, with respect to the externally given sample rate.
	 * @return a discrete time, in samples with respect to the timeline's sample rate.
	 */
	protected long scaleTime(int reqSampleRate, long targetTimeInSamples) {
		if (reqSampleRate == sampleRate)
			return (targetTimeInSamples);
		/* else */return ((long) Math.round((double) (reqSampleRate) * (double) (targetTimeInSamples) / (double) (sampleRate)));
	}

	/**
	 * Unscales a discrete time from the timeline's sample rate.
	 * 
	 * @param reqSampleRate
	 *            the externally given sample rate.
	 * @param timelineTimeInSamples
	 *            a discrete time, with respect to the timeline sample rate.
	 * @return a discrete time, in samples with respect to the externally given sample rate.
	 */
	protected long unScaleTime(int reqSampleRate, long timelineTimeInSamples) {
		if (reqSampleRate == sampleRate)
			return (timelineTimeInSamples);
		/* else */return ((long) Math.round((double) (sampleRate) * (double) (timelineTimeInSamples) / (double) (reqSampleRate)));
	}

	/******************/
	/* DATA ACCESSORS */
	/******************/

	/**
	 * Skip the upcoming datagram at the current position of the byte buffer.
	 * 
	 * @param bb
	 *            bb
	 * @return the duration of the datagram we skipped
	 * @throws IOException
	 *             if we cannot skip another datagram because we have reached the end of the byte buffer
	 */
	protected long skipNextDatagram(ByteBuffer bb) throws IOException {
		long datagramDuration = bb.getLong();
		int datagramSize = bb.getInt();
		if (bb.position() + datagramSize > bb.limit()) {
			throw new IOException("cannot skip datagram: it is not fully contained in byte buffer");
		}
		bb.position(bb.position() + datagramSize);
		return datagramDuration;
	}

	/**
	 * Read and return the upcoming datagram from the given byte buffer. Subclasses should override this method to create
	 * subclasses of Datagram.
	 * 
	 * @param bb
	 *            the timeline byte buffer to read from
	 * 
	 * @return the current datagram, or null if EOF was encountered
	 */
	protected Datagram getNextDatagram(ByteBuffer bb) {
		assert bb != null;
		// If the end of the datagram zone is reached, refuse to read
		if (bb.position() == bb.limit()) {
			return null;
		}
		// Else, read the datagram from the file
		try {
			return new Datagram(bb);
		} catch (IOException ioe) {
			return null;
		}
	}

	/**
	 * Hop the datagrams in the given byte buffer until the one which begins at or contains the desired time (time is in samples;
	 * the sample rate is assumed to be that of the timeline).
	 * 
	 * @param bb
	 *            the timeline byte buffer to use. Must not be null.
	 * @param currentTimeInSamples
	 *            the time position corresponding to the current position of the byte buffer. Must not be negative.
	 * @param targetTimeInSamples
	 *            the time location to reach. Must not be less than currentTimeInSamples
	 * 
	 * @return the actual time at which we end up after hopping. This is less than or equal to targetTimeInSamples, never greater
	 *         than it.
	 * @throws IOException
	 *             if there is a problem skipping the datagrams
	 * @throws IllegalArgumentException
	 *             if targetTimeInSamples is less than currentTimeInSamples
	 */
	protected long hopToTime(ByteBuffer bb, long currentTimeInSamples, long targetTimeInSamples) throws IOException,
			IllegalArgumentException {
		assert bb != null;
		assert currentTimeInSamples >= 0;
		assert targetTimeInSamples >= currentTimeInSamples : "Cannot hop back from time " + currentTimeInSamples + " to time "
				+ targetTimeInSamples;

		/*
		 * If the current time position is the requested time do nothing, you are already at the right position
		 */
		if (currentTimeInSamples == targetTimeInSamples) {
			return currentTimeInSamples;
		}
		/* Else hop: */
		int byteBefore = bb.position();
		long timeBefore = currentTimeInSamples;
		/* Hop until the datagram which comes just after the requested time */
		while (currentTimeInSamples <= targetTimeInSamples) { // Stop after the requested time, we will step back
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
	 * This method produces a new byte buffer whose current position represents the requested positionInFile. It cannot be assumed
	 * that a call to byteBuffer.position() produces any meaningful values. The byte buffer may represent only a part of the
	 * available data; however, at least one datagram can be read from the byte buffer. If no further data can be read from it, a
	 * new byte buffer must be obtained by calling this method again with a new target time.
	 * 
	 * @param targetTimeInSamples
	 *            the time position in the file which should be accessed as a byte buffer, in samples. Must be non-negative and
	 *            less than the total duration of the timeline.
	 * @return a pair representing the byte buffer from which to read, and the exact time corresponding to the current position of
	 *         the byte buffer. The position as such is not meaningful; the time is guaranteed to be less than or equal to
	 *         targetTimeInSamples.
	 * @throws IOException
	 *             IOException
	 * @throws BufferUnderflowException
	 *             , BufferUnderflowException if no byte buffer can be obtained for the requested time.
	 */
	protected Pair<ByteBuffer, Long> getByteBufferAtTime(long targetTimeInSamples) throws IOException, BufferUnderflowException {
		if (mappedBB != null) {
			return getMappedByteBufferAtTime(targetTimeInSamples);
		} else {
			return loadByteBufferAtTime(targetTimeInSamples);
		}
	}

	protected Pair<ByteBuffer, Long> getMappedByteBufferAtTime(long targetTimeInSamples) throws IllegalArgumentException,
			IOException {
		assert mappedBB != null;
		/* Seek for the time index which comes just before the requested time */
		IdxField idxFieldBefore = idx.getIdxFieldBefore(targetTimeInSamples);
		long time = idxFieldBefore.timePtr;
		int bytePos = (int) (idxFieldBefore.bytePtr - datagramsBytePos);
		ByteBuffer bb = mappedBB.duplicate();
		bb.position(bytePos);
		time = hopToTime(bb, time, targetTimeInSamples);
		return new Pair<ByteBuffer, Long>(bb, time);
	}

	protected Pair<ByteBuffer, Long> loadByteBufferAtTime(long targetTimeInSamples) throws IOException {
		assert fileChannel != null;
		// we must load a chunk of data from the FileChannel
		int bufSize = 0x10000; // 64 kB
		/* Seek for the time index which comes just before the requested time */
		IdxField idxFieldBefore = idx.getIdxFieldBefore(targetTimeInSamples);
		long time = idxFieldBefore.timePtr;
		long bytePos = idxFieldBefore.bytePtr;
		if (bytePos + bufSize > timeIdxBytePos) { // must not read index data as datagrams
			bufSize = (int) (timeIdxBytePos - bytePos);
		}
		ByteBuffer bb = loadByteBuffer(bytePos, bufSize);

		while (true) {
			if (!canReadDatagramHeader(bb)) {
				bb = loadByteBuffer(bytePos, bufSize);
				assert canReadDatagramHeader(bb);
			}
			int posBefore = bb.position();
			Datagram d = new Datagram(bb, false);
			if (time + d.getDuration() > targetTimeInSamples) { // d is our datagram
				bb.position(posBefore);
				int datagramNumBytes = Datagram.NUM_HEADER_BYTES + d.getLength();
				// need to make sure we return a byte buffer from which d can be read
				if (!canReadAmount(bb, datagramNumBytes)) {
					bb = loadByteBuffer(bytePos, Math.max(datagramNumBytes, bufSize));
				}
				assert canReadAmount(bb, datagramNumBytes);
				break;
			} else {
				// keep on skipping
				time += d.getDuration();
				if (canReadAmount(bb, d.getLength())) {
					bb.position(bb.position() + d.getLength());
				} else {
					bytePos += bb.position();
					bytePos += d.getLength();
					bb = loadByteBuffer(bytePos, bufSize);
				}
			}
		}
		return new Pair<ByteBuffer, Long>(bb, time);
	}

	/**
	 * @param bytePos
	 *            position in fileChannel from which to load the byte buffer
	 * @param bufSize
	 *            size of the byte buffer
	 * @return the byte buffer, loaded and set such that limit is bufSize and position is 0
	 * @throws IOException
	 *             if the data cannot be read from fileChannel
	 */
	private ByteBuffer loadByteBuffer(long bytePos, int bufSize) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(bufSize);
		fileChannel.read(bb, bytePos); // this will block if another thread is currently reading from fileChannel
		bb.limit(bb.position());
		bb.position(0);
		return bb;
	}

	private boolean canReadDatagramHeader(ByteBuffer bb) {
		return canReadAmount(bb, Datagram.NUM_HEADER_BYTES);
	}

	private boolean canReadAmount(ByteBuffer bb, int amount) {
		return bb.limit() - bb.position() >= amount;
	}

	/**
	 * Get a single datagram from a particular time location, given in the timeline's sampling rate.
	 * 
	 * @param targetTimeInSamples
	 *            the requested position, in samples. Must be non-negative and less than the total duration of the timeline.
	 * 
	 * @return the datagram starting at or overlapping the given time, or null if end-of-file was encountered
	 * @throws IOException
	 *             , BufferUnderflowException if no datagram could be created from the data at the given time.
	 */
	public Datagram getDatagram(long targetTimeInSamples) throws IOException {
		Pair<ByteBuffer, Long> p = getByteBufferAtTime(targetTimeInSamples);
		ByteBuffer bb = p.getFirst();
		return getNextDatagram(bb);
	}

	/**
	 * Get a single datagram from a particular time location.
	 * 
	 * @param targetTimeInSamples
	 *            the requested position, in samples. Must be non-negative and less than the total duration of the timeline.
	 * @param reqSampleRate
	 *            the sample rate for the requested times.
	 * 
	 * @return the datagram starting at or overlapping the given time, or null if end-of-file was encountered
	 * @throws IOException
	 *             if no datagram could be created from the data at the given time.
	 */
	public Datagram getDatagram(long targetTimeInSamples, int reqSampleRate) throws IOException {
		/*
		 * Resample the requested time location, in case the sample times are different between the request and the timeline
		 */
		long scaledTargetTime = scaleTime(reqSampleRate, targetTimeInSamples);
		Datagram dat = getDatagram(scaledTargetTime);
		if (dat == null)
			return null;
		if (reqSampleRate != sampleRate)
			dat.setDuration(unScaleTime(reqSampleRate, dat.getDuration())); // => Don't forget to stay time-consistent!
		return dat;
	}

	/**
	 * Get the datagrams spanning a particular time range from a particular time location, and return the time offset between the
	 * time request and the actual location of the first returned datagram. Irrespective of the values of nDatagrams and
	 * timeSpanInSamples, at least one datagram is always returned.
	 * 
	 * @param targetTimeInSamples
	 *            the requested position, in samples. Must be non-negative and less than the total duration of the timeline.
	 * @param nDatagrams
	 *            the number of datagrams to read. Ignored if timeSpanInSamples is positive.
	 * @param timeSpanInSamples
	 *            the requested time span, in samples. If positive, then datagrams are selected by the given time span.
	 * @param reqSampleRate
	 *            the sample rate for the requested and returned times. Must be positive.
	 * @param returnOffset
	 *            an optional output field. If it is not null, then after the call it must have length of at least 1, and the
	 *            first array field will contain the time difference, in samples, between the time request and the actual
	 *            beginning of the first datagram.
	 * 
	 * @return an array of datagrams containing at least one datagram. If less than the requested amount of datagrams can be read,
	 *         the number of datagrams that can be read is returned.
	 * @throws IllegalArgumentException
	 *             if targetTimeInSamples is negative, or if a returnOffset of length 0 is given.
	 * @throws IOException
	 *             if no data can be read at the given target time
	 */
	private Datagram[] getDatagrams(long targetTimeInSamples, int nDatagrams, long timeSpanInSamples, int reqSampleRate,
			long[] returnOffset) throws IllegalArgumentException, IOException {
		/* Check the input arguments */
		if (targetTimeInSamples < 0) {
			throw new IllegalArgumentException("Can't get a datagram from a negative time position (given time position was ["
					+ targetTimeInSamples + "]).");
		}
		if (reqSampleRate <= 0) {
			throw new IllegalArgumentException("sample rate must be positive, but is " + reqSampleRate);
		}
		// Get the datagrams by number or by time span?
		boolean byNumber;
		if (timeSpanInSamples > 0) {
			byNumber = false;
		} else {
			byNumber = true;
			if (nDatagrams <= 0) {
				nDatagrams = 1; // return at least one datagram
			}
		}

		/*
		 * Resample the requested time location, in case the sample times are different between the request and the timeline
		 */
		long scaledTargetTime = scaleTime(reqSampleRate, targetTimeInSamples);

		Pair<ByteBuffer, Long> p = getByteBufferAtTime(scaledTargetTime);
		ByteBuffer bb = p.getFirst();
		long time = p.getSecond();
		if (returnOffset != null) { // return offset between target and actual start time
			if (returnOffset.length == 0) {
				throw new IllegalArgumentException("If returnOffset is given, it must have length of at least 1");
			}
			returnOffset[0] = unScaleTime(reqSampleRate, (scaledTargetTime - time));
		}

		ArrayList<Datagram> datagrams = new ArrayList<Datagram>(byNumber ? nDatagrams : 10);
		// endTime is stop criterion if reading by time scale:
		long endTime = byNumber ? -1 : scaleTime(reqSampleRate, (targetTimeInSamples + timeSpanInSamples));
		int nRead = 0;
		boolean haveReadAll = false;
		while (!haveReadAll) {
			Datagram dat = getNextDatagram(bb);
			if (dat == null) {
				// we may have reached the end of the current byte buffer... try reading another:
				try {
					p = getByteBufferAtTime(time);
				} catch (Exception ioe) {
					// cannot get another byte buffer -- stop reading.
					break;
				}
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
		return (Datagram[]) datagrams.toArray(new Datagram[0]);
	}

	// ///////////////////// Convenience methods: variants of getDatagrams() ///////////////////////

	// ///////////////////// by time span ////////////////////////////

	/**
	 * Get the datagrams spanning a particular time range from a particular time location, and return the time offset between the
	 * time request and the actual location of the first returned datagram. Irrespective of the value of timeSpanInSamples, at
	 * least one datagram is always returned.
	 * 
	 * @param targetTimeInSamples
	 *            the requested position, in samples. Must be non-negative and less than the total duration of the timeline.
	 * @param timeSpanInSamples
	 *            the requested time span, in samples. If positive, then datagrams are selected by the given time span.
	 * @param reqSampleRate
	 *            the sample rate for the requested and returned times. Must be positive.
	 * @param returnOffset
	 *            an optional output field. If it is not null, then after the call it must have length of at least 1, and the
	 *            first array field will contain the time difference, in samples, between the time request and the actual
	 *            beginning of the first datagram.
	 * 
	 * @return an array of datagrams containing at least one datagram. If less than the requested amount of datagrams can be read,
	 *         the number of datagrams that can be read is returned.
	 * @throws IllegalArgumentException
	 *             if targetTimeInSamples is negative, or if a returnOffset of length 0 is given.
	 * @throws IOException
	 *             , BufferUnderflowException if no data can be read at the given target time
	 */
	public Datagram[] getDatagrams(long targetTimeInSamples, long timeSpanInSamples, int reqSampleRate, long[] returnOffset)
			throws IOException {
		return getDatagrams(targetTimeInSamples, -1, timeSpanInSamples, reqSampleRate, returnOffset);
	}

	/**
	 * Get the datagrams spanning a particular time range from a particular time location. Irrespective of the value of
	 * timeSpanInSamples, at least one datagram is always returned.
	 * 
	 * @param targetTimeInSamples
	 *            the requested position, in samples. Must be non-negative and less than the total duration of the timeline.
	 * @param timeSpanInSamples
	 *            the requested time span, in samples. If positive, then datagrams are selected by the given time span.
	 * @param reqSampleRate
	 *            the sample rate for the requested and returned times. Must be positive.
	 * 
	 * @return an array of datagrams containing at least one datagram. If less than the requested amount of datagrams can be read,
	 *         the number of datagrams that can be read is returned.
	 * @throws IllegalArgumentException
	 *             if targetTimeInSamples is negative, or if a returnOffset of length 0 is given.
	 * @throws IOException
	 *             if no data can be read at the given target time
	 */
	public Datagram[] getDatagrams(long targetTimeInSamples, long timeSpanInSamples, int reqSampleRate) throws IOException {
		return getDatagrams(targetTimeInSamples, timeSpanInSamples, reqSampleRate, null);
	}

	/**
	 * Get a given number of datagrams from a particular time location.
	 * 
	 * @param targetTimeInSamples
	 *            the requested position, in samples. Must be non-negative and less than the total duration of the timeline.
	 * @param timeSpanInSamples
	 *            the span in samples
	 * @return an array of datagrams containing at least one datagram. If less than the requested amount of datagrams can be read,
	 *         the number of datagrams that can be read is returned.
	 * @throws IllegalArgumentException
	 *             if targetTimeInSamples is negative, or if a returnOffset of length 0 is given.
	 * @throws IOException
	 *             if no data can be read at the given target time
	 */
	public Datagram[] getDatagrams(long targetTimeInSamples, long timeSpanInSamples) throws IOException {
		return getDatagrams(targetTimeInSamples, timeSpanInSamples, sampleRate, null);
	}

	// ///////////////////// by number of datagrams ////////////////////////////

	/**
	 * Get a given number of datagrams from a particular time location, and return the time offset between the time request and
	 * the actual location of the first returned datagram.
	 * 
	 * @param targetTimeInSamples
	 *            the requested position, in samples. Must be non-negative and less than the total duration of the timeline.
	 * @param number
	 *            the number of datagrams to read. Even if this is &le; 0, at least one datagram is always returned.
	 * @param reqSampleRate
	 *            the sample rate for the requested and returned times. Must be positive.
	 * @param returnOffset
	 *            an optional output field. If it is not null, then after the call it must have length of at least 1, and the
	 *            first array field will contain the time difference, in samples, between the time request and the actual
	 *            beginning of the first datagram.
	 * 
	 * @return an array of datagrams containing at least one datagram. If less than the requested amount of datagrams can be read,
	 *         the number of datagrams that can be read is returned.
	 * @throws IllegalArgumentException
	 *             if targetTimeInSamples is negative, or if a returnOffset of length 0 is given.
	 * @throws IOException
	 *             if no data can be read at the given target time
	 */
	public Datagram[] getDatagrams(long targetTimeInSamples, int number, int reqSampleRate, long[] returnOffset)
			throws IOException {
		return getDatagrams(targetTimeInSamples, number, -1, reqSampleRate, returnOffset);
	}

	// ///////////////////// by unit ////////////////////////////

	/**
	 * Get the datagrams spanning a particular unit, and return the time offset between the unit request and the actual location
	 * of the first returned datagram. Irrespective of the unit duration, at least one datagram is always returned.
	 * 
	 * @param unit
	 *            The requested speech unit, containing its own position and duration.
	 * @param reqSampleRate
	 *            the sample rate for the requested and returned times. Must be positive.
	 * @param returnOffset
	 *            an optional output field. If it is not null, then after the call it must have length of at least 1, and the
	 *            first array field will contain the time difference, in samples, between the time request and the actual
	 *            beginning of the first datagram.
	 * 
	 * @return an array of datagrams containing at least one datagram. If less than the requested amount of datagrams can be read,
	 *         the number of datagrams that can be read is returned.
	 * @throws IllegalArgumentException
	 *             if targetTimeInSamples is negative, or if a returnOffset of length 0 is given.
	 * @throws IOException
	 *             if no data can be read at the given target time
	 */
	public Datagram[] getDatagrams(Unit unit, int reqSampleRate, long[] returnOffset) throws IOException {
		return getDatagrams(unit.startTime, (long) (unit.duration), reqSampleRate, returnOffset);
	}

	/**
	 * Get the datagrams spanning a particular unit. Irrespective of the unit duration, at least one datagram is always returned.
	 * 
	 * @param unit
	 *            The requested speech unit, containing its own position and duration.
	 * @param reqSampleRate
	 *            the sample rate for the requested and returned times. Must be positive.
	 * 
	 * @return an array of datagrams containing at least one datagram. If less than the requested amount of datagrams can be read,
	 *         the number of datagrams that can be read is returned.
	 * @throws IllegalArgumentException
	 *             if targetTimeInSamples is negative, or if a returnOffset of length 0 is given.
	 * @throws IOException
	 *             if no data can be read at the given target time
	 */
	public Datagram[] getDatagrams(Unit unit, int reqSampleRate) throws IOException {
		return getDatagrams(unit, reqSampleRate, null);
	}

	/*****************************************/
	/* HELPER CLASSES */
	/*****************************************/

	/**
	 * Simple helper class to read the index part of a timeline file. The index points to datagrams at or before a certain point
	 * in time.
	 * 
	 * Note: If no datagram starts at the exact index time, it makes sense to point to the previous datagram rather than the
	 * following one.
	 * 
	 * If one would store the location of the datagram which comes just after the index position (the currently tested datagram),
	 * there would be a possibility that a particular time request falls between the index and the datagram:
	 * 
	 * time axis &rArr; INDEX &larr; REQUEST | &rArr; DATAGRAM
	 * 
	 * This would require a subsequent backwards time hopping, which is impossible because the datagrams are a singly linked list.
	 * 
	 * By registering the location of the previous datagram, any time request will find an index which points to a datagram
	 * falling BEFORE or ON the index location:
	 * 
	 * time axis &rArr; INDEX &larr; REQUEST | DATAGRAM &larr;
	 * 
	 * Thus, forward hopping is always possible and the requested time can always be reached.
	 * 
	 * @author sacha
	 */
	public static class Index {
		private int idxInterval = 0; // The fixed time interval (in samples) separating two index fields.

		/**
		 * For index field i, bytePtrs[i] is the position in bytes, from the beginning of the file, of the datagram coming on or
		 * just before that index field.
		 */
		private long[] bytePtrs;

		/**
		 * For index field i, timePtrs[i] is the time position in samples of the datagram coming on or just before that index
		 * field.
		 */
		private long[] timePtrs;

		/****************/
		/* CONSTRUCTORS */
		/****************/

		/**
		 * Construct an index from a data input stream or random access file. Fundamental guarantee: Once created, the index is
		 * guaranteed to contain a positive index interval and monotonously rising byte and time pointers.
		 * 
		 * @param bb
		 *            byte buffer from which to read the index. Must not be null, and read position must be at start of index.
		 * @throws IOException
		 *             if there is a problem reading.
		 * @throws MaryConfigurationException
		 *             if the index is not well-formed.
		 */
		private Index(DataInput raf) throws IOException, MaryConfigurationException {
			assert raf != null : "null argument";
			load(raf);
		}

		/**
		 * Construct an index from a byte buffer. Fundamental guarantee: Once created, the index is guaranteed to contain a
		 * positive index interval and monotonously rising byte and time pointers.
		 * 
		 * @param rafIn
		 *            data input from which to read the index. Must not be null, and read position must be at start of index.
		 * @throws BufferUnderflowException
		 *             if there is a problem reading.
		 * @throws MaryConfigurationException
		 *             if the index is not well-formed.
		 */
		private Index(ByteBuffer bb) throws BufferUnderflowException, MaryConfigurationException {
			assert bb != null : "null argument";
			load(bb);
		}

		/**
		 * Constructor which builds a new index with a specific index interval and a given sample rate. Fundamental guarantee:
		 * Once created, the index is guaranteed to contain a positive index interval and monotonously rising byte and time
		 * pointers.
		 * 
		 * @param idxInterval
		 *            the index interval, in samples. Must be a positive number.
		 * @param indexFields
		 *            the actual index data. Must not be null.
		 * @throws IllegalArgumentException
		 *             if the index data given is not well-formed.
		 * @throws NullPointerException
		 *             if indexFields are null.
		 */
		public Index(int idxInterval, Vector<IdxField> indexFields) throws IllegalArgumentException, NullPointerException {
			if (idxInterval <= 0) {
				throw new IllegalArgumentException("got index interval <= 0");
			}
			if (indexFields == null) {
				throw new NullPointerException("null argument");
			}
			this.idxInterval = idxInterval;
			bytePtrs = new long[indexFields.size()];
			timePtrs = new long[indexFields.size()];
			for (int i = 0; i < bytePtrs.length; i++) {
				IdxField f = indexFields.get(i);
				bytePtrs[i] = f.bytePtr;
				timePtrs[i] = f.timePtr;
				if (i > 0) {
					if (bytePtrs[i] < bytePtrs[i - 1] || timePtrs[i] < timePtrs[i - 1]) {
						throw new IllegalArgumentException(
								"Pointer positions in index fields must be strictly monotonously rising");
					}
				}
			}
		}

		/*****************/
		/* I/O METHODS */
		/*****************/

		/**
		 * Method which loads an index from a data input (random access file or data input stream).
		 * 
		 * @param rafIn
		 *            data input from which to read the index. Must not be null, and read position must be at start of index.
		 * @throws IOException
		 *             if there is a problem reading.
		 * @throws MaryConfigurationException
		 *             if the index is not well-formed.
		 */
		public void load(DataInput rafIn) throws IOException, MaryConfigurationException {
			int numIdx = rafIn.readInt();
			idxInterval = rafIn.readInt();
			if (idxInterval <= 0) {
				throw new MaryConfigurationException("read negative index interval -- file seems corrupt");
			}

			bytePtrs = new long[numIdx];
			timePtrs = new long[numIdx];
			int numBytesToRead = 16 * numIdx + 16; // 2*8 bytes for each index field + 16 for prevBytePos and prevTimePos

			byte[] data = new byte[numBytesToRead];
			rafIn.readFully(data);
			DataInput bufIn = new DataInputStream(new ByteArrayInputStream(data));

			for (int i = 0; i < numIdx; i++) {
				bytePtrs[i] = bufIn.readLong();
				timePtrs[i] = bufIn.readLong();
				if (i > 0) {
					if (bytePtrs[i] < bytePtrs[i - 1] || timePtrs[i] < timePtrs[i - 1]) {
						throw new MaryConfigurationException(
								"File seems corrupt: Pointer positions in index fields are not strictly monotonously rising");
					}
				}
			}
			/* Obsolete: Read the "last datagram" memory */
			/* prevBytePos = */bufIn.readLong();
			/* prevTimePos = */bufIn.readLong();
		}

		/**
		 * Method which loads an index from a byte buffer.
		 * 
		 * @param bb
		 *            byte buffer from which to read the index. Must not be null, and read position must be at start of index.
		 * @throws BufferUnderflowException
		 *             if there is a problem reading.
		 * @throws MaryConfigurationException
		 *             if the index is not well-formed.
		 */
		private void load(ByteBuffer bb) throws BufferUnderflowException, MaryConfigurationException {
			int numIdx = bb.getInt();
			idxInterval = bb.getInt();
			if (idxInterval <= 0) {
				throw new MaryConfigurationException("read negative index interval -- file seems corrupt");
			}

			bytePtrs = new long[numIdx];
			timePtrs = new long[numIdx];

			for (int i = 0; i < numIdx; i++) {
				bytePtrs[i] = bb.getLong();
				timePtrs[i] = bb.getLong();
				if (i > 0) {
					if (bytePtrs[i] < bytePtrs[i - 1] || timePtrs[i] < timePtrs[i - 1]) {
						throw new MaryConfigurationException(
								"File seems corrupt: Pointer positions in index fields are not strictly monotonously rising");
					}
				}
			}
			/* Obsolete: Read the "last datagram" memory */
			/* prevBytePos = */bb.getLong();
			/* prevTimePos = */bb.getLong();
		}

		/**
		 * Method which writes an index to a RandomAccessFile
		 * 
		 * @param rafIn
		 *            rafIn
		 * @throws IOException
		 *             IOException
		 * @return nBytes
		 * */
		public long dump(RandomAccessFile rafIn) throws IOException {
			long nBytes = 0;
			int numIdx = getNumIdx();
			rafIn.writeInt(numIdx);
			nBytes += 4;
			rafIn.writeInt(idxInterval);
			nBytes += 4;
			for (int i = 0; i < numIdx; i++) {
				rafIn.writeLong(bytePtrs[i]);
				nBytes += 8;
				rafIn.writeLong(timePtrs[i]);
				nBytes += 8;
			}
			// Obsolete, keep only for file format compatibility:
			// Register the "last datagram" memory as an additional field
			// rafIn.writeLong(prevBytePos);
			// rafIn.writeLong(prevTimePos);
			rafIn.writeLong(0l);
			rafIn.writeLong(0l);
			nBytes += 16l;

			return nBytes;
		}

		/**
		 * Method which writes an index to stdout
		 * */
		public void print() {
			System.out.println("<INDEX>");
			int numIdx = getNumIdx();
			System.out.println("interval = " + idxInterval);
			System.out.println("numIdx = " + numIdx);
			for (int i = 0; i < numIdx; i++) {
				System.out.println("( " + bytePtrs[i] + " , " + timePtrs[i] + " )");
			}
			/* Obsolete: Register the "last datagram" memory as an additional field */
			// System.out.println( "Last datagram: "
			// + "( " + prevBytePos + " , " + prevTimePos + " )" );
			System.out.println("</INDEX>");
		}

		/*****************/
		/* ACCESSORS */
		/*****************/
		/**
		 * The number of index entries.
		 * 
		 * @return bytePtrs.length
		 */
		public int getNumIdx() {
			return bytePtrs.length;
		}

		/**
		 * The interval, in samples, between two index entries.
		 * 
		 * @return idxInterval
		 */
		public int getIdxInterval() {
			return idxInterval;
		}

		public IdxField getIdxField(int i) {
			if (i < 0) {
				throw new IndexOutOfBoundsException("Negative index.");
			}
			if (i >= bytePtrs.length) {
				throw new IndexOutOfBoundsException("Requested index no. " + i + ", but highest is " + bytePtrs.length);
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
		 *            the non-negative time
		 * @return an index field representing the index position just before or straight on the requested time.
		 * @throws IllegalArgumentException
		 *             if the given timePosition is negtive
		 */
		public IdxField getIdxFieldBefore(long timePosition) {
			if (timePosition < 0) {
				throw new IllegalArgumentException("Negative time given");
			}
			int index = (int) (timePosition / idxInterval); /*
															 * <= This is an integer division between two longs, implying a
															 * flooring operation on the decimal result.
															 */
			// System.out.println( "TIMEPOS=" + timePosition + " IDXINT=" + idxInterval + " IDX=" + idx );
			// System.out.flush();
			if (index < 0) {
				throw new RuntimeException("Negative index field: [" + index + "] encountered when getting index before time=["
						+ timePosition + "] (idxInterval=[" + idxInterval + "]).");
			}
			if (index >= bytePtrs.length) {
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
	public static class IdxField {
		// TODO: rethink if these should be public fields or if we should add accessors.
		public long bytePtr = 0;
		public long timePtr = 0;

		public IdxField() {
			bytePtr = 0;
			timePtr = 0;
		}

		public IdxField(long setBytePtr, long setTimePtr) {
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
	public static class ProcHeader {

		private String procHeader = null;

		/****************/
		/* CONSTRUCTORS */
		/****************/

		/**
		 * Constructor which loads the procHeader from a RandomAccessFile. Fundamental guarantee: after creation, the ProcHeader
		 * object has a non-null (but possibly empty) string content.
		 * 
		 * @param raf
		 *            input from which to load the processing header. Must not be null and must be positioned so that a processing
		 *            header can be read from it.
		 *
		 * @throws IOException
		 *             if no proc header can be read at the current position.
		 */
		private ProcHeader(RandomAccessFile raf) throws IOException {
			loadProcHeader(raf);
		}

		/**
		 * Constructor which loads the procHeader from a RandomAccessFile Fundamental guarantee: after creation, the ProcHeader
		 * object has a non-null (but possibly empty) string content.
		 * 
		 * @param raf
		 *            input from which to load the processing header. Must not be null and must be positioned so that a processing
		 *            header can be read from it.
		 *
		 * @throws BufferUnderflowException
		 *             , UTFDataFormatException if no proc header can be read at the current position.
		 */
		private ProcHeader(ByteBuffer bb) throws BufferUnderflowException, UTFDataFormatException {
			loadProcHeader(bb);
		}

		/**
		 * Constructor which makes the procHeader from a String. Fundamental guarantee: after creation, the ProcHeader object has
		 * a non-null (but possibly empty) string content.
		 * 
		 * @param procStr
		 *            a non-null string representing the contents of the ProcHeader.
		 * @throws NullPointerException
		 *             if procStr is null
		 * */
		public ProcHeader(String procStr) {
			if (procStr == null) {
				throw new NullPointerException("null argument");
			}
			procHeader = procStr;
		}

		/****************/
		/* ACCESSORS */
		/****************/

		/**
		 * Return the string length of the proc header.
		 * 
		 * @return a non-negative int representling the string length of the proc header.
		 */
		public int getCharSize() {
			assert procHeader != null;
			return procHeader.length();
		}

		/**
		 * Get the string content of the proc header.
		 * 
		 * @return a non-null string representing the string content of the proc header.
		 */
		public String getString() {
			assert procHeader != null;
			return procHeader;
		}

		/*****************/
		/* I/O METHODS */
		/*****************/

		/**
		 * Method which loads the header from a RandomAccessFile.
		 * 
		 * @param rafIn
		 *            file to read from, must not be null.
		 * @throws IOException
		 *             if no proc header can be read at the current position.
		 */
		private void loadProcHeader(RandomAccessFile rafIn) throws IOException {
			assert rafIn != null : "null argument";
			procHeader = rafIn.readUTF();
			assert procHeader != null;
		}

		/**
		 * Method which loads the header from a byte buffer.
		 * 
		 * @param bb
		 *            byte buffer to read from, must not be null.
		 * @throws BufferUnderflowException
		 *             , UTFDataFormatException if no proc header can be read at the current position.
		 * @throws UTFDataFormatException
		 *             UTFDataFormatException
		 */
		private void loadProcHeader(ByteBuffer bb) throws BufferUnderflowException, UTFDataFormatException {
			procHeader = StreamUtils.readUTF(bb);
			assert procHeader != null;
		}

		/**
		 * Method which writes the proc header to a RandomAccessFile.
		 * 
		 * @param rafIn
		 *            rafIn
		 * @throws IOException
		 *             IOException
		 * @return the number of written bytes.
		 * */
		public long dump(RandomAccessFile rafIn) throws IOException {
			long before = rafIn.getFilePointer();
			rafIn.writeUTF(procHeader);
			long after = rafIn.getFilePointer();
			return after - before;
		}
	}

}