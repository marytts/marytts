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

package marytts.signalproc.adaptation.prosody;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * A basic class that contains prosody modification information and corresponding time instants
 * 
 * @author oytun.turk
 * 
 */
public class BasicProsodyModifierParams implements Serializable {
	public float[] tScales; // Time scale factors
	public float[] tScalesTimes; // Instants that the time scale factors are effective.
									// For the time instants in between, linear interpolation is used to estimate corresponding
									// time scaling factor.
	public float[] pScales; // Pitch scale factors
	public float[] pScalesTimes; // Instants that the pitch scale factors are effective.
									// For the time instants in between, linear interpolation is used to estimate corresponding
									// pitch scaling factor.

	public BasicProsodyModifierParams() {
		// this has no effect:
		// tScales = null;
		// tScalesTimes = null;
		// pScales = null;
		// pScalesTimes = null;
	}

	public BasicProsodyModifierParams(BasicProsodyModifierParams existing) {
		setTScales(existing.tScales);
		setTScalesTimes(existing.tScalesTimes);
		setPScales(existing.pScales);
		setPScalesTimes(existing.pScalesTimes);
	}

	public BasicProsodyModifierParams(float[] tScalesIn, float[] tScalesTimesIn) {
		this();

		setTScales(tScalesIn);
		setTScalesTimes(tScalesTimesIn);
	}

	public BasicProsodyModifierParams(float[] tScalesIn, float[] tScalesTimesIn, float[] pScalesIn, float[] pScalesTimesIn) {
		this();

		setTScales(tScalesIn);
		setTScalesTimes(tScalesTimesIn);
		setPScales(pScalesIn);
		setPScalesTimes(pScalesTimesIn);
	}

	// Estimate time and duration scaling factors from source and target labels and pitch contours
	// This version assumes identical source and target labels
	public BasicProsodyModifierParams(String sourcePtcFile, String sourceLabelFile, String targetPtcFile, String targetLabelFile,
			boolean isPitchScale, boolean isTimeScale) throws IOException {
		PitchReaderWriter f0Src = new PitchReaderWriter(sourcePtcFile);
		Labels labSrc = new Labels(sourceLabelFile);
		PitchReaderWriter f0Tgt = new PitchReaderWriter(targetPtcFile);
		Labels labTgt = new Labels(targetLabelFile);

		init(f0Src, labSrc, f0Tgt, labTgt, isPitchScale, isTimeScale);
	}

	// Estimate time and duration scaling factors from source and target labels and pitch contours
	// This version assumes identical source and target labels
	public BasicProsodyModifierParams(PitchReaderWriter f0Src, Labels labSrc, PitchReaderWriter f0Tgt, Labels labTgt,
			boolean isPitchScale, boolean isTimeScale) {
		init(f0Src, labSrc, f0Tgt, labTgt, isPitchScale, isTimeScale);
	}

