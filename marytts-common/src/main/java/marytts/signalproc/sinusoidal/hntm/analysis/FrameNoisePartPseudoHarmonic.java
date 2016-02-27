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

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

import marytts.util.math.ArrayUtils;

/**
 * An alternative model for the noise part of a given speech frame. Fullband harmonic parameters are stored (amplitudes only) at a
 * constant "virtual" f0. Cepstral amplitudes are kept only. Synthesis handles noise part generation using the cepstral amplitudes
 * and random phase generation above maximum frequency of voicing
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class FrameNoisePartPseudoHarmonic implements FrameNoisePart {

	public float[] ceps; // To keep harmonic amplitudes

	public FrameNoisePartPseudoHarmonic() {
		super();

		ceps = null;
	}

	public FrameNoisePartPseudoHarmonic(FrameNoisePartPseudoHarmonic existing) {
		this();

		ceps = ArrayUtils.copy(existing.ceps);
	}

	public FrameNoisePartPseudoHarmonic(DataInputStream dis, int cepsLen) {
		this();

		if (cepsLen > 0) {
			ceps = new float[cepsLen];
			for (int i = 0; i < cepsLen; i++) {
				try {
					ceps[i] = dis.readFloat();
				} catch (IOException e) {
					System.out.println("Error! At least " + String.valueOf(cepsLen) + " cepstrum coefficients required!");
				}
			}
		}
	}

	public FrameNoisePartPseudoHarmonic(ByteBuffer bb, int cepsLen) {
		this();

		if (cepsLen > 0) {
			ceps = new float[cepsLen];
			for (int i = 0; i < cepsLen; i++) {
				try {
					ceps[i] = bb.getFloat();
				} catch (Exception e) {
					throw new IllegalArgumentException(
							"At least " + String.valueOf(cepsLen) + " cepstrum coefficients required!", e);
				}
			}
		}
	}

	public void write(DataOutput out) throws IOException {
		int cepsLen = 0;
		if (ceps != null && ceps.length > 0)
			cepsLen = ceps.length;

		if (cepsLen > 0) {
			for (int i = 0; i < ceps.length; i++)
				out.writeFloat(ceps[i]);
		}
	}

	public boolean equals(FrameNoisePartPseudoHarmonic other) {
		if (ceps != null || other.ceps != null) {
			if (ceps != null && other.ceps == null)
				return false;
			if (ceps == null && other.ceps != null)
				return false;
			if (ceps.length != other.ceps.length)
				return false;
			for (int i = 0; i < ceps.length; i++)
				if (ceps[i] != other.ceps[i])
					return false;
		}

		return true;
	}

	public int getVectorSize() {
		int cepsLen = 0;
		if (ceps != null && ceps.length > 0)
			cepsLen = ceps.length;

		return cepsLen;
	}

	public int getLength() {
		return 4 * getVectorSize();
	}
}
