package marytts.config;

import marytts.exceptions.MaryConfigurationException;
import marytts.config.MaryProperties;

import org.testng.Assert;
import org.testng.annotations.*;

/**
 * @author marc
 *
 */
public class MainConfigTest {

    private PropertiesMaryConfig mc;

    @BeforeTest
    public void setUp() throws MaryConfigurationException {
        mc = new MainConfig();
    }

    @Test
    public void isMainConfig() {
        Assert.assertTrue(mc.isMainConfig());
    }

    @Test
    public void hasProperties() {
        Assert.assertNotNull(mc.getProperties());
    }

    @Test
    public void hasModules() {
        Assert.assertNotNull(MaryProperties.moduleInitInfo());
    }

    @Test
    public void hasSynthesizers() {
        Assert.assertNotNull(MaryProperties.synthesizerClasses());
    }

    @Test
    public void hasEffects() {
        Assert.assertNotNull(MaryProperties.effectClasses());
    }
}
