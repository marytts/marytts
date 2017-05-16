package marytts.features;

/**
 * Exception defined for when a user try to redefined a feature already existing.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class FeatureCollisionException extends Exception
{

    /**
     * Constructor
     *
     * @param msg the message of the exception
     */
    public FeatureCollisionException(String msg)
    {
        super(msg);
    }
}


/* FeatureCollisionException.java ends here */
