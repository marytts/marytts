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

package marytts.tools.voiceimport;

import java.io.File;
import java.util.ArrayList;

import marytts.util.data.Datagram;

/**
 * Abstract class to wrap a data file in a manner suitable for feeding it as {@link Datagram}s into an
 * {@link AbstractTimelineMaker}.
 * 
 * @author steiner
 * 
 */
abstract class AbstractDataFile {

	protected int sampleRate;

	protected float frameSkip;

	protected int numFrames;

	protected int frameDuration;

	protected Datagram[] datagrams;

	/**
	 * main constructor
	 * 
	 * @param file
	 *            to load
	 */
	public AbstractDataFile(File file) {
		load(file);
	}

	/**
	 * load the File; only extension classes know how to do this
	 * 
	 * @param file
	 *            to load
	 */
	protected abstract void load(File file);

	/**
	 * get the sample rate (in Hz)
	 * 
	 * @return the sampleRate
	 */
	public int getSampleRate() {
		return sampleRate;
	}

	/**
	 * get the frame skip (in seconds)
	 * 
	 * @return the frameSkip
	 */
	public float getFrameSkip() {
		return frameSkip;
	}

	/**
	 * get datagrams; no special requirements for their total duration
	 * 
	 * @return datagrams
	 */
	public Datagram[] getDatagrams() {
		return datagrams;
	}

	/**
	 * Get datagrams; if the total duration of all Datagrams is <i>longer</i> than <b>forcedDuration</b>, excess Datagrams will be
	 * silently dropped, and the duration of the last included one is shortened to match forcedDuration. If the total duration of
	 * all Datagrams is <i>shorter</i> than <b>forcedDuration</b>, a final "filler" Datagram is appended which contains no data,
	 * but whose duration increases the total duration to satisfy <b>forcedDuration</b>.
	 * 
	 * @param forcedDuration
	 *            of all Datagrams (in samples)
	 * @return datagrams
	 */
	public Datagram[] getDatagrams(int forcedDuration) {
		// initialize datagrams as a List:
		ArrayList<Datagram> datagramList = new ArrayList<Datagram>(numFrames);
		int durationMismatch = forcedDuration;

		// iterate over all data frames:
		for (Datagram datagram : datagrams) {
			durationMismatch -= datagram.getDuration();
			if (durationMismatch < 0) {
				// if forcedDuration is exceeded, adjust the final frame's duration and break out of loop:
				datagram.setDuration(datagram.getDuration() + durationMismatch);
				datagramList.add(datagram);
				break;
			}
			datagramList.add(datagram);
		}

		// if total duration of all Datagrams is less than forcedDuration, pad with empty filler:
		if (durationMismatch > 0) {
			byte[] nothing = new byte[] {};
			Datagram filler = new Datagram(durationMismatch, nothing);
			datagramList.add(filler);
		}

		// return datagrams as array:
		Datagram[] datagramArray = datagramList.toArray(new Datagram[datagramList.size()]);
		return datagramArray;
	}
}
