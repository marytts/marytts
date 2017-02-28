package marytts.io;

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

import marytts.util.dom.MaryEntityResolver;

import marytts.data.utils.IntegerPair;
import marytts.data.utils.SequenceTypePair;

import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;
import marytts.data.item.linguistic.*;
import marytts.data.item.phonology.*;
import marytts.data.item.prosody.*;
import marytts.data.item.*;

import marytts.features.FeatureMap;
import marytts.features.Feature;


import marytts.util.MaryUtils;
import marytts.util.string.StringUtils;
import org.apache.log4j.Logger;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class XMLSerializer implements Serializer
{
	private static final String NAMESPACE = "http://mary.dfki.de/2002/MaryXML";

    protected Logger logger;

    public XMLSerializer()
    {
		logger = MaryUtils.getLogger("XMLSerializer");
    }

    public Utterance load(File file)
        throws MaryIOException
    {
        return new Utterance("", Locale.ENGLISH); // TODO: makes compiler happy for now
    }

    public void save(File file, Utterance utt)
        throws MaryIOException
    {
    }

    public String toString(Utterance utt)
        throws MaryIOException
    {
        try
        {
            Document doc = generateDocument(utt);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            //initialize StreamResult with File object to save to file
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);
            return result.getWriter().toString();
        }
        catch (TransformerConfigurationException ex)
        {
            throw new MaryIOException("Transformer configuration exception", ex);
        }
        catch (TransformerException ex)
        {
            throw new MaryIOException("Transformer exception", ex);
        }
    }

    public Utterance fromString(String doc_str)
        throws MaryIOException
    {
        try
        {
            return unpackDocument(doc_str);
        }
        catch (Exception ex)
        {
            throw new MaryIOException("couldn't convert the document from string", ex);
        }
    }
    public Utterance unpackDocument(String doc_str)
        throws ParserConfigurationException, SAXException, IOException, MaryIOException
    {
        // 1. generate the doc from the string
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setExpandEntityReferences(true);
		factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
		builder.setEntityResolver(new MaryEntityResolver());

        doc_str = StringUtils.purgeNonBreakingSpaces(doc_str);
        Document doc =  builder.parse(new InputSource(new StringReader(doc_str)));

        // 2. Generate the utterance from the document
        return unpackDocument(doc);
    }



    public Document generateDocument(Utterance utt)
        throws MaryIOException
    {
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
            rootElement.setAttribute("xml:lang", MaryUtils.locale2xmllang(utt.getLocale()));

            // Adding paragraphs
            int nb_par = utt.getSequence(SupportedSequenceType.PARAGRAPH).size();
            for (int i=0; i<nb_par; i++)
            {
                rootElement.appendChild(exportParagraph(utt, i, doc));
            }

            // Finalise and returns the doc
            doc.appendChild(rootElement);
            return doc;
        }
        catch (ParserConfigurationException ex)
        {
            throw new MaryIOException("Parsing exception", ex);
        }
    }

    /************************************************************************************************
     * Element generation part
     ***********************************************************************************************/
    public Element exportParagraph(Utterance utt, int par_index, Document doc)
    {
        // FIXME: to remove
        Element par_element = doc.createElementNS(NAMESPACE, "p");

        // Export node value
        Paragraph paragraph = ((Sequence<Paragraph>) utt.getSequence(SupportedSequenceType.PARAGRAPH)).get(par_index);
        Node text = doc.createTextNode(paragraph.getText());
        par_element.appendChild(text);

        // Export subelements
        Relation rel_par_sent = utt.getRelation(SupportedSequenceType.PARAGRAPH, SupportedSequenceType.SENTENCE);

        // FIXME: needs to have a "containsRelation" method
        if (rel_par_sent != null)
        {
            int[] sentences = rel_par_sent.getRelatedIndexes(par_index);
            for (int i=0; i<sentences.length; i++)
                par_element.appendChild(exportSentence(utt, sentences[i], doc));
        }

        return par_element;
    }

    public Element exportSentence(Utterance utt, int sent_index, Document doc)
    {
        Sentence sentence = ((Sequence<Sentence>) utt.getSequence(SupportedSequenceType.SENTENCE)).get(sent_index);

        Element sent_element = doc.createElementNS(NAMESPACE, "s");

        // Export node value
        Node text = doc.createTextNode(sentence.getText());
        sent_element.appendChild(text);

        Relation rel_sent_phrase = utt.getRelation(SupportedSequenceType.SENTENCE, SupportedSequenceType.PHRASE);
        if (rel_sent_phrase != null)
        {
            int[] phrases = rel_sent_phrase.getRelatedIndexes(sent_index);
            for (int i=0; i<phrases.length; i++)
                sent_element.appendChild(exportPhrase(utt, phrases[i], doc));
        }
        else
        {
            // FIXME: Export subelements
            Relation rel_sent_word = utt.getRelation(SupportedSequenceType.SENTENCE, SupportedSequenceType.WORD);
            if (rel_sent_word != null)
            {
                int[] words = rel_sent_word.getRelatedIndexes(sent_index);
                for (int i=0; i<words.length; i++)
                    sent_element.appendChild(exportWord(utt, words[i], doc));
            }
        }

        return sent_element;
    }

    public Element exportPhrase(Utterance utt, int phrase_index, Document doc)
    {
        Element phrase_element = doc.createElementNS(NAMESPACE, "phrase");

        logger.info("Serializing phrase");


        Relation rel_phrase_word = utt.getRelation(SupportedSequenceType.PHRASE, SupportedSequenceType.WORD);
        if (rel_phrase_word != null)
        {
            int[] words = rel_phrase_word.getRelatedIndexes(phrase_index);
            for (int i=0; i<words.length; i++)
                phrase_element.appendChild(exportWord(utt, words[i], doc));
        }

        Element prosody_element = doc.createElementNS(NAMESPACE, "prosody");
        prosody_element.appendChild(phrase_element);
        return prosody_element;
    }

    public Element exportWord(Utterance utt, int w_index, Document doc)
    {
        Word word = ((Sequence<Word>) utt.getSequence(SupportedSequenceType.WORD)).get(w_index);
        Element word_element = doc.createElementNS(NAMESPACE, "t");

        logger.info("Serializing word \"" + word.getText() + "\"");

        // Export node value
        Node text = doc.createTextNode(word.getText());
        word_element.appendChild(text);

        if (word.getPOS() != null)
            word_element.setAttribute("pos", word.getPOS());

        if (word.getAccent() != null)
            word_element.setAttribute("accent", word.getAccent().getLabel());

        if (word.soundsLike() != null)
            word_element.setAttribute("sounds_like", word.soundsLike());

        ArrayList<Phoneme> phonemes = word.getPhonemes();
        if (phonemes.size() > 0)
        {
            String phonemes_str = "";
            for (int i=0; i<phonemes.size()-1; i++)
                phonemes_str += phonemes.get(i).getLabel() + " - ";
            phonemes_str += phonemes.get(phonemes.size() - 1).getLabel();
            word_element.setAttribute("ph", phonemes_str);
        }

        Relation rel_word_syllable = utt.getRelation(SupportedSequenceType.WORD,
                                                     SupportedSequenceType.SYLLABLE);
        if (rel_word_syllable != null)
        {
            int[] syls = rel_word_syllable.getRelatedIndexes(w_index);
            for (int i=0; i<syls.length; i++)
                word_element.appendChild(exportSyllable(utt, syls[i], doc));
        }

        return word_element;
    }

    public Element exportSyllable(Utterance utt, int syl_index, Document doc)
    {
        Syllable syl = ((Sequence<Syllable>) utt.getSequence(SupportedSequenceType.SYLLABLE)).get(syl_index);
        Element syllable_element = doc.createElementNS(NAMESPACE, "syllable");

        logger.info("Serializing syllable");

        if (syl.getTone() != null)
            syllable_element.setAttribute("tone", syl.getTone().getLabel());

        if (syl.getAccent() != null)
            syllable_element.setAttribute("accent", syl.getAccent().getLabel());

        syllable_element.setAttribute("stress", Integer.toString(syl.getStressLevel()));


        Relation rel_syllable_phone = utt.getRelation(SupportedSequenceType.SYLLABLE,
                                                      SupportedSequenceType.PHONE);
        if (rel_syllable_phone != null)
        {
            int[] indexes = rel_syllable_phone.getRelatedIndexes(syl_index);
            for (int i=0; i<indexes.length; i++)
                syllable_element.appendChild(exportPhone(utt, indexes[i], doc));
        }
        return syllable_element;
    }

    public Element exportPhone(Utterance utt , int ph_index, Document doc)
    {
        Phoneme ph = ((Sequence<Phoneme>) utt.getSequence(SupportedSequenceType.PHONE)).get(ph_index);
        Element phone_element = doc.createElementNS(NAMESPACE, "ph");

        phone_element.setAttribute("p", ph.getLabel());

        if (ph instanceof Phone)
        {
            phone_element.setAttribute("start", String.valueOf(((Phone) ph).getStart()));
            phone_element.setAttribute("d", String.valueOf(((Phone) ph).getDuration()));
        }

        if (utt.hasSequence(SupportedSequenceType.F0))
        {
            Relation rel = utt.getRelation(SupportedSequenceType.PHONE, SupportedSequenceType.F0);
            if (rel != null)
            {
                ArrayList<Item> f0s = (ArrayList<Item>) rel.getRelatedItems(ph_index);
                if (f0s.size() > 0)
                    phone_element.setAttribute("d", f0s.get(0).toString()); // FIXME: only first is taken into account
            }
        }


        if (utt.hasSequence(SupportedSequenceType.FEATURES))
        {
            Relation rel = utt.getRelation(SupportedSequenceType.PHONE, SupportedSequenceType.FEATURES);
            if (rel != null)
            {
                int[] indexes = rel.getRelatedIndexes(ph_index);
                for (int i=0; i<indexes.length; i++)
                {
                    phone_element.appendChild(exportFeatures(utt, indexes[i], doc));
                }
            }
        }

        return phone_element;
    }

    public Element exportFeatures(Utterance utt, int feat_index, Document doc)
    {
        Element features_element = doc.createElementNS(NAMESPACE, "features");
        FeatureMap features = ((Sequence<FeatureMap>) utt.getSequence(SupportedSequenceType.FEATURES)).get(feat_index);

        for (Map.Entry<String, Feature> entry:features.getMap().entrySet())
        {
            if ((entry.getValue() != null) && (!entry.getValue().getStringValue().equals("")))
            {
                Element feature_element = doc.createElementNS(NAMESPACE, "feature");
                feature_element.setAttribute("name", entry.getKey());
                feature_element.setAttribute("value", entry.getValue().getStringValue());
                features_element.appendChild(feature_element);
            }
        }

        return features_element;
    }


    /************************************************************************************************
     * Element generation part
     ***********************************************************************************************/
    public Utterance unpackDocument(Document doc)
        throws MaryIOException
    {
        Hashtable<SequenceTypePair, ArrayList<IntegerPair>> alignments;
        Locale l;
        alignments = new Hashtable<SequenceTypePair, ArrayList<IntegerPair>>();

        Element root = doc.getDocumentElement();
        String[] loc = root.getAttribute("xml:lang").split("-");
        if (loc.length > 1)
            l = new Locale.Builder().setLanguage(loc[0]).setRegion(loc[1]).build();
        else
            l = new Locale.Builder().setLanguage(loc[0]).build();


        Utterance utt = new Utterance("", l); // FIXME: see "Utterance.setText"

        // 1. going through everything and save the alignments
        NodeList elts = root.getElementsByTagName("p");
        String text = "";
        for (int i=0; i<elts.getLength(); i++)
        {
            Element p = (Element) elts.item(i);
            NodeList nl = p.getChildNodes();
            boolean found_text = false;
            int j=0;
            while ((!found_text) && (j < nl.getLength()))
            {
                Node node = nl.item(j);

                if (node.getNodeType() == Node.TEXT_NODE)
                {
                    if (!node.getNodeValue().trim().equals(""))
                    {
                        text += node.getNodeValue().trim() + "\n"; // FIXME: new line directly encoded :
                        found_text = true;
                    }
                }
                j++;
            }

            if (!found_text)
                throw new MaryIOException("Cannot find the text of the paragraph", null);

            generateParagraph(p, utt, alignments);
        }

        utt.setText(text); // FIXME: see "Utterance.setText"

        // 2. Dealing relations
        for (SequenceTypePair k: alignments.keySet())
        {
            logger.info("source: " + utt.getSequence(k.getLeft()));
            logger.info("target: " + utt.getSequence(k.getRight()));
            Relation rel = new Relation(utt.getSequence(k.getLeft()),
                                        utt.getSequence(k.getRight()),
                                        alignments.get(k));

            utt.setRelation(k.getLeft(), k.getRight(), rel);
        }

        return utt;
    }

    public void generateParagraph(Element elt,
                                  Utterance utt,
                                  Hashtable<SequenceTypePair, ArrayList<IntegerPair>> alignments)
        throws MaryIOException
    {
        assert elt.getTagName() == "p";

        // Retrieve the sentence offset
        int sentence_offset = 0;
        if (utt.getSequence(SupportedSequenceType.SENTENCE) != null)
            sentence_offset = utt.getSequence(SupportedSequenceType.SENTENCE).size();

        NodeList nl = elt.getChildNodes();
        String text = null;
        logger.info("Current paragraph contains " + nl.getLength() + " childs");
        for (int j=0; j<nl.getLength(); j++)
        {
            Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE)
            {
                logger.info("Unpack the text");

                if (!node.getNodeValue().trim().equals(""))
                    text = node.getNodeValue().trim();
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                logger.info("Unpack the sentence");
                Element cur_elt = (Element) node;
                if (cur_elt.getTagName() == "s")
                {
                    generateSentence(cur_elt, utt, alignments);
                }
                else if (cur_elt.getTagName() == "voice")
                {
                    throw new MaryIOException("I do not now what to do with voice tag !", null);
                }
            }
            else
            {
                throw new MaryIOException("Unknown node element type during unpacking: " +
                                          node.getNodeType(), null);
            }
        }

        if (text == null)
            throw new MaryIOException("Cannot find the text of the paragraph", null);

        // Create/modify the sequence by adding the paragraph
        Paragraph par = new Paragraph(text);
        Sequence<Paragraph> seq_par =
            (Sequence<Paragraph>) utt.getSequence(SupportedSequenceType.PARAGRAPH);
        if (seq_par == null)
        {
            seq_par = new Sequence<Paragraph>();
        }
        seq_par.add(par);
        utt.addSequence(SupportedSequenceType.PARAGRAPH, seq_par);

        // No sentence => no alignments !
        int size_sentence = utt.getSequence(SupportedSequenceType.SENTENCE).size();
        if (size_sentence == sentence_offset)
            return;

        if (!alignments.containsKey(new SequenceTypePair(SupportedSequenceType.PARAGRAPH,
                                                         SupportedSequenceType.SENTENCE)))
        {
            alignments.put(new SequenceTypePair(SupportedSequenceType.PARAGRAPH,
                                                SupportedSequenceType.SENTENCE),
                           new ArrayList<IntegerPair>());
        }

        ArrayList<IntegerPair> alignment_paragraph_sentence =
            alignments.get(new SequenceTypePair(SupportedSequenceType.PARAGRAPH,
                                                SupportedSequenceType.SENTENCE));
        int id_par = seq_par.size() - 1;

        // Deal with Relation sentences part
        for (int i=sentence_offset; i < size_sentence; i++)
        {
            alignment_paragraph_sentence.add(new IntegerPair(id_par, i));
        }
    }

    public void generateSentence(Element elt,
                                 Utterance utt,
                                 Hashtable<SequenceTypePair, ArrayList<IntegerPair>> alignments)
        throws MaryIOException
    {
        assert elt.getTagName() == "s";

        int phrase_offset = 0;
        if (utt.getSequence(SupportedSequenceType.PHRASE) != null)
            phrase_offset = utt.getSequence(SupportedSequenceType.PHRASE).size();

        int word_offset = 0;
        if (utt.getSequence(SupportedSequenceType.WORD) != null)
            word_offset = utt.getSequence(SupportedSequenceType.WORD).size();


        NodeList nl = elt.getChildNodes();
        String text = null;
        int status_loading = 0; // 0 = none yet, 1 = word found, 2 = phrase found
        for (int j=0; j<nl.getLength(); j++)
        {
            Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE)
            {
                if (!node.getNodeValue().trim().equals(""))
                    text = node.getNodeValue().trim();
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element cur_elt = (Element) node;
                if (cur_elt.getTagName() == "t")
                {
                    if (status_loading == 2)
                        throw new MaryIOException("Cannot unserialize a word isolated from a phrase", null);
                    generateWord(cur_elt, utt, alignments);
                    status_loading = 1;
                }
                else if (cur_elt.getTagName() == "mtu")
                {
                    if (status_loading == 2)
                        throw new MaryIOException("Cannot unserialize a word isolated from a phrase", null);
                    NodeList mtu_nl = cur_elt.getChildNodes();

                    for (int k=0; k<mtu_nl.getLength(); k++)
                    {
                        Node word_node = mtu_nl.item(k);
                        if (word_node.getNodeType() == Node.ELEMENT_NODE)
                        {
                            generateWord((Element) word_node, utt, alignments);
                        }
                        else
                        {
                            throw new MaryIOException("Unknown node element type during unpacking the mtu: " +
                                                      node.getNodeType(), null);
                        }
                    }

                    status_loading = 1;
                }
                else if (cur_elt.getTagName() == "prosody")
                {
                    if (status_loading == 1)
                        throw new MaryIOException("Cannot unserialize a word isolated from a phrase", null);
                    // FIXME: what do we do with this first child idea....
                    generatePhrase((Element) cur_elt.getFirstChild(), utt, alignments);
                    status_loading = 2;
                }
                else if (cur_elt.getTagName() == "phrase")
                {
                    if (status_loading == 1)
                        throw new MaryIOException("Cannot unserialize a word isolated from a phrase", null);

                    generatePhrase(cur_elt, utt, alignments);
                    status_loading = 2;
                }
                else
                {
                    throw new MaryIOException("Unknown node element during unpacking: " +
                                              cur_elt.getTagName(), null);
                }
            }
            else
            {
                throw new MaryIOException("Unknown node element type during unpacking: " +
                                          node.getNodeType(), null);
            }
        }

        // Create/modify the sequence by adding the sentence
        Sentence s = new Sentence(text);
        Sequence<Sentence> seq_sent = (Sequence<Sentence>) utt.getSequence(SupportedSequenceType.SENTENCE);
        if (seq_sent == null)
        {
            seq_sent = new Sequence<Sentence>();
        }
        seq_sent.add(s);
        utt.addSequence(SupportedSequenceType.SENTENCE, seq_sent);
        int id_sent = seq_sent.size() - 1;

        // Sentence/Phrase alignment
        int size_phrase = utt.getSequence(SupportedSequenceType.PHRASE).size();
        if (size_phrase > 0)
        {
            if (!alignments.containsKey(new SequenceTypePair(SupportedSequenceType.SENTENCE,
                                                             SupportedSequenceType.PHRASE)))
            {
                alignments.put(new SequenceTypePair(SupportedSequenceType.SENTENCE,
                                                    SupportedSequenceType.PHRASE),
                               new ArrayList<IntegerPair>());
            }

            ArrayList<IntegerPair> alignment_sentence_phrase =
                alignments.get(new SequenceTypePair(SupportedSequenceType.SENTENCE,
                                                    SupportedSequenceType.PHRASE));

            for (int i=phrase_offset; i < size_phrase; i++)
            {
                alignment_sentence_phrase.add(new IntegerPair(id_sent, i));
            }
        }

        // Sentence/Word alignment
        int size_word = utt.getSequence(SupportedSequenceType.WORD).size();
        if (size_word > 0)
        {
            if (!alignments.containsKey(new SequenceTypePair(SupportedSequenceType.SENTENCE,
                                                             SupportedSequenceType.WORD)))
            {
                alignments.put(new SequenceTypePair(SupportedSequenceType.SENTENCE,
                                                    SupportedSequenceType.WORD),
                               new ArrayList<IntegerPair>());
            }

            ArrayList<IntegerPair> alignment_sentence_word =
                alignments.get(new SequenceTypePair(SupportedSequenceType.SENTENCE,
                                                    SupportedSequenceType.WORD));

            for (int i=word_offset; i < size_word; i++)
            {
                alignment_sentence_word.add(new IntegerPair(id_sent, i));
            }
        }
    }

    public void generatePhrase(Element elt,
                               Utterance utt,
                               Hashtable<SequenceTypePair, ArrayList<IntegerPair>> alignments)
        throws MaryIOException
    {
        assert elt.getTagName().equals("phrase");

        int word_offset = 0;
        if (utt.getSequence(SupportedSequenceType.WORD) != null)
            word_offset = utt.getSequence(SupportedSequenceType.WORD).size();
        Boundary boundary = null;

        NodeList nl = elt.getChildNodes();
        String text = null;
        for (int j=0; j<nl.getLength(); j++)
        {
            Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE)
            {

                if (!node.getNodeValue().trim().equals(""))
                    text = node.getNodeValue().trim();
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element cur_elt = (Element) node;
                if (cur_elt.getTagName() == "t")
                {
                    generateWord(cur_elt, utt, alignments);
                }
                else if (cur_elt.getTagName() == "mtu")
                {
                    NodeList mtu_nl = cur_elt.getChildNodes();

                    for (int k=0; k<mtu_nl.getLength(); k++)
                    {
                        Node word_node = mtu_nl.item(k);
                        if (word_node.getNodeType() == Node.ELEMENT_NODE)
                        {

                            generateWord((Element) word_node, utt, alignments);
                        }
                        else
                        {
                            throw new MaryIOException("Unknown node element type during unpacking the mtu: " +
                                                      node.getNodeType(), null);
                        }
                    }

                }
                else if (cur_elt.getTagName() == "boundary")
                {
                    int breakindex = Integer.parseInt(cur_elt.getAttribute("breakindex"));
                    String tone = cur_elt.getAttribute("tone");
                    boundary = new Boundary(breakindex, tone);
                }
                else
                {
                    throw new MaryIOException("Unknown node element during unpacking: " +
                                              cur_elt.getTagName(), null);
                }
            }
            else
            {
                throw new MaryIOException("Unknown node element type during unpacking: " +
                                          node.getNodeType(), null);
            }
        }

        // Create the phrase and add the phrase to the utterance
        Phrase p = new Phrase(boundary);
        Sequence<Phrase> seq_phrase = (Sequence<Phrase>) utt.getSequence(SupportedSequenceType.PHRASE);
        if (seq_phrase == null)
        {
            seq_phrase = new Sequence<Phrase>();
        }
        utt.addSequence(SupportedSequenceType.PHRASE, seq_phrase);
        seq_phrase.add(p);


        // Phrase/Word alignment
        if (!alignments.containsKey(new SequenceTypePair(SupportedSequenceType.PHRASE,
                                                         SupportedSequenceType.WORD)))
        {
            alignments.put(new SequenceTypePair(SupportedSequenceType.PHRASE,
                                                SupportedSequenceType.WORD),
                           new ArrayList<IntegerPair>());
        }

        ArrayList<IntegerPair> alignment_phrase_word =
            alignments.get(new SequenceTypePair(SupportedSequenceType.PHRASE,
                                                SupportedSequenceType.WORD));

        int size_word = utt.getSequence(SupportedSequenceType.WORD).size();
        int id_phrase = seq_phrase.size() - 1;
        for (int i=word_offset; i < size_word; i++)
        {
            alignment_phrase_word.add(new IntegerPair(id_phrase, i));
        }
    }


    public void generateWord(Element elt,
                             Utterance utt,
                             Hashtable<SequenceTypePair, ArrayList<IntegerPair>> alignments)
        throws MaryIOException
    {
        assert elt.getTagName().equals("t");
        ArrayList<Syllable> syllable_list = new ArrayList<Syllable>();


        int syllable_offset = 0;
        if (utt.getSequence(SupportedSequenceType.SYLLABLE) != null)
            syllable_offset = utt.getSequence(SupportedSequenceType.SYLLABLE).size();

        NodeList nl = elt.getChildNodes();
        String text = null;
        for (int j=0; j<nl.getLength(); j++)
        {
            Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE)
            {
                if (!node.getNodeValue().trim().equals(""))
                    text = node.getNodeValue().trim();

            }
            else if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element syllable_elt = (Element) node;
                generateSyllable(syllable_elt, utt, alignments);
            }
            else
            {
                throw new MaryIOException("Unknown node element type during unpacking: " +
                                          node.getNodeType(), null);
            }
        }

        if (text == null)
            throw new MaryIOException("Cannot find the text of the word", null);

        logger.info("Unpacking word \"" + text + "\"");
        Word w = new Word(text);
        if (elt.hasAttribute("pos"))
        {
            String pos = elt.getAttribute("pos");
            w.setPOS(pos);
        }

        if (elt.hasAttribute("accent"))
        {
            String accent = elt.getAttribute("accent");
            w.setAccent(new Accent(accent));
        }

        // FIXME: this should be a temp hack !
        if (elt.hasAttribute("ph"))
        {
            String[] phoneme_labels = elt.getAttribute("ph").split(" - ");
            ArrayList<Phoneme> phonemes = new ArrayList<Phoneme>();
            for (int i=0; i<phoneme_labels.length; i++)
            {
                phonemes.add(new Phoneme(phoneme_labels[i]));
            }
            w.setPhonemes(phonemes);
        }


        // Create the phrase and add the phrase to the
        Sequence<Word> seq_word = (Sequence<Word>) utt.getSequence(SupportedSequenceType.WORD);
        if (seq_word == null)
        {
            seq_word = new Sequence<Word>();
        }
        utt.addSequence(SupportedSequenceType.WORD, seq_word);
        seq_word.add(w);

        // Word/Syllable alignment
        if ((utt.getSequence(SupportedSequenceType.SYLLABLE) != null) &&
            (utt.getSequence(SupportedSequenceType.SYLLABLE).size() > 0))
        {
            if (!alignments.containsKey(new SequenceTypePair(SupportedSequenceType.WORD,
                                                             SupportedSequenceType.SYLLABLE)))
            {
                alignments.put(new SequenceTypePair(SupportedSequenceType.WORD,
                                                    SupportedSequenceType.SYLLABLE),
                               new ArrayList<IntegerPair>());
            }

            ArrayList<IntegerPair> alignment_word_syllable =
                alignments.get(new SequenceTypePair(SupportedSequenceType.WORD,
                                                    SupportedSequenceType.SYLLABLE));

            int size_syllable = utt.getSequence(SupportedSequenceType.SYLLABLE).size();
            int id_word = seq_word.size() - 1;
            for (int i=syllable_offset; i < size_syllable; i++)
            {
                alignment_word_syllable.add(new IntegerPair(id_word, i));
            }
        }
    }

    public void generateSyllable(Element elt,
                                 Utterance utt,
                                 Hashtable<SequenceTypePair, ArrayList<IntegerPair>> alignments)
        throws MaryIOException
    {
        assert elt.getTagName() == "syllable";
        ArrayList<Phoneme> phoneme_list = new ArrayList<Phoneme>();


        int phone_offset = 0;
        if (utt.getSequence(SupportedSequenceType.PHONE) != null)
            phone_offset = utt.getSequence(SupportedSequenceType.PHONE).size();

        NodeList nl = elt.getChildNodes();
        String text = null;
        for (int j=0; j<nl.getLength(); j++)
        {
            Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE)
            {
                if (!node.getNodeValue().trim().equals(""))
                    text = node.getNodeValue().trim();
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element phoneme_elt = (Element) node;
                generatePhone(phoneme_elt, utt);
            }
            else
            {
                throw new MaryIOException("Unknown node element type during unpacking: " +
                                          node.getNodeType(), null);
            }
        }

        // FIXME: for now the tone phoneme is just based on the label...
        Phoneme tone = null;
        if (elt.hasAttribute("tone"))
        {
            tone = new Phoneme(elt.getAttribute("tone"));
        }

        Accent accent = null;
        if (elt.hasAttribute("accent"))
        {
            accent = new Accent(elt.getAttribute("accent"));
        }

        int stress_level = 0;
        if (elt.hasAttribute("stress"))
        {
            stress_level = Integer.parseInt(elt.getAttribute("stress"));
        }
        Syllable syl = new Syllable(tone, stress_level, accent);

        // Create the phrase and add the phrase to the
        Sequence<Syllable> seq_syllable =
            (Sequence<Syllable>) utt.getSequence(SupportedSequenceType.SYLLABLE);
        if (seq_syllable == null)
        {
            seq_syllable = new Sequence<Syllable>();
        }
        utt.addSequence(SupportedSequenceType.SYLLABLE, seq_syllable);
        seq_syllable.add(syl);

        // Syllable/Phone alignment
        if ((utt.getSequence(SupportedSequenceType.PHONE) != null) &&
            (utt.getSequence(SupportedSequenceType.PHONE).size() > 0))
        {
            if (!alignments.containsKey(new SequenceTypePair(SupportedSequenceType.SYLLABLE,
                                                             SupportedSequenceType.PHONE)))
            {
                alignments.put(new SequenceTypePair(SupportedSequenceType.SYLLABLE,
                                                    SupportedSequenceType.PHONE),
                               new ArrayList<IntegerPair>());
            }

            ArrayList<IntegerPair> alignment_syllable_phone =
                alignments.get(new SequenceTypePair(SupportedSequenceType.SYLLABLE,
                                                    SupportedSequenceType.PHONE));

            int size_phone = utt.getSequence(SupportedSequenceType.PHONE).size();
            int id_syllable = seq_syllable.size() - 1;
            for (int i=phone_offset; i < size_phone; i++)
            {
                alignment_syllable_phone.add(new IntegerPair(id_syllable, i));
            }
        }
    }

    public Phoneme generatePhoneme(Element elt)
        throws MaryIOException
    {
        assert elt.getTagName() == "ph";
        Phoneme ph = new Phoneme(elt.getAttribute("p"));
        return ph;
    }

    public void generatePhone(Element elt, Utterance utt)
    {
        assert elt.getTagName() == "ph";
        Phoneme ph = new Phoneme(elt.getAttribute("p"));

        // Create the phrase and add the phrase to the
        Sequence<Phoneme> seq_phone =
            (Sequence<Phoneme>) utt.getSequence(SupportedSequenceType.PHONE);
        if (seq_phone == null)
        {
            seq_phone = new Sequence<Phoneme>();
        }
        utt.addSequence(SupportedSequenceType.PHONE, seq_phone);
        seq_phone.add(ph);
    }
}
