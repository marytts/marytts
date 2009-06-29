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

/**
 * @author oytun.turk
 *
 */
public class HnmPitchVoicingAnalyzerParams 
{
    public float mvfAnalysisWindowSizeInSeconds;
    public float mvfAnalysisSkipSizeInSeconds;
    public float f0AnalysisWindowSizeInSeconds;
    public float f0AnalysisSkipSizeInSeconds;
    public int fftSize;
    public int numFilteringStages;
    public int medianFilterLength;
    public int movingAverageFilterLength;
    public float numPeriodsInitialPitchEstimation;
    public double cumulativeAmpThreshold;
    public double maximumAmpThresholdInDB;
    public double harmonicDeviationPercent;
    public double sharpPeakAmpDiffInDB;
    public int minimumTotalHarmonics;
    public int maximumTotalHarmonics;
    public float minimumVoicedFrequencyOfVoicing;
    public float maximumVoicedFrequencyOfVoicing;
    public float maximumFrequencyOfVoicingFinalShift;
    public float runningMeanVoicingThreshold;
    public int lastCorrelatedHarmonicNeighbour;
    public double vuvSearchMinHarmonicMultiplier;
    public double vuvSearchMaxHarmonicMultiplier;
    public double neighsPercent;
    
    public HnmPitchVoicingAnalyzerParams(HnmPitchVoicingAnalyzerParams existing)
    {
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
    
    public HnmPitchVoicingAnalyzerParams()
    {
        mvfAnalysisWindowSizeInSeconds = 0.040f;
        mvfAnalysisSkipSizeInSeconds = 0.010f;
        f0AnalysisWindowSizeInSeconds = 0.040f;
        f0AnalysisSkipSizeInSeconds = 0.005f;
        fftSize = 4096;
        
        numFilteringStages = 2; //2;
        medianFilterLength = 12; //12; //Length of median filter for smoothing the max. freq. of voicing contour
        movingAverageFilterLength = 12; //12; //Length of first moving averaging filter for smoothing the max. freq. of voicing contour

        numPeriodsInitialPitchEstimation = 3.0f;
        
        cumulativeAmpThreshold = 2.0; //Decreased ==> Voicing increases (Orig: 2.0)
        maximumAmpThresholdInDB = 13.0; //Decreased ==> Voicing increases (Orig: 13.0)
        harmonicDeviationPercent = 20.0; //Increased ==> Voicing increases (Orig: 20.0)
        sharpPeakAmpDiffInDB = 12.0; //Decreased ==> Voicing increases (Orig: 12.0)
        minimumTotalHarmonics = 0; //Minimum number of total harmonics to be included in voiced region (effective only when f0>10.0)
        maximumTotalHarmonics = 100; //Maximum number of total harmonics to be included in voiced region (effective only when f0>10.0)
        minimumVoicedFrequencyOfVoicing = 3000.0f; //All voiced sections will have at least this freq. of voicing
        maximumVoicedFrequencyOfVoicing = 5000.0f; //All voiced sections will have at least this freq. of voicing
        maximumFrequencyOfVoicingFinalShift = 0.0f; //The max freq. of voicing contour is shifted by this amount finally
        runningMeanVoicingThreshold = 0.5f; //Between 0.0 and 1.0, decrease ==> Max. voicing freq increases

        lastCorrelatedHarmonicNeighbour = -1; //Assume correlation between at most among this many harmonics (-1 ==> full correlation approach) 
        
        //For voicing detection
        vuvSearchMinHarmonicMultiplier = 0.7;
        vuvSearchMaxHarmonicMultiplier = 4.3;
        //

        neighsPercent = 50.0; //Should be between 0.0 and 100.0. 50.0 means the peak in the band should be greater than 50% of the half of the band samples
        ////
    }
}
