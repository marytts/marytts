package marytts.util.signal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.filter.BandPassFilter;
import marytts.signalproc.filter.FIRFilter;
import marytts.signalproc.filter.HighPassFilter;
import marytts.signalproc.filter.LowPassFilter;
import marytts.signalproc.filter.RecursiveFilter;
import marytts.signalproc.window.HammingWindow;
import marytts.util.math.ComplexArray;
import marytts.util.math.FFT;
import marytts.util.math.MathUtils;


public class SignalProcUtils {
    
    public static int getLPOrder(int fs){
        int P = (int)(fs/1000.0f+2);
        
        if (P%2==1)
            P+=1;
        
        return P;
    }
    
    public static int getDFTSize(int fs){
        int dftSize;
        
        if (fs<=8000)
            dftSize = 128;
        else if (fs<=16000)
            dftSize = 256;
        else if (fs<=22050)
            dftSize = 512;
        else if (fs<=32000)
            dftSize = 1024;
        else if (fs<=44100)
            dftSize = 2048;
        else
            dftSize = 4096;
        
        return dftSize;
    }
    
    //Returns an odd filter order depending on the sampling rate for FIR filter design
    public static int getFIRFilterOrder(int fs)
    {
        int oddFilterOrder = getDFTSize(fs)-1;
        if (oddFilterOrder%2==0)
            oddFilterOrder++;
        
        return oddFilterOrder;
    }
    
    public static int getLifterOrder(int fs){
        int lifterOrder = 2*(int)(fs/1000.0f+2);
        
        if (lifterOrder%2==1)
            lifterOrder+=1;
        
        return lifterOrder;
    }
    
    public static int halfSpectrumSize(int fftSize)
    {
        return (int)(Math.floor(fftSize/2.0+1.5));
    }
    
    public static double getEnergydB(double x)
    {   
        double [] y = new double[1];
        y[0] = x;
        
        return getEnergydB(y);
    }
    
    public static double getEnergydB(double [] x)
    {   
        return getEnergydB(x, x.length);
    }
    
    public static double getEnergydB(double [] x, int len)
    {   
        return getEnergydB(x, len, 0);
    }
    
    public static double getEnergydB(double [] x, int len, int start)
    {   
        return 10*Math.log10(getEnergy(x, len, start));
    }
    
    public static double getEnergy(double [] x, int len, int start)
    {
        if (start<0)
            start = 0;
        if (start>x.length-1)
            start=x.length-1;
        if (start+len>x.length)
            len = x.length-start-1;
        
        double en = 0.0;
        
        for (int i=start; i<start+len; i++)
            en += x[i]*x[i];
        
        en = Math.sqrt(en);
        en = Math.max(en, 1e-100); //Put a minimum floor to avoid -Ininity in log based computations
        
        return en;
    }
    
    public static double getEnergy(double [] x, int len)
    {
        return getEnergy(x, len, 0);
    }
    
    public static double getEnergy(double [] x)
    {
        return getEnergy(x, x.length, 0);
    }
    
    public static double getAverageSampleEnergy(double [] x, int len, int start)
    {
        if (start<0)
            start = 0;
        if (start>x.length-1)
            start=x.length-1;
        if (start+len>x.length)
            len = x.length-start-1;
        
        double avgSampEn = 0.0;
        
        for (int i=start; i<start+len; i++)
            avgSampEn += x[i]*x[i];
        
        avgSampEn = Math.sqrt(avgSampEn);
        avgSampEn /= len;
        
        return avgSampEn;
    }
    
    public static double getAverageSampleEnergy(double [] x, int len)
    {   
        return getAverageSampleEnergy(x, len, 0);
    }
    
    public static double getAverageSampleEnergy(double [] x)
    {   
        return getAverageSampleEnergy(x, x.length, 0);
    }
    
    public static double[] getEnergyContourRms(double[] x, double windowSizeInSeconds, double skipSizeInSeconds, int samplingRate)
    {
        int ws = (int)Math.floor(windowSizeInSeconds*samplingRate+0.5);
        int ss = (int)Math.floor(skipSizeInSeconds*samplingRate+0.5);
        int numfrm = (int)Math.floor((x.length-(double)ws)/ss+0.5);
        
        double[] energies = null;
        
        if (numfrm>0)
        {
            energies = new double[numfrm];
            double[] frm = new double[ws];
            int i, j;
            for (i=0; i<numfrm; i++)
            {
                Arrays.fill(frm, 0.0);
                System.arraycopy(x, i*ss, frm, 0, Math.min(ws, x.length-i*ss));
                energies[i] = 0.0;
                for (j=0; j<ws; j++)
                    energies[i] += frm[j]*frm[j];
                energies[i] /= ws;
                energies[i] = Math.sqrt(energies[i]);
                energies[i] = MathUtils.amp2db(energies[i]+1e-20);
            }
        }
        
        return energies;
    }
    
    //Returns the reversed version of the input array
    public static double [] reverse(double [] x)
    {
        double [] y = new double[x.length];
        for (int i=0; i<x.length; i++)
            y[i] = x[x.length-i-1];
        
        return y;
    }
    
    //Returns voiced/unvoiced information for each pitch frame
    public static boolean [] getVuvs(double [] f0s)
    {
        if (f0s != null)
        {
            boolean [] vuvs = new boolean[f0s.length];
            for (int i=0; i<vuvs.length; i++)
            {
                if (f0s[i]<10.0)
                    vuvs[i] = false;
                else
                    vuvs[i] = true;
            }
            
            return vuvs;
        }
        else
            return null;
    }
    
