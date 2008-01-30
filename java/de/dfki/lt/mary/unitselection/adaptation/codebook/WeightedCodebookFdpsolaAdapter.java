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
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.signalproc.FFT;
import de.dfki.lt.signalproc.FFTMixedRadix;
import de.dfki.lt.signalproc.analysis.F0ReaderWriter;
import de.dfki.lt.signalproc.analysis.LPCAnalyser;
import de.dfki.lt.signalproc.analysis.LineSpectralFrequencies;
import de.dfki.lt.signalproc.analysis.Lsfs;
import de.dfki.lt.signalproc.analysis.LPCAnalyser.LPCoeffs;
import de.dfki.lt.signalproc.process.FDPSOLAProcessor;
import de.dfki.lt.signalproc.process.VoiceModificationParametersPreprocessor;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.InterpolationUtils;
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
public class WeightedCodebookFdpsolaAdapter extends FDPSOLAProcessor {
    private boolean isFixedRateVocalTractTransformation;
    
    public WeightedCodebookFdpsolaAdapter(String strInputFile, String strPitchFile, String strLsfFile, String strOutputFile, 
                                          boolean bFixedRateVocalTractTransformation,
                                          double [] pscales, double [] tscales, double [] escales, double [] vscales
                                         ) throws UnsupportedAudioFileException, IOException
    {
        super(strInputFile, strPitchFile, strOutputFile, pscales, tscales, escales, vscales, bFixedRateVocalTractTransformation);

        isFixedRateVocalTractTransformation = bFixedRateVocalTractTransformation;

        init(strInputFile, strPitchFile,  strLsfFile, strOutputFile,
                pscales, tscales, escales, vscales);
    }
    
    public void init(String strInputFile, String strPitchFile, String strLsfFile, String strOutputFile,
                     double [] pscales, double [] tscales, double [] escales, double [] vscales)
    {
        super.init(WAVEFORM_MODIFICATION, 
                   strInputFile, strPitchFile, strOutputFile,
                   pscales,  tscales,  escales,  vscales, isFixedRateVocalTractTransformation);
    }
    
    public void fdpsolaOnline(WeightedCodebookTransformerParams params,
                              WeightedCodebookMapper mapper,
                              WeightedCodebook codebook) throws IOException
    {   
        int i;
        double [] frmIn;
        boolean isLastInputFrame;
        int inputFrameSize;
        int currentPeriod;
        
        params.lsfParams.samplingRate = (int)inputAudio.getFormat().getSampleRate();
        
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

            if (!isFixedRateVocalTractTransformation)
            {
                currentPeriod = pm.pitchMarks[i+1]-pm.pitchMarks[i];
                inputFrameSize = pm.pitchMarks[i+modParams.numPeriods]-pm.pitchMarks[i]+1;
            }
            else
            {
                currentPeriod = -1;
                inputFrameSize = frmIn.length;
            }

            boolean isVoiced;
            if (!isFixedRateVocalTractTransformation)
                isVoiced = pm.vuvs[i];
            else
            {
                if (f0s[i]>10.0)
                    isVoiced=true;
                else
                    isVoiced=false;
            }
            
            processFrame(frmIn, isVoiced, modParams.pscalesVar[i], modParams.tscalesVar[i], modParams.escalesVar[i], modParams.vscalesVar[i], isLastInputFrame, 
                         currentPeriod, inputFrameSize,
                         params, mapper, codebook);
        }

        writeFinal();
        
