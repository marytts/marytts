package de.dfki.lt.signalproc.util;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.signalproc.analysis.FrameBasedAnalyser;
import de.dfki.lt.signalproc.analysis.LPCAnalyser;
import de.dfki.lt.signalproc.analysis.LineSpectralFrequencies;
import de.dfki.lt.signalproc.analysis.LPCAnalyser.LPCoeffs;
import de.dfki.lt.signalproc.window.HammingWindow;

public class DistanceMeasures {
  
    /*
     * Inverse harmonic weighting based LSF distance
     * Performs LP analysis by first preemphasizing and windowing speech frames <speechFrame1> and <speechFrame2>
     * Then, computes LSFs for the two speech frames
     * Finally, the perceptual distance between two lsf vectors are estimated.
     * Note that the input speech frames are not changed during distance computation
     * LP order is automatically decided depending on the sampling rate
     * 
     * speechFrame1: First speech frame (not windowed)
     * speechFrame2: Second speech frame (not windowed)
     * samplingRate: Sampling rate in Hz
     * 
     */
    public static double lsfDist(double [] speechFrame1, double [] speechFrame2, int samplingRate)
    {
        return lsfDist(speechFrame1, speechFrame2, samplingRate, SignalProcUtils.getLPOrder(samplingRate));
    }
    
    
    /*
     * Inverse harmonic weighting based LSF distance
     * Performs LP analysis by first preemphasizing and windowing speech frames <speechFrame1> and <speechFrame2>
     * Then, computes LSFs for the two speech frames
     * Finally, the perceptual distance between two lsf vectors are estimated.
     * Note that the input speech frames are not changed during distance computation
     * 
     * speechFrame1: First speech frame (not windowed)
     * speechFrame2: Second speech frame (not windowed)
     * samplingRate: Sampling rate in Hz
     * lpOrder: Desired LP analysis order
     * 
     */
    public static double lsfDist(double [] speechFrame1, double [] speechFrame2, int samplingRate, int lpOrder)
    {
        //Preemphasis
        double [] windowedSpeechFrame1 = new double[speechFrame1.length];
        System.arraycopy(speechFrame1, 0, windowedSpeechFrame1, 0, speechFrame1.length);
        SignalProcUtils.preemphasize(windowedSpeechFrame1, 0.97);
        
        double [] windowedSpeechFrame2 = new double[speechFrame2.length];
        System.arraycopy(speechFrame2, 0, windowedSpeechFrame2, 0, speechFrame2.length);
        SignalProcUtils.preemphasize(windowedSpeechFrame2, 0.97);
        //
        
        //Windowing
        HammingWindow w1 = new HammingWindow(speechFrame1.length);
        w1.apply(windowedSpeechFrame1, 0);
        HammingWindow w2 = new HammingWindow(speechFrame2.length);
        w2.apply(windowedSpeechFrame2, 0);
        //
        
        //LP analysis
        LPCoeffs lpcs1 = LPCAnalyser.calcLPC(windowedSpeechFrame1, lpOrder);
        LPCoeffs lpcs2 = LPCAnalyser.calcLPC(windowedSpeechFrame2, lpOrder);
        //
        
        //LPC to LSF conversion
        double [] lsfs1 = LineSpectralFrequencies.lpc2lsf(lpcs1.getOneMinusA(), 4, samplingRate);
        double [] lsfs2 = LineSpectralFrequencies.lpc2lsf(lpcs2.getOneMinusA(), 4, samplingRate);
        //
        
        return getLsfDist(lsfs1, lsfs2, samplingRate);
    }
    
    public static double getLsfDist(double [] lsfs1, double [] lsfs2, int samplingRate)
    { 
        double [] lsfWgt = getInverseHarmonicLSFWeights(lsfs1);
        
        return getLsfDist(lsfs1, lsfs2, samplingRate, lsfWgt);
    }
    
    public static double getLsfDist(double [] lsfs1, double [] lsfs2, int samplingRate, double [] lsfWgt)
    {
        assert lsfs1.length == lsfs2.length;
        assert lsfs1.length == lsfWgt.length;
        
        double dist = 0.0;
        
        for (int i=0; i<lsfs1.length; i++)
            dist += 0.1*lsfWgt[i]*Math.abs(lsfs1[i]-lsfs2[i]);
        
        dist = dist*16000/samplingRate;
        dist = Math.min(20.0, dist/lsfs1.length);
        dist = 10*(dist+1e-36);
        
        return dist;
    }
    
    public static double [] getInverseHarmonicLSFWeights(double [] lsfs)
    {
        assert lsfs.length>1;
        
        int P = lsfs.length;
        
        int i;
      
        double [] lsfWgt = new double[P];
        
        lsfWgt[0] = 1.0/Math.abs(lsfs[1]-lsfs[0]);
        lsfWgt[P-1] = 0.5/Math.abs(lsfs[P-1]-lsfs[P-2]);

        for (i=1; i<=P-2; i++)
           lsfWgt[i] = 1.0/Math.min(Math.abs(lsfs[i]-lsfs[i-1]), Math.abs(lsfs[i+1]-lsfs[i]));

        //Emphasize low frequency LSFs 
        double tmp = 0.0;
        for (i=0; i<P; i++)
        {
           lsfWgt[i] = Math.exp(-0.05*i)*lsfWgt[i];
           tmp += lsfWgt[i];
        }
        
        //Normalize
        for (i=0; i<P; i++)
            lsfWgt[i] /= tmp;

        //Compress dynamic range
        tmp = 0.0;
        for (i=0; i<=P-1; i++)
        {
           lsfWgt[i] = Math.sqrt(lsfWgt[i]);
           tmp += lsfWgt[i];
        }
        
      //Normalize
        for (i=0; i<P; i++)
            lsfWgt[i] /= tmp;
        
        return lsfWgt;
    }
    
    public static void main(String[] args)
    {
        AudioInputStream inputAudio = null;
        try {
            inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        } catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (inputAudio != null)
        {
            int samplingRate = (int)inputAudio.getFormat().getSampleRate();
            int ws = (int)(samplingRate*0.020);
            int ss = (int)(samplingRate*0.010);
            
            AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
            double [] x = signal.getAllData();
            
            int numfrm = (int)((x.length-(double)ws)/ss-2.0);
            double [] frm1 = new double[ws]; 
            double [] frm2 = new double[ws];
            double lsfDist;
            
            for (int i=0; i<numfrm; i++)
            {
                System.arraycopy(x, i*ss, frm1, 0, ws);
                System.arraycopy(x, (i+1)*ss, frm2, 0, ws);
                
                lsfDist = DistanceMeasures.lsfDist(frm1, frm2, samplingRate);
                System.out.println("Distance between frame " + String.valueOf(i+1) + " and frame " + String.valueOf(i+2) + " = " + String.valueOf(lsfDist));
            }
        }
    }
}
