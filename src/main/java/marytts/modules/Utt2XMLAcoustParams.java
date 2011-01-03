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
package marytts.modules;

import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;



/**
 * Convert FreeTTS utterances into MaryXML format
 * (Acoustic Parameters, not language specific).
 *
 * @author Marc Schr&ouml;der
 */

public class Utt2XMLAcoustParams extends Utt2XMLBase
{
    public Utt2XMLAcoustParams()
    {
        super("Utt2XML AcoustParams",
              MaryDataType.FREETTS_ACOUSTPARAMS,
              MaryDataType.ACOUSTPARAMS,
              null);
    }


}

