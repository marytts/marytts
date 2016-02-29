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
 * LPC based noise modeling for a given speech frame Full spectrum LP coefficients and LP gain are used Synthesis handles noise
 * generation for upper frequencies(i.e. frequencies larger than maximum voicing freq.)
 * 
 * @author oytun.turk
 * 
 */
public class FrameNoisePartLpc implements FrameNoisePart {

	public float[] lpCoeffs;
	public float lpGain;
	public float origAverageSampleEnergy;
	public float origNoiseStd;

	public FrameNoisePartLpc() {
		super();

		lpCoeffs = null;
		lpGain = 0.0f;
		origAverageSampleEnergy = 0.0f;
		origNoiseStd = 1.0f;
	}

	public FrameNoisePartLpc(FrameNoisePartLpc existing) {
		super();

		origAverageSampleEnergy = existing.origAverageSampleEnergy;
		origNoiseStd = existing.origNoiseStd;

		setLpCoeffs(existing.lpCoeffs, existing.lpGain);
	}

	public FrameNoisePartLpc(DataInputStream dis, int numLpcs) {
		this();

		if (numLpcs > 0) {
			lpCoeffs = new float[numLpcs];
			for (int i = 0; i < numLpcs; i++) {
				try {
					lpCoeffs[i] = dis.readFloat();
				} catch (IOException e) {
					System.out.println("Error! At least " + String.valueOf(numLpcs) + " LP coefficients required!");
				}
			}

			try {
				lpGain = dis.readFloat();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				origAverageSampleEnergy = dis.readFloat();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				origNoiseStd = dis.readFloat();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public FrameNoisePartLpc(ByteBuffer bb, int numLpcs) {
		this();

		if (numLpcs > 0) {
			lpCoeffs = new float[numLpcs];
			for (int i = 0; i < numLpcs; i++) {
				try {
					lpCoeffs[i] = bb.getFloat();
				} catch (Exception e) {
					throw new IllegalArgumentException("At least " + String.valueOf(numLpcs) + " LP coefficients required!", e);
				}
			}

			lpGain = bb.getFloat();
			origAverageSampleEnergy = bb.getFloat();
			origNoiseStd = bb.getFloat();
		}
	}

	public void write(DataOutput out) throws IOException {
		int numLpcs = 0;
		if (lpCoeffs != null && lpCoeffs.length > 0)
			numLpcs = lpCoeffs.length;

		out.writeInt(numLpcs);

		if (numLpcs > 0) {
			for (int i = 0; i < lpCoeffs.length; i++)
				out.writeFloat(lpCoeffs[i]);

			out.writeFloat(lpGain);
			out.writeFloat(origAverageSampleEnergy);
			out.writeFloat(origNoiseStd);
		}
	}

	public boolean equals(FrameNoisePartLpc other) {
		if (lpGain != other.lpGain)
			return false;
		if (origAverageSampleEnergy != other.origAverageSampleEnergy)
			return false;
		if (origNoiseStd != other.origNoiseStd)
			return false;

		if (lpCoeffs != null || other.lpCoeffs != null) {
			if (lpCoeffs != null && other.lpCoeffs == null)
				return false;
			if (lpCoeffs == null && other.lpCoeffs != null)
				return false;
			if (lpCoeffs.length != other.lpCoeffs.length)
				return false;
			for (int i = 0; i < lpCoeffs.length; i++)
				if (lpCoeffs[i] != other.lpCoeffs[i])
					return false;
		}

		return true;
	}

	public int getVectorSize() {
		int lpLen = 0;
		if (lpCoeffs != null && lpCoeffs.length > 0)
			lpLen = lpCoeffs.length;

		return lpLen;
	}

	public int getLength() {
		return 4 * (getVectorSize() + 3);
	}

	public void setLpCoeffs(float[] lpCoeffsIn, float gainIn) {
		lpCoeffs = ArrayUtils.copy(lpCoeffsIn);
		lpGain = gainIn;
	}

	public void setLpCoeffs(double[] lpCoeffsIn, float gainIn) {
		lpCoeffs = ArrayUtils.copyDouble2Float(lpCoeffsIn);
		lpGain = gainIn;
	}
}
