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
package de.dfki.lt.mary.unitselection.featureprocessors;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.dfki.lt.mary.util.ByteStringTranslator;
import de.dfki.lt.mary.util.IntStringTranslator;
import de.dfki.lt.mary.util.ShortStringTranslator;

/**
 * A feature definition object represents the "meaning" of feature vectors.
 * It consists of a list of byte-valued, short-valued and continuous features
 * by name and index position in the feature vector;
 * the respective possible feature values (and corresponding byte and short
 * codes);
 * and, optionally, the weights and, for continuous features, weighting functions
 * for each feature.
 * @author Marc Schr&ouml;der
 *
 */
public class FeatureDefinition
{
    public static final String BYTEFEATURES = "ByteValuedFeatureProcessors";
    public static final String SHORTFEATURES = "ShortValuedFeatureProcessors";
    public static final String CONTINUOUSFEATURES = "ContinuousFeatureProcessors";
    public static final char WEIGHT_SEPARATOR = '|';

    private int numByteFeatures;
    private int numShortFeatures;
    private int numContinuousFeatures;
    private float[] featureWeights;
    private IntStringTranslator featureNames;
    // feature values: for byte and short features only
    private ByteStringTranslator[] byteFeatureValues;
    private ShortStringTranslator[] shortFeatureValues;
    private String[] floatWeightFuncts; // for continuous features only
    
