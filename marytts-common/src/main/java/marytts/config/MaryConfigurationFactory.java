package marytts.config;

import java.util.ArrayList;
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
    private static HashMap<String, ArrayList<MaryConfiguration>> configuration_map;
    static {
        configuration_map = new HashMap<String, ArrayList<MaryConfiguration>>();
        ArrayList<MaryConfiguration> list_conf = new ArrayList<MaryConfiguration>();
        list_conf.add(new MaryConfiguration());
        configuration_map.put(DEFAULT_KEY, list_conf);
    }

    /**
     *  Method to add a configuration to a set. A set can be a locale or anything else
     *
     *  @param set the set
     *  @param configuration the configuration to add
     *  @fixme maybe avoid overriding default?
     */
    public synchronized static void pushConfiguration(String set, MaryConfiguration configuration) throws MaryConfigurationException {
        if (set.equals(DEFAULT_KEY)) {
            throw new MaryConfigurationException("It is forbidden to have multiple default configurations");
        }
        int i = 0;
        if (configuration_map.get(set) != null) {
            i = configuration_map.get(set).size();
        }
        addConfiguration(set, configuration, i);
    }

    /**
     *  Method to add a configuration to a set. A set can be a locale or anything else
     *
     *  @param set the set
     *  @param configuration the configuration to add
     *  @fixme maybe avoid overriding default?
     */
    public synchronized static void addConfiguration(String set, MaryConfiguration configuration) throws MaryConfigurationException {
        addConfiguration(set, configuration, 0);
    }

    /**
     *  Method to add a configuration to a set. A set can be a locale or anything else
     *
     *  @param set the set
     *  @param configuration the configuration to add
     *  @fixme maybe avoid overriding default?
     */
    public synchronized static void addConfiguration(String set, MaryConfiguration configuration, int i) throws MaryConfigurationException {
	if (configuration_map.containsKey(set)) {
            if (i < configuration_map.get(set).size()) {
                configuration_map.get(set).get(i).merge(configuration);
            } else if (i == configuration_map.get(set).size()) {
                configuration_map.get(set).add(i, configuration);
            } else {
                throw new MaryConfigurationException(String.format("The index \"%d\" of the configuration is illogical as the set \"%s\" contains only \"%d\" available configurations",
                                                                   i, set, configuration_map.get(set).size()));
            }
	} else if (i == 0) {
            ArrayList<MaryConfiguration> tmp = new ArrayList<MaryConfiguration>();
            tmp.add(configuration);
            configuration_map.put(set, tmp);
        } else {
            throw new MaryConfigurationException(String.format("Index \"%d\" is invalid as no configurations for the set \"set\" has yet been found",
                                                               i, set));
        }
    }


    /**
     *  Method to get the first configuration knowning the set
     *
     *  @param set the set
     *  @return the configuration
     *  @fixme see for cloning
     */
    public synchronized static MaryConfiguration getConfiguration(String set) {
        return getConfiguration(set, 0);
    }

    /**
     *  Method to get the configuration knowning the set
     *
     *  @param set the set
     *  @param i the index of the configuration (for a module called multiple times)
     *  @return the configuration
     *  @fixme see for cloning
     */
    public synchronized static MaryConfiguration getConfiguration(String set, int i) {
	return configuration_map.get(set).get(i);
    }

    /**
     *  Method to get the default configuration
     *
     *  @return the default configuration
     */
    public synchronized static MaryConfiguration getDefaultConfiguration() {
	return configuration_map.get(MaryConfigurationFactory.DEFAULT_KEY).get(0);
    }

    public synchronized static void applyDefaultConfiguration(Object obj) throws MaryConfigurationException {
	getDefaultConfiguration().applyConfiguration(obj);
    }

    public synchronized static String dump() {
	String result = "{";
	for (String key: configuration_map.keySet()) {
	    result += key + ": \n";
            for (MaryConfiguration conf: configuration_map.get(key)) {
                result += "\t - " + conf.toString().replaceAll("\\\n", "\\\n\\\t\\\t");
                result += "\n";
            }
	    result += "\n";
	}
	return result;
    }
}
