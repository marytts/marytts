/**
 * Copyright 2010 DFKI GmbH.
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

package marytts.unitselection.analysis;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;

import marytts.exceptions.MaryConfigurationException;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechFrame;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizedSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizer;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizerParams;
import marytts.unitselection.data.HnmDatagram;
import marytts.unitselection.data.HnmTimelineReader;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.Datagram;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;

/**
 * Convenience class to dump relevant data from a HNM unit selection voice to a Praat TextGrid and a wav file for inspection of
 * timeline data in external tools (e.g. Praat, WaveSurfer, etc.)
 * 
 * @author steiner
 * 
 */
public class HnmVoiceDataDumper extends VoiceDataDumper {

	private AudioFormat audioformat;

	public HnmVoiceDataDumper() {
		super();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Also set the audioFormat needed in {@link #getSamples(Datagram[])}
	 */
	@Override
	protected HnmTimelineReader loadAudioTimeline(String fileName) throws IOException, MaryConfigurationException {
		HnmTimelineReader audioTimeline = new HnmTimelineReader(fileName);
		int sampleRate = audioTimeline.getSampleRate();
		this.audioformat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, // encoding
				sampleRate, // samples per second
				16, // bits per sample
				1, // mono
				2, // nr. of bytes per frame
				sampleRate, // nr. of frames per second
				true); // big-endian;
		return audioTimeline;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * For {@link HnmDatagram}s, the samples must be resynthesized from the HntmSpeechFrame in each HnmDatagram. This requires
	 * quite a bit of processing.
	 */
	@Override
	protected byte[] getSamples(Datagram[] datagrams) throws IOException {
		// init required objects:
		HntmSynthesizer hnmSynthesizer = new HntmSynthesizer();
		HntmAnalyzerParams hnmAnalysisParams = new HntmAnalyzerParams();
		HntmSynthesizerParams hnmSynthesisParams = new HntmSynthesizerParams();

		// get duration from datagrams:
		float originalDurationInSeconds = 0;
		for (Datagram datagram : datagrams) {
			HnmDatagram hnmDatagram = (HnmDatagram) datagram;
			originalDurationInSeconds += hnmDatagram.getFrame().deltaAnalysisTimeInSeconds;
		}

		// generate HNM signal from frames, correcting the analysis times:
		HntmSpeechSignal hnmSpeechSignal = new HntmSpeechSignal(datagrams.length, unitDB.getAudioTimeline().getSampleRate(),
				originalDurationInSeconds);
		float tAnalysisInSeconds = 0;
		for (int i = 0; i < datagrams.length; i++) {
			HntmSpeechFrame hnmSpeechFrame = ((HnmDatagram) datagrams[i]).getFrame();
			// correct analysis time:
			tAnalysisInSeconds += hnmSpeechFrame.deltaAnalysisTimeInSeconds;
			hnmSpeechFrame.tAnalysisInSeconds = tAnalysisInSeconds;
			hnmSpeechSignal.frames[i] = hnmSpeechFrame;
		}

		// synthesize signal
		HntmSynthesizedSignal hnmSynthesizedSignal = hnmSynthesizer.synthesize(hnmSpeechSignal, null, null, null, null,
				hnmAnalysisParams, hnmSynthesisParams);

		// scale amplitude:
		double[] output = MathUtils.multiply(hnmSynthesizedSignal.output, 1.0 / 32768.0);

		// repack output into byte array:
		BufferedDoubleDataSource buffer = new BufferedDoubleDataSource(output);
		DDSAudioInputStream audio = new DDSAudioInputStream(buffer, audioformat);
		byte[] samples = new byte[(int) audio.getFrameLength() * audioformat.getFrameSize()];
		audio.read(samples);
		return samples;
	}

	public static void main(String[] args) throws Exception {
		new HnmVoiceDataDumper().dumpData(args[0]);
	}

}
