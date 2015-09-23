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
package marytts.signalproc.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.display.FunctionGraph;
import marytts.signalproc.filter.BandPassFilter;
import marytts.signalproc.filter.FIRFilter;
import marytts.signalproc.filter.LowPassFilter;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.io.FileUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;

/**
 * Autocorrelation based F0 tracker with heuristic rules based on statistics for smoothing and halving/doubling prevention
 * 
 * @author Oytun T&uuml;rk
 */
public class F0TrackerAutocorrelationHeuristic {
	public double[] f0s;

	protected PitchFileHeader params; // Pitch detection parameters
	protected int totalVoicedFrames; // Total number of voiced frames
	protected double[] voicingProbabilities; // Probability of voicing for each frame
	protected int minT0Index; // Minimum period length in samples (i.e. corresponding to maximum f0)
	protected int maxT0Index; // Maximum period length in samples (i.e. corresponding to minimum f0)

	protected double[] prevF0s;
	protected double[] voicedF0s; // Voiced frame´s f0 values
	protected double longTermAverageF0; // Long term average f0 in voiced frames
	protected double shortTermAverageF0; // Short term average f0 in voiced frames

	public static double MAX_SAMPLE = 32767.0; // Max 16-bit absolute sample value
	public static double MINIMUM_SPEECH_ENERGY = 50.0; // Minimum average sample energy for detecting unvoiced parts
	protected double averageSampleEnergy; // Keeps average sample energy for the current analysis frame

	// The following are used in internal computations only and are not accessible for the user
	private double[] pitchFrm; // A buffer for analysis speech frames

	private int frameIndex; // Current frame index
	private int ws; // Window size in samples
	private int ss; // Skip size in samples

	//

