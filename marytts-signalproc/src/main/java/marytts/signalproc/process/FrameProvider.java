/**
 * Copyright 2004-2006 DFKI GmbH.
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

import java.io.File;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.display.FunctionGraph;
import marytts.signalproc.display.SignalGraph;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.MathUtils;

/**
 * Cut frames out of a given signal, and provide them one by one, optionally applying a processor to the frame. This base
 * implementation provides frames of a fixed length with a fixed shift.
 * 
 * @author Marc Schr&ouml;der
 * @see PitchFrameProvider
 */
public class FrameProvider {
	protected DoubleDataSource signal;
	protected InlineDataProcessor processor;

	/**
	 * The sampling rate.
	 */
	protected int samplingRate;

	/**
	 * The start time of the currently analysed frame.
	 */
	protected long frameStart;
	protected long nextFrameStart;
	protected int totalRead = 0;
	protected double[] frame;
	protected int validSamplesInFrame;

	protected int frameShift;
	protected int frameLength;
	/**
	 * The part of the original signal to remember for the next overlapping frame.
	 */
	private double[] memory;
	// If !stopWhenTouchingEnd, we must continue reading from memory,
	// padding the respective frames with zeroes. This is the current
	// reading position:
	private int posInMemory;
	private boolean memoryFilled;

	/**
	 * Whether or not this frame provider stops when the first frame touches the last input sample.
	 */
	private boolean stopWhenTouchingEnd;

	/**
	 * Initialise a FrameProvider.
	 * 
	 * @param signal
	 *            the signal source to read from
	 * @param processor
	 *            an optional data processor to apply to the source signal. If null, the original data will be returned.
	 * @param frameLength
	 *            the number of samples in one frame.
	 * @param frameShift
	 *            the number of samples by which to shift the window from one frame analysis to the next; if this is smaller than
	 *            window.getLength(), frames will overlap.
	 * @param samplingRate
	 *            the number of samples in one second.
	 * @param stopWhenTouchingEnd
	 *            whether or not this frame provider stops when the first frame touches the last input sample. When this is set to
	 *            true, the last frame will be the first one including the last sample; when this is set to false, the last frame
	 *            will be the last that still contains any data.
	 */
	public FrameProvider(DoubleDataSource signal, InlineDataProcessor processor, int frameLength, int frameShift,
			int samplingRate, boolean stopWhenTouchingEnd) {
		this.signal = signal;
		this.processor = processor;
		this.frameShift = frameShift;
		this.frameLength = frameLength;
		this.samplingRate = samplingRate;
		this.frame = new double[frameLength];
		this.frameStart = -1;
		this.nextFrameStart = 0;
		validSamplesInFrame = 0;
		// We keep the previous frame in memory (we'll need this if frameShift < frameLength):
		this.memory = new double[frameLength];
		posInMemory = memory.length; // "empty"
		memoryFilled = false;
		this.stopWhenTouchingEnd = stopWhenTouchingEnd;
	}

	/**
	 * Start position of current frame, in seconds
	 * 
	 * @return the start time of the last frame returned by getNextFrame(), or a small negative number if no frame has been served
	 *         yet.
	 */
	public double getFrameStartTime() {
		return (double) frameStart / samplingRate;
	}

	/**
	 * Start position of current frame, in samples
	 * 
	 * @return the start position of the last frame returned by getNextFrame(), or -1 if no frame has been served yet.
	 */
	public long getFrameStartSamples() {
		return frameStart;
	}

	public int getSamplingRate() {
		return samplingRate;
	}

	/**
	 * The amount of time by which one frame is shifted against the next.
	 * 
	 * @return frameShift / samplingRate
	 */
	public double getFrameShiftTime() {
		return (double) frameShift / samplingRate;
	}

	/**
	 * The number of samples by which one frame is shifted against the next.
	 * 
	 * @return frameShift
	 */
	public int getFrameShiftSamples() {
		return frameShift;
	}

	/**
	 * The time length of a frame.
	 * 
	 * @return getFrameLengthSamples / samplingRate
	 */
	public double getFrameLengthTime() {
		return (double) getFrameLengthSamples() / samplingRate;
	}

	/**
	 * The number of samples in the current frame.
	 * 
	 * @return frameLength
	 */
	public int getFrameLengthSamples() {
		return frameLength;
	}

	/**
	 * Whether or not this frame provider stops when the first frame touches the last input sample. When this returns true, the
	 * last frame will be the first one including the last sample; when this returns false, the last frame will be the last that
	 * still contains any data. Defaults to true.
	 * 
	 * @return stopWhenTouchingEnd
	 */
	public boolean stopWhenTouchingEnd() {
		return stopWhenTouchingEnd;
	}

	/**
	 * Whether or not this frameprovider can provide another frame.
	 * 
	 * @return signal.hasMoreData() or different from stopWhenTouchingEnd and memoryFilled and posInMemory &lt; memory.length
	 */
	public boolean hasMoreData() {
		return signal.hasMoreData() || !stopWhenTouchingEnd && memoryFilled && posInMemory < memory.length;
	}

