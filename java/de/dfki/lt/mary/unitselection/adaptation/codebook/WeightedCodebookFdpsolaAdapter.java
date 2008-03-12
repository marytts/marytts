/**
 * Copyright 2007 DFKI GmbH.
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

package de.dfki.lt.mary.unitselection.adaptation.codebook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.unitselection.adaptation.Context;
import de.dfki.lt.mary.unitselection.adaptation.prosody.PitchStatistics;
import de.dfki.lt.mary.unitselection.adaptation.prosody.PitchTransformer;
import de.dfki.lt.mary.unitselection.adaptation.prosody.ProsodyTransformerParams;
import de.dfki.lt.mary.unitselection.adaptation.smoothing.SmoothingDefinitions;
import de.dfki.lt.mary.unitselection.adaptation.smoothing.SmoothingFile;
import de.dfki.lt.mary.unitselection.adaptation.smoothing.TemporalSmoother;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.signalproc.FFT;
import de.dfki.lt.signalproc.FFTMixedRadix;
import de.dfki.lt.signalproc.analysis.F0ReaderWriter;
import de.dfki.lt.signalproc.analysis.LPCAnalyser;
import de.dfki.lt.signalproc.analysis.LineSpectralFrequencies;
import de.dfki.lt.signalproc.analysis.Lsfs;
import de.dfki.lt.signalproc.analysis.PitchMarker;
import de.dfki.lt.signalproc.analysis.LPCAnalyser.LPCoeffs;
import de.dfki.lt.signalproc.process.FDPSOLAProcessor;
import de.dfki.lt.signalproc.process.PSOLAFrameProvider;
import de.dfki.lt.signalproc.process.VoiceModificationParametersPreprocessor;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.ESTLabels;
import de.dfki.lt.signalproc.util.InterpolationUtils;
import de.dfki.lt.signalproc.util.LEDataInputStream;
import de.dfki.lt.signalproc.util.LEDataOutputStream;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.util.MathUtils.Complex;
import de.dfki.lt.signalproc.window.DynamicWindow;
import de.dfki.lt.signalproc.window.Window;

/**
 * @author oytun.turk
 *
 * A class that supports voice conversion through weighted codebook mapping and FDPSOLA based
 * prosody and vocal tract modifications
 * 
 */
public class WeightedCodebookFdpsolaAdapter {
    
    protected DoubleDataSource input;
    protected AudioInputStream inputAudio;
    protected DDSAudioInputStream outputAudio;
    protected VoiceModificationParametersPreprocessor modParams;
    protected int numfrm;
    protected int numfrmFixed;
    protected int lpOrder; //LP analysis order
    protected String outputFile;
    protected String tempOutBinaryFile;
    protected int origLen;
    protected PitchMarker pm;
    protected double[] f0s;
    protected PSOLAFrameProvider psFrm;
    protected double wsFixedInSeconds;
    protected double ssFixedInSeconds;
    protected int numPeriods;
    protected static int NUM_PITCH_SYNC_PERIODS = 3;

    public boolean bSilent;
    protected LEDataOutputStream dout; //Output stream for big-endian wav tests
    protected LEDataInputStream din; //Input stream for big-endian wav tests
    protected DynamicWindow windowIn;
    protected DynamicWindow windowOut;
    protected double [] wgt;
    protected double [] wgty;

    protected int frmSize;
    protected int newFrmSize;
    protected int newPeriod;
    protected int synthFrmInd;
    protected double localDurDiff;
    protected int repeatSkipCount; // -1:skip frame, 0:no repetition (use synthesized frame as it is), >0: number of repetitions for synthesized frame
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
    protected int newFftSize;
    protected int newMaxFreq;

    protected int outBuffLen;
    protected double [] outBuff;
    protected int outBuffStart;
    protected int totalWrittenToFile;

    protected double [] ySynthBuff;
    protected double [] wSynthBuff;
    protected int ySynthInd;
    protected double [] frm;
    protected boolean bWarp;

    protected double [] inputVT;
    protected double [] py2;
    protected Complex hy;
    protected double [] frmy;
    protected double frmEn;
    protected double frmyEn;
    protected double gain;
    protected int newSkipSize;
    protected int halfWin;
    protected double [] newVScales;
    protected double [] tmpvsc;
    //protected boolean isWavFileOutput;
    protected int inputFrameIndex;
    protected static double MIN_PSCALE = 0.1;
    protected static double MAX_PSCALE = 5.0;
    protected static double MIN_TSCALE = 0.1;
    protected static double MAX_TSCALE = 5.0;
    protected int fs;
    protected int fftSize;
    protected int maxFreq;

    protected double tscaleSingle;

    private double desiredFrameTime;
    private boolean bShowSpectralPlots;

    private PitchTransformer pitchTransformer;

    private SmoothingFile smoothingFile;
    private double[][] smoothedVocalTract;
    private int smoothedInd;

    private int[] preselectedIndices;
    private int[] allIndices;
    private ESTLabels labels;
    private int currentLabelIndex;
    
    private WeightedCodebookTransformerParams wctParams;

    public WeightedCodebookFdpsolaAdapter(
            String strInputFile, String strPitchFile, String strLabelFile, String strOutputFile, 
            WeightedCodebookTransformerParams wctParamsIn,
            double [] pscales, double [] tscales, double [] escales, double [] vscales
    ) throws UnsupportedAudioFileException, IOException
    {
        wctParams = new WeightedCodebookTransformerParams(wctParamsIn);
        
        init(strInputFile, strPitchFile, strLabelFile, strOutputFile,
             pscales, tscales, escales, vscales);
    }

