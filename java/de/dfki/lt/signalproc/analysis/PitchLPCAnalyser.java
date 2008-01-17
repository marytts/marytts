/**
 * Copyright 2004-2006 DFKI GmbH.
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

package de.dfki.lt.signalproc.analysis;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.signalproc.FFT;
import de.dfki.lt.signalproc.analysis.FrameBasedAnalyser.FrameAnalysisResult;
import de.dfki.lt.signalproc.analysis.LPCAnalyser.LPCoeffs;
import de.dfki.lt.signalproc.display.FunctionGraph;
import de.dfki.lt.signalproc.display.SignalGraph;
import de.dfki.lt.signalproc.filter.FIRFilter;
import de.dfki.lt.signalproc.window.Window;
import de.dfki.lt.util.ArrayUtils;
import de.dfki.lt.signalproc.process.PitchFrameProvider;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.Defaults;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.ESTTextfileDoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * A Pitch-synchronous LPC analyser.
 * @author Marc Schr&ouml;der
 *
 */
public class PitchLPCAnalyser extends PitchFrameAnalyser
{
    public static int lpOrder = 0;
    
    /**
     * Initialise a PitchLPCAnalyser.
     * @param signal the signal source to read from
     * @param pitchmarks the source of the pitchmarks, in seconds from the start of signal
     * @param windowType type of analysis window to use, @see{de.dfki.signalproc.window.Window#getAvailableTypes()}
     * @param samplingRate the number of samples in one second.
     */
    public PitchLPCAnalyser(DoubleDataSource signal, DoubleDataSource pitchmarks, int windowType, int samplingRate)
    {
        super(signal, pitchmarks, windowType, samplingRate);
    }

    /**
     * Create a new PitchLPCAnalyser with a configurable number of pitch periods per frame
     * and pitch periods to shift by.
     * @param signal audio signal
     * @param pitchmarks an array of pitchmarks; each pitch mark is in seconds from signal start
     * @param windowType type of analysis window to use, @see{de.dfki.signalproc.window.Window#getAvailableTypes()}
     * @param samplingRate number of samples per second in signal
     * @param framePeriods number of periods that each frame should contain
     * @param shiftPeriods number of periods that frames should be shifted by
     */
    public PitchLPCAnalyser(DoubleDataSource signal, DoubleDataSource pitchmarks, int windowType, int samplingRate, int framePeriods, int shiftPeriods)
    {
        super(signal, pitchmarks, windowType, samplingRate, framePeriods, shiftPeriods);
    }
    
    /**
     * Apply this PitchFrameAnalyser to the given data.
     * @param frame the data to analyse, which must be of the length prescribed by this
     * FrameBasedAnalyser, i.e. by @see{#getFrameLengthSamples()}.
     * @return an LPCoeffs object representing the lpc coefficients and gain factor of the frame.
     * @throws IllegalArgumentException if frame does not have the prescribed length 
     */
    public Object analyse(double[] frame)
    {
        // for assertion only:
        int expectedFrameLength = 0;
        for (int i=0; i<periodLengths.length; i++) {
            expectedFrameLength += periodLengths[i];
        }
        if (frame.length != expectedFrameLength)
            System.err.println("Expected frame of length " + expectedFrameLength
                    + "(" + periodLengths.length + " periods)"
                    + ", got " + frame.length);

        return LPCAnalyser.calcLPC(frame, lpOrder);
    }

    public static void main(String[] args) throws Exception
    {
        File audioFile = new File(args[0]);
        File pitchmarkFile = new File(args[1]);
        AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile);
        int samplingRate = (int) ais.getFormat().getSampleRate();
        DoubleDataSource signal = new AudioDoubleDataSource(ais);
        DoubleDataSource pitchmarks = new ESTTextfileDoubleDataSource(new FileReader(pitchmarkFile));

        int windowType = Defaults.getWindowType();
        int fftSize = Defaults.getFFTSize();
        int p = Integer.getInteger("signalproc.lpcorder", 24).intValue();
        
        PitchLPCAnalyser pla = new PitchLPCAnalyser(signal, pitchmarks, windowType, samplingRate, 2, 1);
        FrameAnalysisResult[] far = pla.analyseAllFrames();
        for (int i=0; i<far.length; i++) {
            LPCoeffs coeffs = (LPCoeffs) far[i].get();
            System.out.print(far[i].getStartTime()+": gain "+coeffs.getGain()
                    +", coffs: ");
            for (int j=0; j<coeffs.getOrder(); j++) {
                System.out.print(coeffs.getA(j)+"  ");
            }
            System.out.println();
            
        }
    }
    
}
