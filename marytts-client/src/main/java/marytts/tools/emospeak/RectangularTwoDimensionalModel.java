/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.tools.emospeak;

/**
 * 
 * @author Marc Schr&ouml;der
 */
public class RectangularTwoDimensionalModel implements TwoDimensionalModel {

	private int x;
	private int y;
	private int minX;
	private int maxX;
	private int minY;
	private int maxY;
	private java.util.List changeListeners = new java.util.ArrayList();

	/**
	 * Creates new RectangularTwoDimensionalModel, with all values set to 0.
	 */
	public RectangularTwoDimensionalModel() {
		this(0, 0, 0, 0, 0, 0);
	}

	/**
	 * Creates new RectangularTwoDimensionalModel
	 * 
	 * @param x
	 *            x
	 * @param y
	 *            y
	 * @param minX
	 *            minX
	 * @param maxX
	 *            maxX
	 * @param minY
	 *            minY
	 * @param maxY
	 *            maxY
	 */
	public RectangularTwoDimensionalModel(int x, int y, int minX, int maxX, int minY, int maxY) {
		this.x = x;
		this.y = y;
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
	}

	/**
	 * Adds a ChangeListener to the model's listener list.
	 * 
	 * @param l
	 *            l
	 */
	public void addChangeListener(javax.swing.event.ChangeListener l) {
		if (!changeListeners.contains(l))
			changeListeners.add(l);
	}

	/**
	 * Removes a ChangeListener from the model's listener list.
	 * 
	 * @param l
	 *            l
	 */
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

	/**
	 * Set the Maximum X value.
	 * 
	 * @param maxX
	 *            maxX
	 */
	public void setMaxX(int maxX) {
		this.maxX = maxX;
	}

	/**
	 * Set the Maximum Y value.
	 * 
	 * @param maxY
	 *            maxY
	 */
	public void setMaxY(int maxY) {
		this.maxY = maxY;
	}

	/**
	 * Set the Minimum X value.
	 * 
	 * @param minX
	 *            minX
	 */
	public void setMinX(int minX) {
		this.minX = minX;
	}

	/**
	 * Set the Minimum Y value.
	 * 
	 * @param minY
	 *            minY
	 */
	public void setMinY(int minY) {
		this.minY = minY;
	}

	/**
	 * Set X value. If beyond the Max and Min range, value is ignored.
	 * 
	 * @param x
	 *            x
	 */
	public void setX(int x) {
		if (minX <= x && x <= maxX) {
			this.x = x;
			notifyChangeListeners();
		}
	}

	/**
	 * Jointly set x and y values. If one of the values is beyond the respective Max and Min range, both values are ignored.
	 * 
	 * @param x
	 *            x
	 * @param y
	 *            y
	 */
	public void setXY(int x, int y) {
		if (minX <= x && x <= maxX && minY <= y && y <= maxY) {
			this.x = x;
			this.y = y;
			notifyChangeListeners();
		}
	}

	/**
	 * Set Y value. If beyond the Max and Min range, value is ignored.
	 * 
	 * @param y
	 *            y
	 */
	public void setY(int y) {
		if (minY <= y && y <= maxY) {
			this.y = y;
			notifyChangeListeners();
		}
	}

	/**
	 * Get the X value.
	 * 
	 * @return x
	 */
	public int getX() {
		return x;
	}

	/**
	 * Get the Y value.
	 * 
	 * @return y
	 */
	public int getY() {
		return y;
	}

	public int getMaxX() {
		return maxX;
	}

	public int getMinX() {
		return minX;
	}

	public int getMaxY() {
		return maxY;
	}

	public int getMinY() {
		return minY;
	}
}
