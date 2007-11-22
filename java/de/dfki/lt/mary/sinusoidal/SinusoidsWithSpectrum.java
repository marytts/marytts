package de.dfki.lt.mary.sinusoidal;

import java.util.Vector;

// Single speech frame sinusoids with spectrum
public class SinusoidsWithSpectrum {
    public Sinusoid [] sinusoids;
    public double [] systemAmps;
    public double [] systemPhases;
    
    public SinusoidsWithSpectrum(int numSins)
    {
        if (numSins>0)
            sinusoids = new Sinusoid[numSins];
        else
            sinusoids = null;
        
        systemAmps = null;
        systemPhases = null;
    }
    
    public SinusoidsWithSpectrum(SinusoidsWithSpectrum existing)
    {
        this(existing.sinusoids.length);
        
        for (int i=0; i<existing.sinusoids.length; i++)
            sinusoids[i] = new Sinusoid(existing.sinusoids[i]);
        
        setSystemAmps(existing.systemAmps);
        setSystemPhases(existing.systemPhases);
    }
    
    public void setSystemAmps(double [] newAmps)
    {
        if (newAmps!=null && newAmps.length>0)
        {
            systemAmps = new double[newAmps.length];
            System.arraycopy(newAmps, 0, systemAmps, 0, newAmps.length);
        }
        else
            systemAmps = null;
    }
    
    public void setSystemPhases(double [] newPhases)
    {
        if (newPhases!=null && newPhases.length>0)
        {
            systemPhases = new double[newPhases.length];
            System.arraycopy(newPhases, 0, systemPhases, 0, newPhases.length);
        }
        else
            systemPhases = null;
    }
}
