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
package de.dfki.lt.mary.util;

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

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

/**
 * A wrapper class for output of XML DOM trees in a Mary normalised way:
 *  One tag or text node per line, no indentation.
 *  This is only needed during the transition phase to "real" XML modules.
 * @author Marc Schr&ouml;der
 */

public class MaryNormalisedWriter {
    private static TransformerFactory tFactory = null;
    private static Templates stylesheet = null;

    private static Logger logger; // only used for extensive debug output

    private Transformer transformer;

    /** Default constructor.
     *  Calls <code>startup()</code> if it has not been called before.
     *  @see #startup().
     */
    public MaryNormalisedWriter()
    throws TransformerFactoryConfigurationError, TransformerConfigurationException
    {
        // startup every time:
        //if (tFactory == null) { // need to startup()
        startup();
        //}
        transformer = stylesheet.newTransformer();
    }

    // Methods

    /** Start up the static parts, and compile the normalise-maryxml XSLT
     *  stylesheet which can then be used by multiple threads.
     *  @exception TransformerFactoryConfigurationError
     *      if the TransformerFactory cannot be instanciated.
     *  @exception FileNotFoundException
     *      if the stylesheet file cannot be found.
     *  @exception TransformerConfigurationException
     *      if the templates stylesheet cannot be generated.
     */
    public static void startup()
    throws TransformerFactoryConfigurationError, TransformerConfigurationException
    {
        // only start the stuff if it hasn't been started yet.
        if (tFactory == null) {
            tFactory = TransformerFactory.newInstance();
        }
        if (stylesheet == null) {
            StreamSource stylesheetStream =
                new StreamSource(
                    de.dfki.lt.mary.util.MaryNormalisedWriter.class.getResourceAsStream(
                        "normalise-maryxml.xsl"));
            stylesheet = tFactory.newTemplates(stylesheetStream);
        }
        if (logger == null)
            logger = Logger.getLogger("MaryNormalisedWriter");

    }

    /** The actual output to stdout.
     *  @param input a DOMSource, a SAXSource or a StreamSource.
     *  @see javax.xml.transform.Transformer
     *  @exception TransformerException
     *       if the transformation cannot be performed.
     */
    public void output(Source input, Result destination) throws TransformerException {
        //logger.debug("Before transform");
        transformer.transform(input, destination);
        //logger.debug("After transform");
    }

    /**
     * Output any Source to stdout.
     */
    public void output(Source input) throws TransformerException {
        output(input, new StreamResult(new PrintStream(System.out, true)));
    }

    /** Output a DOM node to stdout.
     *  @see #output(Source)
     */
    public void output(Node input) throws TransformerException {
        output(new DOMSource(input));
    }

    /**
     * Output a DOM node to a specified destination
     */
    public void output(Node input, OutputStream destination) throws TransformerException {
        output(new DOMSource(input), new StreamResult(destination));
    }

    /**
     * The simplest possible command line interface to the
     * MaryNormalisedWriter. Reads a "real" XML document from stdin,
     * and outputs it in the MaryNormalised form to stdout.
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
