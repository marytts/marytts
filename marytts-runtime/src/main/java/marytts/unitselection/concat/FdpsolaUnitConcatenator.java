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
package marytts.unitselection.concat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.AudioInputStream;

import marytts.modules.phonemiser.Allophone;
import marytts.server.MaryProperties;
import marytts.signalproc.process.FDPSOLAProcessor;
import marytts.unitselection.analysis.Phone;
import marytts.unitselection.select.SelectedUnit;
import marytts.unitselection.select.Target;
import marytts.util.data.Datagram;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;

/**
 * A unit concatenator that supports FD-PSOLA based prosody modifications during speech synthesis
 * 
 * @author Oytun T&uuml;rk, modified by steiner
 *
 */
public class FdpsolaUnitConcatenator extends OverlapUnitConcatenator {

	// modification value ranges with hard-coded defaults:
	private double minTimeScaleFactor = 0.5;
	private double maxTimeScaleFactor = 2.0;
	private double minPitchScaleFactor = 0.5;
	private double maxPitchScaleFactor = 2.0;

	/**
     * 
     */
	public FdpsolaUnitConcatenator() {
		super();
	}

	/**
	 * Alternative constructor that allows overriding the modification value ranges
	 * 
	 * @param minTimeScaleFactor
	 *            minimum duration scale factor
	 * @param maxTimeScaleFactor
	 *            maximum duration scale factor
	 * @param minPitchScaleFactor
	 *            minimum F0 scale factor
	 * @param maxPitchScaleFactor
	 *            maximum F0 scale factor
	 */
	public FdpsolaUnitConcatenator(double minTimeScaleFactor, double maxTimeScaleFactor, double minPitchScaleFactor,
			double maxPitchScaleFactor) {
		super();
		this.minTimeScaleFactor = minTimeScaleFactor;
		this.maxTimeScaleFactor = maxTimeScaleFactor;
		this.minPitchScaleFactor = minPitchScaleFactor;
		this.maxPitchScaleFactor = maxPitchScaleFactor;
	}

	/**
	 * Get the Datagrams from a List of SelectedUnits as an array of arrays; the number of elements in the array is equal to the
	 * number of Units, and each element contains that Unit's Datagrams as an array.
	 * 
	 * @param units
	 *            units
	 * @return array of Datagram arrays
	 */
	private Datagram[][] getDatagrams(List<SelectedUnit> units) {
		Datagram[][] datagrams = new Datagram[units.size()][];
		for (int i = 0; i < units.size(); i++) {
			UnitData unitData = (UnitData) units.get(i).getConcatenationData();
			datagrams[i] = unitData.getFrames();
		}
		return datagrams;
	}

	/**
	 * Convenience method to return the rightmost Datagram from each element in a List of SelectedUnits
	 * 
	 * @param units
	 *            units
	 * @return rightmost Datagrams as an array
	 */
	private Datagram[] getRightContexts(List<SelectedUnit> units) {
		Datagram[] rightContexts = new Datagram[units.size()];
		for (int i = 0; i < rightContexts.length; i++) {
			SelectedUnit unit = units.get(i);
			UnitData unitData = (UnitData) unit.getConcatenationData();
			rightContexts[i] = unitData.getRightContextFrame();
		}
		return rightContexts;
	}

	/**
	 * Get voicing for every Datagram in a List of SelectedUnits, as an array of arrays of booleans. This queries the phonological
	 * voicedness value for the Target as defined in the AllophoneSet
	 * 
	 * @param units
	 *            units
	 * @return array of boolean voicing arrays
	 */
	private boolean[][] getVoicings(List<SelectedUnit> units) {
		Datagram[][] datagrams = getDatagrams(units);

		boolean[][] voicings = new boolean[datagrams.length][];

		for (int i = 0; i < datagrams.length; i++) {
			Allophone allophone = units.get(i).getTarget().getAllophone();

			voicings[i] = new boolean[datagrams[i].length];

			if (allophone != null && allophone.isVoiced()) {
				Arrays.fill(voicings[i], true);
			} else {
				Arrays.fill(voicings[i], false);
			}
		}
		return voicings;
	}

