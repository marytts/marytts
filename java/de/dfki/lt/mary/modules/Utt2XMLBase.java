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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.speech.freetts.FeatureSet;
import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.util.MaryNormalisedWriter;
import de.dfki.lt.mary.util.dom.MaryDomUtils;

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

    public Utt2XMLBase(String name, MaryDataType input, MaryDataType output) {
        super(name, input, output);
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
        Element insertHere = doc.getDocumentElement();
        Locale locale = outputType().getLocale();
        if (locale != null) {
            insertHere.setAttribute("xml:lang", locale.getLanguage());
        }
        insertHere = MaryXML.appendChildElement(insertHere, MaryXML.PARAGRAPH);

        List utterances = d.getUtterances();
        Iterator it = utterances.iterator();
        while (it.hasNext()) {
            Utterance utterance = (Utterance) it.next();

            if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                utterance.dump(pw, 2, name(), true); // padding, justRelations
                logger.debug("Converting the following Utterance to XML:\n"+sw.toString());
            }

            // Make sure we have the correct voice:
            de.dfki.lt.mary.modules.synthesis.Voice maryVoice = FreeTTSVoices.getMaryVoice(utterance.getVoice());
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
                // create a new voice element, insert it as a child of this
                // node, and let insertHere point to it
                Element newVoice = MaryXML.createElement(doc, MaryXML.VOICE);
                insertHere.appendChild(newVoice);
                newVoice.setAttribute("name", maryVoice.getName());
                insertHere = newVoice;
            }
            // Now insertHere is the correct <voice> element.

            // Any prosodic settings to insert?
            Element  prosody = insertProsodySettings(insertHere, utterance);

            // Create a sentence element <s> for this utterance:
            Element sentence = MaryXML.createElement(doc, MaryXML.SENTENCE);
            if (prosody != null) prosody.appendChild(sentence);
            else insertHere.appendChild(sentence);

            fillSentence(sentence, utterance);
        }

        if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
            logger.debug("Constructed the following XML structure:");
            MaryNormalisedWriter mnw = new MaryNormalisedWriter();
            ByteArrayOutputStream debugOut = new ByteArrayOutputStream();
            mnw.output(doc, debugOut);
            logger.debug(debugOut.toString());
        }

        MaryData output = new MaryData(outputType());
        output.setDocument(doc);
        return output;
    }

    /**
     * Depending on the data type, find the right information in the utterance
     * and insert it into the sentence.
     */
    protected abstract void fillSentence(Element sentence, Utterance utterance);

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
        Voice maryVoice = FreeTTSVoices.getMaryVoice(tokenItem.getUtterance().getVoice());
        Element insertHere = parent;
        boolean needMtu = false;
        boolean insertPhones = tokenItem.getFeatures().isPresent("phones");
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
            if (insertPhones) {
                String[] phones = (String[]) tokenItem.getFeatures().getObject("phones");
                t.setAttribute("sampa", maryVoice.voicePhonemeArray2sampaString(phones));
                insertPhones = false;
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
            StringBuffer sampa = new StringBuffer();
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
                if (insertPhones) {
                    String[] phones = (String[]) tokenItem.getFeatures().getObject("phones");
                    t.setAttribute("sampa", maryVoice.voicePhonemeArray2sampaString(phones));
                    insertPhones = false;
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
                        sampa.append("-");
                    sampa.append(insertSyllable(syllableItem, t, deep));
                    syllableItem = syllableItem.getNext();
                }
            }
            if (sampa.length() > 0)
                t.setAttribute("sampa", sampa.toString());
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
        StringBuffer sampa = new StringBuffer();
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
            sampa.append(insertSegment(segmentItem, syllable, deep));
            segmentItem = segmentItem.getNext();
        }
        String sampaString = sampa.toString();
        if (deep)
            syllable.setAttribute("sampa", sampaString);
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
        String sampaSegmentString = maryVoice.voice2sampa(segmentString);
        if (deep) {
            Document doc = syllable.getOwnerDocument();
            Element segment = MaryXML.createElement(doc, MaryXML.PHONE);
            syllable.appendChild(segment);
            segment.setAttribute("p", sampaSegmentString);
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
        return sampaSegmentString;
    }

    /**
       * For a given utterance, see if there are any prosodic settings defined,
       * and if so, create a corresponding prosody element as a child of insertHere.
       * @param utterance an utterance, optionally containing prosody settings 
       * @param insertHere an element into which to insert the new prosody element if required.
       * @return the new prosody element, or null if none was created.
       */
    private Element insertProsodySettings(Element insertHere, Utterance utterance) {
        if (insertHere == null || utterance == null)
            throw new NullPointerException("I thoroughly dislike getting null arguments!");
        String rateString = utterance.getString("rate");
        String pitchString = utterance.getString("pitch");
        String rangeString = utterance.getString("range");
        String volumeString = utterance.getString("volume");
        if (rateString == null && pitchString == null && rangeString == null &&
        volumeString == null)
            return insertHere; //no prosodic settings
        Document doc = insertHere.getOwnerDocument();
        Element prosody = MaryXML.createElement(doc, MaryXML.PROSODY);
        insertHere.appendChild(prosody);
        if (rateString != null) prosody.setAttribute("rate", rateString);
        if (pitchString != null) prosody.setAttribute("pitch", pitchString);
        if (rangeString != null) prosody.setAttribute("range", rangeString);
        if (volumeString != null) prosody.setAttribute("volume", volumeString);
        return prosody;
    }

}
