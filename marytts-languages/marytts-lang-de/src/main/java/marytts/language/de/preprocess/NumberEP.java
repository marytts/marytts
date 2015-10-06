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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import marytts.datatypes.MaryXML;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An expansion pattern implementation for basic number patterns.
 *
 * @author Marc Schr&ouml;der
 */

public class NumberEP extends ExpansionPattern {
	private final String[] _knownTypes = { "number", "number:float", "number:integer", "number:ordinal", "number:roman",
			"number:digits", "number:cardinal" };
	/**
	 * Every subclass has its own list knownTypes, an internal string representation of known types. These are possible values of
	 * the <code>type</code> attribute to the <code>say-as</code> element, as defined in MaryXML.dtd. If there is more than one
	 * known type, the first type (<code>knownTypes[0]</code>) is expected to be the most general one, of which the others are
	 * specializations.
	 */
	private final List<String> knownTypes = Arrays.asList(_knownTypes);

	public List<String> knownTypes() {
		return knownTypes;
	}

	// Domain-specific primitives:
	// (floats and integers are recognised up to hundreds of millions (nine digits))
	protected static final String sFloat = "(?:-?(?:[1-9][0-9]{0,8}|0)?(?:\\.|,)[0-9]+)";
	protected static final String sInteger = "(?:-?[1-9][0-9]{0,8}|0)";
	protected static final String sOrdinal = "(?:" + sInteger + "\\.)";
	protected static final String sRoman = "(?:[MDCLXVI]+\\.?)";
	protected static final String sDigits = "(?:[0-9.,]*[0-9][.,]?)";
	protected static final String sCardinal = sFloat + "|" + sInteger;

	// Now the actual match patterns:
	protected final Pattern reFloat = Pattern.compile(sFloat);
	protected final Pattern reInteger = Pattern.compile(sInteger);
	protected final Pattern reOrdinal = Pattern.compile(sOrdinal);
	protected final Pattern reRoman = Pattern.compile(sRoman);
	protected final Pattern reDigits = Pattern.compile(sDigits);

	// We do not use reMatchingChars here, but override isCandidate
	private final Pattern reMatchingChars = null;

	public Pattern reMatchingChars() {
		return reMatchingChars;
	}

	/**
	 * Simple numbers are expected to be entire tokens. They should not be joined together out of several tokens.
	 * 
	 * @return false
	 */
	protected boolean allowMultipleTokens() {
		return false;
	}

	protected boolean isCandidate(Element t) {
		String s = MaryDomUtils.tokenText(t);
		return reFloat.matcher(s).matches() || reInteger.matcher(s).matches() || reOrdinal.matcher(s).matches()
				|| reRoman.matcher(s).matches() || reDigits.matcher(s).matches();
	}

	/**
	 * Every subclass has its own logger. The important point is that if several threads are accessing the variable at the same
	 * time, the logger needs to be thread-safe or it will produce rubbish.
	 */
	private Logger logger = MaryUtils.getLogger("NumberEP");

	public NumberEP() {
		super();
	}

	protected int match(String s, int type) {
		switch (type) {
		case 0:
			if (matchFloat(s))
				return 1;
			if (matchInteger(s))
				return 2;
			if (matchOrdinal(s))
				return 3;
			// if (matchRoman(s)) return 4;
			// Disable unconditional Roman number recognition.
			// Problem: Even Abbreviations that happen to only use
			// roman digits are pronounced as numbers. Example: LDC
			// !!!! Would have to be replaced by a context dependent
			// pronounciation -- only in specific contexts is a roman number
			// expanded as such.
			// For the moment, roman numbers need to be specifically requested
			// via markup.
			if (matchDigits(s))
				return 5;
			// case"6" Cardinal = Float | Integer
			break;
		case 1:
			if (matchFloat(s))
				return 1;
			break;
		case 2:
			if (matchInteger(s))
				return 2;
			break;
		case 3:
			if (matchOrdinal(s))
				return 3;
			break;
		case 4:
			if (matchRoman(s))
				return 4;
			break;
		case 5:
			if (matchDigits(s))
				return 5;
			break;
		case 6:
			if (matchInteger(s))
				return 2;
			if (matchFloat(s))
				return 1;
			break;
		}
		return -1;
	}

