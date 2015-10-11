/**
 * Weighted codebook based voice conversion algorithms.
 * The core weighted codebook mapping algorithm 
 * (WeightedCodebookParallelTrainer) is extended to enable
 *  frame based mapping as well within the same class.
 *  Classical codebook mapping produces more smooth results since
 *  the source and target acoustic mapping is done using average spectral faiuler vectors
 *  corresponding to each source and target phoneme pair observed in the training data.
 *  Frame mapping goes one step beyond to directly map the source and target frame-level features.
 *  Therefore, it is expected to result in more detail 
 *  with an increased probability of output discontinuities.
 *  This can be compensated by to some extent using the temporal transformation function smoother,
 * marytts.signalproc.adaptation.smoothing.TemporalSmoother
 */
package marytts.signalproc.adaptation.codebook;