	// We can try different things in this function
	// 1) Pitch of the selected units can be smoothed without using the target pitch values at all.
	// This will involve creating the target f0 values for each frame by ensuing small adjustments and yet reduce pitch
	// discontinuity
	// 2) Pitch of the selected units can be modified to match the specified target where those target values are smoothed
	// 3) A mixture of (1) and (2) can be devised, i.e. to minimize the amount of pitch modification one of the two methods can be
	// selected for a given unit
	// 4) Pitch segments of selected units can be shifted
	// 5) Pitch segments of target units can be shifted
	// 6) Pitch slopes can be modified for better matching in concatenation boundaries
	private double[][] getPitchScales(List<SelectedUnit> units) {
		Datagram[][] datagrams = getDatagrams(units);
		int len = datagrams.length;
		int i, j;
		double averageUnitF0InHz;
		double averageTargetF0InHz;
		int totalTargetUnits;
		double[][] pscales = new double[len][];
		SelectedUnit prevUnit = null;
		SelectedUnit unit = null;
		SelectedUnit nextUnit = null;

		Target prevTarget = null;
		Target target = null;
		Target nextTarget = null;

		// Estimation of pitch scale modification amounts
		for (i = 0; i < len; i++) {
			if (i > 0)
				prevUnit = (SelectedUnit) units.get(i - 1);
			else
				prevUnit = null;

			unit = (SelectedUnit) units.get(i);

			if (i < len - 1)
				nextUnit = (SelectedUnit) units.get(i + 1);
			else
				nextUnit = null;

			// get Targets for these three Units:
			if (prevUnit != null) {
				prevTarget = prevUnit.getTarget();
			}
			target = unit.getTarget();
			if (nextUnit != null) {
				nextTarget = nextUnit.getTarget();
			}

			Allophone allophone = unit.getTarget().getAllophone();

			int totalDatagrams = 0;
			averageUnitF0InHz = 0.0;
			averageTargetF0InHz = 0.0;
			totalTargetUnits = 0;

			// so we are getting the mean F0 for each unit over a 3-unit window??
			// don't process previous Target if it's null or silence:
			if (i > 0 && prevTarget != null && !prevTarget.isSilence()) {
				for (j = 0; j < datagrams[i - 1].length; j++) {
					// why not use voicings?
					if (allophone != null && (allophone.isVowel() || allophone.isVoiced())) {
						averageUnitF0InHz += ((double) timeline.getSampleRate()) / ((double) datagrams[i - 1][j].getDuration());
						totalDatagrams++;
					}
				}

				averageTargetF0InHz += prevTarget.getTargetF0InHz();
				totalTargetUnits++;
			}

			// don't process Target if it's null or silence:
			if (target != null && !target.isSilence()) {
				for (j = 0; j < datagrams[i].length; j++) {
					if (allophone != null && (allophone.isVowel() || allophone.isVoiced())) {
						averageUnitF0InHz += ((double) timeline.getSampleRate()) / ((double) datagrams[i][j].getDuration());
						totalDatagrams++;
					}

					averageTargetF0InHz += target.getTargetF0InHz();
					totalTargetUnits++;
				}
			}

			// don't process next Target if it's null or silence:
			if (i < len - 1 && prevTarget != null && !prevTarget.isSilence()) {
				for (j = 0; j < datagrams[i + 1].length; j++) {
					if (allophone != null && (allophone.isVowel() || allophone.isVoiced())) {
						averageUnitF0InHz += ((double) timeline.getSampleRate()) / ((double) datagrams[i + 1][j].getDuration());
						totalDatagrams++;
					}
				}

				averageTargetF0InHz += nextTarget.getTargetF0InHz();
				totalTargetUnits++;
			}

			averageTargetF0InHz /= totalTargetUnits;
			averageUnitF0InHz /= totalDatagrams;
			// so what was all that for?? these average frequencies are never used...

			pscales[i] = new double[datagrams[i].length];

			for (j = 0; j < datagrams[i].length; j++) {
				if (allophone != null && allophone.isVoiced()) {
					/*
					 * pscales[i][j] = averageTargetF0InHz/averageUnitF0InHz; if (pscales[i][j]>1.2) pscales[i][j]=1.2; if
					 * (pscales[i][j]<0.8) pscales[i][j]=0.8;
					 */
					pscales[i][j] = 1.0;
				} else {
					pscales[i][j] = 1.0;
				}
			}
		}
		return pscales;
	}

