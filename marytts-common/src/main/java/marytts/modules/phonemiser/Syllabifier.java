/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.modules.phonemiser;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * @deprecated Use {@link AllophoneSet#syllabify(String)} instead.
 */
public class Syllabifier {
	protected AllophoneSet allophoneSet;
	protected boolean removeTrailingOneFromPhones = true;

	public Syllabifier(AllophoneSet allophoneSet, boolean removeTrailingOneFromPhones) {
		this.allophoneSet = allophoneSet;
		this.removeTrailingOneFromPhones = removeTrailingOneFromPhones;
	}

	public Syllabifier(AllophoneSet allophoneSet) {
		this.allophoneSet = allophoneSet;
	}

	/**
	 * Syllabify a phonetic string, marking syllable boundaries with dash characters in the output. If the input marks stressed
	 * vowels with a suffix "1", these marks are removed, and single quotes (') are inserted at the beginning of the corresponding
	 * syllable.
	 * 
	 * @param phoneString
	 *            the phone string to syllabify.
	 * @return a syllabified phone string, with space characters inserted between individual phone symbols
	 */
	public String syllabify(String phoneString) {
		LinkedList<String> phoneList = splitIntoAllophones(phoneString);
		syllabify(phoneList);
		StringBuilder sb = new StringBuilder();
		for (String p : phoneList) {
			if (sb.length() > 0)
				sb.append(" ");
			sb.append(p);
		}
		return sb.toString();
	}

	/**
	 * Syllabify a LinkedList of phones. This is an implementation of the syllabification rules by J&uuml;rgen Trouvain.
	 * 
	 * @param phoneList
	 *            phoneList
	 * @return a LinkedList of phone strings with inserted "-" strings at syllable boundaries.
	 */
	public LinkedList<String> syllabify(LinkedList<String> phoneList) {
		// Regel(1a)
		// Jede Grenze einer morphologischen Wurzel stellt eine
		// Silbengrenze dar.
		// Regel(1b)
		// Jede Grenze eines Präfixes stellt eine Silbengrenze dar.
		// Dort, wo ein Fugensuffix bzw ein Suffix beginnt,
		// gibt es keine morphologische Silbengrenze.
		// Bsp: Lebens-gefaehrte und nicht Leben-s-gefaehrte
		// Bsp: Mei-nung und nicht Mein-ung

		// Note: We don't have morpheme boundaries! (=> so ignore rule 1)

		// 2.: finde nicht-morphologische Silbengrenzen
		// teile Woerter, die eine morphologisch bedingte Silbengrenze haben
		// in ihre Silbenteile, um dort spaeter nach weiteren,
		// nicht-morphologisch bedingten, Silbengrenzen zu suchen

		// Only one such component as long as we don't have morpheme boundaries

		if (phoneList == null) {
			throw new IllegalArgumentException("Cannot syllabify null string");
		}
		ListIterator<String> it = phoneList.listIterator(0);
		if (!it.hasNext()) {
			return phoneList;
		}
		Allophone previous = getAllophone(it.next());
		boolean previousIsVowel = (previous != null && previous.sonority() >= 4);
		while (it.hasNext()) {
			Allophone next = getAllophone(it.next());
			boolean nextIsVowel = (next != null && next.sonority() >= 4);

			// Regel(5)
			// Wenn zwischen zwei Vokalen keine weiteren Phone sind,
			// dann setze die Silbengrenze vor den zweiten Vokal.
			if (previousIsVowel && nextIsVowel && !next.name().equals("6")) {
				// Insert a syllable boundary between the two.
				it.previous(); // one step back
				it.add("-"); // insert syllable boundary
				it.next(); // and forward again
			}
			previousIsVowel = nextIsVowel;
		}

		// Regel(4)
		// Suche das "Tal" (kleinster Level < 4) zwischen zwei benachbarten
		// Vokalen, sofern die Vokale nicht durch eine morphologisch bedingte
		// Grenze getrennt werden.
		it = phoneList.listIterator(0);
		int minSonority = 7; // one higher than possible maximum.
		int minIndex = -1; // position of the sonority minimum
		int syllableStart = -1;
		while (it.hasNext()) {
			String s = it.next();
			if (s.equals("-")) {
				// Forget about all valleys:
				minSonority = 7;
				minIndex = -1;
				syllableStart = it.previousIndex();
			} else {
				Allophone ph = getAllophone(s);
				if (ph != null && ph.sonority() < minSonority) {
					minSonority = ph.sonority();
					minIndex = it.previousIndex();
				} else if (ph != null && ph.sonority() >= 4) {
					// Found a vowel. Now, if there is a (non-initial) sonority
					// valley before this vowel, insert a valley marker:
					if (minIndex > syllableStart + 1) {
						int steps = 0;
						while (it.nextIndex() > minIndex) {
							steps++;
							it.previous();
						}
						it.add(".");
						while (steps > 0) {
							it.next();
							steps--;
						}
					}
					minSonority = 7;
					minIndex = -1;
				}
			}
		}

		// Regel(6a)
		// Steht zwischen einem ungespannten Vokal (Level 5) und dem
		// darauffolgenden Vokal (Level 4, 5 oder 6) nur *ein* Konsonant des
		// Levels 2 oder 3, so ersetze die Talmarkierung durch eine
		// ambisilbische Silbengrenze (Symbol "_").
		// halbformal:
		// ([v5]).([k2,3])([v4,5,6])
		// --> ([v5])_([k2,3])([v4,5,6])

		// Regel(6b)
		// Steht zwischen einem ungespannten Vokal (Level 5) und dem
		// darauffolgenden Vokal (Level 4, 5 oder 6) mehr als ein Konsonant
		// (Levels 1,2 oder 3), und folgt gleichzeitig dem 5er-Vokal eine
		// Talmarkierung, so versetze die "Talmarkierung" ein Phonem weiter und
		// ersetze sie durch eine normale Silbengrenze.
		// halbformal:
		// ([v5]).([k1,2,3])([k1,2,3]+)([v4,5,6])
		// --> ([v5]).([k1,2,3])-([k1,2,3]+)([v4,5,6])

		// Regel(6c)
		// In allen anderen Faellen ersetze die "Talmarkierung" mit einer
		// normalen Silbengrenze.
		it = phoneList.listIterator(0);
		while (it.hasNext()) {
			String s = it.next();
			if (s.equals(".")) {
				it.previous(); // skip . backwards
				Allophone ph = getAllophone(it.previous());
				it.next();
				it.next(); // skip ph and . forwards
				if (ph != null && ph.sonority() == 5) {
					// The phone just after the marker:
					ph = getAllophone(it.next());
					if (ph != null && ph.sonority() <= 3) {
						// Now the big question: another consonant or not?
						ph = getAllophone(it.next());
						if (ph != null && ph.sonority() <= 3) {
							// (6b) remove ., go one further, insert -
							// two ph back, and the .:
							it.previous();
							it.previous();
							it.previous();
							it.remove(); // remove the .
							it.next(); // skip one ph
							it.add("-");
						} else {
							// (6a) replace . with _
							// two ph back, and the .:
							it.previous();
							it.previous();
							it.previous();

							// only use minuses, because underscores denote also pauses
							// it.set("_"); // replace . with _
							it.set("-"); // replace . with -
						}
					} else {
						// unlikely case: no consonant after a 5
						it.previous();
						it.previous();
						it.set("-");
					}
				} else {
					// (6c) simply replace . with -
					it.set("-");
				}
			}
		}

		// Regel(7)
		// Folgt einem Phonem /N/, vor dem unmittelbar eine ambisilbische
		// Silbengrenze steht, ein Vollvokal (Level 5 oder 6), so verschiebe
		// die Silbengrenze um ein Phonem (naemlich hinter das /N/) und
		// ersetze es durch eine normale Silbengrenze.
		// halbformal:
		// _N([v5,6])
		// --> N-([v5,6])
		it = phoneList.listIterator(0);
		while (it.hasNext()) {
			String s = it.next();
			// only use minuses, because underscores denote also pauses
			// if (s.equals("_")) {
			if (s.equals("-")) {
				Allophone ph = getAllophone(it.next());
				if (ph != null && ph.name().equals("N")) {
					ph = getAllophone(it.next());
					if (ph != null && ph.sonority() >= 5) {
						// (7) remove _, put a - after the N
						// skip vowel, N, and _ backwards:
						it.previous();
						it.previous();
						it.previous();
						it.remove(); // remove _
						it.next(); // skip N forwards
						it.add("-"); // insert -
					} // else, just leave it
				}
			}
		}
		correctStressSymbol(phoneList);
		return phoneList;
	}

	/**
	 * For those syllables containing a "1" character, remove that "1" character and add a stress marker ' at the beginning of the
	 * syllable.
	 * 
	 * @param phoneList
	 *            phoneList
	 */
	protected void correctStressSymbol(LinkedList<String> phoneList) {
		boolean stressFound = false;
		ListIterator<String> it = phoneList.listIterator(0);
		while (it.hasNext()) {
			String s = it.next();
			if (s.endsWith("1")) {
				if (this.removeTrailingOneFromPhones) {
					it.set(s.substring(0, s.length() - 1)); // delete "1"
				}
				if (!stressFound) {
					// Only add a stress marker for first occurrence of "1":
					// Search backwards for syllable boundary or beginning of word:
					int steps = 0;
					while (it.hasPrevious()) {
						steps++;
						String t = it.previous();
						if (t.equals("-") || t.equals("_")) { // syllable boundary
							it.next();
							steps--;
							break;
						}
					}
					it.add("'");
					while (steps > 0) {
						it.next();
						steps--;
					}
					stressFound = true;
				}
			}
		}
		// No stressed vowel in word?
		if (!stressFound) {
			// Stress first non-schwa syllable
			it = phoneList.listIterator(0);
			while (it.hasNext()) {
				String s = it.next();
				Allophone ph = allophoneSet.getAllophone(s);
				if (ph.sonority() >= 5) { // non-schwa vowel
					// Search backwards for syllable boundary or beginning of word:
					int steps = 0;
					while (it.hasPrevious()) {
						steps++;
						String t = it.previous();
						if (t.equals("-") || t.equals("_")) { // syllable boundary
							it.next();
							steps--;
							break;
						}
					}
					it.add("'");
					while (steps > 0) {
						it.next();
						steps--;
					}
					break; // OK, that's it.
				}
			}
		}
	}

	/**
	 * Convert a phone string into a list of string representations of individual phones. The input can use the suffix "1" to
	 * indicate stressed vowels.
	 * 
	 * @param phoneString
	 *            the phone string to split
	 * @return a linked list of strings, each string representing an individual phone
	 * @deprecated This duplicates (badly) {@link AllophoneSet#splitAllophoneString(String)}; use that method instead.
	 */
	@Deprecated
	protected LinkedList<String> splitIntoAllophones(String phoneString) {
		LinkedList<String> phoneList = new LinkedList<String>();
		for (int i = 0; i < phoneString.length(); i++) {
			// Try to cut off individual segments,
			// starting with the longest prefixes,
			// and allowing for a suffix "1" marking stress:
			String name = null;
			for (int j = 3; j >= 1; j--) {
				if (i + j <= phoneString.length()) {
					String candidate = phoneString.substring(i, i + j);
					try {
						allophoneSet.getAllophone(candidate);
						name = candidate;
						i += j - 1; // so that the next i++ goes beyond current phone
						break;
					} catch (IllegalArgumentException e) {
						// ignore
					}
				}
			}
			if (name != null) {
				phoneList.add(name);
			}
		}
		return phoneList;
	}

	/**
	 * Get the Allophone object named phone; if phone ends with "1", discard the "1" and use the rest of the string as the phone
	 * symbol.
	 * 
	 * @param phone
	 *            phone
	 * @deprecated Use {@link AllophoneSet#getAllophone(String)} instead
	 * @return allophoneset.getAllophone(phonesubstring(0, phone.length() - 1)) if this.removeTrailingOneFromPhones and
	 *         phone.endsWith("1"), allophoneset.getAllophone(phonesubstring(phone) otherwise
	 */
	@Deprecated
	protected Allophone getAllophone(String phone) {
		if (this.removeTrailingOneFromPhones && phone.endsWith("1"))
			return allophoneSet.getAllophone(phone.substring(0, phone.length() - 1));
		else
			return allophoneSet.getAllophone(phone);
	}

}
