/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.cart.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import marytts.cart.CART;
import marytts.cart.DecisionNode;
import marytts.cart.LeafNode;
import marytts.cart.Node;
import marytts.cart.LeafNode.IntAndFloatArrayLeafNode;
import marytts.features.FeatureDefinition;
import marytts.tools.voiceimport.MaryHeader;

/**
 * IO functions for CARTs in MaryCART format
 * 
 * @author Marcela Charfuelan
 */
public class MaryCARTReader
{
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
    public CART load(String fileName)
    throws IOException
    {
        // open the CART-File and read the header
        DataInput raf = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
        
        MaryHeader maryHeader = new MaryHeader(raf);
        if (!maryHeader.hasLegalMagic()) {
            throw new IOException("No MARY database file!");
        }
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
        int numDecNodes = raf.readInt();  // number of decision nodes

        // First we need to read all nodes into memory, then we can link them properly
        // in terms of parent/child.
        DecisionNode[] dns = new DecisionNode[numDecNodes];
        int[][] childIndexes = new int[numDecNodes][];
        for (int i=0; i<numDecNodes; i++) {
            // read one decision node
            int featureNameIndex = raf.readInt();
            int nodeTypeNr = raf.readInt();
            DecisionNode.Type nodeType = DecisionNode.Type.values()[nodeTypeNr];
            int numChildren = 2; // for binary nodes
            switch (nodeType) {
            case BinaryByteDecisionNode:
                int criterion = raf.readInt();
                dns[i] = new DecisionNode.BinaryByteDecisionNode(featureNameIndex, (byte)criterion, featureDefinition);
                break;
            case BinaryShortDecisionNode:
                criterion = raf.readInt();
                dns[i] = new DecisionNode.BinaryShortDecisionNode(featureNameIndex, (short)criterion, featureDefinition);
                break;
            case BinaryFloatDecisionNode:
                float floatCriterion = raf.readFloat();
                dns[i] = new DecisionNode.BinaryFloatDecisionNode(featureNameIndex, floatCriterion, featureDefinition);
                break;
            case ByteDecisionNode:
                numChildren = raf.readInt();
                if (featureDefinition.getNumberOfValues(featureNameIndex) != numChildren) {
                    throw new IOException("Inconsistent cart file: feature "+
                            featureDefinition.getFeatureName(featureNameIndex)+" should have "+
                            featureDefinition.getNumberOfValues(featureNameIndex)+
                            " values, but decision node "+i+" has only "+numChildren+" child nodes");
                }
                dns[i] = new DecisionNode.ByteDecisionNode(featureNameIndex, numChildren, featureDefinition);
                break;
            case ShortDecisionNode:
                numChildren = raf.readInt();
                if (featureDefinition.getNumberOfValues(featureNameIndex) != numChildren) {
                    throw new IOException("Inconsistent cart file: feature "+
                            featureDefinition.getFeatureName(featureNameIndex)+" should have "+
                            featureDefinition.getNumberOfValues(featureNameIndex)+
                            " values, but decision node "+i+" has only "+numChildren+" child nodes");
                }
                dns[i] = new DecisionNode.ShortDecisionNode(featureNameIndex, numChildren, featureDefinition);
            }
            // now read the children, indexes only:
            childIndexes[i] = new int[numChildren];
            for (int k=0; k<numChildren; k++) {
                childIndexes[i][k] = raf.readInt();
            }
        }
        
        // read the leaves
        int numLeafNodes = raf.readInt(); // number of leaves, it does not include empty leaves
        LeafNode[] lns = new LeafNode[numLeafNodes];
        
        for (int j=0; j<numLeafNodes; j++) {
            // read one leaf node
            int leafTypeNr = raf.readInt();
            LeafNode.LeafType leafNodeType = LeafNode.LeafType.values()[leafTypeNr];
            switch (leafNodeType) {
            case IntArrayLeafNode:
                int numData = raf.readInt();
                int[] data = new int[numData];
                for (int d=0; d<numData; d++) {
                    data[d] = raf.readInt();
                }
                lns[j] = new LeafNode.IntArrayLeafNode(data);
                break;
            case FloatLeafNode:
                float stddev = raf.readFloat();
                float mean = raf.readFloat();
                lns[j] = new LeafNode.FloatLeafNode(new float[] {stddev, mean});
                break;
            case IntAndFloatArrayLeafNode:
            case StringAndFloatLeafNode:
                int numPairs = raf.readInt();
                int[] ints = new int[numPairs];
                float[] floats = new float[numPairs];
                for (int d=0; d<numPairs; d++) {
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
        for (int i=0; i<numDecNodes; i++) {
            for (int k=0; k<childIndexes[i].length; k++) {
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
            ((DecisionNode)rootNode).countData();
        } else if (lns.length > 0) {
            rootNode = lns[0]; // single-leaf tree...
        } else {
            rootNode = null;
        }

        // set the rootNode as the rootNode of cart
        return new CART(rootNode, featureDefinition, props);
    }

    
}
