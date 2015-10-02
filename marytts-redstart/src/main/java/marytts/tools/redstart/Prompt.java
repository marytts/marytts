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

//import marytts.tools.redstart.AdminWindow;
import java.io.File;

/**
 * 
 * @author Mat Wilson &lt;mwilson@dfki.de&gt;
 */
public class Prompt {

	// ______________________________________________________________________
	// Instance fields
	private String basename; // Basename used in filenames for this prompt
	private String promptText; // Prompt text to display to speaker
	private String promptTranscriptionText; // Prompt Transcription text to display to speaker
	protected Synthesis synthesized; // Synthesized version of the prompt - not needed here?
	protected Recording recorded; // Recorded version(s) of the prompt

	// ______________________________________________________________________
	// Class fields

	// ______________________________________________________________________
	// Instance methods

	/**
	 * Gets the basename for the prompt
	 * 
	 * @return The basename for the current prompt
	 */
	public String getBasename() {
		return basename;
	}

	/**
	 * Gets the prompt text for the prompt
	 * 
	 * @return The prompt text for the current prompt
	 */
	public String getPromptText() {
		return promptText;
	}

	/**
	 * Gets the prompt transcription text for the prompt
	 * 
	 * @return The prompt transcription text for the current prompt
	 */
	public String getPromptTranscriptionText() {
		return promptTranscriptionText;
	}

	/**
	 * Sets the prompt text for the prompt
	 * 
	 * @param text
	 *            prompt text for the current prompt
	 */
	public void setPromptText(String text) {
		this.promptText = text;
	}

	/**
	 * Sets the prompt transcription text for the prompt
	 * 
	 * @param text
	 *            prompt transcription text for the current prompt
	 */
	public void setPromptTranscriptionText(String text) {
		this.promptTranscriptionText = text;
	}

	/**
	 * Get the recording object associated to this prompt.
	 * 
	 * @return recorded
	 */
	public Recording getRecording() {
		return recorded;
	}

	/**
	 * Gets the the number of recordings for the current prompt
	 * 
	 * @return The number of recordings for the current prompt
	 */
	public int getRecCount() {
		return recorded.getFileCount();
	}

	public Synthesis getSynthesis() {
		return synthesized;
	}

	// ______________________________________________________________________
	// Class methods

	// ______________________________________________________________________
	// Constructors

	/**
	 * Creates a new instance of Prompt
	 * 
	 * @param passedBasename
	 *            The basename for the prompt (e.g., spike0003)
	 * @param recFolderPath
	 *            The file path for the voice (e.g., path for Spike)
	 * @param synthFolderPath
	 *            synthFolderPath
	 */
	public Prompt(String passedBasename, File recFolderPath, File synthFolderPath) {

		this.basename = passedBasename;
		this.recorded = new Recording(recFolderPath, this.basename);
		this.synthesized = new Synthesis(synthFolderPath, this.basename);

	}

}
