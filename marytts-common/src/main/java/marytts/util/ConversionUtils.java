/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.util;

/**
 * @author Oytun T&uuml;rk
 * 
 */
public class ConversionUtils {
	public static byte[] toByteArray(byte byteArray) {
		return new byte[] { byteArray };
	}

	public static byte[] toByteArray(byte[] byteArray) {
		return byteArray;
	}

	public static byte[] toByteArray(short data) {
		return new byte[] { (byte) ((data >> 8) & 0xff), (byte) ((data >> 0) & 0xff), };
	}

	public static byte[] toByteArray(short[] data) {
		if (data == null)
			return null;

		byte[] byts = new byte[data.length * 2];

		for (int i = 0; i < data.length; i++)
			System.arraycopy(toByteArray(data[i]), 0, byts, i * 2, 2);

		return byts;
	}

	public static byte[] toByteArray(char data) {
		return new byte[] { (byte) ((data >> 8) & 0xff), (byte) ((data >> 0) & 0xff), };
	}

	public static byte[] toByteArray(char[] data) {
		if (data == null)
			return null;

		byte[] byts = new byte[data.length * 2];

		for (int i = 0; i < data.length; i++)
			System.arraycopy(toByteArray(data[i]), 0, byts, i * 2, 2);

		return byts;
	}

	public static byte[] toByteArray(int data) {
		return new byte[] { (byte) ((data >> 24) & 0xff), (byte) ((data >> 16) & 0xff), (byte) ((data >> 8) & 0xff),
				(byte) ((data >> 0) & 0xff), };
	}

	public static byte[] toByteArray(int[] data) {
		if (data == null)
			return null;

		byte[] byts = new byte[data.length * 4];

		for (int i = 0; i < data.length; i++)
			System.arraycopy(toByteArray(data[i]), 0, byts, i * 4, 4);

		return byts;
	}

	public static byte[] toByteArray(long data) {
		return new byte[] { (byte) ((data >> 56) & 0xff), (byte) ((data >> 48) & 0xff), (byte) ((data >> 40) & 0xff),
				(byte) ((data >> 32) & 0xff), (byte) ((data >> 24) & 0xff), (byte) ((data >> 16) & 0xff),
				(byte) ((data >> 8) & 0xff), (byte) ((data >> 0) & 0xff), };
	}

	public static byte[] toByteArray(long[] data) {
		if (data == null)
			return null;

		byte[] byts = new byte[data.length * 8];

		for (int i = 0; i < data.length; i++)
			System.arraycopy(toByteArray(data[i]), 0, byts, i * 8, 8);

		return byts;
	}

	public static byte[] toByteArray(float data) {
		return toByteArray(Float.floatToRawIntBits(data));
	}

	public static byte[] toByteArray(float[] data) {
		if (data == null)
			return null;

		byte[] byts = new byte[data.length * 4];

		for (int i = 0; i < data.length; i++)
			System.arraycopy(toByteArray(data[i]), 0, byts, i * 4, 4);

		return byts;
	}

	public static byte[] toByteArray(double data) {
		return toByteArray(Double.doubleToRawLongBits(data));
	}

	public static byte[] toByteArray(double[] data) {
		if (data == null)
			return null;

		byte[] byts = new byte[data.length * 8];

		for (int i = 0; i < data.length; i++)
			System.arraycopy(toByteArray(data[i]), 0, byts, i * 8, 8);

		return byts;
	}

	public static byte[] toByteArray(boolean data) {
		return new byte[] { (byte) (data ? 0x01 : 0x00) };
	}

	public static byte[] toByteArray(boolean[] data) {
		if (data == null)
			return null;

		int len = data.length;
		byte[] lena = toByteArray(len);
		byte[] byts = new byte[lena.length + (len / 8) + (len % 8 != 0 ? 1 : 0)];

		System.arraycopy(lena, 0, byts, 0, lena.length);

		for (int i = 0, j = lena.length, k = 7; i < data.length; i++) {
			byts[j] |= (data[i] ? 1 : 0) << k--;
			if (k < 0) {
				j++;
				k = 7;
			}
		}

		return byts;
	}

