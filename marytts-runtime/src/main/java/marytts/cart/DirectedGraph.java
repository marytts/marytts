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

import java.util.Iterator;
import java.util.Properties;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.unitselection.select.Target;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;

/**
 * A directed graph is a layered structure of nodes, in which there are mother-daughter relationships between the node. There is a
 * single root node. Each node can have multiple daughters and/or multiple mothers. Three types of nodes are allowed:
 * DirectedGraphNode (which can have multiple mothers, a leaf and a decision node), LeafNodes (which carry data), and
 * DecisionNodes (which can have multiple daughters).
 * 
 * @author marc
 *
 */
public class DirectedGraph {
	protected Logger logger = MaryUtils.getLogger(this.getClass().getName());

	protected Node rootNode;

	// knows the index numbers and types of the features used in DecisionNodes
	protected FeatureDefinition featDef;

	protected Properties properties;

	/**
	 * Build a new empty directed graph
	 * 
	 */
	public DirectedGraph() {
	}

	/**
	 * Build a new empty graph with the given feature definition.
	 * 
	 * @param featDef
	 *            the feature definition used for interpreting the meaning of decision node criteria.
	 */
	public DirectedGraph(FeatureDefinition featDef) {
		this(null, featDef);
	}

	/**
	 * Build a new graph with the given node as the root node
	 * 
	 * @param rootNode
	 *            the root node of the graph
	 * @param featDef
	 *            the feature definition used for interpreting the meaning of decision node criteria.
	 */
	public DirectedGraph(Node rootNode, FeatureDefinition featDef) {
		this(rootNode, featDef, null);
	}

	/**
	 * Build a new graph with the given node as the root node
	 * 
	 * @param rootNode
	 *            the root node of the graph
	 * @param featDef
	 *            the feature definition used for interpreting the meaning of decision node criteria.
	 * @param properties
	 *            a generic properties object, which can be used to encode information about the tree and the way the data in it
	 *            should be represented.
	 */
	public DirectedGraph(Node rootNode, FeatureDefinition featDef, Properties properties) {
		this.rootNode = rootNode;
		this.featDef = featDef;
		this.properties = properties;
	}

	public Object interpret(Target t) {
		return interpret(t.getFeatureVector());
	}

	/**
	 * Walk down the graph as far as possible according to the features in fv, and return the data in the leaf node found there.
	 * 
	 * @param fv
	 *            a feature vector which must be consistent with the graph's feature definition. (@see #getFeatureDefinition()).
	 * @return the most specific non-null leaf node data that can be retrieved, or null if there is no non-null leaf node data
	 *         along the fv's path.
	 */
	public Object interpret(FeatureVector fv) {
		return interpret(rootNode, fv);
	}

	/**
	 * Follow the directed graph down to the most specific leaf with data, starting from node n. This is recursively calling
	 * itself.
	 * 
	 * @param n
	 *            n
	 * @param fv
	 *            fv
	 * @return null if n=null, n.getAllData if n.isLeafNode, interpret (next, fv) if next = ((DecisionNode) n).getNextNode(fv),
	 *         data if data != null, interpret(g.getLeafNode(), fv) otherwise
	 */
	protected Object interpret(Node n, FeatureVector fv) {
		if (n == null)
			return null;
		else if (n.isLeafNode()) {
			return n.getAllData();
		} else if (n.isDecisionNode()) {
			Node next = ((DecisionNode) n).getNextNode(fv);
			return interpret(next, fv);
		} else if (n.isDirectedGraphNode()) {
			DirectedGraphNode g = (DirectedGraphNode) n;
			Object data = interpret(g.getDecisionNode(), fv);
			if (data != null) { // OK, found something more specific
				return data;
			}
			return interpret(g.getLeafNode(), fv);
		}
		throw new IllegalArgumentException("Unknown node type: " + n.getClass());
	}

	/**
	 * Return an iterator which returns all nodes in the tree exactly once. Search is done in a depth-first way.
	 * 
	 * @return a new NodeIterator(Node)
	 */
	public Iterator<Node> getNodeIterator() {
		return new NodeIterator<Node>(this, true, true, true);
	}

	/**
	 * Return an iterator which returns all leaf nodes in the tree exactly once. Search is done in a depth-first way.
	 * 
	 * @return a new NodeIterator(LeafNode)
	 */
	public Iterator<LeafNode> getLeafNodeIterator() {
		return new NodeIterator<LeafNode>(this, true, false, false);
	}

	/**
	 * Return an iterator which returns all decision nodes in the tree exactly once. Search is done in a depth-first way.
	 * 
	 * @return a new NodeIterator(DecisionNode)
	 */
	public Iterator<DecisionNode> getDecisionNodeIterator() {
		return new NodeIterator<DecisionNode>(this, false, true, false);
	}

	/**
	 * Return an iterator which returns all directed graph nodes in the tree exactly once. Search is done in a depth-first way.
	 * 
	 * @return a new NodeIterator(DirectedGraphNode)
	 */
	public Iterator<DirectedGraphNode> getDirectedGraphNodeIterator() {
		return new NodeIterator<DirectedGraphNode>(this, false, false, true);
	}

	/**
	 * A representation of the corresponding node iterator that can be used in extended for() statements.
	 * 
	 * @return a new Iterable(Node)
	 */
	public Iterable<Node> getNodes() {
		return new Iterable<Node>() {
			public Iterator<Node> iterator() {
				return getNodeIterator();
			}
		};
	}

	/**
	 * A representation of the corresponding node iterator that can be used in extended for() statements.
	 * 
	 * @return a new Iterable(LeafNode)
	 */
	public Iterable<LeafNode> getLeafNodes() {
		return new Iterable<LeafNode>() {
			public Iterator<LeafNode> iterator() {
				return getLeafNodeIterator();
			}
		};
	}

	/**
	 * A representation of the corresponding node iterator that can be used in extended for() statements.
	 * 
	 * @return a new Iterable(DecisionNode)
	 */
	public Iterable<DecisionNode> getDecisionNodes() {
		return new Iterable<DecisionNode>() {
			public Iterator<DecisionNode> iterator() {
				return getDecisionNodeIterator();
			}
		};
	}

	/**
	 * A representation of the corresponding node iterator that can be used in extended for() statements.
	 * 
	 * @return a new Iterable(DirectedGraphNode)
	 */
	public Iterable<DirectedGraphNode> getDirectedGraphNodes() {
		return new Iterable<DirectedGraphNode>() {
			public Iterator<DirectedGraphNode> iterator() {
				return getDirectedGraphNodeIterator();
			}
		};
	}

	/**
	 * Get the properties object associated with this tree, or null if there is no such object.
	 * 
	 * @return the properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * Get the root node of this CART
	 * 
	 * @return the root node
	 */
	public Node getRootNode() {
		return rootNode;
	}

	/**
	 * Set the root node of this CART
	 * 
	 * @param rNode
	 *            root node
	 */
	public void setRootNode(Node rNode) {
		rootNode = rNode;
	}

	public FeatureDefinition getFeatureDefinition() {
		return featDef;
	}

	/**
	 * Get the number of nodes in this CART
	 * 
	 * @return the number of nodes
	 */
	public int getNumNodes() {
		if (rootNode == null)
			return 0;
		return rootNode.getNumberOfNodes();
	}

	public String toString() {
		return this.rootNode.toString("");
	}

}
