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
import marytts.util.dom.NameNodeFilter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

/**
 * Transforms a full MaryXML document into an MBROLA format string
 *
 * @author Marc Schr&ouml;der
 */

public class RealisedDurationsExtractor extends InternalModule {
	public RealisedDurationsExtractor() {
		super("Realised durations extractor", MaryDataType.AUDIO, MaryDataType.REALISED_DURATIONS, null);
	}

	public MaryData process(MaryData d) throws Exception {
		Document doc = d.getDocument();
		MaryData result = new MaryData(outputType(), d.getLocale());
		StringBuilder buf = new StringBuilder();
		buf.append("#\n");
		NodeIterator ni = ((DocumentTraversal) doc).createNodeIterator(doc, NodeFilter.SHOW_ELEMENT,
				new NameNodeFilter(new String[] { MaryXML.SENTENCE, MaryXML.PHONE, MaryXML.BOUNDARY }), false);
		Element element = null;
		float end = 0.f;
		float sentenceEnd = 0;
		while ((element = (Element) ni.nextNode()) != null) {
			String sampa = null;
			String durString = null;
			String endString = null;
			if (element.getTagName().equals(MaryXML.PHONE)) {
				sampa = element.getAttribute("p");
				durString = element.getAttribute("d"); // less accurate than end
				// endString = element.getAttribute("end");
			} else if (element.getTagName().equals(MaryXML.SENTENCE)) {
				sentenceEnd += end;
			} else {
				assert element.getTagName().equals(MaryXML.BOUNDARY);
				sampa = "_";
				durString = element.getAttribute("duration");
			}
			boolean printme = false;
			if (endString != null && !endString.equals("")) {
				end = Float.parseFloat(endString);
				printme = true;
			} else if (durString != null && !durString.equals("")) {
				float dur = Float.parseFloat(durString) * 0.001f;
				end += dur;
				printme = true;
			}
			if (printme) {
				buf.append((end + sentenceEnd) + " 125 " + sampa + "\n");
			}
		}

		result.setPlainText(buf.toString());
		return result;
	}
}
