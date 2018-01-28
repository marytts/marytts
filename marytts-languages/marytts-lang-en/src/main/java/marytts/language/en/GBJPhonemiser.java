/**
 * Copyright 2002 DFKI GmbH.
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

package marytts.language.en;

// Configuration
import marytts.config.MaryConfiguration;
import marytts.config.MaryConfigurationFactory;
import marytts.exceptions.MaryConfigurationException;

import java.util.Locale;

/**
 * The phonemiser module -- java implementation.
 *
 * @author Marc Schr&ouml;der
 */

public class GBJPhonemiser extends marytts.modules.nlp.JPhonemiser {

    public GBJPhonemiser() throws MaryConfigurationException {
        super(Locale.UK);
    }

    public void startup() throws Exception {
        super.startup();

	// Apply the german configuration
	MaryConfigurationFactory.getConfiguration("en_GB").applyConfiguration(this);
	setAllophoneSet(this.getClass().getResourceAsStream("/marytts/language/en_GB/lexicon/allophones.en_GB.xml"));
	setLexicon(this.getClass().getResourceAsStream("/marytts/language/en_GB/lexicon/en_GB_lexicon.fst"));
	setLetterToSound(this.getClass().getResourceAsStream("/marytts/language/en_GB/lexicon/en_GB.lts"));


    }
}
