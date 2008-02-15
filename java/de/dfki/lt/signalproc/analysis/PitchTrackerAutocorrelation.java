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

package de.dfki.lt.signalproc.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.signalproc.filter.BandPassFilter;
import de.dfki.lt.signalproc.filter.FIRFilter;
import de.dfki.lt.signalproc.filter.LowPassFilter;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.DynamicWindow;
import de.dfki.lt.signalproc.window.Window;


/**
 * @author oytun.turk
 *
 */
public class PitchTrackerAutocorrelation { 
    public double[] f0s;
    
    protected PitchFileHeader params;
    protected int totalVoicedFrames;
    protected double[] voicingProbabilities;
    protected int minT0Index;
    protected int maxT0Index;
 
    protected double[] prevF0s;
    protected double[] voicedF0s;
    protected double longTermAverageF0;
    protected double shortTermAverageF0;
    
    public static double MAX_SAMPLE = 32767.0;
    public static double MINIMUM_SPEECH_ENERGY = 50.0;
    protected double averageSampleEnergy;
    
    private double[] pitchFrm;
    
    private int frameIndex;
    private int ws;
    private int ss;
    
    public PitchTrackerAutocorrelation(PitchFileHeader paramsIn)
    {
        params = new PitchFileHeader(paramsIn);
        
        init();
    }
    
    public void init()
    {
        int i;
        
        voicingProbabilities = new double[2];
        for (i=0; i<voicingProbabilities.length; i++)
            voicingProbabilities[i] = 0.0;
        
        prevF0s = new double[5];
        
        for (i=0; i<prevF0s.length; i++)
            prevF0s[i] = 0.0;
        
        voicedF0s = new double[20];
        
        for (i=0; i<voicedF0s.length; i++)
            voicedF0s[i] = 0.0;
        
        longTermAverageF0 = 0.5*(params.maximumF0+params.minimumF0);
        shortTermAverageF0 = longTermAverageF0;
        
        frameIndex = 0;
        
        ws =  (int)Math.floor(params.ws*params.fs+0.5);
        ss = (int)Math.floor(params.ss*params.fs+0.5);
        
        pitchFrm = new double[ws];
        minT0Index = (int)Math.floor(params.fs/params.maximumF0+0.5);
        maxT0Index = (int)Math.floor(params.fs/params.minimumF0+0.5);
        
        if (minT0Index<0)
            minT0Index=0;
        if (minT0Index>ws-1)
            minT0Index=ws-1;
        if (maxT0Index<minT0Index)
            maxT0Index=minT0Index;
        if (maxT0Index>ws-1)
            maxT0Index=ws-1;
    }
    
    public PitchFileHeader pitchAnalyzeWavFile(String wavFileIn, String ptcFileOut) throws UnsupportedAudioFileException, IOException
    {   
        pitchAnalyzeWavFile(wavFileIn);
 
        if (f0s!=null)
        {
            params.numfrm = f0s.length;
            F0ReaderWriter.write_pitch_file(ptcFileOut, f0s, (float)(params.ws), (float)(params.ss), params.fs);
        }
        else
            params.numfrm = 0;
        
        return params;
    }
    
    public void pitchAnalyzeWavFile(String wavFile) throws UnsupportedAudioFileException, IOException
    {   
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(wavFile));
        params.fs = (int)inputAudio.getFormat().getSampleRate();
        
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        
        pitchAnalyze(signal.getAllData());
        
