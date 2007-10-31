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
    protected Map processors;
    
    public FeatureProcessorManager()
    {
        processors = new HashMap();

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
        
    }
    
    protected void addFeatureProcessor(MaryFeatureProcessor fp)
    {
        processors.put(fp.getName(), fp);
    }
    
    public MaryFeatureProcessor getFeatureProcessor(String name)
    {
        return (MaryFeatureProcessor) processors.get(name);
    }
}
