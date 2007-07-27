package de.dfki.lt.signalproc.process;

import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.signalproc.FFT;
import de.dfki.lt.signalproc.FFTArbitraryLength;
import de.dfki.lt.signalproc.analysis.F0Reader;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.LEDataOutputStream;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.MathUtils.Complex;
import de.dfki.lt.signalproc.window.Window;
import de.dfki.lt.signalproc.window.DynamicWindow;
import de.dfki.lt.signalproc.window.DynamicTwoHalvesWindow;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.util.InterpolationUtils;
import de.dfki.lt.signalproc.analysis.PitchMarker;

public class FDPSOLAProcessor extends VocalTractModifier {
    private DoubleDataSource input;
    private DoubleDataSource input2;
    private String outputFile;
    private VoiceModificationParametersPreprocessor modParams;
    private int numfrm;
    private String outFile;
    private int origLen;
    PitchMarker pm;
    PSOLAFrameProvider psFrm;
    
    public FDPSOLAProcessor(DoubleDataSource in, DoubleDataSource in2 , String strOutFile, PitchMarker pmIn, 
            VoiceModificationParametersPreprocessor modParamsIn, int numfrmIn)
    {
        super();
        this.modParams = modParamsIn;
        this.input = in;
        this.input2 = in2;
        this.outputFile = outFile;
        this.numfrm = numfrmIn;
        this.outFile = strOutFile;
        this.origLen = (int)in.getDataLength();
        this.pm = pmIn;
    }
    
