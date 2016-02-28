/**
 * Copyright 2002 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package marytts.language.de.preprocess;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import marytts.datatypes.MaryXML;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An expansion pattern implementation for abbreviation patterns.
 *
 * @author Marc Schr&ouml;der
 */

public class AbbrevEP extends ExpansionPattern {
	private final String[] _knownTypes = { "acronym" };
	private final List<String> knownTypes = Arrays.asList(_knownTypes);

	public List<String> knownTypes() {
		return knownTypes;
	}

	private static final Map<String, String[]> abbrevDict = new HashMap<String, String[]>();

	// We don't use sMatchingChars here, but override isCandidate().
	private final Pattern reMatchingChars = null;

	public Pattern reMatchingChars() {
		return reMatchingChars;
	}

	private static final Logger logger = MaryUtils.getLogger("AbbrevEP");

	static {
		try {
			loadAbbrevDict();
		} catch (FileNotFoundException e) {
			logger.warn("Could not load abbreviation file", e);
		} catch (IOException e) {
			logger.warn("Could not load abbreviation file", e);
		}

	}

	public AbbrevEP() {
		super();
	}

	@Override
	protected boolean isCandidate(Element t) {
		String str = MaryDomUtils.tokenText(t);
		return isAbbrev(str) || REPattern.onlyDigits.matcher(str).find() || ".".equals(str);
	}

	protected int canDealWith(String s, int type) {
		return match(s, type);
	}

	protected int match(String s, int type) {
		if (s.length() > 1 && isAbbrev(s))
			return type;
		return -1;
	}

	protected boolean isAbbrev(String s) {
		boolean isLetterDot = REPattern.letterDot.matcher(s).find();
		boolean isNonInitialCapital = REPattern.nonInitialCapital.matcher(s).find();
		boolean isOnlyConsonants = REPattern.onlyConsonants.matcher(s).find();
		boolean isInDict = abbrevDict.containsKey(s) || abbrevDict.containsKey(s + ".");
		return isLetterDot || isNonInitialCapital || isOnlyConsonants || isInDict;
	}

	/**
	 * Expand abbreviations and eventually replace them by <code>mtu</code> structures (for multi-token abbreviations).
	 * 
	 * @param tokens
	 *            tokens
	 * @param s
	 *            s
	 * @param type
	 *            type
	 * @return expanded
	 */
	protected List<Element> expand(List<Element> tokens, String s, int type) {
		if (tokens == null)
			throw new NullPointerException("Received null argument");
		if (tokens.isEmpty())
			throw new IllegalArgumentException("Received empty list");
		// we expect type to be one of the return values of match():
		List<Element> expanded = null;
		expanded = expandAbbrev(tokens);
		replaceTokens(tokens, expanded);
		return expanded;
	}