	public static byte[] toByteArray(String data) {
		return (data == null) ? null : data.getBytes();
	}

	public static byte[] toByteArray(String[] data) {
		if (data == null)
			return null;

		int totalLength = 0;
		int bytesPos = 0;

		byte[] dLen = toByteArray(data.length);
		totalLength += dLen.length;

		int[] sLens = new int[data.length];
		totalLength += (sLens.length * 4);
		byte[][] strs = new byte[data.length][];

		for (int i = 0; i < data.length; i++) {
			if (data[i] != null) {
				strs[i] = toByteArray(data[i]);
				sLens[i] = strs[i].length;
				totalLength += strs[i].length;
			} else {
				sLens[i] = 0;
				strs[i] = new byte[0];
			}
		}

		byte[] bytes = new byte[totalLength];
		System.arraycopy(dLen, 0, bytes, 0, 4);

		byte[] bsLens = toByteArray(sLens);
		System.arraycopy(bsLens, 0, bytes, 4, bsLens.length);

		bytesPos += 4 + bsLens.length; // mark position

		for (byte[] sba : strs) {
			System.arraycopy(sba, 0, bytes, bytesPos, sba.length);
			bytesPos += sba.length;
		}

		return bytes;
	}

	public static byte toByte(byte[] byteArray) {
		return (byteArray == null || byteArray.length == 0) ? 0x0 : byteArray[0];
	}

	public static short toShort(byte[] byteArray) {
		if (byteArray == null || byteArray.length != 2)
			return 0x0;
		// ----------
		return (short) ((0xff & byteArray[0]) << 8 | (0xff & byteArray[1]) << 0);
	}

	public static short[] toShortArray(byte[] byteArray) {
		if (byteArray == null || byteArray.length % 2 != 0)
			return null;

		short[] shts = new short[byteArray.length / 2];

		for (int i = 0; i < shts.length; i++) {
			shts[i] = toShort(new byte[] { byteArray[(i * 2)], byteArray[(i * 2) + 1] });
		}

		return shts;
	}

	public static char toChar(byte[] byteArray) {
		if (byteArray == null || byteArray.length != 2)
			return 0x0;

		return (char) ((0xff & byteArray[0]) << 8 | (0xff & byteArray[1]) << 0);
	}

	public static char[] toCharArray(byte[] byteArray) {
		if (byteArray == null || byteArray.length % 2 != 0)
			return null;

		char[] chrs = new char[byteArray.length / 2];

		for (int i = 0; i < chrs.length; i++) {
			chrs[i] = toChar(new byte[] { byteArray[(i * 2)], byteArray[(i * 2) + 1], });
		}

		return chrs;
	}

	public static int toInt(byte[] byteArray) {
		if (byteArray == null || byteArray.length != 4)
			return 0x0;

		return (int) ((0xff & byteArray[0]) << 24 | (0xff & byteArray[1]) << 16 | (0xff & byteArray[2]) << 8 | (0xff & byteArray[3]) << 0);
	}

	public static int[] toIntArray(byte[] byteArray) {
		if (byteArray == null || byteArray.length % 4 != 0)
			return null;

		int[] ints = new int[byteArray.length / 4];

		for (int i = 0; i < ints.length; i++) {
			ints[i] = toInt(new byte[] { byteArray[(i * 4)], byteArray[(i * 4) + 1], byteArray[(i * 4) + 2],
					byteArray[(i * 4) + 3], });
		}

		return ints;
	}

	public static long toLong(byte[] byteArray) {
		if (byteArray == null || byteArray.length != 8)
			return 0x0;

		return (long) ((long) (0xff & byteArray[0]) << 56 | (long) (0xff & byteArray[1]) << 48
				| (long) (0xff & byteArray[2]) << 40 | (long) (0xff & byteArray[3]) << 32 | (long) (0xff & byteArray[4]) << 24
				| (long) (0xff & byteArray[5]) << 16 | (long) (0xff & byteArray[6]) << 8 | (long) (0xff & byteArray[7]) << 0);
	}

