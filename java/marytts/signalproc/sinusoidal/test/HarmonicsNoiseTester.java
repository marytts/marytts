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
package marytts.signalproc.sinusoidal.test;

import java.io.IOException;

import marytts.signalproc.sinusoidal.Sinusoid;
import marytts.util.io.FileUtils;
import marytts.util.math.MathUtils;

/**
 * @author oytun.turk
 *
 */
public class HarmonicsNoiseTester extends SinusoidsNoiseTester {

    public HarmonicsNoiseTester(HarmonicsTester s, NoiseTester n) {
        super(s, n);
        // TODO Auto-generated constructor stub
    }
    
    public static void main(String[] args) throws IOException
    { 
        int i;
        HarmonicsTester s = null;
        NoiseTester n = null;
        HarmonicsNoiseTester h = null;
        
        //Harmonics part
        float f1 = 400.f;
        int numHarmonics = 8;
        float harmonicsStartTimeInSeconds = 0.0f;
        float harmonicsEndTimeInSeconds = 1.0f;
        s = new HarmonicsTester(f1, numHarmonics, harmonicsStartTimeInSeconds, harmonicsEndTimeInSeconds);
        //
        
        //Noise part
        int numNoises = 1;
        float [][] freqs = new float[numNoises][];
        float [] amps = new float[numNoises];
        float noiseStartTimeInSeconds = 0.7f;
        float noiseEndTimeInSeconds = 1.5f;
        for (i=0; i<numNoises; i++)
            freqs[i] = new float[2];
        
        freqs[0][0] = 4000;
        freqs[0][1] = 6000;
        amps[0] = DEFAULT_AMP;

        n = new NoiseTester(freqs, amps, noiseStartTimeInSeconds, noiseEndTimeInSeconds);
        //
        
        h = new HarmonicsNoiseTester(s, n);
        
        if (args.length>1)
            h.write(args[0], args[1]);
        else
            h.write(args[0]);
    }

}

