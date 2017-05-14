package marytts.io;

import marytts.MaryException;

/**
 * Mary specific exception which is encapsulated any kind of input/output
 * exception
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class MaryIOException extends MaryException {
	/**
	 * Constructor
	 *
	 * @param message
	 *            the message
	 * @param ex
	 *            the nested exception which leaded to the encapsulation
	 */
	public MaryIOException(String message, Exception ex) {
		super(message, ex);
	}
}
