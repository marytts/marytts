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
package marytts.util.io;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class is for general purpose functions such as reading and writing from files, or converting formats of numbers.
 * 
 * @author Sacha K.
 */
public class General {

	/**
	 * Reads the next word (text separated by whitespace) from the given stream
	 * 
	 * @param dis
	 *            the input stream
	 * 
	 * @return the next word
	 * 
	 * @throws IOException
	 *             on error
	 */
	public static String readWord(DataInputStream dis) throws IOException {
		StringBuilder sb = new StringBuilder();
		char c;

		// skip leading whitespace
		do {
			c = readChar(dis);
		} while (Character.isWhitespace(c));

		// read the word
		do {
			sb.append(c);
			c = readChar(dis);
		} while (!Character.isWhitespace(c));
		return sb.toString();
	}

	/**
	 * Reads a single char from the stream
	 * 
	 * @param dis
	 *            the stream to read
	 * @return the next character on the stream
	 * 
	 * @throws IOException
	 *             if an error occurs
	 */
	public static char readChar(DataInputStream dis) throws IOException {
		return (char) dis.readByte();
	}

	/**
	 * Reads a given number of chars from the stream
	 * 
	 * @param dis
	 *            the stream to read
	 * @param num
	 *            the number of chars to read
	 * @return a character array containing the next <code>num</code> in the stream
	 * 
	 * @throws IOException
	 *             if an error occurs
	 */
	public static char[] readChars(DataInputStream dis, int num) throws IOException {
		char[] carray = new char[num];
		for (int i = 0; i < num; i++) {
			carray[i] = readChar(dis);
		}
		return carray;
	}

	/**
	 * Read a float from the input stream, byte-swapping as necessary
	 * 
	 * @param dis
	 *            the inputstream
	 * @param isBigEndian
	 *            whether or not the data being read in is in big endian format.
	 * 
	 * @return a floating pint value
	 * 
	 * @throws IOException
	 *             on error
	 */
	public static float readFloat(DataInputStream dis, boolean isBigEndian) throws IOException {
		float val;
		if (!isBigEndian) {
			val = readLittleEndianFloat(dis);
		} else {
			val = dis.readFloat();
		}
		return val;
	}

	/**
	 * Write a float from the output stream, byte-swapping as necessary
	 * 
	 * @param dos
	 *            the outputstream
	 * @param isBigEndian
	 *            whether or not the data being read in is in big endian format.
	 * @param val
	 *            the floating point value to write
	 * 
	 * @throws IOException
	 *             on error
	 */
	public static void writeFloat(DataOutputStream dos, boolean isBigEndian, float val) throws IOException {
		if (!isBigEndian) {
			writeLittleEndianFloat(dos, val);
		} else {
			dos.writeFloat(val);
		}
	}

	/**
	 * Reads the next float from the given DataInputStream, where the data is in little endian.
	 * 
	 * @param dataStream
	 *            the DataInputStream to read from
	 * @throws IOException
	 *             IOException
	 * @return a float
	 */
	public static float readLittleEndianFloat(DataInputStream dataStream) throws IOException {
		return Float.intBitsToFloat(readLittleEndianInt(dataStream));
	}

	/**
	 * Writes a float to the given DataOutputStream, where the data is in little endian.
	 * 
	 * @param dataStream
	 *            the DataOutputStream to write to.
	 * @param val
	 *            The float value to write.
	 * @throws IOException
	 *             IOException
	 */
	public static void writeLittleEndianFloat(DataOutputStream dataStream, float val) throws IOException {
		writeLittleEndianInt(dataStream, Float.floatToRawIntBits(val));
	}

	/**
	 * Read an integer from the input stream, byte-swapping as necessary
	 * 
	 * @param dis
	 *            the inputstream
	 * @param isBigEndian
	 *            whether or not the data being read in is in big endian format.
	 * 
	 * @return an integer value
	 * 
	 * @throws IOException
	 *             on error
	 */
	public static int readInt(DataInputStream dis, boolean isBigEndian) throws IOException {
		if (!isBigEndian) {
			return readLittleEndianInt(dis);
		} else {
			return dis.readInt();
		}
	}

