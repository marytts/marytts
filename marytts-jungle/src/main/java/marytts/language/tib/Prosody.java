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
package marytts.language.tib;

import java.util.Locale;

import marytts.language.tib.datatypes.TibetanDataTypes;
import marytts.modules.ProsodyGeneric;
import marytts.util.dom.DomUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;


public class Prosody extends ProsodyGeneric
{
    public Prosody()
    {
        super(TibetanDataTypes.TONES_TIB,
                TibetanDataTypes.PHRASES_TIB,
                new Locale("tib"),
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
        while (!rule_fired && (rule = (Element) tw.nextNode()) != null) {
            // rule = the whole rule
            // currentRulePart = part of the rule (condition or action)
            Element currentRulePart = DomUtils.getFirstChildElement(rule);

            while (!rule_fired && currentRulePart != null) {
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
                if (!conditionSatisfied)
                    break; // condition violated, try next rule

                // the previous conditions are satisfied --> check the next rule part
                currentRulePart = DomUtils
                        .getNextSiblingElement(currentRulePart);
            }//while loop that checks the rule parts
        } // while loop that checks the whole rule
        return firstTokenInPhrase;
    }

}


