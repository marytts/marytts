/**
 * Copyright 2007 DFKI GmbH.
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

package de.dfki.lt.mary.unitselection.cart;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;

import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;

/**
 * A representation of a regression tree. Regression trees are CARTs which have
 * a float mean/stddev pair at their leaves.
 * @author marc
 *
 */
public class RegressionTree extends WagonCART {

    public RegressionTree()
    {
        super();
    }
    
    /**
     * @param reader
     * @param featDefinition
     * @throws IOException
     */
    public RegressionTree(BufferedReader reader, FeatureDefinition featDefinition)
            throws IOException {
        super(reader, featDefinition);
        // TODO Auto-generated constructor stub
    }

    /**
     * For a line representing a leaf in Wagon format, create a leaf.
     * This method decides which implementation of LeafNode is used, i.e.
     * which data format is appropriate.
     * This implementation creates an FloatArrayLeafNode, representing the leaf
     * as an array of floats.
     * Lines are of the form
     * ((<floatStdDev> <floatMean>))
     * 
     * @param line a line from a wagon cart file, representing a leaf
     * @return a leaf node representing the line.
     */
    protected LeafNode createLeafNode(String line) {
        StringTokenizer tok = new StringTokenizer(line, " ");
        // read the indices from the tokenized String
        int numTokens = tok.countTokens();
        if (numTokens != 2) { // we need exactly one value pair
            throw new IllegalArgumentException("Expected two tokens in line, got "+numTokens+": '"+line+"'");
        }

        // The data to be saved in the leaf node:
        float[] data = new float[2]; // stddev and mean;
        String nextToken = tok.nextToken();
        nextToken = nextToken.substring(2);
        try {
            data[0] = Float.parseFloat(nextToken);
        } catch (NumberFormatException nfe) {
            data[0] = 0; // cannot make sense of the standard deviation
        }
        nextToken = tok.nextToken();
        nextToken = nextToken.substring(0, nextToken.indexOf(")"));
        try {
            data[1] = Float.parseFloat(nextToken);
        } catch (NumberFormatException nfe) {
            data[1] = 0;
        }
        return new LeafNode.FloatLeafNode(data);
    }

}
