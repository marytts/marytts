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

package marytts.signalproc.process;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.exceptions.MaryConfigurationException;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.LpcAnalyser;
import marytts.signalproc.analysis.LsfAnalyser;
import marytts.signalproc.analysis.LpcAnalyser.LpCoeffs;
import marytts.signalproc.filter.HighPassFilter;
import marytts.signalproc.window.HammingWindow;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.io.FileUtils;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;

/**
 * This class implements post-processing of TTS output to make it sound more intelligible when used in a telephone channel.
 * 
 * Several simple ideas are implemented:
 * 
 * (Step 1) Increasing relative energy of consonants (requires labels along with TTS wav outputs, with the same filename and
 * folder but with .lab extension) (Step 2) Sharpening formants explicitly using LSFs (Step 3) Increasing relative energy of
 * higher formants implicitly by adding highpass filtered version to original (Step 4) Finally, the output is gain adjusted to
 * prevent clipping
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class Blizzard09PostProcessor {

	public static final boolean LABELS_FROM_REALISED_DURATIONS_FILE = true; // If true reads from realised durations file instead
																			// of label file
	public static final String LABEL_FILE_EXTENSION = ".realised_durations";

	// Window and skip sizes for gain processing
	public static final double WINDOW_SIZE_IN_SECONDS_LSF = 0.020;
	public static final double SKIP_SIZE_IN_SECONDS_LSF = 0.005;
	//

	// Step 1. Modify LSFs to sharpen formants explicitly
	public static final boolean SHARPEN_FORMANTS = true; // Apply explicit formant sharpening using LSFs
	public static final double FORMANT_SHARPENING_START_FREQ = 1000.0; // 1000.0; (postproc2) //Lowest frequency in Hz to search
																		// for LSF pairs
	public static final double FORMANT_SHARPENING_END_FREQ = 2500; // 2500.0; (postproc2)//Highest frequency in Hz to search for
																	// LSF pairs
	public static final double RELATIVE_DECREASE_IN_LSF_PAIR_SEPARATION = 15.0; // 5.0; (postproc2)//(0.0-95.0) decrease in LSF
																				// pair distances in percentage of the original
																				// distance in frequency
	public static final double MAX_LSF_PAIR_SEPARATION_IN_HZ = 300.0; // 300.0; (postproc2)//Maximum LSF pair separation for
																		// formants
	//

	// Window and skip sizes for gain processing
	public static final double WINDOW_SIZE_IN_SECONDS_GAIN = 0.020;
	public static final double SKIP_SIZE_IN_SECONDS_GAIN = 0.001;
	//

	// Step 2. Modify relative gain of consonants
	public static final boolean INCREASE_CONSONANT_GAINS = true; // Apply consonant gain adjustment (increasing)?
	// Fixed settings for consonant gain adjustment
	public static final double CONSONANT_MAX_GAIN_FACTOR = 1.5; // 1.8; (postproc2)//Peak gain factor to multiply samples at the
																// consonant´s center (1.0-Infinity)
	public static final double CONSONANT_MAX_GAIN_RELATIVE_DURATION = 50.0; // 60.0; (postproc2)//Relative duration of maximum
																			// gain at the center of the consonant (0.0-100.0),
																			// values close to 100.0 will result in
																			// discontinuities
	//

	// Step 3. Reduce vowel nuclie energy to reduce reverbaration effects
	public static final boolean REDUCE_VOWEL_GAINS = true; // Apply consonant gain adjustment (increasing)?
	// Fixed settings for consonant gain adjustment
	public static final double VOWEL_MIN_GAIN_FACTOR = 0.7; // 0.6; (postproc2)//Peak gain factor to multiply samples at the
															// vowel´s center (0.0-1.0, 1.0 means no change)
	public static final double VOWEL_MIN_GAIN_RELATIVE_DURATION = 50.0; // 60.0; (postproc2)//Relative duration of maximum gain at
																		// the center of the vowel (0.0-100.0), values close to
																		// 100.0 will result in discontinuities
	//

	// Step 4. Add highpass filtered version to boost higher frequency formants implicitly
	public static final boolean APPLY_HIGHPASS_FILTER = false; // true; (postproc2) v//Apply highpass filtering?
	// Fixed settings for higher formant gain adjustment
	public static final double HIGHPASS_FILTER_CUTOFF = 2000.0; // Cut-off of highpass filter in Hz
	public static final double HIGHPASS_FILTER_RELATIVE_GAIN = 0.05; // (0.0-1.0) Relative gain of the highpass filtered signal
																		// when it´s being added with the original
																		// output = (1-relativeGain)*original +
																		// relativeGain*highpassFilterOutput

	//

	public static double[] process(double[] x, Labels labels, Allophone[] allophones, int samplingRateInHz, double absMaxOrig) {
		boolean[] isConsonants = new boolean[labels.items.length];
		boolean[] isVowels = new boolean[labels.items.length];
		boolean[] isPauses = new boolean[labels.items.length];

		for (int i = 0; i < labels.items.length; i++) {
			isConsonants[i] = false;
			int allophoneIndex = -1;
			for (int j = 0; j < allophones.length; j++) {
				if (allophones[j].name().compareTo(labels.items[i].phn) == 0) {
					if (allophones[j].isConsonant() && !allophones[j].isPlosive())
						isConsonants[i] = true;

					break;
				}
			}

			isVowels[i] = false;
			allophoneIndex = -1;
			for (int j = 0; j < allophones.length; j++) {
				if (allophones[j].name().compareTo(labels.items[i].phn) == 0) {
					if (allophones[j].isVowel())
						isVowels[i] = true;

					break;
				}
			}

			isPauses[i] = false;
			allophoneIndex = -1;
			for (int j = 0; j < allophones.length; j++) {
				if (allophones[j].name().compareTo(labels.items[i].phn) == 0) {
					if (allophones[j].isPause())
						isPauses[i] = true;

					break;
				}
			}
		}

		double[] y = ArrayUtils.copy(x);

		// Step 1
		if (SHARPEN_FORMANTS)
			y = processLSFs(y, samplingRateInHz, labels, isVowels, isPauses);

		// Step 2
		if (INCREASE_CONSONANT_GAINS)
			y = processGains(y, samplingRateInHz, labels, isConsonants, CONSONANT_MAX_GAIN_FACTOR,
					CONSONANT_MAX_GAIN_RELATIVE_DURATION);

		// Step 3
		if (REDUCE_VOWEL_GAINS)
			y = processGains(y, samplingRateInHz, labels, isVowels, VOWEL_MIN_GAIN_FACTOR, VOWEL_MIN_GAIN_RELATIVE_DURATION);

		// Step 3
		if (APPLY_HIGHPASS_FILTER)
			y = processHigherFormantGains(y, samplingRateInHz, labels, isPauses);
		//

		// Step 4
		double absMaxNew = MathUtils.absMax(y);

		int startIndex = 0;
		int endIndex;
		int i, j;
		for (i = 0; i < labels.items.length; i++) {
			if (!isPauses[i]) {
				endIndex = SignalProcUtils.time2sample(labels.items[i].time, samplingRateInHz) - 1;
				endIndex = Math.min(endIndex, x.length - 1);

				for (j = startIndex; j <= endIndex; j++)
					y[j] *= absMaxOrig / absMaxNew;

				startIndex = endIndex + 1;
			}
		}
		//

		return y;
	}

	// Multiplies consonant gains with a window to increase their relative energy level
	// The window is 1.0 at both ends to ensure continuity
	// Maximum gain occurs in the middle of the window
	public static double[] processGains(double[] x, int samplingRateInHz, Labels labels, boolean[] toBeProcesseds,
			double extremumGainFactor, double extremumGainRelativeDuration) {
		assert labels.items.length == toBeProcesseds.length;

		boolean isIncreasing = true;
		if (extremumGainFactor < 1.0)
			isIncreasing = false;

		double[] y = null;
		double[] w = null;
		int startIndex = 0;
		int endIndex;
		int ws = SignalProcUtils.time2sample(WINDOW_SIZE_IN_SECONDS_GAIN, samplingRateInHz);
		int ss = SignalProcUtils.time2sample(SKIP_SIZE_IN_SECONDS_GAIN, samplingRateInHz);
		Window wfrm = new HammingWindow(ws);
		wfrm.normalizePeakValue(1.0f);
		double[] frmWgt = wfrm.getCoeffs();

		if (x != null && x.length > 0) {
			y = new double[x.length];
			w = new double[x.length];
			Arrays.fill(y, 0.0);
			Arrays.fill(w, 0.0);

			double[] frm = new double[ws];
			int i, j, k;
			for (i = 0; i < labels.items.length; i++) {
				boolean bProcessed = false;
				endIndex = SignalProcUtils.time2sample(labels.items[i].time, samplingRateInHz) - 1;
				endIndex = Math.min(endIndex, x.length - 1);

				int numfrm = (int) Math.floor((endIndex - startIndex + 1.0) / (double) ss + 0.5) + 1;

				if (numfrm > 0) {
					int windowLen = (int) Math.floor(numfrm * (1.0 - extremumGainRelativeDuration / 100.0) + 0.5);
					double[] wgt = new double[numfrm];
					if (toBeProcesseds[i])
						Arrays.fill(wgt, extremumGainFactor);
					else
						Arrays.fill(wgt, 1.0);

					if (windowLen > 0 && toBeProcesseds[i]) {
						Window wConsonant = new HammingWindow(windowLen);
						if (isIncreasing)
							wConsonant.normalizeRange(1.0f, (float) extremumGainFactor);
						else
							wConsonant.normalizeRange((float) extremumGainFactor, 1.0f);

						double[] lWgt = null;
						double[] rWgt = null;

						if (isIncreasing) {
							lWgt = wConsonant.getCoeffsLeftHalf();
							rWgt = wConsonant.getCoeffsRightHalf();
						} else {
							lWgt = wConsonant.getCoeffsRightHalf();
							rWgt = wConsonant.getCoeffsLeftHalf();
						}

						if (lWgt != null) {
							for (j = 0; j < lWgt.length; j++)
								wgt[j] = lWgt[j];
						}

						if (rWgt != null) {
							for (j = 0; j < rWgt.length; j++)
								wgt[j + numfrm - rWgt.length] = rWgt[j];
						}

						// MaryUtils.plot(wgt);

						for (j = 0; j < numfrm; j++) {
							System.arraycopy(x, j * ss + startIndex, frm, 0, Math.min(ws, x.length - (j * ss + startIndex)));
							for (k = 0; k < Math.min(ws, x.length - (j * ss + startIndex)); k++) {
								y[j * ss + startIndex + k] += x[j * ss + startIndex + k] * frmWgt[k] * wgt[j];
								w[j * ss + startIndex + k] += frmWgt[k];
							}
						}
					} else {
						Window wShort = new HammingWindow(endIndex - startIndex + 1);
						double[] wShortWgt = wShort.getCoeffs();
						for (k = startIndex; k <= endIndex; k++) {
							y[k] += x[k] * wShortWgt[k - startIndex];
							w[k] += wShortWgt[k - startIndex];
						}
					}
				} else {
					Window wShort = new HammingWindow(endIndex - startIndex + 1);
					double[] wShortWgt = wShort.getCoeffs();
					for (k = startIndex; k <= endIndex; k++) {
						y[k] += x[k] * wShortWgt[k - startIndex];
						w[k] += wShortWgt[k - startIndex];
					}
				}

				startIndex = endIndex + 1;
			}

			for (i = 0; i < x.length; i++) {
				if (w[i] > 0.0)
					y[i] /= w[i];
			}
		}

		return y;
	}

	// Detects closest LSF pairs within a frequency range
	// Makes these LSF pairs closer
	// Re-synthesizes the output using modified LSFs and frequency domain AR filtering
	public static double[] processLSFs(double[] x, int samplingRateInHz, Labels labels, boolean[] isVowels, boolean[] isPauses) {
		assert labels.items.length == isVowels.length;
		assert labels.items.length == isPauses.length;

		double[] y = null;
		double[] w = null;
		int startIndex = 0;
		int endIndex;
		int ws = SignalProcUtils.time2sample(WINDOW_SIZE_IN_SECONDS_LSF, samplingRateInHz);
		int ss = SignalProcUtils.time2sample(SKIP_SIZE_IN_SECONDS_LSF, samplingRateInHz);
		Window wfrm = new HammingWindow(ws);
		wfrm.normalizePeakValue(1.0f);
		double[] frmWgt = wfrm.getCoeffs();

		if (x != null && x.length > 0) {
			int lpOrder = SignalProcUtils.getLPOrder(samplingRateInHz);
			y = new double[x.length];
			w = new double[x.length];
			Arrays.fill(y, 0.0);
			Arrays.fill(w, 0.0);

			double[] frm = new double[ws];
			int i, j, k;
			int fftSize = SignalProcUtils.getDFTSize(samplingRateInHz);

			for (i = 0; i < labels.items.length; i++) {
				boolean bProcessed = false;
				endIndex = SignalProcUtils.time2sample(labels.items[i].time + WINDOW_SIZE_IN_SECONDS_LSF, samplingRateInHz) - 1;
				int numfrm = (int) Math.floor((endIndex - startIndex + 1.0) / (double) ss + 0.5) + 1;

				// if (isVowels[i] && numfrm>0)
				if (numfrm > 0) {
					for (j = 0; j < numfrm; j++) {
						Arrays.fill(frm, 0.0);
						if (j * ss + startIndex < x.length) {
							System.arraycopy(x, j * ss + startIndex, frm, 0, Math.min(ws, x.length - (j * ss + startIndex)));
							double[] frmOrig = ArrayUtils.copy(frm);
							double origEn = SignalProcUtils.energy(frmOrig);

							wfrm.apply(frm, 0);
							LpCoeffs lpcs = LpcAnalyser.calcLPC(frm, lpOrder, 0.0f);
							double[] lsfs = LsfAnalyser.lpc2lsfInHz(lpcs.getOneMinusA(), samplingRateInHz);
							double[] lsfsMod = ArrayUtils.copy(lsfs);

							if (isVowels[i]) {
								double[] dists = new double[lsfs.length - 1];
								for (k = 0; k < lsfsMod.length - 1; k++)
									dists[k] = lsfs[k + 1] - lsfs[k];

								for (k = 1; k < dists.length - 1; k++) {
									if (dists[k] < Math.min(dists[k + 1], MAX_LSF_PAIR_SEPARATION_IN_HZ)) // lsfs[k] and lsfs[k+1]
																											// might be pairs
									{
										double meanFreq = 0.5 * (lsfs[k] + lsfs[k + 1]);
										if (meanFreq >= FORMANT_SHARPENING_START_FREQ && meanFreq < FORMANT_SHARPENING_END_FREQ) {
											double shift = 0.5 * RELATIVE_DECREASE_IN_LSF_PAIR_SEPARATION / 100.0 * dists[k];
											lsfsMod[k] = lsfs[k - 1] + shift;
											lsfsMod[k + 1] = lsfs[k - 1] - shift;
											k += 2;
										}
									} else if (dists[k + 1] < Math.min(dists[k], MAX_LSF_PAIR_SEPARATION_IN_HZ)) // lsfs[k+1] and
																													// lsfs[k+2]
																													// might be
																													// pairs
									{
										double meanFreq = 0.5 * (lsfs[k + 1] + lsfs[k + 2]);
										if (meanFreq >= FORMANT_SHARPENING_START_FREQ && meanFreq < FORMANT_SHARPENING_END_FREQ) {
											double shift = 0.5 * RELATIVE_DECREASE_IN_LSF_PAIR_SEPARATION / 100.0 * dists[k];
											lsfsMod[k + 1] = lsfs[k + 1] + shift;
											lsfsMod[k + 2] = lsfs[k + 2] - shift;
											k += 2;
										}
									}
								}
							}

							double[] newOneMinusAs = LsfAnalyser.lsfInHz2lpc(lsfsMod, samplingRateInHz);
							double[] newLpcs = ArrayUtils.subarray(newOneMinusAs, 1, lpOrder);
							newLpcs = MathUtils.multiply(newLpcs, -1.0);
							double[] H = LpcAnalyser.calcSpecLinear(lpcs.getA(), lpcs.getGain(), fftSize);
							double[] HNew = LpcAnalyser.calcSpecLinear(newLpcs, lpcs.getGain(), fftSize);
							// MaryUtils.plot(MathUtils.amp2db(H));
							// MaryUtils.plot(MathUtils.amp2db(HNew));
							double[] HT = MathUtils.divide(HNew, H);

							// SignalProcUtils.displayDFTSpectrumInDB(frmOrig);
							frm = SignalProcUtils.filterfd(HT, frmOrig, samplingRateInHz);
							// SignalProcUtils.displayDFTSpectrumInDB(frm);

							double newEn = SignalProcUtils.energy(frm);
							double gain = Math.sqrt(origEn) / Math.sqrt(newEn);

							for (k = 0; k < Math.min(ws, x.length - (j * ss + startIndex)); k++) {
								y[j * ss + startIndex + k] += gain * frm[k] * frmWgt[k];
								w[j * ss + startIndex + k] += frmWgt[k];
							}
						}
					}
				} else {
					Window wShort = new HammingWindow(endIndex - startIndex + 1);
					double[] wShortWgt = wShort.getCoeffs();
					for (k = startIndex; k <= endIndex; k++) {
						y[k] += x[k] * wShortWgt[k - startIndex];
						w[k] += wShortWgt[k - startIndex];
					}
				}

				startIndex = endIndex - ws;
			}

			for (i = 0; i < x.length; i++) {
				if (w[i] > 0.0)
					y[i] /= w[i];
			}
		}

		return y;
	}

	public static double[] processHigherFormantGains(double[] x, int samplingRateInHz, Labels labels, boolean[] isPauses) {
		assert labels.items.length == isPauses.length;

		double[] y = null;

		if (x != null && x.length > 0) {
			int i, j;
			HighPassFilter hpf = new HighPassFilter(HIGHPASS_FILTER_CUTOFF / samplingRateInHz);
			double[] xhpf = hpf.apply(x);

			for (i = 0; i < x.length; i++)
				xhpf[i] = (1.0 - HIGHPASS_FILTER_RELATIVE_GAIN) * x[i] + HIGHPASS_FILTER_RELATIVE_GAIN * xhpf[i];

			y = new double[x.length];

			int startIndex = 0;
			int endIndex;
			for (i = 0; i < labels.items.length; i++) {
				endIndex = SignalProcUtils.time2sample(labels.items[i].time, samplingRateInHz) - 1;
				endIndex = Math.min(endIndex, x.length - 1);
				if (isPauses[i])
					System.arraycopy(x, startIndex, y, startIndex, endIndex - startIndex + 1);
				else
					System.arraycopy(xhpf, startIndex, y, startIndex, endIndex - startIndex + 1);

				startIndex = endIndex + 1;
			}
		}

		return y;
	}

	public static void mainSingleFile(String inputWavFile, String outputWavFile, Allophone[] allophones)
			throws UnsupportedAudioFileException, IOException {
		// File input
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(inputWavFile));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] x = signal.getAllData();
		double absMaxOrig = MathUtils.absMax(x);

		String strLabFile = StringUtils.modifyExtension(inputWavFile, LABEL_FILE_EXTENSION);
		if (!FileUtils.exists(strLabFile)) // Labels required for transients analysis (unless we design an automatic algorithm)
		{
			System.out.println("Label file not found: " + strLabFile + "...skipping...");
		} else {
			Labels labels = new Labels(strLabFile);
			//

			double[] y = Blizzard09PostProcessor.process(x, labels, allophones, samplingRate, absMaxOrig);

			DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outputWavFile));
		}
	}

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException, MaryConfigurationException {
		if (args.length < 3) {
			System.out.println("Missing parameters:");
			System.out.println("<input wav file or directory> <output wav file or directory> <full path of phone set file>");
			System.out.println("Example phone set file: .../lib/modules/en/us/lexicon/allophones.en_US.xml");
		} else {
			String phoneSetFile = args[2];
			AllophoneSet allophoneSet = AllophoneSet.getAllophoneSet(phoneSetFile);

			Set<String> tmpPhonemes = allophoneSet.getAllophoneNames();
			int count = 0;
			Allophone[] allophones = new Allophone[tmpPhonemes.size()];
			for (Iterator<String> it = tmpPhonemes.iterator(); it.hasNext();) {
				allophones[count] = allophoneSet.getAllophone(it.next());
				count++;

				if (count >= tmpPhonemes.size())
					break;
			}

			if (FileUtils.isDirectory(args[0])) // Process folder
			{
				if (!FileUtils.exists(args[1]))
					FileUtils.createDirectory(args[1]);

				String[] fileList = FileUtils.getFileList(args[0], "wav");
				String outputFolder = StringUtils.checkLastSlash(args[1]);
				if (fileList != null) {
					for (int i = 0; i < fileList.length; i++) {
						String baseFileName = StringUtils.getFileName(fileList[i], true);
						String outputFile = outputFolder + baseFileName + ".wav";
						mainSingleFile(fileList[i], outputFile, allophones);
						System.out.println("Processing completed for file " + String.valueOf(i + 1) + " of "
								+ String.valueOf(fileList.length));
					}
				} else
					System.out.println("No wav files found!");
			} else
				// Process file
				mainSingleFile(args[0], args[1], allophones);

			System.out.println("Processing completed...");
		}
	}
}
