package marytts.io.serializer;

import java.util.Locale;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

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

import marytts.MaryException;

import marytts.todisappear.MaryEntityResolver;

import marytts.data.utils.IntegerPair;
import marytts.data.utils.SequenceTypePair;

import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;
import marytts.data.item.linguistic.*;
import marytts.data.item.phonology.*;
import marytts.data.item.prosody.*;
import marytts.data.item.acoustic.*;
import marytts.data.item.*;

import marytts.features.FeatureMap;
import marytts.io.MaryIOException;
import marytts.features.Feature;

import marytts.util.MaryUtils;
import marytts.util.string.StringUtils;


/* Logger */
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Serialiazer to be able to support the MaryXML document format. Some decisions
 * has been take to force the compatibility with the current code. Therefore,
 * from now, the paragraph node and the sentence node <b>must have</b> the
 * corresponding text.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class XMLSerializer implements Serializer {

    /** The namespace of MaryXML */
    private static final String NAMESPACE = "http://mary.dfki.de/2002/MaryXML";

    /** The logger of the serializer */
    protected static Logger logger =  LogManager.getLogger(XMLSerializer.class);

    /**
     * Constructor
     *
     */
    public XMLSerializer() {
    }

    /**
     * Serialize a given utterance to an XML document.
     *
     * @param utt
     *            the given utterance
     * @return the XML document in String format
     * @throws MaryIOException
     *             if something wrong happened
     */
    public Object export(Utterance utt) throws MaryIOException {
        try {
    Document doc = generateDocument(utt);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            // initialize StreamResult with File object to save to file
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);
            return result.getWriter().toString();
        } catch (TransformerConfigurationException ex) {
    throw new MaryIOException("Transformer configuration exception", ex);
        } catch (TransformerException ex) {
    throw new MaryIOException("Transformer exception", ex);
        }
    }

    /**
     * Load an utterance from the XML document string given in parameter
     *
     * @param doc_str
     *            the XML document
     * @return the loaded utterance
     * @throws MaryIOException
     *             if something wrong happened
     */
    public Utterance load(String doc_str) throws MaryIOException {
        try {
    return unpackDocument(doc_str);
        } catch (Exception ex) {
    throw new MaryIOException("couldn't convert the document from string", ex);
        }
    }

    /************************************************************************************************
     * Document generation part
     ***********************************************************************************************/
    /**
     * Generate the XML document from the utterance
     *
     * @param utt
     *            the utterance
     * @return the XML document
     * @throws MaryIOException
     *             if something wrong happened
     */
    public Document generateDocument(Utterance utt) throws MaryIOException {
        try {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setExpandEntityReferences(true);
            docFactory.setNamespaceAware(true);

            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElementNS(NAMESPACE, "maryxml");

            // FIXME: hardcoded part
            rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            rootElement.setAttribute("version", "0.5");
            // FIXME: rootElement.setAttribute("xml:lang", MaryUtils.locale2xmllang(utt.getLocale()));

            // Adding paragraphs
            int nb_par = utt.getSequence(SupportedSequenceType.PARAGRAPH).size();
            for (int i = 0; i < nb_par; i++) {
    rootElement.appendChild(exportParagraph(utt, i, doc));
            }

            // Finalise and returns the doc
            doc.appendChild(rootElement);
            return doc;
        } catch (ParserConfigurationException ex) {
    throw new MaryIOException("Parsing exception (utterance not created so no rendering)", ex);
        } catch (MaryException ex) {
    throw new MaryIOException("Data structure problem", ex);
        }
    }

    /**
     * Call back to export a paragraph into an XML Element
     *
     * @param utt
     *            the utterance which contains the paragraph sequence
     * @param par_index
     *            the index of the paragraph
     * @param doc
     *            the XML document which is going to contain the created element
     * @return the element corresponding to the paragraph in the XML document
     */
    public Element exportParagraph(Utterance utt, int par_index, Document doc) throws MaryException {
        // FIXME: to remove
        Element par_element = doc.createElementNS(NAMESPACE, "p");

        // Export node value
        Paragraph paragraph = ((Sequence<Paragraph>) utt.getSequence(SupportedSequenceType.PARAGRAPH)).get(
 par_index);
        Node text = doc.createTextNode(paragraph.getText());
        par_element.appendChild(text);

        // Export subelements
        Relation rel_par_sent = utt.getRelation(SupportedSequenceType.PARAGRAPH,
                                                SupportedSequenceType.SENTENCE);

        // FIXME: needs to have a "containsRelation" method
        if (rel_par_sent != null) {
    int[] sentences = rel_par_sent.getRelatedIndexes(par_index);
            for (int i = 0; i < sentences.length; i++) {
    par_element.appendChild(exportSentence(utt, sentences[i], doc));
            }
        }

        return par_element;
    }

    /**
     * Call back to export a sentence into an XML Element
     *
     * @param utt
     *            the utterance which contains the sentence sequence
     * @param sent_index
     *            the index of the sentence
     * @param doc
     *            the XML document which is going to contain the created element
     * @return the element corresponding to the sentence in the XML document
     */
    public Element exportSentence(Utterance utt, int sent_index, Document doc) throws MaryException {
        Sentence sentence = ((Sequence<Sentence>) utt.getSequence(SupportedSequenceType.SENTENCE)).get(sent_index);
        Element sent_element = doc.createElementNS(NAMESPACE, "s");

        // Export node value
        Node text = doc.createTextNode(sentence.getText());
        sent_element.appendChild(text);

        try {
    Relation rel_sent_phrase = utt.getRelation(SupportedSequenceType.SENTENCE,
                                                       SupportedSequenceType.PHRASE);
            int[] phrases = rel_sent_phrase.getRelatedIndexes(sent_index);
            for (int i = 0; i < phrases.length; i++) {
    sent_element.appendChild(exportPhrase(utt, phrases[i], doc));
            }

        } catch(MaryException ex) {
    // FIXME: Export subelements
    Relation rel_sent_word = utt.getRelation(SupportedSequenceType.SENTENCE,
                                                     SupportedSequenceType.WORD);
            if (rel_sent_word != null) {
    int[] words = rel_sent_word.getRelatedIndexes(sent_index);
                for (int i = 0; i < words.length; i++) {
    sent_element.appendChild(exportWord(utt, words[i], doc));
                }
            }
        }

        return sent_element;
    }

    /**
     * Call back to export a phrase into an XML Element
     *
     * @param utt
     *            the utterance which contains the phrase sequence
     * @param phrase_index
     *            the index of the phrase
     * @param doc
     *            the XML document which is going to contain the created element
     * @return the element corresponding to the phrase in the XML document
     */
    public Element exportPhrase(Utterance utt, int phrase_index, Document doc) throws MaryException {
        Element phrase_element = doc.createElementNS(NAMESPACE, "phrase");

        logger.debug("Serializing phrase");

        Relation rel_phrase_word = utt.getRelation(SupportedSequenceType.PHRASE,
                                                   SupportedSequenceType.WORD);
        if (rel_phrase_word != null) {
    int[] words = rel_phrase_word.getRelatedIndexes(phrase_index);
            for (int i = 0; i < words.length; i++) {
    phrase_element.appendChild(exportWord(utt, words[i], doc));
            }
        }

        Element prosody_element = doc.createElementNS(NAMESPACE, "prosody");
        prosody_element.appendChild(phrase_element);
        return prosody_element;
    }

    /**
     * Call back to export a word into an XML Element
     *
     * @param utt
     *            the utterance which contains the word sequence
     * @param w_index
     *            the index of the word
     * @param doc
     *            the XML document which is going to contain the created element
     * @return the element corresponding to the word in the XML document
     */
    public Element exportWord(Utterance utt, int w_index, Document doc) throws MaryException {
        Word word = ((Sequence<Word>) utt.getSequence(SupportedSequenceType.WORD)).get(w_index);
        Element word_element = doc.createElementNS(NAMESPACE, "t");

        logger.debug("Serializing word \"" + word.getText() + "\"");

        // Export node value
        Node text = doc.createTextNode(word.getText());
        word_element.appendChild(text);

        if (word.getPOS() != null) {
    word_element.setAttribute("pos", word.getPOS());
        }

        if (word.getAccent() != null) {
    word_element.setAttribute("accent", word.getAccent().getLabel());
        }

        if (word.soundsLike() != null) {
    word_element.setAttribute("sounds_like", word.soundsLike());
        }

        if (word.getG2PMethod() != null) {
    word_element.setAttribute("g2p_method", word.getG2PMethod());
        }

        Relation rel_word_syllable = null;
        try {
    rel_word_syllable = utt.getRelation(SupportedSequenceType.WORD, SupportedSequenceType.SYLLABLE);
        } catch (MaryException ex) {
        }

        if (rel_word_syllable != null) {
    int[] syls = rel_word_syllable.getRelatedIndexes(w_index);
            for (int i = 0; i < syls.length; i++) {
    word_element.appendChild(exportSyllable(utt, syls[i], doc));
            }
        }

        return word_element;
    }

    /**
     * Call back to export a syllable into an XML Element
     *
     * @param utt
     *            the utterance which contains the syllable sequence
     * @param syl_index
     *            the index of the syllable
     * @param doc
     *            the XML document which is going to contain the created element
     * @return the element corresponding to the syllable in the XML document
     */
    public Element exportSyllable(Utterance utt, int syl_index, Document doc) throws MaryException {
        Syllable syl = ((Sequence<Syllable>) utt.getSequence(SupportedSequenceType.SYLLABLE)).get(
 syl_index);
        Element syllable_element = doc.createElementNS(NAMESPACE, "syllable");

        logger.debug("Serializing syllable");

        if (syl.getTone() != null) {
    syllable_element.setAttribute("tone", syl.getTone().getLabel());
        }

        if (syl.getAccent() != null) {
    syllable_element.setAttribute("accent", syl.getAccent().getLabel());
        }

        syllable_element.setAttribute("stress", Integer.toString(syl.getStressLevel()));

        Relation rel_syllable_phone = utt.getRelation(SupportedSequenceType.SYLLABLE,
                                                      SupportedSequenceType.PHONE);
        if (rel_syllable_phone != null) {
    int[] indexes = rel_syllable_phone.getRelatedIndexes(syl_index);
            for (int i = 0; i < indexes.length; i++) {
    syllable_element.appendChild(exportPhone(utt, indexes[i], doc));
            }
        }
        return syllable_element;
    }

    /**
     * Call back to export a phone into an XML Element
     *
     * @param utt
     *            the utterance which contains the phone sequence
     * @param ph_index
     *            the index of the phone
     * @param doc
     *            the XML document which is going to contain the created element
     * @return the element corresponding to the phone in the XML document
     */
    public Element exportPhone(Utterance utt, int ph_index, Document doc)  throws MaryException {
        Phoneme ph = ((Sequence<Phoneme>) utt.getSequence(SupportedSequenceType.PHONE)).get(ph_index);
        Element phone_element = doc.createElementNS(NAMESPACE, "ph");

        phone_element.setAttribute("p", ph.getLabel());


        if (utt.hasSequence(SupportedSequenceType.SEGMENT)) {
    Relation rel = utt.getRelation(SupportedSequenceType.PHONE, SupportedSequenceType.SEGMENT);
            if (rel != null) {
    Sequence<Segment> seq_seg = (Sequence<Segment>) utt.getSequence(SupportedSequenceType.SEGMENT);
                int[] indexes = rel.getRelatedIndexes(ph_index);
                if (indexes.length > 0 ) {
    double dur = 0;
                    for (int i = 0; i < indexes.length; i++) {
    dur += seq_seg.get(indexes[i]).getDuration();
                    }
                    phone_element.setAttribute("start", String.valueOf(seq_seg.get(indexes[0]).getStart()));
                    phone_element.setAttribute("d", String.valueOf(dur));
                }
            }
        }

        if (utt.hasSequence(SupportedSequenceType.FEATURES)) {
    Relation rel = utt.getRelation(SupportedSequenceType.PHONE, SupportedSequenceType.FEATURES);
            if (rel != null) {
    int[] indexes = rel.getRelatedIndexes(ph_index);
                for (int i = 0; i < indexes.length; i++) {
    phone_element.appendChild(exportFeatures(utt, indexes[i], doc));
                }
            }
        }

        return phone_element;
    }

    /**
     * Call back to export a feature map into an XML Element
     *
     * @param utt
     *            the utterance which contains the feature map sequence
     * @param feat_index
     *            the index of the feature map
     * @param doc
     *            the XML document which is going to contain the created element
     * @return the element corresponding to the feature map in the XML document
     */
    public Element exportFeatures(Utterance utt, int feat_index, Document doc) {
        Element features_element = doc.createElementNS(NAMESPACE, "features");
        FeatureMap features = ((Sequence<FeatureMap>) utt.getSequence(SupportedSequenceType.FEATURES))
    .get(feat_index);

        for (Map.Entry<String, Feature> entry : features.getMap().entrySet()) {
    if ((entry.getValue() != null) && (!entry.getValue().getStringValue().equals(""))) {
    Element feature_element = doc.createElementNS(NAMESPACE, "feature");
                feature_element.setAttribute("name", entry.getKey());
                feature_element.setAttribute("value", entry.getValue().getStringValue());
                feature_element.setAttribute("value_class", entry.getValue().getValue().getClass().getName());
                features_element.appendChild(feature_element);
            }
        }

        return features_element;
    }

    /************************************************************************************************
     * Extract utterance from document
     ***********************************************************************************************/
    /**
     * Generate an utterance from a document in a string format. This method is
     * used to deal with more Exceptions than the import one.
     *
     * @param doc_str
     *            the document in string format
     * @return the created utterance
     * @throws ParserConfigurationException
     *             If something wrong happens in the XML parsing
     * @throws SAXException
     *             If something wrong happens in the XML parsing
     * @throws IOException
     *             If something wrong happens in the XML parsing
     * @throws MaryIOException
     *             if something wrong happened
     */
    public Utterance unpackDocument(String doc_str)
        throws ParserConfigurationException, SAXException, IOException, MaryIOException {
        // 1. generate the doc from the string
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(true);
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(new MaryEntityResolver());

        doc_str = StringUtils.purgeNonBreakingSpaces(doc_str);
        Document doc = builder.parse(new InputSource(new StringReader(doc_str)));

        // 2. Generate the utterance from the document
        return unpackDocument(doc);
    }

    /**
     * Generate the utterance from an XML document
     *
     * @param doc
     *            the XML document
     * @return the created utterance
     * @throws MaryIOException
     *             if something wrong happened
     */
    public Utterance unpackDocument(Document doc) throws MaryIOException {
        Hashtable<SequenceTypePair, ArrayList<IntegerPair>> alignments;
        Locale l;
        alignments = new Hashtable<SequenceTypePair, ArrayList<IntegerPair>>();

        Element root = doc.getDocumentElement();
        String[] loc = root.getAttribute("xml:lang").split("-");
        if (loc.length > 1) {
    l = new Locale.Builder().setLanguage(loc[0]).setRegion(loc[1]).build();
        } else {
    l = new Locale.Builder().setLanguage(loc[0]).build();
        }

        Utterance utt = new Utterance();

        // 1. going through everything and save the alignments
        NodeList elts = root.getElementsByTagName("p");
        String text = "";
        for (int i = 0; i < elts.getLength(); i++) {
    Element p = (Element) elts.item(i);
            NodeList nl = p.getChildNodes();
            boolean found_text = false;
            int j = 0;
            while ((!found_text) && (j < nl.getLength())) {
    Node node = nl.item(j);

                if (node.getNodeType() == Node.TEXT_NODE) {
    if (!node.getNodeValue().trim().equals("")) {
    text += node.getNodeValue().trim() + "\n"; // FIXME: new
                        // line
                        // directly
                        // encoded :
                        found_text = true;
                    }
                }
                j++;
            }

            if (!found_text) {
    throw new MaryIOException("Cannot find the text of the paragraph");
            }

            generateParagraph(p, utt, alignments);
        }


        // 2. Dealing relations
        for (SequenceTypePair k : alignments.keySet()) {
    logger.debug("source: " + utt.getSequence(k.getLeft()));
            logger.debug("target: " + utt.getSequence(k.getRight()));
            Relation rel = new Relation(utt.getSequence(k.getLeft()), utt.getSequence(k.getRight()),
                                        alignments.get(k));

            utt.setRelation(k.getLeft(), k.getRight(), rel);
        }

        return utt;
    }

    /**
     * Extract paragraph from the given element, update the sequence in the
     * utterance and the alignments. The relation are not generated in this
     * method, only the alignments are updated.
     *
     * @param elt
     *            the element representing the paragraph
     * @param utt
     *            the target utterance
     * @param alignments
     *            the saved alignments
     * @throws MaryIOException
     *             if something wrong happened
     */
    public void generateParagraph(Element elt, Utterance utt,
                                  Hashtable<SequenceTypePair, ArrayList<IntegerPair>> alignments) throws MaryIOException {
        assert elt.getTagName() == "p";

        // Retrieve the sentence offset
        int sentence_offset = 0;
        if (utt.getSequence(SupportedSequenceType.SENTENCE) != null) {
    sentence_offset = utt.getSequence(SupportedSequenceType.SENTENCE).size();
        }

        NodeList nl = elt.getChildNodes();
        String text = null;
        logger.debug("Current paragraph contains " + nl.getLength() + " childs");
        for (int j = 0; j < nl.getLength(); j++) {
    Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE) {
    logger.debug("Unpack the text");

                if (!node.getNodeValue().trim().equals("")) {
    text = node.getNodeValue().trim();
                }
            } else if (node.getNodeType() == Node.ELEMENT_NODE) {
    logger.debug("Unpack the sentence");
                Element cur_elt = (Element) node;
                if (cur_elt.getTagName() == "s") {
    generateSentence(cur_elt, utt, alignments);
                } else if (cur_elt.getTagName() == "voice") {
    throw new MaryIOException("I do not now what to do with voice tag !");
                }
            } else {
    throw new MaryIOException("Unknown node element type during unpacking: " + node.getNodeType(),
                                          null);
            }
        }

        if (text == null) {
    throw new MaryIOException("Cannot find the text of the paragraph");
        }

        // Create/modify the sequence by adding the paragraph
        Paragraph par = new Paragraph(text);
        Sequence<Paragraph> seq_par = (Sequence<Paragraph>) utt.getSequence(
 SupportedSequenceType.PARAGRAPH);
        if (seq_par == null) {
    seq_par = new Sequence<Paragraph>();
        }
        seq_par.add(par);
        utt.addSequence(SupportedSequenceType.PARAGRAPH, seq_par);

        // No sentence => no alignments !
        int size_sentence = utt.getSequence(SupportedSequenceType.SENTENCE).size();
        if (size_sentence == sentence_offset) {
    return;
        }

        if (!alignments
            .containsKey(new SequenceTypePair(SupportedSequenceType.PARAGRAPH,
                                              SupportedSequenceType.SENTENCE))) {
    alignments.put(new SequenceTypePair(SupportedSequenceType.PARAGRAPH,
                                                SupportedSequenceType.SENTENCE),
                           new ArrayList<IntegerPair>());
        }

        ArrayList<IntegerPair> alignment_paragraph_sentence = alignments
    .get(new SequenceTypePair(SupportedSequenceType.PARAGRAPH, SupportedSequenceType.SENTENCE));
        int id_par = seq_par.size() - 1;

        // Deal with Relation sentences part
        for (int i = sentence_offset; i < size_sentence; i++) {
    alignment_paragraph_sentence.add(new IntegerPair(id_par, i));
        }
    }

    /**
     * Extract sentence from the given element, update the sequence in the
     * utterance and the alignments. The relation are not generated in this
     * method, only the alignments are updated.
     *
     * @param elt
     *            the element representing the sentence
     * @param utt
     *            the target utterance
     * @param alignments
     *            the saved alignments
     * @throws MaryIOException
     *             if something wrong happened
     */
    public void generateSentence(Element elt, Utterance utt,
                                 Hashtable<SequenceTypePair, ArrayList<IntegerPair>> alignments) throws MaryIOException {
        assert elt.getTagName() == "s";

        int phrase_offset = 0;
        if (utt.getSequence(SupportedSequenceType.PHRASE) != null) {
    phrase_offset = utt.getSequence(SupportedSequenceType.PHRASE).size();
        }

        int word_offset = 0;
        if (utt.getSequence(SupportedSequenceType.WORD) != null) {
    word_offset = utt.getSequence(SupportedSequenceType.WORD).size();
        }

        NodeList nl = elt.getChildNodes();
        String text = null;
        int status_loading = 0; // 0 = none yet, 1 = word found, 2 = phrase
        // found
        for (int j = 0; j < nl.getLength(); j++) {
    Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE) {
    if (!node.getNodeValue().trim().equals("")) {
    text = node.getNodeValue().trim();
                }
            } else if (node.getNodeType() == Node.ELEMENT_NODE) {
    Element cur_elt = (Element) node;
                if (cur_elt.getTagName() == "t") {
    if (status_loading == 2) {
    throw new MaryIOException("Cannot unserialize a word isolated from a phrase");
                    }
                    generateWord(cur_elt, utt, alignments);
                    status_loading = 1;
                } else if (cur_elt.getTagName() == "mtu") {
    if (status_loading == 2) {
    throw new MaryIOException("Cannot unserialize a word isolated from a phrase");
                    }
                    NodeList mtu_nl = cur_elt.getChildNodes();

                    for (int k = 0; k < mtu_nl.getLength(); k++) {
    Node word_node = mtu_nl.item(k);
                        if (word_node.getNodeType() == Node.ELEMENT_NODE) {
    generateWord((Element) word_node, utt, alignments);
                        } else {
    throw new MaryIOException(
 "Unknown node element type during unpacking the mtu: " + node.getNodeType());
                        }
                    }

                    status_loading = 1;
                } else if (cur_elt.getTagName() == "prosody") {
    if (status_loading == 1) {
    throw new MaryIOException("Cannot unserialize a word isolated from a phrase");
                    }

                    // FIXME: assume that the first node is a phrase
                    NodeList cur_nl = cur_elt.getChildNodes();
                    for (int k = 0; k < cur_nl.getLength(); k++) {
    Node first_node = cur_nl.item(k);
                        if (first_node.getNodeType() == Node.ELEMENT_NODE) {
    generatePhrase((Element) first_node, utt, alignments);
                        }
                    }
                    status_loading = 2;
                } else if (cur_elt.getTagName() == "phrase") {
    if (status_loading == 1) {
    throw new MaryIOException("Cannot unserialize a word isolated from a phrase");
                    }

                    generatePhrase(cur_elt, utt, alignments);
                    status_loading = 2;
                } else {
    throw new MaryIOException("Unknown node element during unpacking: " + cur_elt.getTagName());
                }
            } else {
    throw new MaryIOException("Unknown node element type during unpacking: " + node.getNodeType(),
                                          null);
            }
        }

        // Create/modify the sequence by adding the sentence
        Sentence s = new Sentence(text);
        Sequence<Sentence> seq_sent = (Sequence<Sentence>) utt.getSequence(SupportedSequenceType.SENTENCE);
        if (seq_sent == null) {
    seq_sent = new Sequence<Sentence>();
        }
        seq_sent.add(s);
        utt.addSequence(SupportedSequenceType.SENTENCE, seq_sent);
        int id_sent = seq_sent.size() - 1;

        // Sentence/Phrase alignment
        int size_phrase = utt.getSequence(SupportedSequenceType.PHRASE).size();
        if (size_phrase > 0) {
    if (!alignments
                .containsKey(new SequenceTypePair(SupportedSequenceType.SENTENCE, SupportedSequenceType.PHRASE))) {
    alignments.put(new SequenceTypePair(SupportedSequenceType.SENTENCE, SupportedSequenceType.PHRASE),
                               new ArrayList<IntegerPair>());
            }

            ArrayList<IntegerPair> alignment_sentence_phrase = alignments
    .get(new SequenceTypePair(SupportedSequenceType.SENTENCE, SupportedSequenceType.PHRASE));

            for (int i = phrase_offset; i < size_phrase; i++) {
    alignment_sentence_phrase.add(new IntegerPair(id_sent, i));
            }
        }

        // Sentence/Word alignment
        int size_word = utt.getSequence(SupportedSequenceType.WORD).size();
        if (size_word > 0) {
    if (!alignments
                .containsKey(new SequenceTypePair(SupportedSequenceType.SENTENCE, SupportedSequenceType.WORD))) {
    alignments.put(new SequenceTypePair(SupportedSequenceType.SENTENCE, SupportedSequenceType.WORD),
                               new ArrayList<IntegerPair>());
            }

            ArrayList<IntegerPair> alignment_sentence_word = alignments
    .get(new SequenceTypePair(SupportedSequenceType.SENTENCE, SupportedSequenceType.WORD));

            for (int i = word_offset; i < size_word; i++) {
    alignment_sentence_word.add(new IntegerPair(id_sent, i));
            }
        }
    }

    /**
     * Extract phrase from the given element, update the sequence in the
     * utterance and the alignments. The relation are not generated in this
     * method, only the alignments are updated.
     *
     * @param elt
     *            the element representing the phrase
     * @param utt
     *            the target utterance
     * @param alignments
     *            the saved alignments
     * @throws MaryIOException
     *             if something wrong happened
     */
    public void generatePhrase(Element elt, Utterance utt,
                               Hashtable<SequenceTypePair, ArrayList<IntegerPair>> alignments) throws MaryIOException {
        assert elt.getTagName().equals("phrase");

        int word_offset = 0;
        if (utt.getSequence(SupportedSequenceType.WORD) != null) {
    word_offset = utt.getSequence(SupportedSequenceType.WORD).size();
        }
        Boundary boundary = null;

        NodeList nl = elt.getChildNodes();
        String text = null;
        for (int j = 0; j < nl.getLength(); j++) {
    Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE) {

    if (!node.getNodeValue().trim().equals("")) {
    text = node.getNodeValue().trim();
                }
            } else if (node.getNodeType() == Node.ELEMENT_NODE) {
    Element cur_elt = (Element) node;
                if (cur_elt.getTagName() == "t") {
    generateWord(cur_elt, utt, alignments);
                } else if (cur_elt.getTagName() == "mtu") {
    NodeList mtu_nl = cur_elt.getChildNodes();

                    for (int k = 0; k < mtu_nl.getLength(); k++) {
    Node word_node = mtu_nl.item(k);
                        if (word_node.getNodeType() == Node.ELEMENT_NODE) {

    generateWord((Element) word_node, utt, alignments);
                        } else {
    throw new MaryIOException(
 "Unknown node element type during unpacking the mtu: " + node.getNodeType());
                        }
                    }

                } else if (cur_elt.getTagName() == "boundary") {
    int breakindex = Integer.parseInt(cur_elt.getAttribute("breakindex"));
                    String tone = cur_elt.getAttribute("tone");
                    boundary = new Boundary(breakindex, tone);
                } else {
    throw new MaryIOException("Unknown node element during unpacking: " + cur_elt.getTagName());
                }
            } else {
    throw new MaryIOException("Unknown node element type during unpacking: " + node.getNodeType(),
                                          null);
            }
        }

        // Create the phrase and add the phrase to the utterance
        Phrase p = new Phrase(boundary);
        Sequence<Phrase> seq_phrase = (Sequence<Phrase>) utt.getSequence(SupportedSequenceType.PHRASE);
        if (seq_phrase == null) {
    seq_phrase = new Sequence<Phrase>();
        }
        utt.addSequence(SupportedSequenceType.PHRASE, seq_phrase);
        seq_phrase.add(p);

        // Phrase/Word alignment
        if (!alignments.containsKey(new SequenceTypePair(SupportedSequenceType.PHRASE,
                                                         SupportedSequenceType.WORD))) {
    alignments.put(new SequenceTypePair(SupportedSequenceType.PHRASE, SupportedSequenceType.WORD),
                           new ArrayList<IntegerPair>());
        }

        ArrayList<IntegerPair> alignment_phrase_word = alignments
    .get(new SequenceTypePair(SupportedSequenceType.PHRASE, SupportedSequenceType.WORD));

        int size_word = utt.getSequence(SupportedSequenceType.WORD).size();
        int id_phrase = seq_phrase.size() - 1;
        for (int i = word_offset; i < size_word; i++) {
    alignment_phrase_word.add(new IntegerPair(id_phrase, i));
        }
    }

    /**
     * Extract word from the given element, update the sequence in the utterance
     * and the alignments. The relation are not generated in this method, only
     * the alignments are updated.
     *
     * @param elt
     *            the element representing the word
     * @param utt
     *            the target utterance
     * @param alignments
     *            the saved alignments
     * @throws MaryIOException
     *             if something wrong happened
     */
    public void generateWord(Element elt, Utterance utt,
                             Hashtable<SequenceTypePair, ArrayList<IntegerPair>> alignments)
        throws MaryIOException {
        assert elt.getTagName().equals("t");
        ArrayList<Syllable> syllable_list = new ArrayList<Syllable>();

        int syllable_offset = 0;
        if (utt.getSequence(SupportedSequenceType.SYLLABLE) != null) {
    syllable_offset = utt.getSequence(SupportedSequenceType.SYLLABLE).size();
        }

        NodeList nl = elt.getChildNodes();
        String text = null;
        for (int j = 0; j < nl.getLength(); j++) {
    Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE) {
    if (!node.getNodeValue().trim().equals("")) {
    text = node.getNodeValue().trim();
                }

            } else if (node.getNodeType() == Node.ELEMENT_NODE) {
    Element syllable_elt = (Element) node;
                generateSyllable(syllable_elt, utt, alignments);
            } else {
    throw new MaryIOException("Unknown node element type during unpacking: " + node.getNodeType(),
                                          null);
            }
        }

        if (text == null) {
    throw new MaryIOException("Cannot find the text of the word");
        }

        logger.debug("Unpacking word \"" + text + "\"");
        Word w = new Word(text);
        if (elt.hasAttribute("pos")) {
    String pos = elt.getAttribute("pos");
            w.setPOS(pos);
        }

        if (elt.hasAttribute("accent")) {
    String accent = elt.getAttribute("accent");
            w.setAccent(new Accent(accent));
        }

        // Create the phrase and add the phrase to the
        Sequence<Word> seq_word = (Sequence<Word>) utt.getSequence(SupportedSequenceType.WORD);
        if (seq_word == null) {
    seq_word = new Sequence<Word>();
        }
        utt.addSequence(SupportedSequenceType.WORD, seq_word);
        seq_word.add(w);

        // Word/Syllable alignment
        if ((utt.getSequence(SupportedSequenceType.SYLLABLE) != null)
            && (utt.getSequence(SupportedSequenceType.SYLLABLE).size() > 0)) {
    if (!alignments
                .containsKey(new SequenceTypePair(SupportedSequenceType.WORD, SupportedSequenceType.SYLLABLE))) {
    alignments.put(new SequenceTypePair(SupportedSequenceType.WORD, SupportedSequenceType.SYLLABLE),
                               new ArrayList<IntegerPair>());
            }

            ArrayList<IntegerPair> alignment_word_syllable = alignments
    .get(new SequenceTypePair(SupportedSequenceType.WORD, SupportedSequenceType.SYLLABLE));

            int size_syllable = utt.getSequence(SupportedSequenceType.SYLLABLE).size();
            int id_word = seq_word.size() - 1;
            for (int i = syllable_offset; i < size_syllable; i++) {
    alignment_word_syllable.add(new IntegerPair(id_word, i));
            }
        }
    }

    /**
     * Extract syllable from the given element, update the sequence in the
     * utterance and the alignments. The relation are not generated in this
     * method, only the alignments are updated.
     *
     * @param elt
     *            the element representing the syllable
     * @param utt
     *            the target utterance
     * @param alignments
     *            the saved alignments
     * @throws MaryIOException
     *             if something wrong happened
     */
    public void generateSyllable(Element elt, Utterance utt,
                                 Hashtable<SequenceTypePair, ArrayList<IntegerPair>> alignments) throws MaryIOException {
        assert elt.getTagName() == "syllable";
        ArrayList<Phoneme> phoneme_list = new ArrayList<Phoneme>();

        int phone_offset = 0;
        if (utt.getSequence(SupportedSequenceType.PHONE) != null) {
    phone_offset = utt.getSequence(SupportedSequenceType.PHONE).size();
        }

        NodeList nl = elt.getChildNodes();
        String text = null;
        for (int j = 0; j < nl.getLength(); j++) {
    Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE) {
    if (!node.getNodeValue().trim().equals("")) {
    text = node.getNodeValue().trim();
                }
            } else if (node.getNodeType() == Node.ELEMENT_NODE) {
    Element phoneme_elt = (Element) node;
                generatePhone(phoneme_elt, utt, alignments);
            } else {
    throw new MaryIOException("Unknown node element type during unpacking: " + node.getNodeType(),
                                          null);
            }
        }

        // FIXME: for now the tone phoneme is just based on the label...
        Phoneme tone = null;
        if (elt.hasAttribute("tone")) {
    tone = new Phoneme(elt.getAttribute("tone"));
        }

        Accent accent = null;
        if (elt.hasAttribute("accent")) {
    accent = new Accent(elt.getAttribute("accent"));
        }

        int stress_level = 0;
        if (elt.hasAttribute("stress")) {
    stress_level = Integer.parseInt(elt.getAttribute("stress"));
        }
        Syllable syl = new Syllable(tone, stress_level, accent);

        // Create the phrase and add the phrase to the
        Sequence<Syllable> seq_syllable = (Sequence<Syllable>) utt.getSequence(
 SupportedSequenceType.SYLLABLE);
        if (seq_syllable == null) {
    seq_syllable = new Sequence<Syllable>();
        }
        utt.addSequence(SupportedSequenceType.SYLLABLE, seq_syllable);
        seq_syllable.add(syl);

        // Syllable/Phone alignment
        if ((utt.getSequence(SupportedSequenceType.PHONE) != null)
            && (utt.getSequence(SupportedSequenceType.PHONE).size() > 0)) {
    if (!alignments
                .containsKey(new SequenceTypePair(SupportedSequenceType.SYLLABLE, SupportedSequenceType.PHONE))) {
    alignments.put(new SequenceTypePair(SupportedSequenceType.SYLLABLE, SupportedSequenceType.PHONE),
                               new ArrayList<IntegerPair>());
            }

            ArrayList<IntegerPair> alignment_syllable_phone = alignments
    .get(new SequenceTypePair(SupportedSequenceType.SYLLABLE, SupportedSequenceType.PHONE));

            int size_phone = utt.getSequence(SupportedSequenceType.PHONE).size();
            int id_syllable = seq_syllable.size() - 1;
            for (int i = phone_offset; i < size_phone; i++) {
    alignment_syllable_phone.add(new IntegerPair(id_syllable, i));
            }
        }
    }

    /**
     * Extract phone from the given element, update the sequence in the
     * utterance.
     *
     * @param elt
     *            the element representing the phone
     * @param utt
     *            the target utterance
     */
    public void generatePhone(Element elt, Utterance utt,
                              Hashtable<SequenceTypePair, ArrayList<IntegerPair>> alignments) throws MaryIOException {
        assert elt.getTagName() == "ph";


        int features_offset = 0;
        if (utt.getSequence(SupportedSequenceType.FEATURES) != null) {
            features_offset = utt.getSequence(SupportedSequenceType.FEATURES).size();
        }

        // Create the phone and add the phone to the utterance
	Phoneme ph = new Phoneme(elt.getAttribute("p"));
        Sequence<Phoneme> seq_phone = (Sequence<Phoneme>) utt.getSequence(SupportedSequenceType.PHONE);
        if (seq_phone == null) {
            seq_phone = new Sequence<Phoneme>();
        }
        utt.addSequence(SupportedSequenceType.PHONE, seq_phone);
        seq_phone.add(ph);
	if (elt.hasAttribute("start")) {
	    float start = Float.parseFloat(elt.getAttribute("start"));
	    float dur = -1.0f;
	    if (elt.hasAttribute("d")) {
		dur = Float.parseFloat(elt.getAttribute("d"));
	    }

	    if (dur < 0) {
		if (elt.hasAttribute("end")) {
		    dur = Float.parseFloat(elt.getAttribute("end")) - start;
		} else {
		    throw new MaryIOException("no duration information (d/end attribute)");
		}
	    }

	    Segment seg = new Segment(start, dur);

            Sequence<Segment> seq_segment = (Sequence<Segment>) utt.getSequence(SupportedSequenceType.SEGMENT);
            if (seq_segment == null) {
                seq_segment = new Sequence<Segment>();
            }
            utt.addSequence(SupportedSequenceType.SEGMENT, seq_segment);
            seq_segment.add(seg);
	}



        NodeList nl = elt.getChildNodes();
        String text = null;
        for (int j = 0; j < nl.getLength(); j++) {
            Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE) {
                continue;
            } else if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element features_elt = (Element) node;
                if (features_elt.getTagName().equals("features")) {
                    generateFeatures(features_elt, utt);
                } else {
                    throw new MaryIOException("node with tag \"" + features_elt.getTagName() + "\" should not be linked to a phone(me)");
                }
            } else {
                throw new MaryIOException("Unknown node element type during unpacking: " + node.getNodeType(),
                                          null);
            }
        }


        if (!alignments.containsKey(new SequenceTypePair(SupportedSequenceType.SEGMENT, SupportedSequenceType.PHONE))) {
            alignments.put(new SequenceTypePair(SupportedSequenceType.SEGMENT, SupportedSequenceType.PHONE),
                           new ArrayList<IntegerPair>());
        }


        Sequence<Segment> seq_segment = (Sequence<Segment>) utt.getSequence(SupportedSequenceType.SEGMENT);
        ArrayList<IntegerPair> alignment_segment_phone = alignments
            .get(new SequenceTypePair(SupportedSequenceType.SEGMENT, SupportedSequenceType.PHONE));
        int id_segment = seq_segment.size() - 1;
        alignment_segment_phone.add(new IntegerPair(id_segment, id_segment));

        // Syllable/segment Alignment
        if ((utt.getSequence(SupportedSequenceType.FEATURES) != null)
             && (utt.getSequence(SupportedSequenceType.FEATURES).size() > 0)) {

            if (!alignments.containsKey(new SequenceTypePair(SupportedSequenceType.SEGMENT, SupportedSequenceType.FEATURES))) {
                alignments.put(new SequenceTypePair(SupportedSequenceType.SEGMENT, SupportedSequenceType.FEATURES),
                               new ArrayList<IntegerPair>());
            }

            ArrayList<IntegerPair> alignment_segment_features = alignments
                .get(new SequenceTypePair(SupportedSequenceType.SEGMENT, SupportedSequenceType.FEATURES));

            int size_features = utt.getSequence(SupportedSequenceType.FEATURES).size();
            for (int i = features_offset; i < size_features; i++) {
                alignment_segment_features.add(new IntegerPair(id_segment, i));
            }
        }
    }



    public void generateFeatures(Element elt, Utterance utt) throws MaryIOException {

        NodeList nl = elt.getChildNodes();
        String text = null;

        FeatureMap feature_map = new FeatureMap();

        for (int j = 0; j < nl.getLength(); j++) {
    Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE) {
            } else if (node.getNodeType() == Node.ELEMENT_NODE) {
    Element cur_feat_elt = (Element) node;

                if (cur_feat_elt.getTagName().equals("feature")) {
    // FIXME: how to inject value type knowledge ?! (if the type is an integer, we want to extract an integer from the string)
    String value = cur_feat_elt.getAttribute("value");
                    feature_map.put(cur_feat_elt.getAttribute("name"),
                                    new Feature(value));
                } else {
    throw new MaryIOException("node with tag \"" + cur_feat_elt.getTagName() + "\" should not be part of the feature map");
                }
            } else {
    throw new MaryIOException("Unknown node element type during unpacking: " + node.getNodeType(),
                                          null);
            }
        }


        Sequence<FeatureMap> seq_features = (Sequence<FeatureMap>) utt.getSequence(SupportedSequenceType.FEATURES);
        if (seq_features == null) {
    seq_features = new Sequence<FeatureMap>();
        }
        seq_features.add(feature_map);
        utt.addSequence(SupportedSequenceType.FEATURES, seq_features);
    }
}
