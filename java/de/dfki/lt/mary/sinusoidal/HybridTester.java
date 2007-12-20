package de.dfki.lt.mary.sinusoidal;

import java.io.IOException;
import java.util.Arrays;

import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;

//This class combines SinusoidsTester and NoiseTester classes
// You can simply give a SinusoidsTester object and NoiseTester object
// and generate the superposition of the outputs of the two objects,
// i.e. multiple sinusoids located at desired instants of time plus white or pink noise
// with desired bandwidth at desired time instants
// Using this class one can generate fairly complicated yet parametric test signals
public class HybridTester extends BaseTester{
    public HybridTester(SinusoidsTester s, NoiseTester n)
    {
        assert s.fs == n.fs;
        fs = s.fs;
        
        int i;
        int maxLen = 0;
        if (s.signal!=null)
            maxLen = s.signal.length;
        
        if (n.signal!=null && n.signal.length>maxLen)
            maxLen = n.signal.length;
        
        signal = new double[maxLen];
        pitchMarks = null;
        
        Arrays.fill(signal, 0.0);
        
        if (s.signal!=null)
        {
            System.arraycopy(s.signal, 0, signal, 0, s.signal.length);
            
            System.out.println("1: " + String.valueOf(MathUtils.getAbsMax(signal)));
            
            if (n.signal!=null)
            {
                for (i=0; i<n.signal.length; i++)
                    signal[i] += n.signal[i];
            }
            
            System.out.println("2: " + String.valueOf(MathUtils.getAbsMax(signal)));
            
            for (i=0; i<signal.length; i++)
                signal[i] *= 0.5;
            
            System.out.println("3: " + String.valueOf(MathUtils.getAbsMax(signal)));
            
            if (s.pitchMarks!=null)
            {
                if (s.pitchMarks[s.pitchMarks.length-1]>=maxLen-1) //Everything is covered by sinusoids so no need to worry about noise part
                {
                    pitchMarks = new int[s.pitchMarks.length];
                    System.arraycopy(s.pitchMarks, 0, pitchMarks, 0, s.pitchMarks.length);
                }
                else //We need some extra fixed rate marks to cover all signal
                {
                    int maxT0 = (int)Math.floor(0.010f*fs+0.5);
                    int pitchMarksLen = s.pitchMarks.length;
                    int currInd;
                    
                    if (n.pitchMarks!=null)
                    {
                        int noiseInd = n.pitchMarks.length-1;
                        
                         //Compute how much extra we will need:
                         while (n.pitchMarks[noiseInd]-s.pitchMarks[s.pitchMarks.length-1]>maxT0)
                         {
                             noiseInd--;
                             if (noiseInd<0)
                             {
                                 noiseInd=0;
                                 break;
                             }
                         }
                         
                         pitchMarksLen += n.pitchMarks.length-noiseInd;
                         
                         //Check if there is still a gap between the sinusoid end and noise start:
                         if (noiseInd==0)
                         {
                             currInd = n.pitchMarks[0];
                             while (currInd-s.pitchMarks[s.pitchMarks.length-1]>maxT0)
                             {
                                 pitchMarksLen++;
                                 currInd-=maxT0;
                             }
                         }
                         
                         pitchMarks = new int[pitchMarksLen];

                         System.arraycopy(s.pitchMarks, 0, pitchMarks, 0, s.pitchMarks.length);
                         
                         for (i=s.pitchMarks.length; i<s.pitchMarks.length+n.pitchMarks.length-noiseInd; i++)
                             pitchMarks[i] = n.pitchMarks[i-s.pitchMarks.length+noiseInd];
                         
                         for (i=s.pitchMarks.length+n.pitchMarks.length-noiseInd; i<pitchMarksLen; i++)
                             pitchMarks[i] = Math.min(pitchMarks[i-1]+maxT0, maxLen-1);
                    }
                    else //Noise pitch marks do not exist so just generate extra marks at the end to cover all signal
                    {
                        currInd = s.pitchMarks[s.pitchMarks.length-1];
                        while (maxLen-currInd>maxT0)
                        {
                            pitchMarksLen++;
                            currInd+=maxT0;
                        }
                        
                        pitchMarks = new int[pitchMarksLen];

                        System.arraycopy(s.pitchMarks, 0, pitchMarks, 0, s.pitchMarks.length);
                        
                        for (i=s.pitchMarks.length; i<pitchMarksLen; i++)
                            pitchMarks[i] = Math.min(pitchMarks[i-1]+maxT0, maxLen-1);
                    }
                }
                
            }   
            else //Generate fixed pitch marks all along the signal
            {
                int maxT0 = (int)Math.floor(0.010f*fs+0.5);
                int numPitchMarks = (int)(Math.floor(((double)maxLen)/maxT0+0.5)) + 1; 
                pitchMarks = new int[numPitchMarks];
                for (i=0; i<numPitchMarks; i++)
                    pitchMarks[i] = Math.min(i*maxT0, maxLen-1);
            }
        }
        else
        {
            System.arraycopy(n.signal, 0, signal, 0, n.signal.length);
            
            if (n.pitchMarks!=null)
            {
                pitchMarks = new int[n.pitchMarks.length];
                System.arraycopy(n.pitchMarks, 0, pitchMarks, 0, n.pitchMarks.length);
            }
        }
        
        if (pitchMarks!=null)
            f0s = SignalProcUtils.pitchMarks2PitchContour(pitchMarks, ws, ss, fs);
    }

    public static void main(String[] args) throws IOException
    {
        int i, numSins, numNoises;
        
        SinusoidsTester s = null;
        NoiseTester n = null;
        HybridTester h = null;
        
        //Sinus part
        numSins = 4;
        float [] sinFreqs = new float[numSins];
        sinFreqs[0] = 80.0f;
        sinFreqs[1] = 280.0f;
        sinFreqs[2] = 780.0f;
        sinFreqs[3] = 6600.0f;
        
        Sinusoid [] sins = new Sinusoid[numSins];
        for (i=0; i<numSins; i++)
            sins[i] = new Sinusoid(100.0f, sinFreqs[i], 0.0f);
        
        s = new SinusoidsTester(sins);
        //
        
        //Noise part
        numNoises = 1;
        float [][] freqs = new float[numNoises][];
        float [] amps = new float[1];
        for (i=0; i<numNoises; i++)
            freqs[i] = new float[2];
        
        freqs[0][0] = 2000;
        freqs[0][1] = 4000;
        amps[0] = 100.0f;

        n = new NoiseTester(freqs, amps);
        //
        
        h = new HybridTester(s, n);
        
        h.write(args[0], args[1]);
        
        System.out.println(String.valueOf(MathUtils.getAbsMax(h.signal)) + " " + MathUtils.getAbsMax(s.signal) + " " + MathUtils.getAbsMax(n.signal));
        
        int [] pitchMarks = FileUtils.readFromBinaryFile(args[1]);
        for (i=0; i<pitchMarks.length; i++)
            System.out.println(String.valueOf(pitchMarks[i]) + " ");
    }
}
