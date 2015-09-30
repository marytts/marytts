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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Create one DoubleDataSource from a sequence of DoubleDataSources.
 * 
 * @author Marc Schr&ouml;der
 */
public class SequenceDoubleDataSource extends BaseDoubleDataSource {
	protected LinkedList sources;

	/**
	 * 
	 * @param inputSources
	 *            input Sources
	 */
	public SequenceDoubleDataSource(DoubleDataSource[] inputSources) {
		super();
		sources = new LinkedList();
		dataLength = 0;
		for (int i = 0; i < inputSources.length; i++) {
			if (dataLength != DoubleDataSource.NOT_SPECIFIED) {
				long dl = inputSources[i].getDataLength();
				if (dl == DoubleDataSource.NOT_SPECIFIED)
					dataLength = DoubleDataSource.NOT_SPECIFIED;
				else
					dataLength += dl;
			}
			if (inputSources[i] instanceof BlockwiseDoubleDataSource) {
				sources.add(new BufferedDoubleDataSource(inputSources[i]));
			} else {
				sources.add(inputSources[i]);
			}
		}
	}

	/**
	 * 
	 * @param inputSources
	 *            a list of DoubleDataSource objects.
	 */
	public SequenceDoubleDataSource(List inputSources) {
		this((DoubleDataSource[]) inputSources.toArray(new DoubleDataSource[0]));
	}

	public boolean hasMoreData() {
		while (!sources.isEmpty() && !((DoubleDataSource) sources.getFirst()).hasMoreData()) {
			sources.removeFirst();
		}
		if (sources.isEmpty())
			return false;
		return true;
	}

	/**
	 * The number of doubles that can currently be read from this double data source without blocking. This number can change over
	 * time.
	 * 
	 * @return the number of doubles that can currently be read without blocking
	 */
	public int available() {
		int available = 0;
		for (Iterator it = sources.iterator(); it.hasNext();) {
			available += ((DoubleDataSource) it.next()).available();
		}
		return available;
	}

	public int getData(double[] target, int targetPos, int length) {
		if (target.length - targetPos < length) {
			throw new IllegalArgumentException("Target array cannot hold enough data (" + (target.length - targetPos)
					+ " left, but " + length + " requested)");
		}
		int copied = 0;
		while (!sources.isEmpty() && copied < length) {
			DoubleDataSource source = (DoubleDataSource) sources.getFirst();
			int read = source.getData(target, targetPos + copied, length - copied);
			// System.err.println("Read " + read + " samples from " + source.getClass().toString());
			if (read < length - copied) {
				assert !source.hasMoreData();
				sources.removeFirst();
			}
			copied += read;
		}
		return copied;
	}
}
