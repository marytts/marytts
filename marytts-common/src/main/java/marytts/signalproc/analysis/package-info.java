/**
 * A collection of analysis algorithms for signal processing.<br>
 * Important classes are as follows:
 * <ul>
 *   <li>LpcAnalyser: Linear prediction analysis using autocorrelation
 * appraoch and Durbin recursion</li>
 *   <li>LsfAnalyser: Computation of line spectral frequencies (LSFs, or
 * line spectral pairs - LSPs) based on LpcAnalyser</li>
 *   <li>EnergyAnalyser: Energy contour estimation with voice activity
 * detection support</li>
 *   <li>F0TrackerAutocorrelationHeuristic: An autocorrelation based
 * f0 analysis algorithm extended with heuristic post-processing to reduce
 * voiced/unvoiced errors and f0 doubling/halving problems. This tracker
 * works better as compared to Praat but worse as compared to
 * Snack/Wavesurfer. Snack employs RAPT (Robust Algorithm for Pitch
 * Tracking [Talkin, 1995])</li>
 *   <li>SeevocAnalyser: A basic implementation of the Spectral Envelope
 * Estimation Vocoder which fits a spectral envelope to spectral peaks
 * using peak detection and linear interpolation. This is used by
 * sinusoidal models.</li>
 *   <li>RegularizedCepstrumEstimator (and classes derived from it):
 * Estimation of a spectral envelope by cepstrum method using spectral
 * amplitudes measured in frequency domain. These methods are used in
 * sinusoidal and harmonics plus noise models</li>
 * </ul>
 * <p>[Talkin, 1995] D. Talkin, "A robust algorithm for pitch tracking
 * (RAPT)," in <i>Speech Coding and Synthesis</i> (W. B. Kleijn and K. K.
 * Paliwal, eds.), ch. 14, Elsevier Science, 1995, pp. 495-518. </p>
 */
package marytts.signalproc.analysis;

