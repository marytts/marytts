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
package de.dfki.lt.mary.tests.modules.en;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.modules.en.ContourGenerator;
import de.dfki.lt.mary.tests.modules.MaryModuleTestCase;
import de.dfki.lt.mary.util.dom.DomUtils;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class ContourGeneratorTest extends MaryModuleTestCase {


    public ContourGeneratorTest() throws Exception
    {
        super(true); // do need Mary startup
        module = new ContourGenerator();
        module.startup();
    }
    
    public void testDownstepMtu() throws Exception {
        MaryData inData = new MaryData(module.inputType());
        inData.readFrom(this.getClass().getResourceAsStream("downstep_in_mtu.postprocessed"), null);
        MaryData outData = module.process(inData);
        try {
            DomUtils.verifySchemaValid(outData.getDocument()); // throws Exception upon failure
        } catch (Exception e) {
            e.printStackTrace();
            fail();        
        }
    }
 
 
}
