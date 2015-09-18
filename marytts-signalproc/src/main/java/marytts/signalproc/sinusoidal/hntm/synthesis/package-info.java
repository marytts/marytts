/**
 * Synthesis package for harmonics plus noise model consisting of the following modules:
 * HarmonicPartLinearPhaseInterpolatorSynthesizer: harmonic part
synthesis with a linear phase interpolator</li>
 * NoisePartWaveformSynthesizer: Noise part synthesizer when the
noise is kept as original-harmonic waveform</li>
 * NoisePartLpFilterPostHpfLpcSynthesizer: Noise part synthesizer
using linear prediction forward filter with optional post filtering
with an highpass filter
 * NoisePartWindowedOverlapAddLpcSynthesizer: Noise part synthesizer
using a windowed overlap add approach (supports highpass filtering as
well)
 * NoisePartPseudoHarmonicSynthesizer: A pseudo-harmonic approach
for noise part generation using parameters obtained by the harmonic
part analysis algorithm applied to noise part assuming a fixed f0
 * TransientPartSynthesizer: A waveform synthesizer for transient
parts (performs windowing at transition boundaries)
 */
package marytts.signalproc.sinusoidal.hntm.synthesis;