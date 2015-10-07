/**
 * Outlier elimination algorithms for voice conversion.
 * The aim of outlier elimination is to detect source and target pairs
 * that might result in reduced conversion performance or artefacts. 
 * The best working method so far has been the GaussianOutlierEliminator.
 * This method fits a single Gaussian to the difference distributions of various
 * source and target acoustic features (LSFs, duration, f0, energy).
 * Then, the pairs with significant difference as compared to distribution mean are eliminated.
 * The voice conversion training step can also be set to eliminate too close pairs, forcing
 * an average amount of unlikeliness in the training data.
 */
package marytts.signalproc.adaptation.outlier;

