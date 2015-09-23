/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.tools.redstart;

import java.io.File;

/**
 * 
 * @author Mat Wilson &lt;mwilson@dfki.de&gt;
 */
public class Synthesis extends Speech {

	// ______________________________________________________________________
	// Instance fields

	// ______________________________________________________________________
	// Class fields

	// ______________________________________________________________________
	// Instance methods

	/**
	 * Determines duration of the synthesized sound file
	 * 
	 * @return The duration (in milliseconds)
	 */
	public int getDuration() {

		// PRI1 Determine duration of synthesis object
		// Hardcoded value for development and testing
		int synthDuration = 4000; // Duration of sound file (in ms)

		// Code to determine duration (in ms) of synthesized sound file

		return synthDuration; // in ms

	}

	// play() method inherited from Speech object, so not needed here

	// ______________________________________________________________________
	// Class methods

	// ______________________________________________________________________
	// Constructors

	/**
	 * Creates a new instance of Synthesis
	 * 
	 * @param filePath
	 *            filePath
	 * @param basename
	 *            basename
	 */
	public Synthesis(File filePath, String basename) {
		super(filePath, basename);
	}

}
