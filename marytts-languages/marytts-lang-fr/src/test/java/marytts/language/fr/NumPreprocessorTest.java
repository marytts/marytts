/**
 *Copyright (C) 2003 DFKI GmbH. All rights reserved.
 */
package marytts.language.fr;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.language.fr.NumPreprocess;
import marytts.util.MaryUtils;
import marytts.util.dom.DomUtils;

import org.custommonkey.xmlunit.*;
import org.testng.Assert;
import org.testng.annotations.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

/**
 * @author Tristan Hamilton
 *
 *
 */
public class NumPreprocessorTest {

	private static NumPreprocess module;

	@BeforeSuite
	public static void setUpBeforeClass() {
		module = new NumPreprocess();
	}

	@DataProvider(name = "DocData")
	private Object[][] numberExpansionDocData() {
		return new Object[][] { { "1", "un" }, 
								{ "2", "deux" }, 
								{ "3", "trois" }, 
								{ "4", "quatre" },
								{ "42", "quarante-deux"} };
	}

	@Test(dataProvider = "DocData")
	public void testSpellout(String tokenised, String expected) throws Exception, ParserConfigurationException, SAXException,
			IOException {
		Document tokenisedDoc;
		Document expectedDoc;
		String tokens = "<maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.5\" xml:lang=\"fr\"><p><s><t>"
				+ tokenised + "</t></s></p></maryxml>";
		tokenisedDoc = DomUtils.parseDocument(tokens);
		String words = "<maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.5\" xml:lang=\"fr\"><p><s><t>"
				+ expected + "</t></s></p></maryxml>";
		expectedDoc = DomUtils.parseDocument(words);
		module.checkForNumbers(tokenisedDoc);
		Diff diff = XMLUnit.compareXML(expectedDoc, tokenisedDoc);
		Assert.assertTrue(diff.identical());
	}

	@Test(dataProvider = "DocData")
	public void testExpandNum(String token, String word) {
		double x = Double.parseDouble(token);
		String actual = module.expandNumber(x);
		Assert.assertEquals(actual, word);
	}
}
