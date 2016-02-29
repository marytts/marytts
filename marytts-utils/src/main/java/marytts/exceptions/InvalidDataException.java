/**
 * 
 */
package marytts.exceptions;

/**
 * An exception class representing cases where data provided to a processing unit does not match the specifications.
 * 
 * @author marc
 * 
 */
public class InvalidDataException extends RuntimeException {

	/**
	 * @param message
	 *            message
	 */
	public InvalidDataException(String message) {
		super(message);
	}

	/**
	 * @param message
	 *            message
	 * @param cause
	 *            cause
	 */
	public InvalidDataException(String message, Throwable cause) {
		super(message, cause);
	}

}
