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

package marytts.signalproc.analysis;

import java.util.ArrayList;
import java.util.List;

import marytts.signalproc.process.FrameProvider;
import marytts.signalproc.window.Window;
import marytts.util.data.DoubleDataSource;


/**
 * @author Marc Schr&ouml;der
 * 
 * The base class for all frame-based signal analysis algorithms
 * 
 */
public abstract class FrameBasedAnalyser extends FrameProvider
{
    /**
     * Array containing the analysis results, filled by analyseAllFrames().
     * Can be used for future reference to the results.
     */
    protected FrameAnalysisResult[] analysisResults;
    
    /**
     * Initialise a FrameBasedAnalyser.
     * @param signal the signal source to read from
     * @param window the window function to apply to each frame
     * @param frameShift the number of samples by which to shift the window from
     * one frame analysis to the next; if this is smaller than window.getLength(),
     * frames will overlap.
     * @param samplingRate the number of samples in one second.
     */
    public FrameBasedAnalyser(DoubleDataSource signal, Window window, int frameShift, int samplingRate)
    {
        super(signal, window, window.getLength(), frameShift, samplingRate, true);
    }
    
    /**
     * The public method to call in order to trigger the analysis of the next frame.
     * @return the analysis result, or null if no part of the signal is left to analyse.
     */
    public FrameAnalysisResult analyseNextFrame()
    {
        double[] frame = getNextFrame();
        if (frame == null) return null;
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
     * Apply this FrameBasedAnalyser to the given data.
     * @param frame the data to analyse, which must be of the length prescribed by this
     * FrameBasedAnalyser, i.e. by @see{#getFrameLengthSamples()}.
     * @return An analysis result. The data type depends on the concrete analyser.
     * @throws IllegalArgumentException if frame does not have the prescribed length 
     */
    public abstract Object analyse(double[] frame);
    
    protected FrameAnalysisResult constructAnalysisResult(Object analysisResult)
    {
        return new FrameAnalysisResult(frame, getFrameStartTime(), analysisResult);
    }
    
    public static class FrameAnalysisResult
    {
        protected double[] windowedSignal;
        protected double startTime;
        protected Object analysisResult;
        
        protected FrameAnalysisResult(double[] windowedSignal, double startTime, Object analysisResult)
        {
            this.windowedSignal = new double[windowedSignal.length];
            System.arraycopy(windowedSignal, 0, this.windowedSignal, 0, windowedSignal.length);
            this.startTime = startTime;
            this.analysisResult = analysisResult;
        }
        
        public double[] getWindowedSignal() { return windowedSignal; }
        public double getStartTime() { return startTime; }
        public Object get() { return analysisResult; }
        
    }
}
