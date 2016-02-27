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

package marytts.util.data.text;

import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * Representation of an IntervalTier in a Praat TextGrid. Contains a number of intervals, which should not overlap or contain
 * gaps.
 * 
 * @author steiner
 * 
 */
public class PraatIntervalTier implements PraatTier {
	// class used by Praat to distinguish this kind of tier from a TextTier
	private final String tierClass = "IntervalTier";

	// start time of tier
	private double xmin = Double.NaN;

	// end time of tier
	private double xmax = Double.NaN;

	// name of tier
	private String name = null;

	// Vector of intervals containing the actual data
	private Vector<PraatInterval> intervals = new Vector<PraatInterval>();

	/**
	 * bare constructor
	 */
	public PraatIntervalTier() {
		setIntervals(new Vector<PraatInterval>());
	}

	/**
	 * constructor specifying name of new tier
	 * 
	 * @param name
	 *            of tier
	 */
	public PraatIntervalTier(String name) {
		this();
		setName(name);
	}

	/**
	 * constructor providing Vector of intervals
	 * 
	 * @param intervals
	 *            of tier
	 */
	public PraatIntervalTier(Vector<PraatInterval> intervals) {
		setIntervals(intervals);
	}

	/**
	 * getter for class
	 * 
	 * @return class string ("IntervalTier")
	 */
	public String getTierClass() {
		return this.tierClass;
	}

	/**
	 * getter for tier name; should not be null
	 * 
	 * @return tier as String
	 */
	public String getName() {
		if (this.name == null) {
			return "";
		}
		return this.name;
	}

	/**
	 * getter for start time of tier. Assumes that intervals are in sequence
	 * 
	 * @return start time of tier as double
	 */
	public double getXmin() {
		try {
			return this.intervals.firstElement().getXmin();
		} catch (NoSuchElementException nse) {
			return this.xmin;
		}
	}

	/**
	 * getter for end time of tier. Assumes that intervals are in sequence
	 * 
	 * @return end time of tier as double
	 */
	public double getXmax() {
		try {
			return this.intervals.lastElement().getXmax();
		} catch (NoSuchElementException nsu) {
			return this.xmax;
		}
	}

	/**
	 * getter for number of intervals in tier
	 * 
	 * @return number of intervals
	 */
	public int getNumberOfIntervals() {
		return this.intervals.size();
	}

	/**
	 * getter for specific interval
	 * 
	 * @param index
	 *            of desired interval
	 * @return interval
	 */
	public PraatInterval getInterval(int index) {
		return this.intervals.get(index);
	}

	/**
	 * set name of tier
	 * 
	 * @param name
	 *            name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * replace Vector of intervals
	 * 
	 * @param intervals
	 *            intervals
	 */
	public void setIntervals(Vector<PraatInterval> intervals) {
		this.intervals = intervals;
	}

	/**
	 * add interval to the end of intervals
	 * 
	 * @param interval
	 *            interval
	 */
	public void appendInterval(PraatInterval interval) {
		this.intervals.add(interval);
	}

	/**
	 * add times to underspecified (incomplete) intervals
	 */
	public void updateBoundaries() {
		PraatInterval prevInterval = null;
		for (int index = 0; index < getNumberOfIntervals(); index++) {
			PraatInterval interval = getInterval(index);
			if (!interval.isComplete()) {
				if (prevInterval == null) {
					interval.setXmin(0); // preliminary; could just as well be non-zero
				} else {
					interval.setXmin(prevInterval.getXmax());
				}
				if (interval.getDuration() == 0.0) {
					// hack to sidestep problem in Praat; intervals must not be zero
					interval.setDuration(1e-15);
				}
				interval.setXmax(interval.getXmin() + interval.getDuration());
			}
			prevInterval = interval;
		}
	}

	/**
	 * string representation, used for TextGrid output
	 */
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("class = \"" + getTierClass() + "\" \n");
		str.append("name = \"" + getName() + "\" \n");
		str.append("xmin = " + getXmin() + " \n");
		str.append("xmax = " + getXmax() + " \n");
		str.append("intervals: size = " + getNumberOfIntervals() + " \n");
		for (int i = 0; i < getNumberOfIntervals(); i++) {
			str.append("intervals [" + (i + 1) + "]:\n");
			str.append(getInterval(i).toString());
		}
		return str.toString();
	}
}
