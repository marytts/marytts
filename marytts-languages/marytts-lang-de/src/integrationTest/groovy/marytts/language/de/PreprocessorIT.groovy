package marytts.language.de

import marytts.modules.ModuleRegistry
import marytts.tests.modules.MaryModuleTestCase
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class PreprocessorIT extends MaryModuleTestCase {

    PreprocessorIT() {
        super(true) // start MARY
        module = ModuleRegistry.getModule(Preprocess.class)
    }

    String inputEnding() {
        'tokenised'
    }

    String outputEnding() {
        'preprocessed'
    }

    @DataProvider
    Object[][] basenames() {
        [
                ['SPD'],
                ['abbrev9'],
                ['abbrev10'],
                ['abbrev11'],
                ['abbrev12'],
                ['net1'],
                ['specialchar1'],
                ['unicode1'],
                ['iban1'],
                ['iban2'],
                ['iban3'],
                ['vChr']
        ]
    }

    @Test(dataProvider = 'basenames')
    void testCase(String basename) {
        processAndCompare basename, Locale.GERMAN
    }
}
