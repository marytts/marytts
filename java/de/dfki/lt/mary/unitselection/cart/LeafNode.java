package de.dfki.lt.mary.unitselection.cart;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;

/**
 * The leaf of a CART.
 */
public abstract class LeafNode extends Node {



    /**
     * Create a new LeafNode.
     * 
     * @param tok
     *            the String Tokenizer containing the String with the
     *            indices
     * @param openBrackets
     *            the number of opening brackets at the first token
     */
    public LeafNode()
    {
        super();
        isRoot = false;
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
     * Count all the data available at and below this node.
     * The meaning of this depends on the type of nodes; for example,
     * when IntArrayLeafNodes are used, it is the total number of ints
     * that are saved in all leaf nodes below the current node.
     * @return an int counting the data below the current node, or -1
     * if such a concept is not meaningful.
     */
    public abstract int getNumberOfData();

    /**
     * Get all the data at or below this node.
     * The type of data returned depends on the type of nodes; for example,
     * when IntArrayLeafNodes are used, one int[] is returned which contains all
     * int values in all leaf nodes below the current node.
     * @return an object containing all data below the current node, or null
     * if such a concept is not meaningful.
     */
    public abstract Object getAllData();

    /**
     * Write this node's data into the target object at pos,
     * making sure that exactly len data are written.
     * The type of data written depends on the type of nodes; for example,
     * when IntArrayLeafNodes are used, target would be an int[].
     * @param array the object to write to, usually an array.
     * @param pos the position in the target at which to start writing
     * @param len the amount of data items to write, usually equals
     * getNumberOfData().
     */
    protected abstract void fillData(Object target, int pos, int len);


    public static class IntArrayLeafNode extends LeafNode
    {
        private int[] data;
        public IntArrayLeafNode(int[] data)
        {
            super();
            this.data = data;
        }
        
        /**
         * Get all data in this leaf
         * 
         * @return the int array contained in this leaf
         */
        public Object getAllData() {
            return data;
        }
        
        protected void fillData(Object target, int pos, int len)
        {
            if (!(target instanceof int[])) 
                throw new IllegalArgumentException("Expected target object of type int[], got "+target.getClass());
            int[] array = (int[]) target;
            assert len <= data.length;
            System.arraycopy(data, 0, array, pos, len);
        }

        public int getNumberOfData()
        {
            if (data != null) return data.length;
            return 0;
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
            StringBuffer sb = new StringBuffer();
            // open three brackets
            sb.append("(((");
            // for each index, write the index and then a pseudo float
            for (int i = 0; i < data.length; i++) {
                sb.append("(" + data[i] + " 0)");
                if (i + 1 != data.length) {
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

    public static class FeatureVectorLeafNode extends LeafNode
    {
        private FeatureVector[] featureVectors;

        /**
         * Build a new leaf node containing the given feature vectors
         * 
         * @param featureVectors
         *            the feature vectors
         */
        public FeatureVectorLeafNode(FeatureVector[] featureVectors) {
            super();
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
         * Get all data in this leaf
         * 
         * @return the featurevector array contained in this leaf
         */
        public Object getAllData() {
            return featureVectors;
        }
        
        protected void fillData(Object target, int pos, int len)
        {
            if (!(target instanceof FeatureVector[])) 
                throw new IllegalArgumentException("Expected target object of type FeatureVector[], got "+target.getClass());
            FeatureVector[] array = (FeatureVector[]) target;
            assert len <= featureVectors.length;
            System.arraycopy(featureVectors, 0, array, pos, len);
        }

        public int getNumberOfData()
        {
            if (featureVectors != null) return featureVectors.length;
            return 0;
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
            StringBuffer sb = new StringBuffer();
            // open three brackets
            sb.append("(((");
            // for each index, write the index and then a pseudo float
            for (int i = 0; i < featureVectors.length; i++) {
                sb.append("(" + featureVectors[i].getUnitIndex() + " 0)");
                if (i + 1 != featureVectors.length) {
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
}