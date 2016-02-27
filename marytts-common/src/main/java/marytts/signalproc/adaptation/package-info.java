/**
 * Packages for speaking style and speaker identity adaptation in Mary TTS 
 * supporting various voice conversion algorithms.
 * The subpackages include:
 * <ul>
 *   <li>.codebook: WEeighted codebook and weighted frame mapping</li>
 *   <li>.gmm: Gaussian mixture model based spectral conversion</li>
 *   <li>.gmm.jointgmm: Joint source-target GMM based voice conversion
 *   (this method has been used in our group's various publications in 2008 and 2009)</li>
 * </ul>
 * IMPORTANT NOTE:
 * For expressive speech transformation, GMM based technology works better
 * as compared to codebook and frame mapping based methods.
 * For speaker identity conversion, opposite results are observed in various papers.
 */
package marytts.signalproc.adaptation;

