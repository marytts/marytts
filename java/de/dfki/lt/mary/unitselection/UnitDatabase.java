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



import java.io.IOException;

import org.apache.log4j.Logger;

import de.dfki.lt.mary.unitselection.cart.CART;
import de.dfki.lt.mary.unitselection.viterbi.ViterbiCandidate;
import de.dfki.lt.mary.unitselection.voiceimport.BasenameList;

/**
 * The unit database of a voice
 * 
 * @author Marc Schr&ouml;der
 *
 */
public class UnitDatabase
{
    protected TargetCostFunction targetCostFunction;
    protected JoinCostFunction joinCostFunction;
    protected UnitFileReader unitReader;
    protected CART preselectionCART;
    protected TimelineReader audioTimeline;
    protected TimelineReader basenameTimeline;
    protected int backtrace;
    protected Logger logger = Logger.getLogger("UnitDatabase");
    
    
    public UnitDatabase()
    {
    }
    
     public void load(TargetCostFunction aTargetCostFunction,
                      JoinCostFunction aJoinCostFunction,
                      UnitFileReader aUnitReader,
                      CART aPreselectionCART,
                      TimelineReader anAudioTimeline,
                      TimelineReader aBasenameTimeline,
                      int backtraceLeafSize)
     {
         this.targetCostFunction = aTargetCostFunction;
         this.joinCostFunction = aJoinCostFunction;
         this.unitReader = aUnitReader;
         this.preselectionCART = aPreselectionCART;
         this.audioTimeline = anAudioTimeline;
         this.basenameTimeline = aBasenameTimeline;
         this.backtrace = backtraceLeafSize;
     }

     public TargetCostFunction getTargetCostFunction()
     {
         return targetCostFunction;
     }
     
     public JoinCostFunction getJoinCostFunction()
     {
         return joinCostFunction;
     }
     
     public UnitFileReader getUnitFileReader()
     {
         return unitReader;
     }
     
     public TimelineReader getAudioTimeline()
     {
         return audioTimeline;
     }

     
     
    /**
     * Preselect a set of candidates that could be used to realise the
     * given target.
     * @param target a Target object representing an optimal unit
     * @return an array of ViterbiCandidates, each containing the (same) target and a (different) Unit object
     */
    public ViterbiCandidate[] getCandidates(Target target)
    {
        //logger.debug("Looking for candidates in cart "+target.getName());
        //get the cart tree and extract the candidates
        int[] clist = (int[]) preselectionCART.interpret(target,backtrace);
        logger.debug("For target "+target+", selected " + clist.length + " units");

        // Now, clist is an array of unit indexes.
        ViterbiCandidate[] candidates = new ViterbiCandidate[clist.length];
        for (int i = 0; i < clist.length; i++) {
            candidates[i] = new ViterbiCandidate();
            candidates[i].setTarget(target); // The target is the same for all these candidates in the queue
            // remember the actual unit:
            Unit unit = unitReader.getUnit(clist[i]);
            candidates[i].setUnit(unit);
        }
        return candidates;
    }
    
    /**
     * For debugging, return the basename of the original audio file from which
     * the unit is coming, as well as the start time in that file. 
     * @param unit
     * @return a String containing basename followed by a space and the 
     * unit's start time, in seconds, from the beginning of the file. If 
     * no basenameTimeline was specified for this voice, returns the string
     * "unknown origin".
     */
    public String getFilenameAndTime(Unit unit)
    {
       if (basenameTimeline == null) return "unknown origin";
       long[] offset = new long[1];
       try {
           Datagram[] datagrams = basenameTimeline.getDatagrams(unit.getStart(), 1, unitReader.getSampleRate(), offset);
           Datagram filenameData = datagrams[0];
           float time = (float)offset[0]/basenameTimeline.getSampleRate();
           String filename = new String(filenameData.getData(), "UTF-8");
           return filename + " " + time;
       } catch (IOException ioe) {
           return "unknown origin";
       }
    }


}
