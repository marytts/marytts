package marytts.language.lb

import groovy.util.logging.Log4j
import groovy.xml.XmlUtil

import marytts.datatypes.MaryData
import marytts.datatypes.MaryDataType
import marytts.modules.MinimalisticPosTagger
import marytts.util.dom.DomUtils

@Log4j
class EvenMoreMinimalisticPosTagger extends MinimalisticPosTagger {

    public EvenMoreMinimalisticPosTagger(String locale) {
        super(locale, '')
    }

    @Override
    void startup() {
        assert state == MODULE_OFFLINE
        log.info "Module started ($inputType -> $outputType, locale $locale)."
        state = MODULE_RUNNING
    }

    @Override
    public MaryData process(MaryData input) {
        // get XML from input
        def inputXmlStr = DomUtils.serializeToString(input.document)
        def xml = new XmlSlurper(false, false).parseText(inputXmlStr)

        // process XML
        xml.depthFirst().findAll { it.name() == 't' }.each { token ->
            def text = token.text().trim()
            switch (text) {
                case { it =~ /,/}:
                    token.@pos = '$,'
                    break
                case { it =~ /[.?!;:]/ }:
                    token.@pos = '$.'
                    break
                case { it =~ /^[A-Z]/ }:
                    token.@pos = 'NN'
                    break
                default:
                    token.@pos = 'UNKN'
                    break
            }
        }

        // wrap XML into output MaryData
        def output = new MaryData(outputType, input.locale)
        def outputXmlStr = XmlUtil.serialize(xml)
        output.document = DomUtils.parseDocument(outputXmlStr)
        return output
    }

}
