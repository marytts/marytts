package marytts.language.lb;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.language.fr.Phonemiser;
import marytts.modules.JPhonemiser;
import marytts.util.dom.MaryDomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;

import java.io.IOException;

public class LuxembourgishPhonemiser extends JPhonemiser {

    Phonemiser frenchPhonemiser;

    public LuxembourgishPhonemiser() throws IOException, MaryConfigurationException {
        super("lb.");
        frenchPhonemiser = new Phonemiser();
    }

    /**
     * Determine pronunciation for all tokens in the input, adding <code>ph</code> and <code>g2p_method</code> attributes.
     *
     * @param input MaryData to process
     * @return output processed MaryData
     */
    @Override
    public MaryData process(MaryData input) {
        Document document = input.getDocument();

        NodeIterator tokenIterator = MaryDomUtils.createNodeIterator(document, MaryXML.TOKEN);
        Element token;
        while ((token = (Element) tokenIterator.nextNode()) != null) {
            String transcription;
            String text = token.getTextContent().trim();
            String pos = token.getAttribute("pos");
            if ((transcription = userdictLookup(text, pos)) != null) {
                token.setAttribute(MaryXML.PHONE, transcription);
                token.setAttribute("g2p_method", "userdict");
            } else if ((transcription = lexiconLookup(text, pos)) != null) {
                token.setAttribute(MaryXML.PHONE, transcription);
                token.setAttribute("g2p_method", "lexicon");
            } else if ((transcription = frenchPhonemiser.lexiconLookup(text, pos)) != null) {
                token.setAttribute(MaryXML.PHONE, transcription);
                token.setAttribute("g2p_method", "lexicon_fr");
            } else {
                String phones = lts.predictPronunciation(text);
                try {
                    transcription = lts.syllabify(phones);
                    token.setAttribute(MaryXML.PHONE, transcription);
                    token.setAttribute("g2p_method", "rules");
                } catch (IllegalArgumentException e) {
                    logger.error(String.format("Problem with token <%s> [%s]: %s", text, phones, e.getMessage()));
                }
            }
        }

        MaryData output = new MaryData(getOutputType(), input.getLocale());
        output.setDocument(document);
        return output;
    }
}
