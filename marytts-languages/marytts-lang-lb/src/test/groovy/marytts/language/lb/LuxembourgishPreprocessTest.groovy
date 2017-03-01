package marytts.language.lb

import org.testng.Assert
import org.testng.annotations.*

import marytts.datatypes.MaryData

/**
 * @author Tristan Hamilton
 */
class LuxembourgishPreprocessTest {

    def preprocessor

    @BeforeSuite
    void setUp() {
        preprocessor = new LuxembourgishPreprocess()
    }

    @DataProvider
	private Object[][] cardinalExpandData() {
		// @formatter:off
		def testArray = [ [ "1", "eent" ],
						  [ "2", "zwee" ],
						  [ "3", "dräi" ],
						  [ "4", "véier" ],
						  [ "24", "véieranzwanzeg" ],
						  [ "42", "zweeavéierzeg" ],
                          [ "2000000", "zwou Milliounen" ],
                          [ "3567", "dräidausendfënnefhonnertsiwenasiechzeg" ] ]
		// @formatter:on
		return testArray
	}

    @Test(dataProvider = 'cardinalExpandData')
    void testExpandCardinal(String token, String word) {
    	double x = Double.parseDouble(token);
		String actual = preprocessor.expandCardinal(x);
		Assert.assertEquals(actual, word);
    }
}
