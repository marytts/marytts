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

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class LittleEndianBinaryReader {
	private DataInputStream inputStream;
	private long accumLong;
	private int accumInt;
	private int shiftBy;
	private int low;
	private int high;

	public LittleEndianBinaryReader(DataInputStream d) {
		this.inputStream = d;
	}

	public LittleEndianBinaryReader(FileInputStream f) {
		this(new DataInputStream(f));
	}

	public LittleEndianBinaryReader(String filename) throws IOException {
		this(new DataInputStream(new FileInputStream(filename)));
	}

	/*
	 * public float readFloat() throws IOException { for (int i=0; i<4; i++) bytes[i] = inputStream.readByte();
	 * 
	 * return Float.intBitsToFloat(((0x0ff & bytes[0])<<0) | ((0x0ff & bytes[1])<<8) | ((0x0ff & bytes[2])<<16) | ((0x0ff &
	 * bytes[3])<<24)); }
	 */

	public short readShort() throws IOException {
		low = inputStream.readByte() & 0xff;
		high = inputStream.readByte() & 0xff;
		return (short) (high << 8 | low);
	}

	public long readLong() throws IOException {
		accumLong = 0;
		for (shiftBy = 0; shiftBy < 64; shiftBy += 8)
			accumLong |= (long) (inputStream.readByte() & 0xff) << shiftBy;

		return accumLong;
	}

	public char readChar() throws IOException {
		low = inputStream.readByte() & 0xff;
		high = inputStream.readByte();
		return (char) (high << 8 | low);
	}

	public int readInt() throws IOException {
		accumInt = 0;
		for (shiftBy = 0; shiftBy < 32; shiftBy += 8) {
			accumInt |= (inputStream.readByte() & 0xff) << shiftBy;
		}
		return accumInt;
	}

	public double readDouble() throws IOException {
		accumLong = 0;
		for (shiftBy = 0; shiftBy < 64; shiftBy += 8)
			accumLong |= ((long) (inputStream.readByte() & 0xff)) << shiftBy;

		return Double.longBitsToDouble(accumLong);
	}

	public float readFloat() throws IOException {
		accumInt = 0;
		for (shiftBy = 0; shiftBy < 32; shiftBy += 8)
			accumInt |= (inputStream.readByte() & 0xff) << shiftBy;

		return Float.intBitsToFloat(accumInt);
	}

	public byte readByte() throws IOException {
		return inputStream.readByte();
	}

	public void close() throws IOException {
		if (inputStream != null)
			inputStream.close();
	}
}
