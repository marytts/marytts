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
    protected Object concatenationData;
    protected double[] audio;
    
    public SelectedUnit(Unit unit, Target target)
    {
        this.unit = unit;
        this.target = target;
        this.audio = null;
    }

    public Unit getUnit()
    {
        return unit;
    }
    
    public Target getTarget()
    {
        return target;
    }

    

    /**
     * Remember data about this selected unit which is relevant for unit concatenation.
     * What type of data is saved here depends on the UnitConcatenator used.
     * @param concatenationData
     */
    public void setConcatenationData(Object concatenationData)
    {
        this.concatenationData = concatenationData;
    }
    
    public Object getConcatenationData()
    {
        return concatenationData;
    }
    
    public void setAudio(double[] audio)
    {
        this.audio = audio;
    }
    
    public double[] getAudio()
    {
        return audio;
    }
    
    public String toString()
    {
        return "Target: "+target.toString() + " Unit: " + unit.toString()
        + " target duration " + target.getTargetDurationInSeconds();
    }
}
