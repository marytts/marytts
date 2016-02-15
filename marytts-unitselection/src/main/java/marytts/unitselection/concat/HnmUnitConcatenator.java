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

package marytts.unitselection.concat;

import java.io.IOException;
import java.util.List;

import javax.sound.sampled.AudioInputStream;

import marytts.signalproc.adaptation.prosody.BasicProsodyModifierParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechFrame;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizedSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizer;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizerParams;
import marytts.unitselection.data.HnmDatagram;
import marytts.unitselection.data.Unit;
import marytts.unitselection.select.SelectedUnit;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.Datagram;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;

/**
 * A unit concatenator for harmonics plus noise based speech synthesis
 * 
 * @author Oytun T&uuml;rk
 *
 */
public class HnmUnitConcatenator extends OverlapUnitConcatenator {

	public HnmUnitConcatenator() {
		super();
	}

	/**
	 * Get the raw audio material for each unit from the timeline.
	 * 
	 * @param units
	 *            units
	 */
	protected void getDatagramsFromTimeline(List<SelectedUnit> units) throws IOException {
		for (SelectedUnit unit : units) {
			assert !unit.getUnit().isEdgeUnit() : "We should never have selected any edge units!";
			HnmUnitData unitData = new HnmUnitData();
			unit.setConcatenationData(unitData);
			int nSamples = 0;
			int unitSize = unitToTimeline(unit.getUnit().duration); // convert to timeline samples
			long unitStart = unitToTimeline(unit.getUnit().startTime); // convert to timeline samples
			// System.out.println("Unit size "+unitSize+", pitchmarksInUnit "+pitchmarksInUnit);
			// System.out.println(unitStart/((float)timeline.getSampleRate()));
			// System.out.println("Unit index = " + unit.getUnit().getIndex());

			Datagram[] datagrams = timeline.getDatagrams(unitStart, (long) unitSize);
			unitData.setFrames(datagrams);

			// one left context period for windowing:
			Datagram leftContextFrame = null;
			Unit prevInDB = database.getUnitFileReader().getPreviousUnit(unit.getUnit());
			long unitPrevStart = unitToTimeline(prevInDB.startTime); // convert to timeline samples
			if (prevInDB != null && !prevInDB.isEdgeUnit()) {
				long unitPrevSize = unitToTimeline(prevInDB.duration);
				Datagram[] unitPrevDatagrams = timeline.getDatagrams(unitPrevStart, (long) unitPrevSize);
				// leftContextFrame = timeline.getDatagram(unitPrevStart);
				if (unitPrevDatagrams != null && unitPrevDatagrams.length > 0) {
					leftContextFrame = unitPrevDatagrams[unitPrevDatagrams.length - 1];
				}
				unitData.setLeftContextFrame(leftContextFrame);
			}

			// one right context period for windowing:
			Datagram rightContextFrame = null;
			Unit nextInDB = database.getUnitFileReader().getNextUnit(unit.getUnit());
			if (nextInDB != null && !nextInDB.isEdgeUnit()) {
				rightContextFrame = timeline.getDatagram(unitStart + unitSize);
				unitData.setRightContextFrame(rightContextFrame);
			}
		}
	}

	/**
	 * Generate audio to match the target pitchmarks as closely as possible.
	 * 
	 * @param units
	 *            units
	 * @return new DDSAudioInputStream(audioSource, audioformat)
	 */
	protected AudioInputStream generateAudioStream(List<SelectedUnit> units) {
		int len = units.size();
		Datagram[][] datagrams = new Datagram[len][];
		Datagram[] leftContexts = new Datagram[len];
		Datagram[] rightContexts = new Datagram[len];
		for (int i = 0; i < len; i++) {
			SelectedUnit unit = units.get(i);
			HnmUnitData unitData = (HnmUnitData) unit.getConcatenationData();
			assert unitData != null : "Should not have null unitdata here";
			Datagram[] frames = unitData.getFrames();
			assert frames != null : "Cannot generate audio from null frames";
			// Generate audio from frames
			datagrams[i] = frames;

			Unit prevInDB = database.getUnitFileReader().getPreviousUnit(unit.getUnit());
			Unit prevSelected;
			if (i == 0)
				prevSelected = null;
			else
				prevSelected = units.get(i - 1).getUnit();
			if (prevInDB != null && !prevInDB.equals(prevSelected)) {
				// Only use left context if we have a previous unit in the DB is not the
				// same as the previous selected unit.
				leftContexts[i] = (HnmDatagram) unitData.getLeftContextFrame(); // may be null
			}

			Unit nextInDB = database.getUnitFileReader().getNextUnit(unit.getUnit());
			Unit nextSelected;
			if (i + 1 == len)
				nextSelected = null;
			else
				nextSelected = units.get(i + 1).getUnit();
			if (nextInDB != null && !nextInDB.equals(nextSelected)) {
				// Only use right context if we have a next unit in the DB is not the
				// same as the next selected unit.
				rightContexts[i] = unitData.getRightContextFrame(); // may be null
			}
		}

		BufferedDoubleDataSource audioSource = synthesize(datagrams, leftContexts, rightContexts);
		return new DDSAudioInputStream(audioSource, audioformat);
	}

