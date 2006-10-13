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
package de.dfki.lt.mary.modules.tib;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

import de.dfki.lt.mary.Mary;
import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.InternalModule;
import de.dfki.lt.mary.modules.MaryModule;
import de.dfki.lt.mary.modules.phonemiser.Phoneme;
import de.dfki.lt.mary.modules.phonemiser.PhonemeSet;
import de.dfki.lt.mary.util.dom.MaryDomUtils;
import de.dfki.lt.mary.util.dom.NameNodeFilter;

/**
 * The calculation of acoustic parameters module.
 *
 * @author Marc Schr&ouml;der
 */

public class KlattDurationModeller extends InternalModule {
    private PhonemeSet phonemeSet;
    private Properties klattRuleParams;
    /** This map contains the topline-baseline frequency configurations for the
     * currently used phrase and sub-phrase prosody elements. As this is a
     * WeakHashMap, entries will automatically be deleted when not in regular
     * use anymore. */
    private WeakHashMap topBaseConfMap;
    /** This map contains the prosodic settings, as ProsodicSettings objects,
     * for the currently used prosody elements. As this is a WeakHashMap,
     * entries will automatically be deleted when not in regular use
     * anymore. */
    private WeakHashMap prosodyMap;

    public KlattDurationModeller()
    {
        super("KlattDurationModeller", MaryDataType.get("PHRASES_TIB"), MaryDataType.get("DURATIONS_TIB"));
    }

    public void startup() throws Exception {
        super.startup();
        // We depend on the Synthesis module:
        MaryModule synthesis = Mary.getModule(de.dfki.lt.mary.modules.Synthesis.class);
        assert synthesis != null;
        if (synthesis.getState() == MaryModule.MODULE_OFFLINE)
            synthesis.startup();
        // load klatt rules
        klattRuleParams = new Properties();
        klattRuleParams.load(new FileInputStream(MaryProperties.needFilename("tibetan.cap.klattrulefile")));        // load phoneme list
        phonemeSet = PhonemeSet.getPhonemeSet(MaryProperties.needFilename("tibetan.cap.phonemelistfile"));
        // instantiate the Map in which settings are associated with elements:
        // (when the objects serving as keys are not in ordinary use any more,
        // the key-value pairs are deleted from the WeakHashMap earlier or
        // later; that means we do not need to keep track of the hashmaps per
        // thread)
        prosodyMap = new WeakHashMap();
    }

    public MaryData process(MaryData d) throws Exception {
        Document doc = d.getDocument();
        determineProsodicSettings(doc);
        addOrDeleteBoundaries(doc);

        NodeList sentences = doc.getElementsByTagName(MaryXML.SENTENCE);
        for (int i = 0; i < sentences.getLength(); i++) {
            Element sentence = (Element) sentences.item(i);
            processSentence(sentence);
        }
        MaryData result = new MaryData(outputType());
        result.setDocument(doc);
        return result;
    }

    /**
     * For all (possibly nested) prosody elements in the document,
     * calculate their (possibly cumulated) prosodic settings
     * and save them in a map.
     */
    private void determineProsodicSettings(Document doc) {
        // Determine the prosodic setting for each prosody element
        NodeList prosodies = doc.getElementsByTagName(MaryXML.PROSODY);
        for (int i = 0; i < prosodies.getLength(); i++) {
            Element prosody = (Element) prosodies.item(i);
            ProsodicSettings settings = new ProsodicSettings();
            // Neutral default settings:
            ProsodicSettings parentSettings = new ProsodicSettings();
            // Obtain parent settings, if any:
            Element ancestor = (Element) MaryDomUtils.getAncestor(prosody, MaryXML.PROSODY);
            if (ancestor != null) {
                ProsodicSettings testSettings = (ProsodicSettings) prosodyMap.get(ancestor);
                if (testSettings != null) {
                    parentSettings = testSettings;
                }
            }
            // Only accept relative changes, i.e. percentage delta:
            settings.setRate(parentSettings.rate() + getPercentageDelta(prosody.getAttribute("rate")));
            settings.setAccentProminence(
                parentSettings.accentProminence() + getPercentageDelta(prosody.getAttribute("accent-prominence")));
            settings.setAccentSlope(
                parentSettings.accentSlope() + getPercentageDelta(prosody.getAttribute("accent-slope")));
            settings.setNumberOfPauses(
                parentSettings.numberOfPauses() + getPercentageDelta(prosody.getAttribute("number-of-pauses")));
            settings.setPauseDuration(
                parentSettings.pauseDuration() + getPercentageDelta(prosody.getAttribute("pause-duration")));
            settings.setVowelDuration(
                parentSettings.vowelDuration() + getPercentageDelta(prosody.getAttribute("vowel-duration")));
            settings.setPlosiveDuration(
                parentSettings.plosiveDuration() + getPercentageDelta(prosody.getAttribute("plosive-duration")));
            settings.setFricativeDuration(
                parentSettings.fricativeDuration() + getPercentageDelta(prosody.getAttribute("fricative-duration")));
            settings.setNasalDuration(
                parentSettings.nasalDuration() + getPercentageDelta(prosody.getAttribute("nasal-duration")));
            settings.setLiquidDuration(
                parentSettings.liquidDuration() + getPercentageDelta(prosody.getAttribute("liquid-duration")));
            settings.setGlideDuration(
                parentSettings.glideDuration() + getPercentageDelta(prosody.getAttribute("glide-duration")));

            String sVolume = prosody.getAttribute("volume");
            if (sVolume.equals("")) {
                settings.setVolume(parentSettings.volume());
            } else if (isPercentageDelta(sVolume)) {
                int newVolume = parentSettings.volume() + getPercentageDelta(sVolume);
                if (newVolume < 0)
                    newVolume = 0;
                else if (newVolume > 100)
                    newVolume = 100;
                settings.setVolume(newVolume);
            } else if (isUnsignedNumber(sVolume)) {
                settings.setVolume(getUnsignedNumber(sVolume));
            } else if (sVolume.equals("silent")) {
                settings.setVolume(0);
            } else if (sVolume.equals("soft")) {
                settings.setVolume(25);
            } else if (sVolume.equals("medium")) {
                settings.setVolume(50);
            } else if (sVolume.equals("loud")) {
                settings.setVolume(75);
            }
            prosodyMap.put(prosody, settings);
        }
    }

