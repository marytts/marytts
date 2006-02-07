/**
 * Copyright 2006 DFKI GmbH.
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
package de.dfki.lt.mary.unitselection.phrase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;

import de.dfki.lt.freetts.ClusterUnitNamer;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.unitselection.JoinCostFunction;
import de.dfki.lt.mary.unitselection.SilenceTarget;
import de.dfki.lt.mary.unitselection.Target;
import de.dfki.lt.mary.unitselection.TargetCostFunction;
import de.dfki.lt.mary.unitselection.Unit;
import de.dfki.lt.mary.unitselection.UnitDatabase;
import de.dfki.lt.mary.unitselection.UnitSelector;
import de.dfki.lt.mary.unitselection.viterbi.ViterbiCandidate;
import de.dfki.lt.mary.util.dom.MaryDomUtils;

public class PhraseUnitSelector extends UnitSelector
{
    protected Logger logger = Logger.getLogger("PhraseUnitSelector");
    
    public PhraseUnitSelector(TargetCostFunction targetCostFunction, 
			JoinCostFunction joinCostFunction)
    {
        super(targetCostFunction, joinCostFunction);
    }
    
    public void buildUnitList(Relation segs,Utterance utt){}
    
    public List selectUnits(List tokensAndBoundaries, Voice voice, UnitDatabase db, 
            ClusterUnitNamer unitNamer){
        return null;}
    
    
	public ViterbiCandidate getCandidates(Target target){
	    return null;}
    
    
    protected List getTargetChain(List tokensAndBoundaries, UnitDatabase database)
    {
        List targetList = new ArrayList();
        assert database.getUnitType() == Unit.PHRASE;
        assert database instanceof PhraseUnitDatabase;
        PhraseUnitDatabase pDatabase = (PhraseUnitDatabase) database;
        Set phrases = pDatabase.getUnitNames();
        int longestPhrase = pDatabase.getLongestPhraseLength();
        // Now try a longest-prefix match of tokensAndBoundaries against the phrases Set
        for (int i=0; i<tokensAndBoundaries.size(); ) {
            if (((Element)tokensAndBoundaries.get(i)).getTagName().equals(MaryXML.BOUNDARY)) {
                // A boundary
                int dur = getBoundaryDuration((Element)tokensAndBoundaries.get(i));
                if (dur > 0) {
                    targetList.add(new SilenceTarget(dur));
                }
                continue;
            }
            // Current element is a token:
            boolean matched = false;
            for (int j=Math.min(i+longestPhrase, tokensAndBoundaries.size()); j>i; j--) {
                String possiblePhrase = extractTokenText(tokensAndBoundaries, i, j);
                if (possiblePhrase != null && phrases.contains(possiblePhrase)) {
                    // yes, found a phrase!
                    targetList.add(new Target(possiblePhrase));
                    matched = true;
                    break; // leave j loop
                }
            }
            if (!matched) {
                logger.warn("Could not find target word in unit inventory: "+MaryDomUtils.tokenText((Element)tokensAndBoundaries.get(i)));
            }
        }
        return targetList;
    }
    
    /**
     * Extract the string version of all tokens in the indicated subsection
     * of the tokensAndBoundaries list.
     * @param tokensAndBoundaries list of token and boundary MaryXML nodes
     * @param first index position in list of first list item to look at
     * @param last index position in list of last list item to look at
     * @return a String containing the tokenTexts of the relevant tokens 
     * separated by single space characters; return null if the specified range
     * contains at least one boundary of non-zero duration.
     */
    protected String extractTokenText(List tokensAndBoundaries, int first, int last)
    {
        StringBuffer buf = new StringBuffer();
        for (int i=first; i<=last; i++) {
            Element el = (Element) tokensAndBoundaries.get(i);
            if (el.getTagName().equals(MaryXML.BOUNDARY)) {
                if (getBoundaryDuration(el) > 0) {
                    // The requested range contains a pause!
                    return null;
                }
                // boundaries with zero duration are simply ignored
            } else {
                assert el.getTagName().equals(MaryXML.TOKEN);
                buf.append(MaryDomUtils.tokenText(el));
                buf.append(" ");
            }
        }
        return buf.substring(0, buf.length()-1); // cut off the final space
    }
    
    protected int getBoundaryDuration(Element boundary)
    {
        String durString = boundary.getAttribute("duration");
        if (!durString.equals("")) {
            try {
                int milliseconds = Integer.valueOf(durString).intValue();
                if (milliseconds > 0) {
                    return milliseconds;
                }
            } catch (NumberFormatException nfe) {}
        }
        return 0;
    }
}