	protected BufferedDoubleDataSource synthesize(Datagram[][] datagrams, Datagram[] leftContexts, Datagram[] rightContexts) {
		HntmSynthesizer s = new HntmSynthesizer();
		// TO DO: These should come from timeline and user choices...
		HntmAnalyzerParams analysisParams = new HntmAnalyzerParams();
		HntmSynthesizerParams synthesisParams = new HntmSynthesizerParams();
		BasicProsodyModifierParams pmodParams = new BasicProsodyModifierParams();
		int samplingRateInHz = 16000;

		int totalFrm = 0;
		int i, j;
		float originalDurationInSeconds = 0.0f;
		float deltaTimeInSeconds;
		long length;

		for (i = 0; i < datagrams.length; i++) {
			for (j = 0; j < datagrams[i].length; j++) {
				if (datagrams[i][j] != null && (datagrams[i][j] instanceof HnmDatagram)) {
					totalFrm++;
					length = ((HnmDatagram) datagrams[i][j]).getDuration();
					// deltaTimeInSeconds = SignalProcUtils.sample2time(length, samplingRateInHz);
					deltaTimeInSeconds = ((HntmSpeechFrame) ((HnmDatagram) datagrams[i][j]).getFrame()).deltaAnalysisTimeInSeconds;
					originalDurationInSeconds += deltaTimeInSeconds;

					// System.out.println("Unit duration = " + String.valueOf(length));
				}
			}
		}

		HntmSpeechSignal hnmSignal = null;
		hnmSignal = new HntmSpeechSignal(totalFrm, samplingRateInHz, originalDurationInSeconds);
		HntmSpeechFrame[] leftContextFrames = new HntmSpeechFrame[totalFrm];
		HntmSpeechFrame[] rightContextFrames = new HntmSpeechFrame[totalFrm];
		//

		int frameCount = 0;
		float tAnalysisInSeconds = 0.0f;
		for (i = 0; i < datagrams.length; i++) {
			for (j = 0; j < datagrams[i].length; j++) {
				if (datagrams[i][j] != null && (datagrams[i][j] instanceof HnmDatagram) && frameCount < totalFrm) {
					tAnalysisInSeconds += ((HntmSpeechFrame) ((HnmDatagram) datagrams[i][j]).getFrame()).deltaAnalysisTimeInSeconds;

					hnmSignal.frames[frameCount] = ((HnmDatagram) datagrams[i][j]).getFrame();
					hnmSignal.frames[frameCount].tAnalysisInSeconds = tAnalysisInSeconds;

					// tAnalysisInSeconds += SignalProcUtils.sample2time(((HnmDatagram)datagrams[i][j]).getDuration(),
					// samplingRateInHz);

					if (j == 0) {
						if (leftContexts[i] != null && (leftContexts[i] instanceof HnmDatagram))
							leftContextFrames[frameCount] = ((HnmDatagram) leftContexts[i]).getFrame();
					} else {
						if (datagrams[i][j - 1] != null && (datagrams[i][j - 1] instanceof HnmDatagram))
							leftContextFrames[frameCount] = ((HnmDatagram) datagrams[i][j - 1]).getFrame();
					}

					if (j == datagrams[i].length - 1) {
						if (rightContexts[i] != null && (rightContexts[i] instanceof HnmDatagram))
							rightContextFrames[frameCount] = ((HnmDatagram) rightContexts[i]).getFrame();
					} else {
						if (datagrams[i][j + 1] != null && (datagrams[i][j + 1] instanceof HnmDatagram))
							rightContextFrames[frameCount] = ((HnmDatagram) datagrams[i][j + 1]).getFrame();
					}

					frameCount++;
				}
			}
		}

		HntmSynthesizedSignal ss = null;
		if (totalFrm > 0) {
			ss = s.synthesize(hnmSignal, leftContextFrames, rightContextFrames, pmodParams, null, analysisParams, synthesisParams);
			// FileUtils.writeTextFile(hnmSignal.getAnalysisTimes(), "d:\\hnmAnalysisTimes1.txt");
			// FileUtils.writeTextFile(ss.output, "d:\\output.txt");
			if (ss.output != null)
				ss.output = MathUtils.multiply(ss.output, 1.0 / 32768.0);
		}

		if (ss != null && ss.output != null)
			return new BufferedDoubleDataSource(ss.output);
		else
			return null;
	}

	public static class HnmUnitData extends OverlapUnitConcatenator.OverlapUnitData {
		protected Datagram leftContextFrame;

		public void setLeftContextFrame(Datagram aLeftContextFrame) {
			this.leftContextFrame = aLeftContextFrame;
		}

		public Datagram getLeftContextFrame() {
			return leftContextFrame;
		}
	}
}
