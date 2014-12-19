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

import marytts.signalproc.sinusoidal.hntm.analysis.FrameNoisePartWaveform;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechFrame;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.window.HammingWindow;
import marytts.signalproc.window.Window;
import marytts.util.math.ArrayUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * Synthesizes noise part waveform from non-overlapping chunks of data. This model is the most natural one since it involves no
 * noise models.
 * 
 * @author oytun.turk
 * 
 */
public class NoisePartWaveformSynthesizer {
	// TO DO: This should use overlap add since the noise waveform will not be a continuous waveform in TTS
	public static double[] synthesize(HntmSpeechSignal hnmSignal, HntmSpeechFrame[] leftContexts,
			HntmSpeechFrame[] rightContexts, HntmAnalyzerParams analysisParams) {
		double[] noisePartWaveform = null;

		if (hnmSignal != null && hnmSignal.frames != null) {
			int outputLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);

			noisePartWaveform = new double[outputLen];
			double[] wgts = new double[outputLen];
			Arrays.fill(noisePartWaveform, 0.0);
			Arrays.fill(wgts, 0.0);
			int i;

			HntmSpeechFrame prevFrame, nextFrame, currentLeftContext, currentRightContext;

			// TO DO: Overlap waveform noise case! See analysis code!
			for (i = 0; i < hnmSignal.frames.length; i++) {
				if (i > 0)
					prevFrame = hnmSignal.frames[i - 1];
				else
					prevFrame = null;

				if (i < hnmSignal.frames.length - 1)
					nextFrame = hnmSignal.frames[i + 1];
				else
					nextFrame = null;

				boolean isFirstSynthesisFrame = false;
				if (i == 0)
					isFirstSynthesisFrame = true;

				boolean isLastSynthesisFrame = false;
				if (i == hnmSignal.frames.length - 1)
					isLastSynthesisFrame = true;

				boolean existsLeftContexts = true;
				if (leftContexts == null) // Take the previous frame parameters as left context (i.e. the HNM signal is a
											// continuous one, not concatenated one
					existsLeftContexts = false;

				boolean existsRightContexts = true;
				if (rightContexts == null) // Take the previous frame parameters as right context (i.e. the HNM signal is a
											// continuous one, not concatenated one
					existsRightContexts = false;

				currentLeftContext = null;
				if (leftContexts != null)
					currentLeftContext = leftContexts[i];

				currentRightContext = null;
				if (rightContexts != null)
					currentRightContext = rightContexts[i];

				processFrame(prevFrame, hnmSignal.frames[i], nextFrame, hnmSignal.samplingRateInHz, isFirstSynthesisFrame,
						isLastSynthesisFrame, noisePartWaveform, wgts, existsLeftContexts, currentLeftContext,
						existsRightContexts, currentRightContext);
			}

			for (i = 0; i < outputLen; i++) {
				if (wgts[i] > 1.0e-10)
					noisePartWaveform[i] /= wgts[i];
			}
		}

		return noisePartWaveform;
	}

	public static void processFrame(HntmSpeechFrame prevFrame, HntmSpeechFrame currentFrame, HntmSpeechFrame nextFrame,
			int samplingRateInHz, boolean isFirstSynthesisFrame, boolean isLastSynthesisFrame, double[] noisePartWaveform,
			double[] wgts, boolean existsLeftContexts, HntmSpeechFrame currentLeftContext, boolean existsRightContexts,
			HntmSpeechFrame currentRightContext) {
		double[] frameWaveform = null;
		int waveformNoiseStartInd;
		int j;

		double[] leftContextWaveform = null;
		double[] rightContextWaveform = null;

		if (currentFrame.n != null && (currentFrame.n instanceof FrameNoisePartWaveform)) {
			frameWaveform = ((FrameNoisePartWaveform) currentFrame.n).waveform2Doubles();

			if (!existsLeftContexts) // Take the previous frame parameters as left context (i.e. the HNM signal is a continuous
										// one, not concatenated one
			{
				if (!isFirstSynthesisFrame)
					leftContextWaveform = ((FrameNoisePartWaveform) prevFrame.n).waveform2Doubles();
				else {
					leftContextWaveform = new double[frameWaveform.length];
					Arrays.fill(leftContextWaveform, 0.0);
				}
			} else {
				if (currentLeftContext != null && currentLeftContext.n != null) {
					leftContextWaveform = ArrayUtils.copy(((FrameNoisePartWaveform) currentLeftContext.n).waveform2Doubles());
				} else {
					leftContextWaveform = new double[frameWaveform.length];
					Arrays.fill(leftContextWaveform, 0.0);
				}
			}

			waveformNoiseStartInd = SignalProcUtils.time2sample(currentFrame.tAnalysisInSeconds, samplingRateInHz);
			waveformNoiseStartInd -= leftContextWaveform.length;

			if (!existsRightContexts) // Take the next frame parameters as right context (i.e. the HNM signal is a continuous one,
										// not concatenated one
			{
				if (!isLastSynthesisFrame)
					rightContextWaveform = ((FrameNoisePartWaveform) nextFrame.n).waveform2Doubles();
				else {
					rightContextWaveform = new double[frameWaveform.length];
					Arrays.fill(rightContextWaveform, 0.0);
				}
			} else {
				if (currentRightContext != null && currentRightContext.n != null) {
					rightContextWaveform = ArrayUtils.copy(((FrameNoisePartWaveform) currentRightContext.n).waveform2Doubles());
				} else {
					rightContextWaveform = new double[frameWaveform.length];
					Arrays.fill(rightContextWaveform, 0.0);
				}
			}

			frameWaveform = ArrayUtils.combine(leftContextWaveform, frameWaveform);
			frameWaveform = ArrayUtils.combine(frameWaveform, rightContextWaveform);

			if (frameWaveform != null) {
				Window w = new HammingWindow(frameWaveform.length);
				double[] wgt = w.getCoeffs();
				for (j = waveformNoiseStartInd; j < Math.min(waveformNoiseStartInd + frameWaveform.length,
						noisePartWaveform.length); j++) {
					if (waveformNoiseStartInd + j >= 0) {
						noisePartWaveform[j] += frameWaveform[j - waveformNoiseStartInd] * wgt[j - waveformNoiseStartInd];
						wgts[j] += wgt[j - waveformNoiseStartInd];
					}
				}
			}
		}
	}
}
