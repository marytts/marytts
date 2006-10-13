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

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.modules.ProsodyGeneric;
import de.dfki.lt.mary.util.dom.DomUtils;
import de.dfki.lt.mary.util.dom.MaryDomUtils;
import de.dfki.lt.mary.util.dom.NameNodeFilter;

public class Prosody extends ProsodyGeneric
{
    public Prosody()
    {
        super(MaryDataType.get("TONES_TIB"), MaryDataType.get("PHRASES_TIB"),
            "tibetan.prosody.tobipredparams", null, null, "tibetan.prosody.paragraphdeclination");
    }
    
    /**
     * Currently, no accents assigned for Tibetan.
     * @param token a token element
     * @param accent the accent string to assign.
     */
    protected void setAccent(Element token, String accent)
    {
    }

    /** Checks if a boundary is to be inserted after the current token
     * Override default implementation in order not to assign boundary
     * tones, but only break indices (no tones in Tibetan yet).
     * @param token (current token)
     * @param tokens (list of tokens in sentence)
     * @param position (position in token list)
     * @param sentenceType (declarative, exclamative or interrogative)
     * @param specialPositionType (endofvorfeld if sentence has vorfeld and the next token is a finite verb or end of paragraph)
     * @param invalidXML (true if xml structure allows boundary insertion)
     * @param firstTokenInPhrase (begin of intonation phrase)
     * @return firstTokenInPhrase (if a boundary was inserted, firstTokenInPhrase gets null)
     */

    protected Element getBoundary(Element token, NodeList tokens, int position,
            String sentenceType, String specialPositionType,
            boolean invalidXML, Element firstTokenInPhrase)
    {

        String tokenText = MaryDomUtils.tokenText(token); // text of current token

        Element ruleList = null;
        // only the "boundaries" rules are relevant
        ruleList = (Element) tobiPredMap.get("boundaries");
        // search for concrete rules (search for tag "rule")
        TreeWalker tw = ((DocumentTraversal) ruleList.getOwnerDocument())
                .createTreeWalker(ruleList, NodeFilter.SHOW_ELEMENT,
                        new NameNodeFilter(new String[] { "rule" }), false);

        boolean rule_fired = false;
        Element rule = null;

        // search for appropriate rules; the top rule has highest prority
        // if a rule fires (that is: all the conditions are fulfilled), the boundary is inserted and the loop stops
        while (rule_fired == false && (rule = (Element) tw.nextNode()) != null) {
            // rule = the whole rule
            // currentRulePart = part of the rule (condition or action)
            Element currentRulePart = DomUtils.getFirstChildElement(rule);

            while (rule_fired == false && currentRulePart != null) {
                boolean conditionSatisfied = false;

                // if rule part with tag "action": boundary insertion
                if (currentRulePart.getTagName().equals("action")) {
                    int bi = Integer.parseInt(currentRulePart
                            .getAttribute("bi"));
                    if (bi == 0) {
                        // no boundary insertion
                    } else if ((bi >= 4) && (bi <= 6)) {
                        if (!invalidXML) {
                            insertMajorBoundary(tokens, position,
                                    firstTokenInPhrase, null, bi);
                            firstTokenInPhrase = null;
                        } else
                            insertBoundary(token, null, bi);
                    }

                    else
                        insertBoundary(token, null, bi);
                    rule_fired = true;
                    break;
                }

                // check if the condition is satisfied
                conditionSatisfied = checkRulePart(currentRulePart, token,
                        tokens, position, sentenceType, specialPositionType,
                        tokenText);
                if (conditionSatisfied == false)
                    break; // condition violated, try next rule

                // the previous conditions are satisfied --> check the next rule part
                currentRulePart = DomUtils
                        .getNextSiblingElement(currentRulePart);
            }//while loop that checks the rule parts
        } // while loop that checks the whole rule
        return firstTokenInPhrase;
    }

}