	// We can try different things in this function
	// 1) Duration modification factors can be estimated using neighbouring selected and target unit durations
	// 2) Duration modification factors can be limited or even set to 1.0 for different phone classes
	// 3) Duration modification factors can be limited depending on the previous/next phone class
	private double[][] getDurationScales(List<SelectedUnit> units) {
		Datagram[][] datagrams = getDatagrams(units);
		int len = datagrams.length;

		int i, j;
		double[][] tscales = new double[len][];
		int unitDuration;

		double[] unitDurationsInSeconds = new double[datagrams.length];

		SelectedUnit prevUnit = null;
		SelectedUnit unit = null;
		SelectedUnit nextUnit = null;

		for (i = 0; i < len; i++) {
			unitDuration = 0;
			for (j = 0; j < datagrams[i].length; j++) {
				if (j == datagrams[i].length - 1) {
					// if (rightContexts!=null && rightContexts[i]!=null)
					// unitDuration += datagrams[i][j].getDuration();//+rightContexts[i].getDuration();
					// else
					unitDuration += datagrams[i][j].getDuration();
				} else
					unitDuration += datagrams[i][j].getDuration();
			}
			unitDurationsInSeconds[i] = ((double) unitDuration) / timeline.getSampleRate();
		}

		double targetDur, unitDur;
		for (i = 0; i < len; i++) {
			targetDur = 0.0;
			unitDur = 0.0;
			// commented out dead code:
			// if (false && i>0)
			// {
			// prevUnit = (SelectedUnit) units.get(i-1);
			// targetDur += prevUnit.getTarget().getTargetDurationInSeconds();
			// unitDur += unitDurationsInSeconds[i-1];
			// }

			unit = (SelectedUnit) units.get(i);
			targetDur += unit.getTarget().getTargetDurationInSeconds();
			unitDur += unitDurationsInSeconds[i];

			// commented out dead code:
			// if (false && i<len-1)
			// {
			// nextUnit = (SelectedUnit) units.get(i+1);
			// targetDur += nextUnit.getTarget().getTargetDurationInSeconds();
			// unitDur += unitDurationsInSeconds[i+1];
			// }

			tscales[i] = new double[datagrams[i].length];

			for (j = 0; j < datagrams[i].length; j++) {

				tscales[i][j] = targetDur / unitDur;
				// if (tscales[i][j]>1.2)
				// tscales[i][j]=1.2;
				// if (tscales[i][j]<0.8)
				// tscales[i][j]=0.8;

				// tscales[i][j] = 1.2;
			}
			logger.debug("time scaling factor for unit " + unit.getTarget().getName() + " -> " + targetDur / unitDur);
		}
		return tscales;
	}

	// private double[][] getSyllableBasedPitchScales(List<SelectedUnit> units) {
	// List<Phone> phones = ProsodyAnalyzer.parseIntoPhones(units, timeline.getSampleRate());
	// List<Syllable> syllables = Syllable.parseIntoSyllables(phones);
	// ListIterator<Syllable> syllableIterator = syllables.listIterator();
	// while (syllableIterator.hasNext()) {
	// if (!syllableIterator.hasPrevious()) {
	// continue;
	// }
	// // TODO unfinished!
	// }
	// return null;
	// }

