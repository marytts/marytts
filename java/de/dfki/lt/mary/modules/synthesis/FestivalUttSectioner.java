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

import de.dfki.lt.mary.modules.FreeTTS2FestivalUtt;

public class FestivalUttSectioner extends VoiceSectioner
{
    public FestivalUttSectioner(String s, Voice defaultVoice)
    {
        super(s, defaultVoice);
    }

    public VoiceSection nextSection()
    {
        if (pos >= s.length()) return null;
        if (s.startsWith(FreeTTS2FestivalUtt.UTTMARKER, pos)) {
            pos += FreeTTS2FestivalUtt.UTTMARKER.length();
        }
        int n = s.indexOf(FreeTTS2FestivalUtt.UTTMARKER, pos);
        if (n == -1) n = s.length();
        String section = s.substring(pos, n).trim();
        int endline = section.indexOf(System.getProperty("line.separator"));
        if (endline > -1 && section.startsWith(FreeTTS2FestivalUtt.VOICEMARKER)) {
            String voiceName = section.substring(FreeTTS2FestivalUtt.VOICEMARKER.length(), endline);
            Voice newVoice = Voice.getVoice(voiceName);
            if (newVoice == null) {
                logger.warn("Could not find voice named " + voiceName + 
                    ". Using " + currentVoice.getName() + "instead.");
            } else {
                currentVoice = newVoice;
            }
        }
        pos = n;
        logger.debug("Next voice section: voice = "+ currentVoice.getName() + "\n" + section);
        return new VoiceSection(currentVoice, section);
    }
}
