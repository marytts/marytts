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

package marytts.language.it.preprocess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.datatypes.MaryXML;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

/**
 * An expansion pattern implementation for composite patterns. Words consisting of digits and letters and pseudo-composites with a
 * hyphen are split into their components. These will then need to be looked at by the other pattern expanders. CompositeEP
 * directly overrides process(), and does not care about the usual subclass methods isCandidate(), match(), and expand().
 * 
 * @author Marc Schr&ouml;der
 */

public class CompositeEP extends ExpansionPattern {
	// Domain-specific primitives:
	Pattern reTrailingHyphen = Pattern.compile("([A-ZÀÁÈÉÌÍÒÓÙÚa-zàáèéìíòóùú])-$");
	Pattern reLeadingHyphen = Pattern.compile("^-([A-ZÀÁÈÉÌÍÒÓÙÚa-zàáèéìíòóùú])");
	// LettersDigitsAndHyphens consists of parts separated by hyphens,
	// each part containing at least one digit or letter.
	Pattern reLettersDigitsAndHyphens = Pattern
			.compile("([^-]*[A-ZÀÁÈÉÌÍÒÓÙÚa-zàáèéìíòóùú0-9][^-]*)(-[^-]*[A-ZÀÁÈÉÌÍÒÓÙÚa-zàáèéìíòóùú0-9][^-]*)+");

	Pattern reLettersDigitsAndApostrophe = Pattern
			.compile("([^']*[A-ZÀÁÈÉÌÍÒÓÙÚa-zàáèéìíòóùú0-9][^']*)('[^']*[A-ZÀÁÈÉÌÍÒÓÙÚa-zàáèéìíòóùú0-9][^']*)+");

	// this can be used for dell' un' etc..
	/*
	 * Pattern reLettersAndApostrophe =
	 * //Pattern.compile("([^']*[A-ZÀÁÈÉÌÍÒÓÙÚa-zàáèéìíòóùú][^']*)('[^']*[A-ZÀÁÈÉÌÍÒÓÙÚa-zàáèéìíòóùú][^']*)+");
	 * Pattern.compile("([A-ZÀÁÈÉÌÍÒÓÙÚa-zàáèéìíòóùú]+)('([A-ZÀÁÈÉÌÍÒÓÙÚa-zàáèéìíòóùú])+)+");
	 */
	// TODO: FABIO Check if better with new REPattern...
	// This is used for c'X t'X d'X ()
	Pattern reOneLetterAndApostrophe = Pattern
			.compile("([^']*[^EIOUYaeiouyÀÁÈÉÌÍÒÓÄÖÜËÏäöüëïÙÚàáèéìíòóùú])('[hH]?([AEIOUYaeiouyÀÁÈÉÌÍÒÓÄÖÜËÏäöüëïÙÚàáèéìíòóùú][^']*)+)+");

	// Both letters and digits, in any order:
	Pattern reLettersAndDigits = Pattern
			.compile("(?:(?:[A-ZÀÁÈÉÌÍÒÓÙÚa-zàáèéìíòóùú]+[0-9]+)|(?:[0-9]+[A-ZÀÁÈÉÌÍÒÓÙÚa-zàáèéìíòóùú]+))[A-ZÄÖÜa-zàáèéìíòóùú0-9]*");

	public List knownTypes() {
		return new ArrayList();
	}

	private final Pattern reMatchingChars = Pattern.compile("");

	public Pattern reMatchingChars() {
		return reMatchingChars;
	}

	/**
	 * Every subclass has its own logger. The important point is that if several threads are accessing the variable at the same
	 * time, the logger needs to be thread-safe or it will produce rubbish.
	 */
	private Logger logger = MaryUtils.getLogger("CompositeEP");

	public CompositeEP() {
		super();
	}

	/**
	 * Process and expand a list of tokens.
	 * 
	 * @param tokens
	 *            the list of tokens to be expanded one after the other.
	 * @return a list of expanded forms of all the tokens, i.e. the concatenation of the expanded form (or unexpanded form if no
	 *         expansion is possible) of all the tokens.
	 */
	private List process(List tokens) {
		List result = new ArrayList();
		for (Iterator it = tokens.iterator(); it.hasNext();) {
			Element t = (Element) it.next();
			if (!t.getTagName().equals(MaryXML.TOKEN))
				throw new DOMException(DOMException.INVALID_ACCESS_ERR, "Expected t element");
			List expanded = new ArrayList();
			process(t, expanded);
			if (expanded.isEmpty()) // no expansion
				result.add(t);
			else
				result.addAll(expanded);
		}
		return result;
	}

