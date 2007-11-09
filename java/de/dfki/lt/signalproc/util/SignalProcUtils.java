package de.dfki.lt.signalproc.util;

import java.util.Arrays;

import de.dfki.lt.signalproc.FFT;
import de.dfki.lt.signalproc.analysis.PitchMarker;
import de.dfki.lt.signalproc.filter.RecursiveFilter;
import de.dfki.lt.signalproc.window.Window;

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
    public static PitchMarker pitchContour2pitchMarks(double [] f0s, int fs, int len, double ws, double ss, boolean bPaddZerosForFinalPitchMark)
    {
        //Interpolate unvoiced segments
        double [] interpf0s = interpolate_pitch_uv(f0s);
        int numfrm = f0s.length;
        int maxTotalPitchMarks = (int)(Math.floor(len/(fs/500.0) + 0.5) + 10); //Max number of pitch marks if pitch was highest(500.0 Hz), everything was voiced all along the signal
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
            
            T0 = (fs/interpf0s[ind-1]);
            
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

        PitchMarker pm = null;
        if (count>1)
        {
            //Check if last pitch mark corresponds to the end of signal, otherwise put an additional pitch mark to match the last period and note to padd sufficient zeros
            if (bPaddZerosForFinalPitchMark && tmpPitchMarks[count-1] != len-1)
            {
                pm = new PitchMarker(count+1, tmpPitchMarks, tmpVuvs, 0);
                pm.pitchMarks[pm.pitchMarks.length-1] = pm.pitchMarks[pm.pitchMarks.length-2]+(pm.pitchMarks[pm.pitchMarks.length-2]-pm.pitchMarks[pm.pitchMarks.length-3]);
                pm.totalZerosToPadd = pm.pitchMarks[pm.pitchMarks.length-1]-(len-1);
            }
            else
                pm = new PitchMarker(count, tmpPitchMarks, tmpVuvs, 0);
        }
        
        return pm;
    }
    
    // Interpolates unvoiced parts of the f0 contour 
    // using the neighbouring voiced parts
    // Linear interpolation is used
    public static double [] interpolate_pitch_uv(double [] f0s)
    {
        int [] ind_v = MathUtils.find(f0s, MathUtils.GREATER_THAN, 10);
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
    
    public static double [] interpolate_pitch_contour(double [] f0s, float beta)
    {
        double [] new_f0s = null;
        
        int newLen = (int)Math.floor(f0s.length*beta+0.5);
        if (newLen>0)
        {
            new_f0s = new double[newLen];
            Arrays.fill(new_f0s, 0.0);
            
            //Find voiceds and interpolate them segment by segment
            int segSt = -1;
            int segEn = -1;
            for (int i=0; i<f0s.length; i++)
            {
                if (f0s[i]>10.0) //Voiced
                {
                    if (segSt<0) //Start new segment
                        segSt=i;
                    
                    segEn=i; //Update current segment end
                }
                else //Unvoiced
                {
                    if (segSt>=0)
                    {
                        if (segEn>=0) //Segment ended
                        {
                            double [] tmp_f0s = new double[segEn-segSt+1];
                            System.arraycopy(f0s, segSt, tmp_f0s, 0, segEn-segSt+1);
                            int tmpNewLen = (int)Math.floor(tmp_f0s.length*beta+0.5);
                            if (tmpNewLen>0)
                            {
                                double [] tmpNew_f0s = MathUtils.interpolate(tmp_f0s, tmpNewLen);
                                int newSegSt = (int)Math.floor(segSt*beta+0.5);
                                System.arraycopy(tmpNew_f0s, 0, new_f0s, newSegSt, Math.min(tmpNewLen, new_f0s.length-newSegSt));
                            }
                        }
                    }
                    
                    segSt = -1;
                    segEn = -1;
                } 
            }
            
            if (segSt>=0 && (segEn==-1 || segEn==f0s.length-1)) //The final segment is all voiced
            {
                double [] tmp_f0s = new double[segEn-segSt+1];
                System.arraycopy(f0s, segSt, tmp_f0s, 0, segEn-segSt+1);
                int tmpNewLen = (int)Math.floor(tmp_f0s.length*beta+0.5);
                if (tmpNewLen>0)
                {
                    double [] tmpNew_f0s = MathUtils.interpolate(tmp_f0s, tmpNewLen);
                    int newSegSt = (int)Math.floor(segSt*beta+0.5);
                    System.arraycopy(tmpNew_f0s, 0, new_f0s, newSegSt, Math.min(tmpNewLen, new_f0s.length-newSegSt));
                }
            }
        }
        
        
        return new_f0s;
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
    public static void preemphasize(double [] frm, double preCoef)
    {
        double [] coeffs = new double[2];
        coeffs[0] = 1.0;
        coeffs[1] = -preCoef;
        
        RecursiveFilter r = new RecursiveFilter(coeffs);
        
        r.apply(frm);
    }
    
    //Remove preemphasis from preemphasized frame frm (i.e. 1st order lowspass filtering)
    public static void removePreemphasize(double [] frm, double preCoef)
    {
        double [] coeffs = new double[2];
        coeffs[0] = 1.0;
        coeffs[1] = preCoef;
        
        RecursiveFilter r = new RecursiveFilter(coeffs);
        
        r.apply(frm);
    }
    
    public static double freq2bark(double freqInHz)
    {
        return 13.0*Math.atan(0.00076*freqInHz)+3.5*Math.atan((freqInHz*freqInHz/(7500*7500)));
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
}




