package marytts.io.serializer;

import java.util.Locale;
import java.util.ArrayList;
import java.util.Hashtable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.io.File;
import java.io.StringWriter;
import java.io.StringReader;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import marytts.util.dom.MaryEntityResolver;

import marytts.data.utils.IntegerPair;
import marytts.data.utils.SequenceTypePair;
import marytts.io.MaryIOException;
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;
import marytts.data.item.linguistic.*;
import marytts.data.item.phonology.*;
import marytts.data.item.prosody.*;
import marytts.data.item.*;
import marytts.server.MaryProperties; // FIXME: need to be moved !
import marytts.util.MaryUtils;
import marytts.util.string.StringUtils;
import org.apache.log4j.Logger;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class TextSerializer implements Serializer {
	private static final String PARAGRAPH_SEPARATOR = "\\n(\\s*\\n)+";
	private static final String NAMESPACE = "http://mary.dfki.de/2002/MaryXML";
	private boolean splitIntoParagraphs;
	protected Logger logger;

	public TextSerializer() {
		logger = MaryUtils.getLogger("TextSerializer");
		splitIntoParagraphs = MaryProperties.getBoolean("texttomaryxml.splitintoparagraphs");
	}

	public String toString(Utterance utt) throws MaryIOException

	{
		String output = "";
		Sequence<Paragraph> paragraphs = (Sequence<Paragraph>) utt.getSequence(SupportedSequenceType.PARAGRAPH);

		for (Paragraph par : paragraphs) {
			output += par.getText() + "\n";
		}

		return output;
	}
	public Utterance fromString(String str) throws MaryIOException {

		String plain_text = MaryUtils.normaliseUnicodePunctuation(str);
		Locale l = Locale.US; // FIXME: we really need to fix this !

		// New utterance part
		Utterance utt = new Utterance(plain_text, l);
		Sequence<Paragraph> paragraphs = new Sequence<Paragraph>();
		if (splitIntoParagraphs) {
			// Empty lines separate paragraphs
			String[] inputTexts = plain_text.split(PARAGRAPH_SEPARATOR);
			for (int i = 0; i < inputTexts.length; i++) {
				String paragraph_text = inputTexts[i].trim();
				if (paragraph_text.length() == 0)
					continue;
				Paragraph p = new Paragraph(paragraph_text);
				paragraphs.add(p);
			}
		}
		// The whole text as one single paragraph
		else {
			Paragraph p = new Paragraph(plain_text);
			paragraphs.add(p);
		}
		utt.addSequence(SupportedSequenceType.PARAGRAPH, paragraphs);

		return utt;
	}
}
