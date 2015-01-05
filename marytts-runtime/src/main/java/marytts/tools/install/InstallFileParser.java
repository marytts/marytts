/**
 * Copyright 2009 DFKI GmbH.
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

package marytts.tools.install;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author marc
 *
 */
public class InstallFileParser {
	private static DocumentBuilder builder;

	static {
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (Exception e) {
			e.printStackTrace();
			builder = null;
		}
	}

	private List<LanguageComponentDescription> languages = new ArrayList<LanguageComponentDescription>();
	private List<VoiceComponentDescription> voices = new ArrayList<VoiceComponentDescription>();

	public InstallFileParser(URL installFile) throws IOException, SAXException {
		Document doc;
		try {
			InputStream in = installFile.openStream();
			doc = builder.parse(in);
			in.close();
		} catch (Exception e) {
			throw new IOException("Problem parsing install file " + installFile, e);
		}
		Element docElt = doc.getDocumentElement();
		if (!docElt.getTagName().equals("marytts-install")) {
			throw new IllegalArgumentException("Expected <marytts-install> document, but found root node <" + docElt.getTagName()
					+ ">!");
		}
		NodeList languageElements = docElt.getElementsByTagName("language");
		for (int i = 0, max = languageElements.getLength(); i < max; i++) {
			Element languageElement = (Element) languageElements.item(i);
			languages.add(new LanguageComponentDescription(languageElement));
		}
		NodeList voiceElements = docElt.getElementsByTagName("voice");
		for (int i = 0, max = voiceElements.getLength(); i < max; i++) {
			Element voiceElement = (Element) voiceElements.item(i);
			voices.add(new VoiceComponentDescription(voiceElement));
		}
	}

	public List<LanguageComponentDescription> getLanguageDescriptions() {
		return languages;
	}

	public List<VoiceComponentDescription> getVoiceDescriptions() {
		return voices;
	}
}
