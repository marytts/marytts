/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.cart.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.StringTokenizer;

import marytts.cart.DecisionNode;
import marytts.cart.LeafNode;
import marytts.cart.Node;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.util.data.MaryHeader;

/**
 * IO functions for CARTs in WagonCART format
 * 
 * @author Anna Hunecke, Marc Schröder, Marcela Charfuelan
 */
public class WagonCARTReader {

	private Node rootNode;
	private Node lastNode;

	// knows the index numbers and types of the features used in DecisionNodes
	private FeatureDefinition featDef;

	private int openBrackets;

	// Since it is not known from the wagon file lines which kind of leaves
	// should be read, a leafType argument should be provided when creating
	// this class.
	private LeafNode.LeafType leafType;

	// added because StringCART
	private int targetFeature;

	/**
	 * When creating a WagonCARTReader provide a tree type
	 * 
	 * @param leafType
	 *            ClasificationTree, ExtendedClassificationTree, RegressionTree, or TopLevelTree.
	 * 
	 *            <p>
	 *            ClasificationTree &rarr; IntArrayLeafNode
	 *            <p>
	 *            ExtendedClassificationTree &rarr; IntAndFloatArrayLeafNode
	 *            <p>
	 *            RegressionTree &rarr; FloatLeafNode
	 *            <p>
	 *            TopLevelTree &rarr; FeatureVectorLeafNode
	 *            <p>
	 *            StringCART &rarr; StringAndFloatLeafNode
	 */
	public WagonCARTReader(LeafNode.LeafType leafType) {
		this.leafType = leafType;
	}

	/**
	 * For a line representing a leaf in Wagon format, create a leaf. This method decides which implementation of LeafNode is
	 * used, i.e. which data format is appropriate. Lines are of the form ((index1 float1)...(indexN floatN)) 0))
	 * 
	 * @param line
	 *            a line from a wagon cart file, representing a leaf
	 * @return a leaf node representing the line.
	 */
	protected LeafNode createLeafNode(String line) {
		if (leafType == LeafNode.LeafType.IntArrayLeafNode)
			return (createIntArrayLeafNode(line));
		else if (leafType == LeafNode.LeafType.IntAndFloatArrayLeafNode)
			return (createIntAndFloatArrayLeafNode(line));
		else if (leafType == LeafNode.LeafType.FloatLeafNode)
			return (createFloatLeafNode(line));
		else if (leafType == LeafNode.LeafType.FeatureVectorLeafNode)
			return (createFeatureVectorLeafNode(line));
		else if (leafType == LeafNode.LeafType.StringAndFloatLeafNode)
			return (createStringAndFloatLeafNode(line));
		else
			return null;
	}

	// in case of using the reader more than once for different root nodes.
	private void cleadReader() {
		rootNode = null;
		lastNode = null;
		featDef = null;
		openBrackets = 0;
	}

	/**
	 * 
	 * This loads a cart from a wagon tree in textual format, from a reader.
	 * 
	 * @param reader
	 *            the Reader providing the wagon tree
	 * @param featDefinition
	 *            featDefinition
	 * @throws IOException
	 *             IOException
	 * @return rootNode
	 */
	public Node load(BufferedReader reader, FeatureDefinition featDefinition) throws IOException {
		cleadReader();
		featDef = featDefinition;
		openBrackets = 0;
		String line = reader.readLine();
		if (line.equals("")) {// first line is empty, read again
			line = reader.readLine();
		}
		// each line corresponds to a node
		// for each line
		while (line != null) {
			if (!line.startsWith(";;") && !line.equals("")) {
				// parse the line and add the node

				parseAndAdd(line);
			}
			line = reader.readLine();
		}
		// make sure we closed as many brackets as we opened
		if (openBrackets != 0) {
			throw new IOException("Error loading CART: bracket mismatch");
		}
		// Now count all data once, so that getNumberOfData()
		// will return the correct figure.
		if (rootNode instanceof DecisionNode)
			((DecisionNode) rootNode).countData();

		return rootNode;
	}

