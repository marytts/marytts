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

import de.dfki.lt.mary.modules.phonemiser.Phoneme;
import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;

public class DiphoneTarget extends Target {
    protected HalfPhoneTarget left;
    protected HalfPhoneTarget right;
    
    public DiphoneTarget(HalfPhoneTarget left, HalfPhoneTarget right)
    {
        super(null, null, null);
        this.name = left.name.substring(0, left.name.lastIndexOf("_"))
            + "-" + right.name.substring(0, right.name.lastIndexOf("_"));
        assert left.isRightHalf(); // the left half of this diphone must be the right half of a phone
        assert right.isLeftHalf();
        this.left = left;
        this.right = right;
    }
    
    public HalfPhoneTarget getLeft()
    {
        return left;
    }

    public HalfPhoneTarget getRight()
    {
        return right;
    }

    public Item getItem()
    {
        throw new IllegalStateException("This method should not be called for DiphoneTargets.");
    }
    
    
    public FeatureVector getFeatureVector()
    {
        throw new IllegalStateException("This method should not be called for DiphoneTargets.");
    }
    
    public void setFeatureVector(FeatureVector featureVector)
    {
        throw new IllegalStateException("This method should not be called for DiphoneTargets.");
    }
    
    public float getTargetDurationInSeconds()
    {
        throw new IllegalStateException("This method should not be called for DiphoneTargets.");
    }
    
    /**
     * Determine whether this target is a silence target
     * @return true if the target represents silence, false otherwise
     */
    public boolean isSilence()
    {
        throw new IllegalStateException("This method should not be called for DiphoneTargets.");
    }
    
    public Phoneme getSampaPhoneme()
    {
        throw new IllegalStateException("This method should not be called for DiphoneTargets.");
    }
    

}
