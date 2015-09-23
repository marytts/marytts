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
package marytts.unitselection.concat;

import java.io.IOException;
import java.util.List;

import javax.sound.sampled.AudioInputStream;

import marytts.unitselection.data.Unit;
import marytts.unitselection.select.SelectedUnit;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.Datagram;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;

public class OverlapUnitConcatenator extends BaseUnitConcatenator {

	public OverlapUnitConcatenator() {
		super();
	}

	/**
	 * Get the raw audio material for each unit from the timeline.
	 * 
	 * @param units
	 *            units
	 */
	protected void getDatagramsFromTimeline(List<SelectedUnit> units) throws IOException {
		for (SelectedUnit unit : units) {
			assert !unit.getUnit().isEdgeUnit() : "We should never have selected any edge units!";
			OverlapUnitData unitData = new OverlapUnitData();
			unit.setConcatenationData(unitData);
			int nSamples = 0;
			int unitSize = unitToTimeline(unit.getUnit().duration); // convert to timeline samples
			long unitStart = unitToTimeline(unit.getUnit().startTime); // convert to timeline samples
			// System.out.println("Unit size "+unitSize+", pitchmarksInUnit "+pitchmarksInUnit);
			// System.out.println(unitStart/((float)timeline.getSampleRate()));
			// System.out.println("Unit index = " + unit.getUnit().getIndex());

			Datagram[] datagrams = timeline.getDatagrams(unitStart, (long) unitSize);
			unitData.setFrames(datagrams);
			// one right context period for windowing:
			Datagram rightContextFrame = null;
			Unit nextInDB = database.getUnitFileReader().getNextUnit(unit.getUnit());
			if (nextInDB != null && !nextInDB.isEdgeUnit()) {
				rightContextFrame = timeline.getDatagram(unitStart + unitSize);
				unitData.setRightContextFrame(rightContextFrame);
			}
		}
	}

	/**
	 * Determine target pitchmarks (= duration and f0) for each unit.
	 * 
	 * @param units
	 *            units
	 */
	protected void determineTargetPitchmarks(List<SelectedUnit> units) {
		for (SelectedUnit unit : units) {
			UnitData unitData = (UnitData) unit.getConcatenationData();
			assert unitData != null : "Should not have null unitdata here";
			Datagram[] datagrams = unitData.getFrames();
			Datagram[] frames = null; // frames to realise
			// The number and duration of the frames to realise
			// must be the result of the target pitchmark computation.

			// Set target pitchmarks,
			// either by copying from units (data-driven)
			// or by computing from target (model-driven)
			int unitDuration = 0;
			int nZeroLengthDatagrams = 0;
			for (int i = 0; i < datagrams.length; i++) {
				int dur = (int) datagrams[i].getDuration();
				if (dur == 0)
					nZeroLengthDatagrams++;
				unitDuration += datagrams[i].getDuration();
			}
			if (nZeroLengthDatagrams > 0) {
				logger.warn("Unit " + unit + " contains " + nZeroLengthDatagrams + " zero-length datagrams -- removing them");
				Datagram[] dummy = new Datagram[datagrams.length - nZeroLengthDatagrams];
				for (int i = 0, j = 0; i < datagrams.length; i++) {
					if (datagrams[i].getDuration() > 0) {
						dummy[j++] = datagrams[i];
					}
				}
				datagrams = dummy;
				unitData.setFrames(datagrams);
			}
			if (unit.getTarget().isSilence()) {
				int targetDuration = Math.round(unit.getTarget().getTargetDurationInSeconds() * audioformat.getSampleRate());
				if (targetDuration > 0 && datagrams != null && datagrams.length > 0) {
					int firstPeriodDur = (int) datagrams[0].getDuration();
					if (targetDuration < firstPeriodDur) {
						logger.debug("For " + unit + ", adjusting target duration to be at least one period: "
								+ (firstPeriodDur / audioformat.getSampleRate()) + " s instead of requested "
								+ unit.getTarget().getTargetDurationInSeconds() + " s");
						targetDuration = firstPeriodDur;
					}
					if (unitDuration < targetDuration) {
						// insert silence in the middle
						frames = new Datagram[datagrams.length + 1];
						int mid = (datagrams.length + 1) / 2;
						System.arraycopy(datagrams, 0, frames, 0, mid);
						if (mid < datagrams.length) {
							System.arraycopy(datagrams, mid, frames, mid + 1, datagrams.length - mid);
						}
						frames[mid] = createZeroDatagram(targetDuration - unitDuration);
					} else { // unitDuration >= targetDuration
						// cut frames from the middle
						int midright = (datagrams.length + 1) / 2; // first frame of the right part
						int midleft = midright - 1; // last frame of the left part
						while (unitDuration > targetDuration && midright < datagrams.length) {
							unitDuration -= datagrams[midright].getDuration();
							midright++;
							if (unitDuration > targetDuration && midleft > 0) { // force it to leave at least one frame, therefore
																				// > 0
								unitDuration -= datagrams[midleft].getDuration();
								midleft--;
							}
						}
						frames = new Datagram[midleft + 1 + datagrams.length - midright];
						assert midleft >= 0;
						System.arraycopy(datagrams, 0, frames, 0, midleft + 1);
						if (midright < datagrams.length) {
							System.arraycopy(datagrams, midright, frames, midleft + 1, datagrams.length - midright);
						}
					}
					unitDuration = targetDuration; // now they are the same
				} else { // unitSize == 0, we have a zero-length silence unit
					// artificial silence data:
					frames = new Datagram[] { createZeroDatagram(targetDuration) };
					unitDuration = targetDuration;
				}
			} else { // not silence
				// take unit as is
				frames = datagrams;
			}
			unitData.setUnitDuration(unitDuration);
			unitData.setFrames(frames);
		}
	}