	/**
	 * Load the cart from the given file
	 * 
	 * @param fileName
	 *            the file to load the cart from
	 * @param featDefinition
	 *            the feature definition
	 * @param dummy
	 *            unused, just here for compatibility with the FeatureFileIndexer.
	 * @throws IOException
	 *             if a problem occurs while loading
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 * @return rootNode
	 */
	// TODO: CHECK! do we need that String[] dummy???
	public Node load(String fileName, FeatureDefinition featDefinition, String[] dummy) throws IOException,
			MaryConfigurationException {
		cleadReader();
		// System.out.println("Loading file");
		// open the CART-File and read the header
		DataInput raf = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
		MaryHeader maryHeader = new MaryHeader(raf);
		if (!maryHeader.hasCurrentVersion()) {
			throw new IOException("Wrong version of database file");
		}
		if (maryHeader.getType() != MaryHeader.CARTS) {
			throw new IOException("No CARTs file");
		}
		// System.out.println("Reading CART");
		// discard number of CARTs and CART name
		// TODO: Change format of CART-File
		int numNodes = raf.readInt();
		raf.readUTF();

		// load the CART
		featDef = featDefinition;
		// get the backtrace information
		openBrackets = 0;
		// Not elegant, but robust
		try {
			while (true) {
				// parse the line and add the node
				int length = raf.readInt();
				char[] cartChars = new char[length];
				for (int i = 0; i < length; i++) {
					cartChars[i] = raf.readChar();
				}
				String cart = new String(cartChars);
				// System.out.println(cart);
				parseAndAdd(cart);
			}
		} catch (EOFException eof) {
		}

		// make sure we closed as many brackets as we opened
		if (openBrackets != 0) {
			throw new IOException("Error loading CART: bracket mismatch: " + openBrackets);
		}
		// Now count all data once, so that getNumberOfData()
		// will return the correct figure.
		if (rootNode instanceof DecisionNode)
			((DecisionNode) rootNode).countData();
		// System.out.println("Done");

		return rootNode;
	}

