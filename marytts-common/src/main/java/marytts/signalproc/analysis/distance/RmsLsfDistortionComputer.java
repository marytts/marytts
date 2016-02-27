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
package marytts.signalproc.analysis.distance;

import java.io.IOException;

import marytts.modules.phonemiser.AllophoneSet;
import marytts.signalproc.adaptation.BaselineAdaptationItem;
import marytts.signalproc.adaptation.BaselineAdaptationSet;
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.analysis.AlignedLabels;
import marytts.signalproc.analysis.Label;
import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.signalproc.analysis.Lsfs;
import marytts.tools.analysis.TranscriptionAligner;
import marytts.util.io.FileUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;

/**
 * Implements root-mean-square line spectral frequency vector distance given two sets of paired files
 * 
 * @author Oytun T&uuml;rk
 */
public class RmsLsfDistortionComputer extends BaselineDistortionComputer {
	private AllophoneSet allophoneSet;
	private String silenceSymbol;
	private TranscriptionAligner aligner;

	public RmsLsfDistortionComputer() throws IOException {
		super();
		setupTranscriptionAligner();
	}

	private void setupTranscriptionAligner() throws IOException {
		String allophoneSetFilename = System.getProperty("allophoneset");
		if (allophoneSetFilename == null) {
			throw new IOException("Allophone set not provided (use -Dallophoneset=/path/to/allophones.xml)");
		}
		allophoneSet = null;
		try {
			allophoneSet = AllophoneSet.getAllophoneSet(allophoneSetFilename);
		} catch (Exception e) {
			IOException ioe = new IOException("Problem reading Allophones file " + allophoneSetFilename);
			ioe.initCause(e);
			throw ioe;
		}
		silenceSymbol = allophoneSet.getSilence().name();
		aligner = new TranscriptionAligner(allophoneSet);

	}

	public double[] getDistances(String folder1, String folder2, double upperFreqInHz) throws IOException {
		folder1 = StringUtils.checkLastSlash(folder1);
		folder2 = StringUtils.checkLastSlash(folder2);

		BaselineAdaptationSet set1 = new BaselineAdaptationSet(folder1, BaselineAdaptationSet.WAV_EXTENSION_DEFAULT);
		BaselineAdaptationSet set2 = new BaselineAdaptationSet(folder2, BaselineAdaptationSet.WAV_EXTENSION_DEFAULT);

		return getDistances(set1, set2, false, upperFreqInHz);
	}

	public double[] getDistances(String folder1, String folder2, boolean isBark, double upperFreqInHz) throws IOException {
		folder1 = StringUtils.checkLastSlash(folder1);
		folder2 = StringUtils.checkLastSlash(folder2);

		BaselineAdaptationSet set1 = new BaselineAdaptationSet(folder1, BaselineAdaptationSet.WAV_EXTENSION_DEFAULT);
		BaselineAdaptationSet set2 = new BaselineAdaptationSet(folder2, BaselineAdaptationSet.WAV_EXTENSION_DEFAULT);

		return getDistances(set1, set2, isBark, upperFreqInHz);
	}

