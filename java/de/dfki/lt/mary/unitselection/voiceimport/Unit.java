package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Simple Unit entry for the Catalog.
 */
public class Unit {
    public String unitType;
    public int unitNum;
    public String filename;
    public float start;
    public float middle;
    public float end;
    public Unit previous;
    public Unit next;
    public int index;
    private byte[] byteValues;
    private short[] shortValues;
    private float[] floatValues;
    private int startFrame;
    private int endFrame;
    private float leftF0;
    private float rightF0;
    protected boolean haveFeatures=false;
    
    //Null-Konstruktor needed for inheritance
    public Unit(){}
    
    /**
     * Creates a new Unit entry for the catalog.
     *
     * @param unitType the type of this unit
     * @param unitNum the index of this unit
     * @param filename (without extension) where the audio and STS
     * data for this unit can be found
     * @param start the timing info (in seconds) for where the audio
     * and STS data for this unit starts in filename
     * @param middle the timing info (in seconds) for where the middle
     * of the audio and STS data for this unit is in filename
     * @param end the timing info (in seconds) for where the audio
     * and STS data for this unit ends in filename
     * @param previous the unit preceding this one in the recorded
     * utterance
     * @param next the unit following this one in the recorded
     * utterance
     * @param index the index of this unit in the overall catalog
     */
    public Unit(
        String unitType,
        int unitNum,
        String filename,
        float start,
        float middle,
        float end,
        Unit previous,
        Unit next,
        int index) {

        this.unitType = unitType;
        this.unitNum = unitNum;
        this.filename = filename;
        this.start = start;
        this.middle = middle;
        this.end = end;
        this.previous = previous;
        this.next = next;
        this.index = index;
    }

    public void setValues(byte[] byteValues, 
            short[] shortValues, 
            float[] floatValues){
        this.byteValues = byteValues;
        this.shortValues = shortValues;
        this.floatValues = floatValues;     
        haveFeatures = true;
    }
    
    public void setStartEnd(int start, int end){
    	this.startFrame = start;
    	this.endFrame = end;
    }
    
    public void setF0(float leftF0, float rightF0){
        this.leftF0 = leftF0;
        this.rightF0 = rightF0;
    }
    
    public int getEndFrame(){
        return endFrame;
    }
    
    public int getStartFrame(){
        return startFrame;
    }
    
    public boolean dumpTargetValues(DataOutputStream out) throws IOException{
        if (haveFeatures){
        for (int i=0;i<byteValues.length;i++){
            out.write(byteValues[i]);
        }
        for (int i=0;i<shortValues.length;i++){
            out.writeShort(shortValues[i]);
        }
        for (int i=0;i<floatValues.length;i++){
            out.writeFloat(floatValues[i]);
        }
        return true;
        } else {
            return false;
        }
        
    }
    
    public void dumpPseudoTargetValues(DataOutputStream out,
            					int numByteVals,
            					int numShortVals,
            					int numFloatVals) throws IOException{
        for (int i=0;i<numByteVals;i++){
            out.write(0);
        }
        for (int i=0;i<numShortVals;i++){
            out.writeShort(0);
        }
        for (int i=0;i<numFloatVals;i++){
            out.writeFloat(0);
        }
        
    }
    
    public void dumpLeftF0(DataOutputStream out) 
    		throws IOException {
       
        out.writeFloat(leftF0);
    }
    
    public void dumpRightF0(DataOutputStream out) 
	throws IOException {
        
        out.writeFloat(rightF0);
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer(filename + " ");
        if (previous != null) {
            buf.append(previous.unitType + "_" + previous.unitNum + " ");
        } else {
            buf.append("CLUNIT_NONE ");
        }
        buf.append(unitType + "_" + unitNum);
        if (next != null) {
            buf.append(" " + next.unitType + "_" + next.unitNum);
        } else {
            buf.append(" CLUNIT_NONE");
        }
        buf.append(" (index=" + index + ")");
        
        return buf.toString();
    }
}
