/**
 *Copyright (C) 2003 DFKI GmbH. All rights reserved.
 */
package marytts.tests.junit4.language.de;

import java.util.Locale;

import org.junit.Test;

import marytts.modules.ModuleRegistry;
import marytts.tests.modules.MaryModuleTestCase;
import marytts.language.de.Preprocess;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class PreprocessorIT extends MaryModuleTestCase {

    public PreprocessorIT() throws Exception
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

    @Test
    public void testSpellout() throws Exception {
        processAndCompare("SPD", Locale.GERMAN);
    }

    @Test
	public void testAbbrev9() throws Exception {
		processAndCompare("abbrev9", Locale.GERMAN);
	}

    @Test
	public void testAbbrev10() throws Exception {
		processAndCompare("abbrev10", Locale.GERMAN);
	}

    @Test
	public void testAbbrev11() throws Exception {
		processAndCompare("abbrev11", Locale.GERMAN);
	}

    @Test
	public void testAbbrev12() throws Exception {
		processAndCompare("abbrev12", Locale.GERMAN);
	}

    @Test
	public void testNet1() throws Exception {
		processAndCompare("net1", Locale.GERMAN);
	}

    @Test
	public void testSpecialChar1() throws Exception {
		processAndCompare("specialchar1", Locale.GERMAN);
	}

    @Test
    public void testUnicode1() throws Exception {
        processAndCompare("unicode1", Locale.GERMAN);
    }


}
