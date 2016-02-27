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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author marc
 *
 */
public class NodeIterator<T extends Node> implements Iterator<T> {
	private Node root;
	private Node current;
	private boolean showLeafNodes;
	private boolean showDecisionNodes;
	private boolean showDirectedGraphNodes;
	private Set<Node> alreadySeen = new HashSet<Node>();
	// we need to keep our own map of daughter-mother relationships,
	// because for subgraphs, we could move out of a subgraph if we call node.getMother()
	// if the mother via which we entered a multi-parent node is not the first mother.
	private Map<Node, Node> daughterToMother = new HashMap<Node, Node>();

	/**
	 * Iterate over all nodes in the graph.
	 * 
	 * @param graph
	 *            graph
	 * @param showLeafNodes
	 *            showLeafNodes
	 * @param showDecisionNodes
	 *            showDecisionNodes
	 * @param showDirectedGraphNodes
	 *            showDirectedGraphNodes
	 */
	protected NodeIterator(DirectedGraph graph, boolean showLeafNodes, boolean showDecisionNodes, boolean showDirectedGraphNodes) {
		this(graph.getRootNode(), showLeafNodes, showDecisionNodes, showDirectedGraphNodes);
	}

	/**
	 * Iterate over the subtree below rootNode.
	 * 
	 * @param rootNode
	 *            rootNode
	 * @param showLeafNodes
	 *            showLeafNodes
	 * @param showDecisionNodes
	 *            showDecisionNodes
	 * @param showDirectedGraphNodes
	 *            showDirectedGraphNodes
	 */
	protected NodeIterator(Node rootNode, boolean showLeafNodes, boolean showDecisionNodes, boolean showDirectedGraphNodes) {
		this.root = rootNode;
		this.showLeafNodes = showLeafNodes;
		this.showDecisionNodes = showDecisionNodes;
		this.showDirectedGraphNodes = showDirectedGraphNodes;
		this.current = root;
		alreadySeen.add(current);
		if (!currentIsSuitable()) {
			nextSuitableNodeDepthFirst();
		}
	}

	public boolean hasNext() {
		return current != null;
	}

	public T next() {
		T ret = (T) current;
		// and already prepare the current one
		nextSuitableNodeDepthFirst();
		return ret;
	}

	private boolean currentIsSuitable() {
		return (current == null || showDecisionNodes && current.isDecisionNode() || showLeafNodes && current.isLeafNode() || showDirectedGraphNodes
				&& current.isDirectedGraphNode());
	}

	private void nextSuitableNodeDepthFirst() {
		do {
			nextNodeDepthFirst();
		} while (!currentIsSuitable());
	}

	private void nextNodeDepthFirst() {
		if (current == null)
			return;
		if (current.isDecisionNode()) {
			DecisionNode dec = (DecisionNode) current;
			for (int i = 0; i < dec.getNumberOfDaugthers(); i++) {
				Node daughter = dec.getDaughter(i);
				if (daughter == null)
					continue;
				daughterToMother.put(daughter, dec);
				if (unseenNode(dec.getDaughter(i)))
					return;
			}
		} else if (current.isDirectedGraphNode()) {
			// Graph nodes return leaf child first, then decision child
			DirectedGraphNode g = (DirectedGraphNode) current;
			Node leaf = g.getLeafNode();
			if (leaf != null) {
				daughterToMother.put(leaf, g);
				if (unseenNode(leaf))
					return;
			}
			Node dec = g.getDecisionNode();
			if (dec != null) {
				daughterToMother.put(dec, g);
				if (unseenNode(dec))
					return;
			}
		}
		// If we didn't find a suitable child, we need to:
		backtrace();
	}

	private void backtrace() {
		// Only go back to mothers we have come from.
		// This has two effects:
		// 1. We cannot go beyond root node;
		// 2. we don't risk to leave the subgraph defined by root node
		// in cases where we enter into a multi-parent node from a not-first mother
		// (in such cases, getMother() would return the first mother).
		current = daughterToMother.get(current);
		nextNodeDepthFirst();
	}

	/**
	 * Test whether the given node is unseen. If so, move current to it, and remember it as a seen node.
	 * 
	 * @param candidate
	 *            candidate
	 * @return True if candidate != null and !alreadySeen.contains(candidate)
	 */
	private boolean unseenNode(Node candidate) {
		if (candidate != null && !alreadySeen.contains(candidate)) {
			current = candidate;
			alreadySeen.add(current);
			return true;
		}
		return false;

	}

	public void remove() {
		throw new UnsupportedOperationException("Cannot remove nodes using this iterator");
	}

}
