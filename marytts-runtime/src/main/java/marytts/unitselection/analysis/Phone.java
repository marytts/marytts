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

import java.util.Arrays;

import marytts.modules.phonemiser.Allophone;
import marytts.unitselection.concat.BaseUnitConcatenator.UnitData;
import marytts.unitselection.select.HalfPhoneTarget;
import marytts.unitselection.select.SelectedUnit;
import marytts.util.data.Datagram;
import marytts.util.math.MathUtils;

import org.apache.commons.lang.ArrayUtils;
import org.w3c.dom.Element;

/**
 * Convenience class containing the selected units and targets of a phone segment, and a host of getters to access their prosodic
 * attributes
 * 
 * @author steiner
 * 
 */
public class Phone {
	private HalfPhoneTarget leftTarget;

	private HalfPhoneTarget rightTarget;

	private SelectedUnit leftUnit;

	private SelectedUnit rightUnit;

	private double sampleRate;

	private double[] leftF0Targets;

	private double[] rightF0Targets;

	/**
	 * Main constructor
	 * 
	 * @param leftUnit
	 *            which can be null
	 * @param rightUnit
	 *            which can be null
	 * @param sampleRate
	 *            of the TimelineReader containing the SelectedUnits, needed to provide duration information
	 * @throws IllegalArgumentException
	 *             if both the left and right units are null
	 */
	public Phone(SelectedUnit leftUnit, SelectedUnit rightUnit, int sampleRate) throws IllegalArgumentException {
		this.leftUnit = leftUnit;
		this.rightUnit = rightUnit;
		this.sampleRate = (double) sampleRate;
		// targets are extracted from the units for easier access:
		try {
			this.leftTarget = (HalfPhoneTarget) leftUnit.getTarget();
		} catch (NullPointerException e) {
			// leave at null
		}
		try {
			this.rightTarget = (HalfPhoneTarget) rightUnit.getTarget();
		} catch (NullPointerException e) {
			if (leftTarget == null) {
				throw new IllegalArgumentException("A phone's left and right halves cannot both be null!");
			} else {
				// leave at null
			}
		}
	}

	/**
	 * Get the left selected halfphone unit of this phone
	 * 
	 * @return the left unit, or null if there is no left unit
	 */
	public SelectedUnit getLeftUnit() {
		return leftUnit;
	}

	/**
	 * Get the right selected halfphone unit of this phone
	 * 
	 * @return the right unit, or null if there is no right unit
	 */
	public SelectedUnit getRightUnit() {
		return rightUnit;
	}

	/**
	 * Get the left halfphone target of this phone
	 * 
	 * @return the left target, or null if there is no left target
	 */
	public HalfPhoneTarget getLeftTarget() {
		return leftTarget;
	}

	/**
	 * Get the right halfphone target of this phone
	 * 
	 * @return the right target, or null if there is no right target
	 */
	public HalfPhoneTarget getRightTarget() {
		return rightTarget;
	}

	/**
	 * Get the datagrams from a SelectedUnit
	 * 
	 * @param unit
	 *            the SelectedUnit
	 * @return the datagrams in an array, or null if unit is null
	 */
	private Datagram[] getUnitFrames(SelectedUnit unit) {
		UnitData unitData = getUnitData(unit);
		Datagram[] frames = null;
		try {
			frames = unitData.getFrames();
		} catch (NullPointerException e) {
			// leave at null
		}
		return frames;
	}

	/**
	 * Get this phone's left unit's Datagrams
	 * 
	 * @return the left unit's Datagrams in an array, or null if there is no left unit
	 */
	public Datagram[] getLeftUnitFrames() {
		return getUnitFrames(leftUnit);
	}

	/**
	 * Get this phone's right unit's Datagrams
	 * 
	 * @return the right unit's Datagrams in an array, or null if there is no right unit
	 */
	public Datagram[] getRightUnitFrames() {
		return getUnitFrames(rightUnit);
	}

	/**
	 * Get all Datagrams in this phone's units
	 * 
	 * @return the left and right unit's Datagrams in an array
	 */
	public Datagram[] getUnitDataFrames() {
		Datagram[] leftUnitFrames = getLeftUnitFrames();
		Datagram[] rightUnitFrames = getRightUnitFrames();
		Datagram[] frames = (Datagram[]) ArrayUtils.addAll(leftUnitFrames, rightUnitFrames);
		return frames;
	}

