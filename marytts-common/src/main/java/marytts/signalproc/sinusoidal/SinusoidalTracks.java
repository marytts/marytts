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
package marytts.signalproc.sinusoidal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.util.math.ComplexArray;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author Oytun T&uuml;rk
 *
 */
public class SinusoidalTracks {
	public SinusoidalTrack[] tracks;
	public int totalTracks;
	public int currentIndex;
	public int fs; // Sampling rate in Hz, you can change this using setSamplingRate to synthesize speech at a different sampling
					// rate
	public float origDur; // Original duration of the signal modeled by sinusoidal tracks in seconds
	public float[] voicings; // Voicing probabilities
	public float absMaxOriginal; // Absolute maximum of the original waveform
	public float totalEnergy; // Total energy of the original waveform

	public Vector<double[]> sysAmps; // System amplitudes for each speech frame
	public Vector<double[]> sysPhases; // System phases for each speech frame
	public Vector<float[]> sysCeps; // System cepstral coeffs for each speech frame
	public Vector<ComplexArray> frameDfts; // System phases for each speech frame
	public float[] times; // Analysis time instants for each speech frame

	public SinusoidalTracks(int len, int samplingRate) {
		initialize(len, samplingRate);
	}

	public SinusoidalTracks(SinusoidalTracks sinTrks) {
		this(sinTrks, 0, sinTrks.totalTracks - 1);
	}

	public SinusoidalTracks(SinusoidalTracks sinTrks, int startIndex, int endIndex) {
		copy(sinTrks, startIndex, endIndex);
	}

	public void setSamplingRate(int samplingRate) {
		fs = samplingRate;
	}

	public void initialize(int len, int samplingRate) {
		if (len > 0) {
			totalTracks = len;
			tracks = new SinusoidalTrack[totalTracks];
		} else {
			totalTracks = 0;
			tracks = null;
		}

		currentIndex = -1;
		origDur = 0.0f;

		setSamplingRate(samplingRate);

		voicings = null;
	}

	// Copy part of the existing tracks in srcTracks into the current tracks
	// starting from startSinIndex and ending at endSinIndex
	// including startSinIndex and endSinIndex
	public void copy(SinusoidalTracks srcTracks, int startTrackIndex, int endTrackIndex) {
		absMaxOriginal = srcTracks.absMaxOriginal;
		totalEnergy = srcTracks.totalEnergy;

		if (startTrackIndex < 0)
			startTrackIndex = 0;
		if (endTrackIndex < 0)
			endTrackIndex = 0;

		if (endTrackIndex > srcTracks.totalTracks - 1)
			endTrackIndex = srcTracks.totalTracks - 1;
		if (startTrackIndex > endTrackIndex)
			startTrackIndex = endTrackIndex;

		if (totalTracks < endTrackIndex - startTrackIndex + 1)
			initialize(endTrackIndex - startTrackIndex + 1, srcTracks.fs);

		if (totalTracks > 0) {
			for (int i = startTrackIndex; i <= endTrackIndex; i++) {
				tracks[i] = new SinusoidalTrack(srcTracks.tracks[i].totalSins);
				tracks[i].copy(srcTracks.tracks[i]);
			}

			currentIndex = endTrackIndex - startTrackIndex;

			if (srcTracks.origDur > origDur)
				origDur = srcTracks.origDur;
		}

		setVoicings(srcTracks.voicings);
		setTimes(srcTracks.times);
		setSystemAmps(srcTracks.sysAmps);
		setSystemPhases(srcTracks.sysPhases);
		setSystemCeps(srcTracks.sysCeps);
		setFrameDfts(srcTracks.frameDfts);
	}

	// Copy existing tracks (srcTracks) into the current tracks
	public void copy(SinusoidalTracks srcTracks) {
		copy(srcTracks, 0, srcTracks.totalTracks - 1);
	}