    /* Extracts pitch marks from a given pitch contour.
    // It is not very optimized, only inserts a pitch mark and waits for sufficient samples before inserting a new one
    // in order not to insert more than one pitch mark within one pitch period
    //
    // f0s: pitch contour vector
    // fs: sampling rate in Hz
    // len: total samples in original speech signal
    // ws: window size used for pitch extraction in seconds
    // ss: skip size used for pitch extraction in seconds
    //
    // To do:
    // Perform marking on the residual by trying to locate glottal closure instants (might be implemented as a separate function indeed)
    // This may improve PSOLA performance also */
    public static PitchMarks pitchContour2pitchMarks(double [] f0s, int fs, int len, double ws, double ss, boolean bPaddZerosForFinalPitchMark)
    {
        //Interpolate unvoiced segments
        double [] interpf0s = interpolate_pitch_uv(f0s);
        int numfrm = f0s.length;
        int maxTotalPitchMarks = len;
        int [] tmpPitchMarks = MathUtils.zerosInt(maxTotalPitchMarks);
        boolean [] tmpVuvs = new boolean[maxTotalPitchMarks];
        
        int count = 0;
        int prevInd = 1;
        int ind;
        double T0;
        boolean bVuv;
        
        for (int i=1; i<=len; i++)
        {
            ind = (int)(Math.floor(((i-1.0)/fs-0.5*ws)/ss+0.5)+1);
            if (ind<1)
                ind=1;
            
            if (ind>numfrm)
                ind=numfrm;
            
            if (interpf0s[ind-1]>10.0)
                T0 = (fs/interpf0s[ind-1]);
            else
                T0 = (fs/100.0f);
            
            if (f0s[ind-1]>10.0)
                bVuv = true;
            else
                bVuv = false;

            if (i==1 || i-T0>=prevInd) //Insert new pitch mark
            {
                count++;
                
                tmpPitchMarks[count-1] = i-1;
                prevInd = i;
                
                if (i>1)
                    tmpVuvs[count-2] = bVuv;
            }
        }

        PitchMarks pm = null;
        if (count>1)
        {
            //Check if last pitch mark corresponds to the end of signal, otherwise put an additional pitch mark to match the last period and note to padd sufficient zeros
            if (bPaddZerosForFinalPitchMark && tmpPitchMarks[count-1] != len-1)
            {
                pm = new PitchMarks(count+1, tmpPitchMarks, tmpVuvs, 0);
                pm.pitchMarks[pm.pitchMarks.length-1] = pm.pitchMarks[pm.pitchMarks.length-2]+(pm.pitchMarks[pm.pitchMarks.length-2]-pm.pitchMarks[pm.pitchMarks.length-3]);
                pm.totalZerosToPadd = pm.pitchMarks[pm.pitchMarks.length-1]-(len-1);
            }
            else
                pm = new PitchMarks(count, tmpPitchMarks, tmpVuvs, 0);
        }
        
        return pm;
    }
    
    //Convert pitch marks to pitch contour values in Hz using a fixed analysis rate
    public static double [] pitchMarks2PitchContour(int [] pitchMarks, float ws, float ss, int samplingRate)
    {
        double [] f0s = null;
        float [] times = samples2times(pitchMarks, samplingRate);
        int numfrm = (int)Math.floor((times[times.length-1]-0.5*ws)/ss+0.5);
        
        if (numfrm>0)
        {
            f0s = new double[numfrm];
         
            int currentInd;
            float currentTime;
            float T0;
            for (int i=0; i<numfrm; i++)
            {
                currentTime = i*ss+0.5f*ws;
                currentInd = MathUtils.findClosest(times, currentTime);

                if (currentInd>0)
                    f0s[i] = 1.0/(times[currentInd]-times[currentInd-1]);
                else
                    f0s[i] = 1.0/times[currentInd];
            }
        }
        
        return f0s;
    }
    
    public static double[] fixedRateF0Values(PitchMarks pm, double wsFixedInSeconds, double ssFixedInSeconds, int numfrm, int samplingRate)
    {
        double[] f0s = new double[numfrm];

        int i, ind, sample;
        for (i=0; i<numfrm; i++)
        {
            sample = SignalProcUtils.time2sample((float)(i*ssFixedInSeconds+0.5*wsFixedInSeconds), samplingRate);
            ind = MathUtils.findClosest(pm.pitchMarks, sample);
            
            f0s[i] = 0.0;
            if (ind<0)
            {
                if (sample>pm.pitchMarks[pm.pitchMarks.length-1])
                    ind=pm.pitchMarks.length-1;
                else
                    ind=1;
            }
            
            if (pm.vuvs[ind-1])
            {
                if (ind>0)
                    f0s[i] = ((double)samplingRate)/(pm.pitchMarks[ind]-pm.pitchMarks[ind-1]);
                else
                    f0s[i] = ((double)samplingRate)/(pm.pitchMarks[ind+1]-pm.pitchMarks[ind]);
            }
        }
        
        return f0s;
    }
    
    public static double [] interpolate_pitch_uv(double [] f0s)
    {
        return interpolate_pitch_uv(f0s, 10.0);
    }
    
    // Interpolates unvoiced parts of the f0 contour 
    // using the neighbouring voiced parts
    // Linear interpolation is used
    public static double [] interpolate_pitch_uv(double [] f0s, double minVoicedVal)
    {
        int [] ind_v = MathUtils.find(f0s, MathUtils.GREATER_THAN, minVoicedVal);
        double [] new_f0s = null;
        
        if (ind_v==null)
        {
            new_f0s = new double[f0s.length];
            System.arraycopy(f0s, 0, new_f0s, 0, f0s.length);
        }
        else
        {
            double [] tmp_f0s = new double[f0s.length];
            System.arraycopy(f0s, 0, tmp_f0s, 0, f0s.length);
            
            int [] ind_v2 = null;
            if (ind_v[0] != 0)
            {
                tmp_f0s[0] = MathUtils.mean(f0s, ind_v);
                ind_v2 = new int[ind_v.length+1];
                ind_v2[0] = 0;
                System.arraycopy(ind_v, 0, ind_v2, 1, ind_v.length);
            }
            else
            {
                ind_v2 = new int[ind_v.length];
                System.arraycopy(ind_v, 0, ind_v2, 0, ind_v.length);
            }

            int [] ind_v3 = null;
            if (ind_v2[ind_v2.length-1] != tmp_f0s.length-1)
            {
                tmp_f0s[tmp_f0s.length-1] = tmp_f0s[ind_v2[ind_v2.length-1]];
                ind_v3 = new int[ind_v2.length+1];
                System.arraycopy(ind_v2, 0, ind_v3, 0, ind_v2.length);
                ind_v3[ind_v2.length] = f0s.length-1;
            }
            else
            {
                ind_v3 = new int[ind_v2.length];
                System.arraycopy(ind_v2, 0, ind_v3, 0, ind_v2.length);
            }
             
            int i;
            double [] y = new double[ind_v3.length];
            for (i=0; i<ind_v3.length; i++)
                y[i] = tmp_f0s[ind_v3[i]];
            
            int [] xi = new int[f0s.length];
            for (i=0; i<f0s.length; i++)
                xi[i] = i;
            
            new_f0s = MathUtils.interpolate_linear(ind_v3, y, xi);
        }
        
        return new_f0s;
    }
    
    //A least squares line is fit to the given contour
    // and the parameters of the line are returned, i.e. line[0]=intercept and line[1]=slope
    public static double[] getContourLSFit(double[] contour, boolean isPitchUVInterpolation)
    {
        double[] line = null;
        
        if (contour!=null)
        {
            double[] newContour = new double[contour.length];
            System.arraycopy(contour, 0, newContour, 0, contour.length);
        
            if (isPitchUVInterpolation)
                newContour = SignalProcUtils.interpolate_pitch_uv(newContour);
            
            double[] indices = new double[contour.length];
            for (int i=0; i<contour.length; i++)
                indices[i] = i;
            
            line = fitLeastSquaresLine(indices, newContour);
        }
        
        return line;
    }
    
