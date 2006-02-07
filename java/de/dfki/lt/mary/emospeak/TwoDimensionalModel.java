/**
 * Copyright 2000-2006 DFKI GmbH.
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
package de.dfki.lt.mary.emospeak;

/**
 *
 * @author  Marc Schr&ouml;der
 */
public interface TwoDimensionalModel {
    /** Jointly set x and y values.
     * If one of the values is beyond the
     * respective Max and Min range, both values are ignored.
     */
    public void setXY(int x, int y);
    /** Set X value.
     * If beyond the Max and Min range, value is ignored.
     */
    public void setX(int x);
    /** Set Y value.
     * If beyond the Max and Min range, value is ignored.
     */
    public void setY(int y);
    /** Set the Maximum X value. */
    public void setMaxX(int maxX);
    /** Set the Minimum X value. */
    public void setMinX(int minX);
    /** Set the Maximum Y value. */
    public void setMaxY(int maxY);
    /** Set the Minimum Y value. */
    public void setMinY(int minY);
    
    /** Get the X value. */
    public int getX();
    /** Get the Y value. */
    public int getY();
    public int getMaxX();
    public int getMinX();
    public int getMaxY();
    public int getMinY();
    
    /** Adds a ChangeListener to the model's listener list. */
    public void addChangeListener(javax.swing.event.ChangeListener l);
    /** Removes a ChangeListener from the model's listener list. */
    public void removeChangeListener(javax.swing.event.ChangeListener l);
}