	public F0TrackerAutocorrelationHeuristic(String wavFile) throws Exception {
		if (FileUtils.exists(wavFile)) {
			String ptcFile = StringUtils.modifyExtension(wavFile, "ptc");

			params = new PitchFileHeader();

			init();

			PitchReaderWriter f0 = null;
			try {
				f0 = pitchAnalyzeWavFile(wavFile, ptcFile);
			} catch (UnsupportedAudioFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else
			throw new Exception("Wav file not found!");
	}

	public F0TrackerAutocorrelationHeuristic(String wavFile, String ptcFile) throws Exception {
		if (FileUtils.exists(wavFile)) {
			params = new PitchFileHeader();

			init();

			PitchReaderWriter f0 = null;
			try {
				f0 = pitchAnalyzeWavFile(wavFile, ptcFile);
			} catch (UnsupportedAudioFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else
			throw new Exception("Wav file not found!");
	}

	public F0TrackerAutocorrelationHeuristic(PitchFileHeader paramsIn) {
		params = new PitchFileHeader(paramsIn);

		init();
	}

	public void init() {
		int i;

		voicingProbabilities = new double[2];
		for (i = 0; i < voicingProbabilities.length; i++)
			voicingProbabilities[i] = 0.0;

		prevF0s = new double[5];

		for (i = 0; i < prevF0s.length; i++)
			prevF0s[i] = 0.0;

		voicedF0s = new double[20];

		for (i = 0; i < voicedF0s.length; i++)
			voicedF0s[i] = 0.0;

		longTermAverageF0 = 0.5 * (params.maximumF0 + params.minimumF0);
		shortTermAverageF0 = longTermAverageF0;

		frameIndex = 0;

		ws = (int) Math.floor(params.windowSizeInSeconds * params.fs + 0.5);
		ss = (int) Math.floor(params.skipSizeInSeconds * params.fs + 0.5);

		pitchFrm = new double[ws];
		minT0Index = (int) Math.floor(params.fs / params.maximumF0 + 0.5);
		maxT0Index = (int) Math.floor(params.fs / params.minimumF0 + 0.5);

		if (minT0Index < 0)
			minT0Index = 0;
		if (minT0Index > ws - 1)
			minT0Index = ws - 1;
		if (maxT0Index < minT0Index)
			maxT0Index = minT0Index;
		if (maxT0Index > ws - 1)
			maxT0Index = ws - 1;
	}

	public PitchReaderWriter pitchAnalyzeWavFile(String wavFileIn) throws UnsupportedAudioFileException, IOException {
		return pitchAnalyzeWavFile(wavFileIn, null);
	}

	public PitchReaderWriter pitchAnalyzeWavFile(String wavFileIn, String ptcFileOut) throws UnsupportedAudioFileException,
			IOException {
		PitchReaderWriter f0 = new PitchReaderWriter();

		pitchAnalyzeWav(wavFileIn);

		if (f0s != null) {
			params.numfrm = f0s.length;

			if (ptcFileOut != null)
				PitchReaderWriter.write_pitch_file(ptcFileOut, f0s, (float) (params.windowSizeInSeconds),
						(float) (params.skipSizeInSeconds), params.fs);
		} else
			params.numfrm = 0;

		f0.header = new PitchFileHeader(params);
		f0.setContour(f0s);

		return f0;
	}

	public void pitchAnalyzeWav(String wavFile) throws UnsupportedAudioFileException, IOException {
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(wavFile));
		params.fs = (int) inputAudio.getFormat().getSampleRate();

		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);

		pitchAnalyze(signal);
	}

	/**
	 * Analyse the f0 contour of the given audio signal.
	 * 
	 * @param signal
	 *            signal
	 */
	public void pitchAnalyze(DoubleDataSource signal) {
		pitchAnalyze(signal.getAllData());

		if (f0s != null)
			params.numfrm = f0s.length;
		else
			params.numfrm = 0;
	}

	private void pitchAnalyze(double[] x) {
		init();

		if (params.cutOff1 > 0.0 || params.cutOff2 > 0.0) {
			FIRFilter f = null;
			if (params.cutOff2 <= 0.0)
				f = new LowPassFilter(params.cutOff1 / params.fs);
			else
				f = new BandPassFilter(params.cutOff1 / params.fs, params.cutOff2 / params.fs);

			if (f != null)
				f.apply(x);
		}

		f0s = null;

		int numfrm = (int) Math.floor(((double) x.length - ws) / ss + 0.5);

		if (numfrm <= 0)
			return;

		double maxSample = MathUtils.getAbsMax(x);

		f0s = new double[numfrm];

		int i, j;
		frameIndex = 0;
		Arrays.fill(f0s, 0.0);

		Random random = new Random();
		for (i = 0; i < numfrm; i++) {
			System.arraycopy(x, i * ss, pitchFrm, 0, Math.min(ws, x.length - i * ss));
			for (j = 0; j < ws; j++)
				pitchFrm[j] = (pitchFrm[j] / maxSample) * MAX_SAMPLE + 1e-50 * random.nextDouble();

			f0s[i] = pitchFrameAutocorrelation(pitchFrm);

			frameIndex++;
		}
	}

	private double pitchFrameAutocorrelation(double[] frmIn) {
		assert pitchFrm.length == frmIn.length;
		System.arraycopy(pitchFrm, 0, frmIn, 0, frmIn.length);

		averageSampleEnergy = SignalProcUtils.getAverageSampleEnergy(pitchFrm);

		double f0 = 0.0;

		double probabilityOfVoicing;
		double tmp;
		int i, j;

		if (params.centerClippingRatio > 0.0)
			SignalProcUtils.centerClip(pitchFrm, params.centerClippingRatio);

		double r0 = 0.0;
		for (i = 0; i < pitchFrm.length; i++)
			r0 += pitchFrm[i] * pitchFrm[i];

		int maxIndex = 0;
		double maxR = -1.0e10;

		for (i = minT0Index; i <= maxT0Index; i++) {
			tmp = 0.0;
			for (j = 0; j < pitchFrm.length - i; j++)
				tmp += pitchFrm[j] * pitchFrm[j + i];

			if (tmp > maxR) {
				maxIndex = i;
				maxR = tmp;
			}
		}

		if (maxIndex == minT0Index || maxIndex == maxT0Index)
			probabilityOfVoicing = 0.0;
		else
			probabilityOfVoicing = maxR / r0;

		f0 = ((double) params.fs) / maxIndex;

		// look at previous two frame voicing decision to correct F0 estimate
		if (probabilityOfVoicing > params.voicingThreshold) {
			if (voicingProbabilities[0] < params.voicingThreshold && voicingProbabilities[1] > params.voicingThreshold)
				voicingProbabilities[0] = params.voicingThreshold + 0.01;
		} else if (probabilityOfVoicing > params.voicingThreshold - 0.1) {
			if (voicingProbabilities[0] > params.voicingThreshold && voicingProbabilities[1] > params.voicingThreshold)
				probabilityOfVoicing = params.voicingThreshold + 0.01;
		}

		if (probabilityOfVoicing < params.voicingThreshold)
			f0 = 0.0;

		if (averageSampleEnergy < MINIMUM_SPEECH_ENERGY)
			f0 = 0.0;

		for (i = voicingProbabilities.length - 1; i > 0; i--)
			voicingProbabilities[i] = voicingProbabilities[i - 1];

		voicingProbabilities[0] = probabilityOfVoicing;

		if (f0 > 10.0)
			totalVoicedFrames++;

		if (params.isDoublingCheck || params.isHalvingCheck) {
			if (f0 > 10.0) {
				totalVoicedFrames++;
				if (totalVoicedFrames > voicedF0s.length) {
					boolean bNeighVoiced = true;
					for (i = 0; i < voicingProbabilities.length; i++) {
						if (voicingProbabilities[i] < params.voicingThreshold) {
							bNeighVoiced = false;
							break;
						}
					}

					if (bNeighVoiced) {
						if (params.isDoublingCheck && f0 > 1.25 * longTermAverageF0 && f0 > 1.33 * shortTermAverageF0)
							f0 *= 0.5;
						if (params.isHalvingCheck && f0 < 0.80 * longTermAverageF0 && f0 < 0.66 * shortTermAverageF0)
							f0 *= 2.0;
					}
				}
			}
		}

		if (f0 > 10.0) {
			longTermAverageF0 = 0.99 * longTermAverageF0 + 0.01 * f0;
			shortTermAverageF0 = 0.90 * shortTermAverageF0 + 0.10 * f0;
		}

		// Smooth the F0 contour both with a median and linear filter

		prevF0s[2] = f0;

		boolean bAllVoiced = true;
		for (i = 0; i < prevF0s.length; i++) {
			if (prevF0s[i] < 10.0) {
				bAllVoiced = false;
				break;
			}
		}

		if (bAllVoiced) {
			f0 = MathUtils.median(prevF0s);

			tmp = 0.5 * prevF0s[2] + 0.25 * prevF0s[1] + 0.25 * prevF0s[0];
			if (Math.abs(tmp - f0) < 10.0)
				f0 = tmp;

			prevF0s[0] = prevF0s[1];
			prevF0s[1] = prevF0s[2];

			if (totalVoicedFrames == voicedF0s.length) {
				longTermAverageF0 = MathUtils.median(voicedF0s);
				shortTermAverageF0 = longTermAverageF0;
			}
		}

		// System.out.println("Frame=" + String.valueOf(frameIndex) + " " + String.valueOf(averageSampleEnergy) + " " +
		// String.valueOf(probabilityOfVoicing) + " " + String.valueOf(f0));

		return f0;
	}

	/**
	 * The frame shift time, in seconds.
	 * 
	 * @return params.skipSizeInSeconds
	 */
	public double getSkipSizeInSeconds() {
		return params.skipSizeInSeconds;
	}

	/**
	 * The size of the analysis window, in seconds.
	 * 
	 * @return params.windowSizeInSeconds
	 */
	public double getWindowSizeInSeconds() {
		return params.windowSizeInSeconds;
	}

	public double[] getF0Contour() {
		return f0s;
	}

	public static void main(String[] args) throws Exception {
		F0TrackerAutocorrelationHeuristic tracker = new F0TrackerAutocorrelationHeuristic(new PitchFileHeader());
		tracker.pitchAnalyzeWavFile(args[0]);
		FunctionGraph f0Graph = new FunctionGraph(0, tracker.params.skipSizeInSeconds, tracker.f0s);
		f0Graph.showInJFrame("F0 curve for " + args[0], false, true);
	}

}
