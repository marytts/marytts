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
package marytts.modules;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.phonemiser.Syllabifier;
import marytts.modules.synthesis.FreeTTSVoices;
import marytts.modules.synthesis.Voice;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.MaryNormalisedWriter;

import org.apache.log4j.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.speech.freetts.FeatureSet;
import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;


/**
 * Convert FreeTTS utterances into MaryXML format.
 * This abstract base class is to provide the common part for all
 * Utterance to MaryXML converters.
 *
 * @author Marc Schr&ouml;der
 */

public abstract class Utt2XMLBase extends InternalModule {
    protected DocumentBuilderFactory factory = null;
    protected DocumentBuilder docBuilder = null;

    public Utt2XMLBase(String name, MaryDataType input, MaryDataType output, Locale locale) {
        super(name, input, output, locale);
    }

    public void startup() throws Exception {
        if (factory == null) {
            factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
        }
        if (docBuilder == null) {
            docBuilder = factory.newDocumentBuilder();
        }
        super.startup();
        // Initialise FreeTTS
        FreeTTSVoices.load();
    }

    public MaryData process(MaryData d) throws Exception {
        Document doc = MaryXML.newDocument();
        Element root = doc.getDocumentElement();
        Locale locale = d.getLocale();
        String privatelexicon = d.getPrivateLexicon();
        
        if (locale != null) {
            root.setAttribute("xml:lang", MaryUtils.locale2xmllang(locale));
        }
        
        if (privatelexicon!= null && privatelexicon.length() > 0) {
        	root.setAttribute("lexicon", privatelexicon);
        }
        	
        
        Element paragraph = MaryXML.appendChildElement(root, MaryXML.PARAGRAPH);

        List<Utterance> utterances = d.getUtterances();
        Iterator<Utterance> it = utterances.iterator();
        while (it.hasNext()) {
            Utterance utterance = it.next();
            Element insertHere = paragraph;

            if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                utterance.dump(pw, 2, name(), true); // padding, justRelations
                logger.debug("Converting the following Utterance to XML:\n"+sw.toString());
            }

            // Make sure we have the correct voice:
            Voice maryVoice = null;
            if (utterance.getVoice() != null) {
                maryVoice = FreeTTSVoices.getMaryVoice(utterance.getVoice());
            }
            if (maryVoice != null) {
                if (insertHere.getTagName().equals(MaryXML.VOICE)) {
                    // Are utterance voice and voiceElement voice the same?
                    if (maryVoice.hasName(insertHere.getAttribute("name"))) {
                        // then insertHere is set OK, leave it like it is
                    } else {
                        // get one higher up, create new voice element after this
                        // one, and make insertHere point to the new voice element
                        Element parent = (Element) insertHere.getParentNode();
                        Element newVoice = MaryXML.createElement(doc, MaryXML.VOICE);
                        parent.appendChild(newVoice);
                        newVoice.setAttribute("name", maryVoice.getName());
                        insertHere = newVoice;
                    }
                } else {
                    // Check if the last child of insertHere is a voice with the right name
                    Element lastChild = MaryDomUtils.getLastChildElement(insertHere);
                    if (lastChild != null && lastChild.getTagName().equals(MaryXML.VOICE)
                            && maryVoice.hasName(lastChild.getAttribute("name"))) {
                        insertHere = lastChild;
                    } else {
                        // create a new voice element, insert it as a child of this
                        // node, and let insertHere point to it
                        Element newVoice = MaryXML.createElement(doc, MaryXML.VOICE);
                        insertHere.appendChild(newVoice);
                        newVoice.setAttribute("name", maryVoice.getName());
                        insertHere = newVoice;
                    }
                }
                // Now insertHere is the correct <voice> element.

                // Any prosodic settings to insert?
                Element  prosody = insertProsodySettings(insertHere, utterance);
                if (prosody != null) insertHere = prosody;
                
            }
            // Create a sentence element <s> for this utterance:
            Element sentence = MaryXML.createElement(doc, MaryXML.SENTENCE);
            insertHere.appendChild(sentence);

            fillSentence(sentence, utterance);
        }

