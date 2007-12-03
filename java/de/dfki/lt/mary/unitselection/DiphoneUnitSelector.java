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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.ItemContents;
import com.sun.speech.freetts.Relation;

import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.unitselection.HalfPhoneTarget;
import de.dfki.lt.mary.unitselection.Target;

public class DiphoneUnitSelector extends UnitSelector
{

    /**
     * Initialise the unit selector. Need to call load() separately.
     * @see #load(UnitDatabase)
     */
    public DiphoneUnitSelector() throws Exception
    {
        super();
    }

    /**
     * Create the list of targets from the Segments in the utterance.
     * @param segs the Segment relation
     * @return a list of Target objects -- in this case, diphone targets
     */
    protected List<Target> createTargets(Relation segs)
    {
        List<Target> targets = new ArrayList<Target>();
        String silenceSymbol = "_"; // in sampa

        // Insert an initial silence with duration 0 (needed as context)
        Item initialSilence = new Item(segs, new ItemContents());
        initialSilence.getFeatures().setFloat("end", 0.000f);
        HalfPhoneTarget prev = new HalfPhoneTarget(silenceSymbol+"_R", null, initialSilence, false);
        for (Item s = segs.getHead(); s != null; s = s.getNext()) {
            Element maryxmlElement = (Element) s.getFeatures().getObject("maryxmlElement");
            String segName = s.getFeatures().getString("name");
            String sampa = FreeTTSVoices.getMaryVoice(s.getUtterance().getVoice()).voice2sampa(segName);
            HalfPhoneTarget leftHalfPhone = new HalfPhoneTarget(sampa+"_L", maryxmlElement, s, true); // left half
            HalfPhoneTarget rightHalfPhone = new HalfPhoneTarget(sampa+"_R", maryxmlElement, s, false); // right half
            targets.add(new DiphoneTarget(prev, leftHalfPhone));
            prev = rightHalfPhone;
        }
        // Make sure there is a final silence
        if (!prev.getName().startsWith(silenceSymbol)) {
            // need to append final silence of length 0 (needed as context)
            assert prev.getItem().getFeatures().isPresent("end") : "Item '"+prev.getItem()+"' for target '"+prev.getName()+" has no 'end' feature";
            float prevEnd = prev.getItem().getFeatures().getFloat("end");
            Item finalSilence = new Item(segs, new ItemContents());
            finalSilence.getFeatures().setFloat("end", prevEnd+0.000f); // same end time
            HalfPhoneTarget silence = new HalfPhoneTarget(silenceSymbol+"_L", null, finalSilence, true);
            targets.add(new DiphoneTarget(prev, silence));
        }
        return targets;
    }

}