	/**
	 * Get the number of Datagrams in a SelectedUnit's UnitData
	 * 
	 * @param unit
	 *            whose Datagrams to count
	 * @return the number of Datagrams in the unit, or 0 if unit is null
	 */
	private int getNumberOfUnitFrames(SelectedUnit unit) {
		int numberOfFrames = 0;
		try {
			Datagram[] frames = getUnitData(unit).getFrames();
			numberOfFrames = frames.length;
		} catch (NullPointerException e) {
			// leave at 0
		}
		return numberOfFrames;
	}

	/**
	 * Get the number of Datagrams in this phone's left unit
	 * 
	 * @return the number of Datagrams in the left unit, or 0 if there is no left unit
	 */
	public int getNumberOfLeftUnitFrames() {
		return getNumberOfUnitFrames(leftUnit);
	}

	/**
	 * Get the number of Datagrams in this phone's right unit
	 * 
	 * @return the number of Datagrams in the right unit, or 0 if there is no right unit
	 */
	public int getNumberOfRightUnitFrames() {
		return getNumberOfUnitFrames(rightUnit);
	}

	/**
	 * Get the number of Datagrams in this phone's left and right units
	 * 
	 * @return the number of Datagrams in this phone
	 */
	public int getNumberOfFrames() {
		return getNumberOfLeftUnitFrames() + getNumberOfRightUnitFrames();
	}

	/**
	 * Get the durations (in seconds) of each Datagram in this phone's units
	 * 
	 * @return the left and right unit's Datagram durations (in seconds) in an array
	 */
	public double[] getFrameDurations() {
		Datagram[] frames = getUnitDataFrames();
		double[] durations = new double[frames.length];
		for (int f = 0; f < frames.length; f++) {
			durations[f] = frames[f].getDuration() / sampleRate;
		}
		return durations;
	}

	/**
	 * Get the target duration (in seconds) of a HalfPhoneTarget
	 * 
	 * @param target
	 *            the target whose duration to get
	 * @return the target duration in seconds, or 0 if target is null
	 */
	private double getTargetDuration(HalfPhoneTarget target) {
		double duration = 0;
		try {
			duration = target.getTargetDurationInSeconds();
		} catch (NullPointerException e) {
			// leave at 0
		}
		return duration;
	}

	/**
	 * Get this phone's left target's duration (in seconds)
	 * 
	 * @return the left target's duration in seconds, or 0 if there is no left target
	 */
	public double getLeftTargetDuration() {
		return getTargetDuration(leftTarget);
	}

	/**
	 * Get this phone's right target's duration (in seconds)
	 * 
	 * @return the right target's duration in seconds, or 0 if there is no right target
	 */
	public double getRightTargetDuration() {
		return getTargetDuration(rightTarget);
	}

	/**
	 * Get the predicted duration of this phone (which is the sum of the left and right target's duration)
	 * 
	 * @return the predicted phone duration in seconds
	 */
	public double getPredictedDuration() {
		return getLeftTargetDuration() + getRightTargetDuration();
	}

	/**
	 * Convenience getter for a SelectedUnit's UnitData
	 * 
	 * @param unit
	 *            the unit whose UnitData to get
	 * @return the UnitData of the unit, or null, if unit is null
	 */
	private UnitData getUnitData(SelectedUnit unit) {
		UnitData unitData = null;
		try {
			unitData = (UnitData) unit.getConcatenationData();
		} catch (NullPointerException e) {
			// leave at null
		}
		return unitData;
	}

	/**
	 * Get this phone's left unit's UnitData
	 * 
	 * @return the left unit's UnitData, or null if there is no left unit
	 */
	public UnitData getLeftUnitData() {
		return getUnitData(leftUnit);
	}

	/**
	 * Get this phone's right unit's UnitData
	 * 
	 * @return the right unit's UnitData, or null if there is no right unit
	 */
	public UnitData getRightUnitData() {
		return getUnitData(rightUnit);
	}

	/**
	 * Get the actual duration (in seconds) of a SelectedUnit, from its UnitData
	 * 
	 * @param unit
	 *            whose duration to get
	 * @return the unit's duration in seconds, or 0 if unit is null
	 */
	private double getUnitDuration(SelectedUnit unit) {
		int durationInSamples = 0;
		try {
			durationInSamples = getUnitData(unit).getUnitDuration();
		} catch (NullPointerException e) {
			// leave at 0
		}
		double duration = durationInSamples / sampleRate;
		return duration;
	}

	/**
	 * Get this phone's left unit's duration (in seconds)
	 * 
	 * @return the left unit's duration in seconds, or 0 if there is no left unit
	 */
	public double getLeftUnitDuration() {
		return getUnitDuration(leftUnit);
	}

	/**
	 * Get this phone's right unit's duration (in seconds)
	 * 
	 * @return the right unit's duration in seconds, or 0 if there is no right unit
	 */
	public double getRightUnitDuration() {
		return getUnitDuration(rightUnit);
	}

