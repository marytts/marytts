/**
 * Portions Copyright 2006-2007 DFKI GmbH.
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


import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.ProcessException;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;

import de.dfki.lt.mary.unitselection.DiphoneTarget;
import de.dfki.lt.mary.unitselection.HalfPhoneTarget;
import de.dfki.lt.mary.unitselection.Target;
import de.dfki.lt.mary.util.ByteStringTranslator;
import de.dfki.lt.util.FSTLookup;

import java.util.List;
import java.util.ArrayList;

/**
 * A collection of feature processors that operate on Target objects.
 * Their names are all prefixed with "mary_" to make sure no confusion with the old FreeTTS feature processors occurs. 
 * @author schroed
 *
 */
public class MaryGenericFeatureProcessors
{
    /**
     * Navigate from a target to an item.
     * Classes implementing this interface will retrieve
     * meaningful items given the target.
     * @author Marc Schr&ouml;der
     */
    public static interface TargetItemNavigator
    {
        /**
         * Given the target, retrieve an item.
         * @param target
         * @return an item selected according to this navigator,
         * or null if there is no such item.
         */
        public Item getItem(Target target);
    }

    /**
     * Retrieve the segment belonging to this target.
     * @author Marc Schr&ouml;der
     *
     */
    public static class SegmentNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            return segment;
        }
    }

    /**
     * Retrieve the segment preceding the segment which belongs to this target.
     * @author Marc Schr&ouml;der
     *
     */
    public static class PrevSegmentNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            return segment.getPrevious();
        }
    }

    /**
     * Retrieve the segment two before the segment which belongs to this target.
     * @author Marc Schr&ouml;der
     *
     */
    public static class PrevPrevSegmentNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            Item prev = segment.getPrevious();
            if (prev == null) return null;
            return prev.getPrevious();
        }
    }

    /**
     * Retrieve the segment following the segment which belongs to this target.
     * @author Marc Schr&ouml;der
     *
     */
    public static class NextSegmentNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            return segment.getNext();
        }
    }

    /**
     * Retrieve the segment two after the segment which belongs to this target.
     * @author Marc Schr&ouml;der
     *
     */
    public static class NextNextSegmentNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            Item next = segment.getNext();
            if (next == null) return null;
            return next.getNext();
        }
    }

    /**
     * Retrieve the first segment in the word to which this target belongs.
     *
     */
    public static class FirstSegmentInWordNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            Item word = syllable.getParent();
            if (word == null) return null;
            Item firstSyl = word.getDaughter();
            if (firstSyl == null) return null;
            Item firstSeg = firstSyl.getDaughter();
            return firstSeg;
        }
    }

    /**
     * Retrieve the last segment in the word to which this target belongs.
     *
     */
    public static class LastSegmentInWordNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            Item word = syllable.getParent();
            if (word == null) return null;
            Item lastSyl = word.getLastDaughter();
            if (lastSyl == null) return null;
            Item lastSeg = lastSyl.getLastDaughter();
            return lastSeg;
        }
    }

    /**
     * Retrieve the first syllable in the word to which this target belongs.
     *
     */
    public static class FirstSyllableInWordNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            Item word = syllable.getParent();
            if (word == null) return null;
            Item firstSyl = word.getDaughter();
            return firstSyl;
        }
    }

    /**
     * Retrieve the last syllable in the word to which this target belongs.
     *
     */
    public static class LastSyllableInWordNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            Item word = syllable.getParent();
            if (word == null) return null;
            Item lastSyl = word.getLastDaughter();
            return lastSyl;
        }
    }

    /**
     * Retrieve the syllable belonging to this target.
     * @author Marc Schr&ouml;der
     *
     */
    public static class SyllableNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            return syllable;
        }
    }

    /**
     * Retrieve the syllable before the syllable belonging to this target.
     * @author Marc Schr&ouml;der
     *
     */
    public static class PrevSyllableNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            syllable = syllable.getItemAs(Relation.SYLLABLE);
            Item prevSyllable = syllable.getPrevious();
            return prevSyllable;
        }
    }

    /**
     * Retrieve the syllable two before the syllable belonging to this target.
     * @author Marc Schr&ouml;der
     *
     */
    public static class PrevPrevSyllableNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            syllable = syllable.getItemAs(Relation.SYLLABLE);
            Item prevSyllable = syllable.getPrevious();
            if (prevSyllable == null) return null;
            return prevSyllable.getPrevious();
        }
    }

    /**
     * Retrieve the syllable following the syllable belonging to this target.
     * @author Marc Schr&ouml;der
     *
     */
    public static class NextSyllableNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            syllable = syllable.getItemAs(Relation.SYLLABLE);
            Item nextSyllable = syllable.getNext();
            return nextSyllable;
        }
    }

    /**
     * Retrieve the syllable two after the syllable belonging to this target.
     * @author Marc Schr&ouml;der
     *
     */
    public static class NextNextSyllableNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            syllable = syllable.getItemAs(Relation.SYLLABLE);
            Item nextSyllable = syllable.getNext();
            if (nextSyllable == null) return null;
            return nextSyllable.getNext();
        }
    }

    /**
     * Retrieve the word belonging to this target.
     * @author Marc Schr&ouml;der
     *
     */
    public static class WordNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            Item word = syllable.getParent();
            return word;
        }
    }

    public static class FirstSyllableNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            Item word = syllable.getParent();
            if (word == null) return null;
            word = word.getItemAs(Relation.PHRASE);
            if (word == null) return null;
            Item phrase = word.getParent();
            if (phrase == null) return null;
            Item firstWord = phrase.getDaughter();
            if (firstWord == null) return null;
            firstWord = firstWord.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (firstWord == null) return null;
            Item firstSyllable = firstWord.getDaughter();
            return firstSyllable;
            
        }
    }

    public static class LastSyllableNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            Item word = syllable.getParent();
            if (word == null) return null;
            word = word.getItemAs(Relation.PHRASE);
            if (word == null) return null;
            Item phrase = word.getParent();
            if (phrase == null) return null;
            Item lastWord = phrase.getLastDaughter();
            if (lastWord == null) return null;
            lastWord = lastWord.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (lastWord == null) return null;
            Item lastSyllable = lastWord.getLastDaughter();
            return lastSyllable;
        }
    }

    public static class FirstWordInPhraseNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            Item word = syllable.getParent();
            if (word == null) return null;
            word = word.getItemAs(Relation.PHRASE);
            if (word == null) return null;
            Item phrase = word.getParent();
            if (phrase == null) return null;
            Item firstWord = phrase.getDaughter();
            return firstWord;
        }
    }

    public static class LastWordInPhraseNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            Item word = syllable.getParent();
            if (word == null) return null;
            word = word.getItemAs(Relation.PHRASE);
            if (word == null) return null;
            Item phrase = word.getParent();
            if (phrase == null) return null;
            Item lastWord = phrase.getLastDaughter();
            return lastWord;
        }
    }

    public static class FirstWordInSentenceNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            Item word = syllable.getParent();
            if (word == null) return null;
            word = word.getItemAs(Relation.WORD);
            if (word == null) return null;
            Relation wordRelation = word.getOwnerRelation();
            if (wordRelation == null) return null;
            return wordRelation.getHead();
        }
    }

    public static class LastWordInSentenceNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            Item word = syllable.getParent();
            if (word == null) return null;
            word = word.getItemAs(Relation.WORD);
            if (word == null) return null;
            Relation wordRelation = word.getOwnerRelation();
            if (wordRelation == null) return null;
            return wordRelation.getTail();
        }
    }

    public static class PhraseNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            Item word = syllable.getParent();
            if (word == null) return null;
            word = word.getItemAs(Relation.PHRASE);
            if (word == null) return null;
            Item phrase = word.getParent();
            return phrase;
        }
    }

    public static class FirstPhraseNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            Item word = syllable.getParent();
            if (word == null) return null;
            word = word.getItemAs(Relation.PHRASE);
            if (word == null) return null;
            Item phrase = word.getParent();
            if (phrase == null) return null;
            Item prevPhrase = phrase.getPrevious();
            while (prevPhrase != null) {
                phrase = prevPhrase;
                prevPhrase = phrase.getPrevious();
            }
            return phrase;
        }
    }

    public static class LastPhraseNavigator implements TargetItemNavigator
    {
        public Item getItem(Target target)
        {
            Item segment = target.getItem();
            if (segment == null) return null;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return null;
            Item syllable = segment.getParent();
            if (syllable == null) return null;
            Item word = syllable.getParent();
            if (word == null) return null;
            word = word.getItemAs(Relation.PHRASE);
            if (word == null) return null;
            Item phrase = word.getParent();
            if (phrase == null) return null;
            Item nextPhrase = phrase.getNext();
            while (nextPhrase != null) {
                phrase = nextPhrase;
                nextPhrase = phrase.getNext();
            }
            return phrase;
        }
    }





    // no instances
    protected MaryGenericFeatureProcessors()
    {
    }




    /**
     * Rails an int. flite never returns an int more than 19 from a feature
     * processor, we duplicate that behavior here so that our tests will match.
     * 
     * @param val
     *            the value to rail
     * 
     * @return val clipped to be betweein 0 and 19
     */
    private static int rail(int val)
    {
        return val > 19 ? 19 : val;
    }


    /**
     * Indicate whether a unit is an edge unit, which is never the case for a target.
     */
    public static class Edge implements ByteValuedFeatureProcessor
    {
        public String getName() { return "mary_edge"; }
        public String[] getValues()
        {
            return new String[] {"0", "start", "end" };
        }
        
        /**
         * This processor always returns 0 for targets.
         */
        public byte process(Target target)
        {
            return (byte)0;
        }
        
    }

    /**
     * Is the given half phone target a left or a right half?
     * @author Marc Schr&ouml;der
     *
     */
    public static class HalfPhoneLeftRight implements ByteValuedFeatureProcessor
    {
        protected ByteStringTranslator values;
        protected TargetItemNavigator navigator;
        /**
         * Initialise a HalfPhoneLeftRight feature processor. 
         */
        public HalfPhoneLeftRight()
        {
            this.values = new ByteStringTranslator(new String[] {
                    "0", "L", "R"
            });
            this.navigator = new SegmentNavigator();
        }
        public String getName() { return "mary_halfphone_lr"; }
        public String[] getValues() { return values.getStringValues(); }
        public byte process(Target target)
        {
            if (!(target instanceof HalfPhoneTarget))
                throw new IllegalArgumentException("This feature processor should only be called for half-phone unit targets!");
            HalfPhoneTarget hpTarget = (HalfPhoneTarget) target;
            String value = (hpTarget.isLeftHalf() ? "L" : "R");
            return values.get(value);
        }
    }
    
    /**
     * Sentence Style for the given target 
     * @author Sathish Chandra Pammi
     *
     */
    
    public static class SentenceStyle implements ByteValuedFeatureProcessor
    {
        protected ByteStringTranslator values;
        protected TargetItemNavigator navigator;
        /**
         * Initialise a SentenceStyle feature processor.
         */
        public SentenceStyle()
        {
            this.values = new ByteStringTranslator(new String[] {
                    "0", "neutral", "poker", "happy", "sad", "angry", "excited"
            });
            this.navigator = new SegmentNavigator();
        }
        public String getName() { return "mary_style"; }
        public String[] getValues() { return values.getStringValues(); }
        public byte process(Target target)
        {
            Item item = target.getItem();
            Utterance utt = item.getUtterance();
            String style = utt.getString("style");
            if(style == null) style = "0";
            return values.get(style);
        }
    }

    /**
     * Checks to see if the given syllable is accented. 
     */
    public static class Accented implements ByteValuedFeatureProcessor
    {
        protected String name; 
        protected TargetItemNavigator navigator;
        public Accented(String name, TargetItemNavigator syllableNavigator)
        {
            this.name = name;
            this.navigator = syllableNavigator;
        }
        public String getName() { return name; }
        public String[] getValues() {
            return new String[] {"0", "1"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return "1" if the syllable is accented; otherwise "0"
         */
        public byte process(Target target)
        {
            Item syllable = navigator.getItem(target);
            if (syllable != null && syllable.getFeatures().isPresent("accent")) {
                return (byte)1;
            } else {
                return (byte)0;
            }
        }
    }

    /**
     * Checks to see if the given syllable is stressed.
     */
    public static class Stressed implements ByteValuedFeatureProcessor
    {
        protected String name;
        protected TargetItemNavigator navigator;
        public Stressed(String name, TargetItemNavigator syllableNavigator)
        {
            this.name = name;
            this.navigator = syllableNavigator;
        }
        public String getName() { return name; }
        public String[] getValues() {
            return new String[] {"0", "1"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return "1" if the syllable is stressed; otherwise "0"
         */
        public byte process(Target target)
        {
            Item syllable = navigator.getItem(target);
            if (syllable == null) return 0;
            String value = syllable.getFeatures().getString("stress");
            if (value == null) return 0;
            byte stressValue = Byte.parseByte(value);
            if (stressValue > 1){
                //out of range, set to 1
                stressValue =1;
            }
            return stressValue;
        }
    }


    /**
     * Returns as an Integer the number of phrases in the current sentence. This is
     * a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class SentenceNumPhrases implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        public SentenceNumPhrases() {
            this.navigator = new FirstPhraseNavigator();
        }
        public String getName() { return "mary_sentence_numphrases"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * 
         * @param item
         *            the item to process
         * 
         * @return the number of phrases in the sentence
         */
        public byte process(Target target)
        {
            Item phrase = navigator.getItem(target);
            if (phrase == null) return (byte)0;
            int count = 1;
            Item next = phrase.getNext();
            while (next != null) {
                count++;
                next = next.getNext();
            }
            return (byte) rail(count);
        }
    }

    /**
     * Returns as an Integer the number of words in the current sentence. This is
     * a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class SentenceNumWords implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        public SentenceNumWords() {
            this.navigator = new FirstPhraseNavigator();
        }
        public String getName() { return "mary_sentence_numwords"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * 
         * @param item
         *            the item to process
         * 
         * @return the number of words in the sentence
         */
        public byte process(Target target)
        {
            Item phrase = navigator.getItem(target);
            if (phrase == null) return (byte)0;
            Item word = phrase.getDaughter();
            if (word == null) return (byte)0;
            word = word.getItemAs(Relation.WORD);
            if (word == null) return (byte)0;
            int count = 1;
            Item next = word.getNext();
            while (next != null) {
                count++;
                next = next.getNext();
            }
            return (byte) rail(count);
        }
    }

    /**
     * Returns as an Integer the number of phrases in the current sentence. This is
     * a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class PhraseNumSyls implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator lastSyllableNavigator;
        public PhraseNumSyls() {
            this.navigator = new FirstSyllableNavigator();
            this.lastSyllableNavigator = new LastSyllableNavigator();
        }
        public String getName() { return "mary_phrase_numsyls"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * 
         * @param item
         *            the item to process
         * 
         * @return the number of words in the phrase
         */
        public byte process(Target target)
        {
            Item syllable = navigator.getItem(target);
            if (syllable == null) return (byte)0;
            syllable = syllable.getItemAs(Relation.SYLLABLE);
            if (syllable == null) return (byte)0;
            Item last = lastSyllableNavigator.getItem(target);
            int count = 1;
            
            for (Item next = syllable.getNext(); next != null; next = next.getNext()) {
                if (next.equalsShared(last)) break;
                count++;
            }
            return (byte) rail(count);
        }
    }

    /**
     * Returns as an Integer the number of phrases in the current sentence. This is
     * a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class PhraseNumWords implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        public PhraseNumWords() {
            this.navigator = new FirstWordInPhraseNavigator();
        }
        public String getName() { return "mary_phrase_numwords"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * 
         * @param item
         *            the item to process
         * 
         * @return the number of words in the phrase
         */
        public byte process(Target target)
        {
            Item word = navigator.getItem(target);
            if (word == null) return (byte)0;
            word = word.getItemAs(Relation.PHRASE);
            if (word == null) return (byte)0;
            int count = 1;
            Item next = word.getNext();
            while (next != null) {
                count++;
                next = next.getNext();
            }
            return (byte) rail(count);
        }
    }


    /**
     * Returns as an Integer the number of syllables in the given word. This is
     * a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class WordNumSyls implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        public WordNumSyls() {
            this.navigator = new WordNavigator();
        }
        public String getName() { return "mary_word_numsyls"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * 
         * @param item
         *            the item to process
         * 
         * @return the number of syllables in the given word
         */
        public byte process(Target target)
        {
            Item word = navigator.getItem(target);
            if (word == null) return (byte)0;
            word = word.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (word == null) return (byte)0;
            int count = 0;
            Item daughter = word.getDaughter();
            while (daughter != null) {
                count++;
                daughter = daughter.getNext();
            }
            return (byte) rail(count);
        }
    }

    /**
     * Returns as an Integer the number of segments in the given word. This is
     * a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class WordNumSegs implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        public WordNumSegs() {
            this.navigator = new WordNavigator();
        }
        public String getName() { return "mary_word_numsegs"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }
        /**
         * Performs some processing on the given item.
         * 
         * @param item
         *            the item to process
         * 
         * @return the number of segments in the given word
         */
        public byte process(Target target)
        {
            Item word = navigator.getItem(target);
            if (word == null) return (byte)0;
            word = word.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (word == null) return (byte)0;
            int count = 0;
            Item syllable = word.getDaughter();
            while (syllable != null) {
                Item seg = syllable.getDaughter();
                while (seg != null) {
                    count++;
                    seg = seg.getNext();
                }
                syllable = syllable.getNext();
            }
            return (byte) rail(count);
        }
    }

    /**
     * Returns as an Integer the number of segments in the current syllable. This is
     * a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class SylNumSegs implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        public SylNumSegs() {
            this.navigator = new SyllableNavigator();
        }
        public String getName() { return "mary_syl_numsegs"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }
        /**
         * Performs some processing on the given item.
         * 
         * @param item
         *            the item to process
         * 
         * @return the number of segments in the given word
         */
        public byte process(Target target)
        {
            Item syllable = navigator.getItem(target);
            if (syllable == null) return (byte)0;
            syllable = syllable.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (syllable == null) return (byte)0;
            int count = 0;
            Item seg = syllable.getDaughter();
            while (seg != null) {
                count++;
                seg = seg.getNext();
            }
            return (byte) rail(count);
        }
    }

    /**
     * @deprecated, use SegsFromSylStart instead
     */
    public static class PosInSyl extends SegsFromSylStart
    {
        public PosInSyl() { super(); }
        public String getName() { return "mary_pos_in_syl"; }
    }
    

    /**
     * Finds the position of the phoneme in the syllable.
     */
    public static class SegsFromSylStart implements ByteValuedFeatureProcessor
    {
        protected TargetItemNavigator navigator;
        public SegsFromSylStart() {
            this.navigator = new SegmentNavigator();
        }
        public String getName() { return "mary_segs_from_syl_start"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the position of the phoneme in the syllable
         */
        public byte process(Target target)
        {
            int count = 0;
            Item segment = navigator.getItem(target);
            if (segment == null) return (byte)0;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return (byte)0;
            for (Item p = segment.getPrevious(); p != null; p = p.getPrevious()) {
                count++;
            }
            return (byte)rail(count);
        }
    }


    /**
     * Finds the position of the phoneme from the end of the syllable.
     */
    public static class SegsFromSylEnd implements ByteValuedFeatureProcessor
    {
        protected TargetItemNavigator navigator;
        public SegsFromSylEnd() {
            this.navigator = new SegmentNavigator();
        }
        public String getName() { return "mary_segs_from_syl_end"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the position of the phoneme in the syllable
         */
        public byte process(Target target)
        {
            int count = 0;
            Item segment = navigator.getItem(target);
            if (segment == null) return (byte)0;
            segment = segment.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (segment == null) return (byte)0;
            for (Item p = segment.getNext(); p != null; p = p.getNext()) {
                count++;
            }
            return (byte)rail(count);
        }
    }

    /**
     * Finds the position of the segment from the start of the word.
     */
    public static class SegsFromWordStart implements ByteValuedFeatureProcessor
    {
        protected TargetItemNavigator navigator;
        protected TargetItemNavigator firstSegNavigator;
        public SegsFromWordStart() {
            this.navigator = new SegmentNavigator();
            this.firstSegNavigator = new FirstSegmentInWordNavigator();
        }
        public String getName() { return "mary_segs_from_word_start"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the position of the phoneme in the syllable
         */
        public byte process(Target target)
        {
            int count = 0;
            Item segment = navigator.getItem(target);
            Item firstSegment = firstSegNavigator.getItem(target);
            if (firstSegment == null) return (byte)0;
            for (Item p = segment.getItemAs(Relation.SEGMENT); p != null; p = p.getPrevious()) {
                if (p.equalsShared(firstSegment)) break;
                count++;
            }
            return (byte)rail(count);
        }
    }

    /**
     * Finds the position of the segment from the end of the word.
     */
    public static class SegsFromWordEnd implements ByteValuedFeatureProcessor
    {
        protected TargetItemNavigator navigator;
        protected TargetItemNavigator lastSegNavigator;
        public SegsFromWordEnd() {
            this.navigator = new SegmentNavigator();
            this.lastSegNavigator = new LastSegmentInWordNavigator();
        }
        public String getName() { return "mary_segs_from_word_end"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the position of the phoneme in the syllable
         */
        public byte process(Target target)
        {
            int count = 0;
            Item segment = navigator.getItem(target);
            Item lastSegment = lastSegNavigator.getItem(target);
            if (lastSegment == null) return (byte)0;
            for (Item p = segment.getItemAs(Relation.SEGMENT); p != null; p = p.getNext()) {
                if (p.equalsShared(lastSegment)) break;
                count++;
            }
            return (byte)rail(count);
        }
    }
 
    /**
     * Finds the position of the syllable from the start of the word.
     */
    public static class SylsFromWordStart implements ByteValuedFeatureProcessor
    {
        protected TargetItemNavigator navigator;
        protected TargetItemNavigator firstSylNavigator;
        public SylsFromWordStart() {
            this.navigator = new SyllableNavigator();
            this.firstSylNavigator = new FirstSyllableInWordNavigator();
        }
        public String getName() { return "mary_syls_from_word_start"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the position of the phoneme in the syllable
         */
        public byte process(Target target)
        {
            int count = 0;
            Item syllable = navigator.getItem(target);
            if (syllable == null) return (byte)0;
            Item firstSyllable = firstSylNavigator.getItem(target);
            for (Item p = syllable.getItemAs(Relation.SYLLABLE); p != null; p = p.getPrevious()) {
                if (p.equalsShared(firstSyllable)) break;
                count++;
            }
            return (byte)rail(count);
        }
    }

    /**
     * Finds the position of the syllable from the end of the word.
     */
    public static class SylsFromWordEnd implements ByteValuedFeatureProcessor
    {
        protected TargetItemNavigator navigator;
        protected TargetItemNavigator lastSylNavigator;
        public SylsFromWordEnd() {
            this.navigator = new SyllableNavigator();
            this.lastSylNavigator = new LastSyllableInWordNavigator();
        }
        public String getName() { return "mary_syls_from_word_end"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the position of the phoneme in the syllable
         */
        public byte process(Target target)
        {
            int count = 0;
            Item syllable = navigator.getItem(target);
            if (syllable == null) return (byte)0;
            Item lastSyllable = lastSylNavigator.getItem(target);
            for (Item p = syllable.getItemAs(Relation.SYLLABLE); p != null; p = p.getNext()) {
                if (p.equalsShared(lastSyllable)) break;
                count++;
            }
            return (byte)rail(count);
        }
    }
    
    
    /**
     * Determines the break level after this syllable.
     */
    public static class SylBreak implements ByteValuedFeatureProcessor
    {
        protected String name;
        protected TargetItemNavigator navigator;
        public SylBreak(String name, TargetItemNavigator syllableNavigator)
        {
            this.name = name;
            this.navigator = syllableNavigator;
        }
        public String getName() { return name; }
        /**
         * "4" for a big break, "3" for a break; "1" = word-final; "0" = within-word
         */
        public String[] getValues() {
            return new String[] {"0", "1", "unused", "3", "4"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the break level after the syllable returned by syllableNavigator
         */
        public byte process(Target target)
        {
            Item syllable = navigator.getItem(target);
            if (syllable == null) return 0;
            Item ss = syllable.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (ss == null) {
                return 0;
            } else if (ss.getNext() != null) {
                // this is not the last syllable in this word
                return 0;
            } else if (ss.getParent() == null) {
                // syllable which is not part of a word?!
                return 1;
            }
            // this is word-final, calculate the wordBreak
            Item word = ss.getParent();
            Item ww = word.getItemAs(Relation.PHRASE);
            if (ww == null || ww.getNext() != null) {
                return 1;
            }
            String pname = ww.getParent().toString();
            if (pname.equals("BB")) {
                return 4;
            } else if (pname.equals("B")) {
                return 3;
            } else {
                return 1;
            }
        }
    }
    
    /**
     * Classifies the the syllable as single, initial, mid or final.
     */
    public static class PositionType implements ByteValuedFeatureProcessor
    {
        protected TargetItemNavigator navigator;
        protected ByteStringTranslator values;
        public PositionType()
        {
            values = new ByteStringTranslator(new String[] {
                    "0", "single", "final", "initial", "mid"
            });
            navigator = new SyllableNavigator();
        }
        public String getName() { return "mary_position_type"; }
        public String[] getValues() { return values.getStringValues(); }
        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return classifies the syllable as "single", "final", "initial" or
         *         "mid"
         */
        public byte process(Target target)
        {
            Item syllable = navigator.getItem(target);
            if (syllable == null) return 0;

            String type;
            Item s = syllable.getItemAs(Relation.SYLLABLE_STRUCTURE);
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
            return values.get(type);
        }
    }

    /**
     * Checks if segment is a pause.
     */
    public static class IsPause implements ByteValuedFeatureProcessor
    {
        protected TargetItemNavigator navigator;
        protected String name;
        public IsPause(String name, TargetItemNavigator segmentNavigator)
        {
            this.name = name;
            this.navigator = segmentNavigator;
        }
        public String getName() { return name; }
        public String[] getValues() { return new String[] {"0", "1"}; }
        /**
         * Check if segment is a pause
         * @param target the target to process
         * @return 0 if false, 1 if true
         */
        public byte process(Target target)
        {
            Item seg = navigator.getItem(target);
            if (seg == null) return 0;
            Item segItem = seg.getItemAs(Relation.SEGMENT);
            // TODO: "pau" or "_" is hard-coded here as the pause symbol
            if (segItem == null
                || !(segItem.toString().equals("pau") || segItem.toString().equals("_"))) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    /**
     * The ToBI accent of the current syllable.
     */
    public static class TobiAccent implements ByteValuedFeatureProcessor
    {
        protected String name;
        protected TargetItemNavigator navigator;
        protected ByteStringTranslator values;
        
        public TobiAccent(String name, TargetItemNavigator syllableNavigator)
        {
            this.name = name;
            this.navigator = syllableNavigator;
            this.values = new ByteStringTranslator(new String[] {
                    "0", "*", "H*", "!H*", "^H*", "L*", "L+H*", "L*+H", "L+!H*",
                    "L*+!H", "L+^H*", "L*+^H", "H+L*", "H+!H*", "H+^H*",
                    "!H+!H*", "^H+!H*", "^H+^H*", "H*+L", "!H*+L"
            });
        }
        
        public String getName() { return name; }
        public String[] getValues() { return values.getStringValues(); }
        
        /**
         * For the given syllable item, return its tobi accent, 
         * or 0 if there is none.
         */
        public byte process(Target target)
        {
            Item syllable = navigator.getItem(target);
            if (syllable == null) return 0;
            String accent = syllable.getFeatures().getString("accent");
            if (accent == null) {
                return 0;
            }
            return values.get(accent);
        }
    }

    /**
     * The ToBI endtone associated with the current syllable.
     */
    public static class TobiEndtone implements ByteValuedFeatureProcessor
    {
        protected String name;
        protected TargetItemNavigator navigator;
        protected ByteStringTranslator values;
        
        public TobiEndtone(String name, TargetItemNavigator syllableNavigator)
        {
            this.name = name;
            this.navigator = syllableNavigator;
            this.values = new ByteStringTranslator(new String[] {
                    "0", "H-", "!H-", "L-", "H-%", "!H-%", "H-^H%",
                    "!H-^H%", "L-H%", "L-%", "L-L%", "H-H%", "H-L%"
            });
        }
        
        public String getName() { return name; }
        public String[] getValues() { return values.getStringValues(); }

        /**
         * For the given syllable item, return its tobi end tone, 
         * or 0 if there is none.
         */
        public byte process(Target target)
        {
            Item syllable = navigator.getItem(target);
            if (syllable == null) return 0;
            String endtone = syllable.getFeatures().getString("endtone");
            if (endtone == null || endtone.equals("")) {
                return 0;
            }
            return values.get(endtone);
        }
    }

    /**
     * The next ToBI accent following the current syllable in the current phrase.
     */
    public static class NextAccent extends TobiAccent
    {
        protected TargetItemNavigator lastSyllableNavigator;
        public NextAccent()
        {
            super("mary_next_accent", new SyllableNavigator());
            this.lastSyllableNavigator = new LastSyllableNavigator();
        }
        /**
         * Search for an accented syllable, and return its tobi accent, 
         * or 0 if there is none.
         */
        public byte process(Target target)
        {
            Item syllable = navigator.getItem(target);
            if (syllable == null) return 0;
            syllable = syllable.getItemAs(Relation.SYLLABLE);
            if (syllable == null) return 0;
            Item lastSyllable = lastSyllableNavigator.getItem(target);
            if (syllable.equalsShared(lastSyllable)) return 0;
            for (Item n = syllable.getNext(); n != null; n = n.getNext()) {
                String accent = n.getFeatures().getString("accent");
                if (accent != null) {
                    return values.get(accent);
                }
                if (n.equalsShared(lastSyllable)) break;
            }
            return 0;
        }
    }

    /**
     * The previous ToBI accent preceding the current syllable in the current phrase.
     */
    public static class PrevAccent extends TobiAccent
    {
        protected TargetItemNavigator firstSyllableNavigator;
        public PrevAccent()
        {
            super("mary_prev_accent", new SyllableNavigator());
            this.firstSyllableNavigator = new FirstSyllableNavigator();
        }
        /**
         * Search for an accented syllable, and return its tobi accent, 
         * or 0 if there is none.
         */
        public byte process(Target target)
        {
            Item syllable = navigator.getItem(target);
            if (syllable == null) return 0;
            syllable = syllable.getItemAs(Relation.SYLLABLE);
            if (syllable == null) return 0;
            Item firstSyllable = firstSyllableNavigator.getItem(target);
            if (syllable.equalsShared(firstSyllable)) return 0;
            for (Item n = syllable.getPrevious(); n != null; n = n.getPrevious()) {
                String accent = n.getFeatures().getString("accent");
                if (accent != null) {
                    return values.get(accent);
                }
                if (n.equalsShared(firstSyllable)) break;
            }
            return 0;
        }
    }

    
    /**
     * The ToBI endtone associated with the last syllable of the current phrase.
     */
    public static class PhraseEndtone extends TobiEndtone
    {        
        public PhraseEndtone()
        {
            super("mary_phrase_endtone", new LastSyllableNavigator());
        }
    }

    /**
     * The ToBI endtone associated with the last syllable of the previous phrase.
     */
    public static class PrevPhraseEndtone extends TobiEndtone
    {        
        public PrevPhraseEndtone()
        {
            super("mary_prev_phrase_endtone", new FirstSyllableNavigator());
        }

        /**
         * For the given syllable item, return its tobi end tone, 
         * or 0 if there is none.
         */
        public byte process(Target target)
        {
            Item syllable = navigator.getItem(target);
            if (syllable == null) return 0;
            syllable = syllable.getItemAs(Relation.SYLLABLE);
            if (syllable == null) return 0;
            // Now, the syllable before the first one in the current phrase
            // is the last syllable in the previous phrase
            syllable = syllable.getPrevious();
            if (syllable == null) return 0;
            String endtone = syllable.getFeatures().getString("endtone");
            if (endtone == null || endtone.equals("")) {
                return 0;
            }
            return values.get(endtone);
        }
    }


    /**
     * Counts the number of syllables since the start of the phrase. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class SylsFromPhraseStart implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator firstSyllableNavigator;
        public SylsFromPhraseStart() {
            this.navigator = new SyllableNavigator();
            this.firstSyllableNavigator = new FirstSyllableNavigator();
        }

        public String getName() { return "mary_syls_from_phrase_start"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of accented syllables since the last major break
         */
        public byte process(Target target)
        {
            int count = 0;
            Item ss = navigator.getItem(target);
            if (ss == null) return (byte)0;
            ss = ss.getItemAs(Relation.SYLLABLE);
            if (ss == null) return (byte)0;
            Item firstSyllable = firstSyllableNavigator.getItem(target);

            for (Item p = ss; p != null; p = p.getPrevious()) {
                if (p.equalsShared(firstSyllable)) {
                    break;
                }
                count++;
            }
            return (byte)rail(count);
        }
    }
    
    /**
     * Counts the number of syllables until the end of the phrase. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class SylsFromPhraseEnd implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator lastSyllableNavigator;
        public SylsFromPhraseEnd() {
            this.navigator = new SyllableNavigator();
            this.lastSyllableNavigator = new LastSyllableNavigator();
        }

        public String getName() { return "mary_syls_from_phrase_end"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of accented syllables since the last major break
         */
        public byte process(Target target)
        {
            int count = 0;
            Item ss = navigator.getItem(target);
            if (ss == null) return (byte)0;
            ss = ss.getItemAs(Relation.SYLLABLE);
            if (ss == null) return (byte)0;
            Item lastSyllable = lastSyllableNavigator.getItem(target);

            for (Item p = ss; p != null; p = p.getNext()) {
                if (p.equalsShared(lastSyllable)) {
                    break;
                }
                count++;
            }
            return (byte)rail(count);
        }
    }
    
    /**
     * Counts the number of stressed syllables since the start of the phrase. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class StressedSylsFromPhraseStart implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator firstSyllableNavigator;
        public StressedSylsFromPhraseStart() {
            this.navigator = new SyllableNavigator();
            this.firstSyllableNavigator = new FirstSyllableNavigator();
        }

        public String getName() { return "mary_stressed_syls_from_phrase_start"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of accented syllables since the last major break
         */
        public byte process(Target target)
        {
            int count = 0;
            Item ss = navigator.getItem(target);
            if (ss == null) return (byte)0;
            ss = ss.getItemAs(Relation.SYLLABLE);
            if (ss == null) return (byte)0;
            Item firstSyllable = firstSyllableNavigator.getItem(target);
            if (ss.equalsShared(firstSyllable)) return (byte)0;
            
            for (Item p = ss.getPrevious(); p != null; p = p.getPrevious()) {
                if ("1".equals(p.getFeatures().getString("stress"))) {
                    count++;
                }
                if (p.equalsShared(firstSyllable)) {
                    break;
                }
            }
            return (byte)rail(count);
        }
    }

    /**
     * Counts the number of stressed syllables until the end of the phrase. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class StressedSylsFromPhraseEnd implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator lastSyllableNavigator;
        public StressedSylsFromPhraseEnd() {
            this.navigator = new SyllableNavigator();
            this.lastSyllableNavigator = new LastSyllableNavigator();
        }

        public String getName() { return "mary_stressed_syls_from_phrase_end"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of accented syllables since the last major break
         */
        public byte process(Target target)
        {
            int count = 0;
            Item ss = navigator.getItem(target);
            if (ss == null) return (byte)0;
            ss = ss.getItemAs(Relation.SYLLABLE);
            if (ss == null) return (byte)0;
            Item lastSyllable = lastSyllableNavigator.getItem(target);
            if (ss.equalsShared(lastSyllable)) return (byte)0;
            for (Item p = ss.getNext(); p != null; p = p.getNext()) {
                if ("1".equals(p.getFeatures().getString("stress"))) {
                    count++;
                }
                if (p.equalsShared(lastSyllable)) {
                    break;
                }
            }
            return (byte)rail(count);
        }
    }
    
    /**
     * Counts the number of accented syllables since the start of the phrase. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class AccentedSylsFromPhraseStart implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator firstSyllableNavigator;
        public AccentedSylsFromPhraseStart() {
            this.navigator = new SyllableNavigator();
            this.firstSyllableNavigator = new FirstSyllableNavigator();
        }

        public String getName() { return "mary_accented_syls_from_phrase_start"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of accented syllables since the last major break
         */
        public byte process(Target target)
        {
            int count = 0;
            Item ss = navigator.getItem(target);
            if (ss == null) return (byte)0;
            ss = ss.getItemAs(Relation.SYLLABLE);
            if (ss == null) return (byte)0;
            Item firstSyllable = firstSyllableNavigator.getItem(target);
            if (ss.equalsShared(firstSyllable)) return (byte)0;
            for (Item p = ss.getPrevious(); p != null; p = p.getPrevious()) {
                if (p.getFeatures().isPresent("accent")) {
                    count++;
                }
                if (p.equalsShared(firstSyllable)) {
                    break;
                }
            }
            return (byte)rail(count);
        }
    }

    
    /**
     * Counts the number of stressed syllables until the end of the phrase. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class AccentedSylsFromPhraseEnd implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator lastSyllableNavigator;
        public AccentedSylsFromPhraseEnd() {
            this.navigator = new SyllableNavigator();
            this.lastSyllableNavigator = new LastSyllableNavigator();
        }

        public String getName() { return "mary_accented_syls_from_phrase_end"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of accented syllables since the last major break
         */
        public byte process(Target target)
        {
            int count = 0;
            Item ss = navigator.getItem(target);
            if (ss == null) return (byte)0;
            ss = ss.getItemAs(Relation.SYLLABLE);
            if (ss == null) return (byte)0;
            Item lastSyllable = lastSyllableNavigator.getItem(target);
            if (ss.equalsShared(lastSyllable)) return (byte)0;
            for (Item p = ss.getNext(); p != null; p = p.getNext()) {
                if (p.getFeatures().isPresent("accent")) {
                    count++;
                }
                if (p.equalsShared(lastSyllable)) {
                    break;
                }
            }
            return (byte)rail(count);
        }
    }
  
    
    /**
     * Counts the number of words since the start of the phrase. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class WordsFromPhraseStart implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator firstWordNavigator;
        public WordsFromPhraseStart() {
            this.navigator = new WordNavigator();
            this.firstWordNavigator = new FirstWordInPhraseNavigator();
        }

        public String getName() { return "mary_words_from_phrase_start"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of accented syllables since the last major break
         */
        public byte process(Target target)
        {
            int count = 0;
            Item w = navigator.getItem(target);
            if (w == null) return (byte)0;
            w = w.getItemAs(Relation.WORD);
            if (w == null) return (byte)0;
            Item first = firstWordNavigator.getItem(target);

            for (Item p = w; p != null; p = p.getPrevious()) {
                if (p.equalsShared(first)) {
                    break;
                }
                count++;
            }
            return (byte)rail(count);
        }
    }
    
    /**
     * Counts the number of words until the end of the phrase. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class WordsFromPhraseEnd implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator lastWordNavigator;
        public WordsFromPhraseEnd() {
            this.navigator = new WordNavigator();
            this.lastWordNavigator = new LastWordInPhraseNavigator();
        }

        public String getName() { return "mary_words_from_phrase_end"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of accented syllables since the last major break
         */
        public byte process(Target target)
        {
            int count = 0;
            Item w = navigator.getItem(target);
            if (w == null) return (byte)0;
            w = w.getItemAs(Relation.WORD);
            if (w == null) return (byte)0;
            Item last = lastWordNavigator.getItem(target);

            for (Item p = w; p != null; p = p.getNext()) {
                if (p.equalsShared(last)) {
                    break;
                }
                count++;
            }
            return (byte)rail(count);
        }
    }

    /**
     * Counts the number of words since the start of the sentence. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class WordsFromSentenceStart implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator firstPhraseNavigator;
        public WordsFromSentenceStart() {
            this.navigator = new WordNavigator();
            this.firstPhraseNavigator = new FirstPhraseNavigator();
        }

        public String getName() { return "mary_words_from_sentence_start"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of accented syllables since the last major break
         */
        public byte process(Target target)
        {
            int count = 0;
            Item w = navigator.getItem(target);
            if (w == null) return (byte)0;
            w = w.getItemAs(Relation.WORD);
            if (w == null) return (byte)0;
            Item firstPhrase = firstPhraseNavigator.getItem(target);
            if (firstPhrase == null) return (byte)0;
            firstPhrase = firstPhrase.getItemAs(Relation.PHRASE);
            if (firstPhrase == null) return (byte)0;
            Item firstWord = firstPhrase.getDaughter();
            for (Item p = w; p != null; p = p.getPrevious()) {
                if (p.equalsShared(firstWord)) {
                    break;
                }
                count++;
            }
            return (byte)rail(count);
        }
    }
    
    /**
     * Counts the number of words until the end of the sentence. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class WordsFromSentenceEnd implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator lastPhraseNavigator;
        public WordsFromSentenceEnd() {
            this.navigator = new WordNavigator();
            this.lastPhraseNavigator = new LastPhraseNavigator();
        }

        public String getName() { return "mary_words_from_sentence_end"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of accented syllables since the last major break
         */
        public byte process(Target target)
        {
            int count = 0;
            Item w = navigator.getItem(target);
            if (w == null) return (byte)0;
            w = w.getItemAs(Relation.WORD);
            if (w == null) return (byte)0;
            Item lastPhrase = lastPhraseNavigator.getItem(target);
            lastPhrase = lastPhrase.getItemAs(Relation.PHRASE);
            if (lastPhrase == null) return (byte)0;
            Item lastWord = lastPhrase.getLastDaughter();
            for (Item p = w; p != null; p = p.getNext()) {
                if (p.equalsShared(lastWord)) {
                    break;
                }
                count++;
            }
            return (byte)rail(count);
        }
    }

    /**
     * Counts the number of phrases since the start of the sentence. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class PhrasesFromSentenceStart implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator firstPhraseNavigator;
        public PhrasesFromSentenceStart() {
            this.navigator = new PhraseNavigator();
            this.firstPhraseNavigator = new FirstPhraseNavigator();
        }

        public String getName() { return "mary_phrases_from_sentence_start"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of accented syllables since the last major break
         */
        public byte process(Target target)
        {
            int count = 0;
            Item phrase = navigator.getItem(target);
            if (phrase == null) return (byte)0;
            phrase = phrase.getItemAs(Relation.PHRASE);
            if (phrase == null) return (byte)0;
            Item firstPhrase = firstPhraseNavigator.getItem(target);
            if (firstPhrase == null) return (byte)0;
            firstPhrase = firstPhrase.getItemAs(Relation.PHRASE);
            if (firstPhrase == null) return (byte)0;
            for (Item p = phrase; p != null; p = p.getPrevious()) {
                if (p.equalsShared(firstPhrase)) {
                    break;
                }
                count++;
            }
            return (byte)rail(count);
        }
    }
    
    /**
     * Counts the number of phrases until the end of the sentence. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class PhrasesFromSentenceEnd implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator lastPhraseNavigator;
        public PhrasesFromSentenceEnd() {
            this.navigator = new PhraseNavigator();
            this.lastPhraseNavigator = new LastPhraseNavigator();
        }

        public String getName() { return "mary_phrases_from_sentence_end"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of accented syllables since the last major break
         */
        public byte process(Target target)
        {
            int count = 0;
            Item phrase = navigator.getItem(target);
            if (phrase == null) return (byte)0;
            phrase = phrase.getItemAs(Relation.PHRASE);
            if (phrase == null) return (byte)0;
            Item lastPhrase = lastPhraseNavigator.getItem(target);
            lastPhrase = lastPhrase.getItemAs(Relation.PHRASE);
            if (lastPhrase == null) return (byte)0;
            for (Item p = phrase; p != null; p = p.getNext()) {
                if (p.equalsShared(lastPhrase)) {
                    break;
                }
                count++;
            }
            return (byte)rail(count);
        }
    }

    
    /**
     * Counts the number of syllables since the last accent in the current phrase. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class SylsFromPrevAccent implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator firstSyllableNavigator;
        public SylsFromPrevAccent() {
            this.navigator = new SyllableNavigator();
            this.firstSyllableNavigator = new FirstSyllableNavigator();
        }

        public String getName() { return "mary_syls_from_prev_accent"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of syllables since the last accent
         */
        public byte process(Target target)
        {
            int count = 0;
            Item ss = navigator.getItem(target);
            if (ss == null) return (byte)0;
            ss = ss.getItemAs(Relation.SYLLABLE);
            if (ss == null) return (byte)0;
            Item first = firstSyllableNavigator.getItem(target);
            if (first == null) return (byte)0;
            if (ss.equalsShared(first)) return (byte)0;
            for (Item p = ss.getPrevious(); p != null; p = p.getPrevious()) {
                count++;
                if (p.getFeatures().isPresent("accent")) {
                    break;
                }
                if (p.equalsShared(first)) return (byte)0;
            }
            return (byte)rail(count);
        }
    }
  
    /**
     * Counts the number of syllables until the next accent in the current phrase. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class SylsToNextAccent implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator lastSyllableNavigator;
        public SylsToNextAccent() {
            this.navigator = new SyllableNavigator();
            this.lastSyllableNavigator = new LastSyllableNavigator();
        }
        public String getName() { return "mary_syls_to_next_accent"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of syllables until the next accent
         */
        public byte process(Target target)
        {
            int count = 0;
            Item ss = navigator.getItem(target);
            if (ss == null) return (byte)0;
            ss = ss.getItemAs(Relation.SYLLABLE);
            if (ss == null) return (byte)0;
            Item last = lastSyllableNavigator.getItem(target);
            if (last == null) return (byte)0;
            if (ss.equalsShared(last)) return (byte)0;
            for (Item p = ss.getNext(); p != null; p = p.getNext()) {
                count++;
                if (p.getFeatures().isPresent("accent")) {
                    break;
                }
                if (p.equalsShared(last)) return (byte)0;
            }
            return (byte)rail(count);
        }
    }

    /**
     * Counts the number of syllables since the last stressed syllable in the current phrase. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class SylsFromPrevStressed implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator firstSyllableNavigator;
        public SylsFromPrevStressed() {
            this.navigator = new SyllableNavigator();
            this.firstSyllableNavigator = new FirstSyllableNavigator();
        }

        public String getName() { return "mary_syls_from_prev_stressed"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of syllables since the last accent
         */
        public byte process(Target target)
        {
            int count = 0;
            Item ss = navigator.getItem(target);
            if (ss == null) return (byte)0;
            ss = ss.getItemAs(Relation.SYLLABLE);
            if (ss == null) return (byte)0;
            Item first = firstSyllableNavigator.getItem(target);
            if (first == null) return (byte)0;
            if (ss.equalsShared(first)) return (byte)0;
            for (Item p = ss.getPrevious(); p != null; p = p.getPrevious()) {
                count++;
                if ("1".equals(p.getFeatures().getString("stress"))) {
                    break;
                }
                if (p.equalsShared(first)) return (byte)0;
            }
            return (byte)rail(count);
        }
    }
  
    /**
     * Counts the number of syllables until the next stressedSyllable in the current phrase. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class SylsToNextStressed implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator lastSyllableNavigator;
        public SylsToNextStressed() {
            this.navigator = new SyllableNavigator();
            this.lastSyllableNavigator = new LastSyllableNavigator();
        }

        public String getName() { return "mary_syls_to_next_stressed"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        /**
         * Performs some processing on the given item.
         * @param target the target to process
         * @return the number of syllables until the next accent
         */
        public byte process(Target target)
        {
            int count = 0;
            Item ss = navigator.getItem(target);
            if (ss == null) return (byte)0;
            ss = ss.getItemAs(Relation.SYLLABLE);
            if (ss == null) return (byte)0;
            Item last = lastSyllableNavigator.getItem(target);
            if (last == null) return (byte)0;
            if (ss.equalsShared(last)) return (byte)0;
            for (Item p = ss.getNext(); p != null; p = p.getNext()) {
                count++;
                if ("1".equals(p.getFeatures().getString("stress"))) {
                    break;
                }
                if (p.equalsShared(last)) return (byte)0;
            }
            return (byte)rail(count);
        }
    }
  
  
    
    /**
     * Determines the word punctuation. This is a feature processor. A feature
     * processor takes an item, performs some sort of processing on the item and
     * returns an object.
     */
    public static class WordPunc implements ByteValuedFeatureProcessor
    {
        protected String name;
        protected TargetItemNavigator navigator;
        protected ByteStringTranslator values;
        
        /**
         * @param name name of this feature processor
         * @param wordNavigator a navigator which returns a word for a target.
         * This navigator decides the word for which the punctuation will be computed.
         */
        public WordPunc(String name, TargetItemNavigator wordNavigator)
        {
            this.name = name;
            this.navigator = wordNavigator;
            this.values = new ByteStringTranslator(new String[] {
                    "0", ".", ",", ";", ":", "(", ")", "?", "!", "\""
            });
        }

        public String getName() { return name; }
        public String[] getValues() { return values.getStringValues(); }

        public byte process(Target target)
        {
            Item word = navigator.getItem(target);
            if (word == null) return values.get("0");
            Item tokenWord = word.getItemAs(Relation.TOKEN);
            if (tokenWord == null) return values.get("0");
            Item token = tokenWord.getParent();
            if (token == null) return values.get("0");
            String punc = token.getFeatures().getString("punc");
            if (values.contains(punc)) return values.get(punc);
            // unknown punctuation: return "0"
            return values.get("0");
        }
    }

    /**
     * Determines the next word punctuation in the sentence. This is a feature processor. A feature
     * processor takes an item, performs some sort of processing on the item and
     * returns an object.
     */
    public static class NextPunctuation extends WordPunc
    {
        public NextPunctuation()
        {
            super("mary_next_punctuation", new WordNavigator());
        }
        public byte process(Target target)
        {
            Item word = navigator.getItem(target);
            if (word == null) return (byte)0;
            word = word.getItemAs(Relation.WORD);
            for (; word != null; word = word.getNext()) {
                Item tokenWord = word.getItemAs(Relation.TOKEN);
                if (tokenWord == null) return values.get("0");
                Item token = tokenWord.getParent();
                if (token == null) return values.get("0");
                String punc = token.getFeatures().getString("punc");
                if (punc != null && !punc.equals("")) {
                    if (values.contains(punc)) return values.get(punc);
                    // unknown punctuation: return "0"
                    return values.get("0");
                }
            }
            // no next punctuation: return "0"
            return values.get("0");
        }
    }

    /**
     * Determines the previous word punctuation in the sentence. This is a feature processor. A feature
     * processor takes an item, performs some sort of processing on the item and
     * returns an object.
     */
    public static class PrevPunctuation extends WordPunc
    {
        public PrevPunctuation()
        {
            super("mary_prev_punctuation", new WordNavigator());
        }
        public byte process(Target target)
        {
            Item word = navigator.getItem(target);
            if (word == null) return (byte)0;
            word = word.getItemAs(Relation.WORD);
            for (word = word.getPrevious(); word != null; word = word.getPrevious()) {
                Item tokenWord = word.getItemAs(Relation.TOKEN);
                if (tokenWord == null) return values.get("0");
                Item token = tokenWord.getParent();
                if (token == null) return values.get("0");
                String punc = token.getFeatures().getString("punc");
                if (punc != null && !punc.equals("")) {
                    if (values.contains(punc)) return values.get(punc);
                    // unknown punctuation: return "0"
                    return values.get("0");
                }
            }
            // no next punctuation: return "0"
            return values.get("0");
        }
    }
    
    /**
     * Determines the distance in words to the next word punctuation in the sentence. This is a feature processor. A feature
     * processor takes an item, performs some sort of processing on the item and
     * returns an object.
     */
    public static class WordsToNextPunctuation implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        public WordsToNextPunctuation()
        {
            this.navigator = new WordNavigator();
        }
        public String getName() { return "mary_words_to_next_punctuation"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        public byte process(Target target)
        {
            int count = 0;
            Item word = navigator.getItem(target);
            if (word == null) return (byte)0;
            word = word.getItemAs(Relation.WORD);
            for (; word != null; word = word.getNext()) {
                Item tokenWord = word.getItemAs(Relation.TOKEN);
                if (tokenWord == null) return (byte)0;
                Item token = tokenWord.getParent();
                if (token == null) return (byte)0;
                String punc = token.getFeatures().getString("punc");
                if (punc != null && !punc.equals("")) {
                    break;
                }
                count++;
            }
            return (byte)rail(count);
        }
    }

    /**
     * Determines the distance in words from the previous word punctuation in the sentence. This is a feature processor. A feature
     * processor takes an item, performs some sort of processing on the item and
     * returns an object.
     */
    public static class WordsFromPrevPunctuation implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        public WordsFromPrevPunctuation()
        {
            this.navigator = new WordNavigator();
        }
        public String getName() { return "mary_words_from_prev_punctuation"; }
        public String[] getValues() {
            return new String[] {"0", "1", "2", "3", "4", "5", "6", "7",
                    "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19"};
        }

        public byte process(Target target)
        {
            int count = 0;
            Item word = navigator.getItem(target);
            if (word == null) return (byte)0;
            word = word.getItemAs(Relation.WORD);
            for (word = word.getPrevious(); word != null; word = word.getPrevious()) {
                count++;
                Item tokenWord = word.getItemAs(Relation.TOKEN);
                if (tokenWord == null) return (byte)0;
                Item token = tokenWord.getParent();
                if (token == null) return (byte)0;
                String punc = token.getFeatures().getString("punc");
                if (punc != null && !punc.equals("")) {
                    break;
                }
            }
            return (byte)rail(count);
        }
    }

    

    /**
     * Determine the prosodic property of a target
     * 
     * @author Anna Hunecke
     *
     */
    public static class Selection_Prosody implements ByteValuedFeatureProcessor {
        
        protected TargetItemNavigator navigator;
        private List lowEndtones;
        private List highEndtones;
        private AccentedSylsFromPhraseEnd as;
        
        public Selection_Prosody ( TargetItemNavigator syllableNavigator){
            this.navigator = syllableNavigator;
            lowEndtones = new ArrayList();
            lowEndtones.add("L-");
            lowEndtones.add("L-%");
            lowEndtones.add("H-L%");
            lowEndtones.add("L-L%");
            highEndtones = new ArrayList();
            highEndtones.add("H-");
            highEndtones.add("!H-");
            highEndtones.add("H-%");
            highEndtones.add("!H-%");
            highEndtones.add("H-^H");
            highEndtones.add("!H-^H%");
            highEndtones.add("L-H%");
            highEndtones.add("H-H%");
            as = new AccentedSylsFromPhraseEnd();
        }
        public String getName() { return "mary_selection_prosody"; }
        
        public String[] getValues() {
            return new String[] {"0", "stressed", "pre-nuclear", "nuclear", 
                    "finalHigh", "finalLow"};
        }
        
        /**
         * Determine the prosodic property of the target
         * 
         *@param target the target
         *@return 0 - unstressed, 1 - stressed, 2 - pre-nuclear accent
         *		3 - nuclear accent, 4 - phrase final high, 5 - phrase final low
         */
        public byte process(Target target){
            //TODO: find out why java thinks there is an error here
            //first find out if syllable is stressed
            Item syllable = navigator.getItem(target);
            if (syllable == null) return (byte)0;
            String value = syllable.getFeatures().getString("stress");
            if (value == null) return (byte)0;
            boolean stressed = false;
            if (Byte.parseByte(value) != 0)
                stressed = true;
            //find out the position of the target
            byte accSylsFromPhraseEnd = as.process(target);
            if (stressed){
                //find out if we have an accent    
                String accent = syllable.getFeatures().getString("accent");
                if (accent != null) {
                    if (accSylsFromPhraseEnd == 0){
                        return (byte)3;//return nuclear accent
                    }
                    //else just a normal accent
                    return (byte)2;//return pre-nuclear accent
                }
            }  
            //ToBI endtones:"0"->0||1, "H-"->4, "!H-"->4, "L-"->5,
            //				"H-%"->4, "!H-%"->4, "H-^H%"->4,
            //              "!H-^H%"->4, "L-H%"->4, "L-%"->5, 
            //				"L-L%"->5, "H-H%"->4, "H-L%"->5
            if (accSylsFromPhraseEnd == 0){
                String endtone = syllable.getFeatures().getString("endtone");
                if (endtone == null || endtone.equals("")
                        || endtone.equals("0")){ 
                    if (stressed)
                        return (byte)1; //return stressed
                    return (byte)0;//return unstressed
                }
                if (highEndtones.contains(endtone)){
                    return (byte)4; //return phrase final high
                }
                if (lowEndtones.contains(endtone)){
                    return (byte)5; //return phrase final low
                }
            } else {
                if (stressed) return (byte)1; //return stressed
                return (byte)0;//return unstressed
            }
            return (byte)0;//return unstressed
        }
    }
    
    
    

    /**
     * Returns the duration of the given segment This is a feature processor. A
     * feature processor takes an item, performs some sort of processing on the
     * item and returns an object.
     */
    public static class UnitDuration implements ContinuousFeatureProcessor
    {
        public String getName() { return "mary_unit_duration"; }
        public String[] getValues() { return null; }

        public float process(Target target)
        {
            if (target instanceof DiphoneTarget) {
                DiphoneTarget diphone = (DiphoneTarget) target;
                return process(diphone.getLeft()) + process(diphone.getRight());
            }
            Item seg = target.getItem();
            if (seg == null) {
                return 0;
            } 
            if (!seg.getFeatures().isPresent("end")) {
                //System.out.println("Item "+seg+" does not have an 'end' feature");
                return 0;
                //throw new IllegalStateException("Item '"+seg+"' does not have an 'end' feature");
            }
            Item prev = seg.getPrevious();
            if (prev == null) {
                return seg.getFeatures().getFloat("end");
            }
            if (!prev.getFeatures().isPresent("end")) {
                //System.out.println("prev Item "+prev+" does not have an 'end' feature");
                return 0;
                //throw new IllegalStateException("Item "+prev+" does not have an 'end' feature");
            }
            float phoneDuration = seg.getFeatures().getFloat("end")
                - seg.getPrevious().getFeatures().getFloat("end");
            if (target instanceof HalfPhoneTarget)
                return phoneDuration / 2;
            return phoneDuration;
        }
    }



    /**
     * Calculates the pitch of a segment This processor should be used by target
     * items only
     */
    public static class UnitLogF0 implements ContinuousFeatureProcessor
    {
        public String getName() { return "mary_unit_logf0"; }
        public String[] getValues() { return null; }

        public float process(Target target)
        {
            if (target instanceof DiphoneTarget) {
                DiphoneTarget diphone = (DiphoneTarget) target;
                return (process(diphone.getLeft()) + process(diphone.getRight())) / 2;
            }

            Item seg = target.getItem();
            // System.out.println("Looking for pitch...");
            // get mid position of segment
            float mid;
            if (!seg.getFeatures().isPresent("end")) {
                //System.out.println("Item "+seg+" does not have an 'end' feature");
                return 0;
                //throw new IllegalStateException("Item '"+seg+"' does not have an 'end' feature");
            }           
            float end = seg.getFeatures().getFloat("end");
            Item prev = seg.getPrevious();
            float prev_end;
            if (prev == null){
                prev_end = 0;
            } else {
                if (!prev.getFeatures().isPresent("end")){
                    return 0;
                }
                prev_end = prev.getFeatures().getFloat("end");
            }
            mid = prev_end + (end - prev_end) / 2;
            if (target instanceof HalfPhoneTarget) {
                float mymid;
                if (((HalfPhoneTarget)target).isLeftHalf()) {
                    mymid = prev_end + (mid - prev_end) / 2;
                } else {
                    mymid = mid + (end - mid) / 2;
                }
                mid = mymid;
            }
            // Now mid is the middle of the unit
            Relation targetRelation = seg.getUtterance().getRelation("Target");
            // if segment has no target relation, you can not calculate
            // the segment pitch
            if (targetRelation == null) {
                return 0;
            }
            // get F0 and position of previous and next target
            Item nextTargetItem = targetRelation.getHead();
            while (nextTargetItem != null
                    && nextTargetItem.getFeatures().getFloat("pos") < mid) {
                nextTargetItem = nextTargetItem.getNext();
            }
            if (nextTargetItem == null)
                return 0;
            Item lastTargetItem = nextTargetItem.getPrevious();
            if (lastTargetItem == null)
                return 0;
            float lastF0 = lastTargetItem.getFeatures().getFloat("f0");
            float lastPos = lastTargetItem.getFeatures().getFloat("pos");
            float nextF0 = nextTargetItem.getFeatures().getFloat("f0");
            float nextPos = nextTargetItem.getFeatures().getFloat("pos");
            assert lastPos <= mid && mid <= nextPos;
            // build a linear function (f(x) = slope*x+intersectionYAxis)
            float slope = (nextF0 - lastF0) / (nextPos - lastPos);
            // calculate the pitch
            float f0 = lastF0 + slope * (mid - lastPos);
            if (!(lastF0 <= f0 && f0 <= nextF0 || nextF0 <= f0
                    && f0 <= lastF0)) {
                //TODO: Find out whats happening here
                //throw new NullPointerException();
                return 0;
            }

            if (Float.isNaN(f0)) {
                f0 = (float) 0.0;
            }
            if (f0 == 0) return 0;
            return (float) Math.log(f0);
        }
    }
    
    
    
    
}
