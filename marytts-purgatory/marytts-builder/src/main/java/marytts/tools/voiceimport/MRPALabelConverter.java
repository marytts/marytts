/**
 * Copyright 2006 DFKI GmbH.
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class MRPALabelConverter extends VoiceImportComponent {

	private DatabaseLayout db;
	private Map sampamap;

	public final String MRPALABDIR = "MRPALabelConverter.mrpaLabDir";

	public String getName() {
		return "MRPALabelConverter";
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			props.put(MRPALABDIR, db.getProp(db.ROOTDIR) + "st/lab");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(MRPALABDIR, "directory containing the mrpa label files");
	}

	public boolean compute() {

		System.out.println("Converting mrpa labels ... ");
		// get the filename of the sampamapfile
		String marybase = db.getProp(db.MARYBASE);
		String sampamapfile = marybase + "/lib/modules/en/synthesis/sampa2mrpa_en.map";
		try {
			if (sampamap == null) {
				// read in the sampamap
				BufferedReader sampamapFileReader = new BufferedReader(new FileReader(new File(sampamapfile)));

				String line;
				sampamap = new HashMap();
				while ((line = sampamapFileReader.readLine()) != null) {
					if (!line.startsWith("#")) {
						String[] phones = line.split("<->");
						sampamap.put(phones[1], phones[0]);
					}
				}
			}

			// go through the label files

			File rootDirFile = new File(db.getProp(db.ROOTDIR));
			String rootDirName = rootDirFile.getCanonicalPath();
			// get mrpa lab-directory
			File stLabDir = new File(getProp(MRPALABDIR));
			if (!stLabDir.exists()) {
				System.out.println("Error loading mrpa labels: mrpa label directory " + getProp(MRPALABDIR) + " does not exist");
				return false;
			}
			// lab destination directory
			String labDestDir = db.getProp(db.LABDIR);
			String labExtension = db.getProp(db.LABEXT);
			// used to prune the times to 5 positions behind .
			DecimalFormat df = new DecimalFormat("0.00000");
			// go through original lab files
			File[] labFiles = stLabDir.listFiles();
			for (int i = 0; i < labFiles.length; i++) {
				File nextFile = labFiles[i];
				System.out.println(nextFile.getName());

				// open original lab file
				BufferedReader labIn = new BufferedReader(new FileReader(nextFile));

				// open destination lab file
				PrintWriter labOut = new PrintWriter(new FileWriter(new File(labDestDir + nextFile.getName())));

				String pauseString = null;
				String line;
				boolean endOfHeader = false;
				// go through original lab file
				while ((line = labIn.readLine()) != null) {

					if (line.startsWith("#")) {
						// copy the line to destination lab file
						labOut.println(line);
						endOfHeader = true;
					} else {
						if (!endOfHeader) {
							continue;
						}
						// tokenize the line
						StringTokenizer tok = new StringTokenizer(line);

						// first token is time
						double time = Float.parseFloat(tok.nextToken());
						// add 0.012
						// TODO: find out why we are adding 0.012
						time += 0.012;
						// prune time to 5 positions behind the dot
						String timeString = df.format(time);

						// next token is some number
						String mysteriousNumber = tok.nextToken();

						// next token is the phone
						String phone = tok.nextToken();

						if (phone.equals("pau")) {
							// replace silence symbol
							phone = "_";
							// store the pause in pause string; to be written later
							// (this has the effect that if two pauses follow
							// each other, only the last one is printed)
							pauseString = timeString + " " + mysteriousNumber + " " + phone;
						} else {
							if (pauseString != null) {
								// there is still a pause to print
								labOut.println(pauseString);
								// remove the pause
								pauseString = null;
							}
							// convert phone to SAMPA
							phone = convertPhone(phone);
							labOut.println(timeString + " " + mysteriousNumber + " " + phone);
						}

					}
				}

				if (pauseString != null) {
					// print last pause
					labOut.println(pauseString);
				}
				// close files
				labIn.close();
				labOut.flush();
				labOut.close();
			}
			System.out.println("... done.");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private String convertPhone(String phone) {
		if (sampamap.containsKey(phone)) {
			return (String) sampamap.get(phone);
		} else {
			return phone;
		}
	}

	public int getProgress() {
		return -1;
	}

}
