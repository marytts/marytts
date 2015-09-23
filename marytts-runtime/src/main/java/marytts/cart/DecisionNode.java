/**
 * Copyright 2000-2009 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.cart;

import marytts.cart.LeafNode.FeatureVectorLeafNode;
import marytts.cart.LeafNode.IntArrayLeafNode;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;

/**
 * A decision node that determines the next Node to go to in the CART. All decision nodes inherit from this class
 */
public abstract class DecisionNode extends Node {
	public enum Type {
		BinaryByteDecisionNode, BinaryShortDecisionNode, BinaryFloatDecisionNode, ByteDecisionNode, ShortDecisionNode
	};

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

	// unique index used in MaryCART format
	protected int uniqueDecisionNodeId;

	/**
	 * Construct a new DecisionNode
	 * 
	 * @param feature
	 *            the feature
	 * @param numDaughters
	 *            the number of daughters
	 * @param featureDefinition
	 *            feature definition
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
	 * @param featureDefinition
	 *            feature definition
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
	 * Construct a new DecisionNode
	 * 
	 * @param numDaughters
	 *            the number of daughters
	 * @param featureDefinition
	 *            feature definition
	 */
	public DecisionNode(int numDaughters, FeatureDefinition featureDefinition) {
		daughters = new Node[numDaughters];
		isRoot = false;
		// for trace and getDecisionPath():
		this.featureDefinition = featureDefinition;
	}

	@Override
	public boolean isDecisionNode() {
		return true;
	}

	/**
	 * Get the name of the feature
	 * 
	 * @return the name of the feature
	 */
	public String getFeatureName() {
		return feature;
	}

	public int getFeatureIndex() {
		return featureIndex;
	}

	public FeatureDefinition getFeatureDefinition() {
		return featureDefinition;
	}

