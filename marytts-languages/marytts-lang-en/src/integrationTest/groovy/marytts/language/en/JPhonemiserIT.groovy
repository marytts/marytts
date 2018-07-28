package marytts.language.en

import marytts.modules.JPhonemiser
import marytts.modules.ModuleRegistry
import marytts.tests.modules.MaryModuleTestCase
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class JPhonemiserIT extends MaryModuleTestCase {

    JPhonemiserIT() {
        super(true)
        module = ModuleRegistry.getModule(JPhonemiser.class)
    }

    @DataProvider
    Object[][] tags() {
        [
                ['.', true],
                [',', true],
                [':', true],
                ['NN', false]
        ]
    }

    @Test(dataProvider = 'tags')
    void testIsPosPunctuation(String tag, boolean expected) {
        def phonemiser = module as JPhonemiser
        assert phonemiser.isPosPunctuation(tag) == expected
    }

    @DataProvider
    Object[][] pronouncibles() {
        [
                [null, 'NN', false],
                [null, '.', false],
                ['', 'NN', false],
                ['', '.', false],
                ['foo', 'NN', true],
                ['foo', '.', true],
                ['@', 'NN', true],
                ['@', '.', false]
        ]
    }

    @Test(dataProvider = 'pronouncibles')
    void testMaybePronounceable(String text, String pos, boolean expected) {
        def phonemiser = module as JPhonemiser
        assert phonemiser.maybePronounceable(text, pos) == expected
    }
}