    public void init(String strInputFile, String strPitchFile, String strLabelFile, String strOutputFile,
                        double [] pscales, double [] tscales, double [] escales, double [] vscales)
    {
        //Smoothing
        smoothingFile = null;
        if (wctParams.smoothingState==SmoothingDefinitions.NONE)
            wctParams.smoothedVocalTractFile = "";
        if (wctParams.smoothingState==SmoothingDefinitions.ESTIMATING_SMOOTHED_VOCAL_TRACT)
        {
            if (wctParams.smoothedVocalTractFile=="")
                throw new IllegalArgumentException("smoothedVocalTractFile not valid");
            else
            {
                smoothingFile = new SmoothingFile(wctParams.smoothedVocalTractFile, SmoothingFile.OPEN_FOR_WRITE);
                smoothingFile.smoothingMethod = wctParams.smoothingMethod;
                smoothingFile.writeHeader();
            }
        }

        if (wctParams.smoothingState==SmoothingDefinitions.TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT &&
                wctParams.smoothingMethod!=SmoothingDefinitions.NO_SMOOTHING)
        {
            if (!FileUtils.exists(wctParams.smoothedVocalTractFile))
                throw new IllegalArgumentException("smoothedVocalTractFile not found");
            else
            {
                smoothingFile = new SmoothingFile(wctParams.smoothedVocalTractFile, SmoothingFile.OPEN_FOR_READ);
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

        numfrm = 0; //Total pitch synchronous frames (This is the actual number of frames to be processed)
        numfrmFixed = 0; //Total frames if the analysis was fixed skip-rate

        modParams = null;

        outputFile = null; 

        tscaleSingle = 1.0;

        boolean bContinue = true;

        if (!FileUtils.exists(strInputFile))
        {
            System.out.println("Error! Pitch file " + strInputFile + " not found.");
            bContinue = false;
        }

        if (!FileUtils.exists(strPitchFile))
        {
            System.out.println("Error! Pitch file " + strPitchFile + " not found.");
            bContinue = false;
        }

        if (strOutputFile==null || strOutputFile=="")
        {
            System.out.println("Invalid output file...");
            bContinue = false;
        }

        if (bContinue)
        {
            try {
                inputAudio = AudioSystem.getAudioInputStream(new File(strInputFile));
            } catch (UnsupportedAudioFileException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            input = new AudioDoubleDataSource(inputAudio);

            origLen = (int)input.getDataLength();
            fs = (int)inputAudio.getFormat().getSampleRate();

            F0ReaderWriter f0 = new F0ReaderWriter(strPitchFile);
            pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, fs, origLen, f0.header.ws, f0.header.ss, true);

            numfrmFixed = (int)(Math.floor(((double)(origLen + pm.totalZerosToPadd)/fs-0.5*wsFixedInSeconds)/ssFixedInSeconds+0.5)+2); //Total frames if the analysis was fixed skip-rate
            if (!wctParams.isFixedRateVocalTractConversion)
                numfrm = pm.pitchMarks.length-numPeriods; //Total pitch synchronous frames (This is the actual number of frames to be processed)
            else
                numfrm = numfrmFixed;

            f0s = SignalProcUtils.fixedRateF0Values(pm, wsFixedInSeconds, ssFixedInSeconds, numfrmFixed, fs);

            lpOrder = SignalProcUtils.getLPOrder(fs);

            modParams = new VoiceModificationParametersPreprocessor(fs, lpOrder,
                    pscales, tscales, escales, vscales,
                    pm.pitchMarks, wsFixedInSeconds, ssFixedInSeconds,
                    numfrm, numfrmFixed, numPeriods, wctParams.isFixedRateVocalTractConversion);
            tscaleSingle = modParams.tscaleSingle;

            outputFile = strOutputFile;    
            
            if (strLabelFile!="" && FileUtils.exists(strLabelFile))
                labels = new ESTLabels(strLabelFile);
            else
                labels = null;
        }

        if (bContinue)
        {
            tmpvsc = new double[1];
            bSilent = false;

            if (outputFile != null)
                tempOutBinaryFile = outputFile + ".bin";

            if (!wctParams.isFixedRateVocalTractConversion)
                psFrm = new PSOLAFrameProvider(input, pm, modParams.fs, modParams.numPeriods);
            else
                psFrm = new PSOLAFrameProvider(input, wsFixedInSeconds, ssFixedInSeconds, modParams.fs, numfrm);

            try {
                dout = new LEDataOutputStream(tempOutBinaryFile);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            windowIn = new DynamicWindow(Window.HANN);
            windowOut = new DynamicWindow(Window.HANN);

            frmSize = 0;
            newFrmSize = 0;
            newPeriod = 0;
            synthFrmInd = 0;
            localDurDiff = 0.0;
            repeatSkipCount = 0; // -1:skip frame, 0:no repetition (use synthesized frame as it is), >0: number of repetitions for synthesized frame
            localDurDiffSaved = 0.0;
            sumLocalDurDiffs = 0.0;
            nextAdd = 0.0;

            synthSt = pm.pitchMarks[0];

            synthTotal = 0;

            maxFrmSize = (int)(numPeriods*fs/40.0);
            if ((maxFrmSize % 2) != 0)
                maxFrmSize++;

            maxNewFrmSize = (int)(Math.floor(maxFrmSize/MIN_PSCALE+0.5));
            if ((maxNewFrmSize % 2) != 0) 
                maxNewFrmSize++;

            synthFrameInd = 0;
            bLastFrame = false;
            bBroke = false;
            fftSize = (int)Math.pow(2, (Math.ceil(Math.log((double)maxFrmSize)/Math.log(2.0))));
            maxFreq = fftSize/2+1;

            outBuffLen = 500000;
            outBuff = MathUtils.zeros(outBuffLen);
            outBuffStart = 1;
            totalWrittenToFile = 0;

            ySynthBuff = MathUtils.zeros(maxNewFrmSize);
            wSynthBuff = MathUtils.zeros(maxNewFrmSize);
            ySynthInd = 1;
        }
    }

    public void fdpsolaOnline(WeightedCodebookMapper mapper,
                              WeightedCodebook codebook) throws IOException
    {   
        int i;
        double [] frmIn;
        boolean isLastInputFrame;
        int inputFrameSize;
        int currentPeriod;

        desiredFrameTime = 1.06;
        bShowSpectralPlots = false;

        fs = (int)inputAudio.getFormat().getSampleRate();

        PitchStatistics inputF0Statistics = new PitchStatistics(wctParams.prosodyParams.pitchStatisticsType, f0s);

        double[] targetF0s = pitchTransformer.transform(wctParams.prosodyParams,  
                codebook.f0StatisticsMapping,
                inputF0Statistics, 
                f0s,
                modParams.pscalesVar);

        preselectedIndices = null;
        allIndices = null;
        if (codebook!=null && !wctParams.isContextBasedPreselection)
        {
            //Whole codebook
            allIndices = new int[codebook.lsfEntries.length];
            for (i=0; i<allIndices.length; i++)
                allIndices[i] = i;
        }

        inputFrameIndex = 0;
        for (i=0; i<numfrm; i++)
        {   
            frmIn = psFrm.getNextFrame();

            if (bBroke)
                break;

            if (i==numfrm-1)
                isLastInputFrame = true;
            else
                isLastInputFrame = false;

            if (!wctParams.isFixedRateVocalTractConversion)
            {
                currentPeriod = pm.pitchMarks[i+1]-pm.pitchMarks[i];
                inputFrameSize = pm.pitchMarks[i+modParams.numPeriods]-pm.pitchMarks[i]+1;
            }
            else
            {
                currentPeriod = -1;
                inputFrameSize = frmIn.length;
            }

            int index = (int)(Math.floor((psFrm.getCurrentTime()-0.5*wsFixedInSeconds)/ssFixedInSeconds+0.5));
            if (index<0)
                index=0;
            if (index>targetF0s.length-1)
                index=targetF0s.length-1;

            boolean isVoiced;
            if (!wctParams.isFixedRateVocalTractConversion)
                isVoiced = pm.vuvs[i];
            else
            {
                if (f0s[index]>10.0)
                    isVoiced=true;
                else
                    isVoiced=false;
            }

            double currentF0;
            if (isVoiced)
                currentF0 = fs/currentPeriod;
            else
                currentF0 = 0.0;
            
            if (labels!=null)
                currentLabelIndex = SignalProcUtils.time2LabelIndex(psFrm.getCurrentTime(), labels);
            else
                currentLabelIndex = -1;
            
            processFrame(frmIn, isVoiced, 
                    currentF0, targetF0s[index], modParams.tscalesVar[i], modParams.escalesVar[i], modParams.vscalesVar[i], 
                    isLastInputFrame, currentPeriod, inputFrameSize, mapper, codebook);

            if (wctParams.smoothingState==SmoothingDefinitions.TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT &&
                    wctParams.smoothingMethod!=SmoothingDefinitions.NO_SMOOTHING)
            {
                smoothedInd++;
                if (smoothedInd>smoothedVocalTract.length-1)
                    smoothedInd=smoothedVocalTract.length-1;
            }
        }

        writeFinal();

        convertToWav(inputAudio.getFormat());

        inputAudio.close();

        //Perform smoothing on the vocal tract parameter file
        if (wctParams.smoothingState==SmoothingDefinitions.ESTIMATING_SMOOTHED_VOCAL_TRACT)
        {
            System.out.println("Temporal smoothing started using " + String.valueOf(wctParams.smoothingNumNeighbours) + " neighbours...");
            smoothingFile.close();
            smoothingFile = new SmoothingFile(wctParams.smoothedVocalTractFile, SmoothingFile.OPEN_FOR_READ);
            double[][] vts = smoothingFile.readAll();

            double[] tmp1 = new double[vts.length];
            for (i=0; i<vts.length; i++)
                tmp1[i] = vts[i][20];

            vts = TemporalSmoother.smooth(vts, wctParams.smoothingNumNeighbours);

            double[] tmp2 = new double[vts.length];
            for (i=0; i<vts.length; i++)
                tmp2[i] = vts[i][20];

            smoothingFile = new SmoothingFile(wctParams.smoothedVocalTractFile, SmoothingFile.OPEN_FOR_WRITE, wctParams.smoothingMethod);
            smoothingFile.writeAll(vts);
            System.out.println("Temporal smoothing completed...");
        }
        else if (wctParams.smoothingState==SmoothingDefinitions.TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT)
            FileUtils.delete(wctParams.smoothedVocalTractFile);  
        //
    }

    //Voice conversion version
    public double [] processFrame(double [] frmIn, boolean isVoiced, 
            double currentF0, double targetF0, double tscale, double escale, double vscale, 
            boolean isLastInputFrame, int currentPeriod, int inputFrameSize,
            WeightedCodebookMapper mapper,
            WeightedCodebook codebook) throws IOException
   {   
        double pscale;

        if (currentF0>10.0)
            pscale = targetF0/currentF0;
        else
            pscale = 1.0;

        if (pscale<MIN_PSCALE)
            pscale = MIN_PSCALE;
        if (pscale>MAX_PSCALE)
            pscale = MAX_PSCALE;

        if (tscale<MIN_TSCALE)
            tscale = MIN_TSCALE;
        if (tscale>MAX_TSCALE)
            tscale = MAX_TSCALE;

        double [] output = null;
        double [] outputTmp = null;
        int j, k, wInd, kMax;
        int tmpFix, tmpAdd, tmpMul;
        int remain;
        int kInd;
        WeightedCodebookMatch codebookMatch = null;

        windowIn = new DynamicWindow(wctParams.lsfParams.windowType);
        windowOut = new DynamicWindow(wctParams.lsfParams.windowType);

        repeatSkipCount = 0; // -1:skip frame, 0:no repetition (use synthesized frame as it is), >0: number of repetitions for synthesized frame

        // Compute new frame sizes, change in durations due to pitch scaling, and required compensation amount in samples
        // &
        // Find out which pitch-scaled frames to repeat/skip for overall duration
        // compensation
        frmSize = inputFrameSize;
        if ((frmSize % 2) !=0) 
            frmSize++;
        if (frmSize<4)
            frmSize = 4;

        if (isVoiced)
        {
            newFrmSize = (int)(Math.floor(frmSize/pscale+0.5));
            if ((newFrmSize % 2) !=0)
                newFrmSize++;
            if (newFrmSize<4)
                newFrmSize = 4;
        }
        else
            newFrmSize = frmSize;

        newPeriod = (int)Math.floor(((double)newFrmSize)/NUM_PITCH_SYNC_PERIODS+0.5);
        //Compute duration compensation required:
        // localDurDiffs(i) = (DESIRED)-(AFTER PITCHSCALING)
        // (-) if expansion occured, (+) if compression occured
        // We aim to make this as close to zero as possible in the following duration compensation step
        localDurDiff = nextAdd + (frmSize*tscale-newFrmSize)/NUM_PITCH_SYNC_PERIODS;

        nextAdd = 0;
        if (localDurDiff<-0.1*newPeriod) //Expansion occured so skip this frame
        {
            repeatSkipCount--;
            if (!isLastInputFrame)
            {    
                nextAdd = localDurDiff+newPeriod;
                localDurDiff = 0;
            }
        }
        else if (localDurDiff>0.1*newPeriod) //Compression occured so repeat this frame
        {
            while (localDurDiff>0.1*newPeriod)
            {
                repeatSkipCount++;
                localDurDiff -= newPeriod;
            }

            if (!isLastInputFrame)
            {
                nextAdd = localDurDiff;
                localDurDiff = 0;
            }
        }

        sumLocalDurDiffs += localDurDiff;

        if (isLastInputFrame)
        {
            // Check the final length and perform additional repetitions if necessary
            localDurDiff = sumLocalDurDiffs;
            while (localDurDiff>0)
            {
                repeatSkipCount++;
                localDurDiff -= newPeriod;
            }
            //
        }

        if (isLastInputFrame)
        {
            repeatSkipCount++;
            bLastFrame = true;
        }

        double [] tmpSpec;
        Complex tmpComp;

        LPCoeffs inputLPCoeffs = null;
        double[] inputLpcs = null;
        double[] inputLsfs = null; 
        double sqrtInputGain; 
        double [] targetLpcs = null;

        Complex inputDft = null;
        Complex inputExpTerm = null;
        Complex outputExpTerm = null;
        Complex inputResidual = null;
        Complex outputResidual = null;
        Complex outputDft = null;

        double[] inputVocalTractSpectrum = null;
        double[] interpolatedInputLpcs = null;
        double[] sourceVocalTractSpectrumEstimate = null;
        double[] targetVocalTractSpectrumEstimate = null;
        double[] interpolatedInputVocalTractSpectrum = null;
        double[] outputVocalTractSpectrum = null;
        double[] warpedOutputVocalTractSpectrum = null;
        double[] transformationFilter = null;
        Context currentContext = null;

        if (repeatSkipCount>-1)
        {
            frm = MathUtils.zeros(frmSize);
            System.arraycopy(frmIn, 0, frm, 0, Math.min(frmIn.length, frmSize));
            wgt = windowIn.values(frmSize);

            if (vscale != 1.0)
                bWarp=true; 
            else
                bWarp=false; 

            boolean isTransformUnvoiced = true;

            if ((isVoiced && pscale!=1.0) || bWarp || isTransformUnvoiced)
            {
                if (fftSize<frmSize)
                {
                    fftSize = (int)Math.pow(2, (Math.ceil(Math.log((double)frmSize)/Math.log(2.0))));
                    maxFreq = fftSize/2+1;
                }

                newMaxFreq = (int)Math.floor(maxFreq/pscale+0.5);

                if (newMaxFreq<3)
                    newMaxFreq=3;

                if ((newMaxFreq % 2) !=1)
                    newMaxFreq++;

                newFftSize = 2*(newMaxFreq-1);

                frmEn = SignalProcUtils.getEnergy(frm);

                wgt = windowIn.values(frmSize);

                //Windowing
                for (j=0; j<frmSize; j++)
                    frm[j] = frm[j]*wgt[j]; 

                //Preemphasis
                frm = SignalProcUtils.applyPreemphasis(frm, wctParams.lsfParams.preCoef);

                // Compute LPC coefficients
                inputLPCoeffs = LPCAnalyser.calcLPC(frm, wctParams.lsfParams.lpOrder);
                inputLpcs = inputLPCoeffs.getOneMinusA();
                inputLsfs = LineSpectralFrequencies.lpc2lsfInHz(inputLpcs, fs); 
                sqrtInputGain = inputLPCoeffs.getGain();

                //Find target estimate from codebook
                if (wctParams.isVocalTractTransformation)
                {
                    if (wctParams.isContextBasedPreselection)
                    {
                        currentContext = new Context(labels, currentLabelIndex, wctParams.totalContextNeighbours);
                        preselectedIndices = mapper.preselect(currentContext, codebook, wctParams.isVocalTractMatchUsingTargetCodebook, wctParams.mapperParams.numBestMatches);
                    }
                    
                    if (preselectedIndices!=null)
                        codebookMatch = mapper.transform(inputLsfs, codebook, wctParams.isVocalTractMatchUsingTargetCodebook, preselectedIndices);
                    else
                        codebookMatch = mapper.transform(inputLsfs, codebook, wctParams.isVocalTractMatchUsingTargetCodebook, allIndices);
                        
                    //Use source for testing things. Don´t forget to set isSourceVocalTractFromCodeook=false
                    //codebookMatch = new WeightedCodebookMatch(inputLsfs, inputLsfs); 
                }

                inputDft = new Complex(fftSize);

                System.arraycopy(frm, 0, inputDft.real, 0, Math.min(frmSize, inputDft.real.length));

                if (inputDft.real.length > frmSize)
                    Arrays.fill(inputDft.real, inputDft.real.length-frmSize, inputDft.real.length-1, 0);

                Arrays.fill(inputDft.imag, 0, inputDft.imag.length-1, 0);

                inputDft = FFTMixedRadix.fftComplex(inputDft);

                //For checking
                if (bShowSpectralPlots && psFrm.getCurrentTime()>=desiredFrameTime)
                {
                    tmpComp = new Complex(inputDft);
                    tmpSpec = MathUtils.amp2db(tmpComp, 0, maxFreq);
                    MaryUtils.plot(tmpSpec, "1.Input DFT");
                }
                //

                inputExpTerm = LPCAnalyser.calcExpTerm(fftSize, wctParams.lsfParams.lpOrder);
                outputExpTerm = LPCAnalyser.calcExpTerm(newFftSize, wctParams.lsfParams.lpOrder);

                inputVocalTractSpectrum = LPCAnalyser.calcSpecFromOneMinusA(inputLPCoeffs.getOneMinusA(), wctParams.lsfParams.lpOrder, fftSize, inputExpTerm);

                //Use a weighted codebook estimate of the input vocal tract spectrum. This will result in a smoother transformation filter
                if (wctParams.isSourceVocalTractSpectrumFromCodebook && wctParams.isVocalTractTransformation)
                {
                    if (!wctParams.isResynthesizeVocalTractFromSourceCodebook)
                        interpolatedInputLpcs = LineSpectralFrequencies.lsfInHz2lpc(codebookMatch.entry.sourceItem.lsfs, fs);
                    else
                        interpolatedInputLpcs = LineSpectralFrequencies.lsfInHz2lpc(codebookMatch.entry.targetItem.lsfs, fs);

                    sourceVocalTractSpectrumEstimate = LPCAnalyser.calcSpecFromOneMinusA(interpolatedInputLpcs, 1.0f, newFftSize, outputExpTerm);
                }

                for (k=0; k<maxFreq; k++)
                    inputVocalTractSpectrum[k] *= sqrtInputGain;

                //For checking
                if (bShowSpectralPlots && psFrm.getCurrentTime()>=desiredFrameTime)
                {
                    tmpSpec = new double[maxFreq];
                    System.arraycopy(inputVocalTractSpectrum, 0, tmpSpec, 0, tmpSpec.length);
                    tmpSpec = MathUtils.amp2db(tmpSpec);
                    MaryUtils.plot(tmpSpec, "2.Input Vocal Tract");
                }
                //

                //For checking
                if (bShowSpectralPlots && psFrm.getCurrentTime()>=desiredFrameTime && wctParams.isSourceVocalTractSpectrumFromCodebook && wctParams.isVocalTractTransformation)
                {
                    tmpSpec = new double[maxFreq];
                    System.arraycopy(sourceVocalTractSpectrumEstimate, 0, tmpSpec, 0, tmpSpec.length);
                    tmpSpec = MathUtils.amp2db(tmpSpec);
                    MaryUtils.plot(tmpSpec, "3.Source Vocal Tract Estimate");
                }
                //

                inputResidual = new Complex(fftSize);

                // Filter out vocal tract to obtain the input residual spectrum (note that this is the real residual spectrum)
                for (k=0; k<maxFreq; k++)
                {
                    inputResidual.real[k] = inputDft.real[k]/inputVocalTractSpectrum[k];
                    inputResidual.imag[k] = inputDft.imag[k]/inputVocalTractSpectrum[k];
                }

                //For checking
                if (bShowSpectralPlots && psFrm.getCurrentTime()>=desiredFrameTime)
                {
                    tmpComp = new Complex(inputResidual);
                    tmpSpec = MathUtils.amp2db(tmpComp, 0, maxFreq-1);
                    MaryUtils.plot(tmpSpec, "4.Input Residual");
                }
                //

                if (wctParams.isVocalTractTransformation)
                {
                    //Smoothing
                    if (wctParams.smoothingMethod==SmoothingDefinitions.OUTPUT_LSFCONTOUR_SMOOTHING)
                    {
                        if (wctParams.smoothingState==SmoothingDefinitions.ESTIMATING_SMOOTHED_VOCAL_TRACT)
                        {
                            if (!wctParams.isResynthesizeVocalTractFromSourceCodebook)
                                smoothingFile.writeSingle(codebookMatch.entry.targetItem.lsfs);
                            else
                                smoothingFile.writeSingle(codebookMatch.entry.sourceItem.lsfs);    
                        }
                        else if (wctParams.smoothingState==SmoothingDefinitions.TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT)
                        {
                            if (!wctParams.isResynthesizeVocalTractFromSourceCodebook)
                                codebookMatch.entry.targetItem.setLsfs(smoothedVocalTract[smoothedInd]);
                            else
                                codebookMatch.entry.sourceItem.setLsfs(smoothedVocalTract[smoothedInd]);  
                        }
                    }
                    //

                    if (!wctParams.isResynthesizeVocalTractFromSourceCodebook)
                        targetLpcs = LineSpectralFrequencies.lsfInHz2lpc(codebookMatch.entry.targetItem.lsfs, fs);
                    else
                        targetLpcs = LineSpectralFrequencies.lsfInHz2lpc(codebookMatch.entry.sourceItem.lsfs, fs);

                    if (fftSize!=newFftSize)
                    {
                        if (outputExpTerm==null || newMaxFreq*wctParams.lsfParams.lpOrder!=outputExpTerm.real.length)
                            outputExpTerm = LPCAnalyser.calcExpTerm(newFftSize, wctParams.lsfParams.lpOrder);

                        targetVocalTractSpectrumEstimate = LPCAnalyser.calcSpecFromOneMinusA(targetLpcs, 1.0f, newFftSize, outputExpTerm);
                    }
                    else
                        targetVocalTractSpectrumEstimate = LPCAnalyser.calcSpecFromOneMinusA(targetLpcs, 1.0f, newFftSize, inputExpTerm);

                    for (k=0; k<newMaxFreq; k++)
                        targetVocalTractSpectrumEstimate[k] *= sqrtInputGain;
                }

                //For checking
                if (bShowSpectralPlots && psFrm.getCurrentTime()>=desiredFrameTime && wctParams.isVocalTractTransformation)
                {
                    tmpSpec = new double[newMaxFreq];
                    System.arraycopy(targetVocalTractSpectrumEstimate, 0, tmpSpec, 0, tmpSpec.length);
                    tmpSpec = MathUtils.amp2db(tmpSpec);
                    MaryUtils.plot(tmpSpec, "5.Target Vocal Tract Estimate");
                }
                //

                outputVocalTractSpectrum = new double[newMaxFreq];
                interpolatedInputVocalTractSpectrum = MathUtils.interpolate(inputVocalTractSpectrum, newMaxFreq);

                if (wctParams.isVocalTractTransformation)
                {
                    if (wctParams.isSourceVocalTractSpectrumFromCodebook)
                    {
                        for (k=0; k<newMaxFreq; k++)
                            outputVocalTractSpectrum[k] = targetVocalTractSpectrumEstimate[k]/sourceVocalTractSpectrumEstimate[k]*interpolatedInputVocalTractSpectrum[k];
                    }
                    else
                    {
                        for (k=0; k<newMaxFreq; k++)
                            outputVocalTractSpectrum[k] = targetVocalTractSpectrumEstimate[k];
                    }
                }
                else
                {
                    for (k=0; k<newMaxFreq; k++)
                        outputVocalTractSpectrum[k] = interpolatedInputVocalTractSpectrum[k];
                }

                //Smoothing
                if (wctParams.smoothingMethod==SmoothingDefinitions.TRANSFORMATION_FILTER_SMOOTHING)
                {
                    if (wctParams.smoothingState==SmoothingDefinitions.ESTIMATING_SMOOTHED_VOCAL_TRACT)    
                    {
                        transformationFilter = new double[newMaxFreq];

                        if (wctParams.isSourceVocalTractSpectrumFromCodebook)
                        {
                            for (k=0; k<newMaxFreq; k++)
                                transformationFilter[k] = targetVocalTractSpectrumEstimate[k]/sourceVocalTractSpectrumEstimate[k];
                        }
                        else
                        {
                            for (k=0; k<newMaxFreq; k++)
                                transformationFilter[k] = targetVocalTractSpectrumEstimate[k]/interpolatedInputVocalTractSpectrum[k];
                        }

                        smoothingFile.writeSingle(transformationFilter);   

                        //For checking
                        if (bShowSpectralPlots && psFrm.getCurrentTime()>=desiredFrameTime)
                        {
                            tmpSpec = new double[newMaxFreq];
                            System.arraycopy(transformationFilter, 0, tmpSpec, 0, tmpSpec.length);
                            tmpSpec = MathUtils.amp2db(tmpSpec);
                            MaryUtils.plot(tmpSpec, "6.Transformation filter");
                        }
                    }
                    else if (wctParams.smoothingState==SmoothingDefinitions.TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT)  
                    {
                        if (wctParams.isSourceVocalTractSpectrumFromCodebook)
                        {
                            for (k=0; k<newMaxFreq; k++)
                                outputVocalTractSpectrum[k] = smoothedVocalTract[smoothedInd][k]*sourceVocalTractSpectrumEstimate[k];
                        }
                        else
                        {
                            for (k=0; k<newMaxFreq; k++)
                                outputVocalTractSpectrum[k] = smoothedVocalTract[smoothedInd][k]*interpolatedInputVocalTractSpectrum[k];
                        }

                        //For checking
                        if (bShowSpectralPlots && psFrm.getCurrentTime()>=desiredFrameTime)
                        {
                            tmpSpec = new double[newMaxFreq];
                            System.arraycopy(smoothedVocalTract[smoothedInd], 0, tmpSpec, 0, tmpSpec.length);
                            tmpSpec = MathUtils.amp2db(tmpSpec);
                            MaryUtils.plot(tmpSpec, "6.Smoothed transformation filter");
                        }
                    }
                }
                //

                //Perform additional vocal tract scaling
                if (bWarp)
                {
                    tmpvsc[0] = vscale;
                    newVScales = InterpolationUtils.modifySize(tmpvsc, newMaxFreq); //Modify length to match current length of spectrum

                    for (k=0; k<newVScales.length; k++)
                    {
                        if (newVScales[k]<0.05) //Put a floor to avoid divide by zero
                            newVScales[k]=0.05;
                    }

                    warpedOutputVocalTractSpectrum = new double[newMaxFreq];

                    for (k=0; k<newMaxFreq; k++)
                    {
                        wInd = (int)Math.floor((k+1)/newVScales[k]+0.5); //Find new indices
                        if (wInd<1)
                            wInd = 1;
                        if (wInd>newMaxFreq)
                            wInd = newMaxFreq;

                        warpedOutputVocalTractSpectrum[k] = outputVocalTractSpectrum[wInd-1];
                    }

                    System.arraycopy(warpedOutputVocalTractSpectrum, 0, outputVocalTractSpectrum, 0, newMaxFreq);
                }

                //Create output DFT spectrum
                outputResidual = new Complex(newFftSize);
                outputResidual.real = MathUtils.zeros(newFftSize);
                outputResidual.imag = MathUtils.zeros(newFftSize);

                System.arraycopy(inputResidual.real, 0, outputResidual.real, 0, Math.min(maxFreq, newFftSize));
                System.arraycopy(inputResidual.imag, 0, outputResidual.imag, 0, Math.min(maxFreq, newFftSize));

                //Copy & paste samples if required (COMPLEX VERSION TO SUPPORT PSCALE<=0.5)
                // This version fills the spectrum by flipping and pasting the original freq bins as many times as required.
                kMax = 1;
                while (newMaxFreq>(kMax+1)*(maxFreq-2))
                    kMax++;

                for (k=1; k<=kMax; k++)
                {
                    tmpFix = (maxFreq-2)*k;
                    if (k%2==1) //Odd mode
                    {
                        tmpAdd = maxFreq+2;
                        tmpMul = 1;
                    }
                    else
                    {
                        tmpAdd = -1;
                        tmpMul = -1;
                    }

                    for (j=tmpFix+3; j<=Math.min(newMaxFreq, maxFreq+tmpFix); j++)
                    {
                        outputResidual.real[j-1] = inputResidual.real[tmpMul*(tmpFix-j)+tmpAdd-1];
                        outputResidual.imag[j-1] = inputResidual.imag[tmpMul*(tmpFix-j)+tmpAdd-1];
                    }
                }

                outputResidual.real[newMaxFreq-1] = Math.sqrt(outputResidual.real[newMaxFreq-1]*outputResidual.real[newMaxFreq-1] + outputResidual.imag[newMaxFreq-1]*outputResidual.imag[newMaxFreq-1]);
                outputResidual.imag[newMaxFreq-1] = 0.0;

                //For checking
                if (bShowSpectralPlots && psFrm.getCurrentTime()>=desiredFrameTime)
                {
                    tmpComp = new Complex(outputResidual);
                    tmpSpec = MathUtils.amp2db(tmpComp, 0, newMaxFreq-1);
                    MaryUtils.plot(tmpSpec, "7.Output Residual");
                }
                //

                //Filter the output residual with the estimated target vocal tract spectrum
                outputDft = new Complex(newFftSize);

                //Smoothing
                if (wctParams.smoothingMethod==SmoothingDefinitions.OUTPUT_VOCALTRACTSPECTRUM_SMOOTHING)
                {
                    if (wctParams.smoothingState==SmoothingDefinitions.ESTIMATING_SMOOTHED_VOCAL_TRACT)
                    {
                        smoothingFile.writeSingle(outputVocalTractSpectrum, newMaxFreq);   
                    }
                    else if (wctParams.smoothingState==SmoothingDefinitions.TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT)
                    {
                        for (k=0; k<newMaxFreq; k++)
                            outputVocalTractSpectrum[k] = smoothedVocalTract[smoothedInd][k];
                    }
                }
                //

                //For checking
                if (bShowSpectralPlots && psFrm.getCurrentTime()>=desiredFrameTime)
                {
                    tmpSpec = new double[newMaxFreq];
                    System.arraycopy(outputVocalTractSpectrum, 0, tmpSpec, 0, tmpSpec.length);
                    tmpSpec = MathUtils.amp2db(tmpSpec);
                    MaryUtils.plot(tmpSpec, "8.Output Vocal Tract");
                }
                //

                for (k=1; k<=newMaxFreq; k++)
                {
                    outputDft.real[k-1] = outputResidual.real[k-1]*outputVocalTractSpectrum[k-1];
                    outputDft.imag[k-1] = outputResidual.imag[k-1]*outputVocalTractSpectrum[k-1];
                }

                for (k=newMaxFreq+1; k<=newFftSize; k++)
                {
                    outputDft.real[k-1] = outputDft.real[2*newMaxFreq-1-k];
                    outputDft.imag[k-1] = -outputDft.imag[2*newMaxFreq-1-k];
                }

                //For checking
                if (bShowSpectralPlots && psFrm.getCurrentTime()>=desiredFrameTime)
                {
                    tmpComp = new Complex(outputDft);
                    tmpSpec = MathUtils.amp2db(tmpComp, 0, newMaxFreq);
                    MaryUtils.plot(tmpSpec, "9.Output DFT");
                    bShowSpectralPlots = false;
                }
                //

                //Convert back to time domain
                outputDft = FFTMixedRadix.ifft(outputDft);

                frmy = new double[newFrmSize];
                System.arraycopy(outputDft.real, 0, frmy, 0, newFrmSize);
            }
            else
            {
                if (frmSize<newFrmSize)
                    newFrmSize = frmSize; 

                frmy = new double[newFrmSize];
            }       

            frmy = SignalProcUtils.removePreemphasis(frmy, wctParams.lsfParams.preCoef);

            frmyEn = SignalProcUtils.getEnergy(frmy);

            gain = (frmEn/Math.sqrt(frmSize))/(frmyEn/Math.sqrt(newFrmSize))*escale;

            if (!(isVoiced && pscale!=1.0) && !bWarp && !isTransformUnvoiced)
            {
                for (k=0; k<frmSize; k++)
                    frmy[k] = frm[k]*wgt[k];
            }

            //Energy scale compensation + modification
            for (k=0; k<newFrmSize; k++)
                frmy[k] *= gain;

            for (j=1; j<=repeatSkipCount+1; j++)
            {
                if (!wctParams.isFixedRateVocalTractConversion)
                {
                    if (isVoiced)
                        newSkipSize = (int)Math.floor(currentPeriod/pscale+0.5);
                    else
                        newSkipSize = (int)Math.floor(currentPeriod+0.5);
                }
                else
                    newSkipSize = (int)Math.floor(ssFixedInSeconds*fs+0.5);

                if ((isLastInputFrame && j==repeatSkipCount+1)) //| (i~=numfrm & all(repeatSkipCounts(i+1:numfrm)==-1)))
                    bLastFrame = true;
                else
                    bLastFrame = false;

                synthFrameInd++;

                wgty = windowOut.values(newFrmSize);

                if (synthFrameInd==1) //First frame: Do not window the first half of output speech frame to prevent overflow in normalization with hanning coeffs
                {
                    halfWin = (int)Math.floor(newFrmSize/2.0+0.5);
                    synthTotal = synthSt+newFrmSize;

                    //Keep output in an overlap-add buffer
                    if (ySynthInd+newFrmSize-1<=maxNewFrmSize)
                    {
                        for (k=ySynthInd; k<=ySynthInd+halfWin-1; k++)
                        {
                            ySynthBuff[k-1] = frmy[k-ySynthInd];
                            wSynthBuff[k-1] = 1.0;
                        }

                        for (k=ySynthInd+halfWin; k<=ySynthInd+newFrmSize-1; k++)
                        {
                            ySynthBuff[k-1] += frmy[k-ySynthInd]*wgty[k-ySynthInd];
                            wSynthBuff[k-1] += wgty[k-ySynthInd]*wgty[k-ySynthInd];
                        }
                    }
                    else
                    {
                        for (k=ySynthInd; k<=maxNewFrmSize; k++)
                        {
                            if (k-ySynthInd<halfWin)
                            {
                                ySynthBuff[k-1] = frmy[k-ySynthInd];
                                wSynthBuff[k-1] = 1.0;
                            }
                            else
                            {
                                ySynthBuff[k-1] += frmy[k-ySynthInd]*wgty[k-ySynthInd];
                                wSynthBuff[k-1] += wgty[k-ySynthInd]*wgty[k-ySynthInd];
                            }
                        }

                        for (k=1; k<=newFrmSize-1-maxNewFrmSize+ySynthInd; k++)
                        {
                            if (maxNewFrmSize-ySynthInd+k<halfWin)
                            {
                                ySynthBuff[k-1] = frmy[maxNewFrmSize-ySynthInd+k];
                                wSynthBuff[k-1] = 1.0;
                            }
                            else
                            {
                                ySynthBuff[k-1] += frmy[maxNewFrmSize-ySynthInd+k]*wgty[maxNewFrmSize-ySynthInd+k];
                                wSynthBuff[k-1] += wgty[maxNewFrmSize-ySynthInd+k]*wgty[maxNewFrmSize-ySynthInd+k];
                            }
                        }
                    }
                    //

                    if (!bSilent)
                        System.out.println("Synthesized using frame "  + String.valueOf(inputFrameIndex+1)); 
                }
                else if (bLastFrame) //Last frame: Do not window the second half of output speech frame to prevent overflow in normalization with hanning coeffs
                {
                    halfWin = (int)Math.floor(newFrmSize/2.0+0.5);
                    remain = newFrmSize-halfWin;
                    synthTotal = synthSt+halfWin+remain-1;

                    //Keep output in an overlap-add buffer
                    if (ySynthInd+newFrmSize-1<=maxNewFrmSize)
                    {
                        for (k=ySynthInd; k<=ySynthInd+halfWin-1; k++)
                        {
                            ySynthBuff[k-1] += frmy[k-ySynthInd]*wgty[k-ySynthInd];
                            wSynthBuff[k-1] += wgty[k-ySynthInd]*wgty[k-ySynthInd];
                        }

                        for (k=ySynthInd+halfWin; k<=ySynthInd+newFrmSize-1; k++)
                        {
                            ySynthBuff[k-1] += frmy[k-ySynthInd];
                            wSynthBuff[k-1] = 1.0;
                        }
                    }
                    else
                    {
                        for (k=ySynthInd; k<=maxNewFrmSize; k++)
                        {
                            if (k-ySynthInd<halfWin)
                            {
                                ySynthBuff[k-1] += frmy[k-ySynthInd]*wgty[k-ySynthInd];
                                wSynthBuff[k-1] += wgty[k-ySynthInd]*wgty[k-ySynthInd];
                            }
                            else
                            {
                                ySynthBuff[k-1] += frmy[k-ySynthInd];
                                wSynthBuff[k-1] = 1.0;
                            }
                        }

                        for (k=1; k<=newFrmSize-1-maxNewFrmSize+ySynthInd; k++)
                        {
                            if (maxNewFrmSize-ySynthInd+k<halfWin)
                            {
                                ySynthBuff[k-1] += frmy[maxNewFrmSize-ySynthInd+k]*wgty[maxNewFrmSize-ySynthInd+k];
                                wSynthBuff[k-1] += wgty[maxNewFrmSize-ySynthInd+k]*wgty[maxNewFrmSize-ySynthInd+k];
                            }
                            else
                            {
                                ySynthBuff[k-1] += frmy[maxNewFrmSize-ySynthInd+k];
                                wSynthBuff[k-1] = 1.0;
                            }
                        }
                    }
                    //

                    if (!bSilent)
                        System.out.println("Synthesized using frame " + String.valueOf(inputFrameIndex+1)); 
                }
                else //Normal frame
                {
                    if (!isVoiced && ((repeatSkipCount%2)==1)) //Reverse unvoiced repeated frames once in two consecutive repetitions to reduce distortion
                        frmy = SignalProcUtils.reverse(frmy);

                    synthTotal = synthSt+newFrmSize;

                    //Keep output in an overlap-add buffer
                    if (ySynthInd+newFrmSize-1<=maxNewFrmSize)
                    {
                        for (k=ySynthInd; k<=ySynthInd+newFrmSize-1; k++)
                        {
                            ySynthBuff[k-1] += frmy[k-ySynthInd]*wgty[k-ySynthInd];
                            wSynthBuff[k-1] += wgty[k-ySynthInd]*wgty[k-ySynthInd];
                        }
                    }
                    else
                    {
                        for (k=ySynthInd; k<=maxNewFrmSize; k++)
                        {
                            ySynthBuff[k-1] += frmy[k-ySynthInd]*wgty[k-ySynthInd];
                            wSynthBuff[k-1] += wgty[k-ySynthInd]*wgty[k-ySynthInd];
                        }

                        for (k=1; k<=newFrmSize-1-maxNewFrmSize+ySynthInd; k++)
                        {    
                            ySynthBuff[k-1] += frmy[k+maxNewFrmSize-ySynthInd]*wgty[k+maxNewFrmSize-ySynthInd];
                            wSynthBuff[k-1] += wgty[k+maxNewFrmSize-ySynthInd]*wgty[k+maxNewFrmSize-ySynthInd];
                        }
                    }
                    //

                    if (!bSilent)
                    {
                        if (j==1)
                            System.out.println("Synthesized using frame " + String.valueOf(inputFrameIndex+1)); 
                        else 
                            System.out.println("Repeated using frame " + String.valueOf(inputFrameIndex+1));  
                    }
                }

                //Write to output buffer
                for (k=0; k<=newSkipSize-1; k++)
                {
                    kInd = (k+ySynthInd) % maxNewFrmSize;
                    if (kInd==0)
                        kInd=maxNewFrmSize;

                    if (wSynthBuff[kInd-1]>0.0)
                        outBuff[outBuffStart-1] = ySynthBuff[kInd-1]/wSynthBuff[kInd-1];
                    else
                        outBuff[outBuffStart-1] = ySynthBuff[kInd-1];

                    ySynthBuff[kInd-1] = 0.0;
                    wSynthBuff[kInd-1] = 0.0;

                    outBuffStart++;

                    if (outBuffStart>outBuffLen)
                    {
                        if (tscaleSingle!=1.0 || totalWrittenToFile+outBuffLen<=origLen)
                        {
                            dout.writeDouble(outBuff, 0, outBuffLen);
                            totalWrittenToFile += outBuffLen;
                        }
                        else
                        {   
                            dout.writeDouble(outBuff, 0, origLen-totalWrittenToFile);
                            totalWrittenToFile = origLen;
                        }

                        outBuffStart=1;
                    }
                }
                //

                synthSt += newSkipSize;

                //if (!bLastFrame)
                //{
                if (ySynthInd+newSkipSize<=maxNewFrmSize)
                    ySynthInd += newSkipSize;
                else
                    ySynthInd += newSkipSize-maxNewFrmSize;
                //}
                /////////

                if (bLastFrame) 
                {
                    bBroke = true; 
                    break; 
                }
            }
        }
        else
        {
            if (!bSilent)
                System.out.println("Skipped frame " + String.valueOf(inputFrameIndex+1));
        }

        inputFrameIndex++;

        return output;
            }

    public double [] writeFinal() throws IOException
    {
        double [] output = null;
        double [] outputTmp = null;

        int k, kInd;

        if (tscaleSingle==1.0)
            synthTotal=origLen;

        if (outBuffLen>synthTotal)
            outBuffLen = synthTotal;

        //Write the final segment
        for (k=synthSt; k<=synthTotal; k++)
        {
            kInd = (k-synthSt+ySynthInd)%maxNewFrmSize; 

            if (kInd==0)
                kInd=maxNewFrmSize;

            if (wSynthBuff[kInd-1]>0.0)
                outBuff[outBuffStart-1] = ySynthBuff[kInd-1]/wSynthBuff[kInd-1];
            else
                outBuff[outBuffStart-1] = ySynthBuff[kInd-1];

            ySynthBuff[kInd-1] = 0.0;
            wSynthBuff[kInd-1] = 0.0;

            outBuffStart++;

            if (outBuffStart>outBuffLen)
            {                
                if (tscaleSingle!=1.0 || totalWrittenToFile+outBuffLen<=origLen)
                {
                    dout.writeDouble(outBuff, 0, outBuffLen);
                    totalWrittenToFile += outBuffLen;
                }
                else
                {
                    dout.writeDouble(outBuff, 0, origLen-totalWrittenToFile);
                    totalWrittenToFile = origLen;
                }
                outBuffStart=1;
            }
        }

        if (outBuffStart>1)
        {            
            if (tscaleSingle!=1.0 || totalWrittenToFile+outBuffStart-1<=origLen)
            {
                dout.writeDouble(outBuff, 0, outBuffStart-1);
                totalWrittenToFile += outBuffStart-1;
            }
            else
            {
                dout.writeDouble(outBuff, 0, origLen-totalWrittenToFile);
                totalWrittenToFile = origLen;
            }
        }
        //

        if (dout!=null)
            dout.close();

        return output;
    }

    public void convertToWav(AudioFormat audioformat) throws IOException
    {
        //Read the temp binary file into a wav file and delete the temp binary file
        if (tempOutBinaryFile!=null)
        {
            double [] yOut = null;

            din = new LEDataInputStream(tempOutBinaryFile);
            yOut = din.readDouble(totalWrittenToFile);
            din.close();

            double tmpMax = MathUtils.getAbsMax(yOut);
            if (tmpMax>1.0)
            {
                for (int n=0; n<yOut.length; n++)
                    yOut[n] /= tmpMax;
            }

            outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(yOut), audioformat);
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outputFile));

            FileUtils.delete(tempOutBinaryFile);
            //
        }
    }  
}
