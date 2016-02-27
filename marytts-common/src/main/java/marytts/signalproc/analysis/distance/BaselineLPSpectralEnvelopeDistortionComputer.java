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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.adaptation.BaselineAdaptationItem;
import marytts.signalproc.adaptation.BaselineAdaptationSet;
import marytts.signalproc.adaptation.IndexMap;
import marytts.signalproc.analysis.Labels;
import marytts.util.data.AlignLabelsUtils;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;

/**
 * Implements an LP spectral envelope based distortion measure
 * 
 * @author Oytun T&uuml;rk
 */
public class BaselineLPSpectralEnvelopeDistortionComputer extends BaselineDistortionComputer {
	public static final double DEFAULT_WINDOWSIZE = 0.020;
	public static final double DEFAULT_SKIPSIZE = 0.010;
	public static final int DEFAULT_FFTSIZE = -1;
	public static final int DEFAULT_LPORDER = -1;

	public BaselineLPSpectralEnvelopeDistortionComputer() {
		super();
	}

	public double[] getDistances(String folder1, String folder2) throws IOException {
		return getDistances(folder1, folder2, DEFAULT_WINDOWSIZE);
	}

	public double[] getDistances(String folder1, String folder2, double winSizeInSeconds) throws IOException {
		return getDistances(folder1, folder2, winSizeInSeconds, DEFAULT_SKIPSIZE);
	}

	public double[] getDistances(String folder1, String folder2, double winSizeInSeconds, double skipSizeInSeconds)
			throws IOException {
		return getDistances(folder1, folder2, winSizeInSeconds, skipSizeInSeconds, DEFAULT_FFTSIZE);
	}

	public double[] getDistances(String folder1, String folder2, double winSizeInSeconds, double skipSizeInSeconds, int fftSize)
			throws IOException {
		return getDistances(folder1, folder2, winSizeInSeconds, skipSizeInSeconds, fftSize, DEFAULT_LPORDER);
	}

	public double[] getDistances(String folder1, String folder2, double winSizeInSeconds, double skipSizeInSeconds, int fftSize,
			int lpOrder) throws IOException {
		folder1 = StringUtils.checkLastSlash(folder1);
		folder2 = StringUtils.checkLastSlash(folder2);

		BaselineAdaptationSet set1 = new BaselineAdaptationSet(folder1, BaselineAdaptationSet.WAV_EXTENSION_DEFAULT);
		BaselineAdaptationSet set2 = new BaselineAdaptationSet(folder2, BaselineAdaptationSet.WAV_EXTENSION_DEFAULT);

		return getDistances(set1, set2, winSizeInSeconds, skipSizeInSeconds, fftSize, lpOrder);
	}