	private double[][] getPhoneBasedDurationScales(List<SelectedUnit> units) {

		List<Double> timeScaleFactors = prosodyAnalyzer.getDurationFactors();

		// finally, initialize the tscales array...
		double[][] tscales = new double[timeScaleFactors.size()][];
		Datagram[][] datagrams = getDatagrams(units);
		for (int i = 0; i < tscales.length; i++) {
			tscales[i] = new double[datagrams[i].length];
			// ...which currently provides the same time scale factor for every datagram in a selected unit:
			Arrays.fill(tscales[i], timeScaleFactors.get(i));
		}

		// for quick and dirty debugging, dump tscales to Praat DurationTier:
		try {
			prosodyAnalyzer.writePraatDurationTier(MaryProperties.maryBase() + "/tscales.DurationTier");
		} catch (IOException e) {
			logger.warn("Could not dump tscales to file");
		}

		return tscales;
	}

	/**
	 * Convenience method to grep those SelectedUnits from a List which have positive duration
	 * 
	 * @param units
	 *            units
	 * @return units with positive duration
	 */
	@Deprecated
	private List<SelectedUnit> getNonEmptyUnits(List<SelectedUnit> units) {
		ArrayList<SelectedUnit> nonEmptyUnits = new ArrayList<SelectedUnit>(units.size());
		for (SelectedUnit unit : units) {
			UnitData unitData = (UnitData) unit.getConcatenationData();
			if (unitData.getUnitDuration() > 0 && unit.getTarget().getMaryxmlElement() != null) {
				nonEmptyUnits.add(unit);
			}
		}
		return nonEmptyUnits;
	}

	protected Datagram[][] getRealizedDatagrams(List<Phone> phones) {
		List<Datagram[]> datagramList = new ArrayList<Datagram[]>();
		for (Phone phone : phones) {
			if (phone.getLeftTargetDuration() > 0) {
				Datagram[] leftDatagrams = phone.getLeftUnitFrames();
				datagramList.add(leftDatagrams);
			}
			if (phone.getRightTargetDuration() > 0) {
				Datagram[] rightDatagrams = phone.getRightUnitFrames();
				datagramList.add(rightDatagrams);
			}
		}
		Datagram[][] datagramArray = datagramList.toArray(new Datagram[datagramList.size()][]);
		return datagramArray;
	}

	protected Datagram[] getRealizedRightContexts(List<Phone> phones) {
		List<Datagram> datagramList = new ArrayList<Datagram>();
		for (Phone phone : phones) {
			if (phone.getLeftTargetDuration() > 0) {
				UnitData leftUnitData = phone.getLeftUnitData();
				Datagram leftRightContext = leftUnitData.getRightContextFrame();
				datagramList.add(leftRightContext);
			}
			if (phone.getRightTargetDuration() > 0) {
				UnitData rightUnitData = phone.getRightUnitData();
				Datagram rightRightContext = rightUnitData.getRightContextFrame();
				datagramList.add(rightRightContext);
			}
		}
		Datagram[] datagramArray = datagramList.toArray(new Datagram[datagramList.size()]);
		return datagramArray;
	}

	private boolean[][] getRealizedVoicings(List<Phone> phones) {
		List<boolean[]> voicingList = new ArrayList<boolean[]>();
		for (Phone phone : phones) {
			boolean voiced = phone.isVoiced();
			if (phone.getLeftTargetDuration() > 0) {
				int leftNumberOfFrames = phone.getNumberOfLeftUnitFrames();
				boolean[] leftVoiceds = new boolean[leftNumberOfFrames];
				Arrays.fill(leftVoiceds, voiced);
				voicingList.add(leftVoiceds);
			}
			if (phone.getRightTargetDuration() > 0) {
				int rightNumberOfFrames = phone.getNumberOfRightUnitFrames();
				boolean[] rightVoiceds = new boolean[rightNumberOfFrames];
				Arrays.fill(rightVoiceds, voiced);
				voicingList.add(rightVoiceds);
			}
		}
		boolean[][] voicingArray = voicingList.toArray(new boolean[voicingList.size()][]);
		return voicingArray;
	}

