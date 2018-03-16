package marytts.language.fr;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.ibm.icu.util.ULocale;


import marytts.config.MaryConfiguration;
import marytts.MaryException;
import marytts.exceptions.MaryConfigurationException;
import marytts.io.serializer.XMLSerializer;
import marytts.modules.MaryModule;
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.data.item.linguistic.Word;

import com.ibm.icu.text.RuleBasedNumberFormat;

import org.apache.logging.log4j.core.Appender;
/**
 * @author Tristan Hamilton
 *
 *         Processes cardinal and ordinal numbers.
 */
public class Preprocess extends MaryModule {

    private RuleBasedNumberFormat rbnf;
    protected final String cardinalRule;
    protected final String ordinalRule;

    public Preprocess() {
	super("preprocessing");
        this.rbnf = new RuleBasedNumberFormat(ULocale.FRENCH, RuleBasedNumberFormat.SPELLOUT);
        this.cardinalRule = "%spellout-numbering";
        this.ordinalRule = getOrdinalRuleName(rbnf);
    }

    public void setDescription() {
	this.description = "French token preprocessing module.";
    }

    public void checkStartup() throws MaryConfigurationException {
    }
    /**
     *  Check if the input contains all the information needed to be
     *  processed by the module.
     *
     *  @param utt the input utterance
     *  @throws MaryException which indicates what is missing if something is missing
     */
    public void checkInput(Utterance utt) throws MaryException {
        if (!utt.hasSequence(SupportedSequenceType.WORD)) {
            throw new MaryException("Word sequence is missing", null);
        }
    }

    public Utterance process(Utterance utt, MaryConfiguration configuration) throws MaryException {

        checkForNumbers(utt);

        return utt;
    }

    protected void checkForNumbers(Utterance utt) {
        for (Word w : (Sequence<Word>) utt.getSequence(SupportedSequenceType.WORD)) {
            // Ignore phonemise or assmilated token
            if (w.soundsLike() != null) {
                continue;
            }

            String orig_text = w.getText();
            if (orig_text.matches("\\d+(e|er|re|ère|ème)")) {
                String matched = orig_text.split("e|ere|er|re|ère|ème")[0];

                if (matched.equals("1")) {
                    if (orig_text.matches("\\d+er")) {
                        w.soundsLike(expandOrdinal(Double.parseDouble(matched)));
                    } else {
                        String s = expandOrdinal(Double.parseDouble(matched));
                        w.soundsLike(s.replace("ier", "ière"));
                    }
                } else {
                    w.soundsLike(expandOrdinal(Double.parseDouble(matched)));
                }
            } else if (orig_text.matches("\\d+")) {
                w.soundsLike(expandNumber(Double.parseDouble(orig_text)));
            }

        }
    }

    protected String expandNumber(double number) {
        this.rbnf.setDefaultRuleSet(cardinalRule);
        return this.rbnf.format(number);
    }

    protected String expandOrdinal(double number) {
        this.rbnf.setDefaultRuleSet(ordinalRule);
        return this.rbnf.format(number);
    }

    /**
     * Try to extract the rule name for "expand ordinal" from the given
     * RuleBasedNumberFormat.
     * <p>
     * The rule name is locale sensitive, but usually starts with
     * "%spellout-ordinal".
     *
     * @param rbnf
     *            The RuleBasedNumberFormat from where we will try to extract
     *            the rule name.
     * @return The rule name for "ordinal spell out".
     */
    protected static String getOrdinalRuleName(final RuleBasedNumberFormat rbnf) {
        List<String> l = Arrays.asList(rbnf.getRuleSetNames());
        if (l.contains("%spellout-ordinal")) {
            return "%spellout-ordinal";
        } else if (l.contains("%spellout-ordinal-masculine")) {
            return "%spellout-ordinal-masculine";
        } else {
            for (String string : l) {
                if (string.startsWith("%spellout-ordinal")) {
                    return string;
                }
            }
        }

        throw new UnsupportedOperationException(
            "The locale " + rbnf.getLocale(ULocale.ACTUAL_LOCALE) + " doesn't supports ordinal spelling.");
    }
}
