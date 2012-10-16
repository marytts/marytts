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
package marytts.language.it.phonemiser;

import java.util.LinkedList;
import java.util.ListIterator;

import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;


public class Syllabifier extends marytts.modules.phonemiser.Syllabifier
{
    public Syllabifier(AllophoneSet allophoneSet, boolean removeTrailingOneFromPhones)
    {
    	super(allophoneSet, removeTrailingOneFromPhones);
    }
    
    public Syllabifier(AllophoneSet allophoneSet)
    {
    	super(allophoneSet);
    }
    
    
    /**
     * For those syllables containing a "1" character, remove that "1"
     * character and add a stress marker ' at the beginning of the syllable.
     */
    protected void correctStressSymbol(LinkedList<String> phoneList)
    {
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
            // if the word does not end with a vowel and last syllable contains a vowel, stress the last syllable
        	// otherwise stress the second last syllable (or none)
			it = phoneList.listIterator();
			String s = null;
			int phonenumber = 0;
			int lastVowelIndex = -1;
			int lastLastVowelIndex = -1;
			int syllableToStressIndex = -1;

			while (it.hasNext()) {
				s = it.next();
				if (s != null) {
					Allophone ph = allophoneSet.getAllophone(s);
					if (ph != null) {
						if (ph != null && ph.sonority() >= 5) { // non-schwa
																// vowel
							lastLastVowelIndex = lastVowelIndex;
							lastVowelIndex = phonenumber;
						}
					}
				}
				phonenumber++;
			}

			syllableToStressIndex = lastLastVowelIndex;
			if (lastVowelIndex > -1) {
				it = phoneList.listIterator(lastVowelIndex);
				while (it.hasNext()) {
					s = it.next();
					if (s != null) {
						if (!(s.equals("-") || s.equals("_"))) { // not syllable boundary
							Allophone ph = allophoneSet.getAllophone(s);
							if (ph != null) {
								if (!(ph != null && ph.sonority() >= 5)) { // not non-schwa vowel
									// so the words does not end with vowel
									syllableToStressIndex = lastVowelIndex;
									break;
								}
							}
						}
					}
				}
			}
			if(syllableToStressIndex > -1){
				it = phoneList.listIterator(syllableToStressIndex);
				while (it.hasPrevious()) {
					s = it.previous();
					if (s != null) {
						if ((s.equals("-") || s.equals("_"))) { // syllable boundary
							it.next();
							it.add("'");
							syllableToStressIndex = -1;
							break;
						}
					}
				}
				if(syllableToStressIndex > -1){
					it.add("'");
				}
			}
		}
    }


}

