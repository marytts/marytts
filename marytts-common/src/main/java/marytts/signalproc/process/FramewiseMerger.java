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
package marytts.signalproc.process;

import java.util.Arrays;

import marytts.signalproc.window.DynamicTwoHalvesWindow;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.SequenceDoubleDataSource;

/**
 * A class to merge two audio signals, using pitch-synchronous frames.
 * 
 * @author marc
 * 
 */
public class FramewiseMerger extends FrameOverlapAddSource {
	protected DoubleDataSource labelTimes;
	protected DoubleDataSource otherLabelTimes;
	protected FrameProvider otherFrameProvider;
	protected double prevLabel, currentLabel, prevOtherLabel, currentOtherLabel;
	protected double localTimeStretchFactor = 1;

	/**
	 * Create a new merger, creating audio by pitch-synchronous merging of audio frames from a source (aka the "signal") and a
	 * target (aka the "other"), linearly mapping the corresponding times between the two sources.
	 * 
	 * @param inputSource
	 *            the audio data for the signal
	 * @param pitchmarks
	 *            the pitchmarks for the signal
	 * @param samplingRate
	 *            the sampling rate for the signal
	 * @param labelTimes
	 *            optionally, the label times for the signal, needed for time alignment between the signal and the other
	 * @param otherSource
	 *            the audio data for the other
	 * @param otherPitchmarks
	 *            the pitchmarks for the other
	 * @param otherSamplingRate
	 *            the sampling rate for the other
	 * @param otherLabelTimes
	 *            optionally, the label times for the other; if both are present, the time interval between the i-th and the
	 *            (i+1)-th label time is linearly stretched/squeezed in order to find the mapping frame for interpolation
	 * @param merger
	 *            the signal processing method used for merging the properties of the "other" into the corresponding frame in the
	 *            "signal".
	 */
	public FramewiseMerger(DoubleDataSource inputSource, DoubleDataSource pitchmarks, int samplingRate,
			DoubleDataSource labelTimes, DoubleDataSource otherSource, DoubleDataSource otherPitchmarks, int otherSamplingRate,
			DoubleDataSource otherLabelTimes, InlineFrameMerger merger) {
		// Set up label times for time stretching:
		this.labelTimes = labelTimes;
		this.otherLabelTimes = otherLabelTimes;
		// set all current and previous labels to 0:
		prevLabel = 0;
		currentLabel = 0;
		prevOtherLabel = 0;
		currentOtherLabel = 0;

		InlineDataProcessor analysisWindow = new DynamicTwoHalvesWindow(Window.HANNING, 0.5);
		// Overlap-add a properly windowed first period by hand:
		// Read out the first pitchmark:
		double firstPitchmark = pitchmarks.getData(1)[0];
		assert firstPitchmark > 0;
		// If the first pitchmark is too close (closer than 1ms) to origin, skip it:
		if (firstPitchmark < 0.001 * samplingRate)
			firstPitchmark = pitchmarks.getData(1)[0];
		pitchmarks = new SequenceDoubleDataSource(new DoubleDataSource[] {
				new BufferedDoubleDataSource(new double[] { firstPitchmark }), pitchmarks });
		int firstPeriodLength = (int) (firstPitchmark * samplingRate);
		double[] firstPeriod = new double[firstPeriodLength];
		inputSource.getData(firstPeriod, 0, firstPeriodLength);
		inputSource = new SequenceDoubleDataSource(new DoubleDataSource[] { new BufferedDoubleDataSource(firstPeriod),
				inputSource });
		this.memory = new double[2 * firstPeriodLength];
		System.arraycopy(firstPeriod, 0, memory, firstPeriodLength, firstPeriodLength);
		analysisWindow.applyInline(memory, 0, memory.length);
		if (merger != null) {
			// Read out the first pitchmark:
			double firstOtherPitchmark = otherPitchmarks.getData(1)[0];
			assert firstOtherPitchmark > 0;
			// If the first other pitchmark is too close (closer than 1ms) to origin, skip it:
			if (firstOtherPitchmark < 0.001 * otherSamplingRate)
				firstPitchmark = otherPitchmarks.getData(1)[0];
			otherPitchmarks = new SequenceDoubleDataSource(new DoubleDataSource[] {
					new BufferedDoubleDataSource(new double[] { firstOtherPitchmark }), otherPitchmarks });
			int firstOtherPeriodLength = (int) (firstOtherPitchmark * otherSamplingRate);
			double[] firstOtherPeriod = new double[firstOtherPeriodLength];
			otherSource.getData(firstOtherPeriod, 0, firstOtherPeriodLength);
			otherSource = new SequenceDoubleDataSource(new DoubleDataSource[] { new BufferedDoubleDataSource(firstOtherPeriod),
					otherSource });
			double[] frameToMerge = new double[2 * firstOtherPeriodLength];
			System.arraycopy(firstOtherPeriod, 0, frameToMerge, firstOtherPeriodLength, firstOtherPeriodLength);
			merger.setFrameToMerge(frameToMerge);
			merger.applyInline(memory, 0, memory.length);
		}
		// Shift the data left in memory:
		System.arraycopy(memory, firstPeriodLength, memory, 0, firstPeriodLength);
		Arrays.fill(memory, firstPeriodLength, memory.length, 0);
		// And initialise frame providers for normal operation:
		this.frameProvider = new PitchFrameProvider(inputSource, pitchmarks, analysisWindow, samplingRate, 8, 1);
		this.otherFrameProvider = new PitchFrameProvider(otherSource, otherPitchmarks, analysisWindow, otherSamplingRate, 8, 1);
		this.processor = merger;
	}

