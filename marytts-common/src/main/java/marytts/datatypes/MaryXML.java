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
package marytts.datatypes;

// Log4j classes
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import marytts.util.MaryUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Class containing only static methods. All knowledge about MaryXML documents is grouped here.
 */

public class MaryXML {
	public static final String MARYXML = "maryxml";
	public static final String PARAGRAPH = "p";
	public static final String SENTENCE = "s";
	public static final String VOICE = "voice";
	public static final String PHRASE = "phrase";
	public static final String MARK = "mark";
	public static final String SAYAS = "say-as";
	public static final String PHONOLOGY = "phonology";
	public static final String PROSODY = "prosody";
	public static final String AUDIO = "audio";
	public static final String BOUNDARY = "boundary";
	public static final String MTU = "mtu";
	public static final String TOKEN = "t";
	public static final String SYLLABLE = "syllable";
	public static final String PHONE = "ph";
	// public static final String NONVERBAL = "nvv";
	public static final String NONVERBAL = "vocalization";

	private static String version = "0.5";
	private static String namespace = "http://mary.dfki.de/2002/MaryXML";

	private static Logger logger = MaryUtils.getLogger("MaryXML");
	private static DocumentBuilder docBuilder = null;

	// Static constructor:
	static {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setExpandEntityReferences(true);
			factory.setNamespaceAware(true);
			docBuilder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.error("Cannot start up XML parser", e);
		}
	}

	public static String getVersion() {
		return version;
	}

	public static String getNamespace() {
		return namespace;
	}

	public static String getRootTagname() {
		return MARYXML;
	}

	public static Document newDocument() {
		Document doc = docBuilder.getDOMImplementation().createDocument(getNamespace(), getRootTagname(), null);
		Element root = doc.getDocumentElement();
		root.setAttribute("version", getVersion());
		root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		return doc;
	}

	/**
	 * In the given MaryXML document, create a new element of the given name, with the proper namespace.
	 * 
	 * @param doc
	 *            a MaryXML document
	 * @param elementName
	 *            the name of the MaryXML document
	 * @return the newly created MaryXML element.
	 * @throws IllegalArgumentException
	 *             if the document is not a maryxml document with the proper namespace.
	 */
	public static Element createElement(Document doc, String elementName) {
		if (doc == null)
			throw new NullPointerException("Received null document");
		if (!doc.getDocumentElement().getTagName().equals(getRootTagname()))
			throw new IllegalArgumentException("Not a maryxml document: " + doc.getDocumentElement().getTagName());
		if (doc.getDocumentElement().getNamespaceURI() == null) {
			throw new IllegalArgumentException("Document has no namespace!");
		}
		if (!doc.getDocumentElement().getNamespaceURI().equals(getNamespace()))
			throw new IllegalArgumentException("Document has wrong namespace: " + doc.getDocumentElement().getNamespaceURI());
		return doc.createElementNS(getNamespace(), elementName);
	}

	public static Element appendChildElement(Node node, String childName) {
		if (node == null)
			throw new NullPointerException("Received null node");
		return (Element) node.appendChild(createElement(node.getOwnerDocument(), childName));
	}

}
