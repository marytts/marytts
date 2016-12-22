/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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
package marytts.tools.voiceimport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.signalproc.analysis.F0TrackerAutocorrelationHeuristic;
import marytts.signalproc.analysis.PitchFileHeader;
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzer;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizerParams;
import marytts.unitselection.data.HnmDatagram;
import marytts.util.data.ESTTrackReader;
import marytts.util.io.FileUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * HnmTimelineMaker class takes a database root directory and a list of basenames, and converts the related wav files into a hnm
 * timeline in Mary format.
 * 
 * @author Oytun T&uuml;rk
 */
public class HnmTimelineMaker extends VoiceImportComponent {
	protected DatabaseLayout db = null;
	protected int percent = 0;

	protected String hnmAnalysisFileExt = ".ana";
	public final String HNMTIMELINE = "HnmTimelineMaker.hnmTimeline";
	public final String HNMANADIR = "HnmTimelineMaker.hnmAnalysisDir";

	public final String PMDIR = "db.pmDir";
	public final String PMEXT = "db.pmExtension";

	public String getName() {
		return "HnmTimelineMaker";
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout theDb) {
		this.db = theDb;
		if (props == null) {
			HntmAnalyzerParams analysisParams = new HntmAnalyzerParams();
			HntmSynthesizerParams synthesisParams = new HntmSynthesizerParams();

			props = new TreeMap<String, String>();

			props.put(HNMTIMELINE, db.getProp(db.FILEDIR) + "timeline_hnm" + db.getProp(db.MARYEXT));

			props.put(HNMANADIR, db.getProp(db.ROOTDIR) + "hna" + System.getProperty("file.separator"));

			props.put("HnmTimelineMaker.noiseModel", String.valueOf(analysisParams.noiseModel));
			props.put("HnmTimelineMaker.numFiltStages",
					String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.numFilteringStages));
			props.put("HnmTimelineMaker.medianFiltLen",
					String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.medianFilterLength));
			props.put("HnmTimelineMaker.maFiltLen",
					String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.movingAverageFilterLength));
			props.put("HnmTimelineMaker.cumAmpTh",
					String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.cumulativeAmpThreshold));
			props.put("HnmTimelineMaker.maxAmpTh",
					String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.maximumAmpThresholdInDB));
			props.put("HnmTimelineMaker.harmDevPercent",
					String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.harmonicDeviationPercent));
			props.put("HnmTimelineMaker.sharpPeakAmpDiff",
					String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.sharpPeakAmpDiffInDB));
			props.put("HnmTimelineMaker.minHarmonics",
					String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.minimumTotalHarmonics));
			props.put("HnmTimelineMaker.maxHarmonics",
					String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.maximumTotalHarmonics));
			props.put("HnmTimelineMaker.minVoicedFreq",
					String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.minimumVoicedFrequencyOfVoicing));
			props.put("HnmTimelineMaker.maxVoicedFreq",
					String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.maximumVoicedFrequencyOfVoicing));
			props.put("HnmTimelineMaker.maxFreqVoicingFinalShift",
					String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.maximumFrequencyOfVoicingFinalShift));
			props.put("HnmTimelineMaker.neighsPercent",
					String.valueOf(analysisParams.hnmPitchVoicingAnalyzerParams.neighsPercent));
			props.put("HnmTimelineMaker.harmCepsOrder", String.valueOf(analysisParams.harmonicPartCepstrumOrder));
			props.put("HnmTimelineMaker.regCepWarpMethod", String.valueOf(analysisParams.regularizedCepstrumWarpingMethod));
			props.put("HnmTimelineMaker.regCepsLambda", String.valueOf(analysisParams.regularizedCepstrumLambdaHarmonic));
			props.put("HnmTimelineMaker.noiseLpOrder", String.valueOf(analysisParams.noisePartLpOrder));
			props.put("HnmTimelineMaker.preCoefNoise", String.valueOf(analysisParams.preemphasisCoefNoise));
			props.put("HnmTimelineMaker.hpfBeforeNoiseAnalysis", String.valueOf(analysisParams.hpfBeforeNoiseAnalysis));
			props.put("HnmTimelineMaker.harmNumPer", String.valueOf(analysisParams.numPeriodsHarmonicsExtraction));
		}

		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();

		props2Help.put(HNMTIMELINE, "file containing all hnm noise waveform files. Will be created by this module");
		props2Help.put(HNMANADIR, "directory to write the harmnoics plus noise analysis results for each wav file");

		props2Help.put("HnmTimelineMaker.noiseModel", "Noise model: 1=WAVEFORM, 2=LPC, Default=1");
		props2Help
				.put("HnmTimelineMaker.numFiltStages",
						"Number of filtering stages to smooth out maximum frequency of voicing curve: Range={0, 1, 2, 3, 4, 5}. 0 means no smoothing. Default=2");
		props2Help
				.put("HnmTimelineMaker.medianFiltLen",
						"Length of median filter for smoothing the maximum frequency of voicing  curve: Range={4, 8, 12, 16, 20}, Default=12");
		props2Help
				.put("HnmTimelineMaker.maFiltLen",
						"Length of moving average filter for smoothing the maximum frequency of voicing  curve: Range={4, 8, 12, 16, 20}, Default=12");
		props2Help
				.put("HnmTimelineMaker.cumAmpTh",
						"Cumulative amplitude threshold [linear scale] for harmonic band voicing detection; decrease to increase max. freq. of voicing values. Range=[0.1, 10.0], Default=2.0");
		props2Help
				.put("HnmTimelineMaker.maxAmpTh",
						"Maximum amplitude threshold [in DB] for harmonic band voicing detection. Decrease to increase max. freq. of voicing values. Range=[0.0, 20.0], Default=13.0");
		props2Help
				.put("HnmTimelineMaker.harmDevPercent",
						"Percent deviation allowed for harmonic peak. Increase to increase max. freq. of voicing values. Range=[0.0, 100.0], Default=20.0");
		props2Help
				.put("HnmTimelineMaker.sharpPeakAmpDiff",
						"Minimum amplitude difference [in DB] to declare an isolated peak as harmonic. Decrease to increase max. freq. of voicing values. range=[0.0, 20.0], Default=12.0");
		props2Help.put("HnmTimelineMaker.minHarmonics",
				"Minimum total harmonics allowed in voiced regions. Range=[0, 100], Default=0");
		props2Help.put("HnmTimelineMaker.maxHarmonics",
				"Maximum total harmonics allowed in voiced regions. Range=[minHarmonics, 100], Default=100");
		props2Help.put("HnmTimelineMaker.minVoicedFreq",
				"Minimum voiced frequency for voiced regions [in Hz]. Range=[0.0, 0.5*samplingRate], Default=0");
		props2Help
				.put("HnmTimelineMaker.maxVoicedFreq",
						"Maximum voiced frequency for voiced regions [in Hz]. Range=Default=[minVoicedFreq, 0.5*samplingRate], Default=5000");
		props2Help
				.put("HnmTimelineMaker.maxFreqVoicingFinalShift",
						"Final amount of shift to be applied to the MWF curve [in Hz]. Range=[0.0, 0.5*samplingRate-maxVoicedFreq], Default=0");
		props2Help
				.put("HnmTimelineMaker.neighsPercent",
						"Percentage of samples that the harmonic peak needs to be larger than within a band. Decrease to increase max. freq. of voicing values. Range=[0.0, 100.0], Default=50");
		props2Help
				.put("HnmTimelineMaker.harmCepsOrder",
						"Cepstrum order to represent harmonic amplitudes. Increase to obtain better match with actual harmonic values. Range=[8, 40], Default=24");
		props2Help.put("HnmTimelineMaker.regCepWarpMethod",
				"Warping method for regularized cepstrum estimation. 1=POST_MEL, 2=PRE_BARK, Default=1");
		props2Help
				.put("HnmTimelineMaker.regCepsLambda",
						"Regularization term for cepstrum estimation. Increase to obtain smoother spectral match for harmonic amplitudes. However, this reduces the match with the actual amplitudes. Range=[0.0, 0.1], Default=1.0e-5");
		props2Help.put("HnmTimelineMaker.noiseLpOrder", "Linear prediction order for LPC noise part. Range=[8, 50], Default=12");
		props2Help.put("HnmTimelineMaker.preCoefNoise",
				"Pre-emphasis coefficient for linear prediction analysis of noise part. Range=[0.0, 0.99], Default=0.97");
		props2Help.put("HnmTimelineMaker.hpfBeforeNoiseAnalysis",
				"Remove lowpass frequency residual after harmonic subtraction? 0=NO, 1=YES, Default=1");
		props2Help.put("HnmTimelineMaker.harmNumPer", "Total periods for harmonic analysis. Range=[2.0, 4.0], Default=2");
	}

	/**
	 * Performs HNM analysis and writes the results to a single timeline file
	 *
	 */
	public boolean compute() {
		long start = System.currentTimeMillis(); // start timing

		System.out.println("---- Importing Harmonics plus noise parameters\n\n");
		System.out.println("Base directory: " + db.getProp(db.ROOTDIR) + "\n");

		/* Export the basename list into an array of strings */
		String[] baseNameArray = bnl.getListAsArray();

		/* Prepare the output directory for the timelines if it does not exist */
		File timelineDir = new File(db.getProp(db.FILEDIR));

		File ptcDir = new File(db.getProp(db.PTCDIR));
		if (!ptcDir.exists()) {
			ptcDir.mkdir();
		}

		File hnaDir = new File(getProp(HNMANADIR));
		if (!hnaDir.exists()) {
			hnaDir.mkdir();
		}

		try {
			/*
			 * 1) Determine the reference sampling rate as being the sample rate of the first encountered wav file
			 */
			WavReader wav = new WavReader(db.getProp(db.WAVDIR) + baseNameArray[0] + db.getProp(db.WAVEXT));
			int globSampleRate = wav.getSampleRate();
			System.out.println("---- Detected a global sample rate of: [" + globSampleRate + "] Hz.");

			System.out.println("---- Performing HNM analysis...");

			/* Make the file name */
			System.out.println("Will create the hnm timeline in file [" + getProp(HNMTIMELINE) + "].");

			/* An example of processing header: */
			Properties headerProps = new Properties();

			headerProps.setProperty("hnm.noiseModel", props.get("HnmTimelineMaker.noiseModel"));
			headerProps.setProperty("hnm.numFiltStages", props.get("HnmTimelineMaker.numFiltStages"));
			headerProps.setProperty("hnm.medianFiltLen", props.get("HnmTimelineMaker.medianFiltLen"));
			headerProps.setProperty("hnm.maFiltLen", props.get("HnmTimelineMaker.maFiltLen"));
			headerProps.setProperty("hnm.cumAmpTh", props.get("HnmTimelineMaker.cumAmpTh"));
			headerProps.setProperty("hnm.maxAmpTh", props.get("HnmTimelineMaker.maxAmpTh"));
			headerProps.setProperty("hnm.harmDevPercent", props.get("HnmTimelineMaker.harmDevPercent"));
			headerProps.setProperty("hnm.sharpPeakAmpDiff", props.get("HnmTimelineMaker.sharpPeakAmpDiff"));
			headerProps.setProperty("hnm.minHarmonics", props.get("HnmTimelineMaker.minHarmonics"));
			headerProps.setProperty("hnm.maxHarmonics", props.get("HnmTimelineMaker.maxHarmonics"));
			headerProps.setProperty("hnm.minVoicedFreq", props.get("HnmTimelineMaker.minVoicedFreq"));
			headerProps.setProperty("hnm.maxVoicedFreq", props.get("HnmTimelineMaker.maxVoicedFreq"));
			headerProps.setProperty("hnm.maxFreqVoicingFinalShift", props.get("HnmTimelineMaker.maxFreqVoicingFinalShift"));
			headerProps.setProperty("hnm.neighsPercent", props.get("HnmTimelineMaker.neighsPercent"));
			headerProps.setProperty("hnm.harmCepsOrder", props.get("HnmTimelineMaker.harmCepsOrder"));
			headerProps.setProperty("hnm.regCepWarpMethod", props.get("HnmTimelineMaker.regCepWarpMethod"));
			headerProps.setProperty("hnm.regCepsLambda", props.get("HnmTimelineMaker.regCepsLambda"));
			headerProps.setProperty("hnm.noiseLpOrder", props.get("HnmTimelineMaker.noiseLpOrder"));
			headerProps.setProperty("hnm.preCoefNoise", props.get("HnmTimelineMaker.preCoefNoise"));
			headerProps.setProperty("hnm.hpfBeforeNoiseAnalysis", props.get("HnmTimelineMaker.hpfBeforeNoiseAnalysis"));
			headerProps.setProperty("hnm.harmNumPer", props.get("HnmTimelineMaker.harmNumPer"));

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			headerProps.store(baos, null);
			String processingHeader = baos.toString("latin1");

			/* Instantiate the TimelineWriter: */
			TimelineWriter hnmTimeline = new TimelineWriter(getProp(HNMTIMELINE), processingHeader, globSampleRate, 0.1);

			// TO DO: Update these paratemers according to props
			HntmAnalyzerParams analysisParams = new HntmAnalyzerParams();
			HntmSynthesizerParams synthesisParamsBeforeNoiseAnalysis = new HntmSynthesizerParams();

			analysisParams.noiseModel = Integer.valueOf(props.get("HnmTimelineMaker.noiseModel"));
			analysisParams.hnmPitchVoicingAnalyzerParams.numFilteringStages = Integer.valueOf(props
					.get("HnmTimelineMaker.numFiltStages"));
			analysisParams.hnmPitchVoicingAnalyzerParams.medianFilterLength = Integer.valueOf(props
					.get("HnmTimelineMaker.medianFiltLen"));
			analysisParams.hnmPitchVoicingAnalyzerParams.movingAverageFilterLength = Integer.valueOf(props
					.get("HnmTimelineMaker.maFiltLen"));
			analysisParams.hnmPitchVoicingAnalyzerParams.cumulativeAmpThreshold = Float.valueOf(props
					.get("HnmTimelineMaker.cumAmpTh"));
			analysisParams.hnmPitchVoicingAnalyzerParams.maximumAmpThresholdInDB = Float.valueOf(props
					.get("HnmTimelineMaker.maxAmpTh"));
			analysisParams.hnmPitchVoicingAnalyzerParams.harmonicDeviationPercent = Float.valueOf(props
					.get("HnmTimelineMaker.harmDevPercent"));
			analysisParams.hnmPitchVoicingAnalyzerParams.sharpPeakAmpDiffInDB = Float.valueOf(props
					.get("HnmTimelineMaker.sharpPeakAmpDiff"));
			analysisParams.hnmPitchVoicingAnalyzerParams.minimumTotalHarmonics = Integer.valueOf(props
					.get("HnmTimelineMaker.minHarmonics"));
			analysisParams.hnmPitchVoicingAnalyzerParams.maximumTotalHarmonics = Integer.valueOf(props
					.get("HnmTimelineMaker.maxHarmonics"));
			analysisParams.hnmPitchVoicingAnalyzerParams.minimumVoicedFrequencyOfVoicing = Float.valueOf(props
					.get("HnmTimelineMaker.minVoicedFreq"));
			analysisParams.hnmPitchVoicingAnalyzerParams.maximumVoicedFrequencyOfVoicing = Float.valueOf(props
					.get("HnmTimelineMaker.maxVoicedFreq"));
			analysisParams.hnmPitchVoicingAnalyzerParams.maximumFrequencyOfVoicingFinalShift = Float.valueOf(props
					.get("HnmTimelineMaker.maxFreqVoicingFinalShift"));
			analysisParams.hnmPitchVoicingAnalyzerParams.neighsPercent = Float.valueOf(props
					.get("HnmTimelineMaker.neighsPercent"));
			analysisParams.harmonicPartCepstrumOrder = Integer.valueOf(props.get("HnmTimelineMaker.harmCepsOrder"));
			analysisParams.regularizedCepstrumWarpingMethod = Integer.valueOf(props.get("HnmTimelineMaker.regCepWarpMethod"));
			analysisParams.regularizedCepstrumLambdaHarmonic = Float.valueOf(props.get("HnmTimelineMaker.regCepsLambda"));
			analysisParams.noisePartLpOrder = Integer.valueOf(props.get("HnmTimelineMaker.noiseLpOrder"));
			analysisParams.preemphasisCoefNoise = Float.valueOf(props.get("HnmTimelineMaker.preCoefNoise"));
			analysisParams.hpfBeforeNoiseAnalysis = Boolean.valueOf(props.get("HnmTimelineMaker.hpfBeforeNoiseAnalysis"));
			analysisParams.numPeriodsHarmonicsExtraction = Float.valueOf(props.get("HnmTimelineMaker.harmNumPer"));

			analysisParams.isSilentAnalysis = true;
			//

			/* 2) Write the datagrams and feed the index */
			float totalDuration = 0.0f; // Accumulator for the total timeline duration
			long totalTime = 0l;
			long numDatagrams = 0l; // Total number of hnm datagrams in the timeline file

			/* For each wav file: */
			int n, i;
			double f0WindowSizeInSeconds = 0;
			double f0SkipSizeInSeconds = 0;

			for (n = 0; n < baseNameArray.length; n++) {
				percent = 100 * n / baseNameArray.length;
				/* - open+load */
				System.out.println(baseNameArray[n]);
				String wavFile = db.getProp(db.WAVDIR) + baseNameArray[n] + db.getProp(db.WAVEXT);

				ESTTrackReader pmFile = new ESTTrackReader(db.getProp(PMDIR) + baseNameArray[n] + db.getProp(PMEXT));
				totalDuration += pmFile.getTimeSpan();

				HntmAnalyzer ha = new HntmAnalyzer();
				String hnmAnalysisFile = getProp(HNMANADIR) + baseNameArray[n] + hnmAnalysisFileExt;

				HntmSpeechSignal hnmSignal = null;
				if (FileUtils.exists(hnmAnalysisFile))
					hnmSignal = new HntmSpeechSignal(hnmAnalysisFile, analysisParams.noiseModel);
				else {
					wav = new WavReader(wavFile);
					short[] wave = wav.getSamples();

					String ptcFile = db.getProp(db.PTCDIR) + baseNameArray[n] + db.getProp(db.PTCEXT);
					PitchReaderWriter f0 = null;
					if (FileUtils.exists(ptcFile))
						f0 = new PitchReaderWriter(ptcFile);
					else {
						PitchFileHeader pitchDetectorParams = new PitchFileHeader();
						// default values are problematic; for now, re-use the parameters from PraatPitchmarker:
						pitchDetectorParams.minimumF0 = Double.parseDouble(db.getProperty("PraatPitchmarker.minPitch"));
						pitchDetectorParams.maximumF0 = Double.parseDouble(db.getProperty("PraatPitchmarker.maxPitch"));
						F0TrackerAutocorrelationHeuristic pitchDetector = new F0TrackerAutocorrelationHeuristic(
								pitchDetectorParams);
						f0 = pitchDetector.pitchAnalyzeWavFile(wavFile, ptcFile);
					}

					int frameStart = 0;
					int frameEnd = 0;
					long duration;

					for (i = 0; i < pmFile.getNumFrames() - 1; i++) {
						frameStart = (int) ((double) pmFile.getTime(i) * (double) (globSampleRate));
						frameEnd = (int) ((double) pmFile.getTime(i + 1) * (double) (globSampleRate));
						assert frameEnd <= wave.length : "Frame ends after end of wave data: " + frameEnd + " > " + wave.length;
						duration = frameEnd - frameStart;
						if (duration < 5)
							System.out.println("Too short duration");
					}

					PitchMarks pm = new PitchMarks(pmFile, globSampleRate);
					pm.findAndSetUnvoicedF0s(f0.contour, f0.header, globSampleRate);

					if (n == 0) {
						f0WindowSizeInSeconds = f0.header.windowSizeInSeconds;
						if (pmFile.getNumFrames() > 1.0)
							f0SkipSizeInSeconds = SignalProcUtils.sampleFloat2time(
									((float) wave.length - SignalProcUtils.time2sample(f0WindowSizeInSeconds, globSampleRate))
											/ (pmFile.getNumFrames() - 1.0f), globSampleRate);
						else
							f0SkipSizeInSeconds = f0.header.skipSizeInSeconds;
					}

					// Use pitch marks from pm folder
					hnmSignal = ha.analyze(wave, wav.getSampleRate(), pm, f0WindowSizeInSeconds, f0SkipSizeInSeconds, pm.f0s,
							null, analysisParams, synthesisParamsBeforeNoiseAnalysis, hnmAnalysisFile);

					// Use autocorrelation pitch detector based pitch marks
					// hnmSignal = ha.analyze(wave, wav.getSampleRate(), f0, null, analysisParams,
					// synthesisParamsBeforeNoiseAnalysis, hnmAnalysisFile);

					float tAnalysisInSeconds = hnmSignal.frames[0].deltaAnalysisTimeInSeconds;
					for (i = 0; i < hnmSignal.frames.length; i++) {
						frameStart = frameEnd;
						frameEnd = SignalProcUtils.time2sample(tAnalysisInSeconds, hnmSignal.samplingRateInHz);

						assert frameEnd <= wave.length : "Frame ends after end of wave data: " + frameEnd + " > " + wave.length;
						duration = frameEnd - frameStart;

						tAnalysisInSeconds += hnmSignal.frames[i].deltaAnalysisTimeInSeconds;
					}
				}

				/* - For each frame in the hnm modeled speech signal: */
				int frameStart = 0;
				int frameEnd = 0;
				int duration = 0;
				long localTime = 0l;
				int currentIndex;
				int prevIndex = -1;
				float[] analysisTimes = hnmSignal.getAnalysisTimes();
				float tAnalysisInSeconds;

				float[] pmTimes = new float[pmFile.getNumFrames()];

				for (i = 0; i < pmFile.getNumFrames(); i++)
					pmTimes[i] = pmFile.getTime(i);

				tAnalysisInSeconds = hnmSignal.frames[0].deltaAnalysisTimeInSeconds;
				for (i = 0; i < pmFile.getNumFrames(); i++) {
					if (i < hnmSignal.frames.length) {
						frameStart = frameEnd;
						frameEnd = (int) ((double) pmFile.getTime(i) * (double) (globSampleRate));
						duration = frameEnd - frameStart;

						if (frameEnd > 0) {
							currentIndex = MathUtils.findClosest(analysisTimes, pmFile.getTime(i));

							// System.out.println(currentIndex + " " + i);

							hnmSignal.frames[currentIndex].tAnalysisInSeconds = tAnalysisInSeconds;

							// Feed the datagram to the timeline
							hnmTimeline.feed(new HnmDatagram(duration, hnmSignal.frames[currentIndex]), globSampleRate);
							totalTime += duration;
							localTime += duration;
							tAnalysisInSeconds += hnmSignal.frames[currentIndex].deltaAnalysisTimeInSeconds;
						}
					}
				}

				/*
				 * tAnalysisInSeconds = hnmSignal.frames[0].deltaAnalysisTimeInSeconds; for (i=0; i<hnmSignal.frames.length; i++)
				 * { frameStart = frameEnd; frameEnd = SignalProcUtils.time2sample(tAnalysisInSeconds,
				 * hnmSignal.samplingRateInHz); duration = frameEnd - frameStart;
				 * 
				 * // Feed the datagram to the timeline hnmTimeline.feed( new HnmDatagram(duration, hnmSignal.frames[i]) ,
				 * globSampleRate ); totalTime += duration; localTime += duration;
				 * 
				 * tAnalysisInSeconds += hnmSignal.frames[i].deltaAnalysisTimeInSeconds; }
				 */

				System.out.println(String.valueOf(n + 1) + " of " + String.valueOf(baseNameArray.length) + " done...");
				numDatagrams += hnmSignal.frames.length;
			}
			hnmTimeline.close();

			System.out.println("---- Done.");

			/* 7) Print some stats and close the file */
			System.out.println("---- hnm timeline result:");
			System.out.println("Number of files scanned: " + baseNameArray.length);
			System.out.println("Total duration: [" + totalTime + "] samples / ["
					+ ((double) (totalTime) / (double) (globSampleRate)) + "] seconds.");
			System.out.println("Number of frames: [" + numDatagrams + "].");
			System.out.println("Size of the index: [" + hnmTimeline.getIndex().getNumIdx() + "] ("
					+ (hnmTimeline.getIndex().getNumIdx() * 16) + " bytes, i.e. "
					+ new DecimalFormat("#.##").format((double) (hnmTimeline.getIndex().getNumIdx()) * 16.0 / 1048576.0)
					+ " megs).");

			long stop = System.currentTimeMillis(); // stop timing
			System.out.println("The process took " + (stop - start) / (1000.0 * 60) + " minutes to complete..."); // print
																													// execution
																													// time

			System.out.println("---- hnm timeline done.");

		} catch (SecurityException e) {
			System.err.println("Error: you don't have write access to the target database directory.");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e);
		}

		return (true);
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return percent;
	}

	public static void main(String[] args) throws Exception {
		VoiceImportComponent vic = new HnmTimelineMaker();
		DatabaseLayout db = new DatabaseLayout(vic);
		vic.compute();
	}
}
