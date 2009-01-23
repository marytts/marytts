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
package marytts.tests.modules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.modules.MaryModule;
import marytts.server.Mary;
import marytts.util.dom.DomUtils;

import org.apache.log4j.BasicConfigurator;
import org.w3c.dom.Document;


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
        MaryData md = new MaryData(MaryDataType.get("RAWMARYXML_EN"), Locale.ENGLISH);
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
            input = new MaryData(module.inputType(), input.getLocale());
            input.readFrom(this.getClass().getResourceAsStream(basename + "." + inputEnding()), null);
        }
        MaryData targetOut = new MaryData(module.outputType(), input.getLocale());
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

