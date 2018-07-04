package marytts.features;

import marytts.MaryException;

/**
 * Exception to indicate that the user is trying to get a processor which has
 * not been defined.
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class UnknownProcessorException extends MaryException {
    /**
     * Only constructor as a message needs to be defined to explain the proper
     * cause.
     *
     * @param msg
     *            the message to identify the proper cause
     */
    public UnknownProcessorException(String msg) {
        super(msg);
    }
}

