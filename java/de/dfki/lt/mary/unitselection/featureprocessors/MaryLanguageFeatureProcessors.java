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

import com.sun.speech.freetts.en.us.USEnglish;
import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.ProcessException;
import com.sun.speech.freetts.Relation;

import de.dfki.lt.mary.unitselection.Target;
import de.dfki.lt.mary.unitselection.cart.PathExtractor;
import de.dfki.lt.mary.unitselection.cart.PathExtractorImpl;
import de.dfki.lt.mary.util.ByteStringTranslator;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.Set;



/**
 * Provides the set of feature processors that are used by this
 * language as part of the CART processing.
 */
public class MaryLanguageFeatureProcessors extends MaryGenericFeatureProcessors
{


    private final static Pattern DOUBLE_PATTERN 
	= Pattern.compile(USEnglish.RX_DOUBLE);

    private final static Pattern DIGITS_PATTERN  
	= Pattern.compile(USEnglish.RX_DIGITS);

    private static Set englishMonths;
    private static Set englishDays;

    // the set of month names
    static {
        englishMonths = new HashSet();
        englishMonths.add("jan");
        englishMonths.add("january");
        englishMonths.add("feb");
        englishMonths.add("february");
        englishMonths.add("mar");
        englishMonths.add("march");
        englishMonths.add("apr");
        englishMonths.add("april");
        englishMonths.add("may");
        englishMonths.add("jun");
        englishMonths.add("june");
        englishMonths.add("jul");
        englishMonths.add("july");
        englishMonths.add("aug");
        englishMonths.add("august");
        englishMonths.add("sep");
        englishMonths.add("september");
        englishMonths.add("oct");
        englishMonths.add("october");
        englishMonths.add("nov");
        englishMonths.add("november");
        englishMonths.add("dec");
        englishMonths.add("december");
    }

    // the set of week names
    static {
        englishDays = new HashSet();
        englishDays.add("sun");
        englishDays.add("sunday");
        englishDays.add("mon");
        englishDays.add("monday");
        englishDays.add("tue");
        englishDays.add("tuesday");
        englishDays.add("wed");
        englishDays.add("wednesday");
        englishDays.add("thu");
        englishDays.add("thursday");
	englishDays.add("fri");
	englishDays.add("friday");
	englishDays.add("sat");
	englishDays.add("saturday");
    }
    
    // no instances
    private MaryLanguageFeatureProcessors() {}

    /**
     * Tests the onset ctype of the given segment.
     *
     * @param  seg  the segment to test to process
     * @param ctype the ctype to check for
     *
     * @return if Onset Stop "1"; otherwise "0"
     *
     */
    private static  String segOnsetCtype(Item seg, String ctype,PhoneSet phoneSet) {
        Item daughter = seg.getItemAs(
                Relation.SYLLABLE_STRUCTURE).getParent().getDaughter();

        while (daughter != null) {
            if ("+".equals(phoneSet.getPhoneFeature(daughter.toString(), "vc"))) {
                return "0";
            }
            if (ctype.equals(phoneSet.getPhoneFeature(daughter.toString(), "ctype"))) {
                return "1";
            }

            daughter = daughter.getNext();
        }
        return "0";
    }


