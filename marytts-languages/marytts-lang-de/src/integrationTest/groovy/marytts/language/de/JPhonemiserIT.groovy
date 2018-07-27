package marytts.language.de

import marytts.modules.ModuleRegistry
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class JPhonemiserIT extends marytts.tests.modules.MaryModuleTestCase {

    JPhonemiserIT() {
        super(true) // need mary startup
        module = ModuleRegistry.getModule(JPhonemiser.class)
    }

    @DataProvider
    Object[][] tags() {
        [
                ['\$,', true],
                ['\$.', true],
                ['\$(', true],
                ['NN', false]
        ]
    }

    @Test(dataProvider = 'tags')
    void testIsPosPunctuation(tag, expected) {
        def actual = module.isPosPunctuation(tag)
        assert actual == expected
    }
}
