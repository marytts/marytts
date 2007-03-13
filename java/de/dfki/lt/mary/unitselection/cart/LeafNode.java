package de.dfki.lt.mary.unitselection.cart;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;

/**
 * The leaf of a CART.
 */
public class LeafNode extends Node {

    private int[] indices;

    private FeatureVector[] featureVectors;

    /**
     * Create a new LeafNode.
     * 
     * @param tok
     *            the String Tokenizer containing the String with the
     *            indices
     * @param openBrackets
     *            the number of opening brackets at the first token
     */
    public LeafNode(StringTokenizer tok) {
        super();
        isRoot = false;
        // read the indices from the tokenized String
        // lines are of form
        // ((<index1> <float1>)...(<indexN> <floatN>)) 0))
        int numTokens = tok.countTokens();
        int index = 0;
        if (numTokens == 2) { // we do not have any indices
            // discard useless token
            tok.nextToken();
            indices = new int[0];
        } else {
            indices = new int[(numTokens - 1) / 2];

            while (index * 2 < numTokens - 1) { // while we are not at the
                                                // last token
                String nextToken = tok.nextToken();
                if (index == 0) {
                    // we are at first token, discard all open brackets
                    nextToken = nextToken.substring(4);
                } else {
                    // we are not at first token, only one open bracket
                    nextToken = nextToken.substring(1);
                }
                // store the index of the unit
                indices[index] = Integer.parseInt(nextToken);
                // discard next token
                tok.nextToken();
                // increase index
                index++;
            }
        }
    }

    /**
     * Build a new leaf node containing the given feature vectors
     * 
     * @param featureVectors
     *            the feature vectors
     */
    public LeafNode(FeatureVector[] featureVectors) {
        this.featureVectors = featureVectors;
    }

    /**
     * Get the feature vectors of this node
     * 
     * @return the feature vectors
     */
    public FeatureVector[] getFeatureVectors() {
        return featureVectors;
    }

    /**
     * Get all unit indices
     * 
     * @return an int array containing the indices
     */
    public int[] getAllIndices() {
        return indices;
    }
    
    protected void fillIndexArray(int[] array, int pos, int len)
    {
        assert len <= indices.length;
        System.arraycopy(indices, 0, array, pos, len);
    }
    
    public int getNumberOfCandidates()
    {
        if (indices != null) return indices.length;
        else if (featureVectors != null) return featureVectors.length;
        else return 0;
    }
    
    /**
     * Return the leaf node following this one in the tree.
     * @return the next leaf node, or null if this one is the last one.
     */
    public LeafNode getNextLeafNode()
    {
        if (mother == null) return null;
        assert mother instanceof DecisionNode;
        return ((DecisionNode)mother).getNextLeafNode(getNodeIndex()+1);
    }

    /**
     * Retrieve the indices from the feature vectors and store them in the
     * indices field
     */
    private void retrieveIndices() {
        
        indices = new int[featureVectors.length];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = featureVectors[i].getUnitIndex();
        }
    }

    /**
     * Writes the Cart to the given DataOut in Wagon Format
     * 
     * @param out
     *            the outputStream
     * @param extension
     *            the extension that is added to the last daughter
     */
    public void toWagonFormat(DataOutputStream out, String extension,
            PrintWriter pw) throws IOException {
        if (indices == null) {
            // get the indices from the feature vectors
            retrieveIndices();
        }
        StringBuffer sb = new StringBuffer();
        // open three brackets
        sb.append("(((");
        // for each index, write the index and then a pseudo float
        for (int i = 0; i < indices.length; i++) {
            sb.append("(" + indices[i] + " 0)");
            if (i + 1 != indices.length) {
                sb.append(" ");
            }
        }
        // write the ending
        sb.append(") 0))" + extension);
        // dump the whole stuff
        if (out != null) {
            // write to output stream

            CARTWagonFormat.writeStringToOutput(sb.toString(), out);
        } else {
            // write to Standard out
            // System.out.println(sb.toString());
        }
        if (pw != null) {
            // dump to printwriter
            pw.print(sb.toString());
        }
    }

}