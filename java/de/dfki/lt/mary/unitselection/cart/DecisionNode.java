package de.dfki.lt.mary.unitselection.cart;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import de.dfki.lt.mary.unitselection.cart.LeafNode.FeatureVectorLeafNode;
import de.dfki.lt.mary.unitselection.cart.LeafNode.IntArrayLeafNode;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;

/**
 * A decision node that determines the next Node to go to in the CART. All
 * decision nodes inherit from this class
 */
public abstract class DecisionNode extends Node {
    protected boolean TRACE = false; 
    // for debugging:
    protected FeatureDefinition featureDefinition;
    
    
    // a decision node has an array of daughters
    protected Node[] daughters;

    // the feature index
    protected int featureIndex;

    // the feature name
    protected String feature;

    // remember last added daughter
    protected int lastDaughter;

    // the total number of data in the leaves below this node
    protected int nData;

    /**
     * Construct a new DecisionNode
     * 
     * @param feature
     *            the feature
     * @param numDaughters
     *            the number of daughters
     */
    public DecisionNode(String feature, int numDaughters, FeatureDefinition featureDefinition) {
        this.feature = feature;
        this.featureIndex = featureDefinition.getFeatureIndex(feature);
        daughters = new Node[numDaughters];
        isRoot = false;
        // for trace and getDecisionPath():
        this.featureDefinition = featureDefinition;
    }

    /**
     * Construct a new DecisionNode
     * 
     * @param featureIndex
     *            the feature index
     * @param numDaughters
     *            the number of daughters
     */
    public DecisionNode(int featureIndex, int numDaughters, FeatureDefinition featureDefinition) {
        this.featureIndex = featureIndex;
        this.feature = featureDefinition.getFeatureName(featureIndex);
        daughters = new Node[numDaughters];
        isRoot = false;
        // for trace and getDecisionPath():
        this.featureDefinition = featureDefinition;
    }

    /**
     * Get the name of the feature
     * 
     * @return the name of the feature
     */
    public String getFeatureName() {
        return feature;
    }

    /**
     * Add a daughter to the node
     * 
     * @param daughter
     *            the new daughter
     */
    public void addDaughter(Node daughter) {
      
        
        if (lastDaughter > daughters.length - 1) {
            throw new RuntimeException("Can not add daughter number "
                    + (lastDaughter + 1) + ", since node has only "
                    + daughters.length + " daughters!");
        }
        daughters[lastDaughter] = daughter;
        if (daughter != null) {
            daughter.setNodeIndex(lastDaughter);
        }
        lastDaughter++;
    }

    /**
     * Get the daughter at the specified index
     * 
     * @param index
     *            the index of the daughter
     * @return the daughter (potentially null); if index out of range: null
     */
    public Node getDaughter(int index) {
        if (index > daughters.length - 1 || index < 0) {
            return null;
        }
        return daughters[index];
    }

    /**
     * Replace daughter at given index with another daughter
     * 
     * @param newDaughter
     *            the new daughter
     * @param index
     *            the index of the daughter to replace
     */
    public void replaceDaughter(Node newDaughter, int index) {
        if (index > daughters.length - 1 || index < 0) {
            throw new RuntimeException("Can not replace daughter number "
                    + index + ", since daughter index goes from 0 to "
                    + (daughters.length - 1) + "!");
        }
        daughters[index] = newDaughter;
    }

    /**
     * Tests, if the given index refers to a daughter
     * 
     * @param index
     *            the index
     * @return true, if the index is in range of the daughters array
     */
    public boolean hasMoreDaughters(int index) {
        return (index > -1 && index < daughters.length);
    }

    /**
     * Get all unit indices from all leaves below this node
     * 
     * @return an int array containing the indices
     */
    public Object getAllData() {
        // What to do depends on the type of leaves.
        LeafNode firstLeaf = getNextLeafNode(0);
        if (firstLeaf == null) return null;
        Object result;
        if (firstLeaf instanceof IntArrayLeafNode) {
            result = new int[nData];
        } else if (firstLeaf instanceof FeatureVectorLeafNode) {
            result = new FeatureVector[nData];
        } else {
            return null;
        }
        fillData(result, 0, nData);
        return result;
    }

    protected void fillData(Object target, int pos, int total) {
        //assert pos + total <= target.length;
        for (int i = 0; i < daughters.length; i++) {
            int len = daughters[i].getNumberOfData();
            daughters[i].fillData(target, pos, len);
            pos += len;
        }
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
        int nNodes = 1; // this node
        for (int i=0; i<daughters.length; i++) {
            if (daughters[i] != null) nNodes += daughters[i].getNumberOfNodes();
        }
        return nNodes;
    }