	/**
	 * Get the realized duration (in seconds) of this phone (which is the sum of the durations of the left and right units)
	 * 
	 * @return the phone's realized duration in seconds
	 */
	public double getRealizedDuration() {
		return getLeftUnitDuration() + getRightUnitDuration();
	}

	/**
	 * Get the factor needed to convert the realized duration of a unit to the target duration
	 * 
	 * @param unit
	 *            whose realized duration to convert
	 * @param target
	 *            whose duration to match
	 * @return the multiplication factor to convert from the realized duration to the target duration, or 0 if the unit duration
	 *         is 0
	 */
	private double getDurationFactor(SelectedUnit unit, HalfPhoneTarget target) {
		double unitDuration = getUnitDuration(unit);
		if (unitDuration <= 0) {
			// throw new ArithmeticException("Realized duration must be greater than 0!");
			return 0;
		}
		double targetDuration = getTargetDuration(target);
		double durationFactor = targetDuration / unitDuration;
		return durationFactor;
	}

	/**
	 * Get the factor to convert this phone's left unit's duration into this phone's left target duration
	 * 
	 * @return the left duration factor
	 */
	public double getLeftDurationFactor() {
		return getDurationFactor(leftUnit, leftTarget);
	}

	/**
	 * Get the factor to convert this phone's right unit's duration into this phone's right target duration
	 * 
	 * @return the right duration factor
	 */
	public double getRightDurationFactor() {
		return getDurationFactor(rightUnit, rightTarget);
	}

	/**
	 * Get the duration factors for this phone, one per datagram. Each factor corresponding to a datagram in the left unit is that
	 * required to convert the left unit's duration to the left target duration, and likewise for the right unit's datagrams.
	 * 
	 * @return the left and right duration factors for this phone, in an array whose size matched the Datagrams in this phone's
	 *         units
	 */
	public double[] getFramewiseDurationFactors() {
		double[] durationFactors = new double[getNumberOfFrames()];
		int numberOfLeftUnitFrames = getNumberOfLeftUnitFrames();
		double leftDurationFactor = getLeftDurationFactor();
		Arrays.fill(durationFactors, 0, numberOfLeftUnitFrames, leftDurationFactor);
		double rightDurationFactor = getRightDurationFactor();
		Arrays.fill(durationFactors, numberOfLeftUnitFrames, getNumberOfFrames(), rightDurationFactor);
		return durationFactors;
	}

	/**
	 * Set the target F0 values of this phone's left half, with one value per Datagram in the phone's left unit
	 * 
	 * @param f0TargetValues
	 *            array of target F0 values to assign to the left halfphone
	 * @throws IllegalArgumentException
	 *             if the length of f0TargetValues does not match the number of Datagrams in the phone's left unit
	 */
	public void setLeftTargetF0Values(double[] f0TargetValues) throws IllegalArgumentException {
		int numberOfLeftUnitFrames = getNumberOfLeftUnitFrames();
		if (f0TargetValues.length != numberOfLeftUnitFrames) {
			throw new IllegalArgumentException("Wrong number of F0 targets (" + f0TargetValues.length
					+ ") for number of frames (" + numberOfLeftUnitFrames + " in halfphone: '" + leftUnit.toString() + "'");
		}
		this.leftF0Targets = f0TargetValues;
	}

	/**
	 * Set the target F0 values of this phone's right half, with one value per Datagram in the phone's right unit
	 * 
	 * @param f0TargetValues
	 *            array of target F0 values to assign to the right halfphone
	 * @throws IllegalArgumentException
	 *             if the length of f0TargetValues does not match the number of Datagrams in the phone's right unit
	 */
	public void setRightTargetF0Values(double[] f0TargetValues) {
		if (f0TargetValues.length != getNumberOfRightUnitFrames()) {
			throw new IllegalArgumentException("Wrong number of F0 targets (" + f0TargetValues.length
					+ ") for number of frames (" + getNumberOfRightUnitFrames() + " in halfphone: '" + rightUnit.toString() + "'");
		}
		this.rightF0Targets = f0TargetValues;
	}

	/**
	 * Get the target F0 values for this phone's left half, with one value per Datagram in the phone's left unit
	 * 
	 * @return the target F0 values for the left halfphone in an array
	 * @throws NullPointerException
	 *             if the target F0 values for the left halfphone are null
	 */
	public double[] getLeftTargetF0Values() throws NullPointerException {
		if (leftF0Targets == null) {
			throw new NullPointerException("The left target F0 values have not been assigned!");
		}
		return leftF0Targets;
	}