        if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
            logger.debug("Constructed the following XML structure:");
            MaryNormalisedWriter mnw = new MaryNormalisedWriter();
            ByteArrayOutputStream debugOut = new ByteArrayOutputStream();
            mnw.output(doc, debugOut);
            logger.debug(debugOut.toString());
        }

        MaryData output = new MaryData(outputType(), d.getLocale());
        output.setDocument(doc);
        return output;
    }

    /**
     * Depending on the data type, find the right information in the utterance
     * and insert it into the sentence.
     */
    protected final void fillSentence(Element sentence, Utterance utterance)
    {
        Document doc = sentence.getOwnerDocument();
        Relation tokenRelation = utterance.getRelation(Relation.TOKEN);
        if (tokenRelation == null) return;
        Item tokenItem = tokenRelation.getHead();
        Relation phraseRelation = utterance.getRelation(Relation.PHRASE);
        Item phraseItem = null;
        if (phraseRelation != null) {
            phraseItem = phraseRelation.getHead();
            // Challenge: Bring token and phrase relations together. They have
            // common children, which can be interpreted as Word or SylStructure
            // items. Algorithm: For a given phrase, look at tokens. If a token's
            // first child, interpreted in the phrase relation, has the phrase as
            // its parent, then insert the token and all its children, and move to
            // the next token. If not, move to the next phrase.
            while (phraseItem != null) {
                // The phrases:
                Element phrase = MaryXML.createElement(doc, MaryXML.PHRASE);
                sentence.appendChild(phrase);
                Element insertHere = phrase;
                // Is this token part of this phrase?
                while (tokenItem != null &&
                       tokenItem.getDaughter().findItem("R:Phrase.parent").equals(phraseItem)) {
                    FeatureSet tokenFeatures = tokenItem.getFeatures();
                    if (tokenFeatures.isPresent(XML2UttBase.PROSODY_START)) {
                        Element prosody = insertProsodySettings(insertHere, tokenFeatures);
                        if (prosody != null) {
                            insertHere = prosody;
                        }
                    }
                    insertToken(tokenItem, phrase, true); // create deep structure
                    if (tokenFeatures.isPresent(XML2UttBase.PROSODY_END)) {
                        assert insertHere.getTagName().equals(MaryXML.PROSODY);
                        insertHere = (Element) insertHere.getParentNode();
                    }
                    tokenItem = tokenItem.getNext();
                }
                phraseItem = phraseItem.getNext();
            }
        } else {
            // No phrase relation, simply create tokens.
            Element insertHere = sentence;
            while (tokenItem != null) {
                FeatureSet tokenFeatures = tokenItem.getFeatures();
                if (tokenFeatures.isPresent(XML2UttBase.PROSODY_START)) {
                    Element prosody = insertProsodySettings(insertHere, tokenFeatures);
                    if (prosody != null) {
                        insertHere = prosody;
                    }
                }
                insertToken(tokenItem, insertHere);
                if (tokenFeatures.isPresent(XML2UttBase.PROSODY_END)) {
                    if (insertHere.getTagName().equals(MaryXML.PROSODY)) {
                        insertHere = (Element) insertHere.getParentNode();
                    } // else, we are looking at an empty prosody tag with no arguments, which is being deleted right now.
                }
                tokenItem = tokenItem.getNext();
            }

        }
    }

    /**
     * Convert an item in the Token relation into XML, inserting it at the
     * specified location in the XML tree.
     */
    protected void insertToken(Item tokenItem, Element parent) {
        insertToken(tokenItem, parent, false); // no deep structure
    }

    /**
     * Convert an item in the Token relation into XML, inserting it at the
     * specified location in the XML tree.
     * @param deep whether to create a deep structure of <syllable> and <ph> elements or not.
     */
    protected void insertToken(Item tokenItem, Element parent, boolean deep) {
        if (tokenItem == null || parent == null) {
            throw new NullPointerException("Null arguments to insertToken()");
        }
        Document doc = parent.getOwnerDocument();
        Voice maryVoice = null;
        if (tokenItem.getUtterance().getVoice() != null) {
            maryVoice = FreeTTSVoices.getMaryVoice(tokenItem.getUtterance().getVoice());
        }
        AllophoneSet allophoneSet = (AllophoneSet) tokenItem.getUtterance().getObject("allophoneset");
        if (allophoneSet == null) {
            throw new NullPointerException("Utterance does not have an AllophoneSet -- should have been set in XML2UttBase.process()");
        }
        Element insertHere = parent;
        boolean needMtu = false;
        boolean insertPhonesFromToken = tokenItem.getFeatures().isPresent("phones");
        Item testWordItem = null;
        if (tokenItem.getFeatures().isPresent("precedingMarks")) {
            String marks = tokenItem.getFeatures().getString("precedingMarks");
            StringTokenizer markTok = new StringTokenizer(marks, ",");
            while (markTok.hasMoreTokens()) {
                String markStr = markTok.nextToken();
                Element markEl = MaryXML.createElement(doc, MaryXML.MARK);
                markEl.setAttribute("name", markStr);
                insertHere.appendChild(markEl);
            }
        }
        // Any boundary preceding the word?
        if (tokenItem.getFeatures().isPresent("precedingBoundaryTone")
                || tokenItem.getFeatures().isPresent("precedingBoundaryBreakindex")
                || tokenItem.getFeatures().isPresent("precedingBoundaryDuration")) {
            Element boundary = MaryXML.createElement(doc, MaryXML.BOUNDARY);
            insertHere.appendChild(boundary);
            if (tokenItem.getFeatures().isPresent("precedingBoundaryTone"))
                boundary.setAttribute("tone", tokenItem.getFeatures().getString("precedingBoundaryTone"));
            if (tokenItem.getFeatures().isPresent("precedingBoundaryBreakindex"))
                boundary.setAttribute("breakindex", tokenItem.getFeatures().getString("precedingBoundaryBreakindex"));
            if (tokenItem.getFeatures().isPresent("precedingBoundaryDuration"))
                boundary.setAttribute("duration", tokenItem.getFeatures().getString("precedingBoundaryDuration"));
        }
        if (tokenItem.getNthDaughter(1) != null
            || (testWordItem = tokenItem.getDaughter()) != null
            && !testWordItem.toString().equals(tokenItem.toString().toLowerCase())) {
            // Token has more than one daughter, or the only daughter is an
            // expanded form -- need to create an <mtu> element
            needMtu = true;
            Element mtu = MaryXML.createElement(doc, MaryXML.MTU);
            parent.appendChild(mtu);
            mtu.setAttribute("orig", tokenItem.toString());
            insertHere = mtu;
        }
        // Any words?
        FeatureSet tokenFeatureSet = tokenItem.getFeatures();
        Item tokenDaughter = tokenItem.getDaughter();
        if (tokenDaughter == null) { // no word relation present
            // Create a <t> element based on token information only
            Element t = MaryXML.createElement(doc, MaryXML.TOKEN);
            insertHere.appendChild(t);
            MaryDomUtils.setTokenText(t, tokenItem.toString());
            if (insertPhonesFromToken) {
                String[] phones = (String[]) tokenItem.getFeatures().getObject("phones");
                t.setAttribute("ph", phoneArray2phoneString(allophoneSet, phones));
                insertPhonesFromToken = false;
            }
            if (tokenFeatureSet.isPresent("accent")) {
                t.setAttribute("accent", tokenFeatureSet.getString("accent"));
            }
        }
        while (tokenDaughter != null) {
            // Part of speech, if present, is associated with the word
            // relation.
            Item wordItem = tokenDaughter.getItemAs("Word");
            Element t = null;
            StringBuilder sampa = new StringBuilder();
            if (wordItem != null) {
                t = MaryXML.createElement(doc, MaryXML.TOKEN);
                insertHere.appendChild(t);
                String tokenText = null;
                // If there is only one, non-expanded word, use text from
                // tokenItem in order to retain capitalisation:
                if (needMtu)
                    tokenText = wordItem.toString();
                else
                    tokenText = tokenItem.toString();
                MaryDomUtils.setTokenText(t, tokenText);
                if (insertPhonesFromToken) {
                    String[] phones = (String[]) tokenItem.getFeatures().getObject("phones");
                    t.setAttribute("ph", phoneArray2phoneString(allophoneSet, phones));
                    insertPhonesFromToken = false;
                } else if (wordItem.getFeatures().isPresent("phones")) {
                    // the word item has phones, take them only if there are no Token phones
                    String[] phones = (String[]) wordItem.getFeatures().getObject("phones");
                    t.setAttribute("ph", phoneArray2phoneString(allophoneSet, phones));
                }
                if (tokenFeatureSet.isPresent("accent")) {
                    t.setAttribute("accent", tokenFeatureSet.getString("accent"));
                }
                FeatureSet wordFeatureSet = wordItem.getFeatures();
                if (wordFeatureSet.isPresent("pos"))
                    t.setAttribute("pos", wordFeatureSet.getString("pos"));
            }
            // Any syllables?
            Item sylStruct = tokenDaughter.getItemAs("SylStructure");
            if (sylStruct != null && sylStruct.hasDaughters()) {
                Item syllableItem = sylStruct.getDaughter();
                while (syllableItem != null) {
                    if (sampa.length() > 0)
                        sampa.append(" - ");
                    sampa.append(insertSyllable(syllableItem, t, deep));
                    syllableItem = syllableItem.getNext();
                }
            }
            if (sampa.length() > 0)
                t.setAttribute("ph", sampa.toString());
            tokenDaughter = tokenDaughter.getNext();
        }
        // Any marks after the word but before the punctuation?
        if (tokenItem.getFeatures().isPresent("prePuncMarks")) {
            String marks = tokenItem.getFeatures().getString("prePuncMarks");
            StringTokenizer markTok = new StringTokenizer(marks, ",");
            while (markTok.hasMoreTokens()) {
                String markStr = markTok.nextToken();
                Element markEl = MaryXML.createElement(doc, MaryXML.MARK);
                markEl.setAttribute("name", markStr);
                insertHere.appendChild(markEl);
            }
        }
        // Any punctuation after the word?
        if (tokenItem.getFeatures().isPresent("punc")) {
            String puncString = tokenItem.getFeatures().getString("punc");
            if (!puncString.equals("")) {
                Element punctuation = MaryXML.createElement(doc, MaryXML.TOKEN);
                MaryDomUtils.setTokenText(punctuation, puncString);
                String pos = null;
                if (puncString.equals(","))
                    pos = "$,";
                else
                    pos = "$PUNCT";
                punctuation.setAttribute("pos", pos);
                parent.appendChild(punctuation);
            }
        }
        // Any marks after the word?
        if (tokenItem.getFeatures().isPresent("followingMarks")) {
            String marks = tokenItem.getFeatures().getString("followingMarks");
            StringTokenizer markTok = new StringTokenizer(marks, ",");
            while (markTok.hasMoreTokens()) {
                String markStr = markTok.nextToken();
                Element markEl = MaryXML.createElement(doc, MaryXML.MARK);
                markEl.setAttribute("name", markStr);
                insertHere.appendChild(markEl);
            }
        }
        // Any boundary after the word?
        if (tokenItemHasFollowingBoundary(tokenItem)) {
            Element boundary = MaryXML.createElement(doc, MaryXML.BOUNDARY);
            insertHere.appendChild(boundary);
            if (tokenItem.getFeatures().isPresent("followingBoundaryTone"))
                boundary.setAttribute("tone", tokenItem.getFeatures().getString("followingBoundaryTone"));
            
            int breakindex = 0;
            if (tokenItem.getFeatures().isPresent("followingBoundaryBreakindex")) {
                String breakindexString = tokenItem.getFeatures().getString("followingBoundaryBreakindex");
                boundary.setAttribute("breakindex", breakindexString);
                try {
                    breakindex = Integer.parseInt(breakindexString);
                } catch (NumberFormatException nfe) {}
            }
            
            if (tokenItem.getFeatures().isPresent("followingBoundaryDuration"))
                boundary.setAttribute("duration", tokenItem.getFeatures().getString("followingBoundaryDuration"));
            else { // estimate reasonable duration values based on the break index
                if (breakindex >= 4) {
                    boundary.setAttribute("duration", "400");
                } else if (breakindex == 3) {
                    boundary.setAttribute("duration", "200");
                } // and no duration for boundaries with bi < 3
            }
        }

    }

    /**
     * @param tokenItem
     * @return
     */
    private boolean tokenItemHasFollowingBoundary(Item tokenItem)
    {
        assert tokenItem.getOwnerRelation().getName().equals(Relation.TOKEN);
        return tokenItem.getFeatures().isPresent("followingBoundaryTone")
                || tokenItem.getFeatures().isPresent("followingBoundaryBreakindex")
                || tokenItem.getFeatures().isPresent("followingBoundaryDuration");
    }

    /**
     * Convert an item in the Syllable relation into XML, inserting it at the
     * specified location in the XML tree.
     * @param deep whether to create a deep structure of <syllable> and <ph> elements or not.
     */
    protected String insertSyllable(Item syllableItem, Element token, boolean deep) {
        if (syllableItem == null || token == null) {
            throw new NullPointerException("Null arguments to insertSyllable()");
        }
        if (!token.getTagName().equals(MaryXML.TOKEN)) {
            throw new IllegalArgumentException("Syllables can only be inserted in <t> elements");
        }
        Document doc = token.getOwnerDocument();
        Element syllable = null;
        StringBuilder sampa = new StringBuilder();
        if (deep) {
            syllable = MaryXML.createElement(doc, MaryXML.SYLLABLE);
            token.appendChild(syllable);
        }
        if (syllableItem.getFeatures().isPresent("accent")) {
            String accentString = syllableItem.getFeatures().getString("accent");
            if (deep)
                syllable.setAttribute("accent", accentString);
            token.setAttribute("accent", accentString);
        }
        if (syllableItem.getFeatures().isPresent("stress")) {
            String stressString = syllableItem.getFeatures().getString("stress");
            if (!stressString.equals("0")) {
                if (deep)
                    syllable.setAttribute("stress", stressString);
                if (stressString.equals("1"))
                    sampa.append("'");
                else if (stressString.equals("2"))
                    sampa.append(",");
            }
        }
        // Any segments?
        Item segmentItem = syllableItem.getDaughter();
        while (segmentItem != null) {
            if (sampa.length() > 0) sampa.append(" ");
            sampa.append(insertSegment(segmentItem, syllable, deep));
            segmentItem = segmentItem.getNext();
        }
        String sampaString = sampa.toString();
        if (deep)
            syllable.setAttribute("ph", sampaString);
        // Any boundary?
        if (syllableItem.getFeatures().isPresent("endtone")
            && !tokenItemHasFollowingBoundary(syllableItem.getParent().getItemAs(Relation.TOKEN).getParent())) {
            String endtone = syllableItem.getFeatures().getString("endtone");
            if (!endtone.equals("")) {
                Element boundary = MaryXML.createElement(doc, MaryXML.BOUNDARY);
                boundary.setAttribute("tone", endtone);
                boundary.setAttribute("breakindex", "4");
                boundary.setAttribute("duration", "200");
                // And the boundary comes after the current token:
                token.getParentNode().appendChild(boundary);
            }
        }
        return sampaString;
    }

    /**
     * Convert an item in the Segment relation into XML, inserting it at the
     * specified location in the XML tree.
     * @param deep whether to create a deep structure of <syllable> and <ph> elements or not.
     */
    protected String insertSegment(Item segmentItem, Element syllable, boolean deep) {
        // allow for syllable == null if not deep:
        if (segmentItem == null || deep && syllable == null) {
            throw new NullPointerException("Null arguments to insertSegment()");
        }
        if (deep && !syllable.getTagName().equals(MaryXML.SYLLABLE)) {
            throw new IllegalArgumentException("Segments can only be inserted in <syllable> elements");
        }
        String segmentString = segmentItem.toString();
        Voice maryVoice = FreeTTSVoices.getMaryVoice(segmentItem.getUtterance().getVoice());
        if (deep) {
            Document doc = syllable.getOwnerDocument();
            Element segment = MaryXML.createElement(doc, MaryXML.PHONE);
            syllable.appendChild(segment);
            segment.setAttribute("p", segmentString);
            if (segmentItem.getFeatures().isPresent("end")) {
                float endInSeconds = segmentItem.getFeatures().getFloat("end");
                int endInMillis = (int) (1000 * endInSeconds);
                segment.setAttribute("end", String.valueOf(endInMillis));
            }
            if (segmentItem.getFeatures().isPresent("mbr_dur")) {
                int mbrDur = segmentItem.getFeatures().getInt("mbr_dur");
                segment.setAttribute("d", String.valueOf(mbrDur));
            }
            if (segmentItem.getFeatures().isPresent("mbr_targets")) {
                String mbrTargets = segmentItem.getFeatures().getString("mbr_targets");
                if (!mbrTargets.equals("")) {
                    segment.setAttribute("f0", mbrTargets);
                }
            }
        }
        return segmentString;
    }

    /**
       * For a given utterance or token, see if there are any prosodic settings defined,
       * and if so, create a corresponding prosody element as a child of insertHere.
       * @param featureSet an utterance, optionally containing prosody settings 
       * @param insertHere an element into which to insert the new prosody element if required.
       * @return the new prosody element, or null if none was created.
       */
    protected Element insertProsodySettings(Element insertHere, FeatureSet featureSet) {
        if (insertHere == null || featureSet == null)
            throw new NullPointerException("I thoroughly dislike getting null arguments!");
        boolean haveProsodyInfo = false;
        for (String att : XML2UttBase.PROSODY_ATTRIBUTES) {
            if (featureSet.getString(att) != null) {
                haveProsodyInfo = true;
                break;
            }
        }
        if (!haveProsodyInfo) {
            return null;
        }
        Document doc = insertHere.getOwnerDocument();
        Element prosody = MaryXML.createElement(doc, MaryXML.PROSODY);
        insertHere.appendChild(prosody);
        for (String att : XML2UttBase.PROSODY_ATTRIBUTES) {
            String val = featureSet.getString(att);
            if (val != null) {
                prosody.setAttribute(att, val);
            }
        }
        return prosody;
    }

    /** Converts an array of phone symbol strings 
     *  into a single phone string.
     * If stress is marked on input phone symbols ("1" appended), a crude
     * syllabification is done on the phone string.
     */
    public String phoneArray2phoneString(AllophoneSet allophoneSet, String[] voicePhones)
    {
        StringBuilder phoneBuf = new StringBuilder();
        for (int i=0; i<voicePhones.length; i++) {
            phoneBuf.append(voicePhones[i]);
        }
        Syllabifier syllabifier = new Syllabifier(allophoneSet);
        return syllabifier.syllabify(phoneBuf.toString());
    }

    
}