	/**
	 * Process this token. The CompositeEP works as a splitter of single tokens, iteratively expanding a token into its
	 * components.
	 * 
	 * @param t
	 *            the element to expand. After processing, this Element will still exist and be a valid Element, but possibly with
	 *            a different content, and possibly enclosed by an &lt;mtu&gt; element. In addition, &lt;t&gt; may have new
	 *            right-hand neighbors.
	 * @param expanded
	 *            an empty list into which the expanded Elements are placed if an expansion occurred. The list will remain empty
	 *            if no expansion was performed.
	 * @return true if this pattern is confident to have fully expanded this token, false if nothing could be done or more
	 *         expansion may be necessary. CompositeEP always returns false, in order to have other ExpansionPatterns look at the
	 *         components as well.
	 */
	public boolean process(Element t, final List expanded) {
		if (t == null || expanded == null)
			throw new NullPointerException("Received null argument");
		if (!t.getTagName().equals(MaryXML.TOKEN))
			throw new DOMException(DOMException.INVALID_ACCESS_ERR, "Expected t element");
		if (!expanded.isEmpty())
			throw new IllegalArgumentException("Expected empty list, but list has " + expanded.size() + " elements.");
		// Only modify tokens for which no pronunciation is given:
		if (t.hasAttribute("ph") || t.hasAttribute("sounds_like")) {
			return false;
		}

		// /// First, some cleaning up of the token: /////
		String s = MaryDomUtils.tokenText(t);
		// System.err.println("Trying to split " + s);
		if (reTrailingHyphen.matcher(s).find()) {
			// System.err.println("reTrailingHyphen");
			// remove trailing hyphens after letters:
			s = reTrailingHyphen.matcher(s).replaceFirst("$1");
			MaryDomUtils.setTokenText(t, s);
		}
		if (reLeadingHyphen.matcher(s).find()) {
			// System.err.println("reLeadingHyphen");
			// remove leading hyphens before letters:
			s = reLeadingHyphen.matcher(s).replaceFirst("$1");
			MaryDomUtils.setTokenText(t, s);
		}

		// /// Then, see if we can split it: /////
		// System.err.println("Then, see if we can split it: /////");
		if (reLettersDigitsAndHyphens.matcher(s).matches()) {
			// OK, a hyphen between parts containing letters and/or digits.
			// In pseudo-composita, accent is on the first component:
			Element mtu = MaryDomUtils.encloseWithMTU(t, s, "first");
			StringTokenizer st = new StringTokenizer(s, "-");
			assert st.hasMoreTokens();
			MaryDomUtils.setTokenText(t, st.nextToken());
			expanded.add(t);
			while (st.hasMoreTokens()) {
				t = MaryDomUtils.appendToken(t, st.nextToken());
				expanded.add(t);
			}
		}

		// This is for one letter proclitics (c'X, d'X, ...) and qual'X
		else if (reOneLetterAndApostrophe.matcher(s).matches()) {
			// System.err.println("one letter and apostrophe");
			// OK, a hyphen between parts containing letters and/or digits.
			// In pseudo-composita, accent is on the first component:
			// c'X l'X d'X c' is the first-proclitics part
			Element mtu = MaryDomUtils.encloseWithMTU(t, s, "last-proclitics");
			StringTokenizer st = new StringTokenizer(s, "'");
			assert st.hasMoreTokens();
			MaryDomUtils.setTokenText(t, st.nextToken() + "'");
			expanded.add(t);
			while (st.hasMoreTokens()) {
				t = MaryDomUtils.appendToken(t, st.nextToken());
				expanded.add(t);
			}
		} else if (reLettersAndDigits.matcher(s).matches()) {
			// Token consists only of letters and digits.
			// Split between letters and digits.
			// In pseudo-composita, accent is on the first component:
			Element mtu = MaryDomUtils.encloseWithMTU(t, s, "first");
			String s1 = s;
			boolean isFirst = true;
			while (s1.length() > 0) {
				String part;
				Matcher reMatcher = REPattern.initialNonDigits.matcher(s1);
				if (reMatcher.find()) {
					part = reMatcher.group();
					s1 = reMatcher.replaceFirst("");
				} else {
					reMatcher = REPattern.initialDigits.matcher(s1);
					reMatcher.find();
					part = reMatcher.group();
					s1 = reMatcher.replaceFirst("");
				}
				if (isFirst)
					MaryDomUtils.setTokenText(t, part);
				else
					t = MaryDomUtils.appendToken(t, part);
				expanded.add(t);
				isFirst = false;
			}
		} else if (s.equals("'s")) {
			// a standalone 's: simply pronounce it as [s].
			t.setAttribute("ph", "s");
			expanded.add(t);
		} else if (s.endsWith("'s")) {
			// Cases like "geht's": Simply have it pronounced like "gehts".
			// No iteration.
			t.setAttribute("sounds_like", s.substring(0, s.length() - 2));
			t.setAttribute("ph", "*s");
			expanded.add(t);
		} else if (ExpansionPattern.reSplitAtChars().matcher(s).find()
				&& (REPattern.letter.matcher(s).find() || REPattern.digit.matcher(s).find())) {
			// Split into parts, keeping each special char as one part
			// For special characters, accent is on the last component:
			Element mtu = MaryDomUtils.encloseWithMTU(t, s, "last");
			StringTokenizer st = new StringTokenizer(s, ExpansionPattern.getSplitAtChars(), true); // return delimiters
			MaryDomUtils.setTokenText(t, st.nextToken());
			expanded.add(t);
			while (st.hasMoreTokens()) {
				t = MaryDomUtils.appendToken(t, st.nextToken());
				expanded.add(t);
			}
		}
		// iterative call:
		if (expanded.size() > 0) {
			List newExpanded = process(expanded);
			expanded.clear();
			expanded.addAll(newExpanded);
		}

		// Never return true, in order to allow other ExpansionPatterns to
		// expand the components.
		return false;
	}

	protected int canDealWith(String input, int typeCode) {
		return match(input, typeCode);
	}

	protected int match(String input, int typeCode) {
		throw new RuntimeException("This method should not be called.");
	}

	protected List expand(List tokens, String text, int typeCode) {
		throw new RuntimeException("This method should not be called.");
	}

}
