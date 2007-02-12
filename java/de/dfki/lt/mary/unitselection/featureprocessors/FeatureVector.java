package de.dfki.lt.mary.unitselection.featureprocessors;

import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Compact representation of a list of byte-valued, short-valued and float-valued
 * (continuous) features.
 * In the user interface, features are identified through one index number,
 * covering all three types of features. For example, a feature vector consisting
 * of three bytes, four shorts and two floats will have nine features.
 * Use getByteFeature(), getShortFeature() and getContinuousFeature() to
 * access the features as primitives; note that you have to respect the index
 * restrictions, i.e. in the example, calling getShortFeature(6) would be OK, 
 * but getShortFeature(2) or getShortFeature(8) would throw an IndexOutOfBoundsException.
 * Use isShortFeature(i) to test whether a given feature is a short feature.
 * Alternatively, you can use an Object interface to access all features in 
 * a uniform way: getFeature(i) will return a Number object for all valid indexes.  
 * @author Marc Schr&ouml;der
 *
 */
public class FeatureVector
{
    int unitIndex;
    protected byte[] byteValuedDiscreteFeatures;
    protected short[] shortValuedDiscreteFeatures;
    protected float[] continuousFeatures;
    
    public FeatureVector(byte[] byteValuedDiscreteFeatures, short[] shortValuedDiscreteFeatures, float[] continuousFeatures,
            int setUnitIndex )
    {
        this.byteValuedDiscreteFeatures = byteValuedDiscreteFeatures;
        this.shortValuedDiscreteFeatures = shortValuedDiscreteFeatures;
        this.continuousFeatures = continuousFeatures;
        if ( setUnitIndex < 0 ) {
            throw new RuntimeException( "The unit index can't be negative or null when instanciating a new feature vector." );
        }
        this.unitIndex = setUnitIndex;
    }

    /**
     * Is this an edge vector?
     * @return isEdge
     */
    public boolean isEdgeVector(int edgeIndex){
        String edgeValue = getFeature(edgeIndex).toString();
        return (!edgeValue.equals(FeatureDefinition.NULLVALUE));
    }
    
    
    /**
     * Get the total number of features in this feature vector.
     * @return the number of features, irrespective of their type
     */
    public int getLength()
    {
        return byteValuedDiscreteFeatures.length + shortValuedDiscreteFeatures.length + continuousFeatures.length;
    }

    /**
     * Get the index of the unit to which the current feature vector applies.
     * @return The related unit index.
     */
    public int getUnitIndex() {
        return( unitIndex );
    }
    
    /**
     * The number of byte features in this feature vector.
     * @return a number of byte features, possibly 0.
     */
    public int getNumberOfByteFeatures()
    {
        return byteValuedDiscreteFeatures.length;
    }

    /**
     * The number of short features in this feature vector.
     * @return a number of short features, possibly 0.
     */
    public int getNumberOfShortFeatures()
    {
        return shortValuedDiscreteFeatures.length;
    }
    
    /**
     * The number of continuous features in this feature vector.
     * @return a number of continuous features, possibly 0.
     */
    public int getNumberOfContinuousFeatures()
    {
        return continuousFeatures.length;
    }

    /**
     * A uniform way to access features in this feature vector. 
     * @param index a feature index between 0 and getLength()-1
     * @return a Number object, which will be a Byte, a Short or a Float
     * depending on the type of the feature with the given index number.
     */
    public Number getFeature(int index)
    {
        if (index < byteValuedDiscreteFeatures.length)
            return new Byte(byteValuedDiscreteFeatures[index]);
        index -= byteValuedDiscreteFeatures.length;
        if (index < shortValuedDiscreteFeatures.length)
            return new Short(shortValuedDiscreteFeatures[index]);
        index -= shortValuedDiscreteFeatures.length;
        if (index < continuousFeatures.length)
            return new Float(continuousFeatures[index]);
        throw new IndexOutOfBoundsException();
    }
    
    /**
     * A wrapper to getFeature(), to get the result as an int value, e.g.,
     * for subsequent array indexing.
     * 
     * @param index A feature index between 0 and getLength()-1.
     * @return The feature value, as an int.
     * 
     * @see FeatureVector#getFeature(int)
     */
    public int getFeatureAsInt( int index ) {
        return( getFeature( index ).intValue() );
    }
    
