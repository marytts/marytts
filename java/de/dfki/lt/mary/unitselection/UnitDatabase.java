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

import java.util.*;

import org.apache.log4j.Logger;

import de.dfki.lt.mary.unitselection.clunits.FrameSet;
import de.dfki.lt.mary.unitselection.featureprocessors.UnitSelectionFeatProcManager;

/**
 * The unit database of a voice
 * 
 * @author Marc Schr&ouml;der
 *
 */
public abstract class UnitDatabase
{
    protected FrameSet audioFrames;
    protected FrameSet joinCostFeatureVectors;
    

    protected int unitType; // e.g., Unit.PHONE or Unit.DIPHONE
    protected Set unitNames;
    protected Logger logger;
    
    public UnitDatabase(){
        logger = Logger.getLogger(this.getClass().getName());
    }
    
    
    public abstract void load(String databaseFile, 
            			UnitSelectionFeatProcManager featureProcessors,
            			String voice);
    
    public abstract int getSamplingRate();
    
    public int getUnitType()
    {
        return unitType;
    }
    
    /**
     * The list of all names that units can have in the database. 
     * @return a Set of Strings
     */
    public Set getUnitNames()
    {
        return Collections.unmodifiableSet(unitNames);
    }
    
    public FrameSet getAudioFrames(){
        return audioFrames;
    }
    
    public FrameSet getJoinCostFeatureVectors(){
        return joinCostFeatureVectors;
    }

    public Unit getUnit(String unitType, int index){
        return null;
    }
    
    public Unit getUnit(int which){
        return null;
    }
    
    public int getUnitTypeIndex(String unitType){
        return 0;
    }
    
    public List getFeats(){
        return null;
    }
    
    public List getWeights(){
        return null;
    }
    
    public void overwriteWeights(String file){}
    
    /**
     * Determine the chain of candidate sets that could be used to realise the
     * given list of targets.
     * @param targets a list of Target objects defining the optimal utterance
     * @return a list of Candidate Sets
     */
    public List getCandidates(List targets)
    {
        List candidates = new ArrayList(targets.size());
        for (Iterator it = targets.iterator(); it.hasNext(); ) {
            Target target = (Target) it.next();
            candidates.add(getCandidates(target));
        }
        return candidates;
    }

    /**
     * Preselect a set of candidates that could be used to realise the
     * given target.
     * @param target a Target object representing an optimal unit
     * @return a Set containing the Unit objects
     */
    public abstract Set getCandidates(Target target);


}
