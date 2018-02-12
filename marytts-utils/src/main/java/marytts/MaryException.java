package marytts;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class MaryException extends Exception {
    public MaryException(String message) {
        super(message);
    }

    public MaryException(String message, Throwable embedded_exception) {
        super(message, embedded_exception);
    }
}
