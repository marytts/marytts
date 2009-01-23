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

import marytts.util.math.ComplexNumber;

/**
 * @author Oytun T&uumlrk
 *
 */
public class FrameHarmonicPart 
{
    public double[] ceps; //Cepstral coefficients for amplitude envelope
    public float[] phases; //To keep harmonic phases
    public float f0InHz;
    
    public FrameHarmonicPart()
    {        
        this(-1.0f);
    }
    
    public FrameHarmonicPart(float f0InHzIn)
    {        
        f0InHz = f0InHzIn;
        ceps = null;
        phases = null;
    }
}

