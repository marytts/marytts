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

import de.dfki.lt.mary.unitselection.FeatureArrayIndexer;
import de.dfki.lt.mary.unitselection.MaryNode;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;

/**
 * A representation of a classification tree. Classification trees are trees
 * with clusters of items at their leaves, identified by index numbers.
 * @author marc
 *
 */
public class ExtendedClassificationTree extends WagonCART
{

    public ExtendedClassificationTree()
    {
        super();
    }
   
    
    /**
     * @param reader
     * @param featDefinition
     * @throws IOException
     */
    public ExtendedClassificationTree(BufferedReader reader, FeatureDefinition featDefinition)
            throws IOException {
        super(reader, featDefinition);
    }
    
    /**
     * For a line representing a leaf in Wagon format, create a leaf.
     * This method decides which implementation of LeafNode is used, i.e.
     * which data format is appropriate.
     * This implementation creates an IntAndFloatArrayLeafNode, representing the leaf
     * as an array of ints and floats.
     * Lines are of the form
     * ((<index1> <float1>)...(<indexN> <floatN>)) 0))
     * 
     * @param line a line from a wagon cart file, representing a leaf
     * @return a leaf node representing the line.
     */
    protected LeafNode createLeafNode(String line) {
        StringTokenizer tok = new StringTokenizer(line, " ");
        // read the indices from the tokenized String
        int numTokens = tok.countTokens();
        int index = 0;
        // The data to be saved in the leaf node:
        int[] indices;
        // The floats to be saved in the leaf node:
        float[] probs;
        
        //System.out.println("Line: "+line+", numTokens: "+numTokens);
        
        if (numTokens == 2) { // we do not have any indices
            // discard useless token
            tok.nextToken();
            indices = new int[0];
            probs = new float[0];
        } else {
            indices = new int[(numTokens - 1) / 2];
            // same length
            probs = new float[indices.length];
            
            while (index * 2 < numTokens - 1){
            	String token = tok.nextToken();
            	if (index == 0){
            		token = token.substring(4);
            	}else{
            		token = token.substring(1);
            	}
            	//System.out.println("int-token: "+token);
            	indices[index] = Integer.parseInt(token);
            		
            	token = tok.nextToken();
            	int lastIndex = token.length() - 1;
           		if ((index*2) == (numTokens - 3)){
           			token = token.substring(0,lastIndex-1);
           			if (token.equals("inf")){
                		probs[index]=10000;
                		index++;
                		continue;
                	}
           			if (token.equals("nan")){
                		probs[index]=-1;
                		index++;
                		continue;
                	}
           		}else{
           			token = token.substring(0,lastIndex);
           			if (token.equals("inf")){
                		probs[index]=1000000;
                		index++;
                		continue;
                	}
           			if (token.equals("nan")){
                		probs[index]=-1;
                		index++;
                		continue;
                	}
           		}
           		//System.out.println("float-token: "+token);
           		probs[index] = Float.parseFloat(token);
           		index++;	
           	} // end while
       
        } // end if
        
        return new LeafNode.IntAndFloatArrayLeafNode(indices,probs);
    }

}
