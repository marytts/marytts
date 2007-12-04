/**
 * Copyright 2000-2006 DFKI GmbH.
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

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.jsresources.AudioRecorder;
import org.jsresources.SequenceAudioInputStream;
import org.jsresources.TimedAudioRecorder;
import org.tritonus.share.sampled.AudioFileTypes;
import org.tritonus.share.sampled.Encodings;

import de.dfki.lt.mary.modules.synthesis.Voice;


/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class MaryAudioUtils {

	/**
      * Create a single AudioInputStream from a vector of AudioInputStreams.
      * The AudioInputStreams are expected to have the same AudioFormat.
      * @param audioInputStreams a vector containing one or more AudioInputStreams.
      * @return a single AudioInputStream
      * @throws NullPointerException if any of the arguments are null
      * @throws IllegalArgumentException if the vector contains no elements,
      * any element is not an AudioInputStream.
      */
     public static AudioInputStream createSingleAudioInputStream(Vector audioInputStreams) {
         if (audioInputStreams == null)
             throw new NullPointerException("Received null vector of AudioInputStreams");
         if (audioInputStreams.isEmpty())
             throw new IllegalArgumentException("Received empty vector of AudioInputStreams");
         AudioInputStream singleStream;
         if (audioInputStreams.size() == 1)
             singleStream = (AudioInputStream) audioInputStreams.get(0);
         else {
             AudioFormat audioFormat =
                 ((AudioInputStream) audioInputStreams.get(0)).getFormat();
             singleStream =
                 new SequenceAudioInputStream(audioFormat, audioInputStreams);
         }
         return singleStream;
     }

    /**
     * Return an audio file format type for the given string.
     * In addition to the built-in types, this can deal with MP3
     * supported by tritonus.
     */
    public static AudioFileFormat.Type getAudioFileFormatType(String name)
    {
        AudioFileFormat.Type at;
        if (name.equals("MP3")) {
            // Supported by tritonus plugin
            at = AudioFileTypes.getType("MP3", "mp3");
        } else if (name.equals("Vorbis")) {
            // supported by tritonus plugin
            at = new AudioFileFormat.Type("Vorbis", "ogg");
        } else {
            try {
                at = (AudioFileFormat.Type) AudioFileFormat.Type.class.getField(name).get(null);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid audio type: " + name);
            }
        }
        return at;
    }

    /**
     * Determine whether conversion to mp3 is possible.
     *
     */
    public static boolean canCreateMP3()
    {
        return AudioSystem.isConversionSupported(getMP3AudioFormat(), Voice.AF22050);
    }
    
    public static AudioFormat getMP3AudioFormat()
    {
        return new AudioFormat(
            Encodings.getEncoding("MPEG1L3"),
            AudioSystem.NOT_SPECIFIED,
            AudioSystem.NOT_SPECIFIED,
            1,
            AudioSystem.NOT_SPECIFIED,
            AudioSystem.NOT_SPECIFIED,
            false);
            //endianness doesn't matter
    }
    
    /**
     * Determine whether conversion to ogg vorbis format is possible.
     */
    public static boolean canCreateOgg()
    {
        return AudioSystem.isConversionSupported(getOggAudioFormat(), Voice.AF22050);
    }
    
    public static AudioFormat getOggAudioFormat()
    {
        return new AudioFormat(
                new AudioFormat.Encoding("VORBIS"),
                AudioSystem.NOT_SPECIFIED,
                AudioSystem.NOT_SPECIFIED,
                1,
                AudioSystem.NOT_SPECIFIED,
                AudioSystem.NOT_SPECIFIED,
                false);
    }
    
    /**
     * Record a sound file with the recording being limited to a given amount of time
     * @param filename name of the sound file
     * @param millis the given amount of time in milliseconds
     * @param audioFormat the audio format for the actual sound file
     * @return void
     * @throws LineUnavailableException if no recording line can be found
     * @throws InterruptedException if the recording is stopped
     *
     */
    public static void timedRecord(String filename,long millis, AudioFormat audioFormat) 
    {
    	/*
    	 * Our first parameter tells us the file name that the recording
    	 * should be saved into.
    	 */
    	File outputFile = new File(filename);
        timedRecord(outputFile, millis, audioFormat);
    }
    
    /**
     * Record a sound file with the recording being limited to a given amount of time
     * @param filename name of the sound file
     * @param millis the given amount of time in milliseconds
     * @param audioFormat the audio format for the actual sound file
     * @throws LineUnavailableException if no recording line can be found
     * @throws InterruptedException if the recording is stopped
     *
     */
    public static void timedRecord(File targetFile, long millis, AudioFormat audioFormat)
    {
    	/*
    	 * Now, we are trying to get a TargetDataLine. The TargetDataLine is
    	 * used later to read audio data from it. If requesting the line was
    	 * successful, we are opening it (important!).
    	 */
    	DataLine.Info info = new DataLine.Info(TargetDataLine.class,
            audioFormat);
    	TargetDataLine targetDataLine = null;
    	try {
    		targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
    		targetDataLine.open(audioFormat);
    	} catch (LineUnavailableException e) {
    		System.err.println("unable to get a recording line");
    		e.printStackTrace();
    		//System.exit(1);
    	}

    	/*
    	 * Again for simplicity, we've hardcoded the audio file type, too.
    	 */
    	AudioFileFormat.Type targetType = AudioFileFormat.Type.WAVE;

        AudioRecorder.BufferingRecorder recorder = new AudioRecorder.BufferingRecorder(targetDataLine, targetType, targetFile, (int) millis);

    	/*
    	 * Here, the recording is actually started.
    	 */
    	recorder.start();
    	System.out.println("Recording...");

    	try {
    		recorder.join();
    	} catch (InterruptedException ie) {}
    	System.out.println("Recording stopped.");
    	
    	/* 
    	 * Here, our recording should actually be done and all wrapped up.
    	 *
    	 */
    
    }
    
    /**
     * Play back and stop a given wav file.
     *
     */
    static Clip m_clip;
    
    /**
     * Play back a file loop times (0 = only once). Play in the background, non-blocking.
     * @param filename name of the wav file
     * @param loop number of times the file should be repeated (0 = play only once).
     * @throws IOException,LineUnavailableException
     * 
     */
    public static void playWavFile(String filename, int loop)
    {
        playWavFile(filename, loop, false);
    }
 
    /**
     * Play back a file loop times (0 = only once). Play in the background, non-blocking.
     * @param filename name of the wav file
     * @param loop number of times the file should be repeated (0 = play only once).
     * @param waitUntilCompleted whether or not to wait until the file has finished playing before returning.
     * @throws IOException,LineUnavailableException
     * 
     */
    public static void playWavFile(String filename, int loop, boolean waitUntilCompleted) 
    {
    	AudioInputStream 	audioInputStream = null;
    	File clipFile = new File(filename);
    	
    	try
		{
    		audioInputStream = AudioSystem.getAudioInputStream(clipFile);
		}
    	catch (Exception e)
		{
    		e.printStackTrace();
		}
    	if (audioInputStream != null)
    	{
    		AudioFormat	format = audioInputStream.getFormat();
    		DataLine.Info	info = new DataLine.Info(Clip.class, format);
    		try
			{
    			m_clip = (Clip) AudioSystem.getLine(info);
    			m_clip.open(audioInputStream);
			}
    		catch (LineUnavailableException e)
			{
    			e.printStackTrace();
			}
    		catch (IOException e)
			{
    			e.printStackTrace();
			}
    		m_clip.loop(loop);
            if (waitUntilCompleted)
                m_clip.drain();
    	}
    	else
    	{
    		System.out.println("playWavFile<init>(): can't get data from file " + clipFile.getName());
		}
  
	}
    
    /**
     * Stop wav play back
     * 
     */
    public static void stopWavFile()
    {
    	m_clip.stop();
    	m_clip.flush();
    	m_clip.close();  	
    }


}
