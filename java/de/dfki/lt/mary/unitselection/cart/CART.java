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

import java.io.DataOutput;
import java.io.IOException;

import de.dfki.lt.mary.unitselection.Target;

/**
 * Generic interface for Classification and Regression Trees (CARTs) based
 * on the Breiman, Friedman, Olshen, and Stone document "Classification and
 * Regression Trees."  Wadsworth, Belmont, CA, 1984.
 */
public interface CART
{
    /**
     * Load the cart from the given file
     * @param fileName the file to load the cart from
     * @throws IOException if a problem occurs while loading
     */
    public void load(String fileName) throws IOException;
    
    /**
     * Passes the given item through this CART and returns the
     * interpretation.
     *
     * @param target the target to analyze
     *
     * @return the interpretation
     */
    public Object interpret(Target target);
    

    /**
     * Dumps this CART to the output
     *
     * @param os the DataOutputStream or RandomAccessFile to write to.
     *
     * @throws IOException if an error occurs during output
     */
    public void dumpBinary(DataOutput os) throws IOException ;
    
}


  
