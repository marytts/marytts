/**
 * Copyright 2009 DFKI GmbH.
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
package marytts.language.de;
// JAVA classes
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.fst.FSTLookup;
import marytts.language.de.infostruct.GerNetQuery;
import marytts.modules.InternalModule;
import marytts.server.MaryProperties;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
/**
 * Try to recognise the information structure from chunked text.
 *
 * @author Massimo Romanelli, Marc Schr&ouml;der
 */
@Deprecated
public class InformationStructure extends InternalModule {

    private FSTLookup stemmer;
    private GerNetQuery gerNetQuery;

    public InformationStructure() {
        super("InfoStruct", 
                MaryDataType.PARTSOFSPEECH,
//                GermanDataTypes.INFOSTRUCT_DE,
                MaryDataType.PARTSOFSPEECH,
                Locale.GERMAN);
    }

    public void startup() throws Exception {
    	InputStream stemmerStream = MaryProperties.getStream("de.infostruct.stemmer");
    	if (stemmerStream != null) {
    		stemmer = new FSTLookup(stemmerStream, MaryProperties.getProperty("de.infostruct.stemmer"));
    	}
        gerNetQuery = null;
        if (MaryProperties.getBoolean("de.infostruct.usegermanet"))
            gerNetQuery =
                new GerNetQuery(
                    MaryProperties.needProperty("de.infostruct.germanet.database"),
                    MaryProperties.needProperty("de.infostruct.germanet.user"),
                    MaryProperties.needProperty("de.infostruct.germanet.password"));

        super.startup();
    }

    public MaryData process(MaryData d) throws Exception {
        Document doc = d.getDocument();
        Set givenSet = new HashSet();
        Set contrastSet = new HashSet();
        Element currentParagraph = null;
        NodeList sentences = doc.getElementsByTagName(MaryXML.SENTENCE);
        for (int i = 0; i < sentences.getLength(); i++) {
            Element sentence = (Element) sentences.item(i);
            Element paragraph = (Element) MaryDomUtils.getAncestor(sentence, MaryXML.PARAGRAPH);
            // Are we still in the same paragraph?
            // Test if both are the identical object or both are null:
            if (paragraph != currentParagraph) {
                // Not in the same paragraph anymore; empty the sets:
                givenSet.clear();
                contrastSet.clear();
                currentParagraph = paragraph;
            }
            processSentence(sentence, givenSet, contrastSet, doc);
        } //for
        MaryData result = new MaryData(outputType(), d.getLocale());
        result.setDocument(doc);
        return result;
    }
    private void processSentence(Element sentence, Set givenSet, Set contrastSet, Document doc) {
        Vector contrast = new Vector();
        NodeList list = (NodeList) sentence.getElementsByTagName(MaryXML.TOKEN);
        for (int i = 0; i < list.getLength(); i++) {
            Element token = (Element) list.item(i);
            String grapheme = MaryDomUtils.tokenText(token);
            logger.debug("-----> word: " + grapheme);
            //search for contrast indicators 
            if (grapheme.equals("aber")
                || grapheme.equals("hingegen")
                || grapheme.equals("während")
                || grapheme.equals("sondern")) {
                logger.debug(":::::::::::::contrastWord: " + grapheme);
                contrast = checkContrast(list, i, doc);
                logger.debug(":::::::::::::contrast= " + contrast);
            } // if
            //looking for semantic contrast
            if (isNounOrAdjective(token)) {
                String stem = findStem(grapheme);
                String stemInHistory = stem + "#" + token.getAttribute("pos").substring(0, 1);
                //handle contrast
                if (contrastSet.contains(stemInHistory)) {
                    if (!token.hasAttribute("contrast")) {
                        token.setAttribute("contrast", "+");
                        logger.debug("-----> contrast: " + stemInHistory);
                    } //if	
                } else { //word is not in contrast		
                    if (isNoun(token)) {
                        // For nouns, remember all antonyms and hyponyms
                        contrastNoun(contrastSet, token, grapheme);
                    } else if (isAdjective(token)) {
                        // For adjectives, remember all antonyms
                        contrastAdj(contrastSet, token, grapheme);
                    }
                } //else		
                //handle givenness
                if (givenSet.contains(stemInHistory)) {
                    token.setAttribute("given", "+");
                } else { //word is not given
                    if (isNoun(token)) {
                        // For nouns, remember synonyms and hypernyms
                        givennessNoun(givenSet, token, stemInHistory, grapheme);
                    } else if (isAdjective(token)) {
                        // For adjectives, remember synonyms
                        givennessAdj(givenSet, token, stemInHistory, grapheme);
                    }
                } //else
            } //if
        } //for
    } //processSentence

