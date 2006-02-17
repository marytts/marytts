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

import org.w3c.dom.Element;

import java.util.*;

import com.sun.speech.freetts.Item;

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
    protected Map features2Values = null;
    
    public Target(String name)
    {
        this.name = name;
    }
    
    public Target(String name, Item item)
    {
        this.name = name;
        this.item = item;
    }
    
    public Item getItem() { return item; }
    
    public String getName() { return name; }
    
    public float getTargetDurationInSeconds()
    {
        if (item == null)
            throw new NullPointerException("Target "+name+" does not have an item.");
        if (!item.getFeatures().isPresent("end")) {
            throw new IllegalStateException("Item "+item+" does not have an 'end' feature");
        }
        float end = item.getFeatures().getFloat("end"); 
        Item prev = item.getPrevious();
        if (prev == null) {
            return end;
        } else {
            if (!prev.getFeatures().isPresent("end")) {
                throw new IllegalStateException("Item "+prev+" does not have an 'end' feature");
            }
            float prev_end = prev.getFeatures().getFloat("end");
            return end - prev_end;
        }
    }
    
    public void setFeatureAndValue(String feature, String value)
    {
        if (features2Values == null){
            features2Values = new HashMap();
        }
        features2Values.put(feature,value);
    }
    
    public String getValueForFeature(String feature)
    {
        if (features2Values != null 
                && features2Values.containsKey(feature)){
            return (String) features2Values.get(feature);
        } else {
            return null;
        }
    }
    
    
    public String toString()
    {
        return name +  " " + (item != null ? item.toString() : "");
    }
}
