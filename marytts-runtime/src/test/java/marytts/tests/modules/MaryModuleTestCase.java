/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.tests.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.modules.MaryModule;
import marytts.server.Mary;
import marytts.util.MaryUtils;
import marytts.util.dom.DomUtils;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

/**
 * @author Marc Schr&ouml;der
 * 
 * 
 */
public class MaryModuleTestCase {

	protected MaryModule module;

	public MaryModuleTestCase(boolean needMaryStarted) throws Exception {
		if (!MaryUtils.isLog4jConfigured()) {
			BasicConfigurator.configure();
		}
		Logger.getRootLogger().setLevel(Level.DEBUG);
		if (System.getProperty("mary.base") == null) {
			System.setProperty("mary.base", ".");
			Logger.getRootLogger().warn(
					"System property 'mary.base' is not defined -- trying " + new File(".").getAbsolutePath()
							+ " -- if this fails, please start this using VM property \"-Dmary.base=/path/to/mary/runtime\"!");
		}

		if (needMaryStarted) {
			if (Mary.currentState() == Mary.STATE_OFF)
				Mary.startup();
		}
	}

	protected MaryData createMaryDataFromText(String text, Locale locale) {
		Document doc = MaryXML.newDocument();
		doc.getDocumentElement().setAttribute("xml:lang", MaryUtils.locale2xmllang(locale));
		doc.getDocumentElement().appendChild(doc.createTextNode(text));
		MaryData md = new MaryData(MaryDataType.RAWMARYXML, locale);
		md.setDocument(doc);
		return md;
	}

	protected String loadResourceIntoString(String resourceName) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(resourceName), "UTF-8"));
		StringBuilder buf = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			buf.append(line);
			buf.append("\n");
		}
		return buf.toString();
	}

	protected void processAndCompare(String basename, Locale locale) throws Exception {
		assert inputEnding() != null;
		assert outputEnding() != null;
		MaryData input = null;
		if (inputEnding().equals("txt")) {
			String in = loadResourceIntoString(basename + "." + inputEnding());
			input = createMaryDataFromText(in, locale);
		} else {
			input = new MaryData(module.inputType(), locale);
			input.readFrom(this.getClass().getResourceAsStream(basename + "." + inputEnding()), null);
		}
		MaryData targetOut = new MaryData(module.outputType(), input.getLocale());
		targetOut.readFrom(this.getClass().getResourceAsStream(basename + "." + outputEnding()), null);
		MaryData processedOut = module.process(input);
		try {
			DomUtils.compareNodes(targetOut.getDocument(), processedOut.getDocument(), true);
		} catch (Exception afe) {
			StringBuilder msg = new StringBuilder();
			msg.append("XML documents are not equal\n");
			msg.append("==========target:=============\n");
			Document target = (Document) targetOut.getDocument().cloneNode(true);
			DomUtils.trimAllTextNodes(target);
			msg.append(DomUtils.document2String(target)).append("\n\n");
			msg.append("==========processed:============\n");
			Document processed = (Document) processedOut.getDocument().cloneNode(true);
			DomUtils.trimAllTextNodes(processed);
			msg.append(DomUtils.document2String(processed)).append("\n");
			throw new Exception(msg.toString(), afe);
		}
	}

	/**
	 * To be overridden by subclasses using processAndCompare; this string will be used as the filename ending of result files by
	 * processAndCompare().
	 */
	protected String inputEnding() {
		return null;
	}

	/**
	 * To be overridden by subclasses using processAndCompare; this string will be used as the filename ending of result files by
	 * processAndCompare().
	 */
	protected String outputEnding() {
		return null;
	}

}
