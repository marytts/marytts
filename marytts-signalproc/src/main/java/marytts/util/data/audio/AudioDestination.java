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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;

import marytts.signalproc.display.FunctionGraph;
import marytts.util.MaryUtils;

public class AudioDestination {

	private OutputStream os;
	private File f;
	private boolean ram;

	/**
	 * Create an AudioDestination to which the audio data can be written.
	 * 
	 * @param isInRam
	 *            whether to hold the data in RAM or write it into a file. The calling code is responsible for administering this
	 *            AudioDestination.
	 * @throws IOException
	 *             if the underlying OutputStream could not be created.
	 */
	public AudioDestination(boolean isInRam) throws IOException {
		this.ram = isInRam;
		if (ram) {
			os = new ByteArrayOutputStream();
			f = null;
		} else {
			f = MaryUtils.createSelfDeletingTempFile(3600);
			os = new FileOutputStream(f);
		}
	}

	public boolean isInRam() {
		return ram;
	}

	public boolean isFile() {
		return !ram;
	}

	public void write(byte[] b) throws IOException {
		os.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		os.write(b, off, len);
	}

	/**
	 * Convert the audio data into an AudioInputStream of the proper AudioFormat.
	 * 
	 * @param audioFormat
	 *            the format of the audio data.
	 * @return an AudioInputStream from which the synthesised audio data can be read.
	 * @throws IOException
	 *             if a problem occurred with the temporary file (only applies when using files as temporary storage).
	 */
	public AudioInputStream convertToAudioInputStream(AudioFormat audioFormat) throws IOException {
		if (ram) {
			assert os instanceof ByteArrayOutputStream;
			assert f == null;
			byte[] audioData = ((ByteArrayOutputStream) os).toByteArray();
			// logger.debug("Total of " + audioData.length + " bytes of audio data for this section.");
			return new AudioInputStream(new ByteArrayInputStream(audioData), audioFormat, audioData.length
					/ audioFormat.getFrameSize());
		} else {
			assert os instanceof FileOutputStream;
			assert f != null;
			os.close();
			long byteLength = f.length();
			return new AudioInputStream(new FileInputStream(f), audioFormat, byteLength / audioFormat.getFrameSize());
		}
	}

	/**
	 * Convert the audio data into an AudioInputStream of the proper AudioFormat. This method assumes that the audio data starts
	 * with a valid audio file header, so the audio format is read from the data.
	 * 
	 * @return an AudioInputStream from which the synthesized audio data can be read.
	 * @throws IOException
	 *             if a problem occurred with the temporary file (only applies when using files as temporary storage).
	 * @throws UnsupportedAudioFileException
	 *             UnsupportedAudioFileException
	 */
	public AudioInputStream convertToAudioInputStream() throws IOException, UnsupportedAudioFileException {
		if (ram) {
			assert os instanceof ByteArrayOutputStream;
			assert f == null;
			byte[] audioData = ((ByteArrayOutputStream) os).toByteArray();
			// logger.debug("Total of " + audioData.length + " bytes of audio data for this section.");
			return AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioData));
		} else {
			assert os instanceof FileOutputStream;
			assert f != null;
			os.close();
			long byteLength = f.length();
			return AudioSystem.getAudioInputStream(f);
		}
	}

	public static void plot(double[] x) {
		plot(x, false);
	}

	public static void plot(double[] x, boolean bAutoClose) {
		plot(x, bAutoClose, 3000);
	}

	// Plots the values in x
	// If bAutoClose is specified, the figure is closed after milliSecondsToClose milliseconds
	// milliSecondsToClose: has no effect if bAutoClose is false
	public static void plot(double[] x, boolean bAutoClose, int milliSecondsToClose) {
		FunctionGraph graph = new FunctionGraph(400, 200, 0, 1, x);
		JFrame frame = graph.showInJFrame("wgt2", 500, 300, true, false);

		if (bAutoClose) {
			try {
				Thread.sleep(milliSecondsToClose);
			} catch (InterruptedException e) {
			}
			frame.dispose();
		}
	}
}
