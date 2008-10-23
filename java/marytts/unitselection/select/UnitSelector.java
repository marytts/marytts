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
package marytts.unitselection.select;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import marytts.datatypes.MaryXML;
import marytts.exceptions.SynthesisException;
import marytts.features.FeatureVector;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.modules.XML2UttAcoustParams;
import marytts.modules.synthesis.FreeTTSVoices;
import marytts.server.Mary;
import marytts.unitselection.data.UnitDatabase;
import marytts.util.dom.MaryDomUtils;
import marytts.util.string.PrintfFormat;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;


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

    
    /**
     * Initialise the unit selector. Need to call load() separately.
     * @see #load(UnitDatabase)
     */
    public UnitSelector() throws Exception
    {
        logger = Logger.getLogger(this.getClass());
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
            marytts.modules.synthesis.Voice voice)
    throws SynthesisException
    {
        long time = System.currentTimeMillis();

        List<Element> segmentsAndBoundaries = new ArrayList<Element>();
        for (Element tOrB : tokensAndBoundaries) {
            if (tOrB.getTagName().equals(MaryXML.BOUNDARY)) {
                segmentsAndBoundaries.add(tOrB);
            } else {
                assert tOrB.getTagName().equals(MaryXML.TOKEN) : "Expected token, got "+tOrB.getTagName();
                NodeList segs = tOrB.getElementsByTagName(MaryXML.PHONE);
                for (int i=0, max=segs.getLength(); i<max; i++) {
                    segmentsAndBoundaries.add((Element)segs.item(i));
                }
            }
        }

        List<Target> targets = createTargets(segmentsAndBoundaries);
        // compute target features for each target in the chain
        TargetCostFunction tcf = database.getTargetCostFunction();
        for (Target target : targets) {
            tcf.computeTargetFeatures(target);
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
     * Create the list of targets from the XML elements to synthesize.
     * @param segmentsAndBoundaries a list of MaryXML phone and boundary elements
     * @return a list of Target objects
     */
    protected List<Target> createTargets(List<Element> segmentsAndBoundaries)
    {
        List<Target> targets = new ArrayList<Target>();
        for (Element sOrB : segmentsAndBoundaries) {
            String phone = getPhoneSymbol(sOrB);
            targets.add(new Target(phone, sOrB));
        }
        return targets;
    }
    
    public static String getPhoneSymbol(Element segmentOrBoundary)
    {
        String phone;
        if (segmentOrBoundary.getTagName().equals(MaryXML.PHONE)) {
            phone = segmentOrBoundary.getAttribute("p");
        } else {
            assert segmentOrBoundary.getTagName().equals(MaryXML.BOUNDARY) 
                : "Expected boundary element, but got "+segmentOrBoundary.getTagName();
            // TODO: how can we know the silence symbol here?
            phone = "_";
        }
        return phone;
    }
    

 
}