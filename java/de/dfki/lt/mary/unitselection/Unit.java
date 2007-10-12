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
 * Representation of a unit from a unit database. This gives access to
 * everything that is known about a given unit, including all sorts of 
 * features and the actual audio data.
 * @author Marc Schr&ouml;der
 *
 */
public class Unit
{
    protected long startTime;
    protected int duration;
    protected int index;
    
    
    public Unit(long startTime, int duration, int index)
    {
        this.startTime = startTime;
        this.duration = duration;
        this.index = index;
    }
    
    /**
     * Index position of this unit in the unit file.
     * @return
     */
    public int getIndex()
    {
        return index;
    }
    
    /**
     * Unit start time, expressed in samples. To convert into time,
     * divide by UnitFileReader.getSampleRate().
     * @return
     */
    public long getStart()
    {
        return startTime;
    }
    
    /**
     * Unit duration, expressed in samples. To convert into time,
     * divide by UnitFileReader.getSampleRate().
     * @return
     */
    public int getDuration()
    {
        return duration;
    }
    
    
    /**
     * Determine whether the unit is an "edge" unit, i.e.
     * a unit marking the start or the end of an utterance.
     * 
     * @param i The index of the considered unit.
     * @return true if the unit is an edge unit, false otherwise
     */
    public boolean isEdgeUnit()
    {
        if (duration == -1) return true;
        else return false;
    }

    public String toString()
    {
        return "unit "+index+" start: "+startTime+", duration: "+duration;
    }
    
}
