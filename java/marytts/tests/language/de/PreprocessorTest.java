/**
 *Copyright (C) 2003 DFKI GmbH. All rights reserved.
 */
package marytts.tests.language.de;

import marytts.modules.ModuleRegistry;
import marytts.tests.modules.MaryModuleTestCase;
import marytts.language.de.Preprocess;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class PreprocessorTest extends MaryModuleTestCase {

    public PreprocessorTest() throws Exception
    {
        super(true); // need mary startup
        module = ModuleRegistry.getModule(Preprocess.class);
    }
    
    protected String inputEnding() {
        return "tokenised";
    }
    protected String outputEnding() {
        return "preprocessed"; 
    }

    public void testSpellout() throws Exception {
        processAndCompare("SPD");
    }

	public void testAbbrev9() throws Exception {
		processAndCompare("abbrev9");
	}

	public void testAbbrev10() throws Exception {
		processAndCompare("abbrev10");
	}

	public void testAbbrev11() throws Exception {
		processAndCompare("abbrev11");
	}

	public void testAbbrev12() throws Exception {
		processAndCompare("abbrev12");
	}

	public void testNet1() throws Exception {
		processAndCompare("net1");
	}

	public void testSpecialChar1() throws Exception {
		processAndCompare("specialchar1");
	}

    public void testUnicode1() throws Exception {
        processAndCompare("unicode1");
    }


}
