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

import marytts.client.http.MaryHttpClient;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.util.MaryUtils;
import marytts.util.data.text.BasenameClassificationDefinitionFileReader;
import marytts.util.dom.DomUtils;
import marytts.util.http.Address;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * For the given texts, compute allophones, especially boundary tags.
 * 
 * @author Benjamin Roth, adapted from Sathish Chandra Pammi, Steiner
 * 
 */
public class AllophonesExtractor extends VoiceImportComponent {
	protected File textDir;

	protected File promptAllophonesDir;

	protected String featsExt = ".xml";

	protected String locale;

	protected MaryHttpClient mary;

	protected String maryInputType;

	protected String maryOutputType;

	protected DatabaseLayout db = null;

	protected int percent = 0;

	public String STYLEDEFINITIONFILE = getName() + ".styleDefinitionFile";

	// StyleDefinitionFileReader serves as switch for style processing; disabled if null:
	protected BasenameClassificationDefinitionFileReader styleDefinition;

	public String getName() {
		return "AllophonesExtractor";
	}

	@Override
	protected void initialiseComp() {
		locale = db.getProp(db.LOCALE);
		mary = null; // initialised only if needed
		promptAllophonesDir = new File(db.getProp(db.PROMPTALLOPHONESDIR));
		if (!promptAllophonesDir.exists()) {
			System.out.println("Allophones directory does not exist; ");
			if (!promptAllophonesDir.mkdir()) {
				throw new Error("Could not create ALLOPHONES");
			}
			System.out.println("Created successfully.\n");
		}
		maryInputType = "RAWMARYXML";
		maryOutputType = "ALLOPHONES";

		// if styleDefinitionFileName is provided, try to initialize StyleDefinitionFileReader:
		String styleDefinitionFileName = getProp(STYLEDEFINITIONFILE);
		if (!styleDefinitionFileName.equals("")) {
			try {
				styleDefinition = new BasenameClassificationDefinitionFileReader(styleDefinitionFileName);
				if (!styleDefinition.fileOK) {
					System.err.println("There were problems parsing " + styleDefinitionFileName);
				}
			} catch (IOException e) {
				System.err.println("Warning: style definition file " + styleDefinitionFileName
						+ " could not be opened, styles will not be used!");
			}
		}
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout theDb) {
		this.db = theDb;
		if (props == null) {
			props = new TreeMap<String, String>();
			props.put(STYLEDEFINITIONFILE, ""); // empty string -> disabled
			// props.put(STYLEDEFINITIONFILE, db.getProp(db.CONFIGDIR) + "styleDefinition.txt"); // disabled by default
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help
				.put(STYLEDEFINITIONFILE,
						"Text file defining speaking style using glob patterns &ndash; to disable styles, leave this blank."
								+ "<p>Each line in the file should have the format:"
								+ "<pre>GLOB = style</pre>"
								+ "where <tt>GLOB</tt> is a glob expression (e.g. <tt>ob_*</tt> to match all basenames that start with <tt>ob_</tt>)."
								+ "<p>Lines that are empty or start with <tt>#</tt> are ignored.");
	}

	public MaryHttpClient getMaryClient() throws IOException {
		if (mary == null) {
			try {
				Address server = new Address(db.getProp(db.MARYSERVERHOST), Integer.parseInt(db.getProp(db.MARYSERVERPORT)));
				mary = new MaryHttpClient(server);
			} catch (IOException e) {
				IOException myIOE = new IOException("Could not connect to Maryserver at " + db.getProp(db.MARYSERVERHOST) + " "
						+ db.getProp(db.MARYSERVERPORT));
				myIOE.initCause(e);
				throw myIOE;
			}
		}
		return mary;
	}

	public boolean compute() throws IOException, MaryConfigurationException {
		String inputDir = db.getProp(db.TEXTDIR);
		textDir = new File(inputDir);
		System.out.println("Computing ALLOPHONES files for " + bnl.getLength() + " files");
		for (int i = 0; i < bnl.getLength(); i++) {
			percent = 100 * i / bnl.getLength();
			generateAllophonesFile(bnl.getName(i));
			System.out.println("    " + bnl.getName(i));
		}
		System.out.println("...Done.");
		return true;
	}

	/**
	 * 
	 * @param basename
	 *            basename
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public void generateAllophonesFile(String basename) throws IOException, MaryConfigurationException {
		Locale localVoice = MaryUtils.string2locale(locale);
		String xmlLocale = MaryUtils.locale2xmllang(localVoice);
		String inputDir = db.getProp(db.TEXTDIR);
		String outputDir = promptAllophonesDir.getAbsolutePath();
		String fullFileName = inputDir + File.separator + basename + db.getProp(db.TEXTEXT);

		// this string controls whether style attributes are inserted into the prompt_allophones ("" -> disabled):
		String style = "";
		if (styleDefinition != null) {
			style = getStyleFromStyleDefinition(basename);
		}

		File textFile = new File(fullFileName);
		String text = FileUtils.readFileToString(textFile, "UTF-8");

		// First, test if there is a corresponding .rawmaryxml file in textdir:
		File rawmaryxmlFile = new File(db.getProp(db.MARYXMLDIR) + File.separator + basename + db.getProp(db.MARYXMLEXT));
		if (rawmaryxmlFile.exists()) {
			if (style.isEmpty()) {
				// just pass through the raw file:
				text = FileUtils.readFileToString(rawmaryxmlFile, "UTF-8");
			} else {
				// parse the .rawmaryxml file:
				Document document = null;
				try {
					document = DomUtils.parseDocument(rawmaryxmlFile);
				} catch (Exception e) {
					throw new IOException("Error parsing RAWMARYXML file: " + rawmaryxmlFile.getName(), e);
				}

				// get the <maryxml> node:
				Node maryXmlNode = document.getDocumentElement();
				Node firstMaryXmlChild = maryXmlNode.getFirstChild();
				Node lastMaryXmlChild = maryXmlNode.getLastChild();
				// wrap the <maryxml>'s content in new <prosody> element...
				Element topLevelProsody = DomUtils.encloseNodesWithNewElement(firstMaryXmlChild, lastMaryXmlChild,
						MaryXML.PROSODY);
				// ...and set its style attribute:
				topLevelProsody.setAttribute("style", style);

				// convert the document to the text string:
				text = DomUtils.document2String(document);
			}
		} else {
			String prosodyOpeningTag = "";
			String prosodyClosingTag = "";
			if (!style.isEmpty()) {
				prosodyOpeningTag = String.format("<%s style=\"%s\">\n", MaryXML.PROSODY, style);
				prosodyClosingTag = String.format("</%s>\n", MaryXML.PROSODY);
			}
			text = getMaryXMLHeaderWithInitialBoundary(xmlLocale) + prosodyOpeningTag + text + prosodyClosingTag + "</maryxml>";
		}

		OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(outputDir, basename + featsExt)));
		MaryHttpClient maryClient = getMaryClient();
		maryClient.process(text, maryInputType, maryOutputType, db.getProp(db.LOCALE), null, null, os);
		os.flush();
		os.close();
	}

	public static String getMaryXMLHeaderWithInitialBoundary(String locale) // wtf?
	{
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "<maryxml version=\"0.4\"\n"
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + "xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n"
				+ "xml:lang=\"" + locale + "\">\n" + "<boundary  breakindex=\"2\" duration=\"100\"/>\n";

	}

	/**
	 * Get style for basename from style definition file. Do this by matching basename against a glob pattern.
	 * 
	 * @param basename
	 *            basename
	 * @return style as String
	 */
	private String getStyleFromStyleDefinition(String basename) {
		return styleDefinition.getValue(basename);
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