    /**
     * Create a feature definition object, reading textual data
     * from the given BufferedReader.
     * @param input a BufferedReader from which a textual feature definition
     * can be read.
     * @param readWeights a boolean indicating whether or not to read
     * weights from input.
     * @throws IOException if a reading problem occurs
     *
     */
    public FeatureDefinition(BufferedReader input, boolean readWeights)
    throws IOException
    {
        // Section BYTEFEATURES
        String line = input.readLine();
        if (line == null) throw new IOException("Could not read from input");
        if (!line.trim().equals(BYTEFEATURES)) {
            throw new IOException("Unexpected input: expected '"+BYTEFEATURES+"', read '"+line+"'");
        }
        List byteFeatureLines = new ArrayList();
        while (true) {
            line = input.readLine();
            if (line == null) throw new IOException("Could not read from input");
            line = line.trim();
            if (line.equals(SHORTFEATURES)) break; // Found end of section
            byteFeatureLines.add(line);            
        }
        // Section SHORTFEATURES
        List shortFeatureLines = new ArrayList();
        while (true) {
            line = input.readLine();
            if (line == null) throw new IOException("Could not read from input");
            line = line.trim();
            if (line.equals(CONTINUOUSFEATURES)) break; // Found end of section
            shortFeatureLines.add(line);            
        }
        // Section CONTINUOUSFEATURES
        List continuousFeatureLines = new ArrayList();
        while ((line = input.readLine()) != null) { // it's OK if we hit the end of the file now
            line = line.trim();
            if (line.equals("")) break; // empty line: end of section
            continuousFeatureLines.add(line);
        }
        numByteFeatures = byteFeatureLines.size();
        numShortFeatures = shortFeatureLines.size();
        numContinuousFeatures = continuousFeatureLines.size();
        int total = numByteFeatures+numShortFeatures+numContinuousFeatures;
        featureNames = new IntStringTranslator(total);
        byteFeatureValues = new ByteStringTranslator[numByteFeatures];
        shortFeatureValues = new ShortStringTranslator[numShortFeatures];
        if (readWeights) {
            featureWeights = new float[total];
            floatWeightFuncts = new String[numContinuousFeatures];
        }
        
        for (int i=0; i<numByteFeatures; i++) {
            line = (String) byteFeatureLines.get(i);
            String featureDef;
            if (readWeights) {
                int seppos = line.indexOf(WEIGHT_SEPARATOR);
                if (seppos == -1) throw new IOException("Weight separator '"+WEIGHT_SEPARATOR+"' not found in line '"+line+"'");
                String weightDef = line.substring(0, seppos).trim();
                featureDef = line.substring(seppos+1).trim();
                // The weight definition is simply the float number:
                featureWeights[i] = Float.parseFloat(weightDef);
                if (featureWeights[i] < 0) throw new IOException("Negative weight found in line '"+line+"'");
            } else {
                featureDef = line;
            }
            // Now featureDef is a String in which the feature name and all feature values
            // are separated by white space.
            String[] nameAndValues = featureDef.split("\\s+", 2);
            featureNames.set(i, nameAndValues[0]); // the feature name
            byteFeatureValues[i] = new ByteStringTranslator(nameAndValues[1].split("\\s+")); // the feature values
        }

        for (int i=0; i<numShortFeatures; i++) {
            line = (String) shortFeatureLines.get(i);
            String featureDef;
            if (readWeights) {
                int seppos = line.indexOf(WEIGHT_SEPARATOR);
                if (seppos == -1) throw new IOException("Weight separator '"+WEIGHT_SEPARATOR+"' not found in line '"+line+"'");
                String weightDef = line.substring(0, seppos).trim();
                featureDef = line.substring(seppos+1).trim();
                // The weight definition is simply the float number:
                featureWeights[numByteFeatures+i] = Float.parseFloat(weightDef);
                if (featureWeights[numByteFeatures+i] < 0) throw new IOException("Negative weight found in line '"+line+"'");
            } else {
                featureDef = line;
            }
            // Now featureDef is a String in which the feature name and all feature values
            // are separated by white space.
            String[] nameAndValues = featureDef.split("\\s+", 2);
            featureNames.set(numByteFeatures+i, nameAndValues[0]); // the feature name
            shortFeatureValues[i] = new ShortStringTranslator(nameAndValues[1].split("\\s+")); // the feature values
        }
        
        for (int i=0; i<numContinuousFeatures; i++) {
            line = (String) continuousFeatureLines.get(i);
            String featureDef;
            if (readWeights) {
                int seppos = line.indexOf(WEIGHT_SEPARATOR);
                if (seppos == -1) throw new IOException("Weight separator '"+WEIGHT_SEPARATOR+"' not found in line '"+line+"'");
                String weightDef = line.substring(0, seppos).trim();
                featureDef = line.substring(seppos+1).trim();
                // The weight definition is the float number plus a definition of a weight function:
                String[] weightAndFunction = line.split("\\s+", 2);
                featureWeights[numByteFeatures+numShortFeatures+i] = Float.parseFloat(weightAndFunction[0]);
                if (featureWeights[numByteFeatures+numShortFeatures+i] < 0) throw new IOException("Negative weight found in line '"+line+"'");
                floatWeightFuncts[i] = weightAndFunction[1];
            } else {
                featureDef = line;
            }
            // Now featureDef is the feature name. 
            featureNames.set(numByteFeatures+numShortFeatures+i, featureDef);
        }

    }
    
    
    /**
     * Create a feature definition object, reading binary data
     * from the given DataInput.
     * @param input a DataInputStream or a RandomAccessFile from which
     * a binary feature definition can be read.
     * @throws IOException if a reading problem occurs
     */
    public FeatureDefinition(DataInput input) throws IOException
    {
        // Section BYTEFEATURES
        numByteFeatures = input.readInt();
        byteFeatureValues = new ByteStringTranslator[numByteFeatures];
        // Initialise global arrays to byte feature length first;
        // we have no means of knowing how many short or continuous
        // features there will be, so we need to resize later.
        // This will happen automatically for featureNames, but needs
        // to be done by hand for featureWeights.
        featureNames = new IntStringTranslator(numByteFeatures);
        featureWeights = new float[numByteFeatures];
        for (int i=0; i<numByteFeatures; i++) {
            featureWeights[i] = input.readFloat();
            String featureName = input.readUTF();
            featureNames.set(i, featureName);
            byte numberOfValues = input.readByte();
            byteFeatureValues[i] = new ByteStringTranslator(numberOfValues);
            for (byte b=0; b<numberOfValues; b++) {
                String value = input.readUTF();
                byteFeatureValues[i].set(b, value);
            }
        }
        // Section SHORTFEATURES
        numShortFeatures = input.readInt();
        if (numShortFeatures > 0) {
            // resize weight array:
            float[] newWeights = new float[numByteFeatures+numShortFeatures];
            System.arraycopy(featureWeights, 0, newWeights, 0, numByteFeatures);
            featureWeights = newWeights;
        }
        for (int i=0; i<numShortFeatures; i++) {
            featureWeights[numByteFeatures+i] = input.readFloat();
            String featureName = input.readUTF();
            featureNames.set(numByteFeatures+i, featureName);
            short numberOfValues = input.readShort();
            shortFeatureValues[i] = new ShortStringTranslator(numberOfValues);
            for (short s=0; s<numberOfValues; s++) {
                String value = input.readUTF();
                shortFeatureValues[i].set(s, value);
            }
        }
        // Section CONTINUOUSFEATURES
        numContinuousFeatures = input.readInt();
        floatWeightFuncts = new String[numContinuousFeatures];
        if (numContinuousFeatures > 0) {
            // resize weight array:
            float[] newWeights = new float[numByteFeatures+numShortFeatures+numContinuousFeatures];
            System.arraycopy(featureWeights, 0, newWeights, 0, numByteFeatures+numShortFeatures);
            featureWeights = newWeights;
        }
        for (int i=0; i<numContinuousFeatures; i++) {
            featureWeights[numByteFeatures+numShortFeatures+i] = input.readFloat();
            floatWeightFuncts[i] = input.readUTF();
            String featureName = input.readUTF();
            featureNames.set(numByteFeatures+numShortFeatures+i, featureName);
        }
    }
    
