/**
 * Copyright 2004-2006 DFKI GmbH.
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
package marytts.util.math;

/**
 * A collection of static helper functions for dealing with arrays.
 * 
 * @author Marc Schr&ouml;der
 * 
 */
public class ArrayUtils {
	/**
	 * @deprecated use {@link org.apache.commons.lang.ArrayUtils#clone} instead
	 * @param orig
	 *            orig
	 * @return subarray(orig, 0, orig.length) if orig != null, null otherwise
	 */
	public static double[] copy(double[] orig) {
		if (orig != null)
			return subarray(orig, 0, orig.length);
		else
			return null;
	}

	/**
	 * @deprecated use {@link org.apache.commons.lang.ArrayUtils#clone} instead
	 * @param orig
	 *            orig
	 * @return subarray(orig, 0, orig.length) if orig != null, null otherwise
	 */
	public static byte[] copy(byte[] orig) {
		if (orig != null)
			return subarray(orig, 0, orig.length);
		else
			return null;
	}

	/**
	 * @deprecated use {@link org.apache.commons.lang.ArrayUtils#clone} instead
	 * @param orig
	 *            orig
	 * @return subarray(orig, 0, orig.length) if orig != null, null otherwise
	 */
	public static short[] copy(short[] orig) {
		if (orig != null)
			return subarray(orig, 0, orig.length);
		else
			return null;
	}

	/**
	 * @deprecated use {@link org.apache.commons.lang.ArrayUtils#clone} instead
	 * @param orig
	 *            orig
	 * @return subarray(orig, 0, orig.length) if orig != null, null otherwise
	 */
	public static float[] copy(float[] orig) {
		if (orig != null)
			return subarray(orig, 0, orig.length);
		else
			return null;
	}

	/**
	 * @deprecated use {@link org.apache.commons.lang.ArrayUtils#clone} instead
	 * @param orig
	 *            orig
	 * @return subarray(orig, 0, orig.length) if orig != null, null otherwise
	 */
	public static char[] copy(char[] orig) {
		if (orig != null)
			return subarray(orig, 0, orig.length);
		else
			return null;
	}

	/**
	 * @deprecated use {@link org.apache.commons.lang.ArrayUtils#clone} instead
	 * @param orig
	 *            orig
	 * @return subarray(orig, 0, orig.length) if orig != null, null otherwise
	 */
	public static int[] copy(int[] orig) {
		if (orig != null)
			return subarray(orig, 0, orig.length);
		else
			return null;
	}

	public static double[] copyFloat2Double(float[] orig) {
		if (orig != null)
			return subarrayFloat2Double(orig, 0, orig.length);
		else
			return null;
	}

	public static short[] copyFloat2Short(float[] orig) {
		if (orig != null)
			return subarrayFloat2Short(orig, 0, orig.length);
		else
			return null;
	}

	public static double[] copyShort2Double(short[] orig) {
		if (orig != null)
			return subarrayShort2Double(orig, 0, orig.length);
		else
			return null;
	}

	public static float[] copyShort2Float(short[] orig) {
		if (orig != null)
			return subarrayShort2Float(orig, 0, orig.length);
		else
			return null;
	}

	public static float[] copyDouble2Float(double[] orig) {
		if (orig != null)
			return subarrayDouble2Float(orig, 0, orig.length);
		else
			return null;
	}

	public static float[] copyChar2Float(char[] orig) {
		if (orig != null)
			return subarrayChar2Float(orig, 0, orig.length);
		else
			return null;
	}

	public static double[] copyChar2Double(char[] orig) {
		if (orig != null)
			return subarrayChar2Double(orig, 0, orig.length);
		else
			return null;
	}

	public static float[] copyByte2Float(byte[] orig) {
		if (orig != null)
			return subarrayByte2Float(orig, 0, orig.length);
		else
			return null;
	}

	public static double[] copyByte2Double(byte[] orig) {
		if (orig != null)
			return subarrayByte2Double(orig, 0, orig.length);
		else
			return null;
	}

