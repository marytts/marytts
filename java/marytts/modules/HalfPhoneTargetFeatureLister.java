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
package marytts.modules;

import java.util.ArrayList;
import java.util.List;

import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.features.TargetFeatureComputer;
import marytts.modules.synthesis.Voice;
import marytts.unitselection.select.HalfPhoneTarget;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.UnitSelector;

import org.w3c.dom.Element;


public class HalfPhoneTargetFeatureLister extends TargetFeatureLister 
{

    public HalfPhoneTargetFeatureLister()
    {
        super(MaryDataType.HALFPHONE_TARGETFEATURES);
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
     * Access the code from within the our own code so that a subclass
     * can override it. Use this rather than the public static method in local code.
     * @param segs
     * @return
     */
    protected List<Target> overridableCreateTargetsWithPauses(List<Element> segmentsAndBoundaries)
    {
        return HalfPhoneTargetFeatureLister.createTargetsWithPauses(segmentsAndBoundaries);
    }
    
    /**
     * Create the list of targets from the segments to be synthesized
     * Prepend and append pauses if necessary
     * @param segmentsAndBoundaries a list of MaryXML phone and boundary elements
     * @return a list of Target objects
     */
    public static List<Target> createTargetsWithPauses(List<Element> segmentsAndBoundaries) {
        List<Target> targets = new ArrayList<Target>();
        if (segmentsAndBoundaries.size() == 0) return targets;
        // TODO: how can we know the silence symbol here?
        String silenceSymbol = "_";
        Element first = segmentsAndBoundaries.get(0);
        if (!first.getTagName().equals(MaryXML.BOUNDARY)) {
            // need to insert a dummy silence target
            targets.add(new HalfPhoneTarget(silenceSymbol+"_L", null, true));
            targets.add(new HalfPhoneTarget(silenceSymbol+"_R", null, false));
        }
        for (Element sOrB : segmentsAndBoundaries) {
            String phone = UnitSelector.getPhoneSymbol(sOrB);
            targets.add(new HalfPhoneTarget(phone+"_L", sOrB, true));
            targets.add(new HalfPhoneTarget(phone+"_R", sOrB, false));
        }
        if (!targets.get(targets.size()-1).isSilence()) {
            targets.add(new HalfPhoneTarget(silenceSymbol+"_L", null, true));
            targets.add(new HalfPhoneTarget(silenceSymbol+"_R", null, false));
        }
        return targets;
    }
    
}
