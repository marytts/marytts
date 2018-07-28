package marytts.language.it

import marytts.modules.JPhonemiser
import marytts.modules.ModuleRegistry
import marytts.tests.modules.MaryModuleTestCase
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class JPhonemiserIT extends MaryModuleTestCase {

    JPhonemiserIT() {
        super(true) // need mary startup
        module = ModuleRegistry.getModule(JPhonemiser.class)
    }

    @DataProvider
    Object[][] tags() {
        [
                ['FB', true],
                ['FC', true],
                ['FF', true],
                ['FS', true],
                ['NN', false]
        ]
    }

    @Test(dataProvider = 'tags')
    void testIsPosPunctuation(String tag, boolean expected) {
        def phonemiser = module as JPhonemiser
        assert phonemiser.isPosPunctuation(tag) == expected
    }
}