    public static double[] fitLeastSquaresLine(double [] x, double [] y)
    {
        assert x!=null;
        assert y!=null;
        assert x.length==y.length;
        
        double [] params = new double[2];

        double sx = 0.0;
        double sy = 0.0;
        double sxx = 0.0;
        double sxy = 0.0;
        double delta;

        int numPoints = x.length;
        
        for(int i=0; i<numPoints; i++)
        {
            sx  += x[i];
            sy  += y[i];
            sxx += x[i]*x[i];
            sxy += x[i]*y[i];
        }

        delta = numPoints*sxx - sx*sx;

        //Intercept
        params[0] = (sxx*sy -sx*sxy)/delta;
        //Slope
        params[1] = (numPoints*sxy -sx*sy)/delta;

        return params;
    }
    
    public static boolean getVoicing(double [] windowedSpeechFrame, int samplingRateInHz)
    {
        return getVoicing(windowedSpeechFrame, samplingRateInHz, 0.35f);
    }

    public static boolean getVoicing(double [] windowedSpeechFrame, int samplingRateInHz, double voicingThreshold)
    {
        double Pvoiced = getVoicingProbability(windowedSpeechFrame, samplingRateInHz);
        
        if (Pvoiced>=voicingThreshold)
            return true;
        else
            return false;
    }

    public static double getVoicingProbability(double [] windowedSpeechFrame, int samplingRateInHz)
    {   
        int maxT0 = (int)((double)samplingRateInHz/40.0);
        int minT0 = (int)((double)samplingRateInHz/400.0);
        if (maxT0>windowedSpeechFrame.length-1)
            maxT0 = windowedSpeechFrame.length-1;
        if (minT0>maxT0)
            minT0=maxT0;
        
        double [] R = SignalProcUtils.autocorr(windowedSpeechFrame, maxT0);
        
        double maxR = R[minT0];
        
        for (int i=minT0+1; i<=maxT0; i++)
        {
            if (R[i]>maxR)
                maxR = R[i];
        }
        
        double Pvoiced = maxR/R[0];
        
        return Pvoiced;
    }
    
    public static double [] autocorr(double [] x, int LPOrder)
    {
        int N = x.length;
        double [] R = new double[LPOrder+1];
        
        int n, m;
        
        for (m=0; m<=LPOrder; m++)
        {   
            R[m] = 0.0;
            
            for (n=0; n<=x.length-m-1; n++)
                R[m] += x[n]*x[n+m];
        }
        
        return R;
    }
    
    //Apply a 1st order highpass preemphasis filter to speech frame frm
    public static double[] applyPreemphasis(double [] frm, double preCoef)
    {
        double[] frmOut = new double[frm.length];
        System.arraycopy(frm, 0, frmOut, 0, frm.length);
        
        if (preCoef>0.0)
        {  
            double [] coeffs = new double[2];
            coeffs[0] = 1.0;
            coeffs[1] = -preCoef;

            RecursiveFilter r = new RecursiveFilter(coeffs);

            r.apply(frmOut);
        }

        return frmOut;
    }
    
    //Remove preemphasis from preemphasized frame frm (i.e. 1st order lowspass filtering)
    public static double[] removePreemphasis(double [] frm, double preCoef)
    {
        double[] frmOut = new double[frm.length];
        System.arraycopy(frm, 0, frmOut, 0, frm.length);

        if (preCoef>0.0)
        {
            double [] coeffs = new double[2];
            coeffs[0] = 1.0;
            coeffs[1] = preCoef;

            RecursiveFilter r = new RecursiveFilter(coeffs);

            r.apply(frmOut);
        }
        
        return frmOut;
    }
    
    public static double[] freq2bark(double[] freqsInHz)
    {
        double[] barks = null;
        
        if (freqsInHz!=null)
        {
            barks = new double[freqsInHz.length];
            for (int i=0; i<barks.length; i++)
                barks[i] = freq2bark(freqsInHz[i]);
        }
        
        return barks;
    }
    
    public static double freq2bark(double freqInHz)
    {
        return 13.0*Math.atan(0.00076*freqInHz)+3.5*Math.atan((freqInHz*freqInHz/(7500*7500)));
    }
    
    //Convert frequency in Hz to frequency sample index
    // maxFreq corresponds to half sampling rate, i.e. sample no: fftSize/2+1 where freq sample indices are 0,1,...,maxFreq-1
    public static int[] freq2index(double [] freqsInHz, int samplingRateInHz, int maxFreq)
    {
        int [] inds = null;
        
        if (freqsInHz!=null && freqsInHz.length>0)
        {
            inds = new int[freqsInHz.length];
            
            for (int i=0; i<inds.length; i++)
                inds[i] = freq2index(freqsInHz[i], samplingRateInHz, maxFreq);
        }
        
        return inds;
    }
    
    //maxFreqIndex: Actually we have indices from 0,...,maxFreqIndex-1
    public static int freq2index(double freqInHz, int samplingRateInHz, int maxFreqIndex)
    {
        int index = (int)Math.floor(freqInHz/(0.5*samplingRateInHz)*(maxFreqIndex-1)+0.5);
        index = (int)Math.max(0, index);
        index = (int)Math.min(index, maxFreqIndex);
        
        return index;
    }
    
    //Convert frequency in Hz to frequency sample index
    // maxFreq corresponds to half sampling rate, i.e. sample no: fftSize/2+1 where freq sample indices are 0,1,...,maxFreq-1
    public static double[] freq2indexDouble(double [] freqsInHz, int samplingRateInHz, int maxFreq)
    {
        double [] inds = null;
        
        if (freqsInHz!=null && freqsInHz.length>0)
        {
            inds = new double[freqsInHz.length];
            
            for (int i=0; i<inds.length; i++)
                inds[i] = freq2indexDouble(freqsInHz[i], samplingRateInHz, maxFreq);
        }
        
        return inds;
    }
    
    public static double freq2indexDouble(double freqInHz, int samplingRateInHz, int maxFreqIndex)
    {
        double index = freqInHz/(0.5*samplingRateInHz)*(maxFreqIndex-1);
        index = Math.max(0, index);
        index = Math.min(index, maxFreqIndex);
        
        return index;
    }
    
    //Convert a zero based spectrum index value to frequency in Hz
    public static double index2freq(int zeroBasedFreqIndex, int samplingRateInHz, int zeroBasedMaxFreqIndex)
    {
        return zeroBasedFreqIndex*(0.5*samplingRateInHz)/zeroBasedMaxFreqIndex;
    }
    
