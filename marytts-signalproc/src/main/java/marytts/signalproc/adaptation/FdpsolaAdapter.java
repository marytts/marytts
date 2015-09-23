/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.signalproc.adaptation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.adaptation.codebook.WeightedCodebook;
import marytts.signalproc.adaptation.codebook.WeightedCodebookLsfMatch;
import marytts.signalproc.adaptation.codebook.WeightedCodebookMapper;
import marytts.signalproc.adaptation.codebook.WeightedCodebookTransformerParams;
import marytts.signalproc.adaptation.gmm.jointgmm.JointGMMMapper;
import marytts.signalproc.adaptation.gmm.jointgmm.JointGMMMatch;
import marytts.signalproc.adaptation.gmm.jointgmm.JointGMMSet;
import marytts.signalproc.adaptation.gmm.jointgmm.JointGMMTransformerParams;
import marytts.signalproc.adaptation.prosody.PitchMapping;
import marytts.signalproc.adaptation.prosody.PitchStatistics;
import marytts.signalproc.adaptation.prosody.PitchTransformationData;
import marytts.signalproc.adaptation.prosody.PitchTransformer;
import marytts.signalproc.adaptation.prosody.ProsodyTransformerParams;
import marytts.signalproc.adaptation.smoothing.SmoothingDefinitions;
import marytts.signalproc.adaptation.smoothing.SmoothingFile;
import marytts.signalproc.adaptation.smoothing.TemporalSmoother;
import marytts.signalproc.analysis.F0ReaderWriter;
import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.LpcAnalyser;
import marytts.signalproc.analysis.LsfAnalyser;
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.analysis.LpcAnalyser.LpCoeffs;
import marytts.signalproc.process.PsolaFrameProvider;
import marytts.signalproc.process.VoiceModificationParametersPreprocessor;
import marytts.signalproc.window.DynamicWindow;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.display.DisplayUtils;
import marytts.util.io.FileUtils;
import marytts.util.io.LEDataInputStream;
import marytts.util.io.LEDataOutputStream;
import marytts.util.math.ArrayUtils;
import marytts.util.math.ComplexArray;
import marytts.util.math.FFTMixedRadix;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * A class that supports voice conversion through weighted codebook mapping or joint-GMMs and FDPSOLA based prosody and vocal
 * tract modifications
 * 
 * Reference: Moulines, E. and W. Verhelst, 1995, “Time-Domain and Frequency-Domain Techniques for Prosodic Modification of
 * Speech” in Kleijn and Paliwal (eds.), Speech Coding And Synthesis, pp. 519-555, Elsevier Science B.V., Netherlands.
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class FdpsolaAdapter {
	protected DoubleDataSource input;
	protected AudioInputStream inputAudio;
	protected DDSAudioInputStream outputAudio;
	protected VoiceModificationParametersPreprocessor modParams;
	protected int numfrm;
	protected int numfrmFixed;
	protected int lpOrder; // LP analysis order
	protected String outputFile;
	protected String tempOutBinaryFile;
	protected int origLen;
	protected PitchMarks pm;
	protected double[] f0s;
	protected PsolaFrameProvider psFrm;
	protected double wsFixedInSeconds;
	protected double ssFixedInSeconds;
	protected int numPeriods;
	protected static int NUM_PITCH_SYNC_PERIODS = 3;

	public boolean bSilent;
	protected LEDataOutputStream dout; // Output stream for big-endian wav tests
	protected LEDataInputStream din; // Input stream for big-endian wav tests
	protected DynamicWindow windowIn;
	protected DynamicWindow windowOut;
	protected double[] wgt;
	protected double[] wgty;

	protected int frmSize;
	protected int newFrmSize;
	protected int newPeriod;
	protected int synthFrmInd;
	protected double localDurDiff;
	protected int repeatSkipCount; // -1:skip frame, 0:no repetition (use synthesized frame as it is), >0: number of repetitions
									// for synthesized frame
	protected double localDurDiffSaved;
	protected double sumLocalDurDiffs;
	protected double nextAdd;

	protected int synthSt;
	protected int synthTotal;

	protected int maxFrmSize;
	protected int maxNewFrmSize;
	protected int synthFrameInd;
	protected boolean bLastFrame;
	protected boolean bBroke;

	protected int outBuffLen;
	protected double[] outBuff;
	protected int outBuffStart;
	protected int totalWrittenToFile;

	protected double[] ySynthBuff;
	protected double[] wSynthBuff;
	protected int ySynthInd;
	protected double[] frm;
	protected boolean bWarp;

	protected double[] inputVT;
	protected double[] py2;
	protected ComplexArray hy;
	protected double[] frmy;
	protected double frmEn;
	protected double frmyEn;
	protected double gain;
	protected int newSkipSize;
	protected int halfWin;
	protected double[] newVScales;
	protected double[] tmpvsc;
	// protected boolean isWavFileOutput;
	protected int inputFrameIndex;
	protected static double MIN_PSCALE = 0.1;
	protected static double MAX_PSCALE = 5.0;
	protected static double MIN_TSCALE = 0.1;
	protected static double MAX_TSCALE = 5.0;
	protected int fs;

	protected double tscaleSingle;

	private double desiredFrameTime;
	private boolean bShowSpectralPlots;

	private PitchTransformer pitchTransformer;

	private SmoothingFile smoothingFile;
	private double[][] smoothedVocalTract;
	private int smoothedInd;

	private int[] preselectedIndices;
	private int[] allIndices;
	private Labels labels;
	private Labels targetLabels;
	private int currentLabelIndex;
	private double[][] targetLsfs;

	private BaselineTransformerParams baseParams;

	public FdpsolaAdapter(BaselineAdaptationItem inputItem, String strOutputFile, WeightedCodebookTransformerParams wctParamsIn,
			double[] pscales, double[] tscales, double[] escales, double[] vscales) throws UnsupportedAudioFileException,
			IOException {
		baseParams = new WeightedCodebookTransformerParams(wctParamsIn);

		init(inputItem, strOutputFile, pscales, tscales, escales, vscales);
	}

	public FdpsolaAdapter(BaselineAdaptationItem inputItem, String strOutputFile, JointGMMTransformerParams jgmmParamsIn,
			double[] pscales, double[] tscales, double[] escales, double[] vscales) throws UnsupportedAudioFileException,
			IOException {
		baseParams = new JointGMMTransformerParams(jgmmParamsIn);

		init(inputItem, strOutputFile, pscales, tscales, escales, vscales);
	}

	public void init(BaselineAdaptationItem inputItem, String strOutputFile, double[] pscales, double[] tscales,
			double[] escales, double[] vscales) throws IOException {
		// Smoothing
		smoothingFile = null;
		if (baseParams.smoothingState == SmoothingDefinitions.NONE)
			baseParams.smoothedVocalTractFile = "";
		if (baseParams.smoothingState == SmoothingDefinitions.ESTIMATING_SMOOTHED_VOCAL_TRACT) {
			if (baseParams.smoothedVocalTractFile == "")
				throw new IllegalArgumentException("smoothedVocalTractFile not valid");
			else {
				smoothingFile = new SmoothingFile(baseParams.smoothedVocalTractFile, SmoothingFile.OPEN_FOR_WRITE);
				smoothingFile.smoothingMethod = baseParams.smoothingMethod;
				smoothingFile.writeHeader();
			}
		}

		if (baseParams.smoothingState == SmoothingDefinitions.TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT
				&& baseParams.smoothingMethod != SmoothingDefinitions.NO_SMOOTHING) {
			if (!FileUtils.exists(baseParams.smoothedVocalTractFile))
				throw new IllegalArgumentException("smoothedVocalTractFile not found");
			else {
				smoothingFile = new SmoothingFile(baseParams.smoothedVocalTractFile, SmoothingFile.OPEN_FOR_READ);
				smoothedVocalTract = smoothingFile.readAll();
				smoothedInd = 0;
			}
		}
		//

		pitchTransformer = new PitchTransformer();
		inputAudio = null;
		input = null;
		pm = null;
		f0s = null;

		wsFixedInSeconds = 0.02;
		ssFixedInSeconds = 0.01;
		numPeriods = NUM_PITCH_SYNC_PERIODS;

		origLen = 0;
		fs = 16000;

		numfrm = 0; // Total pitch synchronous frames (This is the actual number of frames to be processed)
		numfrmFixed = 0; // Total frames if the analysis was fixed skip-rate

		modParams = null;

		outputFile = null;

		tscaleSingle = 1.0;

		boolean bContinue = true;

		if (!FileUtils.exists(inputItem.audioFile)) {
			System.out.println("Error! Audio file " + inputItem.audioFile + " not found.");
			bContinue = false;
		}

		if (!FileUtils.exists(inputItem.f0File) && !FileUtils.exists(inputItem.pitchFile)) {
			System.out.println("Error! No F0 or pitch file found: " + inputItem.f0File + " " + inputItem.pitchFile);
			bContinue = false;
		}

		if (strOutputFile == null || strOutputFile == "") {
			System.out.println("Invalid output file...");
			bContinue = false;
		}

		if (bContinue) {
			boolean isF0File = false;
			if (FileUtils.exists(inputItem.f0File))
				isF0File = true;

			try {
				inputAudio = AudioSystem.getAudioInputStream(new File(inputItem.audioFile));
			} catch (UnsupportedAudioFileException e) {
				throw new IOException("Cannot open audio", e);
			}

			input = new AudioDoubleDataSource(inputAudio);

			origLen = (int) input.getDataLength();
			fs = (int) inputAudio.getFormat().getSampleRate();

			PitchReaderWriter f0 = null;
			if (FileUtils.exists(inputItem.f0File))
				f0 = new F0ReaderWriter(inputItem.f0File);
			else
				f0 = new PitchReaderWriter(inputItem.pitchFile);

			pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, fs, origLen, f0.header.windowSizeInSeconds,
					f0.header.skipSizeInSeconds, true, 0);

			numfrmFixed = (int) (Math.floor(((double) (origLen + pm.totalZerosToPadd) / fs - 0.5 * wsFixedInSeconds)
					/ ssFixedInSeconds + 0.5) + 2); // Total frames if the analysis was fixed skip-rate
			if (!baseParams.isFixedRateVocalTractConversion)
				numfrm = pm.pitchMarks.length - numPeriods; // Total pitch synchronous frames (This is the actual number of frames
															// to be processed)
			else
				numfrm = numfrmFixed;

			f0s = SignalProcUtils.fixedRateF0Values(pm, wsFixedInSeconds, ssFixedInSeconds, numfrmFixed, fs);

			lpOrder = SignalProcUtils.getLPOrder(fs);

			// Estimation of time varying pitch, duration, energy, and vocal tract scaling amounts using either:
			// - pscales, tscales, escales, vscales
			// - or FESTIVAL_UTT(for pitch and duration) and target wav file(for energy)
			if (!baseParams.isPitchFromTargetFile && !baseParams.isDurationFromTargetFile && !baseParams.isEnergyFromTargetFile) {
				modParams = new VoiceModificationParametersPreprocessor(fs, lpOrder, pscales, tscales, escales, vscales,
						pm.pitchMarks, wsFixedInSeconds, ssFixedInSeconds, numfrm, numfrmFixed, numPeriods,
						baseParams.isFixedRateVocalTractConversion);
				tscaleSingle = modParams.tscaleSingle;
			} else {
				// inputItem.targetEnergyFile should be computed from inputItem.targetWavFile at this point
				// inputItem.energyFile should be computed from inputItem.audioFile at this point

				String targetAlignmentFile = null;
				if (baseParams.targetAlignmentFileType == BaselineTransformerParams.LABELS)
					targetAlignmentFile = inputItem.targetLabelFile;
				else if (baseParams.targetAlignmentFileType == BaselineTransformerParams.FESTIVAL_UTT)
					targetAlignmentFile = inputItem.targetFestivalUttFile;

				String sourcePitchContourFile;
				if (isF0File)
					sourcePitchContourFile = inputItem.f0File;
				else
					sourcePitchContourFile = inputItem.pitchFile;

				String targetPitchContourFile;
				if (isF0File)
					targetPitchContourFile = inputItem.targetF0File;
				else
					targetPitchContourFile = inputItem.targetPitchFile;

				modParams = new VoiceModificationParametersPreprocessor(sourcePitchContourFile, isF0File, inputItem.labelFile,
						inputItem.audioFile, targetPitchContourFile, inputItem.targetWavFile, baseParams.isPitchFromTargetFile,
						baseParams.pitchFromTargetMethod, baseParams.isDurationFromTargetFile,
						baseParams.durationFromTargetMethod, baseParams.isEnergyFromTargetFile,
						baseParams.targetAlignmentFileType, targetAlignmentFile, pm.pitchMarks, wsFixedInSeconds,
						ssFixedInSeconds, numfrm, numfrmFixed, numPeriods, baseParams.isFixedRateVocalTractConversion);

				tscaleSingle = 1.0;
				for (int i = 0; i < modParams.tscalesVar.length; i++) {
					if (modParams.tscalesVar[i] != 1.0) {
						tscaleSingle = -1.0;
						break;
					}
				}
			}

			outputFile = strOutputFile;

			if (inputItem.labelFile != "" && FileUtils.exists(inputItem.labelFile))
				labels = new Labels(inputItem.labelFile);
			else
				labels = null;

			if (inputItem.targetLabelFile != "" && FileUtils.exists(inputItem.targetLabelFile))
				targetLabels = new Labels(inputItem.targetLabelFile);
			else
				targetLabels = null;

			if (inputItem.targetWavFile != "" && FileUtils.exists(inputItem.targetWavFile) && baseParams.isLsfsFromTargetFile) {
				try {
					targetLsfs = LsfAnalyser.lsfAnalyzeWavFile(inputItem.targetWavFile, baseParams.lsfParams);
				} catch (UnsupportedAudioFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else
				targetLsfs = null;
		}

		if (bContinue) {
			tmpvsc = new double[1];
			bSilent = false;

			if (outputFile != null)
				tempOutBinaryFile = outputFile + ".bin";

			if (!baseParams.isFixedRateVocalTractConversion)
				psFrm = new PsolaFrameProvider(input, pm, modParams.fs, modParams.numPeriods);
			else
				psFrm = new PsolaFrameProvider(input, wsFixedInSeconds, ssFixedInSeconds, modParams.fs, numfrm);

			try {
				dout = new LEDataOutputStream(tempOutBinaryFile);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			windowIn = new DynamicWindow(Window.HANNING);
			windowOut = new DynamicWindow(Window.HANNING);

			frmSize = 0;
			newFrmSize = 0;
			newPeriod = 0;
			synthFrmInd = 0;
			localDurDiff = 0.0;
			repeatSkipCount = 0; // -1:skip frame, 0:no repetition (use synthesized frame as it is), >0: number of repetitions for
									// synthesized frame
			localDurDiffSaved = 0.0;
			sumLocalDurDiffs = 0.0;
			nextAdd = 0.0;

			synthSt = pm.pitchMarks[0];

			synthTotal = 0;

			maxFrmSize = (int) (numPeriods * fs / 40.0);
			if ((maxFrmSize % 2) != 0)
				maxFrmSize++;

			maxNewFrmSize = (int) (Math.floor(maxFrmSize / MIN_PSCALE + 0.5));
			if ((maxNewFrmSize % 2) != 0)
				maxNewFrmSize++;

			synthFrameInd = 0;
			bLastFrame = false;
			bBroke = false;

			outBuffLen = 500000;
			outBuff = MathUtils.zeros(outBuffLen);
			outBuffStart = 1;
			totalWrittenToFile = 0;

			ySynthBuff = MathUtils.zeros(maxNewFrmSize);
			wSynthBuff = MathUtils.zeros(maxNewFrmSize);
			ySynthInd = 1;
		}
	}

	public void fdpsolaOnline(VocalTractTransformationFunction vtMapper, VocalTractTransformationData vtData,
			PitchTransformationData ptData) throws IOException {
		int i;
		double[] frmIn;
		boolean isLastInputFrame;
		int inputFrameSize;
		int currentPeriod;

		desiredFrameTime = 1.05;
		bShowSpectralPlots = false;

		fs = (int) inputAudio.getFormat().getSampleRate();

		PitchStatistics inputF0Statistics = new PitchStatistics(baseParams.prosodyParams.pitchStatisticsType, f0s);

		double[] targetF0s = null;

		if (!baseParams.isPitchFromTargetFile) {
			if (ptData instanceof PitchMapping) {
				targetF0s = pitchTransformer.transform(baseParams.prosodyParams, ((PitchMapping) ptData).f0StatisticsMapping,
						inputF0Statistics, f0s, modParams.pscalesVar);
			} else {
				baseParams.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.USE_ONLY_PSCALES;
				targetF0s = pitchTransformer.transform(baseParams.prosodyParams, ((PitchMapping) ptData).f0StatisticsMapping,
						inputF0Statistics, f0s, modParams.pscalesVar);
			}
		} else {
			targetF0s = new double[numfrm];
		}

		preselectedIndices = null;
		allIndices = null;
		if (vtData instanceof WeightedCodebook && baseParams instanceof WeightedCodebookTransformerParams
				&& !((WeightedCodebookTransformerParams) baseParams).isContextBasedPreselection) {
			// Whole codebook
			allIndices = new int[((WeightedCodebook) vtData).entries.length];
			for (i = 0; i < allIndices.length; i++)
				allIndices[i] = i;
		}

		inputFrameIndex = 0;
		for (i = 0; i < numfrm; i++) {
			frmIn = psFrm.getNextFrame();

			if (bBroke)
				break;

			if (i == numfrm - 1)
				isLastInputFrame = true;
			else
				isLastInputFrame = false;

			if (!baseParams.isFixedRateVocalTractConversion) {
				currentPeriod = pm.pitchMarks[i + 1] - pm.pitchMarks[i];
				inputFrameSize = pm.pitchMarks[i + modParams.numPeriods] - pm.pitchMarks[i] + 1;
			} else {
				currentPeriod = -1;
				inputFrameSize = frmIn.length;
			}

			double targetF0Value = 0.0;
			double currentF0Value;
			int index = (int) (Math.floor((psFrm.getCurrentTime() - 0.5 * wsFixedInSeconds) / ssFixedInSeconds + 0.5));

			if (!baseParams.isPitchFromTargetFile) {
				index = MathUtils.CheckLimits(index, 0, targetF0s.length - 1);
				targetF0Value = targetF0s[index];
			}

			currentF0Value = f0s[index];

			if (baseParams.isPitchFromTargetFile) {
				targetF0Value = currentF0Value * modParams.pscalesVar[i];
				targetF0s[i] = targetF0Value;
			}

			boolean isVoiced;
			if (!baseParams.isFixedRateVocalTractConversion) {
				if (pm.f0s[i] > 10.0)
					isVoiced = true;
				else
					isVoiced = false;
			} else {
				if (currentF0Value > 10.0)
					isVoiced = true;
				else
					isVoiced = false;
			}

			double currentF0;
			if (isVoiced)
				currentF0 = fs / currentPeriod;
			else
				currentF0 = 0.0;

			if (labels != null)
				currentLabelIndex = SignalProcUtils.time2LabelIndex(psFrm.getCurrentTime(), labels);
			else
				currentLabelIndex = -1;

			processFrame(frmIn, isVoiced, currentF0, targetF0Value, modParams.tscalesVar[i], modParams.escalesVar[i],
					modParams.vscalesVar[i], isLastInputFrame, currentPeriod, inputFrameSize, vtMapper, vtData);

			if (baseParams.isVocalTractTransformation
					&& baseParams.smoothingState == SmoothingDefinitions.TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT
					&& baseParams.smoothingMethod != SmoothingDefinitions.NO_SMOOTHING) {
				smoothedInd++;
				if (smoothedInd > smoothedVocalTract.length - 1)
					smoothedInd = smoothedVocalTract.length - 1;
			}
		}

		writeFinal();

		convertToWav(inputAudio.getFormat());

		inputAudio.close();

		// Perform smoothing on the vocal tract parameter file
		if (baseParams.smoothingState == SmoothingDefinitions.ESTIMATING_SMOOTHED_VOCAL_TRACT) {
			if (baseParams.isVocalTractTransformation) {
				System.out.println("Temporal smoothing started using " + String.valueOf(baseParams.smoothingNumNeighbours)
						+ " neighbours...");
				smoothingFile.close();
				smoothingFile = new SmoothingFile(baseParams.smoothedVocalTractFile, SmoothingFile.OPEN_FOR_READ);
				double[][] vts = smoothingFile.readAll();

				double[] tmp1 = new double[vts.length];
				for (i = 0; i < vts.length; i++)
					tmp1[i] = vts[i][20];

				vts = TemporalSmoother.smooth(vts, baseParams.smoothingNumNeighbours);

				double[] tmp2 = new double[vts.length];
				for (i = 0; i < vts.length; i++)
					tmp2[i] = vts[i][20];

				smoothingFile = new SmoothingFile(baseParams.smoothedVocalTractFile, SmoothingFile.OPEN_FOR_WRITE,
						baseParams.smoothingMethod);
				smoothingFile.writeAll(vts);
				System.out.println("Temporal smoothing completed...");
			}
		} else if (baseParams.smoothingState == SmoothingDefinitions.TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT)
			FileUtils.delete(baseParams.smoothedVocalTractFile);
		//
		/*
		 * MaryUtils.plot(f0s); MaryUtils.plot(targetF0s);
		 */
	}

	/*
	 * Voice conversion version of FD-PSOLA algorithm. The original FD-PSOLA is described in:
	 * 
	 * Moulines, E. and W. Verhelst, 1995, “Time-Domain and Frequency-Domain Techniques for Prosodic Modification of Speech” in
	 * Kleijn and Paliwal (eds.), Speech Coding And Synthesis, pp. 519-555, Elsevier Science B.V., Netherlands.
	 * 
	 * and the spectral conversion using codebook mapping in:
	 * 
	 * Arslan, L. M., 1999, “Speaker Transformation Algorithm using Segmental Codebooks”, Speech Communication, 28, pp. 211-226.
	 */
	public double[] processFrame(double[] frmIn, boolean isVoiced, double currentF0, double targetF0, double tscale,
			double escale, double vscale, boolean isLastInputFrame, int currentPeriod, int inputFrameSize,
			VocalTractTransformationFunction mapper, VocalTractTransformationData data) throws IOException {
		double pscale;

		if (currentF0 > 10.0)
			pscale = targetF0 / currentF0;
		else
			pscale = 1.0;

		if (pscale < MIN_PSCALE)
			pscale = MIN_PSCALE;
		if (pscale > MAX_PSCALE)
			pscale = MAX_PSCALE;

		if (tscale < MIN_TSCALE)
			tscale = MIN_TSCALE;
		if (tscale > MAX_TSCALE)
			tscale = MAX_TSCALE;

		double[] output = null;
		double[] outputTmp = null;
		int j, k, wInd, kMax;
		int tmpFix, tmpAdd, tmpMul;
		int remain;
		int kInd;
		VocalTractMatch match = null;

		windowIn = new DynamicWindow(baseParams.lsfParams.windowType);
		windowOut = new DynamicWindow(baseParams.lsfParams.windowType);

		repeatSkipCount = 0; // -1:skip frame, 0:no repetition (use synthesized frame as it is), >0: number of repetitions for
								// synthesized frame

		// Compute new frame sizes, change in durations due to pitch scaling, and required compensation amount in samples
		// &
		// Find out which pitch-scaled frames to repeat/skip for overall duration
		// compensation
		frmSize = inputFrameSize;
		if ((frmSize % 2) != 0)
			frmSize++;
		if (frmSize < 4)
			frmSize = 4;

		if (isVoiced) {
			newFrmSize = (int) (Math.floor(frmSize / pscale + 0.5));
			if ((newFrmSize % 2) != 0)
				newFrmSize++;
			if (newFrmSize < 4)
				newFrmSize = 4;
		} else
			newFrmSize = frmSize;

		newPeriod = (int) Math.floor(((double) newFrmSize) / NUM_PITCH_SYNC_PERIODS + 0.5);
		// Compute duration compensation required:
		// localDurDiffs(i) = (DESIRED)-(AFTER PITCHSCALING)
		// (-) if expansion occured, (+) if compression occured
		// We aim to make this as close to zero as possible in the following duration compensation step
		localDurDiff = nextAdd + (frmSize * tscale - newFrmSize) / NUM_PITCH_SYNC_PERIODS;

		nextAdd = 0;
		if (localDurDiff < -0.1 * newPeriod) // Expansion occured so skip this frame
		{
			repeatSkipCount--;
			if (!isLastInputFrame) {
				nextAdd = localDurDiff + newPeriod;
				localDurDiff = 0;
			}
		} else if (localDurDiff > 0.1 * newPeriod) // Compression occured so repeat this frame
		{
			while (localDurDiff > 0.1 * newPeriod) {
				repeatSkipCount++;
				localDurDiff -= newPeriod;
			}

			if (!isLastInputFrame) {
				nextAdd = localDurDiff;
				localDurDiff = 0;
			}
		}

		sumLocalDurDiffs += localDurDiff;

		if (isLastInputFrame) {
			// Check the final length and perform additional repetitions if necessary
			localDurDiff = sumLocalDurDiffs;
			while (localDurDiff > 0) {
				repeatSkipCount++;
				localDurDiff -= newPeriod;
			}
			//
		}

		if (isLastInputFrame) {
			repeatSkipCount++;
			bLastFrame = true;
		}

		double[] tmpSpec;
		ComplexArray tmpComp;

		LpCoeffs inputLPCoeffs = null;
		double[] inputLpcs = null;
		double[] inputLsfs = null;
		double sqrtInputGain;
		double[] targetLpcs = null;

		ComplexArray inputDft = null;
		ComplexArray inputExpTerm = null;
		ComplexArray outputExpTerm = null;
		ComplexArray inputResidual = null;
		ComplexArray outputResidual = null;
		ComplexArray outputDft = null;

		double[] inputVocalTractSpectrum = null;
		double[] interpolatedInputLpcs = null;
		double[] sourceVocalTractSpectrumEstimate = null;
		double[] targetVocalTractSpectrumEstimate = null;
		double[] interpolatedInputVocalTractSpectrum = null;
		double[] outputVocalTractSpectrum = null;
		double[] warpedOutputVocalTractSpectrum = null;
		double[] transformationFilter = null;
		Context currentContext = null;

		if (repeatSkipCount > -1) {
			frm = MathUtils.zeros(frmSize);
			System.arraycopy(frmIn, 0, frm, 0, Math.min(frmIn.length, frmSize));
			wgt = windowIn.values(frmSize);

			if (vscale != 1.0)
				bWarp = true;
			else
				bWarp = false;

			boolean isTransformUnvoiced = true;

			if ((isVoiced && pscale != 1.0) || bWarp || isTransformUnvoiced) {
				frmEn = SignalProcUtils.getEnergy(frm);

				wgt = windowIn.values(frmSize);

				// Windowing
				for (j = 0; j < frmSize; j++)
					frm[j] = frm[j] * wgt[j];

				// Preemphasis
				frm = SignalProcUtils.applyPreemphasis(frm, baseParams.lsfParams.preCoef);

				// Compute LPC coefficients
				inputLPCoeffs = LpcAnalyser.calcLPC(frm, baseParams.lsfParams.dimension);
				inputLpcs = inputLPCoeffs.getOneMinusA();

				if (!baseParams.lsfParams.isBarkScaled)
					inputLsfs = LsfAnalyser.lpc2lsfInHz(inputLpcs, fs);
				else
					inputLsfs = LsfAnalyser.lpc2lsfInBark(inputLpcs, fs);

				sqrtInputGain = inputLPCoeffs.getGain();

				// Find target estimate from codebook
				if (baseParams.isVocalTractTransformation) {
					if (mapper instanceof WeightedCodebookMapper) {
						if (baseParams instanceof WeightedCodebookTransformerParams
								&& ((WeightedCodebookTransformerParams) baseParams).isContextBasedPreselection) {
							currentContext = new Context(labels, currentLabelIndex,
									((WeightedCodebookTransformerParams) baseParams).totalContextNeighbours);
							preselectedIndices = ((WeightedCodebookMapper) mapper).preselect(currentContext,
									(WeightedCodebook) data,
									((WeightedCodebookTransformerParams) baseParams).isVocalTractMatchUsingTargetModel,
									((WeightedCodebookTransformerParams) baseParams).mapperParams.numBestMatches);
						}

						if (preselectedIndices != null)
							match = ((WeightedCodebookMapper) mapper).transform(inputLsfs, (WeightedCodebook) data,
									baseParams.isVocalTractMatchUsingTargetModel, preselectedIndices);
						else
							match = ((WeightedCodebookMapper) mapper).transform(inputLsfs, (WeightedCodebook) data,
									baseParams.isVocalTractMatchUsingTargetModel, allIndices);

						// Use source for testing things. Don´t forget to set isSourceVocalTractFromCodeook=false
						// codebookMatch = new WeightedCodebookMatch(inputLsfs, inputLsfs);
					} else if (mapper instanceof JointGMMMapper) {
						// Different weighting strategies can be tested here, i.e. doing a fuzzy phone classification
						double[] gmmWeights = new double[((JointGMMSet) data).gmms.length];
						Arrays.fill(gmmWeights, 0.0);
						int currentGmmIndex = -1;
						if (currentLabelIndex >= 0 && currentLabelIndex < labels.items.length)
							currentGmmIndex = ((JointGMMSet) data).cgParams.getClassIndex(labels.items[currentLabelIndex].phn);
						if (currentGmmIndex > 0)
							gmmWeights[currentGmmIndex] = 1.0;

						gmmWeights = MathUtils.normalizeToSumUpTo(gmmWeights, 1.0);

						match = ((JointGMMMapper) mapper).transform(inputLsfs, (JointGMMSet) data, gmmWeights,
								baseParams.isVocalTractMatchUsingTargetModel);
					} else if (mapper instanceof TargetLsfCopyMapper) {
						match = ((TargetLsfCopyMapper) mapper).transform(psFrm.getCurrentTime(), labels, targetLabels,
								targetLsfs, baseParams.lsfParams.winsize, baseParams.lsfParams.skipsize);
					}
				}

				inputDft = new ComplexArray(frmSize);
				int maxFreq = frmSize / 2 + 1;

				System.arraycopy(frm, 0, inputDft.real, 0, Math.min(frmSize, inputDft.real.length));

				if (inputDft.real.length > frmSize)
					Arrays.fill(inputDft.real, inputDft.real.length - frmSize, inputDft.real.length - 1, 0);

				Arrays.fill(inputDft.imag, 0, inputDft.imag.length - 1, 0);

				inputDft = FFTMixedRadix.fftComplex(inputDft);

				// For checking
				if (bShowSpectralPlots && psFrm.getCurrentTime() >= desiredFrameTime) {
					tmpComp = new ComplexArray(inputDft);
					tmpSpec = MathUtils.dft2ampdb(tmpComp, 0, maxFreq);
					DisplayUtils.plot(tmpSpec, "1.Input DFT");
				}
				//

				inputExpTerm = LpcAnalyser.calcExpTerm(frmSize, baseParams.lsfParams.dimension);
				outputExpTerm = LpcAnalyser.calcExpTerm(newFrmSize, baseParams.lsfParams.dimension);

				inputVocalTractSpectrum = LpcAnalyser.calcSpecLinearFromOneMinusA(inputLPCoeffs.getOneMinusA(),
						(float) sqrtInputGain, frmSize, inputExpTerm);

				// Use a weighted codebook estimate of the input vocal tract spectrum. This will result in a smoother
				// transformation filter
				if (baseParams.isSourceVocalTractSpectrumFromModel && baseParams.isVocalTractTransformation) {
					if (mapper instanceof WeightedCodebookMapper) {
						if (!baseParams.isResynthesizeVocalTractFromSourceModel) {
							if (!baseParams.lsfParams.isBarkScaled)
								interpolatedInputLpcs = LsfAnalyser.lsfInHz2lpc(
										((WeightedCodebookLsfMatch) match).entry.sourceItem.lsfs, fs);
							else
								interpolatedInputLpcs = LsfAnalyser.lsfInBark2lpc(
										((WeightedCodebookLsfMatch) match).entry.sourceItem.lsfs, fs);
						} else {
							if (!baseParams.lsfParams.isBarkScaled)
								interpolatedInputLpcs = LsfAnalyser.lsfInHz2lpc(
										((WeightedCodebookLsfMatch) match).entry.targetItem.lsfs, fs);
							else
								interpolatedInputLpcs = LsfAnalyser.lsfInBark2lpc(
										((WeightedCodebookLsfMatch) match).entry.targetItem.lsfs, fs);
						}
					} else if (mapper instanceof JointGMMMapper) {
						if (!baseParams.isResynthesizeVocalTractFromSourceModel) {
							if (!baseParams.lsfParams.isBarkScaled)
								interpolatedInputLpcs = LsfAnalyser.lsfInHz2lpc(((JointGMMMatch) match).mappedSourceFeatures, fs);
							else
								interpolatedInputLpcs = LsfAnalyser.lsfInBark2lpc(((JointGMMMatch) match).mappedSourceFeatures,
										fs);
						} else {
							if (!baseParams.lsfParams.isBarkScaled)
								interpolatedInputLpcs = LsfAnalyser.lsfInHz2lpc(((JointGMMMatch) match).outputFeatures, fs);
							else
								interpolatedInputLpcs = LsfAnalyser.lsfInBark2lpc(((JointGMMMatch) match).outputFeatures, fs);
						}
					} else if (mapper instanceof TargetLsfCopyMapper)
						interpolatedInputLpcs = ArrayUtils.copy(inputLpcs);

					sourceVocalTractSpectrumEstimate = LpcAnalyser.calcSpecLinearFromOneMinusA(interpolatedInputLpcs, 1.0f,
							newFrmSize, outputExpTerm);
				}

				// For checking
				if (bShowSpectralPlots && psFrm.getCurrentTime() >= desiredFrameTime) {
					tmpSpec = new double[maxFreq];
					System.arraycopy(inputVocalTractSpectrum, 0, tmpSpec, 0, tmpSpec.length);
					tmpSpec = MathUtils.amp2db(tmpSpec);
					DisplayUtils.plot(tmpSpec, "2.Input Vocal Tract");
					FileUtils.writeToTextFile(inputVocalTractSpectrum, "d:/hmmTest_inputVT.txt");
				}
				//

				// For checking
				if (bShowSpectralPlots && psFrm.getCurrentTime() >= desiredFrameTime
						&& baseParams.isSourceVocalTractSpectrumFromModel && baseParams.isVocalTractTransformation) {
					tmpSpec = new double[maxFreq];
					System.arraycopy(sourceVocalTractSpectrumEstimate, 0, tmpSpec, 0, tmpSpec.length);
					tmpSpec = MathUtils.amp2db(tmpSpec);
					DisplayUtils.plot(tmpSpec, "3.Source Vocal Tract Estimate");
					FileUtils.writeToTextFile(sourceVocalTractSpectrumEstimate, "d:/hmmTest_estimateVT.txt");
				}
				//

				inputResidual = new ComplexArray(frmSize);

				// Filter out vocal tract to obtain the input residual spectrum (note that this is the real residual spectrum)
				for (k = 0; k < maxFreq; k++) {
					inputResidual.real[k] = inputDft.real[k] / inputVocalTractSpectrum[k];
					inputResidual.imag[k] = inputDft.imag[k] / inputVocalTractSpectrum[k];
				}

				// For checking
				if (bShowSpectralPlots && psFrm.getCurrentTime() >= desiredFrameTime) {
					tmpComp = new ComplexArray(inputResidual);
					tmpSpec = MathUtils.dft2ampdb(tmpComp, 0, maxFreq - 1);
					DisplayUtils.plot(tmpSpec, "4.Input Residual");
				}
				//

				int newMaxFreq = newFrmSize / 2 + 1;
				if (baseParams.isVocalTractTransformation) {
					// Smoothing
					if (baseParams.smoothingMethod == SmoothingDefinitions.OUTPUT_LSFCONTOUR_SMOOTHING) {
						if (baseParams.smoothingState == SmoothingDefinitions.ESTIMATING_SMOOTHED_VOCAL_TRACT) {
							if (!baseParams.isResynthesizeVocalTractFromSourceModel)
								smoothingFile.writeSingle(((WeightedCodebookLsfMatch) match).entry.targetItem.lsfs);
							else
								smoothingFile.writeSingle(((WeightedCodebookLsfMatch) match).entry.sourceItem.lsfs);
						} else if (baseParams.smoothingState == SmoothingDefinitions.TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT) {
							if (!baseParams.isResynthesizeVocalTractFromSourceModel)
								((WeightedCodebookLsfMatch) match).entry.targetItem.setLsfs(smoothedVocalTract[smoothedInd]);
							else
								((WeightedCodebookLsfMatch) match).entry.sourceItem.setLsfs(smoothedVocalTract[smoothedInd]);
						}
					}
					//

					if (match instanceof WeightedCodebookLsfMatch) {
						if (!baseParams.isResynthesizeVocalTractFromSourceModel) {
							if (!baseParams.lsfParams.isBarkScaled)
								targetLpcs = LsfAnalyser
										.lsfInHz2lpc(((WeightedCodebookLsfMatch) match).entry.targetItem.lsfs, fs);
							else
								targetLpcs = LsfAnalyser.lsfInBark2lpc(((WeightedCodebookLsfMatch) match).entry.targetItem.lsfs,
										fs);
						} else {
							if (!baseParams.lsfParams.isBarkScaled)
								targetLpcs = LsfAnalyser
										.lsfInHz2lpc(((WeightedCodebookLsfMatch) match).entry.sourceItem.lsfs, fs);
							else
								targetLpcs = LsfAnalyser.lsfInBark2lpc(((WeightedCodebookLsfMatch) match).entry.sourceItem.lsfs,
										fs);
						}
					} else if (match instanceof JointGMMMatch) {
						if (!baseParams.isResynthesizeVocalTractFromSourceModel) {
							if (!baseParams.lsfParams.isBarkScaled)
								targetLpcs = LsfAnalyser.lsfInHz2lpc(((JointGMMMatch) match).outputFeatures, fs);
							else
								targetLpcs = LsfAnalyser.lsfInBark2lpc(((JointGMMMatch) match).outputFeatures, fs);
						} else {
							if (!baseParams.lsfParams.isBarkScaled)
								targetLpcs = LsfAnalyser.lsfInHz2lpc(((JointGMMMatch) match).mappedSourceFeatures, fs);
							else
								targetLpcs = LsfAnalyser.lsfInBark2lpc(((JointGMMMatch) match).mappedSourceFeatures, fs);
						}
					} else if (match instanceof LsfMatch) {
						if (!baseParams.lsfParams.isBarkScaled)
							targetLpcs = LsfAnalyser.lsfInHz2lpc(((LsfMatch) match).lsfs, fs);
						else
							targetLpcs = LsfAnalyser.lsfInBark2lpc(((LsfMatch) match).lsfs, fs);
					}

					if (frmSize != newFrmSize) {
						if (outputExpTerm == null || newMaxFreq * baseParams.lsfParams.dimension != outputExpTerm.real.length)
							outputExpTerm = LpcAnalyser.calcExpTerm(newFrmSize, baseParams.lsfParams.dimension);

						targetVocalTractSpectrumEstimate = LpcAnalyser.calcSpecLinearFromOneMinusA(targetLpcs, 1.0f, newFrmSize,
								outputExpTerm);
					} else
						targetVocalTractSpectrumEstimate = LpcAnalyser.calcSpecLinearFromOneMinusA(targetLpcs, 1.0f, newFrmSize,
								inputExpTerm);

					for (k = 0; k < newMaxFreq; k++)
						targetVocalTractSpectrumEstimate[k] *= sqrtInputGain;
				}

				// For checking
				if (bShowSpectralPlots && psFrm.getCurrentTime() >= desiredFrameTime && baseParams.isVocalTractTransformation) {
					tmpSpec = new double[newMaxFreq];
					System.arraycopy(targetVocalTractSpectrumEstimate, 0, tmpSpec, 0, tmpSpec.length);
					tmpSpec = MathUtils.amp2db(tmpSpec);
					DisplayUtils.plot(tmpSpec, "5.Target Vocal Tract Estimate");
				}
				//

				outputVocalTractSpectrum = new double[newMaxFreq];
				interpolatedInputVocalTractSpectrum = MathUtils.interpolate(inputVocalTractSpectrum, newMaxFreq);

				if (baseParams.isVocalTractTransformation) {
					if (baseParams.isSourceVocalTractSpectrumFromModel) {
						for (k = 0; k < newMaxFreq; k++)
							outputVocalTractSpectrum[k] = targetVocalTractSpectrumEstimate[k]
									/ sourceVocalTractSpectrumEstimate[k] * interpolatedInputVocalTractSpectrum[k];
					} else {
						for (k = 0; k < newMaxFreq; k++)
							outputVocalTractSpectrum[k] = targetVocalTractSpectrumEstimate[k];
					}
				} else {
					for (k = 0; k < newMaxFreq; k++)
						outputVocalTractSpectrum[k] = interpolatedInputVocalTractSpectrum[k];
				}

				// MaryUtils.plot(MathUtils.amp2db(inputVocalTractSpectrum));
				// MaryUtils.plot(MathUtils.amp2db(interpolatedInputVocalTractSpectrum));

				// Estimate transformation filter
				if (baseParams.isVocalTractTransformation) {
					transformationFilter = new double[newMaxFreq];

					if (baseParams.isSourceVocalTractSpectrumFromModel) {
						for (k = 0; k < newMaxFreq; k++)
							transformationFilter[k] = targetVocalTractSpectrumEstimate[k] / sourceVocalTractSpectrumEstimate[k];
					} else {
						for (k = 0; k < newMaxFreq; k++)
							transformationFilter[k] = targetVocalTractSpectrumEstimate[k]
									/ interpolatedInputVocalTractSpectrum[k];
					}
					//

					// Smoothing
					if (baseParams.smoothingMethod == SmoothingDefinitions.TRANSFORMATION_FILTER_SMOOTHING) {
						if (baseParams.smoothingState == SmoothingDefinitions.ESTIMATING_SMOOTHED_VOCAL_TRACT) {
							smoothingFile.writeSingle(transformationFilter);

							// For checking
							if (bShowSpectralPlots && psFrm.getCurrentTime() >= desiredFrameTime) {
								tmpSpec = new double[newMaxFreq];
								System.arraycopy(transformationFilter, 0, tmpSpec, 0, tmpSpec.length);
								tmpSpec = MathUtils.amp2db(tmpSpec);
								DisplayUtils.plot(tmpSpec, "6.Transformation filter");
							}
						} else if (baseParams.smoothingState == SmoothingDefinitions.TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT) {
							if (baseParams.isSourceVocalTractSpectrumFromModel) {
								for (k = 0; k < newMaxFreq; k++)
									outputVocalTractSpectrum[k] = smoothedVocalTract[smoothedInd][k]
											* sourceVocalTractSpectrumEstimate[k];
							} else {
								for (k = 0; k < newMaxFreq; k++)
									outputVocalTractSpectrum[k] = smoothedVocalTract[smoothedInd][k]
											* interpolatedInputVocalTractSpectrum[k];
							}

							// For checking
							if (bShowSpectralPlots && psFrm.getCurrentTime() >= desiredFrameTime) {
								tmpSpec = new double[newMaxFreq];
								System.arraycopy(smoothedVocalTract[smoothedInd], 0, tmpSpec, 0, tmpSpec.length);
								tmpSpec = MathUtils.amp2db(tmpSpec);
								DisplayUtils.plot(tmpSpec, "6.Smoothed transformation filter");
							}
						} else {
							// For checking
							if (bShowSpectralPlots && psFrm.getCurrentTime() >= desiredFrameTime) {
								tmpSpec = new double[newMaxFreq];
								System.arraycopy(transformationFilter, 0, tmpSpec, 0, tmpSpec.length);
								tmpSpec = MathUtils.amp2db(tmpSpec);
								DisplayUtils.plot(tmpSpec, "6.Transformation filter");
							}
						}
					} else {
						// For checking
						if (bShowSpectralPlots && psFrm.getCurrentTime() >= desiredFrameTime) {
							tmpSpec = new double[newMaxFreq];
							System.arraycopy(transformationFilter, 0, tmpSpec, 0, tmpSpec.length);
							tmpSpec = MathUtils.amp2db(tmpSpec);
							DisplayUtils.plot(tmpSpec, "6.Transformation filter");
						}
					}
				}
				//

				// Perform additional vocal tract scaling
				if (bWarp) {
					tmpvsc[0] = vscale;
					newVScales = MathUtils.modifySize(tmpvsc, newMaxFreq); // Modify length to match current length of spectrum

					for (k = 0; k < newVScales.length; k++) {
						if (newVScales[k] < 0.05) // Put a floor to avoid divide by zero
							newVScales[k] = 0.05;
					}

					warpedOutputVocalTractSpectrum = new double[newMaxFreq];

					for (k = 0; k < newMaxFreq; k++) {
						wInd = (int) Math.floor((k + 1) / newVScales[k] + 0.5); // Find new indices
						if (wInd < 1)
							wInd = 1;
						if (wInd > newMaxFreq)
							wInd = newMaxFreq;

						warpedOutputVocalTractSpectrum[k] = outputVocalTractSpectrum[wInd - 1];
					}

					System.arraycopy(warpedOutputVocalTractSpectrum, 0, outputVocalTractSpectrum, 0, newMaxFreq);
				}

				// Create output DFT spectrum
				outputResidual = new ComplexArray(newFrmSize);
				outputResidual.real = MathUtils.zeros(newFrmSize);
				outputResidual.imag = MathUtils.zeros(newFrmSize);

				System.arraycopy(inputResidual.real, 0, outputResidual.real, 0, Math.min(maxFreq, newFrmSize));
				System.arraycopy(inputResidual.imag, 0, outputResidual.imag, 0, Math.min(maxFreq, newFrmSize));

				// Copy & paste samples if required (COMPLEX VERSION TO SUPPORT PSCALE<=0.5)
				// This version fills the spectrum by flipping and pasting the original freq bins as many times as required.
				kMax = 1;
				while (newMaxFreq > (kMax + 1) * (maxFreq - 2))
					kMax++;

				for (k = 1; k <= kMax; k++) {
					tmpFix = (maxFreq - 2) * k;
					if (k % 2 == 1) // Odd mode
					{
						tmpAdd = maxFreq + 2;
						tmpMul = 1;
					} else {
						tmpAdd = -1;
						tmpMul = -1;
					}

					for (j = tmpFix + 3; j <= Math.min(newMaxFreq, maxFreq + tmpFix); j++) {
						outputResidual.real[j - 1] = inputResidual.real[tmpMul * (tmpFix - j) + tmpAdd - 1];
						outputResidual.imag[j - 1] = inputResidual.imag[tmpMul * (tmpFix - j) + tmpAdd - 1];
					}
				}

				outputResidual.real[newMaxFreq - 1] = Math.sqrt(outputResidual.real[newMaxFreq - 1]
						* outputResidual.real[newMaxFreq - 1] + outputResidual.imag[newMaxFreq - 1]
						* outputResidual.imag[newMaxFreq - 1]);
				outputResidual.imag[newMaxFreq - 1] = 0.0;

				// For checking
				if (bShowSpectralPlots && psFrm.getCurrentTime() >= desiredFrameTime) {
					tmpComp = new ComplexArray(outputResidual);
					tmpSpec = MathUtils.dft2ampdb(tmpComp, 0, newMaxFreq - 1);
					DisplayUtils.plot(tmpSpec, "7.Output Residual");
				}
				//

				// Filter the output residual with the estimated target vocal tract spectrum
				outputDft = new ComplexArray(newFrmSize);

				// Smoothing
				if (baseParams.smoothingMethod == SmoothingDefinitions.OUTPUT_VOCALTRACTSPECTRUM_SMOOTHING) {
					if (baseParams.smoothingState == SmoothingDefinitions.ESTIMATING_SMOOTHED_VOCAL_TRACT) {
						smoothingFile.writeSingle(outputVocalTractSpectrum, newMaxFreq);
					} else if (baseParams.smoothingState == SmoothingDefinitions.TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT) {
						for (k = 0; k < newMaxFreq; k++)
							outputVocalTractSpectrum[k] = smoothedVocalTract[smoothedInd][k];
					}
				}
				//

				// For checking
				if (bShowSpectralPlots && psFrm.getCurrentTime() >= desiredFrameTime) {
					tmpSpec = new double[newMaxFreq];
					System.arraycopy(outputVocalTractSpectrum, 0, tmpSpec, 0, tmpSpec.length);
					tmpSpec = MathUtils.amp2db(tmpSpec);
					DisplayUtils.plot(tmpSpec, "8.Output Vocal Tract");
				}
				//

				for (k = 1; k <= newMaxFreq; k++) {
					outputDft.real[k - 1] = outputResidual.real[k - 1] * outputVocalTractSpectrum[k - 1];
					outputDft.imag[k - 1] = outputResidual.imag[k - 1] * outputVocalTractSpectrum[k - 1];
				}

				for (k = newMaxFreq + 1; k <= newFrmSize; k++) {
					outputDft.real[k - 1] = outputDft.real[2 * newMaxFreq - 1 - k];
					outputDft.imag[k - 1] = -outputDft.imag[2 * newMaxFreq - 1 - k];
				}

				// For checking
				if (bShowSpectralPlots && psFrm.getCurrentTime() >= desiredFrameTime) {
					tmpComp = new ComplexArray(outputDft);
					tmpSpec = MathUtils.dft2ampdb(tmpComp, 0, newMaxFreq);
					DisplayUtils.plot(tmpSpec, "9.Output DFT");
					bShowSpectralPlots = false;
				}
				//

				// Convert back to time domain
				outputDft = FFTMixedRadix.ifft(outputDft);

				frmy = new double[newFrmSize];
				System.arraycopy(outputDft.real, 0, frmy, 0, newFrmSize);
			} else {
				if (frmSize < newFrmSize)
					newFrmSize = frmSize;

				frmy = new double[newFrmSize];
			}

			frmy = SignalProcUtils.removePreemphasis(frmy, baseParams.lsfParams.preCoef);

			frmyEn = SignalProcUtils.getEnergy(frmy);

			gain = (frmEn / Math.sqrt(frmSize)) / (frmyEn / Math.sqrt(newFrmSize)) * escale;

			if (!(isVoiced && pscale != 1.0) && !bWarp && !isTransformUnvoiced) {
				for (k = 0; k < frmSize; k++)
					frmy[k] = frm[k] * wgt[k];
			}

			// Energy scale compensation + modification
			for (k = 0; k < newFrmSize; k++)
				frmy[k] *= gain;

			for (j = 1; j <= repeatSkipCount + 1; j++) {
				if (!baseParams.isFixedRateVocalTractConversion) {
					if (isVoiced)
						newSkipSize = (int) Math.floor(currentPeriod / pscale + 0.5);
					else
						newSkipSize = (int) Math.floor(currentPeriod + 0.5);
				} else
					newSkipSize = (int) Math.floor(ssFixedInSeconds * fs + 0.5);

				if ((isLastInputFrame && j == repeatSkipCount + 1)) // | (i~=numfrm & all(repeatSkipCounts(i+1:numfrm)==-1)))
					bLastFrame = true;
				else
					bLastFrame = false;

				synthFrameInd++;

				wgty = windowOut.values(newFrmSize);

				if (synthFrameInd == 1) // First frame: Do not window the first half of output speech frame to prevent overflow in
										// normalization with hanning coeffs
				{
					halfWin = (int) Math.floor(newFrmSize / 2.0 + 0.5);
					synthTotal = synthSt + newFrmSize;

					// Keep output in an overlap-add buffer
					if (ySynthInd + newFrmSize - 1 <= maxNewFrmSize) {
						for (k = ySynthInd; k <= ySynthInd + halfWin - 1; k++) {
							ySynthBuff[k - 1] = frmy[k - ySynthInd];
							wSynthBuff[k - 1] = 1.0;
						}

						for (k = ySynthInd + halfWin; k <= ySynthInd + newFrmSize - 1; k++) {
							ySynthBuff[k - 1] += frmy[k - ySynthInd] * wgty[k - ySynthInd];
							wSynthBuff[k - 1] += wgty[k - ySynthInd] * wgty[k - ySynthInd];
						}
					} else {
						for (k = ySynthInd; k <= maxNewFrmSize; k++) {
							if (k - ySynthInd < halfWin) {
								ySynthBuff[k - 1] = frmy[k - ySynthInd];
								wSynthBuff[k - 1] = 1.0;
							} else {
								ySynthBuff[k - 1] += frmy[k - ySynthInd] * wgty[k - ySynthInd];
								wSynthBuff[k - 1] += wgty[k - ySynthInd] * wgty[k - ySynthInd];
							}
						}

						for (k = 1; k <= newFrmSize - 1 - maxNewFrmSize + ySynthInd; k++) {
							if (maxNewFrmSize - ySynthInd + k < halfWin) {
								ySynthBuff[k - 1] = frmy[maxNewFrmSize - ySynthInd + k];
								wSynthBuff[k - 1] = 1.0;
							} else {
								ySynthBuff[k - 1] += frmy[maxNewFrmSize - ySynthInd + k] * wgty[maxNewFrmSize - ySynthInd + k];
								wSynthBuff[k - 1] += wgty[maxNewFrmSize - ySynthInd + k] * wgty[maxNewFrmSize - ySynthInd + k];
							}
						}
					}
					//

					if (!bSilent)
						System.out.println("Synthesized using frame " + String.valueOf(inputFrameIndex + 1));
				} else if (bLastFrame) // Last frame: Do not window the second half of output speech frame to prevent overflow in
										// normalization with hanning coeffs
				{
					halfWin = (int) Math.floor(newFrmSize / 2.0 + 0.5);
					remain = newFrmSize - halfWin;
					synthTotal = synthSt + halfWin + remain - 1;

					// Keep output in an overlap-add buffer
					if (ySynthInd + newFrmSize - 1 <= maxNewFrmSize) {
						for (k = ySynthInd; k <= ySynthInd + halfWin - 1; k++) {
							ySynthBuff[k - 1] += frmy[k - ySynthInd] * wgty[k - ySynthInd];
							wSynthBuff[k - 1] += wgty[k - ySynthInd] * wgty[k - ySynthInd];
						}

						for (k = ySynthInd + halfWin; k <= ySynthInd + newFrmSize - 1; k++) {
							ySynthBuff[k - 1] += frmy[k - ySynthInd];
							wSynthBuff[k - 1] = 1.0;
						}
					} else {
						for (k = ySynthInd; k <= maxNewFrmSize; k++) {
							if (k - ySynthInd < halfWin) {
								ySynthBuff[k - 1] += frmy[k - ySynthInd] * wgty[k - ySynthInd];
								wSynthBuff[k - 1] += wgty[k - ySynthInd] * wgty[k - ySynthInd];
							} else {
								ySynthBuff[k - 1] += frmy[k - ySynthInd];
								wSynthBuff[k - 1] = 1.0;
							}
						}

						for (k = 1; k <= newFrmSize - 1 - maxNewFrmSize + ySynthInd; k++) {
							if (maxNewFrmSize - ySynthInd + k < halfWin) {
								ySynthBuff[k - 1] += frmy[maxNewFrmSize - ySynthInd + k] * wgty[maxNewFrmSize - ySynthInd + k];
								wSynthBuff[k - 1] += wgty[maxNewFrmSize - ySynthInd + k] * wgty[maxNewFrmSize - ySynthInd + k];
							} else {
								ySynthBuff[k - 1] += frmy[maxNewFrmSize - ySynthInd + k];
								wSynthBuff[k - 1] = 1.0;
							}
						}
					}
					//

					if (!bSilent)
						System.out.println("Synthesized using frame " + String.valueOf(inputFrameIndex + 1));
				} else // Normal frame
				{
					if (!isVoiced && ((repeatSkipCount % 2) == 1)) // Reverse unvoiced repeated frames once in two consecutive
																	// repetitions to reduce distortion
						frmy = SignalProcUtils.reverse(frmy);

					synthTotal = synthSt + newFrmSize;

					// Keep output in an overlap-add buffer
					if (ySynthInd + newFrmSize - 1 <= maxNewFrmSize) {
						for (k = ySynthInd; k <= ySynthInd + newFrmSize - 1; k++) {
							ySynthBuff[k - 1] += frmy[k - ySynthInd] * wgty[k - ySynthInd];
							wSynthBuff[k - 1] += wgty[k - ySynthInd] * wgty[k - ySynthInd];
						}
					} else {
						for (k = ySynthInd; k <= maxNewFrmSize; k++) {
							ySynthBuff[k - 1] += frmy[k - ySynthInd] * wgty[k - ySynthInd];
							wSynthBuff[k - 1] += wgty[k - ySynthInd] * wgty[k - ySynthInd];
						}

						for (k = 1; k <= newFrmSize - 1 - maxNewFrmSize + ySynthInd; k++) {
							ySynthBuff[k - 1] += frmy[k + maxNewFrmSize - ySynthInd] * wgty[k + maxNewFrmSize - ySynthInd];
							wSynthBuff[k - 1] += wgty[k + maxNewFrmSize - ySynthInd] * wgty[k + maxNewFrmSize - ySynthInd];
						}
					}
					//

					if (!bSilent) {
						if (j == 1)
							System.out.println("Synthesized using frame " + String.valueOf(inputFrameIndex + 1));
						else
							System.out.println("Repeated using frame " + String.valueOf(inputFrameIndex + 1));
					}
				}

				// Write to output buffer
				for (k = 0; k <= newSkipSize - 1; k++) {
					kInd = (k + ySynthInd) % maxNewFrmSize;
					if (kInd == 0)
						kInd = maxNewFrmSize;

					if (wSynthBuff[kInd - 1] > 0.0)
						outBuff[outBuffStart - 1] = ySynthBuff[kInd - 1] / wSynthBuff[kInd - 1];
					else
						outBuff[outBuffStart - 1] = ySynthBuff[kInd - 1];

					ySynthBuff[kInd - 1] = 0.0;
					wSynthBuff[kInd - 1] = 0.0;

					outBuffStart++;

					if (outBuffStart > outBuffLen) {
						if (tscaleSingle != 1.0 || totalWrittenToFile + outBuffLen <= origLen) {
							dout.writeDouble(outBuff, 0, outBuffLen);
							totalWrittenToFile += outBuffLen;
						} else {
							dout.writeDouble(outBuff, 0, origLen - totalWrittenToFile);
							totalWrittenToFile = origLen;
						}

						outBuffStart = 1;
					}
				}
				//

				synthSt += newSkipSize;

				// if (!bLastFrame)
				// {
				if (ySynthInd + newSkipSize <= maxNewFrmSize)
					ySynthInd += newSkipSize;
				else
					ySynthInd += newSkipSize - maxNewFrmSize;
				// }
				// ///////

				if (bLastFrame) {
					bBroke = true;
					break;
				}
			}
		} else {
			if (!bSilent)
				System.out.println("Skipped frame " + String.valueOf(inputFrameIndex + 1));
		}

		inputFrameIndex++;

		return output;
	}

	public double[] writeFinal() throws IOException {
		double[] output = null;
		double[] outputTmp = null;

		int k, kInd;

		if (tscaleSingle == 1.0)
			synthTotal = origLen;

		if (outBuffLen > synthTotal)
			outBuffLen = synthTotal;

		// Write the final segment
		for (k = synthSt; k <= synthTotal; k++) {
			kInd = (k - synthSt + ySynthInd) % maxNewFrmSize;

			if (kInd == 0)
				kInd = maxNewFrmSize;

			if (wSynthBuff[kInd - 1] > 0.0)
				outBuff[outBuffStart - 1] = ySynthBuff[kInd - 1] / wSynthBuff[kInd - 1];
			else
				outBuff[outBuffStart - 1] = ySynthBuff[kInd - 1];

			ySynthBuff[kInd - 1] = 0.0;
			wSynthBuff[kInd - 1] = 0.0;

			outBuffStart++;

			if (outBuffStart > outBuffLen) {
				if (tscaleSingle != 1.0 || totalWrittenToFile + outBuffLen <= origLen) {
					dout.writeDouble(outBuff, 0, outBuffLen);
					totalWrittenToFile += outBuffLen;
				} else {
					dout.writeDouble(outBuff, 0, origLen - totalWrittenToFile);
					totalWrittenToFile = origLen;
				}
				outBuffStart = 1;
			}
		}

		if (outBuffStart > 1) {
			if (tscaleSingle != 1.0 || totalWrittenToFile + outBuffStart - 1 <= origLen) {
				dout.writeDouble(outBuff, 0, outBuffStart - 1);
				totalWrittenToFile += outBuffStart - 1;
			} else {
				dout.writeDouble(outBuff, 0, origLen - totalWrittenToFile);
				totalWrittenToFile = origLen;
			}
		}
		//

		if (dout != null)
			dout.close();

		return output;
	}

	public void convertToWav(AudioFormat audioformat) throws IOException {
		// Read the temp binary file into a wav file and delete the temp binary file
		if (tempOutBinaryFile != null) {
			double[] yOut = null;

			din = new LEDataInputStream(tempOutBinaryFile);
			yOut = din.readDouble(totalWrittenToFile);
			din.close();

			double tmpMax = MathUtils.getAbsMax(yOut);
			if (tmpMax > 1.0) {
				for (int n = 0; n < yOut.length; n++)
					yOut[n] /= tmpMax;
			}

			outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(yOut), audioformat);
			AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outputFile));

			FileUtils.delete(tempOutBinaryFile);
			//
		}
	}
}
