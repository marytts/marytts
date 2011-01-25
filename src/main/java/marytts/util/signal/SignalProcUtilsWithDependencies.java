/**
 * Copyright 2011 DFKI GmbH.
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

package marytts.util.signal;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.LpcAnalyser;
import marytts.signalproc.window.HammingWindow;
import marytts.signalproc.window.Window;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.MaryAudioUtils;
import marytts.util.io.AlignLabelsTempUtils;
import marytts.util.math.ComplexArray;
import marytts.util.math.FFTMixedRadix;
import marytts.util.math.MathUtils;
import marytts.util.string.StringUtils;

/**
 * @author marc
 *
 */
public class SignalProcUtilsWithDependencies {

    public static double sourceTime2targetTime(double sourceTime, Labels sourceLabels, Labels targetLabels)
    {
        int[][] map = AlignLabelsTempUtils.alignLabels(sourceLabels.items, targetLabels.items);
        
        return sourceTime2targetTime(sourceTime, sourceLabels, targetLabels, map);
    }

    public static double sourceTime2targetTime(double sourceTime, Labels sourceLabels, Labels targetLabels, int[][] map)
    {
        int sourceLabInd = SignalProcUtils.time2LabelIndex(sourceTime, sourceLabels);
        double sourceDuration, targetDuration;
        double locationInLabelPercent;
    
        if (sourceLabInd>0)
        {
            sourceDuration = sourceLabels.items[sourceLabInd].time-sourceLabels.items[sourceLabInd-1].time;
            locationInLabelPercent = (sourceTime-sourceLabels.items[sourceLabInd-1].time)/sourceDuration;
        }
        else
        {
            sourceDuration = sourceLabels.items[sourceLabInd].time;
            locationInLabelPercent = sourceTime/sourceLabels.items[sourceLabInd].time;
        }
    
        int targetLabInd = StringUtils.findInMap(map, sourceLabInd);
        if (targetLabInd>0)
            targetDuration = targetLabels.items[targetLabInd].time-targetLabels.items[targetLabInd-1].time;
        else
            targetDuration = targetLabels.items[targetLabInd].time;
        
        double targetTime;
        if (targetLabInd>0)
            targetTime = targetLabels.items[targetLabInd-1].time+locationInLabelPercent*targetDuration;
        else
            targetTime = locationInLabelPercent*targetDuration;
        
        return targetTime;
    }

    public static int[] mapFrameIndices(int numfrmSource, Labels srcLabs, double srcWindowSizeInSeconds, double srcSkipSizeInSeconds,
                                        int numFrmTarget, Labels tgtLabs, double tgtWindowSizeInSeconds, double tgtSkipSizeInSeconds)
    {
        int[] mappedInds = null;
        int[][] mappedLabelInds = AlignLabelsTempUtils.alignLabels(srcLabs.items, tgtLabs.items);
        
        if (numfrmSource>0)
        {
            mappedInds = new int[numfrmSource];
            double tSource, tTarget, tFrameInd, sourceDuration, sourceLocationInLabelPercent;
            double sMapStart, tMapStart, sMapEnd, tMapEnd;
            int sourceLabInd, targetLabInd;
            int targetFrmInd;
            for (int i=0; i<numfrmSource; i++)
            {
                tSource = i*srcSkipSizeInSeconds+0.5*srcWindowSizeInSeconds;
                sourceLabInd = SignalProcUtils.time2LabelIndex(tSource, srcLabs);
                targetLabInd = StringUtils.findInMap(mappedLabelInds, sourceLabInd);
    
                if (targetLabInd<0)
                {
                    sMapStart = 0.0;
                    tMapStart = 0.0;
                    sMapEnd = srcLabs.items[srcLabs.items.length-1].time;
                    tMapEnd = tgtLabs.items[tgtLabs.items.length-1].time;
                    
                    for (int j=targetLabInd-1; j>=0; j--)
                    {
                        int prevSourceLabInd = StringUtils.findInMapReverse(mappedLabelInds, j);
                        if (prevSourceLabInd>-1)
                        {
                            sMapStart = srcLabs.items[prevSourceLabInd].time;
                            tMapStart = tgtLabs.items[j].time;
                            break;
                        }
                    }
                    
                    for (int j=targetLabInd+1; j<tgtLabs.items.length; j++)
                    {
                        int nextSourceLabInd = StringUtils.findInMapReverse(mappedLabelInds, j);
                        if (nextSourceLabInd>-1)
                        {
                            sMapEnd = srcLabs.items[nextSourceLabInd].time;
                            tMapEnd = tgtLabs.items[j].time;
                            break;
                        }
                    }
                }
                else
                {
                    sMapStart = 0.0;
                    if (sourceLabInd>0)
                        sMapStart = srcLabs.items[sourceLabInd-1].time;
                    
                    tMapStart = 0.0;
                    if (targetLabInd>0)
                        tMapStart = tgtLabs.items[targetLabInd-1].time;
                    
                    sMapEnd = srcLabs.items[sourceLabInd].time;
                    tMapEnd = tgtLabs.items[targetLabInd].time;
                }  
                
                tTarget = MathUtils.linearMap(tSource, sMapStart, sMapEnd, tMapStart, tMapEnd);
                targetFrmInd = SignalProcUtils.time2frameIndex(tTarget, tgtWindowSizeInSeconds, tgtSkipSizeInSeconds);
                targetFrmInd = MathUtils.CheckLimits(targetFrmInd, 0, numFrmTarget-1);
                mappedInds[i] = targetFrmInd;
            }
        }
        
        return mappedInds;
    }