    /**
     * Adjust the number of boundaries according to rate and the
     * "number-of-pauses" attribute.
     */
    private void addOrDeleteBoundaries(Document doc) {
        // Go through boundaries. A boundary is deleted if the determined
        // minimum breakindex size is larger than this boundary's breakindex.
        NodeIterator it =
            ((DocumentTraversal) doc).createNodeIterator(
                doc,
                NodeFilter.SHOW_ELEMENT,
                new NameNodeFilter(MaryXML.BOUNDARY),
                false);
        Element boundary = null;
        List bi1prosodyElements = null;
        while ((boundary = (Element) it.nextNode()) != null) {
            int minBI = 3;
            Element prosody = (Element) MaryDomUtils.getAncestor(boundary, MaryXML.PROSODY);
            if (prosody != null) {
                ProsodicSettings settings = (ProsodicSettings) prosodyMap.get(prosody);
                assert settings != null;
                int rate = settings.rate();
                int numberOfPauses = settings.numberOfPauses();
                if (numberOfPauses <= 50)
                    minBI = 5;
                else if (numberOfPauses <= 75)
                    minBI = 4;
                else if (numberOfPauses > 150)
                    minBI = 1;
                else if (numberOfPauses > 125)
                    minBI = 2;
                // Rate can only shift the number of pauses by one breakindex
                if (rate < 90 && minBI > 1)
                    minBI--;
                if (minBI == 1) {
                    // Remember that the current prosody element wants bi 1 boundaries:
                    if (bi1prosodyElements == null)
                        bi1prosodyElements = new ArrayList();
                    bi1prosodyElements.add(prosody);
                }
            }
            // This boundary's bi:
            int bi = 3;
            try {
                bi = Integer.parseInt(boundary.getAttribute("breakindex"));
            } catch (NumberFormatException e) {
                logger.info(
                    "Unexpected breakindex value `" + boundary.getAttribute("breakindex") + "', assuming " + bi);
            }
            if (bi < minBI) {
                if (!boundary.hasAttribute("duration"))
                    boundary.getParentNode().removeChild(boundary);
                else
                    boundary.removeAttribute("bi"); // but keep duration
            }
        }
        // Do we need to add any boundaries?
        if (bi1prosodyElements != null) {
            Iterator elIt = bi1prosodyElements.iterator();
            while (elIt.hasNext()) {
                Element prosody = (Element) elIt.next();
                NodeIterator nodeIt =
                    ((DocumentTraversal) doc).createNodeIterator(
                        prosody,
                        NodeFilter.SHOW_ELEMENT,
                        new NameNodeFilter(new String[] { MaryXML.TOKEN, MaryXML.BOUNDARY }),
                        false);
                Element el = null;
                Element prevEl = null;
                while ((el = (Element) nodeIt.nextNode()) != null) {
                    if (el.getTagName().equals(MaryXML.TOKEN) && prevEl != null && prevEl.getTagName().equals(MaryXML.TOKEN)) {
                        // Need to insert a boundary before el:
                        Element newBoundary =
                            MaryXML.createElement(doc, MaryXML.BOUNDARY);
                        newBoundary.setAttribute("breakindex", "1");
                        el.getParentNode().insertBefore(newBoundary, el);
                    }
                    prevEl = el;
                }
            }
        }
    }

    private void processSentence(Element sentence) {
        NodeList tokens = sentence.getElementsByTagName(MaryXML.TOKEN);
        if (tokens.getLength() < 1) {
            return; // no tokens -- what can we do?
        }

        // Create the substructure of each token
        for (int i = 0; i < tokens.getLength(); i++) {
            Element token = (Element) tokens.item(i);
            createSubStructure(token);
        }

        // apply Klatt rules to each segment
        NodeList segments = sentence.getElementsByTagName(MaryXML.PHONE);
        for (int i = 0; i < segments.getLength(); i++) {
            Element segment = (Element) segments.item(i);
            int factor = 100;
            int klatt0 = klattRule0(segment);
            int klatt2 = klattRule2(segment);
            int klatt2a = klattRule2a(segment);
            int klatt3 = klattRule3(segment);
            int klatt4 = klattRule4(segment);
            int klatt5 = klattRule5(segment);
            int klatt6 = klattRule6(segment);
            int klatt7 = klattRule7(segment);
            int klatt8 = klattRule8(segment);
            int klatt10 = klattRule10(segment);
            int accentProminence = accentProminenceRule(segment);
            factor = (factor * klatt0) / 100;
            factor = (factor * klatt2) / 100;
            factor = (factor * klatt2a) / 100;
            factor = (factor * klatt3) / 100;
            factor = (factor * klatt4) / 100;
            factor = (factor * klatt5) / 100;
            factor = (factor * klatt6) / 100;
            factor = (factor * klatt7) / 100;
            factor = (factor * klatt8) / 100;
            factor = (factor * klatt10) / 100;
            factor = (factor * accentProminence) / 100;

            // and determine the actual length:
            int inhDuration = getInhDuration(segment);
            int minDuration = getMinDuration(segment);
            int normalDuration = minDuration + ((inhDuration - minDuration) * factor) / 100;

            // Tempo operates on the entire duration, not just on
            // the stretchable part:
            int tempo = tempoRule(segment);
            int duration = (normalDuration * tempo) / 100;

            segment.setAttribute("d", String.valueOf(duration));
            logger.debug(
                segment.getAttribute("p")
                    + " "
                    + duration
                    + "ms (tempoFactor "
                    + tempo
                    + "%, normal "
                    + normalDuration
                    + ", min "
                    + minDuration
                    + ", inh "
                    + inhDuration
                    + ") "
                    + factor
                    + "% ("
                    + klatt0
                    + "*"
                    + klatt2
                    + "*"
                    + klatt2a
                    + "*"
                    + klatt3
                    + "*"
                    + klatt4
                    + "*"
                    + klatt5
                    + "*"
                    + klatt6
                    + "*"
                    + klatt7
                    + "*"
                    + klatt8
                    + "*"
                    + klatt10
                    + ")");
        }

        // apply Klatt rule 1 to boundaries:
        NodeList boundaries = sentence.getElementsByTagName(MaryXML.BOUNDARY);
        for (int i = 0; i < boundaries.getLength(); i++) {
            Element boundary = (Element) boundaries.item(i);
            if (!boundary.hasAttribute("duration")) {
                int duration = klattRule1(boundary);
                boundary.setAttribute("duration", String.valueOf(duration));
            }
        }

        NodeList phrases = sentence.getElementsByTagName(MaryXML.PHRASE);
        for (int i = 0; i < phrases.getLength(); i++) {
            Element phrase = (Element) phrases.item(i);
            // Now save the accumulated duration in every segment
            // of all segments and boundaries in the phrase.
            calculateAccumulatedDurations(phrase);
        }
    }

