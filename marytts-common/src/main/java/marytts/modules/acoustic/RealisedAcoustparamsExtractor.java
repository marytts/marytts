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
package marytts.modules.acoustic;

// DOM classes
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;

import marytts.modules.InternalModule;

/**
 * Transforms a full MaryXML document into an MBROLA format string
 *
 * @author Marc Schr&ouml;der
 */

public class RealisedAcoustparamsExtractor extends InternalModule {
	public RealisedAcoustparamsExtractor() {
		super("Realised acoustparams extractor", MaryDataType.AUDIO, MaryDataType.REALISED_ACOUSTPARAMS, null);
	}

	public MaryData process(MaryData d) throws Exception {
        return d;
	}

}
