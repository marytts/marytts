package marytts.io;

import java.util.Locale;
import java.util.ArrayList;
import java.util.Hashtable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;


import java.io.File;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import marytts.data.utils.IntegerPair;
import marytts.data.utils.SequenceTypePair;

import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.item.linguistic.*;
import marytts.data.item.phonology.*;
import marytts.data.item.prosody.*;
import marytts.data.item.*;
import marytts.util.MaryUtils;

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
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);

            return writer.toString();
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
            for (int i=0; i<utt.getSequence(Utterance.SupportedSequenceType.PARAGRAPH).size(); i++)
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
        Paragraph paragraph = ((Sequence<Paragraph>) utt.getSequence(Utterance.SupportedSequenceType.PARAGRAPH)).get(par_index);
        Node text = doc.createTextNode(paragraph.getText());
        par_element.appendChild(text);

        // Export subelements
        Relation rel_par_sent = utt.getRelation(Utterance.SupportedSequenceType.PARAGRAPH, Utterance.SupportedSequenceType.SENTENCE);

        // FIXME: needs to have a "containsRelation" method
        if (rel_par_sent != null)
        {
            ArrayList<Sentence> sentences = (ArrayList<Sentence>) rel_par_sent.getRelatedItems(par_index);
            for (Sentence s: sentences)
                par_element.appendChild(exportSentence(s, doc));
        }

        return par_element;
    }

    public Element exportSentence(Sentence sentence, Document doc)
    {

        // FIXME: temp checking as normally we should have directly insider the code this managed
        assert !((sentence.getWords().size() > 0) && (sentence.getPhrases().size() > 0));

        Element sent_element = doc.createElementNS(NAMESPACE, "s");

        // Export node value
        Node text = doc.createTextNode(sentence.getText());
        sent_element.appendChild(text);

        // Export subelements
        for (Word w: sentence.getWords())
            sent_element.appendChild(exportWord(w, doc));

        for (Phrase p: sentence.getPhrases())
            sent_element.appendChild(exportPhrase(p, doc));

        return sent_element;
    }

    public Element exportPhrase(Phrase phrase, Document doc)
    {
        Element phrase_element = doc.createElementNS(NAMESPACE, "phrase");

        logger.info("Serializing phrase");

        // Export subelements
        for (Word w: phrase.getWords())
            phrase_element.appendChild(exportWord(w, doc));

        Element prosody_element = doc.createElementNS(NAMESPACE, "prosody");
        prosody_element.appendChild(phrase_element);
        return prosody_element;
    }

    public Element exportWord(Word word, Document doc)
    {
        Element word_element = doc.createElementNS(NAMESPACE, "t");

        logger.info("Serializing word \"" + word.getText() + "\"");

        // Export node value
        Node text = doc.createTextNode(word.getText());
        word_element.appendChild(text);

        // Export subelements
        for (Syllable s: word.getSyllables())
            word_element.appendChild(exportSyllable(s, doc));

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

        return word_element;
    }

    public Element exportSyllable(Syllable syl, Document doc)
    {
        Element syllable_element = doc.createElementNS(NAMESPACE, "syllable");

        logger.info("Serializing syllable");

        // Export subelements
        for (Phoneme p: syl.getPhonemes())
            syllable_element.appendChild(exportPhone(p, doc));

        if (syl.getTone() != null)
            syllable_element.setAttribute("tone", syl.getTone().getLabel());

        if (syl.getAccent() != null)
            syllable_element.setAttribute("accent", syl.getAccent().getLabel());

        syllable_element.setAttribute("stress", Integer.toString(syl.getStressLevel()));

        // ArrayList<Phoneme> phonemes = syllable.getPhonemes();
        // if (phonemes.size() > 0)
        // {
        //     String phonemes_str = "";
        //     for (int i=0; i<phonemes.size()-1; i++)
        //         phonemes_str += phonemes.get(i).getLabel() + " - ";
        //     phonemes_str += phonemes.get(phonemes.size() - 1).getLabel();
        //     syllable_element.setAttribute("ph", phonemes_str);
        // }

        return syllable_element;
    }

    public Element exportPhone(Phoneme ph, Document doc)
    {
        Element phone_element = doc.createElementNS(NAMESPACE, "ph");

        phone_element.setAttribute("p", ph.getLabel());

        if (ph instanceof Phone)
        {
            phone_element.setAttribute("start", String.valueOf(((Phone) ph).getStart()));
            phone_element.setAttribute("d", String.valueOf(((Phone) ph).getDuration()));
        }
        return phone_element;
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

                if (node.getNodeType() == Node.TEXT_NODE) {
                    text += node.getNodeValue().trim() + "\n"; // FIXME: new line directly encoded :
                    found_text = true;
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
            Relation rel = new Relation(utt.getSequence(k.getLeft()), utt.getSequence(k.getRight()), alignments.get(k));

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
        if (utt.getSequence(Utterance.SupportedSequenceType.SENTENCE) != null)
            sentence_offset = utt.getSequence(Utterance.SupportedSequenceType.SENTENCE).size();

        NodeList nl = elt.getChildNodes();
        String text = null;
        int sentence_index=0;
        logger.info("Current paragraph contains " + nl.getLength() + " childs");
        for (int j=0; j<nl.getLength(); j++)
        {
            Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE)
            {
                logger.info("Unpack the text");
                text = node.getNodeValue().trim();
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                logger.info("Unpack the sentence");
                Element cur_elt = (Element) node;
                if (cur_elt.getTagName() == "s")
                {
                    generateSentence(cur_elt, utt, alignments);
                    sentence_index++;
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
        Sequence<Paragraph> seq_par = (Sequence<Paragraph>) utt.getSequence(Utterance.SupportedSequenceType.PARAGRAPH);
        if (seq_par == null)
        {
            seq_par = new Sequence<Paragraph>();
        }
        seq_par.add(par);
        utt.addSequence(Utterance.SupportedSequenceType.PARAGRAPH, seq_par);

        // No sentence => no alignments !
        if (sentence_index == 0)
            return;

        if (!alignments.containsKey(new SequenceTypePair(Utterance.SupportedSequenceType.PARAGRAPH, Utterance.SupportedSequenceType.SENTENCE)))
        {
            alignments.put(new SequenceTypePair(Utterance.SupportedSequenceType.PARAGRAPH, Utterance.SupportedSequenceType.SENTENCE),
                           new ArrayList<IntegerPair>());
        }

        ArrayList<IntegerPair> alignment_paragraph_sentence = alignments.get(new SequenceTypePair(Utterance.SupportedSequenceType.PARAGRAPH, Utterance.SupportedSequenceType.SENTENCE));
        int id_par = seq_par.size() - 1;

        // Deal with Relation sentences part
        for (int i=0; i < sentence_index; i++)
        {
            alignment_paragraph_sentence.add(new IntegerPair(id_par, sentence_offset + i));
        }
    }

    public void generateSentence(Element elt,
                                 Utterance utt,
                                 Hashtable<SequenceTypePair, ArrayList<IntegerPair>> alignments)
        throws MaryIOException
    {
        assert elt.getTagName() == "s";
        ArrayList<Word> word_list = new ArrayList<Word>();
        ArrayList<Phrase> phrase_list = new ArrayList<Phrase>();

        NodeList nl = elt.getChildNodes();
        String text = null;
        int status_loading = 0; // 0 = none yet, 1 = word found, 2 = phrase found
        for (int j=0; j<nl.getLength(); j++)
        {
            Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE)
            {
                text = node.getNodeValue().trim();
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element cur_elt = (Element) node;
                if (cur_elt.getTagName() == "t")
                {
                    if (status_loading == 2)
                        throw new MaryIOException("Cannot unserialize a word isolated from a phrase", null);
                    word_list.add(generateWord(cur_elt));
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
                            word_list.add(generateWord((Element) word_node));
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
                    phrase_list.add(generatePhrase((Element) cur_elt.getFirstChild()));
                    status_loading = 2;
                }
                else if (cur_elt.getTagName() == "phrase")
                {
                    if (status_loading == 1)
                        throw new MaryIOException("Cannot unserialize a word isolated from a phrase", null);
                    phrase_list.add(generatePhrase(cur_elt));
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

        // FIXME: for now we assume there is no text
        // if (text == null)
        //     throw new MaryIOException("Cannot find the text of the sentence", null);



        // Create/modify the sequence by adding the sentence
        Sentence s = new Sentence(text, word_list);
        s.setPhrases(phrase_list);
        Sequence<Sentence> seq_sent = (Sequence<Sentence>) utt.getSequence(Utterance.SupportedSequenceType.SENTENCE);
        if (seq_sent == null)
        {
            seq_sent = new Sequence<Sentence>();
        }
        seq_sent.add(s);
        utt.addSequence(Utterance.SupportedSequenceType.SENTENCE, seq_sent);
    }

    public Phrase generatePhrase(Element elt)
        throws MaryIOException
    {
        assert elt.getTagName().equals("phrase");
        ArrayList<Word> word_list = new ArrayList<Word>();
        Boundary boundary = null;

        NodeList nl = elt.getChildNodes();
        String text = null;
        for (int j=0; j<nl.getLength(); j++)
        {
            Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE)
            {
                text = node.getNodeValue().trim();
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element cur_elt = (Element) node;
                if (cur_elt.getTagName() == "t")
                {
                    word_list.add(generateWord(cur_elt));
                }
                else if (cur_elt.getTagName() == "mtu")
                {
                    NodeList mtu_nl = cur_elt.getChildNodes();

                    for (int k=0; k<mtu_nl.getLength(); k++)
                    {
                        Node word_node = mtu_nl.item(k);
                        if (word_node.getNodeType() == Node.ELEMENT_NODE)
                        {
                            word_list.add(generateWord((Element) word_node));
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

        Phrase p = new Phrase(boundary, word_list);

        return p;
    }

    public Word generateWord(Element elt)
        throws MaryIOException
    {
        assert elt.getTagName().equals("t");
        ArrayList<Syllable> syllable_list = new ArrayList<Syllable>();

        NodeList nl = elt.getChildNodes();
        String text = null;
        for (int j=0; j<nl.getLength(); j++)
        {
            Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE)
            {
                text = node.getNodeValue().trim();
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element syllable_elt = (Element) node;
                syllable_list.add(generateSyllable(syllable_elt));
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
        Word w = new Word(text, syllable_list);

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

        return w;
    }

    public Syllable generateSyllable(Element elt)
        throws MaryIOException
    {
        assert elt.getTagName() == "syllable";
        ArrayList<Phoneme> phoneme_list = new ArrayList<Phoneme>();

        NodeList nl = elt.getChildNodes();
        String text = null;
        for (int j=0; j<nl.getLength(); j++)
        {
            Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE)
            {
                text = node.getNodeValue().trim();
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element phoneme_elt = (Element) node;
                phoneme_list.add(generatePhoneme(phoneme_elt));
            }
            else
            {
                throw new MaryIOException("Unknown node element type during unpacking: " +
                                          node.getNodeType(), null);
            }
        }

        logger.info("Unpacking word \"" + text + "\"");
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
        Syllable syl = new Syllable(phoneme_list, tone, stress_level, accent);

        return syl;
    }

    public Phoneme generatePhoneme(Element elt)
        throws MaryIOException
    {
        assert elt.getTagName() == "ph";
        Phoneme ph = new Phoneme(elt.getAttribute("p"));
        return ph;
    }
}