    //Convert a zero based spectrum index value to frequency in Hz
    public static double indexDouble2freq(double zeroBasedFreqIndex, int samplingRateInHz, int zeroBasedMaxFreqIndex)
    {
        return zeroBasedFreqIndex*(0.5*samplingRateInHz)/zeroBasedMaxFreqIndex;
    }
 
    //Convert sample index to time value in seconds
    public static float sample2time(int sample, int samplingRate)
    {
        return ((float)sample)/samplingRate;
    }
    
    //Convert time value in seconds to sample index
    public static int time2sample(float time, int samplingRate)
    {
        return (int)Math.floor(time*samplingRate+0.5f);
    }
    
    //Convert sample indices to time values in seconds
    public static float[] samples2times(int [] samples, int samplingRate)
    {
        float [] times = null;

        if (samples!=null && samples.length>0)
        {
            times = new float[samples.length];
            for (int i=0; i<samples.length; i++)
                times[i] = ((float)samples[i])/samplingRate;
        }

        return times;
    }
    
    //Convert time values in seconds to sample indices
    public static int[] times2samples(float [] times, int samplingRate)
    {
        int [] samples = null;

        if (times!=null && times.length>0)
        {
            samples = new int[times.length];
            for (int i=0; i<samples.length; i++)
                samples[i] = (int)Math.floor(times[i]*samplingRate+0.5f);
        }

        return samples;
    }
    
    //Find the scaled version of a given time value t by performing time varying scaling using
    // scales s at times given by alphas
    // t: instant of time which we want to convert to the new time scale
    // s: time scale factors at times given by alphas
    // alphas: time instants at which the time scale modification factor is the corresponding entry in s
    public static float timeScaledTime(float t, float [] s, float [] alphas)
    {
        assert s!=null;
        assert alphas!=null;
        assert s.length==alphas.length;
        
        int N = s.length;
        float tNew = 0.0f;
        
        if (N>0)
        {
            if (t<=alphas[0])
            {
                tNew = t*s[0];
            }
            else if (t>=alphas[N-1])
            {
                tNew = alphas[0]*s[0];
                for (int i=0; i<N-2; i++)
                    tNew += 0.5*(alphas[i+1]-alphas[i])*(s[i]+s[i+1]);
                
                tNew += (t-alphas[N-1])*s[N-1];
            }
            else
            {
                int k = MathUtils.findClosest(alphas, t);
                if (alphas[k]>=t)
                    k--;
                
                tNew = alphas[0]*s[0];
                
                for (int i=0; i<=k-1; i++)
                    tNew += 0.5*(alphas[i+1]-alphas[i])*(s[i]+s[i+1]);
                
                float st0 = (t-alphas[k])*(s[k+1]-s[k])/(alphas[k+1]-alphas[k]) + s[k];
                
                tNew += 0.5*(t-alphas[k])*(st0+s[k]);
            }
        }
        
        return tNew;
    }
    
    //Find the scaled version of a set of time values times by performing time varying scaling using
    // tScales at times given by tScalesTimes
    public static float [] timeScaledTimes(float [] times, float [] tScales, float [] tScalesTimes)
    {
        float [] newTimes = null;
        
        if (times!=null && times.length>0)
        {
            newTimes = new float[times.length];
            
            for (int i=0; i<times.length; i++)
                newTimes[i] = timeScaledTime(times[i], tScales, tScalesTimes);
        }
        
        return newTimes;
    }
    
    //Time scale a pitch contour as specified by a time varying pattern tScales and tScalesTimes
    // f0s: f0 values
    // ws: Window size in seconds in f0 analysis
    // ss: Skip size in seconds in f0 analysis
    // tScales: time scale factors at times given by tScalesTimes
    // tScalesTimes: time instants at which the time scale modification factor is the corresponding entry in tScales
    public static double[] timeScalePitchContour(double[] f0s, float ws, float ss, float[] tScales, float[] tScalesTimes)
    {
        if (tScales==null || tScalesTimes==null)
            return f0s;
        
        assert tScales.length == tScalesTimes.length;
        
        int i, ind;
        
        //First compute the original time axis
        float [] times = new float[f0s.length]; 
        for (i=0; i<f0s.length; i++)
            times[i] = i*ss + 0.5f*ws;
        
        float [] newTimes = timeScaledTimes(times, tScales, tScalesTimes); 
        
        int numfrm = (int)Math.floor((newTimes[newTimes.length-1]-0.5*ws)/ss+0.5);
        
        double [] f0sNew = new double[numfrm];
        
        for (i=0; i<numfrm; i++)
        {
            ind = MathUtils.findClosest(newTimes, i*ss+0.5f*ws);
            f0sNew[i] = f0s[ind];
        }
        
        return f0sNew;
    }
    
    //Pitch scale a pitch contour as specified by a time varying pattern pScales and pScalesTimes
    // f0s: f0 values
    // ws: Window size in seconds in f0 analysis
    // ss: Skip size in seconds in f0 analysis
    // pScales: pitch scale factors at times given by pScalesTimes
    // pScalesTimes: time instants at which the pitch scale modification factor is the corresponding entry in pScales
    public static double[] pitchScalePitchContour(double[] f0s, float ws, float ss, float[] pScales, float[] pScalesTimes)
    {
        if (pScales==null || pScalesTimes==null)
            return f0s;
        
        assert pScales.length == pScalesTimes.length;
        
        int i, smallerCloseInd;
        float currentTime;
        float alpha;
        
        double [] f0sNew = new double[f0s.length];
        
        for (i=0; i<f0s.length; i++)
        {
            currentTime = i*ss+0.5f*ws;
            
            smallerCloseInd = MathUtils.findClosest(pScalesTimes, currentTime);
            
            if (pScalesTimes[smallerCloseInd]>currentTime)
                smallerCloseInd--;
                
            if (smallerCloseInd>=0 && smallerCloseInd<pScales.length-1)
            {
                alpha = (pScalesTimes[smallerCloseInd+1]-currentTime)/(pScalesTimes[smallerCloseInd+1]-pScalesTimes[smallerCloseInd]);
                f0sNew[i] = (alpha*pScales[smallerCloseInd]+(1.0f-alpha)*pScales[smallerCloseInd+1])*f0s[i];
            }
            else
            {
                smallerCloseInd = Math.max(0, smallerCloseInd);
                smallerCloseInd = Math.min(smallerCloseInd, pScales.length-1);
                f0sNew[i] = pScales[smallerCloseInd]*f0s[i];
            }
        }
        
        return f0sNew;
    }
    