	protected int canDealWith(String input, int typeCode) {
		switch (typeCode) {
		case 0:
			if (matchFloat(input))
				return 1;
			if (matchInteger(input))
				return 2;
			if (matchOrdinal(input))
				return 3;
			if (matchRoman(input))
				return 4;
			if (matchDigits(input))
				return 5;
			break;
		case 1:
			if (matchFloat(input))
				return 1;
			break;
		case 2: // integer
			if (matchInteger(input) || matchRoman(input))
				return 2;
			break;
		case 3: // ordinal
			if (matchOrdinal(input) || matchInteger(input) || matchRoman(input))
				return 3;
			break;
		case 4:
			if (matchRoman(input))
				return 4;
			break;
		case 5:
			if (matchDigits(input))
				return 5;
			break;
		case 6: // cardinal; is either integer or float
			if (matchInteger(input))
				return 2;
			if (matchFloat(input))
				return 1;
			break;
		}
		return -1; // no, cannot deal with it as the given type
	}

	protected List<Element> expand(List<Element> tokens, String s, int type) {
		if (tokens == null)
			throw new NullPointerException("Received null argument");
		if (tokens.isEmpty())
			throw new IllegalArgumentException("Received empty list");
		Document doc = ((Element) tokens.get(0)).getOwnerDocument();
		// we expect type to be one of the return values of match():
		List<Element> expanded = null;
		switch (type) {
		case 1:
			expanded = expandFloat(doc, s, true);
			break;
		case 2:
			expanded = expandInteger(doc, s, true);
			break;
		case 3:
			expanded = expandOrdinal(doc, s, true);
			break;
		case 4:
			expanded = expandRoman(doc, s, true);
			break;
		case 5:
			expanded = expandDigits(doc, s, true);
			break;
		}
		replaceTokens(tokens, expanded);
		return expanded;
	}

	protected boolean matchFloat(String s) {
		return reFloat.matcher(s).matches();
	}

	protected boolean matchInteger(String s) {
		return reInteger.matcher(s).matches();
	}

	protected boolean matchOrdinal(String s) {
		return reOrdinal.matcher(s).matches();
	}

	protected boolean matchRoman(String s) {
		return reRoman.matcher(s).matches();
	}

	protected boolean matchDigits(String s) {
		return reDigits.matcher(s).matches();
	}

	protected List<Element> expandInteger(Document doc, String s, boolean createMtu) {
		long value;
		// In canDealWith(), we have made a commitment to deal with
		// roman numbers to be pronounced as integers.
		if (matchRoman(s)) {
			return expandRoman(doc, s, createMtu, false); // roman integer
		}
		try {
			while (s.length() > 1 && s.startsWith("0"))
				s = s.substring(1);
			value = Long.parseLong(s);
		} catch (NumberFormatException e) {
			logger.info("Cannot convert string \"" + s + "\" to long.");
			throw e;
		}

		return expandInteger(doc, value, createMtu, s);
	}

	protected List<Element> expandInteger(Document doc, long value, boolean createMtu, String orig) {
		String expString = expandInteger(value);
		return makeNewTokens(doc, expString, createMtu, orig);
	}

	protected String expandInteger(String s) {
		long value;
		try {
			while (s.length() > 1 && s.startsWith("0"))
				s = s.substring(1);
			value = Long.decode(s).longValue();
		} catch (NumberFormatException e) {
			logger.info("Cannot convert string \"" + s + "\" to long.");
			throw e;
		}

		return expandInteger(value);
	}

