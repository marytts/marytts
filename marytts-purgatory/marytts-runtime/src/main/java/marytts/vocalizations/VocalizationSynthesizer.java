/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.vocalizations;

import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;

import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.features.FeatureDefinition;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.unitselection.data.Unit;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

/**
 * The vocalization synthesis module.
 * 
 * @author Sathish Pammi
 */

public class VocalizationSynthesizer {

	protected VocalizationSynthesisTechnology vSynthesizer;
	protected VocalizationSelector vSelector;
	protected VocalizationUnitFileReader unitFileReader;
	protected boolean f0ContourImposeSupport;

	protected Logger logger = MaryUtils.getLogger("Vocalization Synthesizer");

	public VocalizationSynthesizer(Voice voice) throws MaryConfigurationException {

		if (!voice.hasVocalizationSupport()) {
			throw new MaryConfigurationException("This voice " + voice.toString() + " doesn't support synthesis of vocalizations");
		}

		String unitFileName = MaryProperties.getFilename("voice." + voice.getName() + ".vocalization.unitfile");

		try {
			this.unitFileReader = new VocalizationUnitFileReader(unitFileName);
		} catch (IOException e) {
			throw new MaryConfigurationException("can't read unit file");
		}

		String intonationFile = MaryProperties.getFilename("voice." + voice.getName() + ".vocalization.intonationfile");
		String technology = MaryProperties.getProperty("voice." + voice.getName() + ".vocalization.synthesisTechnology",
				"fdpsola");
		f0ContourImposeSupport = MaryProperties.getBoolean("voice." + voice.getName() + ".f0ContourImposeSupport", false);

		if ("fdpsola".equals(technology)) {
			String timelineFile = MaryProperties.getFilename("voice." + voice.getName() + ".vocalization.timeline");
			vSynthesizer = new FDPSOLASynthesisTechnology(timelineFile, unitFileName, intonationFile, f0ContourImposeSupport);
		} else if ("mlsa".equals(technology)) {
			boolean imposePolynomialContour = MaryProperties.getBoolean("voice." + voice.getName()
					+ ".vocalization.imposePolynomialContour", true);
			String mlsaFeatureFile = MaryProperties.getFilename("voice." + voice.getName() + ".vocalization.mlsafeaturefile");
			String mixedExcitationFilter = MaryProperties.getFilename("voice." + voice.getName()
					+ ".vocalization.mixedexcitationfilter");
			vSynthesizer = new MLSASynthesisTechnology(mlsaFeatureFile, intonationFile, mixedExcitationFilter,
					imposePolynomialContour);
		} else if ("hnm".equals(technology)) {
			String timelineFile = MaryProperties.getFilename("voice." + voice.getName() + ".vocalization.timeline");
			String hnmFeatureFile = MaryProperties.getFilename("voice." + voice.getName() + ".vocalization.hnmfeaturefile");
			vSynthesizer = new HNMSynthesisTechnology(timelineFile, unitFileName, hnmFeatureFile, intonationFile,
					f0ContourImposeSupport);
		} else {
			throw new MaryConfigurationException("the property 'voice." + voice.getName()
					+ ".vocalization.synthesisTechnology' should be one among 'hnm', 'mlsa' and 'fdpsola'");
		}

		this.vSelector = new VocalizationSelector(voice);
	}

	/**
	 * Handle a request for synthesis of vocalization
	 * 
	 * @param voice
	 *            the selected voice
	 * @param aft
	 *            AudioFileFormat of the output AudioInputStream
	 * @param domElement
	 *            target xml element ('vocalization' element)
	 * @return AudioInputStream of requested vocalization it returns null if the voice doesn't support synthesis of vocalizations
	 * @throws Exception
	 *             if domElement contains 'variant' attribute value is greater than available number of vocalizations
	 */
	public AudioInputStream synthesize(Voice voice, AudioFileFormat aft, Element domElement) throws Exception {

		if (!voice.hasVocalizationSupport())
			return null;

		if (domElement.hasAttribute("variant")) {
			return synthesizeVariant(aft, domElement);
		}

		if (f0ContourImposeSupport) {
			return synthesizeImposedIntonation(aft, domElement);
		}

		return synthesizeVocalization(aft, domElement);
	}