        if (f0s!=null)
            params.numfrm = f0s.length;
        else
            params.numfrm = 0;
    }

    private void pitchAnalyze(double[] x)
    {
        init();
        
        if (params.cutOff1>0.0 || params.cutOff2>0.0)
        {
            FIRFilter f = null;
            if (params.cutOff2<=0.0)
                f = new LowPassFilter(params.cutOff1/params.fs);
            else
                f = new BandPassFilter(params.cutOff1/params.fs, params.cutOff2/params.fs);
            
            if (f!=null)
                f.apply(x);            
        }
        
        f0s = null;
        
        int numfrm = (int)Math.floor(((double)x.length-ws)/ss+0.5);
        
        double maxSample = MathUtils.getAbsMax(x);
        
        if (numfrm>0)
            f0s = new double[numfrm];
        
        int i, j;
        frameIndex = 0;
        Arrays.fill(f0s, 0.0);
        
        for (i=0; i<numfrm; i++)
        {
            System.arraycopy(x, i*ss, pitchFrm, 0, Math.min(ws, x.length-i*ss));
            for (j=0; j<ws; j++)
                pitchFrm[j] = (pitchFrm[j]/maxSample)*MAX_SAMPLE + 1e-50*Math.random();
            
            f0s[i] = pitchFrameAutocorrelation(pitchFrm);
             
            frameIndex++;
        }
    }
    
    private double pitchFrameAutocorrelation(double[] frmIn)
    {
        assert pitchFrm.length == frmIn.length;
        System.arraycopy(pitchFrm, 0, frmIn, 0, frmIn.length);
        
        averageSampleEnergy = SignalProcUtils.getAverageSampleEnergy(pitchFrm);
        
        double f0 = 0.0;
        
        double probabilityOfVoicing;
        double tmp;
        int i, j;
         
        if (params.centerClippingRatio>0.0)
            SignalProcUtils.centerClip(pitchFrm, params.centerClippingRatio);
        
        double r0=0.0;
        for (i=0; i<pitchFrm.length; i++)
            r0 += pitchFrm[i]*pitchFrm[i];
        
        int maxIndex=0;
        double maxR=-1.0e10;

        for (i=minT0Index; i<=maxT0Index; i++)
        {
            tmp = 0.0;
            for (j=0; j<pitchFrm.length-i; j++)
                tmp += pitchFrm[j]*pitchFrm[j+i];
            
            if (tmp>maxR)
            {
                maxIndex = i;
                maxR = tmp;
            }
        } 
        
        if (maxIndex==minT0Index || maxIndex==maxT0Index)
            probabilityOfVoicing = 0.0;
        else
            probabilityOfVoicing = maxR/r0;
        
        f0 = ((double)params.fs)/maxIndex;
        
        //look at previous two frame voicing decision to correct F0 estimate 
        if (probabilityOfVoicing>params.voicingThreshold)
        {
            if (voicingProbabilities[0]<params.voicingThreshold && voicingProbabilities[1]>params.voicingThreshold)
                voicingProbabilities[0] = params.voicingThreshold+0.01;
        }
        else if (probabilityOfVoicing>params.voicingThreshold-0.1)
        {
            if (voicingProbabilities[0]>params.voicingThreshold && voicingProbabilities[1]>params.voicingThreshold)
                probabilityOfVoicing = params.voicingThreshold+0.01;
        }
        
        if (probabilityOfVoicing<params.voicingThreshold)
            f0 = 0.0;

        if (averageSampleEnergy < MINIMUM_SPEECH_ENERGY)
            f0 = 0.0;
        
        for (i=voicingProbabilities.length-1; i>0; i--)
            voicingProbabilities[i] = voicingProbabilities[i-1];
        
        voicingProbabilities[0] = probabilityOfVoicing;  
        
        if (f0>10.0)
            totalVoicedFrames++;
        
        if (params.isDoublingCheck || params.isHalvingCheck)
        {
            if (f0>10.0)
            {
                totalVoicedFrames++;
                if (totalVoicedFrames>voicedF0s.length)
                {
                    boolean bNeighVoiced = true;
                    for (i=0; i<voicingProbabilities.length; i++)
                    {
                        if (voicingProbabilities[i]<params.voicingThreshold)
                        {
                            bNeighVoiced = false;
                            break;
                        }
                    }
                    
                    if (bNeighVoiced)
                    {
                        if (params.isDoublingCheck && f0>1.25*longTermAverageF0 && f0>1.33*shortTermAverageF0)
                            f0 *= 0.5;
                        if (params.isHalvingCheck && f0<0.80*longTermAverageF0 && f0<0.66*shortTermAverageF0)
                            f0 *= 2.0;
                    }
                }
            }
        }
        
        if (f0>10.0)
        {
            longTermAverageF0 = 0.99*longTermAverageF0 + 0.01*f0;
            shortTermAverageF0 = 0.90*shortTermAverageF0 + 0.10*f0;
        }
        
        //Smooth the F0 contour both with a median and linear filter  
        
        prevF0s[2] = f0;
        
        boolean bAllVoiced = true;
        for (i=0; i<prevF0s.length; i++)
        {
            if (prevF0s[i]<10.0)
            {    
                bAllVoiced = false;
                break;
            }
        }

        if (bAllVoiced)
        {
            f0 = MathUtils.median(prevF0s);

            tmp = 0.5*prevF0s[2]+0.25*prevF0s[1]+0.25*prevF0s[0];
            if (Math.abs(tmp-f0)<10.0)
                f0 = tmp;

            prevF0s[0] = prevF0s[1];
            prevF0s[1] = prevF0s[2];

            if (totalVoicedFrames==voicedF0s.length)
            {   
                longTermAverageF0 = MathUtils.median(voicedF0s);
                shortTermAverageF0 = longTermAverageF0;
            }  
        }
        
        //System.out.println("Frame=" + String.valueOf(frameIndex) + " " + String.valueOf(averageSampleEnergy) + " " + String.valueOf(probabilityOfVoicing) + " " + String.valueOf(f0));
        
        return f0;
    }
}
