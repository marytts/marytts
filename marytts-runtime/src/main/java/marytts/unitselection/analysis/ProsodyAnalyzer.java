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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import marytts.datatypes.MaryXML;
import marytts.modules.acoustic.ProsodyElementHandler;
import marytts.unitselection.select.HalfPhoneTarget;
import marytts.unitselection.select.SelectedUnit;
import marytts.util.MaryUtils;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Class to provide high-level, phone-based access to the predicted and realized prosodic parameters in a given unit-selection
 * result
 * 
 * @author steiner
 * 
 */
public class ProsodyAnalyzer {

	private List<SelectedUnit> units;

	private int sampleRate;

	private Logger logger;

	private List<Phone> phones;

	/**
	 * Main constructor
	 * <p>
	 * Note that the units are first parsed into phones (and the F0 target values assigned), <i>before</i> any distinction is made
	 * between those with and without a realized duration (e.g. {@link #getRealizedPhones()}).
	 * 
	 * @param units
	 *            whose predicted and realized prosody to analyze
	 * @param sampleRate
	 *            of the unit database, in Hz
	 * @throws Exception
	 *             if the units cannot be parsed into phones
	 */
	public ProsodyAnalyzer(List<SelectedUnit> units, int sampleRate) throws Exception {
		this.units = units;
		this.sampleRate = sampleRate;

		this.logger = MaryUtils.getLogger(this.getClass());

		// List of phone segments:
		this.phones = parseIntoPhones();
	}

	/**
	 * Parse a list of selected units into the corresponding phone segments
	 * 
	 * @return List of Phones
	 * @throws Exception
	 *             if the predicted prosody cannot be determined properly
	 */
	private List<Phone> parseIntoPhones() throws Exception {
		// initialize List of Phones (note that initial size is not final!):
		phones = new ArrayList<Phone>(units.size() / 2);
		// iterate over the units:
		int u = 0;
		while (u < units.size()) {
			// get unit...
			SelectedUnit unit = units.get(u);
			// ...and its target as a HalfPhoneTarget, so that we can...
			HalfPhoneTarget target = (HalfPhoneTarget) unit.getTarget();
			// ...query its position in the phone:
			if (target.isLeftHalf()) {
				// if this is the left half of a phone...
				if (u < units.size() - 1) {
					// ...and there is a next unit in the list...
					SelectedUnit nextUnit = units.get(u + 1);
					HalfPhoneTarget nextTarget = (HalfPhoneTarget) nextUnit.getTarget();
					if (nextTarget.isRightHalf()) {
						// ...and the next unit's target is the right half of the phone, add the phone:
						phones.add(new Phone(unit, nextUnit, sampleRate));
						u++;
					} else {
						// otherwise, add a degenerate phone with no right halfphone:
						phones.add(new Phone(unit, null, sampleRate));
					}
				} else {
					// otherwise, add a degenerate phone with no right halfphone:
					phones.add(new Phone(unit, null, sampleRate));
				}
			} else {
				// otherwise, add a degenerate phone with no left halfphone:
				phones.add(new Phone(null, unit, sampleRate));
			}
			u++;
		}

		// make sure we've seen all the units:
		assert u == units.size();

		// assign target F0 values to Phones:
		insertTargetF0Values();

		return phones;
	}

	/**
	 * Assign predicted F0 values to the phones by parsing the XML Document
	 * 
	 * @throws Exception
	 *             if the Document cannot be accessed
	 */
	private void insertTargetF0Values() throws Exception {
		NodeList phoneNodes;
		try {
			phoneNodes = getPhoneNodes();
		} catch (Exception e) {
			throw new Exception("Could not get the phone Nodes from the Document", e);
		}

		// count the number of Datagrams we need, which is the number of F0 target values the ProsodyElementHandler will return:
		int totalNumberOfFrames = getNumberOfFrames();

		// this method hinges on the F0 attribute parsing done in modules.acoustic
		ProsodyElementHandler elementHandler = new ProsodyElementHandler();
		double[] f0Targets = elementHandler.getF0Contour(phoneNodes, totalNumberOfFrames);

		int f0TargetStartIndex = 0;
		for (Phone phone : phones) {
			int numberOfLeftUnitFrames = phone.getNumberOfLeftUnitFrames();
			int f0TargetMidIndex = f0TargetStartIndex + numberOfLeftUnitFrames;
			double[] leftF0Targets = ArrayUtils.subarray(f0Targets, f0TargetStartIndex, f0TargetMidIndex);
			phone.setLeftTargetF0Values(leftF0Targets);

			int numberOfRightUnitFrames = phone.getNumberOfRightUnitFrames();
			int f0TargetEndIndex = f0TargetMidIndex + numberOfRightUnitFrames;
			double[] rightF0Targets = ArrayUtils.subarray(f0Targets, f0TargetMidIndex, f0TargetEndIndex);
			phone.setRightTargetF0Values(rightF0Targets);

			f0TargetStartIndex = f0TargetEndIndex;
		}
		return;
	}

