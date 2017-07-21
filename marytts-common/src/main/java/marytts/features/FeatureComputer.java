package marytts.features;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import marytts.data.SupportedSequenceType;
import marytts.data.Utterance;
import marytts.data.item.Item;

/**
 * The feature computer. To compute the feature, we assume the availability of 3
 * processors: the level processor, the context processor and the feature
 * processor. The first two processors are meant to navigate in the utterance
 * vertically and horizontally respectively. The feature processor is actually
 * computing the feature of the target item; the one obtained after executing
 * the level and the context processors.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class FeatureComputer {

    /**
     * Constant for the level processor index in the "m_features" table value
     */
    private final static int LEVEL_INDEX = 0;

    /**
     * Constant for the context processor index in the "m_features" table value
     */
    private final static int CONTEXT_INDEX = 1;

    /**
     * Constant for the feature processor index in the "m_features" table value
     */
    private final static int FEATURE_INDEX = 2;

    /** Table of features name => (level, context, feature) */
    protected Hashtable<String, String[]> m_features;

    /** The context processor factory */
    protected ContextProcessorFactory m_context_factory;

    /** The level processor factory */
    protected LevelProcessorFactory m_level_factory;

    /** The feature processor factory */
    protected FeatureProcessorFactory m_feature_factory;

    /**
     * The list of feature names. The order is important that's why it is not a
     * set !
     */
    protected ArrayList<String> m_feature_names;

    /**
     * The user customisation constructor. The user has to defined all the
     * needed factories.
     *
     * @param level_factory
     *            the level processor factory
     * @param context_factory
     *            the context processor factory
     * @param feature_factory
     *            the feature processor factory
     */
    public FeatureComputer(LevelProcessorFactory level_factory, ContextProcessorFactory context_factory,
                           FeatureProcessorFactory feature_factory) {
        m_features = new Hashtable<String, String[]>();
        m_feature_names = new ArrayList<String>();
        m_context_factory = context_factory;
        m_feature_factory = feature_factory;
        m_level_factory = level_factory;
    }

    /**
     * Add a feature definition composed by its name, the level processor name,
     * the context processor name and the feature processor name
     *
     * @param name
     *            the name to identify the feature we want to compute
     * @param level
     *            the level processor name
     * @param context
     *            the context processor name
     * @param feature
     *            the feature processor name
     * @throws FeatureCollisionException
     *             if the feature, identified by its name, is already in the map
     */
    public void addFeature(String name, String level, String context,
                           String feature) throws FeatureCollisionException {

        if (m_features.containsKey(name)) {
            throw new FeatureCollisionException(name + " is already an added feature");
        }

        m_features.put(name, new String[] {level, context, feature});
        m_feature_names.add(name);
    }

    /**
     * Compute the target feature based on the given informations
     *
     * @param utt
     *            the utterance containing the data
     * @param item
     *            the item for which we want the feature
     * @param level
     *            the level processor name
     * @param context
     *            the context processor name
     * @param feature
     *            the feature processor name
     * @return the feature or Feature.UNDEF_FEATURE if the result is undefined
     * @throws Exception
     *             if any kind of errors happened
     */
    protected Feature compute(Utterance utt, Item item, String level, String context,
                              String feature) throws Exception {
        LevelProcessor level_processor = m_level_factory.createLevelProcessor(level);
        ArrayList<? extends Item> level_items = level_processor.get(utt, item);
        if (level_items.size() == 0) {
            return Feature.UNDEF_FEATURE;
        }

        ContextProcessor context_processor = m_context_factory.createContextProcessor(context);
        Item context_item = context_processor.get(level_items.get(0));
        if (context_item == null) {
            return Feature.UNDEF_FEATURE;
        }

        FeatureProcessor feature_processor = m_feature_factory.createFeatureProcessor(feature);
        if (feature_processor == null) {
            throw new Exception(feature + " is not part of the factory");
        }

        return feature_processor.generate(utt, context_item);
    }

    /**
     * Generate the feature map for the given item
     *
     * @param utt
     *            the utterance containing the data
     * @param item
     *            the source item
     * @return the feature map for the given item
     * @throws Exception
     *             if anything is going wrong
     */
    public FeatureMap process(Utterance utt, Item item) throws Exception {
        FeatureMap feature_map = new FeatureMap();
        for (String feature_name : m_features.keySet()) {
            String[] infos = m_features.get(feature_name);
            Feature feature = compute(utt, item, infos[LEVEL_INDEX], infos[CONTEXT_INDEX],
                                      infos[FEATURE_INDEX]);

            // Update
            if (feature != null) {
                feature_map.put(feature_name, feature);
            }
        }

        return feature_map;
    }

    /**
     * List all the feature used for this process
     *
     * @return the list of feature names
     */
    public List<String> listFeatures() {
        return m_feature_names;
    }

    /** A temporary default feature computer (will be deleted soon) */
    public static FeatureComputer the_feature_computer = null;

    /**
     * A temporary initialisation default feature computer (will be deleted
     * soon)
     */
    public static void initDefault() throws Exception {

        // Generate the category
        FeatureProcessorFactory feat_fact = new FeatureProcessorFactory();
        feat_fact.addFeatureProcessor("nbfromsyllable",
                                      "marytts.features.featureprocessor.NbFromSyllableStart");
        feat_fact.addFeatureProcessor("nbtosyllable", "marytts.features.featureprocessor.NbToSyllableEnd");
        feat_fact.addFeatureProcessor("nbfromword", "marytts.features.featureprocessor.NbFromWordStart");
        feat_fact.addFeatureProcessor("nbtoword", "marytts.features.featureprocessor.NbToWordEnd");
        feat_fact.addFeatureProcessor("nbfromphrase",
                                      "marytts.features.featureprocessor.NbFromPhraseStart");
        feat_fact.addFeatureProcessor("nbtophrase", "marytts.features.featureprocessor.NbToPhraseEnd");
        feat_fact.addFeatureProcessor("nbfromsentence",
                                      "marytts.features.featureprocessor.NbFromSentenceStart");
        feat_fact.addFeatureProcessor("nbtosentence", "marytts.features.featureprocessor.NbToSentenceEnd");
        feat_fact.addFeatureProcessor("nbtoparagraph",
                                      "marytts.features.featureprocessor.NbToParagraphEnd");

        feat_fact.addFeatureProcessor("nbphones", "marytts.features.featureprocessor.NbPhonesRelated");
        feat_fact.addFeatureProcessor("nbsyllables",
                                      "marytts.features.featureprocessor.NbSyllablesRelated");
        feat_fact.addFeatureProcessor("nbwords", "marytts.features.featureprocessor.NbWordsRelated");
        feat_fact.addFeatureProcessor("nbphrases", "marytts.features.featureprocessor.NbPhrasesRelated");
        feat_fact.addFeatureProcessor("nbsentences",
                                      "marytts.features.featureprocessor.NbSentencesRelated");

        feat_fact.addFeatureProcessor("nbaccentfromphrase",
                                      "marytts.features.featureprocessor.NbAccentFromPhraseStart");
        feat_fact.addFeatureProcessor("nbaccenttophrase",
                                      "marytts.features.featureprocessor.NbAccentToPhraseEnd");

        feat_fact.addFeatureProcessor("accented", "marytts.features.featureprocessor.IsAccented");
        feat_fact.addFeatureProcessor("stressed", "marytts.features.featureprocessor.StressLevel");
        feat_fact.addFeatureProcessor("string", "marytts.features.featureprocessor.StringFeature");
        feat_fact.addFeatureProcessor("arpa", "marytts.features.featureprocessor.ArpaLabel");
        feat_fact.addFeatureProcessor("text", "marytts.features.featureprocessor.TextFeature");
        feat_fact.addFeatureProcessor("POS", "marytts.features.featureprocessor.POS");
        feat_fact.addFeatureProcessor("InDirectSpeech", "marytts.features.featureprocessor.InDirectSpeech");

        ContextProcessorFactory ctx_fact = new ContextProcessorFactory();
        ctx_fact.addContextProcessor("previousprevious",
                                     "marytts.features.contextprocessor.PreviousPrevious");
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
        FeatureComputer.the_feature_computer.addFeature("phone", "current", "current", "arpa");
        FeatureComputer.the_feature_computer.addFeature("prev_prev_phone", "current", "previousprevious",
                "arpa");
        FeatureComputer.the_feature_computer.addFeature("prev_phone", "current", "previous", "arpa");
        FeatureComputer.the_feature_computer.addFeature("next_phone", "current", "next", "arpa");
        FeatureComputer.the_feature_computer.addFeature("next_next_phone", "current", "nextnext", "arpa");
        FeatureComputer.the_feature_computer.addFeature("ph_from_syl_start", "current", "current",
                "nbfromsyllable");
        FeatureComputer.the_feature_computer.addFeature("ph_from_syl_end", "current", "current",
                "nbtosyllable");

        // Syllable
        FeatureComputer.the_feature_computer.addFeature("prev_syl_numph", "syllable", "previous",
                "nbphones");
        FeatureComputer.the_feature_computer.addFeature("prev_syl_accent", "syllable", "previous",
                "accented");
        FeatureComputer.the_feature_computer.addFeature("prev_syl_stress", "syllable", "previous",
                "stressed");
        FeatureComputer.the_feature_computer.addFeature("syl_numph", "syllable", "current", "nbphones");
        FeatureComputer.the_feature_computer.addFeature("syl_accent", "syllable", "current", "accented");
        FeatureComputer.the_feature_computer.addFeature("syl_stress", "syllable", "current", "stressed");
        FeatureComputer.the_feature_computer.addFeature("next_syl_numph", "syllable", "next", "nbphones");
        FeatureComputer.the_feature_computer.addFeature("next_syl_accent", "syllable", "next", "accented");
        FeatureComputer.the_feature_computer.addFeature("next_syl_stress", "syllable", "next", "stressed");

        FeatureComputer.the_feature_computer.addFeature("syls_from_word_end", "syllable", "current",
                "nbtoword");
        FeatureComputer.the_feature_computer.addFeature("syls_from_phrase_end", "syllable", "current",
                "nbtophrase");

        // Words
        FeatureComputer.the_feature_computer.addFeature("prev_word_pos", "word", "previous", "POS");
        FeatureComputer.the_feature_computer.addFeature("prev_word_numsyls", "word", "previous",
                "nbsyllables");
        FeatureComputer.the_feature_computer.addFeature("word_pos", "word", "current", "POS");
        FeatureComputer.the_feature_computer.addFeature("word_numsyls", "word", "current", "nbsyllables");
        FeatureComputer.the_feature_computer.addFeature("next_word_pos", "word", "next", "POS");
        FeatureComputer.the_feature_computer.addFeature("next_word_numsyls", "word", "next", "nbsyllables");
        FeatureComputer.the_feature_computer.addFeature("word_numsegs", "word", "current", "nbphones");
        FeatureComputer.the_feature_computer.addFeature("words_from_phrase_end", "word", "current",
                "nbtophrase");
        FeatureComputer.the_feature_computer.addFeature("words_from_sentence_end", "word", "current",
                "nbtosentence");

        // Phrases
        FeatureComputer.the_feature_computer.addFeature("prev_phrase_numsyls", "phrase", "previous",
                "nbsyllables");
        FeatureComputer.the_feature_computer.addFeature("prev_phrase_numwords", "phrase", "previous",
                "nbwords");
        FeatureComputer.the_feature_computer.addFeature("phrase_numsyls", "phrase", "current",
                "nbsyllables");
        FeatureComputer.the_feature_computer.addFeature("phrase_numwords", "phrase", "current", "nbwords");
        FeatureComputer.the_feature_computer.addFeature("next_phrase_numsyls", "phrase", "next",
                "nbsyllables");
        FeatureComputer.the_feature_computer.addFeature("next_phrase_numwords", "phrase", "next",
                "nbwords");
        FeatureComputer.the_feature_computer.addFeature("phrases_from_sentence_end", "phrase", "current",
                "nbtosentence");

        // Sentences
        FeatureComputer.the_feature_computer.addFeature("sentence_numsyllables", "sentence", "current",
                "nbsyllables");
        FeatureComputer.the_feature_computer.addFeature("sentence_numwords", "sentence", "current",
                "nbwords");
        FeatureComputer.the_feature_computer.addFeature("sentence_numphrases", "sentence", "current",
                "nbphrases");
        FeatureComputer.the_feature_computer.addFeature("sentence_from_paragraph_end", "sentence",
                "current",
                "nbtoparagraph");

        // Paragraph
        FeatureComputer.the_feature_computer.addFeature("paragraph_numsentences", "paragraph", "current",
                "nbsentences");

        // Speech info
        FeatureComputer.the_feature_computer.addFeature("inDirectSpeech", "current", "current",
                "InDirectSpeech");
    }
}
