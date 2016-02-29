/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.exceptions;

/**
 * A class representing severe expected error conditions, such as wrong format of data files needed to set up the system.
 * Typically a MaryConfigurationException means it is impossible to continue operating. According to the fail-early strategy, it
 * is preferable to throw MaryConfigurationException during server startup, and to abort the startup if one is thrown.
 * 
 * @author marc
 * 
 */
public class MaryConfigurationException extends Exception {
	/**
	 * Construct a MaryConfigurationException with only an error message. This constructor should only be used if our program code
	 * has identified the error condition. In order to wrap another Exception into a MaryConfigurationException with a meaningful
	 * error message, use {@link #MaryConfigurationException(String, Throwable)}.
	 * 
	 * @param message
	 *            a meaningful error message describing the problem.
	 */
	public MaryConfigurationException(String message) {
		super(message);
	}

	/**
	 * Create a MaryConfigurationException with a message and a cause. Use this to wrap another Exception into a
	 * MaryConfigurationException with a meaningful error message.
	 * 
	 * @param message
	 *            a meaningful error message describing the problem.
	 * @param cause
	 *            the exception or error that caused the problem.
	 */
	public MaryConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

}
