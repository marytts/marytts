/**
 * Packages for speaking style and speaker identity adaptation in Mary TTS 
 * supporting various voice conversion algorithms.
 * The subpackages include:
 * .codebook: WEeighted codebook and weighted frame mapping
 * .gmm: Gaussian mixture model based spectral conversion
 *   .gmm.jointgmm: Joint source-target GMM based voice conversion
 *   (this method has been used in our group's various publications in 2008 and 2009)
 * <p>
 * IMPORTANT NOTE:
 * For expressive speech transformation, GMM based technology works better
 * as compared to codebook and frame mapping based methods.
 * For speaker identity conversion, opposite results are observed in various papers.
 */
package marytts.signalproc.adaptation;