    /**
     * A wrapper to getFeature(), to get the result as an String value, e.g.,
     * for subsequent System.out output.
     * 
     * @param index A feature index between 0 and getLength()-1.
     * @param feaDef A FeatureDefinition object allowing to decode the feature value.
     * @return The feature value, as a String.
     * 
     * @see FeatureVector#getFeature(int)
     */
    public String getFeatureAsString( int index, FeatureDefinition feaDef ) {
        if (index < byteValuedDiscreteFeatures.length)
            return feaDef.getFeatureValueAsString( index, byteValuedDiscreteFeatures[index] );
        throw new IndexOutOfBoundsException();
    }
    
    /**
     * An efficient way to access byte-valued features in this feature vector.
     * @param index the index number of the byte-valued feature in this feature vector.
     * @return the byte value of the feature with the given index.
     * @throws IndexOutOfBoundsException if index<0 or index >= getNumberOfByteFeatures().
     * @see #getNumberOfByteFeatures()
     * @see #isByteFeature()
     */
    public byte getByteFeature(int index)
    {
        return byteValuedDiscreteFeatures[index];
    }
    
    /**
     * An efficient way to access short-valued features in this feature vector.
     * @param index the index number of the short-valued feature in this feature vector.
     * @return the short value of the feature with the given index.
     * @throws IndexOutOfBoundsException if index<getNumberOfByteFeatures() or index >= getNumberOfByteFeatures()+getNumberOfShortFeatures().
     * @see #getNumberOfByteFeatures()
     * @see #getNumberOfShortFeatures()
     * @see #isShortFeature()
     */
    public short getShortFeature(int index)
    {
        return shortValuedDiscreteFeatures[index-byteValuedDiscreteFeatures.length];
    }
    
    /**
     * An efficient way to access continuous features in this feature vector.
     * @param index the index number of the continuous feature in this feature vector.
     * @return the float value of the feature with the given index.
     * @throws IndexOutOfBoundsException if index<getNumberOfByteFeatures()+getNumberOfShortFeatures() or index >= getLength().
     * @see #getNumberOfByteFeatures()
     * @see #getNumberOfShortFeatures()
     * @see #getNumberOfContinuousFeatures()
     * @see #getLength()
     * @see #isContinuousFeature()
     */
    public float getContinuousFeature(int index)
    {
        return continuousFeatures[index-byteValuedDiscreteFeatures.length-shortValuedDiscreteFeatures.length];
    }
    
    /**
     * Test whether the feature with the given index number is a byte feature.
     * @param index
     * @return This will return true exactly if getByteFeature(index) would return a value
     * without throwing an exception, i.e. if index>=0 and index < getNumberOfByteFeatures().

     */
    public boolean isByteFeature(int index)
    {
        return 0 <= index && index < byteValuedDiscreteFeatures.length;
    }

    /**
     * Test whether the feature with the given index number is a short feature.
     * @param index
     * @return This will return true exactly if getShortFeature(index) would return a value
     * without throwing an exception, i.e. if index>=getNumberOfByteFeatures() and index < getNumberOfByteFeatures()+getNumberOfShortFeatures().

     */
    public boolean isShortFeature(int index)
    {
        return byteValuedDiscreteFeatures.length <= index && index < byteValuedDiscreteFeatures.length+shortValuedDiscreteFeatures.length;
    }

    /**
     * Test whether the feature with the given index number is a continuous feature.
     * @param index
     * @return This will return true exactly if getContinuousFeature(index) would return a value
     * without throwing an exception, i.e. if index>=getNumberOfByteFeatures()+getNumberOfShortFeatures() and index < getLength().

     */
    public boolean isContinuousFeature(int index)
    {
        return byteValuedDiscreteFeatures.length+shortValuedDiscreteFeatures.length <= index && index < byteValuedDiscreteFeatures.length+shortValuedDiscreteFeatures.length+continuousFeatures.length;
    }

    // TODO: Not sure if these are needed?
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
     * Write a binary representation of this feature vector to the given data output.
     * @param out the DataOutputStream or RandomAccessFile in which to write the binary
     * representation of the feature vector. 
     * @return
     */
    public void writeTo(DataOutput out) throws IOException
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
