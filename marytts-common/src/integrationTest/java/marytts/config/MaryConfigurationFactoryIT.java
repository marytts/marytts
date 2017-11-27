package marytts.config;

import marytts.exceptions.MaryConfigurationException;


import org.testng.Assert;
import org.testng.annotations.*;

public class MaryConfigurationFactoryIT {
    @Test
    public void testDefaultConfigurationLoading() throws MaryConfigurationException {
	PropertiesMaryConfigLoader loader = new PropertiesMaryConfigLoader();
	System.out.println("MaryConfiguration = " + MaryConfigurationFactory.getDefaultConfiguration().toString());
    }
}
