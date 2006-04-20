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

/**
 * A unit selected from Viterbi
 * 
 * @author Marc Schr&ouml;der
 *
 */
public class SelectedUnit
{
    protected Unit unit;
    protected Target target;
    protected int unitStart = -1;
    protected int unitEnd = -1;

    public SelectedUnit(Unit unit, Target target)
    {
        this.unit = unit;
        this.target = target;
    }

    public Unit getUnit()
    {
        return unit;
    }
    
    public Target getTarget()
    {
        return target;
    }

    public void setUnitStart(int unitStart)
    {
        this.unitStart = unitStart;
    }
    
    public void setUnitEnd(int unitEnd)
    {
        this.unitEnd = unitEnd;
    }
    
    public int getUnitStart()
    {
        if (unitStart != -1) {
            return unitStart;
        } else {
            int start = unit.start;
            // TODO: uncomment the following and remove resulting incompabilities
            /**if (target.getUnitSize() == UnitDatabase.HALFPHONE &&
                target.getName().endsWith("right")){
                      start = start + ((((ClusterUnit)unit).end-start)/2);
            } **/
            return start;
        }
    }
    
    public int getUnitEnd()
    {
        if (unitEnd != -1) {
            return unitEnd;
        } else {
            int end = unit.end;
            //TODO: uncomment the following and remove resulting incompabilities
              /**if (target.getUnitSize() == UnitDatabase.HALFPHONE &&
                target.getName().endsWith("left")){
                      end = end - ((((ClusterUnit)unit).start-end)/2);
              } **/
            
            return end;
        }
    }
    
    public int targetDurationInSamples()
    {
        return (int) (target.getTargetDurationInSeconds()*unit.getDatabase().getSamplingRate());
    }
    
    public int unitDurationInSamples()
    {
        return unit.durationInSamples();
    }
    
    public String toString()
    {
        return "Target: "+target.toString() + " Unit: " + unit.toString()
        + (unitStart != -1 ? " unitStart shifted to "+unitStart : " start: " + unit.getStart())
        + (unitEnd != -1 ? " unitEnd shifted to "+unitEnd : " end: " + unit.getEnd())
        + " target duration " + targetDurationInSamples()
        + " prev unit "+ unit.getPrevInstance() + ", next unit " + unit.getNextInstance();
    }
}
