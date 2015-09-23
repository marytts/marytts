/*
 *	AudioRecorder.java
 *
 *	This file is part of jsresources.org
 */

/*
 * Portions Copyright (C) 2007 DFKI GmbH.
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

package org.jsresources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

// IDEA: recording format vs. storage format; possible conversion?
/**
 * <p>
 * AudioRecorder: Recording to an audio file (advanced version)
 * </p>
 * 
 * <p>
 * Purpose: This program opens two lines: one for recording and one for playback. In an infinite loop, it reads data from the
 * recording line and writes them to the playback line. You can use this to measure the delays inside Java Sound: Speak into the
 * microphone and wait untill you hear yourself in the speakers. This can be used to experience the effect of changing the buffer
 * sizes: use the '-e' and '-i' options. You will notice that the delays change, too.
 * </p>
 * 
 * <p>
 * Usage: java
 * <ul>
 * <li>AudioRecorder -l
 * <li>java AudioRecorder [-M &lt;mixername&gt;] [-e &lt;buffersize&gt;] [-i &lt;buffersize&gt;] &lt;audiofile&gt;
 * </ul>
 * 
 * <p>
 * Parameters
 * <ul>
 * <li>-l: lists the available mixers
 * <li>-M &lt;mixername&gt;: selects a mixer to play on
 * <li>-e &lt;buffersize&gt;: the buffer size to use in the application ("extern")
 * <li>-i &lt;buffersize&gt;: the buffer size to use in Java Sound ("intern")
 * </ul>
 * 
 * <p>
 * Bugs, limitations: There is no way to stop the program besides brute force (ctrl-C). There is no way to set the audio quality.
 * </p>
 * 
 * <p>
 * Source code <a href="AudioRecorder.java.html">AudioRecorder.java</a>, <a href="AudioCommon.java.html">AudioCommon.java</a>, <a
 * href="http://www.urbanophile.com/arenn/hacking/download.html">gnu.getopt.Getopt</a>
 * </p>
 */
public class AudioRecorder {
	private static final SupportedFormat[] SUPPORTED_FORMATS = {
			new SupportedFormat("s8", AudioFormat.Encoding.PCM_SIGNED, 8, true),
			new SupportedFormat("u8", AudioFormat.Encoding.PCM_UNSIGNED, 8, true),
			new SupportedFormat("s16_le", AudioFormat.Encoding.PCM_SIGNED, 16, false),
			new SupportedFormat("s16_be", AudioFormat.Encoding.PCM_SIGNED, 16, true),
			new SupportedFormat("u16_le", AudioFormat.Encoding.PCM_UNSIGNED, 16, false),
			new SupportedFormat("u16_be", AudioFormat.Encoding.PCM_UNSIGNED, 16, true),
			new SupportedFormat("s24_le", AudioFormat.Encoding.PCM_SIGNED, 24, false),
			new SupportedFormat("s24_be", AudioFormat.Encoding.PCM_SIGNED, 24, true),
			new SupportedFormat("u24_le", AudioFormat.Encoding.PCM_UNSIGNED, 24, false),
			new SupportedFormat("u24_be", AudioFormat.Encoding.PCM_UNSIGNED, 24, true),
			new SupportedFormat("s32_le", AudioFormat.Encoding.PCM_SIGNED, 32, false),
			new SupportedFormat("s32_be", AudioFormat.Encoding.PCM_SIGNED, 32, true),
			new SupportedFormat("u32_le", AudioFormat.Encoding.PCM_UNSIGNED, 32, false),
			new SupportedFormat("u32_be", AudioFormat.Encoding.PCM_UNSIGNED, 32, true), };

	private static final String DEFAULT_FORMAT = "s16_le";
	private static final int DEFAULT_CHANNELS = 2;
	private static final float DEFAULT_RATE = 44100.0F;
	private static final AudioFileFormat.Type DEFAULT_TARGET_TYPE = AudioFileFormat.Type.WAVE;

	private static boolean sm_bDebug = false;

