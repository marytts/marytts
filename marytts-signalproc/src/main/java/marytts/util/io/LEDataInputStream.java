/**
 * Copyright 2000-2009 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.util.io;

/**
 * LEDataInputStream.java
 * copyright (c) 1998-2007 Roedy Green, Canadian Mind* Products
 * Very similar to DataInputStream except it reads
 * little-endian instead of big-endian binary data. We can't extend
 * DataInputStream directly since it has only final methods, though
 * DataInputStream itself is not final. This forces us implement
 * LEDataInputStream with a DataInputStream object, and use wrapper methods.
 */

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * reads little endian binary data .
 */
public class LEDataInputStream implements DataInput {

	// ------------------------------ FIELDS ------------------------------

	/**
	 * undisplayed copyright notice.
	 * 
	 * @noinspection UnusedDeclaration
	 */
	private static final String EMBEDDEDCOPYRIGHT = "copyright (c) 1999-2007 Roedy Green, Canadian Mind Products, http://mindprod.com";

	/**
	 * to get at the big-Endian methods of a basic DataInputStream
	 * 
	 * 
	 */
	protected final DataInputStream dis;

	/**
	 * to get at the a basic readBytes method.
	 * 
	 * 
	 */
	protected final InputStream is;

	/**
	 * work array for buffering input.
	 * 
	 * 
	 */
	protected final byte[] work;

	// -------------------------- PUBLIC STATIC METHODS --------------------------

	/**
	 * Note. This is a STATIC method!
	 * 
	 * @param in
	 *            stream to read UTF chars from (endian irrelevant)
	 * 
	 * @return string from stream
	 * 
	 * @throws IOException
	 *             if read fails.
	 */
	public static String readUTF(DataInput in) throws IOException {
		return DataInputStream.readUTF(in);
	}

	// -------------------------- PUBLIC INSTANCE METHODS --------------------------
	/**
	 * constructor.
	 * 
	 * @param in
	 *            binary inputstream of little-endian data.
	 */
	public LEDataInputStream(InputStream in) {
		this.is = in;
		this.dis = new DataInputStream(in);
		work = new byte[8];
	}

	public LEDataInputStream(String filename) throws FileNotFoundException {
		this(new FileInputStream(filename));
	}

	/**
	 * close.
	 * 
	 * @throws IOException
	 *             if close fails.
	 */
	public final void close() throws IOException {
		dis.close();
	}

	/**
	 * Read bytes. Watch out, read may return fewer bytes than requested.
	 * 
	 * @param ba
	 *            where the bytes go.
	 * @param off
	 *            offset in buffer, not offset in file.
	 * @param len
	 *            count of bytes to read.
	 * 
	 * @return how many bytes read.
	 * 
	 * @throws IOException
	 *             if read fails.
	 */
	public final int read(byte ba[], int off, int len) throws IOException {
		// For efficiency, we avoid one layer of wrapper
		return is.read(ba, off, len);
	}

	/**
	 * read only a one-byte boolean.
	 * 
	 * @return true or false.
	 * 
	 * @throws IOException
	 *             if read fails.
	 * @see java.io.DataInput#readBoolean()
	 */
	public final boolean readBoolean() throws IOException {
		return dis.readBoolean();
	}

	public final boolean[] readBoolean(int len) throws IOException {
		boolean[] ret = new boolean[len];

		for (int i = 0; i < len; i++)
			ret[i] = readBoolean();

		return ret;
	}

	/**
	 * read byte.
	 * 
	 * @return the byte read.
	 * 
	 * @throws IOException
	 *             if read fails.
	 * @see java.io.DataInput#readByte()
	 */
	public final byte readByte() throws IOException {
		return dis.readByte();
	}

	public final byte[] readByte(int len) throws IOException {
		byte[] ret = new byte[len];

		for (int i = 0; i < len; i++)
			ret[i] = readByte();

		return ret;
	}

	/**
	 * Read on char. like DataInputStream.readChar except little endian.
	 * 
	 * @return little endian 16-bit unicode char from the stream.
	 * 
	 * @throws IOException
	 *             if read fails.
	 */
	public final char readChar() throws IOException {
		dis.readFully(work, 0, 2);
		return (char) ((work[1] & 0xff) << 8 | (work[0] & 0xff));
	}

	public final char[] readChar(int len) throws IOException {
		char[] ret = new char[len];

		for (int i = 0; i < len; i++)
			ret[i] = readChar();

		return ret;
	}

