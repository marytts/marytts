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
package marytts.signalproc.process;

/**
 * @author marc
 * 
 */
public interface InlineFrameMerger extends InlineDataProcessor {
	/**
	 * Set the frame of data to merge into the next call of applyInline().
	 * 
	 * @param frameToMerge
	 *            frame to merge
	 */
	public void setFrameToMerge(double[] frameToMerge);

	/**
	 * Set the frame of data to merge into the next call of applyInline(). This method allows for an interpolation of two frames
	 * to be merged into the data set; for example, in order to correct for time misalignment between signal and other frames.
	 * 
	 * @param frame1
	 *            frame 1
	 * @param frame2
	 *            frame 2
	 * @param relativeWeightFrame1
	 *            , a number between 0 and 1 indicating the relative weight of frame1^ with respect to frame2. Consequently, the
	 *            relative weight of frame 2 will be (1 - relativeWeightFrame1).
	 */
	public void setFrameToMerge(double[] frame1, double[] frame2, double relativeWeightFrame1);
}
