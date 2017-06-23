package marytts.io.serializer;

/* Utils part */
import java.util.Locale;
import java.util.ArrayList;
import java.util.Hashtable;
import marytts.server.MaryProperties; // FIXME: need to be moved !
import marytts.util.MaryUtils;
import marytts.util.string.StringUtils;

/* IO part */
import java.io.File;
import java.io.StringWriter;
import java.io.StringReader;
import java.io.IOException;
import marytts.io.MaryIOException;

/* Marytts data part */
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;
import marytts.data.utils.IntegerPair;
import marytts.data.utils.SequenceTypePair;
import marytts.data.item.linguistic.*;
import marytts.data.item.phonology.*;
import marytts.data.item.prosody.*;
import marytts.data.item.*;

/* Logger */
import org.apache.log4j.Logger;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class TextSerializer implements Serializer {
    private static final String PARAGRAPH_SEPARATOR = "\\n(\\s*\\n)+";
    private boolean splitIntoParagraphs;
    protected Logger logger;

    /**
     * Constructor
     *
     */
    public TextSerializer() {
        logger = MaryUtils.getLogger("TextSerializer");
        splitIntoParagraphs = MaryProperties.getBoolean("texttomaryxml.splitintoparagraphs");
    }

    /**
     * Transform the utterance to a text. The result is actually just the
     * concatenation of the paragraph's text.
     *
     * @param utt
     *            the utterance
     * @return the text composed by the concatenation of the text of the
     *         paragraphs
     * @throws MaryIOException
     *             if anything is going wrong
     */
    public String toString(Utterance utt) throws MaryIOException {
        String output = "";
        Sequence<Paragraph> paragraphs = (Sequence<Paragraph>) utt.getSequence(
                                             SupportedSequenceType.PARAGRAPH);

        for (Paragraph par : paragraphs) {
            output += par.getText() + "\n";
        }

        return output;
    }

    /**
     * Import the text. The text is split into paragraph. Therefore the created
     * utterance contains only a sequence of paragraphs.
     *
     * @param text
     *            the input text
     * @return the created utterance
     * @throws MaryIOException
     *             if anything is going wrong
     */
    public Utterance fromString(String text) throws MaryIOException {
        String plain_text = MaryUtils.normaliseUnicodePunctuation(text);
        Locale l = Locale.US; // FIXME: we really need to fix this !

        // New utterance part
        Utterance utt = new Utterance(plain_text, l);
        Sequence<Paragraph> paragraphs = new Sequence<Paragraph>();
        if (splitIntoParagraphs) {
            // Empty lines separate paragraphs
            String[] inputTexts = plain_text.split(PARAGRAPH_SEPARATOR);
            for (int i = 0; i < inputTexts.length; i++) {
                String paragraph_text = inputTexts[i].trim();
                if (paragraph_text.length() == 0) {
                    continue;
                }
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
