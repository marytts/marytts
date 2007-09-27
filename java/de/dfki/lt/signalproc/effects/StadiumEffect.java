package de.dfki.lt.signalproc.effects;

public class StadiumEffect extends ChorusEffectBase {
    
    public StadiumEffect()
    {
        delaysInMiliseconds = new int[2];
        delaysInMiliseconds[0] = 466;
        delaysInMiliseconds[1] = 600;
        
        amps = new double[2];
        amps[0] = 0.54;
        amps[1] = -0.10;
    }
}