	/**
	 * Synthesize a "variant" vocalization
	 * 
	 * @param aft
	 *            AudioFileFormat of the output AudioInputStream
	 * @param domElement
	 *            target 'vocalization' xml element
	 * @return AudioInputStream of requested vocalization
	 * @throws SynthesisException
	 *             if it can't synthesize vocalization
	 * @throws IllegalArgumentException
	 *             if domElement contains 'variant' attribute value is greater than available number of vocalizations
	 */
	private AudioInputStream synthesizeVariant(AudioFileFormat aft, Element domElement) throws SynthesisException {

		int numberOfBackChannels = unitFileReader.getNumberOfUnits();
		int backchannelNumber = 0;

		if (domElement.hasAttribute("variant")) {
			backchannelNumber = Integer.parseInt(domElement.getAttribute("variant"));
		}

		if (backchannelNumber >= numberOfBackChannels) {
			throw new IllegalArgumentException("This voice has " + numberOfBackChannels
					+ " backchannels only. so it doesn't support unit number " + backchannelNumber);
		}

		return synthesizeSelectedVocalization(backchannelNumber, aft, domElement);
	}

	/**
	 * Synthesize a vocalization which fits better for given target
	 * 
	 * @param aft
	 *            AudioFileFormat of the output AudioInputStream
	 * @param domElement
	 *            target 'vocalization' xml element
	 * @return AudioInputStream output audio
	 * @throws SynthesisException
	 *             if it can't synthesize vocalization
	 */
	private AudioInputStream synthesizeVocalization(AudioFileFormat aft, Element domElement) throws SynthesisException {

		int numberOfBackChannels = unitFileReader.getNumberOfUnits();
		int backchannelNumber = vSelector.getBestMatchingCandidate(domElement);
		// here it is a bug, if getBestMatchingCandidate select a backchannelNumber greater than numberOfBackChannels
		assert backchannelNumber < numberOfBackChannels : "This voice has " + numberOfBackChannels
				+ " backchannels only. so it doesn't support unit number " + backchannelNumber;

		return synthesizeSelectedVocalization(backchannelNumber, aft, domElement);
	}

	/**
	 * Synthesize a vocalization which fits better for given target, in addition, impose intonation from closest best vocalization
	 * according to given feature definition for intonation selection
	 * 
	 * @param aft
	 *            AudioFileFormat of the output AudioInputStream
	 * @param domElement
	 *            target 'vocalization' xml element
	 * @return AudioInputStream output audio
	 * @throws SynthesisException
	 *             if it can't synthesize vocalization
	 */
	private AudioInputStream synthesizeImposedIntonation(AudioFileFormat aft, Element domElement) throws SynthesisException {

		SourceTargetPair imposeF0Data = vSelector.getBestCandidatePairtoImposeF0(domElement);
		int targetIndex = imposeF0Data.getTargetUnitIndex();
		int sourceIndex = imposeF0Data.getSourceUnitIndex();

		logger.debug("Synthesizing candidate " + sourceIndex + " with intonation contour " + targetIndex);

		if (targetIndex == sourceIndex) {
			return synthesizeSelectedVocalization(sourceIndex, aft, domElement);
		}

		return imposeF0ContourOnVocalization(sourceIndex, targetIndex, aft, domElement);
	}

