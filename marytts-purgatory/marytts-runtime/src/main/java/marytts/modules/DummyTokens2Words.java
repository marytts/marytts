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
package marytts.modules;

import java.util.Locale;

import marytts.datatypes.MaryDataType;

/**
 * Dummy modules to support new language (for phone durations and phone f0)
 * 
 * @author Sathish Pammi
 */

public class DummyTokens2Words extends InternalModule {
	public DummyTokens2Words() {
		this((Locale) null);
	}

	/**
	 * Constructor to be called with instantiated objects.
	 * 
	 * @param locale
	 *            locale
	 */
	public DummyTokens2Words(String locale) {
		super("DummyTokens2Words", MaryDataType.TOKENS, MaryDataType.WORDS, new Locale(locale));
	}

	/**
	 * Constructor to be called with instantiated objects.
	 * 
	 * @param locale
	 *            locale
	 */
	public DummyTokens2Words(Locale locale) {
		super("DummyTokens2Words", MaryDataType.TOKENS, MaryDataType.WORDS, locale);
	}

}
