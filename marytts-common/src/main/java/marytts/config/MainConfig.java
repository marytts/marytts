/**
 *
 */
package marytts.config;

import marytts.exceptions.MaryConfigurationException;

/**
 * @author marc
 *
 */
public class MainConfig extends PropertiesMaryConfig {

    public MainConfig() throws MaryConfigurationException {
        super(MainConfig.class.getResourceAsStream("default.config"));
    }

    @Override
    public boolean isMainConfig() {
        return true;
    }
}
