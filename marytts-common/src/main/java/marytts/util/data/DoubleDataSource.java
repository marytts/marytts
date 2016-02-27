/**
 * Copyright 2004-2006 DFKI GmbH.
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
package marytts.util.data;

/**
 * @author Marc Schr&ouml;der
 * 
 */
public interface DoubleDataSource {
	public int NOT_SPECIFIED = -1;

	/**
	 * Request to get the specified amount of data in a new double array.
	 * 
	 * @param amount
	 *            the number of doubles requested
	 * @return a new double array; its length may be smaller than amount if not enough data could be obtained. If no data could be
	 *         read, null is returned.
	 */
	public double[] getData(int amount);

	/**
	 * Try to get as many double data from this DoubleDataSource as target can hold.
	 * 
	 * @param target
	 *            the double array in which to write the data
	 * @return the number of data items written into target. If the returned value is less than target.length, only that many data
	 *         items have been copied into target; further calls will return 0 and not copy anything.
	 */
	public int getData(double[] target);

	/**
	 * Try to get length doubles from this DoubleDataSource, and copy them into target, starting from targetPos.
	 * 
	 * @param target
	 *            the double array to write into
	 * @param targetPos
	 *            position in target where to start writing
	 * @param length
	 *            the amount of data requested
	 * @return the amount of data actually delivered. If the returned value is less than length, only that many data items have
	 *         been copied into target; further calls will return 0 and not copy anything.
	 */
	public int getData(double[] target, int targetPos, int length);

	/**
	 * Whether or not any more data can be read from this data source.
	 * 
	 * @return true if another call to getData() will return data, false otherwise.
	 */
	public boolean hasMoreData();

	/**
	 * The number of doubles that can currently be read from this double data source without blocking. This number can change over
	 * time.
	 * 
	 * @return the number of doubles that can currently be read without blocking
	 */
	public int available();

	/**
	 * Get all the data that can be read from this data source, in a single double array.
	 * 
	 * @throws OutOfMemoryError
	 *             if a sufficiently large double array cannot be created.
	 * @return a double array of exactly the length required to contain all the data that can be read from this source. Returns an
	 *         array of length 0 if no data can be read from this source.
	 */
	public double[] getAllData();

	/**
	 * Get the total length of the data in this data source, if available.
	 * 
	 * @return the number of doubles that can be read from this data source, or DoubleDataSource.NOT_SPECIFIED if unknown.
	 */
	public long getDataLength();

}
