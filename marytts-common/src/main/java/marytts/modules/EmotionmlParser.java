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
package marytts.modules;

// TraX classes
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import emotionml.Checker;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.util.MaryUtils;
import marytts.util.dom.LoggingErrorHandler;

import org.w3c.dom.Document;

/**
 * Transforms a SABLE document into a raw (untokenised) MaryXML document
 * 
 * @author Marc Schr&ouml;der
 */

public class EmotionmlParser extends InternalModule {
	// One stylesheet can be used (read) by multiple threads:
	private static Templates stylesheet = null;

	private DocumentBuilderFactory dbFactory = null;
	private DocumentBuilder docBuilder = null;
	private boolean doWarnClient = false;

	public EmotionmlParser() {
		super("EmotionmlParser", MaryDataType.EMOTIONML, MaryDataType.RAWMARYXML, null);
	}

	public boolean getWarnClient() {
		return doWarnClient;
	}

	public void setWarnClient(boolean doWarnClient) {
		this.doWarnClient = doWarnClient;
	}

	public void startup() throws Exception {
		setWarnClient(true); // !! where should that be decided?
		if (stylesheet == null) {
			TransformerFactory tFactory = TransformerFactory.newInstance();
			StreamSource stylesheetStream = new StreamSource(this.getClass().getResourceAsStream("emotionml-to-maryxml.xsl"));
			stylesheet = tFactory.newTemplates(stylesheetStream);
		}
		if (dbFactory == null) {
			dbFactory = DocumentBuilderFactory.newInstance();
		}
		if (docBuilder == null) {
			docBuilder = dbFactory.newDocumentBuilder();
		}
		super.startup();
	}

	public MaryData process(MaryData d) throws Exception {
		Document emotionml = d.getDocument();

		// Let's be strict about what we accept as EmotionML:
		Checker emotionmlChecker = new Checker();
		emotionmlChecker.validate(emotionml);

		DOMSource domSource = new DOMSource(emotionml);

		Transformer transformer = stylesheet.newTransformer();
		// Log transformation errors to client:
		if (doWarnClient) {
			// Use custom error handler:
			transformer.setErrorListener(new LoggingErrorHandler(Thread.currentThread().getName()
					+ " client.EmotionML transformer"));
		}

		transformer.setParameter("voice", d.getDefaultVoice().getName());

		// Transform DOMSource into a DOMResult
		Document maryxmlDocument = docBuilder.newDocument();
		DOMResult domResult = new DOMResult(maryxmlDocument);
		transformer.transform(domSource, domResult);
		// We add the 'xml:lang' attribute manually:
		maryxmlDocument.getDocumentElement().setAttribute("xml:lang", MaryUtils.locale2xmllang(d.getLocale()));

		MaryData result = new MaryData(outputType(), d.getLocale());
		result.setDocument(maryxmlDocument);
		return result;
	}
}
