/*
 *Copyright (C) 2003 DFKI GmbH. All rights reserved.
 */
package marytts.language.en;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.datatypes.MaryDataType;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.util.dom.DomUtils;

import org.custommonkey.xmlunit.*;
import org.testng.Assert;
import org.testng.annotations.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

/**
 * @author Tristan Hamilton
 *
 *
 */
public class PreprocessTest {

	private static Preprocess module;
	private static MaryInterface mary;

	@BeforeSuite
	public static void setUpBeforeClass() throws MaryConfigurationException {
		module = new Preprocess();
		mary = new LocalMaryInterface();
	}

	@Test
	public void testOneWord() throws Exception {
		String lemma = "7";
		mary.setOutputType(MaryDataType.WORDS.name());
		Document doc = mary.generateXML(lemma);
		String words = "<maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.5\"><p><s><t>"
				+ lemma + "</t></s></p></maryxml>";
		Document expectedDoc = DomUtils.parseDocument(words);
		module.expand(expectedDoc);
		Diff diff = XMLUnit.compareXML(expectedDoc, doc);
		// issue where LocalMaryInterface#generateXML and DomUtils#parseDocument dont build the document in same order
		Assert.assertFalse(diff.identical());
	}

	@DataProvider(name = "ExpandTestData")
	private Object[][] testExpand() {
		// @formatter:off
		return new Object[][] {
			// Integers
			{ "1", "one" },
			{ "2", "two" },
			{ "3", "three" },
			{ "4", "four" },
			{ "100", "one hundred" },
			{ "1,002", "one thousand two" },
			{ "42", "forty-two"},
			{ "1,002", "one thousand two" },
			// Real numbers
			{ "1.8", "one point eight" },
			{ "-2", "minus two" },
			{ "03.45", "three point four five" },
			{ "5,234.56", "five thousand two hundred thirty-four point five six"},
			// Percentages
			{ "42.56%", "forty-two point five six per cent" },
			// Ordinals
			{ "2nd", "second" },
			{ "3rd", "third" },
			{ "4th", "fourth" },
			{ "13th", "thirteenth" },
			{ "21st", "twenty-first" },
			// Currency
			{ "$12.52", "twelve dollars fifty-two cents" },
			{ "$12.5", "twelve dollars fifty cents" },
			{ "£5.20", "five pound sterling twenty pence" },
			{ "€7.05", "seven euro five cents" },
			{ "$1,000.10", "one thousand dollars ten cents" },
			// Years
			{ "1918", "nineteen eighteen" },
			{ "1908", "nineteen oh-eight" },
			{ "2000", "two thousand" },
			{ "2015", "twenty fifteen" },
			{"1920A.D", "nineteen twenty A D" },
		// Word/number concatenations
			{ "123abc", "one two three abc" },
			{ "1hello5", "one hello five" },
			// Time
			{ "09:00", "nine a m" },
			{ "12:15", "twelve fifteen p m" },
			{ "00:05am", "twelve oh five a m" },
			{ "23:30", "eleven thirty p m" },
			// Dates
			{ "06/29/1993", "June twenty-ninth nineteen ninety-three" },
			{ "06/22/1992", "June twenty-second nineteen ninety-two" },
			{ "24/04/2020", "April twenty-fourth twenty twenty"},
			{ "04/24/2020", "April twenty-fourth twenty twenty"},
			{ "04.24.2020", "April twenty-fourth twenty twenty"},
			{ "4/24/2020", "April twenty-fourth twenty twenty"},
			// Abbreviations
			{ "dr.", "drive" },
			{ "mrs", "missus" },
			{ "Mr.", "mister" },
			//{ "qlsv234toinsdcsdl.", "qlsv234toinsdcsdl" }, // Not sure how to handle this (see testExpandUnknownAbbrev())
			// URLs / email addresses
			{ "hello@gmail.com", "hello @ gmail . com" },
			// Ranges
			{ "18-25", "eighteen to twenty-five" },
			// Hashtags
			{"#delta50Gonzo", "hashtag delta fifty Gonzo"},
			{"#weDidIt", "hashtag we Did It"},
			{"#101dalmations", "hashtag one hundred one dalmations"},
			{"#the100", "hashtag the one hundred"},
			// Numbers ending in s
			{ "6s", "sixes" },
			{ "5s", "fives" },
			{ "10s", "tens" },
			{ "20s", "twenties" },
			// Sequence of Consonants
			// { "bbc", "b b c" },	// prohibited by MaryRuntimeUtils.checkLexicon() in Preprocess.java
		};
		// @formatter:on
	}

	@Test(dataProvider = "ExpandTestData")
	public void testExpand(String token, String word) throws Exception {
		String words = "<maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.5\"><p><s><t>"
				+ token + "</t></s></p></maryxml>";
		String retVal = module.expand(DomUtils.parseDocument(words));
		if (!retVal.equals(word)) {
			int dum = 0;		// Debugger breakpoint for testing
		}
		Assert.assertEquals(retVal, word);
	}

	@Test
	public void testExpandUnknownAbbrev()  {
		String word = "qlsv234toinsdcsdl.";
		String actual = module.expandAbbreviation(word, false);
		Assert.assertEquals(actual, word);
	}

	// FIXME: As long as we can't separate phonological words and syntax words => disable this test
	// @Test
	// public void testSplitContraction() {
	// String test = "cat's";
	// String expected = "cat's";
	// test = module.splitContraction(test);
	// Assert.assertEquals(test, expected);
	// }

	@Test
	public void testExpandConsonants() {
		String test = "bbc";
		String expected = "b b c";
		test = module.expandConsonants(test);
		Assert.assertEquals(test, expected);
	}
}