    //This version uses source and target labels to align speech frames
    public static double[] normalizeVocalTract(double[] srcSignal, double[] tgtSignal, Labels sourceLabels, Labels targetLabels, int windowType, double windowSizeInSeconds, double frameShiftInSeconds, int lpcOrder, int samplingRateInHz, float preCoef)
    {
        float[][] sourceLpcs = LpcAnalyser.signal2lpCoeffsf(srcSignal, windowType, windowSizeInSeconds, frameShiftInSeconds, samplingRateInHz, lpcOrder, preCoef);
        float[] sAnalysisInSeconds = SignalProcUtils.getAnalysisTimes(sourceLpcs.length, windowSizeInSeconds, frameShiftInSeconds);
        
        float[][] targetLpcs = LpcAnalyser.signal2lpCoeffsf(tgtSignal, windowType, windowSizeInSeconds, frameShiftInSeconds, samplingRateInHz, lpcOrder, preCoef);
        
        //Mapping
        int[] mappedInds = SignalProcUtilsWithDependencies.mapFrameIndices(sourceLpcs.length, sourceLabels, windowSizeInSeconds, frameShiftInSeconds,
                                                           targetLpcs.length, targetLabels, windowSizeInSeconds, frameShiftInSeconds);
        float[][] mappedTargetLpcs = SignalProcUtils.getMapped(targetLpcs, mappedInds);
 
        return normalizeVocalTract(srcSignal, sAnalysisInSeconds, mappedTargetLpcs, windowType, windowSizeInSeconds, lpcOrder, samplingRateInHz,  preCoef);
    }

    
    public static double[] normalizeVocalTract(double[] s, float[] sAnalysisInSeconds, float[][] mappedTgtLpcs, int windowType, double windowSizeInSeconds, int lpcOrderSrc, int samplingRateInHz, float preCoef)
    {
        float[][] srcLpcs = LpcAnalyser.signal2lpCoeffsf(s, windowType, sAnalysisInSeconds, windowSizeInSeconds, samplingRateInHz, lpcOrderSrc, preCoef);
        
        return normalizeVocalTract(s, sAnalysisInSeconds, srcLpcs, mappedTgtLpcs, windowSizeInSeconds, samplingRateInHz, preCoef);
    }
    
