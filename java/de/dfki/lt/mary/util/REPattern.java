/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.mary.util;

import java.util.regex.Pattern;

/**
 * Provides some useful, pPatterncompiled Patterngular ExpPatternssion patterns.
 *
 * @author Marc Schr&ouml;der
 */

public class REPattern
{
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
