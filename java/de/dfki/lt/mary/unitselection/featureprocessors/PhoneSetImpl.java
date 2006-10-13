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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * Implementation of a <code>PhoneSet</code> that reads the info from
 * a file.  The format of the file is as follows:
 *
 * <pre>
 * phone feature value
 * phone feature value
 * phone feature value
 * ...
 * </pre>
 *
 * Where <code>phone</code> is the phone name, <code>feature</code> is
 * the phone feature such as "vc," "vlng," "vheight," and so on, and
 * "value" is the value of the feature.  There can be multiple lines
 * for the same phone to describe various features of that phone.
 */
public class PhoneSetImpl implements PhoneSet {
    /**
     * Used for informational purposes if there's a bad line in the
     * file.
     */ 
    private int lineCount = 0;

    /**
     * The set of phone features indexed by phone.
     */    
    private Map phonesetMap;

    /**
     * The set of phoneme symbols known by this phoneset.
     */
    private Set phonemes;
    
    /**
     * Create a new <code>PhoneSetImpl</code> by reading from the
     * given URL.
     *
     * @param url the input source
     *
     * @throws IOException if an error occurs
     */ 
    public PhoneSetImpl(URL url) throws IOException {
        BufferedReader reader;
        String line;

	phonesetMap = new HashMap();
    phonemes = new TreeSet();
	reader = new BufferedReader(new
		InputStreamReader(url.openStream()));
	line = reader.readLine();
	lineCount++;
	while (line != null) {
	    if (!line.startsWith("***")) {
		parseAndAdd(line);
	    }
	    line = reader.readLine();
	}
	reader.close();
    }
    
    /**
     * Creates a word from the given input line and add it to the map.
     *
     * @param line the input line
     */
    private void parseAndAdd(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line," ");
	try {
	    String phoneme = tokenizer.nextToken();
	    String feature = tokenizer.nextToken();        
	    String value = tokenizer.nextToken();
        phonemes.add(phoneme);
	    phonesetMap.put(getKey(phoneme, feature), value);
	} catch (NoSuchElementException nse) {
	    throw new Error("part of speech data in bad format at line " 
	    + lineCount);
	}
    }

    /**
     * Given a phoneme and a feature, returns the key that
     * will obtain the value.
     *
     * @param phoneme the phoneme
     * @param feature the name of the feature
     *
     * @return the key used to obtain the value
     */
    private String getKey(String phoneme, String feature) {
	return phoneme + feature;
    }

    /**
     * Given a phoneme and a feature name, returns the feature.
     *
     * @param phone the phoneme of interest
     * @param featureName the name of the feature of interest
     *
     * @return the feature with the given name
     */
    public String getPhoneFeature(String phone, String featureName) {
	return (String) phonesetMap.get(getKey(phone, featureName));
    }
    
    /**
     * Return a list of phonemes, in alphabetical order.
     * @return an array of strings, each string is the name of one phoneme.
     */
    public String[] listPhonemes()
    {
        return (String[]) phonemes.toArray(new String[0]);
    }
}
