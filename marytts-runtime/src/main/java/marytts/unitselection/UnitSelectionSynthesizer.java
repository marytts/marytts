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
package marytts.unitselection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.SynthesisException;
import marytts.modules.synthesis.Voice;
import marytts.modules.synthesis.WaveformSynthesizer;
import marytts.modules.synthesis.Voice.Gender;
import marytts.server.MaryProperties;
import marytts.unitselection.concat.UnitConcatenator;
import marytts.unitselection.concat.BaseUnitConcatenator.UnitData;
import marytts.unitselection.data.Unit;
import marytts.unitselection.data.UnitDatabase;
import marytts.unitselection.select.HalfPhoneTarget;
import marytts.unitselection.select.SelectedUnit;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.UnitSelector;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryNormalisedWriter;
import marytts.util.dom.NameNodeFilter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

/**
 * Builds and synthesizes unit selection voices
 * 
 * @author Marc Schr&ouml;der, Anna Hunecke
 *
 */

public class UnitSelectionSynthesizer implements WaveformSynthesizer {
	/**
	 * A map with Voice objects as keys, and Lists of UtteranceProcessors as values. Idea: For a given voice, find the list of
	 * utterance processors to apply.
	 */
	private Logger logger;

	public UnitSelectionSynthesizer() {
	}

	/**
	 * Start up the waveform synthesizer. This must be called once before calling synthesize().
	 * 
	 * @throws Exception
	 *             Exception
	 */
	public void startup() throws Exception {
		logger = MaryUtils.getLogger("UnitSelectionSynthesizer");
		// Register UnitSelection voices:
		logger.debug("Register UnitSelection voices:");
		List<String> voiceNames = MaryProperties.getList("unitselection.voices.list");
		for (String voiceName : voiceNames) {
			long time = System.currentTimeMillis();
			Voice unitSelVoice = new UnitSelectionVoice(voiceName, this);
			logger.debug("Voice '" + unitSelVoice + "'");
			Voice.registerVoice(unitSelVoice);
			long newtime = System.currentTimeMillis() - time;
			logger.info("Loading of voice " + voiceName + " took " + newtime + " milliseconds");
		}
		logger.info("started.");
	}

