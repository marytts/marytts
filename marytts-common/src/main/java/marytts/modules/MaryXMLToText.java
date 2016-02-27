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

// DOM classes
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.modules.synthesis.Voice;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Create simple text from a rawmaryxml document.
 * 
 * @author Marc Schr&ouml;der
 */

public class MaryXMLToText extends InternalModule {
	public MaryXMLToText() {
		super("MaryXMLToText", MaryDataType.RAWMARYXML, MaryDataType.TEXT, null);
	}

	public void startup() throws Exception {
		// local startup goes here
		super.startup();
	}

	public MaryData process(MaryData d) throws Exception {
		Document doc = d.getDocument();

		MaryData result = new MaryData(outputType(), d.getLocale());
		result.setPlainText(MaryDomUtils.getPlainTextBelow(doc));
		Element voiceElement = MaryDomUtils.getFirstElementByTagName(doc.getDocumentElement(), MaryXML.VOICE);
		if (voiceElement != null) {
			Voice voice = Voice.getVoice(voiceElement.getAttribute("name"));
			if (voice != null)
				result.setDefaultVoice(voice);
		}
		return result;
	}
}