	/**
	 * Create a new merger, creating audio by merging of audio frames at a fixed frame rate, from a source (aka the "signal") and
	 * a target (aka the "other"), linearly mapping the corresponding times between the two sources.
	 * 
	 * @param inputSource
	 *            the audio data for the signal
	 * @param frameLength
	 *            length of the fixed-length frames
	 * @param samplingRate
	 *            the sampling rate for the signal
	 * @param labelTimes
	 *            optionally, the label times for the signal, needed for time alignment between the signal and the other
	 * @param otherSource
	 *            the audio data for the other
	 * @param otherSamplingRate
	 *            the sampling rate for the other
	 * @param otherLabelTimes
	 *            optionally, the label times for the other; if both are present, the time interval between the i-th and the
	 *            (i+1)-th label time is linearly stretched/squeezed in order to find the mapping frame for interpolation
	 * @param merger
	 *            the signal processing method used for merging the properties of the "other" into the corresponding frame in the
	 *            "signal".
	 */
	public FramewiseMerger(DoubleDataSource inputSource, int frameLength, int samplingRate, DoubleDataSource labelTimes,
			DoubleDataSource otherSource, int otherSamplingRate, DoubleDataSource otherLabelTimes, InlineFrameMerger merger) {
		DoubleDataSource paddingOther1 = new BufferedDoubleDataSource(new double[3 * frameLength / 4]);
		DoubleDataSource paddedOtherSource = new SequenceDoubleDataSource(new DoubleDataSource[] { paddingOther1, otherSource });
		this.otherFrameProvider = new FrameProvider(paddedOtherSource, Window.get(Window.HANNING, frameLength, 0.5), frameLength,
				frameLength / 4, samplingRate, true);
		this.blockSize = frameLength / 4;
		int inputFrameshift = blockSize;
		Window window = Window.get(Window.HANNING, frameLength + 1, 0.5);
		this.outputWindow = null;
		this.memory = new double[frameLength];
		// This is used when the last input frame has already been read,
		// to do the last frame output properly:
		this.processor = merger;
		// We need to feed through (and discard) 3 (if overlapFraction == 3/4)
		// blocks of zeroes, so that the first three blocks are properly rebuilt.
		DoubleDataSource padding1 = new BufferedDoubleDataSource(new double[3 * inputFrameshift]);
		DoubleDataSource padding2 = new BufferedDoubleDataSource(new double[3 * inputFrameshift]);
		DoubleDataSource paddedSource = new SequenceDoubleDataSource(new DoubleDataSource[] { padding1, inputSource, padding2 });
		this.frameProvider = new FrameProvider(paddedSource, window, frameLength, inputFrameshift, samplingRate, true);
		double[] dummy = new double[blockSize];
		for (int i = 0; i < 3; i++) {
			// System.err.println("Discarding "+blockSize+" samples:");
			getData(dummy, 0, blockSize); // this calls getNextFrame() indirectly
		}
		this.frameProvider.resetInternalTimer();
		this.otherFrameProvider.resetInternalTimer();

		// Only now, after initialising the overlap-add, set up the label times.
		// Set up label times for time stretching:
		this.labelTimes = labelTimes;
		this.otherLabelTimes = otherLabelTimes;
		// set all current and previous labels to 0:
		prevLabel = 0;
		currentLabel = 0;
		prevOtherLabel = 0;
		currentOtherLabel = 0;
	}

