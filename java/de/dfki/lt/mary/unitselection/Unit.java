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

import de.dfki.lt.mary.unitselection.clunits.Frame;
import java.util.List;

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
    protected String name;
    protected List values;
    protected boolean haveValues = false;

    public int type;
    public int phone;
    protected int start;
    protected int end;
    protected int prev;
    protected int next;
    
    public Unit(UnitDatabase database, String name)
    {
        this.database = database;
        this.name = name;
    }
    
    
    public void setName(String name)
    {
        this.name = name;
    }
    public String getName()
    {
        return name;
    }

    public int getStart(){
        return start;
    }
    
    public int getEnd(){
        return end;
    }
    
    public int getNextInstance(){
        return next;
    }
    
    public int getPrevInstance(){
        return prev;
    }
    
    public Object getTargetCostFeatures()
    {
        return null;
    }
    
    public Frame getJoinCostFeatureVector(int frameNumber)
    {
        return null;
    }
    
    public UnitDatabase getDatabase()
    {
        return database;
    }
   
    public String getValueForFeature(int index)
    {
        if (haveValues){
                return (String) values.get(index);
        } else {
                return null;
        }
    }

    public void setValues(List values)
    {
        this.values = values;
        if (values != null){
            haveValues = true;
        }
    }
    
    public boolean hasValues()
    {
       return haveValues;
    }
    
    public int durationInSamples()
    {
        return -1;
    }
    
}
