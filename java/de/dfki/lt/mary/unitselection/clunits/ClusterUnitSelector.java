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
import java.util.*;

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
        //build targets for the items
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
        List selectedUnits = viterbi.getSelectedUnits();
        // If you can not associate the candidate units in the best path 
        // with the items in the segment relation, there is no best path
        if (selectedUnits == null) {
    	    throw new IllegalStateException("clunits: can't find path");
    	}
        if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            UnitOriginInfo lastOrigin = null;
            Map unitsFromSameFile = new HashMap();
            for (int i=0; i<selectedUnits.size(); i++) {
                SelectedUnit selUnit = 
                    (SelectedUnit)selectedUnits.get(i);
                
                UnitOriginInfo origin = 
                    ((ClusterUnit) selUnit.getUnit()).getOriginInfo();
                if (origin != null){
                    if (lastOrigin != null){
                        String originFile = origin.getFile();
                         if (origin.getFile().equals(lastOrigin.getFile())
                                && origin.getStart() == lastOrigin.getEnd()){
                            if (unitsFromSameFile.containsKey(originFile)){
                                ((List)unitsFromSameFile.get(originFile)).add(new Integer(i));
                            } else {
                                List successiveUnits = new ArrayList();
                                successiveUnits.add(new Integer(i-1));
                                successiveUnits.add(new Integer(i));
                                unitsFromSameFile.put(originFile, successiveUnits);
                            }
                         }
                    }
                    lastOrigin = origin;
                    pw.println("Unit "+i+": "+selUnit.toString()+"\n"
                            +origin.toString());
                } else {
                    pw.println("Unit "+i+": "+selUnit.toString()
                            +"\nNo origin info for selected unit\n ");
                    }
            }
            Set originFiles = unitsFromSameFile.keySet();
            if (originFiles.size() > 0){
                pw.println("Successive units:");
            }
            for (Iterator it = originFiles.iterator(); it.hasNext();){
                String originFile = (String)it.next();
                List successiveUnits = (List)unitsFromSameFile.get(originFile);
                pw.print("\nUnits from file "+originFile+": ");
                for (int i = 0; i < successiveUnits.size(); i++){
                    pw.print((Integer)successiveUnits.get(i)+" ");
                }
                pw.println();
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
     * Finds the best candidates for a given (segment) target.
     * This traverses a CART tree for target cluster selection as described in 
     * the paper introducing the clunits algorithm. This is a first pre-selection,
     * linked to the "target costs" described for general unit selection.
     * 
     *@param target the target
     *@return the head of the candidate queue
     */
    public ViterbiCandidate[] getCandidates(Target target){
        //logger.debug("Looking for candidates in cart "+target.getName());
        //get the cart tree and extract the candidates
        CART cart = database.getTree(target.getName());
    	int[] clist = (int[]) cart.interpret(target.getItem());
    	
    	// Now, clist is a List of instance numbers for the units of type
        // unitType that belong to the best cluster according to the CART.
        
	    ViterbiCandidate[] candidates = new ViterbiCandidate[clist.length+database.getExtendSelections()];
        int extendSelections = database.getExtendSelections();
        Set candidateUnits = null;
        if (extendSelections > 0) candidateUnits = new HashSet(candidates.length);
	    for (int i = 0; i < clist.length; i++) {
	        candidates[i] = new ViterbiCandidate();
	        candidates[i].setTarget(target); // The item is the same for all these candidates in the queue
	        // remember the actual unit:
	        int unitIndex = clist[i];
            Unit unit = database.getUnit(target.getName(), unitIndex);
	        candidates[i].setUnit(unit);
            if (candidateUnits != null) candidateUnits.add(unit);
	    }
	
        // Take into account candidates for previous item?
        // Depending on the setting of EXTEND_SELECTIONS in the database,
        // look the first candidates for the preceding item,
        // and add the units following these (which are not yet candidates)
        // as candidates. EXTEND_SELECTIONS indicates how many of these
        // are added. A high setting will add candidates which don't fit the
        // target well, but which can be smoothly concatenated with the context.
        // In a sense, this means trading target costs against join costs.
        int index = clist.length;
	    if (extendSelections > 0 && target.getItem().getPrevious() != null) {
            // Get the candidates for the preceding (segment) item
	        ViterbiCandidate[] precedingCandidates = 
		           (ViterbiCandidate[]) (target.getItem().getPrevious().getFeatures().getObject("clunit_cands"));
            String targetName = target.getName();
	        for (int pi = 0, piLength = precedingCandidates.length, e = 0;
                 pi < piLength && e < extendSelections;
		         pi++) {
                assert precedingCandidates[pi] != null;
	            ClusterUnit nextClusterUnit = ((ClusterUnit)precedingCandidates[pi].getUnit());
		        if (nextClusterUnit == null) continue;
	            Unit nextUnit = nextClusterUnit.getNext();
		        if (nextUnit == null || !nextUnit.getName().equals(targetName)) continue;
		        if (!candidateUnits.contains(nextUnit)) {
		           // nextUnit is of the right unit type and is not yet one of the candidates.
		           // add it to the candidates for the current item:
		            candidates[index] = new ViterbiCandidate();
		            candidates[index].setTarget(target);
		            candidates[index].setUnit(nextUnit);
                    candidateUnits.add(nextUnit);
		            e++;
                    index++;
		       }
	        }
	    }
        if (index < candidates.length) {
            // could not find extendSelections units to add.
            // Copy the array to avoid having null candidates:
            ViterbiCandidate[] vcs = new ViterbiCandidate[index];
            System.arraycopy(candidates, 0, vcs, 0, index);
            candidates = vcs;
        }
        assert candidates[candidates.length-1] != null;
        // TODO: Find a better way to store the candidates for an item? This is needed only for getting the preceding candidates in the extend-selections code above.
	    target.getItem().getFeatures().setObject("clunit_cands", candidates);
	    return candidates;
    }
    
}
