/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.signalproc.sinusoidal.hntm.analysis;

import marytts.signalproc.sinusoidal.BaseSinusoidalSpeechFrame;

/**
 * @author oytun.turk
 *
 */
public class HntmSpeechFrame extends BaseSinusoidalSpeechFrame 
{
    public FrameHarmonicPart h; //Harmonics component (lower frequencies which are less than maximum frequency of voicing)
    public FrameNoisePart n; //Noise component (upper frequencies)
    
    public float f0InHz;
    public float maximumFrequencyOfVoicingInHz; //If 0.0, then the frame is unvoiced
    public float tAnalysisInSeconds; //Middle of analysis frame in seconds
    public float origAverageSampleEnergy;
    
    public HntmSpeechFrame()
    {
        this(0.0f);
    }
    
    public HntmSpeechFrame(float f0InHzIn)
    {
        h = new FrameHarmonicPart();
        n = new FrameNoisePart();
        f0InHz = f0InHzIn;
        maximumFrequencyOfVoicingInHz = 0.0f;
        tAnalysisInSeconds = -1.0f;
        origAverageSampleEnergy = 0.0f;
    }
    
    public HntmSpeechFrame(HntmSpeechFrame existing)
    {
        h = new FrameHarmonicPart(existing.h);
        if (existing.n instanceof FrameNoisePartLpc)
            n = new FrameNoisePartLpc((FrameNoisePartLpc)existing.n);
        else if (existing.n instanceof FrameNoisePartRegularizedCeps)
            n = new FrameNoisePartRegularizedCeps((FrameNoisePartRegularizedCeps)existing.n);
        else if (existing.n instanceof FrameNoisePartPseudoHarmonic)
            n = new FrameNoisePartPseudoHarmonic((FrameNoisePartPseudoHarmonic)existing.n);
        else if (existing.n instanceof FrameNoisePartWaveform)
            n = new FrameNoisePartWaveform((FrameNoisePartWaveform)existing.n);
        
        f0InHz = existing.f0InHz;
        maximumFrequencyOfVoicingInHz = existing.maximumFrequencyOfVoicingInHz;
        tAnalysisInSeconds = existing.tAnalysisInSeconds;   
        origAverageSampleEnergy = existing.origAverageSampleEnergy;
    }
}

