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

public class Word {

	// morphology flags
	private boolean couldBeParticipleInBaseForm = false;
	private boolean isVerbalGerund = false;
	private boolean couldBeParticiple = false;
	// test
	private boolean cutOffCharacter = false;
	private boolean wordMinusFlectionEndsWithVowel = false;
	private boolean extraSyll = false;
	// Flag for the JPhonemiser
	// private boolean usedOtherLanguageToPhonemise = false;

	private String otherLanguageBaseform = null;
	private String toBePhonemised = null;
	private String flectionEnding = null;

	// private String transcription = null;

	public Word(String toBePhonemised) {
		this.toBePhonemised = toBePhonemised;
	}

	// public boolean getUsedOtherLanguageToPhonemise() {
	// return this.usedOtherLanguageToPhonemise;
	// }

	public boolean getCouldBeParticipleInBaseForm() {
		return this.couldBeParticipleInBaseForm;
	}

	public boolean getIsVerbalGerund() {
		return this.isVerbalGerund;
	}

	public boolean getCouldBeParticiple() {
		return this.couldBeParticiple;
	}

	public boolean getCutOffCharacter() {
		return this.cutOffCharacter;
	}

	public boolean getExtraSyll() {
		return this.extraSyll;
	}

	public boolean getWordMinusFlectionEndsWithVowel() {
		return this.wordMinusFlectionEndsWithVowel;
	}

	// public String getTranscription(){
	// return this.transcription;
	// }

	public String getOtherLanguageBaseForm() {
		return this.otherLanguageBaseform;
	}

	public String getToBePhonemised() {
		return this.toBePhonemised;
	}

	public String getFlectionEnding() {
		return this.flectionEnding;
	}

	// public void setUsedOtherLanguageToPhonemise (boolean usedOtherLanguageToPhonemise) {
	// this.usedOtherLanguageToPhonemise = usedOtherLanguageToPhonemise;
	// }

	public void setCouldBeParticipleInBaseForm(boolean couldBeParticipleInBaseForm) {
		this.couldBeParticipleInBaseForm = couldBeParticipleInBaseForm;
	}

	public void setIsVerbalGerund(boolean isVerbalGerund) {
		this.isVerbalGerund = isVerbalGerund;
	}

	public void setCouldBeParticiple(boolean couldBeParticiple) {
		this.couldBeParticiple = couldBeParticiple;
	}

	public void setCutOffCharacter(boolean cutOffCharacter) {
		this.cutOffCharacter = cutOffCharacter;
	}

	public void setExtraSyll(boolean extraSyll) {
		this.extraSyll = extraSyll;
	}

	public void setWordMinusFlectionEndsWithVowel(boolean wordMinusFlectionEndsWithVowel) {
		this.wordMinusFlectionEndsWithVowel = wordMinusFlectionEndsWithVowel;
	}

	// public void setTranscription (String transcription) {
	// this.transcription = transcription;
	// }

	public void setOtherLanguageBaseForm(String otherLanguageBaseform) {
		this.otherLanguageBaseform = otherLanguageBaseform;
	}

	public void setToBePhonemised(String toBePhonemised) {
		this.toBePhonemised = toBePhonemised;
	}

	public void setFlectionEnding(String flectionEnding) {
		this.flectionEnding = flectionEnding;
	}

}
