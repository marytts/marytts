/**
 * Copyright 2010 DFKI GmbH.
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
import marytts.util.MaryUtils;

public class DummyDuration2AcoustParams extends InternalModule {
	public DummyDuration2AcoustParams() {
		this((Locale) null);
	}

	/**
	 * Constructor to be called with instantiated objects.
	 * 
	 * @param locale
	 *            locale
	 */
	public DummyDuration2AcoustParams(String locale) {
		super("DummyAllophones2AcoustParams", MaryDataType.DURATIONS, MaryDataType.ACOUSTPARAMS, MaryUtils.string2locale(locale));
	}

	/**
	 * Constructor to be called with instantiated objects.
	 * 
	 * @param locale
	 *            locale
	 */
	public DummyDuration2AcoustParams(Locale locale) {
		super("DummyAllophones2AcoustParams", MaryDataType.DURATIONS, MaryDataType.ACOUSTPARAMS, locale);
	}
}