	/**
	 * TODO:
	 * 
	 * @param args
	 *            args
	 */
	public static void main(String[] args) {
		/*
		 * Parsing of command-line options takes place...
		 */
		String strMixerName = null;
		int nInternalBufferSize = AudioSystem.NOT_SPECIFIED;
		String strFormat = DEFAULT_FORMAT;
		int nChannels = DEFAULT_CHANNELS;
		float fRate = DEFAULT_RATE;
		String strExtension = null;
		boolean bDirectRecording = true;
		int millis = 0; // if != null, the number of milliseconds to record

		/*
		 * Parsing of command-line options takes place...
		 */

		if (args.length < 1)
			printUsageAndExit();

		// All but the last args are options:
		String strFilename = null;
		for (int i = 0; i < args.length; i++) {
			if (!args[i].startsWith("-")) {
				// Only the last arg is allowed to be the filename
				if (i == args.length - 1) {
					strFilename = args[i];
					continue;
				} else {
					printUsageAndExit();
				}
			}
			if (args[i].length() != 2)
				printUsageAndExit();
			switch (args[i].charAt(1)) {
			case 'h':
				printUsageAndExit();

			case 'l':
				AudioCommon.listMixersAndExit();

			case 'L':
				listTargetDataLines();
				System.exit(0);

			case 'M':
				strMixerName = args[++i];
				if (sm_bDebug) {
					out("AudioRecorder.main(): mixer name: " + strMixerName);
				}
				break;

			case 'i':
				nInternalBufferSize = Integer.parseInt(args[++i]);
				break;

			case 'f':
				strFormat = args[++i].toLowerCase();
				break;

			case 'c':
				nChannels = Integer.parseInt(args[++i]);
				break;

			case 'r':
				fRate = Float.parseFloat(args[++i]);
				break;

			case 't':
				strExtension = args[++i];
				break;

			case 'D':
				sm_bDebug = true;
				AudioCommon.setDebug(true);
				out("AudioRecorder.main(): enabled debug messages");
				break;

			case 'd':
				bDirectRecording = true;
				out("AudioRecorder.main(): using direct recording");
				break;

			case 'b':
				bDirectRecording = false;
				out("AudioRecorder.main(): using buffered recording");
				break;

			case '?':
				printUsageAndExit();

			case 'T':
				millis = Integer.parseInt(args[++i]);
				break;

			default:
				out("unknown option: " + args[i]);
				printUsageAndExit();
			}
		}

		if (sm_bDebug) {
			out("AudioRecorder.main(): output filename: " + strFilename);
		}
		if (strFilename == null) {
			printUsageAndExit();
		}

		File outputFile = new File(strFilename);

		/*
		 * For convenience, we have some shortcuts to set the properties needed for constructing an AudioFormat.
		 */
		if (strFormat.equals("phone")) {
			// 8 kHz, 8 bit unsigned, mono
			fRate = 8000.0F;
			strFormat = "u8";
			nChannels = 1;
		} else if (strFormat.equals("radio")) {
			// 22.05 kHz, 16 bit signed, mono
			fRate = 22050.0F;
			strFormat = "s16_le";
			nChannels = 1;
		} else if (strFormat.equals("cd")) {
			// 44.1 kHz, 16 bit signed, stereo, little-endian
			fRate = 44100.0F;
			strFormat = "s16_le";
			nChannels = 2;
		} else if (strFormat.equals("dat")) {
			// 48 kHz, 16 bit signed, stereo, little-endian
			fRate = 48000.0F;
			strFormat = "s16_le";
			nChannels = 2;
		}

		/*
		 * Here, we are constructing the AudioFormat to use for the recording. Sample rate (fRate) and number of channels
		 * (nChannels) are already set safely, since they have default values set at the very top. The other properties needed for
		 * AudioFormat are derived from the 'format' specification (strFormat).
		 */
		int nOutputFormatIndex = -1;
		for (int i = 0; i < SUPPORTED_FORMATS.length; i++) {
			if (SUPPORTED_FORMATS[i].getName().equals(strFormat)) {
				nOutputFormatIndex = i;
				break;
			}
		}
		/*
		 * If we haven't found the format (string) requested by the user, we switch to a default format.
		 */
		if (nOutputFormatIndex == -1) {
			out("warning: output format '" + strFormat + "' not supported; using default output format '" + DEFAULT_FORMAT + "'");
			/*
			 * This is the index of "s16_le". Yes, it's a bit quick & dirty to hardcode the index here.
			 */
			nOutputFormatIndex = 2;
		}
		AudioFormat.Encoding encoding = SUPPORTED_FORMATS[nOutputFormatIndex].getEncoding();
		;
		int nBitsPerSample = SUPPORTED_FORMATS[nOutputFormatIndex].getSampleSize();
		boolean bBigEndian = SUPPORTED_FORMATS[nOutputFormatIndex].getBigEndian();
		int nFrameSize = (nBitsPerSample / 8) * nChannels;
		AudioFormat audioFormat = new AudioFormat(encoding, fRate, nBitsPerSample, nChannels, nFrameSize, fRate, bBigEndian);
		if (sm_bDebug) {
			out("AudioRecorder.main(): target audio format: " + audioFormat);
		}

		// extension
		// TODO:

		AudioFileFormat.Type targetType = null;
		if (strExtension == null) {
			/*
			 * The user chose not to specify a target audio file type explicitely. We are trying to guess the type from the target
			 * file name extension.
			 */
			int nDotPosition = strFilename.lastIndexOf('.');
			if (nDotPosition != -1) {
				strExtension = strFilename.substring(nDotPosition + 1);
			}
		}
		if (strExtension != null) {
			targetType = AudioCommon.findTargetType(strExtension);
			if (targetType == null) {
				out("target type '" + strExtension + "' is not supported.");
				out("using default type '" + DEFAULT_TARGET_TYPE.getExtension() + "'");
				targetType = DEFAULT_TARGET_TYPE;
			}
		} else {
			out("target type is neither specified nor can be guessed from the target file name.");
			out("using default type '" + DEFAULT_TARGET_TYPE.getExtension() + "'");
			targetType = DEFAULT_TARGET_TYPE;
		}
		if (sm_bDebug) {
			out("AudioRecorder.main(): target audio file format type: " + targetType);
		}

		TargetDataLine targetDataLine = null;
		targetDataLine = AudioCommon.getTargetDataLine(strMixerName, audioFormat, nInternalBufferSize);
		if (targetDataLine == null) {
			out("can't get TargetDataLine, exiting.");
			System.exit(1);
		}

		Recorder recorder = null;
		if (millis > 0) { // forces buffering recorder
			recorder = new BufferingRecorder(targetDataLine, targetType, outputFile, millis);
		} else if (bDirectRecording) {
			recorder = new DirectRecorder(targetDataLine, targetType, outputFile);
		} else {
			recorder = new BufferingRecorder(targetDataLine, targetType, outputFile, 0);
		}
		if (sm_bDebug) {
			out("AudioRecorder.main(): Recorder: " + recorder);
		}

		out("Press ENTER to start the recording.");
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		recorder.start();
		out("Recording...");
		out("Press ENTER to stop the recording.");
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		recorder.stopRecording();
		out("Recording stopped.");
		// System.exit(0);
	}

