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

package marytts.signalproc.sinusoidal.hntm.synthesis;

/**
 * @author oytun.turk
 * 
 */
public class HntmSynthesizerParams {
	public int harmonicPartSynthesisMethod; // Synthesis algorithm for harmonic part
	public static final int LINEAR_PHASE_INTERPOLATION = 1; // Linear interpolation of phases
	public static final int CUBIC_PHASE_INTERPOLATION = 2; // Cubic interpolation of phases

	public int noisePartLpcSynthesisMethod; // Synthesis algorithm for LPC based noise models
	public static final int OVERLAP_ADD_WITH_WINDOWING = 1; // Windowed overlap add
	public static final int LP_FILTER_WITH_POST_HPF_AND_WINDOWING = 2; // Linear prediction time domain generation filter followed
																		// by highpass filtering and windowing

	// Triangular noise envelope window for voiced segments
	public boolean applyTriangularNoiseEnvelopeForVoicedParts; // Apply triangular envelope?
	public double energyTriangleLowerValue; // Minimum value of the triangular envelope
	public double energyTriangleUpperValue; // Maximum value of the triangular envelope
	//

	public float noiseSynthesisWindowDurationInSeconds; // Noise part synthesis window duration in seconds
	public float noiseSynthesisTransitionOverlapInSeconds; // Transition between noise and non-noise parts in time
	public float harmonicSynthesisTransitionOverlapInSeconds; // Transition of harmonic and non-harmonic parts in time
	public float unvoicedVoicedTrackTransitionInSeconds; // Voiced unvoiced transition length in seconds

	public boolean hpfAfterNoiseSynthesis; // Apply highpass filter after noise synthesis?

	public boolean writeSeparateHarmonicTracksToOutputs; // If true, each harmonic track is written to a separate output file for
															// debugging purposes
	public boolean normalizeHarmonicPartOutputWav; // Normalize harmonic part output wave file peak amplitude?
	public boolean normalizeNoisePartOutputWav; // Normalize noise part output wave file peak amplitude?
	public boolean normalizeOutputWav; // Normalize output wave file peak amplitude?

	public boolean writeHarmonicPartToSeparateFile; // If true, writes the harmonic part to a separate wav file
	public boolean writeNoisePartToSeparateFile; // If true, writes the noise part to a separate wav file
	public boolean writeTransientPartToSeparateFile; // If true, writes the transients part to a separate wav file
	public boolean writeOriginalMinusHarmonicPartToSeparateFile; // If true, writes the difference signal obtained by subtracting
																	// the harmonic part from the original signal to a wav file

	public boolean overlappingHarmonicPartSynthesis; // Use overlap in harmonic part synthesis across consecutive frames
	public float harmonicSynthesisOverlapInSeconds; // Amount of overlap in harmonic part synthesis

	public int synthesisFramesToAccumulateBeforeAudioGeneration; // How many frames to accumulate before generating audio, make
																	// sure it does not conflict with and overlap-add procedures
	public static final int SYNTHESIS_FRAMES_TO_ACCUMULATE_BEFORE_AUDIO_GENERATION = 5;

	public HntmSynthesizerParams() {
		harmonicPartSynthesisMethod = LINEAR_PHASE_INTERPOLATION;
		// harmonicPartSynthesisMethod = QUADRATIC_PHASE_INTERPOLATION

		// noisePartLpcSynthesisMethod = OVERLAP_ADD_WITH_WINDOWING;
		noisePartLpcSynthesisMethod = LP_FILTER_WITH_POST_HPF_AND_WINDOWING;

		// Triangular noise envelope window for voiced segments
		applyTriangularNoiseEnvelopeForVoicedParts = true;
		energyTriangleLowerValue = 1.0;
		energyTriangleUpperValue = 0.5;
		//

		noiseSynthesisWindowDurationInSeconds = 0.050f;
		noiseSynthesisTransitionOverlapInSeconds = 0.010f;
		harmonicSynthesisTransitionOverlapInSeconds = 0.002f;
		unvoicedVoicedTrackTransitionInSeconds = 0.005f;

		hpfAfterNoiseSynthesis = true;

		writeSeparateHarmonicTracksToOutputs = false;
		normalizeHarmonicPartOutputWav = false;
		normalizeNoisePartOutputWav = false;
		normalizeOutputWav = false;

		writeHarmonicPartToSeparateFile = true;
		writeNoisePartToSeparateFile = true;
		writeTransientPartToSeparateFile = true;
		writeOriginalMinusHarmonicPartToSeparateFile = true;

		overlappingHarmonicPartSynthesis = false;
		harmonicSynthesisOverlapInSeconds = 0.020f;

		synthesisFramesToAccumulateBeforeAudioGeneration = SYNTHESIS_FRAMES_TO_ACCUMULATE_BEFORE_AUDIO_GENERATION;
	}

	public HntmSynthesizerParams(HntmSynthesizerParams existing) {
		harmonicPartSynthesisMethod = existing.harmonicPartSynthesisMethod;
		noisePartLpcSynthesisMethod = existing.noisePartLpcSynthesisMethod;

		applyTriangularNoiseEnvelopeForVoicedParts = existing.applyTriangularNoiseEnvelopeForVoicedParts;
		energyTriangleLowerValue = existing.energyTriangleLowerValue;
		energyTriangleUpperValue = existing.energyTriangleUpperValue;

		noiseSynthesisWindowDurationInSeconds = existing.noiseSynthesisWindowDurationInSeconds;
		noiseSynthesisTransitionOverlapInSeconds = existing.noiseSynthesisTransitionOverlapInSeconds;
		harmonicSynthesisTransitionOverlapInSeconds = existing.harmonicSynthesisTransitionOverlapInSeconds;
		unvoicedVoicedTrackTransitionInSeconds = existing.unvoicedVoicedTrackTransitionInSeconds;

		hpfAfterNoiseSynthesis = existing.hpfAfterNoiseSynthesis;

		writeSeparateHarmonicTracksToOutputs = existing.writeSeparateHarmonicTracksToOutputs;
		normalizeHarmonicPartOutputWav = existing.normalizeHarmonicPartOutputWav;
		normalizeNoisePartOutputWav = existing.normalizeNoisePartOutputWav;
		normalizeOutputWav = existing.normalizeOutputWav;

		writeHarmonicPartToSeparateFile = existing.writeHarmonicPartToSeparateFile;
		writeNoisePartToSeparateFile = existing.writeNoisePartToSeparateFile;
		writeTransientPartToSeparateFile = existing.writeTransientPartToSeparateFile;
		writeOriginalMinusHarmonicPartToSeparateFile = existing.writeOriginalMinusHarmonicPartToSeparateFile;

		overlappingHarmonicPartSynthesis = existing.overlappingHarmonicPartSynthesis;
		harmonicSynthesisOverlapInSeconds = existing.harmonicSynthesisOverlapInSeconds;

		synthesisFramesToAccumulateBeforeAudioGeneration = existing.synthesisFramesToAccumulateBeforeAudioGeneration;
	}

}
