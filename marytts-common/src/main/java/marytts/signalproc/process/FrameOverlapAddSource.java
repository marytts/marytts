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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.display.FunctionGraph;
import marytts.signalproc.display.SignalGraph;
import marytts.signalproc.window.Window;
import marytts.util.data.BlockwiseDoubleDataSource;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.SequenceDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;

/**
 * Compute the overlap-add of a framewise-processed input signal, with optional time stretching (in subclasses). The OLA algorithm
 * works as follows. 1. Assuming an input frameshift of 1/12th of the frame length, and a signal length equal to (frame
 * length+4*frameshift), we cover the input data as follows: (+=valid data points, -=zero, |=valid data point at start/end of
 * input)
 * 
 * <pre>
 *     ---|++++++++
 *      --|+++++++++
 *       -|++++++++++
 *        |+++++++++++
 *         ++++++++++++
 *          ++++++++++++
 *           ++++++++++++
 *            +++++++++++|
 *             ++++++++++|-
 *              +++++++++|--
 *               ++++++++|---
 * </pre>
 * 
 * With a synthesis frameshift of 1/4th of the frame length, implying that four frames need to be overlapped to reconstruct the
 * signal, this becomes:
 * 
 * <pre>
 *          ---|++++++++
 *             --|+++++++++
 *                -|++++++++++    
 *                   |+++++++++++ *** first usable
 *                      ++++++++++++
 *                         ++++++++++++
 *                            ++++++++++++
 *                               +++++++++++| 
 *                                  ++++++++++|- 
 *                                     +++++++++|--
 *                                        ++++++++|--- *** last usable: first 3 of this
 * </pre>
 * 
 * It can be seen that three times the input frameshift needs to be zero-padded before the signal, and discarded to reach proper
 * signal reconstruction.
 * 
 * Similarly, the last frame to be used is the one to which three times the input shift has been zero-padded; only the first
 * output frameshift samples of it can be used.
 * 
 * 2. Assuming an input frameshift of 1/24th of the frame length, and a signal length equal to (frame length+4*frameshift), we
 * cover the input data as follows: (+=valid data points, -=zero, |=valid data point at start/end of input)
 * 
 * <pre>
 * --------|+++++++++++++++
 *  -------|++++++++++++++++
 *   ------|+++++++++++++++++
 *    -----|++++++++++++++++++
 *     ----|+++++++++++++++++++
 *      ---|++++++++++++++++++++
 *       --|+++++++++++++++++++++
 *        -|++++++++++++++++++++++
 *         |+++++++++++++++++++++++
 *          ++++++++++++++++++++++++
 *           ++++++++++++++++++++++++
 *            ++++++++++++++++++++++++
 *             +++++++++++++++++++++++|
 *              ++++++++++++++++++++++|-
 *               +++++++++++++++++++++|--
 *                ++++++++++++++++++++|---
 *                 +++++++++++++++++++|----
 *                  ++++++++++++++++++|-----
 *                   +++++++++++++++++|------
 *                    ++++++++++++++++|-------
 *                     +++++++++++++++|--------
 * </pre>
 * 
 * With a synthesis frameshift of 1/8th of the frame length, implying that eight frames need to be overlapped to reconstruct the
 * signal, this becomes:
 * 
 * <pre>
 * -------|++++++++++++++++
 *    ------|+++++++++++++++++
 *       -----|++++++++++++++++++
 *          ----|+++++++++++++++++++
 *             ---|++++++++++++++++++++
 *                --|+++++++++++++++++++++
 *                   -|++++++++++++++++++++++    
 *                      |+++++++++++++++++++++++ *** first usable
 *                         ++++++++++++++++++++++++
 *                            ++++++++++++++++++++++++
 *                               ++++++++++++++++++++++++
 *                                  +++++++++++++++++++++++|
 *                                     ++++++++++++++++++++++|- 
 *                                        +++++++++++++++++++++|--
 *                                           ++++++++++++++++++++|---
 *                                              +++++++++++++++++++|---- 
 *                                                 ++++++++++++++++++|-----
 *                                                    +++++++++++++++++|------
 *                                                       ++++++++++++++++|------- *** last usable: first 3 of this
 * </pre>
 * 
 * It can be seen that seven times the input frameshift needs to be zero-padded before the signal, and discarded to reach proper
 * signal reconstruction.
 * 
 * Similarly, the last frame to be used is the one to which seven times the input shift has been zero-padded; only the first
 * output frameshift samples of it can be used.
 * 
 * 3. Assuming an input frameshift of 1/3rd of the frame length, and a signal length equal to (frame length+4*frameshift), we
 * cover the input data as follows: (+=valid data points, -=zero, |=valid data point at start/end of input)
 * 
 * <pre>
 *  --------|+++
 *      ----|+++++++
 *          |+++++++++++
 *              ++++++++++++
 *                  ++++++++++++
 *                      ++++++++++++
 *                          +++++++++++|
 *                              +++++++|----
 *                                  +++|--------
 * </pre>
 * 
 * With a synthesis frameshift of 1/4th of the frame length, implying that four frames need to be overlapped to reconstruct the
 * signal, this becomes:
 * 
 * <pre>
 *    --------|+++
 *       ----|+++++++
 *          |+++++++++++
 *             ++++++++++++ *** first usable
 *                ++++++++++++
 *                   ++++++++++++
 *                      +++++++++++|
 *                         +++++++|----
 *                            +++|-------- *** last usable: first 3 of this
 * </pre>
 * 
 * It can be seen that only two times the input frameshift needs to be zero-padded before the signal; nevertheless, the first
 * three frames need to be procesesed but discarded to reach proper signal reconstruction.
 * 
 * Similarly, the last frame to be used is the one to which two times the input shift has been zero-padded; only the first output
 * frameshift samples of it can be used.
 * 
 * 4. Assuming an input frameshift of 1/2rd of the frame length, and a signal length equal to (frame length+4*frameshift), we
 * cover the input data as follows: (+=valid data points, -=zero, |=valid data point at start/end of input)
 * 
 * <pre>
 *    ------|+++++
 *          |+++++++++++
 *                ++++++++++++
 *                      ++++++++++++
 *                            ++++++++++++
 *                                  +++++++++++|
 *                                        +++++|------
 * </pre>
 * 
 * With a synthesis frameshift of 1/4th of the frame length, implying that four frames need to be overlapped to reconstruct the
 * signal, this becomes:
 * 
 * <pre>
 *       ------|+++++
 *          |+++++++++++
 *             ++++++++++++
 *                ++++++++++++ *** first usable
 *                   ++++++++++++
 *                      +++++++++++|
 *                         +++++|------ *** last usable: first 3 of this
 * </pre>
 * 
 * It can be seen that only two times the input frameshift needs to be zero-padded before the signal; nevertheless, the first
 * three frames need to be procesesed but discarded to reach proper signal reconstruction.
 * 
 * Similarly, the last frame to be used is the one to which two times the input shift has been zero-padded; only the first output
 * frameshift samples of it can be used.
 * 
 * Generalising: May ro be the output overlap ratio, ro = output frameshift / framelength, and ri be the input overlap ratio, ri =
 * input frameshift / framelength, then n = 1/(1-ro) is the number of frames to be overlapped so that the signal is reconstructed.
 * The amount of zeroes to be padded before and after the signal is (n-1)*input frameshift, or in the case of speeding up,
 * (m-1)*input frameshift where m = 1/ri. (n-1) frames must be read and discarded before the actual data. If the signal length can
 * be described as l = framelength + n*frameshift, exactly output frameshift samples are to be used from the last frame. If the
 * signal is a bit shorter, i.e. l = framelength + n*frameshift - delta, then (output frameshift - delta) samples can be read from
 * the last frame.
 * 
 * @author Marc Schr&ouml;der
 */
