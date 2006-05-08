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
import com.sun.speech.freetts.util.Utilities;
import com.sun.speech.freetts.FeatureProcessor;
import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.ProcessException;

import de.dfki.lt.mary.unitselection.cart.PathExtractor;
import de.dfki.lt.mary.unitselection.cart.PathExtractorImpl;

import java.util.regex.Pattern;



/**
 * Provides the set of language independent feature processors that are used 
 * as part of the CART processing.
 */
public class GenericFeatureProcessors {

    private final static PathExtractor FIRST_SYLLABLE_PATH = 
	new PathExtractorImpl(
      "R:SylStructure.parent.R:Phrase.parent.daughter.R:SylStructure.daughter",
       false);

    private final static PathExtractor LAST_SYLLABLE_PATH = 
	new PathExtractorImpl(
      "R:SylStructure.parent.R:Phrase.parent.daughtern.R:SylStructure.daughter",
      false);

    private final static PathExtractor LAST_LAST_SYLLABLE_PATH = 
	new PathExtractorImpl(
  "R:SylStructure.parent.R:Phrase.parent.daughtern.R:SylStructure.daughtern",
      false);

    private final static PathExtractor SUB_PHRASE_PATH = 
	new PathExtractorImpl("R:SylStructure.parent.R:Phrase.parent.p", false);

    private final static Pattern DOUBLE_PATTERN 
	= Pattern.compile(USEnglish.RX_DOUBLE);

    private final static Pattern DIGITS_PATTERN  
	= Pattern.compile(USEnglish.RX_DIGITS);
    
    // no instances
    private GenericFeatureProcessors() {}

    /**
     * Classifies the type of word break
     *
     * @param  item  the item to process
     *
     * @return  "4" for a big break, "3" for  a break; otherwise "1"
     *
     * @throws ProcessException if an exception occurred during the
     * processing
     */
    public static String wordBreak(Item item) throws ProcessException {
        Item ww = item.getItemAs(Relation.PHRASE);
        if (ww == null || ww.getNext() != null) {
            return "1";
        } else {
            String pname = ww.getParent().toString();
            if (pname.equals("BB")) {
                return "4";
            } else if (pname.equals("B")) {
                	return "3";
            } else {
                return "1";
            }
        }
    }

