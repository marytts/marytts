/**
 *Copyright (C) 2003 DFKI GmbH. All rights reserved.
 */
package marytts.language.fr;

import marytts.language.fr.Preprocess;
import marytts.util.dom.DomUtils;
import java.util.Locale;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;

import org.custommonkey.xmlunit.*;
import org.testng.Assert;
import org.testng.annotations.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author Tristan Hamilton
 *
 *
 */
public class PreprocessTest {

	private static Preprocess module;

	@BeforeSuite
	public static void setUpBeforeClass() {
		module = new Preprocess();
	}

	@DataProvider(name = "DocData")
	private Object[][] numberExpansionDocData() {
		// @formatter:off
		return new Object[][] { { "1", "un" },
								{ "2", "deux" },
								{ "3", "trois" },
								{ "4", "quatre" },
								{ "42", "quarante-deux"},
								{ "1er", "premier"},
								{ "1re", "première"},
								{ "2e", "deuxième" },
								{ "3e", "troisième" },
								{ "4e", "quatrième" } };
		// @formatter:on
	}

	@DataProvider(name = "NumExpandData")
	private Object[][] numberExpansionDocDataCardinal() {
		// @formatter:off
		return new Object[][] { { "1", "un" },
								{ "2", "deux" },
								{ "3", "trois" },
								{ "4", "quatre" },
								{ "42", "quarante-deux"} };
		// @formatter:on
	}

	@DataProvider(name = "OrdinalExpandData")
	private Object[][] numberExpansionDocDataOrdinal() {
		// @formatter:off
		return new Object[][] { { "2", "deuxième" },
								{ "3", "troisième" },
								{ "4", "quatrième" } };
		// @formatter:on
	}

	@Test(dataProvider = "DocData")
	public void testSpellout(String tokenised, String expected)
        throws Exception, ParserConfigurationException, SAXException, IOException
    {
		Document tokenisedDoc;
		String tokens = "<maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.5\" xml:lang=\"fr\"><p> "+ tokenised +"<s>"+ tokenised +"<t>"
            + tokenised + "</t></s></p></maryxml>";
		tokenisedDoc = DomUtils.parseDocument(tokens);
        MaryData input_data = new MaryData(MaryDataType.TOKENS, Locale.FRENCH);
        input_data.setDocument(tokenisedDoc);

        Document expectedDoc;
		String words = "<maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.5\" xml:lang=\"fr\"><p>"+ tokenised +"<s>"+ tokenised +"<t sounds_like=\"" + expected + "\">" + tokenised + "</t></s></p></maryxml>";
		expectedDoc = DomUtils.parseDocument(words);


        System.out.println("======== expected result =========");
        System.out.println(DomUtils.serializeToString(expectedDoc));

        MaryData output_data = module.process(input_data);

        System.out.println("======== achieved result =========");
        System.out.println(DomUtils.serializeToString(output_data.getDocument()));
        Diff diff = XMLUnit.compareXML(expectedDoc, output_data.getDocument());


        System.out.println("======== Diff =========");
        // System.out.println(diff.toString());
        Assert.assertEquals(DomUtils.serializeToString(expectedDoc), DomUtils.serializeToString(output_data.getDocument()));
    }

    @Test(dataProvider = "NumExpandData")
    public void testExpandNum(String token, String word) {
        double x = Double.parseDouble(token);
        String actual = module.expandNumber(x);
        Assert.assertEquals(actual, word);
    }

    @Test(dataProvider = "OrdinalExpandData")
    public void testExpandOrdinal(String token, String word) {
        double x = Double.parseDouble(token);
        String actual = module.expandOrdinal(x);
        Assert.assertEquals(actual, word);
    }
}