	/**
	 * Creates a node from the given input line and add it to the CART.
	 * 
	 * @param line
	 *            a line of input to parse
	 * @throws IOException
	 *             if the line has an unexpected format
	 */
	private void parseAndAdd(String line) throws IOException {
		// remove whitespace
		line = line.trim();
		// at beginning of String there should be at least two opening brackets
		if (!(line.startsWith("(("))) {
			throw new IOException("Invalid input line for CART: " + line);
		}
		if (Character.isLetter(line.charAt(2)) && !line.substring(2, 6).equals("nan ")) { // we have a node
			openBrackets++; // do not count first bracket

			// get the properties of the node
			StringTokenizer tokenizer = new StringTokenizer(line, " ");
			String feature = tokenizer.nextToken().substring(2);
			String type = tokenizer.nextToken();
			String value = tokenizer.nextToken();
			value = value.substring(0, value.length() - 1);
			// some values are enclosed in double quotes:
			if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 2)
				value = value.substring(1, value.length() - 1);

			// a literal double quote is escaped by backslash, so unescape it:
			if (value.contains("\\\"")) {
				value = value.replaceAll("\\\\\"", "\"");
			}

			// build new node depending on type

			Node nextNode;
			try {
				if (type.equals("is")) {
					if (featDef.isByteFeature(feature)) {
						nextNode = new DecisionNode.BinaryByteDecisionNode(feature, value, featDef);
					} else {
						nextNode = new DecisionNode.BinaryShortDecisionNode(feature, value, featDef);
					}
				} else {
					if (type.equals("<")) {
						nextNode = new DecisionNode.BinaryFloatDecisionNode(feature, Float.parseFloat(value), featDef);
					} else {
						if (type.equals("isShortOf")) {
							nextNode = new DecisionNode.ShortDecisionNode(feature, Integer.parseInt(value), featDef);
						} else {
							if (type.equals("isByteOf")) {
								nextNode = new DecisionNode.ByteDecisionNode(feature, Integer.parseInt(value), featDef);
							} else {
								throw new IOException("Unknown node type : " + type);
							}
						}
					}
				}

			} catch (Exception exc) {
				throw new RuntimeException("Cannot create decision node for cart line: '" + line + "'", exc);
			}

			if (lastNode != null) {
				// this node is the daughter of the last node
				((DecisionNode) lastNode).addDaughter(nextNode);
			} else {
				// this is the rootNode
				rootNode = nextNode;
				nextNode.setIsRoot(true);
			}

			// go one step down
			lastNode = nextNode;

		} else { // we have a leaf

			Node nextNode = createLeafNode(line);

			// set the relations of this node to the others
			if (lastNode == null) { // this node is the root
				rootNode = nextNode;
				nextNode.setIsRoot(true);
			} else { // this node is a daughter of lastNode
				((DecisionNode) lastNode).addDaughter(nextNode);
			}

			// look at the bracketing at the end of the line:
			// get the last token out of the tokenizer
			StringTokenizer tokenizer = new StringTokenizer(line, " ");
			for (int i = 0, numTokens = tokenizer.countTokens(); i < numTokens - 1; i++) {
				tokenizer.nextToken();
			}
			String lastToken = tokenizer.nextToken();

			// lastToken should look like "0))"
			// more than two brackets mean that this is
			// the last daughter of one or more nodes
			int length = lastToken.length();
			// start looking at the characters after "0))"
			int index = lastToken.indexOf(')') + 2;

			while (index < length) { // while we have more characters
				char nextChar = lastToken.charAt(index);
				if (nextChar == ')') {
					// if the next character is a closing bracket
					openBrackets--;
					// this is the last daughter of lastNode,
					// try going one step up
					if (lastNode.isRoot()) {
						if (index + 1 != length) {
							// lastNode should not be the root,
							// unless we are at the last bracket
							throw new IOException("Too many closing brackets in line " + line);
						}
					} else { // you can go one step up
						nextNode = lastNode;
						lastNode = lastNode.getMother();
					}
				} else {
					// nextChar is not a closing bracket;
					// something went wrong here
					throw new IOException("Expected closing bracket in line " + line + ", but found " + nextChar);
				}
				index++;
			}
			// for debugging
			if (nextNode != null) {
				int nodeIndex = nextNode.getNodeIndex();
			}

		}
	}

	protected LeafNode createIntArrayLeafNode(String line) {
		StringTokenizer tok = new StringTokenizer(line, " ");
		// read the indices from the tokenized String
		int numTokens = tok.countTokens();
		int index = 0;
		// The data to be saved in the leaf node:
		int[] indices;
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
		return new LeafNode.IntArrayLeafNode(indices);
	}

	protected LeafNode createIntAndFloatArrayLeafNode(String line) {
		StringTokenizer tok = new StringTokenizer(line, " ");
		// read the indices from the tokenized String
		int numTokens = tok.countTokens();
		int index = 0;
		// The data to be saved in the leaf node:
		int[] indices;
		// The floats to be saved in the leaf node:
		float[] probs;

		// System.out.println("Line: "+line+", numTokens: "+numTokens);

		if (numTokens == 2) { // we do not have any indices
			// discard useless token
			tok.nextToken();
			indices = new int[0];
			probs = new float[0];
		} else {
			indices = new int[(numTokens - 1) / 2];
			// same length
			probs = new float[indices.length];

			while (index * 2 < numTokens - 1) {
				String token = tok.nextToken();
				if (index == 0) {
					token = token.substring(4);
				} else {
					token = token.substring(1);
				}
				// System.out.println("int-token: "+token);
				indices[index] = Integer.parseInt(token);

				token = tok.nextToken();
				int lastIndex = token.length() - 1;
				if ((index * 2) == (numTokens - 3)) {
					token = token.substring(0, lastIndex - 1);
					if (token.equals("inf")) {
						probs[index] = 10000;
						index++;
						continue;
					}
					if (token.equals("nan")) {
						probs[index] = -1;
						index++;
						continue;
					}
				} else {
					token = token.substring(0, lastIndex);
					if (token.equals("inf")) {
						probs[index] = 1000000;
						index++;
						continue;
					}
					if (token.equals("nan")) {
						probs[index] = -1;
						index++;
						continue;
					}
				}
				// System.out.println("float-token: "+token);
				probs[index] = Float.parseFloat(token);
				index++;
			} // end while

		} // end if

		return new LeafNode.IntAndFloatArrayLeafNode(indices, probs);
	}

	protected LeafNode createFloatLeafNode(String line) {
		StringTokenizer tok = new StringTokenizer(line, " ");
		// read the indices from the tokenized String
		int numTokens = tok.countTokens();
		if (numTokens != 2) { // we need exactly one value pair
			throw new IllegalArgumentException("Expected two tokens in line, got " + numTokens + ": '" + line + "'");
		}

		// The data to be saved in the leaf node:
		float[] data = new float[2]; // stddev and mean;
		String nextToken = tok.nextToken();
		nextToken = nextToken.substring(2);
		try {
			data[0] = Float.parseFloat(nextToken);
		} catch (NumberFormatException nfe) {
			data[0] = 0; // cannot make sense of the standard deviation
		}
		nextToken = tok.nextToken();
		nextToken = nextToken.substring(0, nextToken.indexOf(")"));
		try {
			data[1] = Float.parseFloat(nextToken);
		} catch (NumberFormatException nfe) {
			data[1] = 0;
		}
		return new LeafNode.FloatLeafNode(data);
	}

	protected LeafNode createFeatureVectorLeafNode(String line) {

		StringTokenizer tok = new StringTokenizer(line, " ");
		// read the indices from the tokenized String
		int numTokens = tok.countTokens();
		int index = 0;
		// The data to be saved in the leaf node:
		if (numTokens != 2) {
			// leaf is not empty -> error
			throw new Error("Leaf in line " + line + " is not empty");
		}
		// discard useless token
		tok.nextToken();
		return new LeafNode.FeatureVectorLeafNode();

	}

	/**
	 * Fill the FeatureVector leafs of a tree with the given feature vectors. This function is only used in TopLeavelTree.
	 * 
	 * @param root
	 *            node of the tree.
	 * @param featureVectors
	 *            the feature vectors.
	 */
	public void fillLeafs(Node root, FeatureVector[] featureVectors) {
		if (leafType == LeafNode.LeafType.FeatureVectorLeafNode) {
			rootNode = root;
			Node currentNode = rootNode;
			Node prevNode = null;

			// loop trough the feature vectors
			for (int i = 0; i < featureVectors.length; i++) {
				currentNode = rootNode;
				prevNode = null;
				FeatureVector featureVector = featureVectors[i];
				// logger.debug("Starting cart at "+nodeIndex);
				while (!(currentNode instanceof LeafNode)) {
					// while we have not reached the bottom,
					// get the next node based on the features of the target
					prevNode = currentNode;
					currentNode = ((DecisionNode) currentNode).getNextNode(featureVector);
					// logger.debug(decision.toString() + " result '"+
					// decision.findFeature(item) + "' => "+ nodeIndex);
				}
				// add the feature vector to the leaf node
				((LeafNode.FeatureVectorLeafNode) currentNode).addFeatureVector(featureVector);
			}
		} else
			throw new IllegalArgumentException("The leaves of this tree are not FeatureVectorLeafNode.");

	}

	protected LeafNode createStringAndFloatLeafNode(String line) {
		// Note: this code is identical to createIntAndFloatArrayLeafNode(),
		// except for the last line.
		StringTokenizer tok = new StringTokenizer(line, " ");
		// read the indices from the tokenized String
		int numTokens = tok.countTokens();
		int index = 0;
		// The data to be saved in the leaf node:
		int[] indices;
		// The floats to be saved in the leaf node:
		float[] probs;

		// System.out.println("Line: "+line+", numTokens: "+numTokens);

		if (numTokens == 2) { // we do not have any indices
			// discard useless token
			tok.nextToken();
			indices = new int[0];
			probs = new float[0];
		} else {
			indices = new int[(numTokens - 1) / 2];
			// same length
			probs = new float[indices.length];

			while (index * 2 < numTokens - 1) {
				String token = tok.nextToken();
				if (index == 0) {
					token = token.substring(4);
				} else {
					token = token.substring(1);
				}
				// System.out.println("int-token: "+token);
				indices[index] = Integer.parseInt(token);

				token = tok.nextToken();
				int lastIndex = token.length() - 1;
				if ((index * 2) == (numTokens - 3)) {
					token = token.substring(0, lastIndex - 1);
					if (token.equals("inf")) {
						probs[index] = 10000;
						index++;
						continue;
					}
					if (token.equals("nan")) {
						probs[index] = -1;
						index++;
						continue;
					}
				} else {
					token = token.substring(0, lastIndex);
					if (token.equals("inf")) {
						probs[index] = 1000000;
						index++;
						continue;
					}
					if (token.equals("nan")) {
						probs[index] = -1;
						index++;
						continue;
					}
				}
				// System.out.println("float-token: "+token);
				probs[index] = Float.parseFloat(token);
				index++;
			} // end while

		} // end if

		return new LeafNode.StringAndFloatLeafNode(indices, probs);
	}

}
