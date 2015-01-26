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

import marytts.signalproc.analysis.FrameBasedAnalyser.FrameAnalysisResult;
import marytts.signalproc.process.PitchFrameProvider;
import marytts.signalproc.window.DynamicWindow;
import marytts.util.data.DoubleDataSource;

/**
 * 
 * @author Marc Schr&ouml;der
 * 
 *         The base class for all frame-based signal analysis algorithms.
 * 
 */
public abstract class PitchFrameAnalyser extends PitchFrameProvider {
	protected DynamicWindow analysisWindow;

	/**
	 * Array containing the analysis results, filled by {@link #analyseAllFrames()}. Can be used for future reference to the
	 * results.
	 */
	protected FrameAnalysisResult[] analysisResults;

	/**
	 * Initialise a PitchFrameAnalyser.
	 * 
	 * @param signal
	 *            the signal source to read from
	 * @param pitchmarks
	 *            the source of the pitchmarks, in seconds from the start of signal
	 * @param windowType
	 *            type of analysis window to use, {@link marytts.signalproc.window.Window#getAvailableTypes()}
	 * @param samplingRate
	 *            the number of samples in one second.
	 */
	public PitchFrameAnalyser(DoubleDataSource signal, DoubleDataSource pitchmarks, int windowType, int samplingRate) {
		this(signal, pitchmarks, windowType, samplingRate, 1, 1);
	}

	/**
	 * Create a new PitchFrameAnalyser with a configurable number of pitch periods per frame and pitch periods to shift by.
	 * 
	 * @param signal
	 *            audio signal
	 * @param pitchmarks
	 *            an array of pitchmarks; each pitch mark is in seconds from signal start
	 * @param windowType
	 *            type of analysis window to use, {@link marytts.signalproc.window.Window#getAvailableTypes()}
	 * @param samplingRate
	 *            number of samples per second in signal
	 * @param framePeriods
	 *            number of periods that each frame should contain
	 * @param shiftPeriods
	 *            number of periods that frames should be shifted by
	 */
	public PitchFrameAnalyser(DoubleDataSource signal, DoubleDataSource pitchmarks, int windowType, int samplingRate,
			int framePeriods, int shiftPeriods) {
		super(signal, pitchmarks, new DynamicWindow(windowType), samplingRate, framePeriods, shiftPeriods);
	}

	/**
	 * The public method to call in order to trigger the analysis of the next frame.
	 * 
	 * @return the analysis result, or null if no part of the signal is left to analyse.
	 */
	public FrameAnalysisResult analyseNextFrame() {
		double[] frame = getNextFrame();
		if (frame == null)
			return null;
		analysisWindow.applyInline(frame, 0, frame.length);
		Object analysisResult = analyse(frame);
		return constructAnalysisResult(analysisResult);
	}

	/**
	 * Analyse the entire signal as frames. Stop as soon as the first frame reaches or passes the end of the signal. Repeated
	 * access to this method returns a stored version of the results.
	 * 
	 * @return an array containing all frame analysis results.
	 */
	public FrameAnalysisResult[] analyseAllFrames() {
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
	 * Analyse the currently available input signal as frames. This method is intended for live signals such as microphone data.
	 * Stop when the amount of data available from the input is less than one frame length. Repeated access to this method will
	 * read new data if new data has become available in the meantime.
	 * 
	 * @return an array containing the frame analysis results for the data that is currently available, or an empty array if no
	 *         new data is available.
	 */
	public FrameAnalysisResult[] analyseAvailableFrames() {
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
	 * 
	 * @param frame
	 *            the data to analyse, which is expected to be the number of pitch periods requested from this PitchFrameAnalyser,
	 *            {@link #getFramePeriods()}.
	 * @return An analysis result. The data type depends on the concrete analyser.
	 * @throws IllegalArgumentException
	 *             if frame does not have the prescribed length
	 */
	public abstract Object analyse(double[] frame);

	protected FrameAnalysisResult constructAnalysisResult(Object analysisResult) {
		return new FrameAnalysisResult(frame, getFrameStartTime(), analysisResult);
	}

}