	public void init(PitchReaderWriter f0Src, Labels labSrc, PitchReaderWriter f0Tgt, Labels labTgt, boolean isPitchScale,
			boolean isTimeScale) {
		int numLabels = 0;
		if (labSrc != null && labTgt != null && labSrc.items != null && labTgt.items != null)
			numLabels = Math.min(labSrc.items.length, labTgt.items.length);

		if (isTimeScale && numLabels > 0) {
			tScales = new float[numLabels];
			tScalesTimes = new float[numLabels];
			float tStartSrc = 0.0f;
			float tStartTgt = 0.0f;
			for (int i = 0; i < numLabels; i++) {
				tScales[i] = (float) ((labTgt.items[i].time - tStartTgt) / (labSrc.items[i].time - tStartSrc));
				tScalesTimes[i] = (float) (0.5 * (tStartSrc + labSrc.items[i].time));
				tStartTgt = (float) labTgt.items[i].time;
				tStartSrc = (float) labSrc.items[i].time;
			}
		} else {
			setTScales(1.0f);
			setTScalesTimes(null);
		}

		if (isPitchScale) {
			pScales = new float[f0Src.header.numfrm];
			pScalesTimes = new float[f0Src.header.numfrm];

			float tStartSrc = 0.0f;
			float tStartTgt = 0.0f;
			int labInd;
			double sourceTime, targetTime;
			int tgtF0Ind;

			for (int i = 0; i < f0Src.header.numfrm; i++) {
				sourceTime = (i * f0Src.header.skipSizeInSeconds + 0.5 * f0Src.header.windowSizeInSeconds);

				if (labSrc != null && labTgt != null && labSrc.items != null && labTgt.items != null) {
					labInd = SignalProcUtils.frameIndex2LabelIndex(i, labSrc, f0Src.header.windowSizeInSeconds,
							f0Src.header.skipSizeInSeconds);
					if (labInd > 1) {
						tStartTgt = (float) labTgt.items[labInd - 1].time;
						tStartSrc = (float) labSrc.items[labInd - 1].time;
					} else {
						tStartSrc = 0.0f;
						tStartTgt = 0.0f;
					}

					targetTime = MathUtils.linearMap(sourceTime, tStartSrc, labSrc.items[labInd].time, tStartTgt,
							labTgt.items[labInd].time);
					tgtF0Ind = SignalProcUtils.time2frameIndex(targetTime, f0Tgt.header.windowSizeInSeconds,
							f0Tgt.header.skipSizeInSeconds);

				} else
					// No labels given, just do a linear mapping between given source and target contours
					tgtF0Ind = MathUtils.linearMap(i, 0, f0Src.contour.length - 1, 0, f0Tgt.contour.length - 1);

				if (f0Src.contour[i] > 10.0f && f0Tgt.contour[tgtF0Ind] > 10.0f)
					pScales[i] = (float) (f0Tgt.contour[tgtF0Ind] / f0Src.contour[i]);
				else
					pScales[i] = 1.0f;

				pScalesTimes[i] = (float) sourceTime;
			}
		} else {
			setPScales(1.0f);
			setPScalesTimes(null);
		}
	}

	public void setTScales(float x) {
		tScales = new float[1];
		tScales[0] = x;
	}

	public void setTScales(float[] x) {
		tScales = ArrayUtils.copy(x);
	}

	public void setTScalesTimes(float x) {
		tScalesTimes = new float[1];
		tScalesTimes[0] = x;
	}

	public void setTScalesTimes(float[] x) {
		tScalesTimes = ArrayUtils.copy(x);
	}

	public void setPScales(float x) {
		pScales = new float[1];
		pScales[0] = x;
	}

	public void setPScales(float[] x) {
		pScales = ArrayUtils.copy(x);
	}

	public void setPScalesTimes(float x) {
		pScalesTimes = new float[1];
		pScalesTimes[0] = x;
	}

	public void setPScalesTimes(float[] x) {
		pScalesTimes = ArrayUtils.copy(x);
	}

	public boolean willProsodyBeModified() {
		int i;
		if (pScales != null) {
			for (i = 0; i < pScales.length; i++) {
				if (pScales[i] != 1.0f)
					return true;
			}
		}

		if (tScales != null) {
			for (i = 0; i < tScales.length; i++) {
				if (tScales[i] != 1.0f)
					return true;
			}
		}

		return false;
	}

	public void writeObject(String fileName) throws IOException {
		try {
			File file = new File(fileName);
			FileOutputStream fos = new FileOutputStream(fileName);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this);
			oos.close();
		} catch (IOException e) {
			throw e;
		}
	}

	public static BasicProsodyModifierParams readObject(String fileName) throws IOException, ClassNotFoundException {
		try {
			File file = new File(fileName);
			FileInputStream fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);
			BasicProsodyModifierParams bpmp = (BasicProsodyModifierParams) ois.readObject();
			ois.close();
			return bpmp;
		} catch (IOException e) {
			throw e;
		} catch (ClassNotFoundException e) {
			throw e;
		}
	}
}
