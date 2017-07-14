package marytts.language.en;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.exceptions.MaryConfigurationException;
import marytts.io.serializer.XMLSerializer;
import marytts.modules.InternalModule;
import marytts.util.MaryRuntimeUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.data.item.linguistic.Word;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Category;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * @author Tristan Hamilton
 *
 *         <p>
 *         Can process following formats:
 *         <ul>
 *         <li>cardinal (handled by real number)
 *         <li>ordinal
 *         <li>year (as a 4 digit number or any number followed by AD/BC
 *         variation)
 *         <li>currency
 *         <li>numberandword together
 *         <li>dashes (read each number singly) or (split into two words)
 *         <li>underscores
 *         <li>decimal point, minus symbol (real numbers) also handles &#037;,
 *         however Jtokenizer splits &#037; into separate tokens
 *         <li>time
 *         <li>dates (in format mm/dd/yyyy)
 *         <li>acronyms (only split into single characters, never expanded)
 *         <li>abbreviations (list of known expansions in resource
 *         preprocess/abbrev.dat, a properties file separated by whitespace. If
 *         an abbrev has two different expansions then the capitalized version
 *         comes first, followed by a comma)
 *         <li>contractions &rarr; first check lexicon, if not then &rarr; split
 *         and check if map contains contraction, if not then just remove
 *         apostrophe else &rarr; split before apostrophe into two tokens, use
 *         map to manually add ph &rarr; for 's if word ends in c,f,k,p,t then
 *         add ph &#061; s otherwise ph &#061; z
 *         <li>ampersand &amp;, "at" &#064; symbol, &rarr; symbols
 *         <li>urls &rarr; note that jtokenizer splits off http[s]?://
 *         <li>number ranges "18-35"
 *         <li>words without vowels &rarr; first check lexicon, if not then
 *         separate into single character tokens
 *         <li>#hashtags
 *         <li>single "A/a" character &rarr; if there is no next token or the
 *         next token is punctuation or next token string.length &#061;&#061; 1
 *         <li>should also as a last processing attempt, split by
 *         punctuation,symbols,etc. and attempt to process these tokens
 *         separately
 *         <li>durations hours:minutes:seconds(:milliseconds)
 *         <li>numbers followed by an s
 *         <li>punctuation &rarr; add ph attribute to tag to prevent
 *         phonemisation
 *         </ul>
 *         <p>
 *         May include:
 *         <ul>
 *         <li>roman numerals
 *         </ul>
 */
public class Preprocess extends InternalModule {

    // abbreviations map
    private Map<Object, Object> abbrevMap;

    // symbols map
    private static final Map<String, String> symbols;

    // contractions map
    private static final Map<String, String[]> contractions;

    // icu4j stuff
    private RuleBasedNumberFormat rbnf;
    protected final String cardinalRule;
    protected final String ordinalRule;
    protected final String yearRule;
    private DateFormat df;

    // Regex matching patterns
    private static final Pattern moneyPattern;
    private static final Pattern timePattern;
    private static final Pattern durationPattern;
    private static final Pattern abbrevPattern;
    private static final Pattern acronymPattern;
    private static final Pattern realNumPattern;
    private static final Pattern numberWordPattern;
    private static final Pattern datePattern;
    private static final Pattern yearPattern;
    private static final Pattern contractPattern;
    private static final Pattern symbolsPattern;
    private static final Pattern URLPattern;
    private static final Pattern rangePattern;
    private static final Pattern consonantPattern;
    private static final Pattern punctuationPattern;
    private static final Pattern myPunctPattern;
    private static final Pattern hashtagPattern;
    private static final Pattern ordinalPattern;
    private static final Pattern currencySymbPattern;
    private static final Pattern numberSPattern;