	/**
	 * Add a daughter to the node
	 * 
	 * @param daughter
	 *            the new daughter
	 */
	public void addDaughter(Node daughter) {
		if (lastDaughter > daughters.length - 1) {
			throw new RuntimeException("Can not add daughter number " + (lastDaughter + 1) + ", since node has only "
					+ daughters.length + " daughters!");
		}
		daughters[lastDaughter] = daughter;
		if (daughter != null) {
			daughter.setMother(this, lastDaughter);
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
			throw new RuntimeException("Can not replace daughter number " + index + ", since daughter index goes from 0 to "
					+ (daughters.length - 1) + "!");
		}
		daughters[index] = newDaughter;
		newDaughter.setMother(this, index);
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
		LeafNode firstLeaf = new NodeIterator<LeafNode>(this, true, false, false).next();
		if (firstLeaf == null)
			return null;
		Object result;
		if (firstLeaf instanceof IntArrayLeafNode) { // this includes subclass IntAndFloatArrayLeafNode
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
		// assert pos + total <= target.length;
		for (int i = 0; i < daughters.length; i++) {
			if (daughters[i] == null)
				continue;
			int len = daughters[i].getNumberOfData();
			daughters[i].fillData(target, pos, len);
			pos += len;
		}
	}

	/**
	 * Count all the nodes at and below this node. A leaf will return 1; the root node will report the total number of decision
	 * and leaf nodes in the tree.
	 * 
	 * @return the number of nodes
	 */
	public int getNumberOfNodes() {
		int nNodes = 1; // this node
		for (int i = 0; i < daughters.length; i++) {
			if (daughters[i] != null)
				nNodes += daughters[i].getNumberOfNodes();
		}
		return nNodes;
	}

	public int getNumberOfData() {
		return nData;
	}

	/**
	 * Number of daughters of current node.
	 * 
	 * @return daughters.length
	 */
	public int getNumberOfDaugthers() {
		return daughters.length;
	}

	/**
	 * Set the number of candidates correctly, by counting while walking down the tree. This needs to be done once for the entire
	 * tree.
	 * 
	 */
	// protected void countData() {
	public void countData() {
		nData = 0;
		for (int i = 0; i < daughters.length; i++) {
			if (daughters[i] instanceof DecisionNode)
				((DecisionNode) daughters[i]).countData();
			if (daughters[i] != null) {
				nData += daughters[i].getNumberOfData();
			}
		}
	}

	public String toString() {
		return "dn" + uniqueDecisionNodeId;
	}

	/**
	 * Get the path leading to the daughter with the given index. This will recursively go up to the root node.
	 * 
	 * @param daughterIndex
	 *            daughterIndex
	 * @return the unique decision node id
	 */
	public abstract String getDecisionPath(int daughterIndex);

	// unique index used in MaryCART format
	public void setUniqueDecisionNodeId(int id) {
		this.uniqueDecisionNodeId = id;
	}

	public int getUniqueDecisionNodeId() {
		return uniqueDecisionNodeId;
	}

	/**
	 * Gets the String that defines the decision done in the node
	 * 
	 * @return the node definition
	 */
	public abstract String getNodeDefinition();

	/**
	 * Get the decision node type
	 * 
	 * @return the decision node type
	 */
	public abstract Type getDecisionNodeType();

	/**
	 * Select a daughter node according to the value in the given target
	 * 
	 * @param featureVector
	 *            the feature vector
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
		 * @param featureDefinition
		 *            featureDefinition
		 */
		public BinaryByteDecisionNode(String feature, String value, FeatureDefinition featureDefinition) {
			super(feature, 2, featureDefinition);
			this.value = featureDefinition.getFeatureValueAsByte(feature, value);
		}

		public BinaryByteDecisionNode(int featureIndex, byte value, FeatureDefinition featureDefinition) {
			super(featureIndex, 2, featureDefinition);
			this.value = value;
		}

		/***
		 * Creates an empty BinaryByteDecisionNode, the feature and feature value of this node should be filled with
		 * setFeatureAndFeatureValue() function.
		 * 
		 * @param uniqueId
		 *            unique index from tree HTS test file.
		 * @param featureDefinition
		 *            featureDefinition
		 */
		public BinaryByteDecisionNode(int uniqueId, FeatureDefinition featureDefinition) {
			super(2, featureDefinition);
			// System.out.println("adding decision node: " + uniqueId);
			this.uniqueDecisionNodeId = uniqueId;
		}

		/***
		 * Fill the feature and feature value of an already created (empty) BinaryByteDecisionNode.
		 * 
		 * @param feature
		 *            feature
		 * @param value
		 *            value
		 */
		public void setFeatureAndFeatureValue(String feature, String value) {
			this.feature = feature;
			this.featureIndex = featureDefinition.getFeatureIndex(feature);
			this.value = featureDefinition.getFeatureValueAsByte(feature, value);
		}

		public byte getCriterionValueAsByte() {
			return value;
		}

		public String getCriterionValueAsString() {
			return featureDefinition.getFeatureValueAsString(featureIndex, value);
		}

		/**
		 * Select a daughter node according to the value in the given target
		 * 
		 * @param featureVector
		 *            the feature vector
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
				System.out.print("    " + feature + ": " + featureDefinition.getFeatureValueAsString(featureIndex, value)
						+ " == " + featureDefinition.getFeatureValueAsString(featureIndex, val));
				if (val == value)
					System.out.println(" YES ");
				else
					System.out.println(" NO ");
			}
			return returnNode;
		}

		public String getDecisionPath(int daughterIndex) {
			String thisNodeInfo;
			if (daughterIndex == 0)
				thisNodeInfo = feature + "==" + featureDefinition.getFeatureValueAsString(featureIndex, value);
			else
				thisNodeInfo = feature + "!=" + featureDefinition.getFeatureValueAsString(featureIndex, value);
			if (mother == null)
				return thisNodeInfo;
			else if (mother.isDecisionNode())
				return ((DecisionNode) mother).getDecisionPath(getNodeIndex()) + " - " + thisNodeInfo;
			else
				return mother.getDecisionPath() + " - " + thisNodeInfo;
		}

		/**
		 * Gets the String that defines the decision done in the node
		 * 
		 * @return the node definition
		 */
		public String getNodeDefinition() {
			return feature + " is " + featureDefinition.getFeatureValueAsString(featureIndex, value);
		}

		public Type getDecisionNodeType() {
			return Type.BinaryByteDecisionNode;
		}

	}

