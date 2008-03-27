package de.dfki.lt.mary.unitselection.voiceimport;

public interface Discretizer {
    
    public int discretize(int aValue);
    
    public int[] getPossibleValues();

}