	/**
	 * Get the target F0 values for this phone's right half, with one value per Datagram in the phone's right unit
	 * 
	 * @return the target F0 values for the right halfphone in an array
	 * @throws NullPointerException
	 *             if the target F0 values for the right halfphone are null
	 */
	public double[] getRightTargetF0Values() throws NullPointerException {
		if (rightF0Targets == null) {
			throw new NullPointerException("The right target F0 values have not been assigned!");
		}
		return rightF0Targets;
	}

	/**
	 * Get the target F0 values for this phone, with one value per Datagram in the phone's left and right units
	 * 
	 * @return the target F0 values with one value per Datagram in an array
	 */
	public double[] getTargetF0Values() {
		double[] f0Targets = ArrayUtils.addAll(leftF0Targets, rightF0Targets);
		return f0Targets;
	}

	/**
	 * Get the mean target F0 for this phone
	 * 
	 * @return the mean predicted F0 value
	 */
	public double getPredictedF0() {
		double meanF0 = MathUtils.mean(getTargetF0Values());
		return meanF0;
	}

	/**
	 * Get the durations (in seconds) of each Datagram in a SelectedUnit's UnitData
	 * 
	 * @param unit
	 *            whose Datagrams' durations to get
	 * @return the durations (in seconds) of each Datagram in the unit in an array, or null if unit is null
	 */
	private double[] getUnitFrameDurations(SelectedUnit unit) {
		Datagram[] frames = null;
		try {
			frames = getUnitData(unit).getFrames();
		} catch (NullPointerException e) {
			return null;
		}
		assert frames != null;

		double[] frameDurations = new double[frames.length];
		for (int f = 0; f < frames.length; f++) {
			long frameDuration = frames[f].getDuration();
			frameDurations[f] = frameDuration / sampleRate; // converting to seconds
		}
		return frameDurations;
	}

	/**
	 * Get the durations (in seconds) of each Datagram in this phone's left unit
	 * 
	 * @return the durations of each Datagram in the left unit in an array, or null if there is no left unit
	 */
	public double[] getLeftUnitFrameDurations() {
		return getUnitFrameDurations(leftUnit);
	}

	/**
	 * Get the durations (in seconds) of each Datagram in this phone's right unit
	 * 
	 * @return the durations of each Datagram in the right unit in an array, or null if there is no right unit
	 */
	public double[] getRightUnitFrameDurations() {
		return getUnitFrameDurations(rightUnit);
	}

	/**
	 * Get the durations (in seconds) of each Datagram in this phone's left and right units
	 * 
	 * @return the durations of all Datagrams in this phone in an array
	 */
	public double[] getRealizedFrameDurations() {
		return ArrayUtils.addAll(getLeftUnitFrameDurations(), getRightUnitFrameDurations());
	}

	/**
	 * Get the F0 values from a SelectedUnit's Datagrams.
	 * <p>
	 * Since these are not stored explicitly, we are forced to rely on the inverse of the Datagram durations, which means that
	 * during unvoiced regions, we recover a value of 100 Hz...
	 * 
	 * @param unit
	 *            from whose Datagrams to recover the F0 values
	 * @return the F0 value of each Datagram in the unit in an array, or null if the unit is null or any of the Datagrams have
	 *         zero duration
	 */
	private double[] getUnitF0Values(SelectedUnit unit) {
		double[] f0Values = null;
		try {
			double[] durations = getUnitFrameDurations(unit);
			if (!ArrayUtils.contains(durations, 0)) {
				f0Values = MathUtils.invert(durations);
			} else {
				// leave at null
			}
		} catch (IllegalArgumentException e) {
			// leave at null
		}
		return f0Values;
	}

	/**
	 * Recover the F0 values from this phone's left unit's Datagrams
	 * 
	 * @return the left unit's F0 values in an array, with one value per Datagram, or null if there is no left unit or any of the
	 *         left unit's Datagrams have zero duration
	 */
	public double[] getLeftUnitFrameF0s() {
		return getUnitF0Values(leftUnit);
	}

	/**
	 * Recover the F0 values from this phone's right unit's Datagrams
	 * 
	 * @return the right unit's F0 values in an array, with one value per Datagram, or null if there is no right unit or any of
	 *         the right unit's Datagrams have zero duration
	 */
	public double[] getRightUnitFrameF0s() {
		return getUnitF0Values(rightUnit);
	}

