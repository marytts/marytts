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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import marytts.datatypes.MaryXML;
import marytts.server.MaryProperties;
import marytts.util.MaryUtils;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;


/** A collection of utilities for MaryXML DOM manipulation or analysis.
 *  No object of class MaryDomUtils is created, all methods are static.
 * @author Marc Schr&ouml;der
 */
public class MaryDomUtils extends DomUtils
{
    // Static constructor:
    static {
        DomUtils.setup();
        MaryDomUtils.setup2();
    }


	/**
	 * 
	 */
	protected static void setup2() {
		try {
            validatingFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                "http://www.w3.org/2001/XMLSchema");
            // Specify other factory configuration settings
            validatingFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource",
                    MaryProperties.localSchemas());
        } catch (Exception x) {
            // This can happen if the parser does not support JAXP 1.2
            logger.warn("Cannot use Schema validation -- turning off validation.");
            validatingFactory.setValidating(false);
        }
        entityResolver = new EntityResolver() {
            public InputSource resolveEntity (String publicId, String systemId)
            {
                if (systemId.equals("http://mary.dfki.de/lib/Sable.v0_2.dtd")) {
                    try {
                        // return a local copy of the sable dtd:
                        String localSableDTD = MaryProperties.maryBase() + "/lib/Sable.v0_2.mary.dtd";
                        return new InputSource(new FileReader(localSableDTD));
                    } catch (FileNotFoundException e) {
                        logger.warn("Cannot find local Sable.v0_2.mary.dtd");
                    }
                } else if (systemId.equals("http://mary.dfki.de/lib/sable-latin.ent")) {
                    try {
                        // return a local copy of the sable dtd:
                        String localFilename = MaryProperties.maryBase() + "/lib/sable-latin.ent";
                        return new InputSource(new FileReader(localFilename));
                    } catch (FileNotFoundException e) {
                        logger.warn("Cannot find local sable-latin.ent");
                    }
                } else if (systemId.equals("http://mary.dfki.de/lib/apml.dtd")
                        || !systemId.startsWith("http")&&systemId.endsWith("apml.dtd")) {
                    try {
                        // return a local copy of the apml dtd:
                        String localFilename = MaryProperties.maryBase() + "/lib/apml.dtd";
                        return new InputSource(new FileReader(localFilename));
                    } catch (FileNotFoundException e) {
                        logger.warn("Cannot find local apml.dtd");
                    }
                }
                // else, use the default behaviour:
                return null;
            }
        };
	}

    /**
     * Create a new <mtu> element, inserted in the tree at the position of t
     * and enclosing t.
     * @param t the <t> element to enclose
     * @param orig the original text for the MTU, saved in the orig attribute
     * @param accentPosition optionally, specify an accent position, saved in
     * the accent attribute of the mtu element. If null, no accent attribute is
     * inserted.
     * @return the newly created MTU element.
     */
    public static Element encloseWithMTU(Element t, String orig, String accentPosition)
    {
        if (!t.getNodeName().equals(MaryXML.TOKEN))
            throw new DOMException(DOMException.INVALID_ACCESS_ERR,
                                   "Only t elements allowed, received " +
                                   t.getNodeName() + ".");
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
     * Create a new <t> element and insert it after t.
     * @return the new <t> element.
     */
    public static Element appendToken(Element t, String newTokenText)
    {
        if (!t.getNodeName().equals(MaryXML.TOKEN))
            throw new DOMException(DOMException.INVALID_ACCESS_ERR,
                                   "Only t elements allowed, received " +
                                   t.getNodeName() + ".");
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
     */
    public static String tokenText(Element t)
    {
        if (!t.getNodeName().equals(MaryXML.TOKEN))
            throw new DOMException(DOMException.INVALID_ACCESS_ERR,
                                   "Only t elements allowed, received " +
                                   t.getNodeName() + ".");
        // Return all text nodes under t, concatenated and trimmed.
        return getPlainTextBelow(t).trim();
    }

    /**
     * Convenience method for setting the text string of a token element.
     */
    public static void setTokenText(Element t, String s)
    {
        if (!t.getNodeName().equals(MaryXML.TOKEN))
            throw new DOMException(DOMException.INVALID_ACCESS_ERR,
                                   "Only " + MaryXML.TOKEN + " elements allowed, received " +
                                   t.getNodeName() + ".");
        // Here, we rely on the fact that a t element has at most
        // one TEXT child with non-whitespace content:
        Document doc = t.getOwnerDocument();
        NodeIterator textIt = ((DocumentTraversal)doc).
            createNodeIterator(t, NodeFilter.SHOW_TEXT, null, false);
        Text text = null;
        String textString = null;
        while ((text = (Text) textIt.nextNode()) != null) {
            textString = text.getData().trim();
            if (!textString.equals("")) break;
        }
        if (text == null) { // token doesn't have a non-whitespace text child yet
            text = (Text)t.getOwnerDocument().createTextNode(s);
            t.appendChild(text);
        } else { // found the one text element with non-whitespace content
            // overwrite it:
            text.setData(s);
        }
    }

    /**
     * Create a default boundary element belonging to document doc, but not yet
     * attached. The boundary has a breakindex of 3 and an unknown tone.
     * @param doc the maryxml document in which to create the boundary.
     */
    public static Element createBoundary(Document doc)
    {
        if (!doc.getDocumentElement().getTagName().equals(MaryXML.MARYXML))
            throw new DOMException
                (DOMException.INVALID_ACCESS_ERR,
                 "Expected <" + MaryXML.MARYXML + "> document, received " +
                 doc.getDocumentElement().getTagName() + ".");
        Element boundary = MaryXML.createElement(doc, MaryXML.BOUNDARY);
        boundary.setAttribute("breakindex", "3");
        boundary.setAttribute("tone", "unknown");
        return boundary;
    }

    /**
     * Try to determine the locale of a document by looking at the xml:lang attribute of the document element.
     * @param doc the document in which to look for a locale.
     * @return the locale set for the document, or null if no locale is set.
     */
    public static Locale getLocale(Document doc) 
    {
        if (doc.getDocumentElement().hasAttribute("xml:lang"))
            return MaryUtils.string2locale(doc.getDocumentElement().getAttribute("xml:lang"));
        return null;
    }

    /**
     * Verify whether a given document is valid in the sense of Schema
     * XML validation.
     * @param doc The document to verify.
     * @throws Exception if the document cannot be Schema validated.
     */
    public static void verifySchemaValid(Document doc) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // The MaryNormalisedWriter works also for non-maryxml documents
        // and gives (because of XSLT) a more standardised form than
        // an XMLSerializer does.
        MaryNormalisedWriter mnw = new MaryNormalisedWriter();
        mnw.output(doc, baos);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(true);
        factory.setNamespaceAware(true);
        factory.setValidating(true);
        factory.setIgnoringElementContentWhitespace(true);
        try {
            factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
            // Specify other factory configuration settings
            factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", MaryProperties.localSchemas());
        } catch (Exception x) {
            // This can happen if the parser does not support JAXP 1.2
            throw new Exception("Parser does not seem to support Schema validation", x);
        }
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        docBuilder.setErrorHandler(new ErrorHandler() {
            public void error(SAXParseException e) throws SAXParseException { throw e; }  
            public void fatalError(SAXParseException e) throws SAXParseException { throw e; }  
            public void warning(SAXParseException e) throws SAXParseException { throw e; }  
        });
        docBuilder.parse(new ByteArrayInputStream(baos.toByteArray()));
    }

}

