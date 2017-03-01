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

package marytts.modules.acoustic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import marytts.datatypes.MaryData;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.modules.acoustic.model.Model;
import marytts.modules.nlp.phonemiser.Allophone;
import marytts.modules.nlp.phonemiser.AllophoneSet;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import marytts.modules.InternalModule;


import marytts.data.Utterance;
import marytts.data.SupportedSequenceType;
import marytts.io.XMLSerializer;

/**
 * Predict duration and F0 using CARTs or other models
 *
 * @author steiner
 *
 */
public class AcousticModeller extends InternalModule {

	// three constructors adapted from DummyAllophones2AcoustParams (used if this is in modules.classes.list):

	public AcousticModeller() {
		this((Locale) null);
	}

	/**
	 * Constructor to be called with instantiated objects.
	 *
	 * @param locale
	 *            locale
	 */
	public AcousticModeller(String locale) {
		this(MaryUtils.string2locale(locale));
	}

	/**
	 * Constructor to be called with instantiated objects.
	 *
	 * @param locale
	 *            locale
	 */
	public AcousticModeller(Locale locale) {
		super("AcousticModeller", locale);
	}

	// three constructors adapted from CARTF0Modeller (used if this is in a voice's preferredModules):


	/**
	 * Constructor to be called with instantiated objects.
	 *
	 * @param locale
	 *            locale
	 * @param propertyPrefix
	 *            the prefix to be used when looking up entries in the config files, e.g. "english.f0"
	 * @param featureProcessorManager
	 *            the manager to use when looking up feature processors.
	 */
	protected AcousticModeller(Locale locale, String propertyPrefix)
    {
		super("AcousticModeller", locale);
	}

	public MaryData process(MaryData d) throws SynthesisException {
        return d;
	}
}
