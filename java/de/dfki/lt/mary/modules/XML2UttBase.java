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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.log4j.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

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
import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.mary.util.dom.DomUtils;
import de.dfki.lt.mary.util.dom.MaryDomUtils;
import de.dfki.lt.mary.util.dom.NameNodeFilter;

/**
 * Convert a MaryXML DOM tree into FreeTTS utterances.
 * This abstract base class is to provide the common part for all
 * MaryXML to Utterance converters.
 *
 * @author Marc Schr&ouml;der
 */

public abstract class XML2UttBase extends InternalModule
{

    protected int lastTargetIndex = 0;
    protected int nextTargetIndex = 1;
    
    
    public XML2UttBase(String name, MaryDataType input, MaryDataType output)
    {
        super(name, input, output);
    }



    public void startup() throws Exception
    {
        super.startup();
        // Initialise FreeTTS
        FreeTTSVoices.load();
    }

    public MaryData process(MaryData d)
    throws Exception
    {
        Document doc = d.getDocument();

        if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
            logger.debug("Converting the following XML structure into Utterances:");
            MaryNormalisedWriter mnw = new MaryNormalisedWriter();
            ByteArrayOutputStream debugOut = new ByteArrayOutputStream();
            mnw.output(doc, debugOut);
            logger.debug(debugOut.toString());
       }

