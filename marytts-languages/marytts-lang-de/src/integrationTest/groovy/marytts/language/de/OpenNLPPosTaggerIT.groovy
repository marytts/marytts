package marytts.language.de

import marytts.modules.OpenNLPPosTagger
import marytts.tests.modules.MaryModuleTestCase
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

    @Test
    void posExample1() {
        // setup SUT:
        module = new OpenNLPPosTagger("de", "de.pos")
        module.startup()
        // exercise:
        processAndCompare 'example1-de', Locale.GERMAN
        // teardown:
        module.shutdown()
        module = null
    }
}
