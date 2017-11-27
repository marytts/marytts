package marytts.config;

import java.util.HashMap;


/**
 *
 *
 * @author <a href="mailto:slemaguer@slemaguer-perso"></a>
 */
public class MaryConfigurationFactory
{
    private static HashMap<String, MaryConfiguration> configuration_map  = new HashMap<String, MaryConfiguration>();



    public synchronized static void addConfiguration(String set, MaryConfiguration configuration) {
	configuration_map.put(set, configuration);
    }

    public synchronized MaryConfiguration getConfiguration(String set) {
	return configuration_map.get(set);
    }
}


/* MaryConfigurationFactory.java ends here */