	public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, boolean isBark, double upperFreqInHz)
			throws IOException {
		int[] map = new int[Math.min(set1.items.length, set2.items.length)];
		for (int i = 0; i < map.length; i++)
			map[i] = i;

		return getDistances(set1, set2, isBark, upperFreqInHz, map);
	}

	public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, boolean isBark, double upperFreqInHz,
			int[] map) throws IOException {
		double[] distances = null;
		double[] tmpDistances = null;

		for (int i = 0; i < map.length; i++) {
			double[] itemDistances = getItemDistances(set1.items[i], set2.items[map[i]], isBark, upperFreqInHz);
			if (distances != null && itemDistances != null) {
				tmpDistances = new double[distances.length];
				System.arraycopy(distances, 0, tmpDistances, 0, distances.length);
				distances = new double[tmpDistances.length + itemDistances.length];
				System.arraycopy(tmpDistances, 0, distances, 0, tmpDistances.length);
				System.arraycopy(itemDistances, 0, distances, tmpDistances.length, itemDistances.length);
			} else {
				distances = new double[itemDistances.length];
				System.arraycopy(itemDistances, 0, distances, 0, itemDistances.length);
			}
		}

		return distances;
	}

	/**
	 * Compute the distances per file
	 * 
	 * @param set1
	 *            set1
	 * @param set2
	 *            set2
	 * @param isBark
	 *            isBark
	 * @param upperFreqInHz
	 *            upperFreqInHz
	 * @param map
	 *            map
	 * @return an array containing, for each file, the array of frame-wise distances.
	 * @throws IOException
	 *             IO Exception
	 */
	public double[][] getDistancesPerFile(BaselineAdaptationSet set1, BaselineAdaptationSet set2, boolean isBark,
			double upperFreqInHz, int[] map) throws IOException {
		double[][] allDistances = new double[map.length][];

		for (int i = 0; i < map.length; i++) {
			double[] itemDistances = getItemDistances(set1.items[i], set2.items[map[i]], isBark, upperFreqInHz);
			allDistances[i] = itemDistances;
		}
		return allDistances;
	}

	/**
	 * Return true if the time given corresponds to an initial or final silence symbol in labels, false otherwise.
	 * 
	 * @param time
	 *            time
	 * @param labels
	 *            labels
	 * @param silenceSymbol
	 *            silenceSymbol
	 * @return (i == 0 or i == labels.items.length -1) and l.phn.equals(silenceSymbol)
	 */
	private boolean isInitialOrFinalSilence(double time, Labels labels, String silenceSymbol) {
		int i = labels.getLabelIndexAtTime(time);
		if (i == -1) { // somehow out of range, fix it
			if (time < 0) {
				i = 0;
			} else {
				i = labels.items.length - 1;
			}
		}
		assert i >= 0;
		Label l = labels.items[i];
		// Exclude initial and final silences:
		return (i == 0 || i == labels.items.length - 1) && l.phn.equals(silenceSymbol);
	}

	/**
	 * Compute the distance between two LSF frames
	 * 
	 * @param l1
	 *            one lsf frame
	 * @param l2
	 *            the other lsf frame
	 * @param isBark
	 *            whether to convert to bark scale before computing distance
	 * @param upperFreqInHz
	 *            the highest frequency up to which to compute the distance
	 * @return the distance
	 */
	private double computeOneFrameDistance(double[] l1, double[] l2, boolean isBark, double upperFreqInHz) {
		int maxInd1 = MathUtils.getLargestIndexSmallerThan(l1, upperFreqInHz);
		int maxInd2 = MathUtils.getLargestIndexSmallerThan(l2, upperFreqInHz);
		int maxInd = Math.min(maxInd1, maxInd2);

		double[] tmp1;
		double[] tmp2;
		if (maxInd + 1 == l1.length) {
			tmp1 = l1;
		} else {
			tmp1 = new double[maxInd + 1];
			System.arraycopy(l1, 0, tmp1, 0, maxInd + 1);
		}
		if (maxInd + 1 == l2.length) {
			tmp2 = l2;
		} else {
			tmp2 = new double[maxInd + 1];
			System.arraycopy(l2, 0, tmp2, 0, maxInd + 1);
		}

		double distance;
		if (!isBark) {
			distance = SignalProcUtils.getRmsDistance(tmp1, tmp2);
		} else {
			distance = SignalProcUtils.getRmsDistance(SignalProcUtils.freq2bark(tmp1), SignalProcUtils.freq2bark(tmp2));
		}
		return distance;
	}

	public double[] getItemDistances(BaselineAdaptationItem item1, BaselineAdaptationItem item2, boolean isBark,
			double upperFreqInHz) throws IOException {
		if (!FileUtils.exists(item1.lsfFile)) // Extract lsfs if necessary
		{
			LsfFileHeader lsfParams = new LsfFileHeader();
			BaselineFeatureExtractor.lsfAnalysis(item1, lsfParams, true);
		}

		if (!FileUtils.exists(item2.lsfFile)) // Extract lsfs if necessary
		{
			LsfFileHeader lsfParams = new LsfFileHeader();
			BaselineFeatureExtractor.lsfAnalysis(item2, lsfParams, true);
		}

		Lsfs lsfs1 = new Lsfs(item1.lsfFile);
		Lsfs lsfs2 = new Lsfs(item2.lsfFile);

		Labels labs1 = new Labels(item1.labelFile);
		Labels labs2 = new Labels(item2.labelFile);

		double[] frameDistances = null;
		int count = 0;

		if (labs1.items == null || labs2.items == null) {
			throw new IOException("Do not have labels for pair " + StringUtils.getFileName(item1.audioFile));
		}

		// Find the optimum alignment between the source and the target labels since the phone sequences may not be identical due
		// to silence periods etc.
		AlignedLabels aligned = aligner.alignLabels(labs1, labs2);
		assert aligned != null;

		// Now compute the frame-wise distances by mapping frames according to this label alignment;
		// for each aligned stretch, we move through the frames of the shorter side of the alignment
		// to make sure that dist(a,b) == dist(b,a)
		frameDistances = new double[Math.max(lsfs1.params.numfrm, lsfs2.params.numfrm)];

		// Make sure we don't use any frame twice:
		int frameSeen1 = -1;
		int frameSeen2 = -1;
		for (AlignedLabels.AlignedTimeStretch ats : aligned.getAlignedTimeStretches()) {
			boolean firstIsShorter = (ats.firstDuration <= ats.secondDuration);
			if (firstIsShorter) {
				int fromIndex = SignalProcUtils.time2frameIndex(ats.firstStart, lsfs1.params.winsize, lsfs1.params.skipsize);
				if (fromIndex < 0) {
					fromIndex = 0;
				}
				if (frameSeen1 >= fromIndex) {
					fromIndex = frameSeen1 + 1;
				}
				int toIndex = SignalProcUtils.time2frameIndex(ats.firstStart + ats.firstDuration, lsfs1.params.winsize,
						lsfs1.params.skipsize);
				if (toIndex >= lsfs1.lsfs.length) {
					break;
				}
				for (int f1 = fromIndex; f1 <= toIndex; f1++) {
					double t1 = SignalProcUtils.frameIndex2Time(f1, lsfs1.params.winsize, lsfs1.params.skipsize);
					double t2 = aligned.mapTimeFromFirstToSecond(t1);
					if (isInitialOrFinalSilence(t1, labs1, silenceSymbol) || isInitialOrFinalSilence(t2, labs2, silenceSymbol)) {
						continue;
					}
					int f2 = SignalProcUtils.time2frameIndex(t2, lsfs2.params.winsize, lsfs2.params.skipsize);
					if (f2 <= frameSeen2) {
						continue;
					}
					if (f2 >= lsfs2.lsfs.length) {
						break;
					}
					frameDistances[count++] = computeOneFrameDistance(lsfs1.lsfs[f1], lsfs2.lsfs[f2], isBark, upperFreqInHz);
					// System.err.println("Compared frames "+f1+" and "+f2);
					frameSeen1 = f1;
					frameSeen2 = f2;
				}
			} else { // second is shorter
				int fromIndex = SignalProcUtils.time2frameIndex(ats.secondStart, lsfs2.params.winsize, lsfs2.params.skipsize);
				if (fromIndex < 0) {
					fromIndex = 0;
				}
				if (frameSeen2 >= fromIndex) {
					fromIndex = frameSeen2 + 1;
				}
				int toIndex = SignalProcUtils.time2frameIndex(ats.secondStart + ats.secondDuration, lsfs2.params.winsize,
						lsfs2.params.skipsize);
				if (toIndex >= lsfs2.lsfs.length) {
					break;
				}
				for (int f2 = fromIndex; f2 <= toIndex; f2++) {
					double t2 = SignalProcUtils.frameIndex2Time(f2, lsfs2.params.winsize, lsfs2.params.skipsize);
					double t1 = aligned.mapTimeFromSecondToFirst(t2);
					if (isInitialOrFinalSilence(t1, labs1, silenceSymbol) || isInitialOrFinalSilence(t2, labs2, silenceSymbol)) {
						continue;
					}
					int f1 = SignalProcUtils.time2frameIndex(t1, lsfs1.params.winsize, lsfs1.params.skipsize);
					if (f1 <= frameSeen1) {
						continue;
					}
					if (f1 >= lsfs1.lsfs.length) {
						break;
					}
					frameDistances[count++] = computeOneFrameDistance(lsfs1.lsfs[f1], lsfs2.lsfs[f2], isBark, upperFreqInHz);
					// System.err.println("Compared frames "+f1+" and "+f2);
					frameSeen1 = f1;
					frameSeen2 = f2;
				}
			}
		}
		/*
		 * int j, labInd1, labInd2, frmInd1, frmInd2; double time1, time2; double startTime1, endTime1, startTime2, endTime2;
		 * double[] tmpLsfs1 = null; double[] tmpLsfs2 = null; int maxInd1, maxInd2, maxInd;
		 * 
		 * labInd1 = 0;
		 * 
		 * 
		 * //Find the corresponding target frame index for each source frame index for (j=0; j<lsfs1.params.numfrm; j++) { time1 =
		 * SignalProcUtils.frameIndex2Time(j, lsfs1.params.winsize, lsfs1.params.skipsize); int i1 =
		 * labs1.getLabelIndexAtTime(time1); assert i1 >= 0; Label l1 = labs1.items[i1]; // Exclude initial and final silences: if
		 * ((i1 == 0 || i1 == labs1.items.length - 1) && l1.phn.equals(silenceSymbol)) { continue; } time2 =
		 * aligned.mapTimeFromFirstToSecond(time1); int i2 = labs2.getLabelIndexAtTime(time2); assert i2 >= 0; Label l2 =
		 * labs2.items[i2]; // Exclude initial and final silences: if ((i2 == 0 || i2 == labs2.items.length - 1) &&
		 * l2.phn.equals(silenceSymbol)) { continue; }
		 * 
		 * frmInd2 = SignalProcUtils.time2frameIndex(time2, lsfs2.params.winsize, lsfs2.params.skipsize); if (frmInd2<0)
		 * frmInd2=0; if (frmInd2>lsfs2.params.numfrm-1) frmInd2=lsfs2.params.numfrm-1;
		 * 
		 * maxInd1 = MathUtils.getLargestIndexSmallerThan(lsfs1.lsfs[j], upperFreqInHz); maxInd2 =
		 * MathUtils.getLargestIndexSmallerThan(lsfs2.lsfs[frmInd2], upperFreqInHz); maxInd = Math.min(maxInd1, maxInd2);
		 * 
		 * tmpLsfs1 = new double[maxInd+1]; tmpLsfs2 = new double[maxInd+1]; System.arraycopy(lsfs1.lsfs[j], 0, tmpLsfs1, 0,
		 * maxInd+1); System.arraycopy(lsfs2.lsfs[frmInd2], 0, tmpLsfs2, 0, maxInd+1);
		 * 
		 * if (!isBark) frameDistances[count++] = SignalProcUtils.getRmsDistance(tmpLsfs1, tmpLsfs2); else frameDistances[count++]
		 * = SignalProcUtils.getRmsDistance(SignalProcUtils.freq2bark(tmpLsfs1), SignalProcUtils.freq2bark(tmpLsfs2));
		 * System.err.println("Compared frames "+j+" and "+frmInd2);
		 * 
		 * if (count>=frameDistances.length) break; }
		 */

		if (count > 0) {
			double[] tmpFrameDistances = frameDistances;
			frameDistances = new double[count];
			System.arraycopy(tmpFrameDistances, 0, frameDistances, 0, count);
		}

		return frameDistances;
	}

	public static void mainParametricInterspeech2008(String method, String emotion, boolean isBark) throws IOException {
		String baseDir = "D:/Oytun/DFKI/voices/Interspeech08_out/objective_test/";

		String tgtFolder = baseDir + "target/" + emotion;
		String srcFolder = baseDir + "source/" + emotion;
		String tfmFolder = baseDir + method + "/" + emotion;

		String outputFile = baseDir + method + "_" + emotion + "_rmsLsf.txt";

		RmsLsfDistortionComputer r = new RmsLsfDistortionComputer();

		double[] distances1 = r.getDistances(tgtFolder, srcFolder, isBark, 8000);
		double[] distances2 = r.getDistances(tgtFolder, tfmFolder, isBark, 8000);

		double m1 = MathUtils.mean(distances1);
		double s1 = MathUtils.standardDeviation(distances1, m1);
		double m2 = MathUtils.mean(distances2);
		double s2 = MathUtils.standardDeviation(distances2, m2);
		double conf95_1 = MathUtils.getConfidenceInterval95(s1);
		double conf99_1 = MathUtils.getConfidenceInterval99(s1);
		double conf95_2 = MathUtils.getConfidenceInterval95(s2);
		double conf99_2 = MathUtils.getConfidenceInterval99(s2);

		double[] tmpOut = new double[distances1.length + distances2.length + 9];
		tmpOut[0] = m1; // tgt-src mean
		tmpOut[1] = s1; // tgt-src std
		tmpOut[2] = m2; // tgt-tfm mean
		tmpOut[3] = s2; // tgt-tfm std
		tmpOut[4] = m1 - m2; // decrease in tgt-src distance by tfm
		tmpOut[5] = conf95_1; // 95% confidence interval for distance tgt-src distances
		tmpOut[6] = conf99_1; // 99% confidence interval for distance tgt-src distances
		tmpOut[7] = conf95_2; // 95% confidence interval for distance tgt-tfm distances
		tmpOut[8] = conf99_2; // 99% confidence interval for distance tgt-tfm distances

		System.arraycopy(distances1, 0, tmpOut, 9, distances1.length);
		System.arraycopy(distances2, 0, tmpOut, distances1.length + 9, distances2.length);
		FileUtils.writeToTextFile(tmpOut, outputFile);

		double c1Left95 = m1 - conf95_1;
		double c1Left99 = m1 - conf99_1;
		double c1Right95 = m1 + conf95_1;
		double c1Right99 = m1 + conf99_1;
		double c2Left95 = m2 - conf95_2;
		double c2Left99 = m2 - conf99_2;
		double c2Right95 = m2 + conf95_2;
		double c2Right99 = m2 + conf99_2;

		System.out.println(method + " " + emotion + " tgt-src: MeanDist=" + String.valueOf(m1) + " " + "StdDist="
				+ String.valueOf(s1));
		System.out.println(method + " " + emotion + " tgt-tfm: MeanDist=" + String.valueOf(m2) + " " + "StdDist="
				+ String.valueOf(s2));
		System.out.println(method + " " + emotion + " distance reduction=" + String.valueOf(m1 - m2));
		System.out.println("Confidence intervals tgt-src %95:  " + String.valueOf(conf95_1) + " --> [" + String.valueOf(c1Left95)
				+ "," + String.valueOf(c1Right95) + "]");
		System.out.println("Confidence intervals tgt-src %99:  " + String.valueOf(conf99_1) + " --> [" + String.valueOf(c1Left99)
				+ "," + String.valueOf(c1Right99) + "]");
		System.out.println("Confidence intervals tgt-tfm %95:  " + String.valueOf(conf95_2) + " --> [" + String.valueOf(c2Left95)
				+ "," + String.valueOf(c2Right95) + "]");
		System.out.println("Confidence intervals tgt-tfm %99:  " + String.valueOf(conf99_2) + " --> [" + String.valueOf(c2Left99)
				+ "," + String.valueOf(c2Right99) + "]");
		System.out.println("---------------------------------");
	}

	// Put source and target wav and lab files into two folders and call this function
	public static void mainInterspeech2008() throws IOException {
		boolean isBark = true;
		String method; // "1_codebook"; "2_frame"; "3_gmm";
		String emotion; // "angry"; "happy"; "sad"; "all";

		method = "1_codebook";
		emotion = "angry";
		mainParametricInterspeech2008(method, emotion, isBark);
		emotion = "happy";
		mainParametricInterspeech2008(method, emotion, isBark);
		emotion = "sad";
		mainParametricInterspeech2008(method, emotion, isBark);
		emotion = "all";
		mainParametricInterspeech2008(method, emotion, isBark);

		method = "2_frame";
		emotion = "angry";
		mainParametricInterspeech2008(method, emotion, isBark);
		emotion = "happy";
		mainParametricInterspeech2008(method, emotion, isBark);
		emotion = "sad";
		mainParametricInterspeech2008(method, emotion, isBark);
		emotion = "all";
		mainParametricInterspeech2008(method, emotion, isBark);

		method = "3_gmm";
		emotion = "angry";
		mainParametricInterspeech2008(method, emotion, isBark);
		emotion = "happy";
		mainParametricInterspeech2008(method, emotion, isBark);
		emotion = "sad";
		mainParametricInterspeech2008(method, emotion, isBark);
		emotion = "all";
		mainParametricInterspeech2008(method, emotion, isBark);

		System.out.println("Objective test completed...");
	}

	public static void mainHmmVoiceConversion(String method1, String method2, String folder1, String folder2,
			String referenceFolder, String outputFile, boolean isBark) throws IOException {
		RmsLsfDistortionComputer r = new RmsLsfDistortionComputer();

		double[] distances1 = r.getDistances(referenceFolder, folder1, isBark, 8000);
		double[] distances2 = r.getDistances(referenceFolder, folder2, isBark, 8000);

		double m1 = MathUtils.mean(distances1);
		double s1 = MathUtils.standardDeviation(distances1, m1);
		double m2 = MathUtils.mean(distances2);
		double s2 = MathUtils.standardDeviation(distances2, m2);
		double conf95_1 = MathUtils.getConfidenceInterval95(s1);
		double conf99_1 = MathUtils.getConfidenceInterval99(s1);
		double conf95_2 = MathUtils.getConfidenceInterval95(s2);
		double conf99_2 = MathUtils.getConfidenceInterval99(s2);

		double[] tmpOut = new double[distances1.length + distances2.length + 9];
		tmpOut[0] = m1; // tgt-src mean
		tmpOut[1] = s1; // tgt-src std
		tmpOut[2] = m2; // tgt-tfm mean
		tmpOut[3] = s2; // tgt-tfm std
		tmpOut[4] = m1 - m2; // decrease in tgt-src distance by tfm
		tmpOut[5] = conf95_1; // 95% confidence interval for distance tgt-src distances
		tmpOut[6] = conf99_1; // 99% confidence interval for distance tgt-src distances
		tmpOut[7] = conf95_2; // 95% confidence interval for distance tgt-tfm distances
		tmpOut[8] = conf99_2; // 99% confidence interval for distance tgt-tfm distances

		System.arraycopy(distances1, 0, tmpOut, 9, distances1.length);
		System.arraycopy(distances2, 0, tmpOut, distances1.length + 9, distances2.length);
		FileUtils.writeToTextFile(tmpOut, outputFile);

		double c1Left95 = m1 - conf95_1;
		double c1Left99 = m1 - conf99_1;
		double c1Right95 = m1 + conf95_1;
		double c1Right99 = m1 + conf99_1;
		double c2Left95 = m2 - conf95_2;
		double c2Left99 = m2 - conf99_2;
		double c2Right95 = m2 + conf95_2;
		double c2Right99 = m2 + conf99_2;

		System.out.println(method1 + " tgt-src: MeanDist=" + String.valueOf(m1) + " " + "StdDist=" + String.valueOf(s1));
		System.out.println(method2 + " tgt-tfm: MeanDist=" + String.valueOf(m2) + " " + "StdDist=" + String.valueOf(s2));
		System.out.println("Distance reduction=" + String.valueOf(m1 - m2));
		System.out.println("Confidence intervals reference-method1 %95:  " + String.valueOf(conf95_1) + " --> ["
				+ String.valueOf(c1Left95) + "," + String.valueOf(c1Right95) + "]");
		System.out.println("Confidence intervals reference-method1 %99:  " + String.valueOf(conf99_1) + " --> ["
				+ String.valueOf(c1Left99) + "," + String.valueOf(c1Right99) + "]");
		System.out.println("Confidence intervals reference-method2 %95:  " + String.valueOf(conf95_2) + " --> ["
				+ String.valueOf(c2Left95) + "," + String.valueOf(c2Right95) + "]");
		System.out.println("Confidence intervals reference-method2 %99:  " + String.valueOf(conf99_2) + " --> ["
				+ String.valueOf(c2Left99) + "," + String.valueOf(c2Right99) + "]");
		System.out.println("---------------------------------");
	}

	public static void mainHmmVoiceConversion() throws IOException {
		String baseInputFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest2/output/final/";
		String baseOutputFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest2/objective_test/";
		boolean isBark = true;
		String method1, method2, folder1, folder2, referenceFolder, outputFile;

		referenceFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest2/output/final/origTarget";

		// No-GV vs GV
		method1 = "NOGV";
		method2 = "GV";
		folder1 = baseInputFolder + "hmmSource_nogv";
		folder2 = baseInputFolder + "hmmSource_gv";
		outputFile = baseOutputFolder + "lsf_" + method1 + "_" + method2 + ".txt";
		mainHmmVoiceConversion(method1, method2, folder1, folder2, referenceFolder, outputFile, isBark);

		// No-GV vs SC
		method1 = "NOGV";
		method2 = "NOGV+SC";
		folder1 = baseInputFolder + "hmmSource_nogv";
		folder2 = baseInputFolder + "tfm_nogv_1092files_128mixes";
		outputFile = baseOutputFolder + "lsf_" + method1 + "_" + method2 + ".txt";
		mainHmmVoiceConversion(method1, method2, folder1, folder2, referenceFolder, outputFile, isBark);

		// GV vs SC
		method1 = "GV";
		method2 = "GV+SC";
		folder1 = baseInputFolder + "hmmSource_gv";
		folder2 = baseInputFolder + "tfm_gv_1092files_128mixes";
		outputFile = baseOutputFolder + "lsf_" + method1 + "_" + method2 + ".txt";
		mainHmmVoiceConversion(method1, method2, folder1, folder2, referenceFolder, outputFile, isBark);

		System.out.println("Objective test completed...");
	}

	/**
	 * Compare distances between two folders; each folder is expected to contain wav files with the same names and accompanying
	 * lab files.
	 * 
	 * @param folder1
	 *            first folder
	 * @param folder2
	 *            second folder
	 * @throws IOException
	 *             if any file names don't match.
	 */
	public void mainDistancesPerFile(String folder1, String folder2) throws IOException {
		long startTime = System.currentTimeMillis();
		RmsLsfDistortionComputer r = new RmsLsfDistortionComputer();
		folder1 = StringUtils.checkLastSlash(folder1);
		folder2 = StringUtils.checkLastSlash(folder2);
		BaselineAdaptationSet set1 = new BaselineAdaptationSet(folder1);
		BaselineAdaptationSet set2 = new BaselineAdaptationSet(folder2);
		boolean isBark = true;
		double upperFreqInHz = 8000;
		int[] map = new int[set1.items.length];
		for (int i = 0; i < map.length; i++) {
			if (!StringUtils.getFileName(set1.items[i].audioFile).equals(StringUtils.getFileName(set2.items[i].audioFile))) {
				// Non-matching audio file names -- I will not have this
				throw new IOException("Audio files in folders do not match:\n" + set1.items[i].audioFile + " doesn't match "
						+ set2.items[i].audioFile);
			}
			map[i] = i;
		}
		double[][] allDistances = r.getDistancesPerFile(set1, set2, isBark, upperFreqInHz, map);
		assert allDistances.length == map.length;

		System.out.println("RMSE Bark-scaled LSF distances between " + folder1 + " and " + folder2);

		// For memory efficiency and computational precision, we compute mean and standard deviation incrementally,
		// using the following formulae:
		// mean[n] = mean[n-1] + (1/n) * (x[n] - mean[n-1])
		// variance[n] = variance[n-1] + (x[n] - mean[n-1]) * (x[n] - mean[n])
		// stddev[n] = sqrt(variance[n] / n)

		// Mean and variance accumulated across all files:
		double allMean = 0;
		double allPrevMean = 0;
		double allVariance = 0;
		long allN = 0;
		for (int i = 0; i < map.length; i++) {
			// Mean and variance for one file:
			double oneMean = 0;
			double onePrevMean = 0;
			double oneVariance = 0;
			int oneN = 0;
			for (int j = 0; j < allDistances[i].length; j++) {
				double x = allDistances[i][j];
				allN++;
				allPrevMean = allMean;
				allMean += (x - allPrevMean) / allN;
				allVariance += (x - allPrevMean) * (x - allMean);
				oneN++;
				onePrevMean = oneMean;
				oneMean += (x - onePrevMean) / oneN;
				oneVariance += (x - onePrevMean) * (x - oneMean);
			}
			double oneStddev = Math.sqrt(oneVariance / oneN);
			System.out.println(StringUtils.getFileName(set1.items[i].audioFile) + " mean " + oneMean + " stddev " + oneStddev);
		}
		double allStddev = Math.sqrt(allVariance / allN);
		System.out.println("Global mean " + allMean + " stddev " + allStddev);

		long timeNeeded = System.currentTimeMillis() - startTime;
		System.err.println("Computed distances between " + map.length + " files in " + timeNeeded + " ms");
	}

	public static void main(String[] args) throws Exception {
		// mainInterspeech2008();

		// mainHmmVoiceConversion();

		/*
		 * RmsLsfDistortionComputer d = new RmsLsfDistortionComputer(); BaselineAdaptationItem item1 = new
		 * BaselineAdaptationItem(); BaselineAdaptationItem item2 = new BaselineAdaptationItem();
		 * item1.setFromWavFilename(args[0]); item2.setFromWavFilename(args[1]); double[] frameDistances =
		 * d.getItemDistances(item1, item2, true, 8000); double meanDist = MathUtils.mean(frameDistances); double stdDist =
		 * MathUtils.standardDeviation(frameDistances, meanDist);
		 * System.out.println(item1.audioFile+"-"+item2.audioFile+" distance: "+meanDist+" (std "+stdDist+")");
		 */

		new RmsLsfDistortionComputer().mainDistancesPerFile(args[0], args[1]);
	}
}
