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

import java.util.Locale;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Read a simple phone string and generate default acoustic parameters.
 * 
 * @author Marc Schr&ouml;der
 */

public class SimplePhoneme2AP extends InternalModule {
	private DocumentBuilderFactory factory = null;
	private DocumentBuilder docBuilder = null;
	protected AllophoneSet allophoneSet;

	public SimplePhoneme2AP(String localeString) {
		this(MaryDataType.SIMPLEPHONEMES, MaryDataType.ACOUSTPARAMS, MaryUtils.string2locale(localeString));
	}

	public SimplePhoneme2AP(MaryDataType inputType, MaryDataType outputType, Locale locale) {
		super("SimplePhoneme2AP", inputType, outputType, locale);
	}

	public void startup() throws Exception {
		allophoneSet = MaryRuntimeUtils.needAllophoneSet(MaryProperties.localePrefix(getLocale()) + ".allophoneset");
		if (factory == null) {
			factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
		}
		if (docBuilder == null) {
			docBuilder = factory.newDocumentBuilder();
		}
		super.startup();
	}

	public MaryData process(MaryData d) throws Exception {
		String phoneString = d.getPlainText();
		MaryData result = new MaryData(outputType(), d.getLocale(), true);
		Document doc = result.getDocument();
		Element root = doc.getDocumentElement();
		root.setAttribute("xml:lang", MaryUtils.locale2xmllang(d.getLocale()));
		Element insertHere = root;
		Voice defaultVoice = d.getDefaultVoice();
		if (defaultVoice != null) {
			Element voiceElement = MaryXML.createElement(doc, MaryXML.VOICE);
			voiceElement.setAttribute("name", defaultVoice.getName());
			root.appendChild(voiceElement);
			insertHere = voiceElement;
		}
		int cumulDur = 0;
		boolean isFirst = true;
		StringTokenizer stTokens = new StringTokenizer(phoneString);
		while (stTokens.hasMoreTokens()) {
			Element token = MaryXML.createElement(doc, MaryXML.TOKEN);
			insertHere.appendChild(token);
			String tokenPhonemes = stTokens.nextToken();
			token.setAttribute("ph", tokenPhonemes);
			StringTokenizer stSyllables = new StringTokenizer(tokenPhonemes, "-_");
			while (stSyllables.hasMoreTokens()) {
				Element syllable = MaryXML.createElement(doc, MaryXML.SYLLABLE);
				token.appendChild(syllable);
				String syllablePhonemes = stSyllables.nextToken();
				syllable.setAttribute("ph", syllablePhonemes);
				int stress = 0;
				if (syllablePhonemes.startsWith("'"))
					stress = 1;
				else if (syllablePhonemes.startsWith(","))
					stress = 2;
				if (stress != 0) {
					// Simplified: Give a "pressure accent" do stressed syllables
					syllable.setAttribute("accent", "*");
					token.setAttribute("accent", "*");
				}
				Allophone[] phones = allophoneSet.splitIntoAllophones(syllablePhonemes);
				for (int i = 0; i < phones.length; i++) {
					Element ph = MaryXML.createElement(doc, MaryXML.PHONE);
					ph.setAttribute("p", phones[i].name());
					int dur = 70;
					if (phones[i].isVowel()) {
						dur = 100;
						if (stress == 1)
							dur *= 1.5;
						else if (stress == 2)
							dur *= 1.2;
					}
					ph.setAttribute("d", String.valueOf(dur));
					cumulDur += dur;
					ph.setAttribute("end", String.valueOf(cumulDur));

					syllable.appendChild(ph);
				}
			}
		}
		Element boundary = MaryXML.createElement(doc, MaryXML.BOUNDARY);
		boundary.setAttribute("bi", "4");
		boundary.setAttribute("duration", "400");
		insertHere.appendChild(boundary);
		return result;
	}
}