    // Regex initialization
    static {
        moneyPattern = Pattern.compile("(\\d+)(\\.\\d+)?");
        currencySymbPattern = Pattern.compile("[$£€]");
        timePattern = Pattern.compile(
                          "((0?[0-9])|(1[0-1])|(1[2-9])|(2[0-3])):([0-5][0-9])(a\\.m\\.|am|pm|p\\.m\\.|a\\.m|p\\.m)?",
                          Pattern.CASE_INSENSITIVE);
        yearPattern = Pattern.compile("(\\d+)(bc|ad|b\\.c\\.|b\\.c|a\\.d\\.|a\\.d)",
                                      Pattern.CASE_INSENSITIVE);
        ordinalPattern = Pattern.compile("\\d+(st|nd|rd|th)", Pattern.CASE_INSENSITIVE);
        durationPattern = Pattern.compile("(\\d+):([0-5][0-9]):([0-5][0-9])(:([0-5][0-9]))?");
        abbrevPattern = Pattern.compile("[a-zA-Z]{2,}\\.");
        acronymPattern = Pattern.compile("([a-zA-Z]\\.[a-zA-Z](\\.)?)+([a-zA-Z](\\.)?)?");
        realNumPattern = Pattern.compile("(-)?(\\d+)?(\\.(\\d+)(%)?)?");
        numberWordPattern = Pattern.compile("([a-zA-Z]+[0-9]+|[0-9]+[a-zA-Z]+)\\w*");
        datePattern = Pattern.compile("(\\d{2})[\\/\\.](\\d{2})[\\/\\.]\\d{4}");
        contractPattern = Pattern.compile("[a-zA-Z]+('[a-zA-Z]+)");
        symbolsPattern = Pattern.compile("[@%#\\/\\+=&><-]");
        rangePattern = Pattern.compile("([0-9]+)-([0-9]+)");
        consonantPattern = Pattern.compile("[b-df-hj-np-tv-z]+", Pattern.CASE_INSENSITIVE);
        punctuationPattern = Pattern.compile("\\p{Punct}");
        numberSPattern = Pattern.compile("([0-9]+)([sS])");
        myPunctPattern = Pattern.compile(",\\.:;?'\"");
        hashtagPattern = Pattern.compile("(#)(\\w+)");
        URLPattern = Pattern.compile(
                         "(https?:\\/\\/)?((www\\.)?([-a-zA-Z0-9@:%._\\\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\\\+.~#?&\\/=]*)))");
    }

    // HashMap initialization
    static {
        contractions = new HashMap<String, String[]>();
        contractions.put("'s", new String[] {"z", "s"});
        contractions.put("'ll", new String[] {"l"});
        contractions.put("'ve", new String[] {"v"});
        contractions.put("'d", new String[] {"d"});
        contractions.put("'m", new String[] {"m"});
        contractions.put("'re", new String[] {"r"});

        symbols = new HashMap<String, String>();
        symbols.put("@", "at");
        symbols.put("#", "hashtag");
        symbols.put("/", "forward slash");
        symbols.put("%", "per cent");
        symbols.put("+", "plus");
        symbols.put("-", "minus");
        symbols.put("=", "equals");
        symbols.put(">", "greater than");
        symbols.put("<", "less than");
        symbols.put("&", "and");
    }