public class FrameOverlapAddSource extends BlockwiseDoubleDataSource {
	public static final int DEFAULT_WINDOWTYPE = Window.HANNING;
	protected FrameProvider frameProvider;
	protected Window outputWindow;
	protected double[] memory;
	protected InlineDataProcessor processor;

	/**
	 * Default constructor for subclasses who want to call initialise() themselves.
	 */
	protected FrameOverlapAddSource() {
		super(null, 0); // need to set blockSize right later
	}

	public FrameOverlapAddSource(DoubleDataSource inputSource, int frameLength, int samplingRate, InlineDataProcessor processor) {
		this(inputSource, DEFAULT_WINDOWTYPE, false, frameLength, samplingRate, processor);
	}

	public FrameOverlapAddSource(DoubleDataSource inputSource, int windowType, boolean applySynthesisWindow, int frameLength,
			int samplingRate, InlineDataProcessor processor) {
		super(null, 0); // need to set blockSize right later
		initialise(inputSource, windowType, applySynthesisWindow, frameLength, samplingRate, processor);
	}

	/**
	 * To be called by constructor in order to set up this frame overlap add source.
	 * 
	 * @param inputSource
	 *            input source
	 * @param windowType
	 *            window type
	 * @param applySynthesisWindow
	 *            apply synthesis window
	 * @param frameLength
	 *            frame length
	 * @param samplingRate
	 *            sampling rate
	 * @param processor
	 *            processor
	 */
	protected void initialise(DoubleDataSource inputSource, int windowType, boolean applySynthesisWindow, int frameLength,
			int samplingRate, InlineDataProcessor processor) {
		double overlapFraction;
		double prescale = 1;
		switch (windowType) {
		case Window.HANNING:
			overlapFraction = 0.75;
			// Prescale to allow for perfect restitution for rate factor 1:
			// If we overlap-add simple hann windows by 3/4, we increase the amplitude by 2;
			// if we overlap-add squared hann windows by 3/4, we increase the amplitude by 1.5.
			// for an overlap ratio of 7/8, these values are twice as large.
			double onceOrTwice = 0.25 / (1 - overlapFraction); // == 2 for overlap 7/8, 1 for overlap 3/4
			prescale = applySynthesisWindow ? Math.sqrt(2. / 3 / onceOrTwice) : 0.5 / onceOrTwice;
			break;
		case Window.BLACKMAN:
		case Window.HAMMING:
			overlapFraction = 0.875;
			break;
		default:
			throw new IllegalArgumentException("Window type not supported");
		}
		// output frameshift is constrained by window type and frame length:
		this.blockSize = (int) (frameLength * (1 - overlapFraction));
		int inputFrameshift = getInputFrameshift(blockSize);
		// System.err.println("Blocksize: "+blockSize+", inputFrameshift: "+inputFrameshift);
		Window window = Window.get(windowType, frameLength + 1, prescale);

		if (applySynthesisWindow)
			this.outputWindow = window;
		else
			this.outputWindow = null;
		this.memory = new double[frameLength];
		// This is used when the last input frame has already been read,
		// to do the last frame output properly:
		this.processor = processor;
		// We need to feed through (and discard) 3 (if overlapFraction == 3/4)
		// blocks of zeroes, so that the first three blocks are properly rebuilt.
		int nBlocks = (int) (1 / (1 - overlapFraction)) - 1;
		// If we insist on 4-fold overlap for speeding up, we need to
		// feed in less zeroes: (m-1)*inputFrameshift, where m = frameLength/inputFrameshift.
		int m = frameLength / inputFrameshift;
		int nZeroes = nBlocks * inputFrameshift < frameLength ? nBlocks : (m - 1);
		DoubleDataSource padding1 = new BufferedDoubleDataSource(new double[nZeroes * inputFrameshift]);
		DoubleDataSource padding2 = new BufferedDoubleDataSource(new double[nZeroes * inputFrameshift]);
		DoubleDataSource paddedSource = new SequenceDoubleDataSource(new DoubleDataSource[] { padding1, inputSource, padding2 });
		this.frameProvider = new FrameProvider(paddedSource, window, frameLength, inputFrameshift, samplingRate, true);
		double[] dummy = new double[blockSize];
		for (int i = 0; i < nBlocks; i++) {
			// System.err.println("Discarding "+blockSize+" samples:");
			getData(dummy, 0, blockSize);
		}
		this.frameProvider.resetInternalTimer();
	}

