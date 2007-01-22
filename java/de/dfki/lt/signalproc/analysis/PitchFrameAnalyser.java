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

import java.util.ArrayList;
import java.util.List;

import de.dfki.lt.signalproc.analysis.FrameBasedAnalyser.FrameAnalysisResult;
import de.dfki.lt.signalproc.process.FrameProvider;
import de.dfki.lt.signalproc.process.InlineDataProcessor;
import de.dfki.lt.signalproc.process.PitchFrameProvider;
import de.dfki.lt.signalproc.window.DynamicWindow;
import de.dfki.lt.signalproc.window.Window;
import de.dfki.lt.signalproc.util.DoubleDataSource;

/**
 * @author Marc Schr&ouml;der
 * The base class for all frame-based signal analysis algorithms.
 */
public abstract class PitchFrameAnalyser extends PitchFrameProvider
{
    protected DynamicWindow analysisWindow;
    
    /**
     * Array containing the analysis results, filled by analyseAllFrames().
     * Can be used for future reference to the results.
     */
    protected FrameAnalysisResult[] analysisResults;
    
    /**
     * Initialise a PitchFrameAnalyser.
     * @param signal the signal source to read from
     * @param pitchmarks the source of the pitchmarks, in seconds from the start of signal
     * @param windowType type of analysis window to use, @see{de.dfki.signalproc.window.Window#getAvailableTypes()}
     * @param samplingRate the number of samples in one second.
     */
    public PitchFrameAnalyser(DoubleDataSource signal, DoubleDataSource pitchmarks, int windowType, int samplingRate)
    {
        this(signal, pitchmarks, windowType, samplingRate, 1, 1);
    }
    
    /**
     * Create a new PitchFrameAnalyser with a configurable number of pitch periods per frame
     * and pitch periods to shift by.
     * @param signal audio signal
     * @param pitchmarks an array of pitchmarks; each pitch mark is in seconds from signal start
     * @param windowType type of analysis window to use, @see{de.dfki.signalproc.window.Window#getAvailableTypes()}
     * @param samplingRate number of samples per second in signal
     * @param framePeriods number of periods that each frame should contain
     * @param shiftPeriods number of periods that frames should be shifted by
     */
    public PitchFrameAnalyser(DoubleDataSource signal, DoubleDataSource pitchmarks,
            int windowType,
            int samplingRate, int framePeriods, int shiftPeriods)
    {
        super(signal, pitchmarks, new DynamicWindow(windowType), samplingRate, framePeriods, shiftPeriods);
    }

    /**
     * The public method to call in order to trigger the analysis of the next frame.
     * @return the analysis result, or null if no part of the signal is left to analyse.
     */
    public FrameAnalysisResult analyseNextFrame()
    {
        double[] frame = getNextFrame();
        if (frame == null) return null;
        analysisWindow.applyInline(frame, 0, frame.length);
        Object analysisResult = analyse(frame);
        return constructAnalysisResult(analysisResult);
    }
    
    /**
     * Analyse the entire signal as frames. Stop as soon as the first frame reaches
     * or passes the end of the signal. Repeated access to this method returns
     * a stored version of the results.
     * @return an array containing all frame analysis results.
     */
    public FrameAnalysisResult[] analyseAllFrames()
    {
        if (analysisResults == null) {
            ArrayList results = new ArrayList();
            FrameAnalysisResult oneResult;
            while ((oneResult = analyseNextFrame()) != null) {
                results.add(oneResult);
            }
            analysisResults = (FrameAnalysisResult[]) results.toArray(new FrameAnalysisResult[0]);
        }        
        return analysisResults;
    }

    /**
     * Analyse the currently available input signal as frames. This method
     * is intended for live signals such as microphone data.
     * Stop when the amount of data available from the input is
     * less than one frame length.
     * Repeated access to this method will read new data if new data has
     * become available in the meantime.
     * @return an array containing the frame analysis results for the data
     * that is currently available, or an empty array if no new data is available.
     */
    public FrameAnalysisResult[] analyseAvailableFrames()
    {
        List results = new ArrayList();
        FrameAnalysisResult oneResult;
        while (signal.available() >= frameLength) {
            oneResult = analyseNextFrame();
            assert oneResult != null;
            results.add(oneResult);
        }
        return (FrameAnalysisResult[]) results.toArray(new FrameAnalysisResult[0]);
    }
    
    /**
     * Apply this PitchFrameAnalyser to the given data.
     * @param frame the data to analyse, which is expected to be the number of
     * pitch periods requested from this PitchFrameAnalyser, @see{#getFramePeriods()}.
     * @return An analysis result. The data type depends on the concrete analyser.
     * @throws IllegalArgumentException if frame does not have the prescribed length 
     */
    public abstract Object analyse(double[] frame);
    
    protected FrameAnalysisResult constructAnalysisResult(Object analysisResult)
    {
        return new FrameAnalysisResult(frame, getFrameStartTime(), analysisResult);
    }
    
}
