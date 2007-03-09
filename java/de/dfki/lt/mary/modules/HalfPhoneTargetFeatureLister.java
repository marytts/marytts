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
package de.dfki.lt.mary.modules;

import java.util.ArrayList;
import java.util.List;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;

import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.unitselection.HalfPhoneTarget;
import de.dfki.lt.mary.unitselection.Target;

public class HalfPhoneTargetFeatureLister extends TargetFeatureLister 
{

    public HalfPhoneTargetFeatureLister(MaryDataType outputType, String configEntryPrefix)
    {
        super(outputType, configEntryPrefix);
    }

    /**
     * Create the list of targets from the Segments in the utterance.
     * @param segs the Segment relation
     * @return a list of Target objects -- in this case, halfphone targets
     */
    protected List createTargets(Relation segs)
    {
        List targets = new ArrayList();
        for (Item s = segs.getHead(); s != null; s = s.getNext()) {
            String segName = s.getFeatures().getString("name");
            targets.add(new HalfPhoneTarget(segName+"_L", s, true)); // left half
            targets.add(new HalfPhoneTarget(segName+"_R", s, false)); // right half
        }
        return targets;
    }

    /**
     * Create the list of targets from the Segments in the utterance.
     * Make sure that first item is a pause
     * @param segs the Segment relation
     * @return a list of Target objects
     */
    protected List createTargetsWithInitialPause(Relation segs) {
        List targets = new ArrayList();
        boolean first = true;
        Item s = segs.getHead();
        Voice v = FreeTTSVoices.getMaryVoice(s.getUtterance().getVoice());
        String silenceSymbol = v.sampa2voice("_");
        for (; s != null; s = s.getNext()) {
            //create next target
            String segName = s.getFeatures().getString("name");
            Target nextLeftTarget = new HalfPhoneTarget(segName+"_L", s, true); 
            Target nextRightTarget = new HalfPhoneTarget(segName+"_R", s, false);
            //if first target is not a pause, add one
            if (first){
                first = false;
                if (! segName.equals(silenceSymbol)){
                    //System.out.println("Adding two pause targets: "
                      //          +silenceSymbol+"_L and "
                        //        +silenceSymbol+"_R");
                    //build new pause item
                    Item newPauseItem = s.prependItem(null);
                    newPauseItem.getFeatures().setString("name", silenceSymbol);
                    
                    //add new targets for item
                    targets.add(new HalfPhoneTarget(silenceSymbol+"_L", newPauseItem, true)); 
                    targets.add(new HalfPhoneTarget(silenceSymbol+"_R", newPauseItem, false));
                }
            }
            targets.add(nextLeftTarget);
            targets.add(nextRightTarget);
        }        
        return targets;
    }
    
}