    //Returns samples from a white noise process. The sample amplitudes are between [-0.5,0.5]
    public static double [] getNoise(double startFreqInHz, double endFreqInHz, int samplingRateInHz, int len)
    {
        return getNoise(startFreqInHz/samplingRateInHz, endFreqInHz/samplingRateInHz, len);
    }
    
    public static double [] getNoise(double normalizedStartFreq, double normalizedEndFreq, int len)
    {
        double [] noise = null;
        
        if (len>0)
        {
            noise = new double[len];
            for (int i=0; i<len; i++)
                noise[i] = Math.random()-0.5;
            
            FIRFilter f=null;
            if (normalizedStartFreq>0.0 && normalizedEndFreq<1.0f) //Bandpass
                f = new BandPassFilter(normalizedStartFreq, normalizedEndFreq);
            else if (normalizedStartFreq>0.0)
                f = new HighPassFilter(normalizedStartFreq);
            else if (normalizedEndFreq<1.0f)
                f = new LowPassFilter(normalizedEndFreq);
            
            if (f!=null)
            {
                double [] noise2 = f.apply(noise);
                System.arraycopy(noise2, 0, noise, 0, len);
            }
        }
        
        return noise;
    }
    
    
    public static float radian2Hz(float rad, int samplingRate)
    {
        return (float)((rad/MathUtils.TWOPI)*samplingRate);
    }
    
    public static double radian2Hz(double rad, int samplingRate)
    {
        return (rad/MathUtils.TWOPI)*samplingRate;
    }
    
    
    public static float hz2radian(float hz, int samplingRate)
    {
        return (float)(hz*MathUtils.TWOPI/samplingRate);
    }
    
    public static double hz2radian(double hz, int samplingRate)
    {
        return hz*MathUtils.TWOPI/samplingRate;
    }
    
    //Median filtering: All values in x are replaced by the median of the 3-closest context neighbours (i.e. the left value, the value itself, and the right value)
    // The output y[k] is the median of x[k-1], x[k], and x[k+1]
    // All out-of-boundary values are assumed 0.0
    public static double[] medianFilter(double[] x)
    {
        return medianFilter(x, 3);
    }
    
    //Median filtering: All values in x are replaced by the median of the N closest context neighbours
    // If N is odd, the output y[k] is the median of x[k-(N-1)/2],...,x[k+(N-1)/2]
    // If N is even, the output y[k] is the median of x[k-(N/2)+1],...,x[k+(N/2)-1], i.e. the average of the (N/2-1)th and (N/2)th of the sorted values
    // All out-of-boundary values are assumed 0.0
    public static double[] medianFilter(double[] x, int N)
    {
        return medianFilter(x, N, 0.0);
    }
    
    //Median filtering: All values in x are replaced by the median of the N closest context neighbours
    // If N is odd, the output y[k] is the median of x[k-(N-1)/2],...,x[k+(N-1)/2]
    // If N is even, the output y[k] is the median of x[k-(N/2)+1],...,x[k+(N/2)-1], i.e. the average of the (N/2-1)th and (N/2)th of the sorted values
    // All out-of-boundary values are assumed outOfBound
    public static double[] medianFilter(double[] x, int N, double outOfBound)
    {
        return medianFilter(x, N, outOfBound, outOfBound);
    }
    
    //Median filtering: All values in x are replaced by the median of the N closest context neighbours and the value itself
    // If N is odd, the output y[k] is the median of x[k-(N-1)/2],...,x[k+(N-1)/2]
    // If N is even, the output y[k] is the median of x[k-(N/2)+1],...,x[k+(N/2)-1], i.e. the average of the (N/2-1)th and (N/2)th of the sorted values
    // The out-of-boundary values are assumed leftOutOfBound for k-i<0 and rightOutOfBound for k+i>x.length-1
    public static double[] medianFilter(double[] x, int N, double leftOutOfBound, double rightOutOfBound)
    {
        double [] y = new double[x.length];
        Vector v = new Vector();

        int k, j, midVal;

        if (N%2==1) //Odd version
        {
            midVal = (N-1)/2;

            for (k=0; k<x.length; k++)
            {
                for (j=0; j<midVal; j++)
                {
                    if (k-j>=0)
                        v.add(x[k-j]);
                    else
                        v.add(leftOutOfBound);
                }
                
                for (j=midVal; j<N; j++)
                {
                    if (k+j<x.length)
                        v.add(x[k+j]);
                    else
                        v.add(rightOutOfBound);
                }
                
                Collections.sort(v);
                
                y[k] = ((Double)(v.get(midVal))).doubleValue();
                
                v.clear();
            }
        }
        else //Even version
        {
            midVal = N/2-1;

            for (k=0; k<x.length; k++)
            {
                for (j=0; j<=midVal; j++)
                {
                    if (k-j>=0)
                        v.add(x[k-j]);
                    else
                        v.add(leftOutOfBound);
                }
                
                for (j=midVal+1; j<N; j++)
                {
                    if (k+j<x.length)
                        v.add(x[k+j]);
                    else
                        v.add(rightOutOfBound);
                }
                
                Collections.sort(v);
                
                y[k] = 0.5*(((Double)(v.get(midVal))).doubleValue()+((Double)(v.get(midVal+1))).doubleValue());
                
                v.clear();
            }
        }
        
        return y;
    }
    
    //Mean filtering: All values in x are replaced by the mean of the N closest context neighbours and the value itself
    // If N is odd, the output y[k] is the mean of x[k-(N-1)/2],...,x[k+(N-1)/2]
    // If N is even, the output y[k] is the mean of x[k-(N/2)+1],...,x[k+(N/2)-1]
    // The out-of-boundary values are assumed leftOutOfBound for k-i<0 and rightOutOfBound for k+i>x.length-1
    public static double[] meanFilter(double[] x, int N, double leftOutOfBound, double rightOutOfBound)
    {
        double [] y = new double[x.length];
        Vector v = new Vector();

        int k, j, midVal;

        if (N%2==1) //Odd version
        {
            midVal = (N-1)/2;

            for (k=0; k<x.length; k++)
            {
                for (j=0; j<midVal; j++)
                {
                    if (k-j>=0)
                        v.add(x[k-j]);
                    else
                        v.add(leftOutOfBound);
                }
                
                for (j=midVal; j<N; j++)
                {
                    if (k+j<x.length)
                        v.add(x[k+j]);
                    else
                        v.add(rightOutOfBound);
                }
                
                y[k] = mean(v);
                
                v.clear();
            }
        }
        else //Even version
        {
            midVal = N/2-1;

            for (k=0; k<x.length; k++)
            {
                for (j=0; j<=midVal; j++)
                {
                    if (k-j>=0)
                        v.add(x[k-j]);
                    else
                        v.add(leftOutOfBound);
                }
                
                for (j=midVal+1; j<N; j++)
                {
                    if (k+j<x.length)
                        v.add(x[k+j]);
                    else
                        v.add(rightOutOfBound);
                }
                
                y[k] = mean(v);
                
                v.clear();
            }
        }
        
        return y;
    }
    
