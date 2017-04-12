package marytts.features;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Set;

import marytts.data.SupportedSequenceType;
import marytts.data.Utterance;
import marytts.data.item.Item;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class FeatureComputer
{
    protected Hashtable<String, String[]> m_features;

    protected ContextProcessorFactory m_context_factory;
    protected LevelProcessorFactory m_level_factory;
    protected FeatureProcessorFactory m_feature_factory;

    public FeatureComputer(LevelProcessorFactory level_factory,
                           ContextProcessorFactory context_factory,
                           FeatureProcessorFactory feature_factory)
    {
        m_features = new Hashtable<String, String[]>();
        m_context_factory = context_factory;
        m_feature_factory = feature_factory;
        m_level_factory = level_factory;
    }

    public void addFeature(String name, String level, String context, String feature)
    {
        m_features.put(name, new String[]{level, context, feature});
    }

    public Feature compute(Utterance utt, Item item, String level, String context, String feature)
        throws Exception // FIXME: be more specific
    {
        LevelProcessor level_processor = m_level_factory.createLevelProcessor(level);
        ArrayList<? extends Item> level_items = level_processor.generate(utt, item);
        if (level_items.size() == 0)
            return null;

        ContextProcessor context_processor = m_context_factory.createContextProcessor(context);
        Item context_item = context_processor.generate(utt, level_items.get(0));
        if (context_item == null)
            return null;

        FeatureProcessor feature_processor = m_feature_factory.createFeatureProcessor(feature);
        return feature_processor.generate(utt, context_item);
    }

    public FeatureMap process(Utterance utt, Item item)
        throws Exception // FIXME: be more specific
    {
        FeatureMap feature_map = new FeatureMap();
        for (String feature_name: m_features.keySet())
        {
            String[] infos = m_features.get(feature_name);
            Feature feature =  compute(utt, item, infos[0], infos[1], infos[2]); // FIXME: using constants instead of hardcoded indexes
            if (feature != null)
                feature_map.put(feature_name, feature);
        }

        return feature_map;
    }

    public Set<String> listFeatures()
    {
        return m_features.keySet();
    }
    /* */
    public static FeatureComputer the_feature_computer = null;

    public static void initDefault()
        throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {

        // Generate the category
        FeatureProcessorFactory feat_fact = new FeatureProcessorFactory();
        feat_fact.addFeatureProcessor("nbfromsyllable", "marytts.features.featureprocessor.NbFromSyllableStart");
        feat_fact.addFeatureProcessor("nbtosyllable", "marytts.features.featureprocessor.NbToSyllableEnd");
        feat_fact.addFeatureProcessor("nbfromword", "marytts.features.featureprocessor.NbFromWordStart");
        feat_fact.addFeatureProcessor("nbtoword", "marytts.features.featureprocessor.NbToWordEnd");
        feat_fact.addFeatureProcessor("nbfromphrase", "marytts.features.featureprocessor.NbFromPhraseStart");
        feat_fact.addFeatureProcessor("nbtophrase", "marytts.features.featureprocessor.NbToPhraseEnd");
        feat_fact.addFeatureProcessor("nbfromsentence", "marytts.features.featureprocessor.NbFromSentenceStart");
        feat_fact.addFeatureProcessor("nbtosentence", "marytts.features.featureprocessor.NbToSentenceEnd");

        feat_fact.addFeatureProcessor("nbphones", "marytts.features.featureprocessor.NbPhonesRelated");
        feat_fact.addFeatureProcessor("nbsyllables", "marytts.features.featureprocessor.NbSyllablesRelated");
        feat_fact.addFeatureProcessor("nbwords", "marytts.features.featureprocessor.NbWordsRelated");
        feat_fact.addFeatureProcessor("nbphrases", "marytts.features.featureprocessor.NbPhrasesRelated");
        feat_fact.addFeatureProcessor("nbsentences", "marytts.features.featureprocessor.NbSentencesRelated");

        feat_fact.addFeatureProcessor("nbaccentfromphrase", "marytts.features.featureprocessor.NbAccentFromPhraseStart");
        feat_fact.addFeatureProcessor("nbaccenttophrase", "marytts.features.featureprocessor.NbAccentToPhraseEnd");

        feat_fact.addFeatureProcessor("accented", "marytts.features.featureprocessor.IsAccented");
        feat_fact.addFeatureProcessor("string", "marytts.features.featureprocessor.StringFeature");
        feat_fact.addFeatureProcessor("text", "marytts.features.featureprocessor.TextFeature");
        feat_fact.addFeatureProcessor("POS", "marytts.features.featureprocessor.POS");


        ContextProcessorFactory ctx_fact = new ContextProcessorFactory();
        ctx_fact.addContextProcessor("previousprevious", "marytts.features.contextprocessor.PreviousPrevious");
        ctx_fact.addContextProcessor("previous", "marytts.features.contextprocessor.Previous");
        ctx_fact.addContextProcessor("current", "marytts.features.contextprocessor.Current");
        ctx_fact.addContextProcessor("next", "marytts.features.contextprocessor.Next");
        ctx_fact.addContextProcessor("nextnext", "marytts.features.contextprocessor.NextNext");

        LevelProcessorFactory lvl_fact = new LevelProcessorFactory();
        lvl_fact.addLevelProcessor("current", "marytts.features.levelprocessor.CurrentLevel");
        lvl_fact.addLevelProcessor("syllable", "marytts.features.levelprocessor.SyllableLevel");
        lvl_fact.addLevelProcessor("word", "marytts.features.levelprocessor.WordLevel");
        lvl_fact.addLevelProcessor("phrase", "marytts.features.levelprocessor.PhraseLevel");
        lvl_fact.addLevelProcessor("sentence", "marytts.features.levelprocessor.SentenceLevel");
        lvl_fact.addLevelProcessor("paragraph", "marytts.features.levelprocessor.ParagraphLevel");

        // Populate feature computer
        FeatureComputer.the_feature_computer = new FeatureComputer(lvl_fact, ctx_fact, feat_fact);
        FeatureComputer.the_feature_computer.addFeature("phone", "current", "current", "string");
        FeatureComputer.the_feature_computer.addFeature("prev_prev_phone", "current", "previousprevious", "string");
        FeatureComputer.the_feature_computer.addFeature("prev_phone", "current", "previous", "string");
        FeatureComputer.the_feature_computer.addFeature("next_phone", "current", "next", "string");
        FeatureComputer.the_feature_computer.addFeature("next_next_phone", "current", "nextnext", "string");
        FeatureComputer.the_feature_computer.addFeature("pos_in_syl", "current", "current", "nbfromsyllable");

        // Syllable
        FeatureComputer.the_feature_computer.addFeature("prev_syl_numph", "syllable", "previous", "nbphones");
        FeatureComputer.the_feature_computer.addFeature("prev_syl_accent", "syllable", "previous", "accented");
        FeatureComputer.the_feature_computer.addFeature("syl_numph", "syllable", "current", "nbphones");
        FeatureComputer.the_feature_computer.addFeature("syl_accent", "syllable", "current", "accented");
        FeatureComputer.the_feature_computer.addFeature("next_syl_numph", "syllable", "next", "nbphones");
        FeatureComputer.the_feature_computer.addFeature("next_syl_accent", "syllable", "next", "accented");

        FeatureComputer.the_feature_computer.addFeature("accented_syls_from_phrase_end", "syllable", "current", "nbtophrase");
        FeatureComputer.the_feature_computer.addFeature("accented_syls_from_phrase_start", "syllable", "current", "nbfromphrase");

        FeatureComputer.the_feature_computer.addFeature("syls_from_phrase_end", "syllable", "current", "nbtophrase");
        FeatureComputer.the_feature_computer.addFeature("syls_from_phrase_start", "syllable", "current", "nbfromphrase");
        FeatureComputer.the_feature_computer.addFeature("syls_from_word_end", "syllable", "current", "nbtoword");
        FeatureComputer.the_feature_computer.addFeature("syls_from_word_start", "syllable", "current", "nbfromword");

        // Words
        FeatureComputer.the_feature_computer.addFeature("prev_word_pos", "word", "previous", "POS");
        FeatureComputer.the_feature_computer.addFeature("prev_word_numsyls", "word", "previous", "nbsyllables");
        FeatureComputer.the_feature_computer.addFeature("word_pos", "word", "current", "POS");
        FeatureComputer.the_feature_computer.addFeature("word_numsyls", "word", "current", "nbsyllables");
        FeatureComputer.the_feature_computer.addFeature("next_word_pos", "word", "next", "POS");
        FeatureComputer.the_feature_computer.addFeature("next_word_numsyls", "word", "next", "nbsyllables");
        FeatureComputer.the_feature_computer.addFeature("word_numsegs", "word", "current", "nbphones");
        FeatureComputer.the_feature_computer.addFeature("words_from_phrase_end", "word", "current", "nbtophrase");
        FeatureComputer.the_feature_computer.addFeature("words_from_phrase_start", "word", "current", "nbfromphrase");
        FeatureComputer.the_feature_computer.addFeature("words_from_sentence_end", "word", "current", "nbtosentence");
        FeatureComputer.the_feature_computer.addFeature("words_from_sentence_start", "word", "current", "nbfromsentence");

        // Phrases
        FeatureComputer.the_feature_computer.addFeature("prev_phrase_numsyls", "phrase", "previous", "nbsyllables");
        FeatureComputer.the_feature_computer.addFeature("prev_phrase_numwords", "phrase", "previous", "nbwords");
        FeatureComputer.the_feature_computer.addFeature("phrase_numsyls", "phrase", "current", "nbsyllables");
        FeatureComputer.the_feature_computer.addFeature("phrase_numwords", "phrase", "current", "nbwords");
        FeatureComputer.the_feature_computer.addFeature("next_phrase_numsyls", "phrase", "next", "nbsyllables");
        FeatureComputer.the_feature_computer.addFeature("next_phrase_numwords", "phrase", "next", "nbwords");
        FeatureComputer.the_feature_computer.addFeature("phrases_from_sentence_end", "phrase", "current", "nbtosentence");
        FeatureComputer.the_feature_computer.addFeature("phrases_from_sentence_start", "phrase", "current", "nbfromsentence");


        // Sentences
        FeatureComputer.the_feature_computer.addFeature("sentence_numsyllables", "sentence", "current", "nbsyllables");
        FeatureComputer.the_feature_computer.addFeature("sentence_numwords", "sentence", "current", "nbwords");
        FeatureComputer.the_feature_computer.addFeature("sentence_numphrases", "sentence", "current", "nbphrases");

        // Paragraph
        FeatureComputer.the_feature_computer.addFeature("paragraph_numsentences", "paragraph", "current", "nbsentences");

    }
}
