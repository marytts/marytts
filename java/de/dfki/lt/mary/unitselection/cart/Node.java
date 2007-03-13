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
    
    public abstract int getNumberOfCandidates();

    /**
     * Get all unit indices from all leaves below this node
     * 
     * @return an int array containing the indices
     */
    public abstract int[] getAllIndices();

    /**
     * Write this node's indices into the given array at pos,
     * making sure that exactly len indices are written.
     * @param array the array to write to
     * @param pos the position in the array at which to start writing
     * @param len the amount of indices to write, usually equals
     * getNumberOfCandidates().
     */
    protected abstract void fillIndexArray(int[] array, int pos, int len);
    
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

}