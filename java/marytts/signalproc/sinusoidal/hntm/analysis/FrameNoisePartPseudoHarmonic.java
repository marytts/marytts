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

import marytts.util.math.ArrayUtils;

/**
 * An alternative model for the noise part of a given speech frame.
 * Fullband harmonic parameters are stored (amplitudes only) at a constant "virtual" f0.
 * Cepstral amplitudes are kept only.
 * Synthesis handles noise part generation using the cepstral amplitudes and random phase generation above maximum frequency of voicing
 * 
 * @author Oytun T&uumlrk
 *
 */
public class FrameNoisePartPseudoHarmonic extends FrameNoisePart {

    public double[] ceps; //To keep harmonic amplitudes
    
    public FrameNoisePartPseudoHarmonic()
    {
        super();
    }
    
    public FrameNoisePartPseudoHarmonic(FrameNoisePartPseudoHarmonic existing)
    {
        super();
        ceps = ArrayUtils.copy(existing.ceps);
    }
}

