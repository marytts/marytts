/**
 * Copyright 2007 DFKI GmbH.
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

package marytts.signalproc.adaptation.test;

import java.io.IOException;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.Vector;

import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;

/**
 * This class implements the acoustic post-processor in an attempt to improve intelligibility of TTS outputs passed from a
 * telephone channel in Blizzard 2009. The algorithm is based on some basic processing to enhance formants, to boost consonant
 * gains as compared to vowels, and optional highpass filtering.
 * 
 * Reference: M. Schröder, S. Pammi, and O. Türk, "Multilingual MARY TTS participation in the Blizzard Challenge 2009", in Proc.
 * of the Blizzard Challenge 2009.
 * 
 * @author oytun.turk
 * 
 */
public class IeeeTaslp2009MaryResultsPreprocessor {
	// Search for all .txt files, read them, extract test results, and write all results to a separate, single file
	// Also compute total time it took for each .txt file to be completed by the subject
	public static void combineResults(String[] folders, String completeResultsFile, String totalDurationsFile) throws IOException {
		String strTmp;
		String strOut;
		Vector<String> allResults = new Vector<String>();
		Vector<String> subjectDurations = new Vector<String>();
		for (int i = 0; i < folders.length; i++) {
			System.out.println("----------------");

			String[] resultFiles = FileUtils.getFileList(folders[i], ".txt");

			for (int j = 0; j < resultFiles.length; j++) {
				String[] currentResults = StringUtils.readTextFile(resultFiles[j], "ASCII");
				double currentTotalTimeInMiliseconds = 0.0;

				for (int k = 0; k < currentResults.length; k++) {
					if (currentResults[k].startsWith("Response")) {
						StringTokenizer st = new StringTokenizer(currentResults[k], " ");

						strTmp = st.nextToken(); // Skip "Response:"

						String filename = st.nextToken(); // Filename
						int beginIndex = filename.lastIndexOf("/");
						filename = filename.substring(beginIndex + 1);

						String duration = st.nextToken(); // Skip "XXXXms"
						int endIndex = duration.indexOf("ms");
						duration = duration.substring(0, endIndex);
						currentTotalTimeInMiliseconds += Double.valueOf(duration);

						strTmp = st.nextToken(); // Skip "selected"

						strTmp = st.nextToken(); // Skip "="

						String score = st.nextToken(); // Score

						if (filename != null && score != null) {
							strOut = filename + " " + score;
							allResults.add(strOut);
						}
					}
				}

				subjectDurations.add(String.valueOf(currentTotalTimeInMiliseconds / 1000.0)); // In seconds

				System.out.println("Folder" + String.valueOf(i + 1) + ", processed file " + String.valueOf(j + 1) + " of "
						+ String.valueOf(resultFiles.length));
			}
		}

		if (subjectDurations.size() > 0)
			FileUtils.writeTextFile(subjectDurations, totalDurationsFile);

		if (allResults.size() > 0) {
			Collections.sort(allResults);
			FileUtils.writeTextFile(allResults, completeResultsFile);
		}
	}

	/**
	 * @param args
	 *            args
	 * @throws Exception
	 *             exception
	 */
	public static void main(String[] args) throws Exception {
		// Emotion
		String[] emoResultsFolders = { "D:/publications/IEEE_TASLP/2009/expressiveVC/listening_test_results/EmotionA",
				"D:/publications/IEEE_TASLP/2009/expressiveVC/listening_test_results/EmotionB" };
		String completeEmoResultsFile = "D:/publications/IEEE_TASLP/2009/expressiveVC/listening_test_results/completeEmo.txt";
		String totalEmoDurationsFile = "D:/publications/IEEE_TASLP/2009/expressiveVC/listening_test_results/durationsEmo.txt";

		combineResults(emoResultsFolders, completeEmoResultsFile, totalEmoDurationsFile);
		//

		// MOS
		String[] mosResultsFolders = { "D:/publications/IEEE_TASLP/2009/expressiveVC/listening_test_results/MOSA",
				"D:/publications/IEEE_TASLP/2009/expressiveVC/listening_test_results/MOSB" };
		String completeMOSResultsFile = "D:/publications/IEEE_TASLP/2009/expressiveVC/listening_test_results/completeMOS.txt";
		String totalMOSDurationsFile = "D:/publications/IEEE_TASLP/2009/expressiveVC/listening_test_results/durationsMOS.txt";

		combineResults(mosResultsFolders, completeMOSResultsFile, totalMOSDurationsFile);
		//
	}

}
