/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.mary.modules;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.util.MaryUtils;

/**
 * Embed plain text input into a raw (untokenised) MaryXML document.
 *
 * @author Marc Schr&ouml;der
 */

public class TextToMaryXML extends InternalModule
{
    private DocumentBuilderFactory factory = null;
    private DocumentBuilder docBuilder = null;
    private boolean splitIntoParagraphs;

    public TextToMaryXML(MaryDataType inputType, MaryDataType outputType)
    {
        super("TextToMaryXML", inputType, outputType);
        // Just to activate the generic data type, "the" default input type:
        MaryDataType.get("TEXT");
        splitIntoParagraphs = MaryProperties.getBoolean("texttomaryxml.splitintoparagraphs");
    }

    public void startup() throws Exception
    {
        if (factory == null) {
            factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
        }
        if (docBuilder == null) {
            docBuilder = factory.newDocumentBuilder();
        }
        super.startup();
    }

    public MaryData process(MaryData d)
    throws Exception
    {
        String plainText = MaryUtils.normaliseUnicodePunctuation(d.getPlainText());
        MaryData result = new MaryData(outputType(), true);
        Document doc = result.getDocument();
        Element root = doc.getDocumentElement();
        String language = getLanguage(plainText);
        if (language == null)
            throw new NullPointerException("Cannot determine language");
        root.setAttribute("xml:lang", language);
        if (splitIntoParagraphs) { // Empty lines separate paragraphs
            String[] inputTexts = plainText.split("\\n(\\s*\\n)+");
            for (int i=0; i<inputTexts.length; i++) {
                String paragraph = inputTexts[i].trim();
                if (paragraph.length() == 0) continue;
                appendParagraph(paragraph, root);
            }
        } else { // The whole text as one single paragraph
            appendParagraph(plainText, root);
        }
        result.setDocument(doc);
        return result;
    }
    
    /**
     * Append one paragraph of text to the rawmaryxml document. If the text
     * language (as determined by #getLanguage(text)) differs from the
     * enclosing document's language, the paragraph element is enclosed with a
     * <code>&lt;voice xml:lang="..."&gt;</code> element.
     * @param text the paragraph text.
     * @param root the root node of the rawmaryxml document, where to insert
     * the paragraph.
     */
    private void appendParagraph(String text, Element root) {        
        Element insertHere = root;
        String rootLanguage = root.getAttribute("xml:lang");
        String textLanguage = getLanguage(text);
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

    protected String getLanguage(String text)
    {
        if (inputType().getLocale() != null)
            return inputType().getLocale().getLanguage();
        return null;
    }

}
