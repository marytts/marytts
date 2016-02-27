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

package marytts.signalproc.sinusoidal.hntm.analysis;

/**
 * A HNM signal with its left and right context
 * 
 * @author oytun.turk
 * 
 */
public class HntmSpeechSignalWithContext {
	public HntmSpeechSignal hntmSignal;
	public HntmSpeechFrame[] leftContexts;
	public HntmSpeechFrame[] rightContexts;

	public HntmSpeechSignalWithContext() {
		hntmSignal = null;
		leftContexts = null;
		rightContexts = null;
	}

	public HntmSpeechSignalWithContext(HntmSpeechSignal hntmSignalIn, HntmSpeechFrame[] leftContextsIn,
			HntmSpeechFrame[] rightContextsIn) {
		this();

		if (hntmSignalIn != null)
			hntmSignal = new HntmSpeechSignal(hntmSignalIn);

		if (leftContextsIn != null && leftContextsIn.length > 0) {
			leftContexts = new HntmSpeechFrame[leftContextsIn.length];
			for (int i = 0; i < leftContextsIn.length; i++)
				leftContexts[i] = new HntmSpeechFrame(leftContextsIn[i]);
		}

		if (rightContextsIn != null && rightContextsIn.length > 0) {
			rightContexts = new HntmSpeechFrame[rightContextsIn.length];
			for (int i = 0; i < rightContextsIn.length; i++)
				rightContexts[i] = new HntmSpeechFrame(rightContextsIn[i]);
		}
	}
}
