/**
 * A collection of analysis algorithms for signal processing.
 * Important classes are as follows:
 *   -LpcAnalyser: Linearprediction analysis using autocorrelation approach and Durbin recursion
 *   -LsfAnalyser: Computation of line spectral frequencies
 *     (LSFs, or line spectral pairs - LSPs) based on LpcAnalyser
 *   -EnergyAnalyser: Energy contour estimation with voice activity detection support
 *   -F0TrackerAutocorrelationHeuristic: An autocorrelation based f0 analysis algorithm extended with
 *     heuristic post-processing to reduce voice/unvoiced errors and f0 doubling/halving problems.
 *     This tracker works better as compared to Praat but worse as compared to Snack/Wavesurfer. 
 *     Snack employs RAPT (Robust Algorithm for Pitch Tracking [Talkin, 1995])
 *   -SeevocAnalyser: A basic implementation of the Spectral Envelope Estimation Vocoder which fits
 *     a spectral envelope to spectral peaks using peak detection and linear interpolation.
 *     This is used by sinusoidal models.
 *   -RegularizedCepstrumEstimator (and classes derived from it): Estimation of a spectral envelope
 *     by cepstrum method using spectral amplitudes measured in frequency domain. These methods are used
 *     in sinusoidal and harmonics plus noise models-
 * [Talkin, 1995] D. Talkin, "A robust algorithm for pitch tracking (RAPT)", in Speech Coding and Synthesis
 * (W. B. Kleijn and K. K. Paliwal, eds.), ch. 14, Elsevier Science, 1995, pp. 495-518.
 */
package marytts.signalproc.analysis;