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
package marytts.language.en.prosody;

/**
 * Information on how to recognize and how to realize different sentence types.
 */
public class SentenceType {
	public static final SentenceType declarative = new SentenceType("declarative", "L-L%", "H-L%", "H-", "!H*", "L+H*");
	public static final SentenceType interrogative = new SentenceType("interrogative", "H-H%", "H-L%", "H-", "L*", "H*");
	public static final SentenceType exclamation = new SentenceType("exclamation", "L-L%", "H-L%", "H-", "H*", "H*");
	public static final SentenceType interrogYN = new SentenceType("interrogYN", "H-H%", "H-L%", "H-", "L*", "H*");
	public static final SentenceType interrogWH = new SentenceType("interrogWH", "L-L%", "H-L%", "H-", "H*", "H*");

	public static SentenceType punctuationType(String punct) {
		if (punct.equals("."))
			return declarative;
		else if (punct.equals("?"))
			return interrogative;
		else if (punct.equals("!"))
			return exclamation;
		else
			return null;
	}

	private String name;
	private String sentenceFinalBoundary;
	private String nonFinalMajorBoundary;
	private String minorBoundary;
	private String nuclearAccent;
	private String nonNuclearAccent;

	private SentenceType(String name, String sentenceFinalBoundary, String nonFinalMajorBoundary, String minorBoundary,
			String nuclearAccent, String nonNuclearAccent) {
		this.name = name;
		this.sentenceFinalBoundary = sentenceFinalBoundary;
		this.nonFinalMajorBoundary = nonFinalMajorBoundary;
		this.minorBoundary = minorBoundary;
		this.nuclearAccent = nuclearAccent;
		this.nonNuclearAccent = nonNuclearAccent;
	}

	public String name() {
		return name;
	}

	public String toString() {
		return name();
	}

	public String sentenceFinalBoundary() {
		return sentenceFinalBoundary;
	}

	public String nonFinalMajorBoundary() {
		return nonFinalMajorBoundary;
	}

	public String minorBoundary() {
		return minorBoundary;
	}

	public String nuclearAccent() {
		return nuclearAccent;
	}

	public String nonNuclearAccent() {
		return nonNuclearAccent;
	}

}
