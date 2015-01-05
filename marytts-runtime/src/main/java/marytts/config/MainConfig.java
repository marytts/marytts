/**
 * 
 */
package marytts.config;

import marytts.exceptions.MaryConfigurationException;

/**
 * @author marc
 *
 */
public class MainConfig extends MaryConfig {

	public MainConfig() throws MaryConfigurationException {
		super(MainConfig.class.getResourceAsStream("marybase.config"));
	}

	@Override
	public boolean isMainConfig() {
		return true;
	}
}
