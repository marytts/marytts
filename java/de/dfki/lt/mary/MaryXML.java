/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.mary;

// Log4j classes
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * Class containing only static methods. All knowledge about MaryXML documents
 * is grouped here.
 */

public class MaryXML
{
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
    
    private static String version = "0.4";
    private static String namespace = "http://mary.dfki.de/2002/MaryXML";

    private static Logger logger = Logger.getLogger("MaryXML");
    private static DocumentBuilder docBuilder = null;


    // Static constructor:
    static {
        try{
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(true);
            factory.setNamespaceAware(true);
            docBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            logger.error("Cannot start up XML parser", e);
        }
    }

    public static String getVersion() { return version; }
    public static String getNamespace() { return namespace; }
    public static String getRootTagname() { return MARYXML; }

    public static Document newDocument()
    {
        Document doc = docBuilder.getDOMImplementation().
            createDocument(getNamespace(), getRootTagname(), null);
        Element root = doc.getDocumentElement();
        root.setAttribute("version", getVersion());
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        return doc;
    }
    
    /**
     * In the given MaryXML document, create a new element of the given name,
     * with the proper namespace.
     * @param doc a MaryXML document
     * @param elementName the name of the MaryXML document
     * @return the newly created MaryXML element.
     * @throws IllegalArgumentException if the document is not a maryxml document
     * with the proper namespace.
     */
    public static Element createElement(Document doc, String elementName)
    {
    	if (doc == null)
    		throw new NullPointerException("Received null document");
    	if (!doc.getDocumentElement().getTagName().equals(getRootTagname()))
    		throw new IllegalArgumentException("Not a maryxml document: "
    		    + doc.getDocumentElement().getTagName());
		if (!doc.getDocumentElement().getNamespaceURI().equals(getNamespace()))
			throw new IllegalArgumentException("Document has wrong namespace: "
			    + doc.getDocumentElement().getNamespaceURI());
		return doc.createElementNS(getNamespace(), elementName);
    }

	public static Element appendChildElement(Node node, String childName)
	{
		if (node == null)
			throw new NullPointerException("Received null node");
	    return (Element) node.appendChild(createElement(node.getOwnerDocument(), childName));
	}

}
