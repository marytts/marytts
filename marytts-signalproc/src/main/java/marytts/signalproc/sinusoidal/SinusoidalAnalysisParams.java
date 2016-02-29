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

package marytts.signalproc.sinusoidal;

import marytts.signalproc.sinusoidal.hntm.analysis.pitch.HnmPitchVoicingAnalyzerParams;
import marytts.signalproc.window.Window;
import marytts.util.signal.SignalProcUtils;

/**
 * Parameters of sinusoidal model based analysis
 * 
 * @author oytun.turk
 * 
 */
public class SinusoidalAnalysisParams {

	// Static values
	public static final float DEFAULT_DELTA_IN_HZ = 50.0f; //
	public static final float DEFAULT_ANALYSIS_WINDOW_SIZE = 0.020f; // Default fixed rate analysisi window size
	public static final float DEFAULT_ANALYSIS_SKIP_SIZE = 0.010f; //
	public static final double MIN_ENERGY_TH = 1e-50; // Minimum energy threshold to analyze a frame
	public static final double MIN_PEAK_IN_DB_LOW = -200.0f; // Minimum allowed peak value in decibels for lower frequencies
	public static final double MIN_PEAK_IN_DB_HIGH = -200.0f; // Minimum allowed peak value in decibels for higher frequencies
	public static final double MIN_VOICED_FREQ_IN_HZ = 4000.0f; // Minimum voiced freq allowed (for voiced regions only)
	public static final double MAX_VOICED_FREQ_IN_HZ = 5000.0f; // Maximum voiced freq allowed (for voiced regions only)

	public static final boolean DEFAULT_REFINE_PEAK_ESTIMATES_PARABOLA = true; // Parabola fitting based refinement of peak
																				// amplitude values to cope with windowing effects
	public static final boolean DEFAULT_REFINE_PEAK_ESTIMATES_BIAS = true; // Bias removal based refinement of peak amplitude
																			// values to cope with windowing effects

	public static final int DEFAULT_FREQ_SAMP_NEIGHS_LOW = 2; // Default search range for low frequencies for spectral peak
																// detection
	public static final int DEFAULT_FREQ_SAMP_NEIGHS_HIGH = 2; // Default search range for high frequencies for spectral peak
																// detection

	public static final float MIN_WINDOW_SIZE = 0.020f;

	public static final int NO_SPEC = -1; // No spectral envelope information is extracted
	public static final int LP_SPEC = 0; // Linear Prediction (LP) based envelope (Makhoul, 1971)
	public static final int SEEVOC_SPEC = 1; // Spectral Envelope Estimation Vocoder (SEEVOC) based envelope (Paul, 1981)
	public static final int REGULARIZED_CEPS = 2; // Regularized cepstrum based envelope (Cappe, et. al. 1995, Stylianou, et. al.
													// 1995)
	//

	HnmPitchVoicingAnalyzerParams hnmPitchVoicingAnalyzerParams;
	public int fs; // Sampling rate in Hz
	public int windowType; // Type of window (See class Window for details)
	public int fftSize; // FFT size in points
	public int LPOrder; // LP analysis order
	public int lifterOrder; // Cepstral lifting order

	public double startFreq; // Lowest analysis frequnecy in Hz
	public double endFreq; // Highest analysis frequency in Hz

	public boolean bRefinePeakEstimatesParabola; // Refine peak and frequency estimates by fitting parabolas?
	public boolean bRefinePeakEstimatesBias; // Further refine peak and frequency estimates by correcting bias?
												// (Only effective when bRefinePeakEstimatesParabola=true)
	public boolean bSpectralReassignment; // Refine spectral peak frequencies considering windowing effect?

	public int ws; // Window size in samples
	public int ss; // Skip size in samples
	public Window win; // Windowing applier

	public boolean bAdjustNeighFreqDependent; // Adjust number of neighbouring samples to search for a peak adaptively depending
												// on frequency?

	public int minWindowSize; // Minimum window size allowed to satisfy 100 Hz criterion for unvoiced sounds computed from
								// MIN_WINDOW_SIZE and sampling rate

	public double absMax; // Keep absolute max of the input signal for normalization after resynthesis
	public double totalEnergy; // Keep total energy for normalization after resynthesis

	public int regularizedCepstrumWarpingMethod;

	public SinusoidalAnalysisParams(SinusoidalAnalysisParams paramsIn) {
		hnmPitchVoicingAnalyzerParams = new HnmPitchVoicingAnalyzerParams(paramsIn.hnmPitchVoicingAnalyzerParams);
		fs = paramsIn.fs;
		windowType = paramsIn.windowType;
		fftSize = paramsIn.fftSize;
		LPOrder = paramsIn.LPOrder;
		lifterOrder = paramsIn.lifterOrder;

		startFreq = paramsIn.startFreq;
		endFreq = paramsIn.endFreq;

		bRefinePeakEstimatesParabola = paramsIn.bRefinePeakEstimatesParabola;
		bRefinePeakEstimatesBias = paramsIn.bRefinePeakEstimatesBias;

		bSpectralReassignment = paramsIn.bSpectralReassignment;

		ws = paramsIn.ws;
		ss = paramsIn.ss;
		win = paramsIn.win;

		bAdjustNeighFreqDependent = paramsIn.bAdjustNeighFreqDependent;

		minWindowSize = paramsIn.minWindowSize;

		absMax = paramsIn.absMax;
		totalEnergy = paramsIn.totalEnergy;
		regularizedCepstrumWarpingMethod = paramsIn.regularizedCepstrumWarpingMethod;
	}

	public SinusoidalAnalysisParams(int samplingRate, double startFreqInHz, double endFreqInHz, int windowTypeIn,
			boolean bRefinePeakEstimatesParabolaIn, boolean bRefinePeakEstimatesBiasIn, boolean bSpectralReassignmentIn,
			boolean bAdjustNeighFreqDependentIn) {
		hnmPitchVoicingAnalyzerParams = new HnmPitchVoicingAnalyzerParams();
		fs = samplingRate;
		startFreq = startFreqInHz;
		if (startFreq < 0.0)
			startFreq = 0.0;

		endFreq = endFreqInHz;
		if (endFreq < 0.0)
			endFreq = 0.5 * fs;

		windowType = windowTypeIn;
		setSinAnaFFTSize(getDefaultFFTSize(fs));

		bRefinePeakEstimatesParabola = bRefinePeakEstimatesParabolaIn;
		bRefinePeakEstimatesBias = bRefinePeakEstimatesBiasIn;
		bSpectralReassignment = bSpectralReassignmentIn;
		bAdjustNeighFreqDependent = bAdjustNeighFreqDependentIn;

		minWindowSize = (int) (Math.floor(fs * MIN_WINDOW_SIZE + 0.5));
		if (minWindowSize % 2 == 0) // Always use an odd window size to have a zero-phase analysis window
			minWindowSize++;

		absMax = -1.0;
		totalEnergy = 0.0;
		LPOrder = SignalProcUtils.getLPOrder(fs);
		lifterOrder = SignalProcUtils.getLifterOrder(fs);
	}

	public static int getDefaultFFTSize(int samplingRate) {
		if (samplingRate < 10000)
			return 1024;
		else if (samplingRate < 20000)
			return 2048;
		else
			return 4096;
	}

	public void setSinAnaFFTSize(int fftSizeIn) {
		fftSize = fftSizeIn;
	}
}
