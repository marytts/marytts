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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;

import de.dfki.lt.mary.Mary;
import de.dfki.lt.mary.modules.MaryModule;
import de.dfki.lt.mary.modules.XML2UttAcoustParams;
import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;
import de.dfki.lt.mary.modules.synthesis.SynthesisException;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.unitselection.viterbi.Viterbi;
import de.dfki.lt.util.PrintfFormat;

/**
 * Selects the units for an utterance
 * 
 * @author Marc Schr&ouml;der
 *
 */
public class UnitSelector
{
    protected UnitDatabase database;
    protected Logger logger;
    protected float targetCostWeight;
    
    private XML2UttAcoustParams x2u;

    
    /**
     * Initialise the unit selector. Need to call load() separately.
     * @see #load(UnitDatabase)
     */
    public UnitSelector() throws Exception
    {
        logger = Logger.getLogger(this.getClass());
        
        // Try to get instances of our tools from Mary; if we cannot get them,
        // instantiate new objects.
        try {
            x2u = (XML2UttAcoustParams) Mary.getModule(XML2UttAcoustParams.class);
        }catch(NullPointerException npe){
            x2u = null;
        }
        if (x2u == null) {
            logger.info("Starting my own XML2UttAcoustParams");
            x2u = new XML2UttAcoustParams();
            x2u.startup();
        } else if (x2u.getState() == MaryModule.MODULE_OFFLINE) {
            x2u.startup();
        }

    }
    
    public void load(UnitDatabase unitDatabase, float targetCostWeight)
    {
        this.database = unitDatabase;
        this.targetCostWeight = targetCostWeight;
    }
    
    /**
     * Select the units for the targets in the given 
     * list of tokens and boundaries. Collect them in a list and return it.
     * 
     * @param tokensAndBoundaries the token and boundary MaryXML elements representing
     * an utterance.
     * @param voice the voice with which to synthesize
     * @param db the database of the voice
     * @param unitNamer a unitNamer
     * @return a list of SelectedUnit objects
     * @throws IllegalStateException if no path for generating the target utterance
     * could be found
     */
    public List<SelectedUnit> selectUnits(List<Element> tokensAndBoundaries,
            de.dfki.lt.mary.modules.synthesis.Voice voice)
    throws SynthesisException
    {
        long time = System.currentTimeMillis();
        Utterance utt = x2u.convert(tokensAndBoundaries, voice);
        if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            utt.dump(pw, 2, this.getClass().getName(), true); // padding, justRelations
            logger.debug("Input to unit selection from voice "+voice.getName()+":\n"+sw.toString());
        }

        // Create target chain for the utterance
        Relation segs = utt.getRelation(Relation.SEGMENT);
        List<Target> targets = createTargets(segs);
        // compute target features for each target in the chain
        for (Target target : targets) {
            database.getTargetCostFunction().computeTargetFeatures(target);
        }

        //Select the best candidates using Viterbi and the join cost function.
        Viterbi viterbi = new Viterbi(targets, database, targetCostWeight);
        viterbi.apply();
        List<SelectedUnit> selectedUnits = viterbi.getSelectedUnits();
        // If you can not associate the candidate units in the best path 
        // with the items in the segment relation, there is no best path
        if (selectedUnits == null) {
            throw new IllegalStateException("Viterbi: can't find path");
        }
        long newtime = System.currentTimeMillis() - time;
        logger.debug("Selection took "+newtime+" milliseconds");
        return selectedUnits;
    }
    
    /**
     * Create the list of targets from the Segments in the utterance.
     * @param segs the Segment relation
     * @return a list of Target objects
     */
    protected List<Target> createTargets(Relation segs)
    {
        List<Target> targets = new ArrayList<Target>();
        for (Item s = segs.getHead(); s != null; s = s.getNext()) {
            Element maryxmlElement = (Element) s.getFeatures().getObject("maryxmlElement");
            String segName = s.getFeatures().getString("name");
            String sampa = FreeTTSVoices.getMaryVoice(s.getUtterance().getVoice()).voice2sampa(segName);
            targets.add(new Target(sampa, maryxmlElement, s));
        }
        return targets;
    }
    

 
}