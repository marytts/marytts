/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import marytts.client.http.MaryHttpClient;

/**
 * Copyright 2006 DFKI GmbH. All Rights Reserved. Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute this software and its documentation without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * this work, and to permit persons to whom this work is furnished to do so, subject to the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of conditions and the following disclaimer. 2. Any modifications
 * must be clearly marked as such. 3. Original authors' names are not deleted. 4. The authors' names are not used to endorse or
 * promote products derived from this software without specific prior written permission.
 * 
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT
 * OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF
 * CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

public class BatchSynth {

	/**
	 * Generate a set of audio files from text. Example call: java -cp maryclient.jar -Dserver.host=localhost -Dserver.port=59125
	 * -Dvoice=kevin16 marytts.client.BatchSynth target/dir path/to/texts.txt The text file must contain a target audio file name
	 * and the corresponding text in each line.
	 * 
	 * @param args
	 *            first argument, the output directory; the rest, file names containing text files. Each text file contains, in
	 *            each line, a file name followed by the sentence to generate as a .wav file.
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		File globalOutputDir = new File(args[0]);
		MaryHttpClient mary = new MaryHttpClient();
		String voice = System.getProperty("voice", "us1");
		boolean haveBasename = "true".equals(System.getProperty("lines-contain-basename", "true")); // default: true, for backward
																									// compatibility
		String inputFormat = "TEXT";
		String locale = System.getProperty("locale", "en_US");
		String outputFormat = System.getProperty("output.type", "AUDIO");
		String extension = outputFormat.equals("AUDIO") ? ".wav" : "." + outputFormat.toLowerCase();
		long globalStartTime = System.currentTimeMillis();
		int globalCounter = 0;
		for (int i = 1; i < args.length; i++) {
			long genreStartTime = System.currentTimeMillis();
			int genreCounter = 0;
			File texts = new File(args[i]);
			String genre = texts.getName().substring(0, texts.getName().lastIndexOf('.'));
			File outputDir = new File(globalOutputDir.getPath() + "/" + genre);
			outputDir.mkdir();
			BufferedReader textReader = new BufferedReader(new InputStreamReader(new FileInputStream(texts), "utf-8"));
			String line;
			while ((line = textReader.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0)
					continue;
				long startTime = System.currentTimeMillis();
				if (line.trim().startsWith("(")) {
					line = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
				}
				StringTokenizer st = new StringTokenizer(line);
				String basename;
				String sentence;
				if (haveBasename) {
					basename = st.nextToken();
					sentence = line.substring(line.indexOf(basename) + basename.length() + 1).trim();
				} else {
					basename = genre + genreCounter;
					sentence = line.trim();
				}

				// remove all backslashes
				sentence = sentence.replaceAll("\\\\", "");
				FileOutputStream audio = new FileOutputStream(outputDir + "/" + basename + extension);
				mary.process(sentence, inputFormat, outputFormat, locale, "WAVE", voice, audio);
				audio.close();
				long endTime = System.currentTimeMillis();
				System.out.println(basename + " synthesized in " + ((float) (endTime - startTime) / 1000.) + " s");
				globalCounter++;
				genreCounter++;
			}
			long genreEndTime = System.currentTimeMillis();
			System.out.println("Genre '" + genre + "' (" + genreCounter + " sentences) synthesized in "
					+ ((float) (genreEndTime - genreStartTime) / 1000.) + " s");
		}
		long globalEndTime = System.currentTimeMillis();
		System.out.println("Total: " + globalCounter + " sentences synthesized in "
				+ ((float) (globalEndTime - globalStartTime) / 1000.) + " s");

	}

}
