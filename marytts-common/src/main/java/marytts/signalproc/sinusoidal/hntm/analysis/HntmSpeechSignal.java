/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package marytts.signalproc.sinusoidal.hntm.analysis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import marytts.util.math.ArrayUtils;

/**
 * @author oytun.turk
 * 
 */
public class HntmSpeechSignal {
	public HntmSpeechFrame[] frames;
	public int samplingRateInHz;
	public float originalDurationInSeconds;

	public HntmSpeechSignal(HntmSpeechSignal existing) {
		frames = null;
		if (existing != null) {
			if (existing.frames != null) {
				frames = new HntmSpeechFrame[existing.frames.length];
				for (int i = 0; i < existing.frames.length; i++)
					frames[i] = new HntmSpeechFrame(existing.frames[i]);
			}

			this.samplingRateInHz = existing.samplingRateInHz;
			this.originalDurationInSeconds = existing.originalDurationInSeconds;
		}
	}

	public HntmSpeechSignal(int totalFrm, int samplingRateInHz, float originalDurationInSeconds) {
		if (totalFrm > 0) {
			frames = new HntmSpeechFrame[totalFrm];
			for (int i = 0; i < totalFrm; i++)
				frames[i] = new HntmSpeechFrame();
		} else
			frames = null;

		this.samplingRateInHz = samplingRateInHz;

		this.originalDurationInSeconds = originalDurationInSeconds;
	}

	public HntmSpeechSignal(String binaryFile, int noiseModel) {
		try {
			read(binaryFile, noiseModel);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void write(String binaryFile) throws IOException {
		DataOutputStream d = new DataOutputStream(new FileOutputStream(new File(binaryFile)));

		write(d);
	}

	public void write(DataOutputStream d) throws IOException {
		int totalFrm = 0;
		if (frames != null && frames.length > 0)
			totalFrm = frames.length;

		d.writeInt(totalFrm);

		if (totalFrm > 0) {
			for (int i = 0; i < totalFrm; i++)
				frames[i].write(d);
		}

		d.writeInt(samplingRateInHz);
		d.writeFloat(originalDurationInSeconds);
	}

	public void read(String binaryFile, int noiseModel) throws IOException {
		DataInputStream d = new DataInputStream(new FileInputStream(new File(binaryFile)));

		read(d, noiseModel);
	}

	public void read(DataInputStream d, int noiseModel) throws IOException {
		int totalFrm = d.readInt();
		frames = null;

		if (totalFrm > 0) {
			frames = new HntmSpeechFrame[totalFrm];
			for (int i = 0; i < totalFrm; i++)
				frames[i] = new HntmSpeechFrame(d, noiseModel);
		}

		samplingRateInHz = d.readInt();

		originalDurationInSeconds = d.readFloat();
	}

	public float[] getAnalysisTimes() {
		float[] times = null;

		if (frames != null) {
			times = new float[frames.length];
			for (int i = 0; i < frames.length; i++)
				times[i] = frames[i].tAnalysisInSeconds;
		}

		return times;
	}

	// Returns track segments for a given harmonic. Each segment corresponds to a voiced segment
	public double[][] getPhasesInRadians() {
		double[][] phases = null;

		if (frames != null && frames.length > 0) {
			phases = new double[frames.length][];

			for (int i = 0; i < frames.length; i++) {
				if (frames[i].h != null)
					phases[i] = frames[i].h.getPhasesInRadians();
			}
		}

		return phases;
	}

	public float[][] getLpcsAll() {
		float[][] lpcsAll = null;

		if (frames != null && frames.length > 0) {
			lpcsAll = new float[frames.length][];
			for (int i = 0; i < frames.length; i++) {
				if (frames[i].n instanceof FrameNoisePartLpc)
					lpcsAll[i] = ArrayUtils.copy(((FrameNoisePartLpc) frames[i].n).lpCoeffs);
			}
		}

		return lpcsAll;
	}

	public float[] getLpcGainsAll() {
		float[] gainsAll = null;

		if (frames != null && frames.length > 0) {
			gainsAll = new float[frames.length];
			for (int i = 0; i < frames.length; i++) {
				if (frames[i].n instanceof FrameNoisePartLpc)
					gainsAll[i] = ((FrameNoisePartLpc) frames[i].n).lpGain;
				else
					gainsAll[i] = -1.0f;
			}
		}

		return gainsAll;
	}

	public float[] getOrigNoiseStds() {
		float[] origNoiseStdsAll = null;

		if (frames != null && frames.length > 0) {
			origNoiseStdsAll = new float[frames.length];
			for (int i = 0; i < frames.length; i++) {
				if (frames[i].n != null && (frames[i].n instanceof FrameNoisePartLpc))
					origNoiseStdsAll[i] = ((FrameNoisePartLpc) frames[i].n).origNoiseStd;
			}
		}

		return origNoiseStdsAll;
	}

	public double[] getMaximumFrequencyOfVoicings() {
		double[] maximumFrequencyOfVoicings = null;

		if (frames != null && frames.length > 0) {
			maximumFrequencyOfVoicings = new double[frames.length];

			for (int i = 0; i < frames.length; i++)
				maximumFrequencyOfVoicings[i] = frames[i].maximumFrequencyOfVoicingInHz;
		}

		return maximumFrequencyOfVoicings;
	}

	public int getTotalFrames() {
		if (frames != null && frames.length > 0)
			return frames.length;
		else
			return 0;
	}
}
