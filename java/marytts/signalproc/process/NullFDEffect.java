package marytts.signalproc.process;

import marytts.util.math.MathUtils;

public class NullFDEffect extends PolarFrequencyProcessor {
    public NullFDEffect(int fftSize)
    {
        super(fftSize);
    }
    
    protected void processPolar(double[] r, double[] phi)
    {

    }
    
}
