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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Properties;

import marytts.cart.CART;
import marytts.cart.DecisionNode;
import marytts.cart.LeafNode;
import marytts.cart.Node;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.util.data.MaryHeader;

/**
 * IO functions for CARTs in MaryCART format
 * 
 * @author Marcela Charfuelan
 */
public class MaryCARTReader {
	/**
	 * Load the cart from the given file
	 * 
	 * @param fileName
	 *            the file to load the cart from
	 * @throws IOException
	 *             if a problem occurs while loading
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 * @return loadFromStream(fis)
	 */
	public CART load(String fileName) throws IOException, MaryConfigurationException {
		FileInputStream fis = new FileInputStream(fileName);
		try {
			return loadFromStream(fis);
		} finally {
			fis.close();
		}
	}

	/**
	 * Load the cart from the given file
	 * 
	 * @param inStream
	 *            the stream to load the cart from
	 * @throws IOException
	 *             if a problem occurs while loading
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 * @return CART(rootNode, featureDefinition, props)
	 */
	public CART loadFromStream(InputStream inStream) throws IOException, MaryConfigurationException {
		// open the CART-File and read the header
		DataInput raf = new DataInputStream(new BufferedInputStream(inStream));

		MaryHeader maryHeader = new MaryHeader(raf);
		if (!maryHeader.hasCurrentVersion()) {
			throw new IOException("Wrong version of database file");
		}
		if (maryHeader.getType() != MaryHeader.CARTS) {
			throw new IOException("No CARTs file");
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
		}

		// Now, link up the decision nodes with their daughters
		for (int i = 0; i < numDecNodes; i++) {
			for (int k = 0; k < childIndexes[i].length; k++) {
				int childIndex = childIndexes[i][k];
				if (childIndex < 0) { // a decision node
					assert -childIndex - 1 < numDecNodes;
					dns[i].addDaughter(dns[-childIndex - 1]);
				} else if (childIndex > 0) { // a leaf node
					dns[i].addDaughter(lns[childIndex - 1]);
				} else { // == 0, an empty leaf
					dns[i].addDaughter(null);
				}
			}
		}

		Node rootNode;
		if (dns.length > 0) {
			rootNode = dns[0];
			// Now count all data once, so that getNumberOfData()
			// will return the correct figure.
			((DecisionNode) rootNode).countData();
		} else if (lns.length > 0) {
			rootNode = lns[0]; // single-leaf tree...
		} else {
			rootNode = null;
		}

		// set the rootNode as the rootNode of cart
		return new CART(rootNode, featureDefinition, props);
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
	 */
	private CART loadFromByteBuffer(String fileName) throws IOException, MaryConfigurationException {
		// open the CART-File and read the header
		FileInputStream fis = new FileInputStream(fileName);
		FileChannel fc = fis.getChannel();
		ByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
		fis.close();

		MaryHeader maryHeader = new MaryHeader(bb);
		if (!maryHeader.hasCurrentVersion()) {
			throw new IOException("Wrong version of database file");
		}
		if (maryHeader.getType() != MaryHeader.CARTS) {
			throw new IOException("No CARTs file");
		}

		// Read properties
		short propDataLength = bb.getShort();
		Properties props;
		if (propDataLength == 0) {
			props = null;
		} else {
			byte[] propsData = new byte[propDataLength];
			bb.get(propsData);
			ByteArrayInputStream bais = new ByteArrayInputStream(propsData);
			props = new Properties();
			props.load(bais);
			bais.close();
		}

		// Read the feature definition
		FeatureDefinition featureDefinition = new FeatureDefinition(bb);

		// read the decision nodes
		int numDecNodes = bb.getInt(); // number of decision nodes

		// First we need to read all nodes into memory, then we can link them properly
		// in terms of parent/child.
		DecisionNode[] dns = new DecisionNode[numDecNodes];
		int[][] childIndexes = new int[numDecNodes][];
		for (int i = 0; i < numDecNodes; i++) {
			// read one decision node
			int featureNameIndex = bb.getInt();
			int nodeTypeNr = bb.getInt();
			DecisionNode.Type nodeType = DecisionNode.Type.values()[nodeTypeNr];
			int numChildren = 2; // for binary nodes
			switch (nodeType) {
			case BinaryByteDecisionNode:
				int criterion = bb.getInt();
				dns[i] = new DecisionNode.BinaryByteDecisionNode(featureNameIndex, (byte) criterion, featureDefinition);
				break;
			case BinaryShortDecisionNode:
				criterion = bb.getInt();
				dns[i] = new DecisionNode.BinaryShortDecisionNode(featureNameIndex, (short) criterion, featureDefinition);
				break;
			case BinaryFloatDecisionNode:
				float floatCriterion = bb.getFloat();
				dns[i] = new DecisionNode.BinaryFloatDecisionNode(featureNameIndex, floatCriterion, featureDefinition);
				break;
			case ByteDecisionNode:
				numChildren = bb.getInt();
				if (featureDefinition.getNumberOfValues(featureNameIndex) != numChildren) {
					throw new IOException("Inconsistent cart file: feature " + featureDefinition.getFeatureName(featureNameIndex)
							+ " should have " + featureDefinition.getNumberOfValues(featureNameIndex)
							+ " values, but decision node " + i + " has only " + numChildren + " child nodes");
				}
				dns[i] = new DecisionNode.ByteDecisionNode(featureNameIndex, numChildren, featureDefinition);
				break;
			case ShortDecisionNode:
				numChildren = bb.getInt();
				if (featureDefinition.getNumberOfValues(featureNameIndex) != numChildren) {
					throw new IOException("Inconsistent cart file: feature " + featureDefinition.getFeatureName(featureNameIndex)
							+ " should have " + featureDefinition.getNumberOfValues(featureNameIndex)
							+ " values, but decision node " + i + " has only " + numChildren + " child nodes");
				}
				dns[i] = new DecisionNode.ShortDecisionNode(featureNameIndex, numChildren, featureDefinition);
			}
			// now read the children, indexes only:
			childIndexes[i] = new int[numChildren];
			for (int k = 0; k < numChildren; k++) {
				childIndexes[i][k] = bb.getInt();
			}
		}

		// read the leaves
		int numLeafNodes = bb.getInt(); // number of leaves, it does not include empty leaves
		LeafNode[] lns = new LeafNode[numLeafNodes];

		for (int j = 0; j < numLeafNodes; j++) {
			// read one leaf node
			int leafTypeNr = bb.getInt();
			LeafNode.LeafType leafNodeType = LeafNode.LeafType.values()[leafTypeNr];
			switch (leafNodeType) {
			case IntArrayLeafNode:
				int numData = bb.getInt();
				int[] data = new int[numData];
				for (int d = 0; d < numData; d++) {
					data[d] = bb.getInt();
				}
				lns[j] = new LeafNode.IntArrayLeafNode(data);
				break;
			case FloatLeafNode:
				float stddev = bb.getFloat();
				float mean = bb.getFloat();
				lns[j] = new LeafNode.FloatLeafNode(new float[] { stddev, mean });
				break;
			case IntAndFloatArrayLeafNode:
			case StringAndFloatLeafNode:
				int numPairs = bb.getInt();
				int[] ints = new int[numPairs];
				float[] floats = new float[numPairs];
				for (int d = 0; d < numPairs; d++) {
					ints[d] = bb.getInt();
					floats[d] = bb.getFloat();
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
		}

		// Now, link up the decision nodes with their daughters
		for (int i = 0; i < numDecNodes; i++) {
			for (int k = 0; k < childIndexes[i].length; k++) {
				int childIndex = childIndexes[i][k];
				if (childIndex < 0) { // a decision node
					assert -childIndex - 1 < numDecNodes;
					dns[i].addDaughter(dns[-childIndex - 1]);
				} else if (childIndex > 0) { // a leaf node
					dns[i].addDaughter(lns[childIndex - 1]);
				} else { // == 0, an empty leaf
					dns[i].addDaughter(null);
				}
			}
		}

		Node rootNode;
		if (dns.length > 0) {
			rootNode = dns[0];
			// Now count all data once, so that getNumberOfData()
			// will return the correct figure.
			((DecisionNode) rootNode).countData();
		} else if (lns.length > 0) {
			rootNode = lns[0]; // single-leaf tree...
		} else {
			rootNode = null;
		}

		// set the rootNode as the rootNode of cart
		return new CART(rootNode, featureDefinition, props);
	}
}
