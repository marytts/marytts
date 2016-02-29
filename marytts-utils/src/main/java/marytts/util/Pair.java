/**
 * Copyright 2007 DFKI GmbH.
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

package marytts.util;

/**
 * @author marc
 *
 */
public class Pair<X, Y> {
	private final X first;
	private final Y second;
	private int hash;
	private boolean haveComputedHash = false;

	public Pair(X first, Y second) {
		this.first = first;
		this.second = second;
	}

	public X getFirst() {
		return first;
	}

	public Y getSecond() {
		return second;
	}

	@Override
	public int hashCode() {
		if (!haveComputedHash) {
			hash = (first == null ? 0 : first.hashCode() * 31) + (second == null ? 0 : second.hashCode());
			haveComputedHash = true;
		}
		return hash;
	}

	@Override
	public boolean equals(Object oth) {
		if (this == oth) {
			return true;
		}

		if (oth == null || !(getClass().isInstance(oth))) {
			return false;
		}
		Pair other = (Pair) getClass().cast(oth);
		return (first == null ? other.first == null : first.equals(other.first))
				&& (second == null ? other.second == null : second.equals(other.second));
	}
}
