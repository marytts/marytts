/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.signalproc.effects;

import java.util.Vector;

/**
 * @author Oytun T&uumlrk
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

