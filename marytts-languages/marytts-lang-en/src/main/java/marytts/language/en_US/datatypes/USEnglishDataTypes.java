/**
 * Copyright 2000-2008 DFKI GmbH.
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
package marytts.language.en_US.datatypes;

import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;

/**
 * This class will register the data types that are specific for the US English synthesis modules.
 * 
 * @author marc
 * 
 */
public class USEnglishDataTypes {
	public static final MaryDataType PAUSES_US = new MaryDataType("PAUSES_US", true, true, MaryDataType.MARYXML, MaryXML.MARYXML);
	public static final MaryDataType PHRASES_US = new MaryDataType("PHRASES_US", true, true, MaryDataType.MARYXML,
			MaryXML.MARYXML);
}
