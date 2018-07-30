package marytts.language.de

import marytts.datatypes.MaryXML
import marytts.features.ByteValuedFeatureProcessor
import marytts.features.FeatureProcessorManager
import marytts.features.FeatureRegistry
import marytts.server.Mary
import marytts.unitselection.select.Target
import marytts.util.dom.MaryDomUtils
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

class FeatureProcessorIT {

    FeatureProcessorManager fpm

    @BeforeClass
    void setupClass() {
        if (Mary.currentState() == Mary.STATE_OFF) {
            Mary.startup()
        }
        fpm = FeatureRegistry.determineBestFeatureProcessorManager(Locale.GERMAN)
    }

    // Utilities

    Target createRareWordTarget() {
        createWordTarget "Sprachsynthese"
    }

    Target createFrequentWordTarget() {
        createWordTarget "der"
    }

    Target createWordTarget(String word) {
        def doc = MaryXML.newDocument()
        def s = MaryXML.appendChildElement(doc.getDocumentElement(), MaryXML.SENTENCE)
        def t = MaryXML.appendChildElement(s, MaryXML.TOKEN)
        MaryDomUtils.setTokenText(t, word)
        def syl = MaryXML.appendChildElement(t, MaryXML.SYLLABLE)
        def ph = MaryXML.appendChildElement(syl, MaryXML.PHONE)
        new Target("dummy", ph)
    }

    // Tests

    @Test
    void testWordFrequency() {
        // Setup SUT
        def wf = (ByteValuedFeatureProcessor) fpm.getFeatureProcessor("word_frequency")
        def t1 = createRareWordTarget()
        def t2 = createFrequentWordTarget()
        // Exercise SUT
        def f1 = wf.process(t1)
        def f2 = wf.process(t2)
        // verify
        assert f1 == 0
        assert f2 == 9
    }

}
