/**
 * Copyright 2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.signalproc.process;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.ESTTextfileDoubleDataSource;
import de.dfki.lt.signalproc.window.DynamicTwoHalvesWindow;

/**
 * Cut frames out of a given signal, and provide them one by one,
 * optionally applying a processor to the frame.
 * This implementation provides non-overlapping frames of varying
 * length, delimited by a series of markers (e.g., pitchmarks).
 * @author Marc Schr&ouml;der
 *
 */
public class PitchFrameProvider extends FrameProvider
{
    protected DoubleDataSource pitchmarks;
    protected int[] periodLengths;
    protected int shiftPeriods;
    protected int periodsInMemory;
    protected long currPitchmark;
    protected DynamicTwoHalvesWindow twoHalvesWindow;
    protected double[] cutFrame;

    /**
     * Create a new PitchFrameProvider providing one period at a time.
     * @param signal audio signal
     * @param pitchmarks an array of pitchmarks; each pitch mark is in seconds from signal start
     * @param processor an optional processor to apply to each input frame (e.g., a DynamicWindow)
     * @param samplingRate number of samples per second in signal
     */
    public PitchFrameProvider(DoubleDataSource signal, DoubleDataSource pitchmarks,
            InlineDataProcessor processor, int samplingRate)
    {
        this(signal, pitchmarks, processor, samplingRate, 1, 1);
    }

    /**
     * Create a new PitchFrameProvider with a configurable number of pitch periods per frame
     * and pitch periods to shift by.
     * @param signal audio signal
     * @param pitchmarks an array of pitchmarks; each pitch mark is in seconds from signal start
     * @param processor an optional processor to apply to each input frame (e.g., a DynamicWindow)
     * @param samplingRate number of samples per second in signal
     * @param framePeriods number of periods that each frame should contain
     * @param shiftPeriods number of periods that frames should be shifted by
     */
    public PitchFrameProvider(DoubleDataSource signal, DoubleDataSource pitchmarks,
            InlineDataProcessor processor, int samplingRate, int framePeriods, int shiftPeriods)
    {
        super(signal, null, 0, 0, samplingRate, true);
        this.pitchmarks = pitchmarks;
        this.periodLengths = new int[framePeriods];
        this.shiftPeriods = shiftPeriods;
        this.periodsInMemory = 0;
        this.currPitchmark = 0;
        // Need to treat an asymmetric window differently, because we need to know
        // the pitchmark position in the "middle" of the window.
        if (processor instanceof DynamicTwoHalvesWindow)
            twoHalvesWindow = (DynamicTwoHalvesWindow) processor;
        else
            this.processor = processor;
    }


    /**
     * Read data from input signal into current frame.
     * This implementation will attempt to read up to the next pitch mark, 
     * filling the frame from the position given in nPrefilled onwards and
     * extending the size of frame if necessary.
     * <br />
     * Note that this implementation will perform zero-padding of periods
     * at the beginning and end of the signal: when the first shiftPeriods periods
     * are read, (framePeriods-shiftPeriods) empty periods (zero signal), equal in length to the
     * first period, will be added to the left; after the end of the signal,
     * (framePeriods-shiftPeriods) empty periods (zero signal), equal in length to the
     * last period, will be added to the right.
     * 
     * @param frame the frame to read into
     * @param nPrefilled number of valid values at the beginning of frame. These should not be lost or overwritten.
     * @return the number of new values read into frame at position nPrefilled. 0 signals that no further data
     * can be read.
     */
    protected int getData(int nPrefilled)
    {
        // When we get here, we assume that either there is more input data or there is some still in memory.
        assert hasMoreData();
        // write into frame at position nPrefilled.
        int nPeriodsToGet;
        if (nPrefilled == 0) { // first time, read full frame
            nPeriodsToGet = periodLengths.length;
            periodsInMemory = 0;
        } else { // next times, just the new ones 
            nPeriodsToGet = shiftPeriods;
            // remember period lengths:
            System.arraycopy(periodLengths, shiftPeriods, periodLengths, 0, periodLengths.length-shiftPeriods);
            // For post-padding empty periods: keep track of how many real periods we have;
            // this is in addition to nPrefilled, which counts data for real and padded periods.
            periodsInMemory -= shiftPeriods;
        }
        int pos = nPrefilled;
        for (int i=periodLengths.length-nPeriodsToGet; i<periodLengths.length; i++) {
            // We read up to the end of signal and pitchmarks, whichever is shorter:
            if (signal.hasMoreData() && pitchmarks.hasMoreData()) {
                long prevPitchmark = currPitchmark;
                double pitchmarkInSeconds = pitchmarks.getData(1)[0];
                currPitchmark = (long) Math.round(pitchmarkInSeconds * samplingRate);
                periodLengths[i] = (int) (currPitchmark-prevPitchmark);
                // Plausibility check: Do not allow periods longer than 30 ms (33 Hz) or shorter than 1 ms (1000 Hz)!
                assert periodLengths[i] < samplingRate/33: "Found pitch period longer than 30 ms (less than 33 Hz)";
                assert periodLengths[i] > samplingRate/1000 : "Found pitch period shorter than 1 ms (more than 1000 Hz)";
                if (pos+periodLengths[i] > frame.length) {
                    // need to increase frame size
                    double[] oldFrame = frame;
                    frame = new double[pos+periodLengths[i]];
                    if (pos>0)
                        System.arraycopy(oldFrame, 0, frame, 0, pos);
                }
                int read = signal.getData(frame, pos, periodLengths[i]);
                assert read == periodLengths[i] : "expected "+periodLengths[i]+", got "+read;
                pos += read;
                periodsInMemory++;
            } else { // no more input data
                // Stop condition: only output if there is at least one valid period in frame
                if (periodsInMemory <= 0) return 0;
                // fill up periods as long as the last periods, but with zero data
                assert i>0;
                periodLengths[i] = periodLengths[i-1];
                if (pos+periodLengths[i] > frame.length) {
                    // need to increase frame size
                    double[] oldFrame = frame;
                    frame = new double[pos+periodLengths[i]];
                    if (pos>0)
                        System.arraycopy(oldFrame, 0, frame, 0, pos);
                }
                Arrays.fill(frame, pos, pos+periodLengths[i], 0);
                pos += periodLengths[i];
            }
        }
        frameShift = 0;
        for (int i=0; i<shiftPeriods; i++) frameShift += periodLengths[i];
        frameLength = 0;
        for (int i=0; i<periodLengths.length; i++) frameLength += periodLengths[i];
        return pos-nPrefilled;
    }
    