	// Add a new track to the tracks
	public void add(SinusoidalTrack track) {
		if (currentIndex + 1 >= totalTracks) // Expand the current track twice its length and then add
		{
			if (totalTracks > 0) {
				SinusoidalTracks tmpTracks = new SinusoidalTracks(this);
				if (tmpTracks.totalTracks < 10)
					initialize(2 * tmpTracks.totalTracks, fs);
				else if (tmpTracks.totalTracks < 100)
					initialize(tmpTracks.totalTracks + 20, fs);
				else if (tmpTracks.totalTracks < 1000)
					initialize(tmpTracks.totalTracks + 200, fs);
				else
					initialize(tmpTracks.totalTracks + 2000, fs);

				this.copy(tmpTracks);
			} else
				initialize(1, fs);
		}

		currentIndex++;

		tracks[currentIndex] = new SinusoidalTrack(1);
		tracks[currentIndex].copy(track);

		if (origDur < track.times[track.totalSins - 1])
			origDur = track.times[track.totalSins - 1];
	}

	public void add(float time, Sinusoid[] sins, float maxFreqOfVoicing, int state) {
		for (int i = 0; i < sins.length; i++) {
			SinusoidalTrack tmpTrack = new SinusoidalTrack(time, sins[i], maxFreqOfVoicing, state);
			add(tmpTrack);

			if (time > origDur)
				origDur = time;
		}
	}

	// Update parameters of <index>th track
	public void update(int index, SinusoidalTrack track) {
		if (index < totalTracks)
			tracks[index].copy(track);
	}

	public void getTrackStatistics() {
		getTrackStatistics(-1.0f, -1.0f);
	}

	public void getTrackStatistics(float windowSizeInSeconds, float skipSizeInSeconds) {
		int longest;
		double average;
		int numShorts;
		int shortLim = 5;

		int numLongs;
		int longLim = 15;

		int i, j;

		longest = 0;
		numShorts = 0;
		numLongs = 0;
		average = 0.0;
		for (i = 0; i < totalTracks; i++) {
			if (tracks[i].totalSins > longest)
				longest = tracks[i].totalSins;

			if (tracks[i].totalSins < shortLim)
				numShorts++;

			if (tracks[i].totalSins > longLim)
				numLongs++;

			average += tracks[i].totalSins;
		}

		average /= totalTracks;

		System.out.println("Total tracks = " + String.valueOf(totalTracks));
		if (windowSizeInSeconds > 0 && skipSizeInSeconds > 0)
			System.out.println("Longest track = " + String.valueOf(longest) + " ("
					+ String.valueOf(longest * skipSizeInSeconds + 0.5 * windowSizeInSeconds) + " sec.)");
		else
			System.out.println("Longest track = " + String.valueOf(longest));

		if (windowSizeInSeconds > 0 && skipSizeInSeconds > 0)
			System.out.println("Mean track length = " + String.valueOf(average) + " ("
					+ String.valueOf(average * skipSizeInSeconds + 0.5 * windowSizeInSeconds) + " sec.)");
		else
			System.out.println("Mean track length = " + String.valueOf(average));

		System.out.println("Total tracks shorter than " + String.valueOf(shortLim) + " speech frames = "
				+ String.valueOf(numShorts));
		System.out
				.println("Total tracks longer than " + String.valueOf(longLim) + " speech frames = " + String.valueOf(numLongs));

		for (i = 0; i < totalTracks; i++)
			tracks[i].getStatistics(true, true, fs, i);
	}

	public float getOriginalDuration() {
		return origDur;
	}

	public void setOriginalDurationAuto() {
		for (int i = 0; i < totalTracks; i++) {
			if (tracks[i].times != null && origDur < tracks[i].times[tracks[i].currentIndex])
				origDur = tracks[i].times[tracks[i].currentIndex];
		}
	}

	public void setOriginalDurationManual(float origDurIn) {
		origDur = origDurIn;
	}

