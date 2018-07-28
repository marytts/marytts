package marytts.features

import groovy.util.logging.Log4j
import marytts.datatypes.MaryData
import marytts.datatypes.MaryDataType
import marytts.datatypes.MaryXML
import marytts.unitselection.select.HalfPhoneTarget
import marytts.unitselection.select.Target
import marytts.util.dom.MaryDomUtils
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import org.w3c.dom.Element
import org.w3c.dom.NodeList

@Log4j
class FeatureProcessorIT {

    MaryData acoustparams
    List<Target> phoneTargets
    List<Target> halfphoneTargets
    FeatureProcessorManager mgr

    @BeforeClass
    void setupClass() {
        acoustparams = new MaryData(MaryDataType.ACOUSTPARAMS, Locale.ENGLISH)
        acoustparams.readFrom FeatureProcessorIT.class.getResourceAsStream('test1.acoustparams')
        phoneTargets = []
        halfphoneTargets = []
        def doc = acoustparams.document
        def segs = MaryDomUtils.createNodeIterator(doc, doc, MaryXML.PHONE, MaryXML.BOUNDARY)
        def s
        while (s = segs.nextNode() as Element) {
            def phone = s.tagName == MaryXML.PHONE ? s.getAttribute('p') : '_'
            phoneTargets << new Target(phone, s)
            halfphoneTargets << new HalfPhoneTarget(phone, s, true)
            halfphoneTargets << new HalfPhoneTarget(phone, s, false)
        }

        if (!System.properties.'mary.base') {
            System.properties.'mary.base' = '.'
            log.warn "System property 'mary.base' is not defined -- trying ${new File('.').absolutePath}"
            log.warn "-- if this fails, please start this using VM property '-Dmary.base=/path/to/mary/runtime'!"
        }
        mgr = new FeatureProcessorManager("en_US")
    }

    @Test
    void haveAcoustparams() {
        assert acoustparams
        assert acoustparams.document
    }

    @Test
    void haveTargets() {
        assert phoneTargets
        assert halfphoneTargets
        assert phoneTargets.size() > 0
        assert phoneTargets.size() * 2 == halfphoneTargets.size()
    }

    @Test
    void sameNumberOfTargetsAndSegments() {
        def segs = acoustparams.document.getElementsByTagName(MaryXML.PHONE)
        def boundaries = acoustparams.document.getElementsByTagName(MaryXML.BOUNDARY)
        assert segs.length + boundaries.length == phoneTargets.size()
    }

    String getPhoneName(Element segmentOrBoundary) {
        switch (segmentOrBoundary) {
            case null:
                return 'null'
            case { segmentOrBoundary.tagName == MaryXML.PHONE }:
                return segmentOrBoundary.getAttribute('p')
            default:
                return '_'
        }
    }

    @DataProvider
    Object[][] targets() {
        phoneTargets + halfphoneTargets
    }

    @Test(dataProvider = 'targets')
    void targetHasMatchingXML(Target p) {
        def sOrB = p.maryxmlElement
        assert sOrB
        assert sOrB.tagName == MaryXML.PHONE || sOrB.tagName == MaryXML.BOUNDARY
        assert p.name == getPhoneName(sOrB)
    }

    @Test
    void segmentNavigators() {
        def segmentNavigator = new MaryGenericFeatureProcessors.SegmentNavigator()
        def prevSegmentNavigator = new MaryGenericFeatureProcessors.PrevSegmentNavigator()
        def nextSegmentNavigator = new MaryGenericFeatureProcessors.NextSegmentNavigator()
        def prevprevSegmentNavigator = new MaryGenericFeatureProcessors.PrevPrevSegmentNavigator()
        def nextnextSegmentNavigator = new MaryGenericFeatureProcessors.NextNextSegmentNavigator()
        def prevprev
        def prev
        def seg = phoneTargets[0].maryxmlElement
        def next = phoneTargets[1].maryxmlElement
        phoneTargets.each { t ->
            def prevprevElement = prevprevSegmentNavigator.getElement(t)
            assert prevprev == prevprevElement, "Mismatch: expected ${getPhoneName(prevprev)}, got  ${getPhoneName(prevprevElement)}"
            assert prev == prevSegmentNavigator.getElement(t)
            assert seg == segmentNavigator.getElement(t)
            def nextElement = nextSegmentNavigator.getElement(t)
            assert next == nextElement, "Mismatch: expected ${getPhoneName(next)}, got ${getPhoneName(nextElement)}"
            prevprev = prev
            prev = seg
            seg = next
            next = nextnextSegmentNavigator.getElement(t)
        }
    }

