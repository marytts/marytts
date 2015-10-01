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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import marytts.cart.CART;
import marytts.cart.DecisionNode;
import marytts.cart.LeafNode;
import marytts.cart.Node;
import marytts.cart.DecisionNode.BinaryByteDecisionNode;
import marytts.cart.DecisionNode.BinaryFloatDecisionNode;
import marytts.cart.DecisionNode.BinaryShortDecisionNode;
import marytts.cart.LeafNode.FeatureVectorLeafNode;
import marytts.cart.LeafNode.FloatLeafNode;
import marytts.cart.LeafNode.IntAndFloatArrayLeafNode;
import marytts.cart.LeafNode.IntArrayLeafNode;
import marytts.cart.LeafNode.LeafType;
import marytts.features.FeatureVector;
import marytts.util.MaryUtils;
import marytts.util.data.MaryHeader;

import org.apache.log4j.Logger;

/**
 * IO functions for CARTs in MaryCART format
 * 
 * @author Marcela Charfuelan
 */
public class MaryCARTWriter {

	protected Logger logger = MaryUtils.getLogger(this.getClass().getName());

	/**
	 * Dump the CARTs in MaryCART format
	 * 
	 * @param cart
	 *            cart
	 * @param destFile
	 *            the destination file
	 * @throws IOException
	 *             IOException
	 */
	public void dumpMaryCART(CART cart, String destFile) throws IOException {
		if (cart == null)
			throw new NullPointerException("Cannot dump null CART");
		if (destFile == null)
			throw new NullPointerException("No destination file");

		logger.debug("Dumping CART in MaryCART format to " + destFile + " ...");

		// Open the destination file (cart.bin) and output the header
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(destFile)));
		// create new CART-header and write it to output file
		MaryHeader hdr = new MaryHeader(MaryHeader.CARTS);
		hdr.writeTo(out);

		Properties props = cart.getProperties();
		if (props == null) {
			out.writeShort(0);
		} else {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			props.store(baos, null);
			byte[] propData = baos.toByteArray();
			out.writeShort(propData.length);
			out.write(propData);
		}

		// feature definition
		cart.getFeatureDefinition().writeBinaryTo(out);

		// dump CART
		dumpBinary(cart.getRootNode(), out);

		// finish
		out.close();
		logger.debug(" ... done\n");
	}

	public void toTextOut(CART cart, PrintWriter pw) throws IOException {
		try {
			int id[] = new int[2];
			id[0] = 0; // number of decision nodes
			id[1] = 0; // number of leaf nodes

			// System.out.println("Total number of nodes:" + rootNode.getNumberOfNodes());
			setUniqueNodeId(cart.getRootNode(), id);
			pw.println("Num decision nodes= " + id[0] + "  Num leaf nodes= " + id[1]);
			printDecisionNodes(cart.getRootNode(), null, pw);
			pw.println("\n----------------\n");
			printLeafNodes(cart.getRootNode(), null, pw);

			pw.flush();
			pw.close();
		} catch (IOException ioe) {
			IOException newIOE = new IOException("Error dumping CART to standard output");
			newIOE.initCause(ioe);
			throw newIOE;
		}
	}

	private void setUniqueNodeId(Node node, int id[]) throws IOException {

		int thisIdNode;
		String leafstr = "";

		// if the node is decision node
		if (node.getNumberOfNodes() > 1) {
			assert node instanceof DecisionNode;
			DecisionNode decNode = (DecisionNode) node;
			id[0]--;
			decNode.setUniqueDecisionNodeId(id[0]);
			String strNode = "";
			// this.decNodeStr = "";
			thisIdNode = id[0];

			// add Ids to the daughters
			for (int i = 0; i < decNode.getNumberOfDaugthers(); i++) {
				setUniqueNodeId(decNode.getDaughter(i), id);
			}

		} else { // the node is a leaf node
			assert node instanceof LeafNode;
			LeafNode leaf = (LeafNode) node;
			if (leaf.isEmpty()) {
				leaf.setUniqueLeafId(0);
			} else {
				id[1]++;
				leaf.setUniqueLeafId(id[1]);
			}

		}

	}

	private void dumpBinary(Node rootNode, DataOutput os) throws IOException {
		try {

			int id[] = new int[2];
			id[0] = 0; // number of decision nodes
			id[1] = 0; // number of leaf nodes
			// first add unique identifiers to decision nodes and leaf nodes
			setUniqueNodeId(rootNode, id);

			// write the number of decision nodes
			os.writeInt(Math.abs(id[0]));
			// lines that start with a negative number are decision nodes
			printDecisionNodes(rootNode, os, null);

			// write the number of leaves.
			os.writeInt(id[1]);
			// lines that start with id are leaf nodes
			printLeafNodes(rootNode, (DataOutputStream) os, null);

		} catch (IOException ioe) {
			IOException newIOE = new IOException("Error dumping CART to output stream");
			newIOE.initCause(ioe);
			throw newIOE;
		}
	}

	private void printDecisionNodes(Node node, DataOutput out, PrintWriter pw) throws IOException {
		if (!(node instanceof DecisionNode))
			return; // nothing to do here

		DecisionNode decNode = (DecisionNode) node;
		int id = decNode.getUniqueDecisionNodeId();
		String nodeDefinition = decNode.getNodeDefinition();
		int featureIndex = decNode.getFeatureIndex();
		DecisionNode.Type nodeType = decNode.getDecisionNodeType();

		if (out != null) {
			// dump in binary form to output
			out.writeInt(featureIndex);
			out.writeInt(nodeType.ordinal());
			// Now, questionValue, which depends on nodeType
			switch (nodeType) {
			case BinaryByteDecisionNode:
				out.writeInt(((BinaryByteDecisionNode) decNode).getCriterionValueAsByte());
				assert decNode.getNumberOfDaugthers() == 2;
				break;
			case BinaryShortDecisionNode:
				out.writeInt(((BinaryShortDecisionNode) decNode).getCriterionValueAsShort());
				assert decNode.getNumberOfDaugthers() == 2;
				break;
			case BinaryFloatDecisionNode:
				out.writeFloat(((BinaryFloatDecisionNode) decNode).getCriterionValueAsFloat());
				assert decNode.getNumberOfDaugthers() == 2;
				break;
			case ByteDecisionNode:
			case ShortDecisionNode:
				out.writeInt(decNode.getNumberOfDaugthers());
			}

			// The child nodes
			for (int i = 0, n = decNode.getNumberOfDaugthers(); i < n; i++) {
				Node daughter = decNode.getDaughter(i);
				if (daughter instanceof DecisionNode) {
					out.writeInt(((DecisionNode) daughter).getUniqueDecisionNodeId());
				} else {
					assert daughter instanceof LeafNode;
					out.writeInt(((LeafNode) daughter).getUniqueLeafId());
				}
			}
		}
		if (pw != null) {
			// dump to print writer
			StringBuilder strNode = new StringBuilder(id + " " + nodeDefinition);
			for (int i = 0, n = decNode.getNumberOfDaugthers(); i < n; i++) {
				strNode.append(" ");
				Node daughter = decNode.getDaughter(i);
				if (daughter instanceof DecisionNode) {
					strNode.append(((DecisionNode) daughter).getUniqueDecisionNodeId());
				} else {
					assert daughter instanceof LeafNode;
					strNode.append("id").append(((LeafNode) daughter).getUniqueLeafId());
				}
			}
			pw.println(strNode.toString());
		}
		// add the daughters
		for (int i = 0; i < ((DecisionNode) node).getNumberOfDaugthers(); i++) {
			if (((DecisionNode) node).getDaughter(i).getNumberOfNodes() > 1)
				printDecisionNodes(((DecisionNode) node).getDaughter(i), out, pw);
		}
	}

	/** This function will print the leaf nodes only, but it goes through all the decision nodes. */
	private void printLeafNodes(Node node, DataOutput out, PrintWriter pw) throws IOException {
		// If the node does not have leaves then it just return.
		// I we are in a decision node then print the leaves of the daughters.
		Node nextNode;
		if (node.getNumberOfNodes() > 1) {
			assert node instanceof DecisionNode;
			DecisionNode decNode = (DecisionNode) node;
			for (int i = 0; i < decNode.getNumberOfDaugthers(); i++) {
				nextNode = decNode.getDaughter(i);
				printLeafNodes(nextNode, out, pw);
			}
		} else {
			assert node instanceof LeafNode;
			LeafNode leaf = (LeafNode) node;
			if (leaf.getUniqueLeafId() == 0) // empty leaf, do not write
				return;
			LeafType leafType = leaf.getLeafNodeType();
			if (leafType == LeafType.FeatureVectorLeafNode) {
				leafType = LeafType.IntArrayLeafNode;
				// save feature vector leaf nodes as int array leaf nodes
			}
			if (out != null) {
				// Leaf node type
				out.writeInt(leafType.ordinal());
			}
			if (pw != null) {
				pw.print("id" + leaf.getUniqueLeafId() + " " + leafType);
			}
			switch (leaf.getLeafNodeType()) {
			case IntArrayLeafNode:
				int data[] = ((IntArrayLeafNode) leaf).getIntData();
				// Number of data points following:
				if (out != null)
					out.writeInt(data.length);
				if (pw != null)
					pw.print(" " + data.length);
				// for each index, write the index
				for (int i = 0; i < data.length; i++) {
					if (out != null)
						out.writeInt(data[i]);
					if (pw != null)
						pw.print(" " + data[i]);
				}
				break;
			case FloatLeafNode:
				float stddev = ((FloatLeafNode) leaf).getStDeviation();
				float mean = ((FloatLeafNode) leaf).getMean();
				if (out != null) {
					out.writeFloat(stddev);
					out.writeFloat(mean);
				}
				if (pw != null) {
					pw.print(" 1 " + stddev + " " + mean);
				}
				break;
			case IntAndFloatArrayLeafNode:
			case StringAndFloatLeafNode:
				int data1[] = ((IntAndFloatArrayLeafNode) leaf).getIntData();
				float floats[] = ((IntAndFloatArrayLeafNode) leaf).getFloatData();
				// Number of data points following:
				if (out != null)
					out.writeInt(data1.length);
				if (pw != null)
					pw.print(" " + data1.length);
				// for each index, write the index and then its float
				for (int i = 0; i < data1.length; i++) {
					if (out != null) {
						out.writeInt(data1[i]);
						out.writeFloat(floats[i]);
					}
					if (pw != null)
						pw.print(" " + data1[i] + " " + floats[i]);
				}
				break;
			case FeatureVectorLeafNode:
				FeatureVector fv[] = ((FeatureVectorLeafNode) leaf).getFeatureVectors();
				// Number of data points following:
				if (out != null)
					out.writeInt(fv.length);
				if (pw != null)
					pw.print(" " + fv.length);
				// for each feature vector, write the index
				for (int i = 0; i < fv.length; i++) {
					if (out != null)
						out.writeInt(fv[i].getUnitIndex());
					if (pw != null)
						pw.print(" " + fv[i].getUnitIndex());
				}
				break;
			case PdfLeafNode:
				throw new IllegalArgumentException("Writing of pdf leaf nodes not yet implemented");
			}
			if (pw != null)
				pw.println();
		}
	}
}
