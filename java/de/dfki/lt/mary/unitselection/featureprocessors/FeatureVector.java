package de.dfki.lt.mary.unitselection.featureprocessors;

public class FeatureVector {
    protected byte[] byteValuedDiscreteFeatures;
    protected short[] shortValuedDiscreteFeatures;
    protected float[] continuousFeatures;
    
    public FeatureVector(byte[] byteValuedDiscreteFeatures, short[] shortValuedDiscreteFeatures, float[] continuousFeatures)
    {
        this.byteValuedDiscreteFeatures = byteValuedDiscreteFeatures;
        this.shortValuedDiscreteFeatures = shortValuedDiscreteFeatures;
        this.continuousFeatures = continuousFeatures;
    }
    
    public byte[] getByteValuedDiscreteFeatures()
    {
        return byteValuedDiscreteFeatures;
    }

    public short[] getShortValuedDiscreteFeatures()
    {
        return shortValuedDiscreteFeatures;
    }

    public float[] getContinuousFeatures()
    {
        return continuousFeatures;
    }
    
    /** 
     * Return a string representation of this set of target features; feature values separated by spaces.
     */
    public String toString()
    {
        StringBuffer out = new StringBuffer();
        for (int i=0; i<byteValuedDiscreteFeatures.length; i++) {
            if (out.length() > 0) out.append(" ");
            out.append((int)byteValuedDiscreteFeatures[i]);
        }
        for (int i=0; i<shortValuedDiscreteFeatures.length; i++) {
            if (out.length() > 0) out.append(" ");
            out.append((int)shortValuedDiscreteFeatures[i]);
        }
        for (int i=0; i<continuousFeatures.length; i++) {
            if (out.length() > 0) out.append(" ");
            out.append(continuousFeatures[i]);
        }
        return out.toString();
    }

}
