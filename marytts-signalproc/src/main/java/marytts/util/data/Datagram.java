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

import java.io.DataOutput;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class Datagram {

	public static final int NUM_HEADER_BYTES = (Long.SIZE + Integer.SIZE) / Byte.SIZE; // the duration and the length
	/****************/
	/* DATA FIELDS */
	/****************/

	/**
	 * The datagram duration, in samples.
	 */
	protected long duration = 0l; // The (time) duration of the datagram, in samples
	/**
	 * The datagram's contents, as a generic byte array.
	 */
	protected byte[] data = null;

	/****************/
	/* CONSTRUCTORS */
	/****************/
	/**
	 * Constructor for subclasses which want to represent data in a different format.
	 * 
	 * @param duration
	 *            the datagram duration, in samples. Must be non-negative.
	 * @throws IllegalArgumentException
	 *             if duration is negative
	 */
	protected Datagram(long duration) {
		if (duration < 0) {
			throw new IllegalArgumentException("Can't create a datagram with the negative duration [" + duration + "].");
		}
		this.duration = duration;
	}

	/**
	 * Constructor from external data.
	 * 
	 * @param setDuration
	 *            the datagram duration, in samples. Must be non-negative.
	 * @param setData
	 *            the datagram.
	 * @throws IllegalArgumentException
	 *             if duration is negative
	 * @throws NullPointerException
	 *             if setData is null.
	 */
	public Datagram(long setDuration, byte[] setData) {
		if (setDuration < 0) {
			throw new IllegalArgumentException("Can't create a datagram with the negative duration [" + setDuration + "].");
		}
		if (setData == null) {
			throw new NullPointerException("null argument");
		}
		duration = setDuration;
		data = setData;
	}

	/**
	 * Constructor which reads a datagram from a random access file.
	 * 
	 * @param raf
	 *            the random access file to read the datagram from.
	 * 
	 * @throws IOException
	 *             if there is a problem initialising the datagram from the file
	 */
	public Datagram(RandomAccessFile raf) throws IOException {
		duration = raf.readLong();
		if (duration < 0) {
			throw new IOException("Can't create a datagram with a negative duration [" + duration + "].");
		}
		int len = raf.readInt();
		if (len < 0) {
			throw new IOException("Can't create a datagram with a negative data size [" + len + "].");
		}
		data = new byte[len];
		raf.readFully(data);
	}

	/**
	 * Constructor which reads a datagram from a byte buffer.
	 * 
	 * @param bb
	 *            the byte buffer to read the datagram from.
	 * 
	 * @throws IOException
	 *             if the datagram has wrong format or if the datagram cannot be fully read
	 */
	public Datagram(ByteBuffer bb) throws IOException {
		this(bb, true);
	}

	/**
	 * Constructor which reads a datagram from a byte buffer.
	 * 
	 * @param bb
	 *            the byte buffer to read the datagram from.
	 * @param readData
	 *            whether to try and read the actual data
	 * 
	 * @throws IOException
	 *             if the datagram has wrong format or if the datagram cannot be fully read
	 */
	public Datagram(ByteBuffer bb, boolean readData) throws IOException {
		duration = bb.getLong();
		if (duration < 0) {
			throw new IOException("Can't create a datagram with a negative duration [" + duration + "].");
		}
		int len = bb.getInt();
		if (len < 0) {
			throw new IOException("Can't create a datagram with a negative data size [" + len + "].");
		}
		data = new byte[len];
		if (!readData) {
			return;
		}
		if (bb.limit() - bb.position() < len) {
			throw new IOException("Not enough data in byte buffer to read the full datagram: datagram length is " + len
					+ ", but can read only " + (bb.limit() - bb.position()));
		}
		bb.get(data);
	}

	/****************/
	/* SETTERS */
	/****************/

	/**
	 * Set the new duration.
	 * 
	 * @param setDuration
	 *            the datagram duration, in samples. Must be non-negative.
	 * @throws IllegalArgumentException
	 *             if duration is negative
	 */
	public void setDuration(long setDuration) {
		if (setDuration < 0) {
			throw new IllegalArgumentException("Can't create a datagram with the negative duration [" + setDuration + "].");
		}
		this.duration = setDuration;
	}

	/****************/
	/* I/O METHODS */
	/****************/

	/**
	 * Write this datagram to a random access file or data output stream. Must only be called if data is not null.
	 * 
	 * @param raf
	 *            the data output to write to.
	 * @throws IllegalStateException
	 *             if called when data is null.
	 * @throws IOException
	 *             if a write error occurs.
	 */
	public void write(DataOutput raf) throws IOException {
		assert duration >= 0;
		if (data == null) {
			throw new IllegalStateException("This method can only be called for data that is not null");
		}
		raf.writeLong(duration);
		raf.writeInt(data.length);
		raf.write(data);
	}

	/****************/
	/* ACCESSORS */
	/****************/

	/**
	 * Get the datagram duration, in samples. Note: the sample rate has to be provided externally.
	 * 
	 * @return the non-negative duration.
	 */
	public long getDuration() {
		assert duration >= 0;
		return duration;
	}

	/**
	 * Get the length, in bytes, of the datagram's data field. Must only be called if data is not null.
	 * 
	 * @return a non-negative integer representing the number of bytes in the data field.
	 * @throws IllegalStateException
	 *             if called when data is null.
	 */
	public int getLength() {
		if (data == null) {
			throw new IllegalStateException("This method must not be called if data is null");
		}
		return data.length;
	}

	/**
	 * Get the datagram's data field.
	 * 
	 * @return the data in this Datagram, or null if there is no such data (should be the case only for subclasses).
	 */
	public byte[] getData() {
		return data;
	}

	/****************/
	/* MISC METHODS */
	/****************/

	/**
	 * Tests if this datagram is equal to another datagram.
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Datagram)) {
			return false;
		}
		Datagram other = (Datagram) obj;
		if (this.duration != other.duration)
			return (false);
		if (this.data.length != other.data.length)
			return (false);
		for (int i = 0; i < this.data.length; i++) {
			if (this.data[i] != other.data[i])
				return (false);
		}
		return (true);
	}

}
