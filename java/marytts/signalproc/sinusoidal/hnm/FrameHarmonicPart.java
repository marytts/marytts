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

import marytts.util.math.ArrayUtils;
import marytts.util.math.ComplexNumber;

/**
 * @author Oytun T&uumlrk
 *
 */
public class FrameHarmonicPart 
{
    public double[] ceps; //Cepstral coefficients for amplitude envelope
    public float[] phases; //To keep harmonic phases
    
    public FrameHarmonicPart()
    {        
        ceps = null;
        phases = null;
    }
    
    public FrameHarmonicPart(FrameHarmonicPart existing)
    {        
        ceps = ArrayUtils.copy(existing.ceps);
        phases = ArrayUtils.copyf(existing.phases);
    }
}

