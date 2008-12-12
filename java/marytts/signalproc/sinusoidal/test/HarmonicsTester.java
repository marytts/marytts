package marytts.signalproc.sinusoidal.test;

import java.io.IOException;

import marytts.signalproc.sinusoidal.Sinusoid;

public class HarmonicsTester extends SinusoidsTester {

    public HarmonicsTester(float freqInHz, int numHarmonics) 
    {
        this(freqInHz, numHarmonics, DEFAULT_DUR, DEFAULT_FS);
    }
    
    public HarmonicsTester(float freqInHz, int numHarmonics, float durationInSeconds) 
    {
        this(freqInHz, numHarmonics, durationInSeconds, DEFAULT_FS);
    }
    
    public HarmonicsTester(float freqInHz, int numHarmonics, float durationInSeconds, int samplingRateInHz) 
    {
        this(freqInHz, numHarmonics, durationInSeconds, samplingRateInHz, null, null);
    }
    
    public HarmonicsTester(float freqInHz, int numHarmonics, float[] amps) 
    {
        this(freqInHz, numHarmonics, amps, null);
    }
    
    public HarmonicsTester(float freqInHz, int numHarmonics, float[] amps, float[] phases) 
    {
        this(freqInHz, numHarmonics, DEFAULT_DUR, DEFAULT_FS, amps, phases);
    }
    
    public HarmonicsTester(float freqInHz, int numHarmonics, float durationInSeconds, float[] amps) 
    {
        this(freqInHz, numHarmonics, durationInSeconds, DEFAULT_FS, amps, null);
    }
    
    public HarmonicsTester(float freqInHz, int numHarmonics, float durationInSeconds, int samplingRateInHz, float[] amps)
    {
        this(freqInHz, numHarmonics, durationInSeconds, samplingRateInHz, amps, null);
    }
    
    public HarmonicsTester(float freqInHz, int numHarmonics, float durationInSeconds, int samplingRateInHz, float[] amps, float[] phases) 
    {
        if (numHarmonics>0)
        {
            Sinusoid[] sins = new Sinusoid[numHarmonics];
            
            float currentAmp, currentPhase;
            for (int i=0; i<numHarmonics; i++)
            {
                if (amps!=null && amps.length>i)
                    currentAmp = amps[i];
                else
                    currentAmp = DEFAULT_AMP;
                
                if (phases!=null && phases.length>i)
                    currentPhase = phases[i];
                else
                    currentPhase = DEFAULT_PHASE;
                
                sins[i] = new Sinusoid(currentAmp, freqInHz*(i+1), currentPhase);
            }
            
            init(sins, durationInSeconds, samplingRateInHz);
        }
    }
    
    public static void main(String[] args) throws IOException
    {
        HarmonicsTester s = null;
        
        //Single sinusoid, time-invariant
        float f1 = 400.0f;
        int numHarmonics = 8;
        s = new HarmonicsTester(f1, numHarmonics);
        //
        
        if (args.length>1)
            s.write(args[0], args[1]);
        else
            s.write(args[0]);
    }
}
