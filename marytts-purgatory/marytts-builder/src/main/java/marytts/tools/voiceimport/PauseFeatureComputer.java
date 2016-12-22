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
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import marytts.client.MaryClient;
import marytts.util.MaryUtils;
import marytts.util.http.Address;
import marytts.util.io.FileUtils;

/**
 * For the given texts, compute unit features and align them with the given unit labels.
 * 
 * @author schroed
 * 
 */
public class PauseFeatureComputer extends VoiceImportComponent {
	protected File textDir;
	protected File pausefeatureDir;
	protected String featsExt = ".pfeats";
	protected String xmlExt = ".xml";
	protected String locale;
	protected MaryClient mary;
	protected String maryInputType;
	protected String maryOutputType;

	protected DatabaseLayout db = null;
	protected int percent = 0;

	public String FEATUREDIR = "PauseFeatureComputer.featureDir";
	public String INTONISED = "PauseFeatureComputer.correctedIntonisedXMLDir";
	public String MARYSERVERHOST = "PauseFeatureComputer.maryServerHost";
	public String MARYSERVERPORT = "PauseFeatureComputer.maryServerPort";

	public String getName() {
		return "PauseFeatureComputer";
	}

	public static String getMaryXMLHeaderWithInitialBoundary(String locale) {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "<maryxml version=\"0.4\"\n"
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + "xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n"
				+ "xml:lang=\"" + locale + "\">\n" + "<boundary  breakindex=\"2\" duration=\"100\"/>\n";

	}

	public void initialiseComp() {
		locale = db.getProp(db.LOCALE);

		mary = null; // initialised only if needed
		pausefeatureDir = new File(getProp(FEATUREDIR));
		if (!pausefeatureDir.exists()) {
			System.out.print(FEATUREDIR + " " + getProp(FEATUREDIR) + " does not exist; ");
			if (!pausefeatureDir.mkdir()) {
				throw new Error("Could not create FEATUREDIR");
			}
			System.out.print("Created successfully.\n");
		}

		maryInputType = "INTONATION";
		maryOutputType = "PAUSEFEATURES";
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			props.put(FEATUREDIR, db.getProp(db.ROOTDIR) + "pausefeatures" + System.getProperty("file.separator"));
			props.put(INTONISED, db.getProp(db.ROOTDIR) + "correctedIntonisedXML" + System.getProperty("file.separator"));
			props.put(MARYSERVERHOST, "localhost");
			props.put(MARYSERVERPORT, "59125");
		}

		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(FEATUREDIR, "directory containing the pause features." + "Will be created if it does not exist");
		props2Help.put(INTONISED, "Directory of corrected Intonised XML files.");
		props2Help.put(MARYSERVERHOST, "the host were the Mary server is running, default: \"localhost\"");
		props2Help.put(MARYSERVERPORT, "the port were the Mary server is listening, default: \"59125\"");
	}

	public MaryClient getMaryClient() throws IOException {
		if (mary == null) {
			try {
				mary = MaryClient.getMaryClient(new Address(getProp(MARYSERVERHOST), Integer.parseInt(getProp(MARYSERVERPORT))));
			} catch (IOException e) {
				throw new IOException("Could not connect to Maryserver at " + getProp(MARYSERVERHOST) + " "
						+ getProp(MARYSERVERPORT));
			}
		}
		return mary;
	}

	public boolean compute() throws IOException {

		textDir = new File(db.getProp(db.TEXTDIR));
		System.out.println("Computing pause features for " + bnl.getLength() + " files");
		for (int i = 0; i < bnl.getLength(); i++) {
			percent = 100 * i / bnl.getLength();
			computeFeaturesFor(bnl.getName(i));
			System.out.println("    " + bnl.getName(i));
		}
		System.out.println("Finished computing the unit features.");
		return true;
	}

	public void computeFeaturesFor(String basename) throws IOException {
		String text;
		Locale localVoice = MaryUtils.string2locale(locale);

		// First, test if there is a corresponding .rawmaryxml file in textdir:
		File rawmaryxmlFile = new File(db.getProp(db.MARYXMLDIR) + basename + db.getProp(db.MARYXMLEXT));
		if (rawmaryxmlFile.exists()) {
			text = FileUtils.getFileAsString(rawmaryxmlFile, "UTF-8");
		} else {
			text = getMaryXMLHeaderWithInitialBoundary(locale)
					+ FileUtils.getFileAsString(new File(db.getProp(db.TEXTDIR) + basename + db.getProp(db.TEXTEXT)), "UTF-8")
					+ "</maryxml>";
		}
		File intonisedxmlFile = new File(getProp(INTONISED) + basename + xmlExt);
		text = FileUtils.getFileAsString(intonisedxmlFile, "UTF-8");

		OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(pausefeatureDir, basename + featsExt)));
		MaryClient maryClient = getMaryClient();

		Vector<MaryClient.Voice> voices = maryClient.getVoices(localVoice);
		if (voices == null) {
			if (locale.equals("en")) {
				locale = "en_US";
				localVoice = MaryUtils.string2locale(locale);
				voices = maryClient.getVoices(localVoice);
			}
		}
		// try again:
		if (voices == null) {
			StringBuilder buf = new StringBuilder("Mary server has no voices for locale '" + localVoice
					+ "' -- known voices are:\n");
			Vector<MaryClient.Voice> allVoices = maryClient.getVoices();
			for (MaryClient.Voice v : allVoices) {
				buf.append(v.toString());
				buf.append("\n");
			}
			throw new RuntimeException(buf.toString());
		}
		MaryClient.Voice defaultVoice = (MaryClient.Voice) voices.firstElement();
		String voiceName = defaultVoice.name();
		// maryClient.process(text, maryInputType, maryOutputType, null, null, os);
		maryClient.process(text, maryInputType, maryOutputType, locale, null, voiceName, os);
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