    public void fdpsolaOnline() throws IOException
    {   
        boolean bSilent = false;
        double [] tmpx = input.getAllData();
        double [] x = new double[origLen+pm.totalZerosToPadd];
        Arrays.fill(x, 0.0);
        System.arraycopy(tmpx, 0, x, 0, origLen);
        
        psFrm = new PSOLAFrameProvider(input2, pm, modParams.fs, modParams.numPeriods);
        
        LEDataOutputStream dout = new LEDataOutputStream(outFile);
        
        DynamicWindow windowIn = new DynamicWindow(Window.HANN);
        DynamicWindow windowOut = new DynamicWindow(Window.HANN);
        double [] wgt;
        double [] wgty;
        
        int frmSize = 0;
        int newFrmSize = 0;
        int newPeriod = 0;
        int synthFrmInd = 0;
        double localDurDiff = 0.0;
        int repeatSkipCount = 0; // -1:skip frame, 0:no repetition (use synthesized frame as it is), >0: number of repetitions for synthesized frame
        int prevRepeatSkipCount = 0;
        double localDurDiffSaved = 0.0;
        double sumLocalDurDiffs = 0.0;
        double nextAdd = 0.0;

        int synthSt = pm.pitchMarks[0];
        int synthTotal = 0;

        //Overlap-add synthesis
        int maxFrmSize = (int)(modParams.numPeriods*modParams.fs/40.0);
        if ((maxFrmSize % 2) != 0)
            maxFrmSize++;
        
        int maxNewFrmSize = (int)(Math.floor(maxFrmSize/MathUtils.min(modParams.pscalesVar)+0.5));
        if ((maxNewFrmSize % 2) != 0) 
            maxNewFrmSize++;

        int synthFrameInd = 0;
        boolean bLastFrame = false;
        boolean bBroke = false;
        fftSize = (int)Math.pow(2, (Math.ceil(Math.log((double)maxFrmSize)/Math.log(2.0))));
        maxFreq = fftSize/2+1;
        int newFftSize;
        int newMaxFreq;
        
        int outBuffLen = 100;
        double [] outBuff = MathUtils.zeros(outBuffLen);
        int outBuffStart = 1;
        int totalWrittenToFile = 0;

        double [] ySynthBuff = MathUtils.zeros(maxNewFrmSize);
        double [] wSynthBuff = MathUtils.zeros(maxNewFrmSize);
        int ySynthInd = 1;
        double [] frm;
        boolean bWarp;
        int kMax;
        int i, j, k, n, m;
        int tmpFix, tmpAdd, tmpMul;
        double [] tmpVScales;
        int wInd;
        double [] Py;
        double [] Py2;
        Complex Hy;
        double [] frmy;
        double frmEn;
        double frmyEn;
        double gain;
        int newSkipSize;
        int halfWin;
        double [] tmpvsc = new double[1];
        int remain;
        int kInd;
        double [] frm2;
       
        for (i=0; i<numfrm; i++)
        {   
            frm2 = psFrm.getNextFrame();
            
            if (bBroke)
                break;

            repeatSkipCount = 0; // -1:skip frame, 0:no repetition (use synthesized frame as it is), >0: number of repetitions for synthesized frame

            // Compute new frame sizes, change in durations due to pitch scaling, and required compensation amount in samples
            // &
            // Find out which pitch-scaled frames to repeat/skip for overall duration
            // compensation
            frmSize = pm.pitchMarks[i+modParams.numPeriods]-pm.pitchMarks[i]+1;
            if ((frmSize % 2) !=0) 
                frmSize++;
            if (frmSize<4)
                frmSize = 4;

            if (pm.vuvs[i])
            {
                newFrmSize = (int)(Math.floor(frmSize/modParams.pscalesVar[i]+0.5));
                if ((newFrmSize % 2) !=0)
                    newFrmSize++;
                if (newFrmSize<4)
                    newFrmSize = 4;
            }
            else
                newFrmSize = frmSize;

            newPeriod = (int)Math.floor(((double)newFrmSize)/modParams.numPeriods+0.5);
            //Compute duration compensation required:
            // localDurDiffs(i) = (DESIRED)-(AFTER PITCHSCALING)
            // (-) if expansion occured, (+) if compression occured
            // We aim to make this as close to zero as possible in the following duration compensation step
            localDurDiff = nextAdd + (frmSize*modParams.tscalesVar[i]-newFrmSize)/modParams.numPeriods;

            nextAdd = 0;
            if (localDurDiff<-0.1*newPeriod) //Expansion occured so skip this frame
            {
                repeatSkipCount--;
                if (i<numfrm-1)
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

                if (i<numfrm-1)
                {
                    nextAdd = localDurDiff;
                    localDurDiff = 0;
                }
            }

            sumLocalDurDiffs += localDurDiff;
            
            if (i==numfrm-1)
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
            
            if (i==numfrm-1 && repeatSkipCount<0 && prevRepeatSkipCount<0)
            {
                repeatSkipCount=0;
                bLastFrame = true;
            }

            prevRepeatSkipCount = repeatSkipCount;
            
            if (repeatSkipCount>-1)
            {
                frm = MathUtils.zeros(frmSize);
                //System.arraycopy(x, pm.pitchMarks[i], frm, 0, Math.min(frmSize, origLen-pm.pitchMarks[i]));
                System.arraycopy(frm2, 0, frm, 0, frmSize);
                wgt = windowIn.values(frmSize);

                if (modParams.vscalesVar[i] != 1.0)
                    bWarp=true; 
                else
                    bWarp=false; 
                
                if ((pm.vuvs[i] && modParams.pscalesVar[i]!=1.0) || bWarp)
                {
                    newMaxFreq = (int)Math.floor(maxFreq/modParams.pscalesVar[i]+0.5);
                    
                    if (newMaxFreq<3)
                        newMaxFreq=3;
                    
                    if ((newMaxFreq % 2) !=1)
                        newMaxFreq++;
                    
                    //This is for being able to use the FFT algorithm that works only with buffers of length power of two
                    //If you have an FFT algorithm that works with any buffer size, simply remove this line
                    //newMaxFreq = (int)Math.floor(0.5*MathUtils.closestPowerOfTwoAbove(2*(newMaxFreq-1))+1.5);
                    //
                    
                    newFftSize = 2*(newMaxFreq-1);

                    frmEn = SignalProcUtils.getEnergy(frm);
                    //[Px, G, alpha, frm, wgt] = frm2lpspec(frm, modParams.fs, P, fftSize, winType);

                    //Compute LP and excitation spectrum
                    initialise(modParams.P, modParams.fs, fftSize, true); //Perform only analysis
                    windowIn.applyInline(frm, 0, frmSize); //Windowing
                    applyInline(frm, 0, frmSize); //LP analysis
                    
                    //Hx = fft(frm, fftSize); //DFT spectrum (complex valued)
                    //Hx(1:maxFreq) = Hx(1:maxFreq)./Px(1:maxFreq); //Excitation spectrum (complex valued)

                    //Expand/Compress the vocal tract spectrum in inverse manner
                    //Py = MathUtils.interpolate(Px, newMaxFreq);
                    Py = MathUtils.interpolate(vtSpectrum, newMaxFreq); //Interpolated vocal tract spectrum

                    //Perform vocal tract scaling
                    if (bWarp)
                    {
                        tmpvsc[0] = modParams.vscalesVar[i];
                        tmpVScales = InterpolationUtils.modifySize(tmpvsc, newMaxFreq); //Modify length to match current length of spectrum
                        
                        for (k=0; k<tmpVScales.length; k++)
                        {
                            if (tmpVScales[k]<0.05) //Put a floor to avoid divide by zero
                                tmpVScales[k]=0.05;
                        }
                        
                        Py2 = new double[newMaxFreq];
                        
                        for (k=0; k<newMaxFreq; k++)
                        {
                            wInd = (int)Math.floor((k+1)/tmpVScales[k]+0.5); //Find new indices
                            if (wInd<1)
                                wInd = 1;
                            if (wInd>newMaxFreq)
                                wInd = newMaxFreq;
                            
                            Py2[k] = Py[wInd-1];
                        }
                        
                        System.arraycopy(Py2, 0, Py, 0, newMaxFreq);
                    }

                    //Create output DFT spectrum
                    Hy = new Complex(newFftSize);
                    Hy.real = MathUtils.zeros(newFftSize);
                    Hy.imag = MathUtils.zeros(newFftSize);

                    System.arraycopy(this.real, 0, Hy.real, 0, Math.min(maxFreq, newFftSize));
                    System.arraycopy(this.imag, 0, Hy.imag, 0, Math.min(maxFreq, newFftSize));

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
                            Hy.real[j-1] = this.real[tmpMul*(tmpFix-j)+tmpAdd-1];
                            Hy.imag[j-1] = this.imag[tmpMul*(tmpFix-j)+tmpAdd-1];
                        }
                    }

                    Hy.real[newMaxFreq-1] = Math.sqrt(Hy.real[newMaxFreq-1]*Hy.real[newMaxFreq-1] + Hy.imag[newMaxFreq-1]*Hy.imag[newMaxFreq-1]);
                    Hy.imag[newMaxFreq-1] = 0.0;
                    
                    //Convolution
                    for (k=1; k<=newMaxFreq; k++)
                    {
                        Hy.real[k-1] *= Py[k-1];
                        Hy.imag[k-1] *= Py[k-1];
                    }
                    
                    for (k=newMaxFreq+1; k<=newFftSize; k++)
                    {
                        Hy.real[k-1] = Hy.real[2*newMaxFreq-1-k];
                        Hy.imag[k-1] = -Hy.imag[2*newMaxFreq-1-k];
                    }

                    //Convert back to time domain
                    //FFT.transform(Hy.real, Hy.imag, true);
                    Hy = FFTArbitraryLength.ifft(Hy);
                    
                    frmy = new double[newFrmSize];
                    System.arraycopy(Hy.real, 0, frmy, 0, newFrmSize);
                    
                    frmyEn = SignalProcUtils.getEnergy(frmy);
                    gain = (frmEn/Math.sqrt(frmSize))/(frmyEn/Math.sqrt(newFrmSize))*modParams.escalesVar[i];
                }
                else
                {
                    if (frmSize<newFrmSize)
                        newFrmSize = frmSize; 
                    
                    frmy = new double[newFrmSize];
                    
                    for (k=0; k<frmSize; k++)
                        frmy[k] = frm[k]*wgt[k];

                    gain = modParams.escalesVar[i];
                }            

                //Energy scale compensation + modification
                for (k=0; k<newFrmSize; k++)
                    frmy[k] *= gain;

                for (j=1; j<=repeatSkipCount+1; j++)
                {
                    if (pm.vuvs[i])
                        newSkipSize = (int)Math.floor((pm.pitchMarks[i+1]-pm.pitchMarks[i])/modParams.pscalesVar[i]+0.5);
                    else
                        newSkipSize = (int)Math.floor((pm.pitchMarks[i+1]-pm.pitchMarks[i])+0.5);

                    if ((i==numfrm-1 && j==repeatSkipCount+1)) //| (i~=numfrm & all(repeatSkipCounts(i+1:numfrm)==-1)))
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
                            System.out.println("Synthesized using frame "  + String.valueOf(i+1) + " of " + String.valueOf(numfrm) + " " + String.valueOf(pm.pitchMarks[i]) + " " + String.valueOf(pm.pitchMarks[i+modParams.numPeriods])); 
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
                            System.out.println("Synthesized using frame " + String.valueOf(i+1) + " of " + String.valueOf(numfrm) + " " + String.valueOf(pm.pitchMarks[i]) + " " + String.valueOf(pm.pitchMarks[i+modParams.numPeriods])); 
                    }
                    else //Normal frame
                    {
                        if (!pm.vuvs[i] && ((repeatSkipCount%2)==1)) //Reverse unvoiced repeated frames once in two consecutive repetitions to reduce distortion
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
                                System.out.println("Synthesized using frame " + String.valueOf(i+1) + " of " + String.valueOf(numfrm) + " " + String.valueOf(pm.pitchMarks[i]) + " " + String.valueOf(pm.pitchMarks[i+modParams.numPeriods])); 
                            else 
                                System.out.println("Repeated using frame " + String.valueOf(i+1) + " of " + String.valueOf(numfrm) + " " + String.valueOf(pm.pitchMarks[i]) + " " + String.valueOf(pm.pitchMarks[i+modParams.numPeriods]));  
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
                            if (modParams.tscaleSingle!=1.0 || totalWrittenToFile+outBuffLen<=origLen)
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
                    System.out.println("Skipped frame " + String.valueOf(i+1) + " of " + String.valueOf(numfrm));
            }
        }