	private static void listTargetDataLines() {
		out("Available Mixers:");
		Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
		for (int i = 0; i < aInfos.length; i++) {
			Mixer mixer = AudioSystem.getMixer(aInfos[i]);
			// mixer.open();
			Line.Info[] lines = mixer.getTargetLineInfo();
			out(aInfos[i].getName());
			for (int j = 0; j < lines.length; j++) {
				out("  " + lines[j].toString());
				if (lines[j] instanceof DataLine.Info) {
					AudioFormat[] formats = ((DataLine.Info) lines[j]).getFormats();
					for (int k = 0; k < formats.length; k++) {
						out("    " + formats[k].toString());
					}
				}
			}
		}
	}

	private static void printUsageAndExit() {
		out("AudioRecorder: usage:");
		out("\tjava AudioRecorder -l");
		out("\tjava AudioRecorder -L");
		out("\tjava AudioRecorder [-f <format>] [-c <numchannels>] [-r <samplingrate>] [-t <targettype>] [-M <mixername>] <soundfile>");
		System.exit(0);
	}

	/**
	 * TODO:
	 * 
	 * @param strMessage
	 *            strMessage
	 */
	private static void out(String strMessage) {
		System.out.println(strMessage);
	}

	// /////////// inner classes ////////////////////

	/**
	 * TODO:
	 */
	private static class SupportedFormat {
		/**
		 * The name of the format.
		 */
		private String m_strName;

		/**
		 * The encoding of the format.
		 */
		private AudioFormat.Encoding m_encoding;

		/**
		 * The sample size of the format. This value is in bits for a single sample (not for a frame).
		 */
		private int m_nSampleSize;

		/**
		 * The endianess of the format.
		 */
		private boolean m_bBigEndian;

