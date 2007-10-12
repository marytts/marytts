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

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;

import de.dfki.lt.mary.unitselection.featureprocessors.MaryGenericFeatureProcessors;

public class HalfPhoneTarget extends Target
{
    protected boolean isLeftHalf;


    /**
     * Create a target associated to the given segment item.
     * @param name a name for the target, which may or may not
     * coincide with the segment name.
     * @param item the phone segment item in the Utterance structure,
     * to be associated to this target 
     * @param isLeftHalf true if this target represents the left half
     * of the phone, false if it represents the right half of the phone
     */
    public HalfPhoneTarget(String name, Element maryxmlElement, Item item, boolean isLeftHalf)
    {
        super(name, maryxmlElement, item);
        this.isLeftHalf = isLeftHalf;
    }

    /**
     * Is this target the left half of a phone?
     * @return
     */
    public boolean isLeftHalf()
    {
        return isLeftHalf;
    }
    
    /**
     * Is this target the right half of a phone?
     * @return
     */
    public boolean isRightHalf()
    {
        return !isLeftHalf;
    }
    
}
