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
package de.dfki.lt.mary.unitselection.clunits;

/** DELETE!
 * Represent a Unit-Target Combination 
 * Is used by ClusterTargetCostFunction
 * 
 * @author Anna Hunecke
 *
 */
public class UnitTargetPair{
    
    private int unitType;
    private int unitInstance;
    private int targetIndex;
    
    public UnitTargetPair(int unitType, 
            			  int unitInstance, 
            			  int targetIndex){
        this.unitType = unitType;
        this.unitInstance = unitInstance;
        this.targetIndex = targetIndex;
    }
    
    public boolean equals(Object o){
        if (o instanceof UnitTargetPair){
            UnitTargetPair utp = (UnitTargetPair) o;
            if (utp.getUnitType() == unitType &&
                utp.getUnitInstance() == unitInstance &&
                utp.getTargetIndex() == targetIndex){
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    
    public int hashCode(){
        return unitType+unitInstance*(targetIndex+1);
    }
    
    public int getUnitType(){
        return unitType;
    }
    
    public int getUnitInstance(){
        return unitInstance;
    }
    
    public int getTargetIndex(){
        return targetIndex;
    }
    
    
}