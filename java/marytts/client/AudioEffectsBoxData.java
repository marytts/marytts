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

package marytts.client;

import marytts.util.string.StringUtils;

/**
 * Data for a set of audio effects, i.e. "an audio effects box".
 * 
 * @author Oytun T&uumlrk
 *
 */
public class AudioEffectsBoxData {

    private AudioEffectControlData[] effectControlsData;
    
    //availableEffects is one large string produced by the server in the following format:
    // <EffectSeparator>charEffectSeparator</EffectSeparator>
    // <Effect>
    //   <Name>effect´s name</Name> 
    //   <SampleParam>example parameters string</SampleParam>
    //   <HelpText>help text string</HelpText>
    // </Effect>
    // <Effect>
    //   <Name>effect´s name</effectName> 
    //   <SampleParam>example parameters string</SampleParam>
    //   <HelpText>help text string</HelpText>
    // </Effect>
    // ...
    // <Effect>
    //   <Name>effect´s name</effectName> 
    //   <SampleParam>example parameters string</SampleParam>
    //   <HelpText>help text string</HelpText>
    // </Effect>
    public AudioEffectsBoxData(String availableEffects)
    {
        effectControlsData = null;
        
        if (availableEffects!=null && availableEffects.length()>0)
            parseAvailableEffects(availableEffects);
    }
    
    public AudioEffectControlData getControlData(int index)
    {
        if (effectControlsData!=null && index>=0 && index<effectControlsData.length)
            return effectControlsData[index];
        else
            return null;
    }
        
    public boolean hasEffects()
    {
        if (effectControlsData!=null)
            return true;
        else
            return false;
    }
    
  //Parse the XML-like full effect set string from the server
    protected int parseAvailableEffects(String availableEffects)
    {
        String[] effectLines = StringUtils.toStringArray(availableEffects);
        effectControlsData = new AudioEffectControlData[effectLines.length];
        for (int i=0; i< effectLines.length; i++) {
            String strEffectName, strParams;
            int iSpace = effectLines[i].indexOf(' ');
            if (iSpace != -1) {
                strEffectName = effectLines[i].substring(0, iSpace);
                strParams = effectLines[i].substring(iSpace+1);
            } else { // no params
                strEffectName = effectLines[i];
                strParams = "";
            }
            effectControlsData[i] = new AudioEffectControlData(strEffectName, strParams, null);
        }
        return getTotalEffects();
    }
    
    public int getTotalEffects()
    {
        if (effectControlsData!=null)
            return effectControlsData.length;
        else
            return 0;
    }
}
