package marytts.features;

import java.util.Hashtable;
import java.util.ArrayList;

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
            else
                feature_map.put(feature_name, Feature.UNDEF_FEATURE);
        }

        return feature_map;
    }
}
