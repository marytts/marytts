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

package marytts.signalproc.sinusoidal.hntm.analysis.pitch;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A class for maximum frequency of voicing, f0, and voicing analysis for HNMs
 * 
 * @author oytun.turk
 * 
 */
public class HnmPitchVoicingAnalyzerParams {
	public float mvfAnalysisWindowSizeInSeconds; // Max. freq. of voicing analysis window size in seconds
	public float mvfAnalysisSkipSizeInSeconds; // Max. freq. of voicing analysis skip size in seconds
	public float f0AnalysisWindowSizeInSeconds; // F0 detection window size in seconds
	public float f0AnalysisSkipSizeInSeconds; // F0 detection skip size in seconds
	public int fftSize; // DFT length for frequency domain analyses
	public int numFilteringStages; // Total consecutive median-moving average filtering steps to smooth out the max. freq. of
									// voicing curve
	public int medianFilterLength; // Length of median filter for smoothing the max. freq. of voicing contour
	public int movingAverageFilterLength; // Length of first moving averaging filter for smoothing the max. freq. of voicing
											// contour
	public float numPeriodsInitialPitchEstimation; // Total number of periods to use for initial pitch estimation
	public float cumulativeAmpThreshold; // Decreased ==> Voicing increases (Orig: 2.0f)
	public float maximumAmpThresholdInDB; // Decreased ==> Voicing increases (Orig: 13.0f)
	public float harmonicDeviationPercent; // Increased ==> Voicing increases (Orig: 20.0f)
	public float sharpPeakAmpDiffInDB; // Decreased ==> Voicing increases (Orig: 12.0f)
	public int minimumTotalHarmonics; // Minimum number of total harmonics to be included in voiced region (effective only when
										// f0>10.0)
	public int maximumTotalHarmonics; // Maximum number of total harmonics to be included in voiced region (effective only when
										// f0>10.0)
	public float minimumVoicedFrequencyOfVoicing; // All voiced sections will have at least this freq. of voicing
	public float maximumVoicedFrequencyOfVoicing; // All voiced sections will have at most this freq. of voicing
	public float maximumFrequencyOfVoicingFinalShift; // The max freq. of voicing contour is shifted by this amount finally
	public float runningMeanVoicingThreshold; // Between 0.0 and 1.0, decrease ==> Max. voicing freq increases
	public int lastCorrelatedHarmonicNeighbour; // Assume correlation between at most among this many harmonics (-1 ==> full
												// correlation approach)
	public float vuvSearchMinHarmonicMultiplier; // Multiplied with f0, gives the minimum frequency above which voicing detection
													// will be carried outv
	public float vuvSearchMaxHarmonicMultiplier; // Multiplied with f0, gives the maximum frequency below which voicing detection
													// will be carried out
	public float neighsPercent; // Should be between 0.0f and 100.0f. 50.0f means the peak in the band should be greater than 50%
								// of the half of the band samples

	public HnmPitchVoicingAnalyzerParams() {
		mvfAnalysisWindowSizeInSeconds = 0.040f;
		mvfAnalysisSkipSizeInSeconds = 0.010f;
		f0AnalysisWindowSizeInSeconds = 0.040f;
		f0AnalysisSkipSizeInSeconds = 0.005f;
		fftSize = 4096;

		numFilteringStages = 2; // 2; //Total consecutive median-moving average filtering steps to smooth out the max. freq. of
								// voicing curve
		medianFilterLength = 12; // 12; //Length of median filter for smoothing the max. freq. of voicing contour
		movingAverageFilterLength = 12; // 12; //Length of first moving averaging filter for smoothing the max. freq. of voicing
										// contour

		numPeriodsInitialPitchEstimation = 3.0f;

		cumulativeAmpThreshold = 2.0f; // Decreased ==> Voicing increases (Orig: 2.0f)
		maximumAmpThresholdInDB = 13.0f; // Decreased ==> Voicing increases (Orig: 13.0f)
		harmonicDeviationPercent = 20.0f; // Increased ==> Voicing increases (Orig: 20.0f)
		sharpPeakAmpDiffInDB = 12.0f; // Decreased ==> Voicing increases (Orig: 12.0f)
		minimumTotalHarmonics = 0; // Minimum number of total harmonics to be included in voiced region (effective only when
									// f0>10.0)
		maximumTotalHarmonics = 100; // Maximum number of total harmonics to be included in voiced region (effective only when
										// f0>10.0)
		minimumVoicedFrequencyOfVoicing = 5000.0f; // All voiced sections will have at least this freq. of voicing
		maximumVoicedFrequencyOfVoicing = 5000.0f; // All voiced sections will have at most this freq. of voicing
		maximumFrequencyOfVoicingFinalShift = 0.0f; // The max freq. of voicing contour is shifted by this amount finally
		runningMeanVoicingThreshold = 0.5f; // Between 0.0 and 1.0, decrease ==> Max. voicing freq increases

		lastCorrelatedHarmonicNeighbour = -1; // Assume correlation between at most among this many harmonics (-1 ==> full
												// correlation approach)

		// For voicing detection
		vuvSearchMinHarmonicMultiplier = 0.7f; // Multiplied with f0, gives the minimum frequency above which voicing detection
												// will be carried out
		vuvSearchMaxHarmonicMultiplier = 4.3f; // Multiplied with f0, gives the maximum frequency below which voicing detection
												// will be carried out
		//

		neighsPercent = 50.0f; // Should be between 0.0f and 100.0f. 50.0f means the peak in the band should be greater than 50%
								// of the half of the band samples
		// //
	}

