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
public interface TwoDimensionalModel {
	/**
	 * Jointly set x and y values. If one of the values is beyond the respective Max and Min range, both values are ignored.
	 * 
	 * @param x
	 *            x
	 * @param y
	 *            y
	 */
	public void setXY(int x, int y);

	/**
	 * Set X value. If beyond the Max and Min range, value is ignored.
	 * 
	 * @param x
	 *            x
	 */
	public void setX(int x);

	/**
	 * Set Y value. If beyond the Max and Min range, value is ignored.
	 * 
	 * @param y
	 *            y
	 */
	public void setY(int y);

	/**
	 * Set the Maximum X value.
	 * 
	 * @param maxX
	 *            maxX
	 */
	public void setMaxX(int maxX);

	/**
	 * Set the Minimum X value.
	 * 
	 * @param minX
	 *            minX
	 */
	public void setMinX(int minX);

	/**
	 * Set the Maximum Y value.
	 * 
	 * @param maxY
	 *            maxY
	 */
	public void setMaxY(int maxY);

	/**
	 * Set the Minimum Y value.
	 * 
	 * @param minY
	 *            minY
	 */
	public void setMinY(int minY);

	/**
	 * Get the X value.
	 * 
	 * @return x
	 */
	public int getX();

	/**
	 * Get the Y value.
	 * 
	 * @return Y
	 */
	public int getY();

	public int getMaxX();

	public int getMinX();

	public int getMaxY();

	public int getMinY();

	/**
	 * Adds a ChangeListener to the model's listener list.
	 * 
	 * @param l
	 *            l
	 */
	public void addChangeListener(javax.swing.event.ChangeListener l);

	/**
	 * Removes a ChangeListener from the model's listener list.
	 * 
	 * @param l
	 *            l
	 */
	public void removeChangeListener(javax.swing.event.ChangeListener l);
}