    public static double[] normalizeVocalTract(double[] x, float[] tAnalysisInSeconds, float[][] srcLpcs, float[][] mappedTgtLpcs, double windowSizeInSeconds, int samplingRateInHz, float preCoef)
    {
        double[] y = null;
        
        assert tAnalysisInSeconds.length == srcLpcs.length;
        assert tAnalysisInSeconds.length == mappedTgtLpcs.length;
        
        int lpOrder = srcLpcs[0].length;
        int numfrm = tAnalysisInSeconds.length;
        int ws = SignalProcUtils.time2sample(windowSizeInSeconds, samplingRateInHz);
        int halfWs = (int)Math.floor(0.5*ws+0.5);
        Window wgt = new HammingWindow(ws);
        double[] winWgt = wgt.getCoeffs();
        double[] frm = new double[ws];
        int frmStartIndex;
        int frmEndIndex;
        
        int fftSize = SignalProcUtils.getDFTSize(samplingRateInHz);
        while (fftSize<ws)
            fftSize *= 2;
        
        ComplexArray expTerm = LpcAnalyser.calcExpTerm(fftSize, lpOrder);
        
        y = new double[x.length];
        double[] w = new double[x.length];
        Arrays.fill(y, 0.0);
        Arrays.fill(w, 0.0);
        
        double[] xPreemp = SignalProcUtils.applyPreemphasis(x, preCoef);
        
        int i, k;
        for (i=0; i<numfrm; i++)
        {
            if (i==0)
                frmStartIndex = 0;
            else
                frmStartIndex = Math.max(0, SignalProcUtils.time2sample(tAnalysisInSeconds[i]-0.5*windowSizeInSeconds, samplingRateInHz));
            
            frmEndIndex = Math.min(frmStartIndex+ws-1, xPreemp.length-1);
            Arrays.fill(frm, 0.0);
            System.arraycopy(xPreemp, frmStartIndex, frm, 0, frmEndIndex-frmStartIndex+1);
            
            frm = wgt.apply(frm, 0);
            double origEn = SignalProcUtils.energy(frm);
            
            double[] inputVocalTractSpectrum = LpcAnalyser.calcSpecLinearf(srcLpcs[i], 1.0f, fftSize, expTerm);
            double[] outputVocalTractSpectrum = LpcAnalyser.calcSpecLinearf(mappedTgtLpcs[i], 1.0f, fftSize, expTerm);
                        
            ComplexArray inputDft = new ComplexArray(fftSize);
            int maxFreq = fftSize/2+1;

            Arrays.fill(inputDft.real, 0.0);
            Arrays.fill(inputDft.imag, 0.0);
            
            System.arraycopy(frm, 0, inputDft.real, 0, ws);

            inputDft = FFTMixedRadix.fftComplex(inputDft);
            
            //MaryUtils.plot(MathUtils.amp2db(MathUtils.abs(inputDft)));
            //MaryUtils.plot(MathUtils.amp2db(inputVocalTractSpectrum));
            //MaryUtils.plot(MathUtils.amp2db(outputVocalTractSpectrum));
            
            for (k=1; k<=maxFreq; k++)
            {
                inputDft.real[k-1] = inputDft.real[k-1]*outputVocalTractSpectrum[k-1]/inputVocalTractSpectrum[k-1];
                inputDft.imag[k-1] = inputDft.imag[k-1]*outputVocalTractSpectrum[k-1]/inputVocalTractSpectrum[k-1];
            }

            for (k=maxFreq+1; k<=fftSize; k++)
            {
                inputDft.real[k-1] = inputDft.real[2*maxFreq-1-k];
                inputDft.imag[k-1] = -inputDft.imag[2*maxFreq-1-k];
            }
            
            //MaryUtils.plot(MathUtils.amp2db(MathUtils.abs(inputDft)));
            
            inputDft = FFTMixedRadix.ifft(inputDft);

            //MaryUtils.plot(frm);
            
            System.arraycopy(inputDft.real, 0, frm, 0, ws);
            
            double newEn = SignalProcUtils.energy(frm);

            frm = MathUtils.multiply(frm, Math.sqrt(origEn)/Math.sqrt(newEn));

            //MaryUtils.plot(frm);
            
            for (k=0; k<ws; k++)
            {
                if (frmStartIndex+k>y.length-1)
                    break;
                
                if (i==0)
                {
                    if (k<halfWs)
                    {    
                        y[frmStartIndex+k] += frm[k]*winWgt[k];
                        w[frmStartIndex+k] += 1.0;
                    }
                    else
                    {
                        y[frmStartIndex+k] += frm[k]*winWgt[k];
                        w[frmStartIndex+k] += winWgt[k]*winWgt[k];
                    }
                }
                else if (i==numfrm-1)
                {
                    if (k>halfWs)
                    {    
                        y[frmStartIndex+k] += frm[k]*winWgt[k];
                        w[frmStartIndex+k] = 1.0;
                    }
                    else
                    {
                        y[frmStartIndex+k] += frm[k]*winWgt[k];
                        w[frmStartIndex+k] += winWgt[k]*winWgt[k];
                    }
                }
                else
                {
                    y[frmStartIndex+k] += frm[k]*winWgt[k];
                    w[frmStartIndex+k] += winWgt[k]*winWgt[k];
                }
            }
            
            System.out.println(String.valueOf(frmStartIndex) + "-" + String.valueOf(frmEndIndex) + " Normalized vocal tract spectrum for frame " + String.valueOf(i+1) + " of " + String.valueOf(numfrm));
        }
        
        for (k=0; k<y.length; k++)
        {
            if (w[k]>0.0)
                y[k] /= w[k];
        }
        
        y = SignalProcUtils.removePreemphasis(y, preCoef);
        
        //MaryUtils.plot(x);
        //MaryUtils.plot(y);
        
        return y;
    }

