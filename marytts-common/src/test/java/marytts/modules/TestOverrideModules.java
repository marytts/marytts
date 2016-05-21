package marytts.modules;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;

import marytts.*;

import marytts.modules.ModuleRegistry;
import marytts.modules.DummyModule;
import marytts.modules.MaryModule;
import marytts.datatypes.MaryDataType;
import java.util.Properties;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Assert that we can override the module part using a system property
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class TestOverrideModules {
	MaryInterface mary;

	@Before
	public void setUp() throws Exception {
		Properties props = System.getProperties();
		props.setProperty("modules.preferred.classes.list", "marytts.modules.DummyModule");
		ModuleRegistry.registerModule(new DummyModule(), null, null);
		mary = new LocalMaryInterface();
	}

	@Test
	public void testSystemPreferredModuleOverride() throws Exception {
		List<MaryModule> mod = ModuleRegistry.getPreferredModulesForInputType(MaryDataType.TEXT);
		assertNotNull(mod);
		assert (!mod.isEmpty());
		assertEquals(mod.get(0).name(), "Dummy");
	}
}
