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
package marytts.signalproc.analysis;

import java.util.ArrayList;
import java.util.List;

import marytts.signalproc.process.FrameProvider;
import marytts.signalproc.window.Window;
import marytts.util.data.DoubleDataSource;

/**
 * 
 * @author Marc Schr&ouml;der
 * 
 *         The base class for all frame-based signal analysis algorithms
 * 
 */
public abstract class FrameBasedAnalyser<T> extends FrameProvider {
	/**
	 * Array containing the analysis results, filled by analyseAllFrames(). Can be used for future reference to the results.
	 */
	protected FrameAnalysisResult<T>[] analysisResults;

	/**
	 * Initialise a FrameBasedAnalyser.
	 * 
	 * @param signal
	 *            the signal source to read from
	 * @param window
	 *            the window function to apply to each frame
	 * @param frameShift
	 *            the number of samples by which to shift the window from one frame analysis to the next; if this is smaller than
	 *            window.getLength(), frames will overlap.
	 * @param samplingRate
	 *            the number of samples in one second.
	 */
	public FrameBasedAnalyser(DoubleDataSource signal, Window window, int frameShift, int samplingRate) {
		super(signal, window, window.getLength(), frameShift, samplingRate, true);
	}

	/**
	 * The public method to call in order to trigger the analysis of the next frame.
	 * 
	 * @return the analysis result, or null if no part of the signal is left to analyse.
	 */
	public FrameAnalysisResult<T> analyseNextFrame() {
		double[] frame = getNextFrame();
		if (frame == null)
			return null;
		T analysisResult = analyse(frame);
		return constructAnalysisResult(analysisResult);
	}

	/**
	 * Analyse the entire signal as frames. Stop as soon as the first frame reaches or passes the end of the signal. Repeated
	 * access to this method returns a stored version of the results.
	 * 
	 * @return an array containing all frame analysis results.
	 */
	public FrameAnalysisResult<T>[] analyseAllFrames() {
		if (analysisResults == null) {
			ArrayList<FrameAnalysisResult<T>> results = new ArrayList<FrameAnalysisResult<T>>();
			FrameAnalysisResult<T> oneResult;
			while ((oneResult = analyseNextFrame()) != null) {
				results.add(oneResult);
			}
			FrameAnalysisResult<T>[] arr = new FrameAnalysisResult[results.size()];
			analysisResults = (FrameAnalysisResult<T>[]) results.toArray(arr);
		}
		return analysisResults;
	}

	/**
	 * Analyse the currently available input signal as frames. This method is intended for live signals such as microphone data.
	 * Stop when the amount of data available from the input is less than one frame length. Repeated access to this method will
	 * read new data if new data has become available in the meantime.
	 * 
	 * @return an array containing the frame analysis results for the data that is currently available, or an empty array if no
	 *         new data is available.
	 */
	public FrameAnalysisResult<T>[] analyseAvailableFrames() {
		List results = new ArrayList<FrameAnalysisResult<T>>();
		FrameAnalysisResult<T> oneResult;
		while (signal.available() >= frameLength) {
			oneResult = analyseNextFrame();
			assert oneResult != null;
			results.add(oneResult);
		}
		FrameAnalysisResult<T>[] arr = new FrameAnalysisResult[results.size()];
		return (FrameAnalysisResult<T>[]) results.toArray(arr);
	}

	/**
	 * Apply this FrameBasedAnalyser to the given data.
	 * 
	 * @param frame
	 *            the data to analyse, which must be of the length prescribed by this FrameBasedAnalyser, i.e. by similar to
	 *            {@link #getFrameLengthSamples()} .
	 * @return An analysis result. The data type depends on the concrete analyser.
	 * @throws IllegalArgumentException
	 *             if frame does not have the prescribed length
	 */
	public abstract T analyse(double[] frame);

	protected FrameAnalysisResult<T> constructAnalysisResult(T analysisResult) {
		return new FrameAnalysisResult<T>(frame, getFrameStartTime(), analysisResult);
	}

	public static class FrameAnalysisResult<T> {
		protected double[] windowedSignal;
		protected double startTime;
		protected T analysisResult;

		protected FrameAnalysisResult(double[] windowedSignal, double startTime, T analysisResult) {
			this.windowedSignal = new double[windowedSignal.length];
			System.arraycopy(windowedSignal, 0, this.windowedSignal, 0, windowedSignal.length);
			this.startTime = startTime;
			this.analysisResult = analysisResult;
		}

		public double[] getWindowedSignal() {
			return windowedSignal;
		}

		/**
		 * @return the start time of the frame, in seconds
		 */
		public double getStartTime() {
			return startTime;
		}

		public T get() {
			return analysisResult;
		}

	}
}
