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
package marytts.modules.phonemiser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;


public class Allophone
{
    private String name;
    private String vc;
    private final Map<String, String> features;

    /**
     * Create a new Allophone object from the given XML Element
     * @param a the allophone definition element
     * @param featureNames the names of features to use for defining the allophone
     * @throws IllegalArgumentException if a is not as expected
     */
    public Allophone(Element a, String[] featureNames)
    {
        name = a.getAttribute("ph");
        if (name.equals(""))
            throw new IllegalArgumentException("Element must have a 'ph' attribute");
        if (a.getTagName().equals("consonant")) {
            vc = "-";
        } else if (a.getTagName().equals("vowel")) {
            vc = "+";
        } else if (a.getTagName().equals("silence")) {
            vc = "0";
        } else {
            throw new IllegalArgumentException("Element must be one of <vowel>, <consonant> and <silence>, but is <"+a.getTagName()+">");
        }
        Map<String, String> feats = new HashMap<String, String>();
        feats.put("vc", vc);
        for (String f : featureNames) {
            feats.put(f, getAttribute(a, f));
        }
        this.features = Collections.unmodifiableMap(feats);
    }
    
    /**
     * Return the requested attribute of e, or "0" if there is no such attribute.
     * @param e
     * @param att
     * @return
     */
    private String getAttribute(Element e, String att)
    {
        String val = e.getAttribute(att);
        if (val.equals("")) return "0";
        return val;
    }

    public String name() { return name; }
    public String toString() { return name; }
    public boolean isVowel() { return vc.equals("+"); }
    public boolean isSyllabic() { return isVowel(); }
    public boolean isConsonant() { return vc.equals("-"); }
    
    public boolean isVoiced()
    {
        return isVowel() || "+".equals(features.get("cvox"));
    }
    
    public boolean isSonorant()
    {
        return "lnr".contains(features.get("ctype")); 
    }
    
    public boolean isLiquid() { return "l".equals(features.get("ctype")); }
    public boolean isNasal() { return "n".equals(features.get("ctype")); }
    public boolean isGlide() { return "r".equals(features.get("ctype")) && !isVowel(); }

    public boolean isFricative()
    {
        return "f".equals(features.get("ctype"));
    }

    public boolean isPlosive() { return "s".equals(features.get("ctype")); }
    public boolean isPause() { return vc.equals("0"); }
    
    public int sonority()
    {
        if (isVowel()) {
            String vlng = features.get("vlng");
            if ("ld".contains(vlng)) return 6;
            else if ("s".equals(vlng)) return 5;
            else if ("a".equals(vlng)) return 4;
            else return 5; // unknown vowel length
        } else if (isSonorant()) return 3;
        else if (isFricative()) return 2;
        return 1;
    }
    
    /**
     * Get the key-value map of features and feature values for this allophone.
     * @return an unmodifiable map.
     */
    public Map<String, String> getFeatures()
    {
        return features;
    }
    
    /**
     * Return the feature with name feat.
     * Three types of values are possible: 1. an informative feature; 2. the value "0" to indicate
     * that the feature exists but is not meaningful for this allophone; 3. null to indicate that the feature 
     * does not exist.
     * @param feat
     * @return the feature value, or null
     */
    public String getFeature(String feat)
    {
        return features.get(feat);
    }
}
