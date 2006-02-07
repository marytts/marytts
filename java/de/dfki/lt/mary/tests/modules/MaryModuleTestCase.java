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
package de.dfki.lt.mary.tests.modules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.w3c.dom.Document;

import de.dfki.lt.mary.Mary;
import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.MaryModule;
import de.dfki.lt.mary.util.dom.DomUtils;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class MaryModuleTestCase extends TestCase {

    protected MaryModule module;

    public MaryModuleTestCase(boolean needMaryStarted) throws Exception
    {
        if (needMaryStarted) {
            if(Mary.currentState() == Mary.STATE_OFF)
                Mary.startup();
        } else {
            // for log4j:
            BasicConfigurator.configure();
        }
    }

    protected MaryData createMaryDataFromText(String text) {
        Document doc = MaryXML.newDocument();
        doc.getDocumentElement().setAttribute("xml:lang", "en");
        doc.getDocumentElement().appendChild(doc.createTextNode(text));
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML_EN"));
        md.setDocument(doc);
        return md;
    }
    
    protected String loadResourceIntoString(String resourceName)
    throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(
            this.getClass().getResourceAsStream(resourceName), "UTF-8"));
        StringBuffer buf = new StringBuffer();
        String line;
        while ((line = br.readLine()) != null) {
            buf.append(line);
            buf.append("\n");
        }
        return buf.toString();
    }

    protected void processAndCompare(String basename) throws Exception {
        assert inputEnding() != null;
        assert outputEnding() != null;
        MaryData input = null;
        if (inputEnding().equals("txt")) {
            String in = loadResourceIntoString(basename + "." + inputEnding());
            input = createMaryDataFromText(in);
        } else {
            input = new MaryData(module.inputType());
            input.readFrom(this.getClass().getResourceAsStream(basename + "." + inputEnding()), null);
        }
        MaryData targetOut = new MaryData(module.outputType());
        targetOut.readFrom(this.getClass().getResourceAsStream(basename + "." + outputEnding()), null);
        MaryData processedOut = module.process(input);
        try {
            assertTrue(DomUtils.areEqual(targetOut.getDocument(), processedOut.getDocument()));
        } catch (AssertionFailedError afe) {
            System.err.println("==========target:=============");
            System.err.println(DomUtils.serializeToString(targetOut.getDocument()));
            System.err.println();
            System.err.println("==========processed:============");
            System.err.println(DomUtils.serializeToString(processedOut.getDocument()));
            throw afe;
        }
    } 
    
    /**
     * To be overridden by subclasses using processAndCompare; this string will be used as the filename
     * ending of result files by processAndCompare().
     */
    protected String inputEnding() {
        return null; 
    }

    /**
     * To be overridden by subclasses using processAndCompare; this string will be used as the filename
     * ending of result files by processAndCompare().
     */
    protected String outputEnding() {
        return null; 
    }

}
