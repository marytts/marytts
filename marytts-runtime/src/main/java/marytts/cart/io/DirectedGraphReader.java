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
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import marytts.cart.DecisionNode;
import marytts.cart.DirectedGraph;
import marytts.cart.DirectedGraphNode;
import marytts.cart.LeafNode;
import marytts.cart.Node;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.util.data.MaryHeader;

/**
 * IO functions for Directed graphs in Mary format
 * 
 * @author Marcela Charfuelan, Marc Schröder
 */
public class DirectedGraphReader {
	/** Bit code for identifying a node id as a leaf node id in binary DirectedGraph files */
	public static int LEAFNODE = 0;
	/** Bit code for identifying a node id as a decision node id in binary DirectedGraph files */
	public static int DECISIONNODE = 1;
	/** Bit code for identifying a node id as a directed node id in binary DirectedGraph files */
	public static int DIRECTEDGRAPHNODE = 2;

	/**
	 * Load the directed graph from the given file
	 * 
	 * @param fileName
	 *            the file to load the cart from
	 * @throws IOException
	 *             , {@link MaryConfigurationException} if a problem occurs while loading
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 * @return load(is)
	 */
	public DirectedGraph load(String fileName) throws IOException, MaryConfigurationException {
		InputStream is = new FileInputStream(fileName);
		try {
			return load(is);
		} finally {
			is.close();
		}
	}