    @Test
    void syllableNavigators() {
        def syllableNavigator = new MaryGenericFeatureProcessors.SyllableNavigator()
        def prevSyllableNavigator = new MaryGenericFeatureProcessors.PrevSyllableNavigator()
        def prevprevSyllableNavigator = new MaryGenericFeatureProcessors.PrevPrevSyllableNavigator()
        def nextSyllableNavigator = new MaryGenericFeatureProcessors.NextSyllableNavigator()
        def nextnextSyllableNavigator = new MaryGenericFeatureProcessors.NextNextSyllableNavigator()
        def phrases = acoustparams.document.getElementsByTagName(MaryXML.PHRASE) as NodeList
        assert phrases.length == 2
        def phrase1 = phrases.item(0) as Element
        def phrase2 = phrases.item(1) as Element
        def syllables1 = phrase1.getElementsByTagName(MaryXML.SYLLABLE) as NodeList
        assert syllables1.length == 2
        def syllables2 = phrase2.getElementsByTagName(MaryXML.SYLLABLE) as NodeList
        def lastSylPhrase1 = syllables1.item(syllables1.length - 1)
        def prevSylPhrase1 = syllables1.item(syllables1.length - 2)
        def firstSylPhrase2 = syllables2.item(0)
        def secondSylPhrase2 = syllables2.item(1)
        def thirdSylPhrase2 = syllables2.item(2)
        def lastSegPhrase1 = MaryDomUtils.getLastChildElement(lastSylPhrase1)
        def firstSegPhrase2 = MaryDomUtils.getFirstChildElement(firstSylPhrase2)
        def t1 = new Target(getPhoneName(lastSegPhrase1), lastSegPhrase1)
        def t2 = new Target(getPhoneName(firstSegPhrase2), firstSegPhrase2)
        assert lastSylPhrase1 == syllableNavigator.getElement(t1)
        assert prevSylPhrase1 == prevSyllableNavigator.getElement(t1)
        assert prevprevSyllableNavigator.getElement(t1) == null
        // syllable navigator crosses phrase boundaries
        assert firstSylPhrase2 == nextSyllableNavigator.getElement(t1)
        assert secondSylPhrase2 == nextnextSyllableNavigator.getElement(t1)
        // and for the other target:
        assert prevSylPhrase1 == prevprevSyllableNavigator.getElement(t2)
        assert lastSylPhrase1 == prevSyllableNavigator.getElement(t2)
        assert firstSylPhrase2 == syllableNavigator.getElement(t2)
        assert secondSylPhrase2 == nextSyllableNavigator.getElement(t2)
        assert thirdSylPhrase2 == nextnextSyllableNavigator.getElement(t2)
    }

