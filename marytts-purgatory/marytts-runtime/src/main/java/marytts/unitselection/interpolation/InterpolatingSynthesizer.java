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
package marytts.unitselection.interpolation;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioInputStream;

import marytts.datatypes.MaryXML;
import marytts.exceptions.SynthesisException;
import marytts.modules.synthesis.Voice;
import marytts.modules.synthesis.WaveformSynthesizer;
import marytts.signalproc.process.FramewiseMerger;
import marytts.signalproc.process.LSFInterpolator;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.concat.BaseUnitConcatenator;
import marytts.unitselection.concat.UnitConcatenator;
import marytts.unitselection.select.SelectedUnit;
import marytts.unitselection.select.UnitSelector;
import marytts.util.MaryUtils;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.Datagram;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.dom.MaryDomUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

/**
 * @author marc
 *
 */
public class InterpolatingSynthesizer implements WaveformSynthesizer {
	protected Logger logger;

	/**
     * 
     */
	public InterpolatingSynthesizer() {
	}

	/**
	 * Start up the waveform synthesizer. This must be called once before calling synthesize().
	 * 
	 * @throws Exception
	 *             Exception
	 */
	public void startup() throws Exception {
		logger = MaryUtils.getLogger("InterpolatingSynthesizer");
		// Register interpolating voice:
		Voice.registerVoice(new InterpolatingVoice(this, "interpolatingvoice"));
		logger.info("started.");
	}

