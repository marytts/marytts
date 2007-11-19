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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.ProcessException;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.en.us.USEnglish;

import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;
import de.dfki.lt.mary.unitselection.HalfPhoneTarget;
import de.dfki.lt.mary.unitselection.Target;
import de.dfki.lt.mary.unitselection.featureprocessors.MaryGenericFeatureProcessors.TargetItemNavigator;
import de.dfki.lt.mary.unitselection.featureprocessors.MaryGenericFeatureProcessors.WordNavigator;
import de.dfki.lt.mary.util.ByteStringTranslator;
import de.dfki.lt.util.FSTLookup;



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
        Item daughter = seg.getItemAs(Relation.SYLLABLE_STRUCTURE).getParent().getLastDaughter();

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
     * The phoneme symbol for the given target.
     * @author Marc Schr&ouml;der
     *
     */
    public static class Phoneme implements ByteValuedFeatureProcessor
    {
        protected String name;
        protected ByteStringTranslator values;
        protected TargetItemNavigator navigator;
        /**
         * Initialise a phoneme feature processor. 
         * @param name the name of the feature
         * @param possibleValues the list of possible phoneme values for the phonetic alphabet used,
         * plus the value "0"=n/a.
         * @param segmentNavigator a navigator returning a segment with respect to the target.
         */
        public Phoneme(String name, String[] possibleValues, TargetItemNavigator segmentNavigator)
        {
            this.name = name;
            this.values = new ByteStringTranslator(possibleValues);
            this.navigator = segmentNavigator;
        }
        public String getName() { return name; }
        public String[] getValues() { return values.getStringValues(); }
        public byte process(Target target)
        {
            Item segment = navigator.getItem(target);
            if (segment == null) return values.get("0");
            String value = segment.getFeatures().getString("name");
            if (value == null) return values.get("0");
            String sampa = FreeTTSVoices.getMaryVoice(segment.getUtterance().getVoice()).voice2sampa(value);
            return values.get(sampa);
        }
    }

    /**
     * The unit name for the given half phone target.
     * @author Marc Schr&ouml;der
     *
     */
    public static class HalfPhoneUnitName implements ByteValuedFeatureProcessor
    {
        protected String name;
        protected ByteStringTranslator values;
        protected TargetItemNavigator navigator;
        /**
         * Initialise a UnitName feature processor. 
         * @param name the name of the feature
         * @param phoneset the phonetic alphabet used
         * @param segmentNavigator a navigator returning a segment with respect to the target.
         */
        public HalfPhoneUnitName(String[] possiblePhonemes)
        {
            this.name = "mary_halfphone_unitname";
            String[] possibleValues = new String[2*possiblePhonemes.length+1];
            possibleValues[0] = "0"; // the "n/a" value
            for (int i=0; i<possiblePhonemes.length; i++) {
                possibleValues[2*i+1] = possiblePhonemes[i]+"_L";
                possibleValues[2*i+2] = possiblePhonemes[i]+"_R";
            }
            this.values = new ByteStringTranslator(possibleValues);
            this.navigator = new SegmentNavigator();
        }
        public String getName() { return name; }
        public String[] getValues() { return values.getStringValues(); }
        public byte process(Target target)
        {
            if (!(target instanceof HalfPhoneTarget))
                throw new IllegalArgumentException("This feature processor should only be called for half-phone unit targets, got a "+ target.getClass()+"!");
            HalfPhoneTarget hpTarget = (HalfPhoneTarget) target;
            Item segment = navigator.getItem(target);
            if (segment == null) return values.get("0");
            String phoneLabel = segment.getFeatures().getString("name");
            if (phoneLabel == null) return values.get("0");
            String sampa = FreeTTSVoices.getMaryVoice(segment.getUtterance().getVoice()).voice2sampa(phoneLabel);
            String unitLabel = sampa + (hpTarget.isLeftHalf() ? "_L" : "_R");
            return values.get(unitLabel);
        }
    }


    /**
     * A parametrisable class which can retrieve all sorts of phone features,
     * given a phone set.
     * @author Marc Schr&ouml;der
     *
     */
    public static class PhoneFeature implements ByteValuedFeatureProcessor
    {
        protected PhoneSet phoneSet;
        protected String name;
        protected String phonesetQuery;
        protected ByteStringTranslator values;
        protected TargetItemNavigator navigator;
        public PhoneFeature(PhoneSet phoneSet, String name, String phonesetQuery,
                String[] possibleValues, TargetItemNavigator segmentNavigator)
        {
            this.phoneSet = phoneSet;
            this.name = name;
            this.phonesetQuery = phonesetQuery;
            this.values = new ByteStringTranslator(possibleValues);
            this.navigator = segmentNavigator;
        }
        public String getName() { return name; }
        public String[] getValues() { return values.getStringValues(); }
        public byte process(Target target)
        {
            Item segment = navigator.getItem(target);
            if (segment == null) return values.get("0");
            String value = phoneSet.getPhoneFeature(segment.toString(), phonesetQuery);
            if (value == null) return values.get("0");
            return values.get(value);
        }
    }

    
    /**
     * Returns the part-of-speech.
     *
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class Pos implements ByteValuedFeatureProcessor
    {
        private ByteStringTranslator values;
        private TargetItemNavigator navigator;

        public String getName() { return "mary_pos"; }
        public String[] getValues() { return values.getStringValues(); }
        
        public Pos(String[] posValues)
        {
            this.values = new ByteStringTranslator(posValues);
            this.navigator = new WordNavigator();
        }
        
    	/**
    	 * Performs some processing on the given item.
    	 * @param  item  the item to process
    	 * @return a guess at the part-of-speech for the item
    	 */
    	public byte process(Target target)
        {
            Item word = navigator.getItem(target);
            if (word == null) return values.get("0");
    	    String pos = word.getFeatures().getString("pos");
            if (pos == null) return values.get("0");
            pos = pos.trim();
            if (values.contains(pos))
                return values.get(pos);
            return values.get("0");
    	}
    }

    /**
     * Returns a guess of the part-of-speech.
     *
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class Gpos implements ByteValuedFeatureProcessor
    {
        private Map posConverter;
        private ByteStringTranslator values;
        private TargetItemNavigator navigator;

        public String getName() { return "mary_gpos"; }
        public String[] getValues() { return values.getStringValues(); }
        
        public Gpos(Map posConverter)
        {
            this.posConverter = posConverter;
            this.values = new ByteStringTranslator(new String[] {
                    "0",
                    "in", // Preposition or subordinating conjunction
                    "to", // to
                    "det", // determiner
                    "md", // modal
                    "cc", // coordinating conjunction
                    "wp", // w-pronouns 
                    "pps", // possive pronouns
                    "aux", // auxiliary verbs
                    "punc", // punctuation
                    "content" // content words
            });
            this.navigator = new WordNavigator();
        }
        
        /**
         * Performs some processing on the given item.
         * @param  item  the item to process
         * @return a guess at the part-of-speech for the item
         */
        public byte process(Target target)
        {
            Item word = navigator.getItem(target);
            if (word == null) return values.get("0");
            String pos = word.getFeatures().getString("pos");
            if (pos == null) return values.get("0");
            pos = pos.trim();
            if (posConverter.containsKey(pos)){
                pos = (String)posConverter.get(pos);}
            return values.get(pos);
        }
    }

    /**
     * Checks for onset coda 
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegOnsetCoda implements ByteValuedFeatureProcessor
    {
        protected TargetItemNavigator navigator;
        protected ByteStringTranslator values;
        private PhoneSet phoneSet;

        public SegOnsetCoda(PhoneSet phoneSet)
        {
            this.phoneSet = phoneSet;
            this.navigator = new SegmentNavigator();
            this.values = new ByteStringTranslator(new String[] {"0", "onset", "coda"});
        }
        
        public String getName() { return "mary_onsetcoda"; }
        public String[] getValues() { return values.getStringValues(); }

        public byte process(Target target)
        {
            Item s = navigator.getItem(target);
            if (s == null) {
                return values.get("coda");
            }
            s = s.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (s == null) {
                return values.get("coda");
            }
    
            s = s.getNext();
            while (s != null) {
                if ("+".equals(phoneSet.getPhoneFeature(s.toString(), "vc"))) {
                    return values.get("onset");
                }
        
                s = s.getNext();
            }
            return values.get("coda");
        }
    }
    
    
    
    
    
    
    /**
     * The phone class for the given target.
     * @author Anna Hunecke
     *
     */
    public static class Selection_PhoneClass implements ByteValuedFeatureProcessor
    {
        protected String name;
        protected Map phones2Classes;
        protected ByteStringTranslator values;
        protected TargetItemNavigator navigator;
        /**
         * Initialise the feature processor. 
         * @param phones2Classes the mapping of phones to their classes
         * @param classes the available phone classes
         * @param segmentNavigator a navigator returning a segment with respect to the target.
         */
        public Selection_PhoneClass(Map phones2Classes, String[] classes, TargetItemNavigator segmentNavigator)
        {
            this.name = "mary_selection_next_phone_class";
            this.phones2Classes = phones2Classes;
            this.values = new ByteStringTranslator(classes);
            this.navigator = segmentNavigator;
        }
        
        public String getName() { 
            return name; 
        }
        
        public String[] getValues() { 
            return values.getStringValues(); 
        }
        
        /**
         * Give back the phone class of the target
         * 
         *@param target
         *@return the phone class of the target
         */
        public byte process(Target target){
            Item segment = navigator.getItem(target);
            if (segment == null) return values.get("0");
            String value = segment.getFeatures().getString("name");
            if (value == null) return values.get("0");
            String sampa = FreeTTSVoices.getMaryVoice(segment.getUtterance().getVoice()).voice2sampa(value);
            String phoneClass = (String) phones2Classes.get(sampa); 
            
            if (phoneClass == null){
                System.out.println("No phoneClass for phone "+sampa);
            }
            return values.get(phoneClass);
        }
    }
    
    
    
    
    
    
    
    
    
    
    ////////////////////////////////////////////////////////
    // TODO: Remove or convert old feature processors below.
    ////////////////////////////////////////////////////////

    
    
    /**
     * Determines the onset size of this syllable
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SylOnsetSize implements ByteValuedFeatureProcessor 
    {
        public String getName() { return "syl_onsetsize"; }
        public String[] getValues() { return null; }

        public byte process(Target target)
        {
            return 0;
        }

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
    public static class SylCodaSize implements ByteValuedFeatureProcessor {
        public String getName() { return "syl_codasize"; }
        public String[] getValues() { return null; }

        public byte process(Target target)
        {
            return 0;
        }

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
    	public String process(Item syl) throws ProcessException 
        {
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
    public static class SegCodaFric implements ByteValuedFeatureProcessor 
    {
        public String getName() { return "seg_coda_fric"; }
        public String[] getValues() { return null; }

        public byte process(Target target)
        {
            return 0;
        }

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
    	public String process(Item seg) throws ProcessException
        {
    	    return segCodaCtype(seg, "f",phoneSet);
    	}
    }

    /**
     * Checks for fricative
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegOnsetFric implements ByteValuedFeatureProcessor 
    {
        public String getName() { return "seg_onset_fric"; }
        public String[] getValues() { return null; }

        public byte process(Target target)
        {
            return 0;
        }

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
    	public String process(Item seg) throws ProcessException 
        {
    	    return segOnsetCtype(seg, "f",phoneSet);
    	}
    }



    /**
     * Checks for coda stop
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegCodaStop implements ByteValuedFeatureProcessor 
    {
        public String getName() { return "seg_coda_stop"; }
        public String[] getValues() { return null; }

        public byte process(Target target)
        {
            return 0;
        }

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
    	public String process(Item seg) throws ProcessException
        {
    	    return segCodaCtype(seg, "s",phoneSet);
    	}
    }

    /**
     * Checks for onset stop
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegOnsetStop implements ByteValuedFeatureProcessor
    {
        public String getName() { return "seg_onset_stop"; }
        public String[] getValues() { return null; }

        public byte process(Target target)
        {
            return 0;
        }

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
    	public String process(Item seg) throws ProcessException 
        {
    	    return segOnsetCtype(seg, "s",phoneSet);
    	}
    }

    /**
     * Checks for coda nasal
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegCodaNasal implements ByteValuedFeatureProcessor 
    {
        public String getName() { return "seg_coda_nasal"; }
        public String[] getValues() { return null; }

        public byte process(Target target)
        {
            return 0;
        }

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
    	public String process(Item seg) throws ProcessException
        {
    	    return segCodaCtype(seg, "n",phoneSet);
    	}
    }

    /**
     * Checks for onset nasal
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegOnsetNasal implements ByteValuedFeatureProcessor
    {
        public String getName() { return "seg_onset_nasal"; }
        public String[] getValues() { return null; }

        public byte process(Target target)
        {
            return 0;
        }

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
    	public String process(Item seg) throws ProcessException
        {
    	    return segOnsetCtype(seg, "n",phoneSet);
    	}
    }

    /**
     * Checks for coda glide
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegCodaGlide implements ByteValuedFeatureProcessor
    {
        public String getName() { return "seg_coda_glide"; }
        public String[] getValues() { return null; }

        public byte process(Target target)
        {
            return 0;
        }

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
    public static class SegOnsetGlide implements ByteValuedFeatureProcessor
    {
        public String getName() { return "seg_onset_glide"; }
        public String[] getValues() { return null; }

        public byte process(Target target)
        {
            return 0;
        }

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

    public static class WordFrequency implements ByteValuedFeatureProcessor
    {
        protected TargetItemNavigator navigator;
        protected ByteStringTranslator values;
        protected FSTLookup wordFrequencies;
        
        public WordFrequency(String fstFilename, String encoding)
        {
            this.navigator = new WordNavigator();
            try {
                if (fstFilename != null) 
                    this.wordFrequencies = new FSTLookup(fstFilename, encoding);
                else
                    this.wordFrequencies = null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            this.values = new ByteStringTranslator(new String[]
                 {"0", "1", "2", "3", "4", "5", "6", "7","8", "9"}
            );
        }
        
        public String getName() { return "mary_word_frequency"; }
        public String[] getValues() {
            return values.getStringValues();
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the frequency of the current word, on a ten-point scale
         * from 0=unknown=very rare to 9=very frequent.
         */
        public byte process(Target target)
        {
            Item word = navigator.getItem(target);
            if (word == null) return (byte)0;
            if (wordFrequencies != null) {
                String[] result = wordFrequencies.lookup(word.toString());
                if (result.length > 0) {
                    String freq = result[0];
                    if (values.contains(freq))
                        return values.get(freq);
                }
                
            }
            return (byte)0; // unknown word
        }
    }
    
    

}
