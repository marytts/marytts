/**
 * Copyright 2004-2006 DFKI GmbH.
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
package marytts.signalproc.process;

import java.io.File;
import java.io.FileReader;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.analysis.LpcAnalyser;
import marytts.signalproc.analysis.LpcAnalyser.LpCoeffs;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.data.text.LabelfileDoubleDataSource;
import marytts.util.io.FileUtils;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class LSFInterpolator extends LPCAnalysisResynthesis implements InlineFrameMerger {
	protected double[] otherFrame1;
	protected double[] otherFrame2;
	protected double relativeWeightOther1;
	protected double r;

	/**
	 * Create an LSF-based interpolator.
	 *
	 * @param p
	 *            the order of LPC analysis
	 * @param r
	 *            the interpolation ratio, between 0 and 1: <code>new = r * this + (1-r) * other</code>
	 */
	public LSFInterpolator(int p, double r) {
		super(p);
		if (r < 0 || r > 1)
			throw new IllegalArgumentException("Mixing ratio r must be between 0 and 1");
		this.r = r;
	}

	/**
	 * Set the frame of data to merge into the next call of applyInline(). This is the data towards which LSF-based interpolation
	 * will be done.
	 *
	 * @param frameToMerge
	 *            frame to merge
	 */
	public void setFrameToMerge(double[] frameToMerge) {
		this.otherFrame1 = frameToMerge;
		this.otherFrame2 = null;
		this.relativeWeightOther1 = 1;
	}

	/**
	 * Set the frame of data to merge into the next call of applyInline(). This method allows for an interpolation of two frames
	 * to be merged into the data set; for example, in order to correct for time misalignment between signal and other frames.
	 *
	 * @param frame1
	 *            frame 1
	 * @param frame2
	 *            frame 2
	 * @param relativeWeightFrame1
	 *            , a number between 0 and 1 indicating the relative weight of frame1^ with respect to frame2. Consequently, the
	 *            relative weight of frame 2 will be (1 - relativeWeightFrame1).
	 */
	public void setFrameToMerge(double[] frame1, double[] frame2, double relativeWeightFrame1) {
		this.otherFrame1 = frame1;
		this.otherFrame2 = frame2;
		this.relativeWeightOther1 = relativeWeightFrame1;
	}

	/**
	 * Process the LPC coefficients in place. This implementation converts the LPC coefficients into line spectral frequencies,
	 * and interpolates between these and the corresponding frame in the "other" signal.
	 *
	 * @param coeffs
	 *            the LPC coefficients
	 */
	protected void processLPC(LpCoeffs coeffs, double[] residual) {
		if (otherFrame1 == null)
			return; // no more other audio -- leave signal as is
		LpCoeffs otherCoeffs = LpcAnalyser.calcLPC(otherFrame1, p);
		double[] otherlsf = otherCoeffs.getLSF();
		double[] lsf = coeffs.getLSF();
		assert lsf.length == otherlsf.length;
		if (otherFrame2 != null && relativeWeightOther1 < 1) { // optionally, interpolate between two "other" frames before
																// merging into the signal
			assert 0 <= relativeWeightOther1;
			LpCoeffs other2Coeffs = LpcAnalyser.calcLPC(otherFrame2, p);
			double[] other2lsf = other2Coeffs.getLSF();
			/*
			 * PrintfFormat f = new PrintfFormat("%      .1f "); System.out.print("LSF     "); for (int i=0; i<lsf.length; i++) {
			 * System.out.print(f.sprintf(lsf[i]*16000)); } System.out.println();
			 *
			 * System.out.print("Other1  "); for (int i=0; i<lsf.length; i++) { System.out.print(f.sprintf(otherlsf[i]*16000)); }
			 * System.out.println();
			 *
			 * System.out.print("Other2  "); for (int i=0; i<lsf.length; i++) { System.out.print(f.sprintf(other2lsf[i]*16000)); }
			 * System.out.println();
			 *
			 * System.out.println();
			 */
			for (int i = 0; i < otherlsf.length; i++) {
				otherlsf[i] = relativeWeightOther1 * otherlsf[i] + (1 - relativeWeightOther1) * other2lsf[i];
			}
		}
		// now interpolate between the two:
		for (int i = 0; i < lsf.length; i++)
			lsf[i] = (1 - r) * lsf[i] + r * otherlsf[i];
		coeffs.setLSF(lsf);
		// Adapt residual gain to also interpolate average energy:
		double gainFactor = Math.sqrt((1 - r) * coeffs.getGain() * coeffs.getGain() + r * otherCoeffs.getGain()
				* otherCoeffs.getGain())
				/ coeffs.getGain();
		// System.out.println("Gain:" + coeffs.getGain() + ", otherGain:"+otherCoeffs.getGain()+", factor="+gainFactor);
		for (int i = 0; i < residual.length; i++)
			residual[i] *= gainFactor;

	}

}