	public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2) throws IOException {
		return getDistances(set1, set2, DEFAULT_WINDOWSIZE);
	}

	public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, double winSizeInSeconds)
			throws IOException {
		return getDistances(set1, set2, winSizeInSeconds, DEFAULT_SKIPSIZE);
	}

	public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, double winSizeInSeconds,
			double skipSizeInSeconds) throws IOException {
		return getDistances(set1, set2, winSizeInSeconds, skipSizeInSeconds, DEFAULT_FFTSIZE);
	}

	public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, double winSizeInSeconds,
			double skipSizeInSeconds, int fftSize) throws IOException {
		return getDistances(set1, set2, winSizeInSeconds, skipSizeInSeconds, fftSize, DEFAULT_LPORDER);
	}

	public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, double winSizeInSeconds,
			double skipSizeInSeconds, int fftSize, int lpOrder) throws IOException {
		int[] map = new int[Math.min(set1.items.length, set2.items.length)];
		for (int i = 0; i < map.length; i++)
			map[i] = i;

		return getDistances(set1, set2, winSizeInSeconds, skipSizeInSeconds, fftSize, lpOrder, map);
	}

	public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, double winSizeInSeconds,
			double skipSizeInSeconds, int fftSize, int lpOrder, int[] map) throws IOException {
		double[] distances = null;
		double[] tmpDistances = null;

		for (int i = 0; i < map.length; i++) {
			double[] itemDistances = getItemDistances(set1.items[i], set2.items[map[i]], winSizeInSeconds, skipSizeInSeconds,
					fftSize, lpOrder);
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

	public double[] getItemDistances(BaselineAdaptationItem item1, BaselineAdaptationItem item2, double winSizeInSeconds,
			double skipSizeInSeconds) throws IOException {
		return getItemDistances(item1, item2, winSizeInSeconds, skipSizeInSeconds, DEFAULT_FFTSIZE);
	}

	public double[] getItemDistances(BaselineAdaptationItem item1, BaselineAdaptationItem item2, double winSizeInSeconds,
			double skipSizeInSeconds, int fftSize) throws IOException {
		return getItemDistances(item1, item2, winSizeInSeconds, skipSizeInSeconds, fftSize, DEFAULT_LPORDER);
	}

	public double[] getItemDistances(BaselineAdaptationItem item1, BaselineAdaptationItem item2, double winSizeInSeconds,
			double skipSizeInSeconds, int fftSize, int lpOrder) throws IOException {
		double[] frameDistances = null;

		// Read wav files & determine avaliable number of frames
		AudioInputStream inputAudio1;
		AudioInputStream inputAudio2;
		try {
			inputAudio1 = AudioSystem.getAudioInputStream(new File(item1.audioFile));
			inputAudio2 = AudioSystem.getAudioInputStream(new File(item2.audioFile));
		} catch (UnsupportedAudioFileException e) {
			throw new IOException("Cannot open audio file", e);
		}

		int i;
		int samplingRate1 = (int) inputAudio1.getFormat().getSampleRate();
		int ws1 = (int) Math.floor(winSizeInSeconds * samplingRate1 + 0.5);
		int ss1 = (int) Math.floor(skipSizeInSeconds * samplingRate1 + 0.5);
		AudioDoubleDataSource signal1 = new AudioDoubleDataSource(inputAudio1);
		double[] x1 = signal1.getAllData();
		double[] frm1 = new double[ws1];
		int numfrm1 = (int) Math.floor((x1.length - ws1) / ((double) ss1) + 0.5);
		double max1 = MathUtils.absMax(x1);
		for (i = 0; i < x1.length; i++)
			x1[i] = x1[i] / max1 * 20000;

		int samplingRate2 = (int) inputAudio2.getFormat().getSampleRate();
		int ws2 = (int) Math.floor(winSizeInSeconds * samplingRate2 + 0.5);
		int ss2 = (int) Math.floor(skipSizeInSeconds * samplingRate2 + 0.5);
		AudioDoubleDataSource signal2 = new AudioDoubleDataSource(inputAudio2);
		double[] x2 = signal2.getAllData();
		double[] frm2 = new double[ws2];
		int numfrm2 = (int) Math.floor((x2.length - ws2) / ((double) ss2) + 0.5);
		double max2 = MathUtils.absMax(x2);
		for (i = 0; i < x2.length; i++)
			x2[i] = x2[i] / max2 * 20000;

		if (fftSize < 0) {
			fftSize = Math.max(SignalProcUtils.getDFTSize(samplingRate1), SignalProcUtils.getDFTSize(samplingRate2));
			while (fftSize < ws1)
				fftSize *= 2;
			while (fftSize < ws2)
				fftSize *= 2;
		}

		if (lpOrder < 0)
			lpOrder = Math.max(SignalProcUtils.getLPOrder(samplingRate1), SignalProcUtils.getLPOrder(samplingRate2));
		//

		Labels labs1 = new Labels(item1.labelFile);
		Labels labs2 = new Labels(item2.labelFile);

		int count = 0;

		if (labs1.items != null && labs2.items != null) {
			// Find the optimum alignment between the source and the target labels since the phone sequences may not be identical
			// due to silence periods etc.
			int[][] labelMap = AlignLabelsUtils.alignLabels(labs1.items, labs2.items);
			//

			if (labelMap != null) {
				int j, labInd1, labInd2, frmInd1, frmInd2;
				double time1, time2;
				double startTime1, endTime1, startTime2, endTime2;
				double[] tmpLsfs1 = null;
				double[] tmpLsfs2 = null;
				int x1Start, x2Start;

				labInd1 = 0;

				frameDistances = new double[numfrm1];

				// Find the corresponding target frame index for each source frame index
				for (j = 0; j < numfrm1; j++) {
					time1 = SignalProcUtils.frameIndex2Time(j, winSizeInSeconds, skipSizeInSeconds);

					while (time1 > labs1.items[labInd1].time) {
						labInd1++;
						if (labInd1 > labs1.items.length - 1) {
							labInd1 = labs1.items.length - 1;
							break;
						}
					}

					if (labInd1 > 0 && labInd1 < labs1.items.length - 1) // Exclude first and last label)
					{
						labInd2 = StringUtils.findInMap(labelMap, labInd1);

						if (labInd2 >= 0 && labs1.items[labInd1].phn.compareTo(labs2.items[labInd2].phn) == 0) {
							if (labInd1 > 0)
								startTime1 = labs1.items[labInd1 - 1].time;
							else
								startTime1 = 0.0;

							if (labInd2 > 0)
								startTime2 = labs2.items[labInd2 - 1].time;
							else
								startTime2 = 0.0;

							endTime1 = labs1.items[labInd1].time;
							endTime2 = labs2.items[labInd2].time;

							time2 = MathUtils.linearMap(time1, startTime1, endTime1, startTime2, endTime2);

							frmInd2 = SignalProcUtils.time2frameIndex(time2, winSizeInSeconds, skipSizeInSeconds);
							if (frmInd2 < 0)
								frmInd2 = 0;
							if (frmInd2 > numfrm2 - 1)
								frmInd2 = numfrm2 - 1;

							x1Start = (int) Math.floor(j * ss1 + 0.5 * ws1 + 0.5);
							x2Start = (int) Math.floor(frmInd2 * ss2 + 0.5 * ws2 + 0.5);

							if (x1Start + ws1 < x1.length)
								System.arraycopy(x1, x1Start, frm1, 0, ws1);
							else {
								Arrays.fill(frm1, 0.0);
								System.arraycopy(x1, x1Start, frm1, 0, x1.length - x1Start);
							}

							if (x2Start + ws2 < x2.length)
								System.arraycopy(x2, x2Start, frm2, 0, ws2);
							else {
								Arrays.fill(frm2, 0.0);
								System.arraycopy(x2, x2Start, frm2, 0, x2.length - x2Start);
							}

							SignalProcUtils.addWhiteNoise(frm1, 1e-10);
							SignalProcUtils.addWhiteNoise(frm2, 1e-10);

							frameDistances[count] = frameDistance(frm1, frm2, fftSize, lpOrder);

							count++;
						}
					}

					if (count >= frameDistances.length)
						break;
				}
			}
		}

		if (count > 0) {
			double[] tmpFrameDistances = new double[count];
			System.arraycopy(frameDistances, 0, tmpFrameDistances, 0, count);
			frameDistances = new double[count];
			System.arraycopy(tmpFrameDistances, 0, frameDistances, 0, count);
		}

		return frameDistances;
	}

	// Implement functionality in derived classes
	public double frameDistance(double[] frm1, double[] frm2, int fftSize, int lpOrder) {
		return 1.0;
	}

	public void mainParametric(String srcFolder, String tgtFolder, String tfmFolder, String outputFile, String infoString)
			throws IOException {
		double[] distances1 = getDistances(tgtFolder, srcFolder);
		double[] distances2 = getDistances(tgtFolder, tfmFolder);

		ComparativeStatisticsItem stats = new ComparativeStatisticsItem(distances1, distances2);
		stats.writeToTextFile(outputFile);

		System.out.println(infoString + " reference-method1: MeanDist=" + String.valueOf(stats.referenceVsMethod1.mean) + " "
				+ "StdDist=" + String.valueOf(stats.referenceVsMethod1.std));
		System.out.println(infoString + " reference-method2: MeanDist=" + String.valueOf(stats.referenceVsMethod2.mean) + " "
				+ "StdDist=" + String.valueOf(stats.referenceVsMethod2.std));
		System.out.println(infoString + " distance reduction="
				+ String.valueOf(stats.referenceVsMethod1.mean - stats.referenceVsMethod2.mean));
	}
}
