/**
 * Copyright 2004-2006 DFKI GmbH.
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

package de.dfki.lt.signalproc.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * @author Marc Schr&ouml;der
 * An audio input stream that draws its audio data from a DoubleDataSource.
 */
public class DDSAudioInputStream extends AudioInputStream {
    public static final int MAX_AMPLITUDE = 32767;
    protected DoubleDataSource source;
    protected double[] sampleBuf;
    protected static final int SAMPLEBUFFERSIZE = 8192;
    
    /**
     * From the given DoubleDataSource, create an AudioInputStream of the given
     * audio format.
     * @param source
     * @param format
     * @throws IllegalArgumentException if the format is not mono, not PCM_SIGNED or PCM_UNSIGNED,
     * or has a sample size in bits other than 8 or 16.
     */
    public DDSAudioInputStream(DoubleDataSource source, AudioFormat format)
    {
        super(new ByteArrayInputStream(new byte[0]), format, AudioSystem.NOT_SPECIFIED);
        if (format.getChannels()>1) {
            throw new IllegalArgumentException("Can only produce mono audio");
        }
        if (!format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) &&
                !format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
            throw new IllegalArgumentException("Can only produce PCM_SIGNED or PCM_UNSIGNED audio");
        }
        int bitsPerSample = format.getSampleSizeInBits();
        if (bitsPerSample != 8 && bitsPerSample != 16 && bitsPerSample != 24) {
            throw new IllegalArgumentException("Can deal with sample size 8 or 16 or 24, but not " + bitsPerSample);
        }
        this.source = source;
        this.sampleBuf = new double[SAMPLEBUFFERSIZE];
        assert frameSize == bitsPerSample/8;        
    }
    
    /**
     * Reads up to a specified maximum number of bytes of data from the audio
     * stream, putting them into the given byte array.
     * <p>This method will always read an integral number of frames.
     * If <code>len</code> does not specify an integral number
     * of frames, a maximum of <code>len - (len % frameSize)
     * </code> bytes will be read.
     *
     * @param b the buffer into which the data is read
     * @param off the offset, from the beginning of array <code>b</code>, at which
     * the data will be written
     * @param len the maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or -1 if there
     * is no more data because the end of the stream has been reached
     * @throws IOException if an input or output error occurs
     * @see #read(byte[])
     * @see #read()
     * @see #skip
     * @see #available
     */
    public int read(byte[] b, int off, int len) throws IOException {
        int nSamples = len/frameSize;
        int totalRead = 0;
        int currentPos = off;
        do {
            int toRead = nSamples-totalRead;
            if (toRead > sampleBuf.length) toRead = sampleBuf.length;
            int nRead = source.getData(sampleBuf, 0, toRead);
            //System.err.println("DDSAudioInputStream: read " + nRead + " samples from source");
            if (frameSize == 1) { // bytes per sample
                for (int i=0; i<nRead; i++, currentPos++) {
                    int sample = (int) Math.round(sampleBuf[i] * 127.0); // de-normalise to value range
                    b[currentPos] = (byte) ((sample>>8)&0xFF);
                }
            } else if (frameSize == 2){ // 16 bit
                boolean bigEndian = format.isBigEndian();
                for (int i=0; i<nRead; i++, currentPos+=2) {
                    int sample = (int) Math.round(sampleBuf[i] * 32767.0); // de-normalise to value range
                    if (sample > MAX_AMPLITUDE || sample < -MAX_AMPLITUDE) {
                        System.err.println("Warning: signal amplitude out of range: "+sample);
                    }
                    byte hibyte = (byte) (sample>>8);
                    byte lobyte = (byte) (sample&0xFF);
                    if (!bigEndian) {
                        b[currentPos] = lobyte;
                        b[currentPos+1] = hibyte;
                    } else {
                        b[currentPos] = hibyte;
                        b[currentPos+1] = lobyte;
                    }
                    //System.err.println("out sample["+i+"]="+sample+" hi:"+Integer.toBinaryString(hibyte)+"/"+hibyte+" lo:"+Integer.toBinaryString(lobyte)+"/"+lobyte);
                }
            } else { // 24 bit
                boolean bigEndian = format.isBigEndian();
                for (int i=0; i<nRead; i++, currentPos+=3) {
                    int sample = (int) Math.round(sampleBuf[i] * 8388605.0); // de-normalise to value range
                    byte hibyte = (byte) (sample>>16);
                    byte midbyte = (byte) ((sample>>8) & 0xFF);
                    byte lobyte = (byte) (sample&0xFF);
                    if (!bigEndian) {
                        b[currentPos] = lobyte;
                        b[currentPos+1] = midbyte;
                        b[currentPos+2] = hibyte;
                    } else {
                        b[currentPos] = hibyte;
                        b[currentPos+1] = midbyte;
                        b[currentPos+2] = lobyte;
                    }
                    //System.err.println("out sample["+i+"]="+sample+" hi:"+Integer.toBinaryString(hibyte)+"/"+hibyte+" lo:"+Integer.toBinaryString(lobyte)+"/"+lobyte);
                }
            }
            totalRead += nRead;
            assert currentPos <= off+len;
        } while (source.hasMoreData() && totalRead < nSamples);
        if (totalRead == 0) return -1;
        else return totalRead*frameSize;
    }
 
    /**
     * Skips over and discards a specified number of bytes from this
     * audio input stream.
     * @param n the requested number of bytes to be skipped
     * @return the actual number of bytes skipped
     * @throws IOException if an input or output error occurs
     * @see #read
     * @see #available
     */
    public long skip(long n) throws IOException {
        double[] data = source.getData((int)n); 
        return data.length;
    }
    
    /**
     * Returns the maximum number of bytes that can be read (or skipped over) from this
     * audio input stream without blocking.  This limit applies only to the next invocation of
     * a <code>read</code> or <code>skip</code> method for this audio input stream; the limit
     * can vary each time these methods are invoked.
     * Depending on the underlying stream,an IOException may be thrown if this
     * stream is closed.
     * @return the number of bytes that can be read from this audio input stream without blocking
     * @throws IOException if an input or output error occurs
     * @see #read(byte[], int, int)
     * @see #read(byte[])
     * @see #read()
     * @see #skip
     */
    public int available() throws IOException {
        return frameSize*source.available();
    }
    
    /**
     * Closes this audio input stream and releases any system resources associated
     * with the stream.
     * @throws IOException if an input or output error occurs
     */
    public void close() throws IOException {
    }
    
    /**
     * Marks the current position in this audio input stream.
     * @param readlimit the maximum number of bytes that can be read before
     * the mark position becomes invalid.
     * @see #reset
     * @see #markSupported
     */
    public void mark(int readlimit) {
    }
    
    /**
     * Repositions this audio input stream to the position it had at the time its
     * <code>mark</code> method was last invoked.
     * @throws IOException if an input or output error occurs.
     * @see #mark
     * @see #markSupported
     */
    public void reset() throws IOException {
    }
    
    /**
     * Tests whether this audio input stream supports the <code>mark</code> and
     * <code>reset</code> methods.
     * @return <code>true</code> if this stream supports the <code>mark</code>
     * and <code>reset</code> methods; <code>false</code> otherwise
     * @see #mark
     * @see #reset
     */
    public boolean markSupported() {
        return false;
    }
    
    public long getFrameLength()
    {
        long dataLength = source.getDataLength();
        if (dataLength == DoubleDataSource.NOT_SPECIFIED) return AudioSystem.NOT_SPECIFIED;
        else return dataLength;
    }
}
