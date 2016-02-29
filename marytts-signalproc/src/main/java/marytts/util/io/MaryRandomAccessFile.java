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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A class that extends RandomAccessFile to read/write arrays of different types while allowing random access to a binary file
 * (i.e. the file can be opened in both read/write mode and there is support for moving the file pointer to any location as
 * required
 * 
 * @author Oytun T&uuml;rk
 */

public final class MaryRandomAccessFile extends RandomAccessFile {
	public MaryRandomAccessFile(File arg0, String arg1) throws FileNotFoundException {
		super(arg0, arg1);
	}

	public MaryRandomAccessFile(String arg0, String arg1) throws FileNotFoundException {
		super(arg0, arg1);
	}

	public final boolean readBooleanEndian() throws IOException {
		boolean ret = readBoolean();

		return ret;
	}

	public final boolean[] readBoolean(int len) throws IOException {
		boolean[] ret = new boolean[len];

		for (int i = 0; i < len; i++)
			ret[i] = readBoolean();

		return ret;
	}

	public final boolean[] readBooleanEndian(int len) throws IOException {
		boolean[] ret = new boolean[len];

		for (int i = 0; i < len; i++)
			ret[i] = readBooleanEndian();

		return ret;
	}

	public final byte readByteEndian() throws IOException {
		byte ret = readByte();

		return ret;
	}

	public final byte[] readByte(int len) throws IOException {
		byte[] ret = new byte[len];

		for (int i = 0; i < len; i++)
			ret[i] = readByte();

		return ret;
	}

	public final byte[] readByteEndian(int len) throws IOException {
		byte[] ret = new byte[len];

		for (int i = 0; i < len; i++)
			ret[i] = readByteEndian();

		return ret;
	}

	public final char readCharEndian() throws IOException {
		char c = (char) readByte();

		return c;
	}

	public final char[] readChar(int len) throws IOException {
		char[] ret = new char[len];

		for (int i = 0; i < len; i++)
			ret[i] = readChar();

		return ret;
	}

	public final char[] readCharEndian(int len) throws IOException {
		char[] ret = new char[len];

		for (int i = 0; i < len; i++)
			ret[i] = readCharEndian();

		return ret;
	}

	public final double readDoubleEndian() throws IOException {
		double ret = readDouble();

		return ret;
	}

	public final double[] readDouble(int len) throws IOException {
		double[] ret = new double[len];

		for (int i = 0; i < len; i++)
			ret[i] = readDouble();

		return ret;
	}

	public final double[] readDoubleEndian(int len) throws IOException {
		double[] ret = new double[len];

		for (int i = 0; i < len; i++)
			ret[i] = readDoubleEndian();

		return ret;
	}

	public final int readDoubleToIntEndian() throws IOException {
		int ret = (int) readDouble();

		return ret;
	}

	public final int[] readDoubleToInt(int len) throws IOException {
		int[] ret = new int[len];

		for (int i = 0; i < len; i++)
			ret[i] = (int) readDouble();

		return ret;
	}

	public final int[] readDoubleToIntEndian(int len) throws IOException {
		int[] ret = new int[len];

		for (int i = 0; i < len; i++)
			ret[i] = readDoubleToIntEndian();

		return ret;
	}

	public final float readFloatEndian() throws IOException {
		float ret = readFloat();

		return ret;
	}

	public final float[] readFloat(int len) throws IOException {
		float[] ret = new float[len];

		for (int i = 0; i < len; i++)
			ret[i] = readFloat();

		return ret;
	}

	public final float[] readFloatEndian(int len) throws IOException {
		float[] ret = new float[len];

		for (int i = 0; i < len; i++)
			ret[i] = readFloatEndian();

		return ret;
	}

	public final int readIntEndian() throws IOException {
		int ret = readInt();

		return ret;
	}

	public final int[] readInt(int len) throws IOException {
		int[] ret = new int[len];

		for (int i = 0; i < len; i++)
			ret[i] = readInt();

		return ret;
	}

	public final int[] readIntEndian(int len) throws IOException {
		int[] ret = new int[len];

		for (int i = 0; i < len; i++)
			ret[i] = readIntEndian();

		return ret;
	}

	public final long readLongEndian() throws IOException {
		long ret = (long) readInt();

		return ret;
	}

	public final long[] readLong(int len) throws IOException {
		long[] ret = new long[len];

		for (int i = 0; i < len; i++)
			ret[i] = readLong();

		return ret;
	}

	public final long[] readLongEndian(int len) throws IOException {
		long[] ret = new long[len];

		for (int i = 0; i < len; i++)
			ret[i] = readLongEndian();

		return ret;
	}

	public final short readShortEndian() throws IOException {
		short ret = readShort();

		return ret;
	}

	public final short[] readShort(int len) throws IOException {
		short[] ret = new short[len];

		for (int i = 0; i < len; i++)
			ret[i] = readShort();

		return ret;
	}

	public final short[] readShortEndian(int len) throws IOException {
		short[] ret = new short[len];

		for (int i = 0; i < len; i++)
			ret[i] = readShortEndian();

		return ret;
	}

	public final int readUnsignedByteEndian() throws IOException {
		int ret = readUnsignedByte();

		return ret;
	}

	public final int[] readUnsignedByte(int len) throws IOException {
		int[] ret = new int[len];

		for (int i = 0; i < len; i++)
			ret[i] = readUnsignedByte();

		return ret;
	}

	public final int[] readUnsignedByteEndian(int len) throws IOException {
		int[] ret = new int[len];

		for (int i = 0; i < len; i++)
			ret[i] = readUnsignedByteEndian();

		return ret;
	}

	public final int readUnsignedShortEndian() throws IOException {
		int ret = readUnsignedShort();

		return ret;
	}

	public final int[] readUnsignedShort(int len) throws IOException {
		int[] ret = new int[len];

		for (int i = 0; i < len; i++)
			ret[i] = readUnsignedShort();

		return ret;
	}

	public final int[] readUnsignedShortEndian(int len) throws IOException {
		int[] ret = new int[len];

		for (int i = 0; i < len; i++)
			ret[i] = readUnsignedShortEndian();

		return ret;
	}

	public final void writeBooleanEndian(boolean v) throws IOException {
		writeBoolean(v);
	}

	public final void writeBoolean(boolean[] v, int startPos, int len) throws IOException {
		assert v.length < startPos + len;

		for (int i = startPos; i < startPos + len; i++)
			writeBoolean(v[i]);
	}

	public final void writeBooleanEndian(boolean[] v, int startPos, int len) throws IOException {
		assert v.length < startPos + len;

		for (int i = startPos; i < startPos + len; i++)
			writeBooleanEndian(v[i]);
	}

	public final void writeBoolean(boolean[] v) throws IOException {
		writeBoolean(v, 0, v.length);
	}

	public final void writeBooleanEndian(boolean[] v) throws IOException {
		writeBooleanEndian(v, 0, v.length);
	}

	public final void writeByteEndian(byte v) throws IOException {
		writeByte(v);
	}

	public final void writeByte(byte[] v, int startPos, int len) throws IOException {
		assert v.length < startPos + len;

		for (int i = startPos; i < startPos + len; i++)
			writeByte(v[i]);
	}

	public final void writeByteEndian(byte[] v, int startPos, int len) throws IOException {
		assert v.length < startPos + len;

		for (int i = startPos; i < startPos + len; i++)
			writeByteEndian(v[i]);
	}

	public final void writeByte(byte[] v) throws IOException {
		writeByte(v, 0, v.length);
	}

	public final void writeByteEndian(byte[] v) throws IOException {
		writeByteEndian(v, 0, v.length);
	}

	public final void writeCharEndian(char c) throws IOException {
		writeByte((byte) c);
	}

	public final void writeChar(char[] v, int startPos, int len) throws IOException {
		assert v.length < startPos + len;

		for (int i = startPos; i < startPos + len; i++)
			writeChar(v[i]);
	}

	public final void writeCharEndian(char[] v, int startPos, int len) throws IOException {
		assert v.length < startPos + len;

		for (int i = startPos; i < startPos + len; i++)
			writeCharEndian(v[i]);
	}

	public final void writeChar(char[] v) throws IOException {
		writeChar(v, 0, v.length);
	}

	public final void writeCharEndian(char[] v) throws IOException {
		writeCharEndian(v, 0, v.length);
	}

	public final void writeDoubleEndian(double v) throws IOException {
		writeDouble(v);
	}

	public final void writeDouble(double[] v, int startPos, int len) throws IOException {
		for (int i = startPos; i < startPos + len; i++)
			writeDouble(v[i]);
	}

	public final void writeDoubleEndian(double[] v, int startPos, int len) throws IOException {
		for (int i = startPos; i < startPos + len; i++)
			writeDoubleEndian(v[i]);
	}

	public final void writeDouble(double[] v) throws IOException {
		writeDouble(v, 0, v.length);
	}

	public final void writeDoubleEndian(double[] v) throws IOException {
		writeDoubleEndian(v, 0, v.length);
	}

	public final void writeFloatEndian(float v) throws IOException {
		writeFloat(v);
	}

	public final void writeFloat(float[] v, int startPos, int len) throws IOException {
		assert v.length < startPos + len;

		for (int i = startPos; i < startPos + len; i++)
			writeFloat(v[i]);
	}

	public final void writeFloatEndian(float[] v, int startPos, int len) throws IOException {
		assert v.length < startPos + len;

		for (int i = startPos; i < startPos + len; i++)
			writeFloatEndian(v[i]);
	}

	public final void writeFloat(float[] v) throws IOException {
		writeFloat(v, 0, v.length);
	}

	public final void writeFloatEndian(float[] v) throws IOException {
		writeFloatEndian(v, 0, v.length);
	}

	public final void writeIntEndian(int v) throws IOException {
		writeInt(v);
	}

	public final void writeInt(int[] v, int startPos, int len) throws IOException {
		assert v.length < startPos + len;

		for (int i = startPos; i < startPos + len; i++)
			writeInt(v[i]);
	}

	public final void writeIntEndian(int[] v, int startPos, int len) throws IOException {
		assert v.length < startPos + len;

		for (int i = startPos; i < startPos + len; i++)
			writeIntEndian(v[i]);
	}

	public final void writeInt(int[] v) throws IOException {
		writeInt(v, 0, v.length);
	}

	public final void writeIntEndian(int[] v) throws IOException {
		writeIntEndian(v, 0, v.length);
	}

	public final void writeLongEndian(long v) throws IOException {
		writeInt((int) v);
	}

	public final void writeLong(long[] v, int startPos, int len) throws IOException {
		assert v.length < startPos + len;

		for (int i = startPos; i < startPos + len; i++)
			writeLong(v[i]);
	}

	public final void writeLongEndian(long[] v, int startPos, int len) throws IOException {
		assert v.length < startPos + len;

		for (int i = startPos; i < startPos + len; i++)
			writeLongEndian(v[i]);
	}

	public final void writeLong(long[] v) throws IOException {
		writeLong(v, 0, v.length);
	}

	public final void writeLongEndian(long[] v) throws IOException {
		writeLongEndian(v, 0, v.length);
	}

	public final void writeShortEndian(short v) throws IOException {
		writeShort(v);
	}

	public final void writeShort(short[] v, int startPos, int len) throws IOException {
		assert v.length < startPos + len;

		for (int i = startPos; i < startPos + len; i++)
			writeShort(v[i]);
	}

	public final void writeShortEndian(short[] v, int startPos, int len) throws IOException {
		assert v.length < startPos + len;

		for (int i = startPos; i < startPos + len; i++)
			writeShortEndian(v[i]);
	}

	public final void writeShort(short[] v) throws IOException {
		writeShort(v, 0, v.length);
	}

	public final void writeShortEndian(short[] v) throws IOException {
		writeShort(v);
	}
}
