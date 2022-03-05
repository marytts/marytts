package marytts.language.lb;

import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.util.ULocale;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.modules.InternalModule;
import marytts.util.dom.MaryDomUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static marytts.util.dom.DomUtils.hasAncestor;

/**
 * @author Tristan Hamilton
 * <p>
 * NOTE: For useful example of writing your own RBNF formatter, see ICU java doc and look at ICU language resource files, for example with German:
 * http://source.icu-project.org/repos/icu/icu/trunk/source/data/rbnf/de.txt
 * <p>
 * we drop the n in 'an' if the latter part of a compound number starts with a consenant and is not 'd', 'h', 'n', 't', 'z'
 * translates to &rarr; 40s, 50s, 60s, 70s drop the n
 * <p>
 * ordinal number rules are regular except for 1 and 8, however I couldn't find any data on ordinals past 20, I assume its regular until we reach 1 million
 */
public class LuxembourgishPreprocess extends InternalModule {

    static final ULocale LB_LOCALE = new ULocale.Builder().setLanguage("lb").build();
    private final String formatRules;
    private final String cardinalRule;
    private final String ordinalRule;
    private final String yearRule;
    private final RuleBasedNumberFormat rbnf;

    public LuxembourgishPreprocess() throws MaryConfigurationException {
        super("LuxembourgischPreprocess", MaryDataType.TOKENS, MaryDataType.WORDS, LB_LOCALE.toLocale());
        try {
            formatRules = IOUtils.toString(getClass().getResourceAsStream("preprocess/formatRules.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MaryConfigurationException("Could not load LuxembourgishPreprocess format rules", e);
        }
        rbnf = new RuleBasedNumberFormat(formatRules, LB_LOCALE);
        cardinalRule = "%spellout-numbering";
        ordinalRule = "%spellout-ordinal";
        yearRule = "%spellout-numbering-year";
    }

    public MaryData process(MaryData input) {
        Document document = input.getDocument();

        NodeIterator tokenIterator = MaryDomUtils.createNodeIterator(document, MaryXML.TOKEN);
        Element token;
        while ((token = (Element) tokenIterator.nextNode()) != null) {
            String origText = token.getTextContent().trim();
            // ignore token if these conditions are not met, assume that if the node has the attributes "ph" or "sounds_like" the attributes won't be null
            if (!hasAncestor(token, MaryXML.SAYAS)
                    && !token.hasAttribute("ph")
                    && !token.hasAttribute("sounds_like")) {
                // ordinal
                if (origText.matches("\\d+\\.")) {
                    token.setTextContent(expandOrdinal(Double.parseDouble(origText)));
                }
                // TODO year ~ not sure if formatting is correct as we have no resources. currently based off of German
                //else if (origText.matches(~/\d{4}/)) {
                //    token.replaceBody expandYear(Double.parseDouble(origText))
                //}
                // cardinal
                else if (origText.matches("\\d+")) {
                    token.setTextContent(expandCardinal(Double.parseDouble(origText)));
                }
            }
            if (!origText.equals(MaryDomUtils.tokenText(token))) {
                MaryDomUtils.encloseWithMTU(token, origText, null);
            }
        }

        MaryData output = new MaryData(getOutputType(), input.getLocale());
        output.setDocument(document);
        return output;
    }

    private String expandCardinal(double number) {
        rbnf.setDefaultRuleSet(cardinalRule);
        return rbnf.format(number);
    }

    private String expandOrdinal(double number) {
        rbnf.setDefaultRuleSet(ordinalRule);
        return rbnf.format(number);
    }

    private String expandYear(double number) {
        rbnf.setDefaultRuleSet(yearRule);
        return rbnf.format(number);
    }
}