	protected String expandInteger(long value) {
		long millions;
		int thousands;
		int hundreds;
		int tens;
		int rest;
		StringBuilder buf = new StringBuilder();

		// Special treatment for the 0:
		if (value == 0) {
			return (new String("null"));
		}
		if (value < 0) {
			buf.append("Minus ");
		}
		millions = value / 1000000;
		rest = (int) value % 1000000; // the part of value below 1 000 000
		if (millions > 1) {
			buf.append(expandInteger(millions)); // recursive call
			buf.append(" ");
			buf.append("Millionen ");
		} else if (millions == 1) {
			buf.append("eine Million ");
		}
		thousands = rest / 1000;
		rest = rest % 1000;
		if (thousands > 1) {
			buf.append(expandInteger(thousands));
			buf.append(" ");
			buf.append("Tausend ");
		} else if (thousands == 1) {
			buf.append("ein Tausend ");
		}
		hundreds = rest / 100;
		rest = rest % 100;
		if (hundreds > 1) {
			buf.append(expandInteger(hundreds));
			buf.append(" ");
			buf.append("Hundert ");
		} else if (hundreds == 1) {
			buf.append("ein Hundert ");
		}
		if (rest >= 20) {
			tens = rest / 10;
			rest = rest % 10;
			switch (rest) {
			case 1:
				buf.append("einund");
				break;
			case 2:
				buf.append("zweiund");
				break;
			case 3:
				buf.append("dreiund");
				break;
			case 4:
				buf.append("vierund");
				break;
			case 5:
				buf.append("fünfund");
				break;
			case 6:
				buf.append("sechsund");
				break;
			case 7:
				buf.append("siebenund");
				break;
			case 8:
				buf.append("achtund");
				break;
			case 9:
				buf.append("neunund");
				break;
			default: // 0: do nothing
			}
			switch (tens) {
			case 2:
				buf.append("zwanzig ");
				break;
			case 3:
				buf.append("dreißig ");
				break;
			case 4:
				buf.append("vierzig ");
				break;
			case 5:
				buf.append("fünfzig ");
				break;
			case 6:
				buf.append("sechzig ");
				break;
			case 7:
				buf.append("siebzig ");
				break;
			case 8:
				buf.append("achtzig ");
				break;
			case 9:
				buf.append("neunzig ");
				break;
			default: // shouldn't happen
			}
		} else { // rest < 20
			switch (rest) {
			case 1:
				buf.append("eins ");
				break;
			case 2:
				buf.append("zwei ");
				break;
			case 3:
				buf.append("drei ");
				break;
			case 4:
				buf.append("vier ");
				break;
			case 5:
				buf.append("fünf ");
				break;
			case 6:
				buf.append("sechs ");
				break;
			case 7:
				buf.append("sieben ");
				break;
			case 8:
				buf.append("acht ");
				break;
			case 9:
				buf.append("neun ");
				break;
			case 10:
				buf.append("zehn ");
				break;
			case 11:
				buf.append("elf ");
				break;
			case 12:
				buf.append("zwölf ");
				break;
			case 13:
				buf.append("dreizehn ");
				break;
			case 14:
				buf.append("vierzehn ");
				break;
			case 15:
				buf.append("fünfzehn ");
				break;
			case 16:
				buf.append("sechzehn ");
				break;
			case 17:
				buf.append("siebzehn ");
				break;
			case 18:
				buf.append("achtzehn ");
				break;
			case 19:
				buf.append("neunzehn ");
				break;
			default: // shouldn't happen
			}
		}
		return buf.toString().trim();
	}

	/**
	 * This will correctly expand integers as well, although matchFloat() does not match them. This seems to be convenient in
	 * cases where "some number", i.e. integer or float, was matched, and needs to be expanded.
	 * 
	 * @param doc
	 *            doc
	 * @param s
	 *            s
	 * @param createMtu
	 *            createMtu
	 * @return makeNewTokens(doc, expString, createMtu, s)
	 */
	protected List<Element> expandFloat(Document doc, String s, boolean createMtu) {
		String expString = expandFloat(s);
		return makeNewTokens(doc, expString, createMtu, s);
	}

	protected String expandFloat(String number) {
		// String <code>number</code> must contain exactly one ',' or '.'
		long whole = 0; // the integer part of the number
		StringBuilder buf = new StringBuilder();
		int i = 0; // index in <code>number</code>
		while (i < number.length() && Character.isDigit(number.charAt(i))) {
			whole *= 10;
			// presupposing charset where '0' + 1 == '1' etc.
			whole += number.charAt(i) - '0';
			i++;
		}
		// Now, if the komma / dot was string-initial, whole is 0,
		// which will be pronounced also.
		// Say the integer part of the float like an integer:
		buf.append(expandInteger(whole));
		buf.append(" ");
		// Spell out the rest:
		if (i < number.length())
			buf.append(expandDigits(number.substring(i)));
		return buf.toString().trim();
	}

	protected List<Element> expandDigits(Document doc, String s, boolean createMtu) {
		String expString = expandDigits(s);
		return makeNewTokens(doc, expString, createMtu, s);
	}

