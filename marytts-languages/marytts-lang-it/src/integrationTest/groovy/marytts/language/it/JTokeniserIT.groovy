package marytts.language.it

import marytts.modules.ModuleRegistry
import marytts.tests.modules.MaryModuleTestCase
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class JTokeniserIT extends MaryModuleTestCase {

    JTokeniserIT() {
        super(true) // need mary startup
        module = ModuleRegistry.getModule(JTokeniser.class)
    }

    String inputEnding() {
        'txt'
    }

    String outputEnding() {
        'tokenised'
    }

    @DataProvider
    Object[][] basenames() {
        [
                ['dots1'],
                ['dots2'],
                ['dots3'],
                ['exclam'],
                ['quest']
        ]
    }

    @Test(dataProvider = 'basenames')
    void testCase(String basename) {
        processAndCompare basename, Locale.ITALIAN
    }
}