    public static void test_normalizeVocalTract() throws UnsupportedAudioFileException, IOException
    {
        String sourceWavFile = "d:\\src.wav";
        String sourceLabFile = "d:\\src.lab";
        String targetWavFile = "d:\\tgt.wav";
        String targetLabFile = "d:\\tgt.lab";
        String outputWavFile = "d:\\srcResidual_tgtVocalTract.wav";
        
        int windowType = Window.HAMMING;
        double windowSizeInSeconds = 0.020;
        double frameShiftInSeconds = 0.010;
        float preCoef = 0.97f;
        
        //File input source and LPC analysis
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(sourceWavFile));
        AudioFormat format = inputAudio.getFormat();
        int fsSrc = (int)inputAudio.getFormat().getSampleRate();
        int lpcOrderSrc = SignalProcUtils.getLPOrder(fsSrc);
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double[] s = signal.getAllData();
        Labels sourceLabels = Labels.readESTLabelFile(sourceLabFile);
        //
        
        //File input target and LPC analysis
        inputAudio = AudioSystem.getAudioInputStream(new File(targetWavFile));
        int fsTgt = (int)inputAudio.getFormat().getSampleRate();
        int lpcOrderTgt = SignalProcUtils.getLPOrder(fsTgt);
        signal = new AudioDoubleDataSource(inputAudio);
        double[] t = signal.getAllData();
        Labels targetLabels = Labels.readESTLabelFile(targetLabFile);
        //

        double[] sNorm = normalizeVocalTract(s, t, sourceLabels, targetLabels, windowType, windowSizeInSeconds, frameShiftInSeconds, lpcOrderSrc, fsSrc, preCoef);

        MaryAudioUtils.writeWavFile(sNorm, outputWavFile, format);
    }

    //This version does linear mapping between the whole source and target signals
    public static double[] normalizeVocalTract(double[] srcSignal, double[] tgtSignal, int windowType, double windowSizeInSeconds, double frameShiftInSeconds, int lpcOrder, int samplingRateInHz, float preCoef)
    {
        float[][] sourceLpcs = LpcAnalyser.signal2lpCoeffsf(srcSignal, windowType, windowSizeInSeconds, frameShiftInSeconds, samplingRateInHz, lpcOrder, preCoef);
        float[] sAnalysisInSeconds = SignalProcUtils.getAnalysisTimes(sourceLpcs.length, windowSizeInSeconds, frameShiftInSeconds);
        
        float[][] targetLpcs = LpcAnalyser.signal2lpCoeffsf(tgtSignal, windowType, windowSizeInSeconds, frameShiftInSeconds, samplingRateInHz, lpcOrder, preCoef);
        
        //Mapping
        int[] mappedInds = new int[sourceLpcs.length];
        for (int i=0; i<sourceLpcs.length; i++)
            mappedInds[i] = MathUtils.linearMap(i, 0, sourceLpcs.length-1, 0, targetLpcs.length-1);
        
        float[][] mappedTargetLpcs = SignalProcUtils.getMapped(targetLpcs, mappedInds);
        
        return normalizeVocalTract(srcSignal, sAnalysisInSeconds, mappedTargetLpcs, windowType, windowSizeInSeconds, lpcOrder, samplingRateInHz,  preCoef);
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {  
        /*
        LowPassFilter f = new LowPassFilter(0.25, 11);
        
        double[] b = f.getDenumeratorCoefficients();
        
        double[] a = new double[1];
        a[0] = 1.0;
        
        double[] x;
        double[] y;
        
        int i;
        String str;
        
        x = new double[100];
        for (i=0; i<x.length; i++)
            x[i] = i;
        
        str = "";
        for (i=0; i<x.length; i++)
            str += String.valueOf(x[i]) + " ";
        System.out.println(str);
        
        y = filter(b, a, x);
        str = "filtered=";
        for (i=0; i<y.length; i++)
            str += String.valueOf(y[i]) + " ";
        System.out.println(str);
        
        y = filtfilt(b, a, x);
        str = "filtfilted=";
        for (i=0; i<y.length; i++)
            str += String.valueOf(y[i]) + " ";
        System.out.println(str);
        */
        
        test_normalizeVocalTract();
    }

}
