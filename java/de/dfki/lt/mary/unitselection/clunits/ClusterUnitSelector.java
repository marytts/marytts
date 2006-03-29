/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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
package de.dfki.lt.mary.unitselection.clunits;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;

import de.dfki.lt.mary.Mary;
import de.dfki.lt.mary.modules.MaryModule;
import de.dfki.lt.mary.modules.XML2UttAcoustParams;
import de.dfki.lt.mary.unitselection.*;
import de.dfki.lt.mary.unitselection.viterbi.Viterbi;
import de.dfki.lt.mary.unitselection.viterbi.ViterbiCandidate;
import de.dfki.lt.mary.unitselection.clunits.ClusterUnit;
import de.dfki.lt.freetts.ClusterUnitNamer;
import de.dfki.lt.mary.unitselection.cart.PathExtractor;
import de.dfki.lt.mary.unitselection.cart.PathExtractorImpl;
import de.dfki.lt.mary.unitselection.cart.CART;

import com.sun.speech.freetts.Item;

import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;
import com.sun.speech.freetts.Voice;

public class ClusterUnitSelector extends UnitSelector
{
    private XML2UttAcoustParams x2u;

    private ClusterUnitDatabase database;
    private ClusterUnitNamer unitNamer;
    private final static PathExtractor DNAME = new PathExtractorImpl(
    	    "R:SylStructure.parent.parent.name", true);
    
    private int unitSize;
    
