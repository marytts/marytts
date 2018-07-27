package marytts.language.de

import marytts.LocalMaryInterface
import marytts.datatypes.MaryDataType
import marytts.util.dom.DomUtils
import org.testng.annotations.Test

class MaryInterfaceDeIT {

    @Test
    void canSetLocale() {
        def mary = new LocalMaryInterface()
        def loc = Locale.GERMAN
        assert loc != mary.locale
        mary.locale = loc
        assert loc == mary.locale
    }

    @Test
    void canProcessTokensToAllophones() {
        // setup
        def mary = new LocalMaryInterface()
        mary.inputType = MaryDataType.TOKENS.name()
        mary.outputType = MaryDataType.ALLOPHONES.name()
        mary.locale = Locale.GERMAN
        def example = MaryDataType.getExampleText(MaryDataType.TOKENS, mary.locale)
        assert example
        def tokens = DomUtils.parseDocument(example)
        // exercise
        def allos = mary.generateXML(tokens)
        // verify
        assert allos
    }
}