    @Test
    void otherNavigators() {
        def firstSegInWordNavigator = new MaryGenericFeatureProcessors.FirstSegmentInWordNavigator()
        def firstSegNextWordNavigator = new MaryGenericFeatureProcessors.FirstSegmentNextWordNavigator()
        def lastSegInWordNavigator = new MaryGenericFeatureProcessors.LastSegmentInWordNavigator()
        def firstSylInWordNavigator = new MaryGenericFeatureProcessors.FirstSyllableInWordNavigator()
        def lastSylInWordNavigator = new MaryGenericFeatureProcessors.LastSyllableInWordNavigator()
        def lastSylInPhraseNavigator = new MaryGenericFeatureProcessors.LastSyllableInPhraseNavigator()
        def wordNavigator = new MaryGenericFeatureProcessors.WordNavigator()
        def nextWordNavigator = new MaryGenericFeatureProcessors.NextWordNavigator()
        def lastWordInSentenceNavigator = new MaryGenericFeatureProcessors.LastWordInSentenceNavigator()

        def phrases = acoustparams.getDocument().getElementsByTagName(MaryXML.PHRASE) as NodeList
        assert phrases.length == 2
        def phrase1 = phrases.item(0) as Element
        def phrase2 = phrases.item(1) as Element
        def syllables1 = phrase1.getElementsByTagName(MaryXML.SYLLABLE)
        assert syllables1.length == 2
        def syllables2 = phrase2.getElementsByTagName(MaryXML.SYLLABLE)
        def firstSylPhrase1 = syllables1.item(0)
        def lastSylPhrase1 = syllables1.item(syllables1.length - 1)
        def firstSylPhrase2 = syllables2.item(0)
        def lastSylPhrase2 = syllables2.item(syllables2.length - 1)

        def firstSegPhrase1 = MaryDomUtils.getFirstChildElement(firstSylPhrase1)
        def lastSegPhrase1 = MaryDomUtils.getLastChildElement(lastSylPhrase1)
        def firstSegPhrase2 = MaryDomUtils.getFirstChildElement(firstSylPhrase2)

        def firstWord = firstSylPhrase1.parentNode
        def firstWordPhrase2 = firstSylPhrase2.parentNode
        def lastWord = lastSylPhrase2.parentNode

        def t0 = new Target(getPhoneName(firstSegPhrase1), firstSegPhrase1)
        def t1 = new Target(getPhoneName(lastSegPhrase1), lastSegPhrase1)
        def t2 = new Target(getPhoneName(firstSegPhrase2), firstSegPhrase2)

        assert firstSegPhrase1 == firstSegInWordNavigator.getElement(t0)
        assert firstSegPhrase1 == firstSegInWordNavigator.getElement(t1)

        assert firstSegPhrase2 == firstSegNextWordNavigator.getElement(t0)
        assert firstSegPhrase2 == firstSegNextWordNavigator.getElement(t1)

        assert lastSegPhrase1 == lastSegInWordNavigator.getElement(t0)
        assert lastSegPhrase1 == lastSegInWordNavigator.getElement(t1)

        assert firstSylPhrase1 == firstSylInWordNavigator.getElement(t0)
        assert firstSylPhrase1 == firstSylInWordNavigator.getElement(t1)

        assert lastSylPhrase1 == lastSylInWordNavigator.getElement(t0)
        assert lastSylPhrase1 == lastSylInWordNavigator.getElement(t1)

        assert lastSylPhrase1 == lastSylInPhraseNavigator.getElement(t0)
        assert lastSylPhrase1 == lastSylInPhraseNavigator.getElement(t1)
        assert lastSylPhrase2 == lastSylInPhraseNavigator.getElement(t2)

        assert firstWord == wordNavigator.getElement(t0)
        assert firstWordPhrase2 == wordNavigator.getElement(t2)

        assert firstWordPhrase2 == nextWordNavigator.getElement(t0)

        assert lastWord == lastWordInSentenceNavigator.getElement(t0)
        assert lastWord == lastWordInSentenceNavigator.getElement(t2)
    }

    @DataProvider
    Object[][] halfphoneTargets() {
        halfphoneTargets
    }

    @Test(dataProvider = 'halfphoneTargets')
    void phone(Target t) {
        def phoneFP = mgr.getFeatureProcessor('phone') as ByteValuedFeatureProcessor
        def phone = getPhoneName(t.maryxmlElement)
        def predicted = phoneFP.values[phoneFP.process(t)]
        assert phone == predicted
    }

    // TODO: write test methods for the all the other feature processors...
}