	/**
	 * Get the next frame of input data. This method is called by prepareBlock() when preparing the output data to be read. This
	 * implementation simply reads the data from the frameProvider.
	 * 
	 * @return the next frame of frameProvider
	 */
	protected double[] getNextFrame() {
		return frameProvider.getNextFrame();
	}

	/**
	 * Prepare one block of data for output. This method is called from the superclass before readBlock() is called.
	 */
	protected void prepareBlock() {
		double[] frame = getNextFrame();
		if (frame == null)
			return;
		int frameLength = frameProvider.getFrameLengthSamples();
		if (processor != null)
			processor.applyInline(frame, 0, frameLength);
		if (outputWindow != null)
			outputWindow.applyInline(frame, 0, frameLength);
		// Extend memory if necessary:
		if (memory.length < frameLength) {
			double[] oldMemory = memory;
			memory = new double[frameLength];
			System.arraycopy(oldMemory, 0, memory, 0, oldMemory.length);
		}
		// The overlap-add part:
		for (int i = 0; i < frameLength; i++) {
			memory[i] += frame[i];
		}
	}

	protected int getBlockSize() {
		return blockSize;
	}

	/**
	 * Provide a block of data. This method is called from the superclass when data is requested. Note that prepareBlock() will be
	 * called before this.
	 */
	protected int readBlock(double[] target, int targetPos) {
		// Now, the first blockSize samples can be output:
		int blockSize = getBlockSize();
		int validSamplesInFrame = frameProvider.validSamplesInFrame();
		// System.err.println("OLA: valid samples in current frame: "+validSamplesInFrame);
		int frameLength = frameProvider.getFrameLengthSamples();
		if (validSamplesInFrame < frameLength) {
			assert !frameProvider.hasMoreData();
			// assert frameLength-validSamplesInFrame < frameProvider.getFrameShiftSamples();
			// But in the case of speeding up, frameLength-validSamplesInFrame
			// can still be > blockSize; in that case, copy only blockSize samples,
			// and discard the rest.
			int nCopied;
			if (blockSize < (frameLength - validSamplesInFrame)) {
				nCopied = blockSize;
			} else {
				nCopied = blockSize - (frameLength - validSamplesInFrame);
			}
			assert nCopied > 0; // otherwise someone should notice we should not be called
			// System.err.println("OLA: Outputting last frame: "+nCopied+" samples ("+blockSize+","+frameLength+","+validSamplesInFrame+")");
			System.arraycopy(memory, 0, target, targetPos, nCopied);
			return nCopied;
		} else {
			// System.err.println("OLA: Outputting normal frame: "+blockSize+" samples (keeping "+(validSamplesInFrame-blockSize)+")");
			System.arraycopy(memory, 0, target, targetPos, blockSize);
			// Shift the data left in memory:
			System.arraycopy(memory, blockSize, memory, 0, memory.length - blockSize);
			Arrays.fill(memory, memory.length - blockSize, memory.length, 0);
			return blockSize;
		}
	}

