package marytts.features;

import java.util.Hashtable;
import marytts.data.item.Item;
import java.util.Map;

/**
 * Map which associate a feature name to its value. For now it is overriding
 * basic map methods. It is extending the Item class
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class FeatureMap extends Item // implements Map<String, Feature>
{
	/** The map which associate the feature name to the feature itself */
	private Hashtable<String, Feature> inner_map;

	/**
	 * Constructor
	 *
	 */
	public FeatureMap() {
		super();
		inner_map = new Hashtable<String, Feature>();
	}

	/**
	 * Put a feature into the map
	 *
	 * @param feature_name
	 *            the feature name
	 * @param the_feature
	 *            the actual feature
	 */
	public void put(String feature_name, Feature the_feature) {
		inner_map.put(feature_name, the_feature);
	}

	/**
	 * Get the feature map. It is a clone version as we don't authorize any
	 * modification outside of this class.
	 *
	 * @return the actual map
	 */
	public Map<String, Feature> getMap() {
		return (Map<String, Feature>) inner_map.clone();
	}

	/**
	 * Get the feature from its name
	 *
	 * @param feature_name
	 *            the name of the wanted feature
	 * @return the feature or null if the feature doesn't exists
	 */
	public Feature get(String feature_name) {
		return inner_map.get(feature_name);
	}

	/**
	 * Check if the given name has a corresponding feature
	 *
	 * @param feature_name
	 *            the name of the wanted feature
	 * @return true if the feature exists, false else
	 */
	public boolean containsKey(String feature_name) {
		return inner_map.containsKey(feature_name);
	}
}
