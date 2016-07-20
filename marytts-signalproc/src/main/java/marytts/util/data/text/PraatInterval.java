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

/**
 * Basic data unit of an IntervalTier in a Praat TextGrid. Every interval has a start and an end time, as well as a label.
 * 
 * @author steiner
 * 
 */
public class PraatInterval {
	// start time of interval
	private double xmin = Double.NaN;

	// end time of interval
	private double xmax = Double.NaN;

	// label String of interval
	private String text = null;

	// duration of interval, stored independently to facilitate creation of IntervalTiers from sparse representation (e.g.
	// Xwaves).
	private double duration = Double.NaN;

	/**
	 * constructor from start and end times, with empty label
	 * 
	 * @param xmin
	 *            start time of interval
	 * @param xmax
	 *            end time of interval
	 */
	public PraatInterval(double xmin, double xmax) {
		setXmin(xmin);
		setXmax(xmax);
	}

	/**
	 * constructor from start and end times and label
	 * 
	 * @param xmin
	 *            start time of interval
	 * @param xmax
	 *            end time of interval
	 * @param text
	 *            label of interval
	 */
	public PraatInterval(double xmin, double xmax, String text) {
		this(xmin, xmax);
		setText(text);
	}

	/**
	 * constructor for underspecified interval from duration, with empty label; start and end times are filled in later, when
	 * interval is one of many
	 * 
	 * @param duration
	 *            of interval
	 */
	public PraatInterval(double duration) {
		setDuration(duration);
	}

	/**
	 * constructor for underspecified interval from duration and label; start and end times are filled in later, when interval is
	 * one of many
	 * 
	 * @param duration
	 *            of interval
	 * @param text
	 *            label of interval
	 */
	public PraatInterval(double duration, String text) {
		this(duration);
		setText(text);
	}

	/**
	 * getter for start time of interval
	 * 
	 * @return start time as double
	 */
	public double getXmin() {
		return this.xmin;
	}

	/**
	 * getter for end time of interval
	 * 
	 * @return end time as double
	 */
	public double getXmax() {
		return this.xmax;
	}

	/**
	 * getter for label String of interval; should not be null
	 * 
	 * @return label as String
	 */
	public String getText() {
		if (this.text == null) {
			return "";
		}
		return this.text;
	}

	/**
	 * getter for duration of interval
	 * 
	 * @return duration of interval as double
	 */
	public double getDuration() {
		return this.duration;
	}

	/**
	 * set start time of interval
	 * 
	 * @param xmin
	 *            new start time
	 */
	public void setXmin(double xmin) {
		this.xmin = xmin;
		updateDuration();
	}

	/**
	 * set end time of interval
	 * 
	 * @param xmax
	 *            new end time
	 */
	public void setXmax(double xmax) {
		this.xmax = xmax;
		updateDuration();
	}

	/**
	 * set label String of interval
	 * 
	 * @param text
	 *            new label
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * set duration of interval
	 * 
	 * @param duration
	 *            duration
	 * @throws IllegalArgumentException
	 *             if duration is negative
	 */
	public void setDuration(double duration) throws IllegalArgumentException {
		if (Double.isNaN(duration) || duration < 0.0) {
			throw new IllegalArgumentException("duration must be a non-negative value, but was " + duration);
		}
		this.duration = duration;
	}

	/**
	 * recalculate and set duration based on current start and end times
	 */
	public void updateDuration() {
		if (!Double.isNaN(getXmin()) && !Double.isNaN(getXmax())) {
			setDuration(getXmax() - getXmin());
		}
	}

	/**
	 * determine if interval is fully specified
	 * 
	 * @return true if both start and end time are set; false otherwise
	 */
	public boolean isComplete() {
		if (Double.isNaN(getXmin()) || Double.isNaN(getXmax())) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * string representation of interval, used for TextGrid output
	 */
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("xmin = " + getXmin() + " \n");
		str.append("xmax = " + getXmax() + " \n");
		str.append("text = \"" + getText() + "\" \n");
		return str.toString();
	}
}