		// sample size is in bits
		/**
		 * Construct a new supported format.
		 * 
		 * @param strName
		 *            the name of the format.
		 * @param encoding
		 *            the encoding of the format.
		 * @param nSampleSize
		 *            the sample size of the format, in bits.
		 * @param bBigEndian
		 *            the endianess of the format.
		 */
		public SupportedFormat(String strName, AudioFormat.Encoding encoding, int nSampleSize, boolean bBigEndian) {
			m_strName = strName;
			m_encoding = encoding;
			m_nSampleSize = nSampleSize;
		}

		/**
		 * @return the name of the format.
		 */
		public String getName() {
			return m_strName;
		}

		/**
		 * @return the encoding of the format.
		 */
		public AudioFormat.Encoding getEncoding() {
			return m_encoding;
		}

		/**
		 * @return the sample size of the format. This value is in bits.
		 */
		public int getSampleSize() {
			return m_nSampleSize;
		}

		/**
		 * @return the endianess of the format.
		 */
		public boolean getBigEndian() {
			return m_bBigEndian;
		}
	}

	// /////////////////////////////////////////////

	public static interface Recorder {
		public void start();

		public void stopRecording();
	}

	public static class AbstractRecorder extends Thread implements Recorder {
		protected TargetDataLine m_line;
		protected AudioFileFormat.Type m_targetType;
		protected File m_file;
		protected boolean m_bRecording;

		public AbstractRecorder(TargetDataLine line, AudioFileFormat.Type targetType, File file) {
			m_line = line;
			m_targetType = targetType;
			m_file = file;
		}

		/**
		 * Starts the recording. To accomplish this, (i) the line is started and (ii) the thread is started.
		 */
		public void start() {
			m_line.start();
			super.start();
		}

		public void stopRecording() {
			m_line.drain();
			m_line.stop();
			m_line.close();
			m_bRecording = false;
		}

		public void stopRecordingNOW() {
			// don't drain, whatever data is in the buffer will not be retained
			m_line.stop();
			m_line.close();
			m_bRecording = false;
		}

	}

	public static class DirectRecorder extends AbstractRecorder {
		private AudioInputStream m_audioInputStream;

		public DirectRecorder(TargetDataLine line, AudioFileFormat.Type targetType, File file) {
			super(line, targetType, file);
			m_audioInputStream = new AudioInputStream(line);
		}

		public void run() {
			try {
				if (sm_bDebug) {
					out("before AudioSystem.write");
				}
				AudioSystem.write(m_audioInputStream, m_targetType, m_file);
				if (sm_bDebug) {
					out("after AudioSystem.write");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public static class BufferingRecorder extends AbstractRecorder {
		private int millis;

		public BufferingRecorder(TargetDataLine line, AudioFileFormat.Type targetType, File file, int millis) {
			super(line, targetType, file);
			this.millis = millis; // millis: if > 0, number of milliseconds to record
		}

		public void run() {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			OutputStream outputStream = byteArrayOutputStream;
			// TODO: intelligent size
			byte[] abBuffer = new byte[65532]; // a multiple of 4 and of 6, to support 16- and 24-bit stereo as well
			AudioFormat format = m_line.getFormat();
			int nFrameSize = format.getFrameSize();
			long totalBytesToRead = (long) (millis * format.getFrameRate() * nFrameSize / 1000);
			if (totalBytesToRead % nFrameSize != 0) {
				totalBytesToRead += nFrameSize - totalBytesToRead % nFrameSize;
			}
			long totalBytes = 0;
			m_bRecording = true;
			while (m_bRecording) {
				int bytesToRead = abBuffer.length;
				if (totalBytesToRead > 0 && totalBytes + abBuffer.length > totalBytesToRead) {
					bytesToRead = (int) (totalBytesToRead - totalBytes);
				}
				if (sm_bDebug) {
					out("BufferingRecorder.run(): trying to read: " + bytesToRead);
				}
				int nBytesRead = m_line.read(abBuffer, 0, bytesToRead);
				totalBytes += nBytesRead;
				if (totalBytesToRead > 0 && totalBytes >= totalBytesToRead) {
					m_bRecording = false; // read all we needed
				}
				if (sm_bDebug) {
					out("BufferingRecorder.run(): read: " + nBytesRead);
				}
				try {
					outputStream.write(abBuffer, 0, nBytesRead);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			/*
			 * We close the ByteArrayOutputStream.
			 */
			try {
				byteArrayOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			byte[] abData = byteArrayOutputStream.toByteArray();
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(abData);

			AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, format, abData.length
					/ format.getFrameSize());
			try {
				AudioSystem.write(audioInputStream, m_targetType, m_file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

/*** AudioRecorder.java ***/