	/**
	 * Get the next frame of input data. This method is called by prepareBlock() when preparing the output data to be read. This
	 * implementation reads the data from the frameProvider. In addition, the appropriate "other" frame is identified; this is the
	 * frame closest in starting time to the starting time of the next "signal" frame (i.e., the starting time of the return value
	 * of this method), correcting for the label times. Concretely, if the start time t_s of the next signal frame is between
	 * labelTimes[i] and labelTimes[i+1], then the optimal other frame starting time to would be: t_o = otherLabelTimes[i] + (t_s
	 * - labelTimes[i])/(labelTimes[i+1]-labelTimes[i]) * (otherLabelTimes[i+1] - otherLabelTimes[i]) The other frame whose
	 * starting time is closest to to will be prepared as for merging.
	 * 
	 * @return the next signal frame.
	 */
	protected double[] getNextFrame() {
		double[] nextSignalFrame = frameProvider.getNextFrame();
		double frameStart = frameProvider.getFrameStartTime();
		// System.out.println("Getting signal frame, start time = "+frameStart);
		while (frameStart >= currentLabel) {
			// move to next label
			if (labelTimes == null || otherLabelTimes == null || !labelTimes.hasMoreData() || !otherLabelTimes.hasMoreData()) {
				currentLabel = Double.POSITIVE_INFINITY;
				localTimeStretchFactor = 1;
			} else {
				prevLabel = currentLabel;
				currentLabel = labelTimes.getData(1)[0];
				assert currentLabel >= prevLabel;
				prevOtherLabel = currentOtherLabel;
				currentOtherLabel = otherLabelTimes.getData(1)[0];
				assert currentOtherLabel >= prevOtherLabel;
				// System.out.println("current label: "+currentLabel+"("+prevLabel+")");
				// System.out.println("other   label: "+currentOtherLabel+"("+prevOtherLabel+")");
				if (currentLabel == prevLabel || currentOtherLabel == prevOtherLabel) {
					localTimeStretchFactor = 1;
				} else {
					localTimeStretchFactor = (currentOtherLabel - prevOtherLabel) / (currentLabel - prevLabel);
				}
			}
		}
		assert prevLabel <= frameStart && frameStart < currentLabel;
		// System.out.println("Local time stretch = "+localTimeStretchFactor);
		double targetOtherStart = prevOtherLabel + (frameStart - prevLabel) * localTimeStretchFactor;
		// System.out.println("Target other start = "+targetOtherStart);
		double otherStart = otherFrameProvider.getFrameStartTime();
		double[] otherFrame = otherFrameProvider.getCurrentFrame();
		double prevOtherStart = -1;
		double[] prevOtherFrame = null;
		// System.out.println("Current other frame starts at "+otherStart);
		if (otherStart < 0) { // no other frame yet
			otherFrame = otherFrameProvider.getNextFrame();
			otherStart = otherFrameProvider.getFrameStartTime();
			// System.out.println("Getting first other frame -- starts at "+otherStart);
		}
		assert otherStart >= 0;
		// Now skip other frames until the current otherStart is closer to targetOtherStart
		// then the next one would be.
		double expectedNextOtherStart = otherStart + otherFrameProvider.getFrameShiftTime();
		// while (Math.abs(expectedNextOtherStart-targetOtherStart)<Math.abs(otherStart-targetOtherStart)
		while (otherStart < targetOtherStart && otherFrameProvider.hasMoreData()) {
			prevOtherFrame = (double[]) otherFrame.clone();
			prevOtherStart = otherStart;
			otherFrame = otherFrameProvider.getNextFrame();
			otherStart = otherFrameProvider.getFrameStartTime();
			// System.out.println("Skipping frame -- new one starts at "+otherStart);
			assert Math.abs(otherStart - expectedNextOtherStart) < 1e-10 : "Other frame starts at " + otherStart
					+ " -- expected was " + expectedNextOtherStart;
			expectedNextOtherStart = otherStart + otherFrameProvider.getFrameShiftTime();
		}
		if (prevOtherFrame == null) {
			((InlineFrameMerger) processor).setFrameToMerge(otherFrame);
		} else {
			assert prevOtherStart < targetOtherStart;
			assert targetOtherStart <= otherStart || !otherFrameProvider.hasMoreData();
			if (targetOtherStart > otherStart)
				targetOtherStart = otherStart;
			// Request interpolation between prevOtherFrame and otherFrame in relation to their distance to targetOtherStart
			// Linear interpolation:
			double rPrev = 1 - (targetOtherStart - prevOtherStart) / (otherStart - prevOtherStart);
			assert 0 <= rPrev;
			assert rPrev < 1;
			// PrintfFormat f = new PrintfFormat("%.3f");
			// System.out.println("Prev: "+f.sprintf(prevOtherStart)+" Target: "+f.sprintf(targetOtherStart)+" Other: "+f.sprintf(otherStart)+" rPrev: "+f.sprintf(rPrev));
			((InlineFrameMerger) processor).setFrameToMerge(prevOtherFrame, otherFrame, rPrev);
		}

		return nextSignalFrame;
	}

	/**
	 * Output blocksize -- here, this is the same as the input frame shift.
	 */
	protected int getBlockSize() {
		return frameProvider.getFrameShiftSamples();
	}

	/**
	 * @param args
	 *            args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
