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
    protected UnitDatabase database;
     

    protected long startTime;
    protected int duration;
    protected int index;
    
    
    public Unit(long startTime, int duration, int index){
        this.startTime = startTime;
        this.duration = duration;
        this.index = index;
    }
    
    public int getIndex(){
        return index;
    }
    
    public long getStart(){
        return startTime;
    }
    
    public int getDuration(){
        return duration;
    }
    
    
 
    
    protected int instanceNumber; // identifies the instances of a given type
    
    
    public boolean isValid(){
        // FIXME: This cannot work -- this was intended to be used with frames, and is now applied to samples. If needed, use more sensible duration estimates, otherwise remove.
        // on average, a period is between 50 and 200 Hz, i.e. between
        // 5 ms and 20 ms long.
        // Treat units with one frame or less as too short, 
        // and units with more than 50 frames (250 ms - 1 second) as too long
        int lowerLimit = 1;
        int upperLimit = 50;
        return (duration > lowerLimit && duration < upperLimit);    
    }
    
    
    
    public void setInstanceNumber(int instanceNumber)
    {
        this.instanceNumber = instanceNumber;
    }
    
    public int getInstanceNumber()
    {
        return instanceNumber;
    }

    public String toString()
    {
        return "unit start : "+startTime+", duration : "+duration;
    }
    
}