    private void createSubStructure(Element token) {
        Document document = token.getOwnerDocument();
        Element prosody = (Element) MaryDomUtils.getAncestor(token, MaryXML.PROSODY);
        String vq = null; // voice quality
        if (prosody != null) {
            ProsodicSettings settings = (ProsodicSettings) prosodyMap.get(prosody);
            int volume = settings.volume();
            if (volume >= 60) {
                vq = "loud";
            } else if (volume <= 40) {
                vq = "soft";
            } else {
                vq = null;
            }
        }
        
        // Create syllables within this token only if it does not have any yet:
        NodeList syls = token.getElementsByTagName(MaryXML.SYLLABLE);
        if (syls.getLength() > 0) {
            for (int i=0; i<syls.getLength(); i++) {
                Element syl = (Element) syls.item(i);
                if (!syl.hasAttribute("sampa")) {
                    logger.warn("Ignoring syllable element without sampa attribute in token '"+MaryDomUtils.tokenText(token));
                } else {
                    createPhonemes(syl, vq);
                }
            }
            return;
        }
        // If we get here, we need to create syllable elements
        String sampa = token.getAttribute("sampa");
        if (sampa.equals(""))
            return; // nothing to do

        StringTokenizer tok = new StringTokenizer(sampa, "-_");
        while (tok.hasMoreTokens()) {
            String sylString = tok.nextToken();
            Element syllable = MaryXML.createElement(document, MaryXML.SYLLABLE);
            token.appendChild(syllable);
            // Check for stress signs:
            String first = sylString.substring(0, 1);
            if (first.equals("'")) {
                syllable.setAttribute("stress", "1");
                // The primary stressed syllable of a word
                // inherits the accent:
                if (token.hasAttribute("accent")) {
                    syllable.setAttribute("accent", token.getAttribute("accent"));
                }
                sylString = sylString.substring(1);
            } else if (first.equals(",")) {
                syllable.setAttribute("stress", "2");
                sylString = sylString.substring(1);
            }
            // Remember transcription without stress sign in sampa attribute:
            syllable.setAttribute("sampa", sylString);
            // Now identify the composing segments:
            createPhonemes(syllable, vq);
        }
    }

    /**
     * Create phoneme elements within a syllable, from its sampa attribute.
     * @param syllable The syllable for which to create phoneme elements.
     * @param vq if not null, the voice quality suffix appended to the sampa
     * symbol for each phoneme, e.g. if vq is 'loud', 'a:' becomes 'a:_loud'.
     */
    private void createPhonemes(Element syllable, String vq)
    {
        if (syllable == null)
            throw new NullPointerException("Received null syllable");
        if (!syllable.hasAttribute("sampa"))
            throw new IllegalArgumentException("Cannot create phonemes if syllable has no sampa attribute");
        Document document = syllable.getOwnerDocument();
        Phoneme[] phonemes = phonemeSet.splitIntoPhonemes(syllable.getAttribute("sampa"));
        for (int i = 0; i < phonemes.length; i++) {
            Element segment = MaryXML.createElement(document, MaryXML.PHONE);
            syllable.appendChild(segment);
            segment.setAttribute("p", phonemes[i].name());
            if (vq != null && !(phonemes[i].name().equals("_") || phonemes[i].name().equals("?")))
                segment.setAttribute("vq", vq);
        }
    }

    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    //////////////////////////// Klatt Rules /////////////////////////////
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////


    /**
     * Klatt Rule 0: Overall default speed.
     * @return A percentage value as a factor for duration
     * (100 corresponds to no change).
     */
    private int klattRule0(Element segment) {
        return getPropertyAsInteger("rule0.all");
    }

    /**
     * Klatt Rule 2: Clause-final lengthening.
     * @return A percentage value as a factor for duration
     * (100 corresponds to no change).
     */
    private int klattRule2(Element segment) {
        Element syllable = getSyllable(segment);
        if (isMinipFinal(syllable)) {
            if (isInNucleus(segment)) {
                return getPropertyAsInteger("rule2.nucleus");
            } else if (isInCoda(segment) && (isLiquid(segment) || isNasal(segment) || isFricative(segment))) {
                return getPropertyAsInteger("rule2.coda");
            }
        }
        // default: Rule not applicable
        return 100;
    }

    /**
     * Rule 2a: Additional final lengthening (J�rgen Trouvain).
     * The final syllable before a boundary with breakindex >= 2,
     * if it is part of an accented word, gets additional lengthening.
     * @return A percentage value as a factor for duration
     * (100 corresponds to no change).
     */
    private int klattRule2a(Element segment) {
        Element syllable = getSyllable(segment);
        Element token = getToken(syllable);
        if (isLastBeforeBoundary(syllable, 2) && hasAccent(token)) {
            if (isInNucleus(segment)) {
                return getPropertyAsInteger("rule2a.nucleus");
            } else if (isInCoda(segment) && isNasal(segment)) {
                return getPropertyAsInteger("rule2a.coda");
            }
        }
        // default: Rule not applicable
        return 100;
    }

    /**
     * Klatt Rule 3: Non-phrase-final shortening.
     * @return A percentage value as a factor for duration
     * (100 corresponds to no change).
     */
    private int klattRule3(Element segment) {
        Element syllable = getSyllable(segment);
        if (!isMajIPFinal(syllable)) {
            if (isInNucleus(segment)) {
                return getPropertyAsInteger("rule3.nucleus");
            }
        } else if (isInCoda(segment) && (isLiquid(segment) || isNasal(segment))) {
            return getPropertyAsInteger("rule3.coda");
        }
        // default: Rule not applicable
        return 100;
    }

    /**
     * Klatt Rule 4: Non-word-final shortening.
     * @return A percentage value as a factor for duration
     * (100 corresponds to no change).
     */
    private int klattRule4(Element segment) {
        Element syllable = getSyllable(segment);
        if (!isWordFinal(syllable)) {
            if (isInNucleus(segment)) {
                return getPropertyAsInteger("rule4.nucleus");
            }
        }
        // default: Rule not applicable
        return 100;
    }

