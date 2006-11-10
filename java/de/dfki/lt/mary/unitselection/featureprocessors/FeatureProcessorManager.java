package de.dfki.lt.mary.unitselection.featureprocessors;

import java.util.HashMap;
import java.util.Map;

public class FeatureProcessorManager 
{
    // TODO: use this to replace UnitSelectionFeatProcManager et al.
    
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
        MaryGenericFeatureProcessors.TargetItemNavigator lastWord = new MaryGenericFeatureProcessors.LastWordNavigator();

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
        addFeatureProcessor(new MaryGenericFeatureProcessors.NextAccent());
        addFeatureProcessor(new MaryGenericFeatureProcessors.LastAccent());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SylIn());
        addFeatureProcessor(new MaryGenericFeatureProcessors.SylOut());
        addFeatureProcessor(new MaryGenericFeatureProcessors.StressedSylIn());
        addFeatureProcessor(new MaryGenericFeatureProcessors.StressedSylOut());
        addFeatureProcessor(new MaryGenericFeatureProcessors.AccentedSylIn());
        addFeatureProcessor(new MaryGenericFeatureProcessors.AccentedSylOut());
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