	public static long[] toLongArray(byte[] byteArray) {
		if (byteArray == null || byteArray.length % 8 != 0)
			return null;

		long[] lngs = new long[byteArray.length / 8];

		for (int i = 0; i < lngs.length; i++) {
			lngs[i] = toLong(new byte[] { byteArray[(i * 8)], byteArray[(i * 8) + 1], byteArray[(i * 8) + 2],
					byteArray[(i * 8) + 3], byteArray[(i * 8) + 4], byteArray[(i * 8) + 5], byteArray[(i * 8) + 6],
					byteArray[(i * 8) + 7], });
		}

		return lngs;
	}

	public static float toFloat(byte[] byteArray) {
		if (byteArray == null || byteArray.length != 4)
			return 0x0;

		return Float.intBitsToFloat(toInt(byteArray));
	}

	public static float[] toFloatArray(byte[] byteArray) {
		if (byteArray == null || byteArray.length % 4 != 0)
			return null;

		float[] flts = new float[byteArray.length / 4];

		for (int i = 0; i < flts.length; i++) {
			flts[i] = toFloat(new byte[] { byteArray[(i * 4)], byteArray[(i * 4) + 1], byteArray[(i * 4) + 2],
					byteArray[(i * 4) + 3], });
		}

		return flts;
	}

	public static double toDouble(byte[] byteArray) {
		if (byteArray == null || byteArray.length != 8)
			return 0x0;

		return Double.longBitsToDouble(toLong(byteArray));
	}

	public static double[] toDoubleArray(byte[] byteArray) {
		if (byteArray == null)
			return null;

		if (byteArray.length % 8 != 0)
			return null;

		double[] dbls = new double[byteArray.length / 8];

		for (int i = 0; i < dbls.length; i++) {
			dbls[i] = toDouble(new byte[] { byteArray[(i * 8)], byteArray[(i * 8) + 1], byteArray[(i * 8) + 2],
					byteArray[(i * 8) + 3], byteArray[(i * 8) + 4], byteArray[(i * 8) + 5], byteArray[(i * 8) + 6],
					byteArray[(i * 8) + 7], });
		}

		return dbls;
	}

	public static boolean toBoolean(byte[] byteArray) {
		return (byteArray == null || byteArray.length == 0) ? false : byteArray[0] != 0x00;
	}

	public static boolean[] toBooleanArray(byte[] byteArray) {
		if (byteArray == null || byteArray.length < 4)
			return null;

		int len = toInt(new byte[] { byteArray[0], byteArray[1], byteArray[2], byteArray[3] });
		boolean[] bools = new boolean[len];

		for (int i = 0, j = 4, k = 7; i < bools.length; i++) {
			bools[i] = ((byteArray[j] >> k--) & 0x01) == 1;
			if (k < 0) {
				j++;
				k = 7;
			}
		}

		return bools;
	}

	public static String toString(byte[] byteArray) {
		return (byteArray == null) ? null : new String(byteArray);
	}

	public static String[] toStringArray(byte[] byteArray) {
		if (byteArray == null || byteArray.length < 4)
			return null;

		byte[] bBuff = new byte[4];

		System.arraycopy(byteArray, 0, bBuff, 0, 4);
		int saLen = toInt(bBuff);

		if (byteArray.length < (4 + (saLen * 4)))
			return null;

		bBuff = new byte[saLen * 4];
		System.arraycopy(byteArray, 4, bBuff, 0, bBuff.length);
		int[] sLens = toIntArray(bBuff);
		if (sLens == null)
			return null;

		String[] strs = new String[saLen];
		for (int i = 0, dataPos = 4 + (saLen * 4); i < saLen; i++) {
			if (sLens[i] > 0) {
				if (byteArray.length >= (dataPos + sLens[i])) {
					bBuff = new byte[sLens[i]];
					System.arraycopy(byteArray, dataPos, bBuff, 0, sLens[i]);
					dataPos += sLens[i];
					strs[i] = toString(bBuff);
				} else
					return null;
			}
		}
		return strs;
	}
}