	protected int getInputFrameshift(int outputFrameshift) {
		return outputFrameshift; // default: inputFrameshift == outputFrameshift
	}

	public boolean hasMoreData() {
		return frameProvider.hasMoreData();
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
			int samplingRate = (int) inputAudio.getFormat().getSampleRate();
			double[] signal = new AudioDoubleDataSource(inputAudio).getAllData();
			FunctionGraph signalGraph = new SignalGraph(signal, samplingRate);
			signalGraph.showInJFrame("signal", true, true);
			FrameOverlapAddSource ola = new FrameOverlapAddSource(new BufferedDoubleDataSource(signal), 2048, samplingRate, null);
			double[] result = ola.getAllData();
			FunctionGraph resultGraph = new SignalGraph(result, samplingRate);
			resultGraph.showInJFrame("result", true, true);
			System.err.println("Signal has length " + signal.length + ", result " + result.length);
			double err = MathUtils.sumSquaredError(signal, result);
			System.err.println("Sum squared error: " + err);

			double[] difference = MathUtils.subtract(signal, result);
			FunctionGraph diffGraph = new SignalGraph(difference, samplingRate);
			diffGraph.showInJFrame("difference", true, true);

			DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(ola), inputAudio.getFormat());
			String outFileName = args[i].substring(0, args[i].length() - 4) + "_copy.wav";
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		}
	}
}
