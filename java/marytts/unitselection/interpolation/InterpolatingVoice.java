/**
 * Copyright 2007 DFKI GmbH.
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

package marytts.unitselection.interpolation;

import java.util.Locale;

import javax.sound.sampled.AudioFormat;

import marytts.exceptions.MaryConfigurationException;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.synthesis.Voice;


/**
 * @author marc
 *
 */
public class InterpolatingVoice extends Voice {

    public static boolean isInterpolatingVoiceName(String name)
    {
        if (name == null) return false;
        String[] parts = name.split("\\s+");
        if (parts.length != 4) return false;
        if (!parts[1].equals("with")) return false;
        if (!parts[2].endsWith("%")) return false;
        int percent;
        try {
            percent = Integer.parseInt(parts[2].substring(0, parts[2].length()-1));
        } catch (NumberFormatException nfe) {
            return false;
        }
        if (Voice.getVoice(parts[0]) == null) return false;
        if (Voice.getVoice(parts[3]) == null) return false;
        return true;
    }

    
    protected Voice firstVoice = null;
    
    public InterpolatingVoice(InterpolatingSynthesizer is, String name)
    throws MaryConfigurationException
    {
        super(new String[] {name}, null, null, is, null);
        if (isInterpolatingVoiceName(name)) {
            String[] parts = name.split("\\s+");
            firstVoice = Voice.getVoice(parts[0]);
        }
    }
    
    /**
     * Determine whether this voice has the given name. For the InterpolatingVoice,
     * the meaning of the name is different from a "normal" voice. It is a specification
     * of how to interpolate two voices. The syntax is:
     * <br/>
     * <code>voice1 with XY% voice2</code><br/>
     * <br/>
     * where voice1 and voice2 must be existing voices, and XY is an integer between 0 and 100.
     * @return true if name matches the specification, false otherwise
     */
    /*
    public boolean hasName(String name)
    {
        if (name == null) return false;
        String[] parts = name.split("\\s+");
        if (parts.length != 4) return false;
        if (!parts[1].equals("with")) return false;
        if (!parts[2].endsWith("%")) return false;
        int percent;
        try {
            percent = Integer.parseInt(parts[2].substring(0, parts[2].length()-1));
        } catch (NumberFormatException nfe) {
            return false;
        }
        if (Voice.getVoice(parts[0]) == null) return false;
        if (Voice.getVoice(parts[3]) == null) return false;
        return true;
    }
*/

    // Forward most of the public methods which are meaningful in a unit selection context to firstVoice:
    
    public AllophoneSet getAllophoneSet()
    {
        if (firstVoice == null) return null;
        return firstVoice.getAllophoneSet();
    }

    public Allophone getAllophone(String phoneSymbol)
    {
        if (firstVoice == null) return null;
        return firstVoice.getAllophone(phoneSymbol);
    }
    

    public Locale getLocale()
    {
        if (firstVoice == null) return null;
        return firstVoice.getLocale();
    }
    public AudioFormat dbAudioFormat() {
        if (firstVoice == null) return null;
        return firstVoice.dbAudioFormat();
    }

    public Gender gender()
    {
        if (firstVoice == null) return null;
        return firstVoice.gender();
    }


}