	/**
	 * Writes an integer to the output stream, byte-swapping as necessary
	 * 
	 * @param dis
	 *            the outputstream.
	 * @param isBigEndian
	 *            whether or not the data being read in is in big endian format.
	 * @param val
	 *            the integer value to write.
	 * 
	 * @throws IOException
	 *             on error
	 */
	public static void writeInt(DataOutputStream dis, boolean isBigEndian, int val) throws IOException {
		if (!isBigEndian) {
			writeLittleEndianInt(dis, val);
		} else {
			dis.writeInt(val);
		}
	}

	/**
	 * Reads the next little-endian integer from the given DataInputStream.
	 * 
	 * @param dataStream
	 *            the DataInputStream to read from
	 * @throws IOException
	 *             IOException
	 * @return an integer
	 */
	public static int readLittleEndianInt(DataInputStream dataStream) throws IOException {
		int bits = 0x00000000;
		for (int shift = 0; shift < 32; shift += 8) {
			int byteRead = (0x000000ff & dataStream.readByte());
			bits |= (byteRead << shift);
		}
		return bits;
	}

	/**
	 * Writes a little-endian integer to the given DataOutputStream.
	 * 
	 * @param dataStream
	 *            the DataOutputStream to write to
	 * @param val
	 *            the integer value to write
	 * 
	 * @throws IOException
	 *             on error
	 */
	public static void writeLittleEndianInt(DataOutputStream dataStream, int val) throws IOException {
		int mask = 0x000000ff;
		for (int shift = 0; shift < 32; shift += 8) {
			dataStream.writeByte(mask & (val >> shift));
		}
	}

	/**
	 * Read a short from the input stream, byte-swapping as necessary
	 * 
	 * @param dis
	 *            the inputstream
	 * @param isBigEndian
	 *            whether or not the data being read in is in big endian format.
	 * 
	 * @return an integer value
	 * 
	 * @throws IOException
	 *             on error
	 */
	public static short readShort(DataInputStream dis, boolean isBigEndian) throws IOException {
		if (!isBigEndian) {
			return readLittleEndianShort(dis);
		} else {
			return dis.readShort();
		}
	}

	/**
	 * Reads the next little-endian short from the given DataInputStream.
	 * 
	 * @param dis
	 *            the DataInputStream to read from
	 * @throws IOException
	 *             IOException
	 * @return a short
	 */
	public static short readLittleEndianShort(DataInputStream dis) throws IOException {
		short bits = (short) (0x0000ff & dis.readByte());
		bits |= (((short) (0x0000ff & dis.readByte())) << 8);
		return bits;
	}

	/**
	 * Convert a short to ulaw format
	 * 
	 * @param sample
	 *            the short to convert
	 * 
	 * @return a short containing an unsigned 8-bit quantity representing the ulaw
	 */
	public static byte shortToUlaw(short sample) {
		final int[] exp_lut = { 0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
				5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6,
				6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
				6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
				7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
				7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
				7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7 };

		int sign, exponent, mantissa;
		short ulawbyte;

		final short CLIP = 32635;
		final short BIAS = 0x0084;

		/* Get the sample into sign-magnitude. */
		sign = (sample >> 8) & 0x80; /* set aside the sign */
		if (sign != 0) {
			sample = (short) -sample; /* get magnitude */
		}
		if (sample > CLIP)
			sample = CLIP; /* clip the magnitude */

		/* Convert from 16 bit linear to ulaw. */
		sample = (short) (sample + BIAS);
		exponent = exp_lut[(sample >> 7) & 0xFF];
		mantissa = (sample >> (exponent + 3)) & 0x0F;
		ulawbyte = (short) ((~(sign | (exponent << 4) | mantissa)) & 0x00FF);
		if (ulawbyte == 0)
			ulawbyte = 0x02; /* optional CCITT trap */
		// Now ulawbyte is an unsigned 8-bit entity.
		// Return as a (signed) byte:
		return (byte) (ulawbyte - 128);
	}

	/**
	 * Convert a ulaw format to short
	 * 
	 * @param ulaw
	 *            a (signed) byte which, after converting into a short and adding 128, will be an unsigned 8-but quantity
	 *            representing a ulaw
	 * 
	 * @return the short equivalent of the ulaw
	 */
	public static short ulawToShort(byte ulaw) {
		short ulawbyte = (short) (ulaw + 128);
		final int[] exp_lut = { 0, 132, 396, 924, 1980, 4092, 8316, 16764 };
		int sign, exponent, mantissa;
		short sample;

		ulawbyte = (short) (ulawbyte & 0x00FF);
		ulawbyte = (short) (~ulawbyte);
		sign = (ulawbyte & ((short) 0x80));
		exponent = (int) ((ulawbyte & (short) 0x00FF) >> 4) & 0x07;
		mantissa = ulawbyte & (short) 0x0F;
		sample = (short) (exp_lut[exponent] + (mantissa << (exponent + 3)));
		if (sign != 0)
			sample = (short) (-sample);

		return sample;
	}

