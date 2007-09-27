package de.dfki.lt.signalproc.effects;

public class JetPilotEffect extends FilterEffectsBase {
    
    public JetPilotEffect()
    {
        super(500.0, 2000.0, BANDPASS_FILTER);
    }
}
