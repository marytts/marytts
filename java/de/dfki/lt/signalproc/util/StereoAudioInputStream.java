package de.dfki.lt.signalproc.util;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat.Encoding;

public class StereoAudioInputStream extends AudioInputStream
{
    protected int inputChannels;
    protected int outputMode;
    protected AudioFormat newFormat;
    
    public StereoAudioInputStream(AudioInputStream input)
    {
        this(input, AudioPlayer.STEREO);
    }
    
    /**
     * 
     * @param input
     * @param outputMode as defined in AudioPlayer: STEREO, LEFT_ONLY or RIGHT_ONLY.
     */
    public StereoAudioInputStream(AudioInputStream input, int outputMode)
    {
        super(input, input.getFormat(), input.getFrameLength());
        this.newFormat = new AudioFormat(input.getFormat().getEncoding(), input.getFormat().getSampleRate(),
                input.getFormat().getSampleSizeInBits(), 2, 2*input.getFormat().getFrameSize()/input.getFormat().getChannels(),
                input.getFormat().getFrameRate(), input.getFormat().isBigEndian());
        this.inputChannels = input.getFormat().getChannels();
        this.outputMode = outputMode;
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
        int sampleSizeInBytes = frameSize / inputChannels;
        int outputFrameSize = sampleSizeInBytes * 2;
        int nFrames = len/outputFrameSize;
        byte[] inputBytes = new byte[nFrames*frameSize];
        int nInputBytes = super.read(inputBytes, 0, inputBytes.length);
        if (nInputBytes <= 0) return nInputBytes;
        // For mono input, copy the mono signal to the output channels indicated in outputMode:
        if (inputChannels == 1) {
            if (outputMode == AudioPlayer.STEREO) {
                for(int i=0, j=off; i<nInputBytes; i+=frameSize, j+=outputFrameSize) {
                    for (int k=0; k<sampleSizeInBytes; k++) {
                        b[j+k] = b[j+sampleSizeInBytes+k] = inputBytes[i+k];
                    }
                }
            } else if (outputMode == AudioPlayer.LEFT_ONLY) {
                if (!getFormat().getEncoding().equals(Encoding.PCM_SIGNED)) {
                    throw new IllegalArgumentException("Channel muting supported only for PCM_SIGNED encoding, got "+getFormat().getEncoding());
                }
                for(int i=0, j=off; i<nInputBytes; i+=frameSize, j+=outputFrameSize) {
                    for (int k=0; k<sampleSizeInBytes; k++) {
                        b[j+k] = inputBytes[i+k];
                        b[j+sampleSizeInBytes+k] = 0;
                    }
                }
            } else {
                assert outputMode == AudioPlayer.RIGHT_ONLY : "Unexpected output mode: "+outputMode;
                if (!getFormat().getEncoding().equals(Encoding.PCM_SIGNED)) {
                    throw new IllegalArgumentException("Channel muting supported only for PCM_SIGNED encoding, got "+getFormat().getEncoding());
                }
                for(int i=0, j=off; i<nInputBytes; i+=frameSize, j+=outputFrameSize) {
                    for (int k=0; k<sampleSizeInBytes; k++) {
                        b[j+k] = 0;
                        b[j+sampleSizeInBytes+k] = inputBytes[i+k];
                    }
                }
            }
        } else {
            // For stereo or more channels' input, retain the first two channels according to outputMode:
            if (outputMode == AudioPlayer.STEREO) {
                for(int i=0, j=off; i<nInputBytes; i+=frameSize, j+=outputFrameSize) {
                    // copy the first two samples in every frame:
                    System.arraycopy(inputBytes, i, b, j, outputFrameSize);
                }
            } else if (outputMode == AudioPlayer.LEFT_ONLY) {
                if (!getFormat().getEncoding().equals(Encoding.PCM_SIGNED)) {
                    throw new IllegalArgumentException("Channel muting supported only for PCM_SIGNED encoding, got "+getFormat().getEncoding());
                }
                for(int i=0, j=off; i<nInputBytes; i+=frameSize, j+=outputFrameSize) {
                    for (int k=0; k<sampleSizeInBytes; k++) {
                        b[j+k] = inputBytes[i+k];
                        b[j+sampleSizeInBytes+k] = 0;
                    }
                }
            } else {
                assert outputMode == AudioPlayer.RIGHT_ONLY : "Unexpected output mode: "+outputMode;
                if (!getFormat().getEncoding().equals(Encoding.PCM_SIGNED)) {
                    throw new IllegalArgumentException("Channel muting supported only for PCM_SIGNED encoding, got "+getFormat().getEncoding());
                }
                for(int i=0, j=off; i<nInputBytes; i+=frameSize, j+=outputFrameSize) {
                    for (int k=0; k<sampleSizeInBytes; k++) {
                        b[j+k] = 0;
                        b[j+sampleSizeInBytes+k] = inputBytes[i+sampleSizeInBytes+k];
                    }
                }
                
            }
        }
        
        
        return 2*nInputBytes/inputChannels;
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
        return 2*super.skip(n/2*inputChannels); 
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
        int av = super.available();
        if (av <= 0) return av;
        return 2*av/inputChannels;
    }

    public AudioFormat getFormat() {
        return newFormat;
    }

}
