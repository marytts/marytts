package marytts.language.lb

import groovy.util.logging.Log4j;
import groovy.xml.XmlUtil

import marytts.datatypes.MaryData
import marytts.modules.JPhonemiser
import marytts.util.dom.DomUtils

/**
 * @author ingmar
 */
@Log4j
class LuxembourgishPhonemiser extends JPhonemiser {

    def french

    public LuxembourgishPhonemiser() {
        super('lb.')
        french = new marytts.language.fr.Phonemiser()
    }

    /**
     * Determine pronunciation for all tokens in the input, adding <code>ph</code> and <code>g2p_method</code> attributes.
     *
     * @param input MaryData to process
     * @return output processed MaryData
     */
    @Override
    public MaryData process(MaryData input) {
        // get XML from input
        def inputXmlStr = DomUtils.serializeToString(input.document)
        def xml = new XmlSlurper(false, false).parseText(inputXmlStr)

        // process XML
        xml.depthFirst().findAll { it.name() == 't' }.each { token ->
            def transcription
            def text = token.text().trim()
            def pos = token.@pos as String
            if (maybePronounceable(text, pos)) {
                if ((transcription = userdictLookup(text, pos))) {
                    token.@ph = transcription
                    token.@g2p_method = 'userdict'
                } else if ((transcription = lexiconLookup(text, pos))) {
                    token.@ph = transcription
                    token.@g2p_method = 'lexicon'
                } else if ((transcription = french.lexiconLookup(text, pos))) {
                    token.@ph = transcription
                    token.@g2p_method = 'lexicon_fr'
                } else {
                    def phones = lts.predictPronunciation(text)
                    try {
                        token.@ph = lts.syllabify(phones)
                        token.@g2p_method = 'rules'
                    } catch (IllegalArgumentException e) {
                        log.error "Problem with token <$text> [$phones]: $e.message"
                    }
                }
            }
        }

        // wrap XML into output MaryData
        def output = new MaryData(outputType, input.locale)
        def outputXmlStr = XmlUtil.serialize(xml)
        output.document = DomUtils.parseDocument(outputXmlStr)
        return output
    }
}
