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

package de.dfki.lt.mary.sinusoidal.pitch;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.sinusoidal.PitchSynchronousSinusoidalAnalyzer;
import de.dfki.lt.mary.sinusoidal.SinusoidalAnalyzer;
import de.dfki.lt.mary.sinusoidal.SinusoidalSpeechSignal;
import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.signalproc.analysis.F0ReaderWriter;
import de.dfki.lt.signalproc.analysis.PitchMarker;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.Window;

/**
 * @author oytun.turk
 * Initial pitch, voicing, maximum frequency of voicing, and refined pitch estimation
 * as described in:
 * Stylianou, Y., "A Pitch and Maximum Voiced Frequency Estimation Technique adapted to Harmonic Models of Speech".
 */
public class HNMPitchVoicingAnalyzer {
    public float [] initialF0s;
    public float [] f0s;
    public boolean [] voicings;
    public float [] maxFrequencyOfVoicings;
    
    public HNMPitchVoicingAnalyzer()
    {
        
    }
    
    public void estimateInitialPitch(double [] x, int samplingRate, float windowSizeInSeconds, float skipSizeInSeconds, 
                                     float f0MinInHz, float f0MaxInHz, float searchStepInHz) 
    {
        int PMax = (int)Math.floor(samplingRate/f0MinInHz+0.5);
        int PMin = (int)Math.floor(samplingRate/f0MaxInHz+0.5);
        
        int ws = (int)Math.floor(windowSizeInSeconds*samplingRate + 0.5);
        ws = Math.max(ws, (int)Math.floor(2.5*PMin+0.5));
        int ss = (int)Math.floor(skipSizeInSeconds*samplingRate + 0.5);
        int numfrm = (int)Math.floor(((double)x.length-ws)/ss+0.5);

        int numCandidates = PMax-PMin+1;
        
        double [] E = new double[numCandidates];
        int [][] minInds = new int[numfrm][];
        double [][] minEs = new double[numfrm][];
        
        int P;
        int i, t, l, k;
        double term1, term2, term3, r;

        double [] frm = new double[ws];
        Window win = Window.get(Window.HANN, ws);
        double [] wgt2 = win.getCoeffs();
        
        for (t=0; t<ws; t++)
            wgt2[t] = wgt2[t]*wgt2[t];
        
        double tmpSum = 0.0;
        for (t=0; t<ws; t++)
            tmpSum += wgt2[t];
        
        for (t=0; t<ws; t++)
            wgt2[t] = wgt2[t]/tmpSum;
        
        double [] wgt4 = new double[ws];
        System.arraycopy(wgt2, 0, wgt4, 0, ws);
        for (t=0; t<ws; t++)
            wgt4[t] = wgt4[t]*wgt4[t];
        
        double termTmp = 0.0;
        for (t=0; t<ws; t++)
            termTmp += wgt4[t];
        
        initialF0s = new float[numfrm]; 

        for (i=0; i<numfrm; i++)
        {
            Arrays.fill(frm, 0.0);
            System.arraycopy(x, i*ss, frm, 0, Math.min(ws, x.length-i*ss));
            
            //MaryUtils.plot(frm);
            
            term1 = 0.0;
            for (t=0; t<ws; t++)
                term1 += frm[t]*frm[t]*wgt2[t];
            
            for (P=PMin; P<=PMax; P++)
            {
                term2 = 0.0;
                for (l=0; l<ws; l++)
                {
                    r=0.0;
                    for (t=0; t<ws-l*P; t++)
                        r += frm[t]*wgt2[t]*frm[t+l*P]*wgt2[t+l*P];

                    term2 += r;
                }
                term2 *= P;

                term3 = 1.0-P*termTmp;

                E[P-PMin] = (term1-term2)/(term1*term3);
            }

            //MaryUtils.plot(E, true, 1000);
            
            minInds[i] = MathUtils.getExtrema(E, 2, 2, false);
            if (minInds[i]!=null)
            {
                minEs[i] = new double[minInds[i].length];
                for (t=0; t<minInds[i].length; t++)
                    minEs[i][t] = E[minInds[i][t]];
            }
            else
                minEs[i] = null;
        }

        //Search for local minimum error paths to assign pitch values
        //Previous and next <neigh> neighbors are used for searching of the local total minimum
        int neigh = 2;
        int [] totalNodes = new int[2*neigh+1];
        int minLocalEInd;
        int [][] pathInds;
        double [] localEs;
        
        for (i=0; i<numfrm; i++)
        {
            if (minEs!=null && minInds[i]!=null)
            {
                for (t=-neigh; t<=neigh; t++)
                {
                    if (i+t>=0 && i+t<minEs.length && minEs[i+t]!=null)
                        totalNodes[t+neigh] = minEs[i+t].length;
                    else
                        totalNodes[t+neigh] = 1;
                }

                //Here is a factorial design of all possible paths
                pathInds = MathUtils.factorialDesign(totalNodes);
                localEs = new double[pathInds.length];

                for (k=0; k<pathInds.length; k++)
                {
                    localEs[k] = 0.0;
                    for (t=0; t<pathInds[k].length; t++)
                    {
                        //System.out.println(String.valueOf(i) + " " + String.valueOf(k) + " " + String.valueOf(t) + " ");
                        
                        if (minEs!=null)
                            if (i-neigh+t>=0)
                                if (i-neigh+t<minEs.length)
                                    if (minEs[i-neigh+t]!=null)
                                        if (pathInds[k][t]<minEs[i-neigh+t].length)
                                            localEs[k] += minEs[i-neigh+t][pathInds[k][t]];
                    }
                }

                minLocalEInd = MathUtils.getMinIndex(localEs);
                //System.out.println(String.valueOf(minLocalEInd));
                initialF0s[i] = ((float)samplingRate)/(minInds[i][pathInds[minLocalEInd][neigh]]+PMin);
            }
            else
                initialF0s[i] = 0.0f;
            
            System.out.println(String.valueOf(initialF0s[i]));
        }
    }
    
    public void estimateRefinedPitch() 
    {
        
    }

    public void estimateVoicing() 
    {
        
    }
    
    public void estimateMaximumVoicingFrequency() 
    {
        
    }
    
    public void analyze(double [] x, int samplingRate, float windowSizeInSeconds, float skipSizeInSeconds,
                        int windowType, float f0MinInHz, float f0MaxInHz, float searchStepInHz) 
    {
        estimateInitialPitch(x, samplingRate, windowSizeInSeconds, skipSizeInSeconds, f0MinInHz, f0MaxInHz, searchStepInHz);
        estimateVoicing();
        estimateMaximumVoicingFrequency();
        estimateRefinedPitch();
    }   
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();
        
        float windowSizeInSeconds = 0.040f;
        float skipSizeInSeconds = 0.005f;
        float f0MinInHz = 60.0f;
        float f0MaxInHz = 500.0f;
        float searchStepInHz = 0.5f;
        
        HNMPitchVoicingAnalyzer h = new HNMPitchVoicingAnalyzer();
        h.estimateInitialPitch(x, samplingRate, windowSizeInSeconds, skipSizeInSeconds, f0MinInHz, f0MaxInHz, searchStepInHz);
        
        for (int i=0; i<h.initialF0s.length; i++)
            System.out.println(String.valueOf(i*skipSizeInSeconds+0.5f*windowSizeInSeconds) + " sec. = " + String.valueOf(h.initialF0s[i]));
    }
}
