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

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.sun.speech.freetts.Item;

import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.phonemiser.Phoneme;
import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.unitselection.featureprocessors.MaryGenericFeatureProcessors;
import de.dfki.lt.mary.util.dom.MaryDomUtils;

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
    
    protected FeatureVector featureVector = null;
    
    protected float duration = -1;
    protected float f0 = -1;
    protected Logger logger;
    protected int isSilence =-1;

    /**
     * Create a target associated to the given segment item.
     * @param name a name for the target, which may or may not
     * coincide with the segment name.
     * @param item the phone segment item in the Utterance structure,
     * to be associated to this target 
     */
    public Target(String name, Item item)
    {
        this(name, null, item);
    }
    
    /**
     * Create a target associated to the given element in the MaryXML tree.
     * @param name a name for the target, which may or may not
     * coincide with the segment name.
     * @param maryxmlElement the phone or boundary element in the MaryXML tree
     * to be associated with this target.
     */
    public Target(String name, Element maryxmlElement)
    {
        this(name, maryxmlElement, null);
    }
    
    /**
     * Create a target associated to the given element in the MaryXML tree and with
     * the given segment item.
     * @param name a name for the target, which may or may not
     * coincide with the segment name.
     * @param maryxmlElement the phone or boundary element in the MaryXML tree
     * to be associated with this target.
     * @param item the phone segment item in the Utterance structure,
     * to be associated to this target 
     */
    public Target(String name, Element maryxmlElement, Item item)
    {
        logger = Logger.getLogger("Target");
        this.name = name;
        this.maryxmlElement = maryxmlElement;
        this.item = item;
    }
    
    public Element getMaryxmlElement() { return maryxmlElement; }
    
    public Item getItem() { return item; }
    
    public String getName() { return name; }
    
    public FeatureVector getFeatureVector() { return featureVector; }
    
    public void setFeatureVector(FeatureVector featureVector)
    {
        this.featureVector = featureVector;
    }
    
    public float getTargetDurationInSeconds()
    {
        if (duration != -1){
            return duration;
        } else {
        if (item == null)
            throw new NullPointerException("Target "+name+" does not have an item.");
        duration = new MaryGenericFeatureProcessors.UnitDuration().process(this);
        return duration;
        }
    }
    
    public float getTargetF0InHz()
    {
        if (f0 != -1){
            return f0;
        } else {
        if (item == null)
            throw new NullPointerException("Target "+name+" does not have an item.");
        float logf0 = new MaryGenericFeatureProcessors.UnitLogF0().process(this);
        if (logf0 == 0) f0 = 0;
        else f0 = (float) Math.exp(logf0);
        return f0;
        }
    }

    
    
    
    /**
     * Determine whether this target is a silence target
     * @return true if the target represents silence, false otherwise
     */
    public boolean isSilence()
    {
        
        if (isSilence == -1) {
            String silenceSymbol = "_"; // we now use SAMPA in all of our voices
            if (name.startsWith(silenceSymbol)) {
                isSilence = 1; //true
            } else {
                silenceSymbol = "pau"; // try MRPA just in case
                if (name.startsWith(silenceSymbol)) {
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
        } else if (maryxmlElement != null) {
            Element voiceElement = (Element) MaryDomUtils.getAncestor(maryxmlElement, MaryXML.VOICE);
            if (voiceElement != null) {
                Voice v = Voice.getVoice(voiceElement);
                if (v != null) {
                    String sampa;
                    if (maryxmlElement.getNodeName().equals(MaryXML.PHONE)) {
                        sampa = maryxmlElement.getAttribute("p");
                    } else {
                        assert maryxmlElement.getNodeName().equals(MaryXML.BOUNDARY);
                        sampa = "_";
                    }
                    return v.getSampaPhoneme(sampa);
                }
            }
        }
        return null;
    }
    
    
    public String toString()
    {
        return name;
    }
}
