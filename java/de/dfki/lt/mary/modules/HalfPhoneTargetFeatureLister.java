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

import org.w3c.dom.Element;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;

import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.unitselection.HalfPhoneTarget;
import de.dfki.lt.mary.unitselection.Target;
import de.dfki.lt.mary.unitselection.featureprocessors.TargetFeatureComputer;

public class HalfPhoneTargetFeatureLister extends TargetFeatureLister 
{

    public HalfPhoneTargetFeatureLister()
    {
        super(MaryDataType.get("HALFPHONE_TARGETFEATURES"));
    }
    
    /**
     * Get the appropriate target feature computer for this target feature lister from voice
     * @param v
     * @return
     */
    protected TargetFeatureComputer getTargetFeatureComputer(Voice v)
    {
        return v.getHalfphoneTargetFeatureComputer();
    }


    /**
     * Create the list of targets from the Segments in the utterance.
     * @param segs the Segment relation
     * @return a list of Target objects -- in this case, halfphone targets
     */
    protected List<Target> createTargets(Relation segs)
    {
        List<Target> targets = new ArrayList<Target>();
        for (Item s = segs.getHead(); s != null; s = s.getNext()) {
            Element maryxmlElement = (Element) s.getFeatures().getObject("maryxmlElement");
            String segName = s.getFeatures().getString("name");
            targets.add(new HalfPhoneTarget(segName+"_L", maryxmlElement, s, true)); // left half
            targets.add(new HalfPhoneTarget(segName+"_R", maryxmlElement, s, false)); // right half
        }
        return targets;
    }

    
    /**
     * Access the code from within the our own code so that a subclass
     * can override it. Use this rather than the public static method in local code.
     * @param segs
     * @return
     */
    protected List<Target> overridableCreateTargetsWithPauses(Relation segs)
    {
        return HalfPhoneTargetFeatureLister.createTargetsWithPauses(segs);
    }

    /**
     * Create the list of targets from the Segments in the utterance.
     * Make sure that first item is a pause
     * @param segs the Segment relation
     * @return a list of Target objects
     */
    public static List<Target> createTargetsWithPauses(Relation segs) {
        List<Target> targets = new ArrayList<Target>();

        boolean first = true;
        Item s = segs.getHead();
        Voice v = FreeTTSVoices.getMaryVoice(s.getUtterance().getVoice());
        String silenceSymbol = v.sampa2voice("_");
        Target lastTarget = null;
        Item lastItem = s;
        for (; s != null; s = s.getNext()) {
            Element maryxmlElement = (Element) s.getFeatures().getObject("maryxmlElement");
            //create next target
            String segName = s.getFeatures().getString("name");
            Target nextLeftTarget = new HalfPhoneTarget(segName+"_L", maryxmlElement, s, true); 
            Target nextRightTarget = new HalfPhoneTarget(segName+"_R", maryxmlElement, s, false);
            //if first target is not a pause, add one
            if (first){
                first = false;
                //if (! segName.equals(silenceSymbol)){
                if (! nextLeftTarget.isSilence()){
                    //System.out.println("Adding two pause targets: "
                      //          +silenceSymbol+"_L and "
                        //        +silenceSymbol+"_R");
                    //build new pause item
                    Item newPauseItem = s.prependItem(null);
                    newPauseItem.getFeatures().setString("name", silenceSymbol);
                    
                    //add new targets for item
                    targets.add(new HalfPhoneTarget(silenceSymbol+"_L", null, newPauseItem, true)); 
                    targets.add(new HalfPhoneTarget(silenceSymbol+"_R", null, newPauseItem, false));
                }
            }
            targets.add(nextLeftTarget);
            targets.add(nextRightTarget);
            lastTarget = nextRightTarget;
            lastItem = s;
        }  
        if (! lastTarget.isSilence()){
                   //System.out.println("Adding pause target "
                     //           +silenceSymbol);
                   //build new pause item
                   Item newPauseItem = lastItem.appendItem(null);
                   newPauseItem.getFeatures().setString("name", silenceSymbol);
                   
                   //add new targets for item
                    targets.add(new HalfPhoneTarget(silenceSymbol+"_L", null, newPauseItem, true)); 
                    targets.add(new HalfPhoneTarget(silenceSymbol+"_R", null, newPauseItem, false));
                }
        return targets;
    }
    
}
