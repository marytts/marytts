/**
 * Portions Copyright 2004 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
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
package de.dfki.lt.mary.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.sun.speech.freetts.audio.AudioPlayer;
import com.sun.speech.freetts.util.BulkTimer;
import com.sun.speech.freetts.util.Utilities;


/**
 * Provides an implementation of <code>AudioPlayer</code> that sends
 * all audio data to the bit bucket. The <code>ByteArrayAudioPlayer</code>
 * is a helper, targeted at obtaining a byte array from the audio stream.
 */
public class ByteArrayAudioPlayer implements AudioPlayer {

    private float volume = 1.0f;
    private AudioFormat audioFormat;
    private final static boolean TRACE 
	= Utilities.getBoolean("com.sun.speech.freetts.audio.trace");
    private boolean firstSound = true;
    private int totalBytes = 0;
    private ByteArrayOutputStream baos;
    private int totalWrites = 0;
    private BulkTimer timer = new BulkTimer();


    /**
     * Constructs a ByteArrayAudioPlayer
     */
    public ByteArrayAudioPlayer() {
    }
    

    /**
     * Sets the audio format for this player
     *
     * @param format the audio format
     */
    public void setAudioFormat(AudioFormat format) {
	this.audioFormat = format;
    }

    /**
     * Retrieves the audio format for this player
     *
     * @return the current audio format.
     */
    public AudioFormat getAudioFormat() {
	return audioFormat;
    }

    /**
     * Cancels all queued output. Current 'write' call will return
     * false
     *
     */
    public void cancel() {
    }


    /**
     * Pauses the audio output
     */
    public void pause() {
    }


    /**
     * Prepares for another batch of output. Larger groups of output
     * (such as all output associated with a single FreeTTSSpeakable)
     * should be grouped between a reset/drain pair.
     */
    public void reset() {
	timer.start("AudioOutput");
    }


    /**
     * Resumes audio output
     */
    public void resume() {
    }
	



    /**
     * Waits for all audio playback to stop, and closes this AudioPlayer.
     */
    public void close() {
    }
        

    /**
     * Returns the current volume.
     *
     * @return the current volume (between 0 and 1)
     */
    public float getVolume() {
	return volume;
    }	      

    /**
     * Sets the current volume.
     *
     * @param volume  the current volume (between 0 and 1)
     */
    public void setVolume(float volume) {
	this.volume = volume;
    }	      


    /**
     * Writes the given bytes to the audio stream
     *
     * @param audioData array of audio data
     *
     * @return <code>true</code> of the write completed successfully, 
     *       	<code> false </code>if the write was cancelled.
     */
    public boolean write(byte[] audioData) {
	return write(audioData, 0, audioData.length);
    }


    /**
     *  Starts the output of a set of data
     *
     * @param size the size of data between now and the end
     *
     */
    public void begin(int size) {
        baos = new ByteArrayOutputStream(size);
    }

    /**
     *  Marks the end of a set of data
     *
     */
    public boolean  end()  {
	return true;
    }

    /**
     * Writes the given bytes to the audio stream
     *
     * @param bytes audio data to write to the device
     * @param offset the offset into the buffer
     * @param size the size into the buffer
     *
     * @return <code>true</code> of the write completed successfully, 
     *       	<code> false </code>if the write was cancelled.
     */
    public boolean write(byte[] bytes, int offset, int size) {
	   baos.write(bytes, offset, size);
    totalBytes += size;
	totalWrites ++;
	if (firstSound) {
	    timer.stop("AudioFirstSound");
	    firstSound = false;
	    if (TRACE) {
		timer.show("ByteArray Trace");
	    }
	}
	if (TRACE) {
	    System.out.println("ByteArrayAudio: write " + size + " bytes.");
	}
	return true;
    }

    /**
     * Starts the first sample timer
     */
    public void startFirstSampleTimer() {
	firstSound = true;
	timer.start("AudioFirstSound");
    }

    /**
     * Waits for all queued audio to be played
     *
     * @return <code>true</code> if the audio played to completion,
     *     	<code> false </code>if the audio was stopped
     */
    public boolean drain()  {
	timer.stop("AudioOutput");
	return true;
    }

    /**
     * Gets the amount of played since the last resetTime
     * Currently not supported.
     *
     * @return the amount of audio in milliseconds
     */
    public long getTime()  {
	return -1L;
    }


    /**
     * Resets the audio clock
     */
    public void resetTime() {
    }

    /**
     * Shows metrics for this audio player
     */
    public void showMetrics() {
	timer.show("ByteArrayAudioPlayer");
    }
    
    /**
     * Provide the audio data that has been written to this AudioPlayer since
     * the last call to begin() as a byte array.
     * @return a byte array
     */
    public byte[] getByteArray()
    {
        return baos.toByteArray();
    }
    
    /**
     * Provide the audio data that has been written to this AudioPlayer since
     * the last call to begin() as a byte array.
     */
    public AudioInputStream getAudioInputStream()
    {
        byte[] bytes = getByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        AudioFormat af = getAudioFormat();
        if (af == null) {
            af = new AudioFormat(16000,16, 1, true, true);
        }
        int samplesize = af.getSampleSizeInBits();
        if (samplesize == AudioSystem.NOT_SPECIFIED)
            samplesize = 16; // usually 16 bit data
        long lengthInSamples = bytes.length / (samplesize/8);
        AudioInputStream ais = new AudioInputStream(bais, af, lengthInSamples);
        return ais;
    }
}