    /**
     * Write this feature definition in binary format to the given
     * output.
     * @param out a DataOutputStream or RandomAccessFile to which the
     * FeatureDefinition should be written.
     * @throws IOException if a problem occurs while writing.
     */
    public void writeBinary(DataOutput out) throws IOException
    {
        // Section BYTEFEATURES
        out.writeInt(numByteFeatures);
        for (int i=0; i<numByteFeatures; i++) {
            out.writeFloat(featureWeights[i]);
            out.writeUTF(featureNames.get(i));
            byte numValues = (byte) getNumberOfValues(i);
            out.writeByte(numValues);
            for (byte b=0; b<numValues; b++) {
                String value = byteFeatureValues[i].get(b);
                out.writeUTF(value);
            }
        }
        // Section SHORTFEATURES
        out.writeInt(numShortFeatures);
        for (int i=0; i<numShortFeatures; i++) {
            out.writeFloat(featureWeights[numByteFeatures+i]);
            out.writeUTF(featureNames.get(numByteFeatures+i));
            short numValues = (short) getNumberOfValues(numByteFeatures+i);
            out.writeShort(numValues);
            for (short b=0; b<numValues; b++) {
                String value = shortFeatureValues[i].get(b);
                out.writeUTF(value);
            }
        }
        // Section CONTINUOUSFEATURES
        out.writeInt(numContinuousFeatures);
        for (int i=0; i<numContinuousFeatures; i++) {
            out.writeFloat(featureWeights[numByteFeatures+numShortFeatures+i]);
            out.writeUTF(floatWeightFuncts[i]);
            out.writeUTF(featureNames.get(numByteFeatures+numShortFeatures+i));
        }
    }
    
    /**
     * Get the total number of features.
     * @return the number of features
     */
    public int getNumberOfFeatures()
    {
        return numByteFeatures+numShortFeatures+numContinuousFeatures;
    }

    /**
     * Get the number of byte features.
     * @return the number of features
     */
    public int getNumberOfByteFeatures()
    {
        return numByteFeatures;
    }

    /**
     * Get the number of short features.
     * @return the number of features
     */
    public int getNumberOfShortFeatures()
    {
        return numShortFeatures;
    }

    /**
     * Get the number of continuous features.
     * @return the number of features
     */
    public int getNumberOfContinuousFeatures()
    {
        return numContinuousFeatures;
    }

    /**
     * Translate between a feature index and a feature name.
     * @param index a feature index, as could be used to access
     * a feature value in a FeatureVector.
     * @return the name of the feature corresponding to the index
     * @throws IndexOutOfBoundsException if index<0 or index>getNumberOfFeatures() 
     */
    public String getFeatureName(int index)
    {
        return featureNames.get(index);
    }
    
    /**
     * Translate between a feature name and a feature index.
     * @param featureName a valid feature name
     * @return a feature index, , as could be used to access
     * a feature value in a FeatureVector.
     * @throws IllegalArgumentException if the feature name is unknown. 
     */
    public int getFeatureIndex(String featureName)
    {
        return featureNames.get(featureName);
    }
    
    /**
     * Get the number of possible values for the feature with the given index number.
     * This method must only be called for byte-valued or short-valued features.
     * @param featureIndex the index number of the feature.
     * @return for byte-valued and short-valued features, return the number of values.
     * @throws IndexOutOfBoundsException if featureIndex < 0 or 
     * featureIndex >= getNumberOfByteFeatures() + getNumberOfShortFeatures().
     */
    public int getNumberOfValues(int featureIndex)
    {
        if (featureIndex < numByteFeatures)
            return byteFeatureValues[featureIndex].getHighestValue();
        featureIndex -= numByteFeatures;
        if (featureIndex < numShortFeatures)
            return shortFeatureValues[featureIndex].getHighestValue();
        throw new IndexOutOfBoundsException("Feature no. "+featureIndex+" is not a byte-valued or short-valued feature");
    }

