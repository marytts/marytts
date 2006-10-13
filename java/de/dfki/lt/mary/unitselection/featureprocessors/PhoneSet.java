/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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
package de.dfki.lt.mary.unitselection.featureprocessors;


/**
 * Maintains a list of phones with various features for those phones.
 */
public interface PhoneSet  {

    /**
     * Vowel or consonant:  + = vowel, - = consonant.
     */
    public final static String VC = "vc";  

    /**
     * Vowel length:  s = short, l = long, d = dipthong, a = schwa.
     */
    public final static String VLNG = "vlng";  

    /**
     * Vowel height:  1 = high,  2 = mid,  3 = low.
     */
    public final static String VHEIGHT = "vheight";  

    /**
     * Vowel frontness:  1 = front, 2 = mid, 3 = back.
     */
    public final static String VFRONT = "vfront";  

    /**
     * Lip rounding:  + = on, - = off.
     */
    public final static String VRND = "vrnd";  

    /**
     * Consonant type:  s = stop, f = fricative,  a = affricative,
     * n = nasal, l = liquid.
     */
    public final static String CTYPE = "ctype";  

    /**
     * Consonant cplace:  l = labial, a = alveolar, p = palatal,
     * b = labio_dental, d = dental, v = velar
     */
    public final static String CPLACE = "cplace";  

    /**
     * Consonant voicing:  + = on, - = off
     */
    public final static String CVOX = "cvox";  

    /**
     * Given a phoneme and a feature name, return the feature.
     *
     * @param phone the phoneme of interest
     * @param featureName the name of the feature of interest
     *
     * @return the feature with the given name
     */
    public String getPhoneFeature(String phone, String featureName);
    
    /**
     * Return a list of phonemes, in alphabetical order.
     * @return an array of strings, each string is the name of one phoneme.
     */
    public String[] listPhonemes();

}
