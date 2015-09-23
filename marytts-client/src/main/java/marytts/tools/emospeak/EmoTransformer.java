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
package marytts.tools.emospeak;

import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import marytts.util.MaryUtils;

/**
 * 
 * @author Marc Schr&ouml;der
 */
public class EmoTransformer extends Thread {
	private int r;
	private ProsodyXMLDisplayer emoSpeak;

	private javax.xml.transform.TransformerFactory tFactory = null;
	private javax.xml.transform.Templates stylesheet = null;
	private javax.xml.transform.Transformer transformer = null;
	private javax.xml.parsers.DocumentBuilderFactory dbFactory = null;
	private javax.xml.parsers.DocumentBuilder docBuilder = null;

	private org.w3c.dom.Document emotionDocument = null;

	private boolean inputAvailable = false;
	private int activation;
	private int evaluation;
	private int power;
	private String text;
	private String maryxmlString;
	private Locale locale;

	private boolean exitRequested = false;

	/**
	 * Creates new EmoTransformer
	 * 
	 * @param emoSpeak
	 *            emoSpeak
	 * @throws TransformerConfigurationException
	 *             TransformerConfigurationException
	 * @throws ParserConfigurationException
	 *             ParserConfigurationException
	 */
	public EmoTransformer(ProsodyXMLDisplayer emoSpeak) throws TransformerConfigurationException, ParserConfigurationException {
		this.emoSpeak = emoSpeak;
		// Try to find a suitable XSLT transformer
		tFactory = javax.xml.transform.TransformerFactory.newInstance();
		/*
		 * if (false && tFactory instanceof org.apache.xalan.processor.TransformerFactoryImpl) { Hashtable xalanEnv = (new
		 * org.apache.xalan.xslt.EnvironmentCheck()).getEnvironmentHash(); String xalan2Version = (String)
		 * xalanEnv.get("version.xalan2x"); if (xalan2Version == null || xalan2Version.equals("")) xalan2Version = (String)
		 * xalanEnv.get("version.xalan2"); if (xalan2Version != null && !xalan2Version.equals("")) System.err.println("Using " +
		 * xalan2Version); } else {
		 */
		System.err.println("Using XSL processor " + tFactory.getClass().getName());
		// }
		javax.xml.transform.stream.StreamSource stylesheetStream = new javax.xml.transform.stream.StreamSource(
				EmoTransformer.class.getResourceAsStream("emotion-to-mary.xsl"));
		stylesheet = tFactory.newTemplates(stylesheetStream);
		dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		docBuilder = dbFactory.newDocumentBuilder();
		transformer = stylesheet.newTransformer();

	}

	/**
	 * Asynchronously set the latest emotion values. Overwrites any previous, unprocessed data.
	 * 
	 * @param activation
	 *            activation
	 * @param evaluation
	 *            evaluation
	 * @param power
	 *            power
	 * @param text
	 *            text
	 * @param locale
	 *            locale
	 * @param r
	 *            r
	 */
	public synchronized void setEmotionValues(int activation, int evaluation, int power, String text, Locale locale, int r) {
		this.activation = activation;
		this.evaluation = evaluation;
		this.power = power;
		this.text = text;
		this.locale = locale;
		inputAvailable = true;
		this.r = r;
		notifyAll();
	}

	public synchronized void requestExit() {
		exitRequested = true;
		notifyAll();
	}

	private void createEmotionDocument() {
		emotionDocument = docBuilder.getDOMImplementation().createDocument(null, "emotion", null);
		org.w3c.dom.Element e = emotionDocument.getDocumentElement();
		e.setAttributeNS("http://www.w3.org/XML/1998/namespace", "lang", MaryUtils.locale2xmllang(locale));
		e.setAttribute("activation", String.valueOf(activation));
		e.setAttribute("evaluation", String.valueOf(evaluation));
		e.setAttribute("power", String.valueOf(power));
		e.appendChild(emotionDocument.createTextNode(text));
	}

	private void transformToMaryXML() throws javax.xml.transform.TransformerException {
		javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource(emotionDocument);
		java.io.StringWriter sw = new java.io.StringWriter();
		javax.xml.transform.stream.StreamResult streamResult = new javax.xml.transform.stream.StreamResult(sw);
		transformer.transform(domSource, streamResult);
		maryxmlString = sw.toString();
	}

	private synchronized void doWait() {
		try {
			wait();
		} catch (InterruptedException e) {
		}
	}

	public void run() {
		while (!exitRequested) {
			if (inputAvailable) {
				inputAvailable = false;
				try {
					int r1 = r;
					System.err.println("EmoTransformer about to process request no. " + r1);
					createEmotionDocument();
					transformToMaryXML();
					System.err.println("EmoTransformer has processed.");
					emoSpeak.updateProsodyXML(maryxmlString, r1);
				} catch (javax.xml.transform.TransformerException e) {
					e.printStackTrace();
				}
			} else {
				doWait();
				System.err.println("EmoTransformer waking up from wait.");
			}
		}
		System.err.println("EmoTransformer exiting.");
	}
}
