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

import marytts.cart.DecisionNode;
import marytts.cart.DirectedGraph;
import marytts.cart.DirectedGraphNode;
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
 * IO functions for directed graphs in Mary format
 * 
 * @author Marcela Charfuelan, Marc Schröder
 */
public class DirectedGraphWriter {

	protected Logger logger = MaryUtils.getLogger(this.getClass().getName());

	/**
	 * Dump the graph in Mary format
	 * 
	 * @param graph
	 *            graph
	 * @param destFile
	 *            the destination file
	 * @throws IOException
	 *             IOException
	 */
	public void saveGraph(DirectedGraph graph, String destFile) throws IOException {
		if (graph == null)
			throw new NullPointerException("Cannot dump null graph");
		if (destFile == null)
			throw new NullPointerException("No destination file");

		logger.debug("Dumping directed graph in Mary format to " + destFile + " ...");

		// Open the destination file and output the header
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(destFile)));
		// create new CART-header and write it to output file
		MaryHeader hdr = new MaryHeader(MaryHeader.DIRECTED_GRAPH);
		hdr.writeTo(out);

		Properties props = graph.getProperties();
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
		graph.getFeatureDefinition().writeBinaryTo(out);

		// dump graph
		dumpBinary(graph, out);

		// finish
		out.close();
		logger.debug(" ... done\n");
	}

	public void toTextOut(DirectedGraph graph, PrintWriter pw) throws IOException {
		try {
			int numLeafNodes = setUniqueLeafNodeIds(graph);
			int numDecNodes = setUniqueDecisionNodeIds(graph);
			int numGraphNodes = setUniqueDirectedGraphNodeIds(graph);
			pw.println("Num decision nodes= " + numDecNodes + "  Num leaf nodes= " + numLeafNodes
					+ "  Num directed graph nodes= " + numGraphNodes);
			printDecisionNodes(graph, null, pw);
			pw.println("\n----------------\n");
			printLeafNodes(graph, null, pw);
			pw.println("\n----------------\n");
			printDirectedGraphNodes(graph, null, pw);

			pw.flush();
			pw.close();
		} catch (IOException ioe) {
			IOException newIOE = new IOException("Error dumping graph to standard output");
			newIOE.initCause(ioe);
			throw newIOE;
		}
	}

	/**
	 * Assign unique ids to leaf nodes.
	 * 
	 * @param graph
	 * @return the number of different leaf nodes
	 */
	private int setUniqueLeafNodeIds(DirectedGraph graph) {
		int i = 0;
		for (LeafNode l : graph.getLeafNodes()) {
			l.setUniqueLeafId(++i);
		}
		return i;
	}

	/**
	 * Assign unique ids to decision nodes.
	 * 
	 * @param graph
	 * @return the number of different decision nodes
	 */
	private int setUniqueDecisionNodeIds(DirectedGraph graph) {
		int i = 0;
		for (DecisionNode d : graph.getDecisionNodes()) {
			d.setUniqueDecisionNodeId(++i);
		}
		return i;
	}

	/**
	 * Assign unique ids to directed graph nodes.
	 * 
	 * @param graph
	 * @return the number of different directed graph nodes
	 */
	private int setUniqueDirectedGraphNodeIds(DirectedGraph graph) {
		int i = 0;
		for (DirectedGraphNode g : graph.getDirectedGraphNodes()) {
			g.setUniqueGraphNodeID(++i);
		}
		return i;
	}

	private void dumpBinary(DirectedGraph graph, DataOutput os) throws IOException {
		try {
			int numLeafNodes = setUniqueLeafNodeIds(graph);
			int numDecNodes = setUniqueDecisionNodeIds(graph);
			int numGraphNodes = setUniqueDirectedGraphNodeIds(graph);
			int maxNum = 1 << 30;
			if (numLeafNodes > maxNum || numDecNodes > maxNum || numGraphNodes > maxNum) {
				throw new UnsupportedOperationException("Cannot write more than " + maxNum + " nodes of one type in this format");
			}
			// write the number of decision nodes
			os.writeInt(numDecNodes);
			printDecisionNodes(graph, os, null);

			// write the number of leaves.
			os.writeInt(numLeafNodes);
			printLeafNodes(graph, os, null);

			// write the number of directed graph nodes
			os.writeInt(numGraphNodes);
			printDirectedGraphNodes(graph, os, null);

		} catch (IOException ioe) {
			IOException newIOE = new IOException("Error dumping CART to output stream");
			newIOE.initCause(ioe);
			throw newIOE;
		}
	}

	private void printDecisionNodes(DirectedGraph graph, DataOutput out, PrintWriter pw) throws IOException {
		for (DecisionNode decNode : graph.getDecisionNodes()) {
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
					if (daughter == null) {
						out.writeInt(0);
					} else if (daughter.isDecisionNode()) {
						int daughterID = ((DecisionNode) daughter).getUniqueDecisionNodeId();
						// Mark as decision node:
						daughterID |= DirectedGraphReader.DECISIONNODE << 30;
						out.writeInt(daughterID);
					} else if (daughter.isLeafNode()) {
						int daughterID = ((LeafNode) daughter).getUniqueLeafId();
						// Mark as leaf node:
						if (daughterID != 0)
							daughterID |= DirectedGraphReader.LEAFNODE << 30;
						out.writeInt(daughterID);
					} else if (daughter.isDirectedGraphNode()) {
						int daughterID = ((DirectedGraphNode) daughter).getUniqueGraphNodeID();
						// Mark as directed graph node:
						if (daughterID != 0)
							daughterID |= DirectedGraphReader.DIRECTEDGRAPHNODE << 30;
						out.writeInt(daughterID);
					}
				}
			}
			if (pw != null) {
				// dump to print writer
				StringBuilder strNode = new StringBuilder("-" + id + " " + nodeDefinition);
				for (int i = 0, n = decNode.getNumberOfDaugthers(); i < n; i++) {
					strNode.append(" ");
					Node daughter = decNode.getDaughter(i);
					if (daughter == null) {
						strNode.append("0");
					} else if (daughter.isDecisionNode()) {
						int daughterID = ((DecisionNode) daughter).getUniqueDecisionNodeId();
						strNode.append("-").append(daughterID);
						out.writeInt(daughterID);
					} else if (daughter.isLeafNode()) {
						int daughterID = ((LeafNode) daughter).getUniqueLeafId();
						if (daughterID == 0)
							strNode.append("0");
						else
							strNode.append("id").append(daughterID);
					} else if (daughter.isDirectedGraphNode()) {
						int daughterID = ((DirectedGraphNode) daughter).getUniqueGraphNodeID();
						if (daughterID == 0)
							strNode.append("0");
						else
							strNode.append("DGN").append(daughterID);
					}
				}
				pw.println(strNode.toString());
			}
		}
	}

	private void printLeafNodes(DirectedGraph graph, DataOutput out, PrintWriter pw) throws IOException {
		for (LeafNode leaf : graph.getLeafNodes()) {
			if (leaf.getUniqueLeafId() == 0) // empty leaf, do not write
				continue;
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

	private void printDirectedGraphNodes(DirectedGraph graph, DataOutput out, PrintWriter pw) throws IOException {
		for (DirectedGraphNode g : graph.getDirectedGraphNodes()) {
			int id = g.getUniqueGraphNodeID();
			if (id == 0)
				continue;// empty node, do not write
			Node leaf = g.getLeafNode();
			int leafID = 0;
			int leafNodeType = 0;
			if (leaf != null) {
				if (leaf instanceof LeafNode) {
					leafID = ((LeafNode) leaf).getUniqueLeafId();
					leafNodeType = DirectedGraphReader.LEAFNODE;
				} else if (leaf instanceof DirectedGraphNode) {
					leafID = ((DirectedGraphNode) leaf).getUniqueGraphNodeID();
					leafNodeType = DirectedGraphReader.DIRECTEDGRAPHNODE;
				} else {
					throw new IllegalArgumentException("Unexpected leaf type: " + leaf.getClass());
				}
			}
			DecisionNode d = g.getDecisionNode();
			int decID = d != null ? d.getUniqueDecisionNodeId() : 0;
			if (out != null) {
				int outLeafId = leafID == 0 ? 0 : leafID | (leafNodeType << 30);
				out.writeInt(outLeafId);
				int outDecId = decID == 0 ? 0 : decID | (DirectedGraphReader.DECISIONNODE << 30);
				out.writeInt(outDecId);
			}
			if (pw != null) {
				pw.print("DGN" + id);
				if (leafID == 0) {
					pw.print(" 0");
				} else if (leaf.isLeafNode()) {
					pw.print(" id" + leafID);
				} else {
					assert leaf.isDirectedGraphNode();
					pw.print(" DGN" + leafID);
				}
				if (decID == 0)
					pw.print(" 0");
				else
					pw.print(" -" + decID);
				pw.println();
			}
		}
	}
}