        if (modParams.tscaleSingle==1.0)
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
                if (modParams.tscaleSingle!=1.0 || totalWrittenToFile+outBuffLen<=origLen)
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
            if (modParams.tscaleSingle!=1.0 || totalWrittenToFile+outBuffStart-1<=origLen)
            {
                dout.writeDouble(outBuff, 0, outBuffStart-1);
                totalWrittenToFile += outBuffStart;
            }
            else
            {
                dout.writeDouble(outBuff, 0, origLen-totalWrittenToFile-1);
                totalWrittenToFile = origLen;
            }
        }
        //

        dout.close();
    }
    
    public static void main(String[] args) throws Exception
    {  
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        AudioInputStream inputAudio2 = AudioSystem.getAudioInputStream(new File(args[0]));
        int fs = (int)inputAudio.getFormat().getSampleRate();
        int P = SignalProcUtils.getLPOrder(fs);
        AudioDoubleDataSource in = new AudioDoubleDataSource(inputAudio);
        AudioDoubleDataSource in2 = new AudioDoubleDataSource(inputAudio2);
        String strOutFile = args[0].substring(0, args[0].length()-4) + "_fdJav.wav";
        String ptcFile = args[0].substring(0, args[0].length()-4) + ".ptc";
        F0Reader f0 = new F0Reader(ptcFile);
        PitchMarker pm = SignalProcUtils.pitchContour2pitchMarks(f0.getContour(), fs, (int)in.getDataLength(), f0.ws, f0.ss, true);
        
        double ws = 0.02;
        double ss = 0.01;
        int numPeriods = 3;
        
        int numfrm = pm.pitchMarks.length-numPeriods; //Total pitch synchronous frames (This is the actual number of frames to be processed)
        int numfrmFixed = (int)(Math.floor(((double)(in.getDataLength()+pm.totalZerosToPadd)/fs-0.5*ws)/ss+0.5)+2); //Total frames if the analysis was fixed skip-rate
        
        double [] pscales = {0.75};
        double [] tscales = {2.0};
        double [] escales = {1.0, 1.2, 1.5, 2.0, 2.5, 3.0};
        double [] vscales = {1.2, 1.5};
        VoiceModificationParametersPreprocessor modParams = new VoiceModificationParametersPreprocessor(fs, P,
                                                                                pscales, tscales, escales, vscales,
                                                                                pm.pitchMarks, ws, ss,
                                                                                numfrm, numfrmFixed, numPeriods);
       
        FDPSOLAProcessor fd = new FDPSOLAProcessor(in, in2, strOutFile, pm, modParams, numfrm);
        fd.fdpsolaOnline();
        
    }
}