    /**
     * Klatt Rule 5: Polysyllabic shortening.
     * @return A percentage value as a factor for duration
     * (100 corresponds to no change).
     */
    private int klattRule5(Element segment) {
        Element token = getToken(segment);
        if (isPolysyllabic(token)) {
            if (isInNucleus(segment)) {
                return getPropertyAsInteger("rule5.nucleus");
            }
        }
        // default: Rule not applicable
        return 100;
    }

    /**
     * Klatt Rule 6: Non-initial consonant shortening.
     * @return A percentage value as a factor for duration
     * (100 corresponds to no change).
     */
    private int klattRule6(Element segment) {
        Element syllable = getSyllable(segment);
        if (isInOnset(segment) && !isWordInitial(syllable)) {
            return getPropertyAsInteger("rule6.onset");
        } else if (isInCoda(segment)) {
            return getPropertyAsInteger("rule6.coda");
        }
        // default: Rule not applicable
        return 100;
    }

    /**
         * Klatt Rule 7: Unstressed shortening
         * @return A percentage value as a factor for duration
         * (100 corresponds to no change).
         */
    private int klattRule7(Element segment) {
        // The stress reduction formulated by Klatt as part of rule 7
        // is relocated to getStress(syllable).
        // The min. duration reduction is relocated to getMinDuration(segment).

        Element token = getToken(segment);
        Element syllable = getSyllable(segment);
        int stress = getStress(syllable);

        if (stress == 2 || stress == 0) {
            if (isInOnset(segment)) {
                if (isLiquid(segment) || isGlide(segment)) {
                    return (getPropertyAsInteger("rule7.onset.liquids"));
                } else {
                    return (getPropertyAsInteger("rule7.others"));
                }
            } else if (isInNucleus(segment)) {
                if (isWordMedial(syllable)) {
                    return (getPropertyAsInteger("rule7.nucleus.medial"));
                } else {
                    return (getPropertyAsInteger("rule7.nucleus.others"));
                }
            } else { // segment is in coda
                return (getPropertyAsInteger("rule7.others"));
            }
        }
        // default: Rule not applicable
        return 100;
    }

    /**
     * Klatt Rule 8: Lengthening for emphasis
     * @return A percentage value as a factor for duration
     * (100 corresponds to no change).
     */
    private int klattRule8(Element segment) {
        Element syllable = getSyllable(segment);
        if (hasAccent(syllable)) {
            if (isInNucleus(segment)) {
                return getPropertyAsInteger("rule8.accent");
            }
        }
        // default: Rule not applicable
        return 100;
    }

    // Klatt Rule 9 (postvocalic context of vowels)
    // is not needed for German.

    /**
     * Klatt Rule 10: Shortening in consonant clusters
     * @return A percentage value as a factor for duration
     * (100 corresponds to no change).
     */
    private int klattRule10(Element segment) {
        boolean hasPrecedingConsonant = false;
        boolean hasFollowingConsonant = false;
        if (isConsonant(segment)) {
            Element preceding = getPreviousSegment(segment);
            if (preceding != null && isConsonant(preceding)) {
                hasPrecedingConsonant = true;
            }
            Element following = getNextSegment(segment);
            if (following != null && isConsonant(following)) {
                hasFollowingConsonant = true;
            }
            if (hasPrecedingConsonant && hasFollowingConsonant) {
                return getPropertyAsInteger("rule10.surrounded");
            } else if (hasPrecedingConsonant) {
                return getPropertyAsInteger("rule10.preceded");
            } else if (hasFollowingConsonant) {
                return getPropertyAsInteger("rule10.followed");
            }
        }
        // default: Rule not applicable
        return 100;
    }

    // Klatt Rule 11 (lengthening due to plosive aspiration)
    // is not needed for German.

    /**
     * Klatt Rule 1: Pause duration. The pause duration depends on the break
     * index, on the speech rate, and on the "pause-duration" attribute.  This
     * rule assumes that every boundary it gets as input is to be realised,
     * i.e. not-to-be-realised boundaries are already deleted at this stage.
     * @return A pause duration, in milliseconds.
     */
    private int klattRule1(Element boundary) {
        int breakindex = getBreakindex(boundary);
        if (breakindex >= 1 && breakindex <= 6) {
            int durationMeasure = 100;
            Element prosody = (Element) MaryDomUtils.getAncestor(boundary, MaryXML.PROSODY);
            if (prosody != null) {
                ProsodicSettings settings = (ProsodicSettings) prosodyMap.get(prosody);
                assert settings != null;
                // Calculate duration measure as a sum of rate and pauseDur.
                int deltaRate = settings.rate() - 100;
                int deltaPauseDur = settings.pauseDuration() - 100;
                durationMeasure = 100 - deltaRate + deltaPauseDur;
            }
            // Now factor is a measure of how long the pauses are to be:
            // 100 medium, 120 long, 140 very long
            // 80 short, 60 very short
            // Intermediate values are interpolated.
            if (durationMeasure == 100) { // probably the most common
                return getPropertyAsInteger("rule1.bi" + String.valueOf(breakindex) + ".medium");
            } else {
                // We could treat 120, 140, 80, and 60 as special cases,
                // but they are probably so rare that it doesn't harm
                // getting them with the interpolation code below.
                int longer;
                int shorter;
                int dist;
                // dist is distance from shorter; our duration value is
                // shorter + dist/20 * (longer - shorter)
                if (durationMeasure > 100) {
                    if (durationMeasure > 120) {
                        // 120 < durationMeasure -- need 120 (long) and 140 (verylong)
                        longer = getPropertyAsInteger("rule1.bi" + String.valueOf(breakindex) + ".verylong");
                        shorter = getPropertyAsInteger("rule1.bi" + String.valueOf(breakindex) + ".long");
                        dist = durationMeasure - 120;
                    } else {
                        // 100 < durationMeasure <= 120 -- need 100 (medium) and 120
                        // (long)
                        longer = getPropertyAsInteger("rule1.bi" + String.valueOf(breakindex) + ".long");
                        shorter = getPropertyAsInteger("rule1.bi" + String.valueOf(breakindex) + ".medium");
                        dist = durationMeasure - 100;
                    }
                } else {
                    if (durationMeasure < 80) {
                        // durationMeasure < 80 -- need 80 (short) and 60 (veryshort)
                        longer = getPropertyAsInteger("rule1.bi" + String.valueOf(breakindex) + ".short");
                        shorter = getPropertyAsInteger("rule1.bi" + String.valueOf(breakindex) + ".veryshort");
                        dist = durationMeasure - 60;
                    } else {
                        // 80 <= durationMeasure < 100 -- need 80 (short) and 100 (medium)
                        longer = getPropertyAsInteger("rule1.bi" + String.valueOf(breakindex) + ".medium");
                        shorter = getPropertyAsInteger("rule1.bi" + String.valueOf(breakindex) + ".short");
                        dist = durationMeasure - 80;
                    }
                }
                int result = shorter + (dist * (longer - shorter)) / 20;
                if (result < 10)
                    result = 10;
                return result;
            }
        }
        // Not a valid break index:
        return 0;
    }

