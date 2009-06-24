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
    public int harmonicPartSynthesisMethod;
    public static final int LINEAR_PHASE_INTERPOLATION = 1;
    public static final int QUADRATIC_PHASE_INTERPOLATION = 2;
    
    public int noisePartLpcSynthesisMethod;
    public static final int OVERLAP_ADD_WITH_WINDOWING = 1;
    public static final int LP_FILTER_WITH_POST_HPF_AND_WINDOWING = 2;
    
    //Triangular noise envelope window for voiced segments
    public boolean applyTriangularNoiseEnvelopeForVoicedParts;
    public double energyTriangleLowerValue;
    public double energyTriangleUpperValue;
    //
    
    public float noiseSynthesisWindowDurationInSeconds;
    public float noiseSynthesisTransitionOverlapInSeconds;
    public float harmonicSynthesisTransitionOverlapInSeconds;
    public float unvoicedVoicedTrackTransitionInSeconds;
    
    public boolean highpassFilterAfterNoiseSynthesis;
    
    public boolean writeSeparateHarmonicTracksToOutputs;
    public boolean normalizeHarmonicPartOutputWav;
    public boolean normalizeNoisePartOutputWav;
    public boolean normalizeOutputWav;
    
    public boolean writeHarmonicPartToSeparateFile;
    public boolean writeNoisePartToSeparateFile;
    public boolean writeTransientPartToSeparateFile;
    public boolean writeOriginalMinusHarmonicPartToSeparateFile;
    
    public boolean applyVocalTractPostNormalizationProcessor; 
    
    public HntmSynthesizerParams()
    {
        harmonicPartSynthesisMethod = LINEAR_PHASE_INTERPOLATION; 
        //harmonicPartSynthesisMethod = QUADRATIC_PHASE_INTERPOLATION
        
        //noisePartLpcSynthesisMethod = OVERLAP_ADD_WITH_WINDOWING;
        noisePartLpcSynthesisMethod = LP_FILTER_WITH_POST_HPF_AND_WINDOWING;
        
        //Triangular noise envelope window for voiced segments
        applyTriangularNoiseEnvelopeForVoicedParts = true;
        energyTriangleLowerValue = 1.0;
        energyTriangleUpperValue = 0.5;
        //
        
        noiseSynthesisWindowDurationInSeconds = 0.050f;
        noiseSynthesisTransitionOverlapInSeconds = 0.010f;
        harmonicSynthesisTransitionOverlapInSeconds = 0.002f;
        unvoicedVoicedTrackTransitionInSeconds = 0.005f;
        
        highpassFilterAfterNoiseSynthesis = true;
        
        writeSeparateHarmonicTracksToOutputs = false;
        normalizeHarmonicPartOutputWav = false;
        normalizeNoisePartOutputWav= false;
        normalizeOutputWav = false;
        
        writeHarmonicPartToSeparateFile = true;
        writeNoisePartToSeparateFile = true;
        writeTransientPartToSeparateFile = true;
        writeOriginalMinusHarmonicPartToSeparateFile = true;
        
        applyVocalTractPostNormalizationProcessor = false; 
    }
    
    public HntmSynthesizerParams(HntmSynthesizerParams existing)
    {
        harmonicPartSynthesisMethod = existing.harmonicPartSynthesisMethod;
        noisePartLpcSynthesisMethod = existing.noisePartLpcSynthesisMethod;
        
        applyTriangularNoiseEnvelopeForVoicedParts = existing.applyTriangularNoiseEnvelopeForVoicedParts;
        energyTriangleLowerValue = existing.energyTriangleLowerValue;
        energyTriangleUpperValue = existing.energyTriangleUpperValue;
        
        noiseSynthesisWindowDurationInSeconds = existing.noiseSynthesisWindowDurationInSeconds;
        noiseSynthesisTransitionOverlapInSeconds = existing.noiseSynthesisTransitionOverlapInSeconds;
        harmonicSynthesisTransitionOverlapInSeconds = existing.harmonicSynthesisTransitionOverlapInSeconds;
        unvoicedVoicedTrackTransitionInSeconds = existing.unvoicedVoicedTrackTransitionInSeconds;
        
        highpassFilterAfterNoiseSynthesis = existing.highpassFilterAfterNoiseSynthesis;
        
        writeSeparateHarmonicTracksToOutputs = existing.writeSeparateHarmonicTracksToOutputs;
        normalizeHarmonicPartOutputWav = existing.normalizeHarmonicPartOutputWav;
        normalizeNoisePartOutputWav= existing.normalizeNoisePartOutputWav;
        normalizeOutputWav = existing.normalizeOutputWav;
        
        writeHarmonicPartToSeparateFile = existing.writeHarmonicPartToSeparateFile;
        writeNoisePartToSeparateFile = existing.writeNoisePartToSeparateFile;
        writeTransientPartToSeparateFile = existing.writeTransientPartToSeparateFile;
        writeOriginalMinusHarmonicPartToSeparateFile = existing.writeOriginalMinusHarmonicPartToSeparateFile;
        
        applyVocalTractPostNormalizationProcessor = existing.applyVocalTractPostNormalizationProcessor; 
    }

}
