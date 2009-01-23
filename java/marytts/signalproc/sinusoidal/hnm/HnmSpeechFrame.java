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
package marytts.signalproc.sinusoidal.hnm;

import marytts.signalproc.sinusoidal.BaseSinusoidalSpeechFrame;

/**
 * @author oytun.turk
 *
 */
public class HnmSpeechFrame extends BaseSinusoidalSpeechFrame 
{
    FrameHarmonicPart h; //Harmonics component (lower frequencies which are less than maximum frequency of voicing)
    FrameNoisePart n; //Noise component (upper frequencies)
    
    float maximumFrequencyOfVoicingInHz; //If 0.0, then the frame is unvoiced
    float tAnalysisInSeconds; //Middle of analysis frame in seconds
    
    public HnmSpeechFrame()
    {
        h = new FrameHarmonicPart();
        n = new FrameNoisePart();
        maximumFrequencyOfVoicingInHz = 0.0f;
        tAnalysisInSeconds = -1.0f;
    }
}