	/**
	 * Impose a target f0 contour onto a (source) unit
	 * 
	 * @param sourceIndex
	 *            unit index of segmentalform unit
	 * @param targetIndex
	 *            unit index of target f0 contour
	 * @param aft
	 *            AudioFileFormat of the output AudioInputStream
	 * @param domElement
	 *            target 'vocalization' xml element
	 * @return AudioInputStream of requested vocalization
	 * @throws SynthesisException
	 *             if no data can be read at the given target time or if audio processing fails
	 */
	private AudioInputStream imposeF0ContourOnVocalization(int sourceIndex, int targetIndex, AudioFileFormat aft,
			Element domElement) throws SynthesisException {

		int numberOfBackChannels = unitFileReader.getNumberOfUnits();

		if (targetIndex >= numberOfBackChannels) {
			throw new IllegalArgumentException("This voice has " + numberOfBackChannels
					+ " backchannels only. so it doesn't support unit number " + targetIndex);
		}

		if (sourceIndex >= numberOfBackChannels) {
			throw new IllegalArgumentException("This voice has " + numberOfBackChannels
					+ " backchannels only. so it doesn't support unit number " + sourceIndex);
		}

		VocalizationUnit bUnit = unitFileReader.getUnit(sourceIndex);
		Unit[] units = bUnit.getUnits();
		String[] unitNames = bUnit.getUnitNames();
		long endTime = 0l;
		for (int i = 0; i < units.length; i++) {
			int unitDuration = units[i].duration * 1000 / unitFileReader.getSampleRate();
			endTime += unitDuration;
			Element element = MaryXML.createElement(domElement.getOwnerDocument(), MaryXML.PHONE);
			element.setAttribute("d", Integer.toString(unitDuration));
			element.setAttribute("end", Long.toString(endTime));
			element.setAttribute("p", unitNames[i]);
			domElement.appendChild(element);
		}

		return this.vSynthesizer.synthesizeUsingImposedF0(sourceIndex, targetIndex, aft);
	}

	/**
	 * Synthesize a selected vocalization
	 * 
	 * @param backchannelNumber
	 *            unit index number
	 * @param aft
	 *            AudioFileFormat of the output AudioInputStream
	 * @param domElement
	 *            target 'vocalization' xml element
	 * @return AudioInputStream output audio
	 * @throws SynthesisException
	 *             if it can't synthesize vocalization
	 * @throws IllegalArgumentException
	 *             if given backchannelNumber > no. of available vocalizations
	 */
	private AudioInputStream synthesizeSelectedVocalization(int backchannelNumber, AudioFileFormat aft, Element domElement)
			throws SynthesisException {

		int numberOfBackChannels = unitFileReader.getNumberOfUnits();
		if (backchannelNumber >= numberOfBackChannels) {
			throw new IllegalArgumentException("This voice has " + numberOfBackChannels
					+ " backchannels only. so it doesn't support unit number " + backchannelNumber);
		}

		VocalizationUnit bUnit = unitFileReader.getUnit(backchannelNumber);
		Unit[] units = bUnit.getUnits();
		String[] unitNames = bUnit.getUnitNames();
		long endTime = 0l;
		for (int i = 0; i < units.length; i++) {
			int unitDuration = units[i].duration * 1000 / unitFileReader.getSampleRate();
			endTime += unitDuration;
			Element element = MaryXML.createElement(domElement.getOwnerDocument(), MaryXML.PHONE);
			element.setAttribute("d", Integer.toString(unitDuration));
			element.setAttribute("end", Long.toString(endTime));
			element.setAttribute("p", unitNames[i]);
			domElement.appendChild(element);
		}

		return this.vSynthesizer.synthesize(backchannelNumber, aft);
	}

	/**
	 * List the possible vocalization names that are available for the given voice. These values can be used in the "name"
	 * attribute of the vocalization tag.
	 * 
	 * @return an array of Strings, each string containing one unique vocalization name.
	 */
	public String[] listAvailableVocalizations() {
		FeatureDefinition featureDefinition = vSelector.getFeatureDefinition();
		assert featureDefinition.hasFeature("name");
		int nameIndex = featureDefinition.getFeatureIndex("name");
		return featureDefinition.getPossibleValues(nameIndex);
	}
}