    /**
     * Initialise the unit selector with the given cost functions. 
     * If they are null, a default cost function will be used 
     * which always reports cost 0.
     * @param targetCostFunction
     * @param joinCostFunction
     */
    public ClusterUnitSelector(TargetCostFunction targetCostFunction, 
            			JoinCostFunction joinCostFunction)
    throws Exception
    {
        super(targetCostFunction, joinCostFunction);
        
        // Try to get instances of our tools from Mary; if we cannot get them,
        // instantiate new objects.
        x2u = (XML2UttAcoustParams) Mary.getModule(XML2UttAcoustParams.class);
        if (x2u == null) {
            logger.info("Starting my own XML2UttAcoustParams");
            x2u = new XML2UttAcoustParams();
            x2u.startup();
        } else if (x2u.getState() == MaryModule.MODULE_OFFLINE) {
            x2u.startup();
        }
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
    public List selectUnits(List tokensAndBoundaries,
            de.dfki.lt.mary.modules.synthesis.Voice voice,
            UnitDatabase db, 
            ClusterUnitNamer unitNamer)
    {
        Utterance utt = x2u.convert(tokensAndBoundaries, voice);
        if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            utt.dump(pw, 2, this.getClass().getName(), true); // padding, justRelations
            logger.debug("Input to unit selection from voice "+voice.getName()+":\n"+sw.toString());
        }

        this.database = (ClusterUnitDatabase) db;
        unitSize = database.getUnitSize();
        this.unitNamer = unitNamer;
        ((PathExtractorImpl)DNAME).setFeatureProcessors(database.getFeatProcManager());
        ((ClusterJoinCostFunction) joinCostFunction).setDatabase(database);
        Relation segs = utt.getRelation(Relation.SEGMENT);
        List targets = new ArrayList();
        for (Item s = segs.getHead(); s != null; s = s.getNext()) {
            setUnitName(s);       
            if (unitSize == UnitDatabase.HALFPHONE){
                targets.add(new Target(s.getFeatures().getString("clunit_name")+"left", s, unitSize));
                targets.add(new Target(s.getFeatures().getString("clunit_name")+"right", s, unitSize));
            } else {
                targets.add(new Target(s.getFeatures().getString("clunit_name"), s, unitSize));
            }
        }
        //Select the best candidates using Viterbi and the join cost function.
        Viterbi viterbi = new Viterbi(targets, database, this, targetCostFunction, joinCostFunction);
        viterbi.apply();
        // If you can not associate the candidate units in the best path 
        // with the items in the segment relation, there is no best path
    	List selectedUnits = viterbi.getSelectedUnits();
        if (selectedUnits == null) {
    	    throw new IllegalStateException("clunits: can't find path");
    	}
        if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            for (int i=0; i<selectedUnits.size(); i++) {
                SelectedUnit selUnit = 
                    (SelectedUnit)selectedUnits.get(i);
                pw.println(selUnit.toString());
                UnitOriginInfo origin = 
                    ((ClusterUnit) selUnit.getUnit()).getOriginInfo();
                if (origin != null){
                    
                    origin.printInfo(pw);
                } else {
                    logger.debug("No origin info for selected unit ");
                    }
            }
            logger.debug("Selected units:\n"+sw.toString());
        }
        return selectedUnits;
    }
    
    /**
     * Sets the cluster unit name given the segment.
     *
     * @param seg the segment item that gets the name
     */
    protected void setUnitName(Item seg) {
        //general domain should have a unitNamer
        if (unitNamer != null) {
            
            unitNamer.setUnitName(seg);
            return;
        }
        //restricted domain may not have a namer
        // default to LDOM naming scheme 'ae_afternoon':
        String cname = null;

        String segName = seg.getFeatures().getString("name");

        Voice voice = seg.getUtterance().getVoice();
        String silenceSymbol = voice.getPhoneFeature("silence", "symbol");
        if (silenceSymbol == null)
            silenceSymbol = "pau";
        if (segName.equals(silenceSymbol)) {
            cname = silenceSymbol + "_" + seg.findFeature("p.name");
        } else {
            // remove single quotes from name
            String dname = ((String) DNAME.findFeature(seg)).toLowerCase();
            cname = segName + "_" + stripQuotes(dname);
        }
        seg.getFeatures().setString("clunit_name", cname);
    }
    
    /**
     * Strips quotes from the given string.
     *
     * @param s the string to strip quotes from
     *
     * @return a string with all single quotes removed
     */
    private String stripQuotes(String s) {
	StringBuffer sb = new StringBuffer(s.length());
	for (int i = 0; i < s.length(); i++) {
	    char c = s.charAt(i);
	    if (c != '\'') {
		sb.append(c);
	    }
	}
	return sb.toString();
    }
    
    /**
     * Get the candidates for a given target
     * 
     *@param target the target
     *@return the head of the candidate queue
     */
    public ViterbiCandidate getCandidates(Target target){
        //logger.debug("Looking for candidates in cart "+target.getName());
        CART cart = database.getTree(target.getName());
    	// Here, the unit candidates are selected.
    	int[] clist = (int[]) cart.interpret(target.getItem());
    	
    	// Now, clist is a List of instance numbers for the units of type
        // unitType that belong to the best cluster according to the CART.
        
	    ViterbiCandidate candidate;
	    ViterbiCandidate first;
	
	    first = null;
	    for (int i = 0; i < clist.length; i++) {
	        candidate = new ViterbiCandidate();
	        candidate.setNext(first); // link them reversely: the first in clist will be at the end of the queue
	        candidate.setTarget(target); // The item is the same for all these candidates in the queue
	        // remember the actual unit:
	        int unitIndex = (int) clist[i];
	        
	        logger.debug("For candidate "+i+" setting unit "+database.getUnit(target.getName(), unitIndex));
	        candidate.setUnit(database.getUnit(target.getName(), unitIndex));
	        first = candidate;
	        
	    }
	
        // Take into account candidates for previous item?
        // Depending on the setting of EXTEND_SELECTIONS in the database,
        // look the first candidates for the preceding item,
        // and add the units following these (which are not yet candidates)
        // as candidates. EXTEND_SELECTIONS indicates how many of these
        // are added. A high setting will add candidates which don't fit the
        // target well, but which can be smoothly concatenated with the context.
        // In a sense, this means trading target costs against join costs.
	    if (database.getExtendSelections() > 0 &&
	        target.getItem().getPrevious() != null) {
            // Get the candidates for the preceding (segment) item
	        ViterbiCandidate precedingCandidate = 
		           (ViterbiCandidate) (target.getItem().getPrevious().getFeatures().getObject("clunit_cands"));
	        for (int e = 0; precedingCandidate!= null && 
		        (e < database.getExtendSelections());
		         precedingCandidate = precedingCandidate.getNext()) {
	            ClusterUnit nextClusterUnit = ((ClusterUnit)precedingCandidate.getUnit());
		        if(nextClusterUnit == null){
		            continue;}
	            Unit nextUnit = nextClusterUnit.getNext();
	            
		        if (nextUnit == null) {
                    continue;
		        }
		        // Look through the list of candidates for the current item:
		        // if this loop is not aborted, gt=null at the end of the loop
		        for (candidate = first; candidate != null; candidate = candidate.getNext()) {
		            //if the candidates unit is the same as nextUnitIndex
		            if (nextUnit.equals(candidate.getUnit())) {
		                // The unit following one of the candidates for the preceding
		                // item already is a candidate for the current item.
		                break;
		            }
		        }

		        if ((candidate == null)&&nextUnit.getName().equals(first.getUnit().getName())) {
		           // nextUnitIndex is of the right unit type and is not yet one of the candidates.
		           // add it to the queue of candidates for the current item:
		            candidate = new ViterbiCandidate();
		            candidate.setNext(first);
		            candidate.setTarget(target);
		            candidate.setUnit(nextUnit);
		            first = candidate;
		            e++;
		       }
	           
	            }
	    }
        // TODO: Find a better way to store the candidates for an item?
	    target.getItem().getFeatures().setObject("clunit_cands", first);
	    return first;
    }
    
}