	public HnmPitchVoicingAnalyzerParams(DataInputStream dis) {
		try {
			read(dis);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public HnmPitchVoicingAnalyzerParams(HnmPitchVoicingAnalyzerParams existing) {
		mvfAnalysisWindowSizeInSeconds = existing.mvfAnalysisWindowSizeInSeconds;
		mvfAnalysisSkipSizeInSeconds = existing.mvfAnalysisSkipSizeInSeconds;
		f0AnalysisWindowSizeInSeconds = existing.f0AnalysisWindowSizeInSeconds;
		f0AnalysisSkipSizeInSeconds = existing.f0AnalysisSkipSizeInSeconds;
		fftSize = existing.fftSize;
		numFilteringStages = existing.numFilteringStages;
		medianFilterLength = existing.medianFilterLength;
		movingAverageFilterLength = existing.movingAverageFilterLength;
		numPeriodsInitialPitchEstimation = existing.numPeriodsInitialPitchEstimation;
		cumulativeAmpThreshold = existing.cumulativeAmpThreshold;
		maximumAmpThresholdInDB = existing.maximumAmpThresholdInDB;
		harmonicDeviationPercent = existing.harmonicDeviationPercent;
		sharpPeakAmpDiffInDB = existing.sharpPeakAmpDiffInDB;
		minimumTotalHarmonics = existing.minimumTotalHarmonics;
		maximumTotalHarmonics = existing.maximumTotalHarmonics;
		minimumVoicedFrequencyOfVoicing = existing.minimumVoicedFrequencyOfVoicing;
		maximumVoicedFrequencyOfVoicing = existing.maximumVoicedFrequencyOfVoicing;
		maximumFrequencyOfVoicingFinalShift = existing.maximumFrequencyOfVoicingFinalShift;
		runningMeanVoicingThreshold = existing.runningMeanVoicingThreshold;
		lastCorrelatedHarmonicNeighbour = existing.lastCorrelatedHarmonicNeighbour;
		vuvSearchMinHarmonicMultiplier = existing.vuvSearchMinHarmonicMultiplier;
		vuvSearchMaxHarmonicMultiplier = existing.vuvSearchMaxHarmonicMultiplier;
		neighsPercent = existing.neighsPercent;
	}

	public boolean equals(HnmPitchVoicingAnalyzerParams existing) {
		if (mvfAnalysisWindowSizeInSeconds != existing.mvfAnalysisWindowSizeInSeconds)
			return false;
		if (mvfAnalysisSkipSizeInSeconds != existing.mvfAnalysisSkipSizeInSeconds)
			return false;
		if (f0AnalysisWindowSizeInSeconds != existing.f0AnalysisWindowSizeInSeconds)
			return false;
		if (f0AnalysisSkipSizeInSeconds != existing.f0AnalysisSkipSizeInSeconds)
			return false;
		if (fftSize != existing.fftSize)
			return false;
		if (numFilteringStages != existing.numFilteringStages)
			return false;
		if (medianFilterLength != existing.medianFilterLength)
			return false;
		if (movingAverageFilterLength != existing.movingAverageFilterLength)
			return false;
		if (numPeriodsInitialPitchEstimation != existing.numPeriodsInitialPitchEstimation)
			return false;
		if (cumulativeAmpThreshold != existing.cumulativeAmpThreshold)
			return false;
		if (maximumAmpThresholdInDB != existing.maximumAmpThresholdInDB)
			return false;
		if (harmonicDeviationPercent != existing.harmonicDeviationPercent)
			return false;
		if (sharpPeakAmpDiffInDB != existing.sharpPeakAmpDiffInDB)
			return false;
		if (minimumTotalHarmonics != existing.minimumTotalHarmonics)
			return false;
		if (maximumTotalHarmonics != existing.maximumTotalHarmonics)
			return false;
		if (minimumVoicedFrequencyOfVoicing != existing.minimumVoicedFrequencyOfVoicing)
			return false;
		if (maximumVoicedFrequencyOfVoicing != existing.maximumVoicedFrequencyOfVoicing)
			return false;
		if (maximumFrequencyOfVoicingFinalShift != existing.maximumFrequencyOfVoicingFinalShift)
			return false;
		if (runningMeanVoicingThreshold != existing.runningMeanVoicingThreshold)
			return false;
		if (lastCorrelatedHarmonicNeighbour != existing.lastCorrelatedHarmonicNeighbour)
			return false;
		if (vuvSearchMinHarmonicMultiplier != existing.vuvSearchMinHarmonicMultiplier)
			return false;
		if (vuvSearchMaxHarmonicMultiplier != existing.vuvSearchMaxHarmonicMultiplier)
			return false;
		if (neighsPercent != existing.neighsPercent)
			return false;

		return true;
	}

	public void write(DataOutputStream dos) throws IOException {
		dos.writeFloat(mvfAnalysisWindowSizeInSeconds);
		dos.writeFloat(mvfAnalysisSkipSizeInSeconds);
		dos.writeFloat(f0AnalysisWindowSizeInSeconds);
		dos.writeFloat(f0AnalysisSkipSizeInSeconds);
		dos.writeInt(fftSize);
		dos.writeInt(numFilteringStages);
		dos.writeInt(medianFilterLength);
		dos.writeInt(movingAverageFilterLength);
		dos.writeFloat(numPeriodsInitialPitchEstimation);
		dos.writeDouble(cumulativeAmpThreshold);
		dos.writeDouble(maximumAmpThresholdInDB);
		dos.writeDouble(harmonicDeviationPercent);
		dos.writeDouble(sharpPeakAmpDiffInDB);
		dos.writeInt(minimumTotalHarmonics);
		dos.writeInt(maximumTotalHarmonics);
		dos.writeFloat(minimumVoicedFrequencyOfVoicing);
		dos.writeFloat(maximumVoicedFrequencyOfVoicing);
		dos.writeFloat(maximumFrequencyOfVoicingFinalShift);
		dos.writeFloat(runningMeanVoicingThreshold);
		dos.writeInt(lastCorrelatedHarmonicNeighbour);
		dos.writeDouble(vuvSearchMinHarmonicMultiplier);
		dos.writeDouble(vuvSearchMaxHarmonicMultiplier);
		dos.writeDouble(neighsPercent);
	}

	public void read(DataInputStream dis) throws IOException {
		mvfAnalysisWindowSizeInSeconds = dis.readFloat();
		mvfAnalysisSkipSizeInSeconds = dis.readFloat();
		f0AnalysisWindowSizeInSeconds = dis.readFloat();
		f0AnalysisSkipSizeInSeconds = dis.readFloat();
		fftSize = dis.readInt();
		numFilteringStages = dis.readInt();
		medianFilterLength = dis.readInt();
		movingAverageFilterLength = dis.readInt();
		numPeriodsInitialPitchEstimation = dis.readFloat();
		cumulativeAmpThreshold = dis.readFloat();
		maximumAmpThresholdInDB = dis.readFloat();
		harmonicDeviationPercent = dis.readFloat();
		sharpPeakAmpDiffInDB = dis.readFloat();
		minimumTotalHarmonics = dis.readInt();
		maximumTotalHarmonics = dis.readInt();
		minimumVoicedFrequencyOfVoicing = dis.readFloat();
		maximumVoicedFrequencyOfVoicing = dis.readFloat();
		maximumFrequencyOfVoicingFinalShift = dis.readFloat();
		runningMeanVoicingThreshold = dis.readFloat();
		lastCorrelatedHarmonicNeighbour = dis.readInt();
		vuvSearchMinHarmonicMultiplier = dis.readFloat();
		vuvSearchMaxHarmonicMultiplier = dis.readFloat();
		neighsPercent = dis.readFloat();
	}
}
