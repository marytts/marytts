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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import marytts.exceptions.MaryConfigurationException;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A collection of utilities for DOM manipulation or analysis. No object of class DomUtils is created, all methods are static.
 * 
 * @author Marc Schr&ouml;der, Sathish and Ingmar
 */
public class DomUtils {
	protected static DocumentBuilderFactory factory;
	protected static DocumentBuilderFactory validatingFactory;

	protected static Logger logger = MaryUtils.getLogger("DomUtils");

	// Static constructor:
	static {
		factory = DocumentBuilderFactory.newInstance();
		factory.setExpandEntityReferences(true);
		factory.setNamespaceAware(true);

		validatingFactory = DocumentBuilderFactory.newInstance();
		validatingFactory.setExpandEntityReferences(true);
		validatingFactory.setNamespaceAware(true);
		validatingFactory.setIgnoringElementContentWhitespace(true);
		validatingFactory.setValidating(true);
		try {
			validatingFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
					"http://www.w3.org/2001/XMLSchema");
			// Specify other factory configuration settings
			Object[] schemas = new Object[] { DomUtils.class.getResource("xml.xsd").toString(),
					DomUtils.class.getResource("MaryXML.xsd").toString() };
			validatingFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", schemas);
		} catch (Exception x) {
			// This can happen if the parser does not support JAXP 1.2
			logger.warn("Cannot use Schema validation -- disabling validating parser factory.");
			validatingFactory = null;
		}
	}

	/**
	 * Parse XML data into a DOM representation, taking local resources and Schemas into account.
	 * 
	 * @param inputData
	 *            a string representation of the XML data to be parsed.
	 * @param validating
	 *            whether to Schema-validate the XML data
	 * @return the DOM document resulting from the parse
	 * @throws ParserConfigurationException
	 *             if no parser could be created
	 * @throws SAXException
	 *             if there was a parse error
	 * @throws IOException
	 *             if there was a problem reading from the string
	 */
	public static Document parseDocument(String inputData, boolean validating) throws ParserConfigurationException, SAXException,
			IOException {
		return parseDocument(new StringReader(inputData), validating);
	}

	/**
	 * Parse XML data into a DOM representation, taking local resources and Schemas into account.
	 * 
	 * @param inputData
	 *            a reader from which the XML data is to be read.
	 * @param validating
	 *            whether to Schema-validate the XML data
	 * @return the DOM document resulting from the parse
	 * @throws ParserConfigurationException
	 *             if no parser could be created
	 * @throws SAXException
	 *             if there was a parse error
	 * @throws IOException
	 *             if there was a problem reading from the reader
	 */
	public static Document parseDocument(Reader inputData, boolean validating) throws ParserConfigurationException, SAXException,
			IOException {
		DocumentBuilder builder = createDocumentBuilder(validating);
		/*
		 * Set Entity resolver for APML and SABLE
		 */
		builder.setEntityResolver(new MaryEntityResolver());

		return builder.parse(new InputSource(inputData));
	}

	/**
	 * Parse XML data into a DOM representation, taking local resources and Schemas into account.
	 * 
	 * @param file
	 *            a file from which the XML data is to be read.
	 * @param validating
	 *            whether to Schema-validate the XML data
	 * @return the DOM document resulting from the parse
	 * @throws ParserConfigurationException
	 *             if no parser could be created
	 * @throws SAXException
	 *             if there was a parse error
	 * @throws IOException
	 *             if there was a problem reading from the file
	 */
	public static Document parseDocument(File file, boolean validating) throws ParserConfigurationException, SAXException,
			IOException {
		return parseDocument(new FileInputStream(file), validating);
	}

	/**
	 * Parse XML data into a DOM representation, taking local resources and Schemas into account.
	 * 
	 * @param is
	 *            an input stream from which the XML data is to be read.
	 * @param validating
	 *            whether to Schema-validate the XML data
	 * @return the DOM document resulting from the parse
	 * @throws ParserConfigurationException
	 *             if no parser could be created
	 * @throws SAXException
	 *             if there was a parse error
	 * @throws IOException
	 *             if there was a problem reading from the stream
	 */
	public static Document parseDocument(InputStream is, boolean validating) throws ParserConfigurationException, SAXException,
			IOException {
		DocumentBuilder builder = createDocumentBuilder(validating);
		return builder.parse(is);
	}

	/**
	 * @param validating
	 *            validating
	 * @return builder
	 * @throws ParserConfigurationException
	 *             ParserConfigurationException
	 */
	private static DocumentBuilder createDocumentBuilder(boolean validating) throws ParserConfigurationException {
		DocumentBuilder builder;
		if (validating) {
			if (validatingFactory == null) {
				throw new ParserConfigurationException("No validating parser factory available");
			} else if (!validatingFactory.isValidating()) {
				throw new ParserConfigurationException("factory should be validating but isn't");
			}
			builder = validatingFactory.newDocumentBuilder();
			assert builder.isValidating();
			builder.setErrorHandler(new ErrorHandler() {
				public void error(SAXParseException e) throws SAXParseException {
					throw e;
				}

				public void fatalError(SAXParseException e) throws SAXParseException {
					throw e;
				}

				public void warning(SAXParseException e) throws SAXParseException {
					throw e;
				}
			});
		} else {
			builder = factory.newDocumentBuilder();
		}
		return builder;
	}

	/**
	 * DOM-parse the given input data. Namespace-aware but non-validating.
	 * 
	 * @param inputData
	 *            inputData
	 * @return parseDocument(inputData, false)
	 * @throws ParserConfigurationException
	 *             ParserConfigurationException
	 * @throws SAXException
	 *             SAXException
	 * @throws IOException
	 *             IOException
	 */
	public static Document parseDocument(String inputData) throws ParserConfigurationException, SAXException, IOException {
		return parseDocument(inputData, false);
	}

	/**
	 * DOM-parse the given input data. Namespace-aware but non-validating.
	 * 
	 * @param inputData
	 *            inputData
	 * @return parseDocument(inputData, false)
	 * @throws ParserConfigurationException
	 *             ParserConfigurationException
	 * @throws SAXException
	 *             SAXException
	 * @throws IOException
	 *             IOException
	 */
	public static Document parseDocument(Reader inputData) throws ParserConfigurationException, SAXException, IOException {
		return parseDocument(inputData, false);
	}

	/**
	 * DOM-parse the given input data. Namespace-aware but non-validating.
	 * 
	 * @param file
	 *            file
	 * @return parseDocument(file, false)
	 * @throws ParserConfigurationException
	 *             ParserConfigurationException
	 * @throws SAXException
	 *             SAXException
	 * @throws IOException
	 *             IOException
	 */
	public static Document parseDocument(File file) throws ParserConfigurationException, SAXException, IOException {
		return parseDocument(file, false);
	}

	/**
	 * DOM-parse the given input data. Namespace-aware but non-validating.
	 * 
	 * @param inputData
	 *            inputData
	 * @return parseDocument(inputData, false)
	 * @throws ParserConfigurationException
	 *             ParserConfigurationException
	 * @throws SAXException
	 *             SAXException
	 * @throws IOException
	 *             IOException
	 */
	public static Document parseDocument(InputStream inputData) throws ParserConfigurationException, SAXException, IOException {
		return parseDocument(inputData, false);
	}

	/**
	 * Verify if <code>ancestor</code> is an ancestor of <code>node</code>
	 * 
	 * @param ancestor
	 *            ancestor
	 * @param node
	 *            node
	 * @return true if ancestor equals to p, false otherwise
	 */
	public static boolean isAncestor(Node ancestor, Node node) {
		Node p = node;
		while ((p = p.getParentNode()) != null) {
			if (ancestor == p)
				return true;
		}
		return false;
	}

	/**
	 * Verify if <code>node</code> has an ancestor with name <code>ancestorName</code>
	 * 
	 * @param node
	 *            node
	 * @param ancestorName
	 *            ancestorName
	 * @return true if p.getNodeName equals ancestorName, false otherwise
	 */
	public static boolean hasAncestor(Node node, String ancestorName) {
		Node p = node;
		// System.err.println("Searching for ancestor \"" + ancestorName + "\" for token \"" + p.getNodeName() + "\"");
		while ((p = p.getParentNode()) != null) {
			// System.err.println("  Looking at next parent node: " + p.getNodeName());
			if (p.getNodeName().equals(ancestorName))
				return true;
		}
		return false;
	}

	/**
	 * If <code>node</code> has an ancestor with name <code>ancestorName</code>, return that ancestor. Else return
	 * <code>null</code>.
	 * 
	 * @param node
	 *            node
	 * @param ancestorName
	 *            ancestorName
	 * @return p if p.getNodeName equals ancestorName
	 */
	public static Node getAncestor(Node node, String ancestorName) {
		Node p = node;
		while ((p = p.getParentNode()) != null) {
			if (p.getNodeName().equals(ancestorName))
				return p;
		}
		return null;
	}

	/**
	 * If <code>node</code> has an ancestor with one of the names in <code>ancestorNames</code>, return the closest of these
	 * ancestors. Else return <code>null</code>.
	 * 
	 * @param node
	 *            node
	 * @param ancestorNames
	 *            ancestorNames
	 * @return closestAncestor
	 */
	public static Node getAncestor(Node node, String[] ancestorNames) {
		if (ancestorNames.length <= 0)
			throw new IllegalArgumentException("No ancestorNames provided.");
		Node closestAncestor = null;
		for (int i = 0; i < ancestorNames.length; i++) {
			Node ancestor = getAncestor(node, ancestorNames[i]);
			if (ancestor != null) {
				if (closestAncestor == null) {
					closestAncestor = ancestor;
				} else if (isAncestor(closestAncestor, ancestor)) { // new one is closer than closest so far
					closestAncestor = ancestor;
				} // else leave as is
			}
		}
		return closestAncestor;
	}

	/**
	 * Search upwards through the ancestors of <code>node</code> with name <code>ancestorName</code>, and return the first
	 * ancestor for which an attribute named <code>attributeName</code> is present. Return <code>null</code> if no such ancestor
	 * exists.
	 * 
	 * @param node
	 *            node
	 * @param ancestorName
	 *            ancestorName
	 * @param attributeName
	 *            attributeName
	 * @return (Element) a
	 */
	public static Element getClosestAncestorWithAttribute(Node node, String ancestorName, String attributeName) {
		for (Node a = getAncestor(node, ancestorName); a != null; a = getAncestor(a, ancestorName)) {
			if (a.getNodeType() == Node.ELEMENT_NODE && ((Element) a).hasAttribute(attributeName)) {
				// Recursion ends here.
				return (Element) a;
			}
		}
		// No such ancestor.
		return null;
	}

	/**
	 * Climb through <code>node</code>'s ancestors, looking for one with an attribute named <code>attributeName</code>,
	 * irrespective of the respective <code>ancestorName</code>, and return the attribute's value
	 * 
	 * @param node
	 *            node
	 * @param attributeName
	 *            attributeName
	 * @return value of attribute from closest ancestor with that attribute, or the empty string if no ancestor has that
	 *         attribute.
	 * 
	 */
	public static String getAttributeFromClosestAncestorOfAnyKind(Node node, String attributeName) {
		Node parentNode;
		while (node != null && (parentNode = node.getParentNode()) != null) {
			if (parentNode.hasAttributes()) {
				Element parentElement = (Element) parentNode;
				if (parentElement.hasAttribute(attributeName)) {
					return parentElement.getAttribute(attributeName);
				}
			}
			node = parentNode;
		}
		return "";
	}

	/**
	 * If <code>node</code> has ancestors with name <code>ancestorName</code>, return the one closest to the root. If there is no
	 * ancestor with that name, return <code>null</code>.
	 * 
	 * @param node
	 *            node
	 * @param ancestorName
	 *            ancestorName
	 * @return highest
	 */
	public static Node getHighestLevelAncestor(Node node, String ancestorName) {
		Node p = node;
		Node highest = null;
		while ((p = getAncestor(p, ancestorName)) != null) {
			highest = p;
		}
		return highest;
	}

	/**
	 * Get the next sibling of <code>e</code> which is an element, or <code>null</code> if there is no such element.
	 * 
	 * @param e
	 *            e
	 * @return null if n is e, true otherwise
	 */
	public static Element getNextSiblingElement(Element e) {
		Node n = e;
		if (n == null)
			return null;
		while ((n = n.getNextSibling()) != null) {
			if (n.getNodeType() == Node.ELEMENT_NODE)
				return (Element) n;
		}
		return null;
	}

	/**
	 * Get the previous sibling of <code>e</code> which is an element, or <code>null</code> if there is no such element.
	 * 
	 * @param e
	 *            e
	 * @return null if n is null, true otherwise
	 */
	public static Element getPreviousSiblingElement(Element e) {
		Node n = e;
		if (n == null)
			return null;
		while ((n = n.getPreviousSibling()) != null) {
			if (n.getNodeType() == Node.ELEMENT_NODE)
				return (Element) n;
		}
		return null;
	}

	/**
	 * Get the next sibling of <code>e</code> which is an element and has tag name <code>name</code>, or <code>null</code> if
	 * there is no such element.
	 * 
	 * @param e
	 *            e
	 * @param name
	 *            name
	 * @return null if n is null, true otherwise
	 */
	public static Element getNextSiblingElementByTagName(Element e, String name) {
		Node n = e;
		if (n == null)
			return null;
		while ((n = n.getNextSibling()) != null) {
			if (n.getNodeType() == Node.ELEMENT_NODE && ((Element) n).getTagName().equals(name))
				return (Element) n;
		}
		return null;
	}

	/**
	 * Get the previous sibling of <code>e</code> which is an element and has tag name <code>name</code>, or <code>null</code> if
	 * there is no such element.
	 * 
	 * @param e
	 *            e
	 * @param name
	 *            name
	 * @return null if n is null
	 */
	public static Element getPreviousSiblingElementByTagName(Element e, String name) {
		Node n = e;
		if (n == null)
			return null;
		while ((n = n.getPreviousSibling()) != null) {
			if (n.getNodeType() == Node.ELEMENT_NODE && ((Element) n).getTagName().equals(name))
				return (Element) n;
		}
		return null;
	}

	/**
	 * Get the first child of <code>e</code> which is an element, or <code>null</code> if there is no such element.
	 * 
	 * @param e
	 *            e
	 * @return n
	 */
	public static Element getFirstChildElement(Element e) {
		Node n = e.getFirstChild();
		while (n != null && n.getNodeType() != Node.ELEMENT_NODE) {
			n = n.getNextSibling();
		}
		// Now n is either null or an Element
		return (Element) n;
	}

	/**
	 * Get the last child of <code>e</code> which is an element, or <code>null</code> if there is no such element.
	 * 
	 * @param e
	 *            e
	 * @return n
	 */
	public static Element getLastChildElement(Element e) {
		Node n = e.getLastChild();
		while (n != null && n.getNodeType() != Node.ELEMENT_NODE) {
			n = n.getPreviousSibling();
		}
		// Now n is either null or an Element
		return (Element) n;
	}

	/**
	 * Get the first child element with the given tag name, or <code>null</code> if there is no such element.
	 * 
	 * @param n
	 *            n
	 * @param name
	 *            name
	 * @return tx.nextNode
	 */
	public static Element getFirstElementByTagName(Node n, String name) {
		Document doc = (n instanceof Document) ? (Document) n : n.getOwnerDocument();
		TreeWalker tw = ((DocumentTraversal) doc).createTreeWalker(n, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(name), true);
		return (Element) tw.nextNode();
	}

	/**
	 * Get the last descendant element with the given tag name, or <code>null</code> if there is no such element.
	 * 
	 * @param e
	 *            e
	 * @param name
	 *            name
	 * @return previous
	 */
	public static Element getLastElementByTagName(Element e, String name) {
		// This implementation is certainly inefficient, but I have
		// no better idea at the moment.
		TreeWalker tw = ((DocumentTraversal) e.getOwnerDocument()).createTreeWalker(e, NodeFilter.SHOW_ELEMENT,
				new NameNodeFilter(name), true);
		Node current = null;
		Node previous = null;
		while ((current = tw.nextNode()) != null)
			previous = current;
		return (Element) previous;
	}

	public static boolean isFirstOfItsKindIn(Node node, String ancestorName) {
		return isFirstOfItsKindIn(node, getAncestor(node, ancestorName));
	}

	public static boolean isFirstOfItsKindIn(Node node, Node ancestor) {
		if (ancestor == null)
			return false;
		Document doc = node.getOwnerDocument();
		if (doc == null)
			return false;
		TreeWalker tw = ((DocumentTraversal) doc).createTreeWalker(doc, NodeFilter.SHOW_ALL,
				new NameNodeFilter(node.getNodeName()), true);
		tw.setCurrentNode(node);
		Node prev = tw.previousNode();

		return prev == null || // no node with same name before this one
				!isAncestor(ancestor, prev); // prev is not in the same ancestor
	}

	public static boolean isLastOfItsKindIn(Node node, String ancestorName) {
		return isLastOfItsKindIn(node, getAncestor(node, ancestorName));
	}

	public static boolean isLastOfItsKindIn(Node node, Node ancestor) {
		if (node == null || ancestor == null)
			return false;
		Document doc = node.getOwnerDocument();
		if (doc == null)
			return false;
		TreeWalker tw = ((DocumentTraversal) doc).createTreeWalker(doc, NodeFilter.SHOW_ALL,
				new NameNodeFilter(node.getNodeName()), true);
		tw.setCurrentNode(node);
		Node next = tw.nextNode();

		return next == null || // no node with same name after this one
				!isAncestor(ancestor, next); // next is not in the same ancestor
	}

	/**
	 * Find the Element with the same tag name as <code>element</code> preceding <code>element</code> within the same subtree
	 * under <code>root</code>. Precondition: <code>root</code> must be an ancestor of <code>element</code>.
	 * 
	 * @param element
	 *            element
	 * @param root
	 *            root
	 * @return that Element, or <code>null</code> if there is no such Element.
	 */
	public static Element getPreviousOfItsKindIn(Element element, Element root) {
		if (element == null || root == null)
			return null;
		String tagname = element.getTagName();
		TreeWalker tw = ((DocumentTraversal) element.getOwnerDocument()).createTreeWalker(root, NodeFilter.SHOW_ELEMENT,
				new NameNodeFilter(tagname), true);
		tw.setCurrentNode(element);
		for (Node previous = tw.previousNode(); previous != null; previous = tw.previousNode()) {
			if (previous.getNodeName().equals(tagname)) {
				return (Element) previous;
			}
		}
		return null;
	}

	/**
	 * Find the Element with the same tag name as <code>element</code> following <code>element</code> within the same subtree
	 * under <code>root</code>. Precondition: <code>root</code> must be an ancestor of <code>element</code>.
	 * 
	 * @param element
	 *            element
	 * @param root
	 *            root
	 * @return that Element, or <code>null</code> if there is no such Element.
	 */
	public static Element getNextOfItsKindIn(Element element, Element root) {
		if (element == null || root == null)
			return null;
		String tagname = element.getTagName();
		TreeWalker tw = ((DocumentTraversal) element.getOwnerDocument()).createTreeWalker(root, NodeFilter.SHOW_ELEMENT,
				new NameNodeFilter(tagname), true);
		tw.setCurrentNode(element);
		for (Node next = tw.nextNode(); next != null; next = tw.nextNode()) {
			if (next.getNodeName().equals(tagname)) {
				return (Element) next;
			}
		}
		return null;
	}

	public static Element closestCommonAncestor(Node first, Node last) {
		// Valid input?
		if (first == null || last == null)
			return null;
		Element a = (Element) first.getParentNode();
		while (a != null && !isAncestor(a, last))
			a = (Element) a.getParentNode();
		// if (a == null)
		// throw new Exception("Elements have no common ancestor");
		// Now a is the closest common ancestor of both Elements.
		return a;
	}

	/**
	 * Create a new Element placed in the document tree such that it encloses two existing Nodes. The new element will have the
	 * same namespace as the document element.
	 * <p>
	 * Algorithm:
	 * <ol>
	 * <li>Find closest common ancestor <code>a</code></li>
	 * <li>Identify first (<code>childA</code>) and last (<code>childZ</code>) of <code>a</code>'s children that need to be
	 * enclosed by the new Element</li>
	 * <li>Insert a new Element node before the first of these children</li>
	 * <li>Move all children between <code>childA</code> and <code>childZ</code> into the new Element.</li>
	 * </ol>
	 * 
	 * @param first
	 *            first
	 * @param last
	 *            last
	 * @param newElementName
	 *            new element name
	 * @return The new element, or null if it could not be created.
	 */
	public static Element encloseNodesWithNewElement(Node first, Node last, String newElementName) {
		// Valid input?
		if (first == null || last == null)
			return null;

		// 1. Find closest common ancestor a
		Node a = closestCommonAncestor(first, last);
		// Now a is the closest common ancestor of both Elements.
		if (a == null)
			return null;

		// 2. Identify first (childA) and last (childZ) of a's children
		// that need to be enclosed by the new Element.
		Node childA = first;
		while (childA.getParentNode() != a)
			childA = childA.getParentNode();
		Node childZ = last;
		while (childZ.getParentNode() != a)
			childZ = childZ.getParentNode();

		// 3. Insert a new Element node before the first of these children
		Document doc = (a.getNodeType() == Node.DOCUMENT_NODE) ? (Document) a : a.getOwnerDocument();
		Element newElement = doc.createElementNS(doc.getDocumentElement().getNamespaceURI(), newElementName);
		a.insertBefore(newElement, childA); // throws DOMException

		// 4. Move all children between childA and childZ into the new Element.
		Node c = childA;
		Node helper;
		while (c != null && c != childZ) {
			helper = c.getNextSibling();
			newElement.appendChild(c); // throws DOMException
			c = helper;
		}
		newElement.appendChild(childZ); // throws DOMException

		return newElement;
	} // encloseElementsWithNewElement

	public static List<Node> getNodeListAsList(NodeList nl) {
		ArrayList<Node> l = new ArrayList<Node>();
		for (int i = 0; i < nl.getLength(); i++) {
			l.add(nl.item(i));
		}
		return l;
	}

	/**
	 * Return the concatenation of the values of all text nodes below the given node. One space character is inserted between
	 * adjacent text nodes.
	 * 
	 * @param n
	 *            n
	 * @return null if n is null
	 */
	public static String getPlainTextBelow(Node n) {
		if (n == null)
			return null;
		Document doc = null;
		if (n.getNodeType() == Node.DOCUMENT_NODE) {
			doc = (Document) n;
		} else {
			doc = n.getOwnerDocument();
		}
		StringBuilder buf = new StringBuilder();
		NodeIterator it = ((DocumentTraversal) doc).createNodeIterator(n, NodeFilter.SHOW_TEXT, null, true);
		Text text = null;
		while ((text = (Text) it.nextNode()) != null) {
			buf.append(text.getData().trim());
			buf.append(" ");
		}
		return buf.toString();
	}

	/**
	 * Analogous to the Node.insertBefore() method, insert a newNode after a refNode.
	 * 
	 * @param newNode
	 *            new node
	 * @param refNode
	 *            ref node
	 * @throws DOMException
	 *             DOMException
	 */
	public static void insertAfter(Node newNode, Node refNode) throws DOMException {
		Node parent = refNode.getParentNode();
		Node next = refNode.getNextSibling();
		if (next == null) {
			parent.appendChild(newNode);
		} else {
			parent.insertBefore(newNode, next);
		}
	}

	/**
	 * Go through all text nodes below this node, and replace their text with a trimmed version of their text. This changes the
	 * DOM document.
	 * 
	 * @param root
	 *            root
	 */
	public static void trimAllTextNodes(Node root) {
		Document doc = root.getNodeType() == Node.DOCUMENT_NODE ? (Document) root : root.getOwnerDocument();
		NodeIterator it = ((DocumentTraversal) doc).createNodeIterator(root, NodeFilter.SHOW_TEXT, null, false);
		Text t = null;
		while ((t = (Text) it.nextNode()) != null) {
			String s = t.getData();
			t.setData(s.trim());
		}
	}

	/**
	 * Serialize a Document to a String.
	 * 
	 * @param doc
	 *            doc
	 * @return string
	 */
	public static String serializeToString(Document doc) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			// The MaryNormalisedWriter works also for non-maryxml documents
			// and gives (because of XSLT) a more standardised form than
			// an XMLSerializer does.
			MaryNormalisedWriter mnw = new MaryNormalisedWriter();
			mnw.output(doc, baos);
		} catch (Exception e1) {
			return "";
		}
		return baos.toString();
	}

	/**
	 * Write a DOM document into a string.
	 * 
	 * @param document
	 *            the document to be written
	 * @throws MaryConfigurationException
	 *             if the DOM document cannot be serialized
	 * @return the string containing the DOM document
	 */
	public static String document2String(Document document) throws MaryConfigurationException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		document2Stream(document, baos);
		try {
			return new String(baos.toByteArray(), "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			throw new MaryConfigurationException("oops", uee);
		}
	}

	/**
	 * Write a DOM document into a file.
	 * 
	 * @param document
	 *            the document to be written
	 * @param file
	 *            where to write it.
	 * @throws MaryConfigurationException
	 *             if the DOM document cannot be serialized
	 * @throws IOException
	 *             if there is a problem accessing the file
	 */
	public static void document2File(Document document, File file) throws MaryConfigurationException, IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			document2Stream(document, fos);
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	/**
	 * Write a DOM document into an output stream.
	 * 
	 * @param document
	 *            the document to be written
	 * @param target
	 *            where to write it.
	 * @throws MaryConfigurationException
	 *             if the DOM document cannot be serialized
	 */
	public static void document2Stream(Document document, OutputStream target) throws MaryConfigurationException {
		LSSerializer serializer = null;
		DOMImplementationLS domImplLS = null;
		try {
			DOMImplementation implementation = DOMImplementationRegistry.newInstance().getDOMImplementation("XML 3.0");
			if (implementation != null) {
				domImplLS = (DOMImplementationLS) implementation.getFeature("LS", "3.0");
			}
		} catch (Exception iae) {
			throw new MaryConfigurationException("Cannot access dom impl registry", iae);
		}
		if (domImplLS != null) {
			serializer = domImplLS.createLSSerializer();
			DOMConfiguration config = serializer.getDomConfig();
			if (config.canSetParameter("format-pretty-print", Boolean.TRUE)) {
				config.setParameter("format-pretty-print", Boolean.TRUE);
			}
			if (config.canSetParameter("canonical-form", Boolean.TRUE)) {
				config.setParameter("canonical-form", Boolean.TRUE);
			}
		}
		if (domImplLS != null) { // have new DOM 3 code available
			LSOutput output = domImplLS.createLSOutput();
			output.setEncoding("UTF-8");
			output.setByteStream(target);
			serializer.write(document, output);
		} else { // revert to older serialisation code
			MaryNormalisedWriter mnw = new MaryNormalisedWriter();
			try {
				mnw.output(document, target);
			} catch (TransformerException te) {
				throw new MaryConfigurationException("Problem writing document with legacy writer", te);
			}
		}
	}

	public static void replaceElement(Element oldElement, NodeList newNodes) {
		Document doc = oldElement.getOwnerDocument();
		Node parent = oldElement.getParentNode();
		int len = newNodes.getLength();
		for (int i = 0; i < len; i++) {
			Node n = newNodes.item(i);
			if (!doc.equals(n.getOwnerDocument())) {
				// first we need to import the node into the document
				n = doc.importNode(n, true);
			}
			parent.insertBefore(n, oldElement);
		}
		parent.removeChild(oldElement);
	}

	public static TreeWalker createTreeWalker(Document doc, Node root, String... tagNames) {
		return ((DocumentTraversal) doc).createTreeWalker(root, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(tagNames), false);
	}

	public static TreeWalker createTreeWalker(Node root, String... tagNames) {
		return createTreeWalker(root.getNodeType() == Node.DOCUMENT_NODE ? (Document) root : root.getOwnerDocument(), root,
				tagNames);
	}

	public static NodeIterator createNodeIterator(Document doc, Node root, String... tagNames) {
		return ((DocumentTraversal) doc).createNodeIterator(root, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(tagNames), false);
	}

	public static NodeIterator createNodeIterator(Node root, String... tagNames) {
		return createNodeIterator(root.getNodeType() == Node.DOCUMENT_NODE ? (Document) root : root.getOwnerDocument(), root,
				tagNames);
	}

	/**
	 * Remove any empty text nodes below node.
	 * 
	 * 
	 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file
	 * distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to you
	 * under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
	 * may obtain a copy of the License at
	 * 
	 * http://www.apache.org/licenses/LICENSE-2.0
	 * 
	 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
	 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
	 * language governing permissions and limitations under the License.
	 * 
	 * @param node
	 *            the node below which to trim.
	 */
	public static void trimEmptyTextNodes(Node node) {
		Element element = null;
		if (node instanceof Document) {
			element = ((Document) node).getDocumentElement();
		} else if (node instanceof Element) {
			element = (Element) node;
		} else {
			return;
		}

		List<Node> nodesToRemove = new ArrayList<Node>();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				trimEmptyTextNodes(child);
			} else if (child instanceof Text) {
				Text t = (Text) child;
				if (t.getData().trim().length() == 0) {
					nodesToRemove.add(child);
				}
			}
		}

		for (Node n : nodesToRemove) {
			element.removeChild(n);
		}
	}

	/**
	 * Compare two DOM trees. if they are not equal, throw an exception providing a description of the the reason why they differ.
	 * 
	 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file
	 * distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to you
	 * under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
	 * may obtain a copy of the License at
	 * 
	 * http://www.apache.org/licenses/LICENSE-2.0
	 * 
	 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
	 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
	 * language governing permissions and limitations under the License.
	 * 
	 * @param expected
	 *            expected
	 * @param actual
	 *            actual
	 * @param trimEmptyTextNodes
	 *            whether or not to remove any empty text nodes
	 * @throws Exception
	 *             Exception
	 */
	public static void compareNodes(Node expected, Node actual, boolean trimEmptyTextNodes) throws Exception {
		if (trimEmptyTextNodes) {
			trimEmptyTextNodes(expected);
			trimEmptyTextNodes(actual);
		}
		if (expected.getNodeType() != actual.getNodeType()) {
			throw new Exception("Different types of nodes: " + expected + " " + actual);
		}
		if (expected instanceof Document) {
			Document expectedDoc = (Document) expected;
			Document actualDoc = (Document) actual;
			compareNodes(expectedDoc.getDocumentElement(), actualDoc.getDocumentElement(), false);
		} else if (expected instanceof Element) {
			Element expectedElement = (Element) expected;
			Element actualElement = (Element) actual;

			// compare element names
			if (!expectedElement.getLocalName().equals(actualElement.getLocalName())) {
				throw new Exception("Element names do not match: " + expectedElement.getLocalName() + " "
						+ actualElement.getLocalName());
			}
			// compare element ns
			String expectedNS = expectedElement.getNamespaceURI();
			String actualNS = actualElement.getNamespaceURI();
			if ((expectedNS == null && actualNS != null) || (expectedNS != null && !expectedNS.equals(actualNS))) {
				throw new Exception("Element namespaces names do not match: " + expectedNS + " " + actualNS);
			}

			String elementName = "{" + expectedElement.getNamespaceURI() + "}" + actualElement.getLocalName();

			// compare attributes
			NamedNodeMap expectedAttrs = expectedElement.getAttributes();
			NamedNodeMap actualAttrs = actualElement.getAttributes();
			if (countNonNamespaceAttributes(expectedAttrs) != countNonNamespaceAttributes(actualAttrs)) {
				throw new Exception(elementName + ": Number of attributes do not match up: "
						+ countNonNamespaceAttributes(expectedAttrs) + " " + countNonNamespaceAttributes(actualAttrs));
			}
			for (int i = 0; i < expectedAttrs.getLength(); i++) {
				Attr expectedAttr = (Attr) expectedAttrs.item(i);
				if (expectedAttr.getName().startsWith("xmlns")) {
					continue;
				}
				Attr actualAttr = null;
				/* We don't do namespace-aware attribute handline in MARY for now: */
				/*
				 * if (expectedAttr.getNamespaceURI() == null) { actualAttr = (Attr)
				 * actualAttrs.getNamedItem(expectedAttr.getName()); } else { actualAttr = (Attr)
				 * actualAttrs.getNamedItemNS(expectedAttr.getNamespaceURI(), expectedAttr.getLocalName()); }
				 */
				actualAttr = (Attr) actualAttrs.getNamedItem(expectedAttr.getName());
				if (actualAttr == null) {
					throw new Exception(elementName + ": No attribute found:" + expectedAttr);
				}
				if (!expectedAttr.getValue().equals(actualAttr.getValue())) {
					throw new Exception(elementName + ": Attribute values do not match: " + expectedAttr.getValue() + " "
							+ actualAttr.getValue());
				}
			}

			// compare children
			NodeList expectedChildren = expectedElement.getChildNodes();
			NodeList actualChildren = actualElement.getChildNodes();
			if (expectedChildren.getLength() != actualChildren.getLength()) {
				throw new Exception(elementName + ": Number of children do not match up: " + expectedChildren.getLength() + " "
						+ actualChildren.getLength());
			}
			for (int i = 0; i < expectedChildren.getLength(); i++) {
				Node expectedChild = expectedChildren.item(i);
				Node actualChild = actualChildren.item(i);
				compareNodes(expectedChild, actualChild, false);
			}
		} else if (expected instanceof Text) {
			String expectedData = ((Text) expected).getData().trim();
			String actualData = ((Text) actual).getData().trim();

			if (!expectedData.equals(actualData)) {
				throw new Exception("Text does not match: " + expectedData + " " + actualData);
			}
		}
	}

	private static int countNonNamespaceAttributes(NamedNodeMap attrs) {
		int n = 0;
		for (int i = 0; i < attrs.getLength(); i++) {
			Attr attr = (Attr) attrs.item(i);
			if (!attr.getName().startsWith("xmlns")) {
				n++;
			}
		}
		return n;
	}

}
