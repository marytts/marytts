package de.dfki.lt.signalproc.process;

import java.util.Arrays;

import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.SequenceDoubleDataSource;
import de.dfki.lt.signalproc.window.DynamicTwoHalvesWindow;
import de.dfki.lt.signalproc.window.Window;

/**
 * A class to merge two audio signals, using pitch-synchronous frames.
 * @author marc
 *
 */
public class FramewiseMerger extends FrameOverlapAddSource
{
    protected DoubleDataSource labelTimes;
    protected DoubleDataSource otherLabelTimes;
    protected FrameProvider otherFrameProvider;
    protected double prevLabel, currentLabel, prevOtherLabel, currentOtherLabel;
    protected double localTimeStretchFactor = 1;

    /**
     * Create a new merger, creating audio by pitch-synchronous merging of audio frames
     * from a source (aka the "signal") and a target (aka the "other"),
     * linearly mapping the corresponding times between the 
     * two sources.
     * @param inputSource the audio data for the signal
     * @param pitchmarks the pitchmarks for the signal
     * @param samplingRate the sampling rate for the signal
     * @param labelTimes optionally, the label times for the signal, needed for 
     * time alignment between the signal and the other
     * @param otherSource the audio data for the other
     * @param otherPitchmarks the pitchmarks for the other
     * @param otherSamplingRate the sampling rate for the other
     * @param otherLabelTimes optionally, the label times for the other; if both
     * are present, the time interval between the i-th and the (i+1)-th label time
     * is linearly stretched/squeezed in order to find the mapping frame for interpolation
     * @param merger the signal processing method used for merging the properties of the "other"
     * into the corresponding frame in the "signal".
     */
    public FramewiseMerger(DoubleDataSource inputSource, DoubleDataSource pitchmarks,
            int samplingRate, DoubleDataSource labelTimes,
            DoubleDataSource otherSource, DoubleDataSource otherPitchmarks, 
            int otherSamplingRate, DoubleDataSource otherLabelTimes,
            InlineFrameMerger merger)
    {
        // Set up label times for time stretching:
        this.labelTimes = labelTimes;
        this.otherLabelTimes = otherLabelTimes;
        
        InlineDataProcessor analysisWindow = new DynamicTwoHalvesWindow(Window.HANN);
        // Overlap-add a properly windowed first period by hand:
        // Read out the first pitchmark:
        double firstPitchmark = pitchmarks.getData(1)[0];
        assert firstPitchmark > 0;
        // If the first pitchmark is too close (closer than 1ms) to origin, skip it:
        if (firstPitchmark < 0.001*samplingRate) 
            firstPitchmark = pitchmarks.getData(1)[0];
        pitchmarks = new SequenceDoubleDataSource(new DoubleDataSource[] {new BufferedDoubleDataSource(new double[]{firstPitchmark}), pitchmarks});
        int firstPeriodLength = (int) (firstPitchmark*samplingRate);
        double[] firstPeriod = new double[firstPeriodLength];
        inputSource.getData(firstPeriod, 0, firstPeriodLength);
        inputSource = new SequenceDoubleDataSource(new DoubleDataSource[] {new BufferedDoubleDataSource(firstPeriod), inputSource});
        this.memory = new double[2*firstPeriodLength];
        System.arraycopy(firstPeriod, 0, memory, firstPeriodLength, firstPeriodLength);
        analysisWindow.applyInline(memory, 0, memory.length);
        if (merger != null) {
            // Read out the first pitchmark:
            double firstOtherPitchmark = otherPitchmarks.getData(1)[0];
            assert firstOtherPitchmark > 0;
            // If the first other pitchmark is too close (closer than 1ms) to origin, skip it:
            if (firstOtherPitchmark < 0.001*otherSamplingRate) 
                firstPitchmark = otherPitchmarks.getData(1)[0];
            otherPitchmarks = new SequenceDoubleDataSource(new DoubleDataSource[] {new BufferedDoubleDataSource(new double[]{firstOtherPitchmark}), otherPitchmarks});
            int firstOtherPeriodLength = (int) (firstOtherPitchmark*otherSamplingRate);
            double[] firstOtherPeriod = new double[firstOtherPeriodLength];
            otherSource.getData(firstOtherPeriod, 0, firstOtherPeriodLength);
            otherSource = new SequenceDoubleDataSource(new DoubleDataSource[] {new BufferedDoubleDataSource(firstOtherPeriod), otherSource});
            double[] frameToMerge = new double[2*firstOtherPeriodLength];
            System.arraycopy(firstOtherPeriod, 0, frameToMerge, firstOtherPeriodLength, firstOtherPeriodLength);
            merger.setFrameToMerge(frameToMerge);
            merger.applyInline(memory, 0, memory.length);
        }
        // Shift the data left in memory:
        System.arraycopy(memory, firstPeriodLength, memory, 0, firstPeriodLength);
        Arrays.fill(memory, firstPeriodLength, memory.length, 0);
        // And initialise frame providers for normal operation:
        this.frameProvider = new PitchFrameProvider(inputSource, pitchmarks, analysisWindow, samplingRate, 2, 1);
        this.otherFrameProvider = new PitchFrameProvider(otherSource, otherPitchmarks, 
                analysisWindow, otherSamplingRate, 2, 1);
        this.processor = merger;
    }
    