	/**
	 * Perform a power-on self test by processing some example input data.
	 * 
	 * @throws Error
	 *             if the module does not work properly.
	 */
	public void powerOnSelfTest() throws Error {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param tokensAndBoundaries
	 *            tokensAndBoundaries
	 * @param voice
	 *            voice
	 * @param outputParams
	 *            outputParams
	 * @throws SynthesisException
	 *             SynthesisException
	 * @return outputAudio
	 */
	public AudioInputStream synthesize(List<Element> tokensAndBoundaries, Voice voice, String outputParams)
			throws SynthesisException {
		if (tokensAndBoundaries.size() == 0)
			return null;

		// 1. determine the two voices involved;
		Element first = (Element) tokensAndBoundaries.get(0);
		Element voiceElement = (Element) MaryDomUtils.getAncestor(first, MaryXML.VOICE);
		String name = voiceElement.getAttribute("name");
		// name has the form: voice1 with XY% voice2
		// We trust that InerpolatingVoice.hasName() has done its job to verify that
		// name actually has that form.
		String[] parts = name.split("\\s+");
		assert parts.length == 4;
		assert parts[1].equals("with");
		assert parts[2].endsWith("%");
		int percent = Integer.parseInt(parts[2].substring(0, parts[2].length() - 1));
		Voice voice1 = Voice.getVoice(parts[0]);
		assert voice1 != null;
		Voice voice2 = Voice.getVoice(parts[3]);
		assert voice2 != null;

		// 2. do unit selection with each;
		if (!(voice1 instanceof UnitSelectionVoice)) {
			throw new IllegalArgumentException("Voices of type " + voice.getClass().getName() + " not supported!");
		}
		if (!(voice2 instanceof UnitSelectionVoice)) {
			throw new IllegalArgumentException("Voices of type " + voice.getClass().getName() + " not supported!");
		}
		UnitSelectionVoice usv1 = (UnitSelectionVoice) voice1;
		UnitSelectionVoice usv2 = (UnitSelectionVoice) voice2;

		UnitSelector unitSel1 = usv1.getUnitSelector();
		List<SelectedUnit> selectedUnits1 = unitSel1.selectUnits(tokensAndBoundaries, voice);
		UnitSelector unitSel2 = usv2.getUnitSelector();
		List<SelectedUnit> selectedUnits2 = unitSel2.selectUnits(tokensAndBoundaries, voice);
		assert selectedUnits1.size() == selectedUnits2.size() : "Unexpected difference in number of units: "
				+ selectedUnits1.size() + " vs. " + selectedUnits2.size();
		int numUnits = selectedUnits1.size();

		// 3. do unit concatenation with each, retrieve actual unit durations from list of units;
		UnitConcatenator unitConcatenator1 = usv1.getConcatenator();
		AudioInputStream audio1;
		try {
			audio1 = unitConcatenator1.getAudio(selectedUnits1);
		} catch (IOException ioe) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			for (Iterator selIt = selectedUnits1.iterator(); selIt.hasNext();)
				pw.println(selIt.next());
			throw new SynthesisException("For voice " + voice1.getName() + ", problems generating audio for unit chain: "
					+ sw.toString(), ioe);
		}
		DoubleDataSource audioSource1 = new AudioDoubleDataSource(audio1);
		UnitConcatenator unitConcatenator2 = usv2.getConcatenator();
		AudioInputStream audio2;
		try {
			audio2 = unitConcatenator2.getAudio(selectedUnits2);
		} catch (IOException ioe) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			for (Iterator selIt = selectedUnits2.iterator(); selIt.hasNext();)
				pw.println(selIt.next());
			throw new SynthesisException("For voice " + voice2.getName() + ", problems generating audio for unit chain: "
					+ sw.toString(), ioe);
		}
		DoubleDataSource audioSource2 = new AudioDoubleDataSource(audio2);
		// Retrieve actual durations from list of units:
		int sampleRate1 = (int) usv1.dbAudioFormat().getSampleRate();
		double[] label1 = new double[numUnits];
		double t1 = 0;
		int sampleRate2 = (int) usv2.dbAudioFormat().getSampleRate();
		double[] label2 = new double[numUnits];
		double t2 = 0;
		for (int i = 0; i < numUnits; i++) {
			SelectedUnit u1 = selectedUnits1.get(i);
			SelectedUnit u2 = selectedUnits2.get(i);
			BaseUnitConcatenator.UnitData ud1 = (BaseUnitConcatenator.UnitData) u1.getConcatenationData();
			int unitDuration1 = ud1.getUnitDuration();
			if (unitDuration1 < 0) { // was not set by the unit concatenator, have to count ourselves
				unitDuration1 = 0;
				Datagram[] d = ud1.getFrames();
				for (int id = 0; id < d.length; id++) {
					unitDuration1 += d[id].getDuration();
				}
			}
			t1 += unitDuration1 / (float) sampleRate1;
			label1[i] = t1;
			BaseUnitConcatenator.UnitData ud2 = (BaseUnitConcatenator.UnitData) u2.getConcatenationData();
			int unitDuration2 = ud2.getUnitDuration();
			if (unitDuration2 < 0) { // was not set by the unit concatenator, have to count ourselves
				unitDuration2 = 0;
				Datagram[] d = ud2.getFrames();
				for (int id = 0; id < d.length; id++) {
					unitDuration2 += d[id].getDuration();
				}
			}
			t2 += unitDuration2 / (float) sampleRate2;
			label2[i] = t2;
			logger.debug(usv1.getName() + " [" + u1.getTarget() + "] " + label1[i] + " -- " + usv2.getName() + " ["
					+ u2.getTarget() + "] " + label2[i]);
		}

		// 4. with these unit durations, run the LSFInterpolator on the two audio streams.
		int frameLength = Integer.getInteger("signalproc.lpcanalysisresynthesis.framelength", 512).intValue();
		int predictionOrder = Integer.getInteger("signalproc.lpcanalysisresynthesis.predictionorder", 20).intValue();
		double r = (double) percent / 100;
		assert r >= 0;
		assert r <= 1;
		FramewiseMerger foas = new FramewiseMerger(audioSource1, frameLength, sampleRate1, new BufferedDoubleDataSource(label1),
				audioSource2, sampleRate2, new BufferedDoubleDataSource(label2), new LSFInterpolator(predictionOrder, r));
		DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), audio1.getFormat());

		return outputAudio;
	}

}
