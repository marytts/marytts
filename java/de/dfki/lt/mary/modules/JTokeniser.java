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

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.util.dom.DomUtils;
import de.dfki.lt.mary.util.dom.MaryDomUtils;
import de.dfki.lt.tools.tokenizer.JTok;
import de.dfki.lt.tools.tokenizer.annotate.AnnotatedString;
import de.dfki.lt.tools.tokenizer.annotate.FastAnnotatedString;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public abstract class JTokeniser extends InternalModule {

    private JTok jtok;

    public JTokeniser(MaryDataType inputType, MaryDataType outputType) {
        super("JTokeniser", inputType, outputType);
    }

     public void startup() throws Exception
     {
         super.startup();
         String tokeniserRoot = MaryProperties.needFilename("tokeniser.root");
         Properties jtokProperties = new Properties();
         System.setProperty("tokenizer.root", tokeniserRoot);
         jtokProperties.load(new FileInputStream(
            tokeniserRoot + File.separator + "jtok.cfg"));
         jtok = new JTok(jtokProperties);
     }

    public MaryData process(MaryData d)
    throws Exception
    {
        Document doc = d.getDocument();
        
        // The challenge in this module is that the tokeniser needs a plain text
        // version of this XML document, but we need to feed its output
        // back into the XML document.
        // Solution strategy: Remember the alignment of text with XML as an
        // annotated string. Each stretch of characters is annotated with the
        // DOM Text object to which it belonged. 
        NodeIterator ni = ((DocumentTraversal)doc).createNodeIterator(
            doc, NodeFilter.SHOW_TEXT, null, false);
        Text textNode;
        StringBuffer inputText = new StringBuffer();
        // Keep this loop in sync with the second loop, below:
        while ((textNode = (Text)ni.nextNode()) != null) {
            String text = textNode.getData().trim();
            if (text.length() == 0) continue;
            // Insert a space character between non-punctuation characters:
            if (inputText.length() > 0 &&
                !Character.isWhitespace(inputText.charAt(inputText.length() - 1)) &&
                Character.isLetterOrDigit(text.charAt(0))) {
                inputText.append(" ");
            }
            inputText.append(text);
        }
        FastAnnotatedString maryText = new FastAnnotatedString(inputText.toString());
        // And now go through the TEXT nodes a second time and annotate
        // the string.
        ni = ((DocumentTraversal)doc).createNodeIterator(
            doc, NodeFilter.SHOW_TEXT, null, false);
        int pos = 0;
        // Keep this loop in sync with the first loop, above:
        while ((textNode = (Text)ni.nextNode()) != null) {
            String text = textNode.getData().trim();
            int len = text.length();
            if (len == 0) continue;
            // Skip the space character between non-punctuation characters:
            if (pos > 0 &&
                !Character.isWhitespace(inputText.charAt(pos - 1)) &&
                Character.isLetterOrDigit(text.charAt(0))) {
                pos++;
            }
            maryText.annotate("MARYXML", textNode, pos, pos+len);
            pos += len;
        }
        // Now maryText is the input text annotated with the Text nodes in the
        // MaryXML document.
        // Tokenise:
        AnnotatedString tokenisedText = jtok.tokenize(inputText.toString(), inputType().getLocale().getLanguage());
        //System.err.println("maryText: `" + maryText.toString() + "'"); 
        //System.err.println("tokenisedText: `" + tokenisedText.toString() + "'"); 
        //assert tokenisedText.toString().equals(maryText.toString());
        // And now merge the output back into the MaryXML document.
        Element firstTokenInSentence = null;
        Element firstTokenInParagraph = null;
        Element previousToken = null;
        Text currentTextNode = null;
        char c = tokenisedText.setIndex(0);
        maryText.setIndex(0);
        while (c != AnnotatedString.DONE) {
            int tokenStart = tokenisedText.getRunStart(JTok.CLASS_ANNO);
            int tokenEnd = tokenisedText.getRunLimit(JTok.CLASS_ANNO);
            // check if c belongs to a token
            if (null != tokenisedText.getAnnotation(JTok.CLASS_ANNO)) {
                // We don't care about the actual annotation, only that there is one.
                // Where to insert the token:
                maryText.setIndex(tokenStart);
                Text tn = (Text) maryText.getAnnotation("MARYXML");
                assert tn != null;
                // Only create new token tag if there was none around the corresponding
                // text node in the input:
                Element t = null;
                if (MaryDomUtils.hasAncestor(tn, MaryXML.TOKEN)) {
                    t = (Element) MaryDomUtils.getAncestor(tn, MaryXML.TOKEN);
                } else { // need to create a new <t> element:
                    String token = tokenisedText.substring(tokenStart, tokenEnd);
                    t = MaryXML.createElement(doc, MaryXML.TOKEN);
                    MaryDomUtils.setTokenText(t, token);
                    // Insert the new token element
                    tn.getParentNode().insertBefore(t, tn);
                }
                // If we have by now moved past currentTextNode, delete it
                // (unless it was already inside a t tag):
                if (currentTextNode != null && currentTextNode != tn &&
                    !MaryDomUtils.hasAncestor(currentTextNode, MaryXML.TOKEN)) {
                    currentTextNode.getParentNode().removeChild(currentTextNode);
                }
                currentTextNode = tn;
                // Is this token the first in a new sentence or paragraph?
                if (null != tokenisedText.getAnnotation(JTok.BORDER_ANNO)) {
                    // Yes, this is the first token of a new sentence
                    // Need to enclose previous sentence with an <s> tag?
                    if (firstTokenInSentence != null) {
                        assert previousToken != null;
                        if (!MaryDomUtils.hasAncestor(firstTokenInSentence, MaryXML.SENTENCE) &&
                            !MaryDomUtils.hasAncestor(previousToken, MaryXML.SENTENCE)) {
                            Element firstPara = (Element) MaryDomUtils.getAncestor(firstTokenInSentence, MaryXML.PARAGRAPH);
                            Element lastPara = (Element) MaryDomUtils.getAncestor(previousToken, MaryXML.PARAGRAPH); 
                            if (firstPara == null && lastPara == null || firstPara.equals(lastPara)) {
                                // Either both tokens have no surrounding paragraph, or both have
                                // the same.
                                encloseWithSentence(firstTokenInSentence, previousToken);
                            }
                        }
                    }
                    firstTokenInSentence = null;
                    // Is it even a paragraph start?
                    if (tokenisedText.getAnnotation(JTok.BORDER_ANNO) ==
                        JTok.P_BORDER) {
                        // Need to enclose previous paragraph with a <p> tag?
                        if (firstTokenInParagraph != null) {
                            assert previousToken != null;
                            if (!MaryDomUtils.hasAncestor(firstTokenInParagraph, MaryXML.PARAGRAPH) &&
                                !MaryDomUtils.hasAncestor(previousToken, MaryXML.PARAGRAPH)) {
                                DomUtils.encloseNodesWithNewElement(
                                    DomUtils.getAncestor(firstTokenInParagraph, MaryXML.SENTENCE),
                                    DomUtils.getAncestor(previousToken, MaryXML.SENTENCE),
                                    MaryXML.PARAGRAPH);
                            }
                        }
                        firstTokenInParagraph = null;
                    }
                }
                previousToken = t;
                if (firstTokenInSentence == null) firstTokenInSentence = t;
                if (firstTokenInParagraph == null) firstTokenInParagraph = t;
            }
            c = tokenisedText.setIndex(tokenEnd);
            maryText.setIndex(tokenEnd);
        }
        // Remove the last old text node
        if (currentTextNode != null // should be
            && !MaryDomUtils.hasAncestor(currentTextNode, MaryXML.TOKEN)) 
            currentTextNode.getParentNode().removeChild(currentTextNode);
        // Insert the last <s> element
        if (firstTokenInSentence != null) {
            assert previousToken != null;
            if (!MaryDomUtils.hasAncestor(firstTokenInSentence, MaryXML.SENTENCE) &&
                !MaryDomUtils.hasAncestor(previousToken, MaryXML.SENTENCE)) {
                Element firstPara = (Element) MaryDomUtils.getAncestor(firstTokenInSentence, MaryXML.PARAGRAPH);
                Element lastPara = (Element) MaryDomUtils.getAncestor(previousToken, MaryXML.PARAGRAPH); 
                if (firstPara == null && lastPara == null || firstPara.equals(lastPara)) {
                    // Either both tokens have no surrounding paragraph, or both have
                    // the same.
                    encloseWithSentence(firstTokenInSentence, previousToken);
                }
            }
        }
        // Insert the last <p> element
        if (firstTokenInParagraph != null) {
            assert previousToken != null;
            if (!MaryDomUtils.hasAncestor(firstTokenInParagraph, MaryXML.PARAGRAPH) &&
                !MaryDomUtils.hasAncestor(previousToken, MaryXML.PARAGRAPH)) {
                DomUtils.encloseNodesWithNewElement(
                    DomUtils.getAncestor(firstTokenInParagraph, MaryXML.SENTENCE),
                    DomUtils.getAncestor(previousToken, MaryXML.SENTENCE),
                    MaryXML.PARAGRAPH);
            }
        }
        MaryData result = new MaryData(outputType());
        result.setDocument(doc);
        return result;
    }

    /**
     * @param firstTokenInSentence
     * @param lastTokenInSentence
     */
    private void encloseWithSentence(Element firstTokenInSentence, Element lastTokenInSentence) {
        // Allow for sentences to start with a <boundary/> element:
        Element encloseFromHere = firstTokenInSentence;
        Element maybeBoundary = DomUtils.getPreviousSiblingElement(firstTokenInSentence);
        if (maybeBoundary != null && maybeBoundary.getTagName().equals(MaryXML.BOUNDARY)) {
            encloseFromHere = maybeBoundary;
        }
        Element encloseToHere = lastTokenInSentence;
        maybeBoundary = DomUtils.getNextSiblingElement(lastTokenInSentence);
        if (maybeBoundary != null && maybeBoundary.getTagName().equals(MaryXML.BOUNDARY)) {
            encloseToHere = maybeBoundary;
        }
        DomUtils.encloseNodesWithNewElement(encloseFromHere, encloseToHere, MaryXML.SENTENCE);
    }


}
