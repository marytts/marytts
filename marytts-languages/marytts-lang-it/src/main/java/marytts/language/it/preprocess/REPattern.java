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
package marytts.language.it.preprocess;

import java.util.regex.Pattern;

/**
 * Provides some useful, pPatterncompiled Patterngular ExpPatternssion patterns.
 * 
 * @author Marc Schr&ouml;der
 */

public class REPattern {
	public static final Pattern letterDot = Pattern.compile("[a-zäöüßA-ZÄÖÜ]\\.");
	public static final Pattern nonInitialCapital = Pattern.compile("[a-zäöüßA-ZÄÖÜ0-9]+[A-ZÄÖÜ]");
	public static final Pattern onlyConsonants = Pattern.compile("^[bcdfghj-np-tvwxzßBCDFGHJ-NP-TVWXZ]+$");
	public static final Pattern letter = Pattern.compile("[a-zäöüßA-ZÄÖÜ]");
	public static final Pattern digit = Pattern.compile("[0-9]");
	public static final Pattern onlyDigits = Pattern.compile("^[0-9]+$");
	public static final Pattern capitalLetter = Pattern.compile("[A-ZÄÖÜ]");
	public static final Pattern initialCapitalLetter = Pattern.compile("^[A-ZÄÖÜ]");
	public static final Pattern initialLowercaseLetter = Pattern.compile("^[a-zäöü]");
	public static final Pattern initialDigits = Pattern.compile("^[0-9]+");
	public static final Pattern initialNonDigits = Pattern.compile("^[^0-9]+");
	public static final Pattern emptyLine = Pattern.compile("^\\s*$");

}