	/**
	 * Perform a power-on self test by processing some example input data.
	 * 
	 * @throws Error
	 *             if the module does not work properly.
	 */
	public void powerOnSelfTest() throws Error {
		try {
			Collection myVoices = Voice.getAvailableVoices(this);
			if (myVoices.size() == 0) {
				return;
			}
			UnitSelectionVoice unitSelVoice = (UnitSelectionVoice) myVoices.iterator().next();
			assert unitSelVoice != null;
			MaryData in = new MaryData(MaryDataType.get("ACOUSTPARAMS"), unitSelVoice.getLocale());
			if (!unitSelVoice.getDomain().equals("general")) {
				logger.info("Cannot perform power-on self test using limited-domain voice '" + unitSelVoice.getName()
						+ "' - skipping.");
				return;
			}
			String exampleText = MaryDataType.ACOUSTPARAMS.exampleText(unitSelVoice.getLocale());
			if (exampleText != null) {
				in.readFrom(new StringReader(exampleText));
				in.setDefaultVoice(unitSelVoice);
				if (in == null) {
					System.out.println(exampleText + " is null");
				}
				List<Element> tokensAndBoundaries = new ArrayList<Element>();
				TreeWalker tw = ((DocumentTraversal) in.getDocument()).createTreeWalker(in.getDocument(),
						NodeFilter.SHOW_ELEMENT, new NameNodeFilter(new String[] { MaryXML.TOKEN, MaryXML.BOUNDARY }), false);
				Element el = null;
				while ((el = (Element) tw.nextNode()) != null)
					tokensAndBoundaries.add(el);
				AudioInputStream ais = synthesize(tokensAndBoundaries, unitSelVoice, null);
				assert ais != null;
			} else {
				logger.debug("No example text -- no power-on self test!");
			}
		} catch (Throwable t) {
			t.printStackTrace();
			throw new Error("Module " + toString() + ": Power-on self test failed.", t);
		}
		logger.info("Power-on self test complete.");
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
	 * @return audio
	 */
	public AudioInputStream synthesize(List<Element> tokensAndBoundaries, Voice voice, String outputParams)
			throws SynthesisException {
		assert voice instanceof UnitSelectionVoice;
		UnitSelectionVoice v = (UnitSelectionVoice) voice;
		UnitDatabase udb = v.getDatabase();
		// Select:
		UnitSelector unitSel = v.getUnitSelector();
		UnitConcatenator unitConcatenator;
		if (outputParams != null && outputParams.contains("MODIFICATION")) {
			unitConcatenator = v.getModificationConcatenator();
		} else {
			unitConcatenator = v.getConcatenator();
		}
		// TODO: check if we actually need to access v.getDatabase() here
		UnitDatabase database = v.getDatabase();
		logger.debug("Selecting units with a " + unitSel.getClass().getName() + " from a " + database.getClass().getName());
		List<SelectedUnit> selectedUnits = unitSel.selectUnits(tokensAndBoundaries, voice);
		// if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
		// StringWriter sw = new StringWriter();
		// PrintWriter pw = new PrintWriter(sw);
		// for (Iterator selIt=selectedUnits.iterator(); selIt.hasNext(); )
		// pw.println(selIt.next());
		// logger.debug("Units selected:\n"+sw.toString());
		// }

		// Concatenate:
		logger.debug("Now creating audio with a " + unitConcatenator.getClass().getName());
		AudioInputStream audio = null;
		try {
			audio = unitConcatenator.getAudio(selectedUnits);
		} catch (IOException ioe) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			for (Iterator selIt = selectedUnits.iterator(); selIt.hasNext();)
				pw.println(selIt.next());
			throw new SynthesisException("Problems generating audio for unit chain: " + sw.toString(), ioe);
		}

		// Propagate unit durations to XML tree:
		float endInSeconds = 0;
		float durLeftHalfInSeconds = 0;
		String unitString = "";
		String unitAttrName = "units"; // name of the attribute that is added for unit selection diagnostics
		for (SelectedUnit su : selectedUnits) {
			Target t = su.getTarget();
			boolean halfphone = (t instanceof HalfPhoneTarget);
			Object concatenationData = su.getConcatenationData();
			assert concatenationData instanceof UnitData;
			UnitData unitData = (UnitData) concatenationData;
			Unit unit = su.getUnit();

			// For the unit durations, keep record in floats because of precision;
			// convert to millis only at export time, and re-compute duration in millis
			// from the end in millis, to avoid discrepancies due to rounding
			int unitDurationInSamples = unitData.getUnitDuration();
			float unitDurationInSeconds = unitDurationInSamples / (float) database.getUnitFileReader().getSampleRate();
			int prevEndInMillis = (int) (1000 * endInSeconds);
			endInSeconds += unitDurationInSeconds;
			int endInMillis = (int) (1000 * endInSeconds);
			int unitDurationInMillis = endInMillis - prevEndInMillis;
			unitString = t.getName() + " " + udb.getFilename(unit) + " " + unit.index + " " + unitDurationInSeconds;
			if (halfphone) {
				if (((HalfPhoneTarget) t).isLeftHalf()) {
					durLeftHalfInSeconds = unitDurationInSeconds;
				} else { // right half
					// re-compute unit duration from both halves
					float totalUnitDurInSeconds = durLeftHalfInSeconds + unitDurationInSeconds;
					float prevEndInSeconds = endInSeconds - totalUnitDurInSeconds;
					prevEndInMillis = (int) (1000 * prevEndInSeconds);
					unitDurationInMillis = endInMillis - prevEndInMillis;
					durLeftHalfInSeconds = 0;
				}
			}

			Element maryxmlElement = t.getMaryxmlElement();
			if (maryxmlElement != null) {
				if (maryxmlElement.getNodeName().equals(MaryXML.PHONE)) {
					if (!maryxmlElement.hasAttribute("d") || !maryxmlElement.hasAttribute("end")) {
						throw new IllegalStateException("No duration information in MaryXML -- check log file"
								+ " for messages warning about unloadable acoustic models"
								+ " instead of voice-specific acoustic feature predictors");
					}
					// int oldD = Integer.parseInt(maryxmlElement.getAttribute("d"));
					// int oldEnd = Integer.parseInt(maryxmlElement.getAttribute("end"));
					// double doubleEnd = Double.parseDouble(maryxmlElement.getAttribute("end"));
					// int oldEnd = (int)(doubleEnd * 1000);
					maryxmlElement.setAttribute("d", String.valueOf(unitDurationInMillis));
					maryxmlElement.setAttribute("end", String.valueOf(endInSeconds));
					// the following messes up all end values!
					// if (oldEnd == oldD) {
					// // start new end computation
					// endInSeconds = unitDurationInSeconds;
					// }
				} else { // not a PHONE
					assert maryxmlElement.getNodeName().equals(MaryXML.BOUNDARY);
					maryxmlElement.setAttribute("duration", String.valueOf(unitDurationInMillis));
				}
				if (maryxmlElement.hasAttribute(unitAttrName)) {
					String prevUnitString = maryxmlElement.getAttribute(unitAttrName);
					maryxmlElement.setAttribute(unitAttrName, prevUnitString + "; " + unitString);
				} else {
					maryxmlElement.setAttribute(unitAttrName, unitString);
				}
			} else {
				logger.debug("Unit " + su.getTarget().getName() + " of length " + unitDurationInMillis
						+ " ms has no maryxml element.");
			}
		}
		if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
			try {
				MaryNormalisedWriter writer = new MaryNormalisedWriter();
				ByteArrayOutputStream debugOut = new ByteArrayOutputStream();
				writer.output(tokensAndBoundaries.get(0).getOwnerDocument(), debugOut);
				logger.debug("Propagating the realised unit durations to the XML tree: \n" + debugOut.toString());
			} catch (Exception e) {
				logger.warn("Problem writing XML to logfile: " + e);
			}
		}

		return audio;
	}

}
