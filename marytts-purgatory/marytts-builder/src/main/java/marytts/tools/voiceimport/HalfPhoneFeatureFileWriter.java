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

import java.util.SortedMap;
import java.util.TreeMap;

import marytts.features.FeatureDefinition;

public class HalfPhoneFeatureFileWriter extends PhoneFeatureFileWriter {
	protected FeatureDefinition leftFeatureDef;
	protected FeatureDefinition rightFeatureDef;

	public HalfPhoneFeatureFileWriter() {
		featureExt = ".hpfeats";
		FEATUREDIR = "HalfPhoneFeatureFileWriter.featureDir";
		FEATUREFILE = "HalfPhoneFeatureFileWriter.featureFile";
		UNITFILE = "HalfPhoneFeatureFileWriter.unitFile";
		WEIGHTSFILE = "HalfPhoneFeatureFileWriter.weightsFile";
		name = "HalfPhoneFeatureFileWriter";
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		if (props == null) {
			props = new TreeMap();
			props.put(FEATUREDIR, db.getProp(db.ROOTDIR) + "halfphonefeatures" + System.getProperty("file.separator"));
			props.put(FEATUREFILE, db.getProp(db.FILEDIR) + "halfphoneFeatures" + db.getProp(db.MARYEXT));
			props.put(UNITFILE, db.getProp(db.FILEDIR) + "halfphoneUnits" + db.getProp(db.MARYEXT));
			props.put(WEIGHTSFILE, db.getProp(db.CONFIGDIR) + "halfphoneUnitFeatureDefinition.txt");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(FEATUREDIR, "directory containing the halfphone features");
		props2Help.put(FEATUREFILE, "file containing all halfphone units and their target cost features."
				+ "Will be created by this module");
		props2Help.put(UNITFILE, "file containing all halfphone units");
		props2Help.put(WEIGHTSFILE, "file containing the list of halfphone target cost features, their values and weights");

	}

}
