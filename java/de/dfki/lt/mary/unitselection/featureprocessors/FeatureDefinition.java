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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Iterator;

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
    public static final String EDGEFEATURE = "mary_edge";
    public static final String EDGEFEATURE_START = "start";
    public static final String EDGEFEATURE_END = "end";
    public static final String NULLVALUE = "0";

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
     * weights from input. If weights are read, they will be normalized
     * so that they sum to one.
     * @throws IOException if a reading problem occurs
     *
     */
    public FeatureDefinition(BufferedReader input, boolean readWeights)
    throws IOException
    {
        // Section BYTEFEATURES
        String line = input.readLine();
        if (line == null) throw new IOException("Could not read from input");
        while ( line.matches("^\\s*#.*") || line.matches("\\s*") ) {
            line = input.readLine();
        }
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
        float sumOfWeights = 0; // for normalisation of weights
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
                sumOfWeights += featureWeights[i];
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
                sumOfWeights += featureWeights[numByteFeatures+i];
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
                String[] weightAndFunction = weightDef.split("\\s+", 2);
                featureWeights[numByteFeatures+numShortFeatures+i] = Float.parseFloat(weightAndFunction[0]);
                sumOfWeights += featureWeights[numByteFeatures+numShortFeatures+i];
                if (featureWeights[numByteFeatures+numShortFeatures+i] < 0) throw new IOException("Negative weight found in line '"+line+"'");
                try {
                    floatWeightFuncts[i] = weightAndFunction[1];
                }
                catch ( ArrayIndexOutOfBoundsException e ) {
//                    System.out.println( "weightDef string was: '" + weightDef + "'." );
//                    System.out.println( "Splitting part 1: '" + weightAndFunction[0] + "'." );
//                    System.out.println( "Splitting part 2: '" + weightAndFunction[1] + "'." );
                    throw new RuntimeException( "The string [" + weightDef + "] appears to be a badly formed"
                            + " weight plus weighting function definition." );
              }
            } else {
                featureDef = line;
            }
            // Now featureDef is the feature name
            // or the feature name followed by the word "float"
            if (featureDef.endsWith("float")){
                String[] featureDefSplit = featureDef.split("\\s+", 2);
                featureNames.set(numByteFeatures+numShortFeatures+i, featureDefSplit[0]);
            } else {
                featureNames.set(numByteFeatures+numShortFeatures+i, featureDef);
            }
        }
        // Normalize weights to sum to one:
        if (readWeights) {
            for (int i=0; i<total; i++) {
                featureWeights[i] /= sumOfWeights;
            }
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
        // There is no need to normalise weights here, because
        // they have already been normalized before the binary
        // file was written.
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
    public void writeBinaryTo(DataOutput out) throws IOException
    {
        // Section BYTEFEATURES
        out.writeInt(numByteFeatures);
        for (int i=0; i<numByteFeatures; i++) {
            out.writeFloat(featureWeights[i]);
            out.writeUTF(getFeatureName(i));
            byte numValues = (byte) getNumberOfValues(i);
            out.writeByte(numValues);
            for (byte b=0; b<numValues; b++) {
                String value = getFeatureValueAsString(i, b);
                out.writeUTF(value);
            }
        }
        // Section SHORTFEATURES
        out.writeInt(numShortFeatures);
        for (int i=numByteFeatures; i<numByteFeatures+numShortFeatures; i++) {
            out.writeFloat(featureWeights[i]);
            out.writeUTF(getFeatureName(i));
            short numValues = (short) getNumberOfValues(i);
            out.writeShort(numValues);
            for (short b=0; b<numValues; b++) {
                String value = getFeatureValueAsString(i, b);
                out.writeUTF(value);
            }
        }
        // Section CONTINUOUSFEATURES
        out.writeInt(numContinuousFeatures);
        for (int i=numByteFeatures+numShortFeatures;
                i<numByteFeatures+numShortFeatures+numContinuousFeatures; i++) {
            out.writeFloat(featureWeights[i]);
            out.writeUTF(floatWeightFuncts[i-numByteFeatures-numShortFeatures]);
            out.writeUTF(getFeatureName(i));
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
     * For the feature with the given index, return the weight.
     * @param featureIndex
     * @return a non-negative weight.
     */
    public float getWeight(int featureIndex)
    {
        return featureWeights[featureIndex];
    }
    
    /**
     * Get the name of any weighting function associated with the
     * given feature index. For byte-valued and short-valued features,
     * this method will always return null; for continuous features,
     * the method will return the name of a weighting function, or null.  
     * @param featureIndex
     * @return the name of a weighting function, or null
     */
    public String getWeightFunctionName(int featureIndex)
    {
        return floatWeightFuncts[featureIndex-numByteFeatures-numShortFeatures];
    }
    
    
    ////////////////////// META-INFORMATION METHODS ///////////////////////
    
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
     * Translate between an array of feature indexes and an array of feature names.
     * @param index an array of feature indexes, as could be used to access
     * a feature value in a FeatureVector.
     * @return an array with the name of the features corresponding to the index
     * @throws IndexOutOfBoundsException if any of the indexes is <0 or >getNumberOfFeatures() 
     */
    public String[] getFeatureNameArray(int[] index)
    {
        String[] ret = new String[index.length];
        for ( int i = 0; i < index.length; i++ ) {
            ret[i] = getFeatureName( index[i] );
        }
        return( ret );
    }
    
    /**
     * List all feature names, separated by white space,
     * in their order of definition.
     * @return
     */
    public String getFeatureNames()
    {
        StringBuffer buf = new StringBuffer();
        for (int i=0, n=getNumberOfFeatures(); i<n; i++) {
            if (buf.length() > 0) buf.append(" ");
            buf.append(featureNames.get(i));
        }
        return buf.toString();
    }
    
    /**
     * Determine whether the feature with the given name is a byte feature.
     * @param featureName
     * @return true if the feature is a byte feature, false if the feature
     * is not known or is not a byte feature 
     */
    public boolean isByteFeature(String featureName)
    {
        try {
            int index = getFeatureIndex(featureName);
            return isByteFeature(index);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Determine whether the feature with the given index number is a byte feature.
     * @param featureIndex
     * @return true if the feature is a byte feature, false if the feature
     * is not a byte feature or is invalid 
     */
    public boolean isByteFeature(int index)
    {
        if (0<=index && index < numByteFeatures) return true;
        else return false;
    }

    /**
     * Determine whether the feature with the given name is a short feature.
     * @param featureName
     * @return true if the feature is a short feature, false if the feature
     * is not known or is not a short feature 
     */
    public boolean isShortFeature(String featureName)
    {
        try {
            int index = getFeatureIndex(featureName);
            return isShortFeature(index);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Determine whether the feature with the given index number is a short feature.
     * @param featureIndex
     * @return true if the feature is a short feature, false if the feature
     * is not a short feature or is invalid 
     */
    public boolean isShortFeature(int index)
    {
        index -= numByteFeatures;
        if (0<=index && index < numShortFeatures) return true;
        else return false;
    }

    /**
     * Determine whether the feature with the given name is a continuous feature.
     * @param featureName
     * @return true if the feature is a continuous feature, false if the feature
     * is not known or is not a continuous feature 
     */
    public boolean isContinuousFeature(String featureName)
    {
        try {
            int index = getFeatureIndex(featureName);
            return isContinuousFeature(index);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Determine whether the feature with the given index number is a continuous feature.
     * @param featureIndex
     * @return true if the feature is a continuous feature, false if the feature
     * is not a continuous feature or is invalid 
     */
    public boolean isContinuousFeature(int index)
    {
        index -= numByteFeatures;
        index -= numShortFeatures;
        if (0<=index && index < numContinuousFeatures) return true;
        else return false;
    }

    /**
     * Translate between a feature name and a feature index.
     * @param featureName a valid feature name
     * @return a feature index, as could be used to access
     * a feature value in a FeatureVector.
     * @throws IllegalArgumentException if the feature name is unknown. 
     */
    public int getFeatureIndex(String featureName)
    {
        return featureNames.get(featureName);
    }
    
    /**
     * Translate between an array of feature names and an array of feature indexes.
     * @param featureName an array of valid feature names
     * @return an array of feature indexes, as could be used to access
     * a feature value in a FeatureVector.
     * @throws IllegalArgumentException if one of the feature names is unknown. 
     */
    public int[] getFeatureIndexArray(String[] featureName)
    {
        int[] ret = new int[featureName.length];
        for ( int i = 0; i < featureName.length; i++ ) {
            ret[i] = getFeatureIndex( featureName[i] );
        }
        return( ret );
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
            return byteFeatureValues[featureIndex].getNumberOfValues();
        featureIndex -= numByteFeatures;
        if (featureIndex < numShortFeatures)
            return shortFeatureValues[featureIndex].getNumberOfValues();
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
     * For the feature with the given index number, translate its String value
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
    public byte getFeatureValueAsByte(int featureIndex, String value)
    {
        if (featureIndex < numByteFeatures)
            return byteFeatureValues[featureIndex].get(value);
        throw new IndexOutOfBoundsException("Feature no. "+featureIndex+" is not a byte-valued feature");
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
    public short getFeatureValueAsShort(int featureIndex, String value)
    {
        featureIndex -= numByteFeatures;
        if (featureIndex < numShortFeatures)
            return shortFeatureValues[featureIndex].get(value);
        throw new IndexOutOfBoundsException("Feature no. "+featureIndex+" is not a short-valued feature");
    }
        

    /**
     * Determine whether two feature definitions are equal, with respect
     * to number, names, and possible values of the three kinds of features
     * (byte-valued, short-valued, continuous). This method does not compare
     * any weights.
     * @param other the feature definition to compare to
     * @return true if all features and values are identical, false otherwise
     */
    public boolean featureEquals(FeatureDefinition other)
    {
        if (numByteFeatures != other.numByteFeatures
                || numShortFeatures != other.numShortFeatures
                || numContinuousFeatures != other.numContinuousFeatures)
            return false;
        // Compare the feature names and values for byte and short features:
        for (int i=0; i<numByteFeatures+numShortFeatures+numContinuousFeatures; i++) {
            if (!getFeatureName(i).equals(other.getFeatureName(i)))
                return false;
        }
        // Compare the values for byte and short features:
        for (int i=0; i<numByteFeatures+numShortFeatures; i++) {
            if (getNumberOfValues(i) != other.getNumberOfValues(i))
                return false;
            for (int v=0, n=getNumberOfValues(i); v<n; v++) {
                if (!getFeatureValueAsString(i, v).equals(other.getFeatureValueAsString(i, v)))
                    return false;
            }
        }
        return true;
    }
    
    /**
     * An extension of the previous method.
     */
    public String featureEqualsAnalyse(FeatureDefinition other)
    {
        if (numByteFeatures != other.numByteFeatures) {
            return( "The number of BYTE features differs: " + numByteFeatures + " versus " + other.numByteFeatures );
        }
        if (numShortFeatures != other.numShortFeatures) {
            return( "The number of SHORT features differs: " + numShortFeatures + " versus " + other.numShortFeatures );
        }
        if (numContinuousFeatures != other.numContinuousFeatures) {
            return( "The number of CONTINUOUS features differs: " + numContinuousFeatures + " versus " + other.numContinuousFeatures );
        }
        // Compare the feature names and values for byte and short features:
        for (int i=0; i<numByteFeatures+numShortFeatures+numContinuousFeatures; i++) {
            if (!getFeatureName(i).equals(other.getFeatureName(i))) {
                return( "The feature name differs at position [" + i + "]: " + getFeatureName(i)
                        + " versus " + other.getFeatureName(i) );
            }
        }
        // Compare the values for byte and short features:
        for (int i=0; i<numByteFeatures+numShortFeatures; i++) {
            if (getNumberOfValues(i) != other.getNumberOfValues(i)) {
                return( "The number of values differs at position [" + i + "]: " + getNumberOfValues(i)
                    + " versus " + other.getNumberOfValues(i) );
            }
            for (int v=0, n=getNumberOfValues(i); v<n; v++) {
                if (!getFeatureValueAsString(i, v).equals(other.getFeatureValueAsString(i, v))) {
                    return( "The feature value differs at position [" + i + "] for feature value [" + v + "]: "
                            + getFeatureValueAsString(i, v)
                            + " versus " + other.getFeatureValueAsString(i, v) );
                }
            }
        }
        return "";
    }
    
    /**
     * Determine whether two feature definitions are equal, regarding both
     * the actual feature definitions and the weights.
     * The comparison of weights will succeed if both have no weights or
     * if both have exactly the same weights
     * @param other the feature definition to compare to
     * @return true if all features, values and weights are identical, false otherwise
     * @see #featureEquals(FeatureDefinition)
     */
    public boolean equals(FeatureDefinition other)
    {
        if (featureWeights == null) {
            if (other.featureWeights != null) return false;
            // Both are null
        } else { // featureWeights != null
            if (other.featureWeights == null) return false;
            // Both != null
            if (featureWeights.length != other.featureWeights.length) return false;
            for (int i=0; i<featureWeights.length; i++) {
                if (featureWeights[i] != other.featureWeights[i]) return false;
            }
            assert floatWeightFuncts != null;
            assert other.floatWeightFuncts != null;
            if (floatWeightFuncts.length != other.floatWeightFuncts.length) return false;
            for (int i=0; i<floatWeightFuncts.length; i++) {
                if (floatWeightFuncts[i] == null) {
                    if (other.floatWeightFuncts[i] != null) return false;
                    // Both are null
                } else { // != null
                    if (other.floatWeightFuncts[i] == null) return false;
                    // Both != null
                    if (!floatWeightFuncts[i].equals(other.floatWeightFuncts[i])) return false;
                }
            }
        }
        // OK, weights are equal
        return featureEquals(other);
    }
    
    /**
     * Create a feature vector consistent with this feature definition
     * by reading the data from a String representation. In that String,
     * the String values for each feature must be separated by white space.
     * For example, this format is created by toFeatureString(FeatureVector).
     * @param unitIndex an index number to assign to the feature vector
     * @param featureString the string representation of a feature vector.
     * @return the feature vector created from the String.
     * @throws IllegalArgumentException if the feature values listed are not
     * consistent with the feature definition.
     * @see #toFeatureString(FeatureVector)
     */
    public FeatureVector toFeatureVector(int unitIndex, String featureString)
    {
        String[] featureValues = featureString.split("\\s+");
        if (featureValues.length != numByteFeatures+numShortFeatures+numContinuousFeatures)
            throw new IllegalArgumentException("Expected "+(numByteFeatures+numShortFeatures+numContinuousFeatures)+" features, got "+featureValues.length);
        byte[] bytes = new byte[numByteFeatures];
        short[] shorts = new short[numShortFeatures];
        float[] floats = new float[numContinuousFeatures];
        for (int i=0; i<numByteFeatures; i++) {
            bytes[i] = Byte.parseByte( featureValues[i] );
        }
        for (int i=0; i<numShortFeatures; i++) {
            shorts[i] = Short.parseShort( featureValues[numByteFeatures+i] );
        }
        for (int i=0; i<numContinuousFeatures; i++) {
            floats[i] = Float.parseFloat(featureValues[numByteFeatures+numShortFeatures+i]);
        }
        return new FeatureVector(bytes, shorts, floats, unitIndex);
    }
    
    /**
     * Create a feature vector consistent with this feature definition
     * by reading the data from the given input.
     * @param input a DataInputStream or RandomAccessFile to read the feature values from.
     * @return a FeatureVector.
     */
    public FeatureVector readFeatureVector(int currentUnitIndex, DataInput input) throws IOException
    {
        byte[] bytes = new byte[numByteFeatures];
        input.readFully(bytes);
        short[] shorts = new short[numShortFeatures];
        for (int i=0; i<shorts.length; i++) {
            shorts[i] = input.readShort();
        }
        float[] floats = new float[numContinuousFeatures];
        for (int i=0; i<floats.length; i++) {
            floats[i] = input.readFloat();
        }
        return new FeatureVector(bytes, shorts, floats, currentUnitIndex);
    }
    
    /**
     * Create a feature vector that marks a start or end of a unit.
     * All feature values are set to the neutral value "0", except for
     * the EDGEFEATURE, which is set to start if start == true, to end otherwise. 
     * @param unitIndex index of the unit
     * @param start true creates a start vector, false creates an end vector. 
     * @return a feature vector representing an edge.
     */
    public FeatureVector createEdgeFeatureVector(int unitIndex, boolean start)
    {
        int edgeFeature = getFeatureIndex(EDGEFEATURE);
        assert edgeFeature < numByteFeatures; // we can assume this is byte-valued
        byte edge;
        if (start) edge = getFeatureValueAsByte(edgeFeature, EDGEFEATURE_START);
        else edge = getFeatureValueAsByte(edgeFeature, EDGEFEATURE_END);
        byte[] bytes = new byte[numByteFeatures];
        short[] shorts = new short[numShortFeatures];
        float[] floats = new float[numContinuousFeatures];
        for (int i=0; i<numByteFeatures; i++) {
            bytes[i] = getFeatureValueAsByte(i, NULLVALUE);
        }
        for (int i=0; i<numShortFeatures; i++) {
            shorts[i] = getFeatureValueAsShort(numByteFeatures+i, NULLVALUE);
        }
        for (int i=0; i<numContinuousFeatures; i++) {
            floats[i] = 0;
        }
        bytes[edgeFeature] = edge;
        return new FeatureVector(bytes, shorts, floats, unitIndex); 
    }
    
    /**
     * Convert a feature vector into a String representation. 
     * @param fv a feature vector which must be consistent with this feature definition.
     * @return a String containing the String values of all features, separated by white space.
     * @throws IllegalArgumentException if the feature vector is not consistent with this
     * feature definition
     * @throws IndexOutOfBoundsException if any value of the feature vector is not consistent with this
     * feature definition 
     */
    public String toFeatureString(FeatureVector fv)
    {
        if (numByteFeatures != fv.getNumberOfByteFeatures()
                || numShortFeatures != fv.getNumberOfShortFeatures()
                || numContinuousFeatures != fv.getNumberOfContinuousFeatures())
            throw new IllegalArgumentException("Feature vector '"+fv+"' is inconsistent with feature definition");
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<numByteFeatures; i++) {
            if (buf.length()>0) buf.append(" ");
            buf.append(getFeatureValueAsString(i, fv.getByteFeature(i)));
        }
        for (int i=numByteFeatures; i<numByteFeatures+numShortFeatures; i++) {
            if (buf.length()>0) buf.append(" ");
            buf.append(getFeatureValueAsString(i, fv.getShortFeature(i)));
        }
        for (int i=numByteFeatures+numShortFeatures; i<numByteFeatures+numShortFeatures+numContinuousFeatures; i++) {
            if (buf.length()>0) buf.append(" ");
            buf.append(fv.getContinuousFeature(i));
        }
        return buf.toString();
    }
    
    /**
     * Export this feature definition in the text format which can also be read by this class.
     * @param out the destination of the data
     * @param writeWeights whether to write weights before every line
     */
    public void writeTo(PrintWriter out, boolean writeWeights)
    {
        out.println("ByteValuedFeatureProcessors");
        for (int i=0; i<numByteFeatures; i++) {            
            if (writeWeights) {
                out.print(featureWeights[i]+" | ");
            }
            out.print(getFeatureName(i));
            for (int v=0, vmax=getNumberOfValues(i); v<vmax; v++) {
                out.print(" ");
                String val = getFeatureValueAsString(i, v);
                out.print(val);
            }
            out.println();
        }
        out.println("ShortValuedFeatureProcessors");
        for (int i=0; i<numShortFeatures; i++) {            
            if (writeWeights) {
                out.print(featureWeights[numByteFeatures+i]+" | ");
            }
            out.print(getFeatureName(numByteFeatures+i));
            for (int v=0, vmax=getNumberOfValues(numByteFeatures+i); v<vmax; v++) {
                out.print(" ");
                String val = getFeatureValueAsString(numByteFeatures+i, v);
                out.print(val);
            }
            out.println();
        }
        out.println("ContinuousFeatureProcessors");
        for (int i=0; i<numContinuousFeatures; i++) {
            if (writeWeights) {
                out.print(featureWeights[numByteFeatures+numShortFeatures+i]);
                out.print(" ");
                out.print(floatWeightFuncts[i]);
                out.print(" | ");
            }

            out.print(getFeatureName(numByteFeatures+numShortFeatures+i));
            out.println();
        }
        
    }

    
    /**
     * Export this feature definition in the "all.desc" format which can be
     * read by wagon.
     * @param out the destination of the data
     */
    public void generateAllDotDescForWagon(PrintWriter out)
    {
        generateAllDotDescForWagon(out, null);
    }
    
    /**
     * Export this feature definition in the "all.desc" format which can be
     * read by wagon.
     * @param out the destination of the data
     * @param featuresToIgnore a set of Strings containing the names of features that 
     * wagon should ignore. Can be null.
     */
    public void generateAllDotDescForWagon(PrintWriter out, Set featuresToIgnore)
    {
        out.println("(");
        out.println("(occurid cluster)");
        for (int i=0, n=getNumberOfFeatures(); i<n; i++) {            
            out.print("( ");
            String featureName = getFeatureName(i);
            out.print(featureName);
            if (featuresToIgnore != null && featuresToIgnore.contains(featureName)) {
                out.print(" ignore");
            }
            if (i<numByteFeatures+numShortFeatures) { // list values
                    for (int v=0, vmax=getNumberOfValues(i); v<vmax; v++) {
                        out.print("  ");
                        // Print values surrounded by double quotes, and make sure any
                        // double quotes in the value are preceded by a backslash --
                        // otherwise, we get problems e.g. for mary_sentence_punc
                        String val = getFeatureValueAsString(i, v);
                        if (val.indexOf('"') != -1) {
                            StringBuffer buf = new StringBuffer();
                            for (int c=0; c<val.length(); c++) {
                                char ch = val.charAt(c);
                                if (ch == '"') buf.append("\\\"");
                                else buf.append(ch);
                            }
                            val = buf.toString();
                        }
                        out.print("\""+val+"\"");
                    }
                    
                    out.println(" )");
             } else { // float feature
                    out.println(" float )");
             }
           
        }
        out.println(")");
    }
    
    /**
     * Print this feature definition plus weights to a .txt file
     * @param out the destination of the data
     */
    public void generateFeatureWeightsFile(PrintWriter out)
    {
        out.println("# This file lists the features and their weights to be used for\n"
                +"# creating the MARY features file.\n"
                +"# The same file can also be used to override weights in a run-time system.\n"
                +"# Three sections are distinguished: Byte-valued, Short-valued, and\n"
                +"# Continuous features.\n"
                +"#\n"
                +"# Lines starting with '#' are ignored; they can be used for comments\n"
                +"# anywhere in the file. Empty lines are also ignored.\n"
                +"# Entries must have the following form:\n"
                +"# \n"
                +"# <weight definition> | <feature definition>\n"
                +"# \n"
                +"# For byte and short features, <weight definition> is simply the \n"
                +"# (float) number representing the weight.\n"
                +"# For continuous features, <weight definition> is the\n"
                +"# (float) number representing the weight, followed by an optional\n"
                +"# weighting function including arguments.\n"
                +"#\n"
                +"# The <feature definition> is the feature name, which in the case of\n"
                +"# byte and short features is followed by the full list of feature values.\n"
                +"#\n"
                +"# Note that the feature definitions must be identical between this file\n"
                +"# and all unit feature files for individual database utterances.\n"
                +"# THIS FILE WAS GENERATED AUTOMATICALLY");
        out.println();
        out.println("ByteValuedFeatureProcessors");
        List getValuesOf10 = new ArrayList();
        getValuesOf10.add("mary_phoneme");
        getValuesOf10.add("mary_ph_vc");
        getValuesOf10.add("mary_prev_phoneme");
        getValuesOf10.add("mary_next_phoneme");
        getValuesOf10.add("mary_stressed");
        getValuesOf10.add("mary_syl_break");
        getValuesOf10.add("mary_prev_syl_break");
        getValuesOf10.add("mary_next_is_pause");
        getValuesOf10.add("mary_prev_is_pause");
        List getValuesOf5 = new ArrayList();
        getValuesOf5.add("cplace");
        getValuesOf5.add("ctype");
        getValuesOf5.add("cvox");
        getValuesOf5.add("vfront");
        getValuesOf5.add("vheight");
        getValuesOf5.add("vlng");
        getValuesOf5.add("vrnd");
        getValuesOf5.add("vc");
        for (int i=0; i<numByteFeatures; i++) { 
            String featureName = getFeatureName(i);
            if (getValuesOf10.contains(featureName)){
                out.print("10 | "+featureName);
            } else {
                boolean found = false;
                for (Iterator it = getValuesOf5.iterator();it.hasNext();){
                    if (featureName.matches(".*"+(String)it.next())){
                        out.print("5 | "+featureName);
                        found=true;
                        break;
                    }
                } 
                if (!found){
                    out.print("0 | "+featureName);
                }
            }
            for (int v=0, vmax=getNumberOfValues(i); v<vmax; v++) {
                String val = getFeatureValueAsString(i, v);
                out.print(" "+val);
            }            
            out.print("\n");
        }
        out.println("ShortValuedFeatureProcessors");
        for (int i=numByteFeatures; i<numShortFeatures; i++) { 
            String featureName = getFeatureName(i);
            out.print("0 | "+featureName);
            for (int v=0, vmax=getNumberOfValues(i); v<vmax; v++) {
                String val = getFeatureValueAsString(i, v);
                out.print(" "+val);
            }            
            out.print("\n");
        }
        out.println("ContinuousFeatureProcessors");
        for (int i=numByteFeatures; i<numByteFeatures+numContinuousFeatures; i++) { 
            String featureName = getFeatureName(i);
            out.println("0 linear | "+featureName);   
        }
        out.flush();
        out.close();
    }
    
    
    
    
    /**
     * Compares two feature vectors in terms of how many discrete features they have in common.
     * WARNING: this assumes that the feature vectors are issued from the same
     * FeatureDefinition; only the number of features is checked for compatibility.
     * 
     * @param v1 A feature vector.
     * @param v2 Another feature vector to compare v1 with.
     * @return The number of common features.
     */
    public static int diff( FeatureVector v1, FeatureVector v2 ) {
        
        int ret = 0;
        
        /* Byte valued features */
        if ( v1.byteValuedDiscreteFeatures.length < v2.byteValuedDiscreteFeatures.length ) {
            throw new RuntimeException( "v1 and v2 don't have the same number of byte-valued features: ["
                    + v1.byteValuedDiscreteFeatures.length + "] versus [" + v2.byteValuedDiscreteFeatures.length
                    + "]." );
        }
        for ( int i = 0; i < v1.byteValuedDiscreteFeatures.length; i++ ) {
            if ( v1.byteValuedDiscreteFeatures[i] == v2.byteValuedDiscreteFeatures[i] ) ret++;
        }
        
        /* Short valued features */
        if ( v1.shortValuedDiscreteFeatures.length < v2.shortValuedDiscreteFeatures.length ) {
            throw new RuntimeException( "v1 and v2 don't have the same number of short-valued features: ["
                    + v1.shortValuedDiscreteFeatures.length + "] versus [" + v2.shortValuedDiscreteFeatures.length
                    + "]." );
        }
        for ( int i = 0; i < v1.shortValuedDiscreteFeatures.length; i++ ) {
            if ( v1.shortValuedDiscreteFeatures[i] == v2.shortValuedDiscreteFeatures[i] ) ret++;
        }
        
        /* TODO: would checking float-valued features make sense ? (Code below.) */
        /* float valued features */
        /* if ( v1.continuousFeatures.length < v2.continuousFeatures.length ) {
            throw new RuntimeException( "v1 and v2 don't have the same number of continuous features: ["
                    + v1.continuousFeatures.length + "] versus [" + v2.continuousFeatures.length
                    + "]." );
        }
        float epsilon = 1.0e-6f;
        float d = 0.0f;
        for ( int i = 0; i < v1.continuousFeatures.length; i++ ) {
            d = ( v1.continuousFeatures[i] > v2.continuousFeatures[i] ?
                    (v1.continuousFeatures[i] - v2.continuousFeatures[i]) :
                    (v2.continuousFeatures[i] - v1.continuousFeatures[i]) ); // => this avoids Math.abs()
            if ( d < epsilon ) ret++;
        } */
        
        return( ret );
    }

}
