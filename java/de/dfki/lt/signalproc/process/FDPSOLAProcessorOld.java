package de.dfki.lt.signalproc.process;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.signalproc.FFTMixedRadix;
import de.dfki.lt.signalproc.analysis.F0ReaderWriter;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.LEDataInputStream;
import de.dfki.lt.signalproc.util.LEDataOutputStream;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.MathUtils.Complex;
import de.dfki.lt.signalproc.window.Window;
import de.dfki.lt.signalproc.window.DynamicWindow;
import de.dfki.lt.signalproc.window.DynamicTwoHalvesWindow;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.util.InterpolationUtils;
import de.dfki.lt.signalproc.analysis.PitchMarker;

public class FDPSOLAProcessorOld extends VocalTractModifier {
    private DoubleDataSource input;
    private AudioInputStream inputAudio;
    private DDSAudioInputStream outputAudio;
    private VoiceModificationParametersPreprocessor modParams;
    private int numfrm;
    private int numfrmFixed;
    private int lpOrder; //LP analysis order
    private String outputFile;
    private String tempOutBinaryFile;
    private int origLen;
    private PitchMarker pm;
    private PSOLAFrameProvider psFrm;
    private double wsFixed;
    private double ssFixed;
    private int numPeriods;
    private static int NUM_PITCH_SYNC_PERIODS = 3;
    
    private boolean bSilent;
    private LEDataOutputStream dout; 
    private LEDataInputStream din; 
    private DynamicWindow windowIn;
    private DynamicWindow windowOut;
    private double [] wgt;
    private double [] wgty;
    
    private int frmSize;
    private int newFrmSize;
    private int newPeriod;
    private int synthFrmInd;
    private double localDurDiff;
    private int repeatSkipCount; // -1:skip frame, 0:no repetition (use synthesized frame as it is), >0: number of repetitions for synthesized frame
    private double localDurDiffSaved;
    private double sumLocalDurDiffs;
    private double nextAdd;

    private int synthSt;
    private int synthTotal;
    
    private int maxFrmSize;
    private int maxNewFrmSize;
    private int synthFrameInd;
    private boolean bLastFrame;
    private boolean bBroke;
    private int newFftSize;
    private int newMaxFreq;
    
    private int outBuffLen;
    private double [] outBuff;
    private int outBuffStart;
    private int totalWrittenToFile;

    private double [] ySynthBuff;
    private double [] wSynthBuff;
    private int ySynthInd;
    private double [] frm;
    private double [] frm2;
    private boolean bWarp;
    
    private double [] py;
    private double [] py2;
    private Complex hy;
    private double [] frmy;
    private double frmEn;
    private double frmyEn;
    private double gain;
    private int newSkipSize;
    private int halfWin;
    private double [] newVScales;
    
    public FDPSOLAProcessorOld(String strInputFile, String strPitchFile, String strOutputFile,
                            double [] pscales, double [] tscales, double [] escales, double [] vscales) throws UnsupportedAudioFileException, IOException
    {
        super();
        
        inputAudio = AudioSystem.getAudioInputStream(new File(strInputFile));
        input = new AudioDoubleDataSource(inputAudio);
        
        origLen = (int)input.getDataLength();
        fs = (int)inputAudio.getFormat().getSampleRate();
        lpOrder = SignalProcUtils.getLPOrder(fs);
        
        wsFixed = 0.02;
        ssFixed = 0.01;
        numPeriods = NUM_PITCH_SYNC_PERIODS;
        
        F0ReaderWriter f0 = new F0ReaderWriter(strPitchFile);
        pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, fs, origLen, f0.header.ws, f0.header.ss, true);
        
        numfrm = pm.pitchMarks.length-numPeriods; //Total pitch synchronous frames (This is the actual number of frames to be processed)
        numfrmFixed = (int)(Math.floor(((double)(origLen + pm.totalZerosToPadd)/fs-0.5*wsFixed)/ssFixed+0.5)+2); //Total frames if the analysis was fixed skip-rate
         
        modParams = new VoiceModificationParametersPreprocessor(fs, lpOrder,
                                                                pscales, tscales, escales, vscales,
                                                                pm.pitchMarks, wsFixed, ssFixed,
                                                                numfrm, numfrmFixed, numPeriods, false);
        
        outputFile = strOutputFile; 
        
