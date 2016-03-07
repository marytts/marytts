package marytts;
/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class MaryException extends Exception
{
    private Exception m_embedded_exception;
    public MaryException(String message, Exception embedded_exception)
    {
        super(message);
        this.m_embedded_exception = embedded_exception;
    }

    public Exception getEmbeddedException()
    {
        return m_embedded_exception;
    }
}