    public Preprocess() {
        super("Preprocess", Locale.ENGLISH);
        this.rbnf = new RuleBasedNumberFormat(ULocale.ENGLISH, RuleBasedNumberFormat.SPELLOUT);
        this.cardinalRule = "%spellout-numbering";
        this.ordinalRule = getOrdinalRuleName(rbnf);
        this.yearRule = getYearRuleName(rbnf);
        this.df = DateFormat.getDateInstance(DateFormat.LONG, ULocale.ENGLISH);
        try {
            this.abbrevMap = loadAbbrevMap();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Utterance process(Utterance utt) throws Exception {
        expand(utt);

	return utt;
    }

    /***
     * processes an utterane, from Tokens to Words which can be phonemised.
     *
     * @param utt
     *            the utterance
     * @throws ParseException
     *             parse exception
     * @throws IOException
     *             IO Exception
     * @throws MaryConfigurationException
     *             mary configuration exception
     */
    protected void expand(Utterance utt) throws ParseException, IOException,
        MaryConfigurationException {
        String whichCurrency = "";
        boolean URLFirst = false;
        boolean isYear;
        boolean isURL = false;
        boolean puncSplit = false;
        boolean dashSplit = false;
        String webEmailTemp = "";
        boolean splitContraction;

        Sequence<Word> tokens = (Sequence<Word>) utt.getSequence(SupportedSequenceType.WORD);
        for (int idx_token = 0; idx_token < tokens.size(); idx_token++) {
            Word t = tokens.get(idx_token);

            /*
             * PRELIM FOR EACH NODE
             */

            // to accommodate the first token being a url
            if (URLFirst) {
                t = tokens.get(idx_token - 1);
                URLFirst = false;
            }
            isYear = true;
            splitContraction = false;

            if (t.soundsLike() != null) {
                continue;
            }

            // save the original token text
            String orig_text = t.getText();
            String expanded_text = t.getText();

            // remove commas
            if (orig_text.matches("[\\$|£|€]?\\d+,[\\d,]+")) {
                expanded_text = orig_text.replaceAll(",", "");

                // presume that a 4 digit number which had commas is not a year
                if (expanded_text.matches("\\d{4}")) {
                    isYear = false;
                }
            }

            // isYear extra check
            if (expanded_text.matches("\\d{4}") && !whichCurrency.equals("")) {
                isYear = false;
            }

            // check if currency
            if (expanded_text.matches(currencySymbPattern.pattern())) {
                whichCurrency = expanded_text;
            }

            /*
             * ACTUAL PROCESSING
             */
            String token_text = expanded_text;

            // ordinal
            if (token_text.matches("(?i)" + ordinalPattern.pattern())) {
                String matched = token_text.split("(?i)st|nd|rd|th")[0];
                expanded_text = expandOrdinal(Double.parseDouble(matched));

            }
            // single a or A character
            else if (token_text.matches("[aA]")) {
                Word next_token = null;

                if ((idx_token + 1) < tokens.size()) {
                    next_token = tokens.get(idx_token + 1);
                }

                if (next_token == null || next_token.getText().matches(myPunctPattern.pattern())
                        || next_token.getText().length() == 1) {
                    expanded_text = "_a";
                }

            }
            // date
            else if (token_text.matches(datePattern.pattern())) {
                expanded_text = expandDate(token_text);
            }
            // number followed by s
            else if (token_text.matches(numberSPattern.pattern())) {
                expanded_text = expandNumberS(token_text);

            }
            // year with bc or ad
            else if (token_text.matches("(?i)" + yearPattern.pattern())) {
                expanded_text = expandYearBCAD(token_text);

            }
            // year as just 4 digits &rarr; this should always be checked BEFORE
            // real number
            else if (token_text.matches("\\d{4}") && isYear == true) {
                expanded_text = expandYear(Double.parseDouble(token_text));

            }
            // wordAndNumber &rarr; must come AFTER year
            else if (token_text.matches(numberWordPattern.pattern())) {
                expanded_text = expandWordNumber(token_text);

            }
            // real number & currency
            else if (token_text.matches(realNumPattern.pattern())) {
                if (!whichCurrency.equals("")) {
                    expanded_text = expandMoney(token_text, whichCurrency);
                    whichCurrency = "";
                } else {
                    expanded_text = expandRealNumber(token_text);
                }

            }
            // contractions
            else if (token_text.matches(contractPattern.pattern())) {
                // first check lexicon
                if (MaryRuntimeUtils.checkLexicon("en_US", token_text).length == 0) {
                    Matcher contractionMatch = contractPattern.matcher(token_text);
                    contractionMatch.find();

                    // if no contraction we allow g2p rules to handle
                    if (!contractions.containsKey(contractionMatch.group(1))) {
                        expanded_text = token_text.replaceAll("'", "");
                    }

                    // FIXME: we do not want to have to phonological word => for
                    // now we do not split !
                    // // if not in lexicon and we have a contraction expansion
                    // then split into two tokens
                    // else
                    // {
                    // splitContraction = true;
                    // expanded_text = splitContraction(token_text);
                    // }
                }

            }
            // acronym
            else if (token_text.matches(acronymPattern.pattern())) {
                expanded_text = expandAcronym(token_text);

            }
            // abbreviation
            else if ((token_text.matches(abbrevPattern.pattern())
                      || this.abbrevMap.containsKey(token_text.toLowerCase())) && !isURL) {
                Word next_token = null;
                boolean nextTokenIsCapital = false;

                if ((idx_token + 1) < tokens.size()) {
                    next_token = tokens.get(idx_token + 1);
                }

                if (next_token != null && Character.isUpperCase(next_token.getText().charAt(0))) {
                    nextTokenIsCapital = true;
                }

                expanded_text = expandAbbreviation(token_text, nextTokenIsCapital);

            }
            // time
            else if (token_text.matches("(?i)" + timePattern.pattern())) {
                Word next_token = null;
                boolean next_token_is_time = false;

                if ((idx_token + 1) < tokens.size()) {
                    next_token = tokens.get(idx_token + 1);
                }

                if (next_token != null && next_token.getText().matches("a\\.m\\.|AM|PM|am|pm|p\\.m\\.")) {
                    next_token_is_time = true;
                }
                expanded_text = expandTime(token_text, next_token_is_time);

            }
            // duration
            else if (token_text.matches(durationPattern.pattern())) {
                expanded_text = expandDuration(token_text);

            }
            // hashtags
            else if (token_text.matches(hashtagPattern.pattern())) {
                expanded_text = expandHashtag(token_text);

            }
            // URLs
            else if (token_text.matches(URLPattern.pattern())) {
                // matching group 2 contains the chunk we want
                Matcher urlMatcher = URLPattern.matcher(token_text);
                urlMatcher.find();
                webEmailTemp = token_text;
                isURL = true;
                expanded_text = expandURL(urlMatcher.group(2));

            }
            // dot . for web and email addresses
            else if (token_text.equals(".") && isURL) {
                expanded_text = "dot";
                webEmailTemp = webEmailTemp.replaceFirst("\\.", "dot");

                if (!webEmailTemp.contains(".")) {
                    isURL = false;
                }
            }
            // symbols
            else if (token_text.matches(symbolsPattern.pattern())) {
                expanded_text = symbols.get(token_text);

            }
            // number ranges &rarr; before checking for dashes
            else if (token_text.matches(rangePattern.pattern())) {
                expanded_text = expandRange(token_text);

            }
            // dashes and underscores
            else if (token_text.contains("-") || token_text.contains("_")) {
                dashSplit = true;
                String[] new_tokens = token_text.split("[-_]");
                int i = 0;
                for (String tok : new_tokens) {
                    if (tok.matches("\\d+")) {
                        String newTok = "";
                        for (char c : tok.toCharArray()) {
                            newTok += expandNumber(Double.parseDouble(String.valueOf(c))) + " ";
                        }
                        new_tokens[i] = newTok;
                    }
                    i++;
                }
                expanded_text = Arrays.toString(new_tokens).replaceAll("[,\\]\\[]", "");

            }
            // words containing only consonants
            else if (token_text.matches("(?i)" + consonantPattern.pattern())) {
                // first check lexicon
                if (MaryRuntimeUtils.checkLexicon("en_US", token_text).length == 0) {
                    expanded_text = expandConsonants(token_text);
                }
            }
            // a final attempt to split by punctuation
            else if (punctuationPattern.matcher(token_text).find() && token_text.length() > 1) {
                puncSplit = true;
                String[] puncTokens = token_text.split("((?<=\\p{Punct})|(?=\\p{Punct}))");
                expanded_text = Arrays.toString(puncTokens).replaceAll("[,\\]\\[]", "");

            }
            // Double quotes
            else if (token_text.equals("\"")) {
                // FIXME: skip quotes for now as we don't have any clever
                // management of the POS for the prosodic feature
            } else if (token_text.matches(punctuationPattern.pattern())) {
                t.setPOS(".");
            }

            // if token isn't ignored but there is no handling rule don't add
            // MTU
            if (!orig_text.equals(expanded_text)) {
                String sounds_like = expanded_text.replaceAll("-", " ");
                t.soundsLike(sounds_like);

                // // if expanded url or punctuation go over each node,
                // otherwise let TreeWalker catch u
                // if (!isURL && !puncSplit && !dashSplit)
                // {
                // idx_token += new_tokens.length - 2;
                // }
                // else
                // {
                // // if the first node in doc is an email or web address,
                // account for this
                // if (idx_token == 0)
                // URLFirst = true;
                // puncSplit = false;
                // dashSplit = false;
                // }
            }
        }
    }

    /**************************************************************************************************************************************************************
     ** Expansion rules
     **************************************************************************************************************************************************************/
    protected String expandNumber(double number) {
        this.rbnf.setDefaultRuleSet(cardinalRule);
        return this.rbnf.format(number);
    }

    protected String expandOrdinal(double number) {
        this.rbnf.setDefaultRuleSet(ordinalRule);
        return this.rbnf.format(number);
    }

    protected String expandYear(double number) {
        this.rbnf.setDefaultRuleSet(yearRule);
        return this.rbnf.format(number);
    }

    protected String expandDuration(String duration) {
        Matcher durMatcher = durationPattern.matcher(duration);
        durMatcher.find();
        String hrs = expandNumber(Double.parseDouble(durMatcher.group(1))) + " hours ";
        String mins = expandNumber(Double.parseDouble(durMatcher.group(2))) + " minutes ";
        String secs = expandNumber(Double.parseDouble(durMatcher.group(3))) + " seconds ";
        String ms = "";
        if (durMatcher.group(4) != null) {
            ms = "and " + expandNumber(Double.parseDouble(durMatcher.group(5))) + " milliseconds ";
        } else {
            secs = "and " + secs;
        }
        return hrs + mins + secs + ms;
    }

    protected String expandAcronym(String acronym) {
        return acronym.replaceAll("\\.", " ");
    }

    /***
     * expand a URL string partially by splitting by @, / and . symbols (but
     * retaining them)
     *
     * @param email
     *            email
     * @return Arrays.toString(tokens).replaceAll("[,\\]\\[]", "")
     */
    protected String expandURL(String email) {
        String[] tokens = email.split("((?<=[\\.@\\/])|(?=[\\.@\\/]))");
        return Arrays.toString(tokens).replaceAll("[,\\]\\[]", "");
    }

    protected String expandYearBCAD(String year) {
        String abbrev = "";
        Matcher yearMatcher = yearPattern.matcher(year);
        yearMatcher.find();
        if (yearMatcher.group(2).contains(".")) {
            String[] abbrevAr = yearMatcher.group(2).split("\\.");
            abbrev = Arrays.toString(abbrevAr).replaceAll("[,\\]\\[]", "");
        } else {
            abbrev = expandConsonants(yearMatcher.group(2));

        }
        return expandYear(Double.parseDouble(yearMatcher.group(1))) + " " + abbrev;
    }

    /***
     * add a space between each char of a string
     *
     * @param consonants
     *            consonants
     * @return Joiner.on(" ").join(Lists.charactersOf(consonants))
     */
    protected String expandConsonants(String consonants) {
        return Joiner.on(" ").join(Lists.charactersOf(consonants));
    }

    protected String expandHashtag(String hashtag) {
        String tag = "";
        String expandedTag = "";
        Matcher hashTagMatcher = hashtagPattern.matcher(hashtag);
        hashTagMatcher.find();
        tag = hashTagMatcher.group(2);
        if (!tag.matches("[a-z]+") || !tag.matches("[A-Z]+")) {
            String temp = "";
            for (char c : tag.toCharArray()) {
                if (Character.isDigit(c) && temp.matches("^$|[0-9]+")) {
                    temp += c;
                } else if (Character.isDigit(c) && temp.matches(".+[0-9]")) {
                    temp += c;
                } else if (Character.isDigit(c)) {
                    temp += " " + c;
                } else if (!temp.equals("") && Character.isUpperCase(c)) {
                    if (Character.isUpperCase(temp.charAt(temp.length() - 1))) {
                        temp += c;
                    } else {
                        temp += " " + c;
                    }
                } else if (Character.isAlphabetic(c) && temp.length() > 0) {
                    if (Character.isDigit(temp.charAt(temp.length() - 1))) {
                        temp += " " + c;
                    } else {
                        temp += c;
                    }
                } else {
                    temp += c;
                }
            }
            expandedTag = temp;
        } else {
            expandedTag = tag;
        }
        return symbols.get(hashTagMatcher.group(1)) + " " + expandedTag;
    }

    protected String expandRange(String range) {
        Matcher rangeMatcher = rangePattern.matcher(range);
        rangeMatcher.find();
        return expandNumber(Double.parseDouble(rangeMatcher.group(1))) + " to "
               + expandNumber(Double.parseDouble(rangeMatcher.group(2)));
    }

    /***
     * expands a digit followed by an s. e.g. 7s and 8s and the 60s
     *
     * @param numberS
     *            numberS
     * @return number
     */
    protected String expandNumberS(String numberS) {
        Matcher numberSMatcher = numberSPattern.matcher(numberS);
        numberSMatcher.find();
        String number = expandNumber(Double.parseDouble(numberSMatcher.group(1)));
        if (number.endsWith("x")) {
            number += "es";
        } else if (number.endsWith("y")) {
            number = number.replace("y", "ies");
        } else {
            number += "s";
        }
        return number;
    }

    protected String splitContraction(String contraction) {
        int aposIndex = contraction.indexOf("'");
        String lemma = contraction.substring(0, aposIndex);
        String end = contraction.substring(aposIndex);
        return lemma + " " + end;
    }

    /***
     *
     * @param abbrev
     *            the token to be expanded
     * @param isCapital
     *            whether the following token begins with a capital letter
     * @return abbrev
     */
    protected String expandAbbreviation(String abbrev, boolean isCapital) {
        String expAbb = abbrev.replaceAll("\\.", "").toLowerCase();
        if (!abbrevMap.containsKey(expAbb)) {
            logger.warn(String.format("Could not expand unknown abbreviation \"%s\", ignoring", abbrev));
            return abbrev;
        }
        expAbb = (String) this.abbrevMap.get(expAbb);
        String[] multiExp = expAbb.split(",");
        if (multiExp.length > 1) {
            if (isCapital) {
                expAbb = multiExp[0];
            } else {
                expAbb = multiExp[1];
            }
        }
        return expAbb;
    }

    protected String expandDate(String date) throws ParseException {
        // date format is "month/day/year"
        Date humanDate = df.getPatternInstance("MM.dd.yyyy", ULocale.ENGLISH).parse(date);
        String[] dateParts = df.format(humanDate).replaceAll(",", "").split("\\s");
        dateParts[1] = expandOrdinal(Double.parseDouble(dateParts[1]));
        dateParts[2] = expandYear(Double.parseDouble(dateParts[2]));
        return Arrays.toString(dateParts).replaceAll("[,\\]\\[]", "");
    }

    /***
     *
     * @param time
     *            the token to be expanded
     * @param isNextTokenTime
     *            whether the following token contains am or pm
     * @return theTime
     */
    protected String expandTime(String time, boolean isNextTokenTime) {
        boolean pastNoon = false;
        String theTime = "";
        String hour = "";
        Double pmHour;
        Matcher timeMatch = timePattern.matcher(time);
        timeMatch.find();
        // hour
        if (timeMatch.group(2) != null || timeMatch.group(3) != null) {
            hour = (timeMatch.group(2) != null) ? timeMatch.group(2) : timeMatch.group(3);
            if (hour.equals("00")) {
                hour = "12";
            }
            theTime += expandNumber(Double.parseDouble(hour));
        } else {
            pastNoon = true;
            hour = (timeMatch.group(4) != null) ? timeMatch.group(4) : timeMatch.group(5);
            pmHour = Double.parseDouble(hour) - 12;
            if (pmHour == 0) {
                hour = "12";
                theTime += expandNumber(Double.parseDouble(hour));
            } else {
                theTime += expandNumber(pmHour);
            }
        }
        // minutes
        if (timeMatch.group(7) != null && !isNextTokenTime) {
            if (!timeMatch.group(6).equals("00")) {
                if (timeMatch.group(6).matches("0\\d")) {
                    theTime += " oh " + expandNumber(Double.parseDouble(timeMatch.group(6)));
                } else {
                    theTime += " " + expandNumber(Double.parseDouble(timeMatch.group(6)));
                }
            }
            for (char c : timeMatch.group(7).replaceAll("\\.", "").toCharArray()) {
                theTime += " " + c;
            }
        } else if (!isNextTokenTime) {
            if (!timeMatch.group(6).equals("00")) {
                if (timeMatch.group(6).matches("0\\d")) {
                    theTime += " oh " + expandNumber(Double.parseDouble(timeMatch.group(6)));
                } else {
                    theTime += " " + expandNumber(Double.parseDouble(timeMatch.group(6)));
                }
            }
            theTime += !pastNoon ? " a m" : " p m";
        } else {
            if (!timeMatch.group(6).equals("00")) {
                if (timeMatch.group(6).matches("0\\d")) {
                    theTime += " oh " + expandNumber(Double.parseDouble(timeMatch.group(6)));
                } else {
                    theTime += " " + expandNumber(Double.parseDouble(timeMatch.group(6)));
                }
            }
        }
        return theTime;
    }

    protected String expandRealNumber(String number) {
        Matcher realNumMatch = realNumPattern.matcher(number);
        realNumMatch.find();
        String newTok = "";
        if (realNumMatch.group(1) != null) {
            newTok += "minus ";
        }
        if (realNumMatch.group(2) != null) {
            newTok += expandNumber(Double.parseDouble(realNumMatch.group(2))) + " ";
        }
        if (realNumMatch.group(3) != null) {
            newTok += "point ";
            for (char c : realNumMatch.group(4).toCharArray()) {
                newTok += expandNumber(Double.parseDouble(String.valueOf(c))) + " ";
            }
            if (realNumMatch.group(5) != null) {
                newTok += "per cent";
            }
        }
        return newTok.trim();
    }

    protected String expandWordNumber(String wordnumseq) {
        String[] groups = wordnumseq.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        int i = 0;
        for (String g : groups) {
            if (g.matches("\\d+")) {
                String newTok = "";
                for (char c : g.toCharArray()) {
                    newTok += expandNumber(Double.parseDouble(String.valueOf(c))) + " ";
                }
                groups[i] = newTok;
            }
            i++;
        }
        return Arrays.toString(groups).replaceAll("[,\\]\\[]", "");
    }

    protected String expandMoney(String money, String currency) {
        String origText = money;
        Matcher currencyMatch = moneyPattern.matcher(money);
        currencyMatch.find();
        switch (currency) {
        case "$" :
            if (Double.parseDouble(currencyMatch.group(1)) > 1) {
                money = expandNumber(Double.parseDouble(currencyMatch.group(1))) + " dollars";
            } else {
                money = expandNumber(Double.parseDouble(currencyMatch.group(1))) + " dollar";
            }
            if (currencyMatch.group(2) != null) {
                int dotIndex = origText.indexOf('.');
                money = money + " " + expandNumber(Double.parseDouble(origText.substring(dotIndex + 1))) + " cents";
            }
            break;
        case "£" :
            money = expandNumber(Double.parseDouble(currencyMatch.group(1))) + " pound sterling";
            if (currencyMatch.group(2) != null) {
                int dotIndex = origText.indexOf('.');
                money = money + " " + expandNumber(Double.parseDouble(origText.substring(dotIndex + 1))) + " pence";
            }
            break;
        case "€" :
            money = expandNumber(Double.parseDouble(currencyMatch.group(1))) + " euro";
            if (currencyMatch.group(2) != null) {
                int dotIndex = origText.indexOf('.');
                money = money + " " + expandNumber(Double.parseDouble(origText.substring(dotIndex + 1))) + " cents";
            }
            break;
        default :
            logger.warn(String.format("Could not expand amount [%s] for currency [%s]", origText, currency));
            break;
        }
        return money;
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
            "The locale " + rbnf.getLocale(ULocale.ACTUAL_LOCALE) + " doesn't support ordinal spelling.");
    }

    /**
     * Try to extract the rule name for "expand year" from the given
     * RuleBasedNumberFormat.
     * <p>
     * The rule name is locale sensitive, but usually starts with
     * "%spellout-numbering-year".
     *
     * @param rbnf
     *            The RuleBasedNumberFormat from where we will try to extract
     *            the rule name.
     * @return The rule name for "year spell out".
     */
    protected static String getYearRuleName(final RuleBasedNumberFormat rbnf) {
        List<String> l = Arrays.asList(rbnf.getRuleSetNames());
        if (l.contains("%spellout-numbering-year")) {
            return "%spellout-numbering-year";
        } else {
            for (String string : l) {
                if (string.startsWith("%spellout-numbering-year")) {
                    return string;
                }
            }
        }
        throw new UnsupportedOperationException(
            "The locale " + rbnf.getLocale(ULocale.ACTUAL_LOCALE) + " doesn't support year spelling.");
    }

    public static Map<Object, Object> loadAbbrevMap() throws IOException {
        Map<Object, Object> abbMap = new Properties();
        ((Properties) abbMap).load(Preprocess.class.getResourceAsStream("preprocess/abbrev.dat"));
        return abbMap;
    }
}
