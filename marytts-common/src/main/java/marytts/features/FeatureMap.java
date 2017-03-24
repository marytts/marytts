package marytts.features;

import java.util.Hashtable;
import marytts.data.item.Item;
import java.util.Map;

/**
 * For now just extend hashtable
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class FeatureMap extends Item // implements Map<String, Feature>
{
    private Hashtable<String, Feature> inner_map;

    public FeatureMap()
    {
        super();
        inner_map = new Hashtable<String, Feature>();
    }


    public void put(String feature_name, Feature the_feature)
    {
        inner_map.put(feature_name, the_feature);
    }

    public Map<String, Feature> getMap()
    {
        return inner_map;
    }


    public Feature get(String feature_name)
    {
        return inner_map.get(feature_name);
    }
}
