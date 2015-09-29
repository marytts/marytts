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

package marytts.signalproc.process;

import java.util.Arrays;

import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;

/**
 * @author oytun.turk
 * 
 */
public class TDPSOLAProcessor {

	// TDPSOLA - Time domain pitch synchronous overlap-add algorithm
	// This version does not actually perform waveform synthesis but computes synthesis time instants from analysis instants
	// The output can be used by a waveform generator to perform the actual PSOLA operations
	// Important features of the algorithm are:
	// (1) Performs duration compensation to match the desired duration exactly (when tscale=1.0)
	// (2) Supports pitch scaling factors <<=0.5
	// (3) Supports fixed and variable rate pitch/time scaling
	//
	// analysisInstants: Pitch synchronous analysis time instants, in seconds (Nx1)
	// samplingRateInHz: Sampling rate in Hz
	// vuvs: Voiced(1)/Unvoiced(0) labels for regions in between two successive pitch marks -- Nx1
	// pScales (Nx1): Pitch scaling amount for each frame (pScales[i]<1 => pitch period expansion (lower f0), Pscales[i]>1 =>
	// pitch period compression (higher f0)
	// tScales (Nx1): Time scaling factor for each frame (tScales[i]<1 => time scale compression, tScales[i]>1 => time scale
	// expansion
	//
	// If the length of the pitch/time scaling vectors are shorter than the fixed skip size frames that can be obtained from the
	// signal,
	// the vectors are linearly interpolated to match the signal length
	//
	// @author Oytun T&uuml;rk
	public static TDPSOLAInstants transformAnalysisInstants(float[] analysisInstants, int samplingRateInHz, boolean[] vuvs,
			float[] tScales, float[] pScales) {
		TDPSOLAInstants synthesisInstants = null;

		int numfrm = analysisInstants.length;

		// Compute new frame sizes, change in durations due to pitch scaling, and required compensation amount in samples
		float[] frmSizesInSeconds = new float[numfrm];
		Arrays.fill(frmSizesInSeconds, 0.0f);
		float[] newFrmSizesInSeconds = new float[numfrm];
		Arrays.fill(newFrmSizesInSeconds, 0.0f);
		float[] newPeriodsInSeconds = new float[numfrm];
		Arrays.fill(newPeriodsInSeconds, 0.0f);
		float[] localDurDiffs = new float[numfrm];
		Arrays.fill(localDurDiffs, 0.0f);

		float newLenInSeconds = 0.0f;
		int i;
		for (i = 0; i < numfrm - 1; i++) {
			frmSizesInSeconds[i] = analysisInstants[i + 1] - analysisInstants[i];

			if (vuvs[i])
				newFrmSizesInSeconds[i] = frmSizesInSeconds[i] / pScales[i];
			else
				newFrmSizesInSeconds[i] = frmSizesInSeconds[i];

			newPeriodsInSeconds[i] = newFrmSizesInSeconds[i];
			// Compute duration compensation required:
			// localDurDiffs(i) = (DESIRED)-(AFTER PITCHSCALING)
			// (-) if expansion occured, (+) if compression occured
			// We aim to make this as close to zero as possible in the following duration compensation step
			localDurDiffs[i] = frmSizesInSeconds[i] * tScales[i] - newFrmSizesInSeconds[i];
			newLenInSeconds += newPeriodsInSeconds[i];
		}
		//

		// Find out which pitch-scaled frames to repeat/skip for overall duration compensation
		int[] repeatSkipCounts = new int[numfrm]; // -1:skip frame, 0:no repetition (use synthesized frame as it is), >0: number
													// of repetitions for synthesized frame
		Arrays.fill(repeatSkipCounts, 0);
		for (i = 0; i < numfrm; i++) {
			if (localDurDiffs[i] < -0.1f * newPeriodsInSeconds[i]) // Expansion occured so skip this frame
			{
				repeatSkipCounts[i] -= 1;
				if (i < numfrm - 1) {
					localDurDiffs[i + 1] += localDurDiffs[i] + newPeriodsInSeconds[i];
					localDurDiffs[i] = 0.0f;
				}
			} else if (localDurDiffs[i] > 0.1 * newPeriodsInSeconds[i]) // Compression occured so repeat this frame
			{
				while (localDurDiffs[i] > 0.1 * newPeriodsInSeconds[i] && newPeriodsInSeconds[i] > 1e-10) {
					repeatSkipCounts[i] += 1;
					localDurDiffs[i] -= newPeriodsInSeconds[i];
					newLenInSeconds += newPeriodsInSeconds[i];
				}

				if (i < numfrm - 1) {
					localDurDiffs[i + 1] += localDurDiffs[i];
					localDurDiffs[i] = 0.0f;
				}
			}
		}
		//

		// Check the final length and perform additional repetitions if necessary
		localDurDiffs[numfrm - 1] = MathUtils.sum(localDurDiffs);
		while (localDurDiffs[numfrm - 1] > 0.0f && newPeriodsInSeconds[numfrm - 1] > 1e-10) {
			repeatSkipCounts[numfrm - 1] += 1;
			localDurDiffs[numfrm - 1] -= newPeriodsInSeconds[numfrm - 1];
			newLenInSeconds += newPeriodsInSeconds[numfrm - 1];
		}
		//

		int numSynthesisInstants = MathUtils.sum(repeatSkipCounts) + repeatSkipCounts.length;

		if (numSynthesisInstants > 0) {
			synthesisInstants = new TDPSOLAInstants(numSynthesisInstants);
			float synthSt = analysisInstants[0];
			float synthTotal = 0.0f;
			boolean bFirstSynthFrame = true;
			boolean bLastFrame = false;
			boolean bBroke = false;

			int j, k;
			int synthesisFrameCounter = 0;
			for (i = 0; i < numfrm; i++) {
				if (bBroke)
					break;

				if (repeatSkipCounts[i] > -1) {
					for (j = 1; j <= repeatSkipCounts[i] + 1; j++) {
						synthesisInstants.analysisInstantsInSeconds[synthesisFrameCounter] = analysisInstants[i];
						synthesisInstants.synthesisInstantsInSeconds[synthesisFrameCounter] = synthSt;
						synthesisFrameCounter++;

						bLastFrame = false;
						if (i == numfrm - 1) {
							if (j == repeatSkipCounts[i] + 1)
								bLastFrame = true;
						} else {
							boolean bAll = true;
							for (k = i + 1; k <= numfrm; k++) {
								if (repeatSkipCounts[k - 1] != -1) {
									bAll = false;
									break;
								}
							}

							if (bAll)
								bLastFrame = true;
						}

						if (i < numfrm - 1) {
							if (vuvs[i])
								synthSt += (analysisInstants[i + 1] - analysisInstants[i]) / pScales[i];
							else
								synthSt += (analysisInstants[i + 1] - analysisInstants[i]);
						} else {
							if (vuvs[i])
								synthSt += (analysisInstants[i] - analysisInstants[i - 1]) / pScales[i];
							else
								synthSt += (analysisInstants[i] - analysisInstants[i - 1]);
						}

						if (bLastFrame) {
							bBroke = true;
							break;
						}
					}
				}
			}
		}

		if (synthesisInstants != null)
			synthesisInstants.repeatSkipCounts = ArrayUtils.copy(repeatSkipCounts);

		return synthesisInstants;
	}

}