	private double[][] getRealizedTimeScales(List<Phone> phones) {
		List<double[]> durationFactorList = new ArrayList<double[]>(phones.size());
		for (Phone phone : phones) {
			if (phone.getLeftTargetDuration() > 0) {
				int leftNumberOfFrames = phone.getNumberOfLeftUnitFrames();
				double leftDurationFactor = phone.getLeftDurationFactor();
				// scale the factor to reasonably safe values:
				if (leftDurationFactor < minTimeScaleFactor) {
					String message = "Left duration factor (" + leftDurationFactor + ") for phone " + phone + " too small;";
					leftDurationFactor = minTimeScaleFactor;
					message += " clipped to " + leftDurationFactor;
					logger.debug(message);
				} else if (leftDurationFactor > maxTimeScaleFactor) {
					String message = "Left duration factor (" + leftDurationFactor + ") for phone " + phone + " too large;";
					leftDurationFactor = maxTimeScaleFactor;
					message += " clipped to " + leftDurationFactor;
					logger.debug(message);
				}
				double[] leftDurationFactors = new double[leftNumberOfFrames];
				Arrays.fill(leftDurationFactors, leftDurationFactor);
				durationFactorList.add(leftDurationFactors);
			}
			if (phone.getRightTargetDuration() > 0) {
				int rightNumberOfFrames = phone.getNumberOfRightUnitFrames();
				double rightDurationFactor = phone.getRightDurationFactor();
				if (phone.isTransient()) {
					rightDurationFactor = 1; // never modify the duration of a burst
				}
				// scale the factor to reasonably safe values:
				if (rightDurationFactor < minTimeScaleFactor) {
					String message = "Right duration factor (" + rightDurationFactor + ") for phone " + phone + " too small;";
					rightDurationFactor = minTimeScaleFactor;
					message += " clipped to " + rightDurationFactor;
					logger.debug(message);
				} else if (rightDurationFactor > maxTimeScaleFactor) {
					String message = "Right duration factor (" + rightDurationFactor + ") for phone " + phone + " too large;";
					rightDurationFactor = maxTimeScaleFactor;
					message += " clipped to " + rightDurationFactor;
					logger.debug(message);
				}
				double[] rightDurationFactors = new double[rightNumberOfFrames];
				Arrays.fill(rightDurationFactors, rightDurationFactor);
				durationFactorList.add(rightDurationFactors);
			}
		}
		double[][] durationFactorArray = durationFactorList.toArray(new double[durationFactorList.size()][]);
		return durationFactorArray;
	}

	private double[][] getRealizedPitchScales(List<Phone> phones) {
		List<double[]> f0FactorList = new ArrayList<double[]>(phones.size());
		for (Phone phone : phones) {
			if (phone.getLeftTargetDuration() > 0) {
				int leftNumberOfFrames = phone.getNumberOfLeftUnitFrames();
				double[] leftF0Factors = phone.getLeftF0Factors();
				boolean clipped = MathUtils.clipRange(leftF0Factors, minPitchScaleFactor, maxPitchScaleFactor);
				if (clipped) {
					logger.debug("Left F0 factors for phone " + phone + " contained out-of-range values; clipped to ["
							+ minPitchScaleFactor + ", " + maxPitchScaleFactor + "]");
				}
				f0FactorList.add(leftF0Factors);
			}
			if (phone.getRightTargetDuration() > 0) {
				int rightNumberOfFrames = phone.getNumberOfRightUnitFrames();
				double[] rightF0Factors = phone.getRightF0Factors();
				boolean clipped = MathUtils.clipRange(rightF0Factors, minPitchScaleFactor, maxPitchScaleFactor);
				if (clipped) {
					logger.debug("Left F0 factors for phone " + phone + " contained out-of-range values; clipped to ["
							+ minPitchScaleFactor + ", " + maxPitchScaleFactor + "]");
				}
				f0FactorList.add(rightF0Factors);
			}
		}
		double[][] f0FactorArray = f0FactorList.toArray(new double[f0FactorList.size()][]);
		return f0FactorArray;
	}

