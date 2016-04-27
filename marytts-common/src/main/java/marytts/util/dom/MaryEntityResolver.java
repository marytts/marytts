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
package marytts.util.dom;

import java.io.IOException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * An entity resolver class to resolve the correct path for DTDs
 * 
 * @author Atta-Ur-Rehman Shah
 */

public class MaryEntityResolver implements EntityResolver {

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		if (systemId.contains("apml.dtd")) {
			// APML DTD - apml.dtd
			return new InputSource(MaryEntityResolver.class.getResourceAsStream("/marytts/dtd/apml.dtd"));
		} else if (systemId.contains("Sable.v0_2.dtd")) {
			// SABLE DTD Sable.v0_2.dtd
			return new InputSource(MaryEntityResolver.class.getResourceAsStream("/marytts/dtd/Sable.v0_2.dtd"));
		} else if (systemId.contains("sable-latin.ent")) {
			// SABLE ENT sable-latin.ent
			return new InputSource(MaryEntityResolver.class.getResourceAsStream("/marytts/dtd/sable-latin.ent"));
		} else {
			return null;
		}
	}

}
