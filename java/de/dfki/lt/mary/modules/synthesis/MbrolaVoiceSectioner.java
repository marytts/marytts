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
package de.dfki.lt.mary.modules.synthesis;

import java.util.Locale;

/**
 * A helper class for the synthesis module,
 *          splitting synthesis input data into sections
 *          to be spoken by the same voice.
 *
 * @author Marc Schr&ouml;der
 */

public class MbrolaVoiceSectioner extends VoiceSectioner
{
    public MbrolaVoiceSectioner(String s, Voice defaultVoice)
    {
        super(s, defaultVoice);
    }
    
    public VoiceSection nextSection()
    {
        if (pos >= s.length()) return null;
        int n = pos;
        Voice newVoice = null;
        while (n < s.length() && newVoice == null) {
            n = s.indexOf("voice ", n);
            if (n == -1) n = s.length(); // no more speaker markers
            else { // found a speaker marker
                n += 6; // length of "voice "
                String name = null;
                String gender = null;
                int l = s.indexOf("name=", n);
                if (l != -1) { // OK, voice identified by its name
                    l += 5; // length of "name="
                    // Now find the end of the voice name
                    int m = l;
                    while (m < s.length() && Character.isLetterOrDigit(s.charAt(m))) m++;
                    name = s.substring(l, m);
                    newVoice = Voice.getVoice(name);
                }
                if (newVoice == null) {
                    // no name attribute or no voice of that name found
                    // Try gender
                    int l1 = s.indexOf("gender=", n);
                    if (l1 != -1) { // OK, a gender attribute present
                        l1 += 7; // length of "gender="
                        // now find the end of the gender
                        int m1 = l1;
                        while (m1 < s.length() && Character.isLetterOrDigit(s.charAt(m1))) m1++;
                        gender = s.substring(l1, m1);
                        // Default locale = german
                        Locale locale = Locale.GERMAN;
                        // Re-use locale from current voice, if any:
                        if (currentVoice != null)
                            locale = currentVoice.getLocale();
                        if (gender.equals("female"))
                            newVoice = Voice.getVoice(locale, Voice.FEMALE);
                        else if (gender.equals("male"))
                            newVoice = Voice.getVoice(locale, Voice.MALE);
                    }
                }
                if (newVoice == null) {
                    logger.info("No known voice matches the description" +
                                (name != null ? " name=`" + name + "'" : "") +
                                (gender != null ? " gender=`" + gender + "'" : "") +
                                 ". Using previous voice instead.");
                } else if (newVoice == currentVoice) {
                    // simply ignore
                    newVoice = null;
                }
            }
        }
        if (newVoice == null || newVoice == currentVoice) {
            // no new voice section until end of string
            n = s.length(); // All the rest of s is of the same speaker
        } else {
            // Have a new Voice.
            // From the current position n, search backwards until we find
            // line.separator.
            n = s.lastIndexOf(System.getProperty("line.separator"),
                              n);
            if (n == -1) { // We are actually in the first line
                // Set current voice to new voice, and redo
                currentVoice = newVoice;
                return nextSection(); // iterative call
            } else {
                n += System.getProperty("line.separator").length();
            }
        }
        // Now the part between pos and n is the next section.
        String sec = s.substring(pos, n);
        pos = n;
        VoiceSection newSection = new VoiceSection(currentVoice, sec);
        currentVoice = newVoice;
        return newSection;
    }
}
