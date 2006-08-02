package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Null-Unit entry for the Catalog.
 */
public class NullUnit extends Unit {
    
    public NullUnit(String unitType){
        super();
        this.unitType = unitType;
    }
   
    
    public boolean dumpTargetValues(DataOutputStream out) throws IOException{
        return false;     
    }
    

    public void dumpJoinValues(DataOutputStream out, int numValues)
    						throws IOException{
        for (int i=0;i<numValues*2;i++){
            out.writeFloat(0);
        } 
    }
    
}
