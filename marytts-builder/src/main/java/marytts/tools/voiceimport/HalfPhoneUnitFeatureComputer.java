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
package marytts.tools.voiceimport;

import java.io.File;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.util.io.FileUtils;

/**
 * @author marc
 *
 */
public class HalfPhoneUnitFeatureComputer extends PhoneUnitFeatureComputer {
	public static final String[] HALFPHONE_FEATURES = new String[] { "halfphone_lr", "halfphone_unitname" };
	public static final String HALFPHONE_UNITNAME = "halfphone_unitname";

	public String getName() {
		return "HalfPhoneUnitFeatureComputer";
	}

	public HalfPhoneUnitFeatureComputer() {
		featsExt = ".hpfeats";
		FEATUREDIR = "HalfPhoneUnitFeatureComputer.featureDir";
		ALLOPHONES = "HalfPhoneUnitFeatureComputer.allophonesDir";
		FEATURELIST = "HalfPhoneUnitFeatureComputer.featureFile";
		MARYSERVERHOST = "HalfPhoneUnitFeatureComputer.maryServerHost";
		MARYSERVERPORT = "HalfPhoneUnitFeatureComputer.maryServerPort";
	}

	@Override
	protected void initialiseComp() throws Exception {
		locale = db.getProp(db.LOCALE);
		mary = null; // initialised only if needed
		unitfeatureDir = new File(getProp(FEATUREDIR));
		if (!unitfeatureDir.exists()) {
			System.out.print(FEATUREDIR + " " + getProp(FEATUREDIR) + " does not exist; ");
			if (!unitfeatureDir.mkdir()) {
				throw new Error("Could not create FEATUREDIR");
			}
			System.out.print("Created successfully.\n");
		}
		maryInputType = "ALLOPHONES";
		maryOutputType = "HALFPHONE_TARGETFEATURES";
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout theDb) {
		this.db = theDb;
		if (props == null) {
			props = new TreeMap<String, String>();
			props.put(FEATUREDIR, db.getProp(db.ROOTDIR) + "halfphonefeatures" + System.getProperty("file.separator"));
			props.put(ALLOPHONES, db.getProp(db.ROOTDIR) + "allophones" + System.getProperty("file.separator"));
			props.put(FEATURELIST, db.getProp(db.CONFIGDIR) + "features.txt");
			props.put(MARYSERVERHOST, "localhost");
			props.put(MARYSERVERPORT, "59125");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(FEATUREDIR, "directory containing the halfphone features." + "Will be created if it does not exist.");
		props2Help.put(ALLOPHONES, "Directory of corrected Allophones files.");
		props2Help.put(MARYSERVERHOST, "the host were the Mary server is running, default: \"localhost\"");
		props2Help.put(MARYSERVERPORT, "the port were the Mary server is listening, default: \"59125\"");
	}

	@Override
	protected void loadFeatureList() throws IOException {
		File featureFile = new File(getProp(FEATURELIST));
		if (!featureFile.exists()) {
			System.out.println("No feature file: '" + getProp(FEATURELIST) + "'");
		} else {
			System.out.println("Loading features from file " + getProp(FEATURELIST));
			try {
				featureList = FileUtils.getFileAsString(featureFile, "UTF-8");
				featureList = featureList.replaceAll("\\s+", " ");
				// Make sure specific halfphone features are included:
				for (String f : HALFPHONE_FEATURES) {
					if (!featureList.contains(f)) {
						featureList = f + " " + featureList;
					}
				}
				if (!featureList.startsWith(HALFPHONE_UNITNAME)) {
					// HALFPHONE_UNITNAME must be the first one in the list
					featureList = featureList.replaceFirst("\\s+" + HALFPHONE_UNITNAME + "\\s+", " ");
					featureList = HALFPHONE_UNITNAME + " " + featureList;

				}
			} catch (IOException e) {
				IOException ioe = new IOException("Cannot read feature list");
				ioe.initCause(e);
				throw ioe;
			}
		}

	}
}
