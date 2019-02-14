package marytts.language.en

import marytts.modules.ModuleRegistry
import marytts.tests.modules.MaryModuleTestCase
import org.testng.annotations.Test

class PreprocessIT extends MaryModuleTestCase {

    PreprocessIT() {
        super(true)
        module = ModuleRegistry.getModule(Preprocess.class)
    }

    String inputEnding() {
        'tokenised'
    }

    String outputEnding() {
        'words'
    }

    @Test
    void testParensAndNumber() {
        processAndCompare 'parens-and-number', Locale.US
    }

    @Test
    void testStackoverflow() {
        processAndCompare 'stackoverflow', Locale.US
    }
}
