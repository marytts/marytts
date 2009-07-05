/**
 *Copyright (C) 2003 DFKI GmbH. All rights reserved.
 */
package marytts.tests.language.de;

import marytts.modules.ModuleRegistry;
import marytts.tests.modules.MaryModuleTestCase;
import marytts.language.de.JTokeniser;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class JTokeniserTest extends MaryModuleTestCase {

    public JTokeniserTest() throws Exception
    {
        super(true); // need mary startup
        module = ModuleRegistry.getModule(JTokeniser.class);
    }

    protected String inputEnding() {
        return "txt";
    }

    protected String outputEnding() {
        return "tokenised";
    }

    public void testSimpleSentence() throws Exception {
        processAndCompare("simplesentence");
    }

    public void testTwoSimpleSentences() throws Exception {
        processAndCompare("twosimplesentences");
    }

    public void testOrdinal1() throws Exception {
        processAndCompare("ordinal1");
    }

    public void testOrdinal2() throws Exception {
        processAndCompare("ordinal2");
    }

    /*
    public void testOrdinal3() throws Exception {
        processAndCompare("ordinal3");
    }
    */

    public void testAbbrev1() throws Exception {
        processAndCompare("abbrev1");
    }

    /*
    public void testAbbrev2() throws Exception {
        processAndCompare("abbrev2");
    }
    */

    public void testAbbrev3() throws Exception {
        processAndCompare("abbrev3");
    }

    public void testAbbrev4() throws Exception {
        processAndCompare("abbrev4");
    }

    public void testAbbrev5() throws Exception {
        processAndCompare("abbrev5");
    }

    public void testAbbrev6() throws Exception {
        processAndCompare("abbrev6");
    }

    public void testAbbrev7() throws Exception {
        processAndCompare("abbrev7");
    }
    
    public void testAbbrev8() throws Exception {
    	processAndCompare("abbrev8");
    }

	public void testAbbrev9() throws Exception {
		processAndCompare("abbrev9");
	}

	public void testNet1() throws Exception {
		processAndCompare("net1");
	}

    public void testWeb1() throws Exception {
        processAndCompare("web1");
    }

    public void testWeb2() throws Exception {
        processAndCompare("web2");
    }

    public void testOmission1() throws Exception {
        processAndCompare("omission1");
    }

    public void testOmission2() throws Exception {
        processAndCompare("omission2");
    }

    public void testOmission3() throws Exception {
        processAndCompare("omission3");
    }

    public void testDigit1() throws Exception {
        processAndCompare("digit1");
    }

    public void testDigit2() throws Exception {
        processAndCompare("digit2");
    }

    public void testDigit3() throws Exception {
        processAndCompare("digit3");
    }



}
