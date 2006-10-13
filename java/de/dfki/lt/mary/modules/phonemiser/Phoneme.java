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


public class Phoneme
{
    private String name;
    private int inherentDuration;
    private int minimalDuration;
    private boolean isVowel;
    private boolean isSyllabic;
    private boolean isVoiced;
    private boolean isSonorant;
    private boolean isLiquid;
    private boolean isNasal;
    private boolean isGlide;
    private boolean isPlosive;
    private boolean isPause;
    private int sonority;
    private String example;

    /**
     * Create a new Phoneme object, with the settings specified.
     * <code>name</code> is the phonetic symbol for this segment.
     * <code>inherentDuration</code> and <code>minimalDuration</code>
     * are values for the Klatt rules of duration specification.
     * <code>phonology</code> is a String consisting of 9 characters,
     * each of them a <code>+</code> or a <code>-</code> character, which
     * have the following meanings (in this order): vowel, syllabic, voiced,
     * sonorant, liquid, nasal, glide, plosive, and pause. E.g., the string
     * <code>"--++-+---"</code> describes a nasal consonant, i.e. voiced,
     * sonorant, and nasal.
     * <code>example</code> is a free text string giving a pronounciation
     * example of the segment, which may be the empty string.
     */
    public Phoneme(String name, int inherentDuration, int minimalDuration,
                    String phonology, int sonority, String example)
    {
        this.name = name;
        this.inherentDuration = inherentDuration;
        this.minimalDuration = minimalDuration;
        this.isVowel    = phonology.charAt(0) == '+';
        this.isSyllabic = phonology.charAt(1) == '+';
        this.isVoiced   = phonology.charAt(2) == '+';
        this.isSonorant = phonology.charAt(3) == '+';
        this.isLiquid   = phonology.charAt(4) == '+';
        this.isNasal    = phonology.charAt(5) == '+';
        this.isGlide    = phonology.charAt(6) == '+';
        this.isPlosive  = phonology.charAt(7) == '+';
        this.isPause    = phonology.charAt(8) == '+';
        this.sonority   = sonority;
        this.example = example;
    }

    public String name() { return name; }
    public String toString() { return name; }
    public int inherentDuration() { return inherentDuration; }
    public int minimalDuration() { return minimalDuration; }
    public boolean isVowel() { return isVowel; }
    public boolean isSyllabic() { return isSyllabic; }
    public boolean isVoiced() { return isVoiced; }
    public boolean isSonorant() { return isSonorant; }
    public boolean isLiquid() { return isLiquid; }
    public boolean isNasal() { return isNasal; }
    public boolean isGlide() { return isGlide; }
    public boolean isPlosive() { return isPlosive; }
    public boolean isPause() { return isPause; }
    public int sonority() { return sonority; }
    public String getExample() { return example; }

    public boolean isFricative()
    { return !isVowel() && !isSonorant() && !isPlosive(); }
}
