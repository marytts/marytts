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

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.modules.InternalModule;
import marytts.util.MaryRuntimeUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import com.ibm.icu.util.ULocale;
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
 *         <li>year (as a 4 digit number or any number followed by AD/BC variation)
 *         <li>currency
 *         <li>numberandword together
 *         <li>dashes (read each number singly) or (split into two words)
 *         <li>underscores
 *         <li>decimal point, minus symbol (real numbers) also handles &#037;, however Jtokeniser splits &#037; into separate
 *         tokens
 *         <li>time
 *         <li>dates (in format mm/dd/yyyy)
 *         <li>acronyms (only split into single characters, never expanded)
 *         <li>abbreviations (list of known expansions in resource preprocess/abbrev.dat, a properties file separated by
 *         whitespace. If an abbrev has two different expansions then the capitalized version comes first, followed by a comma)
 *         <li>contractions &rarr; first check lexicon, if not then &rarr; split and check if map contains contraction, if not
 *         then just remove apostrophe else &rarr; split before apostrophe into two tokens, use map to manually add ph &rarr; for
 *         's if word ends in c,f,k,p,t then add ph &#061; s otherwise ph &#061; z
 *         <li>ampersand &amp;, "at" &#064; symbol, &rarr; symbols
 *         <li>urls &rarr; note that jtokeniser splits off http[s]?://
 *         <li>number ranges "18-35"
 *         <li>words without vowels &rarr; first check lexicon, if not then separate into single character tokens
 *         <li>#hashtags
 *         <li>single "A/a" character &rarr; if there is no next token or the next token is punctuation or next token
 *         string.length &#061;&#061; 1
 *         <li>should also as a last processing attempt, split by punctuation,symbols,etc. and attempt to process these tokens
 *         separately
 *         <li>durations hours:minutes:seconds(:milliseconds)
 *         <li>numbers followed by an s
 *         <li>punctuation &rarr; add ph attribute to tag to prevent phonemisation
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
	private static final Map<String, String> symbols = new HashMap<>();

	// contractions map
	private static final Map<String, String[]> contractions = new HashMap<>();

	// icu4j stuff
	private final RuleBasedNumberFormat rbnf;
	protected final String cardinalRule;
	protected final String ordinalRule;
	protected final String yearRule;
	private final DateFormat df;

	// Regex matching patterns
	private static final Pattern moneyPattern = Pattern.compile("(\\d+)(\\.\\d+)?");
	private static final Pattern timePattern = Pattern.compile("((0?[0-9])|(1[0-1])|(1[2-9])|(2[0-3])):([0-5][0-9])" +
																																 "(a\\.m\\.|am|pm|p\\.m\\.|a\\.m|p\\.m)?", Pattern.CASE_INSENSITIVE);
	private static final Pattern durationPattern = Pattern.compile("(\\d+):([0-5][0-9]):([0-5][0-9])(:([0-5][0-9]))?");
	private static final Pattern abbrevPattern = Pattern.compile("[a-zA-Z]{2,}\\.");
	private static final Pattern acronymPattern = Pattern.compile("([a-zA-Z]\\.[a-zA-Z](\\.)?)+([a-zA-Z](\\.)?)?");
	private static final Pattern realNumPattern = Pattern.compile("(-)?(\\d+)?(\\.(\\d+)(%)?)?");
	private static final Pattern numberWordPattern = Pattern.compile("([a-zA-Z]+[0-9]+|[0-9]+[a-zA-Z]+)\\w*");
	private static final Pattern datePattern = Pattern.compile("(\\d{2})[/.](\\d{2})[/.]\\d{4}");
	private static final Pattern yearPattern = Pattern.compile("(\\d+)(bc|ad|b\\.c\\.|b\\.c|a\\.d\\.|a\\.d)", Pattern.CASE_INSENSITIVE);
	private static final Pattern contractPattern = Pattern.compile("[a-zA-Z]+('[a-zA-Z]+)");
	private static final Pattern symbolsPattern = Pattern.compile("[@%#/+=&><-]");
	// TODO: URLPattern does not validate using http://xenon.stanford.edu/~xusch/regexp/
	private static final Pattern URLPattern = Pattern.compile("(https?://)?((www\\.)?([-a-zA-Z0-9@:%._\\\\+~#=]{2,256}\\." +
																														"[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\\\+.~#?&/=]*)))");
	private static final Pattern rangePattern = Pattern.compile("([0-9]+)-([0-9]+)");
	private static final Pattern consonantPattern = Pattern.compile("[b-df-hj-np-tv-z]+", Pattern.CASE_INSENSITIVE);
	private static final Pattern punctuationPattern = Pattern.compile("\\p{Punct}");
	private static final Pattern myPunctPattern = Pattern.compile(",\\.:;?'\"");
	private static final Pattern hashtagPattern = Pattern.compile("(#)(\\w+)");
	private static final Pattern ordinalPattern = Pattern.compile("\\d+(st|nd|rd|th)", Pattern.CASE_INSENSITIVE);
	private static final Pattern currencySymbPattern = Pattern.compile("[$£€]");
	private static final Pattern numberSPattern = Pattern.compile("([0-9]+)([sS])");
	private static final Pattern commasInNumberPattern = Pattern.compile("[\\d,.]+");
	private static final Pattern fourDigitsPattern = Pattern.compile("\\d{4}");
	//private static final Pattern amPmPattern = Pattern.compile("a\\.m\\.|AM|PM|am|pm|p\\.m\\.");

	// HashMap initialization
	static {
		contractions.put("'s", new String[] { "z", "s" });
		contractions.put("'ll", new String[] { "l" });
		contractions.put("'ve", new String[] { "v" });
		contractions.put("'d", new String[] { "d" });
		contractions.put("'m", new String[] { "m" });
		contractions.put("'re", new String[] { "r" });
		contractions.put("'t", new String[] {"t"});

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
		super("Preprocess", MaryDataType.TOKENS, MaryDataType.WORDS, Locale.ENGLISH);
		rbnf = new RuleBasedNumberFormat(ULocale.ENGLISH, RuleBasedNumberFormat.SPELLOUT);
		cardinalRule = "%spellout-numbering";
		ordinalRule = getOrdinalRuleName(rbnf);
		yearRule = getYearRuleName(rbnf);
		df = DateFormat.getDateInstance(DateFormat.LONG, ULocale.ENGLISH);
		try {
			abbrevMap = loadAbbrevMap();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public MaryData process(MaryData d) throws Exception {
		Document doc = d.getDocument();
		expand(doc);
		MaryData result = new MaryData(getOutputType(), d.getLocale());
		result.setDocument(doc);
		return result;
	}

	/***
	 * processes a document in mary xml format, from Tokens to Words which can be phonemised.
	 *
	 * @param doc
	 *            doc
	 * @throws ParseException
	 *             parse exception
	 * @throws IOException
	 *             IO Exception
	 * @throws MaryConfigurationException
	 *             mary configuration exception
	 */
	protected void expand(Document doc) throws ParseException, IOException, MaryConfigurationException {
		String whichCurrency = "";
		boolean URLFirst = false;
		boolean isURL = false;
		boolean puncSplit = false;
		boolean dashSplit = false;
		String webEmailTemp = "";
		TreeWalker tw = ((DocumentTraversal) doc).createTreeWalker(doc, NodeFilter.SHOW_ELEMENT,
											new NameNodeFilter(MaryXML.TOKEN), false);
		Element t;
		// loop through each node in dom tree
		while ((t = (Element) tw.nextNode()) != null) {

			/*
			 * PRELIM FOR EACH NODE
			 */

			// to accommodate the first token being a url
			if (URLFirst) {
				t = (Element) tw.previousNode();
				URLFirst = false;
			}
			boolean isYear = true;
			boolean splitContraction = false;

			if (MaryDomUtils.hasAncestor(t, MaryXML.SAYAS) || t.hasAttribute("ph") || t.hasAttribute("sounds_like")) {
				// if token already has any of these attributes then ignore
				continue;
			}

			String tokenText = MaryDomUtils.tokenText(t);

			// remove commas
			if (commasInNumberPattern.matcher(tokenText).matches()) {
				MaryDomUtils.setTokenText(t, tokenText = tokenText.replaceAll(",", ""));
				// presume that a 4 digit number which had commas is not a year
				if (fourDigitsPattern.matcher(tokenText).matches()) {
					isYear = false;
				}
			}
			// isYear extra check
			if (fourDigitsPattern.matcher(tokenText).matches() && !whichCurrency.equals("")) {
				isYear = false;
			}

			// check if currency
			if (currencySymbPattern.matcher(tokenText).matches()) {
				whichCurrency = tokenText;
			}

			/*
			 * ACTUAL PROCESSING
			 */

			// ordinal
			if (ordinalPattern.matcher(tokenText).matches()) {
				String matched = tokenText.split("(?i)st|nd|rd|th")[0];
				MaryDomUtils.setTokenText(t, expandOrdinal(Double.parseDouble(matched)));
				// single a or A character
			} else if (tokenText.matches("[aA]")) {
				Element checkNextNode = MaryDomUtils.getNextSiblingElement(t);
				if (checkNextNode == null || myPunctPattern.matcher(MaryDomUtils.tokenText(checkNextNode)).matches()
						|| MaryDomUtils.tokenText(checkNextNode).length() == 1) {
					MaryDomUtils.setTokenText(t, "_a");
				}
				// date
			} else if (datePattern.matcher(tokenText).matches()) {
				MaryDomUtils.setTokenText(t, expandDate(tokenText));
				// number followed by s
			} else if (numberSPattern.matcher(tokenText).matches()) {
				MaryDomUtils.setTokenText(t, expandNumberS(tokenText));
				// year with bc or ad
			} else if (yearPattern.matcher(tokenText).matches()) {
				MaryDomUtils.setTokenText(t, expandYearBCAD(tokenText));
				// year as just 4 digits &rarr; this should always be checked BEFORE real number
			} else if (fourDigitsPattern.matcher(tokenText).matches() && isYear) {
				MaryDomUtils.setTokenText(t, expandYear(Double.parseDouble(tokenText)));
				// wordAndNumber &rarr; must come AFTER year
			} else if (numberWordPattern.matcher(tokenText).matches()) {
				MaryDomUtils.setTokenText(t, expandWordNumber(tokenText));
				// real number & currency
			} else if (realNumPattern.matcher(tokenText).matches()) {
				if (!whichCurrency.equals("")) {
					MaryDomUtils.setTokenText(t, expandMoney(tokenText, whichCurrency));
					whichCurrency = "";
				} else {
					MaryDomUtils.setTokenText(t, expandRealNumber(tokenText));
				}
				// contractions
			} else if (contractPattern.matcher(tokenText).matches()) {
				// first check lexicon
				if (MaryRuntimeUtils.checkLexicon("en_US", tokenText).length == 0) {
					Matcher contractionMatch = contractPattern.matcher(tokenText);
					if (contractionMatch.find()) {
						// if no contraction we allow g2p rules to handle
						if (!contractions.containsKey(contractionMatch.group(1))) {
							MaryDomUtils.setTokenText(t, tokenText.replaceAll("'", ""));
						}
					} else {
						throw new IllegalStateException("No match for find()");
					}

					// FIXME: we do not want to have to phonological word => for now we do not split !
					// // if not in lexicon and we have a contraction expansion then split into two tokens
					// else
					// {
					// splitContraction = true;
					// MaryDomUtils.setTokenText(t, splitContraction(tokenText));
					// }
				}
				// acronym
			} else if (acronymPattern.matcher(tokenText).matches()) {
				MaryDomUtils.setTokenText(t, expandAcronym(tokenText));
				// abbreviation
			} else if ((abbrevPattern.matcher(tokenText).matches() ||
					       abbrevMap.containsKey(tokenText.toLowerCase())) && !isURL) {
				Element testAbbNode = MaryDomUtils.getNextSiblingElement(t);
				boolean nextTokenIsCapital = false;
				if (testAbbNode != null && Character.isUpperCase(MaryDomUtils.tokenText(testAbbNode).charAt(0))) {
					nextTokenIsCapital = true;
				}
				MaryDomUtils.setTokenText(t, expandAbbreviation(tokenText, nextTokenIsCapital));
				// time
			} else if (timePattern.matcher(tokenText).matches()) {
				MaryDomUtils.setTokenText(t, expandTime(tokenText));
				// duration
			} else if (durationPattern.matcher(tokenText).matches()) {
				MaryDomUtils.setTokenText(t, expandDuration(tokenText));
				// hashtags
			} else if (hashtagPattern.matcher(tokenText).matches()) {
				MaryDomUtils.setTokenText(t, expandHashtag(tokenText));
				// URLs
			} else if (URLPattern.matcher(tokenText).matches()) {
				// matching group 2 contains the chunk we want
				Matcher urlMatcher = URLPattern.matcher(tokenText);
				if (urlMatcher.find()) {
					webEmailTemp = tokenText;
					isURL = true;
					MaryDomUtils.setTokenText(t, expandURL(urlMatcher.group(2)));
				} else {
					throw new IllegalStateException("No match for find()");
				}
				// dot . for web and email addresses
			} else if (tokenText.equals(".") && isURL) {
				MaryDomUtils.setTokenText(t, "dot");
				webEmailTemp = webEmailTemp.replaceFirst("\\.", "dot");
				if (!webEmailTemp.contains(".")) {
					isURL = false;
				}
				// symbols
			} else if (symbolsPattern.matcher(tokenText).matches()) {
				MaryDomUtils.setTokenText(t, symbols.get(tokenText));
				// number ranges &rarr; before checking for dashes
			} else if (rangePattern.matcher(tokenText).matches()) {
				MaryDomUtils.setTokenText(t, expandRange(tokenText));
				// dashes and underscores
			} else if (tokenText.contains("-") || tokenText.contains("_")) {
				dashSplit = true;
				String[] tokens = tokenText.split("[-_]");
				int i = 0;
				for (String tok : tokens) {
					if (tok.matches("\\d+")) {
						StringBuilder newTok = new StringBuilder();
						for (char c : tok.toCharArray()) {
							newTok.append(expandNumber(Double.parseDouble(String.valueOf(c)))).append(" ");
						}
						tokens[i] = newTok.toString();
					}
					i++;
				}
				MaryDomUtils.setTokenText(t, Arrays.toString(tokens).replaceAll("[,\\]\\[]", ""));
				// words containing only consonants
			} else if (consonantPattern.matcher(tokenText).matches()) {
				// first check lexicon
				if (MaryRuntimeUtils.checkLexicon("en_US", tokenText).length == 0) {
					MaryDomUtils.setTokenText(t, expandConsonants(tokenText));
				}
				// a final attempt to split by punctuation
			} else if (punctuationPattern.matcher(tokenText).find() && tokenText.length() > 1) {
				puncSplit = true;
				String[] puncTokens = tokenText.split("((?<=\\p{Punct})|(?=\\p{Punct}))");
				MaryDomUtils.setTokenText(t, Arrays.toString(puncTokens).replaceAll("[,\\]\\[]", ""));
			} else if (tokenText.equals("\"")) {
				// FIXME: skip quotes for now as we don't have any clever management of the POS for the prosodic feature
			} else if (punctuationPattern.matcher(tokenText).matches()) {
				t.setAttribute("pos", ".");
			}
			// if token isn't ignored but there is no handling rule don't add MTU
			if (!tokenText.equals(MaryDomUtils.tokenText(t))) {
				MaryDomUtils.encloseWithMTU(t, tokenText, null);
				// finally, split new expanded token separated by spaces into separate tokens (also catch any leftover dashes)
				String[] newTokens = MaryDomUtils.tokenText(t).replaceAll("-", " ").split("\\s+");
				MaryDomUtils.setTokenText(t, newTokens[0]);
				for (int i = 1; i < newTokens.length; i++) {
					assert t != null;
					MaryDomUtils.appendToken(t, newTokens[i]);
					t = MaryDomUtils.getNextSiblingElement(t);
					// if tokens are an expanded contraction
					// TODO: the followng expression is always false...
					if (splitContraction && newTokens.length == 2) {
						if (newTokens[0].substring(newTokens[0].length() - 1).matches("[cfkpt]")
								&& contractions.get(newTokens[i]).length > 1) {
							t.setAttribute("ph", contractions.get(newTokens[i])[1]);
						} else {
							t.setAttribute("ph", contractions.get(newTokens[i])[0]);
						}
					}
				}
				// if expanded url or punctuation go over each node, otherwise let TreeWalker catch up
				if (!isURL && !puncSplit && !dashSplit) {
					tw.setCurrentNode(t);
				} else {
					Node n = tw.previousNode();
					// if the first node in doc is an email or web address, account for this
					if (n == null) {
						URLFirst = true;
					}
					puncSplit = false;
					dashSplit = false;
				}
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

	protected String expandYear(double number) {
		this.rbnf.setDefaultRuleSet(yearRule);
		return this.rbnf.format(number);
	}

	protected String expandDuration(String duration) {
		Matcher durMatcher = durationPattern.matcher(duration);
		if (durMatcher.find()) {
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
		throw new IllegalStateException("No match for find()");
	}

	protected String expandAcronym(String acronym) {
		return acronym.replaceAll("\\.", " ");
	}

	/***
	 * expand a URL string partially by splitting by @, / and . symbols (but retaining them)
	 *
	 * @param email
	 *            email
	 * @return Arrays.toString(tokens).replaceAll("[,\\]\\[]", "")
	 */
	protected String expandURL(String email) {
		String[] tokens = email.split("((?<=[.@/])|(?=[.@/]))");
		return Arrays.toString(tokens).replaceAll("[,\\]\\[]", "");
	}

	protected String expandYearBCAD(String year) {
		Matcher yearMatcher = yearPattern.matcher(year);
		if (yearMatcher.find()) {
			String abbrev;
			if (yearMatcher.group(2).contains(".")) {
				String[] abbrevAr = yearMatcher.group(2).split("\\.");
				abbrev = Arrays.toString(abbrevAr).replaceAll("[,\\]\\[]", "");
			} else {
				abbrev = expandConsonants(yearMatcher.group(2));
			}
			return expandYear(Double.parseDouble(yearMatcher.group(1))) + " " + abbrev;
		}
		throw new IllegalStateException("No match for find()");
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

	// expand matches to (#)(\w+)
	protected String expandHashtag(String hashtag) {
		Matcher hashTagMatcher = hashtagPattern.matcher(hashtag);
		if (hashTagMatcher.find()) {
			StringBuilder temp = new StringBuilder("hashtag ");
			String tag = hashTagMatcher.group(2);
			if (!tag.matches("[a-z]+") || !tag.matches("[A-Z]+")) {
				char lastChar = ' ';
				for (int ii = 0; ii < tag.length(); ii++) {
					char cc = tag.charAt(ii);
					if (Character.isDigit(cc)) {
						if (lastChar != ' ') {
							temp.append(' ');
						}
						int number = 0;
						while (ii < tag.length() && Character.isDigit(cc = tag.charAt(ii))) {
							number *= 10;
							number += Integer.parseInt(Character.toString(lastChar = cc));
							ii++;
						}
						ii--;
						temp.append(expandNumber(number));
					} else {
						if (Character.isUpperCase(cc) || Character.isDigit(lastChar)) {
							temp.append(' ');
						}
						temp.append(lastChar = cc);
					}
				}
			} else {
				temp.append(tag);
			}
			return temp.toString();
		}
		throw new IllegalStateException("No match for find()");
	}

	protected String expandRange(String range) {
		Matcher rangeMatcher = rangePattern.matcher(range);
		if (rangeMatcher.find()) {
			return expandNumber(Double.parseDouble(rangeMatcher.group(1))) + " to "
					+ expandNumber(Double.parseDouble(rangeMatcher.group(2)));
		}
		throw new IllegalStateException("No match for find()");
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
		if (numberSMatcher.find()) {
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
		throw new IllegalStateException("No match for find()");
	}

	// TODO: this method is never called
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
		expAbb = (String) abbrevMap.get(expAbb);
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

	protected String removeCommas (String numberText) {
		return numberText.replaceAll(",", "");
	}

	protected String expandDate(String date) throws ParseException {
		// Fix date in case format not completely correct
		date = date.replace('.', '/');
		String[] dateParts = date.split("/");
		ULocale locale = Integer.parseInt(dateParts[0]) > 12 ? ULocale.UK : ULocale.US;
		Date humanDate = DateFormat.getPatternInstance("dd.MM.yyyy", locale).parse(date);
		dateParts = df.format(humanDate).replaceAll(",", "").split("\\s");
		dateParts[1] = expandOrdinal(Double.parseDouble(dateParts[1]));
		dateParts[2] = expandYear(Double.parseDouble(dateParts[2]));
		return Arrays.toString(dateParts).replaceAll("[,\\]\\[]", "");
	}

	/***
	 *
	 * @param time
	 *            the token to be expanded
	 * @return theTime
	 */
	protected String expandTime(String time) {
		StringBuilder theTime = new StringBuilder();
		Matcher timeMatch = timePattern.matcher(time);
		if (timeMatch.find()) {
			// expand hour value
			String hour = timeMatch.group(1);
			int hourValue = Integer.parseInt(hour);
			boolean pastNoon = hourValue >= 12;
			if (hourValue > 12) {
				hourValue -= 12;
			} else if (hourValue == 0){
				hourValue = 12;
				pastNoon = true;
			}
			theTime.append(expandNumber(hourValue));
			// expand minutes value
			if (!timeMatch.group(6).equals("00")) {
				if (timeMatch.group(6).matches("0\\d")) {
					theTime.append(" oh ").append(expandNumber(Double.parseDouble(timeMatch.group(6))));
				} else {
					theTime.append(" ").append(expandNumber(Double.parseDouble(timeMatch.group(6))));
				}
			}
			if (timeMatch.group(7) != null) {
				for (char c : timeMatch.group(7).replaceAll("\\.", "").toCharArray()) {
					theTime.append(" ").append(c);
				}
			} else {
				theTime.append(!pastNoon ? " a m" : " p m");
			}
			return theTime.toString();
		}
		throw new IllegalStateException("No match for find()");
	}

	// expand matches to (-)?(\d+)?(\.(\d+)(%)?)?
	protected String expandRealNumber(String number) {
		Matcher realNumMatch = realNumPattern.matcher(number);
		if (realNumMatch.find()) {
			StringBuilder newTok = new StringBuilder();
			if (realNumMatch.group(1) != null) {
				newTok.append("minus ");
			}
			if (realNumMatch.group(2) != null) {
				newTok.append(expandNumber(Double.parseDouble(realNumMatch.group(2)))).append(" ");
			}
			if (realNumMatch.group(3) != null) {
				newTok.append("point ");
				for (char c : realNumMatch.group(4).toCharArray()) {
					newTok.append(expandNumber(Double.parseDouble(String.valueOf(c)))).append(" ");
				}
				if (realNumMatch.group(5) != null) {
					newTok.append("per cent");
				}
			}
			return newTok.toString().trim();
		}
		throw new IllegalStateException("No match for find()");
	}

	protected String expandWordNumber(String wordnumseq) {
		String[] groups = wordnumseq.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
		int i = 0;
		for (String g : groups) {
			if (g.matches("\\d+")) {
				StringBuilder newTok = new StringBuilder();
				for (char c : g.toCharArray()) {
					newTok.append(expandNumber(Double.parseDouble(String.valueOf(c)))).append(" ");
				}
				groups[i] = newTok.toString();
			}
			i++;
		}
		return Arrays.toString(groups).replaceAll("[,\\]\\[]", "");
	}

	protected String expandMoney(String money, String currency) {
		Matcher currencyMatch = moneyPattern.matcher(money);
		if (currencyMatch.find()) {
			StringBuilder buf = new StringBuilder();
			double whole = Double.parseDouble(currencyMatch.group(1));
			String fraction = currencyMatch.group(2);
			double fracValue = fraction != null ? Double.parseDouble(fraction) * 100 : 0;
			switch (currency) {
			case "$":
				buf.append(expandNumber(whole)).append(whole > 1 ? " dollars" : " dollar");
				if (fraction != null) {
					buf.append(" ").append(expandNumber(fracValue)).append(" cents");
				}
				break;
			case "£":
				buf.append(expandNumber(whole)).append(" pound sterling");
				if (fraction != null) {
					buf.append(" ").append(expandNumber(fracValue)).append(" pence");
				}
				break;
			case "€":
				buf.append(expandNumber(whole)).append(" euro");
				if (fraction != null) {
					buf.append(" ").append(expandNumber(fracValue)).append(" cents");
				}
				break;
			default:
				logger.warn(String.format("Could not expand amount [%s] for currency [%s]", money, currency));
				break;
			}
			return buf.toString();
		}
		throw new IllegalStateException("No match for find()");
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
		throw new UnsupportedOperationException("The locale " + rbnf.getLocale(ULocale.ACTUAL_LOCALE)
				+ " doesn't support ordinal spelling.");
	}

	/**
	 * Try to extract the rule name for "expand year" from the given RuleBasedNumberFormat.
	 * <p>
	 * The rule name is locale sensitive, but usually starts with "%spellout-numbering-year".
	 *
	 * @param rbnf
	 *            The RuleBasedNumberFormat from where we will try to extract the rule name.
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
		throw new UnsupportedOperationException("The locale " + rbnf.getLocale(ULocale.ACTUAL_LOCALE)
				+ " doesn't support year spelling.");
	}

	public static Map<Object, Object> loadAbbrevMap() throws IOException {
		Properties abbMap = new Properties();
		abbMap.load(Preprocess.class.getResourceAsStream("preprocess/abbrev.dat"));
		return abbMap;
	}
}
