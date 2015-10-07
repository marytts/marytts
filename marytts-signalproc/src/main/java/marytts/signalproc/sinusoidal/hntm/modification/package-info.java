/**
 * PSOLA like prosody modification algorithms for harmonics plus noise models.
 * The major difference is that in HNM, we repeat or eliminate speech frame parameters
 * to realise prosody modifications instead of directly using speech waveform itself.
 * The synthesis stage performs appropriate interpolations among successive frames to ensure continuity.
 */
package marytts.signalproc.sinusoidal.hntm.modification;