	/**
	 * Convert an array from short to ulaw.
	 * 
	 * @param samples
	 *            an array in linear representation
	 * @return an array in ulaw representation.
	 * @see #shortToUlaw(short)
	 */
	public static byte[] shortToUlaw(short[] samples) {
		if (samples == null)
			return null;
		byte[] ulaw = new byte[samples.length];
		for (int i = 0; i < samples.length; i++) {
			ulaw[i] = shortToUlaw(samples[i]);
		}
		return ulaw;
	}

	/**
	 * Convert an array from ulaw to short.
	 * 
	 * @param ulaw
	 *            an array in ulaw representation
	 * @return an array in linear representation.
	 * @see #ulawToShort(byte)
	 */
	public static short[] ulawToShort(byte[] ulaw) {
		if (ulaw == null)
			return null;
		short[] samples = new short[ulaw.length];
		for (int i = 0; i < ulaw.length; i++) {
			samples[i] = ulawToShort(ulaw[i]);
		}
		return samples;
	}

	/**
	 * Print a float type's internal bit representation in hex
	 * 
	 * @param f
	 *            the float to print
	 * 
	 * @return a string containing the hex value of <code>f</code>
	 */
	public static String hex(float f) {
		return Integer.toHexString(Float.floatToIntBits(f));
	}

	/**
	 * Quantize a float variable over the 16bits signed short range
	 * 
	 * @param f
	 *            the float to quantize
	 * @param fMin
	 *            the minimum possible value for variable f
	 * @param fRange
	 *            the possible range for variable f
	 * 
	 * @return the 16bits signed codeword, returned as a signed short
	 * 
	 * 
	 */
	public static short quantize(float f, float fMin, float fRange) {
		return ((short) (((double) f - (double) fMin) * 65535.0 / ((double) fRange) - 32768.0));
	}

	/**
	 * Quantize an array of floats over the 16bits signed short range
	 * 
	 * @param f
	 *            the array of floats to quantize
	 * @param fMin
	 *            the minimum possible value for variable f
	 * @param fRange
	 *            the possible range for variable f
	 * 
	 * @return an array of 16bits signed codewords, returned as signed shorts
	 * 
	 * 
	 */
	public static short[] quantize(float[] f, float fMin, float fRange) {

		int len = f.length;
		short[] ret = new short[len];

		for (int i = 0; i < len; i++)
			ret[i] = quantize(f[i], fMin, fRange);

		return (ret);
	}

	/**
	 * Unquantize a 16bits signed short over a float range
	 * 
	 * @param s
	 *            the 16bits signed codeword
	 * @param fMin
	 *            the minimum possible value for variable f
	 * @param fRange
	 *            the possible range for variable f
	 * 
	 * @return the corresponding float value
	 * 
	 * 
	 */
	public static float unQuantize(short s, float fMin, float fRange) {
		return ((float) (((double) (s) + 32768.0) * (double) fRange / 65535.0 - (double) fMin));
	}

	/**
	 * Unquantize an array of 16bits signed shorts over a float range
	 * 
	 * @param s
	 *            the array of 16bits signed codewords
	 * @param fMin
	 *            the minimum possible value for variable f
	 * @param fRange
	 *            the possible range for variable f
	 * 
	 * @return the corresponding array of float values
	 * 
	 * 
	 */
	public static float[] unQuantize(short[] s, float fMin, float fRange) {

		int len = s.length;
		float[] ret = new float[len];

		for (int i = 0; i < len; i++)
			ret[i] = unQuantize(s[i], fMin, fRange);

		return (ret);
	}