    /**
     * Get the list of possible String values for the feature with the given index number.
     * This method must only be called for byte-valued or short-valued features.
     * The position in the String array corresponds to the byte or short value of the
     * feature obtained from a FeatureVector.
     * @param featureIndex the index number of the feature.
     * @return for byte-valued and short-valued features, return the array of String values.
     * @throws IndexOutOfBoundsException if featureIndex < 0 or 
     * featureIndex >= getNumberOfByteFeatures() + getNumberOfShortFeatures().
     */
    public String[] getPossibleValues(int featureIndex)
    {
        if (featureIndex < numByteFeatures)
            return byteFeatureValues[featureIndex].getStringValues();
        featureIndex -= numByteFeatures;
        if (featureIndex < numShortFeatures)
            return shortFeatureValues[featureIndex].getStringValues();
        throw new IndexOutOfBoundsException("Feature no. "+featureIndex+" is not a byte-valued or short-valued feature");
    }
    
    /**
     * For the feature with the given index number, translate its byte or short value
     * to its String value.
     * This method must only be called for byte-valued or short-valued features.
     * @param featureIndex the index number of the feature.
     * @param value the feature value. This must be in the range of acceptable values for
     * the given feature.
     * @return for byte-valued and short-valued features, return the String representation
     * of the feature value.
     * @throws IndexOutOfBoundsException if featureIndex < 0 or 
     * featureIndex >= getNumberOfByteFeatures() + getNumberOfShortFeatures()
     * @throws IndexOutOfBoundsException if value is not a legal value for this feature
     * 
     * 
     */
    public String getFeatureValueAsString(int featureIndex, int value)
    {
        if (featureIndex < numByteFeatures)
            return byteFeatureValues[featureIndex].get((byte)value);
        featureIndex -= numByteFeatures;
        if (featureIndex < numShortFeatures)
            return shortFeatureValues[featureIndex].get((short)value);
        throw new IndexOutOfBoundsException("Feature no. "+featureIndex+" is not a byte-valued or short-valued feature");
    }

    /**
     * For the feature with the given name, translate its String value
     * to its byte value.
     * This method must only be called for byte-valued features.
     * @param featureName the name of the feature.
     * @param value the feature value. This must be among the acceptable values for
     * the given feature.
     * @return for byte-valued features, return the byte representation 
     * of the feature value.
     * @throws IllegalArgumentException if featureName is not a valid feature name,
     * or if featureName is not a byte-valued feature.
     * @throws IllegalArgumentException if value is not a legal value for this feature
     */
    public byte getFeatureValueAsByte(String featureName, String value)
    {
        int featureIndex = getFeatureIndex(featureName);
        if (featureIndex < numByteFeatures)
            return byteFeatureValues[featureIndex].get(value);
        throw new IndexOutOfBoundsException("Feature '"+featureName+"' is not a byte-valued feature");
    }

    /**
     * For the feature with the given name, translate its String value
     * to its short value.
     * This method must only be called for short-valued features.
     * @param featureName the name of the feature.
     * @param value the feature value. This must be among the acceptable values for
     * the given feature.
     * @return for short-valued features, return the short representation 
     * of the feature value.
     * @throws IllegalArgumentException if featureName is not a valid feature name,
     * or if featureName is not a short-valued feature.
     * @throws IllegalArgumentException if value is not a legal value for this feature
     */
    public short getFeatureValueAsShort(String featureName, String value)
    {
        int featureIndex = getFeatureIndex(featureName);
        featureIndex -= numByteFeatures;
        if (featureIndex < numShortFeatures)
            return shortFeatureValues[featureIndex].get(value);
        throw new IndexOutOfBoundsException("Feature '"+featureName+"' is not a short-valued feature");
    }

    public boolean featureEquals(FeatureDefinition other)
    {
        
    }
    
    public FeatureVector toFeatureVector(String featureString)
    {
        throw new IllegalArgumentException("Feature string '"+featureString+"' is inconsistent with feature definition");
    }
    
    /**
     * Create a feature vector consistent with this feature definition
     * by reading the data from the given input.
     * @param input a DataInputStream or RandomAccessFile to read the feature values from.
     * @return a FeatureVector.
     */
    public FeatureVector readFeatureVector(int currentUnitIndex, DataInput input) throws IOException
    {
        byte[] bytes = new byte[numberOfByteValues];
        input.readFully(bytes);
        short[] shorts = new short[numberOfShortValues];
        for (int i=0; i<shorts.length; i++) {
            shorts[i] = input.readShort();
        }
        float[] floats = new float[numberOfFloatValues];
        for (int i=0; i<floats.length; i++) {
            floats[i] = input.readFloat();
        }
        return new FeatureVector(bytes, shorts, floats, currentUnitIndex);
    }
    
    public FeatureVector getEmptyFeatureVector()
    {
        
    }
    
    public String toFeatureString(FeatureVector fv)
    {
        throw new IllegalArgumentException("Feature vector '"+fv+"' is inconsistent with feature definition");
        
    }
}
