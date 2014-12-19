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

package marytts.language.de.postlex;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The rules for the postlexical phonological processes module.
 * 
 * @author Marc Schr&ouml;der
 */

public class PhonologicalRules {
	// Rules as regular expressions with substitution patterns:
	// The first string is a regular expression pattern, the three others are
	// substitution patterns for PRECISE, NORMAL and SLOPPY pronunciation
	// respectively. They may contain bracket references $1, $2, ...
	// as in the example below:
	// {"([bdg])@(n)", "$1@$2", "$1@$2", "$1@$2"}
	private static final String[][] _rules = {
			// @-Elision mit -en, -el, -em
			{ "([dlrszSt])@n", "$1@n", "$1@n", "$1@n" },
			// warning: mbrola de1/2 don't have Z-n diphone

			// @Elision mit -en, -el, -em; Assimilation
			{ "f@n", "f@n", "f@n", "f@n" },
			{ "g@n", "g@n", "g@n", "g@n" }, // warning: mbrola de1 doesn't have g-N diphone
			{ "k@n", "k@n", "k@n", "k@n" },// warning: mbrola de1 doesn't have k-N diphone
			{ "p@n", "p@n", "p@n", "p@n" },
			{ "x@n", "x@n", "x@n", "x@n" },// warning: mbrola de1/2 don't have x-N diphone

			// @-Elision mit -en, -el, -em; Assimilation und Geminatenreduktion
			{ "b@n", "b@n", "b@n", "b@n" },// warning: mbrola de1 doesn't have b-m diphone
			{ "m@n", "m@n", "m@n", "m@n" },
			{ "n@n", "n@n", "n@n", "n@n" },
			// bei Geminatenreduktion wird der uebrigbleibende Laut eigentlich gelaengt.
			// Da es jedoch noch kein Symbol und keine Semantik fuer Laengung gibt,
			// soll an dieser Stelle nur darauf hingewiesen werden.

			// Assimilation der Artikulationsart
			{ "g-n", "g-n", "g-n", "g-n" },

			// Assimilation und Geminatenreduktion
			{ "m-b", "m-b", "m-b", "m-b" },
			{ "t-t", "t-t", "t-t", "t-t" },
			// bei Geminatenreduktion wird der uebrigbleibende Laut eigentlich gelaengt.
			// Da es jedoch noch kein Symbol und keine Semantik fuer Laengung gibt,
			// soll an dieser Stelle nur darauf hingewiesen werden.

			// glottal stop removal:
			{ "\\?(aI|OY|aU|[iIyYe\\{E29uUoOaA])", "?$1", "?$1", "?$1" },

			// Reduce E6 -> 6 in unstressed syllables only:
			// {"^([^'-]*)E6", "$16", "$16", "$16"},
			// {"-([^'-]*)E6", "-$16", "-$16", "-$16"},

			// be more specific: reduce fE6 -> f6 in unstressed syllables only
			{ "^([^'-]*)fE6", "$1f6", "$1f6", "$1f6" },
			{ "-([^'-]*)fE6", "-$1f6", "-$1f6", "-$1f6" },

			// Replace ?6 with ?E6 wordinitial
			{ "\\?6", "\\?E6", "\\?E6", "\\?E6" },

			// !! Translate the old MARY SAMPA to the new MARY SAMPA:
			{ "O~:", "a~", "a~", "a~" }, { "o~:", "o~", "o~", "o~" }, { "9~:", "9~", "9~", "9~" }, { "E~:", "e~", "e~", "e~" },
			{ "O~", "a~", "a~", "a~" }, { "o~", "o~", "o~", "o~" }, { "9~", "9~", "9~", "9~" }, { "E~", "e~", "e~", "e~" },
			{ "\\{", "E", "E", "E" },
	// {"r", "R", "R", "R"}
	};
	private static final List rules = initialiseRules();

	private static List initialiseRules() {
		List r = new ArrayList();
		for (int i = 0; i < _rules.length; i++) {
			r.add(new PhonologicalRules(_rules[i]));
		}
		return r;
	}

	public static List getRules() {
		return rules;
	}

	public static final int PRECISE = 1;
	public static final int NORMAL = 2;
	public static final int SLOPPY = 3;

	private Pattern key;
	private String precise;
	private String normal;
	private String sloppy;

	public PhonologicalRules(String[] data) {
		try {
			key = Pattern.compile(data[0]);
		} catch (PatternSyntaxException e) {
			System.err.println("Cannot compile regular expression `" + data[0] + "':");
			e.printStackTrace();
		}
		precise = data[1];
		normal = data[2];
		sloppy = data[3];
	}

	public boolean matches(String input) {
		return key.matcher(input).find();
	}

	public String apply(String input, int precision) {
		String repl = normal;
		if (precision == PRECISE)
			repl = precise;
		else if (precision == SLOPPY)
			repl = sloppy;
		return key.matcher(input).replaceAll(repl);
	}

}
