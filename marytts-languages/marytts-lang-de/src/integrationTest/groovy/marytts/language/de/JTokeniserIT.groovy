package marytts.language.de

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
                ['simplesentence'],
                ['twosimplesentences'],
                ['ordinal1'],
                ['ordinal2'],
//                ['ordinal3'],
                ['abbrev1'],
//                ['abbrev2'],
                ['abbrev3'],
                ['abbrev4'],
                ['abbrev5'],
                ['abbrev6'],
                ['abbrev7'],
//                ['abbrev8'],
                ['abbrev9'],
                ['net1'],
                ['web1'],
                ['web2'],
                ['omission1'],
                ['omission2'],
                ['omission3'],
                ['digit1'],
                ['digit2'],
                ['digit3'],
                ['dots1'],
                ['dots2'],
                ['dots3'],
                ['exclam'],
                ['quest']
        ]
    }

    @Test(dataProvider = 'basenames')
    void testCase(String basename) {
        processAndCompare basename, Locale.GERMAN
    }

}
