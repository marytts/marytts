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

import marytts.signalproc.analysis.RegularizedCepstrumEstimator;
import marytts.signalproc.analysis.RegularizedPostWarpedCepstrumEstimator;
import marytts.signalproc.analysis.RegularizedPreWarpedCepstrumEstimator;
import marytts.signalproc.window.GaussWindow;
import marytts.util.math.ArrayUtils;
import marytts.util.math.ComplexNumber;
import marytts.util.math.MathUtils;

/**
 * @author Oytun T&uuml;rk
 * 
 */
public class FrameHarmonicPart {
	public ComplexNumber[] complexAmps; // Keep complex amplitudes of harmonics

	public FrameHarmonicPart() {
		complexAmps = null;
	}

	public FrameHarmonicPart(FrameHarmonicPart existing) {
		this();

		if (existing != null) {
			complexAmps = ArrayUtils.copy(existing.complexAmps);
		}
	}

	public FrameHarmonicPart(DataInputStream dis, int numHarmonics) {
		complexAmps = null;

		if (numHarmonics > 0) {
			complexAmps = new ComplexNumber[numHarmonics];
			for (int i = 0; i < complexAmps.length; i++) {
				try {
					complexAmps[i] = new ComplexNumber(dis.readFloat(), dis.readFloat());
				} catch (IOException e) {
					complexAmps[i] = null;
				}
			}
		}
	}

	public FrameHarmonicPart(ByteBuffer bb, int numHarmonics) {
		complexAmps = null;

		if (numHarmonics > 0) {
			complexAmps = new ComplexNumber[numHarmonics];
			for (int i = 0; i < complexAmps.length; i++) {
				try {
					complexAmps[i] = new ComplexNumber(bb.getFloat(), bb.getFloat());
				} catch (Exception e) {
					complexAmps[i] = null;
				}
			}
		}
	}

	public boolean equals(FrameHarmonicPart other) {
		if (complexAmps != null || other.complexAmps != null) {
			if (complexAmps != null && other.complexAmps == null)
				return false;
			if (complexAmps == null && other.complexAmps != null)
				return false;
			if (complexAmps.length != other.complexAmps.length)
				return false;
			for (int i = 0; i < complexAmps.length; i++)
				if (!complexAmps[i].equals(other.complexAmps[i]))
					return false;
		}

		return true;
	}

	public int getLength() {
		int len = 0;
		if (complexAmps != null && complexAmps.length > 0)
			len = complexAmps.length;

		return 4 * 2 * len;
	}

	public void write(DataOutput out) throws IOException {
		int numHarmonics = 0;
		if (complexAmps != null && complexAmps.length > 0)
			numHarmonics = complexAmps.length;

		if (numHarmonics > 0) {
			for (int i = 0; i < complexAmps.length; i++) {
				out.writeFloat(complexAmps[i].real);
				out.writeFloat(complexAmps[i].imag);
			}
		}
	}

	public float[] getCeps(double f0InHz, int samplingRateInHz, HntmAnalyzerParams params) {
		float[] ceps = null;

		if (complexAmps != null) {
			double[] linearAmps = new double[complexAmps.length];
			double[] freqsInHz = new double[complexAmps.length];

			int j;
			for (j = 0; j < complexAmps.length; j++) {
				freqsInHz[j] = (j + 1) * f0InHz;
				linearAmps[j] = MathUtils.magnitudeComplex(complexAmps[j]);
			}
			//

			double[] harmonicWeights = null;
			if (params.useWeightingInRegularizedCepstrumEstimationHarmonic) {
				GaussWindow g = new GaussWindow(2 * linearAmps.length);
				g.normalizeRange(0.1f, 1.0f);
				harmonicWeights = g.getCoeffsRightHalf();
			}

			if (params.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_PRE_BARK_WARPING)
				ceps = RegularizedPreWarpedCepstrumEstimator.freqsLinearAmps2cepstrum(linearAmps, freqsInHz, samplingRateInHz,
						params.harmonicPartCepstrumOrder, harmonicWeights, params.regularizedCepstrumLambdaHarmonic);
			else if (params.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING)
				ceps = RegularizedPostWarpedCepstrumEstimator.freqsLinearAmps2cepstrum(linearAmps, freqsInHz, samplingRateInHz,
						params.harmonicPartCepstrumOrderPreMel, params.harmonicPartCepstrumOrder, harmonicWeights,
						params.regularizedCepstrumLambdaHarmonic);
		}

		return ceps;
	}

	public double[] getLinearAmps() {
		if (complexAmps != null)
			return MathUtils.magnitudeComplex(complexAmps);
		else
			return null;
	}

	public double[] getDBAmps() {
		return MathUtils.amp2db(getLinearAmps());
	}

	public double[] getPhasesInRadians() {
		double[] phasesInRadians = null;
		if (complexAmps != null) {
			phasesInRadians = new double[complexAmps.length];
			for (int k = 0; k < complexAmps.length; k++)
				phasesInRadians[k] = MathUtils.phaseInRadians(complexAmps[k]);
		}

		return phasesInRadians;
	}
}
