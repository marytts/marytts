/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.tools.voiceimport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import marytts.modules.phonemiser.Allophone;
import marytts.signalproc.analysis.EnergyContourRms;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

import org.apache.commons.lang.ArrayUtils;

/**
 * Compute unit labels from phone labels.
 * 
 * @author schroed
 *
 */
public class HalfPhoneUnitLabelComputer extends PhoneUnitLabelComputer {

	private String ENERGYBASEDTRANSIENTSPLITTING = getName() + ".energyBasedTransientSplitting";
	private boolean energyBasedTransientSplitting;
	private String energyExt = ".energy";
	// these could be user configurable properties, but at this stage, it's too easy to screw up:
	private double windowSizeInSeconds = 0.005;
	private double skipSizeInSeconds = 0.0025;

	public String getName() {
		return "HalfPhoneUnitLabelComputer";
	}

	public HalfPhoneUnitLabelComputer() {
	}

	@Override
	protected void initialiseComp() throws Exception {
		super.initialiseComp();
		unitlabelDir = new File(db.getProp(DatabaseLayout.HALFPHONELABDIR));
		unitlabelExt = db.getProp(DatabaseLayout.HALFPHONELABEXT);
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap<String, String>();
			props.put(ENERGYBASEDTRANSIENTSPLITTING, "false");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(ENERGYBASEDTRANSIENTSPLITTING,
				"Whether to analyze energy in the speech signal to determine midpoints of transient phones (plosives).");
	}

	@Override
	public boolean compute() throws Exception {
		energyBasedTransientSplitting = Boolean.parseBoolean(db.getProperty(ENERGYBASEDTRANSIENTSPLITTING));
		return super.compute();
	}

	@Override
	protected List<Double> getMidTimes(List<String> labels, List<Double> endTimes) {
		assert labels.size() == endTimes.size();

		List<Double> midTimes = new ArrayList<Double>(endTimes.size());
		double startTime = 0;
		for (int i = 0; i < labels.size(); i++) {
			String label = labels.get(i);
			double endTime = endTimes.get(i);

			boolean isTransient = false;
			double peakTime = Double.NaN;
			if (energyBasedTransientSplitting) {
				try {
					Allophone allophone = db.getAllophoneSet().getAllophone(label);
					isTransient = allophone.isPlosive() || allophone.isAffricate();
					if (isTransient) {
						peakTime = getEnergyPeak(startTime, endTime);
					}
				} catch (NullPointerException e) {
					// ignore for now
				} catch (IOException e) {
					// ignore for now
				}
			}

			double midTime;
			if (isTransient && !Double.isNaN(peakTime)) {
				midTime = peakTime;
			} else {
				midTime = (startTime + endTime) / 2;
			}
			midTimes.add(midTime);
			startTime = endTime;
		}
		return midTimes;
	}

	/**
	 * Get time of energy peak difference between startTime and endTime, based on energy analysis of the wav file for the current
	 * baseName.
	 * <p>
	 * The energy analysis (based on the provided parameters {@link #windowSizeInSeconds} and {@link #skipSizeInSeconds}) is saved
	 * to a binary file, which is reused if present (and if the parameter values match those encountered in the file header).
	 * 
	 * @param startTime
	 *            of energy analysis
	 * @param endTime
	 *            of energy analysis
	 * @return the time of the greatest increase in energy between startTime and endTime, or {@link Double#NaN} if no such time
	 *         can be determined from the signal (this is then handled in {@link #getMidTimes(List, List)})
	 * @throws IOException
	 *             if the energy analysis file cannot be read or (initially) created
	 * @see EnergyContourRms#WriteEnergyFile(EnergyContourRms, String)
	 */
	private double getEnergyPeak(double startTime, double endTime) throws IOException {
		// determine wav file name and energy analysis file name:
		String wavDir = db.getProperty(DatabaseLayout.WAVDIR);
		String baseName = bnl.getName(basenameIndex);
		String wavExt = db.getProperty(DatabaseLayout.WAVEXT);
		File wavFile = new File(wavDir, baseName + wavExt);
		File energyFile = new File(unitlabelDir, baseName + energyExt);

		// load or create energy analysis file:
		EnergyContourRms energyContourRMS;
		try {
			energyContourRMS = EnergyContourRms.ReadEnergyFile(energyFile.getAbsolutePath());
			if (energyContourRMS.header.windowSizeInSeconds != windowSizeInSeconds
					|| energyContourRMS.header.skipSizeInSeconds != skipSizeInSeconds) {
				logger.debug("File header of " + energyFile.getAbsolutePath()
						+ " has unexpected parameter values! Will re-analyze...");
				throw new IOException();
			}
		} catch (IOException e) {
			logger.info("Analyzing " + wavFile.getAbsolutePath() + " and saving result to " + energyFile.getAbsolutePath());
			energyContourRMS = new EnergyContourRms(wavFile.getAbsolutePath(), energyFile.getAbsolutePath(), windowSizeInSeconds,
					skipSizeInSeconds);
		}

		// get energy analysis frames between startTime and endTime from energy contour:
		double[] energyContour = energyContourRMS.contour;
		int startFrame = SignalProcUtils.time2frameIndex(startTime, windowSizeInSeconds, skipSizeInSeconds);
		int endFrame = SignalProcUtils.time2frameIndex(endTime, windowSizeInSeconds, skipSizeInSeconds);
		double[] energyLocalContour = ArrayUtils.subarray(energyContour, startFrame, endFrame);

		// get framewise differences:
		double[] energyDiffs = MathUtils.diff(energyLocalContour);
		// we need more than one diff frame:
		if (energyDiffs.length < 2) {
			return Double.NaN;
		}
		// find frame index of peak diff:
		int peakLocalFrame = MathUtils.findGlobalPeakLocation(energyDiffs);
		int peakGlobalFrame = startFrame + peakLocalFrame;
		// convert frame index to time, adding half a window because diffs are between frames:
		double peakTime = SignalProcUtils.frameIndex2Time(peakGlobalFrame, windowSizeInSeconds, skipSizeInSeconds)
				+ windowSizeInSeconds / 2;

		// adjust peak diff time to lie inside time range:
		if (peakTime < startTime) {
			peakTime = startTime;
		} else if (peakTime > endTime) {
			peakTime = endTime;
		}
		return peakTime;
	}

	@Override
	@Deprecated
	protected String[] toUnitLabels(String[] phoneLabels) {
		// We will create exactly two half phones for every phone:
		String[] halfPhoneLabels = new String[2 * phoneLabels.length];
		float startTime = 0;
		int unitIndex = 0;
		for (int i = 0; i < phoneLabels.length; i++) {
			unitIndex++;
			StringTokenizer st = new StringTokenizer(phoneLabels[i]);
			String endTimeString = st.nextToken();
			String dummyNumber = st.nextToken();
			String phone = st.nextToken();
			assert !st.hasMoreTokens();
			float endTime = Float.parseFloat(endTimeString);
			float duration = endTime - startTime;
			assert duration > 0 : "Duration is not > 0 for phone " + i + " (" + phone + ")";
			float midTime = startTime + duration / 2;
			String leftUnitLine = midTime + " " + unitIndex + " " + phone + "_L";
			unitIndex++;
			String rightUnitLine = endTime + " " + unitIndex + " " + phone + "_R";
			halfPhoneLabels[2 * i] = leftUnitLine;
			halfPhoneLabels[2 * i + 1] = rightUnitLine;
			startTime = endTime;
		}
		return halfPhoneLabels;
	}

}
