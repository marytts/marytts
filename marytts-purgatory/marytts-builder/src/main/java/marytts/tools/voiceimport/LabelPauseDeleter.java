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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class LabelPauseDeleter extends VoiceImportComponent {

	private DatabaseLayout db;
	private File ehmm;
	private String outputDir;
	protected String labExt = ".lab";
	protected String pauseSymbol;
	private int progress;
	private String locale;

	public final String EDIR = "LabelPauseDeleter.eDir";
	public final String OUTLABDIR = "LabelPauseDeleter.outputLabDir";
	public final String PAUSETHR = "LabelPauseDeleter.pauseDurationThreshold";

	public final String getName() {
		return "LabelPauseDeleter";
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			String ehmmdir = System.getProperty("EHMMDIR");
			if (ehmmdir == null) {
				ehmmdir = "/project/mary/Festival/festvox/src/ehmm/";
			}

			props.put(EDIR, db.getProp(db.ROOTDIR) + "ehmm" + System.getProperty("file.separator"));
			props.put(OUTLABDIR, db.getProp(db.ROOTDIR) + "lab" + System.getProperty("file.separator"));
			props.put(PAUSETHR, "100");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(EDIR, "directory containing all files used for training and labeling.");
		props2Help.put(OUTLABDIR, "Directory to store generated lebels from EHMM.");
		props2Help.put(PAUSETHR, "Threshold for deleting pauses from label files");
	}

	@Override
	protected void initialiseComp() {
		locale = db.getProp(db.LOCALE);
		this.pauseSymbol = System.getProperty("pause.symbol", "_");
	}

	/**
	 * Do the computations required by this component.
	 * 
	 * @throws Exception
	 *             Exception
	 * @return true on success, false on failure
	 */
	public boolean compute() throws Exception {

		ehmm = new File(getProp(EDIR));
		System.out.println("Copying label files into lab directory ...");
		getProperLabelFormat();
		System.out.println(" ... done.");

		return true;
	}

	/**
	 * Post processing Step to convert Label files to MARY supportable format
	 * 
	 * @throws Exception
	 *             Exception
	 */
	private void getProperLabelFormat() throws Exception {

		List<String> problems = new ArrayList<String>();
		for (int i = 0; i < bnl.getLength(); i++) {
			progress = 100 * i / bnl.getLength();
			String basename = bnl.getName(i);
			boolean ok = convertSingleLabelFile(basename);
			if (ok) {
				System.out.println("    " + basename);
			} else {
				System.out.println("     cannot read " + basename);
				problems.add(basename);
			}
		}
		if (problems.size() > 0) {
			System.out.println(problems.size() + " out of " + bnl.getLength() + " could not be read:");
			for (String b : problems) {
				System.out.println("    " + b);
			}
		}
	}

	/**
	 * Post Processing single Label file
	 * 
	 * @param basename
	 *            basename
	 * @throws Exception
	 *             Exception
	 * @return true on success, false on failure
	 */
	private boolean convertSingleLabelFile(String basename) throws Exception {

		String line;
		String previous, current;
		String regexp1 = "pau";
		String regexp2 = "ssil";

		File labDir = new File(getProp(OUTLABDIR));
		if (!labDir.exists()) {
			labDir.mkdir();
		}

		// READ LABEL FILE
		String filename = getProp(EDIR) + "/lab/" + basename + labExt;
		if (!new File(filename).exists())
			return false;
		try {
			UnitLabel[] ulab = UnitLabel.readLabFile(filename);

			// Remove multiple consecutive pauses
			ArrayList<UnitLabel> arrayLabel = new ArrayList<UnitLabel>();
			for (int i = 0; i < ulab.length; i++) {
				boolean iscPause = ulab[i].getUnitName().matches(regexp1) || ulab[i].getUnitName().matches(regexp2);
				if ((i + 1) < ulab.length) {
					boolean isnPause = ulab[i + 1].getUnitName().matches(regexp1) || ulab[i + 1].getUnitName().matches(regexp2);
					if (iscPause && isnPause) {
						ulab[i + 1].setStartTime(ulab[i].getStartTime());
						// System.out.println(ulab[i].getEndTime()+" "+ulab[i].getUnitIndex()+" "+ulab[i].getUnitName());
						continue;
					}
				}
				if (iscPause) {
					ulab[i].setUnitName(pauseSymbol);
				}
				arrayLabel.add(ulab[i]);
			}

			// Remove pauses below given threshold
			for (int i = 0; i < arrayLabel.size(); i++) {
				UnitLabel ul = arrayLabel.get(i);
				if (i > 0 && (i + 1) < arrayLabel.size()) {
					if (ul.getUnitName().equals(pauseSymbol)) {
						double duration = ul.endTime - ul.startTime;
						if (!isRealPause(duration)) {
							System.out.println("deleting... " + ul.startTime + " " + ul.endTime + " " + ul.unitName);
							UnitLabel pul = arrayLabel.get(i - 1);
							UnitLabel nul = arrayLabel.get(i + 1);
							pul.setEndTime(pul.getEndTime() + ((double) duration / 2.0));
							nul.setStartTime(nul.getStartTime() - ((double) duration / 2.0));
							arrayLabel.remove(i--);
						}
					}
				}
			}
			ulab = arrayLabel.toArray(new UnitLabel[0]);

			// write labels into given file
			UnitLabel.writeLabFile(ulab, labDir + "/" + basename + labExt);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true; // success

	}

	private boolean isRealPause(double phoneDuration) {
		/*
		 * TODO: Here we need to modify thresholds
		 */

		double threshold = Double.parseDouble(getProp(PAUSETHR));
		if (phoneDuration > (threshold / (double) 1000.0)) {
			return true;
		}

		return false;
	}

	/**
	 * To get Label Unit DATA (time stamp, index, phone unit)
	 * 
	 * @param line
	 *            line
	 * @return ArrayList contains time stamp, index and phone unit
	 * @throws IOException
	 *             IOException
	 */
	private ArrayList getLabelUnitData(String line) throws IOException {
		if (line == null)
			return null;
		ArrayList unitData = new ArrayList();
		StringTokenizer st = new StringTokenizer(line.trim());
		// the first token is the time
		unitData.add(st.nextToken());
		// the second token is the unit index
		unitData.add(st.nextToken());
		// the third token is the phone
		unitData.add(st.nextToken());
		return unitData;
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return progress;
	}

}