	public void setVoicings(float[] voicingsIn) {
		if (voicingsIn != null && voicingsIn.length > 0) {
			voicings = new float[voicingsIn.length];
			System.arraycopy(voicingsIn, 0, voicings, 0, voicingsIn.length);
		} else
			voicings = null;
	}

	public void setTimes(float[] timesIn) {
		if (timesIn != null && timesIn.length > 0) {
			times = new float[timesIn.length];
			System.arraycopy(timesIn, 0, times, 0, timesIn.length);
		} else
			times = null;
	}

	public void setSystemAmps(Vector<double[]> sysAmpsIn) {
		sysAmps = sysAmpsIn;
	}

	public void setSystemPhases(Vector<double[]> sysPhasesIn) {
		sysPhases = sysPhasesIn;
	}

	public void setSystemCeps(Vector<float[]> sysCepsIn) {
		sysCeps = sysCepsIn;
	}

	public void setFrameDfts(Vector<ComplexArray> frameDftsIn) {
		frameDfts = frameDftsIn;
	}

	public void writeToTextFile(String filename) throws IOException {
		File outFile = new File(filename);
		FileWriter out = new FileWriter(outFile);
		String str;

		for (int i = 0; i < this.totalTracks; i++) {
			str = "*** Track index= " + String.valueOf(i) + "\r\n" + "AMP(lin)\tFREQ(Hz)\tPHASE(rad)\tPHASE(°)\tTIME(sec)"
					+ "\r\n";
			out.write(str);

			for (int j = 0; j < tracks[i].totalSins; j++) {
				str = String.format("%1$f", tracks[i].amps[j]) + "\t"
						+ String.format("%1$f", SignalProcUtils.radian2hz(tracks[i].freqs[j], fs)) + "\t"
						+ String.format("%1$f", tracks[i].phases[j]) + "\t"
						+ String.format("%1$f", MathUtils.unwrapToRange(MathUtils.radian2degrees(tracks[i].phases[j]), -180.0f))
						+ "\t" + String.format("%1$f", tracks[i].times[j]) + "\r\n";

				out.write(str);
			}

			str = "********************************************************" + "\r\n";
			out.write(str);
		}

		out.close();
	}

	public void setSysAmpsAndTimes(NonharmonicSinusoidalSpeechFrame[] framesSins) {
		if (framesSins == null || framesSins.length <= 0) {
			sysAmps = null;
			sysPhases = null;
			sysCeps = null;
			frameDfts = null;
			times = null;
		} else {
			sysAmps = new Vector<double[]>();
			sysPhases = new Vector<double[]>();
			sysCeps = new Vector<float[]>();
			frameDfts = new Vector<ComplexArray>();
			times = new float[framesSins.length];

			for (int i = 0; i < framesSins.length; i++) {
				sysAmps.add(framesSins[i].systemAmps);
				sysPhases.add(framesSins[i].systemPhases);
				sysCeps.add(framesSins[i].systemCeps);
				frameDfts.add(framesSins[i].frameDfts);
				times[i] = framesSins[i].time;
			}
		}
	}

	public void setSysAmpsAndTimes(HntmSpeechSignal hntmSignal, HntmAnalyzerParams params) {
		sysAmps = null;
		sysPhases = null;
		frameDfts = null;

		if (hntmSignal == null || hntmSignal.frames == null || hntmSignal.frames.length <= 0) {
			sysCeps = null;
			times = null;
		} else {
			sysCeps = new Vector<float[]>();
			times = new float[hntmSignal.frames.length];

			for (int i = 0; i < hntmSignal.frames.length; i++) {
				sysCeps.add(hntmSignal.frames[i].h.getCeps(hntmSignal.frames[i].f0InHz, hntmSignal.samplingRateInHz, params));
				times[i] = hntmSignal.frames[i].tAnalysisInSeconds;
			}
		}
	}
}