	protected String expandDigits(String digits) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < digits.length(); i++) {
			switch (digits.charAt(i)) {
			case ',':
				buf.append("Komma ");
				break;
			case '.':
				buf.append("Punkt ");
				break;
			case '0':
				buf.append("null ");
				break;
			case '1':
				buf.append("eins ");
				break;
			case '2':
				buf.append("zwei ");
				break;
			case '3':
				buf.append("drei ");
				break;
			case '4':
				buf.append("vier ");
				break;
			case '5':
				buf.append("fünf ");
				break;
			case '6':
				buf.append("sechs ");
				break;
			case '7':
				buf.append("sieben ");
				break;
			case '8':
				buf.append("acht ");
				break;
			case '9':
				buf.append("neun ");
				break;
			default: // other characters (e.g., letters): output individually
				buf.append(digits.charAt(i));
				buf.append(' ');
			}
		}
		return buf.toString().trim();
	}

	/**
	 * For ordinals we put the expanded form in the sounds_like attribute and keep the surface form. This is for the POS tagger to
	 * tell a later module whether the ordinal is adverbial or adjectival.
	 * 
	 * @param doc
	 *            doc
	 * @param s
	 *            s
	 * @param createMtu
	 *            createMtu
	 * @return expandOrdinal(doc, value, createMtu, s)
	 */
	protected List<Element> expandOrdinal(Document doc, String s, boolean createMtu) {
		long value;
		// In canDealWith(), we have made a commitment to deal with
		// integers and roman numbers to be pronounced as ordinals.
		if (matchRoman(s)) {
			return expandRoman(doc, s, createMtu, true); // roman ordinal
		}
		String intString; // the string s without final dot, if any
		if (matchInteger(s)) {
			intString = s;
		} else {
			intString = s.substring(0, s.length() - 1);
		}
		try {
			while (intString.length() > 1 && intString.startsWith("0"))
				intString = intString.substring(1);
			value = Long.decode(intString).longValue();
		} catch (NumberFormatException e) {
			logger.info("Cannot convert string \"" + intString + "\" to long.");
			throw e;
		}
		return expandOrdinal(doc, value, createMtu, s);
	}

	protected List<Element> expandOrdinal(Document doc, long value, boolean createMtu, String orig) {
		StringBuilder exp = new StringBuilder();
		int rest = (int) value % 100;
		if (Math.abs(value) >= 100) {
			exp.append(expandInteger(value - rest));
			if (rest != 0)
				exp.append(" ");
		}
		switch (rest) {
		case 0:
			if (value == 0)
				exp.append("null");
			// else it is 100. etc.
			break;
		case 1:
			exp.append("ers");
			break;
		case 3:
			exp.append("drit");
			break;
		case 7:
			exp.append("sieb");
			break;
		case 8:
			exp.append("ach");
			break;
		default:
			exp.append(expandInteger(rest));
		}
		if (rest == 0 && value != 0 || rest >= 20)
			exp.append("s");
		exp.append("te");
		// OK, exp construction complete.
		// Now create the t element.
		Element t = MaryXML.createElement(doc, MaryXML.TOKEN);
		// Original surface form as graphemic form:
		MaryDomUtils.setTokenText(t, orig);
		// Expanded form in sounds_like attribute:
		t.setAttribute("sounds_like", exp.toString());
		t.setAttribute("ending", "ordinal");
		t.setAttribute("pos", "ADJA"); // part-of-speech: adjective
		List<Element> result = new ArrayList<Element>();
		if (createMtu) {
			// create mtu element enclosing the expanded tokens:
			Element mtu = MaryXML.createElement(doc, MaryXML.MTU);
			mtu.setAttribute("orig", orig);
			mtu.appendChild(t);
			result.add(mtu);
		} else {
			result.add(t);
		}
		return result;
	}

	protected List<Element> expandRoman(Document doc, String number, boolean createMtu) {
		// First, find out whether it is an ordinal or a simple integer:
		boolean isOrdinal = false;
		if (number.charAt(number.length() - 1) == '.') {
			isOrdinal = true;
			number = number.substring(0, number.length() - 1);
		}
		return expandRoman(doc, number, createMtu, isOrdinal);
	}

	protected List<Element> expandRoman(Document doc, String number, boolean createMtu, boolean isOrdinal) {
		// First make sure there is no dot at the end of number:
		// (here, we consider the dot an artefact of the fact that
		// reRoman allows an optional dot. This causes, e.g.,
		// <SAYAS MODE="cardinal">V.</SAYAS> to accept V., but
		// it is to be spoken as an integer.)
		if (number.charAt(number.length() - 1) == '.') {
			number = number.substring(0, number.length() - 1);
		}
		int value = MaryUtils.romanToInt(number);
		if (isOrdinal)
			return expandOrdinal(doc, value, createMtu, number);
		else
			return expandInteger(doc, value, createMtu, number);
	}

}
