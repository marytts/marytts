package marytts.language.lb

import groovy.xml.*
import groovy.util.slurpersupport.*

import marytts.datatypes.MaryData
import marytts.datatypes.MaryDataType
import marytts.datatypes.MaryXML
import marytts.util.dom.DomUtils
import marytts.modules.InternalModule

import com.ibm.icu.util.ULocale
import com.ibm.icu.text.RuleBasedNumberFormat

/**
 * @author Tristan Hamilton
 *
 * NOTE: For useful example of writing youre own rbnf formatter, see icu java doc and look at icu language resource files, for example with german:
 * http://source.icu-project.org/repos/icu/icu/trunk/source/data/rbnf/de.txt
 *
 * we drop the n in 'an' if the latter part of a compound number starts with a consenant and is not 'd', 'h', 'n', 't', 'z'
 * translates to - > 40s, 50s, 60s, 70's drop the n
 *
 * ordinal number rules are regular except for 1 and 8, however I couldnt find any data on ordinals past 20, I assume its regular until we reach 1 million
 */
class LuxembourgishPreprocess extends InternalModule {

    //FIXME should region also be set to "LB"?
    static final ULocale LB_LOCALE = new ULocale.Builder().setLanguage("lb").build();
    String formatRules
    final String cardinalRule
    final String ordinalRule
    final String yearRule
    def rbnf


    public LuxembourgishPreprocess() {
        super("LuxembourgishPreprocess", MaryDataType.TOKENS, MaryDataType.WORDS, LB_LOCALE.toLocale());
        formatRules = this.getClass().getResource('preprocess/formatRules.txt').getText('UTF-8')
        rbnf = new RuleBasedNumberFormat(formatRules, LB_LOCALE)
        cardinalRule = "%spellout-numbering"
        ordinalRule = "%spellout-ordinal"
        yearRule = "%spellout-numbering-year"
    }

    public MaryData process(MaryData input) {
        // get XML from input
        def inputXmlStr = DomUtils.serializeToString input.document
        def xml = new XmlSlurper(false, false).parseText inputXmlStr
        def origText

        // process XML
        xml.depthFirst().findAll { it.name() == 't' }.each { token ->
            origText = token.text().trim()
            // ignore token if these conditions are not met, assume that if the node has the attributes "ph" or "sounds_like" the attributes won't be null
            if (!hasAncestor(token, MaryXML.SAYAS) && !token.attributes().containsKey("ph") && !token.attributes().containsKey("sounds_like")) {
                // ordinal
                if (origText.matches(~/\d+\./)) {
                    token.replaceBody expandOrdinal(Double.parseDouble(origText))
                }
                // TODO year ~ not sure if formatting is correct as we have no resources. currently based off of german
                //else if (origText.matches(~/\d{4}/)) {
                    //token.replaceBody expandYear(Double.parseDouble(origText))
                //}
                // cardinal
                else if (origText.matches(~/\d+/)) {
                    token.replaceBody expandCardinal(Double.parseDouble(origText))
                }
            }
            // if token isn't ignored but there is no handling rule don't add MTU
            if (!origText.equals(token.text())) {
	            token.replaceNode {
		            mtu(orig:origText) {
		                t(token.text())
		            }
                }
            }
        }

        // wrap XML into output MaryData
        def output = new MaryData(outputType, input.locale)
	    def outputXmlStr = new StreamingMarkupBuilder().bind { mkp.yield xml }.toString()
        output.document = DomUtils.parseDocument outputXmlStr
        return output
    }

    /**
     * Verify if <code>node</code> has an ancestor with name <code>ancestorName</code>
     */
    public static boolean hasAncestor(NodeChild node, String ancestorName) {
        // NodeChild#getAt() actually returns its Node member variable if you pass it 0... which is what we actually want
        Node p = node.getAt(0)
        while ((p = p.parent()) != null) {
            if (p.name().equals(ancestorName)) {
                return true
            }
        }
        return false
    }

    private String expandCardinal(double number) {
        rbnf.setDefaultRuleSet cardinalRule
        return rbnf.format(number)
    }

    private String expandOrdinal(double number) {
        rbnf.setDefaultRuleSet ordinalRule
        return rbnf.format(number)
    }

    private String expandYear(double number) {
        rbnf.setDefaultRuleSet yearRule
        return rbnf.format(number)
    }
}