	/**
	 * Load the directed graph from the given file
	 * 
	 * @param inStream
	 *            the input stream
	 * @throws IOException
	 *             , {@link MaryConfigurationException} if a problem occurs while loading
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 * @return MaryCARTReader().loadFromStream(buffInStream)
	 */
	public DirectedGraph load(InputStream inStream) throws IOException, MaryConfigurationException {
		BufferedInputStream buffInStream = new BufferedInputStream(inStream);
		assert buffInStream.markSupported();
		buffInStream.mark(10000);
		// open the CART-File and read the header
		DataInput raf = new DataInputStream(buffInStream);

		MaryHeader maryHeader = new MaryHeader(raf);
		if (!maryHeader.hasCurrentVersion()) {
			throw new IOException("Wrong version of database file");
		}
		if (maryHeader.getType() != MaryHeader.DIRECTED_GRAPH) {
			if (maryHeader.getType() == MaryHeader.CARTS) {
				buffInStream.reset();
				return new MaryCARTReader().loadFromStream(buffInStream);
			} else {
				throw new IOException("Not a directed graph file");
			}
		}

		// Read properties
		short propDataLength = raf.readShort();
		Properties props;
		if (propDataLength == 0) {
			props = null;
		} else {
			byte[] propsData = new byte[propDataLength];
			raf.readFully(propsData);
			ByteArrayInputStream bais = new ByteArrayInputStream(propsData);
			props = new Properties();
			props.load(bais);
			bais.close();
		}

		// Read the feature definition
		FeatureDefinition featureDefinition = new FeatureDefinition(raf);

		// read the decision nodes
		int numDecNodes = raf.readInt(); // number of decision nodes

		// First we need to read all nodes into memory, then we can link them properly
		// in terms of parent/child.
		DecisionNode[] dns = new DecisionNode[numDecNodes];
		int[][] childIndexes = new int[numDecNodes][];
		for (int i = 0; i < numDecNodes; i++) {
			// read one decision node
			int featureNameIndex = raf.readInt();
			int nodeTypeNr = raf.readInt();
			DecisionNode.Type nodeType = DecisionNode.Type.values()[nodeTypeNr];
			int numChildren = 2; // for binary nodes
			switch (nodeType) {
			case BinaryByteDecisionNode:
				int criterion = raf.readInt();
				dns[i] = new DecisionNode.BinaryByteDecisionNode(featureNameIndex, (byte) criterion, featureDefinition);
				break;
			case BinaryShortDecisionNode:
				criterion = raf.readInt();
				dns[i] = new DecisionNode.BinaryShortDecisionNode(featureNameIndex, (short) criterion, featureDefinition);
				break;
			case BinaryFloatDecisionNode:
				float floatCriterion = raf.readFloat();
				dns[i] = new DecisionNode.BinaryFloatDecisionNode(featureNameIndex, floatCriterion, featureDefinition);
				break;
			case ByteDecisionNode:
				numChildren = raf.readInt();
				if (featureDefinition.getNumberOfValues(featureNameIndex) != numChildren) {
					throw new IOException("Inconsistent cart file: feature " + featureDefinition.getFeatureName(featureNameIndex)
							+ " should have " + featureDefinition.getNumberOfValues(featureNameIndex)
							+ " values, but decision node " + i + " has only " + numChildren + " child nodes");
				}
				dns[i] = new DecisionNode.ByteDecisionNode(featureNameIndex, numChildren, featureDefinition);
				break;
			case ShortDecisionNode:
				numChildren = raf.readInt();
				if (featureDefinition.getNumberOfValues(featureNameIndex) != numChildren) {
					throw new IOException("Inconsistent cart file: feature " + featureDefinition.getFeatureName(featureNameIndex)
							+ " should have " + featureDefinition.getNumberOfValues(featureNameIndex)
							+ " values, but decision node " + i + " has only " + numChildren + " child nodes");
				}
				dns[i] = new DecisionNode.ShortDecisionNode(featureNameIndex, numChildren, featureDefinition);
			}
			dns[i].setUniqueDecisionNodeId(i + 1);
			// now read the children, indexes only:
			childIndexes[i] = new int[numChildren];
			for (int k = 0; k < numChildren; k++) {
				childIndexes[i][k] = raf.readInt();
			}
		}

		// read the leaves
		int numLeafNodes = raf.readInt(); // number of leaves, it does not include empty leaves
		LeafNode[] lns = new LeafNode[numLeafNodes];

		for (int j = 0; j < numLeafNodes; j++) {
			// read one leaf node
			int leafTypeNr = raf.readInt();
			LeafNode.LeafType leafNodeType = LeafNode.LeafType.values()[leafTypeNr];
			switch (leafNodeType) {
			case IntArrayLeafNode:
				int numData = raf.readInt();
				int[] data = new int[numData];
				for (int d = 0; d < numData; d++) {
					data[d] = raf.readInt();
				}
				lns[j] = new LeafNode.IntArrayLeafNode(data);
				break;
			case FloatLeafNode:
				float stddev = raf.readFloat();
				float mean = raf.readFloat();
				lns[j] = new LeafNode.FloatLeafNode(new float[] { stddev, mean });
				break;
			case IntAndFloatArrayLeafNode:
			case StringAndFloatLeafNode:
				int numPairs = raf.readInt();
				int[] ints = new int[numPairs];
				float[] floats = new float[numPairs];
				for (int d = 0; d < numPairs; d++) {
					ints[d] = raf.readInt();
					floats[d] = raf.readFloat();
				}
				if (leafNodeType == LeafNode.LeafType.IntAndFloatArrayLeafNode)
					lns[j] = new LeafNode.IntAndFloatArrayLeafNode(ints, floats);
				else
					lns[j] = new LeafNode.StringAndFloatLeafNode(ints, floats);
				break;
			case FeatureVectorLeafNode:
				throw new IllegalArgumentException("Reading feature vector leaf nodes is not yet implemented");
			case PdfLeafNode:
				throw new IllegalArgumentException("Reading pdf leaf nodes is not yet implemented");
			}
			lns[j].setUniqueLeafId(j + 1);
		}

		// Graph nodes
		int numDirectedGraphNodes = raf.readInt();
		DirectedGraphNode[] graphNodes = new DirectedGraphNode[numDirectedGraphNodes];
		int[] dgnLeafIndices = new int[numDirectedGraphNodes];
		int[] dgnDecIndices = new int[numDirectedGraphNodes];
		for (int g = 0; g < numDirectedGraphNodes; g++) {
			graphNodes[g] = new DirectedGraphNode(null, null);
			graphNodes[g].setUniqueGraphNodeID(g + 1);
			dgnLeafIndices[g] = raf.readInt();
			dgnDecIndices[g] = raf.readInt();
		}

		// Now, link up the decision nodes with their daughters
		for (int i = 0; i < numDecNodes; i++) {
			// System.out.print(dns[i]+" "+dns[i].getFeatureName()+" ");
			for (int k = 0; k < childIndexes[i].length; k++) {
				Node child = childIndexToNode(childIndexes[i][k], dns, lns, graphNodes);
				dns[i].addDaughter(child);
				// System.out.print(" "+dns[i].getDaughter(k));
			}
			// System.out.println();
		}
		// And link up directed graph nodes
		for (int g = 0; g < numDirectedGraphNodes; g++) {
			Node leaf = childIndexToNode(dgnLeafIndices[g], dns, lns, graphNodes);
			graphNodes[g].setLeafNode(leaf);
			Node dec = childIndexToNode(dgnDecIndices[g], dns, lns, graphNodes);
			if (dec != null && !dec.isDecisionNode())
				throw new IllegalArgumentException("Only decision nodes allowed, read " + dec.getClass());
			graphNodes[g].setDecisionNode((DecisionNode) dec);
			// System.out.println("Graph node "+(g+1)+", leaf: "+Integer.toHexString(dgnLeafIndices[g])+", "+leaf+" -- dec: "+Integer.toHexString(dgnDecIndices[g])+", "+dec);
		}

		Node rootNode;
		if (graphNodes.length > 0) {
			rootNode = graphNodes[0];
		} else if (dns.length > 0) {
			rootNode = dns[0];
			// CART behaviour, not sure if this is needed:
			// Now count all data once, so that getNumberOfData()
			// will return the correct figure.
			((DecisionNode) rootNode).countData();
		} else if (lns.length > 0) {
			rootNode = lns[0]; // single-leaf tree...
		} else {
			rootNode = null;
		}

		// set the rootNode as the rootNode of cart
		return new DirectedGraph(rootNode, featureDefinition, props);
	}

	private Node childIndexToNode(int childIndexAndType, DecisionNode[] dns, LeafNode[] lns, DirectedGraphNode[] graphNodes) {
		int childIndex = childIndexAndType & 0x3fffffff; // the lower 30 bits
		int childType = (childIndexAndType >> 30) & 0x03; // the highest two bits
		if (childIndex == 0) { // an empty leaf
			return null;
		} else if (childType == DECISIONNODE) { // a decision node
			assert childIndex - 1 < dns.length;
			return dns[childIndex - 1];
		} else if (childType == LEAFNODE) { // a leaf node
			assert childIndex - 1 < lns.length;
			return lns[childIndex - 1];
		} else if (childType == DIRECTEDGRAPHNODE) {
			assert childIndex - 1 < graphNodes.length;
			return graphNodes[childIndex - 1];
		} else {
			throw new IllegalArgumentException("Unexpected child type: " + childType);
		}

	}
}
