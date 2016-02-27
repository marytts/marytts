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
package marytts.signalproc.filter;

import marytts.signalproc.process.InlineDataProcessor;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;

/**
 * @author Marc Schr&ouml;der A purely recursive filter, following the equation:
 *         <code>x[n] = e[n] + a[1]*x[n-1] + a[2]*x[n-2] + ... + a[p]*x[n-p]</code>
 */
public class RecursiveFilter {
	/**
	 * The prediction coefficients, as in * <code>x[n] = e[n] + a[1]*x[n-1] + a[2]*x[n-2] + ... + a[p]*x[n-p]</code>
	 */
	protected final double[] a;

	/**
	 * Create a new recursive filter.
	 * 
	 * @param a
	 *            the recursive prediction coefficients
	 */
	public RecursiveFilter(double[] a) {
		this.a = a;
	}

	/**
	 * Apply this filter to the given input signal. The input signal is filtered piece by piece, as it is read from the data
	 * source returned by this method. This is the recommended way to filter longer signals.
	 * 
	 * @param signal
	 *            the signal to which this filter should be applied
	 * @return a DoubleDataSource from which the data can be read.
	 */
	public DoubleDataSource apply(DoubleDataSource signal) {
		return new BufferedDoubleDataSource(signal, new Processor(a));
	}

	/**
	 * Apply this filter to the given input signal. This method filters the entire signal, and returns the entire filtered signal.
	 * For long signals, it is better to use apply(DoubleDataSource).
	 * 
	 * @param signal
	 *            the signal to which this filter should be applied
	 * @return the filtered signal.
	 */
	public double[] apply(double[] signal) {
		return new BufferedDoubleDataSource(signal, new Processor(a)).getAllData();
	}

	public static class Processor implements InlineDataProcessor {
		/**
		 * The prediction coefficiednts, as in * <code>x[n] = e[n] + a[1]*x[n-1] + a[2]*x[n-2] + ... + a[p]*x[n-p]</code>
		 */
		protected final double[] a;
		/**
		 * The prediction order, i.e. the length of a.
		 */
		protected final int p;

		/**
		 * A memory of <code>x[n-1]...x[n-p]</code>, when reading data in chunks.
		 */
		protected double[] memory;

		public Processor(double[] a) {
			this.a = a;
			this.p = a.length;
			this.memory = new double[p];
		}

		/**
		 * Perform recursive filter processing on the data.
		 * 
		 * @param data
		 *            data to filter, e.g. a residual
		 * @param off
		 *            position in data to start processing
		 * @param len
		 *            number of sample points to process
		 */
		public void applyInline(double[] data, int off, int len) {
			if (off < 0 || len <= 0 || off + len > data.length)
				throw new IllegalArgumentException("off or len out of bounds");
			for (int n = 0; n < len; n++) {
				int offn = off + n;
				for (int i = 1; i <= p; i++) {
					if (n < i) // n-i<0, i.e. we need to look to the left of the current data chunk
						data[offn] += a[i - 1] * memory[p + n - i]; // don't be fooled -- this is a[i]*x[n-i]
					else
						data[offn] += a[i - 1] * data[offn - i]; // don't be fooled -- this is a[i]*x[n-i]
				}
			}
			// Remember last p points in memory
			if (len < p) { // "Pathological" case: read less than p samples
				// Can only take len new samples into memory
				System.arraycopy(memory, len, memory, 0, p - len);
				System.arraycopy(data, off, memory, p - len, len);
			} else { // Normal processing: read at least p samples
				// Copy last p samples into memory
				System.arraycopy(data, off + len - p, memory, 0, p);
			}
		}
	}
}
