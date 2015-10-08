/**
 * Smoothing algorithms for voice conversion.
 * Smoothing acts as post-processing step to smooth out 
 * the spectral transformation filter from one frame to another.
 * It is highly suggested for detailed conversion approaches such as weighted frame mapping.
 * For already-smooth approaches like classical codebook and GMM mapping,
 * it may cause oversmoothing and muffling effects.
 */
package marytts.signalproc.adaptation.smoothing;

