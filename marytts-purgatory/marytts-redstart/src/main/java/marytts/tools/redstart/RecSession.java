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

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 
 * @author Mat Wilson &lt;mwilson@dfki.de&gt;
 */
public class RecSession {

	// ______________________________________________________________________
	// Instance fields

	private Prompt[] promptArray; // Set of prompts

	// ______________________________________________________________________
	// Class fields

	// ______________________________________________________________________
	// Instance methods

	/**
	 * Gets the array of prompts for the current recording session
	 * 
	 * @return The array of prompts for the current recording session
	 */
	public Prompt[] getPromptArray() {
		return promptArray;
	}

	// ______________________________________________________________________
	// Class methods

	// ______________________________________________________________________
	// Constructors

	/**
	 * Creates a new instance of RecSession
	 * 
	 * @param adminWindow
	 *            adminWindow
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 * @throws IOException
	 *             IOException
	 */
	public RecSession(AdminWindow adminWindow) throws FileNotFoundException, IOException {

		// Create a new prompt set object
		PromptSet sessionPrompts = new PromptSet(adminWindow);

		this.promptArray = sessionPrompts.promptArray;
	}

}
