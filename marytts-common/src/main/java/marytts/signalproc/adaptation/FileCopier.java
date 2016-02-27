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
package marytts.signalproc.adaptation;

import java.io.IOException;

import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;

/**
 * Generic utility class for renaming and copying voice conversion training files
 * 
 * @author Oytun T&uuml;rk
 */
public class FileCopier {
	public FileCopier() {

	}

	// Generate appropriate wav and lab files by copying
	// source and target files from sourceInputBaseDir and targetInputBaseDir.
	// This is required since source and target files might not have identical filenames
	//
	// to sourceTrainingBaseDir and targetTrainingBaseDir with appropriate renaming
	// The output will be wav and lab files for source and target for parallel voice conversion training
	// and when the wav and lab files are sorted according to filenames, they will be
	// identical in content,
	//
	// sourceTargetFile is a text file which has two columns that list the mapping between
	// source and target files under input base directories:
	//
	// sourceFileName1 targetFileName1
	// sourceFileName2 targetFileName2
	// ...etc
	//
	// The genearted source files will have identical filenames with the input source files
	// Target files will be copied with a new name in the following format:
	//
	// sourceFileName1_targetFileName1.wav,
	// sourceFileName1_targetFileName1.lab, etc.
	//
	public void copy(String sourceTargetFile, // Input
			String sourceInputBaseDir, // Input
			String targetInputBaseDir, // Input
			String sourceTrainingBaseDir, // Output
			String targetTrainingBaseDir) // Output
			throws IOException {
		String[][] stNameMap = StringUtils.readTextFileInRows(sourceTargetFile, "UTF-8", 2);
		int i;

		// Determine source and target input sub directories
		sourceInputBaseDir = StringUtils.checkLastSlash(sourceInputBaseDir);
		targetInputBaseDir = StringUtils.checkLastSlash(targetInputBaseDir);
		String sourceInputWavDir = sourceInputBaseDir + "wav/";
		String targetInputWavDir = targetInputBaseDir + "wav/";
		String sourceInputLabDir = sourceInputBaseDir + "lab/";
		String targetInputLabDir = targetInputBaseDir + "lab/";
		if (!FileUtils.exists(sourceInputWavDir)) {
			System.out.println("Error! Folder not found: " + sourceInputWavDir);
			return;
		}
		if (!FileUtils.exists(targetInputWavDir)) {
			System.out.println("Error! Folder not found: " + targetInputWavDir);
			return;
		}
		if (!FileUtils.exists(sourceInputLabDir)) {
			System.out.println("Error! Folder not found: " + sourceInputLabDir);
			return;
		}
		if (!FileUtils.exists(targetInputLabDir)) {
			System.out.println("Error! Folder not found: " + targetInputLabDir);
			return;
		}
		//

		// Create training sub-folders for source and target
		sourceTrainingBaseDir = StringUtils.checkLastSlash(sourceTrainingBaseDir);
		targetTrainingBaseDir = StringUtils.checkLastSlash(targetTrainingBaseDir);
		FileUtils.createDirectory(sourceTrainingBaseDir);
		FileUtils.createDirectory(targetTrainingBaseDir);
		//

		if (stNameMap != null) {
			System.out.println("Generating - " + sourceTrainingBaseDir + " and " + targetTrainingBaseDir);

			String tmpFileIn, tmpFileOut;
			for (i = 0; i < stNameMap.length; i++) {
				// Source wav
				tmpFileIn = sourceInputWavDir + stNameMap[i][0] + ".wav";
				tmpFileOut = sourceTrainingBaseDir + stNameMap[i][0] + ".wav";
				if (!FileUtils.exists(tmpFileOut)) {
					if (FileUtils.exists(tmpFileIn)) {
						try {
							FileUtils.copy(tmpFileIn, tmpFileOut);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						System.out.println("Error! Input file not found: " + tmpFileIn);
						return;
					}
				}
				//

				// Source lab
				tmpFileIn = sourceInputLabDir + stNameMap[i][0] + ".lab";
				tmpFileOut = sourceTrainingBaseDir + stNameMap[i][0] + ".lab";
				if (!FileUtils.exists(tmpFileOut)) {
					if (FileUtils.exists(tmpFileIn)) {
						try {
							FileUtils.copy(tmpFileIn, tmpFileOut);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						System.out.println("Error! Input file not found: " + tmpFileIn);
						return;
					}
				}
				//

				// Target wav
				tmpFileIn = targetInputWavDir + stNameMap[i][1] + ".wav";
				tmpFileOut = targetTrainingBaseDir + stNameMap[i][0] + "_" + stNameMap[i][1] + ".wav";
				if (!FileUtils.exists(tmpFileOut)) {
					if (FileUtils.exists(tmpFileIn)) {
						try {
							FileUtils.copy(tmpFileIn, tmpFileOut);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						System.out.println("Error! Input file not found: " + tmpFileIn);
						return;
					}
				}
				//

				// Target lab
				tmpFileIn = targetInputLabDir + stNameMap[i][1] + ".lab";
				tmpFileOut = targetTrainingBaseDir + stNameMap[i][0] + "_" + stNameMap[i][1] + ".lab";
				if (!FileUtils.exists(tmpFileOut)) {
					if (FileUtils.exists(tmpFileIn)) {
						try {
							FileUtils.copy(tmpFileIn, tmpFileOut);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						System.out.println("Error! Input file not found: " + tmpFileIn);
						return;
					}
				}
				//

				System.out.println(String.valueOf(i + 1) + " of " + String.valueOf(stNameMap.length));
			}
		}

	}

	public static void main(String[] args) throws Exception {
		FileCopier f = new FileCopier();
		String sourceTargetFile, sourceInputBaseDir, targetInputBaseDir, sourceTrainingBaseDir, targetTrainingBaseDir;

		sourceTargetFile = "D:/Oytun/DFKI/voices/Interspeech08/mappings-mini-ea.txt";
		sourceInputBaseDir = "D:/Oytun/DFKI/voices/DFKI_German_Neutral";
		sourceTrainingBaseDir = "D:/Oytun/DFKI/voices/Interspeech08/neutral";

		// Obadiah_Sad
		targetInputBaseDir = "D:/Oytun/DFKI/voices/DFKI_German_Obadiah_Sad";
		targetTrainingBaseDir = "D:/Oytun/DFKI/voices/Interspeech08/sad";
		f.copy(sourceTargetFile, sourceInputBaseDir, targetInputBaseDir, sourceTrainingBaseDir, targetTrainingBaseDir);

		// Poppy_Happy
		targetInputBaseDir = "D:/Oytun/DFKI/voices/DFKI_German_Poppy_Happy";
		targetTrainingBaseDir = "D:/Oytun/DFKI/voices/Interspeech08/happy";
		f.copy(sourceTargetFile, sourceInputBaseDir, targetInputBaseDir, sourceTrainingBaseDir, targetTrainingBaseDir);

		// Spike_Angry
		targetInputBaseDir = "D:/Oytun/DFKI/voices/DFKI_German_Spike_Angry";
		targetTrainingBaseDir = "D:/Oytun/DFKI/voices/Interspeech08/angry";
		f.copy(sourceTargetFile, sourceInputBaseDir, targetInputBaseDir, sourceTrainingBaseDir, targetTrainingBaseDir);
	}

}
