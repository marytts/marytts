/**
 * Copyright 2006-2007 DFKI GmbH.
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

import java.util.HashMap;
import java.util.Map;

public class FeatureProcessorManager 
{
    protected Map<String,MaryFeatureProcessor> processors;

    protected Map<String, String[]> phonefeatures2values;
    
    public FeatureProcessorManager()
    {
        processors = new HashMap<String,MaryFeatureProcessor>();

        MaryGenericFeatureProcessors.TargetItemNavigator segment = new MaryGenericFeatureProcessors.SegmentNavigator();
        MaryGenericFeatureProcessors.TargetItemNavigator prevSegment = new MaryGenericFeatureProcessors.PrevSegmentNavigator();
        MaryGenericFeatureProcessors.TargetItemNavigator nextSegment = new MaryGenericFeatureProcessors.NextSegmentNavigator();
        MaryGenericFeatureProcessors.TargetItemNavigator syllable = new MaryGenericFeatureProcessors.SyllableNavigator();
        MaryGenericFeatureProcessors.TargetItemNavigator prevSyllable = new MaryGenericFeatureProcessors.PrevSyllableNavigator();
        MaryGenericFeatureProcessors.TargetItemNavigator nextSyllable = new MaryGenericFeatureProcessors.NextSyllableNavigator();
        MaryGenericFeatureProcessors.TargetItemNavigator nextNextSyllable = new MaryGenericFeatureProcessors.NextNextSyllableNavigator();
        MaryGenericFeatureProcessors.TargetItemNavigator lastWord = new MaryGenericFeatureProcessors.LastWordInSentenceNavigator();

        addFeatureProcessor(new MaryGenericFeatureProcessors.Edge());
        addFeatureProcessor(new MaryGenericFeatureProcessors.HalfPhoneLeftRight());
        addFeatureProcessor(new MaryGenericFeatureProcessors.Accented("mary_accented", syllable));
        addFeatureProcessor(new MaryGenericFeatureProcessors.Stressed("mary_stressed", syllable));
        addFeatureProcessor(new MaryGenericFeatureProcessors.Stressed("mary_prev_stressed", prevSyllable));
        addFeatureProcessor(new MaryGenericFeatureProcessors.Stressed("mary_next_stressed", nextSyllable));
        addFeatureProcessor(new MaryGenericFeatureProcessors.WordNumSyls());
        addFeatureProcessor(new MaryGenericFeatureProcessors.PosInSyl());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SylBreak("mary_syl_break", syllable));
        addFeatureProcessor(new MaryGenericFeatureProcessors.SylBreak("mary_prev_syl_break", prevSyllable));
        addFeatureProcessor(new MaryGenericFeatureProcessors.PositionType());
        addFeatureProcessor(new MaryGenericFeatureProcessors.IsPause("mary_prev_is_pause", prevSegment));
        addFeatureProcessor(new MaryGenericFeatureProcessors.IsPause("mary_next_is_pause", nextSegment));
        addFeatureProcessor(new MaryGenericFeatureProcessors.TobiAccent("mary_tobi_accent", syllable));
        addFeatureProcessor(new MaryGenericFeatureProcessors.TobiAccent("mary_next_tobi_accent", nextSyllable));
        addFeatureProcessor(new MaryGenericFeatureProcessors.TobiAccent("mary_nextnext_tobi_accent", nextNextSyllable));
        addFeatureProcessor(new MaryGenericFeatureProcessors.TobiEndtone("mary_tobi_endtone", syllable));
        addFeatureProcessor(new MaryGenericFeatureProcessors.TobiEndtone("mary_next_tobi_endtone", nextSyllable));
        addFeatureProcessor(new MaryGenericFeatureProcessors.TobiEndtone("mary_nextnext_tobi_endtone", nextNextSyllable));
        addFeatureProcessor(new MaryGenericFeatureProcessors.WordPunc("mary_sentence_punc", lastWord));
        addFeatureProcessor(new MaryGenericFeatureProcessors.SylsFromPhraseStart());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SylsFromPhraseEnd());
        addFeatureProcessor(new MaryGenericFeatureProcessors.StressedSylsFromPhraseStart());
        addFeatureProcessor(new MaryGenericFeatureProcessors.StressedSylsFromPhraseEnd());
        addFeatureProcessor(new MaryGenericFeatureProcessors.AccentedSylsFromPhraseStart());
        addFeatureProcessor(new MaryGenericFeatureProcessors.AccentedSylsFromPhraseEnd());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SylsFromPrevStressed());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SylsToNextStressed());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SylsFromPrevAccent());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SylsToNextAccent());
        addFeatureProcessor(new MaryGenericFeatureProcessors.WordNumSegs());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SegsFromSylStart());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SegsFromSylEnd());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SylNumSegs());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SentenceNumPhrases());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SentenceNumWords());
        addFeatureProcessor(new MaryGenericFeatureProcessors.PhraseNumWords());
        addFeatureProcessor(new MaryGenericFeatureProcessors.PhraseNumSyls());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SegsFromWordStart());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SegsFromWordEnd());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SylsFromWordStart());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SylsFromWordEnd());
        addFeatureProcessor(new MaryGenericFeatureProcessors.WordsFromPhraseStart());
        addFeatureProcessor(new MaryGenericFeatureProcessors.WordsFromPhraseEnd());
        addFeatureProcessor(new MaryGenericFeatureProcessors.WordsFromSentenceStart());
        addFeatureProcessor(new MaryGenericFeatureProcessors.WordsFromSentenceEnd());
        addFeatureProcessor(new MaryGenericFeatureProcessors.PhrasesFromSentenceStart());
        addFeatureProcessor(new MaryGenericFeatureProcessors.PhrasesFromSentenceEnd());
        addFeatureProcessor(new MaryGenericFeatureProcessors.NextAccent());
        addFeatureProcessor(new MaryGenericFeatureProcessors.PrevAccent());
        addFeatureProcessor(new MaryGenericFeatureProcessors.PhraseEndtone());
        addFeatureProcessor(new MaryGenericFeatureProcessors.PrevPhraseEndtone());
        addFeatureProcessor(new MaryGenericFeatureProcessors.PrevPunctuation());
        addFeatureProcessor(new MaryGenericFeatureProcessors.NextPunctuation());
        addFeatureProcessor(new MaryGenericFeatureProcessors.WordsFromPrevPunctuation());
        addFeatureProcessor(new MaryGenericFeatureProcessors.WordsToNextPunctuation());
        addFeatureProcessor(new MaryGenericFeatureProcessors.Selection_Prosody(syllable));
        addFeatureProcessor(new MaryGenericFeatureProcessors.SentenceStyle());
        addFeatureProcessor(new MaryGenericFeatureProcessors.UnitDuration());
        addFeatureProcessor(new MaryGenericFeatureProcessors.UnitLogF0());

        // Set up default values for phone features:
        phonefeatures2values = new HashMap<String, String[]>();
        // cplace: 0-n/a l-labial a-alveolar p-palatal b-labio_dental d-dental v-velar g-?
        phonefeatures2values.put("cplace", new String[] { "0", "l", "a", "p", "b", "d", "v", "g"});
        // ctype: 0-n/a s-stop f-fricative a-affricative n-nasal l-liquid r-r
        phonefeatures2values.put("ctype", new String[] {"0", "s", "f", "a", "n", "l", "r"});
        // cvox: 0=n/a +=on -=off
        phonefeatures2values.put("cvox", new String[] {"0", "+", "-"});
        // vc: 0=n/a +=vowel -=consonant
        phonefeatures2values.put("vc", new String[] {"0", "+", "-"});
        // vfront: 0-n/a 1-front  2-mid 3-back
        phonefeatures2values.put("vfront", new String[] {"0", "1", "2", "3"});
        // vheight: 0-n/a 1-high 2-mid 3-low
        phonefeatures2values.put("vheight", new String[] {"0", "1", "2", "3"});
        // vlng: 0-n/a s-short l-long d-dipthong a-schwa
        phonefeatures2values.put("vlng", new String[] {"0", "s", "l", "d", "a"});
        // vrnd: 0=n/a +=on -=off
        phonefeatures2values.put("vrnd", new String[] {"0", "+", "-"});
    }
    
    protected void addFeatureProcessor(MaryFeatureProcessor fp)
    {
        processors.put(fp.getName(), fp);
    }
    
    public MaryFeatureProcessor getFeatureProcessor(String name)
    {
        return processors.get(name);
    }
    
    protected void setupPhonemeFeatureProcessors(PhoneSet phoneset, String[] phonemeValues)
    {
        MaryGenericFeatureProcessors.TargetItemNavigator segment = new MaryGenericFeatureProcessors.SegmentNavigator();

        addFeatureProcessor(new MaryLanguageFeatureProcessors.Phoneme("mary_phoneme", phonemeValues, segment));
        addFeatureProcessor(new MaryLanguageFeatureProcessors.HalfPhoneUnitName(phonemeValues));
        addFeatureProcessor(new MaryLanguageFeatureProcessors.SegOnsetCoda(phoneset));
        // Phone features:
        for (String feature : phonefeatures2values.keySet()) {
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature
                    (phoneset, "mary_ph_"+feature, feature, phonefeatures2values.get(feature), segment));
        }

        Map<String,MaryGenericFeatureProcessors.TargetItemNavigator> segments =
            new HashMap<String, MaryGenericFeatureProcessors.TargetItemNavigator>();
        segments.put("prev", new MaryGenericFeatureProcessors.PrevSegmentNavigator());
        segments.put("prev_prev", new MaryGenericFeatureProcessors.PrevPrevSegmentNavigator());
        segments.put("prev", new MaryGenericFeatureProcessors.NextSegmentNavigator());
        segments.put("next_next", new MaryGenericFeatureProcessors.NextNextSegmentNavigator());

        for (String position : segments.keySet()) {
            MaryGenericFeatureProcessors.TargetItemNavigator navi = segments.get(position);
            addFeatureProcessor(new MaryLanguageFeatureProcessors.Phoneme("mary_"+position+"_phoneme", phonemeValues, segment));
            // Phone features:
            for (String feature : phonefeatures2values.keySet()) {
                addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature
                        (phoneset, "mary_"+position+"_"+feature, feature, phonefeatures2values.get(feature), navi));
            }
            
        }

    }

}