    /**
     * Provide the next frame of data.
     * @return the next frame on success, null on failure.
     */
    public double[] getNextFrame()
    {
        double[] uncutFrame = super.getNextFrame();
        if (uncutFrame == null) {
            cutFrame = null;
            return null;
        }
        int frameLength = super.getFrameLengthSamples();
        cutFrame = new double[frameLength];
        System.arraycopy(uncutFrame, 0, cutFrame, 0, frameLength);
        if (twoHalvesWindow != null) {
            // using two half windows only makes sense if we have an even number of
            // pitch periods in frame
            assert periodLengths.length % 2 == 0 : "Using two half windows makes sense only for an even number of periods per frame";
            int middle = 0;
            for (int i=0; i<periodLengths.length/2; i++) {
                middle += periodLengths[i];
            }
            assert middle < frameLength : "Middle "+middle+" larger than framelength "+frameLength+"!";
            twoHalvesWindow.applyInlineLeftHalf(cutFrame, 0, middle);
            twoHalvesWindow.applyInlineRightHalf(cutFrame, middle, frameLength-middle);
        }
        return cutFrame;
    }
    
    public double[] getCurrentFrame()
    {
        return cutFrame;
    }
    
    /**
     * The number of periods provided in one frame.
     * @return
     */
    public int getFramePeriods()
    {
        return periodLengths.length;
    }

    /**
     * The number of periods by which the analysis window is shifted.
     * @return
     */
    public int getShiftPeriods()
    {
        return shiftPeriods;
    }

  
    
    /**
     * Whether or not this frameprovider can provide another frame.
     */
    public boolean hasMoreData()
    {
        return signal.hasMoreData() && pitchmarks.hasMoreData() || periodsInMemory - shiftPeriods > 0;
    }


    
    /**
     * Test this pitch frame provider, by printing information about the pitch frames of an audio file.
     * @param args two args are expected: the name of an audio file, and the name of the corresponding pitch mark file. 
     */
    public static void main(String[] args) throws Exception
    {
        File audioFile = new File(args[0]);
        File pitchmarkFile = new File(args[1]);
        AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile);
        int samplingRate = (int) ais.getFormat().getSampleRate();
        DoubleDataSource signal = new AudioDoubleDataSource(ais);
        DoubleDataSource pitchmarks = new ESTTextfileDoubleDataSource(new FileReader(pitchmarkFile));
        PitchFrameProvider pfp = new PitchFrameProvider(signal, pitchmarks, null, samplingRate);
        double[] frame = null;
        int n = 0;
        int avgF0 = 0;
        while ((frame = pfp.getNextFrame()) != null) {
            int periodLength = pfp.validSamplesInFrame();
            if (periodLength > 0) {
                int f0 = samplingRate / periodLength;
                double frameStartTime = pfp.getFrameStartTime();
                double frameEndTime = frameStartTime + pfp.getFrameLengthTime();
                avgF0 += f0; n++;
                System.err.println("Frame "+frameStartTime+" - "+frameEndTime+" s: "+periodLength+" samples, "+f0+" Hz");
            } else {
                System.err.println("Read empty frame");
            }
        }
        avgF0 /= n;
        System.err.println("Average F0: "+avgF0+" Hz");

    }

}
