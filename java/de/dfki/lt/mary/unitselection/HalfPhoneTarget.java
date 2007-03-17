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
    public HalfPhoneTarget(String name, Item item, boolean isLeftHalf)
    {
        super(name, item);
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
    
    public float getTargetDurationInSeconds()
    {
        return super.getTargetDurationInSeconds() / 2;
    }
    
    public float getTargetF0InHz()
    {
        if (f0 != -1){
            return f0;
        } else {
        if (item == null)
            throw new NullPointerException("Target "+name+" does not have an item.");

        // System.out.println("Looking for pitch...");
        // get mid position of segment
        float mid;
        float end = item.getFeatures().getFloat("end");
        Item prev = item.getPrevious();
        float prev_end;
        if (prev == null) {
            prev_end = 0;
        } else {
            prev_end = prev.getFeatures().getFloat("end");
        }
        mid = prev_end + (end - prev_end) / 2;
        float mymid;
        if (isLeftHalf()) {
            mymid = prev_end + (mid - prev_end) / 2;
        } else {
            mymid = mid + (end - mid) / 2;
        }
            
        Relation targetRelation = item.getUtterance().getRelation("Target");
        // if segment has no target relation, you can not calculate
        // the segment pitch
        if (targetRelation == null) {
            return 0;
        }
        // get F0 and position of previous and next target
        Item nextTargetItem = targetRelation.getHead();
        while (nextTargetItem != null
                && nextTargetItem.getFeatures().getFloat("pos") < mid) {
            nextTargetItem = nextTargetItem.getNext();
        }
        if (nextTargetItem == null)
            return 0;
        Item lastTargetItem = nextTargetItem.getPrevious();
        if (lastTargetItem == null)
            return 0;
        float lastF0 = lastTargetItem.getFeatures().getFloat("f0");
        float lastPos = lastTargetItem.getFeatures().getFloat("pos");
        float nextF0 = nextTargetItem.getFeatures().getFloat("f0");
        float nextPos = nextTargetItem.getFeatures().getFloat("pos");
        assert lastPos <= mid && mid <= nextPos;
        // build a linear function (f(x) = slope*x+intersectionYAxis)
        float slope = (nextF0 - lastF0) / (nextPos - lastPos);
        // calculate the pitch
        f0 = lastF0 + slope * (mid - lastPos);
        assert lastF0 <= f0 && f0 <= nextF0 || nextF0 <= f0 && f0 <= lastF0;

        if (Float.isNaN(f0)) {
            f0 = (float) 0.0;
        }
        return f0;
        }
    }


    
}
