package de.dfki.lt.mary.unitselection.featureprocessors;


import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.ProcessException;
import com.sun.speech.freetts.Relation;

import de.dfki.lt.mary.unitselection.Target;
import de.dfki.lt.mary.util.ByteStringTranslator;

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

    public static class LastWordNavigator implements TargetItemNavigator
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
            return Byte.parseByte(value);
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
     * Counts the number of accented syllables since the last major break. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class AccentedSylIn implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator firstSyllableNavigator;
        public AccentedSylIn() {
            this.navigator = new SyllableNavigator();
            this.firstSyllableNavigator = new FirstSyllableNavigator();
        }

        public String getName() { return "mary_asyl_in"; }
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
     * Finds the position of the phoneme in the syllable.
     */
    public static class PosInSyl implements ByteValuedFeatureProcessor
    {
        protected TargetItemNavigator navigator;
        public PosInSyl() {
            this.navigator = new SegmentNavigator();
        }
        public String getName() { return "mary_pos_in_syl"; }
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
            byte count = 0;
            Item segment = navigator.getItem(target);
            for (Item p = segment.getItemAs(Relation.SYLLABLE_STRUCTURE); p != null; p = p.getPrevious()) {
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
            if (endtone == null) {
                return 0;
            }
            return values.get(endtone);
        }
    }

    
    /**
     * Counts the number of stressed syllables since the last major break. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class StressedSylIn implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator firstSyllableNavigator;
        public StressedSylIn() {
            this.navigator = new SyllableNavigator();
            this.firstSyllableNavigator = new FirstSyllableNavigator();
        }

        public String getName() { return "mary_ssyl_in"; }
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
     * Counts the number of syllables since the last major break. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class SylIn implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator firstSyllableNavigator;
        public SylIn() {
            this.navigator = new SyllableNavigator();
            this.firstSyllableNavigator = new FirstSyllableNavigator();
        }

        public String getName() { return "mary_syl_in"; }
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
                count++;
                if (p.equalsShared(firstSyllable)) {
                    break;
                }
            }
            return (byte)rail(count);
        }
    }
    
    /**
     * Counts the number of syllables until the next major break. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class SylOut implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator lastSyllableNavigator;
        public SylOut() {
            this.navigator = new SyllableNavigator();
            this.lastSyllableNavigator = new LastSyllableNavigator();
        }

        public String getName() { return "mary_syl_out"; }
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
                count++;
                if (p.equalsShared(lastSyllable)) {
                    break;
                }
            }
            return (byte)rail(count);
        }
    }
    
    /**
     * Counts the number of stressed syllables until the next major break. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class StressedSylOut implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator lastSyllableNavigator;
        public StressedSylOut() {
            this.navigator = new SyllableNavigator();
            this.lastSyllableNavigator = new LastSyllableNavigator();
        }

        public String getName() { return "mary_ssyl_out"; }
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
     * Counts the number of stressed syllables until the next major break. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class AccentedSylOut implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        TargetItemNavigator lastSyllableNavigator;
        public AccentedSylOut() {
            this.navigator = new SyllableNavigator();
            this.lastSyllableNavigator = new LastSyllableNavigator();
        }

        public String getName() { return "mary_asyl_out"; }
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
     * Counts the number of stressed syllables until the next major break. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class LastAccent implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        public LastAccent() {
            this.navigator = new SyllableNavigator();
        }

        public String getName() { return "mary_last_accent"; }
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

            for (Item p = ss; p != null; p = p.getPrevious(), count++) {
                if (p.getFeatures().isPresent("accent")) {
                    break;
                }
            }
            return (byte)rail(count);
        }
    }
  
    /**
     * Counts the number of stressed syllables until the next major break. This
     * is a feature processor. A feature processor takes an item, performs some
     * sort of processing on the item and returns an object.
     */
    public static class NextAccent implements ByteValuedFeatureProcessor
    {
        TargetItemNavigator navigator;
        public NextAccent() {
            this.navigator = new SyllableNavigator();
        }

        public String getName() { return "mary_next_accent"; }
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

            for (Item p = ss; p != null; p = p.getNext(), count++) {
                if (p.getFeatures().isPresent("accent")) {
                    break;
                }
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

    
    
    ////////////////////////////////////////////////////////
    // TODO: Remove or convert old feature processors below.
    ////////////////////////////////////////////////////////
    





    /**
     * Counts the number of phrases before this one. This is a feature
     * processor. A feature processor takes an item, performs some sort of
     * processing on the item and returns an object.
     */
    public static class SubPhrases implements ByteValuedFeatureProcessor
    {
        public String getName() { return "sub_phrases"; }
        public String[] getValues() { return null; }

        public byte process(Target target)
        {
            return 0;
        }

        /**
         * Performs some processing on the given item.
         * 
         * @param item
         *            the item to process
         * 
         * @return the number of phrases before this one
         * 
         * @throws ProcessException
         *             if an exception occurred during the processing
         */
        public String process(Item item) throws ProcessException
        {
/*            int count = 0;
            Item inPhrase = (Item) SUB_PHRASE_PATH.findTarget(item);
            // Item inPhrase = new PhraseNavigator().getItem(target).getPrevious();

            for (Item p = inPhrase; p != null; p = p.getPrevious()) {
                count++;
            }
            return Integer.toString(rail(count));
*/          return null;
        }
    }

    /**
     * Returns the duration of the given segment This is a feature processor. A
     * feature processor takes an item, performs some sort of processing on the
     * item and returns an object.
     */
    public static class SegmentDuration implements ContinuousFeatureProcessor
    {
        public String getName() { return "segment_duration"; }
        public String[] getValues() { return null; }

        public float process(Target target)
        {
            return 0;
        }

        /**
         * Performs some processing on the given item.
         * 
         * @param seg
         *            the item to process
         * 
         * @return the duration of the segment as a string.
         * 
         * @throws ProcessException
         *             if an exception occurred during the processing
         */
        public String process(Item seg) throws ProcessException
        {
            if (seg == null) {
                return "0";
            } else if (seg.getPrevious() == null) {
                return seg.getFeatures().getObject("end").toString();
            } else {
                return Float.toString(seg.getFeatures().getFloat("end")
                        - seg.getPrevious().getFeatures().getFloat("end"));
            }
        }
    }

    /**
     * Checks if segment is sylfinal This is a feature processor. A feature
     * processor takes an item, performs some sort of processing on the item and
     * returns an object.
     */
    public static class SylFinal implements ByteValuedFeatureProcessor
    {
        public String getName() { return "syl_final"; }
        public String[] getValues() { return null; }

        public byte process(Target target)
        {
            return 0;
        }

        public String process(Item seg) throws ProcessException
        {
            Item sylItem = seg.getItemAs(Relation.SYLLABLE_STRUCTURE);
            if (sylItem == null || sylItem.getNext() != null) {
                return "0";
            } else {
                return "1";
            }
        }
    }


    /**
     * Calculates the pitch of a segment This processor should be used by target
     * items only
     */
    public static class Seg_Pitch implements ContinuousFeatureProcessor
    {
        public String getName() { return "seg_pitch"; }
        public String[] getValues() { return null; }

        public float process(Target target)
        {
            return 0;
        }
        public String process(Item seg) throws ProcessException
        {
            // System.out.println("Looking for pitch...");
            // get mid position of segment
            float mid;
            float end = seg.getFeatures().getFloat("end");
            Item prev = seg.getPrevious();
            if (prev == null) {
                mid = end / 2;
            } else {
                float prev_end = prev.getFeatures().getFloat("end");
                mid = prev_end + (end - prev_end) / 2;
            }
            Relation targetRelation = seg.getUtterance().getRelation("Target");
            // if segment has no target relation, you can not calculate
            // the segment pitch
            if (targetRelation == null) {
                return "0.0";
            }
            // get F0 and position of previous and next target
            Item nextTargetItem = targetRelation.getHead();
            while (nextTargetItem != null
                    && nextTargetItem.getFeatures().getFloat("pos") < mid) {
                nextTargetItem = nextTargetItem.getNext();
            }
            if (nextTargetItem == null)
                return "0.0";
            Item lastTargetItem = nextTargetItem.getPrevious();
            if (lastTargetItem == null)
                return "0.0";
            float lastF0 = lastTargetItem.getFeatures().getFloat("f0");
            float lastPos = lastTargetItem.getFeatures().getFloat("pos");
            float nextF0 = nextTargetItem.getFeatures().getFloat("f0");
            float nextPos = nextTargetItem.getFeatures().getFloat("pos");
            assert lastPos <= mid && mid <= nextPos;
            // build a linear function (f(x) = slope*x+intersectionYAxis)
            float slope = (nextF0 - lastF0) / (nextPos - lastPos);
            // calculate the pitch
            float pitch = lastF0 + slope * (mid - lastPos);
            if (!(lastF0 <= pitch && pitch <= nextF0 || nextF0 <= pitch
                    && pitch <= lastF0)) {
                throw new NullPointerException();
            }

            if (Float.isNaN(pitch)) {
                pitch = (float) 0.0;
            }
            return Float.toString(pitch);
        }
    }
    
    
}
