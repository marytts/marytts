/**
 *Copyright (C) 2003 DFKI GmbH. All rights reserved.
 */
package marytts.language.fr;

import marytts.language.fr.Preprocess;
import java.util.Locale;
import marytts.datatypes.MaryData;

import org.testng.Assert;
import org.testng.annotations.*;

import java.io.IOException;
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
        return new Object[][] {{"1", "un"}, {"2", "deux"}, {"3", "trois"}, {"4", "quatre"}, {"42", "quarante-deux"},
            {"1er", "premier"}, {"1re", "première"}, {"2e", "deuxième"}, {"3e", "troisième"}, {"4e", "quatrième"}
        };
        // @formatter:on
    }

    @DataProvider(name = "NumExpandData")
    private Object[][] numberExpansionDocDataCardinal() {
        // @formatter:off
        return new Object[][] {{"1", "un"}, {"2", "deux"}, {"3", "trois"}, {"4", "quatre"}, {"42", "quarante-deux"}};
        // @formatter:on
    }

    @DataProvider(name = "OrdinalExpandData")
    private Object[][] numberExpansionDocDataOrdinal() {
        // @formatter:off
        return new Object[][] {{"2", "deuxième"}, {"3", "troisième"}, {"4", "quatrième"}};
        // @formatter:on
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
