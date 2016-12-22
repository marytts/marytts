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
package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Makes the file etc/.txt.done.data from the text files in text directory
 * 
 * @author Anna Hunecke
 * 
 */
public class Mary2FestvoxTranscripts extends VoiceImportComponent {

	private DatabaseLayout db;

	public final String TRANSCRIPTFILE = "Mary2FestvoxTranscripts.transcriptFile";

	public String getName() {
		return "Mary2FestvoxTranscripts";
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			props.put(TRANSCRIPTFILE, db.getProp(db.ROOTDIR) + "txt.done.data");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(TRANSCRIPTFILE, "file containing the transcripts in festvox format");
	}

	public boolean compute() {
		try {
			// open output file
			PrintWriter textOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(getProp(TRANSCRIPTFILE))),
					"UTF-8"));

			// go through the text files
			String[] basenames = bnl.getListAsArray();
			String textDir = db.getProp(db.TEXTDIR);
			String textExt = db.getProp(db.TEXTEXT);
			for (int i = 0; i < basenames.length; i++) {
				// open the next file
				try {
					File nextFile = new File(textDir + basenames[i] + textExt);
					BufferedReader fileIn = new BufferedReader(new InputStreamReader(new FileInputStream(nextFile), "UTF-8"));

					String line = fileIn.readLine().trim();
					// line = line.replaceAll("\"","=");
					fileIn.close();
					textOut.println("( " + basenames[i] + " \"" + line + "\" )");
				} catch (FileNotFoundException fnfe) {
					bnl.remove(basenames[i]);
					continue;
				}
			}
			textOut.flush();
			textOut.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public int getProgress() {
		return -1;
	}

}
