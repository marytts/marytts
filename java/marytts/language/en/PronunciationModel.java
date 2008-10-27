/**
 * Copyright 2008 DFKI GmbH.
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

package marytts.language.en;

import java.util.Locale;

import marytts.datatypes.MaryXML;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Element;
import org.w3c.dom.traversal.TreeWalker;

/**
 * A pronunciation model that takes into account some English postlexical rules.
 * @author marc
 *
 */
public class PronunciationModel extends marytts.modules.PronunciationModel
{
    /**
     * 
     */
    public PronunciationModel()
    {
        super(Locale.ENGLISH);
    }

    /**
     * Optionally, a language-specific subclass can implement any postlexical rules
     * on the document.
     * @param token a <t> element with a <syllable> and <ph> substructure. 
     * @param allophoneSet
     * @return true if something was changed in the content of the <ph> elements for this <t>, false otherwise
     */
    @Override
    protected boolean postlexicalRules(Element token, AllophoneSet allophoneSet)
    {
        String word = MaryDomUtils.tokenText(token);

        if (word.equals("'s") || word.equals("'ve") || word.equals("'ll") || word.equals("'d")) {
            Element sentence = (Element) MaryDomUtils.getAncestor(token, MaryXML.SENTENCE);
            if (sentence == null) return false;
            TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.PHONE);
            tw.setCurrentNode(token);
            Element currentSegment = (Element)tw.nextNode();
            if (currentSegment == null) return false;
            Element prevSegment = (Element) tw.previousNode();
            if (prevSegment == null) return false;
            String pname = prevSegment.getAttribute("p");
            Allophone prev = allophoneSet.getAllophone(pname);
            if (prev == null) return false;
            
            if (word.equals("'s")) {
                if ("fa".contains(prev.getFeature("ctype"))
                    && "ap".contains(prev.getFeature("cplace"))) {
                    // an alveolar or palatal fricative or affricate: s,z,S,Z,tS,dZ
                    prependSchwa(currentSegment);
                    return true;
                } else if (prev.getFeature("cvox").equals("-")) {
                    // any unvoiced consonant
                    currentSegment.setAttribute("p", "s"); // devoice
                    return true;
                }
            } else { // one of 've, 'll or 'd
                if (prev.isConsonant()) {
                    prependSchwa(currentSegment);
                    return true;
                }
            }
        }
        return false;

    }

    private void prependSchwa(Element currentSegment)
    {
        Element syllable = (Element) currentSegment.getParentNode();
        assert syllable != null;
        Element schwa = MaryXML.createElement(syllable.getOwnerDocument(), MaryXML.PHONE);
        schwa.setAttribute("p", "@");
        syllable.insertBefore(schwa, currentSegment);
    }
}
