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
package marytts.util.dom;

// DOM classes
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.util.MaryUtils;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

/**
 * A collection of utilities for MaryXML DOM manipulation or analysis. No object of class MaryDomUtils is created, all methods are
 * static.
 * 
 * @author Marc Schr&ouml;der
 */
public class MaryDomUtils extends DomUtils {

	/**
	 * Create a new &lt;mtu&gt; element, inserted in the tree at the position of t and enclosing t.
	 * 
	 * @param t
	 *            the &lt;t&gt; element to enclose
	 * @param orig
	 *            the original text for the MTU, saved in the orig attribute
	 * @param accentPosition
	 *            optionally, specify an accent position, saved in the accent attribute of the mtu element. If null, no accent
	 *            attribute is inserted.
	 * @return the newly created MTU element.
	 */
	public static Element encloseWithMTU(Element t, String orig, String accentPosition) {
		if (!t.getNodeName().equals(MaryXML.TOKEN))
			throw new DOMException(DOMException.INVALID_ACCESS_ERR, "Only t elements allowed, received " + t.getNodeName() + ".");
		Element parent = (Element) t.getParentNode();
		assert parent != null;
		Document doc = t.getOwnerDocument();
		Element mtu = MaryXML.createElement(doc, MaryXML.MTU);
		mtu.setAttribute("orig", orig);
		// Which of the components gets a possible accent:
		if (accentPosition != null)
			mtu.setAttribute("accent", accentPosition);
		parent.insertBefore(mtu, t);
		mtu.appendChild(t);
		return mtu;
	}

	/**
	 * Create a new &lt;t&gt; element and insert it after t.
	 * 
	 * @param t
	 *            t
	 * @param newTokenText
	 *            newTokenText
	 * @return the new &lt;t&gt; element.
	 */
	public static Element appendToken(Element t, String newTokenText) {
		if (!t.getNodeName().equals(MaryXML.TOKEN))
			throw new DOMException(DOMException.INVALID_ACCESS_ERR, "Only t elements allowed, received " + t.getNodeName() + ".");
		Element parent = (Element) t.getParentNode();
		Document doc = t.getOwnerDocument();
		Element next = getNextSiblingElement(t);
		Element newT = MaryXML.createElement(doc, MaryXML.TOKEN);
		setTokenText(newT, newTokenText);
		parent.insertBefore(newT, next);
		return newT;
	}

	/**
	 * Convenience method returning the text string of a token element.
	 * 
	 * @param t
	 *            t
	 * @return getPlainTextBelow(t).trim()
	 */
	public static String tokenText(Element t) {
		if (!t.getNodeName().equals(MaryXML.TOKEN))
			throw new DOMException(DOMException.INVALID_ACCESS_ERR, "Only t elements allowed, received " + t.getNodeName() + ".");
		// Return all text nodes under t, concatenated and trimmed.
		return getPlainTextBelow(t).trim();
	}

	/**
	 * Convenience method for setting the text string of a token element.
	 * 
	 * @param t
	 *            t
	 * @param s
	 *            s
	 */
	public static void setTokenText(Element t, String s) {
		if (!t.getNodeName().equals(MaryXML.TOKEN))
			throw new DOMException(DOMException.INVALID_ACCESS_ERR, "Only " + MaryXML.TOKEN + " elements allowed, received "
					+ t.getNodeName() + ".");
		// Here, we rely on the fact that a t element has at most
		// one TEXT child with non-whitespace content:
		Document doc = t.getOwnerDocument();
		NodeIterator textIt = ((DocumentTraversal) doc).createNodeIterator(t, NodeFilter.SHOW_TEXT, null, false);
		Text text = null;
		String textString = null;
		while ((text = (Text) textIt.nextNode()) != null) {
			textString = text.getData().trim();
			if (!textString.equals(""))
				break;
		}
		if (text == null) { // token doesn't have a non-whitespace text child yet
			text = (Text) t.getOwnerDocument().createTextNode(s);
			t.appendChild(text);
		} else { // found the one text element with non-whitespace content
			// overwrite it:
			text.setData(s);
		}
	}

	/**
	 * Create a default boundary element belonging to document doc, but not yet attached. The boundary has a breakindex of 3 and
	 * an unknown tone.
	 * 
	 * @param doc
	 *            the maryxml document in which to create the boundary.
	 * @return boundary
	 */
	public static Element createBoundary(Document doc) {
		if (!doc.getDocumentElement().getTagName().equals(MaryXML.MARYXML))
			throw new DOMException(DOMException.INVALID_ACCESS_ERR, "Expected <" + MaryXML.MARYXML + "> document, received "
					+ doc.getDocumentElement().getTagName() + ".");
		Element boundary = MaryXML.createElement(doc, MaryXML.BOUNDARY);
		boundary.setAttribute("breakindex", "3");
		boundary.setAttribute("tone", "unknown");
		return boundary;
	}

	/**
	 * Try to determine the locale of a document by looking at the xml:lang attribute of the document element.
	 * 
	 * @param doc
	 *            the document in which to look for a locale.
	 * @return the locale set for the document, or null if no locale is set.
	 */
	public static Locale getLocale(Document doc) {
		if (doc.getDocumentElement().hasAttribute("xml:lang"))
			return MaryUtils.string2locale(doc.getDocumentElement().getAttribute("xml:lang"));
		return null;
	}

	/**
	 * Verify whether a given document is valid in the sense of Schema XML validation. Note that this implementation will merely
	 * return false if the document is not valid, but will not provide any details. Use a combination of document2String() and
	 * parseDocument() to get the detailed error message.
	 * 
	 * @param doc
	 *            The document to verify.
	 * @throws MaryConfigurationException
	 *             if the validation cannot be carried out
	 * @return true if the document is Schema valid, false if not.
	 */
	public static boolean isSchemaValid(Document doc) throws MaryConfigurationException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// The MaryNormalisedWriter works also for non-maryxml documents
		// and gives (because of XSLT) a more standardised form than
		// an XMLSerializer does.
		MaryNormalisedWriter mnw = new MaryNormalisedWriter();
		try {
			mnw.output(doc, baos);
		} catch (TransformerException te) {
			throw new MaryConfigurationException("Cannot serialize document for Schema-valid parsing", te);
		}
		try {
			parseDocument(new ByteArrayInputStream(baos.toByteArray()), true /* validating */);
		} catch (ParserConfigurationException pce) {
			throw new MaryConfigurationException("Problem setting up parser", pce);
		} catch (IOException ioe) {
			throw new MaryConfigurationException("IOException should not occur but it does", ioe);
		} catch (SAXException se) {
			// document is not schema valid
			return false;
		}
		return true;
	}

}
