/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.mary.modules.phonemiser;

import java.util.LinkedList;
import java.util.ListIterator;

import de.dfki.lt.mary.util.MaryUtils;

public class Syllabifier
{
    protected PhonemeSet phonemeSet;
    
    public Syllabifier(PhonemeSet phonemeSet)
    {
        this.phonemeSet = phonemeSet;
    }

    /**
     * Syllabify a phonemic string, marking syllable boundaries with dash characters in the output.
     * If the input marks stressed vowels with a suffix "1", these marks are removed, and
     * single quotes (') are inserted at the beginning of the corresponding syllable. 
     * @param phonemeString the phoneme string to syllabify.
     * @return a syllabified phone string
     */
    public String syllabify(String phonemeString)
    {
        LinkedList phonemeList = splitIntoPhonemes(phonemeString);
        syllabify(phonemeList);
        return MaryUtils.joinStrings(phonemeList);
    }

    /**
     * Syllabify a linked list of phonemes. This is an implementation of the
     * syllabification rules by J&uml;rgen Trouvain.
     * @return a linked list of phoneme strings with inserted "-" strings at
     * syllable boundaries.
     */
    public LinkedList syllabify(LinkedList phoneList)
    {
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

        if (phoneList == null) return null;
        ListIterator it = phoneList.listIterator(0);
        if (!it.hasNext()) return phoneList;
        Phoneme previous = getPhoneme((String)it.next());
        boolean previousIsVowel = false;
        if (previous != null && previous.sonority() >= 4)
            previousIsVowel = true;
        while (it.hasNext()) {
            Phoneme next = getPhoneme((String)it.next());
            boolean nextIsVowel = false;
            if (next != null && next.sonority() >= 4)
                nextIsVowel = true;
            // Regel(5)
            // Wenn zwischen zwei Vokalen keine weiteren Phoneme sind,
            // dann setze die Silbengrenze vor den zweiten Vokal.
            if (previousIsVowel && nextIsVowel &&
                !next.name().equals("6")) {
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
        while (it.hasNext()) {
            String s = (String)it.next();
            if (s.equals("-")) {
                // Forget about all valleys:
                minSonority = 7;
                minIndex = -1;
            } else {
                Phoneme ph = getPhoneme(s);
                if (ph != null && ph.sonority() < minSonority) {
                    minSonority = ph.sonority();
                    minIndex = it.previousIndex();
                } else if (ph != null && ph.sonority() >= 4) {
                    // Found a vowel. Now, if there is a (non-initial) sonority
                    // valley before this vowel, insert a valley marker:
                    if (minIndex > 0) {
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
            String s = (String) it.next();
            if (s.equals(".")) {
                it.previous(); // skip . backwards
                Phoneme ph = getPhoneme((String)it.previous());
                it.next(); it.next(); // skip ph and . forwards
                if (ph != null && ph.sonority() == 5) {
                    // The phoneme just after the marker:
                    ph = getPhoneme((String)it.next());
                    if (ph != null && ph.sonority() <= 3) {
                        // Now the big question: another consonant or not?
                        ph = getPhoneme((String)it.next());
                        if (ph != null && ph.sonority() <= 3) {
                            // (6b) remove ., go one further, insert -
                            // two ph back, and the .:
                            it.previous(); it.previous(); it.previous();
                            it.remove(); // remove the .
                            it.next(); // skip one ph
                            it.add("-");
                        } else {
                            // (6a) replace . with _
                            // two ph back, and the .:
                            it.previous(); it.previous(); it.previous();
                            it.set("_"); // replace . with _
                        }
                    } else {
                        // unlikely case: no consonant after a 5
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
            String s = (String) it.next();
            if (s.equals("_")) {
                Phoneme ph = getPhoneme((String)it.next());
                if (ph != null && ph.name().equals("N")) {
                    ph = getPhoneme((String)it.next());
                    if (ph != null && ph.sonority() >= 5) {
                        // (7) remove _, put a - after the N
                        // skip vowel, N, and _ backwards:
                        it.previous(); it.previous(); it.previous();
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
     * For those syllables containing a "1" character, remove that "1"
     * character and add a stress marker ' at the beginning of the syllable.
     */
    protected LinkedList correctStressSymbol(LinkedList phoneList)
    {
        boolean stressFound = false;
        ListIterator it = phoneList.listIterator(0);
        while (it.hasNext()) {
            String s = (String) it.next();
            if (s.endsWith("1")) {
                it.set(s.substring(0, s.length()-1)); // delete "1"
                if (!stressFound) {
                    // Only add a stress marker for first occurrence of "1":
                    // Search backwards for syllable boundary or beginning of word:
                    int steps = 0;
                    while (it.hasPrevious()) {
                        steps++;
                        String t = (String) it.previous();
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
                String s = (String) it.next();
                Phoneme ph = phonemeSet.getPhoneme(s);
                if (ph != null && ph.sonority() >= 5) { // non-schwa vowel
                    // Search backwards for syllable boundary or beginning of word:
                    int steps = 0;
                    while (it.hasPrevious()) {
                        steps++;
                        String t = (String) it.previous();
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
        return phoneList;
    }


    /**
     * Convert a phoneme string into a list of string representations of
     * individual phonemes. The input can use the suffix "1" to indicate
     * stressed vowels.  
     * @param phonemeString the phoneme string to split
     * @return a linked list of strings, each string representing an individual phoneme
     */
    protected LinkedList splitIntoPhonemes(String phonemeString)
    {
        LinkedList phoneList = new LinkedList();
        for (int i=0; i<phonemeString.length(); i++) {
            // Try to cut off individual segments, 
            // starting with the longest prefixes,
            // and allowing for a suffix "1" marking stress:
            String name = null;
            for (int j=3; j>=1; j--) {
                if (i+j <= phonemeString.length()) {
                    String candidate = phonemeString.substring(i, i+j);
                    if (getPhoneme(candidate) != null) { // found
                        name = candidate;
                        i+=j-1; // so that the next i++ goes beyond current phoneme
                        break;
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
     * Get the Phoneme object named phoneme; if phoneme ends with "1",
     * discard the "1" and use the rest of the string as the phoneme symbol.
     */
    protected Phoneme getPhoneme(String phoneme)
    {
        if (phoneme.endsWith("1"))
            return phonemeSet.getPhoneme(phoneme.substring(0,phoneme.length()-1));
        else
            return phonemeSet.getPhoneme(phoneme);
    }

}
