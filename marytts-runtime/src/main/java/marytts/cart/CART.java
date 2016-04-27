/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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
package marytts.cart;

import java.util.Properties;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.unitselection.select.Target;

/**
 * A tree is a specific kind of directed graph in which each node can have only a single parent node. It consists exclusively of
 * DecisionNode and LeafNode nodes.
 * 
 * @author marc
 * 
 */
public class CART extends DirectedGraph {

	/**
	 * Build a new empty cart
	 * 
	 */
	public CART() {
	}

	/**
	 * Build a new empty cart with the given feature definition.
	 * 
	 * @param featDef
	 *            featDef
	 */
	public CART(FeatureDefinition featDef) {
		super(featDef);
	}

	/**
	 * Build a new cart with the given node as the root node
	 * 
	 * @param rootNode
	 *            the root node of the CART
	 * @param featDef
	 *            the feature definition used for interpreting the meaning of decision node criteria.
	 */
	public CART(Node rootNode, FeatureDefinition featDef) {
		super(rootNode, featDef);
	}

	/**
	 * Build a new cart with the given node as the root node
	 * 
	 * @param rootNode
	 *            the root node of the CART
	 * @param featDef
	 *            the feature definition used for interpreting the meaning of decision node criteria.
	 * @param properties
	 *            a generic properties object, which can be used to encode information about the tree and the way the data in it
	 *            should be represented.
	 */
	public CART(Node rootNode, FeatureDefinition featDef, Properties properties) {
		super(rootNode, featDef, properties);
	}

	/**
	 * Passes the given item through this CART and returns the leaf Node, or the Node it stopped walking down.
	 * 
	 * @param target
	 *            the target to analyze
	 * @param minNumberOfData
	 *            the minimum number of data requested. If this is 0, walk down the CART until the leaf level.
	 * 
	 * @return the Node
	 */
	public Node interpretToNode(Target target, int minNumberOfData) {
		return interpretToNode(target.getFeatureVector(), minNumberOfData);
	}

	/**
	 * Passes the given item through this CART and returns the leaf Node, or the Node it stopped walking down.
	 * 
	 * @param featureVector
	 *            the target to analyze
	 * @param minNumberOfData
	 *            the minimum number of data requested. If this is 0, walk down the CART until the leaf level.
	 * 
	 * @return the Node
	 */
	public Node interpretToNode(FeatureVector featureVector, int minNumberOfData) {
		Node currentNode = rootNode;
		Node prevNode = null;

		// logger.debug("Starting cart at "+nodeIndex);
		while (currentNode != null && currentNode.getNumberOfData() > minNumberOfData && !(currentNode instanceof LeafNode)) {
			// while we have not reached the bottom,
			// get the next node based on the features of the target
			prevNode = currentNode;
			currentNode = ((DecisionNode) currentNode).getNextNode(featureVector);
			// logger.debug(decision.toString() + " result '"+
			// decision.findFeature(item) + "' => "+ nodeIndex);
		}
		// Now usually we will have gone down one level too far
		if (currentNode == null || currentNode.getNumberOfData() < minNumberOfData && prevNode != null) {
			currentNode = prevNode;
		}

		assert currentNode.getNumberOfData() >= minNumberOfData || currentNode == rootNode;
		try {
			assert minNumberOfData > 0 || (currentNode instanceof LeafNode);
		} catch (AssertionError e) {
			logger.debug(e.getMessage());
		}
		return currentNode;

	}

	/**
	 * Passes the given item through this CART and returns the interpretation.
	 * 
	 * @param target
	 *            the target to analyze
	 * @param minNumberOfData
	 *            the minimum number of data requested. If this is 0, walk down the CART until the leaf level.
	 * 
	 * @return the interpretation
	 */
	public Object interpret(Target target, int minNumberOfData) {

		// get the indices from the leaf node
		Object result = this.interpretToNode(target, minNumberOfData).getAllData();

		return result;

	}

	/**
	 * In this tree, replace the given leaf with the given CART
	 * 
	 * @param cart
	 *            the CART
	 * @param leaf
	 *            the leaf
	 * @return the ex-root node from cart which now replaces leaf.
	 */
	public static Node replaceLeafByCart(CART cart, LeafNode leaf) {
		DecisionNode mother = (DecisionNode) leaf.getMother();
		Node newNode = cart.getRootNode();
		mother.replaceDaughter(newNode, leaf.getNodeIndex());
		newNode.setIsRoot(false);
		return newNode;
	}

}
