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
    
    public FeatureDefinition()
    {
        
    }
    
    public void readTextWithWeights(BufferedReader r)
    {
        readText(r, true);
    }
    
    public void readTextWithoutWeights(BufferedReader r)
    {
        readText(r, false);
    }
    
    protected void readText(BufferedReader r, boolean withWeights)
    {
        
    }
    
    public void readBinary(DataInput input) throws IOException
    {
        
    }
    
    public void writeBinary(DataOutput out) throws IOException
    {
        
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
    public FeatureVector readFeatureVector(DataInput input) throws IOException
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
        return new FeatureVector(bytes, shorts, floats);
    }
    
    public FeatureVector getEmptyFeatureVector()
    {
        
    }
    
    public String toFeatureString(FeatureVector fv)
    {
        throw new IllegalArgumentException("Feature vector '"+fv+"' is inconsistent with feature definition");
        
    }
}