    /**
     * Create a new merger, creating audio by merging of audio frames at a fixed frame rate,
     * from a source (aka the "signal") and a target (aka the "other"),
     * linearly mapping the corresponding times between the 
     * two sources.
     * @param inputSource the audio data for the signal
     * @param frameLength length of the fixed-length frames
     * @param samplingRate the sampling rate for the signal
     * @param labelTimes optionally, the label times for the signal, needed for 
     * time alignment between the signal and the other
     * @param otherSource the audio data for the other
     * @param otherSamplingRate the sampling rate for the other
     * @param otherLabelTimes optionally, the label times for the other; if both
     * are present, the time interval between the i-th and the (i+1)-th label time
     * is linearly stretched/squeezed in order to find the mapping frame for interpolation
     * @param merger the signal processing method used for merging the properties of the "other"
     * into the corresponding frame in the "signal".
     */
    public FramewiseMerger(DoubleDataSource inputSource, int frameLength, int samplingRate, DoubleDataSource labelTimes,
            DoubleDataSource otherSource, int otherSamplingRate, DoubleDataSource otherLabelTimes,
            InlineFrameMerger merger)
    {
        DoubleDataSource paddingOther1 = new BufferedDoubleDataSource(new double[3*frameLength/4]);
        DoubleDataSource paddedOtherSource = new SequenceDoubleDataSource(new DoubleDataSource[]{paddingOther1, otherSource});
        this.otherFrameProvider = new FrameProvider(paddedOtherSource, Window.get(Window.HANN, frameLength, 0.5), frameLength, frameLength/4, samplingRate, true);
        this.blockSize = frameLength/4;
        int inputFrameshift = blockSize;
        Window window = Window.get(Window.HANN, frameLength+1);
        this.outputWindow = null;
        this.memory = new double[frameLength];
        // This is used when the last input frame has already been read,
        // to do the last frame output properly:
        this.processor = merger;
        // We need to feed through (and discard) 3 (if overlapFraction == 3/4)
        // blocks of zeroes, so that the first three blocks are properly rebuilt.
        DoubleDataSource padding1 = new BufferedDoubleDataSource(new double[3*inputFrameshift]);
        DoubleDataSource padding2 = new BufferedDoubleDataSource(new double[3*inputFrameshift]);
        DoubleDataSource paddedSource = new SequenceDoubleDataSource(new DoubleDataSource[]{padding1, inputSource, padding2});
        this.frameProvider = new FrameProvider(paddedSource, window, frameLength, inputFrameshift, samplingRate, true);
        double[] dummy = new double[blockSize];
        for (int i=0; i<3; i++) {
            //System.err.println("Discarding "+blockSize+" samples:");
            getData(dummy, 0, blockSize); // this calls getNextFrame() indirectly
        }
        this.frameProvider.resetInternalTimer();
        this.otherFrameProvider.resetInternalTimer();

        // Only now, after initialising the overlap-add, set up the label times.
        // Set up label times for time stretching:
        this.labelTimes = labelTimes;
        this.otherLabelTimes = otherLabelTimes;
    }


