package marytts.config;

import java.util.HashMap;
import marytts.exceptions.MaryConfigurationException;

/**
 * The MaryConfiguration factory. All methods are static are we don't
 * need multiple instances of it.
 *
 * @author <a href="mailto:slemaguer@slemaguer-perso"></a>
 */
public class MaryConfigurationFactory
{
    /** The default configuration name in the map */
    public static final String DEFAULT_KEY = "default";

    /** The map containing the configuration */
    private static HashMap<String, MaryConfiguration> configuration_map  = new HashMap<String, MaryConfiguration>();

    /**
     *  Method to add a configuration to a set. A set can be a locale or anything else
     *
     *  @param set the set
     *  @param configuration the configuration to add
     */
    public synchronized static void addConfiguration(String set, MaryConfiguration configuration) {
	if (configuration_map.containsKey(set)) {
	    configuration_map.get(set).merge(configuration); // .merge();
	}
	configuration_map.put(set, configuration);
    }

    /**
     *  Method to get the configuration knowning the set
     *
     *  @param set the set
     *  @return the configuration
     */
    // FIXME: see for cloning
    public synchronized static MaryConfiguration getConfiguration(String set) {
	return configuration_map.get(set);
    }

    /**
     *  Method to get the default configuration
     *
     *  @return the default configuration
     */
    public synchronized static MaryConfiguration getDefaultConfiguration() {
	return configuration_map.get(MaryConfigurationFactory.DEFAULT_KEY);
    }

    public synchronized static void applyDefaultConfiguration(Object obj) throws MaryConfigurationException {
	getDefaultConfiguration().applyConfiguration(obj);
    }
}


/* MaryConfigurationFactory.java ends here */
