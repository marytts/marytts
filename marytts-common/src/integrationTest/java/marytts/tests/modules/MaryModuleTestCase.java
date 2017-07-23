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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import marytts.io.MaryIOException;
import marytts.io.serializer.ROOTSJSONSerializer;
import marytts.io.serializer.XMLSerializer;
import marytts.data.Utterance;
import marytts.modules.MaryModule;
import marytts.server.Mary;
import marytts.util.MaryUtils;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class MaryModuleTestCase {

    protected MaryModule module;
    protected Logger logger;

    public MaryModuleTestCase(boolean needMaryStarted) throws Exception {
        if (!MaryUtils.isLog4jConfigured()) {
            BasicConfigurator.configure();
        }
        Logger.getRootLogger().setLevel(Level.DEBUG);
        if (System.getProperty("mary.base") == null) {
            System.setProperty("mary.base", ".");
            Logger.getRootLogger().warn("System property 'mary.base' is not defined -- trying "
                                        + new File(".").getAbsolutePath()
                                        + " -- if this fails, please start this using VM property \"-Dmary.base=/path/to/mary/runtime\"!");
        }

        if (needMaryStarted) {
            if (Mary.currentState() == Mary.STATE_OFF) {
                Mary.startup();
            }
        }

        logger = MaryUtils.getLogger(getClass());
    }

    protected Utterance loadXMLResource(String resourceName) throws IOException, MaryIOException {
        // Define a reader to the resource
        BufferedReader br = new BufferedReader(
            new InputStreamReader(this.getClass().getResourceAsStream(resourceName), "UTF-8"));

        // Loading the XML content file into a string
        StringBuilder buf = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            buf.append(line);
            buf.append("\n");
        }
        String document = buf.toString();

        // Using serializer to extract the utterance the from "string" document
        XMLSerializer xml_ser = new XMLSerializer();
        Utterance utt = xml_ser.load(document);

        // Return loaded utterance
        return utt;
    }

    protected boolean processAndCompare(String in, String target_out, Locale locale) throws Exception {
        Utterance input = loadXMLResource(in);
        Utterance targetOut = loadXMLResource(target_out);
        Utterance processedOut = module.process(input);

        ROOTSJSONSerializer out_ser = new ROOTSJSONSerializer();
        logger.debug(" ======================== expected =====================");
        logger.debug(out_ser.export(targetOut));
        logger.debug(" ======================== achieved =====================");
        logger.debug(out_ser.export(processedOut));
        logger.debug(" =======================================================");

        return targetOut.equals(processedOut);
    }

}