	/**
	 * Get the List of Phones
	 * 
	 * @return the Phones
	 */
	public List<Phone> getPhones() {
		return phones;
	}

	/**
	 * Get the List of Phones that have a predicted duration greater than zero
	 * 
	 * @return the List of realized Phones
	 */
	public List<Phone> getRealizedPhones() {
		List<Phone> realizedPhones = new ArrayList<Phone>(phones.size());
		for (Phone phone : phones) {
			if (phone.getPredictedDuration() > 0) {
				realizedPhones.add(phone);
			}
		}
		return realizedPhones;
	}

	/**
	 * Get NodeList for Phones from Document
	 * 
	 * @return NodeList of Phones
	 * @throws Exception
	 *             if Document cannot be accessed
	 */
	private NodeList getPhoneNodes() throws Exception {
		Document document = getDocument();
		NodeList phoneNodes;
		try {
			phoneNodes = document.getElementsByTagName(MaryXML.PHONE);
		} catch (NullPointerException e) {
			throw new Exception("Could not access the Document!", e);
		}
		return phoneNodes;
	}

	/**
	 * For the first phone with a MaryXMLElement we encounter, return that Element's Document
	 * 
	 * @return the Document containing the {@link #phones} or null if no phone is able to provide a MaryXMLElement
	 */
	private Document getDocument() {
		for (Phone phone : phones) {
			Element phoneElement = phone.getMaryXMLElement();
			if (phoneElement != null) {
				return phoneElement.getOwnerDocument();
			}
		}
		return null;
	}

	/**
	 * Get the number of Datagrams in all Phones
	 * 
	 * @return the number of Datagrams in all Phones
	 */
	private int getNumberOfFrames() {
		int totalNumberOfFrames = 0;
		for (Phone phone : phones) {
			totalNumberOfFrames += phone.getNumberOfFrames();
		}
		return totalNumberOfFrames;
	}

	/**
	 * Get duration factors representing ratio of predicted and realized halfphone Unit durations. Units with zero predicted or
	 * realized duration receive a factor of 0.
	 * 
	 * @return List of duration factors
	 */
	public List<Double> getDurationFactors() {
		// list of duration factors, one per halfphone unit:
		List<Double> durationFactors = new ArrayList<Double>(units.size());

		// iterate over phone segments:
		for (Phone phone : phones) {
			double leftDurationFactor = phone.getLeftDurationFactor();
			if (leftDurationFactor > 0) {
				durationFactors.add(leftDurationFactor);
				logger.debug("duration factor for unit " + phone.getLeftUnit().getTarget().getName() + " -> "
						+ leftDurationFactor);
			}
			double rightDurationFactor = phone.getRightDurationFactor();
			if (rightDurationFactor > 0) {
				// ...add the duration factor to the list:
				durationFactors.add(rightDurationFactor);
				logger.debug("duration factor for unit " + phone.getRightUnit().getTarget().getName() + " -> "
						+ rightDurationFactor);
			}
		}

		return durationFactors;
	}

	/*
	 * Some ad-hoc methods for HnmUnitConcatenator:
	 */

	public double[] getDurationFactorsFramewise() {
		double[] f0Factors = null;
		for (Phone phone : phones) {
			double[] phoneF0Factors = phone.getFramewiseDurationFactors();
			f0Factors = ArrayUtils.addAll(f0Factors, phoneF0Factors);
		}
		return f0Factors;
	}

	public double[] getFrameMidTimes() {
		double[] frameDurations = null;
		for (Phone phone : phones) {
			double[] phoneFrameDurations = phone.getFrameDurations();
			frameDurations = ArrayUtils.addAll(frameDurations, phoneFrameDurations);
		}

		assert frameDurations != null;
		double[] frameMidTimes = new double[frameDurations.length];
		double frameStartTime = 0;
		for (int f = 0; f < frameDurations.length; f++) {
			frameMidTimes[f] = frameStartTime + frameDurations[f] / 2;
			frameStartTime += frameDurations[f];
		}

		return frameMidTimes;
	}

