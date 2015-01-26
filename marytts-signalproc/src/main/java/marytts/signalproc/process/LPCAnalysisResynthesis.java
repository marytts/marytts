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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.analysis.LpcAnalyser;
import marytts.signalproc.analysis.LpcAnalyser.LpCoeffs;
import marytts.signalproc.filter.FIRFilter;
import marytts.signalproc.filter.RecursiveFilter;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.ArrayUtils;

/**
 * A base class for LPC-based analysis and resynthesis, which does nothing.
 * 
 * @author Marc Schr&ouml;der
 * 
 */
public class LPCAnalysisResynthesis implements InlineDataProcessor {
	protected int p;

	/**
	 * Apply LPC analysis-resynthesis.
	 * 
	 * @param p
	 *            prediction order, i.e. number of LPC coefficients to compute.
	 */
	public LPCAnalysisResynthesis(int p) {
		this.p = p;
	}

	public void applyInline(double[] data, int off, int len) {
		assert off == 0;
		assert len == data.length;
		// Compute LPC coefficients and residual
		LpCoeffs coeffs = LpcAnalyser.calcLPC(data, p);
		// double gain = coeffs.getGain();
		double[] residual = ArrayUtils.subarray(new FIRFilter(coeffs.getOneMinusA()).apply(data), 0, len);
		// Do something fancy with the lpc coefficients and/or the residual
		processLPC(coeffs, residual);
		// Resynthesise audio from residual and LPC coefficients
		double[] newData = new RecursiveFilter(coeffs.getA()).apply(residual);
		// System.err.println("Sum squared error:"+MathUtils.sumSquaredError(data, newData));
		System.arraycopy(newData, 0, data, 0, len);
	}

	/**
	 * Process the LPC coefficients and/or the residual in place. This method does nothing; subclasses may want to override it to
	 * do something meaningful.
	 * 
	 * @param coeffs
	 *            the LPC coefficients
	 * @param residual
	 *            the residual, of length framelength
	 */
	protected void processLPC(LpCoeffs coeffs, double[] residual) {
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
			int samplingRate = (int) inputAudio.getFormat().getSampleRate();
			AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
			int frameLength = Integer.getInteger("signalproc.lpcanalysissynthesis.framelength", 512).intValue();
			int predictionOrder = Integer.getInteger("signalproc.lpcanalysissynthesis.predictionorder", 20).intValue();
			FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANNING, true, frameLength, samplingRate,
					new LPCAnalysisResynthesis(predictionOrder));
			DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
			String outFileName = args[i].substring(0, args[i].length() - 4) + "_lpc_ar.wav";
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		}

	}

}
