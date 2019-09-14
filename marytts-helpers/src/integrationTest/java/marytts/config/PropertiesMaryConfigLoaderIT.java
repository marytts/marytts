package marytts.config;

import marytts.exceptions.MaryConfigurationException;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.testng.Assert;
import org.testng.annotations.*;

public class PropertiesMaryConfigLoaderIT {

    @Test(expectedExceptions = MaryConfigurationException.class)
    public void testMalformedProperty() throws Exception {
	String test_str = "incorrect = test";
	InputStream stream = new ByteArrayInputStream(test_str.getBytes(StandardCharsets.UTF_8.name()));
	PropertiesMaryConfigLoader loader = new PropertiesMaryConfigLoader();
	loader.loadConfiguration("test", stream);
    }

    @Test(expectedExceptions = MaryConfigurationException.class)
    public void testMalformedClassName() throws Exception {
	String test_str = "nonexistingclass.method = test";
	InputStream stream = new ByteArrayInputStream(test_str.getBytes(StandardCharsets.UTF_8.name()));
	PropertiesMaryConfigLoader loader = new PropertiesMaryConfigLoader();
	loader.loadConfiguration("test", stream);
    }


    @Test(expectedExceptions = MaryConfigurationException.class)
    public void testMalformedMethodName() throws Exception {
	String test_str = "marytts.modules.nlp.JPhonemiser.unknownmethod = test";
	InputStream stream = new ByteArrayInputStream(test_str.getBytes(StandardCharsets.UTF_8.name()));
	PropertiesMaryConfigLoader loader = new PropertiesMaryConfigLoader();
	loader.loadConfiguration("test", stream);
    }

    @Test
    public void testDumpAvailableConfiguration() throws Exception {
	System.out.println(MaryConfigurationFactory.dump());
    }
}
