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

package marytts.signalproc.sinusoidal.hntm.synthesis;

import java.util.Arrays;

import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmPlusTransientsSpeechSignal;
import marytts.signalproc.window.HammingWindow;
import marytts.signalproc.window.Window;
import marytts.util.signal.SignalProcUtils;

/**
 * Syntehsizer for the transient part waveform segments.
 * 
 * @author oytun.turk
 * 
 */
public class TransientPartSynthesizer {

	public static double[] synthesize(HntmPlusTransientsSpeechSignal hnmSignal, HntmAnalyzerParams analysisParams) {
		int outputLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);

		double[] transientPart = new double[outputLen];
		Arrays.fill(transientPart, 0.0);

		if (hnmSignal.transients != null) {
			int i, j;
			int startInd, endInd;
			int windowLeftEndInd, windowRightStartInd;
			int ws = SignalProcUtils.time2sample(2 * analysisParams.overlapBetweenTransientAndNontransientSectionsInSeconds,
					hnmSignal.samplingRateInHz);
			if (ws % 2 == 0)
				ws++;
			Window win = new HammingWindow(ws);
			win.normalizePeakValue(1.0f);
			int winMidInd = (ws - 1) / 2;
			for (i = 0; i < hnmSignal.transients.segments.length; i++) {
				if (hnmSignal.transients.segments[i] != null && hnmSignal.transients.segments[i].waveform != null
						&& hnmSignal.transients.segments[i].waveform.length > 0
						&& hnmSignal.transients.segments[i].startTime >= 0.0f) {
					startInd = Math.min(
							SignalProcUtils.time2sample(hnmSignal.transients.segments[i].startTime, hnmSignal.samplingRateInHz),
							outputLen - 1);
					windowLeftEndInd = Math.min(startInd + winMidInd, outputLen - 1);
					endInd = Math.min(
							SignalProcUtils.time2sample(hnmSignal.transients.segments[i].startTime, hnmSignal.samplingRateInHz)
									+ hnmSignal.transients.segments[i].waveform.length - 1, outputLen - 1);
					windowRightStartInd = endInd - winMidInd;

					for (j = startInd; j <= windowLeftEndInd; j++)
						transientPart[j] = hnmSignal.transients.segments[i].waveform[j - startInd] * win.value(j - startInd);
					for (j = windowLeftEndInd + 1; j < windowRightStartInd; j++)
						transientPart[j] = hnmSignal.transients.segments[i].waveform[j - startInd];
					for (j = windowRightStartInd; j <= endInd; j++)
						transientPart[j] = hnmSignal.transients.segments[i].waveform[j - startInd]
								* win.value((j - endInd) + ws - 1);
				}
			}
		}

		return transientPart;
	}
}