    public int getNumberOfData() {
        return nData;
    }

    /**
     * Try to find a leaf node below the given daughter index. If there is no
     * such daughter, backtrace to our mother, and make the mother continue to
     * our right.
     * 
     * @param daughterIndex
     * @return the next leaf node, or null if there is no further leaf node in
     *         the tree.
     */
    protected LeafNode getNextLeafNode(int daughterIndex) {
        if (daughterIndex < 0 || daughterIndex >= daughters.length) {
            // nothing we can do -- backtrace to mother
            if (mother == null)
                return null; // no further options
            assert mother instanceof DecisionNode;
            // Try next sibling or cause backtrace:
            return ((DecisionNode) mother).getNextLeafNode(getNodeIndex() + 1);
        }
        if (daughters[daughterIndex] instanceof LeafNode)
            return (LeafNode) daughters[daughterIndex];
        assert daughters[daughterIndex] instanceof DecisionNode;
        return ((DecisionNode) daughters[daughterIndex]).getNextLeafNode(0);
    }

    /**
     * Set the number of candidates correctly, by counting while walking down
     * the tree. This needs to be done once for the entire tree.
     * 
     */
    protected void countData() {
        nData = 0;
        for (int i = 0; i < daughters.length; i++) {
            if (daughters[i] instanceof DecisionNode)
                ((DecisionNode) daughters[i]).countData();
            nData += daughters[i].getNumberOfData();
        }
    }

