/**
 *Copyright (C) 2003 DFKI GmbH. All rights reserved.
 */
package marytts.language.de;

import java.util.Locale;

import marytts.language.de.JTokeniser;
import marytts.modules.ModuleRegistry;
import marytts.tests.modules.MaryModuleTestCase;

import org.junit.Test;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class JTokeniserIT extends MaryModuleTestCase {

	public JTokeniserIT() throws Exception {
		super(true); // need mary startup
		module = ModuleRegistry.getModule(JTokeniser.class);
	}

	protected String inputEnding() {
		return "txt";
	}

	protected String outputEnding() {
		return "tokenised";
	}

	@Test
	public void testSimpleSentence() throws Exception {
		processAndCompare("simplesentence", Locale.GERMAN);
	}

	@Test
	public void testTwoSimpleSentences() throws Exception {
		processAndCompare("twosimplesentences", Locale.GERMAN);
	}

	@Test
	public void testOrdinal1() throws Exception {
		processAndCompare("ordinal1", Locale.GERMAN);
	}

	@Test
	public void testOrdinal2() throws Exception {
		processAndCompare("ordinal2", Locale.GERMAN);
	}

	/*
	 * public void testOrdinal3() throws Exception { processAndCompare("ordinal3"); }
	 */

	public void testAbbrev1() throws Exception {
		processAndCompare("abbrev1", Locale.GERMAN);
	}

	/*
	 * public void testAbbrev2() throws Exception { processAndCompare("abbrev2"); }
	 */

	public void testAbbrev3() throws Exception {
		processAndCompare("abbrev3", Locale.GERMAN);
	}

	public void testAbbrev4() throws Exception {
		processAndCompare("abbrev4", Locale.GERMAN);
	}

	public void testAbbrev5() throws Exception {
		processAndCompare("abbrev5", Locale.GERMAN);
	}

	public void testAbbrev6() throws Exception {
		processAndCompare("abbrev6", Locale.GERMAN);
	}

	public void testAbbrev7() throws Exception {
		processAndCompare("abbrev7", Locale.GERMAN);
	}

	public void testAbbrev8() throws Exception {
		processAndCompare("abbrev8", Locale.GERMAN);
	}

	public void testAbbrev9() throws Exception {
		processAndCompare("abbrev9", Locale.GERMAN);
	}

	public void testNet1() throws Exception {
		processAndCompare("net1", Locale.GERMAN);
	}

	public void testWeb1() throws Exception {
		processAndCompare("web1", Locale.GERMAN);
	}

	public void testWeb2() throws Exception {
		processAndCompare("web2", Locale.GERMAN);
	}

	public void testOmission1() throws Exception {
		processAndCompare("omission1", Locale.GERMAN);
	}

	public void testOmission2() throws Exception {
		processAndCompare("omission2", Locale.GERMAN);
	}

	public void testOmission3() throws Exception {
		processAndCompare("omission3", Locale.GERMAN);
	}

	public void testDigit1() throws Exception {
		processAndCompare("digit1", Locale.GERMAN);
	}

	public void testDigit2() throws Exception {
		processAndCompare("digit2", Locale.GERMAN);
	}

	public void testDigit3() throws Exception {
		processAndCompare("digit3", Locale.GERMAN);
	}

	@Test
	public void testDots1() throws Exception {
		processAndCompare("dots1", Locale.GERMAN);
	}

	@Test
	public void testDots2() throws Exception {
		processAndCompare("dots2", Locale.GERMAN);
	}

	@Test
	public void testDots3() throws Exception {
		processAndCompare("dots3", Locale.GERMAN);
	}

	@Test
	public void testExclam() throws Exception {
		processAndCompare("exclam", Locale.GERMAN);
	}

	@Test
	public void testQuest() throws Exception {
		processAndCompare("quest", Locale.GERMAN);
	}

}