	/**
	 * A binary decision Node that compares two short values.
	 */
	public static class BinaryShortDecisionNode extends DecisionNode {

		// the value of this node
		private short value;

		/**
		 * Create a new binary String DecisionNode.
		 * 
		 * @param feature
		 *            the string used to get a value from an Item
		 * @param value
		 *            the value to compare to
		 * @param featureDefinition
		 *            featureDefinition
		 */
		public BinaryShortDecisionNode(String feature, String value, FeatureDefinition featureDefinition) {
			super(feature, 2, featureDefinition);
			this.value = featureDefinition.getFeatureValueAsShort(feature, value);
		}

		public BinaryShortDecisionNode(int featureIndex, short value, FeatureDefinition featureDefinition) {
			super(featureIndex, 2, featureDefinition);
			this.value = value;
		}

		public short getCriterionValueAsShort() {
			return value;
		}

		public String getCriterionValueAsString() {
			return featureDefinition.getFeatureValueAsString(featureIndex, value);
		}

		/**
		 * Select a daughter node according to the value in the given target
		 * 
		 * @param featureVector
		 *            the feature vector
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

		public String getDecisionPath(int daughterIndex) {
			String thisNodeInfo;
			if (daughterIndex == 0)
				thisNodeInfo = feature + "==" + featureDefinition.getFeatureValueAsString(featureIndex, value);
			else
				thisNodeInfo = feature + "!=" + featureDefinition.getFeatureValueAsString(featureIndex, value);
			if (mother == null)
				return thisNodeInfo;
			else if (mother.isDecisionNode())
				return ((DecisionNode) mother).getDecisionPath(getNodeIndex()) + " - " + thisNodeInfo;
			else
				return mother.getDecisionPath() + " - " + thisNodeInfo;
		}

		/**
		 * Gets the String that defines the decision done in the node
		 * 
		 * @return the node definition
		 */
		public String getNodeDefinition() {
			return feature + " is " + featureDefinition.getFeatureValueAsString(featureIndex, value);
		}

		public Type getDecisionNodeType() {
			return Type.BinaryShortDecisionNode;
		}

	}

	/**
	 * A binary decision Node that compares two float values.
	 */
	public static class BinaryFloatDecisionNode extends DecisionNode {

		// the value of this node
		private float value;
		private boolean isByteFeature;

		/**
		 * Create a new binary String DecisionNode.
		 * 
		 * @param featureIndex
		 *            the string used to get a value from an Item
		 * @param value
		 *            the value to compare to
		 * @param featureDefinition
		 *            featureDefinition
		 */
		public BinaryFloatDecisionNode(int featureIndex, float value, FeatureDefinition featureDefinition) {
			this(featureDefinition.getFeatureName(featureIndex), value, featureDefinition);
		}

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

		public float getCriterionValueAsFloat() {
			return value;
		}

		public String getCriterionValueAsString() {
			return String.valueOf(value);
		}

		/**
		 * Select a daughter node according to the value in the given target
		 * 
		 * @param featureVector
		 *            the feature vector
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

		public String getDecisionPath(int daughterIndex) {
			String thisNodeInfo;
			if (daughterIndex == 0)
				thisNodeInfo = feature + "<" + value;
			else
				thisNodeInfo = feature + ">=" + value;
			if (mother == null)
				return thisNodeInfo;
			else if (mother.isDecisionNode())
				return ((DecisionNode) mother).getDecisionPath(getNodeIndex()) + " - " + thisNodeInfo;
			else
				return mother.getDecisionPath() + " - " + thisNodeInfo;
		}

		/**
		 * Gets the String that defines the decision done in the node
		 * 
		 * @return the node definition
		 */
		public String getNodeDefinition() {
			return feature + " < " + value;
		}

		public Type getDecisionNodeType() {
			return Type.BinaryFloatDecisionNode;
		}

	}