    public static double mean(Vector v)
    {
        double m = 0.0;
        
        for (int i=0; i<v.size(); i++)
            m += ((Double) (v.get(i))).doubleValue();
        
        m /= v.size();
        
        return m;
    }
    
    public static float frameIndex2Time(int zeroBasedFrameIndex, float windowSizeInSeconds, float skipSizeInSeconds)
    {
        return Math.max(0.0f, 0.5f*windowSizeInSeconds+zeroBasedFrameIndex*skipSizeInSeconds);
    }
    
    public static double frameIndex2Time(int zeroBasedFrameIndex, double windowSizeInSeconds, double skipSizeInSeconds)
    {
        return Math.max(0.0, 0.5*windowSizeInSeconds+zeroBasedFrameIndex*skipSizeInSeconds);
    }
    
    public static int time2frameIndex(float time, float windowSizeInSeconds, float skipSizeInSeconds)
    {
        return (int)Math.max(0, Math.floor((time-0.5f*windowSizeInSeconds)/skipSizeInSeconds+0.5));
    }
    
    public static int time2frameIndex(double time, double windowSizeInSeconds, double skipSizeInSeconds)
    {
        return (int)Math.max(0, Math.floor((time-0.5*windowSizeInSeconds)/skipSizeInSeconds+0.5));
    }
    
    //Center-clipping using the amount <ratio>
    //Valid values of ratio are in the range [0.0,1.0]
    // greater values result in  more clipping (i.e. with 1.0 you will get all zeros at the output)
    public static void centerClip(double[] x, double ratio)
    {
        if (ratio<0.0)
            ratio=0.0;
        if (ratio>1.0)
            ratio=1.0;
        
        double positiveMax = MathUtils.getMax(x);
        double negativeMax = MathUtils.getMin(x);
        double positiveTh = positiveMax*ratio;
        double negativeTh = negativeMax*ratio;
        
        for (int i=0; i<x.length; i++)
        {
            if (x[i]>positiveTh)
                x[i] -= positiveTh;
            else if (x[i]<negativeTh)
                x[i] -= negativeTh;
            else
                x[i] = 0.0;
        }
    }
    
    public static double[] getVoiceds(double[] f0s)
    {
        double[] voiceds = null;

        if (f0s!=null)
        {
            int totalVoiceds = 0;
            int i;

            for (i=0; i<f0s.length; i++)
            {
                if (f0s[i]>10.0)
                    totalVoiceds++;
            }

            if (totalVoiceds>0)
            {
                voiceds = new double[totalVoiceds];
                int count = 0;
                for (i=0; i<f0s.length; i++)
                {
                    if (f0s[i]>10.0)
                        voiceds[count++] = f0s[i];

                    if (count>=totalVoiceds)
                        break;
                }
            }
        }
        
        return voiceds;
    }
    
    //Convert an f0 contour into a log-f0 contour by handling unvoiced parts specially
    //The unvoiced values (i.e. f0 values less than or equal to 10 Hz are set to 0.0
    public static double[] getLogF0s(double[] f0s)
    {
        return MathUtils.log(f0s, 10.0, 0.0);
    }
    
    //Inverse of getLogF0s functions
    //i.e. log f0 values are converted to values in Hz with special handling of unvoiceds
    public static double[] getExpF0s(double[] logF0s)
    {
        double[] f0s = null;
        
        if (logF0s!=null)
        {
            f0s = new double[logF0s.length];
            
            for (int i=0; i<f0s.length; i++)
            {
                if (logF0s[i]>Math.log(10.0))
                    f0s[i] = Math.exp(logF0s[i]);
                else
                    f0s[i] = 0.0;
            }
        }
        
        return f0s;
    }
    
    public static double getF0Range(double[] f0s)
    {
        return getF0Range(f0s, 0.10, 0.10);
    }
    
    public static double getF0Range(double[] f0s, double percentileMin, double percentileMax)
    {
        double range = 0.0;
        
        double[] voiceds = SignalProcUtils.getVoiceds(f0s);
        
        if (voiceds!=null)
        {
            if (percentileMin<0.0)
                percentileMin = 0.0;
            if (percentileMin>1.0)
                percentileMin = 1.0;
            if (percentileMax<0.0)
                percentileMax = 0.0;
            if (percentileMax>1.0)
                percentileMax = 1.0;
            
            MathUtils.quickSort(voiceds);
            int ind1 = (int)Math.floor(voiceds.length*percentileMin+0.5);
            int ind2 = (int)Math.floor(voiceds.length*(1.0-percentileMax)+0.5);
            range = Math.max(0.0, voiceds[ind2] - voiceds[ind1]);
        }
        
        return range;
    }
    
    public static int frameIndex2LabelIndex(int zeroBasedFrameIndex, Labels labels, double windowSizeInSeconds, double skipSizeInSeconds)
    {
        double time = zeroBasedFrameIndex*skipSizeInSeconds + 0.5*windowSizeInSeconds;
        
        return time2LabelIndex(time, labels);
    }
    
    public static int time2LabelIndex(double time, Labels labels)
    {
        int index = -1;
        
        for (int i=0; i<labels.items.length; i++)
        {
            if (labels.items[i].time<=time)
                index++;
            else
                break;
        }
        
        if (index<0)
            index=0;
        if (index>labels.items.length-1)
            index=labels.items.length-1;

        return index;
        
    }
    
    public static double getRmsDistance(double[] x, double[] y)
    {
        double rmsDist = 0.0;
        
        for (int i=0; i<Math.min(x.length, y.length); i++)
            rmsDist += (x[i]-y[i])*(x[i]-y[i]);
        
        rmsDist /= Math.min(x.length, y.length);
        rmsDist = Math.sqrt(rmsDist);
        
        return rmsDist;
    }
    
    public static int[] merge(int[] x1, int[] x2)
    {
        int[] y = null;
        int ylen = 0;
        if (x1!=null)
            ylen+=x1.length;
        if (x2!=null)
            ylen+=x2.length;
        y = new int[ylen];
        int pos = 0;
        if (x1!=null)
        {
            System.arraycopy(x1, 0, y, 0, x1.length);
            pos += x1.length;
        }
        
        if (x2!=null)
            System.arraycopy(x2, 0, y, pos, x2.length);
        
        return y;
    }
    
