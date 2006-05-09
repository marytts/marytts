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
package de.dfki.lt.mary.unitselection.viterbi;

import java.util.*;

import org.apache.log4j.Logger;

import de.dfki.lt.mary.unitselection.*;



/**
 * Provides support for the Viterbi Algorithm.
 *
 * Implementation Notes
 * <p>
 * For each candidate for the current unit, calculate the cost
 * between it and the first candidate in the next unit.  Save
 * only the path that has the least cost. By default, if two
 * candidates come from units that are adjacent in the
 * database, the cost is 0 (i.e., they were spoken together,
 * so they are a perfect match).
 * <p>
 * 
 * Repeat the previous process for each candidate in the next
 * unit, creating a list of least cost paths between the
 * candidates between the current unit and the unit following
 * it.
 * <p>
 * 
 * Toss out all candidates in the current unit that are not
 * included in a path.
 * <p>
 * 
 * Move to the next unit and repeat the process.
 */
public  class Viterbi
{
    //a general flag indicating which type of viterbi search
	//to use:
    //-1: unlimited search
    // n>0: beam search, retain only the n best paths at each step.
    protected int searchStrategy = 25;
    
    protected ViterbiPoint firstPoint = null;
    protected ViterbiPoint lastPoint = null;
    protected LinkedHashMap f = null;
    private UnitDatabase unitDB;
    private UnitSelector unitSelector;
    protected TargetCostFunction targetCostFunction;
    protected JoinCostFunction joinCostFunction;
    protected Logger logger;
    // for debugging, try to get an idea of the average effect of join vs. target costs:
    protected double cumulJoinCosts;
    protected int nJoinCosts;
    protected double cumulTargetCosts;
    protected int nTargetCosts;
    
    /**
     * Creates a Viterbi class to process the given utterance.
     * A queue of ViterbiPoints corresponding to the Items in the Relation segs 
     * is built up.
     * 
     */
	public Viterbi(List targets, UnitDatabase database, 
	        		UnitSelector selector, TargetCostFunction tcf,
	        		JoinCostFunction jcf){
	    this.unitDB = database;
	    this.unitSelector = selector;
	    this.targetCostFunction = tcf;
	    this.joinCostFunction = jcf;
        this.logger = Logger.getLogger("Viterbi");
        this.cumulJoinCosts = 0;
        this.nJoinCosts = 0;
        this.cumulTargetCosts = 0;
        this.nTargetCosts = 0;
        ViterbiPoint last = null;
        f = new LinkedHashMap();
        //for each segment, build a ViterbiPoint
        for (Iterator it = targets.iterator(); it.hasNext(); ) {
            Target target = (Target) it.next();
            ViterbiPoint nextPoint = new ViterbiPoint(target);
            
            if (last != null) { // continue to build up the queue
                last.setNext(nextPoint);
            } else { // firstPoint is the start of the queue
                firstPoint = nextPoint;
                // dummy start path:
                firstPoint.getPaths().add(new ViterbiPath());
            }
            last = nextPoint;
        }
        lastPoint = last;
        if (searchStrategy == 0) {
            throw new IllegalStateException("General beam search not implemented");
    	}
    }
   
    
    /**
     * Sets the given feature to the given value.
     *
     * @param name the name of the feature
     * @param obj the new value.
     */
    public void setFeature(String name, Object obj) {
        f.put(name, obj);
    }
    
    /**
     * Gets the value for the given feature.
     *
     * @param name the name of the feature
     *
     * @return the value of the feature
     */
    public Object getFeature(String name) {
        return f.get(name);
    }
    
    /**
     * Carry out a Viterbi search in for a prepared queue of ViterbiPoints.
     * In a nutshell, each Point represents a target item (a target segment);
     * for each target Point, a number of Candidate units in the voice database 
     * are determined; a Path structure is built up, based on local best transitions.
     * Concretely, a Path consists of a (possibly empty) previous Path, a current Candidate,
     * and a Score. This Score is a quality measure of the Path; it is calculated as the
     * sum of the previous Path's score, the Candidate's score, and the Cost of joining
     * the Candidate to the previous Path's Candidate. At each step, only one Path 
     * leading to each Candidate is retained, viz. the Path with the best Score.
     * All that is left to do is to call result() to get the best-rated
     * path from among the paths associated with the last Point, and to associate
     * the resulting Candidates with the segment items they will realise. 
     */
    public void apply() {
        //go through all but the last point
        //(since last point has no item)
        for (ViterbiPoint point = firstPoint; point.getNext() != null; point = point.getNext()) {
            // The candidates for the current item:
            // candidate selection is carried out by UnitSelector
            point.setCandidates(getCandidates(point.getTarget()));
            assert searchStrategy != 0; // general beam search not implemented
    
            // Now go through all existing paths and all candidates 
            // for the current item;
            // tentatively extend each existing path to each of 
            // the candidates, but only retain the best one
            SortedSet paths = point.getPaths();
            int nPaths = paths.size();
            if (searchStrategy != -1 && searchStrategy < nPaths) {
                // beam search, look only at the best n paths:
                nPaths = searchStrategy;
            }
            // for searchStrategy == -1, no beam -- look at all candidates.
            Iterator pathIt = paths.iterator();
            for (int i = 0; i < nPaths; i++) {
                ViterbiPath pp = (ViterbiPath) pathIt.next();
                assert pp != null;
                // We are at the very beginning of the search, 
                // or have a usable path to extend
                for (ViterbiCandidate c = point.getCandidates(); 
                    c != null; c = c.getNext()) {
                    // For the candidate c, create a path extending the 
                    // previous path pp to that candidate, taking into
                    // account the target and join costs:
                    ViterbiPath np = getPath(pp, c);
                    // Compare this path to the existing best path 
                    // (if any) leading to candidate c; only retain 
                    // the one with the better score.
                    addPath(point.getNext(), np);
                }
            }
        }
        logger.debug("Computed "+nTargetCosts+" target costs (avg. "+ (cumulTargetCosts/nTargetCosts)+")");
        logger.debug("Computed "+nJoinCosts+" join costs (avg. "+ (cumulJoinCosts/nJoinCosts)+")");
    }
    
