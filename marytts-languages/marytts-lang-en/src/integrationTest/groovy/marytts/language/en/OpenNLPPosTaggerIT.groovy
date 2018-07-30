package marytts.language.en

import marytts.modules.OpenNLPPosTagger
import marytts.tests.modules.MaryModuleTestCase
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeTest
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class OpenNLPPosTaggerIT extends MaryModuleTestCase {

    OpenNLPPosTaggerIT() {
        super(true) // start MARY
    }

    String inputEnding() {
        'words'
    }

    String outputEnding() {
        'partsofspeech'
    }

    @BeforeTest
    void setUp() {
        module = new OpenNLPPosTagger('en', 'en.pos')
        module.startup()
    }

    @DataProvider
    Object[][] basenames() {
        [
                ['example2-en_US'],
                ['examplesingle-en_US'],
                ['exampleshift-en_US']
        ]
    }

    @Test(dataProvider = 'basenames')
    void testExample(String basename) {
        processAndCompare basename, Locale.US
    }

    @AfterTest
    void tearDown() {
        module.shutdown()
        module = null
    }
}
