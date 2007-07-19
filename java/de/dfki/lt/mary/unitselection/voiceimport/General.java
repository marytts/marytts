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
package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * This class is for general purpose functions such as reading and
 * writing from files, or converting formats of numbers.
 */
public class General
{ 

    /**
     * Reads the next word (text separated by whitespace) from the
     * given stream
     *
     * @param dis the input stream
     *
     * @return the next word
     *
     * @throws IOException on error
     */
    public static String readWord(DataInputStream dis) throws IOException {
        StringBuffer sb = new StringBuffer();
        char c;

        // skip leading whitespace
        do {
            c = readChar(dis);
        } while(Character.isWhitespace(c));

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
     * @param dis the stream to read
     * @return the next character on the stream
     *
     * @throws IOException if an error occurs
     */
    public static char readChar(DataInputStream dis) throws IOException {
        return (char) dis.readByte();
    }

    /**
     * Reads a given number of chars from the stream
     *
     * @param dis the stream to read
     * @param num the number of chars to read
     * @return a character array containing the next <code>num<code>
     *          in the stream
     *
     * @throws IOException if an error occurs
     */
    public static char[] readChars(DataInputStream dis, int num)
            throws IOException {
        char[] carray = new char[num];
        for (int i = 0; i < num; i++) {
            carray[i] = readChar(dis);
        }
        return carray;
    }

    /**
     * Read a float from the input stream, byte-swapping as
     * necessary
     *
     * @param dis the inputstream
     * @param isBigEndian whether or not the data being read in is in
     *          big endian format.
     *
     * @return a floating pint value
     *
     * @throws IOException on error
     */
    public static float readFloat(DataInputStream dis, boolean isBigEndian)
            throws IOException {
        float val;
        if (!isBigEndian) {
            val =  readLittleEndianFloat(dis);
        } else {
            val =  dis.readFloat();
        }
        return val;
    }

    /**
     * Write a float from the output stream, byte-swapping as
     * necessary
     *
     * @param dos the outputstream
     * @param isBigEndian whether or not the data being read in is in
     *          big endian format.
     * @param val the floating point value to write
     *
     * @throws IOException on error
     */
    public static void writeFloat(DataOutputStream dos, boolean isBigEndian, float val)
            throws IOException {
        if (!isBigEndian) {
            writeLittleEndianFloat(dos,val);
        } else {
            dos.writeFloat( val );
        }
        return;
    }

    /**
     * Reads the next float from the given DataInputStream,
     * where the data is in little endian.
     *
     * @param dataStream the DataInputStream to read from
     *
     * @return a float
     */
    public static float readLittleEndianFloat(DataInputStream dataStream)
            throws IOException {
        return Float.intBitsToFloat(readLittleEndianInt(dataStream));
    }

    /**
     * Writes a float to the given DataOutputStream,
     * where the data is in little endian.
     *
     * @param dataStream the DataOutputStream to write to.
     * @param val The float value to write.
     */
    public static void writeLittleEndianFloat(DataOutputStream dataStream,float val)
            throws IOException {
        writeLittleEndianInt( dataStream, Float.floatToRawIntBits(val) );
    }

    /**
     * Read an integer from the input stream, byte-swapping as
     * necessary
     *
     * @param dis the inputstream
     * @param isBigEndian whether or not the data being read in is in
     *          big endian format.
     *
     * @return an integer value
     *
     * @throws IOException on error
     */
    public static int readInt(DataInputStream dis, boolean isBigEndian)
            throws IOException {
        if (!isBigEndian) {
            return readLittleEndianInt(dis);
        } else {
            return dis.readInt();
        }
    }

    /**
     * Writes an integer to the output stream, byte-swapping as
     * necessary
     *
     * @param dis the outputstream.
     * @param isBigEndian whether or not the data being read in is in
     *          big endian format.
     * @param val the integer value to write.
     *
     * @throws IOException on error
     */
    public static void writeInt(DataOutputStream dis, boolean isBigEndian, int val)
            throws IOException {
        if (!isBigEndian) {
            writeLittleEndianInt(dis,val);
        } else {
            dis.writeInt(val);
        }
        return;
    }

    /**
     * Reads the next little-endian integer from the given DataInputStream.
     *
     * @param dataStream the DataInputStream to read from
     *
     * @return an integer
     */
    public static int readLittleEndianInt(DataInputStream dataStream)
            throws IOException {
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
     * @param dataStream the DataOutputStream to write to
     * @param val the integer value to write
     *
     * @throws IOException on error
     */
    public static void writeLittleEndianInt(DataOutputStream dataStream, int val)
            throws IOException {
        int mask = 0x000000ff;
        for (int shift = 0; shift < 32; shift += 8) {
            dataStream.writeByte( mask & (val >> shift) );
        }
        return;
    }

    /**
     * Read a short from the input stream, byte-swapping as
     * necessary
     *
     * @param dis the inputstream
     * @param isBigEndian whether or not the data being read in is in
     *          big endian format.
     *
     * @return an integer value
     *
     * @throws IOException on error
     */
    public static short readShort(DataInputStream dis, boolean isBigEndian)
        throws IOException {
        if (!isBigEndian) {
            return readLittleEndianShort(dis);
        } else {
            return dis.readShort();
        }
    }

    /**
     * Reads the next little-endian short from the given DataInputStream.
     *
     * @param dataStream the DataInputStream to read from
     *
     * @return a short
     */
    public static short readLittleEndianShort(DataInputStream dis)
        throws IOException {
        short bits = (short)(0x0000ff & dis.readByte());
        bits |= (((short)(0x0000ff & dis.readByte())) << 8);
        return bits;
    }

    /**
     * Convert a short to ulaw format
     * 
     * @param sample the short to convert
     *
     * @return a short containing an unsigned 8-bit quantity
     *          representing the ulaw
     */
    public static byte shortToUlaw(short sample) {
        final int[] exp_lut = {0,0,1,1,2,2,2,2,3,3,3,3,3,3,3,3,
                                   4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
                                   5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
                                   5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
                                   6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
                                   6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
                                   6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
                                   6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7};

        int sign, exponent, mantissa;
        short ulawbyte;

        final short CLIP = 32635;
        final short BIAS = 0x0084;

        /* Get the sample into sign-magnitude. */
        sign = (sample >> 8) & 0x80; /* set aside the sign */
        if ( sign != 0 ) {
            sample = (short) -sample; /* get magnitude */
        }
        if ( sample > CLIP ) sample = CLIP; /* clip the magnitude */

        /* Convert from 16 bit linear to ulaw. */
        sample = (short) (sample + BIAS);
        exponent = exp_lut[( sample >> 7 ) & 0xFF];
        mantissa = ( sample >> ( exponent + 3 ) ) & 0x0F;
        ulawbyte = (short)
            ((~ ( sign | ( exponent << 4 ) | mantissa)) & 0x00FF);
        if ( ulawbyte == 0 ) ulawbyte = 0x02; /* optional CCITT trap */
        // Now ulawbyte is an unsigned 8-bit entity.
        // Return as a (signed) byte:
        return (byte) (ulawbyte-128);
    }

    /**
     * Convert a ulaw format to short
     * 
     * @param ulaw a (signed) byte which, after converting into a short and
     * adding 128, will be an unsigned 8-but quantity representing a ulaw
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
        sign = ( ulawbyte & ((short) 0x80) );
        exponent = (int) ( (ulawbyte & (short) 0x00FF) >> 4 ) & 0x07;
        mantissa = ulawbyte & (short) 0x0F;
        sample = (short) (exp_lut[exponent] + (mantissa << (exponent + 3)));
        if ( sign != 0 ) sample = (short) (-sample);

        return sample;
    }

    /**
     * Convert an array from short to ulaw.
     * @param samples an array in linear representation
     * @return an array in ulaw representation.
     * @see #shortToUlaw(short)
     */
    public static byte[] shortToUlaw(short[] samples)
    {
        if (samples == null) return null;
        byte[] ulaw = new byte[samples.length];
        for (int i=0; i<samples.length; i++) {
            ulaw[i] = shortToUlaw(samples[i]);
        }
        return ulaw;
    }
    
    /**
     * Convert an array from ulaw to short.
     * @param samples an array in ulaw representation
     * @return an array in linear representation.
     * @see #ulawToShort(byte)
     */
    public static short[] ulawToShort(byte[] ulaw)
    {
        if (ulaw == null) return null;
        short[] samples = new short[ulaw.length];
        for (int i=0; i<ulaw.length; i++) {
            samples[i] = ulawToShort(ulaw[i]);
        }
        return samples;
    }

    /**
     * Print a float type's internal bit representation in hex
     *
     * @param f the float to print
     *
     * @return a string containing the hex value of <code>f</code>
     */
    public static String hex(float f) {
        return Integer.toHexString(Float.floatToIntBits(f));
    }


    /**
     * Quantize a float variable over the 16bits signed short range
     * 
     * @param f the float to quantize
     * @param min the minimum possible value for variable f
     * @param range the possible range for variable f
     * 
     * @return the 16bits signed codeword, returned as a signed short
     * 
     * @author Sacha K.
     */
    public static short quantize( float f, float fMin, float fRange ) {
        return( (short)( ( (double)f - (double)fMin ) * 65535.0/((double)fRange) - 32768.0) );
    }
    

    /**
     * Quantize an array of floats over the 16bits signed short range
     * 
     * @param f the array of floats to quantize
     * @param min the minimum possible value for variable f
     * @param range the possible range for variable f
     * 
     * @return an array of 16bits signed codewords, returned as signed shorts
     * 
     * @author Sacha K.
     */
    public static short[] quantize( float[] f, float fMin, float fRange ) {
        
        int len = f.length;
        short[] ret = new short[len];
        
        for( int i = 0; i < len; i++ ) ret[i] = quantize( f[i], fMin, fRange );
        
        return( ret );
    }
    

    /**
     * Unquantize a 16bits signed short over a float range
     * 
     * @param s the 16bits signed codeword
     * @param min the minimum possible value for variable f
     * @param range the possible range for variable f
     * 
     * @return the corresponding float value
     * 
     * @author Sacha K.
     */
    public static float unQuantize( short s, float fMin, float fRange ) {
        return( (float)( ((double)(s) + 32768.0) * (double)fRange / 65535.0 - (double)fMin ) );
    }
    
    /**
     * Unquantize an array of 16bits signed shorts over a float range
     * 
     * @param s the array of 16bits signed codewords
     * @param min the minimum possible value for variable f
     * @param range the possible range for variable f
     * 
     * @return the corresponding array of float values
     * 
     * @author Sacha K.
     */
    public static float[] unQuantize( short[] s, float fMin, float fRange ) {
        
        int len = s.length;
        float[] ret = new float[len];
        
        for( int i = 0; i < len; i++ ) ret[i] = unQuantize( s[i], fMin, fRange );
        
        return( ret );
    }
    
}