    /**
     * Gets the punctuation associated with the word
     *
     * @param  item  the word to process
     *
     * @return  the punctuation associated with the word
     *
     * @throws ProcessException if an exception occurred during the
     * processing
     */
    public static String wordPunc(Item item) throws ProcessException {
        Item ww = item.getItemAs(Relation.TOKEN);
        if (ww != null && ww.getNext() != null) {
            return "";
        } else {
            if (ww != null && ww.getParent() != null) {
                return ww.getParent().getFeatures().getString("punc");
            } else {
                return "";
            }
        }
    }

    
    /**
     * Determines if the given item is accented
     *
     * @param item the item of interest
     *
     * @return <code>true</code> if the item is accented, otherwise
     * <code>false</code>
     */
    private static boolean isAccented(Item item) {
        return (item.getFeatures().isPresent("accent") ||
                item.getFeatures().isPresent("endtone"));
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
     * Returns as an Integer the number of syllables in the given
     * word.  This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class WordNumSyls implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  item  the item to process
	 *
	 * @return the number of syllables in the given word
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item item) throws ProcessException {
	    int count = 0;
	    Item daughter = item.getItemAs(
                Relation.SYLLABLE_STRUCTURE).getDaughter();
	    while (daughter != null) {
		count++;
		daughter = daughter.getNext();
	    }
	    return Integer.toString(rail(count));
	}
    }

    /**
     * Counts the number of accented syllables since the last major break.
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class AccentedSylIn implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  item  the item to process
	 *
	 * @return the number of accented syllables since the last
	 *    major break
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item item) throws ProcessException {
	    int count = 0;
	    Item ss = item.getItemAs(Relation.SYLLABLE);
	    Item firstSyllable  = (Item) FIRST_SYLLABLE_PATH.findTarget(item);

	    for (Item p = ss; p != null; p = p.getPrevious() )  {
		if (isAccented(p)) {
		    count++;
		}
		if (p.equalsShared(firstSyllable)) {
		    break;
		}
	    }
	    return Integer.toString(rail(count));
	}
    }


    /**
     * Counts the number of stressed syllables since the last major break.
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class StressedSylIn implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  item  the item to process
	 *
	 * @return the number of stresses syllables since the last
	 * major break
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item item) throws ProcessException {
	    int count = 0;
	    Item ss = item.getItemAs(Relation.SYLLABLE);
	    Item firstSyllable  = (Item) FIRST_SYLLABLE_PATH.findTarget(item);

	    // this should include the first syllable, but
	    // flite 1.1 and festival don't.

	    for (Item p = ss.getPrevious(); 
		    p != null && !p.equalsShared(firstSyllable);
		    p = p.getPrevious() )  {
		if ("1".equals(p.getFeatures().getString("stress"))) {
		    count++;
		}
	    }
	    return Integer.toString(rail(count));
	}
    }

    /**
     * Counts the number of stressed syllables since the last major break.
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SylIn implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  item  the item to process
	 * 
	 * @return the number of stressed syllables since the last
	 * major break
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item item) throws ProcessException {
	    int count = 0;
	    Item ss = item.getItemAs(Relation.SYLLABLE);
	    Item firstSyllable  = (Item) FIRST_SYLLABLE_PATH.findTarget(item);

	    for (Item p = ss; p != null; p = p.getPrevious(), count++ )  {
		if (p.equalsShared(firstSyllable)) {
		    break;
		}
	    }
	    return Integer.toString(rail(count));
	}
    }

    /**
     * Counts the number of stressed syllables since the last major break.
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SylOut implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  item  the item to process
	 *
	 * @return the number of stressed syllables since the last
	 * major break
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item item) throws ProcessException {
	    int count = 0;
	    Item ss = item.getItemAs(Relation.SYLLABLE);
	    Item firstSyllable  = (Item)LAST_LAST_SYLLABLE_PATH.findTarget(item);

	    for (Item p = ss; p != null; p = p.getNext() )  {
		if (p.equalsShared(firstSyllable)) {
		    break;
		}
		count++;
	    }
	    return Integer.toString(rail(count));
	}
    }
    
    /**
     * Counts the number of stressed syllables until the next major break.
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class StressedSylOut implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  item  the item to process
	 *
     	 * @return the number of stressed syllables until the next major break
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item item) throws ProcessException {
	    int count = 0;
	    Item ss = item.getItemAs(Relation.SYLLABLE);
	    Item lastSyllable  = (Item)LAST_SYLLABLE_PATH.findTarget(item);

	    for (Item p = ss.getNext(); p != null; p = p.getNext() )  {
		if ("1".equals(p.getFeatures().getString("stress"))) {
		    count++;
		}
		if (p.equalsShared(lastSyllable)) {
		    break;
		}
	    }
	    return Integer.toString(rail(count));
	}
    }

    /**
     * Returns the length of the string. (generally this is a digit
     * string)
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class NumDigits implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  item  the item to process
	 *
	 * @return the length of the string
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item item) throws ProcessException {
	    String name = item.getFeatures().getString("name");
	    return Integer.toString(rail(name.length()));
	}
    }

    /**
     * Returns true ("1") if the given item is a number between 0 and
     * 32 exclusive, otherwise, returns "0".
     * string)
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class MonthRange implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  item  the item to process
	 *
	 * @return returns "1" if the given item is a number between 0
	 * and 32 (exclusive) otherwise returns "0"
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item item) throws ProcessException {
	    int v = Integer.parseInt(item.getFeatures().getString("name"));
	    if ((v > 0) && (v < 32)) {
		return "1";
	    } else {
		return "0";
	    }
	}
    }


    /**
     * Checks to see if the given syllable is accented.
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class Accented implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 * @param  item  the item to process
	 *
	 * @return "1" if the syllable is accented; otherwise "0"
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item item) throws ProcessException {
	    if (isAccented(item)) {
		return "1";
	    } else {
		return "0";
	    }
	}
    }

    /**
     * Find the last accented syllable
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class LastAccent implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  item  the item to process
	 * 
	 * @return the count of the last accented syllable
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item item) throws ProcessException {
	    int count = 0;

	    for (Item p = item.getItemAs(Relation.SYLLABLE); 
		      p != null; p = p.getPrevious(), count++)  {
		if (isAccented(p)) {
		    break;
		}
	    }
	    return Integer.toString(rail(count));
	}
    }

    /**
     * Finds the position of the phoneme in the syllable
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class PosInSyl implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  item  the item to process
	 *
	 * @return the position of the phoneme in the syllable
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item item) throws ProcessException {
	    int count = -1;

	    for (Item p = item.getItemAs(Relation.SYLLABLE_STRUCTURE); 
		      p != null; p = p.getPrevious() )  {
		count++;
	    }
	    return Integer.toString(rail(count));
	}
    }

    /**
     * Classifies the the syllable as single, initial, mid or final.
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class PositionType implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  item  the item to process
	 *
	 * @return classifies the syllable as "single", "final",
	 * "initial" or "mid"
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item item) throws ProcessException {
	    String type;

	    Item s = item.getItemAs(Relation.SYLLABLE_STRUCTURE); 
	    if (s == null) {
		type = "single";
	    } else if (s.getNext() == null) {
		if (s.getPrevious() == null) {
		    type = "single";
		} else {
		    type = "final";
		}
	    } else if (s.getPrevious() == null) {
		type = "initial";
	    } else {
		type = "mid";
	    }
	    return type;
	}
    }


    
    /**
     * Determines the break level after this syllable
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SylBreak implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  syl  the item to process
	 *
	 * @return the break level after this syllable
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item syl) throws ProcessException {
        Utilities.debug("SylBreak: Got item '" + syl + "' in the '" + syl.getOwnerRelation().getName() + "' relation.");
	    Item ss = syl.getItemAs(Relation.SYLLABLE_STRUCTURE);
	    if (ss == null) {
            Utilities.debug("SylBreak: Cannot get this as SYLLABLE_STRUCTURE item");
		return "1";
	    } else if (ss.getNext() != null) {
            Utilities.debug("SylBreak: this is not the last syllable in this word");
		return "0";
	    } else if (ss.getParent() == null) {
		return "1";
	    } else {
            Utilities.debug("SylBreak: this is word-final, calculate the wordBreak.");
		return wordBreak(ss.getParent());
	    }
	}
    }



    /**
     * Determines the word break.
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class WordBreak implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  word  the item to process
	 *
	 * @return the break level for this word
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item word) throws ProcessException {
	    return wordBreak(word);
	}
    }

    /**
     * Determines the word punctuation.
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class WordPunc implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  word  the item to process
	 *
	 * @return the punctuation for this word
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item word) throws ProcessException {
	    return wordPunc(word);
	}
    }

    
    /**
     * Counts the number of phrases before this one.
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SubPhrases implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  item  the item to process
	 *
	 * @return the number of phrases before this one
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item item) throws ProcessException {
	    int count = 0;
	    Item inPhrase  = (Item) SUB_PHRASE_PATH.findTarget(item);

	    for (Item p = inPhrase; p != null; p = p.getPrevious() )  {
		count++;
	    }
	    return Integer.toString(rail(count));
	}
    }

    /**
     * Returns the duration of the given segment
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SegmentDuration implements FeatureProcessor {

	/**
	 * Performs some processing on the given item.
	 *
	 * @param  seg  the item to process
	 *
	 * @return the duration of the segment as a string.
	 *
	 * @throws ProcessException if an exception occurred during the
	 * processing
	 */
	public String process(Item seg) throws ProcessException {
	    if (seg == null) {
		return "0";
	    } else if (seg.getPrevious() == null) {
		return seg.getFeatures().getObject("end").toString();
	    } else {
		return Float.toString(
			seg.getFeatures().getFloat("end") -
		        seg.getPrevious().getFeatures().getFloat("end")
		   );
	    }
	}
    }

    /**
     * Checks if segment is sylfinal
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class SylFinal implements FeatureProcessor{
        
        public String process (Item seg) throws ProcessException{
            Item sylItem = seg.getItemAs(Relation.SYLLABLE_STRUCTURE); 
    		if (sylItem == null || sylItem.getNext() != null){
    		    return "0";
    		}else{ return "1";}
        }
    }
    
    /**
     * Checks if segment is a pause
     * This is a feature processor. A feature processor takes an item,
     * performs some sort of processing on the item and returns an object.
     */
    public static class LispIsPau implements FeatureProcessor{
        
        /**
         * Check if segment is a pause 
         *@param seg the segment
         *@return 0 if false, 1 if true
         *@throws ProcessException
         */
        public String process (Item seg) throws ProcessException{
            Item segItem = seg.getItemAs(Relation.SEGMENT);
            if (segItem == null || !(segItem.toString().equals("pau"))){
                return "0";}
            else{return "1";}
        }
    }
    
    /**
     * Calculates the pitch of a segment
     * This processor should be used by target items only
     */
    public static class Seg_Pitch implements FeatureProcessor{
        
        public String process (Item seg) throws ProcessException{
            //System.out.println("Looking for pitch...");
            //get mid position of segment
            float mid;
            float end = seg.getFeatures().getFloat("end"); 
            Item prev = seg.getPrevious();
            if (prev == null) {
                mid = end/2;
            } else {
                float prev_end = prev.getFeatures().getFloat("end");
                mid = prev_end + (end - prev_end)/2;
            }
            Relation targetRelation = seg.getUtterance().getRelation("Target");
            //if segment has no target relation, you can not calculate
            //the segment pitch
            if (targetRelation == null){
                return "0.0";
            }
            //get F0 and position of previous and next target
            Item nextTargetItem = targetRelation.getHead();
            while (nextTargetItem != null && nextTargetItem.getFeatures().getFloat("pos") < mid) {
                nextTargetItem = nextTargetItem.getNext();
            }
            if (nextTargetItem == null) return "0.0";
            Item lastTargetItem = nextTargetItem.getPrevious();
            if (lastTargetItem == null) return "0.0";
            float lastF0 = lastTargetItem.getFeatures().getFloat("f0");
            float lastPos = lastTargetItem.getFeatures().getFloat("pos");
            float nextF0 = nextTargetItem.getFeatures().getFloat("f0");
            float nextPos = nextTargetItem.getFeatures().getFloat("pos");
            assert lastPos <= mid && mid <= nextPos;
            //build a linear function (f(x) = slope*x+intersectionYAxis)
            float slope = (nextF0 - lastF0) / (nextPos - lastPos);
            float intersectionYAxis = lastF0 - slope*lastPos;
            //calculate the pitch
            float pitch = slope*mid+intersectionYAxis;
            assert lastF0 <= pitch && pitch <= nextF0 || nextF0 <= pitch && pitch <= lastF0;

            if (Float.isNaN(pitch)){
                pitch = (float) 0.0;
            }
            return Float.toString(pitch);
        }
    }
}