	/**
	 * A general process launcher for the various tasks
	 * 
	 * @param cmdLine
	 *            the command line to be launched.
	 * @param task
	 *            a task tag for error messages, such as "Pitchmarks" or "LPC".
	 * @param baseName
	 *            basename of the file currently processed, for error messages.
	 */
	public static void launchProc(String cmdLine, String task, String baseName) {

		Process proc = null;
		String line = null;
		// String[] cmd = null; // Java 5.0 compliant code

		try {
			/* Java 5.0 compliant code below. */
			/* Hook the command line to the process builder: */
			/*
			 * cmd = cmdLine.split( " " ); pb.command( cmd ); /* /* Launch the process:
			 */
			/* proc = pb.start(); */

			/* Java 1.0 equivalent: */
			proc = Runtime.getRuntime().exec(cmdLine);

			/* Collect stdout and send it to System.out: */
			InputStream procStdOut = proc.getInputStream();
			InputStream procStdErr = proc.getErrorStream();

			StreamLogger stdOutLogger = new StreamLogger(procStdOut, System.out);
			StreamLogger stdErrLogger = new StreamLogger(procStdErr, System.err);

			stdOutLogger.start();
			stdErrLogger.start();

			try {
				stdOutLogger.join();
				stdErrLogger.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			/* Wait and check the exit value */
			proc.waitFor();

			if (proc.exitValue() != 0) {
				throw new RuntimeException(task + " computation failed on file [" + baseName + "]!\n" + "Command line was: ["
						+ cmdLine + "].");
			}
		} catch (IOException e) {
			throw new RuntimeException(task + " computation provoked an IOException on file [" + baseName + "].", e);
		} catch (InterruptedException e) {
			throw new RuntimeException(task + " computation interrupted on file [" + baseName + "].", e);
		}

	}

	/**
	 * A general process launcher for the various tasks but using an intermediate batch file
	 * 
	 * @param cmdLine
	 *            the command line to be launched.
	 * @param task
	 *            a task tag for error messages, such as "Pitchmarks" or "LPC".
	 * @param filedir
	 *            filedir of the file currently processed, for error messages and for creating a temporal batch file.
	 */
	public static void launchBatchProc(String cmdLine, String task, String filedir) {

		Process proc = null;
		Process proctmp = null;
		BufferedReader procStdout = null;
		String line = null;
		String tmpFile = filedir + "tmp.bat";
		System.out.println("Running: " + cmdLine);
		// String[] cmd = null; // Java 5.0 compliant code

		try {
			FileWriter tmp = new FileWriter(tmpFile);
			tmp.write(cmdLine);
			tmp.close();

			/* make it executable... */
			proctmp = Runtime.getRuntime().exec("chmod +x " + tmpFile);
			proctmp.waitFor();
			if (proctmp.exitValue() != 0) {
				BufferedReader errReader = new BufferedReader(new InputStreamReader(proctmp.getErrorStream()));
				while ((line = errReader.readLine()) != null) {
					System.err.println("ERR> " + line);
				}
				errReader.close();
				throw new RuntimeException(task + " computation failed on file [" + filedir + "]!\n"
						+ "Command line was: [chmod +x " + tmpFile + "].");
			}

			/* Java 5.0 compliant code below. */
			/* Hook the command line to the process builder: */
			/*
			 * cmd = cmdLine.split( " " ); pb.command( cmd ); /* /* Launch the process:
			 */
			/* proc = pb.start(); */

			/* Java 1.0 equivalent: */
			proc = Runtime.getRuntime().exec(tmpFile);

			InputStream procStdOut = proc.getInputStream();
			InputStream procStdErr = proc.getErrorStream();

			StreamLogger stdOutLogger = new StreamLogger(procStdOut, System.out);
			StreamLogger stdErrLogger = new StreamLogger(procStdErr, System.err);

			stdOutLogger.start();
			stdErrLogger.start();

			try {
				stdOutLogger.join();
				stdErrLogger.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			/* Wait and check the exit value */
			proc.waitFor();
			if (proc.exitValue() != 0) {
				BufferedReader errReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				while ((line = errReader.readLine()) != null) {
					System.err.println("ERR> " + line);
				}
				errReader.close();
				throw new RuntimeException(task + " computation failed on file [" + filedir + "]!\n" + "Command line was: ["
						+ cmdLine + "].");
			}

			// Delete tmp.bat if created
			File batchFile = new File(tmpFile);
			if (batchFile.exists()) {
				batchFile.delete();
			}

		} catch (IOException e) {
			throw new RuntimeException(task + " computation provoked an IOException on file [" + filedir + "].", e);
		} catch (InterruptedException e) {
			throw new RuntimeException(task + " computation interrupted on file [" + filedir + "].", e);
		}

	}

}
