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

import javax.swing.JFrame;

import de.dfki.lt.signalproc.display.FunctionGraph;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class SEEVOCAnalyser {
    public static double[] calcSpecEnvelope(double [] absMagSpecIndB, int samplingRate)
    {
        return calcSpecEnvelope(absMagSpecIndB, samplingRate, 100.0);
    }
    
    //The returned spectral envelope is in linear scale
    public static double[] calcSpecEnvelopeLinear(double [] absMagSpecIndB, int samplingRate, double f0)
    {
        double [] H = calcSpecEnvelope(absMagSpecIndB, samplingRate, f0);

        return MathUtils.db2linear(H);
    }
    
    //The returned spectral envelope is also in dB
    public static double[] calcSpecEnvelope(double [] absMagSpecIndB, int samplingRate, double f0)
    {
        int i, j;
        if (f0<10.0)
            f0 = 100.0f;
        
        int maxPeaks = (int)Math.floor(0.5*samplingRate/f0+0.5)+10;
        
        double [] peakVals = new double[maxPeaks];
        int [] peakInds = new int[maxPeaks];
        double [] peakFreqs = new double[maxPeaks];
        
        int maxFreqInd = absMagSpecIndB.length-1;
        double [] freqs = new double[maxFreqInd+1];
        
        for (i=0; i<=maxFreqInd; i++)
            freqs[i] = SignalProcUtils.index2freq(i, samplingRate,  maxFreqInd);
        
        int numPeaks = 0;
        double currentFreq = 0.0;
        double currentMax;
        int currentInd;
        int startInd, endInd;
        boolean bEndNotCovered = true;
        
        while(true)
        {
            if (currentFreq+1.5*f0>0.5*samplingRate)
                break;
            
            startInd = SignalProcUtils.freq2index(currentFreq+0.5*f0, samplingRate, maxFreqInd);
            endInd = SignalProcUtils.freq2index(currentFreq+1.5*f0, samplingRate, maxFreqInd);
            
            startInd = Math.max(0, startInd);
            endInd = Math.min(endInd, maxFreqInd);
            startInd = Math.min(startInd, endInd);    
            
            if (endInd==maxFreqInd)
                bEndNotCovered = false;
            
            currentInd = MathUtils.getAbsMaxInd(absMagSpecIndB, startInd, endInd);
        
            if (currentInd==-1)
            {
                peakVals[numPeaks+1] = Math.max(absMagSpecIndB[startInd],absMagSpecIndB[endInd]);
                currentInd = (int)Math.floor(0.5*(startInd+endInd)+0.5); 
            }
            else
                peakVals[numPeaks+1] = absMagSpecIndB[currentInd];
            
            currentFreq = SignalProcUtils.index2freq(currentInd, samplingRate,  maxFreqInd);
            
            peakInds[numPeaks+1] = currentInd;
            peakFreqs[numPeaks+1] = currentFreq;
                     
            numPeaks++;
            
            //Search for the for the first interval
            if (numPeaks==1)
            {
                startInd = 0;
                endInd = SignalProcUtils.freq2index(currentFreq-0.5*f0, samplingRate, maxFreqInd);
                
                startInd = Math.max(0, startInd);
                endInd = Math.min(endInd, maxFreqInd);
                startInd = Math.min(startInd, endInd);            
                
                currentInd = MathUtils.getAbsMaxInd(absMagSpecIndB, startInd, endInd);
            
                if (currentInd==-1)
                {
                    peakVals[0] = Math.max(absMagSpecIndB[startInd],absMagSpecIndB[endInd]);
                    currentInd = (int)Math.floor(0.5*(startInd+endInd)+0.5); 
                }
                else
                    peakVals[0] = absMagSpecIndB[currentInd];
                
                peakInds[0] = currentInd;
                peakFreqs[0] = SignalProcUtils.index2freq(currentInd, samplingRate,  maxFreqInd);
            }
            //
            
            if (numPeaks>maxPeaks-3)
                break;
        }
        
        //Search for the last interval
        if (bEndNotCovered && numPeaks<maxPeaks)
        {
            startInd = SignalProcUtils.freq2index(currentFreq+0.5*f0, samplingRate, maxFreqInd);
            endInd = maxFreqInd;
            
            startInd = Math.max(0, startInd);
            startInd = Math.min(startInd, endInd);            
            
            currentInd = MathUtils.getAbsMaxInd(absMagSpecIndB, startInd, endInd);
        
            if (currentInd==-1)
            {
                peakVals[numPeaks+1] = Math.max(absMagSpecIndB[startInd],absMagSpecIndB[endInd]);
                currentInd = (int)Math.floor(0.5*(startInd+endInd)+0.5); 
            }
            else
                peakVals[numPeaks+1] = absMagSpecIndB[currentInd];
            
            peakInds[numPeaks+1] = currentInd;
            peakFreqs[numPeaks+1] = SignalProcUtils.index2freq(currentInd, samplingRate,  maxFreqInd); 
            
            numPeaks++;
        }
        //
        
        double [] H = new double[maxFreqInd+1];
        
        for (j=0; j<peakInds[0]; j++)
            H[j] = absMagSpecIndB[0] + (absMagSpecIndB[peakInds[0]]-absMagSpecIndB[0])/peakFreqs[0]*freqs[j];
        
        for (i=0; i<numPeaks-1; i++)
        {
            for (j=peakInds[i]; j<peakInds[i+1]; j++)
                H[j] = absMagSpecIndB[peakInds[i]] + (absMagSpecIndB[peakInds[i+1]]-absMagSpecIndB[peakInds[i]])/(peakFreqs[i+1]-peakFreqs[i])*(freqs[j]-peakFreqs[i]);
        }

        for (j=peakInds[numPeaks-1]; j<=maxFreqInd; j++)
            H[j] = absMagSpecIndB[peakInds[numPeaks-1]] + (absMagSpecIndB[maxFreqInd]-absMagSpecIndB[peakInds[numPeaks-1]])/(0.5*samplingRate-peakFreqs[numPeaks-1])*(freqs[j]-peakFreqs[numPeaks-1]);
        
        //H = SignalProcUtils.medianFilter(H, 5, H[0], H[H.length-1]); 
        //H = SignalProcUtils.meanFilter(H, 19, H[0], H[H.length-1]);
        
        //Plot the DFT and the estimated spectral envelope to check visually
        //double [] excIndB = new double[H.length];
        //for (i=0; i<H.length; i++)
        //    excIndB[i] = absMagSpecIndB[i]-H[i];
        
        //JFrame frame1 = showGraph(excIndB, "DFT spectrum");
        
        //JFrame frame1 = showGraph(H, "SEEVOC1");
        //JFrame frame2 = showGraph(H2, "SEEVOC2");
        //try { Thread.sleep(3000); } catch (InterruptedException e) {}
        //frame1.dispose();
        //frame2.dispose();
        
        return H;
    }
    
    protected static JFrame showGraph(double[] array, String title)
    {
        FunctionGraph graph = new FunctionGraph(400, 200, 0, 1, array);
        JFrame frame = graph.showInJFrame(title, 500, 300, true, false);
        
        return frame;
    }
}
