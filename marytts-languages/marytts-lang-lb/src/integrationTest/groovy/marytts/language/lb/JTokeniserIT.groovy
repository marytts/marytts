package marytts.language.lb

import marytts.modules.JTokeniser
import marytts.modules.ModuleRegistry
import marytts.tests.modules.MaryModuleTestCase
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class JTokeniserIT extends MaryModuleTestCase {

    Locale locale

    JTokeniserIT() throws Exception {
        super(true)
        module = ModuleRegistry.getModule(JTokeniser.class)
        locale = Locale.forLanguageTag('lb')
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
        processAndCompare basename, locale
    }
}
