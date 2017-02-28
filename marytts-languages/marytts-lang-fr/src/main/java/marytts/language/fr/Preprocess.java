package marytts.language.fr;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.ibm.icu.util.ULocale;

import marytts.datatypes.MaryData;
import marytts.modules.InternalModule;


import marytts.io.XMLSerializer;
import marytts.data.Utterance;
import marytts.data.item.linguistic.Word;

import com.ibm.icu.text.RuleBasedNumberFormat;

/**
 * @author Tristan Hamilton
 *
 *         Processes cardinal and ordinal numbers.
 */
public class Preprocess extends InternalModule {

    private RuleBasedNumberFormat rbnf;
    protected final String cardinalRule;
    protected final String ordinalRule;

    public Preprocess()
    {
        super("Preprocess", Locale.FRENCH);
        this.rbnf = new RuleBasedNumberFormat(ULocale.FRENCH, RuleBasedNumberFormat.SPELLOUT);
        this.cardinalRule = "%spellout-numbering";
        this.ordinalRule = getOrdinalRuleName(rbnf);
    }

    public MaryData process(MaryData d)
        throws Exception
    {
        Utterance utt = d.getData();

        checkForNumbers(utt);

        MaryData result = new MaryData(d.getLocale(), utt);
        return result;
    }

    protected void checkForNumbers(Utterance utt)
    {
        for (Word w: utt.getAllWords())
        {
            // Ignore phonemise or assmilated token
            if ((w.getPhonemes().size() > 0) || (w.soundsLike() != null))
                continue;

            String orig_text = w.getText();
            if (orig_text.matches("\\d+(e|er|re|ère|ème)"))
            {
                String matched = orig_text.split("e|ere|er|re|ère|ème")[0];

                if (matched.equals("1"))
                {
                    if (orig_text.matches("\\d+er"))
                    {
                        w.soundsLike(expandOrdinal(Double.parseDouble(matched)));
                    }
                    else
                    {
                        String s = expandOrdinal(Double.parseDouble(matched));
                        w.soundsLike(s.replace("ier", "ière"));
                    }
                }
                else
                {
                    w.soundsLike(expandOrdinal(Double.parseDouble(matched)));
                }
            }
            else if (orig_text.matches("\\d+"))
            {
                w.soundsLike(expandNumber(Double.parseDouble(orig_text)));
            }

        }
    }

    protected String expandNumber(double number)
    {
        this.rbnf.setDefaultRuleSet(cardinalRule);
        return this.rbnf.format(number);
    }

    protected String expandOrdinal(double number)
    {
        this.rbnf.setDefaultRuleSet(ordinalRule);
        return this.rbnf.format(number);
    }

    /**
     * Try to extract the rule name for "expand ordinal" from the given RuleBasedNumberFormat.
     * <p>
     * The rule name is locale sensitive, but usually starts with "%spellout-ordinal".
     *
     * @param rbnf
     *            The RuleBasedNumberFormat from where we will try to extract the rule name.
     * @return The rule name for "ordinal spell out".
     */
    protected static String getOrdinalRuleName(final RuleBasedNumberFormat rbnf)
    {
        List<String> l = Arrays.asList(rbnf.getRuleSetNames());
        if (l.contains("%spellout-ordinal"))
        {
            return "%spellout-ordinal";
        }
        else if (l.contains("%spellout-ordinal-masculine"))
        {
            return "%spellout-ordinal-masculine";
        }
        else
        {
            for (String string : l)
            {
                if (string.startsWith("%spellout-ordinal"))
                {
                    return string;
                }
            }
        }

        throw new UnsupportedOperationException("The locale " + rbnf.getLocale(ULocale.ACTUAL_LOCALE)
                                                + " doesn't supports ordinal spelling.");
    }
}
