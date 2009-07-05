/**
 *Copyright (C) 2003 DFKI GmbH. All rights reserved.
 */
package marytts.tests.language.de;

import marytts.datatypes.MaryData;
import marytts.tests.modules.MaryModuleTestCase;
import marytts.util.dom.DomUtils;
import marytts.language.de.ContourGenerator;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class JCapTest extends MaryModuleTestCase {


    public JCapTest() throws Exception
    {
        super(true); // do need Mary startup
        module = new ContourGenerator();
        module.startup();
    }
    
    public void testDownstepMtu() throws Exception {
        MaryData inData = new MaryData(module.inputType(), module.getLocale());
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
