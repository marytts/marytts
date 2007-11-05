package de.dfki.lt.mary.unitselection.cart;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

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

    public String getDecisionPath()
    {
        if (mother == null) return "null - "+toString();
        assert mother instanceof DecisionNode;
        return ((DecisionNode)mother).getDecisionPath(getNodeIndex()) + " - " + toString();
    }
    
    /**
     * Count all the nodes at and below this node.
     * A leaf will return 1; the root node will 
     * report the total number of decision and leaf nodes
     * in the tree.
     * @return
     */
    public int getNumberOfNodes()
    {
        return 1;
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


    /**
     * An LeafNode class suitable for representing the leaves of 
     * classification trees -- the leaf is a collection of items identified
     * by an index number.
     * @author marc
     *
     */
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

                CART.writeStringToOutput(sb.toString(), out);
            } else {
                // write to Standard out
                // System.out.println(sb.toString());
            }
            if (pw != null) {
                // dump to printwriter
                pw.print(sb.toString());
            }
        }
        
        public String toString()
        {
            if (data == null) return "int[null]";
            return "int["+data.length+"]";
        }

    }

    public static class IntAndFloatArrayLeafNode extends LeafNode{
    	
    	private int[] data;
    	private float[] floats;
    	
    	public IntAndFloatArrayLeafNode(int[] data, float[] floats)
        {
            super();
            this.data = data;
            this.floats = floats;
        }
    	
    	protected void fillData(Object target, int pos, int len){
    		System.out.println("Not implemented for IntAndFloatArrayLeafNode");
    	}
    	
    	public int getNumberOfData(){
    		if (data != null) return data.length;
    		return 0;
    	}
    	
    	public Object getAllData(){
    		return data;
    	}
        
    	public int[] getIntData(){
            return data;
        }
        
    	public float[] getFloatData(){
    		return floats;
    	}

    	/**
         * Delete a candidate of the leaf by its given data/index
         * @param target
         *            the given data
         */
        public void eraseData(int target){
        	int[] newData = new int[data.length-1];
        	float[] newFloats = new float[floats.length-1];
        	int index = 0;
        	for (int i = 0; i < data.length; i++){
        		if (data[i] != target){
        			newData[index] = data[i];
        			newFloats[index] = floats[i];
        			index++;
        		}
        	}
        	data = newData;
        	floats = newFloats;
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
            // for each index, write the index and then its float
            for (int i = 0; i < data.length; i++) {
                sb.append("(" + data[i] + " "+floats[i]+")");
                if (i + 1 != data.length) {
                    sb.append(" ");
                }
            }
            // write the ending
            sb.append(") 0))" + extension);
            // dump the whole stuff
            if (out != null) {
                // write to output stream

                CART.writeStringToOutput(sb.toString(), out);
            } else {
                // write to Standard out
                // System.out.println(sb.toString());
            }
            if (pw != null) {
                // dump to printwriter
                // TODO: change print to println
                pw.print(sb.toString());
            }
        }
        
        /**
         * This returns a String representation of this node. A prefix is given to
         * indent the nodes.
         */
        public String toString(String prefix){
            StringBuffer sb = new StringBuffer();
            
            String lineBreak = System.getProperty("line.separator");
            
            int maxData = 0;
            float maxFloat = 0.0f;
            
            sb.append(lineBreak);
            // open three brackets
            sb.append(prefix + "(((");
            // for each index, write the index and then its float
            for (int i = 0; i < data.length; i++) {
                
                if ( floats[i] > maxFloat ){
                    maxFloat = floats[i];
                    maxData = data[i];
                }
                
                sb.append("(" + data[i] + " "+floats[i]+")");
                if (i + 1 != data.length) {
                    sb.append(" ");
                }
            }
            // write the ending
            sb.append(") " + maxData+ "))");
                    
            return sb.toString();
        }
    }
    

    
    public static class FeatureVectorLeafNode extends LeafNode
    {
        private FeatureVector[] featureVectors;
        private List featureVectorList;
        private boolean growable;

        /**
         * Build a new leaf node containing the given feature vectors
         * 
         * @param featureVectors
         *            the feature vectors
         */
        public FeatureVectorLeafNode(FeatureVector[] featureVectors) {
            super();
            this.featureVectors = featureVectors;
            growable = false;
        }

        /** 
         * Build a new, empty leaf node
         * to be filled with vectors later
         *         
         */
        public FeatureVectorLeafNode(){
            super();
            featureVectorList = new ArrayList();
            featureVectors = null;
            growable = true;
        }
        
        public void addFeatureVector(FeatureVector fv){
            featureVectorList.add(fv);
        }
        
        /**
         * Get the feature vectors of this node
         * 
         * @return the feature vectors
         */
        public FeatureVector[] getFeatureVectors() {
            if (growable && 
                     (featureVectors == null
                             || featureVectors.length == 0)){
                featureVectors = (FeatureVector[])
                    featureVectorList.toArray(
                        new FeatureVector[featureVectorList.size()]);
            }
            return featureVectors;
        }
        
	public void setFeatureVectors(FeatureVector[] fv)
	{
	    this.featureVectors = fv;
	}

        /**
         * Get all data in this leaf
         * 
         * @return the featurevector array contained in this leaf
         */
        public Object getAllData() {
             if (growable && 
                     (featureVectors == null
                             || featureVectors.length == 0)){
                featureVectors = (FeatureVector[])
                    featureVectorList.toArray(
                        new FeatureVector[featureVectorList.size()]);
            }
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
            if (growable){
                return featureVectorList.size();
            }
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
            //make sure that we have a feature vector array
            if (growable && 
                     (featureVectors == null
                             || featureVectors.length == 0)){
                featureVectors = (FeatureVector[])
                    featureVectorList.toArray(
                        new FeatureVector[featureVectorList.size()]);
            }
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

                CART.writeStringToOutput(sb.toString(), out);
            } else {
                // write to Standard out
                // System.out.println(sb.toString());
            }
            if (pw != null) {
                // dump to printwriter
                pw.println(sb.toString());
            }
        }
        
        public String toString()
        {
            if (growable) return "fv["+featureVectorList.size()+"]";
            if (featureVectors == null) return "fv[null]";
            return "fv["+featureVectors.length+"]";
        }

    }

    /**
     * A leaf class that is suitable for regression trees.
     * Here, a leaf consists of a mean and a standard deviation.
     * @author marc
     *
     */
    public static class FloatLeafNode extends LeafNode
    {
        private float[] data;
        public FloatLeafNode(float[] data)
        {
            super();
            if (data.length != 2) throw new IllegalArgumentException("data must have length 2, found "+data.length);
            this.data = data;
        }
        
        /**
         * Get all data in this leaf
         * 
         * @return the mean/standard deviation value contained in this leaf
         */
        public Object getAllData() {
            return data;
        }

        protected void fillData(Object target, int pos, int len)
        {
            throw new IllegalStateException("This method should not be called for FloatLeafNodes");
        }

        public int getNumberOfData()
        {
            return 1;
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
            String s = "(("
                + data[0] // stddev
                + " "
                + data[1] // mean
                + "))";
            // dump the whole stuff
            if (out != null) {
                // write to output stream

                CART.writeStringToOutput(s, out);
            } else {
                // write to Standard out
                // System.out.println(sb.toString());
            }
            if (pw != null) {
                // dump to printwriter
                pw.print(s);
            }
        }
        
        public String toString()
        {
            if (data == null) return "mean=null, stddev=null";
            return "mean="+data[1]+", stddev="+data[0];
        }

    }

}