	public static short[] copyDouble2Short(double[] orig) {
		if (orig != null)
			return subarrayDouble2Short(orig, 0, orig.length);
		else
			return null;
	}

	public static char[] copyDouble2Char(double[] orig) {
		if (orig != null)
			return subarrayDouble2Char(orig, 0, orig.length);
		else
			return null;
	}

	public static char[] copyFloat2Char(float[] orig) {
		if (orig != null)
			return subarrayFloat2Char(orig, 0, orig.length);
		else
			return null;
	}

	public static byte[] copyDouble2Byte(double[] orig) {
		if (orig != null)
			return subarrayDouble2Byte(orig, 0, orig.length);
		else
			return null;
	}

	public static byte[] copyFloat2Byte(float[] orig) {
		if (orig != null)
			return subarrayFloat2Byte(orig, 0, orig.length);
		else
			return null;
	}

	public static ComplexNumber[] copy(ComplexNumber[] orig) {
		ComplexNumber[] out = null;

		if (orig != null) {
			out = new ComplexNumber[orig.length];
			for (int i = 0; i < orig.length; i++)
				out[i] = new ComplexNumber(orig[i]);
		}

		return out;
	}

	public static double[] subarray(double[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		double[] sub = new double[len];
		System.arraycopy(orig, off, sub, 0, len);
		return sub;
	}

	public static byte[] subarray(byte[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		byte[] sub = new byte[len];
		System.arraycopy(orig, off, sub, 0, len);
		return sub;
	}

	public static float[] subarray(float[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		float[] sub = new float[len];
		for (int i = 0; i < len; i++)
			sub[i] = orig[i + off];

		return sub;
	}

	public static char[] subarray(char[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		char[] sub = new char[len];
		System.arraycopy(orig, off, sub, 0, len);
		return sub;
	}

	public static short[] subarray(short[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		short[] sub = new short[len];
		System.arraycopy(orig, off, sub, 0, len);
		return sub;
	}

	public static int[] subarray(int[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		int[] sub = new int[len];
		for (int i = 0; i < len; i++)
			sub[i] = orig[i + off];

		return sub;
	}

	public static double[] subarrayShort2Double(short[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		double[] sub = new double[len];
		for (int i = 0; i < len; i++)
			sub[i] = orig[i + off];

		return sub;
	}

	public static float[] subarrayShort2Float(short[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		float[] sub = new float[len];
		for (int i = 0; i < len; i++)
			sub[i] = orig[i + off];

		return sub;
	}

	public static double[] subarrayChar2Double(char[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		double[] sub = new double[len];
		for (int i = 0; i < len; i++)
			sub[i] = orig[i + off];

		return sub;
	}

	public static float[] subarrayChar2Float(char[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		float[] sub = new float[len];
		for (int i = 0; i < len; i++)
			sub[i] = orig[i + off];

		return sub;
	}

	public static double[] subarrayByte2Double(byte[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		double[] sub = new double[len];
		for (int i = 0; i < len; i++)
			sub[i] = orig[i + off];

		return sub;
	}

	public static float[] subarrayByte2Float(byte[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		float[] sub = new float[len];
		for (int i = 0; i < len; i++)
			sub[i] = orig[i + off];

		return sub;
	}

	public static short[] subarrayDouble2Short(double[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		short[] sub = new short[len];
		for (int i = 0; i < len; i++)
			sub[i] = (short) orig[i + off];

		return sub;
	}

	public static char[] subarrayDouble2Char(double[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		char[] sub = new char[len];
		for (int i = 0; i < len; i++)
			sub[i] = (char) orig[i + off];

		return sub;
	}

	public static char[] subarrayFloat2Char(float[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		char[] sub = new char[len];
		for (int i = 0; i < len; i++)
			sub[i] = (char) orig[i + off];

		return sub;
	}

	public static byte[] subarrayDouble2Byte(double[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		byte[] sub = new byte[len];
		for (int i = 0; i < len; i++)
			sub[i] = (byte) orig[i + off];

		return sub;
	}

	public static byte[] subarrayFloat2Byte(float[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		byte[] sub = new byte[len];
		for (int i = 0; i < len; i++)
			sub[i] = (byte) orig[i + off];

		return sub;
	}

	public static double[] subarrayFloat2Double(float[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		double[] sub = new double[len];
		for (int i = 0; i < len; i++)
			sub[i] = orig[i + off];

		return sub;
	}

	public static short[] subarrayFloat2Short(float[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		short[] sub = new short[len];
		for (int i = 0; i < len; i++)
			sub[i] = (short) orig[i + off];

		return sub;
	}

	public static float[] subarrayDouble2Float(double[] orig, int off, int len) {
		if (off + len > orig.length)
			throw new IllegalArgumentException("requested subarray exceeds array length");
		float[] sub = new float[len];
		for (int i = 0; i < len; i++)
			sub[i] = (float) orig[i + off];

		return sub;
	}

	// Returns true if val is at least once contained in array
	// Otherwise returns false
	public static boolean isOneOf(int[] array, int val) {
		boolean ret = false;
		for (int i = 0; i < array.length; i++) {
			if (array[i] == val) {
				ret = true;
				break;
			}
		}

		return ret;
	}

	// Appends val to the beginning of array
	public static int[] appendToStart(int[] array, int val) {
		int len = 1;
		if (array != null)
			len += array.length;

		int[] arrayOut = new int[len];
		arrayOut[0] = val;
		if (array != null)
			System.arraycopy(array, 0, arrayOut, 1, array.length);

		return arrayOut;
	}

	// Appends val to the end of array
	public static int[] appendToEnd(int[] array, int val) {
		int len = 1;
		if (array != null)
			len += array.length;

		int[] arrayOut = new int[len];
		arrayOut[len - 1] = val;
		if (array != null)
			System.arraycopy(array, 0, arrayOut, 0, array.length);

		return arrayOut;
	}

	/**
	 * Returns the vector [x y]
	 * 
	 * @deprecated use {@link org.apache.commons.lang.ArrayUtils#addAll} instead
	 * @param x
	 *            x
	 * @param y
	 *            y
	 * @return z
	 */
	public static float[] combine(float[] x, float[] y) {
		int len = 0;
		if (x != null)
			len += x.length;
		if (y != null)
			len += y.length;

		float[] z = null;

		if (len > 0) {
			z = new float[len];

			int currentPos = 0;
			if (x != null) {
				System.arraycopy(x, 0, z, currentPos, x.length);
				currentPos = x.length;
			}

			if (y != null)
				System.arraycopy(y, 0, z, currentPos, y.length);
		}

		return z;
	}

	/**
	 * Returns the vector [x y]
	 * 
	 * @deprecated use {@link org.apache.commons.lang.ArrayUtils#addAll} instead
	 * @param x
	 *            x
	 * @param y
	 *            y
	 * @return z
	 */
	public static double[] combine(double[] x, double[] y) {
		int len = 0;
		if (x != null)
			len += x.length;
		if (y != null)
			len += y.length;

		double[] z = null;

		if (len > 0) {
			z = new double[len];

			int currentPos = 0;
			if (x != null) {
				System.arraycopy(x, 0, z, currentPos, x.length);
				currentPos = x.length;
			}

			if (y != null)
				System.arraycopy(y, 0, z, currentPos, y.length);
		}

		return z;
	}

	public static boolean isZero(float[] array) {
		boolean isZero = true;
		for (int j = 0; j < array.length; j++) {
			if (array[j] != 0) {
				isZero = false;
				break;
			}
		}
		return isZero;
	}

	public static boolean isZero(double[] array) {
		boolean isZero = true;
		for (int j = 0; j < array.length; j++) {
			if (array[j] != 0) {
				isZero = false;
				break;
			}
		}
		return isZero;
	}

}
