package marytts.features;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import marytts.exceptions.MaryConfigurationException;

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
    public FeatureComputer() {
        m_features = new Hashtable<String, String[]>();
        m_feature_names = new ArrayList<String>();
        m_context_factory = new ContextProcessorFactory();
        m_feature_factory = new FeatureProcessorFactory();
        m_level_factory = new LevelProcessorFactory();
    }

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


    public void addContextProcessor(String context_name, String context_class_name) throws MaryConfigurationException {
	try {
	    m_context_factory.addContextProcessor(context_name, context_class_name);
	} catch (Exception ex) {
	    throw new MaryConfigurationException("Cannot add context \"" + context_name +
						 "\" with class \"" + context_class_name + "\"",
						 ex);
	}
    }


    public void addLevelProcessor(String level_name, String level_class_name) throws MaryConfigurationException {
	try {
	    m_level_factory.addLevelProcessor(level_name, level_class_name);
	} catch (Exception ex) {
	    throw new MaryConfigurationException("Cannot add level \"" + level_name +
						 "\" with class \"" + level_class_name + "\"",
						 ex);
	}
    }


    public void addFeatureProcessor(String feature_name, String feature_class_name) throws MaryConfigurationException {
	try {
	    m_feature_factory.addFeatureProcessor(feature_name, feature_class_name);
	} catch (Exception ex) {
	    throw new MaryConfigurationException("Cannot add feature \"" + feature_name +
						 "\" with class \"" + feature_class_name + "\"",
						 ex);
	}
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
                           String feature) throws FeatureCollisionException, UnknownProcessorException {

        if (m_features.containsKey(name)) {
            throw new FeatureCollisionException(name + " is already an added feature");
        }

        if (! m_level_factory.canCreateLevelProcessor(level)) {
            throw new UnknownProcessorException(name + ": cannot create level processor \"" + level + "\"");
        }

        if (! m_context_factory.canCreateContextProcessor(context)) {
            throw new UnknownProcessorException(name + ": cannot create context processor \"" + context + "\"");
        }

        if (! m_feature_factory.canCreateFeatureProcessor(feature)) {
            throw new UnknownProcessorException(name + ": cannot create feature processor \"" + feature + "\"");
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
            throw new Exception(feature + " is not part of the factory for items : (level=" + level + ", context=" + context + ", feature=" +
                                feature + ")");
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
    public ArrayList<String> listFeatures() {
        return m_feature_names;
    }
}