	/**
	 * Read a double. like DataInputStream.readDouble except little endian.
	 * 
	 * @return little endian IEEE double from the datastream.
	 * @throws IOException
	 *             IOException
	 */
	public final double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}

	public final double[] readDouble(int len) throws IOException {
		double[] ret = new double[len];

		for (int i = 0; i < len; i++)
			ret[i] = readDouble();

		return ret;
	}

	public final int[] readDoubleToInt(int len) throws IOException {
		int[] ret = new int[len];

		for (int i = 0; i < len; i++)
			ret[i] = (int) readDouble();

		return ret;
	}

	/**
	 * Read one float. Like DataInputStream.readFloat except little endian.
	 * 
	 * @return little endian IEEE float from the datastream.
	 * 
	 * @throws IOException
	 *             if read fails.
	 */
	public final float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	public final float[] readFloat(int len) throws IOException {
		float[] ret = new float[len];

		for (int i = 0; i < len; i++)
			ret[i] = readFloat();

		return ret;
	}

	/**
	 * Read bytes until the array is filled.
	 * 
	 * @see java.io.DataInput#readFully(byte[])
	 */
	public final void readFully(byte ba[]) throws IOException {
		dis.readFully(ba, 0, ba.length);
	}

	/**
	 * Read bytes until the count is satisfied.
	 * 
	 * @throws IOException
	 *             if read fails.
	 * @see java.io.DataInput#readFully(byte[],int,int)
	 */
	public final void readFully(byte ba[], int off, int len) throws IOException {
		dis.readFully(ba, off, len);
	}

	/**
	 * Read an int, 32-bits. Like DataInputStream.readInt except little endian.
	 * 
	 * @return little-endian binary int from the datastream
	 * 
	 * @throws IOException
	 *             if read fails.
	 */
	public final int readInt() throws IOException {
		dis.readFully(work, 0, 4);
		return (work[3]) << 24 | (work[2] & 0xff) << 16 | (work[1] & 0xff) << 8 | (work[0] & 0xff);
	}

	public final int[] readInt(int len) throws IOException {
		int[] ret = new int[len];

		for (int i = 0; i < len; i++)
			ret[i] = readInt();

		return ret;
	}

	/**
	 * Read a line.
	 * 
	 * @return a rough approximation of the 8-bit stream as a 16-bit unicode string
	 * @throws IOException
	 *             IOException
	 * @deprecated This method does not properly convert bytes to characters. Use a Reader instead with a little-endian encoding.
	 */
	public final String readLine() throws IOException {
		return dis.readLine();
	}

	/**
	 * read a long, 64-bits. Like DataInputStream.readLong except little endian.
	 * 
	 * @return little-endian binary long from the datastream.
	 * @throws IOException
	 *             IOException
	 */
	public final long readLong() throws IOException {
		dis.readFully(work, 0, 8);
		return (long) (work[7]) << 56 |
		/* long cast needed or shift done modulo 32 */
		(long) (work[6] & 0xff) << 48 | (long) (work[5] & 0xff) << 40 | (long) (work[4] & 0xff) << 32
				| (long) (work[3] & 0xff) << 24 | (long) (work[2] & 0xff) << 16 | (long) (work[1] & 0xff) << 8
				| (long) (work[0] & 0xff);
	}

	public final long[] readLong(int len) throws IOException {
		long[] ret = new long[len];

		for (int i = 0; i < len; i++)
			ret[i] = readLong();

		return ret;
	}

	/**
	 * Read short, 16-bits. Like DataInputStream.readShort except little endian.
	 * 
	 * @return little endian binary short from stream.
	 * 
	 * @throws IOException
	 *             if read fails.
	 */
	public final short readShort() throws IOException {
		dis.readFully(work, 0, 2);
		return (short) ((work[1] & 0xff) << 8 | (work[0] & 0xff));
	}

	public final short[] readShort(int len) throws IOException {
		short[] ret = new short[len];

		for (int i = 0; i < len; i++)
			ret[i] = readShort();

		return ret;
	}

	/**
	 * Read UTF counted string.
	 * 
	 * @return String read.
	 */
	public final String readUTF() throws IOException {
		return dis.readUTF();
	}

	/**
	 * Read an unsigned byte. Note: returns an int, even though says Byte (non-Javadoc)
	 * 
	 * @throws IOException
	 *             if read fails.
	 * @see java.io.DataInput#readUnsignedByte()
	 */
	public final int readUnsignedByte() throws IOException {
		return dis.readUnsignedByte();
	}

	public final int[] readUnsignedByte(int len) throws IOException {
		int[] ret = new int[len];

		for (int i = 0; i < len; i++)
			ret[i] = readUnsignedByte();

		return ret;
	}

	/**
	 * Read an unsigned short, 16 bits. Like DataInputStream.readUnsignedShort except little endian. Note, returns int even though
	 * it reads a short.
	 * 
	 * @return little-endian int from the stream.
	 * 
	 * @throws IOException
	 *             if read fails.
	 */
	public final int readUnsignedShort() throws IOException {
		dis.readFully(work, 0, 2);
		return ((work[1] & 0xff) << 8 | (work[0] & 0xff));
	}

	public final int[] readUnsignedShort(int len) throws IOException {
		int[] ret = new int[len];

		for (int i = 0; i < len; i++)
			ret[i] = readUnsignedShort();

		return ret;
	}

	/**
	 * <p>
	 * Skip over bytes in the stream. See the general contract of the <code>skipBytes</code> method of <code>DataInput</code>.
	 * </p>
	 * Bytes for this operation are read from the contained input stream.
	 * 
	 * @param n
	 *            the number of bytes to be skipped.
	 * 
	 * @return the actual number of bytes skipped.
	 * 
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public final int skipBytes(int n) throws IOException {
		return dis.skipBytes(n);
	}
}// end class LEDataInputStream