	public double[] getF0Factors() {
		double[] f0Factors = null;
		for (Phone phone : phones) {
			double[] phoneF0Factors = phone.getF0Factors();
			f0Factors = ArrayUtils.addAll(f0Factors, phoneF0Factors);
		}
		return f0Factors;
	}

	/**
	 * For debugging, generate Praat DurationTier, which can be used for PSOLA-based manipulation in Praat.
	 * <p>
	 * Notes:
	 * <ul>
	 * <li>Initial silence is skipped.</li>
	 * <li>Units with zero realized duration are ignored.</li>
	 * <li>To avoid gradual interpolation between points, place two points around each unit boundary, separated by
	 * <code>MIN_SKIP</code>; this workaround allows one constant factor per unit.</li>
	 * </ul>
	 * 
	 * @param fileName
	 *            of the DurationTier to be generated
	 * @throws IOException
	 *             IOException
	 */
	public void writePraatDurationTier(String fileName) throws IOException {

		// initialize times and values with a size corresponding to two elements (start and end) per unit:
		ArrayList<Double> times = new ArrayList<Double>(units.size() * 2);
		ArrayList<Double> values = new ArrayList<Double>(units.size() * 2);

		final double MIN_SKIP = 1e-15;

		// cumulative time pointer:
		double time = 0;

		// iterate over phones, skipping the initial silence:
		// TODO is this really robust?
		ListIterator<Phone> phoneIterator = phones.listIterator(1);
		while (phoneIterator.hasNext()) {
			Phone phone = phoneIterator.next();

			// process left halfphone unit:
			if (phone.getLeftUnitDuration() > 0) {
				// add point at unit start:
				times.add(time);
				values.add(phone.getLeftDurationFactor());

				// increment time pointer by unit duration:
				time += phone.getLeftUnitDuration();

				// add point at unit end:
				times.add(time - MIN_SKIP);
				values.add(phone.getLeftDurationFactor());
			}
			// process right halfphone unit:
			if (phone.getRightUnitDuration() > 0) {
				// add point at unit start:
				times.add(time);
				values.add(phone.getRightDurationFactor());

				// increment time pointer by unit duration:
				time += phone.getRightUnitDuration();

				// add point at unit end:
				times.add(time - MIN_SKIP);
				values.add(phone.getRightDurationFactor());
			}
		}

		// open file for writing:
		File durationTierFile = new File(fileName);
		PrintWriter out = new PrintWriter(durationTierFile);

		// print header:
		out.println("\"ooTextFile\"");
		out.println("\"DurationTier\"");
		out.println(String.format("0 %f %d", time, times.size()));

		// print points (times and values):
		for (int i = 0; i < times.size(); i++) {
			// Note: time precision should be greater than MIN_SKIP:
			out.println(String.format("%.16f %f", times.get(i), values.get(i)));
		}

		// flush and close:
		out.close();
	}

	/**
	 * For debugging, generate Praat PitchTier, which can be used for PSOLA-based manipulation in Praat.
	 * 
	 * @param fileName
	 *            of the PitchTier to be generated
	 * @throws IOException
	 *             IOException
	 */
	public void writePraatPitchTier(String fileName) throws IOException {

		// initialize times and values:
		ArrayList<Double> times = new ArrayList<Double>();
		ArrayList<Double> values = new ArrayList<Double>();

		// cumulative time pointer:
		double time = 0;

		// iterate over phones, skipping the initial silence:
		ListIterator<Phone> phoneIterator = phones.listIterator(1);
		while (phoneIterator.hasNext()) {
			Phone phone = phoneIterator.next();
			double[] frameTimes = phone.getRealizedFrameDurations();
			double[] frameF0s = phone.getUnitFrameF0s();
			for (int f = 0; f < frameF0s.length; f++) {
				time += frameTimes[f];
				times.add(time);
				values.add(frameF0s[f]);
			}
		}

		// open file for writing:
		File durationTierFile = new File(fileName);
		PrintWriter out = new PrintWriter(durationTierFile);

		// print header:
		out.println("\"ooTextFile\"");
		out.println("\"PitchTier\"");
		out.println(String.format("0 %f %d", time, times.size()));

		// print points (times and values):
		for (int i = 0; i < times.size(); i++) {
			out.println(String.format("%.16f %f", times.get(i), values.get(i)));
		}

		// flush and close:
		out.close();
	}
}
