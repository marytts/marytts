/*
 *	AudioCommon.java
 *
 *	This file is part of jsresources.org
 */

/*
 * Portions Copyright (c) 2007 DFKI GmbH.
 * Portions Copyright (c) 1999 - 2001 by Matthias Pfisterer
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

package marytts.util.data.audio;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * Common methods for audio examples.
 */
public class AudioCommon {
	private static boolean DEBUG = false;

	public static void setDebug(boolean bDebug) {
		DEBUG = bDebug;
	}

	/**
	 * TODO:
	 */
	public static void listSupportedTargetTypes() {
		StringBuilder strMessage = new StringBuilder("Supported target types:");
		AudioFileFormat.Type[] aTypes = AudioSystem.getAudioFileTypes();
		for (int i = 0; i < aTypes.length; i++) {
			strMessage.append(" ").append(aTypes[i].getExtension());
		}
		out(strMessage.toString());
	}

	/**
	 * Trying to get an audio file type for the passed extension. This works by examining all available file types. For each type,
	 * if the extension this type promisses to handle matches the extension we are trying to find a type for, this type is
	 * returned. If no appropriate type is found, null is returned.
	 * 
	 * @param strExtension
	 *            str Extension
	 * @return atypes[i]
	 */
	public static AudioFileFormat.Type findTargetType(String strExtension) {
		AudioFileFormat.Type[] aTypes = AudioSystem.getAudioFileTypes();
		for (int i = 0; i < aTypes.length; i++) {
			if (aTypes[i].getExtension().equals(strExtension)) {
				return aTypes[i];
			}
		}
		return null;
	}

	/**
	 * TODO:
	 */
	public static void listMixersAndExit() {
		out("Available Mixers:");
		Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
		for (int i = 0; i < aInfos.length; i++) {
			out(aInfos[i].getName());
		}
		if (aInfos.length == 0) {
			out("[No mixers available]");
		}
		System.exit(0);
	}

	/**
	 * List Mixers. Only Mixers that support either TargetDataLines or SourceDataLines are listed, depending on the value of
	 * bPlayback.
	 * 
	 * @param bPlayback
	 *            bPlayback
	 */
	public static void listMixersAndExit(boolean bPlayback) {
		out("Available Mixers:");
		Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
		for (int i = 0; i < aInfos.length; i++) {
			Mixer mixer = AudioSystem.getMixer(aInfos[i]);
			Line.Info lineInfo = new Line.Info(bPlayback ? SourceDataLine.class : TargetDataLine.class);
			if (mixer.isLineSupported(lineInfo)) {
				out(aInfos[i].getName());
			}
		}
		if (aInfos.length == 0) {
			out("[No mixers available]");
		}
		System.exit(0);
	}

	/**
	 * TODO: This method tries to return a Mixer.Info whose name matches the passed name. If no matching Mixer.Info is found, null
	 * is returned.
	 * 
	 * @param strMixerName
	 *            str mixer name
	 * @return aInfos[i]
	 */
	public static Mixer.Info getMixerInfo(String strMixerName) {
		Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
		for (int i = 0; i < aInfos.length; i++) {
			if (aInfos[i].getName().equals(strMixerName)) {
				return aInfos[i];
			}
		}
		return null;
	}

	/**
	 * TODO:
	 * 
	 * @param strMixerName
	 *            strMixerName
	 * @param audioFormat
	 *            audio format
	 * @param nBufferSize
	 *            n buffer size
	 * @return target data line
	 */
	public static TargetDataLine getTargetDataLine(String strMixerName, AudioFormat audioFormat, int nBufferSize) {
		/*
		 * Asking for a line is a rather tricky thing. We have to construct an Info object that specifies the desired properties
		 * for the line. First, we have to say which kind of line we want. The possibilities are: SourceDataLine (for playback),
		 * Clip (for repeated playback) and TargetDataLine (for recording). Here, we want to do normal capture, so we ask for a
		 * TargetDataLine. Then, we have to pass an AudioFormat object, so that the Line knows which format the data passed to it
		 * will have. Furthermore, we can give Java Sound a hint about how big the internal buffer for the line should be. This
		 * isn't used here, signaling that we don't care about the exact size. Java Sound will use some default value for the
		 * buffer size.
		 */
		TargetDataLine targetDataLine = null;
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat, nBufferSize);
		try {
			if (strMixerName != null) {
				Mixer.Info mixerInfo = getMixerInfo(strMixerName);
				if (mixerInfo == null) {
					out("AudioCommon.getTargetDataLine(): mixer not found: " + strMixerName);
					return null;
				}
				Mixer mixer = AudioSystem.getMixer(mixerInfo);
				targetDataLine = (TargetDataLine) mixer.getLine(info);
			} else {
				if (DEBUG) {
					out("AudioCommon.getTargetDataLine(): using default mixer");
				}
				targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
			}

			/*
			 * The line is there, but it is not yet ready to receive audio data. We have to open the line.
			 */
			if (DEBUG) {
				out("AudioCommon.getTargetDataLine(): opening line...");
			}
			targetDataLine.open(audioFormat, nBufferSize);
			if (DEBUG) {
				out("AudioCommon.getTargetDataLine(): opened line");
			}
		} catch (LineUnavailableException e) {
			if (DEBUG) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			if (DEBUG) {
				e.printStackTrace();
			}
		}
		if (DEBUG) {
			out("AudioCommon.getTargetDataLine(): returning line: " + targetDataLine);
		}
		return targetDataLine;
	}

	/**
	 * Checks if the encoding is PCM.
	 * 
	 * @param encoding
	 *            encoding
	 * @return encoding equals audioformat encoding pcm_signed or pcm_unsigned
	 */
	public static boolean isPcm(AudioFormat.Encoding encoding) {
		return encoding.equals(AudioFormat.Encoding.PCM_SIGNED) || encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED);
	}

	/**
	 * TODO:
	 * 
	 * @param strMessage
	 *            str message
	 */
	private static void out(String strMessage) {
		System.out.println(strMessage);
	}

}

/*** AudioCommon.java ***/
