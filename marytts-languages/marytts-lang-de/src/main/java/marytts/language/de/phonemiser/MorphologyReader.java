/**
 * Copyright 2005 DFKI GmbH.
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

package marytts.language.de.phonemiser;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import marytts.exceptions.MaryConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MorphologyReader {

	public MorphologyReader() {

	}

	public Hashtable<String, String> loadInputModel(InputStream modelStream) throws ParserConfigurationException, SAXException,
			IOException, MaryConfigurationException {
		Hashtable<String, String> modelTable;
		modelTable = new Hashtable<String, String>(40);

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = factory.newDocumentBuilder();
		Document doc = parser.parse(modelStream);
		Element rootNode = doc.getDocumentElement();

		if (rootNode.getNodeName().equals("entries")) {
			NodeList entriesList = rootNode.getChildNodes();

			for (int i = 0; i < entriesList.getLength(); i++) {

				if (entriesList.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
					Element entry = (Element) entriesList.item(i);
					NodeList keyList = entry.getElementsByTagName("key");
					NodeList valueList = entry.getElementsByTagName("value");
					if (keyList.getLength() == 1 && valueList.getLength() == 1) {
						NodeList keyTextNodeList = ((Element) keyList.item(0)).getChildNodes();
						NodeList valueTextNodeList = ((Element) valueList.item(0)).getChildNodes();

						String keyString = "";
						String valueString = "";

						for (int a = 0; a < keyTextNodeList.getLength(); a++) {
							if (keyTextNodeList.item(a).getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
								org.w3c.dom.Text textNode = (org.w3c.dom.Text) keyTextNodeList.item(a);
								keyString += textNode.getData().trim();
							}
						}

						for (int a = 0; a < valueTextNodeList.getLength(); a++) {
							if (valueTextNodeList.item(a).getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
								org.w3c.dom.Text textNode = (org.w3c.dom.Text) valueTextNodeList.item(a);
								valueString += textNode.getData().trim();
							}
						}
						modelTable.put(keyString, valueString);
					} else {
						System.out.println("ERROR: The " + i + "th entry contains wrong number of subElements !!");
					}
				}
			}
		} else {
			throw new MaryConfigurationException("Document is not wellformed. Top-level element should be <entries>.");
		}
		return modelTable;
	}

	/**
	 * Prints a textual representation of a DOM object into a text string..
	 *
	 * @param element
	 *            DOM object to parse.
	 * @return String representation of <i>document</i>.
	 */
	public static String document2String(Element element) {
		String result = null;

		StringWriter strWtr = new StringWriter();
		StreamResult strResult = new StreamResult(strWtr);
		TransformerFactory tfac = TransformerFactory.newInstance();
		try {
			Transformer trans = tfac.newTransformer();
			trans.setOutputProperty("omit-xml-declaration", "yes");
			trans.transform(new DOMSource(element), strResult);
		} catch (Exception e) {
			System.err.println("XMLUtils.document2String(): " + e);
		}
		result = strResult.getWriter().toString();

		return result;
	}// document2String()

}