    /**
     * Tests the coda ctype of the given segment.
     *
     * @param seg the segment to test
     * @param ctype the ctype to check for
     * 
     * @return "1" on match "0" on no match
     */
    private static String segCodaCtype(Item seg, String ctype, PhoneSet phoneSet) {
        Item daughter 
	    	= seg.getItemAs(
	    	        Relation.SYLLABLE_STRUCTURE).getParent().getLastDaughter();

        while (daughter != null) {
            if ("+".equals(phoneSet.getPhoneFeature(daughter.toString(), "vc"))) {
                return "0";
            }
            if (ctype.equals(phoneSet.getPhoneFeature(daughter.toString(), "ctype"))) {
                return "1";
            }

            daughter = daughter.getPrevious();
        }
        return "0";
    }

    
    /**
     * Rails an int. flite never returns an int more than 19 from
     * a feature processor, we duplicate that behavior
     * here so that our tests will match.
     *
     * @param val the value to rail
     *
     * @return val clipped to be betweein 0 and 19
     */
    private static int rail(int val) {
        return val > 19 ? 19 : val;
    }
    
    
    /**
     * Returns as an Integer the number of syllables in the given word. This is
     * a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class Phoneme implements ByteValuedFeatureProcessor
    {
        public String getName() { return "mary_phoneme"; }
        public String[] getValues() { return values.getStringValues(); }
        private ByteStringTranslator values;
        
        public Phoneme(PhoneSet phoneSet){
            this.values = new ByteStringTranslator(phoneSet.listPhonemes());
        }
        /**
         * Performs some processing on the given item.
         *
         * @param  item  the item to process
         *
         * @return consonant cplace
         */
        public byte process(Target target)
        {
            Item segment = getSegment(target);
            return values.get(segment.getFeatures().getString("name"));
        }
    }


    
    /**
     * Attempts to guess the part of speech.
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class TokenPosGuess implements FeatureProcessor {
        public String getName() { return "token_pos_guess"; }
    /**
	 * Performs some processing on the given item.
	 *
	 * @param  item  the item to process
	 *
	 * @return  a guess at the part of speech
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item item) throws ProcessException {
	    String name = item.getFeatures().getString("name");
	    String dc = name.toLowerCase();
	    if (DIGITS_PATTERN.matcher(dc).matches()) {
		return "numeric";
	    } else if (DOUBLE_PATTERN.matcher(dc).matches()) {
		return "number";
	    } else if (englishMonths.contains(dc)) {
		return "month";
	    } else if (englishDays.contains(dc)) {
		return "day";
	    } else if (dc.equals("a")) {
		return "a";
	    } else if (dc.equals("flight")) {
		return "flight";
	    } else if (dc.equals("to")) {
		return "to";
	    } else {
		return "_other_";
	    }
	}
    }
    

    /**
     * Returns a guess of the part-of-speech.
     *
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class Gpos implements ByteValuedFeatureProcessor {
        public String getName() { return "mary_gpos"; }
        public String[] getValues() { return values.getStringValues(); }
        private Map posConverter;
        private ByteStringTranslator values;
        
        public Gpos(Map posConverter){
            this.posConverter = posConverter;
            this.values = new ByteStringTranslator();
            Collection valueCollection = posConverter.values();
            byte count = 0;
            for (Iterator it = valueCollection.iterator(); it.hasNext(); ) {
                String val = (String) it.next();
                values.set(count, val);
                count++;
            }
        }
        
	/**
	 * Performs some processing on the given item.
	 *
	 * @param  item  the item to process
	 *
	 * @return a guess at the part-of-speech for the item
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public byte process(Target target)
    {
        Item word = getWord(target);
	    String pos = word.getFeatures().getString("pos");
	    if (posConverter.containsKey(pos)){
	        pos = (String)posConverter.get(pos);}
	    return values.get(pos);
	}
    }
    
    /**
     * Return consonant cplace 
     *   0-n/a l-labial a-alveolar p-palatal b-labio_dental d-dental v-velar g-?
     *
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class PH_CPlace implements ByteValuedFeatureProcessor
    {
        public String getName() { return "mary_ph_cplace"; }
        public String[] getValues() { return values.getStringValues(); }
        private ByteStringTranslator values;

        private PhoneSet phoneSet;
        
        public PH_CPlace(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
            this.values = new ByteStringTranslator(new String[] {
                    "0", "l", "a", "p", "b", "d", "v", "g"
            });
        }
    	/**
    	 * Performs some processing on the given item.
    	 *
    	 * @param  item  the item to process
    	 *
    	 * @return consonant cplace
    	 */
    	public byte process(Target target)
        {
            Item segment = getSegment(target);
    	    return values.get(phoneSet.getPhoneFeature(segment.toString(), "cplace"));
    	}
    }

    /**
     * Return consonant type 
     *   0-n/a s-stop f-fricative a-affricative n-nasal l-liquid r-r
     *
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class PH_CType implements ByteValuedFeatureProcessor {
        public String getName() { return "mary_ph_ctype"; }
        public String[] getValues() { return values.getStringValues(); }
        private ByteStringTranslator values;
        
        private PhoneSet phoneSet;
        
        public PH_CType(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
            this.values = new ByteStringTranslator(new String[] {
                    "0", "s", "f", "a", "n", "l", "r"
            });
        }
    	/**
    	 * Performs some processing on the given item.
    	 *
    	 * @param  item  the item to process
    	 *
    	 * @return consonant type
    	 */
    	public byte process(Target target) 
        {
            Item segment = getSegment(target);
            return values.get(phoneSet.getPhoneFeature(segment.toString(), "ctype"));
    	}
    }

    /**
     * Return consonant voicing 
     *   0=n/a +=on -=off
     *
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class PH_CVox implements ByteValuedFeatureProcessor {
        public String getName() { return "mary_ph_cvox"; }
        public String[] getValues() { return values.getStringValues(); }
        private ByteStringTranslator values;
        
        private PhoneSet phoneSet;
        
        public PH_CVox(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
            this.values = new ByteStringTranslator(new String[] {
                    "0", "+", "-"
            });
        }
        /**
         * Performs some processing on the given item.
         *
         * @param  item  the item to process
         *
         * @return consonant voicing
         */
        public byte process(Target target) 
        {
            Item segment = getSegment(target);
            return values.get(phoneSet.getPhoneFeature(segment.toString(), "cvox"));
        }
    }

    /**
     * Return vowel or consonant
     *   +=on -=off
     *
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class PH_VC implements ByteValuedFeatureProcessor {
        public String getName() { return "mary_ph_vc"; }
        public String[] getValues() { return values.getStringValues(); }
        private ByteStringTranslator values;
        
        private PhoneSet phoneSet;
        
        public PH_VC(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
            this.values = new ByteStringTranslator(new String[] {
                    "+", "-"
            });
        }
        /**
         * Performs some processing on the given item.
         *
         * @param  item  the item to process
         *
         * @return consonant voicing
         */
        public byte process(Target target) 
        {
            Item segment = getSegment(target);
            return values.get(phoneSet.getPhoneFeature(segment.toString(), "vc"));
        }
    }

    /**
     * Return vowel frontness
     *  0-n/a 1-front  2-mid 3-back
     *
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class PH_VFront implements ByteValuedFeatureProcessor {
        public String getName() { return "mary_ph_vfront"; }
        public String[] getValues() { return values.getStringValues(); }
        private ByteStringTranslator values;
        
        private PhoneSet phoneSet;
        
        public PH_VFront(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
            this.values = new ByteStringTranslator(new String[] {
                    "0", "1", "2", "3"
            });
        }
        /**
         * Performs some processing on the given item.
         *
         * @param  item  the item to process
         *
         * @return vowel frontness
         */
        public byte process(Target target) 
        {
            Item segment = getSegment(target);
            return values.get(phoneSet.getPhoneFeature(segment.toString(), "vfront"));
        }
    }

    /**
     * Return vowel height
     *   0-n/a 1-high 2-mid 3-low
     *
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class PH_VHeight implements ByteValuedFeatureProcessor {
        public String getName() { return "mary_ph_vheight"; }
        public String[] getValues() { return values.getStringValues(); }
        private ByteStringTranslator values;
        
        private PhoneSet phoneSet;
        
        public PH_VHeight(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
            this.values = new ByteStringTranslator(new String[] {
                    "0", "1", "2", "3"
            });
        }
        /**
         * Performs some processing on the given item.
         *
         * @param  item  the item to process
         *
         * @return vowel height
         */
        public byte process(Target target) 
        {
            Item segment = getSegment(target);
            return values.get(phoneSet.getPhoneFeature(segment.toString(), "vheight"));
        }
    }


    /**
     * Return vowel length
     *   0-n/a s-short l-long d-dipthong a-schwa
     *
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class PH_VLength implements ByteValuedFeatureProcessor {
        public String getName() { return "mary_ph_vlng"; }
        public String[] getValues() { return values.getStringValues(); }
        private ByteStringTranslator values;
        
        private PhoneSet phoneSet;
        
        public PH_VLength(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
            this.values = new ByteStringTranslator(new String[] {
                    "0", "s", "l", "d", "a"
            });
        }
        /**
         * Performs some processing on the given item.
         *
         * @param  item  the item to process
         *
         * @return vowel length
         */
        public byte process(Target target) 
        {
            Item segment = getSegment(target);
            return values.get(phoneSet.getPhoneFeature(segment.toString(), "vlng"));
        }
    }



    /**
     * Return vowel rnd (lip rounding)
     *   lip rounding  0=n/a +=on -=off
     *
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class PH_VRnd implements ByteValuedFeatureProcessor {
        public String getName() { return "mary_ph_vrnd"; }
        public String[] getValues() { return values.getStringValues(); }
        private ByteStringTranslator values;
        
        private PhoneSet phoneSet;
        
        public PH_VRnd(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
            this.values = new ByteStringTranslator(new String[] {
                    "0", "+", "-"
            });
        }
        /**
         * Performs some processing on the given item.
         *
         * @param  item  the item to process
         *
         * @return lip rounding for vowels
         */
        public byte process(Target target) 
        {
            Item segment = getSegment(target);
            return values.get(phoneSet.getPhoneFeature(segment.toString(), "vrnd"));
        }
    }

    /**
     * Determines the onset size of this syllable
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SylOnsetSize implements FeatureProcessor {
        public String getName() { return "syl_onsetsize"; }

        private PhoneSet phoneSet;
        
        public SylOnsetSize(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
        }
	/**
	 * Performs some processing on the given item.
	 *
	 * @param  syl  the item to process
	 *
	 * @return onset size of this syllable
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item syl) throws ProcessException {
	    int count = 0;
	    Item daughter = syl.getItemAs(
                Relation.SYLLABLE_STRUCTURE).getDaughter();
	    while (daughter != null) {
		if ("+".equals(phoneSet.getPhoneFeature(daughter.toString(), "vc"))) {
		    break;
		}
		count++;
		daughter = daughter.getNext();
	    }
	    return Integer.toString(rail(count));
	}
    }


    /**
     * Determines the coda size
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SylCodaSize implements FeatureProcessor {
        public String getName() { return "syl_codasize"; }

        private PhoneSet phoneSet;
        
        public SylCodaSize(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
        }
	/**
	 * Performs some processing on the given item.
	 *
	 * @param  syl  the item to process
	 *
	 * @return coda size
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item syl) throws ProcessException {
	    int count = 0;
	    Item daughter = syl.getItemAs(
                Relation.SYLLABLE_STRUCTURE).getLastDaughter();

	    while (daughter != null) {
		if ("+".equals(phoneSet.getPhoneFeature(daughter.toString(), "vc"))) {
		    break;
		}

		daughter = daughter.getPrevious();
		count++;
	    }
	    return Integer.toString(rail(count));
	}
    }


    /**
     * Checks for fricative
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegCodaFric implements FeatureProcessor {
        public String getName() { return "seg_coda_fric"; }

        private PhoneSet phoneSet;
        
        public SegCodaFric(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
        }
	/**
	 * Performs some processing on the given item.
	 *
	 * @param  seg  the item to process
	 * 
	 * @return "1" if fricative; else "0"
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item seg) throws ProcessException {
	    return segCodaCtype(seg, "f",phoneSet);
	}
    }

    /**
     * Checks for fricative
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegOnsetFric implements FeatureProcessor {
        public String getName() { return "seg_onset_fric"; }

        private PhoneSet phoneSet;
        
        public SegOnsetFric(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
        }
	/**
	 * Performs some processing on the given item.
	 *
	 * @param  seg  the item to process
	 * 
	 * @return "1" if fricative; else "0"
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item seg) throws ProcessException {
	    return segOnsetCtype(seg, "f",phoneSet);
	}
    }



    /**
     * Checks for coda stop
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegCodaStop implements FeatureProcessor {
        public String getName() { return "seg_coda_stop"; }

        private PhoneSet phoneSet;
        
        public SegCodaStop(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
        }
	/**
	 * Performs some processing on the given item.
	 *
	 * @param  seg  the item to process
	 *
	 * @return if coda stop "1"; otherwise "0"
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item seg) throws ProcessException {
	    return segCodaCtype(seg, "s",phoneSet);
	}
    }

    /**
     * Checks for onset stop
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegOnsetStop implements FeatureProcessor {
        public String getName() { return "seg_onset_stop"; }

        private PhoneSet phoneSet;
        
        public SegOnsetStop(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
        }
	/**
	 * Performs some processing on the given item.
	 *
	 * @param  seg  the item to process
	 *
	 * @return if Onset Stop "1"; otherwise "0"
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item seg) throws ProcessException {
	    return segOnsetCtype(seg, "s",phoneSet);
	}
    }

    /**
     * Checks for coda nasal
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegCodaNasal implements FeatureProcessor {
        public String getName() { return "seg_coda_nasal"; }

        private PhoneSet phoneSet;
        
        public SegCodaNasal(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
        }
	/**
	 * Performs some processing on the given item.
	 *
	 * @param  seg  the item to process
	 *
	 * @return if coda stop "1"; otherwise "0"
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item seg) throws ProcessException {
	    return segCodaCtype(seg, "n",phoneSet);
	}
    }

    /**
     * Checks for onset nasal
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegOnsetNasal implements FeatureProcessor {
        public String getName() { return "seg_onset_nasal"; }

        private PhoneSet phoneSet;
        
        public SegOnsetNasal(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
        }
	/**
	 * Performs some processing on the given item.
	 *
	 * @param  seg  the item to process
	 *
	 * @return if Onset Stop "1"; otherwise "0"
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item seg) throws ProcessException {
	    return segOnsetCtype(seg, "n",phoneSet);
	}
    }

    /**
     * Checks for coda glide
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegCodaGlide implements FeatureProcessor {
        public String getName() { return "seg_coda_glide"; }

        private PhoneSet phoneSet;
        
        public SegCodaGlide(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
        }
	/**
	 * Performs some processing on the given item.
	 *
	 * @param  seg  the item to process
	 *
	 * @return if coda stop "1"; otherwise "0"
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item seg) throws ProcessException {
	    if (segCodaCtype(seg, "r",phoneSet).equals("0")) {
		return segCodaCtype(seg, "l",phoneSet);
	    }
	    return "1";
	}
    }

    /**
     * Checks for onset glide
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegOnsetGlide implements FeatureProcessor {
        public String getName() { return "seg_onset_glide"; }

        private PhoneSet phoneSet;
        
        public SegOnsetGlide(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
        }
	/**
	 * Performs some processing on the given item.
	 *
	 * @param  seg  the item to process
	 *
	 * @return if coda stop "1"; otherwise "0"
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item seg) throws ProcessException {
	    if (segOnsetCtype(seg, "r",phoneSet).equals("0")) {
		return segOnsetCtype(seg, "l",phoneSet);
	    }
	    return "1";
	}
    }


    /**
     * Checks for onset coda 
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegOnsetCoda implements FeatureProcessor {
        public String getName() { return "seg_onsetcoda"; }

        private PhoneSet phoneSet;
        
        public SegOnsetCoda(PhoneSet phoneSet){
            this.phoneSet = phoneSet;
        }
	/**
	 * Performs some processing on the given item.
	 *
	 * @param  seg  the item to process
	 *
	 * @return if onset coda "1"; otherwise "0"
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item seg) throws ProcessException {
	    Item s = seg.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (s == null) {
                return "coda";
            }

            s = s.getNext();
	    while (s != null) {
		if ("+".equals(phoneSet.getPhoneFeature(s.toString(), "vc"))) {
		    return "onset";
		}

		s = s.getNext();
	    }
            
	    return "coda";
	}
    }

}