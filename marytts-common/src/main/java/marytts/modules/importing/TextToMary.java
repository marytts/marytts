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
package marytts.modules.importing;

import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import marytts.data.Utterance;
import marytts.data.item.Paragraph;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.server.MaryProperties;
import marytts.util.MaryUtils;

import marytts.modules.InternalModule;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * Embed plain text input into a raw (untokenised) MaryXML document.
 *
 * @author Marc Schr&ouml;der
 */

public class TextToMary extends InternalModule {
    private static final String PARAGRAPH_SEPARATOR = "\\n(\\s*\\n)+";
    private DocumentBuilderFactory factory = null;
	private DocumentBuilder docBuilder = null;
	private boolean splitIntoParagraphs;

	public TextToMary() {
		super("TextToMary", MaryDataType.TEXT, MaryDataType.RAWMARYXML, null);
		splitIntoParagraphs = MaryProperties.getBoolean("texttomaryxml.splitintoparagraphs");
	}

	public void startup() throws Exception {
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
		String plain_text = MaryUtils.normaliseUnicodePunctuation(d.getPlainText());
		Locale l = determineLocale(plain_text, d.getLocale());

        // FIXME: old xml part still here, remove that
        MaryData result = new MaryData(outputType(), d.getLocale(), true);
		Document doc = result.getDocument();
		Element root = doc.getDocumentElement();
		root.setAttribute("xml:lang", MaryUtils.locale2xmllang(l));
		if (splitIntoParagraphs) { // Empty lines separate paragraphs
			String[] inputTexts = plain_text.split(PARAGRAPH_SEPARATOR);
			for (int i = 0; i < inputTexts.length; i++) {
				String paragraph = inputTexts[i].trim();
				if (paragraph.length() == 0)
					continue;
				appendParagraph(paragraph, root, d.getLocale());
			}
		} else { // The whole text as one single paragraph
			appendParagraph(plain_text, root, d.getLocale());
		}
		result.setDocument(doc);

        // New utterance part
        Utterance utt = new Utterance(plain_text, l);
		if (splitIntoParagraphs) { // Empty lines separate paragraphs
			String[] inputTexts = plain_text.split(PARAGRAPH_SEPARATOR);
			for (int i = 0; i < inputTexts.length; i++) {
				String paragraph_text = inputTexts[i].trim();
				if (paragraph_text.length() == 0)
					continue;
                Paragraph p = new Paragraph(paragraph_text);
                utt.addParagraph(p);
            }
		} else { // The whole text as one single paragraph
            Paragraph p = new Paragraph(plain_text);
            utt.addParagraph(p);
		}

		return result;
	}

	/**
	 * Append one paragraph of text to the rawmaryxml document. If the text language (as determined by #getLanguage(text)) differs
	 * from the enclosing document's language, the paragraph element is enclosed with a <code>&lt;voice xml:lang="..."&gt;</code>
	 * element.
	 *
	 * @param text
	 *            the paragraph text.
	 * @param root
	 *            the root node of the rawmaryxml document, where to insert the paragraph.
	 * @param defaultLocale
	 *            the default locale, in case the language of the text cannot be determined.
	 */
	private void appendParagraph(String text, Element root, Locale defaultLocale) {
		Element insertHere = root;
		String rootLanguage = root.getAttribute("xml:lang");
		String textLanguage = MaryUtils.locale2xmllang(determineLocale(text, defaultLocale));
		if (!textLanguage.equals(rootLanguage)) {
			Element voiceElement = MaryXML.appendChildElement(root, MaryXML.VOICE);
			voiceElement.setAttribute("xml:lang", textLanguage);
			insertHere = voiceElement;
		}
		insertHere = MaryXML.appendChildElement(insertHere, MaryXML.PARAGRAPH);
		// Now insert the entire plain text as a single text node
		insertHere.appendChild(root.getOwnerDocument().createTextNode(text));
		// And, for debugging, read it:
		Text textNode = (Text) insertHere.getFirstChild();
		String textNodeString = textNode.getData();
		logger.debug("textNodeString=`" + textNodeString + "'");
	}

	/**
	 * Try to determine the locale of the given text. This implementation simply returns the default locale; subclasses can try to
	 * do something fancy here.
	 *
	 * @param text
	 *            the text whose locale to determine
	 * @param defaultLocale
	 *            the default locale of the document.
	 * @return the locale as inferred from the text and the default locale
	 */
	protected Locale determineLocale(String text, Locale defaultLocale) {
		if (defaultLocale == null) {
			defaultLocale = Locale.getDefault();
			logger.warn("Locale is null, overriding with " + defaultLocale);
		}
		return defaultLocale;
	}

}