	/**
	 * Generate audio to match the target pitchmarks as closely as possible.
	 * 
	 * @param units
	 *            units
	 * @return stream
	 * @throws IOException
	 *             IOException
	 */
	protected AudioInputStream generateAudioStream(List<SelectedUnit> units) throws IOException {
		// gather arguments for FDPSOLA processing:
		// Datagram[][] datagrams = getDatagrams(units);
		// Datagram[] rightContexts = getRightContexts(units);
		// boolean[][] voicings = getVoicings(units);
		// double[][] pscales = getPitchScales(units);
		// double[][] tscales = getDurationScales(units);
		// double[][] tscales = getPhoneBasedDurationScales(units);

		List<Phone> realizedPhones = prosodyAnalyzer.getRealizedPhones();
		Datagram[][] datagrams = getRealizedDatagrams(realizedPhones);
		Datagram[] rightContexts = getRealizedRightContexts(realizedPhones);
		boolean[][] voicings = getRealizedVoicings(realizedPhones);
		double[][] tscales = getRealizedTimeScales(realizedPhones);
		double[][] pscales = getRealizedPitchScales(realizedPhones);

		// process into audio stream:
		DDSAudioInputStream stream = (new FDPSOLAProcessor()).processDecrufted(datagrams, rightContexts, audioformat, voicings,
				pscales, tscales);

		// update durations from processed Datagrams:
		// updateUnitDataDurations(units, datagrams);
		updateRealizedUnitDataDurations(realizedPhones, datagrams);

		return stream;
	}

	/**
	 * Explicitly propagate durations of Datagrams to UnitData for each SelectedUnit; those durations are otherwise oblivious to
	 * the data they describe...
	 * 
	 * @param units
	 *            whose data should have its durations updated
	 * @param datagrams
	 *            processed array of arrays of Datagrams which had their durations updated in
	 *            {@link FDPSOLAProcessor#processDecrufted}
	 */
	private void updateUnitDataDurations(List<SelectedUnit> units, Datagram[][] datagrams) {
		for (int i = 0; i < datagrams.length; i++) {
			SelectedUnit unit = units.get(i);
			UnitData unitData = (UnitData) unit.getConcatenationData();
			int unitDuration = 0;
			for (int j = 0; j < datagrams[i].length; j++) {
				int datagramDuration = (int) datagrams[i][j].getDuration();
				unitData.getFrame(j).setDuration(datagramDuration);
				unitDuration += datagramDuration;
			}
			unitData.setUnitDuration(unitDuration);
		}
	}

	private void updateRealizedUnitDataDurations(List<Phone> phones, Datagram[][] datagrams) {
		int phIndex = 0;
		for (Phone phone : phones) {
			if (phone.getLeftTargetDuration() > 0) {
				UnitData leftUnitData = phone.getLeftUnitData();
				int leftUnitDataDuration = 0;
				for (int dg = 0; dg < datagrams[phIndex].length; dg++) {
					int datagramDuration = (int) datagrams[phIndex][dg].getDuration();
					leftUnitData.getFrame(dg).setDuration(datagramDuration);
					leftUnitDataDuration += datagramDuration;
				}
				phIndex++;
				leftUnitData.setUnitDuration(leftUnitDataDuration);
			}
			if (phone.getRightTargetDuration() > 0) {
				UnitData rightUnitData = phone.getRightUnitData();
				int rightUnitDataDuration = 0;
				for (int dg = 0; dg < datagrams[phIndex].length; dg++) {
					int datagramDuration = (int) datagrams[phIndex][dg].getDuration();
					rightUnitData.getFrame(dg).setDuration(datagramDuration);
					rightUnitDataDuration += datagramDuration;
				}
				phIndex++;
				rightUnitData.setUnitDuration(rightUnitDataDuration);
			}
		}
	}
}
