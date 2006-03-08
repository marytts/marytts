package de.dfki.lt.mary.unitselection.clunits;

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