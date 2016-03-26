package marytts.io;

import java.util.Locale;
import java.util.ArrayList;

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

import marytts.data.Utterance;
import marytts.data.item.linguistic.*;
import marytts.data.item.phonology.*;
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
            for (Paragraph p: utt.getParagraphs())
            {
                rootElement.appendChild(exportParagraph(p, doc));
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

    public Utterance unpackDocument(Document doc)
        throws MaryIOException
    {
        Locale l;
        Element root = doc.getDocumentElement();
        String[] loc = root.getAttribute("xml:lang").split("-");
        if (loc.length > 1)
            l = new Locale.Builder().setLanguage(loc[0]).setRegion(loc[1]).build();
        else
            l = new Locale.Builder().setLanguage(loc[0]).build();

        ArrayList<Paragraph> par = new ArrayList<Paragraph>();
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
                    text += node.getNodeValue() + "\n"; // FIXME: new line directly encoded :
                    found_text = true;
                }
                j++;
            }

            if (!found_text)
                throw new MaryIOException("Cannot find the text of the paragraph", null);

            par.add(generateParagraph(p));
        }
        // Build the text
        Utterance utt = new Utterance(text, l, par);
        return utt;
    }

    /************************************************************************************************
     * Element generation part
     ***********************************************************************************************/
    public Element exportParagraph(Paragraph paragraph, Document doc)
    {
        Element par_element = doc.createElement("p");

        // Export node value
        Node text = doc.createTextNode(paragraph.getText());
        par_element.appendChild(text);

        // Export subelements
        for (Sentence s: paragraph.getSentences())
            par_element.appendChild(exportSentence(s, doc));

        return par_element;
    }

    public Element exportSentence(Sentence sentence, Document doc)
    {
        Element sent_element = doc.createElement("s");

        // Export node value
        Node text = doc.createTextNode(sentence.getText());
        sent_element.appendChild(text);

        // Export subelements
        for (Word w: sentence.getWords())
            sent_element.appendChild(exportWord(w, doc));

        return sent_element;
    }

    public Element exportWord(Word word, Document doc)
    {
        Element word_element = doc.createElement("t");

        logger.info("Serializing word \"" + word.getText() + "\"");

        // Export node value
        Node text = doc.createTextNode(word.getText());
        word_element.appendChild(text);

        // Export subelements
        for (Syllable s: word.getSyllables())
            word_element.appendChild(exportSyllable(s, doc));

        if (word.getPOS() != null)
            word_element.setAttribute("pos", word.getPOS());

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
        return null;
    }

    public Element exportPhone(Phone ph, Document doc)
    {
        return null;
    }


    /************************************************************************************************
     * Element generation part
     ***********************************************************************************************/
    public Paragraph generateParagraph(Element elt)
        throws MaryIOException
    {
        assert elt.getTagName() == "p";
        ArrayList<Sentence> sentence_list = new ArrayList<Sentence>();

        NodeList nl = elt.getChildNodes();
        String text = null;
        logger.info("Current paragraph contains " + nl.getLength() + " childs");
        for (int j=0; j<nl.getLength(); j++)
        {
            Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE)
            {
                logger.info("Unpack the text");
                text = node.getNodeValue();
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                logger.info("Unpack the sentence");
                Element sentence_elt = (Element) node;
                sentence_list.add(generateSentence(sentence_elt));
            }
            else
            {
                throw new MaryIOException("Unknown node element type during unpacking: " + node.getNodeType(), null);
            }
        }

        if (text == null)
            throw new MaryIOException("Cannot find the text of the paragraph", null);

        return new Paragraph(text, sentence_list);
    }

    public Sentence generateSentence(Element elt)
        throws MaryIOException
    {
        assert elt.getTagName() == "s";
        ArrayList<Word> word_list = new ArrayList<Word>();

        NodeList nl = elt.getChildNodes();
        String text = null;
        for (int j=0; j<nl.getLength(); j++)
        {
            Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE)
            {
                text = node.getNodeValue();
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element word_elt = (Element) node;
                word_list.add(generateWord(word_elt));
            }
            else
            {
                throw new MaryIOException("Unknown node element type during unpacking: " + node.getNodeType(), null);
            }
        }

        // FIXME: for now we assume there is no text
        // if (text == null)
        //     throw new MaryIOException("Cannot find the text of the sentence", null);

        return new Sentence(text, word_list);
    }

    public Word generateWord(Element elt)
        throws MaryIOException
    {
        assert elt.getTagName() == "t";
        ArrayList<Syllable> syllable_list = new ArrayList<Syllable>();

        NodeList nl = elt.getChildNodes();
        String text = null;
        for (int j=0; j<nl.getLength(); j++)
        {
            Node node = nl.item(j);

            if (node.getNodeType() == Node.TEXT_NODE)
            {
                text = node.getNodeValue();
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element syllable_elt = (Element) node;
                syllable_list.add(generateSyllable(syllable_elt));
            }
            else
            {
                throw new MaryIOException("Unknown node element type during unpacking: " + node.getNodeType(), null);
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

        return w;

    }

    public Syllable generateSyllable(Element elt)
        throws MaryIOException
    {
        return null;
    }

    public Phone generatePhone(Element elt)
        throws MaryIOException
    {
        return null;
    }

}
