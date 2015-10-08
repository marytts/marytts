/**
 * Copyright 2005 DFKI GmbH.
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
package marytts.language.de.phonemiser;

/**
 * @author steigner
 * 
 *         To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code
 *         and Comments
 */
public class Result {
	private boolean usedOtherLanguageToPhonemise = false;
	private String transcription = null;

	public Result() {
	}

	/**
	 * @return transcription
	 */
	public String getTranscription() {
		return transcription;
	}

	/**
	 * @return usedOtherLanguageToPhonemise
	 */
	public boolean isUsedOtherLanguageToPhonemise() {
		return usedOtherLanguageToPhonemise;
	}

	/**
	 * @param string
	 *            string
	 */
	public void setTranscription(String string) {
		transcription = string;
	}

	/**
	 * @param b
	 *            b
	 */
	public void setUsedOtherLanguageToPhonemise(boolean b) {
		usedOtherLanguageToPhonemise = b;
	}

}
