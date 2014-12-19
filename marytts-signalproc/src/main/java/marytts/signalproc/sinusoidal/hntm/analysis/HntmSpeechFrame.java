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
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

import marytts.signalproc.sinusoidal.BaseSinusoidalSpeechFrame;

/**
 * @author oytun.turk
 * 
 */
public class HntmSpeechFrame extends BaseSinusoidalSpeechFrame {
	public FrameHarmonicPart h; // Harmonics component (lower frequencies which are less than maximum frequency of voicing)
	public FrameNoisePart n; // Noise component (upper frequencies)

	public float f0InHz;
	public float maximumFrequencyOfVoicingInHz; // If 0.0, then the frame is unvoiced
	public float tAnalysisInSeconds; // Current analysis instant (middle of window) in seconds
	public float deltaAnalysisTimeInSeconds; // Difference between middle of next analysis frame and current analysis frame in
												// seconds

	public HntmSpeechFrame() {
		this(0.0f);
	}

	public HntmSpeechFrame(float f0InHzIn) {
		h = null;
		n = null;
		f0InHz = f0InHzIn;
		maximumFrequencyOfVoicingInHz = 0.0f;
		tAnalysisInSeconds = -1.0f;
		deltaAnalysisTimeInSeconds = 0.0f;
	}

	public HntmSpeechFrame(HntmSpeechFrame existing) {
		this();

		if (existing != null) {
			if (existing.h != null)
				h = new FrameHarmonicPart(existing.h);

			if (existing.n != null) {
				if (existing.n instanceof FrameNoisePartLpc)
					n = new FrameNoisePartLpc((FrameNoisePartLpc) existing.n);
				else if (existing.n instanceof FrameNoisePartPseudoHarmonic)
					n = new FrameNoisePartPseudoHarmonic((FrameNoisePartPseudoHarmonic) existing.n);
				else if (existing.n instanceof FrameNoisePartWaveform)
					n = new FrameNoisePartWaveform((FrameNoisePartWaveform) existing.n);

				f0InHz = existing.f0InHz;
				maximumFrequencyOfVoicingInHz = existing.maximumFrequencyOfVoicingInHz;
				tAnalysisInSeconds = existing.tAnalysisInSeconds;
				deltaAnalysisTimeInSeconds = existing.deltaAnalysisTimeInSeconds;
			}
		}
	}

	public HntmSpeechFrame(DataInputStream dis, int noiseModel) throws IOException, EOFException {
		this();

		f0InHz = dis.readFloat();
		maximumFrequencyOfVoicingInHz = dis.readFloat();
		tAnalysisInSeconds = dis.readFloat();
		deltaAnalysisTimeInSeconds = dis.readFloat();

		int numHarmonics = dis.readInt();
		if (numHarmonics > 0)
			h = new FrameHarmonicPart(dis, numHarmonics);

		int vectorSize = dis.readInt();
		if (noiseModel == HntmAnalyzerParams.LPC) {
			n = new FrameNoisePartLpc(dis, vectorSize);
			if (((FrameNoisePartLpc) n).lpCoeffs == null)
				n = null;
		} else if (noiseModel == HntmAnalyzerParams.PSEUDO_HARMONIC) {
			n = new FrameNoisePartPseudoHarmonic(dis, vectorSize);
			if (((FrameNoisePartPseudoHarmonic) n).ceps == null)
				n = null;
		} else if (noiseModel == HntmAnalyzerParams.WAVEFORM) {
			n = new FrameNoisePartWaveform(dis, vectorSize);
			if (((FrameNoisePartWaveform) n).waveform == null)
				n = null;
		}
	}

	public HntmSpeechFrame(ByteBuffer bb, int noiseModel) throws IOException, EOFException {
		this();

		f0InHz = bb.getFloat();
		maximumFrequencyOfVoicingInHz = bb.getFloat();
		tAnalysisInSeconds = bb.getFloat();
		deltaAnalysisTimeInSeconds = bb.getFloat();

		int numHarmonics = bb.getInt();
		if (numHarmonics > 0)
			h = new FrameHarmonicPart(bb, numHarmonics);

		int vectorSize = bb.getInt();
		if (noiseModel == HntmAnalyzerParams.LPC) {
			n = new FrameNoisePartLpc(bb, vectorSize);
			if (((FrameNoisePartLpc) n).lpCoeffs == null)
				n = null;
		} else if (noiseModel == HntmAnalyzerParams.PSEUDO_HARMONIC) {
			n = new FrameNoisePartPseudoHarmonic(bb, vectorSize);
			if (((FrameNoisePartPseudoHarmonic) n).ceps == null)
				n = null;
		} else if (noiseModel == HntmAnalyzerParams.WAVEFORM) {
			n = new FrameNoisePartWaveform(bb, vectorSize);
			if (((FrameNoisePartWaveform) n).waveform == null)
				n = null;
		}
	}

	public void write(DataOutput out) throws IOException {
		out.writeFloat(f0InHz);
		out.writeFloat(maximumFrequencyOfVoicingInHz);
		out.writeFloat(tAnalysisInSeconds);
		out.writeFloat(deltaAnalysisTimeInSeconds);

		int numHarmonics = 0;
		if (h != null && h.complexAmps != null)
			numHarmonics = h.complexAmps.length;

		out.writeInt(numHarmonics);

		if (h != null)
			h.write(out);

		int vectorSize = 0;
		if (n != null)
			vectorSize = n.getVectorSize();

		out.writeInt(vectorSize);

		if (n != null)
			n.write(out);
	}

	public boolean equals(HntmSpeechFrame other) {
		if (!h.equals(other.h))
			return false;
		if (!n.equals(other.n))
			return false;
		if (f0InHz != other.f0InHz)
			return false;
		if (maximumFrequencyOfVoicingInHz != other.maximumFrequencyOfVoicingInHz)
			return false;
		if (tAnalysisInSeconds != other.tAnalysisInSeconds)
			return false;
		if (deltaAnalysisTimeInSeconds != other.deltaAnalysisTimeInSeconds)
			return false;

		return true;
	}

	// Returns the size of this object in bytes
	public int getLength() {
		int len = 4 * (4 + 1 + 1);
		if (h != null)
			len += h.getLength();
		if (n != null)
			len += n.getLength();

		return len;
	}
}