	/**
	 * Recover the F0 values from each Datagram in this phone's left and right units
	 * 
	 * @return the F0 values for each Datagram in this phone, or null if either the left or the right unit contain a Datagram with
	 *         zero duration
	 */
	public double[] getUnitFrameF0s() {
		double[] leftUnitFrameF0s = getLeftUnitFrameF0s();
		double[] rightUnitFrameF0s = getRightUnitFrameF0s();
		double[] unitFrameF0s = ArrayUtils.addAll(leftUnitFrameF0s, rightUnitFrameF0s);
		return unitFrameF0s;
	}

	/**
	 * Get the realized F0 by recovering the F0 from all Datagrams in this phone and computing the mean
	 * 
	 * @return the mean F0 of all Datagrams in this phone's left and right units
	 */
	public double getRealizedF0() {
		double meanF0 = MathUtils.mean(getUnitFrameF0s());
		return meanF0;
	}

	/**
	 * Get the factors required to convert the F0 values recovered from the Datagrams in a SelectedUnit to the target F0 values.
	 * 
	 * @param unit
	 *            from which to recover the realized F0 values
	 * @param target
	 *            for which to get the target F0 values
	 * @return each Datagram's F0 factor in an array, or null if realized and target F0 values differ in length
	 * @throws ArithmeticException
	 *             if any of the F0 values recovered from the unit's Datagrams is zero
	 */
	private double[] getUnitF0Factors(SelectedUnit unit, HalfPhoneTarget target) throws ArithmeticException {
		double[] unitF0Values = getUnitF0Values(unit);
		if (ArrayUtils.contains(unitF0Values, 0)) {
			throw new ArithmeticException("Unit frames must not have F0 of 0!");
		}

		double[] targetF0Values;
		if (target == null || target.isLeftHalf()) {
			targetF0Values = getLeftTargetF0Values();
		} else {
			targetF0Values = getRightTargetF0Values();
		}

		double[] f0Factors = null;
		try {
			f0Factors = MathUtils.divide(targetF0Values, unitF0Values);
		} catch (IllegalArgumentException e) {
			// leave at null
		}
		return f0Factors;
	}

	/**
	 * Get the factors to convert each of the F0 values in this phone's left half to the corresponding target value
	 * 
	 * @return the F0 factors for this phone's left half in an array with one value per Datagram, or null if the number of
	 *         Datagrams does not match the number of left target F0 values
	 */
	public double[] getLeftF0Factors() {
		return getUnitF0Factors(leftUnit, leftTarget);
	}

	/**
	 * Get the factors to convert each of the F0 values in this phone's right half to the corresponding target value
	 * 
	 * @return the F0 factors for this phone's right half in an array with one value per Datagram, or null if the number of
	 *         Datagrams does not match the number of right target F0 values
	 */
	public double[] getRightF0Factors() {
		return getUnitF0Factors(rightUnit, rightTarget);
	}

	/**
	 * Get the F0 factor for each Datagram in this phone's left and right units
	 * 
	 * @return the F0 factors, in an array with one value per Datagram
	 */
	public double[] getF0Factors() {
		return ArrayUtils.addAll(getLeftF0Factors(), getRightF0Factors());
	}

	/**
	 * Get the Allophone represented by this
	 * 
	 * @return the Allophone
	 */
	private Allophone getAllophone() {
		if (leftTarget != null) {
			return leftTarget.getAllophone();
		} else if (rightTarget != null) {
			return rightTarget.getAllophone();
		}
		return null;
	}

	/**
	 * Determine whether this is a transient phone (i.e. a plosive)
	 * 
	 * @return true if this is a plosive, false otherwise
	 */
	public boolean isTransient() {
		Allophone allophone = getAllophone();
		if (allophone.isPlosive() || allophone.isAffricate()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Determine whether this is a voiced phone
	 * 
	 * @return true if this is voiced, false otherwise
	 */
	public boolean isVoiced() {
		Allophone allophone = getAllophone();
		if (allophone.isVoiced()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * get the MaryXML Element corresponding to this Phone's Target
	 * <p>
	 * If both <b>leftTarget</b> and <b>rightTarget</b> are not <b>null</b>, their respective MaryXML Elements should be
	 * <i>equal</i>.
	 * 
	 * @return the MaryXML Element, or null if both halfphone targets are null
	 */
	public Element getMaryXMLElement() {
		if (leftTarget != null) {
			return leftTarget.getMaryxmlElement();
		} else if (rightTarget != null) {
			return rightTarget.getMaryxmlElement();
		}
		return null;
	}

	/**
	 * for debugging, provide the names of the left and right targets as the string representation of this class
	 */
	public String toString() {
		String string = "";
		if (leftTarget != null) {
			string += " " + leftTarget.getName();
		}
		if (rightTarget != null) {
			string += " " + rightTarget.getName();
		}
		return string;
	}
}
