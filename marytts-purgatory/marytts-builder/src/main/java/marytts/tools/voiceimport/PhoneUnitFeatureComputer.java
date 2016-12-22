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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.client.http.MaryHttpClient;
import marytts.util.http.Address;
import marytts.util.io.FileUtils;

/**
 * For the given texts, compute unit features and align them with the given unit labels.
 * 
 * @author schroed
 *
 */
public class PhoneUnitFeatureComputer extends VoiceImportComponent {
	public static final String PHONEFEATURE = "phone";

	protected File textDir;
	protected File unitfeatureDir;
	protected String featureList;
	protected String featsExt = ".pfeats";
	protected String xmlExt = ".xml";
	protected String locale;
	protected MaryHttpClient mary;
	protected String maryInputType;
	protected String maryOutputType;

	protected DatabaseLayout db = null;
	protected int percent = 0;

	public String FEATUREDIR = "PhoneUnitFeatureComputer.featureDir";
	public String ALLOPHONES = "PhoneUnitFeatureComputer.allophonesDir";
	public String FEATURELIST = "PhoneUnitFeatureComputer.featureFile";
	public String MARYSERVERHOST = "PhoneUnitFeatureComputer.maryServerHost";
	public String MARYSERVERPORT = "PhoneUnitFeatureComputer.maryServerPort";

	public String getName() {
		return "PhoneUnitFeatureComputer";
	}

	public static String getMaryXMLHeaderWithInitialBoundary(String locale) {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "<maryxml version=\"0.4\"\n"
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + "xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n"
				+ "xml:lang=\"" + locale + "\">\n" + "<boundary  breakindex=\"2\" duration=\"100\"/>\n";

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
		maryOutputType = "TARGETFEATURES";
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout theDb) {
		this.db = theDb;
		if (props == null) {
			props = new TreeMap<String, String>();
			props.put(FEATUREDIR, db.getProp(db.ROOTDIR) + "phonefeatures" + System.getProperty("file.separator"));
			props.put(ALLOPHONES, db.getProp(db.ROOTDIR) + "allophones" + System.getProperty("file.separator"));
			props.put(FEATURELIST, db.getProp(db.CONFIGDIR) + "features.txt");
			props.put(MARYSERVERHOST, "localhost");
			props.put(MARYSERVERPORT, "59125");
		}

		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(FEATUREDIR, "directory containing the phone features." + "Will be created if it does not exist");
		props2Help.put(ALLOPHONES, "Directory of corrected allophones files.");
		props2Help.put(MARYSERVERHOST, "the host were the Mary server is running, default: \"localhost\"");
		props2Help.put(MARYSERVERPORT, "the port were the Mary server is listening, default: \"59125\"");
	}

	public MaryHttpClient getMaryClient() throws IOException {
		if (mary == null) {
			try {
				mary = new MaryHttpClient(new Address(getProp(MARYSERVERHOST), Integer.parseInt(getProp(MARYSERVERPORT))));
			} catch (IOException e) {
				throw new IOException("Could not connect to Maryserver at " + getProp(MARYSERVERHOST) + " "
						+ getProp(MARYSERVERPORT));
			}
		}
		return mary;
	}

	protected void loadFeatureList() throws IOException {
		File featureFile = new File(getProp(FEATURELIST));
		if (!featureFile.exists()) {
			System.out.println("No feature file: '" + getProp(FEATURELIST) + "'");
		} else {
			System.out.println("Loading features from file " + getProp(FEATURELIST));
			try {
				featureList = FileUtils.getFileAsString(featureFile, "UTF-8");
				featureList = featureList.replaceAll("\\s+", " ");
				// Exclude specific halfphone features if present:
				for (String f : HalfPhoneUnitFeatureComputer.HALFPHONE_FEATURES) {
					if (featureList.contains(f)) {
						featureList = featureList.replaceAll(f, "");
					}
				}
				if (!featureList.contains(PHONEFEATURE)) {
					throw new RuntimeException("Feature list does not contain feature '" + PHONEFEATURE
							+ "'. It makes no sense to continue.");
				}
				if (!featureList.startsWith(PHONEFEATURE)) {
					// PHONEFEATURE must be the first one in the list
					featureList = featureList.replaceFirst("\\s+" + PHONEFEATURE + "\\s*", " ");
					featureList = PHONEFEATURE + " " + featureList;
				}
			} catch (IOException e) {
				IOException ioe = new IOException("Cannot read list of features");
				ioe.initCause(e);
				throw ioe;
			}
		}

	}

	public boolean compute() throws IOException {

		loadFeatureList();

		textDir = new File(db.getProp(db.TEXTDIR));
		System.out.println("Computing unit features for " + bnl.getLength() + " files");
		for (int i = 0; i < bnl.getLength(); i++) {
			percent = 100 * i / bnl.getLength();
			computeFeaturesFor(bnl.getName(i));
			System.out.println("    " + bnl.getName(i));
		}
		System.out.println("Finished computing the unit features.");
		return true;
	}

	public void computeFeaturesFor(String basename) throws IOException {
		File allophoneFile = new File(getProp(ALLOPHONES) + basename + xmlExt);
		String text = FileUtils.getFileAsString(allophoneFile, "UTF-8");

		OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(unitfeatureDir, basename + featsExt)));
		MaryHttpClient maryClient = getMaryClient();

		maryClient.process(text, maryInputType, maryOutputType, locale, null, null, "", null, featureList, os);
		os.flush();
		os.close();
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return percent;
	}

}
