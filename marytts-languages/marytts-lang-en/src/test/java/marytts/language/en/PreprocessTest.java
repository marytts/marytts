/**
 *Copyright (C) 2003 DFKI GmbH. All rights reserved.
 */
package marytts.language.en;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.datatypes.MaryDataType;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.language.en.Preprocess;
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

	@DataProvider(name = "NumExpandData")
	private Object[][] numberExpansionDocDataCardinal() {
		// @formatter:off
		return new Object[][] { { "1", "one" },
								{ "2", "two" },
								{ "3", "three" },
								{ "4", "four" },
								{ "100", "one hundred" },
								{ "42", "forty-two"} };
		// @formatter:on
	}

	@DataProvider(name = "RealNumExpandData")
	private Object[][] numberExpansionDocDataRealNumbers() {
		// @formatter:off
		return new Object[][] { { "1.8", "one point eight" },
								{ "-2", "minus two" },
								{ "03.45", "three point four five" },
								{ "42.56%", "forty-two point five six per cent" } };
		// @formatter:on
	}

	@DataProvider(name = "OrdinalExpandData")
	private Object[][] numberExpansionDocDataOrdinal() {
		// @formatter:off
		return new Object[][] { { "2", "second" },
								{ "3", "third" },
								{ "4", "fourth" } };
		// @formatter:on
	}

	@DataProvider(name = "YearExpandData")
	private Object[][] numberExpansionDocDataYear() {
		// @formatter:off
		return new Object[][] { { "1918", "nineteen eighteen" },
								{ "1908", "nineteen oh-eight" },
								{ "2000", "two thousand" },
								{ "2015", "twenty fifteen" } };
		// @formatter:on
	}

	@DataProvider(name = "wordNumExpandData")
	private Object[][] expansionDocDataNumWord() {
		// @formatter:off
		return new Object[][] { { "123abc", "one two three  abc" },
								{ "1hello5", "one  hello five " } };
		// @formatter:on
	}

	@DataProvider(name = "timeExpandData")
	private Object[][] expansionDocDataTime() {
		// @formatter:off
		return new Object[][] { { "09:00", "nine a m" },
								{ "12:15", "twelve fifteen p m" },
								{ "00:05am", "twelve oh five a m" },
								{ "23:30", "eleven thirty p m" } };
		// @formatter:on
	}

	@DataProvider(name = "dateExpandData")
	private Object[][] expansionDocDataDate() {
		// @formatter:off
		return new Object[][] { { "06/29/1993", "June twenty-ninth nineteen ninety-three" },
								{ "06/22/1992", "June twenty-second nineteen ninety-two" } };
		// @formatter:on
	}

	@DataProvider(name = "abbrevExpandData")
	private Object[][] expansionDocDataAbbrev() {
		// @formatter:off
		return new Object[][] { { "dr.", "drive" },
								{ "mrs", "missus" },
								{ "Mr.", "mister" } };
		// @formatter:on
	}

	@Test
	public void testOneWord() throws SynthesisException, ParserConfigurationException, SAXException, IOException, ParseException,
			MaryConfigurationException {
		String lemma = "7";
		mary.setOutputType(MaryDataType.WORDS.name());
		Document doc = mary.generateXML(lemma);
		String words = "<maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.5\"><p><s><t>"
				+ lemma + "</t></s></p></maryxml>";
		Document expectedDoc = DomUtils.parseDocument(words);
		module.expand(expectedDoc);
		Diff diff = XMLUnit.compareXML(expectedDoc, doc);
		// issue where LocalMaryInterface#generateXML and DomUtils#parseDocument dont build the document in same order
		Assert.assertFalse(diff.identical());
	}

	@Test(dataProvider = "NumExpandData")
	public void testExpandNum(String token, String word) {
		double x = Double.parseDouble(token);
		String actual = module.expandNumber(x);
		Assert.assertEquals(actual, word);
	}

	@Test(dataProvider = "RealNumExpandData")
	public void testExpandRealNum(String token, String word) {
		String actual = module.expandRealNumber(token);
		Assert.assertEquals(actual, word);
	}

	@Test(dataProvider = "OrdinalExpandData")
	public void testExpandOrdinal(String token, String word) {
		double x = Double.parseDouble(token);
		String actual = module.expandOrdinal(x);
		Assert.assertEquals(actual, word);
	}

	@Test(dataProvider = "YearExpandData")
	public void testExpandYear(String token, String word) {
		double x = Double.parseDouble(token);
		String actual = module.expandYear(x);
		Assert.assertEquals(actual, word);
	}

	@Test(dataProvider = "wordNumExpandData")
	public void testExpandNumWord(String token, String word) {
		String actual = module.expandWordNumber(token);
		Assert.assertEquals(actual, word);
	}

	@Test(dataProvider = "timeExpandData")
	public void testExpandTime(String token, String word) {
		String actual = module.expandTime(token, false);
		Assert.assertEquals(actual, word);
	}

	@Test(dataProvider = "dateExpandData")
	public void testExpandDate(String token, String word) throws ParseException {
		String actual = module.expandDate(token);
		Assert.assertEquals(actual, word);
	}

	@Test(dataProvider = "abbrevExpandData")
	public void testExpandAbbrev(String token, String word) throws ParseException {
		String actual = module.expandAbbreviation(token, false);
		Assert.assertEquals(actual, word);
	}

	@Test
	public void testExpandUnknownAbbrev() throws ParseException {
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
	public void testExpandURL() {
		String test = "hello@gmail.com";
		String expected = "hello @ gmail . com";
		test = module.expandURL(test);
		Assert.assertEquals(test, expected);
	}

	@Test
	public void testExpandYearBCAD() {
		String test = "1920A.D";
		String expected = "nineteen twenty A D";
		test = module.expandYearBCAD(test);
		Assert.assertEquals(test, expected);
	}

	@Test
	public void testExpandRange() {
		String test = "18-25";
		String expected = "eighteen to twenty-five";
		test = module.expandRange(test);
		Assert.assertEquals(test, expected);
	}

	@Test
	public void testExpandHashtag() {
		String test = "#weDidIt";
		String expected = "hashtag we Did It";
		test = module.expandHashtag(test);
		Assert.assertEquals(test, expected);
	}

	@Test
	public void testExpandNumberS() {
		String test = "6s";
		String expected = "sixes";
		test = module.expandNumberS(test);
		Assert.assertEquals(test, expected);
	}

	@Test
	public void testExpandConsonants() {
		String test = "bbc";
		String expected = "b b c";
		test = module.expandConsonants(test);
		Assert.assertEquals(test, expected);
	}
}
