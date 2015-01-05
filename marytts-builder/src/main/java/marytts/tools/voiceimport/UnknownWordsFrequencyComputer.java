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
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.client.MaryClient;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.util.MaryUtils;
import marytts.util.dom.NameNodeFilter;
import marytts.util.http.Address;
import marytts.util.io.FileUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

/**
 * For the given texts, compute unit features and align them with the given unit labels.
 * 
 * @author schroed
 *
 */
public class UnknownWordsFrequencyComputer extends VoiceImportComponent {
	protected File textDir;
	protected File unitfeatureDir;
	protected String featsExt = ".pfeats";
	protected String locale;
	protected MaryClient mary;
	protected String maryInputType;
	protected String maryOutputType;

	protected DatabaseLayout db = null;
	protected int percent = 0;

	public String FEATUREDIR = "UnknownWordsFrequencyComputer.featureDir";
	public String MARYSERVERHOST = "UnknownWordsFrequencyComputer.maryServerHost";
	public String MARYSERVERPORT = "UnknownWordsFrequencyComputer.maryServerPort";

	public String getName() {
		return "UnknownWordsFrequencyComputer";
	}

	public static String getMaryXMLHeaderWithInitialBoundary(String locale) {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "<maryxml version=\"0.4\"\n"
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + "xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n"
				+ "xml:lang=\"" + locale + "\">\n" + "<boundary duration=\"100\"/>\n";

	}

	@Override
	protected void initialiseComp() {
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

		maryInputType = "RAWMARYXML";
		maryOutputType = "PHONEMES";
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			props.put(FEATUREDIR, db.getProp(db.ROOTDIR) + "phonemisedXML" + System.getProperty("file.separator"));
			props.put(MARYSERVERHOST, "localhost");
			props.put(MARYSERVERPORT, "59125");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(FEATUREDIR, "directory containing the phone features." + "Will be created if it does not exist");
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

	public boolean compute() throws IOException, Exception {
		textDir = new File(db.getProp(db.TEXTDIR));
		System.out.println("Computing unit features for " + bnl.getLength() + " files");
		for (int i = 0; i < bnl.getLength(); i++) {
			percent = 100 * i / bnl.getLength();
			computeFeaturesFor(bnl.getName(i));
			// System.out.println( "    " + bnl.getName(i) );
		}
		bnl.write(db.getProp(db.ROOTDIR) + File.separator + "newbaselist.txt");
		System.out.println("Finished computing the unit features.");
		return true;
	}

	public void computeFeaturesFor(String basename) throws IOException, Exception {
		String text;
		Locale localVoice;
		localVoice = MaryUtils.string2locale(locale);

		// First, test if there is a corresponding .rawmaryxml file in textdir:
		File rawmaryxmlFile = new File(db.getProp(db.MARYXMLDIR) + basename + db.getProp(db.MARYXMLEXT));
		if (rawmaryxmlFile.exists()) {
			text = FileUtils.getFileAsString(rawmaryxmlFile, "UTF-8");
		} else {
			text = getMaryXMLHeaderWithInitialBoundary(locale)
					+ FileUtils.getFileAsString(new File(db.getProp(db.TEXTDIR) + basename + db.getProp(db.TEXTEXT)), "UTF-8")
					+ "</maryxml>";
		}
		File pfeatFile = new File(unitfeatureDir, basename + featsExt);
		OutputStream os = new BufferedOutputStream(new FileOutputStream(pfeatFile));
		MaryClient maryClient = getMaryClient();
		/*
		 * Vector voices = maryClient.getVoices(localVoice); MaryClient.Voice defaultVoice = (MaryClient.Voice)
		 * voices.firstElement(); String voiceName = defaultVoice.name();
		 */
		// maryClient.process(text, maryInputType, maryOutputType, null, null, os);

		maryClient.process(text, maryInputType, maryOutputType, locale, null, "slt-arctic", os);
		// maryClient.process(text, maryInputType, maryOutputType, null, "slt-arctic", os, timeout);
		// maryClient.getOutputDataTypes().size()
		// MaryData result = new MaryData(os);

		os.flush();
		os.close();

		// System.out.println(" TO STRING: "+new FileReader(pfeatFile).toString());
		// BufferedReader bfr = new BufferedReader(new FileReader(pfeatFile));
		String line;
		MaryData d = new MaryData(MaryDataType.get("PHONEMISED_EN"), Locale.US);
		// d.readFrom(new ByteArrayInputStream(os.toByteArray()));
		d.readFrom(new FileReader(pfeatFile));

		// MaryData d = new MaryData(pfeatFile);
		Document doc = d.getDocument();
		// Document acoustparams = d.getDocument();

		// NodeIterator it = ((DocumentTraversal)acoustparams).createNodeIterator(acoustparams, NodeFilter.SHOW_ELEMENT,new
		// NameNodeFilter(new String[]{MaryXML.TOKEN, MaryXML.BOUNDARY}),false);
		NodeIterator it = ((DocumentTraversal) doc).createNodeIterator(doc, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(
				MaryXML.TOKEN), false);

		Element t = null;
		while ((t = (Element) it.nextNode()) != null) {
			if (t.hasAttribute("g2p_method")) {
				String g2p = t.getAttribute("g2p_method");
				String nodeText = t.getTextContent().trim();
				if (g2p.equals("rules")) {// && nodeText.equals("!")){
					System.out.print(basename + " ----> " + nodeText);
					if (bnl.contains(basename))
						bnl.remove(basename);
					System.out.println(" SO removing basename: " + basename);

				}

				// System.out.println("G2P:"+t.getAttribute("g2p_method"));
				// System.out.println("Text:"+t.getTextContent());
			}
		}

		/*
		 * while((line =bfr.readLine()) != null){ //boolean b = m.matches(); if(Pattern.matches("rules", line))
		 * System.out.println(basename + " LINE ---> " + line);
		 * 
		 * }
		 */
		// System.out.println(" TO STRING: "+line);

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
