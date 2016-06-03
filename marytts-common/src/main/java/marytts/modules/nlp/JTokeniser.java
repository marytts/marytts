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
package marytts.modules.nlp;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.util.dom.DomUtils;
import marytts.util.dom.MaryDomUtils;


import marytts.data.utils.IntegerPair;
import marytts.data.utils.SequenceTypePair;
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.item.linguistic.Paragraph;
import marytts.data.item.linguistic.Sentence;
import marytts.data.item.linguistic.Word;
import marytts.io.XMLSerializer;

import marytts.modules.InternalModule;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import de.dfki.lt.tools.tokenizer.JTok;
import de.dfki.lt.tools.tokenizer.annotate.AnnotatedString;
import de.dfki.lt.tools.tokenizer.annotate.FastAnnotatedString;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class JTokeniser extends InternalModule {
	public static final int TOKEN_MAXLENGTH = 100;

	private JTok jtok;
	private String jtokLocale;

	public JTokeniser() {
		this((Locale) null);
	}

	public JTokeniser(String locale) {
		super("JTokeniser", MaryDataType.RAWMARYXML, MaryDataType.TOKENS, new Locale(locale));
	}

	public JTokeniser(Locale locale) {
		this(MaryDataType.RAWMARYXML, MaryDataType.TOKENS, locale);
	}

	public JTokeniser(MaryDataType inputType, MaryDataType outputType, Locale locale) {
		super("JTokeniser", inputType, outputType, locale);
		// Which language to use in the Tokenizer?
		if (locale == null) {
			// if locale == null, use English tokeniser as default/fallback tokeniser
			jtokLocale = "en";
		} else {
			jtokLocale = locale.getLanguage();
		}
	}

	/**
	 * Set the tokenizer language to be different from the Locale of the module. This can be useful
	 * when reusing another language's tokenizer data.
	 *
	 * @param languageCode the language-code to use, as a two-character string such as "de" or "en".
	 */
	protected void setTokenizerLanguage(String languageCode) {
		jtokLocale = languageCode;
	}

	public void startup() throws Exception {
		super.startup();
		Properties jtokProperties = new Properties();
		jtokProperties.setProperty("languages", jtokLocale);
		jtokProperties.setProperty(jtokLocale, "jtok/" + jtokLocale);
		jtok = new JTok(jtokProperties);
	}

	public MaryData process(MaryData d) throws Exception {
		Document doc = d.getDocument();

        XMLSerializer xml_ser = new XMLSerializer();
        Utterance utt = xml_ser.unpackDocument(doc);

        // The challenge in this module is that the tokeniser needs a plain text
        // version of this XML document, but we need to feed its output
        // back into the XML document.
        // Solution strategy: Remember the alignment of text with XML as an
        // annotated string. Each stretch of characters is annotated with the
        // DOM Text object to which it belonged.
        StringBuilder inputText = new StringBuilder();
        for (Paragraph p : (Sequence<Paragraph>) utt.getSequence(Utterance.SupportedSequenceType.PARAGRAPH))
        {
            String text = ((Paragraph) p).getText().trim();

            // Insert a space character between non-punctuation characters:
            if ((inputText.length() > 0) &&
                (!Character.isWhitespace(inputText.charAt(inputText.length() - 1))) &&
                (Character.isLetterOrDigit(text.charAt(0))))
            {
                inputText.append(" ");
            }
            inputText.append(text);
        }
        FastAnnotatedString maryText = new FastAnnotatedString(inputText.toString());

        // And now go through the TEXT nodes a second time and annotate
        // the string.
        int pos = 0;
        for (Paragraph p : (Sequence<Paragraph>) utt.getSequence(Utterance.SupportedSequenceType.PARAGRAPH))
        {
            String text = ((Paragraph) p).getText().trim();
            int len = text.length();
            if (len == 0)
                continue;

            // Skip the space character between non-punctuation characters:
            if ((pos > 0) &&
                (!Character.isWhitespace(inputText.charAt(pos - 1))) &&
                (Character.isLetterOrDigit(text.charAt(0))))
            {
                pos++;
            }

            maryText.annotate("MARYXML", p, pos, pos + len);
            pos += len;
        }

        // Now maryText is the input text annotated with the Text nodes in the
        // MaryXML document.
        // Tokenise:
        AnnotatedString tokenisedText;
        tokenisedText = jtok.tokenize(inputText.toString(), jtokLocale);

        Sequence<Sentence> sentences = new Sequence<Sentence>();
        Sequence<Word> words = new Sequence<Word>();
        ArrayList<IntegerPair> alignment_paragraph_sentence = new ArrayList<IntegerPair>();
        ArrayList<IntegerPair> alignment_sentence_word = new ArrayList<IntegerPair>();

        // And now merge the output back into the utterance
        String sent_text = "";
        Word previousToken = null;
        int sentence_offset = 0;
        int sentence_index = 0;
        int word_offset = 0;
        int word_index = 0;
        int paragraph_index = 0;

        Text currentTextNode = null;
        char c = tokenisedText.setIndex(0);
        Word w = null;
        Paragraph p = null;
        maryText.setIndex(0);

        while (c != AnnotatedString.DONE)
        {
            int tokenStart = tokenisedText.getRunStart(JTok.CLASS_ANNO);
            int tokenEnd = tokenisedText.getRunLimit(JTok.CLASS_ANNO);

            // check if c belongs to a token
            if (null != tokenisedText.getAnnotation(JTok.CLASS_ANNO))
            {
                // We don't care about the actual annotation, only that there is one.
                // Where to insert the token:
                maryText.setIndex(tokenStart);

                // FIXME: should we keep that ?
                // p = (Paragraph) maryText.getAnnotation("MARYXML");
                // assert p != null;

                w = new Word(tokenisedText.substring(tokenStart, tokenEnd));
                words.add(w);
                sent_text += w.getText() + " ";
                word_index++;

                // Is this token the first in a new sentence or paragraph?
                if (null != tokenisedText.getAnnotation(JTok.BORDER_ANNO))
                {
                    // Flush the saved token to a sentence
                    if (word_offset < word_index)
                    {
                        Sentence s = new Sentence(sent_text, null);
                        sentences.add(s);

                        for(int i=word_offset; i<word_index; i++)
                            alignment_sentence_word.add(new IntegerPair(sentence_index, i));
                        word_offset = word_index;
                        sentence_index++;
                        sent_text = "";

                        if (tokenisedText.getAnnotation(JTok.BORDER_ANNO) == JTok.P_BORDER)
                        {
                            for (int i=sentence_offset; i<sentence_index; i++)
                            {
                                alignment_paragraph_sentence.add(new IntegerPair(paragraph_index, i));
                            }

                            // Move to the next paragraph
                            sentence_offset = sentence_index;
                            sentence_index = 0;
                            paragraph_index++;
                        }

                    }
                }
            }

            c = tokenisedText.setIndex(tokenEnd);
            maryText.setIndex(tokenEnd);
        }

        // Flush the saved token to a sentence
        if (word_offset < word_index)
        {
            Sentence s = new Sentence(sent_text, null);
            sentences.add(s);

            for(int i=word_offset; i<word_index; i++)
            {
                alignment_sentence_word.add(new IntegerPair(sentence_index, i));
            }

            sentence_index++;

            for (int i=sentence_offset; i<sentence_index; i++)
            {
                alignment_paragraph_sentence.add(new IntegerPair(paragraph_index, i));
            }
        }

        // Add the sequences to the utterance
        utt.addSequence(Utterance.SupportedSequenceType.SENTENCE, sentences);
        utt.addSequence(Utterance.SupportedSequenceType.WORD, words);


        // Create the relations and add them to the utterance
        Relation rel_par_sent = new Relation(utt.getSequence(Utterance.SupportedSequenceType.PARAGRAPH),
                                             utt.getSequence(Utterance.SupportedSequenceType.SENTENCE),
                                             alignment_paragraph_sentence);
        utt.setRelation(Utterance.SupportedSequenceType.PARAGRAPH, Utterance.SupportedSequenceType.SENTENCE, rel_par_sent);

        Relation rel_sent_wrd = new Relation(utt.getSequence(Utterance.SupportedSequenceType.SENTENCE),
                                             utt.getSequence(Utterance.SupportedSequenceType.WORD),
                                             alignment_sentence_word);
        utt.setRelation(Utterance.SupportedSequenceType.SENTENCE, Utterance.SupportedSequenceType.WORD, rel_sent_wrd);

        // Generate the result
        MaryData result = new MaryData(outputType(), d.getLocale());
        result.setDocument(xml_ser.generateDocument(utt));
        return result;
    }
}
