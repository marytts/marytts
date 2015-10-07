package marytts.language.lb

import groovy.xml.*
import org.custommonkey.xmlunit.*
import org.testng.Assert
import org.testng.annotations.*

import marytts.datatypes.MaryData
import marytts.util.dom.DomUtils

/**
 * @author ingmar
 */
class LuxembourgishPhonemiserTest {

    def phonemiser

    @BeforeSuite
    void setUp() {
        phonemiser = new LuxembourgishPhonemiser()
    }

    /**
     * Add any number of lemmas with their corresponding expected transcription to the map below.
     * @return data provider 2-D array for parameterized testing
     */
    @DataProvider
    public Object[][] data() {
        return this.getClass().getResourceAsStream('phonemiser_testdata.txt')?.readLines()?.collect { line ->
            def (lemma, transcription, pos, method) = line.split('\t')
            [lemma, transcription, pos, method]
        }
    }

    @Test(dataProvider = 'data')
    void testProcess(String lemma, String expectedTranscription, String expectedPos, String expectedMethod) {
        // create XML
        def xmlStr = new StreamingMarkupBuilder().bind {
            mkp.declareNamespace 'xsi': 'http://www.w3.org/2001/XMLSchema-instance', '': 'http://mary.dfki.de/2002/MaryXML'
            maryxml('xml:lang': 'lb', version: 0.5) {
                p {
                    s {
                        phrase {
                            t pos: expectedPos, lemma
                        }
                    }
                }
            }
        }.toString()

        // wrap XML into input MaryData
        def input = new MaryData(phonemiser.inputType, new Locale('lb'))
        input.document = DomUtils.parseDocument(xmlStr)

        // expected XML string
        def xml = new XmlSlurper(false, false).parseText(xmlStr)
        def token = xml.p.s.phrase.t[0]
        token.@g2p_method = expectedMethod
        token.@ph = expectedTranscription
        def expected = XmlUtil.serialize(xml)

        // actual XML string after processing
        def output = phonemiser.process(input)
        def actual = DomUtils.serializeToString(output.document)

        // compare XML
        XMLUnit.ignoreWhitespace = true
        def diff = XMLUnit.compareXML(expected, actual)
        def detailedDiff = new DetailedDiff(diff)
        Assert.assertTrue(detailedDiff.identical(), detailedDiff.toString())
    }
}
