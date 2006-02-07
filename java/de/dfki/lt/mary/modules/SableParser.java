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
package de.dfki.lt.mary.modules;

// TraX classes
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.util.LoggingErrorHandler;

/**
 * Transforms a SABLE document into a raw (untokenised) MaryXML document
 *
 * @author Marc Schr&ouml;der
 */

public class SableParser extends InternalModule
{
    // One stylesheet can be used (read) by multiple threads:
    private static Templates stylesheet = null;

    private Transformer transformer = null;
    private DocumentBuilderFactory dbFactory = null;
    private DocumentBuilder docBuilder = null;
    private boolean doWarnClient = false;

    public SableParser()
    {
        super("SableParser",
              MaryDataType.get("SABLE"),
              MaryDataType.get("RAWMARYXML")
              );
    }

    public boolean getWarnClient() { return doWarnClient; }
    public void setWarnClient(boolean doWarnClient)
    {
        this.doWarnClient = doWarnClient;
    }

    public void startup() throws Exception
    {
        setWarnClient(true); // !! where should that be decided?
        if (stylesheet == null) {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            StreamSource stylesheetStream = new StreamSource
                (this.getClass().getResourceAsStream("sable-to-mary.xsl"));
            stylesheet = tFactory.newTemplates( stylesheetStream );
        }
        if (dbFactory == null) {
            dbFactory = DocumentBuilderFactory.newInstance();
        }
        if (docBuilder == null) {
            docBuilder = dbFactory.newDocumentBuilder();
        }
        if (transformer == null) {
            transformer = stylesheet.newTransformer();
        }
        super.startup();
    }

    public MaryData process(MaryData d)
    throws Exception
    {
        DOMSource domSource = new DOMSource(d.getDocument());

        // Log transformation errors to client:
        if (doWarnClient) {
            // Use custom error handler:
            transformer.setErrorListener(new LoggingErrorHandler(Thread.currentThread().getName() + " client.Sable transformer"));
        }

        // Transform DOMSource into a DOMResult
        Document maryxmlDocument = docBuilder.newDocument();
        DOMResult domResult = new DOMResult(maryxmlDocument);
        transformer.transform(domSource, domResult);
        MaryData result = new MaryData(outputType());
        result.setDocument(maryxmlDocument);
        return result;
    }
}
