/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.util.data.audio;

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

import marytts.util.data.BufferedDoubleDataSource;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class MaryAudioUtils {

	/**
	 * Create a single AudioInputStream from a vector of AudioInputStreams. The AudioInputStreams are expected to have the same
	 * AudioFormat.
	 * 
	 * @param audioInputStreams
	 *            a vector containing one or more AudioInputStreams.
	 * @return a single AudioInputStream
	 * @throws NullPointerException
	 *             if any of the arguments are null
	 * @throws IllegalArgumentException
	 *             if the vector contains no elements, any element is not an AudioInputStream.
	 */
	public static AudioInputStream createSingleAudioInputStream(Vector<AudioInputStream> audioInputStreams) {
		if (audioInputStreams == null)
			throw new NullPointerException("Received null vector of AudioInputStreams");
		if (audioInputStreams.isEmpty())
			throw new IllegalArgumentException("Received empty vector of AudioInputStreams");
		AudioInputStream singleStream;
		if (audioInputStreams.size() == 1)
			singleStream = (AudioInputStream) audioInputStreams.get(0);
		else {
			AudioFormat audioFormat = ((AudioInputStream) audioInputStreams.get(0)).getFormat();
			singleStream = new SequenceAudioInputStream(audioFormat, audioInputStreams);
		}
		return singleStream;
	}

	/**
	 * Return an audio file format type for the given string. In addition to the built-in types, this can deal with MP3 supported
	 * by tritonus.
	 * 
	 * @param name
	 *            name
	 * @return the audio file format type if it is known, or null.
	 */
	public static AudioFileFormat.Type getAudioFileFormatType(String name) {
		AudioFileFormat.Type at;
		if (name.equals("MP3")) {
			// Supported by tritonus plugin
			at = new AudioFileFormat.Type("MP3", "mp3");
		} else if (name.equals("Vorbis")) {
			// supported by tritonus plugin
			at = new AudioFileFormat.Type("Vorbis", "ogg");
		} else {
			try {
				at = (AudioFileFormat.Type) AudioFileFormat.Type.class.getField(name).get(null);
			} catch (Exception e) {
				return null;
			}
		}

		return at;
	}

	/**
	 * Record a sound file with the recording being limited to a given amount of time
	 * 
	 * @param filename
	 *            name of the sound file
	 * @param millis
	 *            the given amount of time in milliseconds
	 * @param audioFormat
	 *            the audio format for the actual sound file
	 */
	public static void timedRecord(String filename, long millis, AudioFormat audioFormat) {
		/*
		 * Our first parameter tells us the file name that the recording should be saved into.
		 */
		File outputFile = new File(filename);
		timedRecord(outputFile, millis, audioFormat);
	}

	/**
	 * Record a sound file with the recording being limited to a given amount of time
	 * 
	 * @param targetFile
	 *            name of the sound file
	 * @param millis
	 *            the given amount of time in milliseconds
	 * @param audioFormat
	 *            the audio format for the actual sound file
	 */
	public static void timedRecord(File targetFile, long millis, AudioFormat audioFormat) {
		/*
		 * Now, we are trying to get a TargetDataLine. The TargetDataLine is used later to read audio data from it. If requesting
		 * the line was successful, we are opening it (important!).
		 */
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
		TargetDataLine targetDataLine = null;
		try {
			targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
			targetDataLine.open(audioFormat);
		} catch (LineUnavailableException e) {
			System.err.println("unable to get a recording line");
			e.printStackTrace();
			// System.exit(1);
		}

		/*
		 * Again for simplicity, we've hardcoded the audio file type, too.
		 */
		AudioFileFormat.Type targetType = AudioFileFormat.Type.WAVE;

		AudioRecorder.BufferingRecorder recorder = new AudioRecorder.BufferingRecorder(targetDataLine, targetType, targetFile,
				(int) millis);

		/*
		 * Here, the recording is actually started.
		 */
		recorder.start();
		System.out.println("Recording...");

		try {
			recorder.join();
		} catch (InterruptedException ie) {
		}
		System.out.println("Recording stopped.");

		/*
		 * Here, our recording should actually be done and all wrapped up.
		 */

	}

	/**
	 * Play back and stop a given wav file.
	 *
	 */
	static Clip m_clip;

	/**
	 * Play back a file loop times (0 = only once). Play in the background, non-blocking.
	 * 
	 * @param filename
	 *            name of the wav file
	 * @param loop
	 *            number of times the file should be repeated (0 = play only once).
	 */
	public static void playWavFile(String filename, int loop) {
		playWavFile(filename, loop, false);
	}

	/**
	 * Play back a file loop times (0 = only once). Play in the background, non-blocking.
	 * 
	 * @param filename
	 *            name of the wav file
	 * @param loop
	 *            number of times the file should be repeated (0 = play only once).
	 * @param waitUntilCompleted
	 *            whether or not to wait until the file has finished playing before returning.
	 */
	public static void playWavFile(String filename, int loop, boolean waitUntilCompleted) {
		AudioInputStream audioInputStream = null;
		File clipFile = new File(filename);

		try {
			audioInputStream = AudioSystem.getAudioInputStream(clipFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (audioInputStream != null) {
			AudioFormat format = audioInputStream.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format);
			try {
				m_clip = (Clip) AudioSystem.getLine(info);
				m_clip.open(audioInputStream);
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			m_clip.loop(loop);
			if (waitUntilCompleted)
				m_clip.drain();
		} else {
			System.out.println("playWavFile<init>(): can't get data from file " + clipFile.getName());
		}

	}

	/**
	 * Stop wav play back
	 * 
	 */
	public static void stopWavFile() {
		m_clip.stop();
		m_clip.flush();
		m_clip.close();
	}

	public static double[] getSamplesAsDoubleArray(AudioInputStream ais) {
		return new AudioDoubleDataSource(ais).getAllData();
	}

	public static void writeWavFile(double[] x, String outputFile, AudioFormat format) throws IOException {
		DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(x), format);

		AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outputFile));
	}
}
