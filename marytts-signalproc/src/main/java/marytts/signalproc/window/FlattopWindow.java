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

package marytts.signalproc.window;

/**
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class FlattopWindow extends Window {

	public FlattopWindow(int length) {
		super(length);
	}

	public FlattopWindow(int length, double prescalingFactor) {
		super(length, prescalingFactor);
	}

	protected void initialise() {
		boolean prescale = (prescalingFactor != 1.);

		for (int i = 0; i < window.length; i++) {
			window[i] = 1.0 - 1.93 * Math.cos((2 * Math.PI * i) / (window.length - 1)) + 1.29
					* Math.cos((4 * Math.PI * i) / (window.length - 1)) - 0.388
					* Math.cos((6 * Math.PI * i) / (window.length - 1)) + 0.032
					* Math.cos((8 * Math.PI * i) / (window.length - 1));
			if (prescale)
				window[i] *= prescalingFactor;
		}
	}

	public String toString() {
		return "Bartlett window";
	}

}