	/**
	 * This tells how many valid samples have been read into the current frame (before applying the optional data processor!).
	 * 
	 * @return validSamplesInFrame
	 */
	public int validSamplesInFrame() {
		return validSamplesInFrame;
	}

	/**
	 * Fill the internal double array with the next frame of data. The last frame, if only partially filled with the rest of the
	 * signal, is filled up with zeroes. If stopWhenTouchingEnd() returns true, this method will provide not more than a single
	 * zero-padded frame at the end of the signal.
	 * 
	 * @return the next frame on success, null on failure.
	 */
	public double[] getNextFrame() {
		frameStart = nextFrameStart;
		if (!hasMoreData()) {
			validSamplesInFrame = 0;
			return null;
		}
		// A frame is composed from two sources:
		// 1. memory and 2. newly read signal.
		int nFromMemory;
		// 1. Prepend some memory?
		if (memoryFilled && posInMemory < memory.length) {
			nFromMemory = memory.length - posInMemory;
			// System.err.println("Reusing " + nFromMemory + " samples from previous frame");
			System.arraycopy(memory, posInMemory, frame, 0, nFromMemory);
		} else {
			nFromMemory = 0;
			// System.err.println("No data to reuse from previous frame.");
		}
		// 2. Read new bit of signal:
		int read = getData(nFromMemory);
		totalRead += read;
		// At end of input signal, we are unable to fill the frame completely:
		if (nFromMemory + read < frameLength) { // zero-pad last frame(s)
			assert !signal.hasMoreData();
			// Pad with zeroes.
			Arrays.fill(frame, nFromMemory + read, frame.length, 0);
		}
		validSamplesInFrame = nFromMemory + read; // = frame.length except for last frame
		// OK, the frame is filled.

		// For overlapping frames,
		// remember the frame data in order to reuse part of it for next frame
		int amountToRemember = frameLength - frameShift;
		if (validSamplesInFrame < frameLength) {
			amountToRemember = validSamplesInFrame - frameShift;
		}
		if (amountToRemember > 0) {
			if (memory.length < amountToRemember) {
				memory = new double[amountToRemember];
			}
			System.arraycopy(frame, validSamplesInFrame - amountToRemember, memory, memory.length - amountToRemember,
					amountToRemember);
			posInMemory = memory.length - amountToRemember;
			memoryFilled = true;
		} else {
			posInMemory = memory.length;
			memoryFilled = false;
		}

		// Apply the processor to the data:
		if (processor != null)
			processor.applyInline(frame, 0, frameLength);

		nextFrameStart = frameStart + frameShift;
		// System.err.println("FrameProvider: Frame "+" (" + frameStartTime+"-"+(frameStartTime+getFrameLengthTime()) + "): read "
		// + read + "(total "+totalRead+"), posInMemory " + posInMemory + ", memory.length " + memory.length + ", valid " +
		// validSamplesInFrame);

		return frame;
	}

	public double[] getCurrentFrame() {
		return frame;
	}

	/**
	 * Read data from input signal into current frame. This base implementation will attempt to fill the frame from the position
	 * given in nPrefilled onwards.
	 * 
	 * @param nPrefilled
	 *            number of valid values at the beginning of frame. These should not be lost or overwritten.
	 * @return the number of new values read into frame at position nPrefilled.
	 */
	protected int getData(int nPrefilled) {
		return signal.getData(frame, nPrefilled, frame.length - nPrefilled);
	}

	/**
	 * Reset the internal time stamp to 0.
	 */
	public void resetInternalTimer() {
		this.frameStart = -1;
		this.nextFrameStart = 0;
		this.totalRead = 0;
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
			int samplingRate = (int) inputAudio.getFormat().getSampleRate();
			double[] signal = new AudioDoubleDataSource(inputAudio).getAllData();
			FrameProvider fp = new FrameProvider(new BufferedDoubleDataSource(signal), null, 2048, 512, samplingRate, false);
			double[] result = new double[signal.length];
			int resultPos = 0;
			while (fp.hasMoreData()) {
				double[] frame = fp.getNextFrame();
				if (fp.validSamplesInFrame() >= fp.getFrameShiftSamples()) {
					System.arraycopy(frame, 0, result, resultPos, fp.getFrameShiftSamples());
					resultPos += fp.getFrameShiftSamples();
				} else {
					System.arraycopy(frame, 0, result, resultPos, fp.validSamplesInFrame());
					resultPos += fp.validSamplesInFrame();
				}
			}
			System.err.println("Signal has length " + signal.length + ", result " + resultPos);
			double err = MathUtils.sumSquaredError(signal, result);
			System.err.println("Sum squared error: " + err);
			if (err > 0.000001) {
				double[] difference = MathUtils.subtract(signal, result);
				FunctionGraph diffGraph = new SignalGraph(difference, samplingRate);
				diffGraph.showInJFrame("difference", true, true);
			}
		}
	}

}
