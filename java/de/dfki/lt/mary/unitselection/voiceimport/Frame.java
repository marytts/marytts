package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.*;
/**
 * An audio frame
 */
public class Frame {
    public float pitchmarkTime;
    public int[] parameters;
    protected int numRes;
    public int index;
       
    public Frame() {
    }
    
    public Frame(float pitchmarkTime, int[] parameters) {
        this.pitchmarkTime = pitchmarkTime;
        this.parameters = parameters;
    }

    /**
     * Dumps the ASCII form of this Frame.
     */
    public void dumpBinary(DataOutputStream out) throws IOException {
        for (int i = 0; i < parameters.length; i++) {
            out.writeFloat(parameters[i]);
        }
    }    
}