    public static double[] merge(double[] x1, double[] x2)
    {
        double[] y = null;
        int ylen = 0;
        if (x1!=null)
            ylen+=x1.length;
        if (x2!=null)
            ylen+=x2.length;
        y = new double[ylen];
        int pos = 0;
        if (x1!=null)
        {
            System.arraycopy(x1, 0, y, 0, x1.length);
            pos += x1.length;
        }
        
        if (x2!=null)
            System.arraycopy(x2, 0, y, pos, x2.length);
        
        return y;
    }
    
    //Decimate data by a factor D
    //For non-integer index values, linear interpolation is performed
    public static double[] decimate(double[] x, double D)
    {        
        double[] y = null;
        
        if (x!=null)
        {
            int ylen = (int)Math.floor(x.length/D+0.5);
            y = new double[ylen];
            double dind = 0.5*D;
            int total = 0;
            int ind1, ind2;
            while (total<ylen)
            {
                ind1 = (int)Math.floor(dind);
                ind2 = ind1+1;
                if (ind2>x.length-1)
                    ind2 = x.length-1;
                ind1 = ind2-1;
                
                y[total++] = (ind2-dind)*x[ind1]+(dind-ind1)*x[ind2];
                
                dind += D;
            }
        }
        
        return y;
    }
    
    //Interpolate data by a factor D
    //For non-integer index values, linear interpolation is performed
    public static double[] interpolate(double[] x, double D)
    {   
        double[] y = null;
        
        if (x!=null)
        {
            int ylen = (int)Math.floor(x.length*D+0.5);
            y = new double[ylen];
            int xind;
            double xindDouble;
            for (int i=0; i<ylen; i++)
            {
                xindDouble = i/D;
                xind = (int)Math.floor(xindDouble);
                
                if (xind<=0)
                    y[i] = (xind-xindDouble)*(2*x[0]-x[1]) + (1-xind+xindDouble)*x[xind];
                else if (xind>x.length-1)
                    y[i] = (x[x.length-1]-x[x.length-2])*(xindDouble-x.length+1)+x[x.length-1];
                else
                    y[i] = (xind-xindDouble)*x[xind-1] + (1-xind+xindDouble)*x[xind];
            }
        }
        
        return y;
    }
    
    public static double energy(double[] x)
    {
        double e = 0.0;
        for (int i=0; i<x.length; i++)
            e += x[i]*x[i];
        
        return e;
    }
    
    public static double[] filter(double[] b, double[] x)
    {
        double[] a = new double[1];
        a[0] = 1.0;
        
        return filter(b, a, x);
    }
    
    public static double[] filter(double[] b, double[] a, double[] x)
    {
        return filter(b, a, x, false);
    }
    
    public static double[] filter(double[] b, double[] x, boolean bNormalize)
    {
        double[] a = new double[1];
        a[0] = 1.0;
        
        return filter(b, a, x, bNormalize);
    }
    
    public static double[] filter(double[] b, double[] a, double[] x, boolean bNormalize)
    {
        double[] zi = new double[Math.max(a.length, b.length)-1];
        Arrays.fill(zi, 0.0);
        
        return filter(b, a, x, bNormalize, zi);
    }
    
    public static double[] filter(double[] b, double[] x, boolean bNormalize, double[] zi)
    {
        double[] a = new double[1];
        a[0] = 1.0;
        
        return filter(b, a, x, bNormalize, zi);
    }
    
    //Time domain digital filtering
    //  a[0]*y[n] = b[0]*x[n] + b[1]*x[n-1] + ... + b[nb]*x[n-nb]
    //              - a[1]*y[n-1] - ... - a[na]*y[n-na]  
    //  b and a are filter coefficients (impulse response of the filter)
    //  If bNormalize is true, all the coeffs are normalized with a[0].
    // The initial conditions should be specified in zi.
    // Setting zi to all zeroes causes no initial conditions to be used.
    // Length of zi should be max(a.length, b.length)-1.
    public static double[] filter(double[] b, double[] a, double[] x, boolean bNormalize, double[] zi)
    {
        int n;
        double x_terms;
        double y_terms;
        int ind;

        double[] y = new double[x.length];

        int nb = b.length-1;
        int na = a.length-1;

        if (bNormalize)
        {
            //Normalize with a[0] first
            if (a[0]!=1.0)
            {
                for (n=0; n<b.length; n++)
                    b[n] /= a[0];
                for (n=0; n<a.length; n++)
                    a[n] /= a[0];
            }
        }

        for (n=0; n<x.length; n++)
        {
            x_terms = 0.0;
            for (ind=n; ind>n-nb-1; ind--)
            {
                if (ind>=0)
                    x_terms += b[n-ind]*x[ind];
                else 
                    x_terms += b[n-ind]*zi[-ind-1];
            }

            y_terms = 0.0;
            for (ind=n-1; ind>n-na-1; ind--)
            {
                if (ind>=0)
                    y_terms += -a[n-ind]*y[ind];
            }

            y[n] = x_terms + y_terms;
        }

        return y;
    }
    
    //Time domain digital filtering with phase distortion correction
    // using filter denumerator b. The numerator is 1.0, i.e. applies FIR filtering
    public static double[] filtfilt(double[] b, double[] x)
    {
        double[] a = new double[1];
        a[0] = 1.0;
        
        return filtfilt(b, a, x);
    }
    