    /**
     * Get the path leading to the daughter with the given index. This
     * will recursively go up to the root node.
     * @param daughterIndex
     * @return
     */
    public abstract String getDecisionPath(int daughterIndex);

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
        if (out != null) {
            // dump to output stream
            // two open brackets + definition of node
            CART.writeStringToOutput("((" + getNodeDefinition() + ")", out);
        } else {
            // dump to Standard out
            // two open brackets + definition of node
            // System.out.println("(("+getNodeDefinition());
        }
        if (pw != null) {
            // dump to print writer
            // two open brackets + definition of node
            pw.println("((" + getNodeDefinition() + ")");
        }
        // add the daughters
        for (int i = 0; i < daughters.length; i++) {

            if (daughters[i] == null) {
                String nullDaughter = "";

                if (i + 1 != daughters.length) {
                    nullDaughter = "((() 0))";

                } else {
                    // extension must be added to last daughter
                    if (extension != null) {
                        nullDaughter = "((() 0)))" + extension;

                    } else {
                        // we are in the root node, add a closing bracket
                        nullDaughter = "((() 0)))";
                    }
                }

                if (out != null) {
                    // dump to output stream
                    CART.writeStringToOutput(nullDaughter, out);
                } else {
                    // dump to Standard out
                    // System.out.println(nullDaughter);
                }
                if (pw != null) {
                    pw.println(" " + nullDaughter);
                }
            } else {
                if (i + 1 != daughters.length) {

                    daughters[i].toWagonFormat(out, "", pw);
                } else {

                    // extension must be added to last daughter
                    if (extension != null) {
                        daughters[i].toWagonFormat(out, ")" + extension, pw);
                    } else {
                        // we are in the root node, add a closing bracket
                        daughters[i].toWagonFormat(out, ")", pw);
                    }
                }
            }
        }
    }
    
    /**
     * This returns a String representation of this node. A prefix is given to
     * indent the nodes.
     */
    public String toString(String prefix){
        StringBuffer sb = new StringBuffer();
        
        String lineBreak = System.getProperty("line.separator");

        sb.append(lineBreak);
        sb.append(prefix + "((" + getNodeDefinition() + ")");
        
        // add the daughters
        for (int i = 0; i < daughters.length; i++) {
            sb.append(daughters[i].toString(prefix + " "));
            
        }
        
        sb.append(")");
        
        return sb.toString();
    }

    /**
     * Gets the String that defines the decision done in the node
     * 
     * @return the node definition
     */
    public abstract String getNodeDefinition();

    /**
     * Select a daughter node according to the value in the given target
     * 
     * @param target
     *            the target
     * @return a daughter
     */
    public abstract Node getNextNode(FeatureVector featureVector);

    /**
     * A binary decision Node that compares two byte values.
     */
    public static class BinaryByteDecisionNode extends DecisionNode {

        // the value of this node
        private byte value;

        /**
         * Create a new binary String DecisionNode.
         * 
         * @param feature
         *            the string used to get a value from an Item
         * @param value
         *            the value to compare to
         */
        public BinaryByteDecisionNode(String feature, String value, FeatureDefinition featureDefinition) {
            super(feature, 2, featureDefinition);
            this.value = featureDefinition.getFeatureValueAsByte(feature, value);
        }

        /**
         * Select a daughter node according to the value in the given target
         * 
         * @param target
         *            the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            byte val = featureVector.getByteFeature(featureIndex);
            Node returnNode;
            if (val == value) {
                returnNode = daughters[0];
            } else {
                returnNode = daughters[1];
            }
            if (TRACE) {
                System.out.print(feature + ": " + featureDefinition.getFeatureValueAsString(featureIndex, val));
                if (val == value)
                    System.out.print(" == ");
                else
                    System.out.print(" != ");
                System.out.println(featureDefinition.getFeatureValueAsString(featureIndex, value));
            }
            return returnNode;
        }
        
        public String getDecisionPath(int daughterIndex)
        {
            String thisNodeInfo;
            if (daughterIndex == 0) thisNodeInfo = feature + "==" + featureDefinition.getFeatureValueAsString(featureIndex, value);
            else thisNodeInfo = feature + "!=" + featureDefinition.getFeatureValueAsString(featureIndex, value);
            if (mother == null) return thisNodeInfo;
            assert mother instanceof DecisionNode;
            return ((DecisionNode)mother).getDecisionPath(getNodeIndex()) + " - " + thisNodeInfo;
        }


        /**
         * Gets the String that defines the decision done in the node
         * 
         * @return the node definition
         */
        public String getNodeDefinition() {
            return feature + " is " + featureDefinition.getFeatureValueAsString(featureIndex, value);
        }

    }

    /**
     * A binary decision Node that compares two short values.
     */
    static class BinaryShortDecisionNode extends DecisionNode {

        // the value of this node
        private short value;

        /**
         * Create a new binary String DecisionNode.
         * 
         * @param feature
         *            the string used to get a value from an Item
         * @param value
         *            the value to compare to
         */
        public BinaryShortDecisionNode(String feature, String value, FeatureDefinition featureDefinition) {
            super(feature, 2, featureDefinition);
            this.value = featureDefinition.getFeatureValueAsShort(feature, value);
        }

        /**
         * Select a daughter node according to the value in the given target
         * 
         * @param target
         *            the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            short val = featureVector.getShortFeature(featureIndex);
            Node returnNode;
            if (val == value) {
                returnNode = daughters[0];
            } else {
                returnNode = daughters[1];
            }
            if (TRACE) {
                System.out.print(feature + ": " + featureDefinition.getFeatureValueAsString(featureIndex, val));
                if (val == value)
                    System.out.print(" == ");
                else
                    System.out.print(" != ");
                System.out.println(featureDefinition.getFeatureValueAsString(featureIndex, value));
            }
            return returnNode;
        }
        
        public String getDecisionPath(int daughterIndex)
        {
            String thisNodeInfo;
            if (daughterIndex == 0) thisNodeInfo = feature + "==" + featureDefinition.getFeatureValueAsString(featureIndex, value);
            else thisNodeInfo = feature + "!=" + featureDefinition.getFeatureValueAsString(featureIndex, value);
            if (mother == null) return thisNodeInfo;
            assert mother instanceof DecisionNode;
            return ((DecisionNode)mother).getDecisionPath(getNodeIndex()) + " - " + thisNodeInfo;
        }


        /**
         * Gets the String that defines the decision done in the node
         * 
         * @return the node definition
         */
        public String getNodeDefinition() {
            return feature + " is " + featureDefinition.getFeatureValueAsString(featureIndex, value);
        }

    }

    /**
     * A binary decision Node that compares two float values.
     */
    static class BinaryFloatDecisionNode extends DecisionNode {

        // the value of this node
        private float value;
        private boolean isByteFeature;

        /**
         * Create a new binary String DecisionNode.
         * 
         * @param feature
         *            the string used to get a value from an Item
         * @param value
         *            the value to compare to
         */
        public BinaryFloatDecisionNode(String feature, float value, FeatureDefinition featureDefinition) {
            super(feature, 2, featureDefinition);
            this.value = value;
            // check for pseudo-floats:
            // TODO: clean this up:
            if (featureDefinition.isByteFeature(featureIndex))
                isByteFeature = true;
            else
                isByteFeature = false;
        }

        /**
         * Select a daughter node according to the value in the given target
         * 
         * @param target
         *            the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            float val;
            if (isByteFeature) 
                val = (float) featureVector.getByteFeature(featureIndex);
            else
                val = featureVector.getContinuousFeature(featureIndex);
            Node returnNode;
            if (val < value) {
                returnNode = daughters[0];
            } else {
                returnNode = daughters[1];
            }
            if (TRACE) {
                System.out.print(feature + ": " + val);
                if (val < value)
                    System.out.print(" < ");
                else
                    System.out.print(" >= ");
                System.out.println(value);
            }

            return returnNode;
        }

        public String getDecisionPath(int daughterIndex)
        {
            String thisNodeInfo;
            if (daughterIndex == 0) thisNodeInfo = feature + "<" + value;
            else thisNodeInfo = feature + ">=" + value;
            if (mother == null) return thisNodeInfo;
            assert mother instanceof DecisionNode;
            return ((DecisionNode)mother).getDecisionPath(getNodeIndex()) + " - " + thisNodeInfo;
        }

        /**
         * Gets the String that defines the decision done in the node
         * 
         * @return the node definition
         */
        public String getNodeDefinition() {
            return feature + " < " + value;
        }

    }

    /**
     * An decision Node with an arbitrary number of daughters. Value of the
     * target corresponds to the index number of next daughter.
     */
    static class ByteDecisionNode extends DecisionNode {

        /**
         * Build a new byte decision node
         * 
         * @param feature
         *            the feature name
         * @param numDaughters
         *            the number of daughters
         */
        public ByteDecisionNode(String feature, int numDaughters, FeatureDefinition featureDefinition) {
            super(feature, numDaughters, featureDefinition);
        }

        /**
         * Build a new byte decision node
         * 
         * @param feature
         *            the feature name
         * @param numDaughters
         *            the number of daughters
         */
        public ByteDecisionNode(int featureIndex, int numDaughters, FeatureDefinition featureDefinition) {
            super(featureIndex, numDaughters, featureDefinition);
        }

        /**
         * Select a daughter node according to the value in the given target
         * 
         * @param target
         *            the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            byte val = featureVector.getByteFeature(featureIndex);
            if (TRACE) {
                System.out.println(feature+": "+featureDefinition.getFeatureValueAsString(featureIndex, val));
            }
            return daughters[val];
        }
        
        public String getDecisionPath(int daughterIndex)
        {
            String thisNodeInfo = feature + "==" + featureDefinition.getFeatureValueAsString(featureIndex, daughterIndex);
            if (mother == null) return thisNodeInfo;
            assert mother instanceof DecisionNode;
            return ((DecisionNode)mother).getDecisionPath(getNodeIndex()) + " - " + thisNodeInfo;
        }


        /**
         * Gets the String that defines the decision done in the node
         * 
         * @return the node definition
         */
        public String getNodeDefinition() {
            return feature + " isByteOf " + daughters.length;
        }

    }

    /**
     * An decision Node with an arbitrary number of daughters. Value of the
     * target corresponds to the index number of next daughter.
     */
    static class ShortDecisionNode extends DecisionNode {

        /**
         * Build a new short decision node
         * 
         * @param feature
         *            the feature name
         * @param numDaughters
         *            the number of daughters
         */
        public ShortDecisionNode(String feature, int numDaughters, FeatureDefinition featureDefinition) {
            super(feature, numDaughters, featureDefinition);
        }

        /**
         * Build a new short decision node
         * 
         * @param featureIndex
         *            the feature index
         * @param numDaughters
         *            the number of daughters
         */
        public ShortDecisionNode(int featureIndex, int numDaughters, FeatureDefinition featureDefinition) {
            super(featureIndex, numDaughters, featureDefinition);
        }

        /**
         * Select a daughter node according to the value in the given target
         * 
         * @param target
         *            the target
         * @return a daughter
         */
        public Node getNextNode(FeatureVector featureVector) {
            short val = featureVector.getShortFeature(featureIndex);
            if (TRACE) {
                System.out.println(feature+": "+featureDefinition.getFeatureValueAsString(featureIndex, val));
            }
            return daughters[val];
        }
        
        public String getDecisionPath(int daughterIndex)
        {
            String thisNodeInfo = feature + "==" + featureDefinition.getFeatureValueAsString(featureIndex, daughterIndex);
            if (mother == null) return thisNodeInfo;
            assert mother instanceof DecisionNode;
            return ((DecisionNode)mother).getDecisionPath(getNodeIndex()) + " - " + thisNodeInfo;
        }


        /**
         * Gets the String that defines the decision done in the node
         * 
         * @return the node definition
         */
        public String getNodeDefinition() {
            return feature + " isShortOf " + daughters.length;
        }

    }

}
