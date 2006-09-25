package de.dfki.lt.mary.unitselection.featureprocessors;

import java.io.DataOutput;
import java.io.IOException;

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

    public byte getByteValuedDiscreteFeature(int index)
    {
        return byteValuedDiscreteFeatures[index];
    }
    
    public short[] getShortValuedDiscreteFeatures()
    {
        return shortValuedDiscreteFeatures;
    }

    public short getShortValuedDiscreteFeature(int index)
    {
        return shortValuedDiscreteFeatures[index];
    }

    public float[] getContinuousFeatures()
    {
        return continuousFeatures;
    }

    public float getContinuousFeature(int index)
    {
        return continuousFeatures[index];
    }
    
    /**
     * Write a binary representation of this feature vector to the given data output.
     * @param out the DataOutputStream or RandomAccessFile in which to write the binary
     * representation of the feature vector. 
     * @return
     */
    public void write(DataOutput out) throws IOException
    {
        if (byteValuedDiscreteFeatures != null) {
            out.write(byteValuedDiscreteFeatures);
        }
        if (shortValuedDiscreteFeatures != null) {
            for (int i=0; i<shortValuedDiscreteFeatures.length; i++) {
                out.writeShort(shortValuedDiscreteFeatures[i]);
            }
        }
        if (continuousFeatures != null) {
            for (int i=0; i<continuousFeatures.length; i++) {
                out.writeFloat(continuousFeatures[i]);
            }
        }
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
