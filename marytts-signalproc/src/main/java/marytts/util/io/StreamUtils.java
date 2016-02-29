/**
 * Copyright 2009 DFKI GmbH.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * @author marc
 * 
 */
public class StreamUtils {

	public static double[] readDoubleArray(DataInput stream, int len) throws IOException {
		byte[] raw = new byte[len * Double.SIZE / 8];
		stream.readFully(raw);
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw));
		double[] data = new double[len];
		for (int i = 0; i < len; i++) {
			data[i] = in.readDouble();
		}
		return data;
	}

	public static void writeDoubleArray(DataOutput stream, double[] data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(baos);
		for (int i = 0; i < data.length; i++) {
			out.writeDouble(data[i]);
		}
		out.close();
		byte[] raw = baos.toByteArray();
		assert raw.length == data.length * Double.SIZE / 8;
		stream.write(raw);
	}

	/**
	 * Reads from the bytebuffer <code>bb</code> a representation of a Unicode character string encoded in <a
	 * href="DataInput.html#modified-utf-8">modified UTF-8</a> format; this string of characters is then returned as a
	 * <code>String</code>. The details of the modified UTF-8 representation are exactly the same as for the <code>readUTF</code>
	 * method of <code>DataInput</code>.
	 * 
	 * @param bb
	 *            a byte buffer.
	 * @return a Unicode string.
	 * @exception BufferUnderflowException
	 *                if the input stream reaches the end before all the bytes.
	 * @exception UTFDataFormatException
	 *                if the bytes do not represent a valid modified UTF-8 encoding of a Unicode string.
	 * @see java.io.DataInputStream#readUnsignedShort()
	 */
	public static String readUTF(ByteBuffer bb) throws BufferUnderflowException, UTFDataFormatException {
		int utflen = readUnsignedShort(bb);
		byte[] bytearr = new byte[utflen];
		char[] chararr = new char[utflen];

		int c, char2, char3;
		int count = 0;
		int chararr_count = 0;

		bb.get(bytearr);

		while (count < utflen) {
			c = (int) bytearr[count] & 0xff;
			if (c > 127)
				break;
			count++;
			chararr[chararr_count++] = (char) c;
		}

		while (count < utflen) {
			c = (int) bytearr[count] & 0xff;
			switch (c >> 4) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				/* 0xxxxxxx */
				count++;
				chararr[chararr_count++] = (char) c;
				break;
			case 12:
			case 13:
				/* 110x xxxx 10xx xxxx */
				count += 2;
				if (count > utflen)
					throw new UTFDataFormatException("malformed input: partial character at end");
				char2 = (int) bytearr[count - 1];
				if ((char2 & 0xC0) != 0x80)
					throw new UTFDataFormatException("malformed input around byte " + count);
				chararr[chararr_count++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
				break;
			case 14:
				/* 1110 xxxx 10xx xxxx 10xx xxxx */
				count += 3;
				if (count > utflen)
					throw new UTFDataFormatException("malformed input: partial character at end");
				char2 = (int) bytearr[count - 2];
				char3 = (int) bytearr[count - 1];
				if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
					throw new UTFDataFormatException("malformed input around byte " + (count - 1));
				chararr[chararr_count++] = (char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0));
				break;
			default:
				/* 10xx xxxx, 1111 xxxx */
				throw new UTFDataFormatException("malformed input around byte " + count);
			}
		}
		// The number of chars produced may be less than utflen
		return new String(chararr, 0, chararr_count);
	}

	/**
	 * See the general contract of the <code>readUnsignedShort</code> method of <code>DataInput</code>.
	 * <p>
	 * Bytes for this operation are read from the given byte buffer
	 * 
	 * @param bb
	 *            bb
	 * @return the next two bytes of this input stream, interpreted as an unsigned 16-bit integer.
	 * @exception BufferUnderflowException
	 *                if this input stream reaches the end before reading two bytes.
	 * @see java.io.FilterInputStream#in
	 */
	public static int readUnsignedShort(ByteBuffer bb) throws BufferUnderflowException {
		int ch1 = bb.get() & 0xFF; // convert byte to unsigned byte
		int ch2 = bb.get() & 0xFF; // convert byte to unsigned byte
		return (ch1 << 8) + (ch2 << 0);
	}
}