    /**
     * Get the next frame of input data. This method is called by prepareBlock()
     * when preparing the output data to be read. This implementation
     * reads the data from the frameProvider. In addition, the appropriate "other"
     * frame is identified; this is the frame closest in starting time to the 
     * starting time of the next "signal" frame (i.e., the starting time of the return
     * value of this method), correcting for the label times. Concretely, if 
     * the start time t_s of the next signal frame is between labelTimes[i] and
     * labelTimes[i+1], then the optimal other frame starting time to would be:
     *      t_o = otherLabelTimes[i] + (t_s - labelTimes[i])/(labelTimes[i+1]-labelTimes[i]) * (otherLabelTimes[i+1] - otherLabelTimes[i])
     * The other frame whose starting time is closest to to will be prepared as
     * for merging.
     * @return the next signal frame.
     */
    protected double[] getNextFrame()
    {
        double[] nextSignalFrame = frameProvider.getNextFrame();
        double frameStart = frameProvider.getFrameStartTime();
        //System.out.println("Getting signal frame, start time = "+frameStart);
        while (frameStart >= currentLabel) {
            // move to next label
            if (labelTimes == null || otherLabelTimes == null
                    || !labelTimes.hasMoreData()  || !otherLabelTimes.hasMoreData()) {
                currentLabel = Double.POSITIVE_INFINITY;
                localTimeStretchFactor = 1;
            } else {
                prevLabel = currentLabel;
                currentLabel = labelTimes.getData(1)[0];
                assert currentLabel > prevLabel;
                prevOtherLabel = currentOtherLabel;
                currentOtherLabel = otherLabelTimes.getData(1)[0];
                assert currentOtherLabel > prevOtherLabel;
                localTimeStretchFactor = (currentOtherLabel - prevOtherLabel) / (currentLabel - prevLabel);
            }
        }
        assert prevLabel <= frameStart && frameStart < currentLabel;
        //System.out.println("Local time stretch = "+localTimeStretchFactor);
        double targetOtherStart = prevOtherLabel + (frameStart - prevLabel) * localTimeStretchFactor;
        //System.out.println("Target other start = "+targetOtherStart);
        double otherStart = otherFrameProvider.getFrameStartTime();
        //System.out.println("Current other frame starts at "+otherStart);
        if (otherStart < 0) { // no other frame yet
            double[] otherFrame = otherFrameProvider.getNextFrame();
            ((InlineFrameMerger)processor).setFrameToMerge(otherFrame);
            otherStart = otherFrameProvider.getFrameStartTime();
            //System.out.println("Getting first other frame -- starts at "+otherStart);
        }
        assert otherStart >= 0;
        // Now skip other frames until the current otherStart is closer to targetOtherStart
        // then the next one would be.
        double expectedNextOtherStart = otherStart + otherFrameProvider.getFrameShiftTime();
        while (Math.abs(expectedNextOtherStart-targetOtherStart)<Math.abs(otherStart-targetOtherStart)
                && otherFrameProvider.hasMoreData()) {
            double[] otherFrame = otherFrameProvider.getNextFrame();
            ((InlineFrameMerger)processor).setFrameToMerge(otherFrame);
            otherStart = otherFrameProvider.getFrameStartTime();
            //System.out.println("Skipping frame -- new one starts at "+otherStart);
            assert Math.abs(otherStart - expectedNextOtherStart) < 1e-10 : "Other frame starts at "+otherStart+" -- expected was "+expectedNextOtherStart;
            expectedNextOtherStart = otherStart + otherFrameProvider.getFrameShiftTime();
        }
        return nextSignalFrame;
    }

    /**
     * Output blocksize -- here, this is the same as the input frame shift.
     */
    protected int getBlockSize()
    {
        return frameProvider.getFrameShiftSamples();
    }


    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
