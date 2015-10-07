/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.tools.redstart;

import java.io.File;

import javax.sound.sampled.SourceDataLine;

import marytts.util.data.audio.AudioPlayer;

/**
 * 
 * @author Mat Wilson &lt;mwilson@dfki.de&gt;
 */
public class Speech {

	// ______________________________________________________________________
	// Instance fields

	int duration; // Duration of most recent speech file
	int fileCount; // Number of files
	File filePath; // Path and filename of speech file(s)
	String basename; // Basename for the associated speech file(s)

	// ______________________________________________________________________
	// Class fields
	private static AudioPlayer audioPlayer;

	// ______________________________________________________________________
	// Instance methods

	/**
	 * Determines how many recordings a prompt has
	 * 
	 */
	public void updateFileCount() {
		// Note: This method doesn't increment the file count; it only asks the Speech object to initiate
		// an update. Therefore public access should be okay.

		File[] fileList = this.filePath.listFiles(); // List of files in wav folder
		int fileListLength = fileList.length; // Number of files in wav folder

		int count = 0;

		if (fileList != null) {
			for (int i = 0; i < fileListLength; i++) {
				File wavFile = fileList[i];

				// Only consider files, not folders
				if (wavFile.isFile()) {

					// If filename contains basename
					if (wavFile.getName().indexOf(this.basename) != -1) {
						count++;
					}

				} // wavFile
			} // i
		} // fileList

		// TEST CODE
		Test.output("Files for " + this.basename + ": " + count);

		this.fileCount = count;
	}

	/**
	 * Gets duration of the speech file
	 * 
	 * @return The duration of the speech file (in milliseconds)
	 */
	public int getDuration() {
		return duration;
	}

	/**
	 * Gets the file path for the speech file
	 * 
	 * @return The file path for the current speech file
	 */
	public File getFilePath() {
		return filePath;
	}

	public void setFilePath(File newPath) {
		this.filePath = newPath;
	}

	public String getBasename() {
		return basename;
	}

	public void setBasename(String name) {
		this.basename = name;
	}

	public File getFile() {
		return new File(filePath, basename + ".wav");
	}

	/**
	 * Get the number of files in filePath containing basename in their file name.
	 * 
	 * @return fileCount
	 */
	public int getFileCount() {
		return fileCount;
	}

	/**
	 * Plays a sound file once via the indicated sourcedataline. The method blocks until the playing has completed.
	 * 
	 * @param soundFilePathString
	 *            soundFilePathString
	 * @param line
	 *            line
	 * @param outputMode
	 *            outputMode
	 */
	public static void play(String soundFilePathString, SourceDataLine line, int outputMode) {
		play(new File(soundFilePathString), line, outputMode);
	}

	/**
	 * Plays a sound file once via the indicated sourcedataline. The method blocks until the playing has completed.
	 * 
	 * @param soundFile
	 *            soundFile
	 * @param line
	 *            line
	 * @param outputMode
	 *            outputMode
	 */
	public static void play(File soundFile, SourceDataLine line, int outputMode) {
		try {
			audioPlayer = new AudioPlayer(soundFile, line, null, outputMode);
			audioPlayer.start();
			audioPlayer.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void stopPlaying() {
		if (audioPlayer != null)
			audioPlayer.cancel();
	}

	// ______________________________________________________________________
	// Class methods

	// ______________________________________________________________________
	// Constructors

	/**
	 * Creates a new instance of Speech given a file path
	 * 
	 * @param passedFilePath
	 *            The file path containing the sound files (i.e., the wav or wav_synth directory path)
	 * @param passedBasename
	 *            The base name
	 * 
	 */
	public Speech(File passedFilePath, String passedBasename) {

		this.basename = passedBasename;
		this.filePath = passedFilePath;
		if (!this.filePath.isDirectory()) {
			System.err.println("Creating directory: " + this.filePath);
			boolean success = this.filePath.mkdir();
			if (!success) {
				throw new RuntimeException("could not create directory '" + this.filePath + "'");
			}
		}
		updateFileCount();

		Test.output("Speech object has " + this.fileCount + " file(s)."); // TESTCODE

	}

}
