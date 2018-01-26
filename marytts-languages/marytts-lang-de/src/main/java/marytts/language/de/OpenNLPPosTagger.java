/**
 * Copyright 2003 DFKI GmbH.
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
package marytts.language.de;

import java.io.InputStream;

import marytts.exceptions.MaryConfigurationException;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de"></a>
 */
public class OpenNLPPosTagger extends marytts.modules.nlp.OpenNLPPosTagger
{

    /**
     * Constructor which can be directly called from init info in the config
     * file. Different languages can call this code with different settings.
     *
     * @throws Exception
     *             Exception
     */
    public OpenNLPPosTagger() throws Exception {
        super();

	// Set default
	InputStream stream = this.getClass().getResourceAsStream("/marytts/language/de/tagger/de-pos-maxent.bin");
	setModel(stream);
    }
}


/* OpenNLPPosTagger.java ends here */
