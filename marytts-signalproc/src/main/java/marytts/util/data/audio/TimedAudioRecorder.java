package marytts.util.data.audio;

/*
 *	TimedAudioRecorder.java
 *
 *	This file is derived from SimpleAudioRecorder which is part of jsresources.org
 */

/*
 * Portions Copyright (c) 2004 DFKI GmbH.
 * Portions Copyright (c) 1999 - 2003 by Matthias Pfisterer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 |<---            this code is formatted to fit into 80 columns             --->|
 */

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

/**
 * <p>
 * SimpleAudioRecorder: Recording to an audio file (simple version)
 * </p>
 * 
 * <p>
 * Purpose: Records audio data and stores it in a file. The data is recorded in CD quality (44.1 kHz, 16 bit linear, stereo) and
 * stored in a .wav file.
 * </p>
 * 
 * <p>
 * Usage: java SimpleAudioRecorder choice="plain" -h; java SimpleAudioRecorder choice="plain" audiofile
 * </p>
 * 
 * <p>
 * Parameters:
 * <ul>
 * <li>-h: print usage information, then exit
 * <li>audiofile: the file name of the audio file that should be produced from the recorded data
 * </ul>
 * 
 * <p>
 * Bugs, limitations: You cannot select audio formats and the audio file type on the command line. See AudioRecorder for a version
 * that has more advanced options. Due to a bug in the Sun jdk1.3/1.4, this program does not work with it.
 * </p>
 * 
 * <p>
 * Source code: <a href="SimpleAudioRecorder.java.html">SimpleAudioRecorder.java </a>
 * </p>
 * 
 */
public class TimedAudioRecorder extends Thread {
	private TargetDataLine m_line;

	private AudioFileFormat.Type m_targetType;

	private AudioInputStream m_audioInputStream;

	private File m_outputFile;

	private long timeout;

	public TimedAudioRecorder(TargetDataLine line, AudioFileFormat.Type targetType, File file, long timeout) {
		m_line = line;
		m_audioInputStream = new AudioInputStream(line);
		m_targetType = targetType;
		m_outputFile = file;
		this.timeout = timeout;
	}

	/**
	 * Starts the recording. To accomplish this, (i) the line is started and (ii) the thread is started.
	 */
	public void start() {
		/*
		 * Starting the TargetDataLine. It tells the line that we now want to read data from it. If this method isn't called, we
		 * won't be able to read data from the line at all.
		 */
		m_line.start();

		/*
		 * Starting the thread. This call results in the method 'run()' (see below) being called. There, the data is actually read
		 * from the line.
		 */
		super.start();
	}

	/**
	 * Stops the recording.
	 * 
	 * Note that stopping the thread explicitely is not necessary. Once no more data can be read from the TargetDataLine, no more
	 * data be read from our AudioInputStream. And if there is no more data from the AudioInputStream, the method
	 * 'AudioSystem.write()' (called in 'run()' returns. Returning from 'AudioSystem.write()' is followed by returning from
	 * 'run()', and thus, the thread is terminated automatically.
	 * 
	 * It's not a good idea to call this method just 'stop()' because stop() is a (deprecated) method of the class 'Thread'. And
	 * we don't want to override this method.
	 */
	public void stopRecording() {
		m_line.stop();
		m_line.close();
	}

	/**
	 * Main working method. You may be surprised that here, just 'AudioSystem.write()' is called. But internally, it works like
	 * this: AudioSystem.write() contains a loop that is trying to read from the passed AudioInputStream. Since we have a special
	 * AudioInputStream that gets its data from a TargetDataLine, reading from the AudioInputStream leads to reading from the
	 * TargetDataLine. The data read this way is then written to the passed File. Before writing of audio data starts, a header is
	 * written according to the desired audio file type. Reading continues untill no more data can be read from the
	 * AudioInputStream. In our case, this happens if no more data can be read from the TargetDataLine. This, in turn, happens if
	 * the TargetDataLine is stopped or closed (which implies stopping). (Also see the comment above.) Then, the file is closed
	 * and 'AudioSystem.write()' returns.
	 */
	public void run() {
		final Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				stopRecording();
				timer.cancel();
			}
		}, timeout);
		try {
			AudioSystem.write(m_audioInputStream, m_targetType, m_outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		if (args.length < 3 || args[0].equals("-h")) {
			printUsageAndExit();
		}

		long duration = 0;
		long rate = 16000;
		int channels = 1; // mono
		int i = 0;
		// Go through all arguments except the last one
		while (i < args.length - 1) {
			if (args[i].equals("-dur")) {
				duration = Long.parseLong(args[i + 1]);
				i += 2;
				continue;
			} else if (args[i].equals("-mono")) {
				channels = 1;
				i++;
				continue;
			} else if (args[i].equals("-stereo")) {
				channels = 2;
				i++;
				continue;
			} else if (args[i].equals("-rate")) {
				rate = Long.parseLong(args[i + 1]);
				i += 2;
				continue;
			} else { // unknown option
				printUsageAndExit();
			}
		}
		/*
		 * We have made shure that there is only one command line argument. This is taken as the filename of the soundfile to
		 * store to.
		 */
		String strFilename = args[args.length - 1];
		File outputFile = new File(strFilename);

		AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, channels, 2 * channels, rate, false);

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
			out("unable to get a recording line");
			e.printStackTrace();
			System.exit(1);
		}

		/*
		 * Again for simplicity, we've hardcoded the audio file type, too.
		 */
		AudioFileFormat.Type targetType = AudioFileFormat.Type.WAVE;

		/*
		 * Now, we are creating an SimpleAudioRecorder object. It contains the logic of starting and stopping the recording,
		 * reading audio data from the TargetDataLine and writing the data to a file.
		 */
		TimedAudioRecorder recorder = new TimedAudioRecorder(targetDataLine, targetType, outputFile, duration);

		/*
		 * Here, the recording is actually started.
		 */
		recorder.start();
		out("Recording...");

		try {
			recorder.join();
		} catch (InterruptedException ie) {
		}
		out("Recording stopped.");
	}

	private static void printUsageAndExit() {
		out("TimedAudioRecorder: usage:");
		out("\tjava org.jsresources.TimedAudioRecorder -h");
		out("\tjava org.jsresources.TimeedAudioRecorder -dur <dur> [-rate <samplerate>] [-stereo|-mono] <audiofile>");
		System.exit(0);
	}

	private static void out(String strMessage) {
		System.out.println(strMessage);
	}
}

/** * SimpleAudioRecorder.java ** */

