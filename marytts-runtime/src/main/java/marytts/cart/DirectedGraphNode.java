/**
 * Copyright 2009 DFKI GmbH.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import marytts.features.FeatureVector;

/**
 * A type of node that can be at the same time a decision node and a leaf node, and that can have more than one mother. Other than
 * tree nodes, thus, directed graph nodes are not necessarily contained in a strict tree structure; furthermore, each node can
 * potentially carry data.
 * 
 * @author marc
 *
 */
public class DirectedGraphNode extends Node {

	private DecisionNode decisionNode;
	private Node leafNode;

	private Map<Node, Integer> motherToIndex = new HashMap<Node, Integer>();
	private List<Node> mothers = new ArrayList<Node>();
	private int uniqueID;

	/**
	 * @param decisionNode
	 *            decisionNode
	 * @param leafNode
	 *            leafNode
	 */
	public DirectedGraphNode(DecisionNode decisionNode, Node leafNode) {
		setDecisionNode(decisionNode);
		setLeafNode(leafNode);
	}

	public DecisionNode getDecisionNode() {
		return decisionNode;
	}

	@Override
	public boolean isDirectedGraphNode() {
		return true;
	}

	public void setDecisionNode(DecisionNode newNode) {
		this.decisionNode = newNode;
		if (newNode != null)
			newNode.setMother(this, 0);
	}

	public Node getLeafNode() {
		return leafNode;
	}

	public void setLeafNode(Node newNode) {
		if (!(newNode == null || newNode instanceof DirectedGraphNode || newNode instanceof LeafNode)) {
			throw new IllegalArgumentException("Only leaf nodes and directed graph nodes allowed as leafNode");
		}
		this.leafNode = newNode;
		if (newNode != null)
			newNode.setMother(this, 0);
	}

	@Override
	public void setMother(Node node, int nodeIndex) {
		mothers.add(node);
		motherToIndex.put(node, nodeIndex);
	}

	/**
	 * Get a mother node of this node. DirectedGraphNodes can have more than one node.
	 * 
	 * @return the first mother, or null if there is no mother.
	 */
	public Node getMother() {
		if (mothers.isEmpty())
			return null;
		return mothers.get(0);
	}

	/**
	 * Get the index of this node in the mother returned by getMother().
	 * 
	 * @return the index in the mother's daughter array, or 0 if there is no mother.
	 */
	public int getNodeIndex() {
		Node firstMother = getMother();
		if (firstMother != null)
			return motherToIndex.get(firstMother);
		return 0;
	}

	public List<Node> getMothers() {
		return mothers;
	}

	/**
	 * Return this node's index in the given mother's array of daughters.
	 * 
	 * @param aMother
	 *            aMother
	 * @return motherToIndex.get(aMother)
	 * @throws IllegalArgumentException
	 *             if mother is not a mother of this node.
	 */
	public int getNodeIndex(Node aMother) {
		if (!motherToIndex.containsKey(aMother))
			throw new IllegalArgumentException("The given node is not a mother of this node");
		return motherToIndex.get(aMother);
	}

	/**
	 * Remove the given node from the list of mothers.
	 * 
	 * @param aMother
	 *            aMother
	 * @throws IllegalArgumentException
	 *             if mother is not a mother of this node.
	 */
	public void removeMother(Node aMother) {
		if (!motherToIndex.containsKey(aMother))
			throw new IllegalArgumentException("The given node is not a mother of this node");
		motherToIndex.remove(aMother);
		mothers.remove(aMother);
	}

	@Override
	protected void fillData(Object target, int pos, int len) {
		if (leafNode != null)
			leafNode.fillData(target, pos, len);
	}

	@Override
	public Object getAllData() {
		if (leafNode != null)
			return leafNode.getAllData();
		else if (decisionNode != null)
			return decisionNode.getAllData();
		return null;
	}

	@Override
	public int getNumberOfData() {
		if (leafNode != null)
			return leafNode.getNumberOfData();
		else if (decisionNode != null)
			return decisionNode.getNumberOfData();
		return 0;
	}

	@Override
	public int getNumberOfNodes() {
		if (decisionNode != null)
			return decisionNode.getNumberOfNodes();
		return 0;
	}

	public Node getNextNode(FeatureVector fv) {
		if (decisionNode != null) {
			Node next = decisionNode.getNextNode(fv);
			if (next != null)
				return next;
		}
		return leafNode;
	}

	public int getUniqueGraphNodeID() {
		return uniqueID;
	}

	public void setUniqueGraphNodeID(int id) {
		this.uniqueID = id;
	}

	public String getDecisionPath() {
		StringBuilder ancestorInfo = new StringBuilder();
		if (getMothers().size() == 0)
			ancestorInfo.append("null");
		for (Node mum : getMothers()) {
			if (ancestorInfo.length() > 0) {
				ancestorInfo.append(" or\n");
			}
			if (mum.isDecisionNode()) {
				ancestorInfo.append(((DecisionNode) mum).getDecisionPath(getNodeIndex()));
			} else {
				ancestorInfo.append(mum.getDecisionPath());
			}
		}
		return ancestorInfo + " - " + toString();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("DGN");
		sb.append(uniqueID);
		if (motherToIndex.size() > 1) {
			sb.append(" (").append(motherToIndex.size()).append(" mothers)");
		}
		return sb.toString();
	}
}
