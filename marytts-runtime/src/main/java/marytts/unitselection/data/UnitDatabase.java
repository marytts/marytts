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

import java.util.ArrayList;
import java.util.List;

import marytts.cart.CART;
import marytts.unitselection.select.JoinCostFunction;
import marytts.unitselection.select.StatisticalCostFunction;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.TargetCostFunction;
import marytts.unitselection.select.viterbi.ViterbiCandidate;
import marytts.util.MaryUtils;
import marytts.util.data.Datagram;
import marytts.util.dom.DomUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

/**
 * The unit database of a voice
 * 
 * @author Marc Schr&ouml;der
 *
 */
public class UnitDatabase {
	protected TargetCostFunction targetCostFunction;
	protected JoinCostFunction joinCostFunction;
	protected StatisticalCostFunction sCostFunction = null;
	protected UnitFileReader unitReader;
	protected int numUnits;
	protected CART preselectionCART;
	protected TimelineReader audioTimeline;
	protected TimelineReader basenameTimeline;
	protected int backtrace;
	protected Logger logger = MaryUtils.getLogger("UnitDatabase");

	public UnitDatabase() {
	}

	public void load(TargetCostFunction aTargetCostFunction, JoinCostFunction aJoinCostFunction, UnitFileReader aUnitReader,
			CART aPreselectionCART, TimelineReader anAudioTimeline, TimelineReader aBasenameTimeline, int backtraceLeafSize) {
		this.targetCostFunction = aTargetCostFunction;
		this.joinCostFunction = aJoinCostFunction;
		this.unitReader = aUnitReader;
		this.numUnits = (unitReader != null ? unitReader.getNumberOfUnits() : 0);
		this.preselectionCART = aPreselectionCART;
		this.audioTimeline = anAudioTimeline;
		this.basenameTimeline = aBasenameTimeline;
		this.backtrace = backtraceLeafSize;
	}

	public void load(TargetCostFunction aTargetCostFunction, JoinCostFunction aJoinCostFunction,
			StatisticalCostFunction asCostFunction, UnitFileReader aUnitReader, CART aPreselectionCART,
			TimelineReader anAudioTimeline, TimelineReader aBasenameTimeline, int backtraceLeafSize) {
		this.targetCostFunction = aTargetCostFunction;
		this.joinCostFunction = aJoinCostFunction;
		this.sCostFunction = asCostFunction;
		this.unitReader = aUnitReader;
		this.numUnits = (unitReader != null ? unitReader.getNumberOfUnits() : 0);
		this.preselectionCART = aPreselectionCART;
		this.audioTimeline = anAudioTimeline;
		this.basenameTimeline = aBasenameTimeline;
		this.backtrace = backtraceLeafSize;
	}

	public TargetCostFunction getTargetCostFunction() {
		return targetCostFunction;
	}

	public JoinCostFunction getJoinCostFunction() {
		return joinCostFunction;
	}

	public UnitFileReader getUnitFileReader() {
		return unitReader;
	}

	public TimelineReader getAudioTimeline() {
		return audioTimeline;
	}

	public StatisticalCostFunction getSCostFunction() {
		return sCostFunction;
	}

	/**
	 * Preselect a set of candidates that could be used to realise the given target.
	 * 
	 * @param target
	 *            a Target object representing an optimal unit
	 * @return an <span style="color:red;">unsorted</span> ArrayList of ViterbiCandidates, each containing the (same) target and a
	 *         (different) Unit object
	 */
	public List<ViterbiCandidate> getCandidates(Target target) {
		// BEGIN blacklisting
		// The point of this is to get the value of the "blacklist" attribute in the first child element of the MaryXML
		// and store it in the blacklist String variable.
		// This code seems rather inelegant; perhaps there is a better way to access the MaryXML from this method?
		String blacklist = "";
		String unitBasename = "This must never be null or the empty string!"; // otherwise candidate selection fails!
		Element targetElement = target.getMaryxmlElement();
		blacklist = DomUtils.getAttributeFromClosestAncestorOfAnyKind(targetElement, "blacklist");
		// END blacklisting

		// logger.debug("Looking for candidates in cart "+target.getName());
		// get the cart tree and extract the candidates
		int[] clist = (int[]) preselectionCART.interpret(target, backtrace);
		logger.debug("For target " + target + ", selected " + clist.length + " units");

		// Now, clist is an array of unit indexes.
		List<ViterbiCandidate> candidates = new ArrayList<ViterbiCandidate>();
		for (int i = 0; i < clist.length; i++) {
			// The target is the same for all these candidates in the queue
			// remember the actual unit:
			Unit unit = unitReader.getUnit(clist[i]);
			candidates.add(new ViterbiCandidate(target, unit, targetCostFunction));
		}

		// Blacklisting without crazy performance drop:
		// just remove candidates again if their basenames are blacklisted
		java.util.Iterator<ViterbiCandidate> candIt = candidates.iterator();
		while (candIt.hasNext()) {
			ViterbiCandidate candidate = candIt.next();
			unitBasename = getFilename(candidate.getUnit());
			if (blacklist.contains(unitBasename)) {
				candIt.remove();
			}
		}

		return candidates;
	}

	/**
	 * For debugging, return the basename of the original audio file from which the unit is coming, as well as the start time in
	 * that file.
	 * 
	 * @param unit
	 *            unit
	 * @return a String containing basename followed by a space and the unit's start time, in seconds, from the beginning of the
	 *         file. If no basenameTimeline was specified for this voice, returns the string "unknown origin".
	 */
	public String getFilenameAndTime(Unit unit) {
		if (basenameTimeline == null)
			return "unknown origin";
		long[] offset = new long[1];
		try {
			Datagram[] datagrams = basenameTimeline.getDatagrams(unit.startTime, 1, unitReader.getSampleRate(), offset);
			Datagram filenameData = datagrams[0];
			float time = (float) offset[0] / basenameTimeline.getSampleRate();
			String filename = new String(filenameData.getData(), "UTF-8");
			return filename + " " + time;
		} catch (Exception e) {
			logger.warn("Problem getting filename and time for unit " + unit.index + " at time " + unit.startTime, e);
			return "unknown origin";
		}
	}

	/**
	 * For debugging, return the basename of the original audio file from which the unit is coming.
	 * 
	 * @param unit
	 *            unit
	 * @return a String containing basename. If no basenameTimeline was specified for this voice, returns the string
	 *         "unknown origin".
	 */
	public String getFilename(Unit unit) {
		// if (basenameTimeline == null) return "unknown origin";
		try {
			Datagram filenameData = basenameTimeline.getDatagram(unit.startTime);
			String filename = new String(filenameData.getData(), "UTF-8");
			return filename;
		} catch (Exception e) {
			logger.warn("Problem getting filename for unit " + unit.index, e);
			return "unknown origin";
		}
	}

}
