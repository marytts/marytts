/**
 * Copyright 2004-2006 DFKI GmbH.
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

package marytts.signalproc.effects;

import java.util.Vector;

/**
 * @author oytun.turk
 *
 */
//A set of audio effects with names, default parameters, and help information
public class AudioEffectsSet {
    public BaseAudioEffect [] effects;
    public int totalEffects;
    
    //Create an effect set from a string like "Whisper(amount=50.0),Robot,FIRFilter"
    public AudioEffectsSet(String strEffectSet)
    {
        effects = null;
        totalEffects = 0;
        
        if (strEffectSet!=null && strEffectSet!="")
        {
            EffectsApplier ef = new EffectsApplier();
            ef.parseEffectsAndParams(strEffectSet);
            if (ef.audioEffects.length>0)
            {
                int ind = 0;
                int i;
                totalEffects = 0;
                for (i=0; i<ef.audioEffects.length; i++)
                {
                    if (ef.audioEffects[i]!=null)
                        totalEffects++;
                }

                effects = new BaseAudioEffect[totalEffects];
                for (i=0; i<ef.audioEffects.length; i++)
                {
                    if (ef.audioEffects[i]!=null)
                        effects[ind++] = new BaseAudioEffect(ef.audioEffects[i]);
                }
            }
        }
    }

    //Create an effect set from a Vector array of Strings
    public AudioEffectsSet(Vector<String> effectSet)
    {
        if (effectSet.size()>0)
        {
            totalEffects = effectSet.size();
            effects = new BaseAudioEffect[totalEffects];
            for (int i=0; i<totalEffects; i++)
            {
                EffectsApplier ef = new EffectsApplier(); //Use it to parse a single effect only
                ef.parseEffectsAndParams((String)effectSet.get(i));
                effects[i] = new BaseAudioEffect(ef.audioEffects[0]);
            }
        }
        else
        {
            effects = null;
            totalEffects = 0;
        }
    }
    
    public String getEffectName(int index)
    {
        if(effects!=null && index>=0 && index<totalEffects)
            return effects[index].getName();
        else 
            return "";
    }

    public String getEffectParams(int index)
    {
       return getEffectParams(index, true);
    }
    
    public String getEffectParams(int index, boolean bWithParantheses)
    {
        if(effects!=null && index>=0 && index<totalEffects)
            return effects[index].getParamsAsString(bWithParantheses);
        else 
            return "";
    }
    
    public String[] getEffectNames()
    {
        String [] strEffectNames = null;
        
        if (totalEffects>0)
        {
            strEffectNames = new String[totalEffects];
            
            for (int i=0; i<totalEffects; i++)
                strEffectNames[i] = effects[i].getName();
        }
        
        return strEffectNames;
    }
    
    public String[] getExampleParams()
    {
        String [] strExampleParams = null;
        
        if (totalEffects>0)
        {
            strExampleParams = new String[totalEffects];
            
            for (int i=0; i<totalEffects; i++)
                strExampleParams[i] = effects[i].getExampleParameters();
        }
        
        return strExampleParams;
    }
    
    public String[] getHelpTexts()
    {
        String [] strHelpTexts = null;
        
        if (totalEffects>0)
        {
            strHelpTexts = new String[totalEffects];
            
            for (int i=0; i<totalEffects; i++)
                strHelpTexts[i] = effects[i].getHelpText();
        }
        
        return strHelpTexts;
    }
                                 
}