	/**
	 * An decision Node with an arbitrary number of daughters. Value of the target corresponds to the index number of next
	 * daughter.
	 */
	public static class ByteDecisionNode extends DecisionNode {

		/**
		 * Build a new byte decision node
		 * 
		 * @param feature
		 *            the feature name
		 * @param numDaughters
		 *            the number of daughters
		 * @param featureDefinition
		 *            featureDefinition
		 */
		public ByteDecisionNode(String feature, int numDaughters, FeatureDefinition featureDefinition) {
			super(feature, numDaughters, featureDefinition);
		}

		/**
		 * Build a new byte decision node
		 * 
		 * @param featureIndex
		 *            the feature index
		 * @param numDaughters
		 *            the number of daughters
		 * @param featureDefinition
		 *            featureDefinition
		 */
		public ByteDecisionNode(int featureIndex, int numDaughters, FeatureDefinition featureDefinition) {
			super(featureIndex, numDaughters, featureDefinition);
		}

		/**
		 * Select a daughter node according to the value in the given target
		 * 
		 * @param featureVector
		 *            the feature vector
		 * @return a daughter
		 */
		public Node getNextNode(FeatureVector featureVector) {
			byte val = featureVector.getByteFeature(featureIndex);
			if (TRACE) {
				System.out.println(feature + ": " + featureDefinition.getFeatureValueAsString(featureIndex, val));
			}
			return daughters[val];
		}

		public String getDecisionPath(int daughterIndex) {
			String thisNodeInfo = feature + "==" + featureDefinition.getFeatureValueAsString(featureIndex, daughterIndex);
			if (mother == null)
				return thisNodeInfo;
			else if (mother.isDecisionNode())
				return ((DecisionNode) mother).getDecisionPath(getNodeIndex()) + " - " + thisNodeInfo;
			else
				return mother.getDecisionPath() + " - " + thisNodeInfo;
		}

		/**
		 * Gets the String that defines the decision done in the node
		 * 
		 * @return the node definition
		 */
		public String getNodeDefinition() {
			return feature + " isByteOf " + daughters.length;
		}

		public Type getDecisionNodeType() {
			return Type.ByteDecisionNode;
		}

	}

	/**
	 * An decision Node with an arbitrary number of daughters. Value of the target corresponds to the index number of next
	 * daughter.
	 */
	public static class ShortDecisionNode extends DecisionNode {

		/**
		 * Build a new short decision node
		 * 
		 * @param feature
		 *            the feature name
		 * @param numDaughters
		 *            the number of daughters
		 * @param featureDefinition
		 *            featureDefinition
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
		 * @param featureDefinition
		 *            featureDefinition
		 */
		public ShortDecisionNode(int featureIndex, int numDaughters, FeatureDefinition featureDefinition) {
			super(featureIndex, numDaughters, featureDefinition);
		}

		/**
		 * Select a daughter node according to the value in the given target
		 * 
		 * @param featureVector
		 *            the feature vector
		 * @return a daughter
		 */
		public Node getNextNode(FeatureVector featureVector) {
			short val = featureVector.getShortFeature(featureIndex);
			if (TRACE) {
				System.out.println(feature + ": " + featureDefinition.getFeatureValueAsString(featureIndex, val));
			}
			return daughters[val];
		}

		public String getDecisionPath(int daughterIndex) {
			String thisNodeInfo = feature + "==" + featureDefinition.getFeatureValueAsString(featureIndex, daughterIndex);
			if (mother == null)
				return thisNodeInfo;
			else if (mother.isDecisionNode())
				return ((DecisionNode) mother).getDecisionPath(getNodeIndex()) + " - " + thisNodeInfo;
			else
				return mother.getDecisionPath() + " - " + thisNodeInfo;
		}

		/**
		 * Gets the String that defines the decision done in the node
		 * 
		 * @return the node definition
		 */
		public String getNodeDefinition() {
			return feature + " isShortOf " + daughters.length;
		}

		public Type getDecisionNodeType() {
			return Type.ShortDecisionNode;
		}

	}

}