        convertToWav(inputAudio.getFormat());
    }
    
    //Voice conversion version
    public double [] processFrame(double [] frmIn, boolean isVoiced, 
                                  double pscale, double tscale, double escale, double vscale, 
                                  boolean isLastInputFrame, int currentPeriod, int inputFrameSize,
                                  WeightedCodebookTransformerParams params,
                                  WeightedCodebookMapper mapper,
                                  WeightedCodebook codebook) throws IOException
    {   
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
        double [] targetLsfs = null;
        
        windowIn = new DynamicWindow(params.lsfParams.windowType);
        windowOut = new DynamicWindow(params.lsfParams.windowType);
        
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
        int desiredFrameInd = 70;
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
                frm = SignalProcUtils.applyPreemphasis(frm, params.lsfParams.preCoef);

                // Compute LPC coefficients
                LPCoeffs inputLPCoeffs = LPCAnalyser.calcLPC(frm, params.lsfParams.lpOrder);
                double[] inputLpcs = inputLPCoeffs.getOneMinusA();
                double[] inputLsfs = LineSpectralFrequencies.lpc2lsfInHz(inputLpcs, params.lsfParams.samplingRate); 
                double sqrtInputGain = inputLPCoeffs.getGain();
                
                Complex inputDft = new Complex(fftSize);
                
                System.arraycopy(frm, 0, inputDft.real, 0, Math.min(frmSize, inputDft.real.length));
                
                if (inputDft.real.length > frmSize)
                    Arrays.fill(inputDft.real, inputDft.real.length-frmSize, inputDft.real.length-1, 0);
                
                Arrays.fill(inputDft.imag, 0, inputDft.imag.length-1, 0);
                
                inputDft = FFTMixedRadix.fftComplex(inputDft);
                
                //For checking
                if (false && inputFrameIndex==desiredFrameInd)
                {
                    tmpComp = new Complex(inputDft);
                    tmpSpec = MathUtils.amp2db(tmpComp, 0, maxFreq);
                    MaryUtils.plot(tmpSpec);
                }
                //
                
                Complex inputExpTerm = LPCAnalyser.calcExpTerm(fftSize, params.lsfParams.lpOrder);
                Complex outputExpTerm = LPCAnalyser.calcExpTerm(newFftSize, params.lsfParams.lpOrder);
                double[] inputVocalTractSpectrum = LPCAnalyser.calcSpec(inputLPCoeffs.getA(), params.lsfParams.lpOrder, fftSize, inputExpTerm);
                
                for (k=0; k<maxFreq; k++)
                    inputVocalTractSpectrum[k] *= sqrtInputGain;
                
                //For checking
                if (false && inputFrameIndex==desiredFrameInd)
                {
                    tmpSpec = new double[maxFreq];
                    System.arraycopy(inputVocalTractSpectrum, 0, tmpSpec, 0, tmpSpec.length);
                    tmpSpec = MathUtils.amp2db(tmpSpec);
                    MaryUtils.plot(tmpSpec);
                }
                //
                
                Complex inputResidual = new Complex(fftSize);
                
                // Filter out vocal tract to obtain the input residual spectrum
                for (k=0; k<maxFreq; k++)
                {
                    inputResidual.real[k] = inputDft.real[k]/inputVocalTractSpectrum[k];
                    inputResidual.imag[k] = inputDft.imag[k]/inputVocalTractSpectrum[k];
                }

                //For checking
                if (false && inputFrameIndex==desiredFrameInd)
                {
                    tmpComp = new Complex(inputResidual);
                    tmpSpec = MathUtils.amp2db(tmpComp, 0, newMaxFreq-1);
                    MaryUtils.plot(tmpSpec);
                }
                //
                
                //First transform then interpolate!
                targetLsfs = mapper.transform(inputLsfs, codebook);
                //targetLsfs = new double[inputLsfs.length]; //Check: use original lsfs to see if it is able to resynthesize the original back
                //System.arraycopy(inputLsfs, 0, targetLsfs, 0, inputLsfs.length);
                
                double [] targetLpcs = LineSpectralFrequencies.lsfInHz2lpc(targetLsfs, params.lsfParams.samplingRate);
                
                double[] targetVocalTractSpectrum = null;
                if (fftSize!=newFftSize)
                {
                    Complex expTermNew = LPCAnalyser.calcExpTerm(newFftSize, params.lsfParams.lpOrder);
                    targetVocalTractSpectrum = LPCAnalyser.calcSpecFromOneMinusA(targetLpcs, 1.0f, newFftSize, outputExpTerm);
                }
                else
                    targetVocalTractSpectrum = LPCAnalyser.calcSpecFromOneMinusA(targetLpcs, 1.0f, newFftSize, inputExpTerm);

                for (k=0; k<newMaxFreq; k++)
                    targetVocalTractSpectrum[k] *= sqrtInputGain;
                
                //For checking
                if (false && inputFrameIndex==desiredFrameInd)
                {
                    tmpSpec = new double[newMaxFreq];
                    System.arraycopy(targetVocalTractSpectrum, 0, tmpSpec, 0, tmpSpec.length);
                    tmpSpec = MathUtils.amp2db(tmpSpec);
                    MaryUtils.plot(tmpSpec);
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
                    
                    double[] tempSpectrum = new double[newMaxFreq];
                    
                    for (k=0; k<newMaxFreq; k++)
                    {
                        wInd = (int)Math.floor((k+1)/newVScales[k]+0.5); //Find new indices
                        if (wInd<1)
                            wInd = 1;
                        if (wInd>newMaxFreq)
                            wInd = newMaxFreq;
                        
                        tempSpectrum[k] = targetVocalTractSpectrum[wInd-1];
                    }
                    
                    System.arraycopy(tempSpectrum, 0, targetVocalTractSpectrum, 0, newMaxFreq);
                }

                //Create output DFT spectrum
                Complex outputResidual = new Complex(newFftSize);
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
                if (false && inputFrameIndex==desiredFrameInd)
                {
                    tmpComp = new Complex(outputResidual);
                    tmpSpec = MathUtils.amp2db(tmpComp, 0, newMaxFreq-1);
                    MaryUtils.plot(tmpSpec);
                }
                //
                
                //Filter the output residual with the estimated target vocal tract spectrum
                Complex outputDft = new Complex(newFftSize);
                
                for (k=1; k<=newMaxFreq; k++)
                {
                    outputDft.real[k-1] = outputResidual.real[k-1]*targetVocalTractSpectrum[k-1];
                    outputDft.imag[k-1] = outputResidual.imag[k-1]*targetVocalTractSpectrum[k-1];
                }
                
                for (k=newMaxFreq+1; k<=newFftSize; k++)
                {
                    outputDft.real[k-1] = outputDft.real[2*newMaxFreq-1-k];
                    outputDft.imag[k-1] = -outputDft.imag[2*newMaxFreq-1-k];
                }
                
                //For checking
                if (false && inputFrameIndex==desiredFrameInd)
                {
                    tmpComp = new Complex(outputDft);
                    tmpSpec = MathUtils.amp2db(tmpComp, 0, newMaxFreq);
                    MaryUtils.plot(tmpSpec);
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
            
            frmy = SignalProcUtils.removePreemphasis(frmy, params.lsfParams.preCoef);
            
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
                if (!isFixedRateVocalTractTransformation)
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
                            if (isWavFileOutput)
                                dout.writeDouble(outBuff, 0, outBuffLen);
                            else
                            { 
                                if (output == null)
                                {
                                    output = new double[outBuffLen];
                                    System.arraycopy(outBuff, 0, output, 0, outBuffLen);
                                }
                                else
                                {
                                    outputTmp = new double[output.length];
                                    System.arraycopy(output, 0, outputTmp, 0, output.length);
                                    output = new double[outputTmp.length + outBuffLen];
                                    System.arraycopy(outputTmp, 0, output, 0, outputTmp.length);
                                    System.arraycopy(outBuff, 0, output, outputTmp.length, outBuffLen);
                                }
                            }
                            
                            totalWrittenToFile += outBuffLen;
                        }
                        else
                        {   
                            if (isWavFileOutput)
                                dout.writeDouble(outBuff, 0, origLen-totalWrittenToFile);
                            else
                            { 
                                if (output == null)
                                {
                                    output = new double[origLen-totalWrittenToFile];
                                    System.arraycopy(outBuff, 0, output, 0, origLen-totalWrittenToFile);  
                                }
                                else
                                {
                                    outputTmp = new double[output.length];
                                    System.arraycopy(output, 0, outputTmp, 0, output.length);
                                    output = new double[outputTmp.length + origLen-totalWrittenToFile];
                                    System.arraycopy(outputTmp, 0, output, 0, outputTmp.length);
                                    System.arraycopy(outBuff, 0, output, outputTmp.length, origLen-totalWrittenToFile);
                                }
                            }
                            
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
}
