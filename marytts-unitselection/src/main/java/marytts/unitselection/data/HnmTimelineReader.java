/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.unitselection.data;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Properties;

import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.exceptions.MaryConfigurationException;
import marytts.signalproc.adaptation.prosody.BasicProsodyModifierParams;
import marytts.signalproc.sinusoidal.hntm.analysis.FrameNoisePartWaveform;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechFrame;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizedSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizer;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizerParams;
import marytts.util.data.Datagram;
import marytts.util.io.FileUtils;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;

/**
 * A reader class for the harmonics plus noise timeline file.
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class HnmTimelineReader extends TimelineReader {
	public HntmAnalyzerParams analysisParams;

	public HnmTimelineReader(String fileName) throws IOException, MaryConfigurationException {
		super();
		load(fileName);
	}

	protected void load(String fileName) throws IOException, MaryConfigurationException {
		super.load(fileName);
		// Now make sense of the processing header
		Properties props = new Properties();
		ByteArrayInputStream bais = new ByteArrayInputStream(procHdr.getString().getBytes("latin1"));
		props.load(bais);
		ensurePresent(props, "hnm.noiseModel");

		analysisParams = new HntmAnalyzerParams();

		analysisParams.noiseModel = Integer.parseInt(props.getProperty("hnm.noiseModel"));
		analysisParams.hnmPitchVoicingAnalyzerParams.numFilteringStages = Integer
				.parseInt(props.getProperty("hnm.numFiltStages"));
		analysisParams.hnmPitchVoicingAnalyzerParams.medianFilterLength = Integer
				.parseInt(props.getProperty("hnm.medianFiltLen"));
		analysisParams.hnmPitchVoicingAnalyzerParams.movingAverageFilterLength = Integer.parseInt(props
				.getProperty("hnm.maFiltLen"));
		analysisParams.hnmPitchVoicingAnalyzerParams.cumulativeAmpThreshold = Float.parseFloat(props.getProperty("hnm.cumAmpTh"));
		analysisParams.hnmPitchVoicingAnalyzerParams.maximumAmpThresholdInDB = Float
				.parseFloat(props.getProperty("hnm.maxAmpTh"));
		analysisParams.hnmPitchVoicingAnalyzerParams.harmonicDeviationPercent = Float.parseFloat(props
				.getProperty("hnm.harmDevPercent"));
		analysisParams.hnmPitchVoicingAnalyzerParams.sharpPeakAmpDiffInDB = Float.parseFloat(props
				.getProperty("hnm.sharpPeakAmpDiff"));
		analysisParams.hnmPitchVoicingAnalyzerParams.minimumTotalHarmonics = Integer.parseInt(props
				.getProperty("hnm.minHarmonics"));
		analysisParams.hnmPitchVoicingAnalyzerParams.maximumTotalHarmonics = Integer.parseInt(props
				.getProperty("hnm.maxHarmonics"));
		analysisParams.hnmPitchVoicingAnalyzerParams.minimumVoicedFrequencyOfVoicing = Float.parseFloat(props
				.getProperty("hnm.minVoicedFreq"));
		analysisParams.hnmPitchVoicingAnalyzerParams.maximumVoicedFrequencyOfVoicing = Float.parseFloat(props
				.getProperty("hnm.maxVoicedFreq"));
		analysisParams.hnmPitchVoicingAnalyzerParams.maximumFrequencyOfVoicingFinalShift = Float.parseFloat(props
				.getProperty("hnm.maxFreqVoicingFinalShift"));
		analysisParams.hnmPitchVoicingAnalyzerParams.neighsPercent = Float.parseFloat(props.getProperty("hnm.neighsPercent"));
		analysisParams.harmonicPartCepstrumOrder = Integer.parseInt(props.getProperty("hnm.harmCepsOrder"));
		analysisParams.regularizedCepstrumWarpingMethod = Integer.parseInt(props.getProperty("hnm.regCepWarpMethod"));
		analysisParams.regularizedCepstrumLambdaHarmonic = Float.parseFloat(props.getProperty("hnm.regCepsLambda"));
		analysisParams.noisePartLpOrder = Integer.parseInt(props.getProperty("hnm.noiseLpOrder"));
		analysisParams.preemphasisCoefNoise = Float.parseFloat(props.getProperty("hnm.preCoefNoise"));
		analysisParams.hpfBeforeNoiseAnalysis = Boolean.parseBoolean(props.getProperty("hnm.hpfBeforeNoiseAnalysis"));
		analysisParams.numPeriodsHarmonicsExtraction = Float.parseFloat(props.getProperty("hnm.harmNumPer"));
	}

	private void ensurePresent(Properties props, String key) throws MaryConfigurationException {
		if (!props.containsKey(key))
			throw new MaryConfigurationException("Processing header does not contain required field '" + key + "'");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param bb
	 *            bb
	 * @return d
	 */
	@Override
	protected Datagram getNextDatagram(ByteBuffer bb) {

		Datagram d = null;

		/* If the end of the datagram zone is reached, refuse to read */
		if (bb.position() == bb.limit()) {
			// throw new IndexOutOfBoundsException( "Time out of bounds: you are trying to read a datagram at" +
			// " a time which is bigger than the total timeline duration." );
			return null;
		}
		/* Else, pop the datagram out of the file */
		try {
			d = new HnmDatagram(bb, analysisParams.noiseModel);
		}
		/* Detect a possible EOF encounter */
		catch (IOException e) {
			return null;
		}

		return d;
	}

	private void testSynthesizeFromDatagrams(LinkedList<HnmDatagram> datagrams, int startIndex, int endIndex,
			DataOutputStream output) throws IOException {
		HntmSynthesizer s = new HntmSynthesizer();
		// TODO: These should come from timeline and user choices...
		HntmAnalyzerParams hnmAnalysisParams = new HntmAnalyzerParams();
		HntmSynthesizerParams synthesisParams = new HntmSynthesizerParams();
		BasicProsodyModifierParams pmodParams = new BasicProsodyModifierParams();
		int samplingRateInHz = this.getSampleRate();

		int totalFrm = 0;
		int i;
		float originalDurationInSeconds = 0.0f;
		float deltaTimeInSeconds;

		for (i = startIndex; i <= endIndex; i++) {
			HnmDatagram datagram;
			try {
				datagram = datagrams.get(i);
			} catch (IndexOutOfBoundsException e) {
				throw e;
			}
			if (datagram != null && datagram instanceof HnmDatagram) {
				totalFrm++;
				// deltaTimeInSeconds = SignalProcUtils.sample2time(((HnmDatagram)datagrams.get(i)).getDuration(),
				// samplingRateInHz);
				deltaTimeInSeconds = datagram.frame.deltaAnalysisTimeInSeconds;
				originalDurationInSeconds += deltaTimeInSeconds;
			}
		}

		HntmSpeechSignal hnmSignal = new HntmSpeechSignal(totalFrm, samplingRateInHz, originalDurationInSeconds);

		int frameCount = 0;
		float tAnalysisInSeconds = 0.0f;
		for (i = startIndex; i <= endIndex; i++) {
			HnmDatagram datagram;
			try {
				datagram = datagrams.get(i);
			} catch (IndexOutOfBoundsException e) {
				throw e;
			}
			if (datagram != null && datagram instanceof HnmDatagram) {
				// tAnalysisInSeconds += SignalProcUtils.sample2time(((HnmDatagram)datagrams.get(i)).getDuration(),
				// samplingRateInHz);
				tAnalysisInSeconds += datagram.getFrame().deltaAnalysisTimeInSeconds;

				if (frameCount < totalFrm) {
					hnmSignal.frames[frameCount] = new HntmSpeechFrame(datagram.getFrame());
					hnmSignal.frames[frameCount].tAnalysisInSeconds = tAnalysisInSeconds;
					frameCount++;
				}
			}
		}

		HntmSynthesizedSignal ss = null;
		if (totalFrm > 0) {
			ss = s.synthesize(hnmSignal, null, null, pmodParams, null, hnmAnalysisParams, synthesisParams);
			FileUtils.writeBinaryFile(ArrayUtils.copyDouble2Short(ss.output), output);
			if (ss.output != null) {
				ss.output = MathUtils.multiply(ss.output, 1.0 / 32768.0); // why is this done here?
			}
		}
		return;
	}

	/**
	 * Dump audio from HNM timeline to a series big-endian raw audio files in chunks of Datagrams (<tt>clusterSize</tt>). Run this
	 * with
	 * 
	 * <pre>
	 * -ea - Xmx2gb
	 * </pre>
	 * 
	 * @param args
	 *            <ol>
	 *            <li>path to <tt>timeline_hnm.mry</tt> file</li>
	 *            <li>path to dump output files</li>
	 *            </ol>
	 * @throws UnsupportedAudioFileException
	 *             UnsupportedAudioFileException
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public static void main(String[] args) throws UnsupportedAudioFileException, IOException, MaryConfigurationException {
		HnmTimelineReader h = new HnmTimelineReader(args[0]);

		LinkedList<HnmDatagram> datagrams = new LinkedList<HnmDatagram>();
		int count = 0;
		long startDatagramTime = 0;
		int numDatagrams = (int) h.numDatagrams;
		// long numDatagrams = 2000;

		Datagram[] rawDatagrams = h.getDatagrams(0l, numDatagrams, h.getSampleRate(), null);
		for (int i = 0; i < rawDatagrams.length; i++) {
			HnmDatagram d = (HnmDatagram) rawDatagrams[i];
			datagrams.add(d);
			count++;
			System.out.println("Datagram " + String.valueOf(count) + "Noise waveform size="
					+ ((FrameNoisePartWaveform) (((HnmDatagram) d).frame.n)).waveform().length);
		}

		int clusterSize = 1000;
		int numClusters = (int) Math.floor((numDatagrams) / ((double) clusterSize) + 0.5);
		int startIndex, endIndex;
		for (int i = 0; i < numClusters; i++) {
			DataOutputStream output = new DataOutputStream(new FileOutputStream(
					new File(String.format("%s_%06d.bin", args[1], i))));
			startIndex = (int) (i * clusterSize);
			endIndex = (int) Math.min((i + 1) * clusterSize - 1, numDatagrams - 1);
			h.testSynthesizeFromDatagrams(datagrams, startIndex, endIndex, output);
			System.out.println("Timeline cluster " + String.valueOf(i + 1) + " of " + String.valueOf(numClusters)
					+ " synthesized...");
			output.close();
		}
	}
}
