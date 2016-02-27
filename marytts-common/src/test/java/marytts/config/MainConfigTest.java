/**
 * 
 */
package marytts.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import marytts.exceptions.MaryConfigurationException;
import marytts.server.MaryProperties;

import org.junit.Before;
import org.junit.Test;

/**
 * @author marc
 *
 */
public class MainConfigTest {

	private MaryConfig mc;

	@Before
	public void setUp() throws MaryConfigurationException {
		mc = new MainConfig();
	}

	@Test
	public void isMainConfig() {
		assertTrue(mc.isMainConfig());
	}

	@Test
	public void hasProperties() {
		assertNotNull(mc.getProperties());
	}

	@Test
	public void hasModules() {
		assertNotNull(MaryProperties.moduleInitInfo());
	}

	@Test
	public void hasSynthesizers() {
		assertNotNull(MaryProperties.synthesizerClasses());
	}

	@Test
	public void hasEffects() {
		assertNotNull(MaryProperties.effectClasses());
	}
}