        initialise();
    }
    
    public void initialise() throws FileNotFoundException
    {        
        bSilent = false;
        
        //double [] tmpx = input.getAllData();
        //double [] x = new double[origLen+pm.totalZerosToPadd];
        //Arrays.fill(x, 0.0);
        //System.arraycopy(tmpx, 0, x, 0, origLen);
        
        psFrm = new PSOLAFrameProvider(input, pm, modParams.fs, modParams.numPeriods);
        
        tempOutBinaryFile = outputFile + ".bin";
        dout = new LEDataOutputStream(tempOutBinaryFile);
        
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
        
        maxFrmSize = (int)(modParams.numPeriods*modParams.fs/40.0);
        if ((maxFrmSize % 2) != 0)
            maxFrmSize++;
        
        maxNewFrmSize = (int)(Math.floor(maxFrmSize/MathUtils.min(modParams.pscalesVar)+0.5));
        if ((maxNewFrmSize % 2) != 0) 
            maxNewFrmSize++;

        synthFrameInd = 0;
        bLastFrame = false;
        bBroke = false;
        fftSize = (int)Math.pow(2, (Math.ceil(Math.log((double)maxFrmSize)/Math.log(2.0))));
        maxFreq = fftSize/2+1;
        
        outBuffLen = 100;
        outBuff = MathUtils.zeros(outBuffLen);
        outBuffStart = 1;
        totalWrittenToFile = 0;

        ySynthBuff = MathUtils.zeros(maxNewFrmSize);
        wSynthBuff = MathUtils.zeros(maxNewFrmSize);
        ySynthInd = 1;
    }
    public void fdpsolaOnline() throws IOException
    {   
        int kMax;
        int i, j, k, n, m;
        int tmpFix, tmpAdd, tmpMul;
        int wInd;

        double [] tmpvsc = new double[1];
        int remain;
        int kInd; 
       
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
            
            if (i==numfrm-1)
            {
                repeatSkipCount++;
                bLastFrame = true;
            }
            
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

                    //Compute LP and excitation spectrum
                    initialise(modParams.lpOrder, modParams.fs, fftSize, true); //Perform only analysis
                    windowIn.applyInline(frm, 0, frmSize); //Windowing
                    applyInline(frm, 0, frmSize); //LP analysis
                    
                    //Expand/Compress the vocal tract spectrum in inverse manner
                    py = MathUtils.interpolate(vtSpectrum, newMaxFreq); //Interpolated vocal tract spectrum

                    //Perform vocal tract scaling
                    if (bWarp)
                    {
                        tmpvsc[0] = modParams.vscalesVar[i];
                        newVScales = InterpolationUtils.modifySize(tmpvsc, newMaxFreq); //Modify length to match current length of spectrum
                        
                        for (k=0; k<newVScales.length; k++)
                        {
                            if (newVScales[k]<0.05) //Put a floor to avoid divide by zero
                                newVScales[k]=0.05;
                        }
                        
                        py2 = new double[newMaxFreq];
                        
                        for (k=0; k<newMaxFreq; k++)
                        {
                            wInd = (int)Math.floor((k+1)/newVScales[k]+0.5); //Find new indices
                            if (wInd<1)
                                wInd = 1;
                            if (wInd>newMaxFreq)
                                wInd = newMaxFreq;
                            
                            py2[k] = py[wInd-1];
                        }
                        
                        System.arraycopy(py2, 0, py, 0, newMaxFreq);
                    }

                    //Create output DFT spectrum
                    hy = new Complex(newFftSize);
                    hy.real = MathUtils.zeros(newFftSize);
                    hy.imag = MathUtils.zeros(newFftSize);

                    System.arraycopy(this.h.real, 0, hy.real, 0, Math.min(maxFreq, newFftSize));
                    System.arraycopy(this.h.imag, 0, hy.imag, 0, Math.min(maxFreq, newFftSize));

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
                            hy.real[j-1] = this.h.real[tmpMul*(tmpFix-j)+tmpAdd-1];
                            hy.imag[j-1] = this.h.imag[tmpMul*(tmpFix-j)+tmpAdd-1];
                        }
                    }

                    hy.real[newMaxFreq-1] = Math.sqrt(hy.real[newMaxFreq-1]*hy.real[newMaxFreq-1] + hy.imag[newMaxFreq-1]*hy.imag[newMaxFreq-1]);
                    hy.imag[newMaxFreq-1] = 0.0;
                    
                    //Convolution
                    for (k=1; k<=newMaxFreq; k++)
                    {
                        hy.real[k-1] *= py[k-1];
                        hy.imag[k-1] *= py[k-1];
                    }
                    
                    for (k=newMaxFreq+1; k<=newFftSize; k++)
                    {
                        hy.real[k-1] = hy.real[2*newMaxFreq-1-k];
                        hy.imag[k-1] = -hy.imag[2*newMaxFreq-1-k];
                    }

                    //Convert back to time domain
                    //FFT.transform(hy.real, hy.imag, true);
                    //hy = FFTArbitraryLength.ifft(hy);
                    hy = FFTMixedRadix.ifft(hy);
                    
                    frmy = new double[newFrmSize];
                    System.arraycopy(hy.real, 0, frmy, 0, newFrmSize);
                    
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
                totalWrittenToFile += outBuffStart-1;
            }
            else
            {
                dout.writeDouble(outBuff, 0, origLen-totalWrittenToFile);
                totalWrittenToFile = origLen;
            }
        }
        //

        dout.close();
        
        //Read the temp binary file into a wav file and delete the temp binary file
        din = new LEDataInputStream(tempOutBinaryFile);
        double [] yOut = din.readDouble(totalWrittenToFile);
        din.close();
        
        double tmpMax = MathUtils.getAbsMax(yOut);
        if (tmpMax>32735)
        {
            for (i=0; i<yOut.length; i++)
                yOut[i] *= 32735/tmpMax;
        }
        
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(yOut), inputAudio.getFormat());
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outputFile));
        
        File tmpFile = new File(tempOutBinaryFile);
        tmpFile.delete();
        //
    }
    
    public static void main(String[] args) throws Exception
    {  
        String strOutputFile = args[0].substring(0, args[0].length()-4) + "_fdJavOld.wav";
        String strPitchFile = args[0].substring(0, args[0].length()-4) + ".ptc";
        
        double [] pscales = {1.2, 0.3};
        double [] tscales = {1.5};
        double [] escales = {1.0};
        double [] vscales = {1.8, 0.4};
       
        FDPSOLAProcessorOld fd = new FDPSOLAProcessorOld(args[0], strPitchFile, strOutputFile, 
                                                    pscales, tscales, escales, vscales);
        
        fd.fdpsolaOnline();
        
    }
}