    //Time domain digital filtering with phase distortion correction
    // using filter denumerator b and numerator a
    public static double[] filtfilt(double[] b, double[] a, double[] x)
    {
        int nfilt;
        int i, j;
        double[] tmpb = null;
        double[] tmpa = null;
        
        if (b.length>a.length)
        {
            nfilt = b.length;
            tmpb = new double[nfilt];
            tmpa = new double[nfilt];
            
            for (i=0; i<b.length; i++)
                tmpb[i] = b[i];
            for (i=0; i<a.length; i++)
                tmpa[i] = a[i];
            for (i=a.length; i<nfilt; i++)
                tmpa[i] = 0.0;
        }
        else
        {
            nfilt = a.length;
            tmpb = new double[nfilt];
            tmpa = new double[nfilt];
            
            for (i=0; i<a.length; i++)
                tmpa[i] = a[i];
            for (i=0; i<b.length; i++)
                tmpb[i] = b[i];
            for (i=b.length; i<nfilt; i++)
                tmpb[i] = 0.0;
        }
        
        int nfact = 3*(nfilt-1); //Length of edge transients
        int rwlen = 3*nfilt-5;
        
        int ylen = 2*nfact+x.length;
        double[] y = new double[ylen];
        
        double[] yRet = null;
        if (!(x.length<=nfact)) //Input data too short!
        {   
            //Solve system of linear equations for initial conditions
            //  zi are the steady-state states of the filter b(z)/a(z) in the state-space 
            //  implementation of the 'filter' command.
            int[] rows = new int[rwlen];
            for (i=0; i<=nfilt-2; i++)
                rows[i] = i;
            for (i=nfilt-1; i<=2*nfilt-4; i++)
                rows[i] = i-nfilt+2;
            for (i=2*nfilt-3; i<=3*nfilt-6; i++)
                rows[i] = i-2*nfilt+3;
            
            int[] cols = new int[rwlen];
            for (i=0; i<=nfilt-2; i++)
                cols[i] = 0;
            for (i=nfilt-1; i<=2*nfilt-4; i++)
                cols[i] = i-nfilt+2;
            for (i=2*nfilt-3; i<=3*nfilt-6; i++)
                cols[i] = i-2*nfilt+4;
            
            double[] data = new double[rwlen];
            data[0] = 1.0+tmpa[1];
            for (i=1; i<=nfilt-2; i++)
                data[i] = tmpa[i+1];
            for (i=nfilt-1; i<=2*nfilt-4; i++)
                data[i] = 1.0;
            for (i=2*nfilt-3; i<=3*nfilt-6; i++)
                data[i] = -1.0;
            
            int N = nfilt-1;
            double[][] sp = new double[N][N];
            
            for (i=0; i<N; i++)
            {
                for (j=0; j<N; j++)
                    sp[i][j] = 0.0f;
            }
            
            for (i=0; i<rwlen; i++)
                sp[rows[i]][cols[i]] = data[i];
            
            double[] denum = new double[N];
            for (i=0; i<N; i++)
                denum[i] = 0.0;
            for (i=2; i<nfilt+1; i++)
                denum[i-2] = b[i-1]-tmpa[i-1]*b[0];
            
            double[] zi = new double[N];
            for (i=0; i<N; i++)
                zi[i] = 0.0;
            
            sp = MathUtils.inverse(sp);
            
            double tmp;
            for (i=0; i<N; i++)
            {
                tmp=0.0;
                
                for (j=0; j<N; j++) 
                    tmp += sp[i][j]*denum[i];
                
                zi[i] = tmp;
            }
            
            //Extrapolate beginning and end of data sequence using a "reflection
            //  method".  Slopes of original and extrapolated sequences match at
            //  the end points.
            //  This reduces end effects.
            for (i=0; i<nfact; i++)
                y[i] = 2*x[0]-x[nfact-i];
            
            for (i=0; i<x.length; i++)
                y[i+nfact] = x[i];
            
            for (i=0; i<nfact; i++)
                y[nfact+x.length+i] = 2*x[x.length-1]-x[x.length-2-i];
            
            //Filter, reverse the data, filter again, and reverse the data again
            for (i=0; i<N; i++)
                zi[i] = zi[i]*y[0];
            
            y = filter(tmpb, tmpa, y, false, zi);
            y = SignalProcUtils.reverse(y);
            
            y = filter(tmpb, tmpa, y, false, zi);
            y = SignalProcUtils.reverse(y);

            // remove extrapolated pieces of y to write the output to x
            yRet = new double[x.length];
            for (i=0; i<x.length; i++)
                yRet[i] = y[i+nfact];
        }
        else
        {
            yRet = filter(b,a, x);
        }

        return yRet;
    }

    public static double[] filterfd(double[] filterFFTAbsMag, double[] x, double samplingRate)
    {
        return filterfd(filterFFTAbsMag, x, samplingRate, 0.020);
    }
    
    public static double[] filterfd(double[] filterFFTAbsMag, double[] x, double samplingRate, double winsize)
    {
        return filterfd(filterFFTAbsMag, x, samplingRate, winsize, 0.010);
    }
    
    public static double[] filterfd(double[] filterFFTAbsMag, double[] x, double samplingRate, double winsize, double skipsize)
    {
        double[] y = null;

        if (x!=null && filterFFTAbsMag!=null)
        {
            int ws = (int)Math.floor(winsize*samplingRate + 0.5);
            int ss = (int)Math.floor(skipsize*samplingRate + 0.5);

            int maxFreq = filterFFTAbsMag.length;
            int fftSize = 2*(maxFreq-1);
            if (ws>fftSize)
                ws = fftSize;

            int numfrm = (int)Math.floor((x.length-0.5*ws)/ss+0.5)+1;
            HammingWindow wgt = new HammingWindow(ws);
            wgt.normalize(1.0f);

            y = new double[x.length];
            Arrays.fill(y, 0.0);
            double[] w = new double[x.length];
            Arrays.fill(w, 0.0);
            int i, j;

            ComplexArray XFRM = new ComplexArray(fftSize);
            double[] yfrm = new double[ws];

            for (i=1; i<=numfrm; i++)
            {
                Arrays.fill(XFRM.real, 0.0);
                Arrays.fill(XFRM.imag, 0.0);
                for (j=1; j<=Math.min(ws, x.length-(i-1)*ss); j++)
                    XFRM.real[j-1] = x[(i-1)*ss+j-1];

                wgt.applyInline(XFRM.real, 0, ws);

                FFT.transform(XFRM.real, XFRM.imag, false);

                for (j=0; j<maxFreq; j++)
                {
                    XFRM.real[j] *= filterFFTAbsMag[j];
                    XFRM.imag[j] *= filterFFTAbsMag[j];
                }

                for (j=maxFreq+1; j<=fftSize; j++)
                {
                    XFRM.real[j-1] = XFRM.real[2*maxFreq-1-j];
                    XFRM.imag[j-1] = -XFRM.imag[2*maxFreq-1-j];
                }

                FFT.transform(XFRM.real, XFRM.imag, true);
                System.arraycopy(XFRM.real, 0, yfrm, 0, ws);

                for (j=(i-1)*ss+1; j<=Math.min(x.length, (i-1)*ss+ws); j++)
                {
                    y[j-1] += wgt.value(j-(i-1)*ss-1)*yfrm[j-(i-1)*ss-1];
                    w[j-1] += wgt.value(j-(i-1)*ss-1)*wgt.value(j-(i-1)*ss-1);
                }
            }

            for (i=1; i<=x.length; i++)
            {
                if (w[i-1]>0.0)
                    y[i-1] /= w[i-1];
            }
        }

        return y;
    }
    
    public static void addWhiteNoise(double[] x, double level)
    {
        for (int i=0; i<x.length; i++)
            x[i] += level*Math.random();
    }
    
    public static double[] getWhiteNoise(int totalSamples, double maxAbsGain)
    {
        double[] n = null;
        
        if (totalSamples>0)
        {
            n = new double[totalSamples];
            for (int i=0; i<totalSamples; i++)
                n[i] = 2.0*maxAbsGain*(Math.random()-0.5);
        }
        
        return n;
    }
    
    public static void main(String[] args)
    {  
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
    }
}




