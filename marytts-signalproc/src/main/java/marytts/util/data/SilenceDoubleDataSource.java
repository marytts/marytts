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

import java.util.Arrays;

public class SilenceDoubleDataSource extends BaseDoubleDataSource {
	protected long n;

	/**
	 * Construct an double data source from which a given amount of silence can be read.
	 * 
	 * @param n
	 *            the number of silence samples to be read
	 */
	public SilenceDoubleDataSource(long n) {
		super();
		this.n = n;
		dataLength = n;
	}

	public boolean hasMoreData() {
		return n > 0;
	}

	/**
	 * The number of doubles that can currently be read from this double data source without blocking. This number can change over
	 * time.
	 * 
	 * @return the number of doubles that can currently be read without blocking
	 */
	public int available() {
		return (int) n;
	}

	public int getData(double[] target, int targetPos, int length) {
		if (target.length - targetPos < length) {
			throw new IllegalArgumentException("Target array cannot hold enough data (" + (target.length - targetPos)
					+ " left, but " + length + " requested)");
		}
		int toCopy = (int) Math.min(length, n);
		Arrays.fill(target, targetPos, targetPos + toCopy, 0.);
		n -= toCopy;
		return toCopy;
	}

}
