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

// TraX classes
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import marytts.exceptions.MaryConfigurationException;
import marytts.util.MaryUtils;
import marytts.util.io.ReaderSplitter;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

/**
 * A wrapper class for output of XML DOM trees in a Mary normalised way: One tag or text node per line, no indentation. This is
 * only needed during the transition phase to "real" XML modules.
 * 
 * @author Marc Schr&ouml;der
 */

public class MaryNormalisedWriter {
	private static TransformerFactory tFactory = null;
	private static Templates stylesheet = null;

	private static Logger logger; // only used for extensive debug output

	private Transformer transformer;

	/**
	 * Default constructor. Calls <code>startup()</code> if it has not been called before.
	 * 
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 * @see #startup()
	 */
	public MaryNormalisedWriter() throws MaryConfigurationException {
		try {
			// startup every time:
			startup();
			transformer = stylesheet.newTransformer();
		} catch (Exception e) {
			throw new MaryConfigurationException("Cannot initialise XML writing code", e);
		}
	}

	// Methods

	/**
	 * Start up the static parts, and compile the normalise-maryxml XSLT stylesheet which can then be used by multiple threads.
	 * 
	 * @exception TransformerFactoryConfigurationError
	 *                if the TransformerFactory cannot be instanciated.
	 * @exception FileNotFoundException
	 *                if the stylesheet file cannot be found.
	 * @exception TransformerConfigurationException
	 *                if the templates stylesheet cannot be generated.
	 */
	private static void startup() throws TransformerFactoryConfigurationError, TransformerConfigurationException {
		// only start the stuff if it hasn't been started yet.
		if (tFactory == null) {
			tFactory = TransformerFactory.newInstance();
		}
		if (stylesheet == null) {
			StreamSource stylesheetStream = new StreamSource(
					MaryNormalisedWriter.class.getResourceAsStream("normalise-maryxml.xsl"));
			stylesheet = tFactory.newTemplates(stylesheetStream);
		}
		if (logger == null)
			logger = MaryUtils.getLogger("MaryNormalisedWriter");

	}

	/**
	 * The actual output to stdout.
	 * 
	 * @param input
	 *            a DOMSource, a SAXSource or a StreamSource.
	 * @param destination
	 *            destination
	 * @see javax.xml.transform.Transformer
	 * @exception TransformerException
	 *                if the transformation cannot be performed.
	 */
	public void output(Source input, Result destination) throws TransformerException {
		// logger.debug("Before transform");
		transformer.transform(input, destination);
		// logger.debug("After transform");
	}

	/**
	 * Output any Source to stdout.
	 * 
	 * @param input
	 *            input
	 * @throws TransformerException
	 *             TransformerException
	 * 
	 */
	public void output(Source input) throws TransformerException {
		output(input, new StreamResult(new PrintStream(System.out, true)));
	}

	/**
	 * Output a DOM node to stdout.
	 * 
	 * @param input
	 *            input
	 * @throws TransformerException
	 *             TransformerException
	 * @see #output(Source)
	 */
	public void output(Node input) throws TransformerException {
		output(new DOMSource(input));
	}

	/**
	 * Output a DOM node to a specified destination
	 * 
	 * @param input
	 *            input
	 * @param destination
	 *            destination
	 * @throws TransformerException
	 *             TransformerException
	 */
	public void output(Node input, OutputStream destination) throws TransformerException {
		output(new DOMSource(input), new StreamResult(destination));
	}

	/**
	 * The simplest possible command line interface to the MaryNormalisedWriter. Reads a "real" XML document from stdin, and
	 * outputs it in the MaryNormalised form to stdout.
	 * 
	 * @param args
	 *            args
	 * @throws Throwable
	 *             Throwable
	 */
	public static void main(String[] args) throws Throwable {
		startup();
		MaryNormalisedWriter writer = new MaryNormalisedWriter();

		ReaderSplitter splitter = new ReaderSplitter(new InputStreamReader(System.in), "</maryxml>");

		Reader oneXMLStructure = null;
		while ((oneXMLStructure = splitter.nextReader()) != null) {
			writer.output(new StreamSource(oneXMLStructure));
		}
	}
}