	/**
	 * Expand the abbreviation list <code>abbr</code>. First, try to find longest entries in database, then shorter. If no entry
	 * was found, expand by rule. Return the list of newly created, but not yet attached tokens.
	 * 
	 * @param abbrTokens
	 *            abbrTokens
	 * @return exp
	 */
	private List<Element> expandAbbrev(List<Element> abbrTokens) {
		ArrayList<Element> exp = new ArrayList<Element>();
		ArrayList<Element> abbr = new ArrayList<Element>(abbrTokens);
		ArrayList<Element> match = new ArrayList<Element>(abbr);
		boolean tryLowerCase = false;
		if (MaryDomUtils.isFirstOfItsKindIn((Element) abbr.get(0), MaryXML.SENTENCE)
				&& REPattern.initialCapitalLetter.matcher(MaryDomUtils.tokenText((Element) abbr.get(0))).find()) {
			// At sentence start, maybe need to lowercase first char
			// before matching
			tryLowerCase = true;
		}
		StringBuilder sb = new StringBuilder();
		while (!match.isEmpty()) {
			sb.setLength(0);
			Iterator<Element> it = match.iterator();
			while (it.hasNext()) {
				sb.append(MaryDomUtils.tokenText((Element) it.next()));
			}
			logger.debug("Looking up abbreviation in dictionary: `" + sb.toString() + "'");
			if (abbrevDict.containsKey(sb.toString())) {
				break; // OK, found a match
			}
			if (tryLowerCase) {
				sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
				logger.debug("Looking up abbreviation in dictionary: `" + sb.toString() + "'");
				if (abbrevDict.containsKey(sb.toString()))
					break; // OK, found a match
			}
			// Try to append a dot:
			sb.append(".");
			logger.debug("Looking up abbreviation in dictionary: `" + sb.toString() + "'");
			if (abbrevDict.containsKey(sb.toString())) {
				break; // OK, found a match
			}
			match.remove(match.size() - 1); // remove last in list
		}
		if (!match.isEmpty()) { // found an abbrevDict entry
			exp.addAll(dictionaryExpandAbbrev(match, sb.toString()));
			abbr.removeAll(match);
			logger.debug("Have found abbreviation in dictionary: `" + sb.toString() + "'");
		} else { // no abbrevDict entry - expand one token by rule
			Element token = (Element) abbr.get(0);
			// Verify that token consists of more than a single character:
			String text = MaryDomUtils.tokenText(token);
			// Only digits? Pronounce as an integer.
			if (REPattern.onlyDigits.matcher(text).find()) {
				if (Pattern.matches(NumberEP.sInteger, text)) {
					logger.debug("Expanding as integer: `" + text + "'");
					exp.addAll(makeNewTokens(token.getOwnerDocument(), number.expandInteger(text)));
				} else {
					logger.debug("Expanding as digits: `" + text + "'");
					exp.addAll(makeNewTokens(token.getOwnerDocument(), number.expandDigits(text)));
				}

			} else if (text.length() > 1) {
				logger.debug("Expanding one token by rule: `" + text + "'");
				// Slow down the mtu containing this token (or the token
				// itself), so the spelling is understandable.
				slowDown(token);
				// And now expand:
				exp.addAll(ruleExpandAbbrev(token));
			} else { // only single character
				// Need to copy the character into a new XML document,
				// otherwise replaceTokens will kill it.
				exp.addAll(makeNewTokens(token.getOwnerDocument(), text));
			}
			abbr.remove(0);
		}
		if (!abbr.isEmpty())
			exp.addAll(expandAbbrev(abbr));
		if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
			StringBuilder logBuf = new StringBuilder();
			for (Iterator<Element> it = exp.iterator(); it.hasNext();) {
				Element elt = (Element) it.next();
				if (elt.getTagName().equals(MaryXML.TOKEN)) {
					logBuf.append(MaryDomUtils.tokenText(elt));
				} else {
					logBuf.append(elt.getTagName());
				}
				logBuf.append(" ");
			}
			logger.debug("Expanded abbreviation: " + logBuf.toString());
		}
		return exp;
	}

	/**
	 * Expand a recognised abbreviation from the dictionary. <code>match</code> is the list of token elements forming the
	 * abbreviation; <code>abbrev</code> is a string representation of that abbreviation. Tokens for the expanded form are
	 * created, but not yet attached to the dom tree.
	 * 
	 * @param match
	 *            match
	 * @param abbrev
	 *            abbrev
	 * @return exp
	 */
	private List<Element> dictionaryExpandAbbrev(List<Element> match, String abbrev) {
		Document doc = ((Element) match.get(0)).getOwnerDocument();
		ArrayList<Element> exp = new ArrayList<Element>();
		String[] value = (String[]) abbrevDict.get(abbrev);
		String flex = value[0]; // inflection info
		String graph = value[1]; // expanded form, possibly with pronunciation
		// For Sentence-initial abbreviation, make sure the expanded
		// form starts with a capital letter.
		if (MaryDomUtils.isFirstOfItsKindIn((Element) match.get(0), "div")
				&& REPattern.initialLowercaseLetter.matcher(graph).find()) {
			StringBuilder sb = new StringBuilder(graph);
			sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
			graph = sb.toString();
			// And while we're at it, correct abbrev because we need it
			// for the mtu tag later.
			sb.setLength(0);
			sb.append(abbrev);
			sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
			abbrev = sb.toString();
		}
		exp.addAll(makeNewTokens(doc, graph, true, abbrev));
		if (exp.isEmpty())
			return exp;
		// Now some post-hoc modification of the first expanded token:
		if (flex != null && flex.length() > 0) {
			Element t = (Element) exp.get(0);
			while (t != null && !t.getTagName().equals(MaryXML.TOKEN))
				t = MaryDomUtils.getFirstChildElement(t);
			if (t != null) {
				String firstWord = MaryDomUtils.tokenText(t);
				// First expanded word gets the inflection info
				// (usually only used in one-word expansions).
				t.setAttribute("ending", flex);
				// If we have an inflexion ending, keep the abbreviated form
				// as the graphemic form (for pos-tagger and chunker), and
				// save the expanded form in the `sounds_like' attribute.
				t.setAttribute("sounds_like", firstWord);
				MaryDomUtils.setTokenText(t, abbrev);
			}
		}
		return exp;
	}

	protected List<Element> ruleExpandAbbrev(Element token) {
		Document doc = token.getOwnerDocument();
		String orig = MaryDomUtils.tokenText(token);
		String expandedString = ruleExpandAbbrev(orig, false); // do not say specialChar
		// Force an accent on every item in this mtu,
		// just to make sure the token in the mtu whose accent
		// is to be retained actually *has* an accent.
		return makeNewTokens(doc, expandedString, true, orig, true);
	}

	protected String ruleExpandAbbrev(String orig, boolean saySpecialChar) {
		// Spell out if <= 5 letters:
		if (orig.indexOf('.') == -1 && // not dot and
				orig.length() <= 5 || // maximally five letters or
				orig.indexOf('.') == orig.length() - 1 && // final dot and
				orig.length() <= 6) { // maximally five letters + dot
			return spellOutAbbrev(orig, saySpecialChar);
		}
		// Contains dot or other specialChar:
		else if (specialChar.reMatchingChars().matcher(orig).find()) {
			StringBuilder sb = new StringBuilder();
			StringTokenizer st = new StringTokenizer(orig, specialChar.sMatchingCharsSimpleString, saySpecialChar);
			// If saySpecialChar is true, st will return specialChar signs
			// as individual tokens of length one.
			while (st.hasMoreTokens()) {
				// recursive call:
				sb.append(ruleExpandAbbrev(st.nextToken(), saySpecialChar));
				sb.append(" ");
			}
			return sb.toString().trim();
		} else {
			// Else, no dot and too long for spelling - print as is
			return orig;
		}
	}

	/**
	 * Spell out a token. If saySpecialChar is true, specialChar is pronounced as well; otherwise, it is silently ignored. The
	 * spelled out version is returned as a String in which individual tokens are separated by a space character.
	 * 
	 * @param s
	 *            s
	 * @param saySpecialChar
	 *            saySpecialChar
	 * @return sb.toString().trim()
	 */
	private String spellOutAbbrev(String s, boolean saySpecialChar) {
		if (s == null || s.length() == 0) // nothing to do
			return s;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			if (specialChar.matchSpecialChar(s.substring(i, i + 1))) {
				// A specialChar character
				if (saySpecialChar) {
					sb.append(specialChar.expandSpecialChar(s.substring(i, i + 1)));
					sb.append(" ");
				}
			} else { // a normal letter
				if ((i + 2 == s.length() || i + 2 < s.length() && specialChar.matchSpecialChar(s.substring(i + 2, i + 3)))
						&& Character.isUpperCase(s.charAt(i)) && s.charAt(i + 1) == 's') {
					// This is the second-to-last character, it is an uppercase
					// letter, and the last one is a lowercase 's' => treat the
					// s like a plural and attach it to this letter's
					// pronunciation.
					sb.append(s.substring(i, i + 1));
					sb.append("[*s]");
					i++;
				} else {
					sb.append(s.substring(i, i + 1));
					sb.append(" ");
				}
			}
		}
		return sb.toString().trim();
	}

	private static void loadAbbrevDict() throws FileNotFoundException, IOException {
		InputStream abbrevStream = AbbrevEP.class.getResourceAsStream("abbrev.dat");
		BufferedReader br = new BufferedReader(new InputStreamReader(abbrevStream, "UTF-8"));
		String line;
		while ((line = br.readLine()) != null) {
			if (Pattern.compile("^\\#").matcher(line).find() || REPattern.emptyLine.matcher(line).find()) {
				// comment or empty line, ignore
				continue;
			}
			// Fields separated by a slash (/):
			StringTokenizer st = new StringTokenizer(line, "/");
			// Each line contains three fields,
			// key (the abbreviation),
			// flex (optional inflection information),
			// and graph (the graphemic (and possibly phonemic) expanded form.
			// Remove leading/trailing whitespace from each field.
			String key = st.nextToken().trim();
			String flex = st.nextToken().trim();
			String graph = st.nextToken().trim();
			// In addition, replace all whitespace in graph by a single blank
			graph = graph.replaceAll("\\s+", " ");
			// Now key should not contain any whitespace:
			if (Pattern.compile("\\s").matcher(key).find()) {
				logger.info("In abbrev.dat: Abbreviation \"" + key + "\" contains whitespace. Ignoring.");
				continue;
			}
			// In the hashmap, save a reference to an array containing
			// two elements:
			// 1. The inflection information, if any, and
			// 2. The replacement token(s) as one string.
			String[] value = new String[2];
			value[0] = flex;
			value[1] = graph;
			abbrevDict.put(key, value);
		}
	}

}
