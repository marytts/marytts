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

import marytts.signalproc.sinusoidal.BaseSinusoidalSpeechSignal;

/**
 * 
 * 
 * @author Oytun T&uumlrk
 *
 */
public class HnmSpeechSignal extends BaseSinusoidalSpeechSignal 
{    
    public HnmSpeechFrame[] frames;
    public float originalDurationInSeconds;
    public int samplingRateInHz;
    public float windowDurationInSecondsNoise;
    public float preCoefNoise;
    
    public HnmSpeechSignal(int totalFrm, int samplingRateInHz, float originalDurationInSeconds, float windowDurationInSecondsNoise, float preCoefNoise)
    {
        if (totalFrm>0)
        {
            frames =  new HnmSpeechFrame[totalFrm];
            for (int i=00; i<totalFrm; i++)
                frames[i] = new HnmSpeechFrame();
        }
        else
            frames = null;
        
        this.samplingRateInHz = samplingRateInHz;
        this.originalDurationInSeconds = originalDurationInSeconds;
        this.windowDurationInSecondsNoise = windowDurationInSecondsNoise;
        this.preCoefNoise = preCoefNoise;
    }
}

