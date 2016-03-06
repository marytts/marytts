package marytts.data.item;

/**
 *  A phone representation. A phone is a phoneme with acoustic properties.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Phone extends Phoneme
{
	private double m_duration;
	
	public Phone(String label, double duration)
	{
		super(label);
		setDuration(duration);
	}

	public double getDuration() 
	{
		return m_duration;
	}

	protected void setDuration(double duration) 
	{
		m_duration = duration;
	}
}