    /**
     * Tempo rule: Take into account the prosody settings
     * for modifying the segment durations, realising speech tempo.
     */
    private int tempoRule(Element segment) {
        Element prosody = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PROSODY);
        if (prosody != null) {
            ProsodicSettings settings = (ProsodicSettings) prosodyMap.get(prosody);
            assert settings != null;
            int rate = settings.rate();
            // Duration is the inverse of rate:
            int durFactor = 10000 / rate;
            Phoneme ph = phonemeSet.getPhoneme(segment.getAttribute("p"));
            if (ph != null) {
                if (ph.isVowel())
                    durFactor = (durFactor * settings.vowelDuration()) / 100;
                else if (ph.isPlosive())
                    durFactor = (durFactor * settings.plosiveDuration()) / 100;
                else if (ph.isFricative())
                    durFactor = (durFactor * settings.fricativeDuration()) / 100;
                else if (ph.isNasal())
                    durFactor = (durFactor * settings.nasalDuration()) / 100;
                else if (ph.isLiquid())
                    durFactor = (durFactor * settings.liquidDuration()) / 100;
                else if (ph.isGlide())
                    durFactor = (durFactor * settings.glideDuration()) / 100;
            }
            return durFactor;
        }
        // default: Rule not applicable
        return 100;
    }

    /**
     * Accent prominence rule: The "accent-prominence" attribute influences
     * nucleus duration for accented syllables (in addition to Klatt rule 8),
     * and affects voice quality for accented syllables. In
     * addition, but not here, the "accent-prominence" attribute causes a
     * topline/baseline overshoot / undershoot.
     * @return A percentage value as a factor for duration
     * (100 corresponds to no change).
     * @see #calculateTargetFrequency()
     */
    private int accentProminenceRule(Element segment) {
        // In addition to Klatt rule 8, take into account the
        // "accent-prominence" attribute:
        int returnValue = 100; // default value
        Element syllable = getSyllable(segment);
        if (hasAccent(syllable)) {
            Element prosody = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PROSODY);
            if (prosody != null) {
                ProsodicSettings settings = (ProsodicSettings) prosodyMap.get(prosody);
                if (settings != null) {
                    int accentProminence = settings.accentProminence();
                    if (accentProminence != 100) {
                        if (isInNucleus(segment)) {
                            returnValue = accentProminence;
                        }
                        // And affect voice quality:
                        String vq = segment.getAttribute("vq");
                        if (accentProminence >= 150) {
                            if (vq.equals("soft") || vq.equals("modal") || vq.equals(""))
                                vq = "loud";
                        } else if (accentProminence >= 125) {
                            if (vq.equals("soft")) {
                                vq = "modal";
                            } else if (vq.equals("modal") || vq.equals("")) {
                                vq = "loud";
                            }
                        }
                        if (!vq.equals(segment.getAttribute("vq"))) {
                            segment.setAttribute("vq", vq);
                        }
                    }
                }
            }
        }
        return returnValue;

    }

    /**
     * For each segment in the given phrase, calculate the accumulated duration
     * since the beginning of the phrase, including this segment's duration,
     * and save it in the segment's <code>end</code> attribute.  (This value is
     * then comparable to the <code>end</code> feature in FreeTTS, but we use
     * milliseconds, they use seconds.)
     */
    private void calculateAccumulatedDurations(Element phrase) {
        TreeWalker tw =
            ((DocumentTraversal) phrase.getOwnerDocument()).createTreeWalker(
                phrase,
                NodeFilter.SHOW_ELEMENT,
                new NameNodeFilter(new String[] { MaryXML.PHONE, MaryXML.BOUNDARY }),
                false);
        int totalDuration = 0;
        Element element;
        while ((element = (Element) tw.nextNode()) != null) {
            if (element.getTagName().equals(MaryXML.PHONE)) {
                // A segment
                int d = 0;
                try {
                    d = Integer.parseInt(element.getAttribute("d"));
                } catch (NumberFormatException e) {
                    logger.warn("Unexpected duration value `" + element.getAttribute("d") + "'");
                }
                totalDuration += d;
                element.setAttribute("end", String.valueOf(totalDuration));
            } else {
                // A boundary
                int d = 0;
                try {
                    d = Integer.parseInt(element.getAttribute("duration"));
                } catch (NumberFormatException e) {
                    logger.warn("Unexpected duration value `" + element.getAttribute("duration") + "'");
                }
                totalDuration += d;
            }
        }
    }


    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    ////////////////////////////// Helpers ///////////////////////////////
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////

    private int getPropertyAsInteger(String prop) {
        int value = 100;
        try {
            value = Integer.parseInt(klattRuleParams.getProperty(prop));
        } catch (NumberFormatException e) {
            logger.warn("Cannot read property " + prop + " in klattrule parameter file. Using default.");
        }
        return value;
    }

    private Element getToken(Element segmentOrSyllable) {
        return (Element) MaryDomUtils.getAncestor(segmentOrSyllable, MaryXML.TOKEN);
    }

    private Element getSyllable(Element segment) {
        return (Element) MaryDomUtils.getAncestor(segment, MaryXML.SYLLABLE);
    }

    private int getStress(Element syllable) {
        // Klatt's usage of 1ary and 2ary stress (Klatt, 1979):
        // primary lexical stress is reserved for vowels in open-class content
        // words, only one 1ary stress per word;
        // 2ary lexical stress is used in some content words, in compounds,
        // in the strongest syllable of polysyllabic function words, and for
        // pronouns (excluding personal pronouns).
        // Approximately adapt our input to Klatt's input:
        // * accented prosodic words (have a tobi accent) can stay as they are
        // * for each unaccented prosodic word (no tobi accent)
        //   - if it is monosyllabic and not a pronoun, remove any stress sign
        //   - if it is polysyllabic, remove 2ary stress,
        //     and reduce 1ary to 2ary.

        int stress = 0;

        if (syllable.hasAttribute("stress")) {
            String helper = syllable.getAttribute("stress");
            if (helper.equals("1"))
                stress = 1;
            else if (helper.equals("2"))
                stress = 2;
        }

        if (stress != 0) {
            // it is worth thinking about stress reduction
            Element token = getToken(syllable);
            // stress reduction:
            if (!hasAccent(token)) {
                // unaccented word
                if (isPolysyllabic(token)) {
                    // polysyllabic:
                    // reduce 1ary to 2ary, 2ary to no stress:
                    if (stress == 1)
                        stress = 2;
                    else if (stress == 2)
                        stress = 0;
                } else {
                    // monosyllabic:
                    if (!isPronoun(token)) {
                        // not a pronoun
                        // remove any stress:
                        stress = 0;
                    }
                }
            }
        }

        return stress;
    }

    /**
     * Find the segment preceding this segment within the same
     * <code>phrase</code>.
     * @return that segment, or <code>null</code> if there is no such segment.
     */
    private static Element getPreviousSegment(Element segment) {
        Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
        return MaryDomUtils.getPreviousOfItsKindIn(segment, phrase);
    }

    /**
     * Find the segment following this segment within the same
     * <code>phrase</code>.
     * @return that segment, or <code>null</code> if there is no such segment.
     */
    private static Element getNextSegment(Element segment) {
        Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
        return MaryDomUtils.getNextOfItsKindIn(segment, phrase);
    }

    /**
     * Find the syllable preceding this syllable within the same
     * <code>phrase</code>.
     * @return that syllable, or <code>null</code> if there is no such
     * syllable.
     */
    private static Element getPreviousSyllable(Element syllable) {
        Element phrase = (Element) MaryDomUtils.getAncestor(syllable, MaryXML.PHRASE);
        return MaryDomUtils.getPreviousOfItsKindIn(syllable, phrase);
    }

    /**
     * Find the syllable following this syllable within the same
     * <code>phrase</code>.
     * @return that syllable, or <code>null</code> if there is no such
     * syllable.
     */
    private static Element getNextSyllable(Element syllable) {
        if (syllable == null)
            return null;
        Element phrase = (Element) MaryDomUtils.getAncestor(syllable, MaryXML.PHRASE);
        return MaryDomUtils.getNextOfItsKindIn(syllable, phrase);
    }

    private int getMinDuration(Element segment) {
        Phoneme ph = phonemeSet.getPhoneme(segment.getAttribute("p"));
        assert ph != null;
        int minDuration = ph.minimalDuration();

        // additional reduction for unstressed segments:
        // (this comes from klatt's original rule no. 7)
        if (getStress(getSyllable(segment)) == 0) {
            // For unstressed segments,
            // increase stretchability by reducing minimum duration:
            return (minDuration * getPropertyAsInteger("rule7.mindur")) / 100;
        } else { // default
            return minDuration;
        }
    }

    private int getInhDuration(Element segment) {
        Phoneme ph = phonemeSet.getPhoneme(segment.getAttribute("p"));
        assert ph != null;
        return ph.inherentDuration();
    }

    private boolean isPronoun(Element token) {
        String pos = token.getAttribute("pos");
        return pos.equals("PDS")
            || pos.equals("PDAT")
            || pos.equals("PIS")
            || pos.equals("PIAT")
            || pos.equals("PIDAT")
            || pos.equals("PPER")
            || pos.equals("PPOSS")
            || pos.equals("PPOSAT")
            || pos.equals("PRELS")
            || pos.equals("PRELAT")
            || pos.equals("PRF")
            || pos.equals("PWS")
            || pos.equals("PWAT")
            || pos.equals("PWAV");
    }

    private boolean isPolysyllabic(Element token) {
        return token.getElementsByTagName(MaryXML.SYLLABLE).getLength() > 1;
    }

    private boolean hasAccent(Element token) {
        String accent = token.getAttribute("accent");
        return !accent.equals("");
    }

    /**
     * Search for boundary and syllable elements following the given syllable.
     * If the next matching element found is a boundary with breakindex
     * <code>minBreakindex</code> or larger, return true; otherwise,
     * return false.
     * If there is no next node, return true.
     */
    private boolean isLastBeforeBoundary(Element syllable, int minBreakindex) {
        Document doc = syllable.getOwnerDocument();
        Element sentence = (Element) MaryDomUtils.getAncestor(syllable, MaryXML.SENTENCE);
        TreeWalker tw =
            ((DocumentTraversal) doc).createTreeWalker(
                sentence,
                NodeFilter.SHOW_ELEMENT,
                new NameNodeFilter(new String[] { MaryXML.SYLLABLE, MaryXML.BOUNDARY }),
                false);
        tw.setCurrentNode(syllable);
        Element next = (Element) tw.nextNode();
        if (next == null) {
            // no matching node after syllable --
            // we must be in a final position.
            return true;
        }
        if (next.getNodeName().equals(MaryXML.BOUNDARY)) {
            if (getBreakindex(next) >= minBreakindex)
                return true;
        }
        // This syllable is either followed by another syllable or
        // by a boundary with breakindex < minBreakindex
        return false;
    }

    private boolean isMajIPFinal(Element syllable) {
        // If this syllable is followed by a boundary with breakindex
        // 4 or above, return true.
        return isLastBeforeBoundary(syllable, 4);
    }

    private boolean isMinipFinal(Element syllable) {
        // If this syllable is followed by a boundary with breakindex
        // 3 or above, return true.
        return isLastBeforeBoundary(syllable, 3);
    }

    private boolean isWordFinal(Element syllable) {
        Element e = syllable;
        while (e != null) {
            e = MaryDomUtils.getNextSiblingElement(e);
            if (e != null && e.getNodeName().equals(MaryXML.SYLLABLE))
                return false;
        }
        return true;
    }

    private boolean isWordMedial(Element syllable) {
        return !(isWordFinal(syllable) || isWordInitial(syllable));

    }

    private boolean isWordInitial(Element syllable) {
        Element e = syllable;
        while (e != null) {
            e = MaryDomUtils.getPreviousSiblingElement(e);
            if (e != null && e.getNodeName().equals(MaryXML.SYLLABLE))
                return false;
        }
        return true;
    }

    private boolean isInOnset(Element segment) {
        Phoneme ph = phonemeSet.getPhoneme(segment.getAttribute("p"));
        assert ph != null;
        if (ph.isSyllabic()) {
            return false;
        }
        // OK, segment is not syllabic. See if it is followed by a syllabic
        // segment:
        for (Element e = MaryDomUtils.getNextSiblingElement(segment);
            e != null;
            e = MaryDomUtils.getNextSiblingElement(e)) {
            ph = phonemeSet.getPhoneme(e.getAttribute("p"));
            assert ph != null;
            if (ph.isSyllabic()) {
                return true;
            }
        }
        return false;
    }

    private boolean isInNucleus(Element segment) {
        Phoneme ph = phonemeSet.getPhoneme(segment.getAttribute("p"));
        assert ph != null;
        return ph.isSyllabic();
    }

    private boolean isInCoda(Element segment) {
        Phoneme ph = phonemeSet.getPhoneme(segment.getAttribute("p"));
        assert ph != null;
        if (ph.isSyllabic()) {
            return false;
        }
        // OK, segment is not syllabic. See if it is preceded by a syllabic
        // segment:
        for (Element e = MaryDomUtils.getPreviousSiblingElement(segment);
            e != null;
            e = MaryDomUtils.getPreviousSiblingElement(e)) {
            ph = phonemeSet.getPhoneme(e.getAttribute("p"));
            assert ph != null;
            if (ph.isSyllabic()) {
                return true;
            }
        }
        return false;
    }

    private boolean isConsonant(Element segment) {
        return !isVowel(segment);
    }

    private boolean isVowel(Element segment) {
        Phoneme ph = phonemeSet.getPhoneme(segment.getAttribute("p"));
        assert ph != null;
        return ph.isVowel();
    }

    private boolean isLiquid(Element segment) {
        Phoneme ph = phonemeSet.getPhoneme(segment.getAttribute("p"));
        assert ph != null;
        return ph.isLiquid();
    }

    private boolean isGlide(Element segment) {
        Phoneme ph = phonemeSet.getPhoneme(segment.getAttribute("p"));
        assert ph != null;
        return ph.isGlide();
    }

    private boolean isNasal(Element segment) {
        Phoneme ph = phonemeSet.getPhoneme(segment.getAttribute("p"));
        assert ph != null;
        return ph.isNasal();
    }

    private boolean isFricative(Element segment) {
        Phoneme ph = phonemeSet.getPhoneme(segment.getAttribute("p"));
        assert ph != null;
        return ph.isFricative();
    }

    private int getBreakindex(Element boundary) {
        int breakindex = 0;
        try {
            breakindex = Integer.parseInt(boundary.getAttribute("breakindex"));
        } catch (NumberFormatException e) {
            logger.warn("Unexpected breakindex value `" + boundary.getAttribute("breakindex") + "'");
        }
        return breakindex;
    }

    /**
     * Tell whether the string contains a positive or negative percentage
     * delta, i.e., a percentage number with an obligatory + or - sign.
     */
    private boolean isPercentageDelta(String string) {
        String s = string.trim();
        if (s.length() < 3)
            return false;
        if (s.substring(s.length() - 1).equals("%") && isNumberDelta(s.substring(0, s.length() - 1)))
            return true;
        else
            return false;
    }

    /**
     * For a string containing a percentage delta as judged by
     * <code>isPercentageDelta()</code>, return the numerical value, rounded to
     * an integer.
     * @return the numeric part of the percentage, rounded to an integer, or 0
     * if the string is not a valid percentage delta.
     */
    private int getPercentageDelta(String string) {
        String s = string.trim();
        if (!isPercentageDelta(s))
            return 0;
        return getNumberDelta(s.substring(0, s.length() - 1));
    }

    /**
     * Tell whether the string contains a positive or negative semitones delta,
     * i.e., a semitones number with an obligatory + or - sign, such as
     * "+3.2st" or "-13.2st".
     */
    private boolean isSemitonesDelta(String string) {
        String s = string.trim();
        if (s.length() < 4)
            return false;
        if (s.substring(s.length() - 2).equals("st") && isNumberDelta(s.substring(0, s.length() - 2)))
            return true;
        else
            return false;
    }

    /**
     * For a string containing a semitones delta as judged by
     * <code>isSemitonesDelta()</code>, return the numerical value, as a
     * double.
     * @return the numeric part of the semitones delta, or 0
     * if the string is not a valid semitones delta.
     */
    private double getSemitonesDelta(String string) {
        String s = string.trim();
        if (!isSemitonesDelta(s))
            return 0;
        String num = s.substring(0, s.length() - 2);
        double value = 0;
        try {
            value = Double.parseDouble(num);
        } catch (NumberFormatException e) {
            logger.warn("Unexpected number value `" + num + "'");
        }
        return value;
    }

    /**
     * Tell whether the string contains a positive or negative number
     * delta, i.e., a number with an obligatory + or - sign.
     */
    private boolean isNumberDelta(String string) {
        String s = string.trim();
        if (s.length() < 2)
            return false;
        if ((s.charAt(0) == '+' || s.charAt(0) == '-') && isUnsignedNumber(s.substring(1)))
            return true;
        else
            return false;
    }

    /**
     * For a string containing a number delta as judged by
     * <code>isNumberDelta()</code>, return the numerical value, rounded to
     * an integer.
     * @return the numeric value, rounded to an integer, or 0
     * if the string is not a valid number delta.
     */
    private int getNumberDelta(String string) {
        String s = string.trim();
        if (!isNumberDelta(s))
            return 0;
        double value = 0;
        try {
            value = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            logger.warn("Unexpected number value `" + s + "'");
        }
        return (int) Math.round(value);

    }

    /**
     * Tell whether the string contains an unsigned semitones expression, such
     * as "12st" or "5.4st".
     */
    private boolean isUnsignedSemitones(String string) {
        String s = string.trim();
        if (s.length() < 3)
            return false;
        if (s.substring(s.length() - 2).equals("st") && isUnsignedNumber(s.substring(0, s.length() - 2)))
            return true;
        else
            return false;
    }

    /**
     * For a string containing an unsigned semitones expression as judged by
     * <code>isUnsignedSemitones()</code>, return the numerical value as a
     * double.
     * @return the numeric part of the semitones expression, or 0 if the string
     * is not a valid unsigned semitones expression.
     */
    private double getUnsignedSemitones(String string) {
        String s = string.trim();
        if (!isUnsignedSemitones(s))
            return 0;
        String num = s.substring(0, s.length() - 2);
        double value = 0;
        try {
            value = Double.parseDouble(num);
        } catch (NumberFormatException e) {
            logger.warn("Unexpected number value `" + num + "'");
        }
        return value;
    }

    /**
     * Tell whether the string contains an unsigned number.
     */
    private boolean isUnsignedNumber(String string) {
        String s = string.trim();
        if (s.length() < 1)
            return false;
        if (s.charAt(0) != '+' && s.charAt(0) != '-') {
            double value = 0;
            try {
                value = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * For a string containing an unsigned number as judged by
     * <code>isUnsignedNumber()</code>, return the numerical value, rounded to
     * an integer.
     * @return the numeric value, rounded to an integer, or 0
     * if the string is not a valid unsigned number.
     */
    private int getUnsignedNumber(String string) {
        String s = string.trim();
        if (!isUnsignedNumber(s))
            return 0;
        double value = 0;
        try {
            value = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            logger.warn("Unexpected number value `" + s + "'");
        }
        return (int) Math.round(value);
    }

    /**
     * Tell whether the string contains a number.
     */
    private boolean isNumber(String string) {
        String s = string.trim();
        if (s.length() < 1)
            return false;
        double value = 0;
        try {
            value = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * For a string containing a number as judged by
     * <code>isNumber()</code>, return the numerical value, rounded to
     * an integer.
     * @return the numeric value, rounded to an integer, or 0
     * if the string is not a valid number.
     */
    private int getNumber(String string) {
        String s = string.trim();
        if (!isNumber(s))
            return 0;
        double value = 0;
        try {
            value = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            logger.warn("Unexpected number value `" + s + "'");
        }
        return (int) Math.round(value);
    }

    /**
     * For a given token, find the stressed syllable. If no syllable has
     * primary stress, return the first syllable with secondary stress. If none
     * has secondary stress, return the first syllable in the token. If there
     * is no syllable in the token or the element given in the argument is not
     * a token element, return null.
     */
    private Element getStressedSyllable(Element token) {
        if (token == null || !token.getTagName().equals(MaryXML.TOKEN))
            return null;
        Element syl = MaryDomUtils.getFirstElementByTagName(token, MaryXML.SYLLABLE);
        while (syl != null && !syl.getAttribute("stress").equals("1")) {
            syl = MaryDomUtils.getNextSiblingElementByTagName(syl, MaryXML.SYLLABLE);
        }
        if (syl != null) {
            return syl;
        }
        // If we get here, there is no stressed syllable. As a fallback, use
        // the first syllable with secondary stress, or the first syllable if
        // none has secondary stress.
        Element first = MaryDomUtils.getFirstElementByTagName(token, MaryXML.SYLLABLE);
        Element secondary = first;
        while (secondary != null && !secondary.getAttribute("stress").equals("2")) {
            secondary = MaryDomUtils.getNextSiblingElementByTagName(secondary, MaryXML.SYLLABLE);
        }
        if (secondary != null) {
            return secondary;
        }
        return first;
    }

    /**
     * For a syllable, return the first child segment which is a nucleus
     * segment. Return <code>null</code> if there is no such segment.
     */
    private Element getNucleus(Element syllable) {
        if (syllable == null || !syllable.getTagName().equals(MaryXML.SYLLABLE))
            return null;
        Element seg = MaryDomUtils.getFirstElementByTagName(syllable, MaryXML.PHONE);
        while (seg != null && !isInNucleus(seg)) {
            seg = MaryDomUtils.getNextSiblingElementByTagName(seg, MaryXML.PHONE);
        }
        return seg;
    }

    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    /////////////////////////// Helper Classes ///////////////////////////
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////

    static class ProsodicSettings {
        // Relative settings: 100 = 100% = no change
        int rate;
        int accentProminence;
        int accentSlope;
        int numberOfPauses;
        int pauseDuration;
        int vowelDuration;
        int plosiveDuration;
        int fricativeDuration;
        int nasalDuration;
        int liquidDuration;
        int glideDuration;
        int volume;

        ProsodicSettings() {
            this.rate = 100;
            this.accentProminence = 100;
            this.accentSlope = 100;
            this.numberOfPauses = 100;
            this.pauseDuration = 100;
            this.vowelDuration = 100;
            this.plosiveDuration = 100;
            this.fricativeDuration = 100;
            this.nasalDuration = 100;
            this.liquidDuration = 100;
            this.glideDuration = 100;
            this.volume = 50;
        }

        ProsodicSettings(
            int rate,
            int accentProminence,
            int accentSlope,
            int numberOfPauses,
            int pauseDuration,
            int vowelDuration,
            int plosiveDuration,
            int fricativeDuration,
            int nasalDuration,
            int liquidDuration,
            int glideDuration,
            int volume) {
            this.rate = rate;
            this.accentProminence = accentProminence;
            this.accentSlope = accentSlope;
            this.numberOfPauses = numberOfPauses;
            this.pauseDuration = pauseDuration;
            this.vowelDuration = vowelDuration;
            this.plosiveDuration = plosiveDuration;
            this.fricativeDuration = fricativeDuration;
            this.nasalDuration = nasalDuration;
            this.liquidDuration = liquidDuration;
            this.glideDuration = glideDuration;
            this.volume = volume;
        }

        int rate() {
            return rate;
        }
        int accentProminence() {
            return accentProminence;
        }
        int accentSlope() {
            return accentSlope;
        }
        int numberOfPauses() {
            return numberOfPauses;
        }
        int pauseDuration() {
            return pauseDuration;
        }
        int vowelDuration() {
            return vowelDuration;
        }
        int plosiveDuration() {
            return plosiveDuration;
        }
        int fricativeDuration() {
            return fricativeDuration;
        }
        int nasalDuration() {
            return nasalDuration;
        }
        int liquidDuration() {
            return liquidDuration;
        }
        int glideDuration() {
            return glideDuration;
        }
        int volume() {
            return volume;
        }

        void setRate(int value) {
            rate = value;
        }
        void setAccentProminence(int value) {
            accentProminence = value;
        }
        void setAccentSlope(int value) {
            accentSlope = value;
        }
        void setNumberOfPauses(int value) {
            numberOfPauses = value;
        }
        void setPauseDuration(int value) {
            pauseDuration = value;
        }
        void setVowelDuration(int value) {
            vowelDuration = value;
        }
        void setPlosiveDuration(int value) {
            plosiveDuration = value;
        }
        void setFricativeDuration(int value) {
            fricativeDuration = value;
        }
        void setNasalDuration(int value) {
            nasalDuration = value;
        }
        void setLiquidDuration(int value) {
            liquidDuration = value;
        }
        void setGlideDuration(int value) {
            glideDuration = value;
        }
        void setVolume(int value) {
            volume = value;
        }

    }


}