        List utterances = new ArrayList();
        NodeIterator sentenceIt = ((DocumentTraversal)doc).
            createNodeIterator(doc, NodeFilter.SHOW_ELEMENT,
                               new NameNodeFilter(MaryXML.SENTENCE), false);
        Element sentence = null;
        while ((sentence = (Element) sentenceIt.nextNode()) != null) {
            // Make sure we have the correct voice:
            Element voice = (Element) MaryDomUtils.getAncestor(sentence, MaryXML.VOICE);
            Voice maryVoice = Voice.getVoice(voice);
            if (maryVoice == null) {                
                maryVoice = d.getDefaultVoice();
            }
            if (maryVoice == null) {
                // Determine Locale in order to use default voice
                Locale locale = MaryUtils.string2locale(doc.getDocumentElement().getAttribute("xml:lang"));
                maryVoice = Voice.getDefaultVoice(locale);
                assert maryVoice != null;
            }
            com.sun.speech.freetts.Voice freettsVoice =
                FreeTTSVoices.getFreeTTSVoice(maryVoice);
            if (freettsVoice == null) {
                throw new NullPointerException("No FreeTTS voice for mary voice " + maryVoice.getName());
            }
            Utterance utterance = new Utterance(freettsVoice);
            if (voice != null && voice.hasAttribute("style")) {
                String style = voice.getAttribute("style");
                utterance.setString("style", style);
            }
            // Any prosodic settings present?
            insertProsodySettings(utterance, sentence);

            fillUtterance(utterance, sentence);
            utterances.add(utterance);
            if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                utterance.dump(pw, 2, name(), true); // padding, justRelations
                logger.debug("Constructed the following Utterance:");
                logger.debug(sw.toString());
            }

        }

        if (utterances.size() > 0) {
            Utterance firstUtt = (Utterance) utterances.get(0);
            firstUtt.setFirst(true);
            Utterance lastUtt = (Utterance) utterances.get(utterances.size()-1);
            lastUtt.setLast(true);
        }

        MaryData output = new MaryData(outputType());
        output.setUtterances(utterances);
        return output;
    }

    /**
     * Depending on the data type, find the right information in the sentence
     * and insert it into the utterance.
     */
    protected abstract void fillUtterance(Utterance utterance, Element sentence);
    /**
     * Depending on the data type, find the right information in the sentence
     * and insert it into the utterance.
     */
    protected void fillUtterance(Utterance utterance, Element sentence,
                                 boolean createWordRelation,
                                 boolean createSylStructRelation,
                                 boolean createTargetRelation)
    {
        Document doc = sentence.getOwnerDocument();
        boolean isLastSentence = DomUtils.isLastOfItsKindIn(sentence, doc);
        Relation tokenRelation = utterance.createRelation(Relation.TOKEN);
        Relation phraseRelation = null;
        Relation segmentRelation = null;
        Relation targetRelation = null;
        if (createWordRelation)
            utterance.createRelation(Relation.WORD);
        if (createSylStructRelation) {
            utterance.createRelation(Relation.SYLLABLE_STRUCTURE);
            utterance.createRelation(Relation.SYLLABLE);
            segmentRelation = utterance.createRelation(Relation.SEGMENT);
        }
        if (createTargetRelation)
            targetRelation = utterance.createRelation(Relation.TARGET);
        NodeIterator phraseIt = ((DocumentTraversal)doc).createNodeIterator
            (sentence, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(MaryXML.PHRASE), false);
        Element topElement = (Element) phraseIt.nextNode();
        if (topElement != null) { // we have phrases
            phraseRelation = utterance.createRelation(Relation.PHRASE);
        } else { // we have no phrases
            topElement = sentence;
        }

        StringBuffer sentenceBuf = new StringBuffer();
        // Either iterate through phrases or run once through entire sentence:
        while (topElement != null) {
            NodeIterator tokenIt = ((DocumentTraversal)doc).createNodeIterator
                (topElement, NodeFilter.SHOW_ELEMENT,
                 new NameNodeFilter(new String[]{MaryXML.MTU, MaryXML.TOKEN, MaryXML.BOUNDARY}),
                 false);
            Element element = null;
            while ((element = (Element) tokenIt.nextNode()) != null) {
                String elementText = addOneElement(utterance,
                        element,
                        createWordRelation,
                        createSylStructRelation,
                        createTargetRelation);
                assert elementText != null;
                if (elementText.length() > 0 && sentenceBuf.length() > 0)
                    sentenceBuf.append(" ");
                sentenceBuf.append(elementText);
                if (element.getTagName().equals(MaryXML.MTU)) {
                    // skip enclosed tokens
                    Element mtu = element;
                    while ((element = (Element) tokenIt.nextNode()) != null
                    && (element.getTagName().equals(MaryXML.TOKEN)
                            || element.getTagName().equals(MaryXML.MTU))
                    && MaryDomUtils.isAncestor(mtu, element)) {
                      // do nothing, move on
                    }
                    // Now if we have not reached the end (element==null),
                    // we went one step too far with tokenIt -- go one
                    // step back:
                    if (element != null)
                        tokenIt.previousNode();
                }

            }
            topElement = (Element) phraseIt.nextNode(); // or null
        }
        // Any marks after the last token in last sentence?
        if (isLastSentence) {
            Element lastTokenElement = DomUtils.getLastElementByTagName(sentence, MaryXML.TOKEN);
            String finalMarks = searchFollowingMarks(lastTokenElement);
            if (finalMarks != null) {
                Item lastToken = tokenRelation.getTail();
                if (lastToken != null) {
                    if (lastToken.getFeatures().isPresent("followingMarks")) {
                        finalMarks = lastToken.getFeatures().getString("followingMarks") + "," + finalMarks;
                    }
                    lastToken.getFeatures().setString("followingMarks", finalMarks);
                }
            }
        }
        // Check if default name needs to be added to last phrase item:
        if (phraseRelation != null) {
            Item phraseItem = phraseRelation.getTail();
            if (phraseItem != null && !phraseItem.getFeatures().isPresent("name")) {
                phraseItem.getFeatures().setString("name", "BB");
            }
        }
        utterance.setString("input_text", sentenceBuf.toString());
        // Append a last target at the very end of the utterance, because
        // the FreeTTS diphone code only creates audio up to the last target:
        if (createTargetRelation && createSylStructRelation) {
            assert segmentRelation != null;
            Item lastSegment = segmentRelation.getTail();
            if (lastSegment != null && lastSegment.getFeatures().isPresent("end")) {
                lastTargetIndex++;
                nextTargetIndex++;
                float pos = lastSegment.getFeatures().getFloat("end");
                float f0;
                Item lastTarget = targetRelation.getTail();
                if (lastTarget != null) f0 = lastTarget.getFeatures().getFloat("f0");
                else f0 = 100;
                Item finalTarget = targetRelation.appendItem();
                finalTarget.getFeatures().setFloat("pos", pos);
                finalTarget.getFeatures().setFloat("f0", f0);
            }
            
            if (!lastSegment.getFeatures().isPresent("lastTarget")){
            	lastSegment.getFeatures().setInt("lastTarget",lastTargetIndex);
                lastSegment.getFeatures().setInt("nextTarget",nextTargetIndex);
            }
        }
        
    }


    /**
     * To a given utterance in which relations have already been created appropriately,
     * add a single boundary, t or mtu element.
     * @param utterance
     * @param element
     * @param createWordRelation
     * @param createSylStructRelation
     * @param createTargetRelation
     * @return a string containing the text in this element, or an empty string 
     */
    protected String addOneElement(Utterance utterance, Element element,
        boolean createWordRelation,
        boolean createSylStructRelation,
        boolean createTargetRelation)
         
    {
        Voice maryVoice = FreeTTSVoices.getMaryVoice(utterance.getVoice());
        StringBuffer sentenceBuf = new StringBuffer();
        List phonemeList = null;
        Relation tokenRelation = utterance.getRelation(Relation.TOKEN);
        assert tokenRelation != null;
        Relation wordRelation = null;
        if (createWordRelation) {
            wordRelation = utterance.getRelation(Relation.WORD);
            assert wordRelation != null;
        }
        Relation phraseRelation = utterance.getRelation(Relation.PHRASE);
        // For the phrase relation, phrase items will be created by insertWordItem
        // if required. A phrase item is marked "full" by giving it a name;
        // this happens when a boundary is found (its breakindex
        // is used to determine the name: bi4=BB, bi3=B) or when the sentence is
        // finished (BB).
        
        if (element.getTagName().equals(MaryXML.BOUNDARY)) {
            if (phraseRelation != null) {
                Item phraseItem = phraseRelation.getTail();
                if (phraseItem != null) {
                //what about 5 and 6 ?
                    if (element.getAttribute("breakindex").equals("4")) {
                        phraseItem.getFeatures().setString("name", "BB"); // big break
                    } else if (element.getAttribute("breakindex").equals("3")) {
                        phraseItem.getFeatures().setString("name", "B"); // break
                    }
                }
            }
            if (createSylStructRelation) {
                Relation syllableRelation = utterance.getRelation(Relation.SYLLABLE);
                assert syllableRelation != null;
                Item syllableItem = syllableRelation.getTail();
                if (syllableItem != null) {
                    syllableItem.getFeatures().setString
                        ("endtone", element.getAttribute("tone"));
                }
                if (element.hasAttribute("duration")) {
                    int dur = 0;
                    try {
                        dur = Integer.parseInt(element.getAttribute("duration"));
                    } catch (NumberFormatException nfe) {}
                    if (dur > 0) {
                        Relation segmentRelation = utterance.getRelation(Relation.SEGMENT);
                        assert segmentRelation != null;
                        Item segTail = segmentRelation.getTail();
                        float prevEnd = 0f;
                        if (segTail != null && segTail.getFeatures().isPresent("end")) {
                            prevEnd = segTail.getFeatures().getFloat("end");
                        }
                        float end = prevEnd + dur * 0.001f;
                        Item segItem = segmentRelation.appendItem();
                        segItem.getFeatures().setObject("maryxmlElement", element);
                        // Silence symbol in voice-specific phonetic alphabet:
                        String silence = maryVoice.sampa2voice("_");
                        assert silence != null;
                        logger.debug("In voice '"+maryVoice.getName()+"', voice silence symbol is '"+silence+"'");
                        segItem.getFeatures().setString("name", silence);
                        segItem.getFeatures().setInt("mbr_dur", dur);
                        segItem.getFeatures().setFloat("end", end);
                        segItem.getFeatures().setInt("lastTarget",lastTargetIndex);
                        segItem.getFeatures().setInt("nextTarget",nextTargetIndex);
                    }
                }                
            }
            // Also remember boundary in Token!
            Item token = tokenRelation.getTail();
            String where;
            if (token == null) {
                where = "preceding";
                // create a "placeholder" token item:
                token = appendTokenItem(tokenRelation, null);
            } else {
                where = "following";
            }
            if (element.hasAttribute("tone")) {
                token.getFeatures().setString(where+"BoundaryTone", element.getAttribute("tone"));
            }
            if (element.hasAttribute("breakindex")) {
                token.getFeatures().setString(where+"BoundaryBreakindex", element.getAttribute("breakindex"));
            }
            if (element.hasAttribute("duration")) {
                token.getFeatures().setString(where+"BoundaryDuration", element.getAttribute("duration"));
            }
        } else if (element.getTagName().equals(MaryXML.MTU)) {
            Element mtu = element;
            String mark = searchPrecedingMarks(mtu, new String[] {MaryXML.TOKEN});
            String tokenText = mtu.getAttribute("orig");
            if (sentenceBuf.length() > 0) sentenceBuf.append(" ");
            sentenceBuf.append(tokenText);
            Item tokenItem = appendTokenItem(tokenRelation, tokenText);
            if (mark != null)
                tokenItem.getFeatures().setString("precedingMarks", mark);
            if (createWordRelation) {
                // Now the following elements returned by tokenIt are
                // the children of the <mtu> element:
                NodeList tokens = mtu.getElementsByTagName(MaryXML.TOKEN);
                for (int i=0; i<tokens.getLength(); i++) {
                    Element t = (Element) tokens.item(i);
                    if (!createSylStructRelation &&
                        t.hasAttribute("sampa")) {
                        // If any of the <t>s inside an <mtu> have a
                        // sampa attribute, the concatenated sampa
                        // attribute values will be used as the
                        // pronunciation of the entire <mtu>.
                        List onePhonemeList = maryVoice.sampaString2voicePhonemeList(t.getAttribute("sampa"));
                        if (phonemeList == null) {
                            phonemeList = onePhonemeList;
                        } else {
                            phonemeList.addAll(onePhonemeList);
                        }
                    }
                    // Each t corresponds to one word item
                    Item wordItem = insertWordItem(wordRelation, phraseRelation, tokenItem, t);
                    if (createSylStructRelation)
                        createSylStructure(wordItem, t, createTargetRelation);
                }
            }
        } else { // a plain <t> element
            Element t = element;
            String mark = searchPrecedingMarks(t);
            String tokenText = MaryDomUtils.tokenText(t);
            String pos = "";
            if (t.hasAttribute("pos")){
            	pos = t.getAttribute("pos");
            	}
            	//why is ( and ) not in here?
            if (tokenText.matches("[.,!?:;]+") ||
            	pos.equals("punc")) { // it's a punctuation
                sentenceBuf.append(tokenText);
                Item lastToken = tokenRelation.getTail();
                if (lastToken != null) {
                    lastToken.getFeatures().setString("punc", tokenText);
                    if (mark != null) {
                        lastToken.getFeatures().setString("prePuncMarks", mark);
                    }
                }
            } else { // not a punctuation
                if (sentenceBuf.length() > 0) sentenceBuf.append(" ");
                sentenceBuf.append(tokenText);
                Item tokenItem = appendTokenItem(tokenRelation, tokenText);
                if (mark != null)
                    tokenItem.getFeatures().setString("precedingMarks", mark);
                if (!createSylStructRelation) {
                    if (t.hasAttribute("sampa")) {
                        phonemeList = maryVoice.sampaString2voicePhonemeList(t.getAttribute("sampa"));
                    }
                    if (t.hasAttribute("accent")) {
                        tokenItem.getFeatures().setString("accent", t.getAttribute("accent"));
                    }
                }
                if (createWordRelation) {
                    Item wordItem = insertWordItem(wordRelation, phraseRelation, tokenItem, t);
                    if (createSylStructRelation)
                        createSylStructure(wordItem, t, createTargetRelation);
                }
            }
        }
        if (phonemeList != null) {
            String[] phonemeArray = (String[]) phonemeList.toArray(new String[]{});
            tokenRelation.getTail().getFeatures().setObject
                ("phones", phonemeArray);
            phonemeList = null;
        }
        return sentenceBuf.toString();
    }

    /**
     * Append one item to the token relation, and return it.
     * If tokenText is null, a "placeholder" token item will be created,
     * in which no "name" feature is present, but to which other features
     * can already be added. If the tail of tokenRelation is such a placeholder
     * token, the tokenText will be set as that placeholder's name feature,
     * and the ex-placeholder item will be returned. If the tail of tokenRelation is
     * a placeholder and tokenText is null, no new placeholder will be created, i.e.
     * it is not possible to create two subsequent placeholder items.
     * @param tokenRelation
     * @param tokenText
     * @return the new item
     */
    protected Item appendTokenItem(Relation tokenRelation, String tokenText)
    {
        Item tokenItem;
        Item tail = tokenRelation.getTail();
        if (tail != null && !tail.getFeatures().isPresent("name")) {
            tokenItem = tail;
        } else {
            tokenItem = tokenRelation.appendItem();
        }
        assert tokenItem != null;
        FeatureSet tokenFeatureSet = tokenItem.getFeatures();
        if (tokenText != null)
            tokenFeatureSet.setString("name", tokenText);
        tokenFeatureSet.setString("whitespace", " ");
        tokenFeatureSet.setString("punc", "");
        return tokenItem;
    }

    protected Item insertWordItem(Relation wordRelation, Relation phraseRelation,
                                  Item tokenItem,
                                  Element wordElement)
    {
        Item wordItem = tokenItem.createDaughter();
        wordRelation.appendItem(wordItem);
        FeatureSet wordFeatureSet = wordItem.getFeatures();
        wordFeatureSet.setString
            ("name", MaryDomUtils.tokenText(wordElement).toLowerCase());
        if (wordElement.hasAttribute("pos"))
            wordFeatureSet.setString("pos", wordElement.getAttribute("pos"));

        if (phraseRelation != null) {
            Item phraseItem = phraseRelation.getTail();
            // Words can only be added to phrase items that don't have a name yet:
            if (phraseItem == null || phraseItem.getFeatures().isPresent("name")) {
                // need to add a new phrase item
                phraseItem = phraseRelation.appendItem();
            }
            phraseItem.addDaughter(wordItem);
        }
        return wordItem;
    }

    protected Item createSylStructure(Item wordItem, Element t, boolean createTargetRelation)
    {
        Utterance utt = wordItem.getOwnerRelation().getUtterance();
        Voice maryVoice = FreeTTSVoices.getMaryVoice(utt.getVoice());
        assert utt != null;
        Relation sylRelation = utt.getRelation(Relation.SYLLABLE);
        Relation sylStructRelation = utt.getRelation(Relation.SYLLABLE_STRUCTURE);
        Relation segRelation = utt.getRelation(Relation.SEGMENT);
        Relation targetRelation = null;
        assert sylRelation != null;
        assert sylStructRelation != null;
        assert segRelation != null;
        if (createTargetRelation) {
            targetRelation = utt.getRelation(Relation.TARGET);
            assert targetRelation != null;
        }

        Item sylStructWordItem = sylStructRelation.appendItem(wordItem);
        float totalDur = 0f; // in case we have duration info, in seconds
        // update this value from already-existing segments:
        Item segTail = segRelation.getTail(); 
        if (segTail != null && segTail.getFeatures().isPresent("end")) {
            float end = segTail.getFeatures().getFloat("end");
            if (end != 0f) {
                totalDur = end;
            }
        }

        Document doc = t.getOwnerDocument();
        NodeIterator sylIt = ((DocumentTraversal)doc).createNodeIterator
            (t, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(MaryXML.SYLLABLE), false);
        Element sylElement = null;
        
        while ((sylElement = (Element) sylIt.nextNode()) != null) {
            Item sylItem = sylRelation.appendItem();
            Item sylStructSylItem = sylStructWordItem.addDaughter(sylItem);
            String stress = sylElement.getAttribute("stress");
            if (stress.equals("")) stress = "0";
            sylStructSylItem.getFeatures().setString("stress", stress);
            if (sylElement.hasAttribute("accent"))
                sylStructSylItem.getFeatures().setString
                    ("accent", sylElement.getAttribute("accent"));
            NodeIterator segIt = ((DocumentTraversal)doc).createNodeIterator
                (sylElement, NodeFilter.SHOW_ELEMENT,
                 new NameNodeFilter(MaryXML.PHONE), false);
            Element segElement = null;
            while ((segElement = (Element) segIt.nextNode()) != null) {
                Item segItem = segRelation.appendItem();
                segItem.getFeatures().setObject("maryxmlElement", segElement);
                sylStructSylItem.addDaughter(segItem);
                String sampa = segElement.getAttribute("p");
                assert !sampa.equals("");
                // Convert to voice-specific phonetic alphabet
                String phoneSymbol = maryVoice.sampa2voice(sampa);
                assert phoneSymbol != null;
                segItem.getFeatures().setString("name", phoneSymbol);
                int dur = 0; // in case we have duration info, in millisecs
                if (segElement.hasAttribute("d")) {
                    try {
                        dur = Integer.parseInt(segElement.getAttribute("d"));
                    } catch (NumberFormatException nfe) {
                        logger.debug("Cannot parse duration attribute `" +
                                     segElement.getAttribute("d") +
                                     "' as integer.",
                                     nfe);
                    }
                    segItem.getFeatures().setInt("mbr_dur", dur);
                    totalDur += dur * 0.001f;
                    segItem.getFeatures().setFloat("end", totalDur);
                    
                }
                String mbrTargets = null;
                if (segElement.hasAttribute("f0")) {
                    mbrTargets = segElement.getAttribute("f0");
                    segItem.getFeatures().setString("mbr_targets", mbrTargets);
                    lastTargetIndex++;
                    nextTargetIndex++;
                }
                segItem.getFeatures().setInt("lastTarget",lastTargetIndex);
                segItem.getFeatures().setInt("nextTarget",nextTargetIndex);
                if (createTargetRelation && dur != 0 && mbrTargets != null) {
                    // mbrTargets contains one or more pairs of numbers, 
                    // either enclosed by (a,b) or just separated by whitespace.
                    StringTokenizer st = new StringTokenizer(mbrTargets, " (,)");
                    while (st.hasMoreTokens()) {
                        String posString = "";
                        while (st.hasMoreTokens() && posString.equals("")) posString = st.nextToken();
                        String f0String = "";
                        while (st.hasMoreTokens() && f0String.equals("")) f0String = st.nextToken();      
                        
                        float pos = totalDur - (1 - Float.parseFloat(posString) * 0.01f) * ((float) dur)*0.001f; 
                        float f0 = Float.parseFloat(f0String);
                        Item item = targetRelation.appendItem();
                        item.getFeatures().setFloat("pos", pos);
                        if (f0 > 500.0) { 
                            item.getFeatures().setFloat("f0", 500.0f);
                        } else if (f0 < 50.0)  {
                            item.getFeatures().setFloat("f0", 50.0f);
                        } else {
                            item.getFeatures().setFloat("f0", f0);
                        }
                    }
                }
            }
        }
        return sylStructWordItem;
    }

    /**
      * For a given element, extract essential settings defined by the closest ancestor
      * prosody element and save them into the Utterance.
      * @param utterance an utterance in which to save the prosody settings 
      * @param element an element somewhere below the prosody element to convert.
      */
     private void insertProsodySettings(Utterance utterance, Element element)
     {
         Element prosody = (Element) MaryDomUtils.getAncestor(element, MaryXML.PROSODY);
         Element voice = (Element) MaryDomUtils.getAncestor(element, MaryXML.VOICE);
         if (prosody == null || voice != null && MaryDomUtils.isAncestor(prosody, voice))
             return;
         String rateString = prosody.getAttribute("rate");
         String pitchString = prosody.getAttribute("pitch");
        String rangeString = prosody.getAttribute("range");
        String volumeString = prosody.getAttribute("volume");
        if (!rateString.equals("")) utterance.setString("rate", rateString);
        if (!pitchString.equals("")) utterance.setString("pitch", pitchString);
        if (!rangeString.equals("")) utterance.setString("range", rangeString);
        if (!volumeString.equals("")) utterance.setString("volume", volumeString);
     }

    /**
     * Search backwards for <code>mark</code> elements
     * that occur between the given element and the preceding (if any) element
     * with the same tagname. 
     * @param element
     * @return A String containing a comma-separated list of all marks found
     * between the given element and the preceding element with the same name,
     * or null if no mark was found.
     */
    protected String searchPrecedingMarks(Element element)
    {
        return searchPrecedingMarks(element, new String[] {element.getTagName()});
    }
    /**
     * Search backwards for <code>mark</code> elements
     * that occur between the given element and the preceding (if any) element
     * with one of the given tagnames. 
     * @param element
     * @param tagnames
     * @return A String containing a comma-separated list of all marks found
     * between the given element and the preceding element with the same name,
     * or null if no mark was found.
     */
    protected String searchPrecedingMarks(Element element, String[] tagnames)
    {
        Document doc = element.getOwnerDocument();
        String [] searchTagnames = new String[tagnames.length + 1];
        searchTagnames[0] = MaryXML.MARK;
        System.arraycopy(tagnames, 0, searchTagnames, 1, tagnames.length); 
        TreeWalker tw = ((DocumentTraversal)doc).
            createTreeWalker(doc.getDocumentElement(), NodeFilter.SHOW_ELEMENT,
            new NameNodeFilter(searchTagnames), false);
        tw.setCurrentNode(element);
        Element prev;
        String mark = null;
        while ((prev = (Element) tw.previousNode()) != null &&
               prev.getTagName().equals(MaryXML.MARK)) {
            if (mark == null) mark = prev.getAttribute("name");
            else mark = prev.getAttribute("name") + "," + mark;
        }
        return mark;
    }

    /**
     * Search forewards for <code>mark</code> elements
     * that occur between the given element and the preceding (if any) element
     * with the same tagname. 
     * @param element
     * @return A String containing a comma-separated list of all marks found
     * between the given element and the preceding element with the same name,
     * or null if no mark was found.
     */
    protected String searchFollowingMarks(Element element)
    {
        return searchFollowingMarks(element, new String[] {element.getTagName()});
    }
    /**
     * Search forewards for <code>mark</code> elements
     * that occur between the given element and the preceding (if any) element
     * with one of the given tagnames. 
     * @param element
     * @param tagnames
     * @return A String containing a comma-separated list of all marks found
     * between the given element and the preceding element with the same name,
     * or null if no mark was found.
     */
    protected String searchFollowingMarks(Element element, String[] tagnames)
    {
        Document doc = element.getOwnerDocument();
        String [] searchTagnames = new String[tagnames.length + 1];
        searchTagnames[0] = MaryXML.MARK;
        System.arraycopy(tagnames, 0, searchTagnames, 1, tagnames.length); 
        TreeWalker tw = ((DocumentTraversal)doc).
            createTreeWalker(doc.getDocumentElement(), NodeFilter.SHOW_ELEMENT,
            new NameNodeFilter(searchTagnames), false);
        tw.setCurrentNode(element);
        Element prev;
        String mark = null;
        while ((prev = (Element) tw.nextNode()) != null &&
               prev.getTagName().equals(MaryXML.MARK)) {
            if (mark == null) mark = prev.getAttribute("name");
            else mark = mark+ "," + prev.getAttribute("name");
        }
        return mark;
    }

}
