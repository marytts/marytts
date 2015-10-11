package marytts.language.lb

import groovy.xml.*
import org.custommonkey.xmlunit.*
import org.testng.Assert
import org.testng.annotations.*

import marytts.datatypes.MaryData
import marytts.util.dom.DomUtils

/**
 * @author Tristan Hamilton
 */
class LuxembourgishPreprocessTest {

    def preprocessor

    @BeforeSuite
    void setUp() {
        preprocessor = new LuxembourgishPreprocess()
    }

    /**
     * Add any number of lemmas with their corresponding expected word form.
     * @return data provider 2-D array for parameterized testing
     */
    @DataProvider
    public Object[][] docData() {
        def data = []
        this.getClass().getResourceAsStream('preprocess_testdata.txt')?.splitEachLine(/\s+/){ toks ->
            data.add([toks[0], toks[1]])
        }
        return data
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

    @Test(dataProvider = 'docData')
    void testProcess(String token, String word) {
        // create XML
        def xmlStr = new StreamingMarkupBuilder().bind {
            mkp.declareNamespace 'xsi': 'http://www.w3.org/2001/XMLSchema-instance', '': 'http://mary.dfki.de/2002/MaryXML'
            maryxml('xml:lang': 'lb', version: 0.5) {
                p {
                    s {
                        phrase {
                            t token
                        }
                    }
                }
            }
        }.toString()

        // wrap XML into input MaryData
        def input = new MaryData(preprocessor.inputType, new Locale('lb'))
        input.document = DomUtils.parseDocument xmlStr

        // expected XML string
        def xml = new XmlSlurper(false, false).parseText xmlStr
        xml.p.s.phrase.t[0].replaceNode {
            mtu(orig:token) {
                t(word)
            }
        }
        def expected = new StreamingMarkupBuilder().bind { mkp.yield xml }.toString()

        // actual XML string after processing
        def output = preprocessor.process input
        def actual = DomUtils.serializeToString output.document

        // compare XML
        XMLUnit.ignoreWhitespace = true
        def diff = XMLUnit.compareXML expected, actual
        def detailedDiff = new DetailedDiff(diff)
        Assert.assertTrue(detailedDiff.identical(), detailedDiff.toString())
    }

    @Test(dataProvider = 'cardinalExpandData')
    void testExpandCardinal(String token, String word) {
    	double x = Double.parseDouble(token);
		String actual = preprocessor.expandCardinal(x);
		Assert.assertEquals(actual, word);
    }
}
