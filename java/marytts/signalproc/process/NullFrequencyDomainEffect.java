package marytts.signalproc.process;


public class NullFrequencyDomainEffect extends PolarFrequencyProcessor {
    public NullFrequencyDomainEffect(int fftSize)
    {
        super(fftSize);
    }
    
    protected void processPolar(double[] r, double[] phi)
    {

    }
    
}