	/**
	 * Generate audio to match the target pitchmarks as closely as possible.
	 * 
	 * @param units
	 *            units
	 * @return new DDSAudioInputStream(new BufferedDoubleDataSource(audioSource), audioformat)
	 * @throws IOException
	 *             IOException
	 */
	protected AudioInputStream generateAudioStream(List<SelectedUnit> units) throws IOException {
		int len = units.size();
		Datagram[][] datagrams = new Datagram[len][];
		Datagram[] rightContexts = new Datagram[len];
		for (int i = 0; i < len; i++) {
			SelectedUnit unit = units.get(i);
			OverlapUnitData unitData = (OverlapUnitData) unit.getConcatenationData();
			assert unitData != null : "Should not have null unitdata here";
			Datagram[] frames = unitData.getFrames();
			assert frames != null : "Cannot generate audio from null frames";
			// Generate audio from frames
			datagrams[i] = frames;
			Unit nextInDB = database.getUnitFileReader().getNextUnit(unit.getUnit());
			Unit nextSelected;
			if (i + 1 == len)
				nextSelected = null;
			else
				nextSelected = units.get(i + 1).getUnit();
			if (nextInDB != null && !nextInDB.equals(nextSelected)) {
				// Only use right context if we have a next unit in the DB is not the
				// same as the next selected unit.
				rightContexts[i] = unitData.getRightContextFrame(); // may be null
			}
		}

		DoubleDataSource audioSource = new DatagramOverlapDoubleDataSource(datagrams, rightContexts);
		return new DDSAudioInputStream(new BufferedDoubleDataSource(audioSource), audioformat);
	}

	public static class OverlapUnitData extends BaseUnitConcatenator.UnitData {
		protected Datagram rightContextFrame;

		public void setRightContextFrame(Datagram aRightContextFrame) {
			this.rightContextFrame = aRightContextFrame;
		}

		public Datagram getRightContextFrame() {
			return rightContextFrame;
		}
	}
}
