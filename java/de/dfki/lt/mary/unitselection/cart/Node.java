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
package de.dfki.lt.mary.unitselection.cart;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * A node for the CART. All node types inherit from this class
 */
public abstract class Node {

    // isRoot should be set to true if this node is the root node
    protected boolean isRoot;

    // every node except the root node has a mother
    protected Node mother;

    // the index of the node in the daughters array of its mother
    protected int nodeIndex;

    
    /**
     * set the mother node of this node
     * 
     * @param node
     *            the mother node
     */
    public void setMother(Node node) {
        this.mother = node;
    }

    /**
     * Get the mother node of this node
     * 
     * @return the mother node
     */
    public Node getMother() {
        return mother;
    }

    /**
     * Set isRoot to the given value
     * 
     * @param isRoot
     *            the new value of isRoot
     */
    public void setIsRoot(boolean isRoot) {
        this.isRoot = isRoot;
    }

    /**
     * Get the setting of isRoot
     * 
     * @return the setting of isRoot
     */
    public boolean isRoot() {
        return isRoot;
    }

    /**
     * Set the index of this node
     * 
     * @param index
     *            the index
     */
    public void setNodeIndex(int index) {
        this.nodeIndex = index;
    }

    /**
     * Get the index of this node
     * 
     * @return the index
     */
    public int getNodeIndex() {
        return nodeIndex;
    }
    
    public Node getRootNode()
    {
        if (isRoot) {
            assert mother == null;
            return this;
        } else {
            assert mother != null : " I am not root but I have no mother :-(";
            return mother.getRootNode();
        }
    }
    
    /**
     * Count all the nodes at and below this node.
     * A leaf will return 1; the root node will 
     * report the total number of decision and leaf nodes
     * in the tree.
     * @return
     */
    public abstract int getNumberOfNodes();
    
    /**
     * Count all the data available at and below this node.
     * The meaning of this depends on the type of nodes; for example,
     * when IntArrayLeafNodes are used, it is the total number of ints
     * that are saved in all leaf nodes below the current node.
     * @return an int counting the data below the current node, or -1
     * if such a concept is not meaningful.
     */
    public abstract int getNumberOfData();

    /**
     * Get all the data at or below this node.
     * The type of data returned depends on the type of nodes; for example,
     * when IntArrayLeafNodes are used, one int[] is returned which contains all
     * int values in all leaf nodes below the current node.
     * @return an object containing all data below the current node, or null
     * if such a concept is not meaningful.
     */
    public abstract Object getAllData();

    /**
     * Write this node's data into the target object at pos,
     * making sure that exactly len data are written.
     * The type of data written depends on the type of nodes; for example,
     * when IntArrayLeafNodes are used, target would be an int[].
     * @param array the object to write to, usually an array.
     * @param pos the position in the target at which to start writing
     * @param len the amount of data items to write, usually equals
     * getNumberOfData().
     */
    protected abstract void fillData(Object target, int pos, int len);
    
    /**
     * Writes the Cart to the given DataOut in Wagon Format
     * 
     * @param out
     *            the outputStream
     * @param extension
     *            the extension that is added to the last daughter
     */
    public abstract void toWagonFormat(DataOutputStream out,
            String extension, PrintWriter pw) throws IOException;
    
    public String toString(String prefix){
        return prefix + this.toString();
    }

}