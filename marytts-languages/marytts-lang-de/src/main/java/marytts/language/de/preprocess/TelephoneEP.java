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
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An expansion pattern implementation for telephone number patterns.
 *
 * @author Marc Schr&ouml;der
 */

public class TelephoneEP extends ExpansionPattern {
	private final String[] _knownTypes = { "telephone", };
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
	protected final String sTelephone = "(?:[0+][0-9/\\-\\.]+)";
	protected final String sMatchingChars = "[0-9\\+\\/\\-\\.]";

	// Now the actual match patterns:
	protected final Pattern reTelephone = Pattern.compile(sTelephone);
	private final Pattern reMatchingChars = Pattern.compile(sMatchingChars);

	public Pattern reMatchingChars() {
		return reMatchingChars;
	}

	/**
	 * Every subclass has its own logger. The important point is that if several threads are accessing the variable at the same
	 * telephone, the logger needs to be thread-safe or it will produce rubbish.
	 */
	// private Logger logger = MaryUtils.getLogger("TelephoneEP");

	public TelephoneEP() {
		super();
	}

	protected int match(String s, int type) {
		switch (type) {
		case 0:
			if (matchTelephone(s))
				return 0;
			break;
		}
		return -1;
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
		case 0:
			expanded = expandTelephone(doc, tokens);
			break;
		}
		replaceTokens(tokens, expanded);
		return expanded;
	}

	protected boolean matchTelephone(String s) {
		return reTelephone.matcher(s).matches();
	}

	protected int canDealWith(String input, int typeCode) {
		if (typeCode != 0)
			return -1;
		if (REPattern.digit.matcher(input).find()) // contains at least one digit
			return 0; // OK
		else
			return -1; // failure
	}

	/**
	 * This method, differently from what is usually done, does not take a string argument, but the original tokens. The reason is
	 * that grouping of telephone number parts is often done using whitespace, information that would be lost if the
	 * whitespace-free string was used.
	 * 
	 * @param doc
	 *            doc
	 * @param tokens
	 *            tokens
	 * @return exp
	 */
	protected List<Element> expandTelephone(Document doc, List<Element> tokens) {
		// Before expansion, split into parts as follows:
		// - token boundaries are separators
		// - non-digits are separators
		// - If a part is longer than 3 digits, split it in
		// 3-2-...-2 (odd number of digits) or
		// 2-2-...-2 (even number of digits) digit parts.
		if (tokens == null || tokens.size() == 0)
			return null;
		ArrayList<Element> exp = new ArrayList<Element>();
		ArrayList<String> parts = new ArrayList<String>();
		// The very first character in the telephone number may be a +
		// (for international area code).
		Element firstToken = (Element) tokens.get(0);
		String firstText = MaryDomUtils.tokenText(firstToken);
		if (firstText != null && firstText.length() > 0 && firstText.charAt(0) == '+') {
			exp.addAll(makeNewTokens(doc, "Plus"));
			MaryDomUtils.setTokenText(firstToken, firstText.substring(1)); // remove + sign
		}
		for (Iterator<Element> it = tokens.iterator(); it.hasNext();) {
			Element t = (Element) it.next();
			String s = MaryDomUtils.tokenText(t);
			if (!REPattern.digit.matcher(s).find()) // no digits in this token
				continue; // skip this token
			if (REPattern.onlyDigits.matcher(s).matches()) {
				parts.add(s);
			} else {
				int first = -1; // index in s of first digit of a new part
				for (int i = 0; i < s.length(); i++) {
					if (Character.isDigit(s.charAt(i))) {
						if (first == -1) { // first digit of new part found
							first = i;
						}
					} else { // not a digit
						if (first != -1) { // first non-digit after a part found
							parts.add(s.substring(first, i));
							first = -1;
						}
					}
				}
				if (first != -1) { // s ends in digits
					parts.add(s.substring(first));
				}
			}
		}
		// So now parts contains the digit groups.
		// Now find long digit groups and
		// split according to number of digits.
		for (int i = 0; i < parts.size(); i++) {
			String p = (String) parts.get(i);
			if (p.length() > 3) {
				if (p.length() % 2 != 0) { // odd number of digits
					// replace long entry by one group of three
					parts.set(i, p.substring(0, 3));
					p = p.substring(3);
				} else {
					// replace long group by one group of two
					parts.set(i, p.substring(0, 2));
					p = p.substring(2);
				}
				// now remove groups of two
				while (p.length() > 0) {
					i++; // the current insert position
					parts.add(i, p.substring(0, 2));
					p = p.substring(2);
				}
			}
		}
		// Now parts contains the groups we are to speak.
		for (Iterator<String> it = parts.iterator(); it.hasNext();) {
			exp.addAll(number.expandDigits(doc, (String) it.next(), true));
			// Force accent on last token in mtu:
			Element mtu = (Element) exp.get(exp.size() - 1);
			Element t = (Element) mtu.getLastChild();
			t.setAttribute("accent", "unknown");
			// And add a boundary after the group, unless it is the last group:
			if (it.hasNext()) {
				exp.add(MaryDomUtils.createBoundary(doc));
			}
		}
		return exp;
	}

}