    /**
     * Search for identical lists of parts-of-speech before and after a given
     * "contrast word", ignoring punctuation tokens.
     * @param list list of tokens
     * @param counter the index of the "contrast word" in the list.
     * @param doc the document to which the tokens belong
     * @return
     */
    private Vector checkContrast(NodeList list, int counter, Document doc) {
        Vector result = new Vector();
        Vector before = new Vector();
        Vector after = new Vector();
        int listLength = list.getLength();
        for (int i = 1, j = 1;
            i<= 3 && i <= counter && i < (listLength - counter)
            && j <= 3 && j <= counter && j < (listLength - counter);
            i++, j++) {
            logger.debug(">>>>>>>>>>>contrast: i: " + i + " j: " + j);
            if (((Element) list.item(counter - i)).getAttribute("pos").equals("$,")
                    && i < counter)
                i = i + 1;
            if (((Element) list.item(counter + j)).getAttribute("pos").equals("$,")
                    && j < (listLength - counter - 1))
                j = j + 1;
            logger.debug(">>>>>>>>>>>contrast++: i: " + i + " j: " + j);
            Element tokenBefore = (Element) list.item(counter - i);
            Element tokenAfter = (Element) list.item(counter + j);
            String posBefore = tokenBefore.getAttribute("pos");
            String posAfter = tokenAfter.getAttribute("pos");
            logger.debug(">>>>>>>>>>>contrast: \n posBefore" + posBefore + " posAfter" + posAfter);
            before.add(0, posBefore);
            after.add(posAfter);
            result.add(tokenAfter);
            logger.debug(">>>>>>>>>>>contrast: \n before" + before + " after" + after);
            if (after.equals(before)) {
                result = after;
				//set attribute contrast=before
				if(posBefore.equals("NN") && !tokenBefore.hasAttribute("contrast")){
					tokenBefore.setAttribute("contrast","before");
				}
				//set attribute contrast=after
				if(posAfter.equals("NN") && !tokenAfter.hasAttribute("contrast")){
			  		tokenAfter.setAttribute("contrast","after");
				}
                break;
            } //if
        } //for
        return result;
    }

    private void givennessNoun(Set givenSet, Element token, String stemInHistory, String grapheme) {
        givenSet.add(stemInHistory);
        givenSet.addAll(getSyn(grapheme, token.getAttribute("pos").substring(0, 1)));
        givenSet.addAll(getHyper(grapheme, token.getAttribute("pos").substring(0, 1)));
        logger.debug("givennessNoun(), noun: " + grapheme + " givenSet: " + givenSet);
    }
    private void givennessAdj(Set givenSet, Element token, String stemInHistory, String grapheme) {
        givenSet.add(stemInHistory);
        givenSet.addAll(getSyn(grapheme, token.getAttribute("pos").substring(0, 1)));
        logger.debug("givennessAdj(), adj: " + grapheme + " givenSet: " + givenSet);
    }

    private void contrastNoun(Set contrastSet, Element token, String grapheme) {
        contrastSet.addAll(getAnto(grapheme, token.getAttribute("pos").substring(0, 1)));
        //contrastSet.addAll(getHypo(grapheme, token.getAttribute("pos").substring(0, 1), gerNetQuery));
        logger.debug("contrastNoun(), noun: " + grapheme + " contrastSet: " + contrastSet);
    }

    private void contrastAdj(Set contrastSet, Element token, String grapheme) {
        contrastSet.addAll(getAnto(grapheme, token.getAttribute("pos").substring(0, 1)));
        logger.debug("contrastAdj(), adj: " + grapheme + " contrastSet: " + contrastSet);
    }

    //--------------------------helpers-----------------------------------

    /**
     * Lookup the given word in the stemmer.
     * @param word the word to look up in the stemmer.
     * @return the stem of the word, or the word itself if no stem could be found.
     */
    private String findStem(String word) {
        if (word == null)
            return null;
        if (stemmer == null)
            return word;
        String[] results = stemmer.lookup(word);
        if (results.length > 0)
            return results[0];
        else
            return word;
    }

