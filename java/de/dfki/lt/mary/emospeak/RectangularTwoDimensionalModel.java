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
public class RectangularTwoDimensionalModel implements TwoDimensionalModel {

    private int x;
    private int y;
    private int minX;
    private int maxX;
    private int minY;
    private int maxY;
    private java.util.List changeListeners = new java.util.ArrayList();
    
    /** Creates new RectangularTwoDimensionalModel, with all values
     * set to 0. */
    public RectangularTwoDimensionalModel() {
        this(0,0,0,0,0,0);
    }

    /** Creates new RectangularTwoDimensionalModel */
    public RectangularTwoDimensionalModel(int x, int y, int minX, int maxX, int minY, int maxY) {
        this.x = x;
        this.y = y;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    /** Adds a ChangeListener to the model's listener list.  */
    public void addChangeListener(javax.swing.event.ChangeListener l) {
        if (!changeListeners.contains(l))
            changeListeners.add(l);
    }
    
    /** Removes a ChangeListener from the model's listener list.  */
    public void removeChangeListener(javax.swing.event.ChangeListener l) {
        changeListeners.remove(l);
    }

    private void notifyChangeListeners() {
        java.util.Iterator it = changeListeners.iterator();
        javax.swing.event.ChangeEvent e = new javax.swing.event.ChangeEvent(this);
        while (it.hasNext()) {
            javax.swing.event.ChangeListener l = (javax.swing.event.ChangeListener) it.next();
            l.stateChanged(e);
        }
    }
    
    /** Set the Maximum X value.  */
    public void setMaxX(int maxX) {
        this.maxX = maxX;
    }
    
    /** Set the Maximum Y value.  */
    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }
    
    /** Set the Minimum X value.  */
    public void setMinX(int minX) {
        this.minX = minX;
    }
    
    /** Set the Minimum Y value.  */
    public void setMinY(int minY) {
        this.minY = minY;
    }
    
    /** Set X value.
     * If beyond the Max and Min range, value is ignored.
     */
    public void setX(int x) {
        if (minX <= x && x <= maxX) {
            this.x = x;
            notifyChangeListeners();
        }
    }
    
    /** Jointly set x and y values.
     * If one of the values is beyond the
     * respective Max and Min range, both values are ignored.
     */
    public void setXY(int x, int y) {
        if (minX <= x && x <= maxX &&
            minY <= y && y <= maxY) {
            this.x = x;
            this.y = y;
            notifyChangeListeners();
        }
    }
    
    /** Set Y value.
     * If beyond the Max and Min range, value is ignored.
     */
    public void setY(int y) {
        if (minY <= y && y <= maxY) {
            this.y = y;
            notifyChangeListeners();
        }
    }
    
    /** Get the X value. */
    public int getX() { return x; }
    /** Get the Y value. */
    public int getY() { return y; }
    public int getMaxX() { return maxX; }
    public int getMinX() { return minX; }
    public int getMaxY() { return maxY; }
    public int getMinY() { return minY; }
}
