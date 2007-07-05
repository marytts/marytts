package de.dfki.lt.signalproc.process;

import de.dfki.lt.signalproc.util.MathUtils;

public class NullFDEffect extends PolarFrequencyProcessor {
    public NullFDEffect(int fftSize)
    {
        super(fftSize);
    }
    
    protected void processPolar(double[] r, double[] phi)
    {

    }
    
}