    private boolean isNounOrAdjective(Element token) {
        if (token.getAttribute("pos").equals("NN")
            || token.getAttribute("pos").equals("NE")
            || token.getAttribute("pos").equals("ADJD")
            || token.getAttribute("pos").equals("ADJA")) {
            return true;
        } else
            return false;
    }
    private boolean isNoun(Element token) {
        if (token.getAttribute("pos").equals("NN") || token.getAttribute("pos").equals("NE")) {
            return true;
        } else
            return false;
    }
    private boolean isAdjective(Element token) {
        if (token.getAttribute("pos").equals("ADJD") || token.getAttribute("pos").equals("ADJA")) {
            return true;
        } else
            return false;
    }
    private Vector getSyn(String graph, String pos) {
        if (gerNetQuery == null)
            return new Vector();
        Vector result = gerNetQuery.getSynVector(graph, pos);
        Vector tmp = new Vector();
        if (result.isEmpty()) {
            String nForm = findStem(graph);
            result = gerNetQuery.getSynVector(nForm, pos);
            if (result.isEmpty()) {
                result = gerNetQuery.getSynVector(nForm + "e", pos);
                if (result.isEmpty())
                    result = gerNetQuery.getSynVector(nForm + "er", pos);
            }
        }
        if (!result.isEmpty()) {
            for (int x = 0; x < result.size(); x++) {
                tmp.add(findStem((String) result.get(x)) + "#" + pos);
            } //for
            result = tmp;
        }
        logger.debug("<<<---getSyn(): " + result);
        return result;
    }
    private Vector getHyper(String graph, String pos) {
        if (gerNetQuery == null)
            return new Vector();
        Vector result = gerNetQuery.getHyperVector(graph, pos);
        Vector tmp = new Vector();
        if (result.isEmpty()) {
            String nForm = findStem(graph);
            result = gerNetQuery.getHyperVector(nForm, pos);
            if (result.isEmpty()) {
                result = gerNetQuery.getHyperVector(nForm + "e", pos);
                if (result.isEmpty())
                    result = gerNetQuery.getHyperVector(nForm + "er", pos);
            }
        }
        if (!result.isEmpty()) {
            for (int x = 0; x < result.size(); x++) {
                tmp.add(findStem((String) result.get(x)) + "#" + pos);
            } //for
            result = tmp;
        }
        logger.debug("<<<---getHyp(): " + result);
        return result;
    }
    private Vector getHypo(String graph, String pos) {
        if (gerNetQuery == null)
            return new Vector();
        Vector result = gerNetQuery.getHypoVector(graph, pos);
        Vector tmp = new Vector();
        if (result.isEmpty()) {
            String nForm = findStem(graph);
            result = gerNetQuery.getHypoVector(nForm, pos);
            if (result.isEmpty()) {
                result = gerNetQuery.getHypoVector(nForm + "e", pos);
                if (result.isEmpty())
                    result = gerNetQuery.getHypoVector(nForm + "er", pos);
            }
        }
        if (!result.isEmpty()) {
            for (int x = 0; x < result.size(); x++) {
                tmp.add(findStem((String) result.get(x)) + "#" + pos);
            } //for
            result = tmp;
        }
        logger.debug("<<<---getHyp(): " + result);
        return result;
    }
    private Vector getAnto(String graph, String pos) {
        if (gerNetQuery == null)
            return new Vector();
        Vector result = gerNetQuery.getAntoVector(graph, pos);
        Vector tmp = new Vector();
        if (result.isEmpty()) {
            String nForm = findStem(graph);
            result = gerNetQuery.getAntoVector(nForm, pos);
            if (result.isEmpty()) {
                result = gerNetQuery.getAntoVector(nForm + "e", pos);
                if (result.isEmpty())
                    result = gerNetQuery.getAntoVector(nForm + "er", pos);
            }
        }
        if (!result.isEmpty()) {
            for (int x = 0; x < result.size(); x++) {
                tmp.add(findStem((String) result.get(x)) + "#" + pos);
            } //for
            result = tmp;
        }
        logger.debug("<<<---getHyp(): " + result);
        return result;
    }
    /********************************
     * Beispiele:
     * - Ich habe ein gelbes Haus und ein gr�nes Haus gesehen.
     * INPUT: Das ist Haus Chianti, die Weine dieses Hauses sind weltbekannt.
     * OUTPUT: <?xml version="1.0" encoding="UTF-8"?>
     * <!DOCTYPE maryxml SYSTEM "http://mary.dfki.de/lib/MaryXML.dtd">
     * <maryxml>
     * <speaker gender="female">
     *<div>
     *<t pos="PDS" syn_attach="1" syn_phrase="_">
     *Das
     *</t>
     *<t pos="VAFIN" syn_attach="1" syn_phrase="_">
     * ist
     * </t>
     * <t lexGiv="-" pos="NE" syn_attach="1" syn_phrase="MPN">
     * Haus
     * </t>
     * <t lexGiv="-" pos="NE" syn_attach="0" syn_phrase="MPN">
     * Chianti
     * </t>
     * <t pos="$," syn_attach="2" syn_phrase="_">
     * ,
     * </t>
     * <t pos="ART" syn_attach="1" syn_phrase="NP">
     * die
     * </t>
     * <t lexGiv="-" pos="NN" syn_attach="0" syn_phrase="NP">
     * Weine
     * </t>
     * <t pos="PDAT" syn_attach="1" syn_phrase="NP">
     * dieses
     * </t>
     * <t lexGiv="+" pos="NN" syn_attach="0" syn_phrase="NP">
     * Hauses
     * </t>
     * <t pos="VAFIN" syn_attach="1" syn_phrase="_">
     *  sind
     * </t>
     * <t lexGiv="-" pos="ADJD" syn_attach="1" syn_phrase="_">
     * weltbekannt
     * </t>
     * <t pos="$." syn_attach="2" syn_phrase="_">
     * .
     * </t>
     * </div>
     * </speaker>
     * </maryxml>
     *
     * Das ist ein schlechtes Beispiel und kein gutes Muster.class=java.lang.String
     *******************************/
}
