package de.dfki.lt.mary.sinusoidal;

public class SinusoidalSpeechSignal {
    public SinusoidalSpeechFrame [] framesSins;
    public float originalDurationInSeconds ;
    
    public SinusoidalSpeechSignal(int totalFrm)
    {
        if (totalFrm>0)
            framesSins =  new SinusoidalSpeechFrame[totalFrm];
        else
            framesSins = null;
    }
}
