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
package marytts.language.en.tests;

import marytts.datatypes.MaryData;
import marytts.modules.TobiContourGenerator;
import marytts.tests.modules.MaryModuleTestCase;
import marytts.util.dom.MaryDomUtils;

import static org.junit.Assert.*;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class ContourGeneratorTest extends MaryModuleTestCase {


    public ContourGeneratorTest() throws Exception
    {
        super(true); // do need Mary startup
        module = new TobiContourGenerator("en_US");
        module.startup();
    }
    
    public void testDownstepMtu() throws Exception {
        MaryData inData = new MaryData(module.inputType(), module.getLocale());
        inData.readFrom(this.getClass().getResourceAsStream("downstep_in_mtu.postprocessed"), null);
        MaryData outData = module.process(inData);
        try {
            MaryDomUtils.verifySchemaValid(outData.getDocument()); // throws Exception upon failure
        } catch (Exception e) {
            e.printStackTrace();
            fail();        
        }
    }
 
 
}

