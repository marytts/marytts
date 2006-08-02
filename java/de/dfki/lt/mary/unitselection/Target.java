/**
 * Copyright 2006 DFKI GmbH.
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
package de.dfki.lt.mary.unitselection;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.sun.speech.freetts.Item;

import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.modules.phonemiser.Phoneme;

/**
 * A representation of a target representing the ideal properties of
 * a unit in a target utterance.
 * @author Marc Schr&ouml;der
 *
 */
public class Target
{
    protected String name;
    protected Element maryxmlElement;
    protected Item item;
    //a map containing this targets features
    protected String[] byteVals = null;
    protected String[] shortVals = null;
    protected float[] floatVals = null;
    protected float end = -1;
    protected float duration = -1;
    protected Logger logger;
    protected int unitSize;
    protected int isSilence =-1;
        
    public Target(String name)
    {
        logger = Logger.getLogger("Target");
        this.name = name;
    }
    
    public Target(String name, Item item, int unitSize)
    {
        logger = Logger.getLogger("Target");
        this.name = name;
        this.item = item;
        this.unitSize = unitSize;
    }
    
    public int getUnitSize() { return unitSize; }
    
    public Item getItem() { return item; }
    
    public String getName() { return name; }
    
    public float getTargetDurationInSeconds()
    {
        if (duration != -1){
            return duration;
        } else {
        if (item == null)
            throw new NullPointerException("Target "+name+" does not have an item.");
        if (!item.getFeatures().isPresent("end")) {
            throw new IllegalStateException("Item "+item+" does not have an 'end' feature");
        }
        end = item.getFeatures().getFloat("end"); 
        Item prev = item.getPrevious();
        if (prev == null) {
            duration = end;
        } else {
            if (!prev.getFeatures().isPresent("end")) {
                throw new IllegalStateException("Item "+prev+" does not have an 'end' feature");
            }
            float prev_end = prev.getFeatures().getFloat("end");
            duration = end - prev_end;
        }
        if (unitSize == UnitDatabase.HALFPHONE){
           duration = duration/2; 
        }
        return duration;
        }
    }
    
    /**
     * Add value and features to the featuresMap
     * @param feature the feature
     * @param value the value
     */
    public void addByteValue(String val, int numVals, int pos)
    {
        if (byteVals == null){
            byteVals = new String[numVals];
        }
        byteVals[pos] = val;
    }
    
    /**
     * Add value and features to the featuresMap
     * @param feature the feature
     * @param value the value
     */
    public void addShortValue(String val, int numVals, int pos)
    {
        if (shortVals == null){
            shortVals = new String[numVals];
        }
        shortVals[pos] = val;
    }
    
    /**
     * Add value and features to the featuresMap
     * @param feature the feature
     * @param value the value
     */
    public void addFloatValue(float val, int numVals, int pos)
    {
        if (floatVals == null){
            floatVals = new float[numVals];
        }
        floatVals[pos] = val;
    }
    
    
    /**
     * Get the value for a feature if present
     * @param feature the feature
     * @return the value
     */
    public String getValueForByteFeature(int index)
    {
        if (byteVals != null && 
                (index>-1 || index < byteVals.length)){
            return byteVals[index];
    	} else {
    	    return null;
    	}
    }
    
    /**
     * Get the value for a feature if present
     * @param feature the feature
     * @return the value
     */
    public String getValueForShortFeature(int index)
    {
        if (shortVals != null && 
                (index>-1 || index < byteVals.length)){
            return shortVals[index];
    	} else {
    	    return null;
    	}
    }
    /**
     * Get the value for a feature if present
     * @param feature the feature
     * @return the value
     */
    public float getValueForFloatFeature(int index)
    {
        if (floatVals != null && 
                (index>-1 || index < byteVals.length)){
            return floatVals[index];
    	} else {
    	    return -1;
    	}
    }
    
    
    
    /**
     * Determine whether this target is a silence target
     * @return true if the target represents silence, false otherwise
     */
    public boolean isSilence()
    {
        
        if (isSilence == -1) {
        
            if (item != null) {
                Voice v = FreeTTSVoices.getMaryVoice(item.getUtterance().getVoice());
                String silenceSymbol = v.sampa2voice("_");
                if (name.equals(silenceSymbol)) {
                    isSilence = 1; //true
                } else {
                    isSilence = 0; //false
                }
            } else { // compare to typical silence symbol names
                if (name.equals("pau") || name.equals("_")) {
                    isSilence = 1; //true
                } else {
                    isSilence = 0; //false
                }
            }
        }
        if (isSilence == 1){
            return true;
        } else {
            return false;
        }
       
        
    }
    
    public Phoneme getSampaPhoneme()
    {
        if (item != null) {
            Voice v = FreeTTSVoices.getMaryVoice(item.getUtterance().getVoice());
            return v.getSampaPhoneme(v.voice2sampa(item.toString()));
        }
        return null;
    }
    
    
    public String toString()
    {
        return name +  " " + (item != null ? item.toString() : "");
    }
}