    /**
     * Add the new path to the state path if it is
     * better than the current path. In this, state means
     * the position of the candidate associated with this
     * path in the candidate queue
     * for the corresponding segment item. In other words,
     * this method uses newPath as the one path leading to
     * the candidate newPath.candidate, if it has a better
     * score than the previously best path leading to that candidate.
     *
     * @param point where the path is added
     * @param newPath the path to add if its score is best
     */
    void addPath(ViterbiPoint point, ViterbiPath newPath) {
        //get the position of newPath's candidate 
        //in path array statePath of point
        ViterbiCandidate candidate = newPath.getCandidate();
        assert candidate != null;
        ViterbiPath bestPathSoFar = candidate.getBestPath();
        SortedSet paths = point.getPaths();
        if (bestPathSoFar == null) {
            // we don't have a path for the candidate yet, so this is best
            paths.add(newPath);
            candidate.setBestPath(newPath);
        } else if (newPath.getScore() < bestPathSoFar.getScore()) {
            // newPath is a better path for the candidate
            paths.remove(bestPathSoFar);
            paths.add(newPath);
            candidate.setBestPath(newPath);
        }
    }
    
    /**
     * Collect and return the best path, as a List of SelectedUnit objects.
     * Note: This is a replacement for result().
     * @return the list of selected units, or null if no path could be found.
     */
    public List getSelectedUnits()
    {
        LinkedList selectedUnits = new LinkedList();
        if (firstPoint == null || firstPoint.getNext() == null) {
            return selectedUnits; // null case
        }
        ViterbiPath path = findBestPath();
        if (path == null){
            //System.out.println("No best path found");
            return null;
        }
        for (; path != null; path = path.getPrevious()) {
            if (path.getCandidate() != null) {
                SelectedUnit sel = new SelectedUnit(path.getCandidate().getUnit(),
                        path.getCandidate().getTarget());
                selectedUnits.addFirst(sel);
                                
                if (path.isPresent("unit_this_move")) {
                    sel.setUnitStart(((Integer)path.getFeature("unit_this_move")).intValue());
                }
                if (path.getNext() != null && path.getNext().isPresent("unit_prev_move")) {
                    sel.setUnitEnd(((Integer)path.getFeature("unit_prev_move")).intValue());
                }
            }
        }
        return selectedUnits;
    }
    
    /**
     * Finds the best (queue of) candidate(s) for a given (segment) item.
     * This traverses a CART tree for target cluster selection as described in 
     * the paper introducing the clunits algorithm. This corresponds to the
     * "target costs" described for general unit selection.
     * @return the first candidate in the queue of candidate units for this item.
     */
    private ViterbiCandidate getCandidates(Target target) {
	    return unitSelector.getCandidates(target);
    }
    
    /**
     * Construct a new path element linking a previous path to the given candidate.
     * The (penalty) score associated with the new path is calculated as the sum of
     * the score of the old path plus the score of the candidate itself plus the
     * join cost of appending the candidate to the nearest candidate in the given path.
     * This join cost takes into account optimal coupling if the database has
     * OPTIMAL_COUPLING set to 1. The join position is saved in the new path, as
     * the features "unit_prev_move" and "unit_this_move".
     *
     * @param path the previous path, or null if this candidate starts a new path
     * @param candiate the candidate to add to the path
     *
     * @return a new path, consisting of this candidate appended to the previous path, and
     * with the cumulative (penalty) score calculated. 
     */
    private ViterbiPath getPath(ViterbiPath path, 
				ViterbiCandidate candidate) {
        int cost;
        ViterbiPath newPath = new ViterbiPath();

        Unit candidateUnit = candidate.getUnit();
        
        newPath.setCandidate(candidate);
        newPath.setPrevious(path);
        int joinCost;
        int targetCost;
        // Target costs:
        targetCost = candidate.getTargetCost(targetCostFunction);
        
        if (path == null || path.getCandidate() == null) {
            joinCost = 0;
        } else {
            // Join costs:
            Unit prevUnit = path.getCandidate().getUnit();
            joinCost = joinCostFunction.cost(prevUnit, candidateUnit, newPath);
            // TODO: clean this up
            joinCost *= 5;  // magic number ("continuity weight") from flite
        }
        cost = joinCost + targetCost;
        cumulJoinCosts += joinCost;
        nJoinCosts++;
        cumulTargetCosts += targetCost;
        nTargetCosts++;
        //logger.debug(candidateUnit+": target cost "+targetCost+", join cost "+joinCost);

        if (path == null) {
            newPath.setScore(cost);
        } else {
            newPath.setScore(cost + path.getScore());
        }	
	
        return newPath;
    }
    
    /**
     * Find the best path. This requires apply() to have been run.
     *
     * @return the best path.
     */
    private ViterbiPath findBestPath()
    {
        assert searchStrategy != 0;
        // All paths end in lastPoint, and take into account
        // previous path segment's scores. Therefore, it is
        // sufficient to find the best path from among the
        // paths for lastPoint.
        SortedSet paths = lastPoint.getPaths();
        return (ViterbiPath) paths.first();
    }
    
 
    
   

   
    
    
}    

    
    



    



 

